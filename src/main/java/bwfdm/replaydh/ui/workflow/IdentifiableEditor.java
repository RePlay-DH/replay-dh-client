/*
 * Unless expressly otherwise stated, code from this project is licensed under the MIT license [https://opensource.org/licenses/MIT].
 * 
 * Copyright (c) <2018> <Markus Gärtner, Volodymyr Kushnarenko, Florian Fritze, Sibylle Hermann and Uli Hahn>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A 
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package bwfdm.replaydh.ui.workflow;

import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.DefaultComponentFactory;
import com.jgoodies.forms.factories.Forms;
import com.jgoodies.forms.factories.Paddings;

import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.DocumentAdapter;
import bwfdm.replaydh.ui.helper.Editor;
import bwfdm.replaydh.ui.helper.EditorControl;
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.ui.icons.Resolution;
import bwfdm.replaydh.ui.workflow.IdentifiableEditor.EditProxy;
import bwfdm.replaydh.utils.Mutable.MutableObject;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifiable.Type;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Person;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.Tool;
import bwfdm.replaydh.workflow.impl.DefaultPerson;
import bwfdm.replaydh.workflow.impl.DefaultTool;
import bwfdm.replaydh.workflow.schema.CompoundLabel;
import bwfdm.replaydh.workflow.schema.IdentifierSchema;
import bwfdm.replaydh.workflow.schema.IdentifierType;
import bwfdm.replaydh.workflow.schema.IdentifierType.Uniqueness;
import bwfdm.replaydh.workflow.schema.LabelSchema;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * An editor for sets of {@link Identifiable identifiables}.
 * Note that this implementation operates directly on the provided
 * identifiables and doesn't use backups or proxies to store the
 * original contents.
 *
 * @author Markus Gärtner
 *
 */
public class IdentifiableEditor implements Editor<Set<EditProxy>>, ListSelectionListener {

	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * <pre>
	 * +--------+-------------------------+
	 * |     DESCRIPTION + HELP           |
	 * +--------+-------------------------+
	 * |        |  Path/URL  ___________  |
	 * |   L    |  Desc      ___________  |
	 * |   I    |  Type      ___________  |
	 * |   S    |  Identifiers         +  |
	 * |   T    |     __________________  |
	 * |        |     __________________  |
	 * |        |        <INFO>           |
	 * |        |                         |
	 * |        |    DONE    IGNORE       |
	 * +--------+-------------------------+
	 * </pre>
	 *
	 * Modes:
	 *
	 *
	 */

	private final Identifiable.Type type;

	private final Function<Identifiable, IdentifierType> titleSelector;

	private final JList<EditProxy> identifiableList;
	private final JTextArea taHeader;
	private final JButton bDone, bIgnore, bAddIdentifier;
	private final JTextField tfTitle; //supposed to not be editable
	private final JTextArea taDescription;
	private final JTextField tfParameters;
	private final JTextArea taEnvironment;
	private final JComboBox<CompoundLabel> cbRoleType;
	private final JSplitPane splitPane;
	private final JPanel itemPanel;
	private final JPanel identifierPanel;

	private final JLabel titleLabel;
	private final JTextArea taInfo;

	private final JPanel panel;

	private final WorkflowSchema schema;

	private final CompoundLabel defaultTypeOrRole;

	private final Set<EditProxy> identifiers = new HashSet<>();

	private Set<EditProxy> editingItem;
	private EditProxy currentIdentifiable;

	private Identifier titleIdentifier;
	private boolean contentChanged = false;

	private final String UNNAMED_ID;

	//FIXME make use of the editor control to provide feedback
	private EditorControl editorControl;

	public static void main(String[] args) throws Exception {
		WorkflowSchema schema = WorkflowSchema.getDefaultSchema();


		Person person = DefaultPerson.uniquePerson();
		person.addIdentifier(new Identifier(schema.getPersonIdentifierSchema().getDefaultIdentifierType(), "John"));

		Tool tool = DefaultTool.uniqueTool();
		tool.addIdentifier(new Identifier(schema.getResourceIdentifierSchema().getDefaultIdentifierType(), "XYZ"));
		tool.setEnvironment("Win 7, 64 bit, ...");
		tool.setParameters("-v -file D://x/y/z/masterFolder/out.xml");

		Identifiable identifiable = tool;
		Identifiable.Type type = identifiable.getType();

		IdentifiableEditor editor = newBuilder()
				.schema(schema)
				.type(type)
				.useDefaultTitleSelector()
				.build();
		JFrame frame = new JFrame("Test");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

		editor.setEditingItem(wrap(Collections.singleton(identifiable)));

		GuiUtils.showEditorDialogWithControl(frame, editor, true);

		frame.setVisible(false);
	}

	protected IdentifiableEditor(Builder builder) {
		schema = builder.getSchema();
		type = builder.getType();
		titleSelector = builder.getTitleSelector();

		ResourceManager rm = ResourceManager.getInstance();
		IconRegistry ir = IconRegistry.getGlobalRegistry();

		UNNAMED_ID = rm.get("replaydh.ui.editor.identifiable.unnamed");

		panel = new JPanel(new BorderLayout());

		// LEFT AREA
		identifiableList = new JList<>(new DefaultListModel<>());
		identifiableList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		identifiableList.setCellRenderer(new WorkflowUIUtils.EditProxyCellRenderer());
		JScrollPane leftScrollPane = new JScrollPane(identifiableList);

		// TOP AREA
		taHeader = GuiUtils.createTextArea(null);
		panel.add(taHeader, BorderLayout.NORTH);

		// RIGHT AREA
		tfTitle = new JTextField();
		tfTitle.setEditable(builder.isTitleEditable());
		if(tfTitle.isEditable()) {
			String typeLabel = type.getDisplayLabel();
			tfTitle.setToolTipText(rm.get("replaydh.ui.editor.identifiable.titleTooltip", typeLabel));
			tfTitle.getDocument().addDocumentListener(new DocumentAdapter() {
				@Override
				public void anyUpdate(DocumentEvent e) {
					refreshTitle();
					refreshControl();
				}
			});
			GuiUtils.autoSelectFullContent(tfTitle);
			GuiUtils.prepareChangeableBorder(tfTitle);
		}

		tfParameters = GuiUtils.autoSelectFullContent(new JTextField());

		taEnvironment = GuiUtils.autoSelectFullContent(new JTextArea());
		taEnvironment.setRows(3);

		taDescription = GuiUtils.autoSelectFullContent(new JTextArea());
		taDescription.setRows(3);

		bDone = new JButton(rm.get("replaydh.ui.editor.resourceCache.confirm.label"));
		bDone.setToolTipText(rm.get("replaydh.ui.editor.resourceCache.confirm.description"));
		bDone.addActionListener(this::onDoneButtonClicked);

		bIgnore = new JButton(rm.get("replaydh.ui.editor.resourceCache.ignore.label"));
		bIgnore.setToolTipText(rm.get("replaydh.ui.editor.resourceCache.ignore.description"));
		bIgnore.addActionListener(this::onIgnoreButtonClicked);

		bAddIdentifier = new JButton(ir.getIcon("add_obj.gif", Resolution.forSize(16)));
		bAddIdentifier.setToolTipText(rm.get("replaydh.ui.editor.resourceCache.addIdentifier.description"));
		bAddIdentifier.setPreferredSize(new Dimension(18, 18));
		bAddIdentifier.addActionListener(this::onAddIdentifierButtonClicked);

		cbRoleType = WorkflowUIUtils.createLabelComboBox(getLabelSchema());
		defaultTypeOrRole = WorkflowUIUtils.createDefaultLabel(getLabelSchema());
		cbRoleType.setSelectedItem(defaultTypeOrRole);
		cbRoleType.addActionListener(this::onRoleTypeComboBoxClicked);

		identifierPanel = new JPanel();
		identifierPanel.setLayout(new BoxLayout(identifierPanel, BoxLayout.Y_AXIS));
		identifierPanel.setBorder(BorderFactory.createEmptyBorder(3, 20, 3, 3));
		GuiUtils.prepareChangeableBorder(identifierPanel);

		titleLabel = DefaultComponentFactory.getInstance().createLabel(null);

		taInfo = GuiUtils.createTextArea(null);
		taInfo.setForeground(Color.red);
		taInfo.setBorder(Paddings.DLU2);

		String descLabel = rm.get("replaydh.ui.editor.identifiable.description");
		String typeLabel = rm.get("replaydh.ui.editor.identifiable.type");
		String identifiersLabel = rm.get("replaydh.ui.editor.identifiable.identifiers");

		boolean showToolFields = isToolEditor();

		itemPanel = FormBuilder.create()
				.columns("max(35dlu;pref), 2dlu, pref:grow:fill, 2dlu, pref")
				.rows("pref, $lg, pref, $lg, pref, $lg, pref, $lg, pref, $nlg, pref, max(50dlu;pref):grow:fill, $nlg, pref")
				.add(titleLabel).xy(1, 1, "left, top").add(tfTitle).xyw(3, 1, 3)
				.addLabel(descLabel+":").xy(1, 3, "left, top").addScrolled(taDescription).xyw(3, 3, 3)
				.addLabel(showToolFields, rm.get("replaydh.ui.editor.identifiable.parameters")+":").xy(1, 5, "left, top").add(showToolFields, tfParameters).xyw(3, 5, 3)
				.addLabel(showToolFields, rm.get("replaydh.ui.editor.identifiable.environment")+":").xy(1, 7, "left, top").addScrolled(showToolFields, taEnvironment).xyw(3, 7, 3)
				.addLabel(typeLabel+":").xy(1, 9, "left, top").add(cbRoleType).xyw(3, 9, 3)
				.addSeparator(identifiersLabel).xyw(1, 11, 3, "left, top").add(bAddIdentifier).xy(5, 11)
				.add(identifierPanel).xyw(1, 12, 5, "fill, top")
				.add(Forms.buttonBar(bIgnore, bDone)).xyw(1, 14, 5, "center, bottom")
				.padding(Paddings.DLU2)
				.build();
		panel.setMinimumSize(panel.getPreferredSize());

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, leftScrollPane, itemPanel);
		splitPane.setResizeWeight(0);
		splitPane.setOneTouchExpandable(false);
		splitPane.setBorder(null);
		panel.add(splitPane, BorderLayout.CENTER);
	}

	public boolean isPersonEditor() {
		return type==Type.PERSON;
	}

	public boolean isToolEditor() {
		return type==Type.TOOL;
	}

	private IdentifierSchema getIdentifierSchema() {
		return isPersonEditor() ? schema.getPersonIdentifierSchema() : schema.getResourceIdentifierSchema();
	}

	private LabelSchema getLabelSchema() {
		return isPersonEditor() ? schema.getRoleSchema() : schema.getResourceTypeSchema();
	}

	@Override
	public void setControl(EditorControl control) {
		editorControl = control;

		refresh();
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.Editor#getEditorComponent()
	 */
	@Override
	public Component getEditorComponent() {
		return panel;
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.Editor#setEditingItem(java.lang.Object)
	 */
	@Override
	public void setEditingItem(Set<EditProxy> item) {
		this.editingItem = item;

		resetEdit();
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.Editor#getEditingItem()
	 */
	@Override
	public Set<EditProxy> getEditingItem() {
		return editingItem;
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.Editor#resetEdit()
	 */
	@Override
	public void resetEdit() {

		identifiers.clear();
		contentChanged = false;

		if(editingItem!=null) {
			identifiers.addAll(editingItem);
		}

		// Special handling of single item edits
		if(identifiers.size()==1) {
			identifiers.iterator().next().state = EditState.IGNORED;
		}

		refresh();
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.Editor#applyEdit()
	 */
	@Override
	public void applyEdit() {
		editingItem.clear();
		editingItem.addAll(identifiers);
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.Editor#hasChanges()
	 */
	@Override
	public boolean hasChanges() {
		return contentChanged;
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.Editor#close()
	 */
	@Override
	public void close() {
		editingItem = null;
		identifiers.clear();
	}

	private boolean hasMultipleResources() {
		return identifiers.size()>1;
	}

	private boolean isValidTitle() {
		String text = tfTitle.getText();

		return text!=null && !text.isEmpty()
				&& !UNNAMED_ID.equalsIgnoreCase(text);
	}

	private void refresh() {
		DefaultListModel<EditProxy> model = (DefaultListModel<EditProxy>) identifiableList.getModel();
		model.removeAllElements();

		boolean needsMultiResourceView = hasMultipleResources();

		bDone.setVisible(needsMultiResourceView);
		bIgnore.setVisible(needsMultiResourceView);

		if(needsMultiResourceView) {
			// Prepare identifiers list
			List<EditProxy> tmp = new ArrayList<>(identifiers);
			tmp.sort(RESOURCE_SORTER);
			tmp.forEach(model::addElement);

			// "Grow" panel
			splitPane.setRightComponent(itemPanel);
			panel.add(splitPane, BorderLayout.CENTER);
			identifiableList.setSelectedIndex(0);
		} else {
			// "Shrink" panel
			panel.add(itemPanel, BorderLayout.CENTER);

			EditProxy proxy = null;

			if(!identifiers.isEmpty()) {
				proxy = identifiers.iterator().next();
			}

			displayIdentifiable(proxy);
		}
	}

	private void refreshTitle() {
		// Only bother if the user can actually edit the title!
		if(!tfTitle.isEditable()) {
			return;
		}

		boolean isValidId = isValidTitle();

		if(currentIdentifiable!=null && titleIdentifier!=null) {

			// Remove old title identifier
			Identifiable identifiable = currentIdentifiable.getTarget();
			if(identifiable.hasIdentifier(titleIdentifier)) {
				identifiable.removeIdentifier(titleIdentifier);
			}

			if(isValidId) {
				// Create new identifier usable as title
				Identifier newTitleIdentifier = new Identifier(titleIdentifier.getType(), tfTitle.getText());

				// Provide new (potentially incomplete) identifier
				identifiable.addIdentifier(newTitleIdentifier);

				titleIdentifier = newTitleIdentifier;
			}
		}

		GuiUtils.toggleChangeableBorder(tfTitle, !isValidId);
	}

	private void refreshIdentifierInfo() {
		if(isPersonEditor()) {
			return;
		}

		identifierPanel.remove(taInfo);

		if(currentIdentifiable!=null) {
			Identifiable identifiable = currentIdentifiable.getTarget();
			if(identifiable.getIdentifier(IdentifierType.PATH, IdentifierType.URL)==null) {
				String infoText = ResourceManager.getInstance().get(
						"replaydh.ui.editor.identifiable.requiredIdentifiers");

				infoText += "\n"+schema.getDefaultPathIdentifierType().getName();
				infoText += "\n"+schema.getDefaultURLIdentifierType().getName();

				taInfo.setText(infoText);
				taInfo.setRows(3);

				identifierPanel.add(taInfo);
			}
		}
	}

	private void displayIdentifiable(EditProxy proxy) {
//		if(Objects.equals(currentIdentifiable, proxy)) {
//			return;
//		}

		if(currentIdentifiable!=null && hasMultipleResources()) {
			if(currentIdentifiable.state==EditState.EDITING) {
				//TODO ???
			}
		}

		currentIdentifiable = proxy;

		identifierPanel.removeAll();

		if(currentIdentifiable==null) {
			titleIdentifier = null;
			tfTitle.setText(null);
			taDescription.setText(null);
			cbRoleType.setSelectedItem(null);
		} else {
			Identifiable identifiable = proxy.getTarget();

			// Pick default identifier for title
			titleIdentifier = getTitleIdentifier(identifiable);

			titleLabel.setText(titleIdentifier.getType().getName());
			titleLabel.setToolTipText(titleIdentifier.getType().getDescription());

			tfTitle.setText(titleIdentifier.getId());

			taDescription.setText(identifiable.getDescription());

			// Find or create the correct target type label
			ComboBoxModel<CompoundLabel> cbModel = cbRoleType.getModel();

			String label = getTypeOrRoleLabel(identifiable);
			CompoundLabel selectedLabel = getSelectedLabel(cbModel, getLabelSchema(), label);
			cbRoleType.setSelectedItem(selectedLabel);

			// Special treatment of tools
			if(isToolEditor()) {
				Tool tool = (Tool) identifiable;
				tfParameters.setText(tool.getParameters());
				taEnvironment.setText(tool.getEnvironment());
			}

			// Fetch remaining identifier types
			List<Identifier> identifiers = new ArrayList<>(identifiable.getIdentifiers());
			identifiers.remove(titleIdentifier);
			Collections.sort(identifiers);

			// Add identifiers in sorted order to panel
			for(Identifier identifier : identifiers) {
				addIdentifierPanel(identifier);
			}
		}

		refreshTitle();
		refreshControl();
		refreshIdentifierInfo();
	}

	/**
	 *
	 * @param identifiable
	 * @return
	 *
	 * @see #isPersonEditor()
	 */
	private Identifier getTitleIdentifier(Identifiable identifiable) {
		IdentifierType type = titleSelector.apply(identifiable);

		Identifier identifier = identifiable.getIdentifier(type);
		if(identifier==null) {
			identifier = new Identifier(type, UNNAMED_ID);
		}

		return identifier;
	}

	/**
	 *
	 * @param identifiable
	 * @return
	 *
	 * @see #isPersonEditor()
	 */
	private String getTypeOrRoleLabel(Identifiable identifiable) {
		return isPersonEditor() ? ((Person)identifiable).getRole() : ((Resource)identifiable).getResourceType();
	}

	private CompoundLabel getSelectedLabel(ComboBoxModel<CompoundLabel> model, LabelSchema schema, String label) {
		CompoundLabel selectedLabel = null;

		if(label!=null) {
			for(int i=0; i<model.getSize(); i++) {
				CompoundLabel cl = model.getElementAt(i);
				if(cl.toString().equals(label)) {
					selectedLabel = cl;
					break;
				}
			}
			if(selectedLabel==null) {
				selectedLabel = new CompoundLabel(schema, label);
			}
		}
		if(selectedLabel==null) {
			selectedLabel = defaultTypeOrRole;
		}

		return selectedLabel;
	}

	private void addIdentifierPanel(Identifier identifier) {
		identifierPanel.add(new IdentifierPanel(identifier));
		identifierPanel.revalidate();
		identifierPanel.repaint();

		refreshIdentifierInfo();
	}

	private void removeIdentifierPanel(IdentifierPanel panel) {
		removeIdentifierFromCurrentIdentifiable(panel.getIdentifier());
		identifierPanel.remove(panel);
		identifierPanel.revalidate();
		identifierPanel.repaint();

		refreshIdentifierInfo();
	}

	private void onDoneButtonClicked(ActionEvent ae) {
		//TODO
	}

	private void onIgnoreButtonClicked(ActionEvent ae) {
		//TODO
	}

	private void onRoleTypeComboBoxClicked(ActionEvent ae) {
		// Fetch target type and assign to current target
		Object selectedItem = cbRoleType.getSelectedItem();

		String resourceType = null;
		if(selectedItem instanceof String) {
			resourceType = (String) selectedItem;
		} else if(selectedItem instanceof CompoundLabel) {
			resourceType = ((CompoundLabel)selectedItem).getLabel();
		}

		if(resourceType!=null) {
			setResourceTypeOrRoleToCurrentResource(resourceType);
		}
	}

	private void onAddIdentifierButtonClicked(ActionEvent ae) {
		String title = ResourceManager.getInstance().get("replaydh.ui.editor.identifiable.addIdentifier.title");

		// Filter by all the already selected identifiers for current target
		Set<IdentifierType> usedTypes = new HashSet<>();
		if(titleIdentifier!=null) {
			usedTypes.add(titleIdentifier.getType());
		}
		for(int i=0; i<identifierPanel.getComponentCount(); i++) {
			Component comp = identifierPanel.getComponent(i);
			if(comp instanceof IdentifierPanel) {
				usedTypes.add(((IdentifierPanel)comp).identifier.getType());
			}
		}
		Predicate<IdentifierType> filter = usedTypes::contains;

		// Delegate UI work
		Identifier identifier = WorkflowUIUtils.showIdentifierDialog(
				panel, title, getIdentifierSchema(), filter);

		// If user actually chose a valid identifier, add it and refresh UI
		if(identifier!=null) {
			currentIdentifiable.getTarget().addIdentifier(identifier);
			// Currently just appends new identifiers. TODO maybe do a sorted insert?
			addIdentifierPanel(identifier);
		}

		refreshControl();
	}

	/**
	 *
	 * @param label
	 *
	 * @see #isPersonEditor()
	 */
	private void setResourceTypeOrRoleToCurrentResource(String label) {
		if(currentIdentifiable==null) {
			return;
		}

		if(isPersonEditor()) {
			((Person)currentIdentifiable.getTarget()).setRole(label);
		} else {
			((Resource)currentIdentifiable.getTarget()).setResourceType(label);
		}

		refreshControl();
	}

	private void removeIdentifierFromCurrentIdentifiable(Identifier identifier) {
		if(currentIdentifiable==null) {
			return;
		}

		currentIdentifiable.getTarget().removeIdentifier(identifier);

		refreshControl();
	}

	/**
	 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
	 */
	@Override
	public void valueChanged(ListSelectionEvent e) {
		EditProxy proxy = identifiableList.getSelectedValue();
		if(proxy!=null) {
			displayIdentifiable(proxy);
		}
	}

	/**
	 * Uses the {@link EditorControl} if available and
	 * only allows the editor dialog to continue if current
	 * content is {@link #isContentValid() valid}.
	 */
	private void refreshControl() {
		if(editorControl==null) {
			return;
		}

		GuiUtils.invokeEDT(() -> editorControl.setApplyEnabled(isContentValid()));
	}

	/**
	 * Returns {@code true} if the current content of this editor is valid,
	 * i.e. in case all of the following are met:
	 *
	 * <ul>
	 * <li>All identifiables are ignored</li>
	 * <li>All non-ignored identifiables have a valid role and or type assigned</li>
	 * <li>All non-ignored identifiables have another identifier assigned besides the basic
	 * {@link IdentifierType#PATH} or {@link IdentifierType#URL}</li>
	 * </ul>
	 *
	 * @return
	 */
	public boolean isContentValid() {
		boolean contentValid = isValidTitle();

		if(contentValid) {
			for(EditProxy proxy : identifiers) {
				if(proxy.isIgnored()) {
					continue;
				}

				if(!proxy.isDone()) {
					contentValid = false;
				} else {
					Identifiable identifiable = proxy.target;

					boolean basicIdentifierMet = true;

					switch (type) {
					case PERSON:
						contentValid &= ((Person) identifiable).getRole()!=null;
						break;

					case TOOL:
					case RESOURCE:
						contentValid &= ((Resource) identifiable).getResourceType()!=null;
						basicIdentifierMet &= identifiable.getIdentifier(IdentifierType.PATH, IdentifierType.URL) != null;
						break;

					default:
						break;
					}

					contentValid &= basicIdentifierMet;
				}

				if(!contentValid) {
					break;
				}
			}
		}

		return contentValid;
	}

	/**
	 * Sorts identifiers by their main identifier
	 */
	private static final Comparator<EditProxy> RESOURCE_SORTER = new Comparator<EditProxy>() {

		private MutableObject<String> labelBuffer = new MutableObject<>();

		private String getLabel(Identifiable identifiable) {
			labelBuffer.clear();

			identifiable.forEachIdentifier(i -> {
				if(i.getType().getUniqueness()!=Uniqueness.HASH) {
					labelBuffer.set(i.getId());
				}
			});

			return labelBuffer.get();
		}

		@Override
		public int compare(EditProxy o1, EditProxy o2) {

			Identifiable r1 = o1.target;
			Identifiable r2 = o2.target;

			int result = r1.getType().compareTo(r2.getType());

			if(result==0) {
				result = getLabel(r1).compareTo(getLabel(r2));
			}

			return result;
		}
	};

	private enum EditState {
		BLANK,
		EDITING,
		DONE,
		IGNORED,
		;
	}

	public static class EditProxy {
		private final Identifiable target;
		private EditState state = EditState.BLANK;

		private EditProxy(Identifiable target) {
			this.target = requireNonNull(target);
		}

		@SuppressWarnings("unchecked")
		public <I extends Identifiable> I getTarget() {
			return (I)target;
		}

		public boolean isDone() {
			return state.compareTo(EditState.DONE)>=0;
		}

		public boolean isIgnored() {
			return state.compareTo(EditState.IGNORED)>=0;
		}
	}

	/**
	 * Wraps the given identifiers into proxies.
	 *
	 * @param identifiers
	 * @return
	 */
	public static Set<EditProxy> wrap(Set<? extends Identifiable> resources) {
		Set<EditProxy> proxies = new HashSet<>();

		for(Identifiable identifiable : resources) {
			proxies.add(new EditProxy(identifiable));
		}

		return proxies;
	}

	/**
	 * Unwraps all the proxies that are done and have not been ignored.
	 *
	 * @param proxies
	 * @return
	 */
	public static <I extends Identifiable> Set<I> unwrap(Set<EditProxy> proxies) {
		Set<I> result = new HashSet<>();

		boolean canDiscardStates = proxies.size()==1;

		for(EditProxy proxy : proxies) {
			if(canDiscardStates || (proxy.isDone() && !proxy.isIgnored())) {
				result.add(proxy.getTarget());
			}
		}

		return result;
	}

	/**
	 * Panel to display a single {@link Identifier} with the option to remove it
	 * from the host {@link Identifiable}
	 *
	 * @author Markus Gärtner
	 *
	 * @see IdentifiableEditor#removeIdentifierPanel(IdentifierPanel)
	 */
	private class IdentifierPanel extends JPanel implements ActionListener {

		private static final long serialVersionUID = -3977881752966957467L;

		private final Identifier identifier;

		IdentifierPanel(Identifier identifier) {
			super(new BorderLayout());

			this.identifier = requireNonNull(identifier);

			// Limit horizontal size of identifier text
			String id = identifier.getId();
			if(id.length()>30) {
				id = id.substring(0, 24)+" [...]";
			}
			String text = identifier.getType().getName()+": "+id;
			JLabel label = new JLabel(text);

			// In tooltip always present the entire identifier
			String tooltip = identifier.getType().getDescription()+"\n\n"+identifier.getId();
			label.setToolTipText(GuiUtils.toUnwrappedSwingTooltip(tooltip));
			add(label, BorderLayout.CENTER);

			JButton button = new JButton(IconRegistry.getGlobalRegistry().getIcon(
					"delete_obj.gif", Resolution.forSize(16)));
			button.addActionListener(this);
			button.setPreferredSize(new Dimension(18, 18));
			add(button, BorderLayout.EAST);
		}

		public Identifier getIdentifier() {
			return identifier;
		}

		/**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			removeIdentifierPanel(this);
		}
	}

	public static final Function<Identifiable, Identifier> DEFAULT_TITLE_LIST = identifiable -> {

		if(identifiable.getType()==Identifiable.Type.PERSON) {
			return identifiable.getIdentifier(IdentifierType.NAME);
		} else {
			return identifiable.getIdentifier(IdentifierType.NAME_VERSION, IdentifierType.PATH, IdentifierType.URL);
		}
	};

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	public static class DefaultTitleSelector implements Function<Identifiable, IdentifierType> {

		private final WorkflowSchema schema;

		/**
		 * Flag to only allow actual 'names' as title identifiers, no URL or PATH
		 */
		private final boolean namesOnly;

		public DefaultTitleSelector(WorkflowSchema schema) {
			this(schema, false);
		}

		public DefaultTitleSelector(WorkflowSchema schema, boolean namesOnly) {
			this.schema = requireNonNull(schema);
			this.namesOnly = namesOnly;
		}

		/**
		 * @see java.util.function.Function#apply(java.lang.Object)
		 */
		@Override
		public IdentifierType apply(Identifiable identifiable) {

			IdentifierType fallbackType = null;
			Identifier presentIdentifier = null;

			if(identifiable.getType()==Identifiable.Type.PERSON) {
				presentIdentifier = identifiable.getIdentifier(IdentifierType.NAME);
				fallbackType = schema.getDefaultNameIdentifierType();
			} else {
				if(namesOnly) {
					presentIdentifier = identifiable.getIdentifier(IdentifierType.NAME_VERSION);
				} else {
					presentIdentifier = identifiable.getIdentifier(IdentifierType.NAME_VERSION, IdentifierType.PATH, IdentifierType.URL);
				}
				fallbackType = schema.getDefaultNameVersionIdentifierType();
			}

			if(presentIdentifier!=null) {
				return presentIdentifier.getType();
			} else {
				return fallbackType;
			}
		}

	}

	public static final boolean DEFAULT_TITLE_EDITABLE = false;

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	public static class Builder {

		private WorkflowSchema schema;
		private Identifiable.Type type;
		private Function<Identifiable, IdentifierType> titleSelector;

		private Boolean titleEditable;

		private Builder() {
			// no-op
		}

		public Builder titleEditable(boolean titleEditable) {
			checkState("Flag 'titleEditable' already set", this.titleEditable==null);

			this.titleEditable = titleEditable==DEFAULT_TITLE_EDITABLE ? null : Boolean.valueOf(titleEditable);

			return this;
		}

		public Builder schema(WorkflowSchema schema) {
			requireNonNull(schema);
			checkState("Schema already set", this.schema==null);

			this.schema = schema;

			return this;
		}

		public Builder type(Identifiable.Type type) {
			requireNonNull(type);
			checkState("Type already set", this.type==null);

			this.type = type;

			return this;
		}

		public Builder titleSelector(Function<Identifiable, IdentifierType> titleSelector) {
			requireNonNull(titleSelector);
			checkState("Title selector already set", this.titleSelector==null);

			this.titleSelector = titleSelector;

			return this;
		}

		public Builder useDefaultTitleSelector() {
			checkState("Need schema to be set before assigning default title selector", schema!=null);
			return titleSelector(new DefaultTitleSelector(schema));
		}

		public Builder useDefaultTitleSelector(boolean namesOnly) {
			checkState("Need schema to be set before assigning default title selector", schema!=null);
			return titleSelector(new DefaultTitleSelector(schema, namesOnly));
		}

		public WorkflowSchema getSchema() {
			return schema;
		}

		public Identifiable.Type getType() {
			return type;
		}

		public Function<Identifiable, IdentifierType> getTitleSelector() {
			return titleSelector;
		}

		public boolean isTitleEditable() {
			return titleEditable==null ? DEFAULT_TITLE_EDITABLE : titleEditable.booleanValue();
		}

		protected void validate() {
			checkState("Schema missing", schema!=null);
			checkState("Type missing", type!=null);
			checkState("Title selector missing", titleSelector!=null);
		}

		public IdentifiableEditor build() {
			validate();

			return new IdentifiableEditor(this);
		}
	}
}

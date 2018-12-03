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

import java.awt.Component;
import java.awt.Frame;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.function.Predicate;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;

import bwfdm.replaydh.io.LocalFileObject;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.DocumentAdapter;
import bwfdm.replaydh.utils.Label;
import bwfdm.replaydh.utils.Mutable.MutableObject;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifiable.Role;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.WorkflowUtils;
import bwfdm.replaydh.workflow.impl.DefaultResource;
import bwfdm.replaydh.workflow.schema.CompoundLabel;
import bwfdm.replaydh.workflow.schema.IdentifierSchema;
import bwfdm.replaydh.workflow.schema.IdentifierType;
import bwfdm.replaydh.workflow.schema.IdentifierType.Uniqueness;
import bwfdm.replaydh.workflow.schema.LabelSchema;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;
import bwfdm.replaydh.workflow.schema.impl.IdentifierTypeImpl;

/**
 * @author Markus Gärtner
 *
 */
public final class WorkflowUIUtils {

	public static class LabelCellRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = 7558903382948353704L;

		/**
		 * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
		 */
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {

			String tooltip = null;

			if(value instanceof CompoundLabel) {
				CompoundLabel label = (CompoundLabel) value;

				if(label.getSubLabel()==null) {
					value = label.getMainLabelString();
					tooltip = label.getMainLabelDescription();
				} else {
					value = "   "+label.getSeparator()+label.getSubLabelString();
					tooltip = label.getSubLabelDescription();
				}
			}

			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			setToolTipText(tooltip);

			return this;
		}
	}

	public static class EditProxyCellRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = -1514081471047843359L;

		/**
		 * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
		 */
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {

			String tooltip = null;
			Icon icon = null;

			if(value instanceof IdentifiableEditor.EditProxy) {
				IdentifiableEditor.EditProxy proxy = (IdentifiableEditor.EditProxy) value;

				//TODO
			}

			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			setToolTipText(tooltip);
			setIcon(icon);

			return this;
		}
	}

	private WorkflowUIUtils() {
		// no external instantiation
	}

	public static Identifier showIdentifierDialog(Component parent, String title,
			IdentifierSchema schema, Predicate<? super IdentifierType> filter) {

		ResourceManager rm = ResourceManager.getInstance();

		final JButton bOk = new JButton(rm.get("replaydh.labels.ok"));
		final JButton bCancel = new JButton(rm.get("replaydh.labels.cancel"));

		final JComboBox<IdentifierTypeProxy> cbType = createIdentifierTypeComboBox(schema, filter);

		final JTextField tfId = GuiUtils.autoSelectFullContent(new JTextField());

		final Runnable checkInput = () -> {
			String text = tfId.getText();
			Object type = cbType.getSelectedItem();

			boolean inputValid = text!=null && !text.isEmpty() && type!=null;

			if(type instanceof String) {
				inputValid &= !((String)type).isEmpty();
			}

			bOk.setEnabled(inputValid);
		};

		cbType.addActionListener(a -> checkInput.run());

		tfId.getDocument().addDocumentListener(new DocumentAdapter() {
			@Override
			public void anyUpdate(DocumentEvent e) {
				checkInput.run();
			}
		});

		/**
		 * <pre>
		 * +---------------+
		 * |     INFO      |
		 * | TYPE:  ______ |
		 * | NAME:  ______ |
		 * |               |
		 * |  OK    CANCEL |
		 * +---------------+
		 * </pre>
		 */

		JTextArea taInfo = GuiUtils.createTextArea(rm.get("replaydh.ui.editor.identifiable.addIdentifier.description"));

		JPanel panel = FormBuilder.create()
				.columns("pref:grow, 4dlu, pref:grow")
				.rows("max(pref;20dlu), 7dlu, pref, $lg, pref, 6dlu, pref")
				.columnGroups(new int[] {1,3})
				.add(taInfo).xyw(1, 1, 3, "fill, top")
				.addLabel(rm.get("replaydh.labels.type")).xy(1, 3, "left, center")
				.add(cbType).xy(3, 3, "fill, center")
				.addLabel(rm.get("replaydh.labels.name")).xy(1, 5, "left, center")
				.add(tfId).xy(3, 5, "fill, center")
				.add(bOk).xy(1, 7, "right, bottom")
				.add(bCancel).xy(3, 7, "left, bottom")
				.padding(Paddings.DIALOG)
				.build();

		final Frame owner = parent==null ? null : (Frame)SwingUtilities.getAncestorOfClass(Frame.class, parent);

		final JDialog dialog = new JDialog(owner, title, true);
		dialog.add(panel);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);

		final MutableObject<Identifier> result = new MutableObject<>();

		bOk.addActionListener(a -> {
			Object type = cbType.getSelectedItem();
			String id = tfId.getText();

			if(type==null || id==null) {
				return;
			}

			result.set(createIdentifier(type, id));

			dialog.setVisible(false);
		});

		bCancel.addActionListener(a -> {
			result.clear();
			dialog.setVisible(false);
		});

		checkInput.run();
		dialog.setVisible(true);

		return result.get();
	}

	public static Predicate<IdentifierType> createIdentifierFilter(Identifiable identifiable) {
		Set<IdentifierType> usedTypes = new HashSet<>();
		identifiable.forEachIdentifier(i -> usedTypes.add(i.getType()));
		return usedTypes::contains;
	}

	/**
	 *
	 * @param type either a custom String or an established {@link IdentifierType}
	 * @param id
	 * @return
	 */
	private static Identifier createIdentifier(Object type, String id) {
		IdentifierType identifierType = null;

		if(type instanceof IdentifierTypeProxy) {
			IdentifierTypeProxy proxy = (IdentifierTypeProxy) type;
			if(proxy.identifierType!=null) {
				type = proxy.identifierType;
			} else {
				type = proxy.label;
			}
		}

		if(type instanceof IdentifierType) {
			identifierType = (IdentifierType) type;
		} else if(type instanceof String) {
			String label = (String) type;
			identifierType = (IdentifierType) new IdentifierTypeImpl()
					.setUniqueness(Uniqueness.AMBIGUOUS)
					.setDescription(ResourceManager.getInstance().get("replaydh.workflowSchema.customIdentifierType"))
					.setLabel(label)
					.setName(label);
		} else
			throw new IllegalArgumentException("Not a valid identifier type: "+type);

		return new Identifier(identifierType, id);
	}

	private static <E extends Object> boolean isLegal(E item, Predicate<? super E> filter) {
		return filter==null || !filter.test(item);
	}

	/**
	 * Creates a {@link JComboBox} based on the supplied {@link IdentifierSchema schema}.
	 * The combo box will contain all the identifier types defined in the schema that
	 * are not {@link Predicate#test(Object) filtered} by the optional {@link Predicate filter}.
	 * <p>
	 * If the schema {@link IdentifierSchema#allowCustomIdentifierTypes() allows custom identifiers}
	 * then the combo box will be {@link JComboBox#isEditable() editable}.
	 * <p>
	 * To account for the latter case of user defined custom identifier types the component
	 * type of the combo box is generic. Values {@linkp JComboBox#getSelectedItem() selected} by
	 * the user can be either of type {@link IdentifierType} or {@link String}.
	 *
	 * @param schema
	 * @param filter
	 * @return
	 */
	public static JComboBox<IdentifierTypeProxy> createIdentifierTypeComboBox(IdentifierSchema schema,
			Predicate<? super IdentifierType> filter) {
		Vector<IdentifierTypeProxy> values = new Vector<>();

		schema.forEachIdentifierType(type -> {
			if(isLegal(type, filter)) {
				values.add(new IdentifierTypeProxy(type));
			}
		});

		Collections.sort(values);

		DefaultComboBoxModel<IdentifierTypeProxy> model = new DefaultComboBoxModel<>(values);
		if(isLegal(schema.getDefaultIdentifierType(), filter)) {
			model.setSelectedItem(new IdentifierTypeProxy(schema.getDefaultIdentifierType()));
		}

		JComboBox<IdentifierTypeProxy> comboBox = new JComboBox<>(model);
		comboBox.setRenderer(new IdentifierTypeListCellRenderer());

		comboBox.setEditable(schema.allowCustomIdentifierTypes());

		return comboBox;
	}

	public static CompoundLabel createDefaultLabel(LabelSchema schema) {
		Label defaultLabel = schema.getDefaultLabel();
		return defaultLabel==null ? null : new CompoundLabel(schema).setMainLabel(defaultLabel);
	}

	public static JComboBox<CompoundLabel> createLabelComboBox(LabelSchema schema) {

		Vector<CompoundLabel> values = new Vector<>();

		if(!schema.getLabels().isEmpty()) {
			String separator = schema.getCompoundSeparator();
			for(Label label : schema.getLabels()) {
				// Add the main-label alone
				values.add(new CompoundLabel(schema)
						.setMainLabel(label));

				if(schema.allowCompoundLabels() && schema.allowCompoundLabel(label)) {
					for(Label subLabel : schema.getDefaultSubLabels(label)) {

						// Add a compound entry for every sub-label
						values.add(new CompoundLabel(schema)
								.setMainLabel(label)
								.setSeparator(separator)
								.setSubLabel(subLabel));
					}
				}
			}
		}

		Collections.sort(values);

		boolean editable = schema.allowCustomLabels()
				|| schema.allowCompoundLabels()
				|| values.isEmpty();

		JComboBox<CompoundLabel> result = new JComboBox<>(values);
		result.setEditable(editable);
		result.setRenderer(new LabelCellRenderer());

		return result;
	}

	@SuppressWarnings("unchecked")
	public static <R extends Resource> Map<R,Path> extractResources(
			List<LocalFileObject> files, Identifiable.Type type) {
		Map<R, Path> result = new IdentityHashMap<>();

		for(LocalFileObject fileObject : files) {
			Resource resource = fileObject.getResource();

			//if(resource==null) {
				// Just a dummy, we'll convert if needed anyway
			resource = DefaultResource.withIdentifiers(fileObject.getIdentifiers());
			//}

			resource = WorkflowUtils.derive(resource, type);

			result.put((R) resource, fileObject.getFile());
		}

		return result;
	}

	public static <R extends Resource> void showFileResourceDialog(
			WorkflowSchema schema,
			boolean titleEditable,
			Role role,
			Map<R, Path> resourceMap) {

		IdentifiableEditor editor = IdentifiableEditor.newBuilder()
				.schema(schema)
				.type(role.asIdentifiableType())
				.useDefaultTitleSelector()
				.titleEditable(titleEditable)
				.build();

		showFileResourceDialog(editor, role, resourceMap);
	}


	public static <R extends Resource> void showFileResourceDialog(
			IdentifiableEditor editor,
			Role role,
			Map<R, Path> resourceMap) {

		String title = ResourceManager.getInstance().get("replaydh.panels.fileCache.fileDialog.title");

		editor.setEditingItem(IdentifiableEditor.wrap(resourceMap.keySet()));

		if(!GuiUtils.showEditorDialogWithControl(null, editor, title, true)) {
			resourceMap.clear();
		} else {
			Set<Resource> resources = IdentifiableEditor.unwrap(editor.getEditingItem());

			resourceMap.keySet().retainAll(resources);
		}
	}
}

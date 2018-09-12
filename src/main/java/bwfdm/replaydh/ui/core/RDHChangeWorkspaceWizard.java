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
package bwfdm.replaydh.ui.core;

import static java.util.Objects.requireNonNull;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.core.WorkspaceValidator.WorkspaceState;
import bwfdm.replaydh.ui.helper.AbstractWizardStep;
import bwfdm.replaydh.ui.helper.DocumentAdapter;
import bwfdm.replaydh.ui.helper.FilePanel;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.helper.Wizard.Page;
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.ui.icons.Resolution;
import bwfdm.replaydh.ui.workflow.WorkflowSchemaListCellRenderer;
import bwfdm.replaydh.utils.RDHUtils;
import bwfdm.replaydh.workflow.schema.SchemaManager;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public abstract class RDHChangeWorkspaceWizard {

	public static Wizard<ChangeWorkspaceContext> getWizard(Window parent, RDHEnvironment environment) {
		@SuppressWarnings("unchecked")
		Wizard<ChangeWorkspaceContext> wizard = new Wizard<>(
				parent, "changeWorkspace", ResourceManager.getInstance().get("replaydh.wizard.changeWorkspace.title"),
				environment, SELECT_WORKSPACE, NEW_WORKSPACE, VALIDATE_WORKSPACE, SCHEMA, DESCRIPTION, FINISH);

		return wizard;
	}

	public static final class ChangeWorkspaceContext {

		public static ChangeWorkspaceContext blank() {
			return new ChangeWorkspaceContext(null);
		}

		public static ChangeWorkspaceContext withWorkspaces(String workspacesProperty) {
			requireNonNull(workspacesProperty);

			// Fetch raw definitions
			String[] paths = workspacesProperty.split(";");

			// Translate into proper paths
			Path[] workspaces = new Path[paths.length];

			for(int i=0; i<paths.length; i++) {
				workspaces[i] = Paths.get(paths[i]);
			}

			if(workspaces.length==0) {
				workspaces = null;
			} else {
				Arrays.sort(workspaces);
			}

			return new ChangeWorkspaceContext(workspaces);
		}

		ChangeWorkspaceContext(Path[] workspaces) {
			this.workspaces = workspaces;
		}

		final Path[] workspaces;
		Path workspacePath;
		WorkspaceValidator.WorkspaceState workspaceState;
		WorkflowSchema schema;

		String title;
		String description;

		public Path getWorkspacePath() {
			return workspacePath;
		}

		public WorkflowSchema getSchema() {
			return schema;
		}

		public boolean hasFullWorkspaceInformation() {
			return workspacePath!=null && schema!=null;
		}

		public String getTitle() {
			return title;
		}

		public String getDescription() {
			return description;
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	private static abstract class ChangeWorkspaceStep extends AbstractWizardStep<ChangeWorkspaceContext> {
		protected ChangeWorkspaceStep(String id, String titleKey, String descriptionKey) {
			super(id, titleKey, descriptionKey);
		}
	}


	/**
	 * First step - allow user to select a workspace from history
	 */
	private static final ChangeWorkspaceStep SELECT_WORKSPACE = new ChangeWorkspaceStep(
			"selectWorkspace",
			"replaydh.wizard.changeWorkspace.selectWorkspace.title",
			"replaydh.wizard.changeWorkspace.selectWorkspace.description") {

		private final Object NEW_WORKSPACE_ACTION = ResourceManager.getInstance().get(
				"replaydh.wizard.changeWorkspace.selectWorkspace.createNewWorkspace");

		private JComboBox<Object> jbWorkspace;

		@Override
		protected JPanel createPanel() {

			jbWorkspace = new JComboBox<>();
			jbWorkspace.setEditable(false);
			jbWorkspace.setRenderer(new WorkspaceListCellRenderer());
			jbWorkspace.addActionListener(this::onActionSelected);

			return FormBuilder.create()
					.columns("10dlu, fill:pref:grow, 10dlu")
					.rows("top:pref, 8dlu, pref, 6dlu")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.changeWorkspace.selectWorkspace.message"))).xyw(1, 1, 3, "fill, top")
					.add(jbWorkspace).xy(2, 3)
					.build();
		}

		private void onActionSelected(ActionEvent ae) {
			setNextEnabled(isValidSelection());
		}

		private boolean isValidSelection() {
			Object selectedItem = jbWorkspace.getSelectedItem();
			boolean isValid = selectedItem==NEW_WORKSPACE_ACTION;

			if(selectedItem instanceof Path) {
				Path path = (Path) selectedItem;
				isValid = Files.exists(path, LinkOption.NOFOLLOW_LINKS);
			}

			return isValid;
		}

		@Override
		public void refresh(RDHEnvironment environment, ChangeWorkspaceContext context) {
			Path workspace = environment.getWorkspacePath();

			jbWorkspace.removeAllItems();
			jbWorkspace.addItem(NEW_WORKSPACE_ACTION);
			if(context.workspaces!=null) {
				for(Path path : context.workspaces) {
					if(workspace==null || !path.equals(workspace)) {
						jbWorkspace.addItem(path);
					}
				}
			}

			jbWorkspace.setSelectedItem(NEW_WORKSPACE_ACTION);
		};

		@Override
		public Page<ChangeWorkspaceContext> next(RDHEnvironment environment, ChangeWorkspaceContext context) {

			Object selectedItem = jbWorkspace.getSelectedItem();

			if(selectedItem==NEW_WORKSPACE_ACTION) {
				return NEW_WORKSPACE;
			} else if(selectedItem instanceof Path) {
				context.workspacePath = (Path) selectedItem;
				return VALIDATE_WORKSPACE;
			} else
				throw new IllegalStateException("Unexpected workspace selection: "+selectedItem);
		}
	};

	private static class WorkspaceListCellRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = -6818402803319175560L;

		private Font boldFont;
		private Font normalFont;


		/**
		 * @see javax.swing.JLabel#updateUI()
		 */
		@Override
		public void updateUI() {
			super.updateUI();

			normalFont = getFont();

			if(normalFont!=null) {
				boldFont = normalFont.deriveFont(Font.BOLD);
			}
		}

		/**
		 * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
		 */
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {

			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			if(value instanceof Path) {
				Path workspace = (Path) value;

				setToolTipText(workspace.toString());

				if(Files.exists(workspace, LinkOption.NOFOLLOW_LINKS)) {
					setText(workspace.toString());
				} else {
					setText(ResourceManager.getInstance().get(
							"replaydh.wizard.changeWorkspace.selectWorkspace.missingWorkspace",
							workspace.toString()));
					setForeground(Color.RED);
				}

				setFont(normalFont);
			} else {
				setFont(boldFont);
			}

			return this;
		}
	}

	/**
	 * Ask user for desired new workspace
	 */
	private static final ChangeWorkspaceStep NEW_WORKSPACE = new ChangeWorkspaceStep(
			"newWorkspace",
			"replaydh.wizard.changeWorkspace.newWorkspace.title",
			"replaydh.wizard.changeWorkspace.newWorkspace.description") {

		FilePanel fpWorkspace;

		@Override
		public Page<ChangeWorkspaceContext> next(RDHEnvironment environment, ChangeWorkspaceContext context) {
			context.workspacePath = fpWorkspace.getFile();

			return VALIDATE_WORKSPACE;
		}

		private boolean isValidWorkspace() {
			Path workspace = fpWorkspace.getFile();

			return workspace!=null
					&& Files.exists(workspace, LinkOption.NOFOLLOW_LINKS)
					&& Files.isDirectory(workspace, LinkOption.NOFOLLOW_LINKS);
		}

		private void configureFileChooser(JFileChooser fileChooser) {
			fileChooser.setDialogTitle(ResourceManager.getInstance().get(
					"replaydh.wizard.changeWorkspace.newWorkspace.dialogTitle"));

			if(fpWorkspace!=null) {
				Path workspace = fpWorkspace.getFile();
				if(workspace!=null && Files.isDirectory(workspace, LinkOption.NOFOLLOW_LINKS)) {
					fileChooser.setCurrentDirectory(workspace.toFile());
				}
			}
		}

		@Override
		protected JPanel createPanel() {

			fpWorkspace = FilePanel.newBuilder()
					.acceptedFileType(JFileChooser.DIRECTORIES_ONLY)
					.fileLimit(1)
					.fileFilter(FilePanel.SHARED_DIRECTORY_FILE_FILTER)
					.fileChooserSetup(this::configureFileChooser)
					.build();

			fpWorkspace.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					setNextEnabled(isValidWorkspace());
				}
			});

			return FormBuilder.create()
					.columns("10dlu, pref:grow, 10dlu")
					.rows("top:pref, 10dlu, fill:pref, 10dlu, top:pref")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.changeWorkspace.newWorkspace.message"))).xy(2, 1)
					.add(fpWorkspace).xy(2, 3, "fill, center")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.changeWorkspace.newWorkspace.message2"))).xy(2, 5)
					.build();
		}

		@Override
		public void refresh(RDHEnvironment environment, ChangeWorkspaceContext context) {
			Path workspacePath = context.workspacePath;
			if(workspacePath==null) {
				workspacePath = environment.getWorkspacePath();
			}
			String workspaceString = workspacePath==null ? null : workspacePath.toString();

			fpWorkspace.setFile(workspaceString);
		}

		@Override
		public void cancel(RDHEnvironment environment, ChangeWorkspaceContext context) {
			context.workspacePath = null;
		};
	};

	/**
	 * Validate the user specified workspace directory for the following criteria:
	 * <ol>
	 * <li>If it is an empty directory, continue</li>
	 * <li>Search for any git repository in the directory and all nested sub folders</li>
	 * <li>If a git repository is found, check if it's a RDH-created one</li>
	 * <li>Abort if the repo is foreign</li>
	 * <li>Show warning if workspace is not empty</li>
	 * </ol>
	 */
	private static final ChangeWorkspaceStep VALIDATE_WORKSPACE = new ChangeWorkspaceStep(
			"validateWorkspace",
			"replaydh.wizard.changeWorkspace.validateWorkspace.title",
			"replaydh.wizard.changeWorkspace.validateWorkspace.description") {

		/**
		 * <pre>
		 * +------------------------------------+
		 * |                +---+               |
		 * |                |BTN|               |
		 * |                +---+               |
		 * |                STATE               |
		 * +------------------------------------+
		 * </pre>
		 */

		JButton bValidate;

		JLabel lFolderState, lRepoState;

		JTextArea taStatus;

		JLabel lPath;

		WorkspaceValidator workspaceValidator;

		Path workspace;

		WorkspaceValidator.WorkspaceState workspaceState;

		// State icons
		private final Icon ICON_UNKNOWN = IconRegistry.getGlobalRegistry().getIcon("icons8-Help-48.png", Resolution.forSize(24));
		private final Icon ICON_FAILED = IconRegistry.getGlobalRegistry().getIcon("icons8-Cancel-48.png", Resolution.forSize(24));
		private final Icon ICON_CHECKED = IconRegistry.getGlobalRegistry().getIcon("icons8-Ok-48.png", Resolution.forSize(24));
		private final Icon ICON_WARN = IconRegistry.getGlobalRegistry().getIcon("icons8-Error-48.png", Resolution.forSize(24));

		// Folder state labels
		private final String LABEL_UNKNOWN_STATE = ResourceManager.getInstance().get("replaydh.wizard.changeWorkspace.validateWorkspace.unknownState");
		private final String LABEL_EMPTY_FOLDER = ResourceManager.getInstance().get("replaydh.wizard.changeWorkspace.validateWorkspace.emptyFolder");
		private final String LABEL_USED_FOLDER = ResourceManager.getInstance().get("replaydh.wizard.changeWorkspace.validateWorkspace.usedFolder");

		// Repo state labels
		private final String LABEL_NO_REPO = ResourceManager.getInstance().get("replaydh.wizard.changeWorkspace.validateWorkspace.noRepo");
		private final String LABEL_RDH_REPO = ResourceManager.getInstance().get("replaydh.wizard.changeWorkspace.validateWorkspace.existingRDHRepo");
		private final String LABEL_FOREIG_REPO = ResourceManager.getInstance().get("replaydh.wizard.changeWorkspace.validateWorkspace.existingForeignRepo");

		// Workspace states
		private final String TEXT_INVALID_WORKSPACE = ResourceManager.getInstance().get("replaydh.wizard.changeWorkspace.validateWorkspace.invalidWorkspace");
		private final String TEXT_USED_WORKSPACE = ResourceManager.getInstance().get("replaydh.wizard.changeWorkspace.validateWorkspace.usedWorkspace");
		private final String TEXT_VALID_WORKSPACE = ResourceManager.getInstance().get("replaydh.wizard.changeWorkspace.validateWorkspace.validWorkspace");
		private final String TEXT_RDH_WORKSPACE = ResourceManager.getInstance().get("replaydh.wizard.changeWorkspace.validateWorkspace.rdhWorkspace");

		@Override
		public Page<ChangeWorkspaceContext> next(RDHEnvironment environment, ChangeWorkspaceContext context) {
			context.workspaceState = workspaceState;

			return context.workspaceState==WorkspaceState.RDH_REPO ? FINISH : SCHEMA;
		}

		@Override
		protected JPanel createPanel() {

			bValidate = new JButton();
			bValidate.setFont(bValidate.getFont().deriveFont(Font.BOLD, 16));
			bValidate.addActionListener(this::onButtonClicked);

			lFolderState = createStateLabel(LABEL_UNKNOWN_STATE);
			lRepoState = createStateLabel(LABEL_NO_REPO);

			lPath = new JLabel("", SwingConstants.LEFT);

			taStatus = GuiUtils.createTextArea(null);

			return FormBuilder.create()
					.columns("4dlu, fill:pref:grow, 4dlu")
					.rows("bottom:pref, 4dlu, pref, pref, 12dlu, pref, 6dlu, top:pref:grow")
					.add(bValidate).xy(2, 1, "center, center")
					.add(lFolderState).xy(2, 3, "left, center")
					.add(lRepoState).xy(2, 4, "left, center")
					.add(lPath).xy(2, 6, "fill, center")
					.add(taStatus).xy(2, 8, "fill, fill")
					.build();
		}

		@Override
		public boolean close() {
			workspaceValidator = null;
			workspaceState = null;
			workspace = null;

			return super.close();
		};

		private JLabel createStateLabel(String text) {
			JLabel label = new JLabel(text);
			label.setHorizontalAlignment(SwingConstants.CENTER);
			label.setHorizontalTextPosition(SwingConstants.RIGHT);
			label.setVerticalAlignment(SwingConstants.CENTER);
			label.setIcon(ICON_UNKNOWN);
			//TODO increase font size?
			return label;
		}

		private boolean isScanning() {
			return workspaceValidator!=null && !workspaceValidator.isDone();
		}

		private void onButtonClicked(ActionEvent ae) {
			if(isScanning()) {
				workspaceValidator.cancel(true);
			} else {
				workspaceValidator = createValidator(workspace);
				workspaceValidator.execute();
			}

			GuiUtils.invokeEDT(this::refreshButton);
		}

		private void displayState(WorkspaceValidator.WorkspaceState state) {
			workspaceState = state;

			Icon iconFolder = lFolderState.getIcon();
			Icon iconRepo = lRepoState.getIcon();

			String labelFolder = lFolderState.getText();
			String labelRepo = lRepoState.getText();

			String status = null;

			if(state!=null) {
				switch (state) {
				case EMPTY_FOLDER:
					iconFolder = ICON_CHECKED;
					iconRepo = ICON_CHECKED;
					labelFolder = LABEL_EMPTY_FOLDER;
					labelRepo = LABEL_NO_REPO;
					status = TEXT_VALID_WORKSPACE;
					break;

				case USED_FOLDER:
					iconFolder = ICON_WARN;
					labelFolder = LABEL_USED_FOLDER;
					iconRepo = ICON_CHECKED;
					labelRepo = LABEL_NO_REPO;
					status = TEXT_USED_WORKSPACE;
					break;

				case RDH_REPO:
					iconFolder = ICON_WARN;
					labelFolder = LABEL_USED_FOLDER;
					iconRepo = ICON_CHECKED;
					labelRepo = LABEL_RDH_REPO;
					status = TEXT_RDH_WORKSPACE;
					break;

				case FOREIGN_REPO:
					iconFolder = ICON_WARN;
					labelFolder = LABEL_USED_FOLDER;
					iconRepo = ICON_FAILED;
					labelRepo = LABEL_FOREIG_REPO;
					status = TEXT_INVALID_WORKSPACE;
					break;

				default:
					break;
				}
			} else {
				iconFolder = iconRepo = ICON_UNKNOWN;
				labelFolder = LABEL_UNKNOWN_STATE;
				labelRepo = LABEL_NO_REPO;
			}

			lFolderState.setIcon(iconFolder);
			lFolderState.setText(labelFolder);

			lRepoState.setIcon(iconRepo);
			lRepoState.setText(labelRepo);

			taStatus.setText(status);

			refreshButton();
		}

		private void displayFolder(Path folder) {

			ResourceManager rm = ResourceManager.getInstance();

			if(folder==null) { // Invalid workspace folder
				String text = rm.get("replaydh.wizard.changeWorkspace.validateWorkspace.missingFolder");
				lPath.setText(text);
			} else if(isScanning()){ // Scan in progress -> display folder currently being scanned

				String pathString = RDHUtils.toPathString(folder, RDHUtils.DEFAULT_PATH_STRING_LENGTH_LIMIT);
				String text = rm.get("replaydh.wizard.changeWorkspace.validateWorkspace.currentFolder", pathString);
				lPath.setText(text);

			} else { // No active scan -> show selected workspace

				String pathString = RDHUtils.toPathString(folder, RDHUtils.DEFAULT_PATH_STRING_LENGTH_LIMIT);
				String text = rm.get("replaydh.wizard.changeWorkspace.validateWorkspace.selectedWorkspace", pathString);
				lPath.setText(text);
			}

			refreshButton();
		}

		private void refreshButton() {
			ResourceManager rm = ResourceManager.getInstance();
			IconRegistry ir = IconRegistry.getGlobalRegistry();

			if(isScanning()) {
				bValidate.setText(rm.get("replaydh.wizard.changeWorkspace.validateWorkspace.cancel.label"));
				bValidate.setToolTipText(rm.get("replaydh.wizard.changeWorkspace.validateWorkspace.cancel.description"));
				bValidate.setIcon(ir.getIcon("loading-64.gif", Resolution.forSize(24)));
			} else {
				bValidate.setText(rm.get("replaydh.wizard.changeWorkspace.validateWorkspace.validate.label"));
				bValidate.setToolTipText(rm.get("replaydh.wizard.changeWorkspace.validateWorkspace.validate.description"));
				bValidate.setIcon(ir.getIcon("update-icon.png", Resolution.forSize(24)));
			}

			bValidate.setEnabled(workspace!=null);
		}

		private WorkspaceValidator createValidator(Path workspace) {
			return new WorkspaceValidator(workspace){
				@Override
				protected void process(List<Path> chunks) {
					if(!chunks.isEmpty()) {
						displayFolder(chunks.get(chunks.size()-1));
					}
				}

				@Override
				protected void done() {

					workspaceValidator = null;

					if(isCancelled()) {
						displayState(null);
					} else {
						boolean workspaceValid = getWorkspaceState().compareTo(WorkspaceValidator.WorkspaceState.RDH_REPO)<=0;

						displayState(getWorkspaceState());
						setNextEnabled(workspaceValid);
						refreshButton();
					}
				};
			};
		}

		@Override
		public void refresh(RDHEnvironment environment, ChangeWorkspaceContext context) {
			workspace = context.workspacePath;

			boolean canSkip = false;

			WorkspaceValidator.WorkspaceState state = context.workspaceState;
			if(state!=null) {
				canSkip = state.compareTo(WorkspaceValidator.WorkspaceState.RDH_REPO)<=0;
			} else {
				displayFolder(workspace);
			}

			displayState(state);

			setNextEnabled(canSkip);
		};

		@Override
		public void cancel(RDHEnvironment environment, ChangeWorkspaceContext context) {
			context.workspaceState = null;
			workspace = null;
			workspaceState = null;
		};
	};

	/**
	 * Let user select workflow schema for the workspace
	 */
	private static final ChangeWorkspaceStep SCHEMA = new ChangeWorkspaceStep(
			"selectSchema",
			"replaydh.wizard.changeWorkspace.selectSchema.title",
			"replaydh.wizard.changeWorkspace.selectSchema.description") {

		JComboBox<WorkflowSchema> cbSchema;

		@Override
		protected JPanel createPanel() {
			ResourceManager rm = ResourceManager.getInstance();

			cbSchema = new JComboBox<>(new DefaultComboBoxModel<>());
			cbSchema.setEditable(false);
			cbSchema.setRenderer(new WorkflowSchemaListCellRenderer());
			cbSchema.addActionListener(this::onComboBoxClicked);

			return FormBuilder.create()
					.columns("pref, 4dlu, pref:grow:fill")
					.rows("pref, 6dlu, pref, 6dlu, pref")
					.add(GuiUtils.createTextArea(rm.get("replaydh.wizard.changeWorkspace.selectSchema.message"))).xyw(1, 1, 3)
					.addLabel(rm.get("replaydh.wizard.changeWorkspace.selectSchema.selectedSchema")).xy(1, 3)
					.add(cbSchema).xy(3, 3)
					.add(GuiUtils.createTextArea(rm.get("replaydh.wizard.changeWorkspace.selectSchema.message2"))).xyw(1, 5, 3)
					.build();
		}

		private void onComboBoxClicked(ActionEvent ae) {
			WorkflowSchema schema = (WorkflowSchema) cbSchema.getSelectedItem();

			setNextEnabled(schema!=null);
		}

		@Override
		public void refresh(RDHEnvironment environment, ChangeWorkspaceContext context) {
			DefaultComboBoxModel<WorkflowSchema> model = (DefaultComboBoxModel<WorkflowSchema>) cbSchema.getModel();
			if(model.getSize()==0) {
				SchemaManager schemaManager = environment.getClient().getSchemaManager();

				List<WorkflowSchema> items = new ArrayList<>(schemaManager.getAvailableSchemas());
				Collections.sort(items, (s1, s2) -> s1.getId().compareTo(s2.getId()));

				for(WorkflowSchema schema : items) {
					model.addElement(schema);
				}
			}

			cbSchema.setSelectedItem(context.getSchema());
		}

		@Override
		public void cancel(RDHEnvironment environment, ChangeWorkspaceContext context) {
			context.schema = null;
		};

		@Override
		public Page<ChangeWorkspaceContext> next(RDHEnvironment environment, ChangeWorkspaceContext context) {

			context.schema = (WorkflowSchema) cbSchema.getSelectedItem();

			return DESCRIPTION;
		}
	};

	/**
	 * Let user select workflow schema for the workspace
	 */
	private static final ChangeWorkspaceStep DESCRIPTION = new ChangeWorkspaceStep(
			"describeWorkflow",
			"replaydh.wizard.changeWorkspace.describeWorkflow.title",
			"replaydh.wizard.changeWorkspace.describeWorkflow.description") {

		JTextField tfTitle;
		JTextArea taDescription;
		JScrollPane scrollPane;

		@Override
		protected JPanel createPanel() {
			ResourceManager rm = ResourceManager.getInstance();

			tfTitle = new JTextField();

			taDescription = new JTextArea();
			taDescription.setLineWrap(true);
			taDescription.setWrapStyleWord(true);
			taDescription.setBorder(Paddings.DLU2);
			taDescription.setRows(10);

			scrollPane = new JScrollPane(taDescription,
					ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
					ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

			GuiUtils.prepareChangeableBorder(tfTitle);
			GuiUtils.prepareChangeableBorder(scrollPane);

			final DocumentListener listener = new DocumentAdapter() {
				@Override
				public void anyUpdate(DocumentEvent e) {
					refreshNextEnabled();
				}
			};
			tfTitle.getDocument().addDocumentListener(listener);
			taDescription.getDocument().addDocumentListener(listener);

			return FormBuilder.create()
					.columns("pref, 4dlu, pref:grow:fill")
					.rows("pref, 12dlu, pref, 6dlu, pref")
					.add(GuiUtils.createTextArea(rm.get("replaydh.wizard.changeWorkspace.describeWorkflow.message"))).xyw(1, 1, 3)
					.addLabel(rm.get("replaydh.labels.title")+": ").xy(1, 3, "left, top")
					.add(tfTitle).xy(3, 3, "fill, top")
					.addLabel(rm.get("replaydh.labels.description")+": ").xy(1, 5, "left, top")
					.add(scrollPane).xy(3, 5, "fill, fill")
					.build();
		}

		private void refreshNextEnabled() {
			String title = tfTitle.getText();
			boolean titleValid = title!=null && !title.trim().isEmpty();

			String description = taDescription.getText();
			boolean descriptionValid = description!=null && !description.trim().isEmpty();

			GuiUtils.toggleChangeableBorder(tfTitle, !titleValid);
			GuiUtils.toggleChangeableBorder(scrollPane, !descriptionValid);

			setNextEnabled(titleValid && descriptionValid);
		}

		@Override
		public void refresh(RDHEnvironment environment, ChangeWorkspaceContext context) {
			tfTitle.setText(context.getTitle());
			taDescription.setText(context.getDescription());

			refreshNextEnabled();
		}

		@Override
		public void cancel(RDHEnvironment environment, ChangeWorkspaceContext context) {
			context.title = null;
			context.description = null;
		};

		@Override
		public Page<ChangeWorkspaceContext> next(RDHEnvironment environment, ChangeWorkspaceContext context) {

			context.title = tfTitle.getText();
			context.description = taDescription.getText();

			return FINISH;
		}
	};

	/**
	 * Wrap up info
	 */
	private static final ChangeWorkspaceStep FINISH = new ChangeWorkspaceStep(
			"changeWorkspace",
			"replaydh.wizard.changeWorkspace.finish.title",
			"replaydh.wizard.changeWorkspace.finish.description") {

		@Override
		public Page<ChangeWorkspaceContext> next(RDHEnvironment environment, ChangeWorkspaceContext context) {
			return null;
		}

		@Override
		protected JPanel createPanel() {
			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("top:pref")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.changeWorkspace.finish.message"))).xy(1, 1)
					.build();
		}

//		@Override
//		public void persist(RDHEnvironment environment, ChangeWorkspaceContext context) {
//			Path workspacePath = context.workspacePath;
//			checkState("workspace is not set", workspacePath!=null);
//
//			environment.getClient().loadWorkspace(workspacePath);
//		}
	};
}

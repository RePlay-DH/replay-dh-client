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

import java.awt.Window;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.AbstractWizardStep;
import bwfdm.replaydh.ui.helper.DocumentAdapter;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.helper.Wizard.Page;

/**
 * @author Markus Gärtner
 *
 */
public abstract class RDHWelcomeWizard {

	public static Wizard<WelcomeContext> getWizard(Window parent, RDHEnvironment environment) {
		@SuppressWarnings("unchecked")
		Wizard<WelcomeContext> wizard = new Wizard<>(
				parent, ResourceManager.getInstance().get("replaydh.wizard.welcome.title"),
				environment, INIT, IDENTITY, /*WORKSPACE, VALIDATE_WORKSPACE,*/ FINISH);

		return wizard;
	}

	public static final class WelcomeContext {
		String username;
		String organization;
		Path workspace;
		WorkspaceValidator.WorkspaceState workspaceState;

		public Path getWorkspace() {
			return workspace;
		}

		public String getUsername() {
			return username;
		}

		public String getOrganization() {
			return organization;
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	private static abstract class WelcomeStep extends AbstractWizardStep<WelcomeContext> {
		protected WelcomeStep(String titleKey, String descriptionKey) {
			super(titleKey, descriptionKey);
		}
	}


	/**
	 * First step - just a textual introduction
	 */
	private static final WelcomeStep INIT = new WelcomeStep(
			"replaydh.wizard.welcome.init.title",
			"replaydh.wizard.welcome.init.description") {

		@Override
		public Page<WelcomeContext> next(RDHEnvironment environment, WelcomeContext context) {
			return IDENTITY;
		}

		@Override
		protected JPanel createPanel() {
			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("top:pref")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.welcome.init.message"))).xy(1, 1)
					.build();
		}
	};

	/**
	 * Ask user for username and identity of organization
	 */
	private static final WelcomeStep IDENTITY = new WelcomeStep(
			"replaydh.wizard.welcome.identity.title",
			"replaydh.wizard.welcome.identity.description") {

		JTextField tfOrganization;
		JTextField tfUsername;

		@Override
		public Page<WelcomeContext> next(RDHEnvironment environment, WelcomeContext context) {
			context.username = tfUsername.getText().trim();
			context.organization = tfOrganization.getText().trim();

			return FINISH;
		}

		/**
		 * Allowed punctuation symbols: -_.@
		 * Invalid:
		 * 		A control character: [\x00-\x1F\x7F]
		 * 		A whitespace character (other than space): [\t\n\x0B\f\r]
		 * 		Punctuation: One of !"#$%&'()*+,/:;<=>?[\]^`{|}~}
		 */
		private final Matcher INVALID_NAME_MATCHER = Pattern.compile(
				"[!\"#$%&'\\(\\)\\*+,/:;<=>\\?\\[\\\\]\\^`\\{|\\}~\t\n\\x{0B}\f\r\\p{Cntrl}]").matcher("");

		private boolean isValidUsername() {
			String username = tfUsername.getText().trim();
			if(username.isEmpty()) {
				return false;
			}

//			INVALID_NAME_MATCHER.reset(username);

//			return !INVALID_NAME_MATCHER.find();

			for(int i=0; i<username.length(); i++) {
				char c = username.charAt(i);
				if(c=='_' || c=='-' || c=='.' || c==' ') {
					continue;
				}

				int codePoint = username.codePointAt(i);
				if(codePoint!=c) {
					i++;
				}

				if(!Character.isLetterOrDigit(codePoint)) {
					return false;
				}
			}

			return true;
		}

		@Override
		protected JPanel createPanel() {
			tfOrganization = new JTextField(20);
			tfUsername = new JTextField(20);
			tfUsername.getDocument().addDocumentListener(new DocumentAdapter(){
				@Override
				public void anyUpdate(DocumentEvent e) {
					setNextEnabled(isValidUsername());
				}
			});

			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("top:pref, $nlg, pref, $nlg, top:pref, $nlg, pref")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.welcome.identity.user"))).xy(1, 1, "fill, top")
					.add(tfUsername).xy(1, 3, "center, center")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.welcome.identity.organization"))).xy(1, 5, "fill, top")
					.add(tfOrganization).xy(1, 7, "center, center")
					.build();
		}

		@Override
		public void refresh(RDHEnvironment environment, WelcomeContext context) {
			String username = context.username;
			if(username==null) {
				username = environment.getProperty(RDHProperty.CLIENT_USERNAME);
			}
			tfUsername.setText(username);

			String organization = context.organization;
			if(organization==null) {
				organization = environment.getProperty(RDHProperty.CLIENT_ORGANIZATION);
			}
			tfOrganization.setText(organization);

			setNextEnabled(isValidUsername());
		}
	};

//	/**
//	 * Ask user for desired workspace
//	 */
//	private static final WelcomeStep WORKSPACE = new WelcomeStep(
//			"replaydh.wizard.welcome.workspace.title",
//			"replaydh.wizard.welcome.workspace.description") {
//
//		FilePanel fpWorkspace;
//
//		@Override
//		public Page<WelcomeContext> next(RDHEnvironment environment, WelcomeContext context) {
//			context.workspace = fpWorkspace.getFile();
//
//			return VALIDATE_WORKSPACE;
//		}
//
//		private boolean isValidWorkspace() {
//			Path workspace = fpWorkspace.getFile();
//
//			return workspace!=null
//					&& Files.exists(workspace, LinkOption.NOFOLLOW_LINKS)
//					&& Files.isDirectory(workspace, LinkOption.NOFOLLOW_LINKS);
//		}
//
//		private void configureFileChooser(JFileChooser fileChooser) {
//			fileChooser.setDialogTitle(ResourceManager.getInstance().get(
//					"replaydh.wizard.welcome.workspace.dialogTitle"));
//
//			if(fpWorkspace!=null) {
//				Path workspace = fpWorkspace.getFile();
//				if(workspace!=null && Files.isDirectory(workspace, LinkOption.NOFOLLOW_LINKS)) {
//					fileChooser.setCurrentDirectory(workspace.toFile());
//				}
//			}
//		}
//
//		@Override
//		protected JPanel createPanel() {
//
//			fpWorkspace = FilePanel.newBuilder()
//					.acceptedFileType(JFileChooser.DIRECTORIES_ONLY)
//					.fileLimit(1)
//					.fileFilter(FilePanel.SHARED_DIRECTORY_FILE_FILTER)
//					.fileChooserSetup(this::configureFileChooser)
//					.build();
//
//			fpWorkspace.addChangeListener(new ChangeListener() {
//				@Override
//				public void stateChanged(ChangeEvent e) {
//					setNextEnabled(isValidWorkspace());
//				}
//			});
//
//			return FormBuilder.create()
//					.columns("10dlu, pref:grow, 10dlu")
//					.rows("top:pref, 10dlu, fill:pref, 10dlu, top:pref")
//					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
//							"replaydh.wizard.welcome.workspace.message"))).xy(2, 1)
//					.add(fpWorkspace).xy(2, 3, "fill, center")
//					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
//							"replaydh.wizard.welcome.workspace.message2"))).xy(2, 5)
//					.build();
//		}
//
//		@Override
//		public void refresh(RDHEnvironment environment, WelcomeContext context) {
//			Path workspacePath = context.workspace;
//			if(workspacePath==null) {
//				Workspace workspace = environment.getWorkspace();
//				if(workspace!=null) {
//					workspacePath = workspace.getFolder();
//				}
//			}
//			String workspaceString = workspacePath==null ? null : workspacePath.toString();
//
//			fpWorkspace.setFile(workspaceString);
//		}
//
//		@Override
//		public void cancel(RDHEnvironment environment, WelcomeContext context) {
//			context.workspace = null;
//		};
//	};

//	/**
//	 * Validate the user specified workspace directory for the following criteria:
//	 * <ol>
//	 * <li>If it is an empty directory, continue</li>
//	 * <li>Search for any git repository in the directory and all nested sub folders</li>
//	 * <li>If a git repository is found, check if it's a RDH-created one</li>
//	 * <li>Abort if the repo is foreign?</li>
//	 * <li>Show warning if workspace is not empty</li>
//	 * </ol>
//	 */
//	private static final WelcomeStep VALIDATE_WORKSPACE = new WelcomeStep(
//			"replaydh.wizard.welcome.validateWorkspace.title",
//			"replaydh.wizard.welcome.validateWorkspace.description") {
//
//		/**
//		 * <pre>
//		 * +------------------------------------+
//		 * |                +---+               |
//		 * |                |BTN|               |
//		 * |                +---+               |
//		 * |                STATE               |
//		 * +------------------------------------+
//		 * </pre>
//		 */
//
//		JButton bValidate;
//
//		JLabel lFolderState, lRepoState;
//
//		JTextArea taStatus;
//
//		JLabel lPath;
//
//		WorkspaceValidator workspaceValidator;
//
//		Path workspace;
//
//		WorkspaceValidator.WorkspaceState workspaceState;
//
//		// State icons
//		private final Icon ICON_UNKNOWN = IconRegistry.getGlobalRegistry().getIcon("icons8-Help-48.png", Resolution.forSize(24));
//		private final Icon ICON_FAILED = IconRegistry.getGlobalRegistry().getIcon("icons8-Cancel-48.png", Resolution.forSize(24));
//		private final Icon ICON_CHECKED = IconRegistry.getGlobalRegistry().getIcon("icons8-Ok-48.png", Resolution.forSize(24));
//		private final Icon ICON_WARN = IconRegistry.getGlobalRegistry().getIcon("icons8-Error-48.png", Resolution.forSize(24));
//
//		// Folder state labels
//		private final String LABEL_UNKNOWN_STATE = ResourceManager.getInstance().get("replaydh.wizard.welcome.validateWorkspace.unknownState");
//		private final String LABEL_EMPTY_FOLDER = ResourceManager.getInstance().get("replaydh.wizard.welcome.validateWorkspace.emptyFolder");
//		private final String LABEL_USED_FOLDER = ResourceManager.getInstance().get("replaydh.wizard.welcome.validateWorkspace.usedFolder");
//
//		// Repo state labels
//		private final String LABEL_NO_REPO = ResourceManager.getInstance().get("replaydh.wizard.welcome.validateWorkspace.noRepo");
//		private final String LABEL_RDH_REPO = ResourceManager.getInstance().get("replaydh.wizard.welcome.validateWorkspace.existingRDHRepo");
//		private final String LABEL_FOREIG_REPO = ResourceManager.getInstance().get("replaydh.wizard.welcome.validateWorkspace.existingForeignRepo");
//
//		// Workspace states
//		private final String TEXT_INVALID_WORKSPACE = ResourceManager.getInstance().get("replaydh.wizard.welcome.validateWorkspace.invalidWorkspace");
//		private final String TEXT_USED_WORKSPACE = ResourceManager.getInstance().get("replaydh.wizard.welcome.validateWorkspace.usedWorkspace");
//		private final String TEXT_VALID_WORKSPACE = ResourceManager.getInstance().get("replaydh.wizard.welcome.validateWorkspace.validWorkspace");
//		private final String TEXT_RDH_WORKSPACE = ResourceManager.getInstance().get("replaydh.wizard.welcome.validateWorkspace.rdhWorkspace");
//
//		@Override
//		public Page<WelcomeContext> next(RDHEnvironment environment, WelcomeContext context) {
//			context.workspaceState = workspaceState;
//
//			return workspaceState==WorkspaceState.RDH_REPO ? FINISH : SCHEMA;
//		}
//
//		@Override
//		protected JPanel createPanel() {
//
//			bValidate = new JButton();
//			bValidate.setFont(bValidate.getFont().deriveFont(Font.BOLD, 16));
//			bValidate.addActionListener(this::onButtonClicked);
//
//			lFolderState = createStateLabel(LABEL_UNKNOWN_STATE);
//			lRepoState = createStateLabel(LABEL_NO_REPO);
//
//			lPath = new JLabel("", SwingConstants.LEFT);
//
//			taStatus = GuiUtils.createTextArea(null);
//
//			return FormBuilder.create()
//					.columns("4dlu, fill:pref:grow, 4dlu")
//					.rows("bottom:pref, 4dlu, pref, pref, 12dlu, pref, 6dlu, top:pref:grow")
//					.add(bValidate).xy(2, 1, "center, center")
//					.add(lFolderState).xy(2, 3, "left, center")
//					.add(lRepoState).xy(2, 4, "left, center")
//					.add(lPath).xy(2, 6, "fill, center")
//					.add(taStatus).xy(2, 8, "fill, fill")
//					.build();
//		}
//
//		@Override
//		public void close() {
//			workspaceValidator = null;
//			workspaceState = null;
//			workspace = null;
//		};
//
//		private JLabel createStateLabel(String text) {
//			JLabel label = new JLabel(text);
//			label.setHorizontalAlignment(SwingConstants.CENTER);
//			label.setHorizontalTextPosition(SwingConstants.RIGHT);
//			label.setVerticalAlignment(SwingConstants.CENTER);
//			label.setIcon(ICON_UNKNOWN);
//			//TODO increase font size?
//			return label;
//		}
//
//		private boolean isScanning() {
//			return workspaceValidator!=null && !workspaceValidator.isDone();
//		}
//
//		private void onButtonClicked(ActionEvent ae) {
//			if(isScanning()) {
//				workspaceValidator.cancel(true);
//			} else {
//				workspaceValidator = createValidator(workspace);
//				workspaceValidator.execute();
//			}
//
//			GuiUtils.invokeEDT(this::refreshButton);
//		}
//
//		private void displayState(WorkspaceValidator.WorkspaceState state) {
//			workspaceState = state;
//
//			Icon iconFolder = lFolderState.getIcon();
//			Icon iconRepo = lRepoState.getIcon();
//
//			String labelFolder = lFolderState.getText();
//			String labelRepo = lRepoState.getText();
//
//			String status = null;
//
//			if(state!=null) {
//				switch (state) {
//				case EMPTY_FOLDER:
//					iconFolder = ICON_CHECKED;
//					iconRepo = ICON_CHECKED;
//					labelFolder = LABEL_EMPTY_FOLDER;
//					labelRepo = LABEL_NO_REPO;
//					status = TEXT_VALID_WORKSPACE;
//					break;
//
//				case USED_FOLDER:
//					iconFolder = ICON_WARN;
//					labelFolder = LABEL_USED_FOLDER;
//					iconRepo = ICON_CHECKED;
//					labelRepo = LABEL_NO_REPO;
//					status = TEXT_USED_WORKSPACE;
//					break;
//
//				case RDH_REPO:
//					iconFolder = ICON_WARN;
//					labelFolder = LABEL_USED_FOLDER;
//					iconRepo = ICON_CHECKED;
//					labelRepo = LABEL_RDH_REPO;
//					status = TEXT_RDH_WORKSPACE;
//					break;
//
//				case FOREIGN_REPO:
//					iconFolder = ICON_WARN;
//					labelFolder = LABEL_USED_FOLDER;
//					iconRepo = ICON_FAILED;
//					labelRepo = LABEL_FOREIG_REPO;
//					status = TEXT_INVALID_WORKSPACE;
//					break;
//
//				default:
//					break;
//				}
//			} else {
//				iconFolder = iconRepo = ICON_UNKNOWN;
//				labelFolder = LABEL_UNKNOWN_STATE;
//				labelRepo = LABEL_NO_REPO;
//			}
//
//			lFolderState.setIcon(iconFolder);
//			lFolderState.setText(labelFolder);
//
//			lRepoState.setIcon(iconRepo);
//			lRepoState.setText(labelRepo);
//
//			taStatus.setText(status);
//
//			refreshButton();
//		}
//
//		private void displayFolder(Path folder) {
//
//			ResourceManager rm = ResourceManager.getInstance();
//
//			if(folder==null) { // Invalid workspace folder
//				String text = rm.get("replaydh.wizard.welcome.validateWorkspace.missingFolder");
//				lPath.setText(text);
//			} else if(isScanning()){ // Scan in progress -> display folder currently being scanned
//
//				String pathString = toPathString(folder);
//				String text = rm.get("replaydh.wizard.welcome.validateWorkspace.currentFolder", null, pathString);
//				lPath.setText(text);
//
//			} else { // No active scan -> show selected workspace
//
//				String pathString = toPathString(folder);
//				String text = rm.get("replaydh.wizard.welcome.validateWorkspace.selectedWorkspace", null, pathString);
//				lPath.setText(text);
//			}
//
//			refreshButton();
//		}
//
//		private String toPathString(Path file) {
//			file = file.toAbsolutePath();
//
//			String path = file.toString();
//
//			if(path.length()>50 && file.getNameCount()>3) {
//				StringBuilder left = new StringBuilder(30);
//				StringBuilder right = new StringBuilder(30);
//				char SEP = File.separatorChar;
//
//				Path root = file.getRoot();
//				if(root!=null) {
//					left.append(root.toString()).append(SEP);
//				}
//
//				int leftIndex = 0;
//				int rightIndex = file.getNameCount()-1;
//
//				boolean useRight = true;
//
//				while(left.length()+right.length()<50 && rightIndex>leftIndex) {
//					if(useRight) {
//						right.insert(0, file.getName(rightIndex--).toString()).insert(0, SEP);
//					} else {
//						left.append(file.getName(leftIndex++).toString()).append(SEP);
//					}
//
//					useRight = !useRight;
//				}
//
//				path = left.append("...").append(right).toString();
//			}
//
//			return path;
//		}
//
//		private void refreshButton() {
//			ResourceManager rm = ResourceManager.getInstance();
//			IconRegistry ir = IconRegistry.getGlobalRegistry();
//
//			if(isScanning()) {
//				bValidate.setText(rm.get("replaydh.wizard.welcome.validateWorkspace.cancel.label"));
//				bValidate.setToolTipText(rm.get("replaydh.wizard.welcome.validateWorkspace.cancel.description"));
//				bValidate.setIcon(ir.getIcon("loading-64.gif", Resolution.forSize(24)));
//			} else {
//				bValidate.setText(rm.get("replaydh.wizard.welcome.validateWorkspace.validate.label"));
//				bValidate.setToolTipText(rm.get("replaydh.wizard.welcome.validateWorkspace.validate.description"));
//				bValidate.setIcon(ir.getIcon("update-icon.png", Resolution.forSize(24)));
//			}
//
//			bValidate.setEnabled(workspace!=null);
//		}
//
//		private WorkspaceValidator createValidator(Path workspace) {
//			return new WorkspaceValidator(workspace){
//				@Override
//				protected void process(List<Path> chunks) {
//					if(!chunks.isEmpty()) {
//						displayFolder(chunks.get(chunks.size()-1));
//					}
//				}
//
//				@Override
//				protected void done() {
//
//					workspaceValidator = null;
//
//					if(isCancelled()) {
//						displayState(null);
//					} else {
//						boolean workspaceValid = getWorkspaceState().compareTo(WorkspaceValidator.WorkspaceState.RDH_REPO)<=0;
//
//						displayState(getWorkspaceState());
//						setNextEnabled(workspaceValid);
//						refreshButton();
//					}
//				};
//			};
//		}
//
//		@Override
//		public void refresh(RDHEnvironment environment, WelcomeContext context) {
//			workspace = context.workspace;
//
//			boolean canSkip = false;
//
//			WorkspaceValidator.WorkspaceState state = context.workspaceState;
//			if(state!=null) {
//				canSkip = state.compareTo(WorkspaceValidator.WorkspaceState.RDH_REPO)<=0;
//			} else {
//				displayFolder(workspace);
//			}
//
//			displayState(state);
//
//			setNextEnabled(canSkip);
//		};
//
//		@Override
//		public void cancel(RDHEnvironment environment, WelcomeContext context) {
//			context.workspaceState = null;
//		};
//	};

	/**
	 * Wrap up info
	 */
	private static final WelcomeStep FINISH = new WelcomeStep(
			"replaydh.wizard.welcome.finish.title",
			"replaydh.wizard.welcome.finish.description") {

		@Override
		public Page<WelcomeContext> next(RDHEnvironment environment, WelcomeContext context) {
			return null;
		}

		@Override
		protected JPanel createPanel() {
			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("top:pref")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.welcome.finish.message"))).xy(1, 1)
					.build();
		}
	};
}

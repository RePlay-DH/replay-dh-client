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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.UserFolder;
import bwfdm.replaydh.io.resources.FileResource;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.AbstractWizardStep;
import bwfdm.replaydh.ui.helper.DocumentAdapter;
import bwfdm.replaydh.ui.helper.FilePanel;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.helper.Wizard.Page;
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.ui.icons.Resolution;
import bwfdm.replaydh.utils.AccessMode;
import bwfdm.replaydh.utils.RDHUtils;
import bwfdm.replaydh.workflow.schema.WorkflowSchemaManager;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;
import bwfdm.replaydh.workflow.schema.WorkflowSchemaXml;

/**
 * @author Markus Gärtner
 *
 */
public class AddWorkflowSchemaWizard {

	private static final Logger log = LoggerFactory.getLogger(AddWorkflowSchemaWizard.class);

	public static Wizard<AddWorkflowSchemaContext> getWizard(Window parent, RDHEnvironment environment) {
		@SuppressWarnings("unchecked")
		Wizard<AddWorkflowSchemaContext> wizard = new Wizard<>(
				parent, "addWorkflowSchema", ResourceManager.getInstance().get("replaydh.wizard.addWorkflowSchema.title"),
				environment, SELECT_SOURCE_FILE, VALIDATE_SCHEMA, DEFINE_TARGET_FILE, FINISH);

		return wizard;
	}

	public static final class AddWorkflowSchemaContext {

		public static AddWorkflowSchemaContext blank() {
			return new AddWorkflowSchemaContext();
		}

		AddWorkflowSchemaContext() {
			// no-op
		}

		Path sourceFile;
		Path targetFile;
		WorkflowSchema schema;

		public Path getSourceFile() {
			return sourceFile;
		}

		public Path getTargetFile() {
			return targetFile;
		}

		public WorkflowSchema getSchema() {
			return schema;
		}
	}

	private enum ValidationResultType {
		UNKNOWN,
		MISSING_FILE,
		EMPTY_SCHEMA,
		INVALID_CONTENT,
		DUPLICATE_ID,
		ERROR,
		CANCELLED,
		VALID,
		;
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	private static abstract class AddWorkflowSchemaStep extends AbstractWizardStep<AddWorkflowSchemaContext> {
		protected AddWorkflowSchemaStep(String id, String titleKey, String descriptionKey) {
			super(id, titleKey, descriptionKey);
		}
	}


	/**
	 * Let user provide the file that should be loaded
	 */
	private static final AddWorkflowSchemaStep SELECT_SOURCE_FILE = new AddWorkflowSchemaStep(
			"selectSourceFile",
			"replaydh.wizard.addWorkflowSchema.selectSourceFile.title",
			"replaydh.wizard.addWorkflowSchema.selectSourceFile.description") {

		private FilePanel filePanel;

		private void configureFileChooser(JFileChooser fileChooser) {
			//TODO add title for chooser
		}

		private void onFilePanelChange(ChangeEvent evt) {
			Path file = filePanel.getFile();
			setNextEnabled(file!=null && Files.exists(file, LinkOption.NOFOLLOW_LINKS));
		}

		@Override
		protected JPanel createPanel() {

			ResourceManager rm = ResourceManager.getInstance();

			filePanel = FilePanel.newBuilder()
					.acceptedFileType(JFileChooser.FILES_ONLY)
					.fileLimit(1)
					.fileChooserSetup(this::configureFileChooser)
					.build();

			filePanel.addChangeListener(this::onFilePanelChange);

			return FormBuilder.create()
					.columns("pref:grow:fill")
					.rows("pref, 6dlu, pref, 6dlu, pref")
					.add(GuiUtils.createTextArea(rm.get("replaydh.wizard.addWorkflowSchema.selectSourceFile.message"))).xy(1, 1)	//TODO
					.add(filePanel).xy(1, 3)
					.add(GuiUtils.createTextArea(rm.get(""))).xy(1, 5)	//TODO add further info text
					.build();
		}

		@Override
		public void refresh(RDHEnvironment environment, AddWorkflowSchemaContext context) {
			filePanel.setFile(context.sourceFile);
		};

		@Override
		public Page<AddWorkflowSchemaContext> next(RDHEnvironment environment, AddWorkflowSchemaContext context) {
			context.sourceFile = filePanel.getFile();

			return VALIDATE_SCHEMA;
		}
	};

	private static class ValidationResult {
		ValidationResultType type;
		Exception exception;
		String schemaId;
		WorkflowSchema schema;
	}

	/**
	 * Try to read schema -> XML validation
	 * Check if duplicate key -> consistency
	 */
	private static final AddWorkflowSchemaStep VALIDATE_SCHEMA = new AddWorkflowSchemaStep(
			"validateSchema",
			"replaydh.wizard.addWorkflowSchema.validateSchema.title",
			"replaydh.wizard.addWorkflowSchema.validateSchema.description") {

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

		/**
		 * Initiate validation process
		 */
		JButton bValidate;

		/**
		 * Displays if file could be read
		 */
		JLabel lFileState;

		/**
		 * Displays if schema is correct/unique
		 */
		JLabel lSchemaState;

		/**
		 * General place for result information
		 */
		JTextArea taStatus;

		/**
		 * Path to the schema file
		 */
		JLabel lPath;

		ValidationResult validationResult;

		SwingWorker<ValidationResult, Void> validator;

		PropertyChangeListener validatorObserver;

		// State icons
		private final Icon ICON_UNKNOWN = IconRegistry.getGlobalRegistry().getIcon("icons8-Help-48.png", Resolution.forSize(24));
		private final Icon ICON_FAILED = IconRegistry.getGlobalRegistry().getIcon("icons8-Cancel-48.png", Resolution.forSize(24));
		private final Icon ICON_CHECKED = IconRegistry.getGlobalRegistry().getIcon("icons8-Ok-48.png", Resolution.forSize(24));
		private final Icon ICON_WARN = IconRegistry.getGlobalRegistry().getIcon("icons8-Error-48.png", Resolution.forSize(24));

		private final String LABEL_FILE_MISSING = ResourceManager.getInstance().get(
				"replaydh.wizard.addWorkflowSchema.validateSchema.fileMissing");
		private final String LABEL_EMPTY_SCHEMA = ResourceManager.getInstance().get(
				"replaydh.wizard.addWorkflowSchema.validateSchema.emptySchema");
		private final String LABEL_INVALID_CONTENT = ResourceManager.getInstance().get(
				"replaydh.wizard.addWorkflowSchema.validateSchema.invalidContent");
		private final String LABEL_DUPLICATE_ID = ResourceManager.getInstance().get(
				"replaydh.wizard.addWorkflowSchema.validateSchema.duplicateId");

		private final String LABEL_FILE_STATE = ResourceManager.getInstance().get(
				"replaydh.wizard.addWorkflowSchema.validateSchema.fileState");
		private final String LABEL_SCHEMA_STATE = ResourceManager.getInstance().get(
				"replaydh.wizard.addWorkflowSchema.validateSchema.schemaState");

		private final String LABEL_VALID_FILE = ResourceManager.getInstance().get(
				"replaydh.wizard.addWorkflowSchema.validateSchema.validFile");
		private final String LABEL_VALID_SCHEMA = ResourceManager.getInstance().get(
				"replaydh.wizard.addWorkflowSchema.validateSchema.validSchema");

		private final String LABEL_FILE_ERROR = ResourceManager.getInstance().get(
				"replaydh.wizard.addWorkflowSchema.validateSchema.fileError");
		private final String LABEL_SCHEMA_ERROR = ResourceManager.getInstance().get(
				"replaydh.wizard.addWorkflowSchema.validateSchema.schemaError");

		private final String LABEL_CANCELLED = ResourceManager.getInstance().get(
				"replaydh.wizard.addWorkflowSchema.validateSchema.cancelled");
		private final String LABEL_VALIDATION_FAILED = ResourceManager.getInstance().get(
				"replaydh.wizard.addWorkflowSchema.validateSchema.validationFailed");
		private final String LABEL_VALIDATION_DONE = ResourceManager.getInstance().get(
				"replaydh.wizard.addWorkflowSchema.validateSchema.validationDone");

		@Override
		protected JPanel createPanel() {

			bValidate = new JButton();
			bValidate.setFont(bValidate.getFont().deriveFont(Font.BOLD, 16));
			bValidate.addActionListener(this::onButtonClicked);

			lFileState = createStateLabel(LABEL_FILE_STATE);
			lSchemaState = createStateLabel(LABEL_SCHEMA_STATE);

			lPath = new JLabel("", SwingConstants.LEFT);

			taStatus = GuiUtils.createTextArea(null);

			return FormBuilder.create()
					.columns("4dlu, fill:pref:grow, 4dlu")
					.rows("bottom:pref, 4dlu, pref, pref, 12dlu, pref, 6dlu, top:pref:grow")
					.add(bValidate).xy(2, 1, "center, center")
					.add(lFileState).xy(2, 3, "left, center")
					.add(lSchemaState).xy(2, 4, "left, center")
					.add(lPath).xy(2, 6, "fill, center")
					.add(taStatus).xy(2, 8, "fill, fill")
					.build();
		}

		private JLabel createStateLabel(String text) {
			JLabel label = new JLabel(text);
			label.setHorizontalAlignment(SwingConstants.CENTER);
			label.setHorizontalTextPosition(SwingConstants.RIGHT);
			label.setVerticalAlignment(SwingConstants.CENTER);
			label.setIcon(ICON_UNKNOWN);

			return label;
		}

		private boolean isValidating() {
			return validator!=null && validator.getState()==StateValue.STARTED;
		}

		private boolean canValidate() {
			return validator!=null && validator.getState()==StateValue.PENDING;
		}

		private void onButtonClicked(ActionEvent ae) {
			if(isValidating()) {
				validator.cancel(true);
			} else if(canValidate()) {
				validator.execute();
			}

//			GuiUtils.invokeEDT(this::refreshButton);
		}

		private void onValidatorStateChange(PropertyChangeEvent pce) {
			if("state".equals(pce.getPropertyName())) {
				GuiUtils.invokeEDT(this::refreshButton);
			}
		}

		private SwingWorker<ValidationResult, Void> createValidator(final RDHEnvironment environment, final Path file) {
			return new SwingWorker<ValidationResult, Void>() {

				@Override
				protected ValidationResult doInBackground() throws Exception {
					ValidationResult result = new ValidationResult();

					// Step 1: Try to read the schema file
					if(!Files.exists(file, LinkOption.NOFOLLOW_LINKS))  {
						result.type = ValidationResultType.MISSING_FILE;
						return result;
					}

					try {
						if(Files.size(file)==0) {
							result.type = ValidationResultType.EMPTY_SCHEMA;
							return result;
						}
					} catch(IOException e) {
						log.error("Failed to obtain file size of schema file: {}", file, e);
						result.type = ValidationResultType.ERROR;
						result.exception = e;
						return result;
					}

					try {
						result.schema = WorkflowSchemaXml.readSchema(new FileResource(file, AccessMode.READ));
					} catch (ExecutionException e) {
						log.error("Failed to read schema file: {}", file, e.getCause());
						result.type = ValidationResultType.INVALID_CONTENT;
						result.exception = (Exception) e.getCause();
						return result;
					}

					// Step 2: Verify integrity by looking up the id
					WorkflowSchemaManager schemaManager = environment.getClient().getWorkflowSchemaManager();
					result.schemaId = result.schema.getId();
					if(schemaManager.lookupSchema(result.schemaId)!=null) {
						result.type = ValidationResultType.DUPLICATE_ID;
						return result;
					}

					result.type = ValidationResultType.VALID;

					return result;
				}

				@Override
				protected void done() {
					ValidationResult result = null;

					try {
						if(isCancelled()) {
							result = new ValidationResult();
							result.type = ValidationResultType.CANCELLED;
						} else {
							result = get();
						}
					} catch (InterruptedException e) {
						// allowed cancellation
						result = new ValidationResult();
						result.type = ValidationResultType.CANCELLED;
					} catch (ExecutionException e) {
						log.error("Unexpected error in validation process", e.getCause());
						result = new ValidationResult();
						result.type = ValidationResultType.ERROR;
						result.exception = (Exception) e.getCause();
					} finally {
						validator = null;
					}

					validationResult = result;

					showResult();
				};
			};
		}

		private void showResult() {

			// Default values follow the "UNKNOWN" type
			String status = null;
			String fileState = LABEL_FILE_STATE;
			String schemaState = LABEL_SCHEMA_STATE;
			Icon fileIcon = ICON_UNKNOWN;
			Icon schemaIcon = ICON_UNKNOWN;

			ValidationResultType type = validationResult==null ?
					ValidationResultType.UNKNOWN : validationResult.type;

			switch (type) {
			case VALID:
				status = LABEL_VALIDATION_DONE;
				fileState = LABEL_VALID_FILE;
				schemaState = LABEL_VALID_SCHEMA;
				fileIcon = schemaIcon = ICON_CHECKED;
				break;

			case MISSING_FILE:
				status = LABEL_VALIDATION_FAILED;
				fileState = LABEL_FILE_MISSING;
				fileIcon = ICON_FAILED;
				break;

			case EMPTY_SCHEMA:
				status = LABEL_VALIDATION_FAILED;
				fileState = LABEL_EMPTY_SCHEMA;
				fileIcon = ICON_FAILED;
				break;

			case INVALID_CONTENT:
				status = LABEL_VALIDATION_FAILED;
				fileState = LABEL_VALID_FILE;
				fileIcon = ICON_CHECKED;
				schemaState = LABEL_INVALID_CONTENT;
				schemaIcon = ICON_FAILED;
				break;

			case DUPLICATE_ID:
				status = LABEL_VALIDATION_FAILED;
				fileState = LABEL_VALID_FILE;
				fileIcon = ICON_CHECKED;
				schemaState = LABEL_DUPLICATE_ID;
				schemaIcon = ICON_FAILED;
				break;

			case ERROR:
				// Can only have this type if validation failed
				Exception e = validationResult.exception;
				status = ResourceManager.getInstance().get(
						"replaydh.wizard.addWorkflowSchema.validateSchema.unexpectedError", e.getMessage());
				fileState = LABEL_FILE_ERROR;
				schemaState = LABEL_SCHEMA_ERROR;
				fileIcon = schemaIcon = ICON_FAILED;
				break;

			case CANCELLED:
				status = LABEL_CANCELLED;
				fileIcon = schemaIcon = ICON_WARN;
				break;

			default:
				break;
			}

			taStatus.setText(status);
			lFileState.setText(fileState);
			lFileState.setIcon(fileIcon);
			lSchemaState.setText(schemaState);
			lSchemaState.setIcon(schemaIcon);

			refreshButton();

			setNextEnabled(type==ValidationResultType.VALID);
		}

		private void refreshButton() {
			ResourceManager rm = ResourceManager.getInstance();
			IconRegistry ir = IconRegistry.getGlobalRegistry();

			if(isValidating()) {
				// CANCEL mode
				bValidate.setText(rm.get("replaydh.wizard.addWorkflowSchema.validateSchema.cancel.label"));
				bValidate.setToolTipText(GuiUtils.toSwingTooltip(
						rm.get("replaydh.wizard.addWorkflowSchema.validateSchema.cancel.description")));
				bValidate.setIcon(ir.getIcon("loading-64.gif", Resolution.forSize(24)));
			} else {
				// VALIDATE mode
				bValidate.setText(rm.get("replaydh.wizard.addWorkflowSchema.validateSchema.validate.label"));
				bValidate.setToolTipText(GuiUtils.toSwingTooltip(
						rm.get("replaydh.wizard.addWorkflowSchema.validateSchema.validate.description")));
				bValidate.setIcon(ir.getIcon("update-icon.png", Resolution.forSize(24)));
			}

			bValidate.setEnabled(canValidate());
		}

		@Override
		public void refresh(RDHEnvironment environment, AddWorkflowSchemaContext context) {

			if(validationResult==null) {
				validator = createValidator(environment, context.sourceFile);

				validatorObserver = this::onValidatorStateChange;
				validator.addPropertyChangeListener(validatorObserver);
			}

			showResult();

			setNextEnabled(validationResult!=null && validationResult.type==ValidationResultType.VALID);
		};

		@Override
		public void cancel(RDHEnvironment environment, AddWorkflowSchemaContext context) {
			context.schema = null;
		};

		@Override
		public boolean close() {
			if(validator!=null && validatorObserver!=null) {
				validator.removePropertyChangeListener(validatorObserver);
			}

			validationResult = null;
			validator = null;
			validatorObserver = null;

			return super.close();
		};

		@Override
		public Page<AddWorkflowSchemaContext> next(RDHEnvironment environment, AddWorkflowSchemaContext context) {
			context.schema = validationResult.schema;
			return DEFINE_TARGET_FILE;
		}
	};

	/**
	 * Let user pick a name for the target file in our schema folder
	 */
	private static final AddWorkflowSchemaStep DEFINE_TARGET_FILE = new AddWorkflowSchemaStep(
			"defineTargetFile",
			"replaydh.wizard.addWorkflowSchema.defineTargetFile.title",
			"replaydh.wizard.addWorkflowSchema.defineTargetFile.description") {

		private JTextField tfName;
		private JTextArea taInfo;
		private JTextArea taStatus;

		private DocumentListener inputObserver;

		private static final String DEFAULT_FILE_ENDING = ".xml";

		@Override
		protected JPanel createPanel() {

			taInfo = GuiUtils.createTextArea(null);
			taStatus = GuiUtils.createTextArea(null);

			tfName = new JTextField(25);
			GuiUtils.prepareChangeableBorder(tfName);

			return FormBuilder.create()
					.columns("pref, 4dlu, pref:grow:fill")
					.rows("pref, 12dlu, pref, 6dlu, pref")
					.add(taInfo).xyw(1, 1, 3)
					.addLabel(ResourceManager.getInstance().get("replaydh.wizard.addWorkflowSchema.defineTargetFile.fileName")).xy(1, 3)
					.add(tfName).xy(3, 3)
					.add(taStatus).xyw(1, 5, 3)
					.build();
		}

		private String prepareName(String name) {
			if(!name.endsWith(DEFAULT_FILE_ENDING)) {
				name = name+DEFAULT_FILE_ENDING;
			}
			return name;
		}

		@Override
		public void refresh(RDHEnvironment environment, AddWorkflowSchemaContext context) {

			final Path folder = environment.getClient().getUserFolder(UserFolder.SCHEMAS);
			final ResourceManager rm = ResourceManager.getInstance();

			String text = rm.get("replaydh.wizard.addWorkflowSchema.defineTargetFile.message",
					RDHUtils.toPathString(folder, RDHUtils.DEFAULT_PATH_STRING_LENGTH_LIMIT));
			taInfo.setText(text);

			inputObserver = new DocumentAdapter() {
				@Override
				public void anyUpdate(DocumentEvent de) {
					String warningMessage = null;

					String name = tfName.getText();

					if(name==null || name.isEmpty()) {
						warningMessage = rm.get("replaydh.wizard.addWorkflowSchema.defineTargetFile.emptyFileName");
					} else {
						boolean valid = true;
						try {
							Path file = folder.resolve(name);
							if(Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
								warningMessage = rm.get("replaydh.wizard.addWorkflowSchema.defineTargetFile.fileAlreadyExists");
							}
							valid &= !Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS);
						} catch(InvalidPathException e) {
							valid = false;
						}

						if(!valid && warningMessage==null) {
							warningMessage = rm.get("replaydh.wizard.addWorkflowSchema.defineTargetFile.invalidFileName");
						}
					}

					boolean canContinue = warningMessage==null;

					taStatus.setText(warningMessage);
					GuiUtils.toggleChangeableBorder(tfName, !canContinue);
					setNextEnabled(canContinue);
				};
			};
			tfName.getDocument().addDocumentListener(inputObserver);

			if(context.targetFile!=null) {
				tfName.setText(context.targetFile.getFileName().toString());
			} else if(context.sourceFile!=null) {
				String name = prepareName(context.sourceFile.getFileName().toString());
				tfName.setText(name);
			} else {
				tfName.setText(null);
			}
		};

		@Override
		public void cancel(RDHEnvironment environment, AddWorkflowSchemaContext context) {
			close();
		};

		@Override
		public boolean close() {
			if(inputObserver!=null) {
				tfName.getDocument().removeDocumentListener(inputObserver);
			}

			return super.close();
		};

		@Override
		public Page<AddWorkflowSchemaContext> next(RDHEnvironment environment, AddWorkflowSchemaContext context) {

			String name = prepareName(tfName.getText());

			Path folder = environment.getClient().getUserFolder(UserFolder.SCHEMAS);
			context.targetFile = folder.resolve(name);

			return FINISH;
		}
	};

	/**
	 * Just some wrapup
	 */
	private static final AddWorkflowSchemaStep FINISH = new AddWorkflowSchemaStep(
			"finish",
			"replaydh.wizard.addWorkflowSchema.finish.title",
			"replaydh.wizard.addWorkflowSchema.finish.description") {

		private JTextArea taInfo;

		@Override
		protected JPanel createPanel() {
			JPanel panel = new JPanel(new BorderLayout());

			taInfo = GuiUtils.createTextArea(null);
			panel.add(taInfo, BorderLayout.CENTER);

			return panel;
		}

		@Override
		public void refresh(RDHEnvironment environment, AddWorkflowSchemaContext context) {

			String source = RDHUtils.toPathString(context.sourceFile, RDHUtils.DEFAULT_PATH_STRING_LENGTH_LIMIT);
			String target = context.targetFile.getFileName().toString();
			String text = ResourceManager.getInstance().get(
					"replaydh.wizard.addWorkflowSchema.finish.message", source, target);

			taInfo.setText(text);
		};

		@Override
		public Page<AddWorkflowSchemaContext> next(RDHEnvironment environment, AddWorkflowSchemaContext context) {
			return null;
		}
	};
}

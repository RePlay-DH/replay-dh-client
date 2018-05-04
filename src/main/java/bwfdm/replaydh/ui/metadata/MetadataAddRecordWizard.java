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
package bwfdm.replaydh.ui.metadata;

import static java.util.Objects.requireNonNull;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.io.LocalFileObject;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRepository;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.AbstractWizardStep;
import bwfdm.replaydh.ui.helper.FilePanel;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.helper.Wizard.Page;
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.ui.icons.Resolution;
import bwfdm.replaydh.utils.LookupResult;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.impl.DefaultResource;
import bwfdm.replaydh.workflow.resolver.IdentifiableResolver;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public class MetadataAddRecordWizard {

	private static final Logger log = LoggerFactory.getLogger(MetadataAddRecordWizard.class);

	public static Wizard<AddRecordContext> getWizard(Window parent, RDHEnvironment environment) {
		@SuppressWarnings("unchecked")
		Wizard<AddRecordContext> wizard = new Wizard<>(
				parent, ResourceManager.getInstance().get("replaydh.wizard.addRecord.title"),
				environment, SELECT_TYPE, SELECT_TARGET, VERIFY_TARGET,
				//SELECT_METHOD, IMPORT_RECORD,  //TODO include steps in final wizard
				FINISH);

		return wizard;
	}

	public static final class AddRecordContext {

		public static AddRecordContext blank(MetadataRepository repository, RDHEnvironment environment) {
			return new AddRecordContext(repository, environment);
		}

		public static AddRecordContext asEdit(MetadataRepository repository, RDHEnvironment environment, MetadataRecord record) {
			AddRecordContext context = blank(repository, environment);
			context.sourceRecord = requireNonNull(record);
			return context;
		}

		public static AddRecordContext forResource(MetadataRepository repository, RDHEnvironment environment, Identifiable identifiable) {
			AddRecordContext context = blank(repository, environment);
			context.identifiable = requireNonNull(identifiable);
			return context;
		}


		private AddRecordContext(MetadataRepository repository, RDHEnvironment environment) {
			this.repository = requireNonNull(repository);
			this.environment = requireNonNull(environment);
		}

		final MetadataRepository repository;
		final RDHEnvironment environment;

		Identifiable identifiable;
		MetadataRecord record;
		MetadataRecord sourceRecord;

		Type type;
		Method method;
		Path file;
		URL url;

		VerificationResult verificationResult;

		public MetadataRepository getRepository() {
			return repository;
		}
		public RDHEnvironment getEnvironment() {
			return environment;
		}
		public Identifiable getIdentifiable() {
			return identifiable;
		}
		public MetadataRecord getRecord() {
			return record;
		}
		public Path getFile() {
			return file;
		}
		public URL getUrl() {
			return url;
		}
		public Object getTarget() {
			return type==Type.FILE ? getFile() : getUrl();
		}
	}

	//TODO needed?
	enum Mode {
		FULL_CREATE,
		FIXED_TARGET,
		COPY_RECORD,
		;
	}

	enum Type {
		FILE,
		URL,
		;
	}

	enum Method {
		CREATE,
		IMPORT_FILE,
		IMPORT_RECORD,
		;
	}

	enum VerificationResult {
		/**
		 * No resource matching the given identifiers is known.
		 */
		UNKNOWN_RESOURCE,
		/**
		 * A resource has already been registered for the given identifiers.
		 */
		KNOWN_RESOURCE,
		/**
		 * A metadata record is already present in the repository for the given resource.
		 */
		KNOWN_RECORD,
		/**
		 * Indicating any kind of exception or otherwise unexpected situation
		 * during verification process.
		 */
		VERIFICATION_FAILED,
		;
	}

	private static String recordLabel(AddRecordContext context) {
		String label = context.repository.getDisplayName(context.record);
		if(label==null) {
			//FIXME
			return "???";
		}
		return label;
	}

	private static String repositoryLabel(AddRecordContext context) {
		return context.repository.getDisplayName();
	}

	private static String resourceLabel(AddRecordContext context) {
		Identifiable resource = context.identifiable;
		Identifier identifier = Identifiable.getBestIdentifier(resource);

		if(identifier==null) {
			return ResourceManager.getInstance().get("replaydh.labels.unnamed", resource.getType());
		} else {
			return identifier.getId();
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	private static abstract class AddRecordStep extends AbstractWizardStep<AddRecordContext> {
		protected AddRecordStep(String titleKey, String descriptionKey) {
			super(titleKey, descriptionKey);
		}
	}

	/**
	 * First step - let user decide what type of resource should get new metadata:
	 * <ul>
	 * <li>local file</li>
	 * <li>remote resource (URL)</li>
	 * <li>a person (???)</li>
	 * </ul>
	 */
	private static final AddRecordStep SELECT_TYPE = new AddRecordStep(
			"replaydh.wizard.addRecord.selectType.title",
			"replaydh.wizard.addRecord.selectType.description") {

		private ButtonGroup buttonGroup;

		private JRadioButton rbFile, rbUrl;


		@Override
		protected JPanel createPanel() {
			ResourceManager rm = ResourceManager.getInstance();

			buttonGroup = new ButtonGroup();

			rbFile = new JRadioButton(rm.get("replaydh.wizard.addRecord.selectType.file"));
			rbUrl = new JRadioButton(rm.get("replaydh.wizard.addRecord.selectType.url"));

			ChangeListener changeListener = this::onSelectionChange;
			rbFile.addChangeListener(changeListener);
			rbUrl.addChangeListener(changeListener);

			buttonGroup.add(rbFile);
			buttonGroup.add(rbUrl);

			return FormBuilder.create()
					.columns("10dlu, fill:pref:grow")
					.rows("top:pref, 8dlu, pref, 5dlu, pref")
					.add(GuiUtils.createTextArea(
							rm.get("replaydh.wizard.addRecord.selectType.message"))).xy(2, 1)
					.add(rbFile).xy(2, 3, "left, center")
					.add(rbUrl).xy(2, 5, "left, center")
					.build();
		}

		private void onSelectionChange(ChangeEvent ce) {
			setNextEnabled(rbFile.isSelected() || rbUrl.isSelected());
		}

		@Override
		public void refresh(RDHEnvironment environment, AddRecordContext context) {
			Type type = context.type;
			if(type==null) {
				type = Type.FILE;
			}

			rbFile.setSelected(type==Type.FILE);
			rbUrl.setSelected(type==Type.URL);
		};

		@Override
		public void cancel(RDHEnvironment environment, AddRecordContext context) {
			context.type = null;
		};

		@Override
		public Page<AddRecordContext> next(RDHEnvironment environment, AddRecordContext context) {

			if(rbFile.isSelected()) {
				context.type = Type.FILE;
			} else if(rbUrl.isSelected()) {
				context.type = Type.URL;
			}

			return SELECT_TARGET;
		}
	};

	/**
	 * Lets the user pick a target of the previously chosen type
	 */
	private static final AddRecordStep SELECT_TARGET = new AddRecordStep(
			"replaydh.wizard.addRecord.selectTarget.title",
			"replaydh.wizard.addRecord.selectTarget.description") {

		FilePanel fpResource;

		JTextField tfResource;

		Border defaultBorder, errorBorder, emptyBorder;

		JPanel filePanel, urlPanel;

		CardLayout layout;

		private void configureFileChooser(JFileChooser fileChooser) {
			fileChooser.setDialogTitle(ResourceManager.getInstance().get(
					"replaydh.wizard.addRecord.selectTarget.dialogTitle"));

			if(fpResource!=null) {
				Path resource = fpResource.getFile();
				if(resource!=null && Files.isRegularFile(resource, LinkOption.NOFOLLOW_LINKS)) {
					fileChooser.setCurrentDirectory(resource.getParent().toFile());
				}
			}
		}

		private boolean isValidResourceFile() {
			Path file = fpResource.getFile();
			return file!=null && Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS);
		}

		private void onUrlChange() {
			URL url = fetchURL(false);
			boolean validUrl = url!=null;

			if(validUrl) {
				tfResource.setBorder(BorderFactory.createCompoundBorder(emptyBorder, defaultBorder));
			} else {
				tfResource.setBorder(BorderFactory.createCompoundBorder(errorBorder, defaultBorder));
			}

			setNextEnabled(validUrl);
		}

		private URL fetchURL(boolean allowError) {
			String urlString = tfResource.getText();

			try {
				return new URL(urlString);
			} catch (MalformedURLException e) {
				if(allowError) {
					throw new RDHException("Failed to fetch resource URL", e);
				}
				return null;
			}
		}

		@Override
		protected JPanel createPanel() {

			fpResource = FilePanel.newBuilder()
					.acceptedFileType(JFileChooser.FILES_ONLY)
					.fileLimit(1)
					.fileChooserSetup(this::configureFileChooser)
					.build();

			fpResource.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					setNextEnabled(isValidResourceFile());
				}
			});

			tfResource = new JTextField();

			tfResource.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void removeUpdate(DocumentEvent e) {
					onUrlChange();
				}
				@Override
				public void insertUpdate(DocumentEvent e) {
					onUrlChange();
				}
				@Override
				public void changedUpdate(DocumentEvent e) {
					onUrlChange();
				}
			});

			filePanel = FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("top:pref, 8dlu, pref")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.addRecord.selectTarget.fileMessage"))).xy(1, 1)
					.add(fpResource).xy(1, 3)
					.build();


			//FIXME adjust to 2 columns with URL label in front of text field
			urlPanel = FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("top:pref, 8dlu, pref")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.addRecord.selectTarget.urlMessage"))).xy(1, 1)
					.add(tfResource).xy(1, 3)
					.build();

			defaultBorder = tfResource.getBorder();
			errorBorder = BorderFactory.createLineBorder(Color.red, 2);
			emptyBorder = BorderFactory.createLineBorder(urlPanel.getBackground(), 2);

			layout = new CardLayout();

			JPanel panel = new JPanel(layout);
			panel.add(filePanel);
			panel.add(urlPanel);

			return panel;
		}

		@Override
		public void refresh(RDHEnvironment environment, AddRecordContext context) {
			if(context.type==Type.FILE) {
				layout.first(panel);
			} else {
				layout.last(panel);
			}

			if(context.file!=null) {
				fpResource.setFile(context.file);
			} else if(context.url!=null) {
				tfResource.setText(context.url.toExternalForm());
			} else {
				fpResource.setFile((Path)null);
				tfResource.setText(null);
			}
		};

		@Override
		public void cancel(RDHEnvironment environment, AddRecordContext context) {
			context.file = null;
			context.url = null;
		};

		@Override
		public Page<AddRecordContext> next(RDHEnvironment environment, AddRecordContext context) {
			switch (context.type) {
			case FILE:
				context.file = fpResource.getFile();
				break;

			case URL:
				context.url = fetchURL(true);
				break;

			default:
				break;
			}

			return VERIFY_TARGET;
		}
	};

	private static final AddRecordStep VERIFY_TARGET = new AddRecordStep(
			"replaydh.wizard.addRecord.verifyTarget.title",
			"replaydh.wizard.addRecord.verifyTarget.description") {
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

		JButton bVerify;

		JTextArea taStatus;

		SwingWorker<VerificationResult, Object> worker;

		@Override
		protected JPanel createPanel() {

			bVerify = new JButton();
			bVerify.setFont(bVerify.getFont().deriveFont(Font.BOLD, 16));
			bVerify.addActionListener(this::onButtonClicked);

			taStatus = GuiUtils.createTextArea(null);

			return FormBuilder.create()
					.columns("4dlu, fill:pref:grow, 4dlu")
					.rows("bottom:pref, 6dlu, top:pref:grow")
					.add(bVerify).xy(2, 1, "center, center")
					.add(taStatus).xy(2, 3, "fill, fill")
					.build();
		}

		private void onButtonClicked(ActionEvent ae) {
			if(isVerifying()) {
				worker.cancel(true);
			} else {
				worker.execute();
			}

			GuiUtils.invokeEDT(this::refreshButton);
		}

		@Override
		public void refresh(final RDHEnvironment environment, final AddRecordContext context) {

			Workflow workflow = environment.getClient().getWorkflowSource().get();
			WorkflowSchema schema = workflow.getSchema();

			worker = new SwingWorker<VerificationResult, Object>(){

				private Identifiable resolveIdentifiable(Set<Identifier> identifiers) {

					IdentifiableResolver resolver = environment.getClient().getResourceResolver();

					Identifiable identifiable = null;

					resolver.lock();
					try {
						List<LookupResult<Identifiable, Set<Identifier>>> resolvedIdentifiers =
							resolver.resolve(1, identifiers);

						if(!resolvedIdentifiers.isEmpty()) {
							identifiable = resolvedIdentifiers.get(0).getTarget();
						}

					} finally {
						resolver.unlock();
					}

					return identifiable;
				}

				private MetadataRecord resolveRecord(Identifiable identifiable) {
					return context.repository.getRecord(identifiable);
				}

				private void registerIdentifiable(Identifiable resource) {
					IdentifiableResolver resolver = environment.getClient().getResourceResolver();
					resolver.lock();
					try {
						resolver.update(Collections.singleton(resource));
					} finally {
						resolver.unlock();
					}
				}

				@Override
				protected VerificationResult doInBackground() throws Exception {
					Set<Identifier> identifiers = new HashSet<>();
					Identifiable resource = null;
					MetadataRecord record = null;

					// STEP 1:  create/load identifiers and in case of FILE type, resolve resource
					if(context.type==Type.FILE) {
						LocalFileObject fileObject = new LocalFileObject(context.file);
						try {
							LocalFileObject.ensureOrRefreshResource(fileObject, environment);
						} catch (InterruptedException e) {
							// Legal cancellation by user!
							return null;
						}

						identifiers = fileObject.getIdentifiers();
						resource = fileObject.getResource();
					} else {
						identifiers.add(new Identifier(schema.getDefaultURLIdentifierType(), context.url.toString()));
					}

					// STEP 2: If we didn't create an identifier or previous resolution failed, try to do it here
					if(resource==null) {
						resource = resolveIdentifiable(identifiers);
					}

					// STEP 3: If we managed to resolve an identifier, try the next step and grab a matching record
					if(record==null && resource!=null) {
						record = resolveRecord(resource);
					}

					// Make sure our internal identifier cache gets updated properly
					if(resource!=null) {
						registerIdentifiable(resource);
					}

					// Copy over our resolved stuff
					context.identifiable = resource;
					context.record = record;

					// Now translate verification into result enums
					if(record!=null) {
						return VerificationResult.KNOWN_RECORD;
					} else if(resource!=null) {
						return VerificationResult.KNOWN_RESOURCE;
					} else {
						if(identifiers.isEmpty())
							throw new IllegalStateException("No valid identifiers available for target: "+context.getTarget());

						// Create a fresh new resource based on our identifiers
						context.identifiable = DefaultResource.withIdentifiers(identifiers);

						return VerificationResult.UNKNOWN_RESOURCE;
					}
				}

				@Override
				protected void done() {
					if(isCancelled()) {
						//TODO maybe change to display some extra information?
						return;
					}

					VerificationResult verificationResult = null;
					try {
						verificationResult = get();
					} catch (InterruptedException e) {
						// Legal cancellation by user
					} catch (ExecutionException e) {
						log.error("Failed to verify target resource", e);
						verificationResult = VerificationResult.VERIFICATION_FAILED;
					}

					context.verificationResult = verificationResult;

					displayResult(context);
				};
			};

			taStatus.setText(ResourceManager.getInstance().get("replaydh.wizard.addRecord.verifyTarget.message"));

			refreshButton();

			setNextEnabled(false);
		};

		private boolean isVerifying() {
			return worker.getState()==StateValue.STARTED;
		}

		private void refreshButton() {
			ResourceManager rm = ResourceManager.getInstance();
			IconRegistry ir = IconRegistry.getGlobalRegistry();

			if(isVerifying()) {
				bVerify.setText(rm.get("replaydh.wizard.addRecord.verifyTarget.cancel.label"));
				bVerify.setToolTipText(rm.get("replaydh.wizard.addRecord.verifyTarget.cancel.description"));
				bVerify.setIcon(ir.getIcon("loading-64.gif", Resolution.forSize(24)));
			} else {
				bVerify.setText(rm.get("replaydh.wizard.addRecord.verifyTarget.verify.label"));
				bVerify.setToolTipText(rm.get("replaydh.wizard.addRecord.verifyTarget.verify.description"));
				bVerify.setIcon(ir.getIcon("update-icon.png", Resolution.forSize(24)));
			}

			bVerify.setEnabled(!worker.isDone());
		}

		private void displayResult(AddRecordContext context) {
			requireNonNull(context);

			ResourceManager rm = ResourceManager.getInstance();

			VerificationResult result = context.verificationResult;

			boolean targetValid = false;
			String text = null;

			switch (result) {
			case KNOWN_RECORD: {
				text = rm.get("replaydh.wizard.addRecord.verifyTarget.knownRecord",
						resourceLabel(context),
						recordLabel(context),
						repositoryLabel(context)
						);
			} break;

			case KNOWN_RESOURCE: {
				text = rm.get("replaydh.wizard.addRecord.verifyTarget.knownResource",
						null,
						resourceLabel(context)
						);

				targetValid = true;
			} break;

			case UNKNOWN_RESOURCE: {
				text = rm.get("replaydh.wizard.addRecord.verifyTarget.unknownResource",
						null,
						resourceLabel(context)
						);

				targetValid = true;
			} break;

			case VERIFICATION_FAILED: {
				text = rm.get("replaydh.wizard.addRecord.verifyTarget.verificationFailed");
			} break;

			default:
				throw new IllegalArgumentException("Unknown cerification result: "+result);
			}

			taStatus.setText(text);

			//TODO discuss if we should allow a user to create redundant metadata records (KNOWN_RECORD result)?
			setNextEnabled(targetValid);

			GuiUtils.invokeEDT(this::refreshButton);
		}

		@Override
		public boolean close() {
			worker = null;
			return super.close();
		};

		@Override
		public void cancel(RDHEnvironment environment, AddRecordContext context) {
			context.verificationResult = null;
			if(worker!=null) {
				worker.cancel(true);
			}
		};

		@Override
		public Page<AddRecordContext> next(RDHEnvironment environment, AddRecordContext context) {
			//TODO based on result skip/delegate to different pages?

			//TODO route via the SELECT_METHOD step
			context.method = Method.CREATE;
			return FINISH;
		}
	};

	/**
	 * Lets the user decide whether or not to create a blank new metadata record
	 * or to import an existing one.
	 */
	private static final AddRecordStep SELECT_METHOD = new AddRecordStep(
			"replaydh.wizard.addRecord.selectMethod.title",
			"replaydh.wizard.addRecord.selectMethod.description") {

		private ButtonGroup buttonGroup;

		private JRadioButton rbImportFile, rbImportRecord, rbCreateRecord;

		@Override
		protected JPanel createPanel() {
			ResourceManager rm = ResourceManager.getInstance();

			buttonGroup = new ButtonGroup();

			rbCreateRecord = new JRadioButton(rm.get("replaydh.wizard.addRecord.selectMethod.createNewRecord"));
			rbImportFile = new JRadioButton(rm.get("replaydh.wizard.addRecord.selectMethod.importFromFile"));
			rbImportRecord = new JRadioButton(rm.get("replaydh.wizard.addRecord.selectMethod.importFromRepo"));

			ChangeListener changeListener = this::onSelectionChange;
			rbCreateRecord.addChangeListener(changeListener);
			rbImportFile.addChangeListener(changeListener);
			rbImportRecord.addChangeListener(changeListener);

			buttonGroup.add(rbCreateRecord);
			buttonGroup.add(rbImportFile);
			buttonGroup.add(rbImportRecord);

			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("top:pref, 8dlu, pref, pref, pref, 8dlu, top:pref")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.addRecord.selectMethod.message"))).xy(1, 1)
					.add(rbCreateRecord).xy(1, 3, "left, center")
					.add(rbImportFile).xy(1, 4, "left, center")
					.add(rbImportRecord).xy(1, 5, "left, center")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.addRecord.selectMethod.message2"))).xy(1, 7)
					.build();
		}

		private void onSelectionChange(ChangeEvent ce) {
			setNextEnabled(rbCreateRecord.isSelected() || rbImportFile.isSelected() || rbImportRecord.isSelected());
		}

		@Override
		public void refresh(RDHEnvironment environment, AddRecordContext context) {
			Method method = context.method;
			if(method==null) {
				method = Method.CREATE;
			}

			rbCreateRecord.setSelected(method==Method.CREATE);
			rbImportFile.setSelected(method==Method.IMPORT_FILE);
			rbImportRecord.setSelected(method==Method.IMPORT_RECORD);
		};

		@Override
		public void cancel(RDHEnvironment environment, AddRecordContext context) {
			context.method = null;
		};

		@Override
		public Page<AddRecordContext> next(RDHEnvironment environment, AddRecordContext context) {
			if(rbCreateRecord.isSelected()) {
				context.method = Method.CREATE;
			} else if(rbImportFile.isSelected()) {
				context.method = Method.IMPORT_FILE;
			} else if(rbImportRecord.isSelected()) {
				context.method = Method.IMPORT_RECORD;
			}

			return context.method==Method.CREATE ? FINISH : IMPORT_RECORD;
		}
	};

	private static final AddRecordStep IMPORT_RECORD = new AddRecordStep(
			"replaydh.wizard.addRecord.importRecord.title",
			"replaydh.wizard.addRecord.importRecord.description") {

		@Override
		protected JPanel createPanel() {
			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("top:pref")
					.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
							"replaydh.wizard.addRecord.importRecord.message"))).xy(1, 1)
					.build();
		}

		//TODO add components and handling for importing record from repo or file

		@Override
		public Page<AddRecordContext> next(RDHEnvironment environment, AddRecordContext context) {
			return FINISH;
		}
	};

	private static final AddRecordStep FINISH = new AddRecordStep(
			"replaydh.wizard.addRecord.finish.title",
			"replaydh.wizard.addRecord.finish.description") {

		private JTextArea taMessage;

		@Override
		protected JPanel createPanel() {

			taMessage = GuiUtils.createTextArea(null);

			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("top:pref:grow")
					.add(taMessage).xy(1, 1)
					.build();
		}

		@Override
		public void refresh(RDHEnvironment environment, AddRecordContext context) {
			ResourceManager rm = ResourceManager.getInstance();

			if(context.method==Method.CREATE) {
				taMessage.setText(rm.get("replaydh.wizard.addRecord.finish.buildMessage"));
			} else {
				taMessage.setText(rm.get("replaydh.wizard.addRecord.finish.editMessage"));
			}
		};

		@Override
		public Page<AddRecordContext> next(RDHEnvironment environment, AddRecordContext context) {
			return null;
		}
	};
}

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
package bwfdm.replaydh.workflow.export.dspace;

import static java.util.Objects.requireNonNull;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.AbstractWizardStep;
import bwfdm.replaydh.ui.helper.DocumentAdapter;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.helper.Wizard.Page;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo;
import bwfdm.replaydh.workflow.export.generic.ExportRepository;

public class DSpacePublisherWizard {

	private static final Logger log = LoggerFactory.getLogger(DSpacePublisherWizard.class);

	public static Wizard<DSpaceExporterContext> getWizard(Window parent, RDHEnvironment environment) {
		@SuppressWarnings("unchecked")
		Wizard<DSpaceExporterContext> wizard = new Wizard<>(
				parent, "dspacePublisher", ResourceManager.getInstance().get("replaydh.wizard.dspacePublisher.title"),
				environment/*, FINISH /*<-- TEST*/ , CHOOSE_REPOSITORY, CHOOSE_COLLECTION, CHOOSE_FILES, EDIT_METADATA, FINISH);
		return wizard;
	}

	/**
	 * Context for the wizard
	 */
	public static final class DSpaceExporterContext{

		final WorkflowExportInfo exportInfo;

		RDHEnvironment environment;
		String repositoryURL;		//e.g. "http://123.11.11.11:8080/xmlui/" or "http://123.11.11.11:8080"
		String userSubmissionsURL; 	//e.g. http://123.11.11.11:8080/xmlui/submissions
									//TODO: could be also JSPUI !!!
		String serviceDocumentURL;
		String restURL;
		String collectionURL;
		String userLogin;
		Map<String, String> availableCollections;
		List<File> filesToPublish;
		ExportRepository exportRepository;
		MetadataObject metadataObject;

		public DSpaceExporterContext(WorkflowExportInfo exportInfo) {
			this.exportInfo = requireNonNull(exportInfo);
		}

		//TODO: add metadata. As an MetadataObject? Use MetadataEditor @see MetadataUIEditor

		public String getRepositoryURL() {
			return repositoryURL;
		}
		public String getUserSubmissionsURL() {
			return userSubmissionsURL;
		}
		public String getCollectionURL() {
			return collectionURL;
		}
		public String getUserLogin() {
			return userLogin;
		}
		public ExportRepository getExportRepository() {
			return exportRepository;
		}
		public Map<String, String> getAvailableCollections() {
			return availableCollections;
		}
		public RDHEnvironment getEnvironment() {
			return environment;
		}
		public List<File> getFilesToPublish() {
			return filesToPublish;
		}
		public MetadataObject getMetadataObject() {
			return metadataObject;
		}

	}

	public static final class MetadataObject{

		//TODO: Map<String, List<String>>
		Map<String, List<String>> mapDoublinCoreToMetadata;
		Map<String, String> mapDoublinCoreToLabel;

		/**
		 * Get map with key=doublin.core, value=metadata.
		 * <p>
		 * Should be used for the publication to the repository.
		 */
		public Map<String, List<String>> getMapDoublinCoreToMetadata(){
			if(mapDoublinCoreToMetadata != null){
				return mapDoublinCoreToMetadata;
			} else {
				return new HashMap<>();
			}
		}

		/**
		 * Get map with key=label, value=metadata.
		 * <p>
		 * Should be used ONLY for the representation of the metadata.
		 */
		//TODO: Map<String, List<String>>
		public Map<String, List<String>> getMapLabelToMetadata(){

			Map<String, List<String>> metadataMap = new HashMap<>();
			if((mapDoublinCoreToMetadata != null) && (mapDoublinCoreToLabel != null)) {
				for(Map.Entry<String, List<String>> entryDoublinCoreToMetadata: mapDoublinCoreToMetadata.entrySet()) {
					for(Map.Entry<String, String> entryDoublinCoreToLabel: mapDoublinCoreToLabel.entrySet()) {
						if(entryDoublinCoreToLabel.getKey().equals(entryDoublinCoreToMetadata.getKey())) {
							metadataMap.put(entryDoublinCoreToLabel.getValue(), entryDoublinCoreToMetadata.getValue());
							break;
						}
					}
				}
			}
			return metadataMap;
		}
	}


	/**
	 * <pre>
	 * Remove from the url:
	 * -- one symbol '/' on the end
	 * Add to the url:
	 * -- "http://" if "http://" or "https://" do not exist.
	 * </pre>
	 * @param fullURL
	 * @return
	 */
	private static String getCorrectedURL(final String fullURL) {
		requireNonNull(fullURL);
		String url = new String(fullURL);

		// Remove last '/'
		if(url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}

		// Add "http://" if not existed
		//TODO: should we use "https" instead?? Replace after the detailed testing with different repositories.
		if((!url.startsWith("http://")) && (!url.startsWith("https://"))) {
			url = "http://" + url;
		}

		return url;
	}

	/**
	 * <pre>
	 * Call at first {@link #getCorrectedURL(String)} and remove after that:
	 * -- "/xmlui" or "/jspui" on the end
	 * </pre>
	 * @param fullURL
	 * @return
	 */
	private static String getHostURL(final String fullURL) {
		requireNonNull(fullURL);

		// Remove '/' and add "http" if needed
		String url = getCorrectedURL(fullURL);

		// Remove extensions as "/xmlui" or "/jspui"
		if(url.endsWith("/xmlui")) {
			url = url.substring(0, url.length() - 6); //remove "/xmlui"
		} else {
			if(url.endsWith("/jspui")) {
				url = url.substring(0, url.length() - 6); //remove "/jspui"
			}
		}
		return url;
	}

	/**
	 * Create service document url based on the original export repository URL
	 *
	 * @param url {@link String} with original publication repository URL
	 * @return {@link String} string with the URL
	 */
	private static String createServiceDocumentURL(String url) {
		String sdURL = getHostURL(url);
		sdURL += "/swordv2/servicedocument";

		// Explicit replace "https://" with "http://", because of the SWORD-client, which does not support SSL-certificates
		if(sdURL.startsWith("https://")) {
			sdURL = sdURL.replaceFirst("https://", "http://");
		}
		return sdURL;
	}

	private static String createUserSubmissionsURL(String url) {
		String submissionsURL = getCorrectedURL(url);
		submissionsURL += "/submissions";
		return submissionsURL;
	}

	/**
	 * Create the rest-url based on the original export repository url
	 * @param url
	 * @return
	 */
	public static String createRestURL(String url) {
		String restURL = getHostURL(url);
		restURL += "/rest";
		return restURL;
	}

	/**
	 * Wait until worker is finished, make error message visible in case of timeout or exception
	 *
	 * @param worker
	 * @param exceptionMessage
	 */
	public static void executeWorkerWithTimeout(SwingWorker<Boolean, Object> worker, long timeOut, String exceptionMessage) {
		try {
			worker.execute();
			worker.get(timeOut, TimeUnit.SECONDS);
		} catch (TimeoutException | InterruptedException | ExecutionException e) {
			log.error(exceptionMessage + ": " + e.getClass().getSimpleName() + ": " + e.getMessage());
			worker.cancel(true);
		}
	}

//	/**
//	 * Reserved class for the future, if needed.
//	 * <p>
//	 * Timer in background, is used to cancel the SwingWorker instance after the timeout.
//	 * Should be called as "new Thread(new BackgroundTimer<T,V>(worker, timeOut)).start();"
//	 * <p>
//	 * e.g.:
//	 * worker.execute();
//	 * new Thread(new BackgroundTimer<Boolean, Object>(worker, timeOut)).start();
//   *
//	 * @author vk
//	 *
//	 * @param <T> the first parameter of SwingWorker
//	 * @param <V> the second parameter of SwingWorker
//	 */
//	private static class BackgroundTimer<T,V> implements Runnable{
//		private long timeOut;
//		private SwingWorker<T,V> worker;
//
//		private BackgroundTimer(SwingWorker<T,V> worker, long duration) {
//			this.timeOut = duration;
//			this.worker = worker;
//		}
//
//		@Override
//		public void run() {
//			try {
//				Thread.sleep(timeOut * 1000);
//				worker.cancel(true);
//				return;
//			} catch (InterruptedException e) {
//				log.error("Exception in background timer: " + e.getClass().getSimpleName() + ": " + e.getMessage());
//				worker.cancel(true);
//				return;
//			}
//		}
//	}

	/**
	 * Representation of the collection of <String, String> in the ComboBox
	 * @author Volodymyr Kushnarenko
	 */
	private static class CollectionEntry{

		private Map.Entry<String, String> entry;
		protected CollectionEntry(Map.Entry<String, String> entry) {
			this.entry = entry;
		}

		public Map.Entry<String, String> getEntry() {
			return entry;
		}

		@Override
		public String toString() {
			return entry.getValue();
		}
	}

	/**
	 * Abstract class for the wizard page
	 * @author Volodymyr Kushnarenko
	 */
	private static abstract class DSpaceExporterStep extends AbstractWizardStep<DSpaceExporterContext> {
		protected DSpaceExporterStep(String id, String titleKey, String descriptionKey) {
			super(id, titleKey, descriptionKey);
		}
	}

	/**
	 * 1st. page - choose repository and check the user registration
	 */
	private static final DSpaceExporterStep CHOOSE_REPOSITORY = new DSpaceExporterStep(
			"chooseRepository",
			"replaydh.wizard.dspacePublisher.chooseRepository.title",
			"replaydh.wizard.dspacePublisher.chooseRepository.description") {
		//TODO: replace publisher to exporter (see above)
		
		private JTextField tfUrl;
		private JTextField tfUserLogin;
		private JPasswordField pfUserPassword;
		private JButton checkLoginButton;
		private JTextArea statusMessage;
		private JButton openRepositoryButton;

		private String restURL;
		private String serviceDocumentURL;

		private boolean loginOK;
		private boolean restOK;

		private long timeOut; //in seconds

		private Map<String, String> availableCollections;
		private ExportRepository exportRepository;

		/**
		 * Check the connection via REST-interface.
		 * Sets the global flag {@code restOK=true} if connection is working, and {@code restOK=false} otherwise
		 * @param dspaceRepository
		 */
		private void checkAndCorrectRestURL(DSpace_v6 dspaceRepository) {

			SwingWorker<Boolean, Object> worker = new SwingWorker<Boolean, Object>(){

				/**
				 * Replace "http://" via "https://" and otherwise
				 * @param url
				 */
				protected void exchangeHttpHttps(String url) {
					if(url.startsWith("http://")) {
						url = url.replaceFirst("http://", "https://");
					} else {
						if(url.startsWith("https://")) {
							url = url.replaceFirst("https://", "http://");
						}
					}
				}

				@Override
				protected Boolean doInBackground() throws Exception {

					if(restURL == null) {
						return false;
					}
					String correctedRestURL = new String(restURL);
					dspaceRepository.setAllRestURLs(correctedRestURL);

					//Exchange "http://" and "https://" if REST is not accessible
					if(!dspaceRepository.isRestAccessible()) {
						exchangeHttpHttps(correctedRestURL);
						// If the exchange did not help, move to the previous condition
						if(!dspaceRepository.isRestAccessible()) {
							if(restURL == null) {
								return false; //not really needed here
							}
							correctedRestURL = new String(restURL);
							dspaceRepository.setAllRestURLs(correctedRestURL);
							restOK = false;
							restURL = String.valueOf(correctedRestURL.toCharArray());
							return restOK;
						}
					}

					restURL = String.valueOf(correctedRestURL.toCharArray());
					restOK = true;
					return restOK;
				}

				@Override
				protected void done() {
					if(!isCancelled()) {
						if(restOK) {
							//no-op
						} else {
							if(tfUrl.isEnabled() && tfUrl.isEditable()) {
								GuiUtils.toggleChangeableBorder(tfUrl, true); //set red border as a sign of wrong URL
							}
							statusMessage.setText(ResourceManager.getInstance().get("replaydh.wizard.dspacePublisher.chooseRepository.wrongUrlMessage"));
							checkLoginButton.setEnabled(true); //in case of the Internet problem user have to click it again
						}
					} else {
						restOK = false;
						statusMessage.setText(ResourceManager.getInstance().get("replaydh.wizard.dspacePublisher.chooseRepository.terminationMessage"));
						checkLoginButton.setEnabled(true); //in case of the Internet problem user have to click it again
						setNextEnabled(false);
					}
				}
			};
			executeWorkerWithTimeout(worker, timeOut, "Exception by exchanging http/https");
		}


		/**
		 * Check if user is registered, get available collections.
		 * Repository request is realized as a worker in background, not EDT.
		 * Sets the global flag {@code loginOK=true} if login is correct, and {@code loginOK=false} otherwise
		 *
		 * @param exportRepository
		 * @param userLogin
		 */
		private void checkUserRegistrationAndGetCollections(ExportRepository exportRepository) {

			SwingWorker<Boolean, Object> worker = new SwingWorker<Boolean, Object>(){

				@Override
				protected Boolean doInBackground() throws Exception {
					//Thread.sleep(20000); //for test, to imitate a long task and provocate termination on timeout
					loginOK = exportRepository.hasRegisteredCredentials();
					if(exportRepository instanceof DSpaceRepository) {
						availableCollections = ((DSpaceRepository)exportRepository).getAvailableCollectionsWithFullName(" -- ");
					} else {
						availableCollections = exportRepository.getAvailableCollections();
					}
					return loginOK;
				}

				@Override
				protected void done() {
					// Worker was finished properly
					if(!isCancelled()) {
						if(loginOK) {
							checkLoginButton.setEnabled(false);
						} else {
							pfUserPassword.setText("");
							GuiUtils.toggleChangeableBorder(pfUserPassword, true); //set red border as a sign of the wrong password
							checkLoginButton.setEnabled(false);
							statusMessage.setText(ResourceManager.getInstance().get("replaydh.wizard.dspacePublisher.chooseRepository.wrongLoginMessage"));
						}
					}
					// Worker was terminated (timeout or exception)
					else {
						loginOK = false;
						statusMessage.setText(ResourceManager.getInstance().get("replaydh.wizard.dspacePublisher.chooseRepository.terminationMessage"));
						checkLoginButton.setEnabled(true); //in case of the Internet problem user have to click it again
						setNextEnabled(false);
					}
				}
			};
			executeWorkerWithTimeout(worker, timeOut, "Exception by checking the user registrations");

//			//Second option to start a timer in background thread. Self-made approach.
//			//No exceptions, but GUI is working without delays.
//			worker.execute();
//			new Thread(new BackgroundTimer<Boolean, Object>(worker, timeOut)).start();
		}

		@Override
		public Page<DSpaceExporterContext> next(RDHEnvironment environment, DSpaceExporterContext context) {

			// Save context:
			context.repositoryURL = getHostURL(tfUrl.getText());
			context.serviceDocumentURL = createServiceDocumentURL(tfUrl.getText());
			context.restURL = createRestURL(tfUrl.getText());
			context.userSubmissionsURL = createUserSubmissionsURL(tfUrl.getText());
			context.userLogin = tfUserLogin.getText();
			context.availableCollections = availableCollections;
			context.exportRepository = exportRepository;

			return CHOOSE_COLLECTION;
		}

		@Override
		public void refresh(RDHEnvironment environment, DSpaceExporterContext context) {
			super.refresh(environment, context); //call parent "refresh"

			if(environment != null) {
				tfUrl.setText(environment.getProperty(RDHProperty.DSPACE_REPOSITORY_URL));
			}

			if(!loginOK) {
				pfUserPassword.setText(""); //clear password field
				setNextEnabled(false); 		//disable "next" button
			}
		};

		private boolean checkAndUpdateBorder(JTextField tf) {
			boolean isValid = (tf.getText().trim()!=null) && (!tf.getText().trim().isEmpty()); // do not store "getText()" result in extra variable because of the password
			GuiUtils.toggleChangeableBorder(tf, !isValid);
			return isValid;
		}

		@Override
		protected JPanel createPanel() {

			ResourceManager rm = ResourceManager.getInstance();

			loginOK = false;
			restOK = false;
			timeOut = 60; //seconds

			//TODO: -> tfUrl.setEditable(true)
			tfUrl = new JTextField();
			tfUrl.setEditable(false);	//make the URL not editable for test reasons.
										//But wizard is already available to check the URL automatically
										//and provide messages in case of error

			tfUserLogin = new JTextField();
			pfUserPassword = new JPasswordField();

			GuiUtils.prepareChangeableBorder(tfUrl);
			GuiUtils.prepareChangeableBorder(tfUserLogin);
			GuiUtils.prepareChangeableBorder(pfUserPassword);

			statusMessage = GuiUtils.createTextArea(rm.get("replaydh.wizard.dspacePublisher.chooseRepository.pleaseLoginMessage"));

			DocumentAdapter adapter = new DocumentAdapter() {

				@Override
				public void anyUpdate(DocumentEvent e) {

					loginOK = false;
					setNextEnabled(false);
					statusMessage.setText(ResourceManager.getInstance().get("replaydh.wizard.dspacePublisher.chooseRepository.pleaseLoginMessage"));

					boolean loginButtonEnabled = true;
					loginButtonEnabled &= checkAndUpdateBorder(tfUrl);
					loginButtonEnabled &= checkAndUpdateBorder(tfUserLogin);
					loginButtonEnabled &= checkAndUpdateBorder(pfUserPassword);

					checkLoginButton.setEnabled(loginButtonEnabled);
				}
			};

			// Check any update:
			tfUrl.getDocument().addDocumentListener(adapter);
			tfUserLogin.getDocument().addDocumentListener(adapter);
			pfUserPassword.getDocument().addDocumentListener(adapter);


			// Login button
			checkLoginButton = new JButton(rm.get("replaydh.wizard.dspacePublisher.chooseRepository.loginButton"));
			checkLoginButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {

					// Exit if some fields are empty
					if((tfUrl.getText().equals("")) || (tfUserLogin.getText().equals(""))
							|| (pfUserPassword.getPassword().length == 0)){
						return;
					}

					restURL = createRestURL(tfUrl.getText());
					serviceDocumentURL = createServiceDocumentURL(tfUrl.getText());

					exportRepository = new DSpace_v6(serviceDocumentURL,
															restURL,
															tfUserLogin.getText(),
															pfUserPassword.getPassword()
															);
					// Prepare GUI for the repository requests
					checkLoginButton.setEnabled(false);
					statusMessage.setText(ResourceManager.getInstance().get("replaydh.wizard.dspacePublisher.chooseRepository.waitMessage"));
					restOK = false;
					loginOK = false;

					// Start repository requests
					SwingUtilities.invokeLater(new Runnable() {
				        @Override
						public void run() {
				        	checkAndCorrectRestURL((DSpace_v6)exportRepository);
				        	if(restOK) {
				        		checkUserRegistrationAndGetCollections(exportRepository);
				        		if(loginOK) {
				        			statusMessage.setText(ResourceManager.getInstance().get("replaydh.wizard.dspacePublisher.chooseRepository.successMessage"));
									setNextEnabled(true);
								} else {
									setNextEnabled(false);
					        	}
				        	}
				        }
				    });

				}
			});

			openRepositoryButton = new JButton(rm.get("replaydh.wizard.dspacePublisher.chooseRepository.loginInfoButton"));
			openRepositoryButton.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {

					String urlString = tfUrl.getText();
					try {
				        Desktop.getDesktop().browse(new URL(urlString).toURI());
				    } catch (Exception ex) {
				    	log.error("Exception by openning the browser: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
				    }
				}
			});

			return FormBuilder.create()
					.columns("pref, 6dlu, fill:pref:grow")
					.rows("pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref")
					.padding(Paddings.DLU4)
					.add(new JLabel(rm.get("replaydh.wizard.dspacePublisher.chooseRepository.urlLabel"))).xy(1, 1)
					.add(tfUrl).xy(3, 1)
					.add(new JLabel(rm.get("replaydh.wizard.dspacePublisher.chooseRepository.loginLabel"))).xy(1, 3)
					.add(tfUserLogin).xy(3, 3)
					.add(new JLabel(rm.get("replaydh.wizard.dspacePublisher.chooseRepository.passwordLabel"))).xy(1, 5)
					.add(pfUserPassword).xy(3, 5)
					.add(checkLoginButton).xy(3, 7)
					.add(openRepositoryButton).xy(3, 9)
					.add(statusMessage).xyw(1, 11, 3)
					.build();
		}

	};



	/**
	 * 2nd. page - choose collection, where to publish
	 */
	private static final DSpaceExporterStep CHOOSE_COLLECTION = new DSpaceExporterStep(
			"chooseCollection",
			"replaydh.wizard.dspacePublisher.chooseCollection.title",
			"replaydh.wizard.dspacePublisher.chooseCollection.description") {

		private JComboBox<CollectionEntry> collectionsComboBox;
		private JTextArea noAvailableCollectionsMessage;

		@Override
		public void refresh(RDHEnvironment environment, DSpaceExporterContext context) {
			super.refresh(environment, context); //call parent "refresh"

			// Update combobox with collections
			collectionsComboBox.removeAllItems();
			for(Map.Entry<String, String> entry: context.getAvailableCollections().entrySet()) {
				collectionsComboBox.addItem(new CollectionEntry(entry));
			}

			// Display the error message if there are no collections available
			noAvailableCollectionsMessage.setVisible(context.getAvailableCollections().isEmpty());

			// Remove selection and disable "next" button
			collectionsComboBox.setSelectedIndex(-1);
			setNextEnabled(false);
		};

		@Override
		public Page<DSpaceExporterContext> next(RDHEnvironment environment, DSpaceExporterContext context) {
			// Store collection url
			context.collectionURL = ((CollectionEntry)collectionsComboBox.getSelectedItem()).getEntry().getKey();

			return CHOOSE_FILES;
		}

		@Override
		protected JPanel createPanel() {

			collectionsComboBox = new JComboBox<CollectionEntry>();
			collectionsComboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setNextEnabled(true);
				}
			});

			noAvailableCollectionsMessage = GuiUtils.createTextArea(ResourceManager.getInstance()
					.get("replaydh.wizard.dspacePublisher.chooseCollection.noCollectionsMessage"));

			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("pref, $nlg, pref, $nlg, pref")
					.padding(Paddings.DLU4)
					.add(new JLabel(ResourceManager.getInstance().get("replaydh.wizard.dspacePublisher.chooseCollection.collectionLabel"))).xy(1, 1)
					.add(collectionsComboBox).xy(1, 3)
					.add(noAvailableCollectionsMessage).xy(1, 5)
					.build();
		}
	};


	/**
	 * 3rd. page - choose files for publishing
	 */
	private static final DSpaceExporterStep CHOOSE_FILES = new DSpaceExporterStep(
			"chooseFiles",
			"replaydh.wizard.dspacePublisher.chooseFiles.title",
			"replaydh.wizard.dspacePublisher.chooseFiles.description") {

		private JTextArea messageArea;
		private DefaultTableModel tableModel;
		private JTable filesTable;
		private JButton addFilesButton;
		private JButton removeFilesButton;
		private RDHEnvironment localEnvironment;
		private File workspaceFile;

		/**
		 * addFilesButton was pushed
		 */
		private void addFilesButtonPressed(ActionEvent ae) {

			ResourceManager rm = ResourceManager.getInstance();

			// Disable remove/create options (e.g. "create new folder")
			boolean filechooserReadOnlyOrig = UIManager.getBoolean("FileChooser.readOnly"); //store value
			UIManager.put("FileChooser.readOnly", Boolean.TRUE);

			FileSystemView fsv = new DirectoryRestrictedFileSystemView(workspaceFile); //custom FileSystemView
			JFileChooser fileChooser = new JFileChooser(fsv);
			fileChooser.setCurrentDirectory(workspaceFile);
			fileChooser.setFileHidingEnabled(true);
			fileChooser.setApproveButtonText(rm.get("replaydh.labels.select"));
			fileChooser.setDialogTitle(rm.get("replaydh.wizard.workflowExport.selectOutput.fileChooserTitle"));
			fileChooser.setAcceptAllFileFilterUsed(false);
			fileChooser.setMultiSelectionEnabled(true);
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY); //TODO: think also about directories

			if(fileChooser.showDialog(getPageComponent(), null)==JFileChooser.APPROVE_OPTION) {
				File[] files = fileChooser.getSelectedFiles();
				for(File file: files) {
					if(!isObjectInTable(file, filesTable)) {
						Object[] obj = new Object[1];
						obj[0] = file;
						tableModel.addRow(obj);
					}
				}
			}

			// Restore default value of remove/create options
			UIManager.put("FileChooser.readOnly", filechooserReadOnlyOrig);
		}

		/**
		 * removeFilesButton button was pushed
		 */
		private void removeFilesButtonPressed(ActionEvent ae) {
			DefaultTableModel model = (DefaultTableModel)filesTable.getModel();
			int selectedRowsCount = filesTable.getSelectedRowCount();

			for(int i=0; i<selectedRowsCount; i++) {
				int[] rows = filesTable.getSelectedRows(); //update the list every time after removing to avoid an exception by multiple selection
				if(rows.length != 0) {
					model.removeRow(rows[0]);
				}
			}
		}

		/**
		 * Check if the object already exists inside the table
		 */
		private boolean isObjectInTable(Object obj, JTable table) {

			TableModel model = table.getModel();
			int columnCount = model.getColumnCount();
			int rowCount = model.getRowCount();

			for(int row=0; row<rowCount; row++) {
				for(int column=0; column<columnCount; column++) {
					if(model.getValueAt(row, column).equals(obj)) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public void refresh(RDHEnvironment environment, DSpaceExporterContext context) {
			super.refresh(environment, context); //call parent "refresh"

			localEnvironment = environment;
			workspaceFile = localEnvironment.getWorkspacePath().toFile();

			if(filesTable != null) {
				filesTable.clearSelection(); //clear selection for case, when wizard is reused again

				// IMPORTANT: save workspace path to the table name to use it in the custom cellRenderer (@see PathCellRenderer)
				filesTable.setName(localEnvironment.getWorkspacePath().toString());

				//TODO: remove the files from the table -> now the previous file list is always here
			}
		};

		@Override
		public Page<DSpaceExporterContext> next(RDHEnvironment environment, DSpaceExporterContext context) {

			// Add files to the context
			context.filesToPublish = new ArrayList<File>();
			for(int column=0; column<tableModel.getColumnCount(); column++) {
				for(int row=0; row<tableModel.getRowCount(); row++) {
					context.filesToPublish.add((File)tableModel.getValueAt(row, column));
				}
			}

			return EDIT_METADATA;
		}

		@Override
		protected JPanel createPanel() {

			ResourceManager rm = ResourceManager.getInstance();

			tableModel = new DefaultTableModel();
			tableModel.addColumn(""); //without a column name

			filesTable = new JTable(tableModel);
			filesTable.setPreferredScrollableViewportSize(new Dimension(300,150));
			filesTable.setFillsViewportHeight(true);
			filesTable.setTableHeader(null); //remove the table header
			filesTable.getColumnModel().getColumn(0).setCellRenderer(new PathCellRenderer()); //cellRenderer to display only the name of file
			filesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() { //analyze row selection
				@Override
				public void valueChanged(ListSelectionEvent e) {
					if(filesTable.getSelectedRowCount() != 0) {
						removeFilesButton.setEnabled(true);	//enable the remove button in case of selection
					} else {
						removeFilesButton.setEnabled(false); //disable remove button in case of no selection
					}
				}
			});

			addFilesButton = new JButton(rm.get("replaydh.wizard.dspacePublisher.chooseFiles.addFilesButton"));
			addFilesButton.addActionListener(this::addFilesButtonPressed);

			removeFilesButton = new JButton(rm.get("replaydh.wizard.dspacePublisher.chooseFiles.removeFilesButton"));
			removeFilesButton.addActionListener(this::removeFilesButtonPressed);
			removeFilesButton.setEnabled(false);

			messageArea = GuiUtils.createTextArea(rm.get("replaydh.wizard.dspacePublisher.chooseFiles.infoText"));

			JPanel buttonPanel = FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("top:pref, $nlg, top:pref")
					.add(addFilesButton).xy(1, 1)
					.add(removeFilesButton).xy(1, 3)
					.build();

			return FormBuilder.create()
					.columns("fill:pref:grow, 6dlu, pref")
					.rows("pref, $nlg, top:pref, $nlg, pref")
					.padding(Paddings.DLU4)
					.add(new JLabel(rm.get("replaydh.wizard.dspacePublisher.chooseFiles.filesLabel"))).xy(1, 1)
					.add(filesTable).xy(1, 3)
					.add(buttonPanel).xy(3, 3)
					.add(messageArea).xyw(1, 5, 3)
					.build();
		}
	};


	/**
	 * 4th. page - edit metadata
	 */
	private static final DSpaceExporterStep EDIT_METADATA = new DSpaceExporterStep(
			"editMetadata",
			"replaydh.wizard.dspacePublisher.editMetadata.title",
			"replaydh.wizard.dspacePublisher.editMetadata.description") {

		private JTextField tfCreator;
		private JTextField tfTitle;
		private JTextField tfDescription;
		private JFormattedTextField tfPublicationYear;

		//Not used metadata fields
		//private JTextField tfIdentifier;
		//private JTextField tfPublisher;
		//private JTextField tfResourceType;

		private JTextArea messageArea;

		private DateFormat format;

		@Override
		public void refresh(RDHEnvironment environment, DSpaceExporterContext context) {
			super.refresh(environment, context); //call parent "refresh"

			//TODO: use it to fill in the text fields with not null values. Should be used later, when we use some metadata-schema
			MetadataObject mdObject = context.metadataObject;

			// Creator
			String creator = null;
			if(creator==null) { 	//TODO fetch user defined value if mdObject is not null (see todo above)
				creator = environment.getProperty(RDHProperty.CLIENT_USERNAME);
			}
			tfCreator.setText(creator);

			//TODO: should we use workflow title or workflow-step title is also possible? Because we publish files from the current workflow-step

			// Title
			tfTitle.setText(context.exportInfo.getWorkflow().getTitle());

			// Description
			tfDescription.setText(context.exportInfo.getWorkflow().getDescription());

			// Publication year
			int year = Calendar.getInstance().get(Calendar.YEAR);
			tfPublicationYear.setText(String.valueOf(year));

			refreshNextEnabled();

			//TODO: remove previous metadata when the page is opened again. Now previous metadata is kept. Se todo with mdObject above

		};

		@Override
		public Page<DSpaceExporterContext> next(RDHEnvironment environment, DSpaceExporterContext context) {

			ResourceManager rm = ResourceManager.getInstance();

			// Store metadata
			context.metadataObject = new MetadataObject();
			context.metadataObject.mapDoublinCoreToMetadata = new HashMap<>();
			context.metadataObject.mapDoublinCoreToLabel = new HashMap<>();

			// Title
			context.metadataObject.mapDoublinCoreToMetadata.put("title", Arrays.asList(tfTitle.getText()));
			context.metadataObject.mapDoublinCoreToLabel.put("title", rm.get("replaydh.wizard.dspacePublisher.editMetadata.titleLabel"));

			// Description
			context.metadataObject.mapDoublinCoreToMetadata.put("description", Arrays.asList(tfDescription.getText()));
			context.metadataObject.mapDoublinCoreToLabel.put("description", rm.get("replaydh.wizard.dspacePublisher.editMetadata.descriptionLabel"));

			// Creator
			context.metadataObject.mapDoublinCoreToMetadata.put("creator", Arrays.asList(tfCreator.getText()));
			context.metadataObject.mapDoublinCoreToLabel.put("creator", rm.get("replaydh.wizard.dspacePublisher.editMetadata.creatorLabel"));

			// Publication year
			context.metadataObject.mapDoublinCoreToMetadata.put("issued", Arrays.asList(tfPublicationYear.getText()));
			context.metadataObject.mapDoublinCoreToLabel.put("issued", rm.get("replaydh.wizard.dspacePublisher.editMetadata.publicationYearLabel"));

//			// Not used (reserved) metadata fields
//			context.metadataObject.mapDoublinCoreToMetadata.put("identifier", tfIdentifier.getText());
//			context.metadataObject.mapDoublinCoreToLabel.put("identifier", rm.get("replaydh.wizard.dspacePublisher.editMetadata.identifierLabel"));
//			context.metadataObject.mapDoublinCoreToMetadata.put("publisher", tfPublisher.getText());
//			context.metadataObject.mapDoublinCoreToLabel.put("publisher", rm.get("replaydh.wizard.dspacePublisher.editMetadata.publisherLabel"));
//			context.metadataObject.mapDoublinCoreToMetadata.put("type", tfResourceType.getText());
//			context.metadataObject.mapDoublinCoreToLabel.put("type", rm.get("replaydh.wizard.dspacePublisher.editMetadata.resourceTypeLabel"));

			return FINISH;
		}

		private void refreshNextEnabled() {
			boolean nextEnabled = true;

			nextEnabled &= checkAndUpdateBorder(tfCreator);
			nextEnabled &= checkAndUpdateBorder(tfTitle);
			nextEnabled &= checkAndUpdateBorder(tfDescription);
			nextEnabled &= checkAndUpdateBorder(tfPublicationYear);

			// Not used metadata fields
			//nextEnabled &= checkAndUpdateBorder(tfIdentifier);
			//nextEnabled &= checkAndUpdateBorder(tfPublisher);
			//nextEnabled &= checkAndUpdateBorder(tfResourceType);

			setNextEnabled(nextEnabled);
		}

		private boolean checkAndUpdateBorder(JTextField tf) {
			String text = tf.getText().trim();
			boolean isValid = text!=null && !text.isEmpty();
			GuiUtils.toggleChangeableBorder(tf, !isValid);
			return isValid;
		}

		@Override
		protected JPanel createPanel() {

			ResourceManager rm = ResourceManager.getInstance();

			tfCreator = new JTextField();
			tfTitle = new JTextField();
			tfDescription = new JTextField();
			format = new SimpleDateFormat("YYYY");
			tfPublicationYear = new JFormattedTextField(format);
			tfPublicationYear.setToolTipText("YYYY");

			GuiUtils.prepareChangeableBorder(tfCreator);
			GuiUtils.prepareChangeableBorder(tfTitle);
			GuiUtils.prepareChangeableBorder(tfDescription);
			GuiUtils.prepareChangeableBorder(tfPublicationYear);

			// Not used metadata fields
			//tfIdentifier = new JTextField();
			//tfPublisher = new JTextField();
			//tfResourceType = new JTextField();
			//
			//GuiUtils.prepareChangeableBorder(tfIdentifier);
			//GuiUtils.prepareChangeableBorder(tfPublisher);
			//GuiUtils.prepareChangeableBorder(tfResourceType);


			messageArea = GuiUtils.createTextArea(rm.get("replaydh.wizard.dspacePublisher.editMetadata.infoMessage"));


			final DocumentAdapter adapter = new DocumentAdapter() {
				@Override
				public void anyUpdate(DocumentEvent e) {
					refreshNextEnabled();
				}
			};

			tfCreator.getDocument().addDocumentListener(adapter);
			tfTitle.getDocument().addDocumentListener(adapter);
			tfDescription.getDocument().addDocumentListener(adapter);
			tfPublicationYear.getDocument().addDocumentListener(adapter);

			// Not used metadata fields
			//tfIdentifier.getDocument().addDocumentListener(adapter);
			//tfPublisher.getDocument().addDocumentListener(adapter);
			//tfResourceType.getDocument().addDocumentListener(adapter);

			return FormBuilder.create()
					.columns("pref, 6dlu, fill:pref:grow")
					.rows("pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref")
					.padding(Paddings.DLU4)
					.add(new JLabel(rm.get("replaydh.wizard.dspacePublisher.editMetadata.titleLabel"))).xy(1, 1)
					.add(tfTitle).xy(3, 1)
					.add(new JLabel(rm.get("replaydh.wizard.dspacePublisher.editMetadata.descriptionLabel"))).xy(1, 3)
					.add(tfDescription).xy(3, 3)
					.add(new JLabel(rm.get("replaydh.wizard.dspacePublisher.editMetadata.creatorLabel"))).xy(1, 5)
					.add(tfCreator).xy(3, 5)
					.add(new JLabel(rm.get("replaydh.wizard.dspacePublisher.editMetadata.publicationYearLabel"))).xy(1, 7)
					.add(tfPublicationYear).xy(3, 7)
//					.add(new JLabel(rm.get("replaydh.wizard.dspacePublisher.editMetadata.resourceTypeLabel"))).xy(1, 9)
//					.add(tfResourceType).xy(3, 9)
//					.add(new JLabel(rm.get("replaydh.wizard.dspacePublisher.editMetadata.identifierLabel"))).xy(1, 11)
//					.add(tfIdentifier).xy(3, 11)
//					.add(new JLabel(rm.get("replaydh.wizard.dspacePublisher.editMetadata.publisherLabel"))).xy(1, 13)
//					.add(tfPublisher).xy(3, 13)
					.add(messageArea).xyw(1, 9, 3)
					.build();
		}
	};




	/**
	 * Last page - entry point for the publication
	 */
	private static final DSpaceExporterStep FINISH = new DSpaceExporterStep(
			"finish",
			"replaydh.wizard.dspacePublisher.finish.title",
			"replaydh.wizard.dspacePublisher.finish.description") {

		private JTextArea repositoryUrlArea;
		private JTextArea userLoginArea;
		private JTextArea choosenCollectionArea;
		private JTextArea choosenFilesArea;
		private JTextArea metadataArea;
		private JTextArea messageArea;

		@Override
		public void refresh(RDHEnvironment environment, DSpaceExporterContext context) {
			super.refresh(environment, context); //call parent "refresh"

			// Repository URL
			if(context.getRepositoryURL() != null) {
				repositoryUrlArea.setText(context.getRepositoryURL());
			}

			// User login
			if(context.getUserLogin() != null) {
				userLoginArea.setText(context.getUserLogin());
			}

			// Chosen collection
			if((context.getAvailableCollections() != null) && (context.getCollectionURL() != null)) {
				choosenCollectionArea.setText(context.getAvailableCollections().get(context.getCollectionURL()));
			}

			// Chosen files
			String strFiles = "";
			if(context.getFilesToPublish() != null) {
				for(File file: context.getFilesToPublish()) {
					strFiles += file.getPath().substring(environment.getWorkspacePath().toString().length() + 1);
					strFiles += "\n";
				}
			}
			if(!strFiles.equals("")) {
				strFiles = strFiles.substring(0, strFiles.length() - 1); //remove last "\n"
			} else {
				strFiles = ResourceManager.getInstance().get("replaydh.wizard.dspacePublisher.finish.noDataMessage");
			}
			choosenFilesArea.setText(strFiles);

			// Metadata
			String strMetadata = "";
			if(context.getMetadataObject() != null) {
				for(Map.Entry<String, List<String>> entry: context.getMetadataObject().getMapLabelToMetadata().entrySet()) {
					strMetadata += entry.getKey();
					for(String metadataEntry: entry.getValue()) {
						strMetadata += ": " + metadataEntry;
					}
					strMetadata += "\n";
				}
			}
			if(!strMetadata.equals("")) {
				strMetadata = strMetadata.substring(0, strMetadata.length() - 1); //remove last "\n"
			} else {
				strMetadata = ResourceManager.getInstance().get("replaydh.wizard.dspacePublisher.finish.noDataMessage");
			}
			metadataArea.setText(strMetadata);

			// Info message
			messageArea.setText(ResourceManager.getInstance().get("replaydh.wizard.dspacePublisher.finish.infoMessage"));
		};

		@Override
		public Page<DSpaceExporterContext> next(RDHEnvironment environment, DSpaceExporterContext context) {
			return null;
		}

		@Override
		protected JPanel createPanel() {

			ResourceManager rm = ResourceManager.getInstance();

			repositoryUrlArea = GuiUtils.createTextArea("");
			userLoginArea = GuiUtils.createTextArea("");
			choosenCollectionArea = GuiUtils.createTextArea("");
			choosenFilesArea = GuiUtils.createTextArea("");
			metadataArea = GuiUtils.createTextArea("");
			messageArea = GuiUtils.createTextArea("");

			//TODO: add separators
			return FormBuilder.create()
					.columns("pref, 6dlu, fill:pref:grow")
					.rows("top:pref, $nlg, top:pref, $nlg, top:pref, $nlg, top:pref, $nlg, top:pref, $nlg, top:pref")
					.padding(Paddings.DLU4)
					.add(new JLabel(rm.get("replaydh.wizard.dspacePublisher.chooseRepository.urlLabel"))).xy(1, 1)
					.add(repositoryUrlArea).xy(3, 1)
					.add(new JLabel(rm.get("replaydh.wizard.dspacePublisher.chooseRepository.loginLabel"))).xy(1, 3)
					.add(userLoginArea).xy(3, 3)
					.add(new JLabel(rm.get("replaydh.wizard.dspacePublisher.chooseCollection.collectionLabel"))).xy(1, 5)
					.add(choosenCollectionArea).xy(3, 5)
					.add(new JLabel(rm.get("replaydh.wizard.dspacePublisher.chooseFiles.filesLabel"))).xy(1, 7)
					.add(choosenFilesArea).xy(3, 7)
					.add(new JLabel(rm.get("replaydh.wizard.dspacePublisher.finish.metadataLabel"))).xy(1, 9)
					.add(metadataArea).xy(3, 9)
					.add(messageArea).xyw(1, 11, 3)
					.build();
		}
	};


	/**
	 * Custom implementation of FileSystemView to disable changing the working directory inside the FileChooser.
	 * <p>
	 * An idea comes from <{@link https://stackoverflow.com/questions/32529/how-do-i-restrict-jfilechooser-to-a-directory}
	 * @author Volodymyr Kushnarenko
	 */
	private static class DirectoryRestrictedFileSystemView extends FileSystemView
	{
	    private final File[] rootDirectories;

	    DirectoryRestrictedFileSystemView(File rootDirectory) {
	        this.rootDirectories = new File[] {rootDirectory};
	    }

	    DirectoryRestrictedFileSystemView(File[] rootDirectories) {
	        this.rootDirectories = rootDirectories;
	    }

	    @Override
		public File createNewFolder(File containingDir) throws IOException {
			return null;
		}

	    @Override
	    public File[] getRoots() {
	        return rootDirectories;
	    }

	    @Override
	    public boolean isRoot(File file) {
	        for (File root : rootDirectories) {
	            if (root.equals(file)) {
	                return true;
	            }
	        }
	        return false;
	    }

	    @Override
	    public File getHomeDirectory() {
	    	return rootDirectories[0];
	    }
	}

	/**
	 * <pre>
	 * Custom cell renderer to show only the file name without the workspace directory.
	 * e.g.:
	 * file.txt
	 * folder/file.txt
	 * folder/folder/file.txt
	 *
	 * IMPORTANT: workspace directory MUST be set in the table name!
	 * </pre>
	 *
	 * @author Volodymyr Kushnarenko
	 */
	private static class PathCellRenderer extends DefaultTableCellRenderer {

		@Override
		public Component getTableCellRendererComponent(
                JTable table, Object value,
                boolean isSelected, boolean hasFocus,
                int row, int column) {

	    	JLabel label = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	    	String fileName = ((File)value).getPath();

	    	// IMPORTANT: "table name" MUST be used as a workspace directory path
	    	String workspacePath = table.getName();
	    	if(workspacePath != null) {
	    		// Remove the workspace directory path to show only the file (with possible parent folders)
	    		//fileName = fileName.substring(workspacePath.length() + 1); // "+1" to remove the separator ('/')
	    		Path pathAbsolute = Paths.get(fileName);
	    		Path pathBase = Paths.get(workspacePath);
	    		fileName = pathBase.relativize(pathAbsolute).toString();
	    	}

	    	label.setText(fileName);
	    	label.setToolTipText(((File)value).getPath());

	    	return label;
		}
	}

}



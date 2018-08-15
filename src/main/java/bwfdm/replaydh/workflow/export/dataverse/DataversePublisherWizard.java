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
package bwfdm.replaydh.workflow.export.dataverse;

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
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
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

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;
import com.jgoodies.forms.layout.FormLayout;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.AbstractWizardStep;
import bwfdm.replaydh.ui.helper.DocumentAdapter;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.helper.Wizard.Page;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo;

/**
 * @author Volodymyr Kushnarenko
 * @author Florian Fritze
 *
 */
public class DataversePublisherWizard {

	private static final Logger log = LoggerFactory.getLogger(DataversePublisherWizard.class);

	public static Wizard<DataversePublisherContext> getWizard(Window parent, RDHEnvironment environment) {
		@SuppressWarnings("unchecked")
		Wizard<DataversePublisherContext> wizard = new Wizard<>(
				parent, ResourceManager.getInstance().get("replaydh.wizard.dataversePublisher.title"),
				environment, CHOOSE_REPOSITORY, CHOOSE_COLLECTION, CHOOSE_DATASET, CHOOSE_FILES, EDIT_METADATA, FINISH);
		return wizard;
	}

	/**
	 * Context for the wizard
	 */
	public static final class DataversePublisherContext{

		final WorkflowExportInfo exportInfo;

		private RDHEnvironment environment;
		private String repositoryURL;		
		
		private String serviceDocumentURL;
		private String collectionURL;
		private Map<String, String> availableCollections;
		
		private Map<String, String> availableDatasetsInCollection;

		private List<File> filesToPublish;
		private DataverseRepository_v4 publicationRepository;
		private MetadataObject metadataObject;
		
		private boolean exportProcessMetadataAllowed;
		
		private boolean replaceMetadataAllowed;
		
		private String chosenDataset;
		private String jsonObjectWithMetadata;
		
		public boolean isReplaceMetadataAllowed() {
			return replaceMetadataAllowed;
		}

		public void setReplaceMetadataAllowed(boolean replaceMetadataAllowed) {
			this.replaceMetadataAllowed = replaceMetadataAllowed;
		}

		public String getJsonObjectWithMetadata() {
			return jsonObjectWithMetadata;
		}

		public String getChosenDataset() {
			return chosenDataset;
		}

		public boolean isExportProcessMetadataAllowed() {
			return exportProcessMetadataAllowed;
		}

		public void setExportProcessMetadataAllowed(boolean exportProcessMetadata) {
			this.exportProcessMetadataAllowed = exportProcessMetadata;
		}

		public DataversePublisherContext(WorkflowExportInfo exportInfo) {
			this.exportInfo = requireNonNull(exportInfo);
		}

		//TODO: add metadata. As an MetadataObject? Use MetadataEditor @see MetadataUIEditor

		public String getRepositoryURL() {
			return repositoryURL;
		}
		public String getCollectionURL() {
			return collectionURL;
		}
		public DataverseRepository getPublicationRepository() {
			return publicationRepository;
		}
		public Map<String, String> getAvailableCollections() {
			return availableCollections;
		}
		public void setAvailableCollections(Map<String, String> availableCollections) {
			this.availableCollections = availableCollections;
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
		public String getServiceDocumentURL() {
			return serviceDocumentURL;
		}
	}

	public static final class MetadataObject{

		Map<String, List<String>> mapDublinCoreToMetadata;
		Map<String, String> mapDublinCoreToLabel;

		/**
		 * Get map with key=doublin.core, value=metadata.
		 * <p>
		 * Should be used for the publication to the repository.
		 */
		public Map<String, List<String>> getMapDoublinCoreToMetadata(){
			if(mapDublinCoreToMetadata != null){
				return mapDublinCoreToMetadata;
			} else {
				return new HashMap<>();
			}
		}

		/**
		 * Get map with key=label, value=metadata.
		 * <p>
		 * Should be used ONLY for the representation of the metadata.
		 */
		public Map<String, List<String>> getMapLabelToMetadata(){

			Map<String, List<String>> metadataMap = new HashMap<>();
			if((mapDublinCoreToMetadata != null) && (mapDublinCoreToLabel != null)) {
				for(Map.Entry<String, List<String>> entryDoublinCoreToMetadata: mapDublinCoreToMetadata.entrySet()) {
					for(Map.Entry<String, String> entryDoublinCoreToLabel: mapDublinCoreToLabel.entrySet()) {
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
		String url = new String(fullURL.trim());

		// Remove last '/'
		if(url.endsWith("/")) {
			url = url.substring(0, url.length() - 1);
		}

		// Add "http://" if not existed
		//TODO: should we use "https" instead?? Replace after the detailed testing with different repositories.
		if((!url.startsWith("http://")) && (!url.startsWith("https://"))) {
			url = "https://" + url;
		}

		return url;
	}

	/**
	 * Create the service document url based on the original publication repository url
	 *
	 * @param url
	 * @return
	 */
	private static String createServiceDocumentURL(String url) {
		url = getCorrectedURL(url);
		url += "/dvn/api/data-deposit/v1.1/swordv2/service-document";

		return url;
	}

	private static String createMetadataUrl(String url, String doi) {
		url = getCorrectedURL(url);
		url += "/api/datasets/:persistentId?persistentId="+doi;

		return url;
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
	 * Abstract class for the wizard page
	 * @author Volodymyr Kushnarenko
	 * @author Florian Fritze
	 */
	private static abstract class DataversePublisherStep extends AbstractWizardStep<DataversePublisherContext> implements ActionListener {
		protected DataversePublisherStep(String titleKey, String descriptionKey) {
			super(titleKey, descriptionKey);
		}
	}

	/**
	 * 1st. page - choose repository and check the user registration
	 */
	private static final DataversePublisherStep CHOOSE_REPOSITORY = new DataversePublisherStep(
			"replaydh.wizard.dataversePublisher.chooseRepository.title",
			"replaydh.wizard.dataversePublisher.chooseRepository.description") {

		private JTextField tfUrl;
		private JPasswordField pAPIkey;
		private JButton checkLoginButton;
		private JTextArea statusMessage;
		private JButton openRepositoryButton;
		private JButton resetButton;

		private String serviceDocumentURL;
		private boolean loginOK;

		private boolean swordOK;
		private boolean collectionsAvailable;

		private long timeOut = 2; //in seconds

		private Map<String, String> availableCollections;
		private DataverseRepository_v4 publicationRepository;

		/**
		 * Check the connection via REST-interface.
		 * Sets the global flag {@code restOK=true} if connection is working, and {@code restOK=false} otherwise
		 * @param publicationRepository
		 */
		private void checkSWORDURL() {

			SwingWorker<Boolean, Object> worker = new SwingWorker<Boolean, Object>(){

				@Override
				protected Boolean doInBackground() throws Exception {

					if(serviceDocumentURL == null) {
						return false;
					}

					//Exchange "http://" and "https://" if REST is not accessible
					if(!publicationRepository.isSwordAccessible()) {
						// If the exchange did not help, move to the previous condition
						swordOK = false;
						return swordOK;
					} else if (publicationRepository.getUserAvailableCollectionsWithTitle() != null) {
						availableCollections=publicationRepository.getUserAvailableCollectionsWithTitle();
						collectionsAvailable = true;
					}

					swordOK = true;
					return swordOK;
				}

				@Override
				protected void done() {
					if(!isCancelled()) {
						if(swordOK) {
							//no-op
						} else {
							if(tfUrl.isEnabled() && tfUrl.isEditable()) {
								GuiUtils.toggleChangeableBorder(tfUrl, true); //set red border as a sign of wrong URL
							}
							statusMessage.setText(ResourceManager.getInstance().get("replaydh.wizard.dataversePublisher.chooseRepository.wrongUrlMessage"));
							checkLoginButton.setEnabled(true); //in case of the Internet problem user have to click it again
						}
					} else {
						swordOK = false;
						statusMessage.setText(ResourceManager.getInstance().get("replaydh.wizard.dataversePublisher.chooseRepository.terminationMessage"));
						checkLoginButton.setEnabled(true); //in case of the Internet problem user have to click it again
						setNextEnabled(false);
					}
				}
			};
			executeWorkerWithTimeout(worker, timeOut, "Exception by exchanging http/https");
		}


		@Override
		public Page<DataversePublisherContext> next(RDHEnvironment environment, DataversePublisherContext context) {

			// Save context:
			context.serviceDocumentURL = createServiceDocumentURL(tfUrl.getText());
			context.availableCollections = availableCollections;
			context.publicationRepository = publicationRepository;
			context.repositoryURL = getCorrectedURL(tfUrl.getText());
			environment.setProperty(RDHProperty.DATAVERSE_REPOSITORY_URL, context.repositoryURL);

			return CHOOSE_COLLECTION;
		}

		@Override
		public void refresh(RDHEnvironment environment, DataversePublisherContext context) {
			super.refresh(environment, context); //call parent "refresh"

			if(environment != null) {
				tfUrl.setText(environment.getProperty(RDHProperty.DATAVERSE_REPOSITORY_URL));
			}

			if(!loginOK) {
				pAPIkey.setText(""); //clear password field
				setNextEnabled(false); 		//disable "next" button
			}
			SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	                pAPIkey.requestFocusInWindow();
	            }
	        });
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

			//TODO: -> tfUrl.setEditable(true)
			tfUrl = new JTextField();
			tfUrl.setEditable(true);	//make the URL not editable for test reasons.
										//But wizard is already available to check the URL automatically
										//and provide messages in case of error

			pAPIkey = new JPasswordField();

			GuiUtils.prepareChangeableBorder(tfUrl);
			GuiUtils.prepareChangeableBorder(pAPIkey);

			GuiUtils.toggleChangeableBorder(tfUrl, true);
			GuiUtils.toggleChangeableBorder(pAPIkey, true);

			statusMessage = GuiUtils.createTextArea(rm.get("replaydh.wizard.dataversePublisher.chooseRepository.pleaseLoginMessage"));

			DocumentAdapter adapter = new DocumentAdapter() {

				@Override
				public void anyUpdate(DocumentEvent e) {

					loginOK = false;
					setNextEnabled(false);
					statusMessage.setText(ResourceManager.getInstance().get("replaydh.wizard.dataversePublisher.chooseRepository.pleaseLoginMessage"));

					boolean loginButtonEnabled = true;
					loginButtonEnabled &= checkAndUpdateBorder(tfUrl);
					loginButtonEnabled &= checkAndUpdateBorder(pAPIkey);

					checkLoginButton.setEnabled(loginButtonEnabled);
				}
			};

			// Check any update:
			tfUrl.getDocument().addDocumentListener(adapter);
			pAPIkey.getDocument().addDocumentListener(adapter);
			
			// Reset Button
			resetButton = new JButton(rm.get("replaydh.wizard.dataversePublisher.chooseRepository.ResetButton"));
			resetButton.addActionListener(this);

			// Login button
			checkLoginButton = new JButton(rm.get("replaydh.wizard.dataversePublisher.chooseRepository.loginButton"));
			checkLoginButton.setEnabled(false);
			checkLoginButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {

					// Exit if some fields are empty
					if((tfUrl.getText().equals("")) || (pAPIkey.getPassword().length == 0)){
						checkLoginButton.setEnabled(false);
						return;
					}

					serviceDocumentURL = createServiceDocumentURL(tfUrl.getText());
					publicationRepository = new DataverseRepository_v4(serviceDocumentURL, pAPIkey.getPassword());
					// Prepare GUI for the repository requests
					statusMessage.setText(ResourceManager.getInstance().get("replaydh.wizard.dataversePublisher.chooseRepository.waitMessage"));
					loginOK = false;

					// Start repository requests
					SwingUtilities.invokeLater(new Runnable() {
				        @Override
						public void run() {
				        	checkSWORDURL();
				        	if (swordOK) {
				        		if(collectionsAvailable) {
				        			statusMessage.setText(ResourceManager.getInstance().get("replaydh.wizard.dataversePublisher.chooseRepository.successMessage"));
									setNextEnabled(true);
								} else {
									setNextEnabled(false);
					        	}
				        	}
				        }
				    });

				}
			});

			openRepositoryButton = new JButton(rm.get("replaydh.wizard.dataversePublisher.chooseRepository.loginInfoButton"));
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
					.add(new JLabel(rm.get("replaydh.wizard.dataversePublisher.chooseRepository.urlLabel"))).xy(1, 1)
					.add(tfUrl).xy(3, 1)
					.add(new JLabel(rm.get("replaydh.wizard.dataversePublisher.chooseRepository.apikey"))).xy(1, 3)
					.add(pAPIkey).xy(3, 3)
					.add(checkLoginButton).xy(3, 5)
					.add(resetButton).xy(3, 7)
					//.add(openRepositoryButton).xy(3, 7)
					.add(statusMessage).xyw(1, 11, 3)
					.build();
		}


		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			Object source = e.getSource();
			if (source == resetButton) {
				tfUrl.setText("");
				pAPIkey.setText("");
			}
		}

	};



	/**
	 * 2nd. page - choose collection, where to publish
	 */
	private static final DataversePublisherStep CHOOSE_COLLECTION = new DataversePublisherStep(
			"replaydh.wizard.dataversePublisher.chooseCollection.title",
			"replaydh.wizard.dataversePublisher.chooseCollection.description") {

		private JComboBox<String> collectionsComboBox;
		private JTextArea noAvailableCollectionsMessage;
		private CollectionEntry collectionEntries;

		@Override
		public void refresh(RDHEnvironment environment, DataversePublisherContext context) {
			super.refresh(environment, context); //call parent "refresh"

			// Update combobox with collections
			collectionsComboBox.removeAllItems();
			collectionEntries = new CollectionEntry(context.getAvailableCollections().entrySet());
			
			for(String value: collectionEntries.getValues()) {
				collectionsComboBox.addItem(value);
			}

			// Display the error message if there are no collections available
			noAvailableCollectionsMessage.setVisible(context.getAvailableCollections().isEmpty());

			// Remove selection and disable "next" button
			collectionsComboBox.setSelectedIndex(-1);
			setNextEnabled(false);
		};

		@Override
		public Page<DataversePublisherContext> next(RDHEnvironment environment, DataversePublisherContext context) {
			// Store collection url
			context.collectionURL = collectionEntries.getKey(collectionsComboBox.getSelectedItem().toString());

			return CHOOSE_DATASET;
		}

		@Override
		protected JPanel createPanel() {

			collectionsComboBox = new JComboBox<String>();
			collectionsComboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setNextEnabled(true);
				}
			});

			noAvailableCollectionsMessage = GuiUtils.createTextArea(ResourceManager.getInstance()
					.get("replaydh.wizard.dataversePublisher.chooseCollection.noCollectionsMessage"));

			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("pref, $nlg, pref, $nlg, pref")
					.padding(Paddings.DLU4)
					.add(new JLabel(ResourceManager.getInstance().get("replaydh.wizard.dataversePublisher.chooseCollection.collectionLabel"))).xy(1, 1)
					.add(collectionsComboBox).xy(1, 3)
					.add(noAvailableCollectionsMessage).xy(1, 5)
					.build();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub

		}
	};
	
	
	/**
	 * Showing all the files in a chosen collection
	 */
	private static final DataversePublisherStep CHOOSE_DATASET = new DataversePublisherStep(
			"replaydh.wizard.dataversePublisher.chooseDataset.title",
			"replaydh.wizard.dataversePublisher.chooseDataset.description") {

		private JComboBox<String> collectionsComboBox;
		private JTextArea noAvailableCollectionsMessage;
		
		private CollectionEntry collectionEntries;
		
		private long timeOut = 2; //in seconds
		
		private ResourceManager rm = ResourceManager.getInstance();
		
		@Override
		public void refresh(RDHEnvironment environment, DataversePublisherContext context) {
			super.refresh(environment, context); //call parent "refresh"

			checkFilesAvailable(context);
			
			// Update combobox with collections
			collectionsComboBox.removeAllItems();
			
			collectionsComboBox.addItem(rm.get("replaydh.wizard.dataversePublisher.chooseDataset.create"));
			
			collectionEntries = new CollectionEntry(context.availableDatasetsInCollection.entrySet());

			for(String value: collectionEntries.getValuesForDatasets()) {
				collectionsComboBox.addItem(value);
			}

			// Display the error message if there are no collections available
			noAvailableCollectionsMessage.setVisible(context.availableDatasetsInCollection.isEmpty());

			// Remove selection and disable "next" button
			collectionsComboBox.setSelectedIndex(-1);
			setNextEnabled(false);
		};
		
		private void checkFilesAvailable(DataversePublisherContext context) {

			SwingWorker<Boolean, Object> worker = new SwingWorker<Boolean, Object>(){

				@Override
				protected Boolean doInBackground() throws Exception {
					boolean filesAvailable = false;
					if (context.getPublicationRepository().getDatasetsInDataverseCollection(context.collectionURL) != null) {
						context.availableDatasetsInCollection=context.getPublicationRepository().getDatasetsInDataverseCollection(context.collectionURL);
						filesAvailable=true;
					}
					return filesAvailable;
				}
				
			};
			executeWorkerWithTimeout(worker, timeOut, "Exception by exchanging http/https");
		}

		@Override
		public Page<DataversePublisherContext> next(RDHEnvironment environment, DataversePublisherContext context) {
			context.chosenDataset = collectionEntries.getKeyForDatasets(collectionsComboBox.getSelectedItem().toString());
			return CHOOSE_FILES;
		}

		@Override
		protected JPanel createPanel() {

			collectionsComboBox = new JComboBox<String>();
			collectionsComboBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					setNextEnabled(true);
				}
			});

			noAvailableCollectionsMessage = GuiUtils.createTextArea(ResourceManager.getInstance()
					.get("replaydh.wizard.dataversePublisher.chooseCollection.noCollectionsMessage"));

			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("pref, $nlg, pref, $nlg, pref")
					.padding(Paddings.DLU4)
					.add(new JLabel(ResourceManager.getInstance().get("replaydh.wizard.dataversePublisher.chooseDataset.collectionLabel"))).xy(1, 1)
					.add(collectionsComboBox).xy(1, 3)
					.add(noAvailableCollectionsMessage).xy(1, 5)
					.build();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub

		}
	};


	/**
	 * 3rd. page - choose files for publishing
	 */
	private static final DataversePublisherStep CHOOSE_FILES = new DataversePublisherStep(
			"replaydh.wizard.dataversePublisher.chooseFiles.title",
			"replaydh.wizard.dataversePublisher.chooseFiles.description") {

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
		public void refresh(RDHEnvironment environment, DataversePublisherContext context) {
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
		public Page<DataversePublisherContext> next(RDHEnvironment environment, DataversePublisherContext context) {

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

			addFilesButton = new JButton(rm.get("replaydh.wizard.dataversePublisher.chooseFiles.addFilesButton"));
			addFilesButton.addActionListener(this::addFilesButtonPressed);

			removeFilesButton = new JButton(rm.get("replaydh.wizard.dataversePublisher.chooseFiles.removeFilesButton"));
			removeFilesButton.addActionListener(this::removeFilesButtonPressed);
			removeFilesButton.setEnabled(false);

			messageArea = GuiUtils.createTextArea(rm.get("replaydh.wizard.dataversePublisher.chooseFiles.infoText"));

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
					.add(new JLabel(rm.get("replaydh.wizard.dataversePublisher.chooseFiles.filesLabel"))).xy(1, 1)
					.add(filesTable).xy(1, 3)
					.add(buttonPanel).xy(3, 3)
					.add(messageArea).xyw(1, 5, 3)
					.build();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub

		}
	};


	/**
	 * 4th. page - edit metadata
	 */
	private static final DataversePublisherStep EDIT_METADATA = new DataversePublisherStep(
			"replaydh.wizard.dataversePublisher.editMetadata.title",
			"replaydh.wizard.dataversePublisher.editMetadata.description") {
		
		private JPanel mainPanel;
		//private GUIElement ePublicationYear;
		private GUIElement eIdentifier;
		private GUIElement ePublisher;
		private GUIElement eResourceType;
		private GUIElement eCreator;
		private GUIElement eTitle;
		private GUIElement eDescription;
		private GUIElement eSubjects;
		private GUIElement eVersion;
		private GUIElement eReference;
		private GUIElement eLicense;
		private GUIElement eRights;
		private GUIElement eDate;
		private GUIElement eSources;
		private GUIElement resetButton;
		private JTextArea messageArea;
		private JCheckBox processMetadata;
		private JCheckBox replaceMetadata;

		private List<GUIElement> creatorslist;
		private List<GUIElement> publisherslist;
		private List<GUIElement> subjectslist;
		private List<GUIElement> sourceslist;
		private List<String> listofkeys;
		private Map<String, JPanel> propertypanels;
		private Map<String, List<GUIElement>> elementsofproperty;
		private FormBuilder builder;
		private Map<String, Integer> panelRow;
		private ResourceManager rm;
		
		private List<String> titleElements;
		private List<String> descriptionElements;
		private List<String> creatorElements;
		//private List<String> issuedElements;
		private List<String> identifierElements;
		private List<String> publisherElements;
		private List<String> typeElements;
		private List<String> subjectElements;
		private List<String> hasVersionElements;
		private List<String> referenceElements;
		private List<String> licenseElements;
		private List<String> rightsElements;
		private List<String> dateElements;
		private List<String> sourcesElements;
		
		private long timeOut = 2; //in seconds
		private DocumentAdapter adapter;
		
		private List<Object> jsonObjects;
		private String propertyForSwitch;
		private String propertyvalue;
		private List<Object> authors;
		private List<Object> subjects;
		private List<Object> keywords;
		private List<Object> publisher;
		
		private RDHEnvironment myEnvironment;
		private DataversePublisherContext myContext;
		
		@Override
		public void refresh(RDHEnvironment environment, DataversePublisherContext context) {
			super.refresh(environment, context); //call parent "refresh"
			// Creator
			if (context.chosenDataset == null) {
				clearGUI();
				createNewDataset(environment, context);
				replaceMetadata.setSelected(false);
			} else {
				clearGUI();
				resetButton.getResetButton().setText(rm.get("replaydh.wizard.dataversePublisher.editMetadata.ResetButton"));
				getJSONObject(environment, context);
				replaceMetadata.setSelected(true);
			}
				
			
			myEnvironment=environment;
			myContext=context;
			
			refreshNextEnabled();

			//TODO: remove previous metadata when the page is opened again. Now previous metadata is kept. Se todo with mdObject above

		};
		
		public void createNewDataset(RDHEnvironment environment, DataversePublisherContext context) {
			String creator = null;
			if(creator==null) { 	//TODO fetch user defined value if mdObject is not null (see todo above)
				creator = environment.getProperty(RDHProperty.CLIENT_USERNAME);
			}
			eCreator.getTextfield().setText(creator);

			//TODO: should we use workflow title or workflow-step title is also possible? Because we publish files from the current workflow-step

			// Title
			eTitle.getTextfield().setText(context.exportInfo.getWorkflow().getTitle());

			// Description
			eDescription.getTextfield().setText(context.exportInfo.getWorkflow().getDescription());

			// Publication year
			int year = Calendar.getInstance().get(Calendar.YEAR);
			eDate.getTextfield().setText(String.valueOf(year));
			
			eLicense.getTextfield().setText("NONE");
		}
		
		private void getJSONObject(RDHEnvironment environment, DataversePublisherContext context) {

			SwingWorker<Boolean, Object> worker = new SwingWorker<Boolean, Object>(){

				@Override
				protected Boolean doInBackground() throws Exception {
					boolean metadataAvailable = false;
					if (context.chosenDataset != null) {
						String doi=context.chosenDataset.substring(context.chosenDataset.indexOf("doi:"), context.chosenDataset.length());
						String metadataUrl = createMetadataUrl(environment.getProperty(RDHProperty.DATAVERSE_REPOSITORY_URL),doi);
						if (context.getPublicationRepository().getJSONMetadata(metadataUrl) != null) {
							context.jsonObjectWithMetadata=context.getPublicationRepository().getJSONMetadata(metadataUrl);
							metadataAvailable=true;
						}
					}
					return metadataAvailable;
				}
				protected void done() {
					if (context.chosenDataset != null) {
						Configuration conf = Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);
						String license = JsonPath.using(conf).parse(context.jsonObjectWithMetadata).read("$.data.latestVersion.license");
						if (license != null) {
							eLicense.getTextfield().setText(license);
						} else {
							license = JsonPath.using(conf).parse(context.jsonObjectWithMetadata).read("$.data.license");
							if (license != null) {
								eLicense.getTextfield().setText("");
							}
						}
						String rights = JsonPath.using(conf).parse(context.jsonObjectWithMetadata).read("$.data.latestVersion.termsOfUse");
						if (rights != null) {
							eRights.getTextfield().setText(rights);
						} else {
							rights = JsonPath.using(conf).parse(context.jsonObjectWithMetadata).read("$.data.termsOfUse");
							if (rights != null) {
								eRights.getTextfield().setText("");
							}
						}
						jsonObjects = JsonPath.read(context.jsonObjectWithMetadata,"$.data.latestVersion.metadataBlocks.citation.fields[*].typeName");
						for (int i=0; i < jsonObjects.size(); i++) {
							propertyForSwitch = JsonPath.read(context.jsonObjectWithMetadata,"$.data.latestVersion.metadataBlocks.citation.fields["+i+"].typeName");
							switch(propertyForSwitch) {
							case "title":
								propertyvalue=JsonPath.read(context.jsonObjectWithMetadata,"$.data.latestVersion.metadataBlocks.citation.fields["+i+"].value");
								if (propertyvalue != null) {
									eTitle.getTextfield().setText(propertyvalue);
								}
								break;
							case "otherId":
								propertyvalue=JsonPath.read(context.jsonObjectWithMetadata,"$.data.latestVersion.metadataBlocks.citation.fields["+i+"].value[0].otherIdValue.value");
								if (propertyvalue != null) {
									eIdentifier.getTextfield().setText(propertyvalue);
								}
								break;
							case "author":
								if (authors == null) {
									authors = JsonPath.read(context.jsonObjectWithMetadata,"$.data.latestVersion.metadataBlocks.citation.fields["+i+"].value[*]");
									for (int index=0; index < authors.size(); index++) {
										propertyvalue=JsonPath.read(context.jsonObjectWithMetadata,"$.data.latestVersion.metadataBlocks.citation.fields["+i+"].value["+index+"].authorName.value");
										if (index+1 < authors.size()) {
											GUIElement element = createGUIElement("creator");
											elementsofproperty.get("creator").add(element);
											element.getTextfield().getDocument().addDocumentListener(adapter);
										}
										elementsofproperty.get("creator").get(index).getTextfield().setText(propertyvalue);
									}
								}
								refreshPanel("creator");
								break;
							case "dsDescription":
								propertyvalue=JsonPath.read(context.jsonObjectWithMetadata,"$.data.latestVersion.metadataBlocks.citation.fields["+i+"].value[0].dsDescriptionValue.value");
								if (propertyvalue != null) {
									eDescription.getTextfield().setText(propertyvalue);
								}
								break;
							case "subject":
								if (subjects == null) {
									subjects = JsonPath.read(context.jsonObjectWithMetadata,"$.data.latestVersion.metadataBlocks.citation.fields["+i+"].value[*]");
									for (int index=0; index < subjects.size(); index++) {
										propertyvalue=JsonPath.read(context.jsonObjectWithMetadata,"$.data.latestVersion.metadataBlocks.citation.fields["+i+"].value["+index+"]");
										if (index+1 < subjects.size()) {
											GUIElement element = createGUIElement("subject");
											elementsofproperty.get("subject").add(element);
											element.getTextfield().getDocument().addDocumentListener(adapter);
										}
										elementsofproperty.get("subject").get(index).getTextfield().setText(propertyvalue);
									}
								}
								refreshPanel("subject");
								break;
							case "keyword":
								if (keywords == null) {
									keywords = JsonPath.read(context.jsonObjectWithMetadata,"$.data.latestVersion.metadataBlocks.citation.fields["+i+"].value[*]");
									int numbersToAdd=elementsofproperty.get("subject").size();
									for (int index=0; index < keywords.size(); index++) {
										propertyvalue=JsonPath.read(context.jsonObjectWithMetadata,"$.data.latestVersion.metadataBlocks.citation.fields["+i+"].value["+index+"].keywordValue.value");
										if (index < keywords.size()) {
											GUIElement element = createGUIElement("subject");
											elementsofproperty.get("subject").add(element);
											element.getTextfield().getDocument().addDocumentListener(adapter);
										}
										elementsofproperty.get("subject").get(index+numbersToAdd).getTextfield().setText(propertyvalue);
									}
								}
								refreshPanel("subject");
								break;
							case "producer":
								if (publisher == null) {
									publisher = JsonPath.read(context.jsonObjectWithMetadata,"$.data.latestVersion.metadataBlocks.citation.fields["+i+"].value[*]");
									for (int index=0; index < publisher.size(); index++) {
										propertyvalue=JsonPath.read(context.jsonObjectWithMetadata,"$.data.latestVersion.metadataBlocks.citation.fields["+i+"].value["+index+"].producerName.value");
										if (index+1 < publisher.size()) {
											GUIElement element = createGUIElement("publisher");
											elementsofproperty.get("publisher").add(element);
											element.getTextfield().getDocument().addDocumentListener(adapter);
										}
										elementsofproperty.get("publisher").get(index).getTextfield().setText(propertyvalue);
									}
								}
								refreshPanel("publisher");
								break;
							case "productionDate":
								propertyvalue=JsonPath.read(context.jsonObjectWithMetadata,"$.data.latestVersion.metadataBlocks.citation.fields["+i+"].value");
								if (propertyvalue != null) {
									eDate.getTextfield().setText(propertyvalue);
								} else {
									eDate.getTextfield().setText("");
								}
								break;
							case "kindOfData":
								propertyvalue=JsonPath.read(context.jsonObjectWithMetadata,"$.data.latestVersion.metadataBlocks.citation.fields["+i+"].value[0]");
								if (propertyvalue != null) {
									eResourceType.getTextfield().setText(propertyvalue);
								} else {
									eResourceType.getTextfield().setText("");
								}
								break;
							}
						}
					}
				}
			};
			executeWorkerWithTimeout(worker, timeOut, "Exception by exchanging http/https");
		}

		@Override
		public Page<DataversePublisherContext> next(RDHEnvironment environment, DataversePublisherContext context) {

			// Store metadata
			context.metadataObject = new MetadataObject();
			context.metadataObject.mapDublinCoreToMetadata = new HashMap<>();
			context.metadataObject.mapDublinCoreToLabel = new HashMap<>();

			// Title
			if (titleElements == null) {
				titleElements = new ArrayList<>();
			} else {
				titleElements.clear();
			}
			titleElements.add(eTitle.getTextfield().getText());
			context.metadataObject.mapDublinCoreToMetadata.put("title", titleElements);
			context.metadataObject.mapDublinCoreToLabel.put("title", rm.get("replaydh.wizard.dataversePublisher.editMetadata.titleLabel"));

			// Description
			if (descriptionElements == null) {
				descriptionElements = new ArrayList<>();
			} else {
				descriptionElements.clear();
			}
			descriptionElements.add(eDescription.getTextfield().getText());
			context.metadataObject.mapDublinCoreToMetadata.put("description", descriptionElements);
			context.metadataObject.mapDublinCoreToLabel.put("description", rm.get("replaydh.wizard.dataversePublisher.editMetadata.descriptionLabel"));

			// Creator
			if (creatorElements == null) {
				creatorElements = new ArrayList<>();
			} else {
				creatorElements.clear();
			}
			for (String property : getValuesOfProperty("creator")) {
				creatorElements.add(property);
			}
			context.metadataObject.mapDublinCoreToMetadata.put("creator", creatorElements);
			context.metadataObject.mapDublinCoreToLabel.put("creator", rm.get("replaydh.wizard.dataversePublisher.editMetadata.creatorLabel"));

			// Publication year
			/*if (issuedElements == null) {
				issuedElements = new ArrayList<>();
			} else {
				issuedElements.clear();
			}
			issuedElements.add(ePublicationYear.getTextfield().getText());
			context.metadataObject.mapDublinCoreToMetadata.put("issued", issuedElements);
			context.metadataObject.mapDublinCoreToLabel.put("issued", rm.get("replaydh.wizard.dataversePublisher.editMetadata.publicationYearLabel"));*/

			// Not used (reserved) metadata fields
			if (identifierElements == null) {
				identifierElements = new ArrayList<>();
			} else {
				identifierElements.clear();
			}
			identifierElements.add(eIdentifier.getTextfield().getText());
			context.metadataObject.mapDublinCoreToMetadata.put("identifier", identifierElements);
			context.metadataObject.mapDublinCoreToLabel.put("identifier", rm.get("replaydh.wizard.dataversePublisher.editMetadata.identifierLabel"));

			if (publisherElements == null) {
				publisherElements = new ArrayList<>();
			} else {
				publisherElements.clear();
			}
			for (String property : getValuesOfProperty("publisher")) {
				publisherElements.add(property);
			}
			context.metadataObject.mapDublinCoreToMetadata.put("publisher", publisherElements);
			context.metadataObject.mapDublinCoreToLabel.put("publisher", rm.get("replaydh.wizard.dataversePublisher.editMetadata.publisherLabel"));

			if (typeElements == null) {
				typeElements = new ArrayList<>();
			} else {
				typeElements.clear();
			}
			typeElements.add(eResourceType.getTextfield().getText());
			context.metadataObject.mapDublinCoreToMetadata.put("type", typeElements);
			context.metadataObject.mapDublinCoreToLabel.put("type", rm.get("replaydh.wizard.dataversePublisher.editMetadata.resourceTypeLabel"));

			if (subjectElements == null) {
				subjectElements = new ArrayList<>();
			} else {
				subjectElements.clear();
			}
			for (String property : getValuesOfProperty("subject")) {
				subjectElements.add(property);
			}
			context.metadataObject.mapDublinCoreToMetadata.put("subject", subjectElements);
			context.metadataObject.mapDublinCoreToLabel.put("subject", rm.get("replaydh.wizard.dataversePublisher.editMetadata.subjectLabel"));

			if (hasVersionElements == null) {
				hasVersionElements = new ArrayList<>();
			} else {
				hasVersionElements.clear();
			}
			hasVersionElements.add(eVersion.getTextfield().getText());
			context.metadataObject.mapDublinCoreToMetadata.put("hasVersion", hasVersionElements);
			context.metadataObject.mapDublinCoreToLabel.put("hasVersion", rm.get("replaydh.wizard.dataversePublisher.editMetadata.versionLabel"));

			if (referenceElements == null) {
				referenceElements = new ArrayList<>();
			} else {
				referenceElements.clear();
			}
			referenceElements.add(eReference.getTextfield().getText());
			context.metadataObject.mapDublinCoreToMetadata.put("isReferencedBy", referenceElements);
			context.metadataObject.mapDublinCoreToLabel.put("isReferencedBy", rm.get("replaydh.wizard.dataversePublisher.editMetadata.isReferencedByLabel"));

			if (licenseElements == null) {
				licenseElements = new ArrayList<>();
			} else {
				licenseElements.clear();
			}
			if (!((eLicense.getTextfield().getText().equals("CC0")) || (eLicense.getTextfield().getText().equals("NONE")))) {
				licenseElements.add("NONE");
			}
			licenseElements.add(eLicense.getTextfield().getText());
			context.metadataObject.mapDublinCoreToMetadata.put("license", licenseElements);
			context.metadataObject.mapDublinCoreToLabel.put("license", rm.get("replaydh.wizard.dataversePublisher.editMetadata.LicenseLabel"));
			
			if (dateElements == null) {
				dateElements = new ArrayList<>();
			} else {
				dateElements.clear();
			}
			dateElements.add(eDate.getTextfield().getText());
			context.metadataObject.mapDublinCoreToMetadata.put("date", dateElements);
			context.metadataObject.mapDublinCoreToLabel.put("date", rm.get("replaydh.wizard.dataversePublisher.editMetadata.dateLabel"));

			if (rightsElements == null) {
				rightsElements = new ArrayList<>();
			} else {
				rightsElements.clear();
			}
			rightsElements.add(eRights.getTextfield().getText());
			context.metadataObject.mapDublinCoreToMetadata.put("rights", rightsElements);
			context.metadataObject.mapDublinCoreToLabel.put("rights", rm.get("replaydh.wizard.dataversePublisher.editMetadata.RightsLabel"));
			
			if (sourcesElements == null) {
				sourcesElements = new ArrayList<>();
			} else {
				sourcesElements.clear();
			}
			for (String property : getValuesOfProperty("sources")) {
				sourcesElements.add(property);
			}
			sourcesElements.add(eSources.getTextfield().getText());
			context.metadataObject.mapDublinCoreToMetadata.put("sources", sourcesElements);
			context.metadataObject.mapDublinCoreToLabel.put("sources", rm.get("replaydh.wizard.dataversePublisher.editMetadata.sourcesLabel"));
			
			context.setExportProcessMetadataAllowed(processMetadata.isSelected());

			context.setReplaceMetadataAllowed(replaceMetadata.isSelected());
			
			return FINISH;
		}

		private void refreshNextEnabled() {
			boolean nextEnabled = true;

			
			nextEnabled &= refreshBorder(elementsofproperty.get("creator"));
			nextEnabled &= checkAndUpdateBorder(eTitle.getTextfield());
			nextEnabled &= checkAndUpdateBorder(eDescription.getTextfield());
			nextEnabled &= checkAndUpdateBorder(eDate.getTextfield());
			
			if (!((eLicense.getTextfield().getText().equals("CC0")) || (eLicense.getTextfield().getText().equals("NONE")))) {
				eLicense.getTextfield().setBorder(GuiUtils.getDefaulterrorborder());
				nextEnabled &=false;
			} else {
				eLicense.getTextfield().setBorder(GuiUtils.getStandardBorder());
				nextEnabled &=true;
			}

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
			listofkeys = new ArrayList<>();
			propertypanels = new HashMap<>();
			elementsofproperty = new HashMap<>();
			builder = FormBuilder.create();
			panelRow = new HashMap<>();

			DateFormat format;

			rm = ResourceManager.getInstance();

			JTextField tfTitle = new JTextField();
			JLabel lTitle = new JLabel(rm.get("replaydh.wizard.dataversePublisher.editMetadata.titleLabel"));
			eTitle = new GUIElement();
			eTitle.setTextfield(tfTitle);
			eTitle.setLabel(lTitle);
			eTitle.create();
			listofkeys.add("title");

			JTextField tfDescription = new JTextField();
			JLabel lDescription = new JLabel(rm.get("replaydh.wizard.dataversePublisher.editMetadata.descriptionLabel"));
			eDescription = new GUIElement();
			eDescription.setTextfield(tfDescription);
			eDescription.setLabel(lDescription);
			eDescription.create();
			listofkeys.add("description");

			JTextField tfCreator = new JTextField();
			JLabel lCreator = new JLabel(rm.get("replaydh.wizard.dataversePublisher.editMetadata.creatorLabel"));
			eCreator = new GUIElement();
			eCreator.setTextfield(tfCreator);
			eCreator.setLabel(lCreator);
			eCreator.setButton(new JButton());
			eCreator.getButton().addActionListener(this);
			eCreator.setMinusbutton(new JButton());
			eCreator.getMinusbutton().addActionListener(this);
			eCreator.create();
			listofkeys.add("creator");

			/*format = new SimpleDateFormat("YYYY");
			JFormattedTextField tfPublicationYear = new JFormattedTextField(format);
			JLabel lPubYear = new JLabel(rm.get("replaydh.wizard.dataversePublisher.editMetadata.publicationYearLabel"));
			ePublicationYear = new GUIElement();
			ePublicationYear.setTextfield(tfPublicationYear);
			ePublicationYear.setLabel(lPubYear);
			tfPublicationYear.setToolTipText("YYYY");
			ePublicationYear.create();
			listofkeys.add("year");*/
			
			//JTextField tfDate = new JTextField();
			format = new SimpleDateFormat("YYYY");
			JFormattedTextField tfDate = new JFormattedTextField(format);
			JLabel lDate = new JLabel(rm.get("replaydh.wizard.dataversePublisher.editMetadata.dateLabel"));
			eDate = new GUIElement();
			eDate.setTextfield(tfDate);
			eDate.setLabel(lDate);
			eDate.create();
			listofkeys.add("date");

			JTextField tfResourceType = new JTextField();
			JLabel lResourceType = new JLabel(rm.get("replaydh.wizard.dataversePublisher.editMetadata.resourceTypeLabel"));
			eResourceType = new GUIElement();
			eResourceType.setTextfield(tfResourceType);
			eResourceType.setLabel(lResourceType);
			eResourceType.create();
			listofkeys.add("resourceType");

			JTextField tfIdentifier = new JTextField();
			JLabel lIdentifier = new JLabel(rm.get("replaydh.wizard.dataversePublisher.editMetadata.identifierLabel"));
			eIdentifier = new GUIElement();
			eIdentifier.setTextfield(tfIdentifier);
			eIdentifier.setLabel(lIdentifier);
			eIdentifier.create();
			listofkeys.add("identifier");

			JTextField tfPublisher = new JTextField();
			JLabel lPublisher = new JLabel(rm.get("replaydh.wizard.dataversePublisher.editMetadata.publisherLabel"));
			ePublisher = new GUIElement();
			ePublisher.setTextfield(tfPublisher);
			ePublisher.setLabel(lPublisher);
			ePublisher.setButton(new JButton());
			ePublisher.getButton().addActionListener(this);
			ePublisher.setMinusbutton(new JButton());
			ePublisher.getMinusbutton().addActionListener(this);
			ePublisher.create();
			listofkeys.add("publisher");

			JTextField tfSubjects = new JTextField();
			JLabel lSubjects = new JLabel(rm.get("replaydh.wizard.dataversePublisher.editMetadata.subjectLabel"));
			eSubjects = new GUIElement();
			eSubjects.setTextfield(tfSubjects);
			eSubjects.setLabel(lSubjects);
			eSubjects.setButton(new JButton());
			eSubjects.getButton().addActionListener(this);
			eSubjects.setMinusbutton(new JButton());
			eSubjects.getMinusbutton().addActionListener(this);
			eSubjects.create();
			listofkeys.add("subject");

			JTextField tfVersion = new JTextField();
			JLabel lversion = new JLabel(rm.get("replaydh.wizard.dataversePublisher.editMetadata.versionLabel"));
			eVersion = new GUIElement();
			eVersion.setTextfield(tfVersion);
			eVersion.setLabel(lversion);
			eVersion.create();
			listofkeys.add("version");

			JTextField tfReference = new JTextField();
			JLabel lreference = new JLabel(rm.get("replaydh.wizard.dataversePublisher.editMetadata.isReferencedByLabel"));
			eReference = new GUIElement();
			eReference.setTextfield(tfReference);
			eReference.setLabel(lreference);
			eReference.create();
			listofkeys.add("reference");

			JTextField tfLicense = new JTextField("NONE");
			JLabel lLicense = new JLabel(rm.get("replaydh.wizard.dataversePublisher.editMetadata.LicenseLabel"));
			eLicense = new GUIElement();
			eLicense.setTextfield(tfLicense);
			eLicense.getTextfield().setToolTipText(rm.get("replaydh.wizard.dataversePublisher.editMetadata.licenseToolTip"));
			eLicense.setLabel(lLicense);
			eLicense.create();
			listofkeys.add("license");

			JTextField tfRights = new JTextField();
			JLabel lRights = new JLabel(rm.get("replaydh.wizard.dataversePublisher.editMetadata.RightsLabel"));
			eRights = new GUIElement();
			eRights.setTextfield(tfRights);
			eRights.setLabel(lRights);
			eRights.create();
			listofkeys.add("rights");
			
			JTextField tfSource = new JTextField();
			JLabel lSource = new JLabel(rm.get("replaydh.wizard.dataversePublisher.editMetadata.sourcesLabel"));
			eSources = new GUIElement();
			eSources.setTextfield(tfSource);
			eSources.setLabel(lSource);
			eSources.setButton(new JButton());
			eSources.getButton().addActionListener(this);
			eSources.setMinusbutton(new JButton());
			eSources.getMinusbutton().addActionListener(this);
			eSources.create();
			listofkeys.add("sources");

			resetButton = new GUIElement();
			resetButton.createResetButton(rm.get("replaydh.wizard.dataversePublisher.editMetadata.ResetButton"));
			resetButton.getResetButton().addActionListener(this);

			GuiUtils.prepareChangeableBorder(tfCreator);
			GuiUtils.prepareChangeableBorder(tfTitle);
			GuiUtils.prepareChangeableBorder(tfDescription);
			GuiUtils.prepareChangeableBorder(tfDate);

			messageArea = GuiUtils.createTextArea(rm.get("replaydh.wizard.dataversePublisher.editMetadata.infoMessage"));
			
			adapter = new DocumentAdapter() {
				@Override
				public void anyUpdate(DocumentEvent e) {
					refreshNextEnabled();
				}
			};

			eCreator.getTextfield().getDocument().addDocumentListener(adapter);
			eLicense.getTextfield().getDocument().addDocumentListener(adapter);
			tfTitle.getDocument().addDocumentListener(adapter);
			tfDescription.getDocument().addDocumentListener(adapter);
			tfDate.getDocument().addDocumentListener(adapter);

			processMetadata = new JCheckBox(rm.get("replaydh.wizard.dataversePublisher.editMetadata.processMetadata"));
			processMetadata.setSelected(true);
			
			replaceMetadata = new JCheckBox(rm.get("replaydh.wizard.dataversePublisher.editMetadata.replaceMetadata"));
			replaceMetadata.setSelected(false);

			builder.columns("pref:grow");
			builder.rows("pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref");
			builder.padding(Paddings.DLU4);
			createGUI();
			mainPanel=builder.getPanel();
			return builder.build();
		}
		
		public void clearGUI() {
			for (String propertyname : listofkeys) {
				if(elementsofproperty.get(propertyname) != null) {
					int size = elementsofproperty.get(propertyname).size();
					for (int i=size-1; i > 0; i--) {
						elementsofproperty.get(propertyname).remove(i);
					}
					elementsofproperty.get(propertyname).get(0).getTextfield().setText("");
					refreshPanel(propertyname);
				}
			}
			subjects=null;
			authors=null;
			keywords=null;
			publisher=null;
			//ePublicationYear.getTextfield().setText("");
			eIdentifier.getTextfield().setText("");
			eResourceType.getTextfield().setText("");
			eTitle.getTextfield().setText("");
			eDescription.getTextfield().setText("");
			eVersion.getTextfield().setText("");
			eReference.getTextfield().setText("");
			eLicense.getTextfield().setText("");
			eRights.getTextfield().setText("");
			eDate.getTextfield().setText("");
		}
		
		public void createGUI() {
			
			if (publisherslist == null) {
				publisherslist = new ArrayList<>();
			} else {
				publisherslist.clear();
			}
			
			publisherslist.add(ePublisher);
			elementsofproperty.put("publisher", publisherslist);
			propertypanels.put("publisher", elementsofproperty.get("publisher").get(0).getPanel());
			
			if (subjectslist == null) {
				subjectslist = new ArrayList<>();
			} else {
				subjectslist.clear();
			}
			
			subjectslist.add(eSubjects);
			elementsofproperty.put("subject", subjectslist);
			propertypanels.put("subject", elementsofproperty.get("subject").get(0).getPanel());
			
			if (creatorslist == null) {
				creatorslist = new ArrayList<>();
			} else {
				creatorslist.clear();
			}
			
			creatorslist.add(eCreator);
			elementsofproperty.put("creator", creatorslist);
			propertypanels.put("creator", elementsofproperty.get("creator").get(0).getPanel());
			
			if (sourceslist == null) {
				sourceslist = new ArrayList<>();
			} else {
				sourceslist.clear();
			}
			
			sourceslist.add(eSources);
			elementsofproperty.put("sources", sourceslist);
			propertypanels.put("sources", elementsofproperty.get("sources").get(0).getPanel());
			
			builder.add(eTitle.getPanel()).xy(1, 1);
			panelRow.put("title", 1);
			builder.add(eDescription.getPanel()).xy(1, 3);
			panelRow.put("description", 3);
			builder.add(propertypanels.get("creator")).xy(1, 5);
			panelRow.put("creator", 5);
			//builder.add(ePublicationYear.getPanel()).xy(1, 7);
			//panelRow.put("year", 7);
			builder.add(eDate.getPanel()).xy(1, 7);
			panelRow.put("date", 7);
			builder.add(eResourceType.getPanel()).xy(1, 9);
			panelRow.put("resourceType", 9);
			builder.add(eIdentifier.getPanel()).xy(1, 11);
			panelRow.put("identifier", 11);
			builder.add(propertypanels.get("publisher")).xy(1, 13);
			panelRow.put("publisher", 13);
			builder.add(propertypanels.get("subject")).xy(1, 15);
			panelRow.put("subject", 15);
			builder.add(eVersion.getPanel()).xy(1, 17);
			panelRow.put("version", 17);
			builder.add(eReference.getPanel()).xy(1, 19);
			panelRow.put("reference", 19);
			builder.add(eLicense.getPanel()).xy(1, 21);
			panelRow.put("license", 21);
			builder.add(eRights.getPanel()).xy(1, 23);
			panelRow.put("rights", 23);
			builder.add(propertypanels.get("sources")).xy(1, 25);
			panelRow.put("sources", 25);
			builder.add(resetButton.getPanel()).xy(1, 27);
			builder.add(processMetadata).xy(1, 29);
			builder.add(replaceMetadata).xy(1, 31);
			builder.add(messageArea).xyw(1, 33, 1);
		}

		public GUIElement createGUIElement(String metadataproperty) {
			GUIElement elementToAdd = new GUIElement();
			JTextField textfield = new JTextField();
			elementToAdd.setTextfield(textfield);
			JButton button = new JButton();
			elementToAdd.setButton(button);
			JButton minusbutton = new JButton();
			elementToAdd.setMinusbutton(minusbutton);
			elementToAdd.create();
			return elementToAdd;
		}

		/**
		 * Refreshes one JPanel according to the specified metadata property and its position (index) in the main
		 * panelbuilder (builder)
		 * @param metadatapropertyname
		 */
		public void refreshPanel(String metadatapropertyname) {
			String columns="pref:grow";
			String rows="pref";

			int counter=0;
			for(GUIElement oneguielement : elementsofproperty.get(metadatapropertyname)) {
				oneguielement.getButton().removeActionListener(this);
				if (counter > 0) {
					oneguielement.getMinusbutton().removeActionListener(this);
				}
				counter++;
			}

			FormLayout layout = new FormLayout(columns,rows);


			JPanel onepropertypanel = propertypanels.get(metadatapropertyname);

			onepropertypanel.removeAll();

			onepropertypanel.setLayout(layout);



			JPanel newpropertypanel = new JPanel();


			FormBuilder propertybuilder = FormBuilder.create();
			propertybuilder.columns(columns);
			propertybuilder.rows(rows);



			propertybuilder.panel(newpropertypanel);

			propertypanels.put(metadatapropertyname, newpropertypanel);
			onepropertypanel.removeAll();
			onepropertypanel.setLayout(layout);

			int numberOfElements=elementsofproperty.get(metadatapropertyname).size();

			int z=0;

			for(GUIElement oneguielement : elementsofproperty.get(metadatapropertyname)) {

				if (z == 0) {
					oneguielement.create();
					if (oneguielement.getLabel().getText().equals("")) {
						switch (metadatapropertyname) {
						case "creator":
							oneguielement.getLabel().setText(rm.get("replaydh.wizard.dataversePublisher.editMetadata.creatorLabel"));
							break;
						case "publisher":
							oneguielement.getLabel().setText(rm.get("replaydh.wizard.dataversePublisher.editMetadata.publisherLabel"));
							break;
						case "subject":
							oneguielement.getLabel().setText(rm.get("replaydh.wizard.dataversePublisher.editMetadata.subjectLabel"));
							break;
						case "sources":
							oneguielement.getLabel().setText(rm.get("replaydh.wizard.dataversePublisher.editMetadata.sourcesLabel"));
							break;
						}
					}
				}
				oneguielement.getButton().addActionListener(this);

				oneguielement.getMinusbutton().addActionListener(this);

				propertybuilder.add(oneguielement.getPanel()).xy(1, (z*2)+1);

				if (numberOfElements > 1) {
					propertybuilder.appendRows("$nlg, pref");
				}

				z++;


			}
			if (elementsofproperty.get(metadatapropertyname).size() > 1) {
				propertybuilder.addSeparator("").xyw(1, ((z*2)+1), 1);
				z++;
			}
			builder.add(propertybuilder.build()).xy(1, panelRow.get(metadatapropertyname));
			Window parentComponent = (Window) SwingUtilities.getAncestorOfClass(Window.class, mainPanel);
			if (parentComponent != null) {
				parentComponent.pack();
			}
		}

		public void removeElementFromPanel(String metadatapropertyname, int buttonNumber) {
			elementsofproperty.get(metadatapropertyname).remove(buttonNumber);
			refreshPanel(metadatapropertyname);
		}

		public List<String> getValuesOfProperty(String metadatapropertyname) {
			List<String> propertyValues = new ArrayList<>();
			for(GUIElement oneguielement: elementsofproperty.get(metadatapropertyname)) {
				if (!(oneguielement.getTextfield().getText().equals(""))) {
					propertyValues.add(oneguielement.getTextfield().getText());
				}
			}
			return propertyValues;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			Object source = e.getSource();
			JButton buttonpressed = null;
			JButton minusbuttonpressed = null;
			boolean done=false;
			for (String propertyname : listofkeys) {
				if (elementsofproperty.get(propertyname) != null) {
					for (int buttonNumber = 0; buttonNumber < elementsofproperty.get(propertyname).size(); buttonNumber++) {
						buttonpressed=elementsofproperty.get(propertyname).get(buttonNumber).getButton();
						minusbuttonpressed=elementsofproperty.get(propertyname).get(buttonNumber).getMinusbutton();
						if (source == buttonpressed) {
							GUIElement element = createGUIElement(propertyname);
							elementsofproperty.get(propertyname).add(element);
							if (propertyname.equals("creator")) {
								element.getTextfield().getDocument().addDocumentListener(adapter);
								refreshBorder(elementsofproperty.get(propertyname));
							}
							refreshPanel(propertyname);
							done=true;
							break;
						}
						if (source == minusbuttonpressed) {
							if (elementsofproperty.get(propertyname).size() > 1) {
								if (propertyname.equals("creator")) {
									elementsofproperty.get(propertyname).get(buttonNumber).getTextfield().getDocument().removeDocumentListener(adapter);
								}
								removeElementFromPanel(propertyname,buttonNumber);
							} else {
								elementsofproperty.get(propertyname).get(0).getTextfield().setText("");
							}
							if (propertyname.equals("creator")) {
								refreshBorder(elementsofproperty.get(propertyname));
							}
							done=true;
							break;
						}
					}
					if (done == true) {
						break;
					}
				}
			}
			if (source == resetButton.getResetButton()) {
				if (resetButton.getResetButton().getText().equals(rm.get("replaydh.wizard.dataversePublisher.editMetadata.RestoreButton"))) {
					resetButton.getResetButton().setText(rm.get("replaydh.wizard.dataversePublisher.editMetadata.ResetButton"));
					if (myContext.chosenDataset == null) {
						createNewDataset(myEnvironment, myContext);
					} else {
						clearGUI();
						getJSONObject(myEnvironment, myContext);
					}
				} else {
					builder.getPanel().removeAll();
					createGUI();
					clearGUI();
					resetButton.getResetButton().setText(rm.get("replaydh.wizard.dataversePublisher.editMetadata.RestoreButton"));
					Window parentComponent = (Window) SwingUtilities.getAncestorOfClass(Window.class, builder.getPanel());
					parentComponent.pack();
				}
			}
		}
		
		public boolean refreshBorder(List<GUIElement> propertylist) {
			boolean allEmpty=true;
			for (GUIElement checkElement : propertylist) {
				if (!(checkElement.getTextfield().getText().equals(""))) {
					allEmpty=false;
					break;
				}
			}
			if (!(allEmpty)) {
				for (GUIElement changeElement : propertylist) {
					changeElement.getTextfield().setBorder(GuiUtils.getStandardBorder());
				}
			} else {
				for (GUIElement changeElement : propertylist) {
					changeElement.getTextfield().setBorder(GuiUtils.getDefaulterrorborder());
				}
			}
			return !allEmpty;
		}
	};




	/**
	 * Last page - entry point for the publication
	 */
	private static final DataversePublisherStep FINISH = new DataversePublisherStep(
			"replaydh.wizard.dataversePublisher.finish.title",
			"replaydh.wizard.dataversePublisher.finish.description") {

		private JTextArea repositoryUrlArea;
		private JTextArea choosenCollectionArea;
		private JTextArea choosenFilesArea;
		private JTextArea metadataArea;
		private JTextArea metadataNotice;
		private JTextArea messageArea;
		
		private ResourceManager rm = ResourceManager.getInstance();

		@Override
		public void refresh(RDHEnvironment environment, DataversePublisherContext context) {
			super.refresh(environment, context); //call parent "refresh"

			// Repository URL
			if(context.getRepositoryURL() != null) {
				repositoryUrlArea.setText(context.getRepositoryURL());
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
				strFiles = ResourceManager.getInstance().get("replaydh.wizard.dataversePublisher.finish.noDataMessage");
			}
			choosenFilesArea.setText(strFiles);

			// Metadata
			String strMetadata = "";
			if(context.getMetadataObject() != null) {
				for(Map.Entry<String, List<String>> entry: context.getMetadataObject().getMapLabelToMetadata().entrySet()) {
					strMetadata += entry.getKey() + ": ";
					for (String property : entry.getValue()) {
						strMetadata += property + ", ";
					}
					if (strMetadata.substring(strMetadata.length()-2, strMetadata.length()).equals(", ")) {
						strMetadata=strMetadata.substring(0, strMetadata.length()-2);
					}
					strMetadata += "\n";
				}
			}
			if(!strMetadata.equals("")) {
				strMetadata = strMetadata.substring(0, strMetadata.length() - 1); //remove last "\n"
			} else {
				strMetadata = ResourceManager.getInstance().get("replaydh.wizard.dataversePublisher.finish.noDataMessage");
			}
			metadataArea.setText(strMetadata);
			
			if (context.isExportProcessMetadataAllowed()) {
				metadataNotice.setText(rm.get("replaydh.wizard.dataversePublisher.finish.processMetadata"));
			} else {
				metadataNotice.setText("");
			}

			// Info message
			messageArea.setText(ResourceManager.getInstance().get("replaydh.wizard.dataversePublisher.finish.infoMessage"));
		};

		@Override
		public Page<DataversePublisherContext> next(RDHEnvironment environment, DataversePublisherContext context) {
			return null;
		}

		@Override
		protected JPanel createPanel() {

			ResourceManager rm = ResourceManager.getInstance();

			repositoryUrlArea = GuiUtils.createTextArea("");
			choosenCollectionArea = GuiUtils.createTextArea("");
			choosenFilesArea = GuiUtils.createTextArea("");
			metadataArea = GuiUtils.createTextArea("");
			metadataNotice = GuiUtils.createTextArea("");
			messageArea = GuiUtils.createTextArea("");

			//TODO: add separators
			return FormBuilder.create()
					.columns("pref, 6dlu, fill:pref:grow")
					.rows("top:pref, $nlg, top:pref, $nlg, top:pref, $nlg, top:pref, $nlg, top:pref, $nlg, top:pref, $nlg, top:pref")
					.padding(Paddings.DLU4)
					.add(new JLabel(rm.get("replaydh.wizard.dataversePublisher.chooseRepository.urlLabel"))).xy(1, 1)
					.add(repositoryUrlArea).xy(3, 1)
					.add(new JLabel(rm.get("replaydh.wizard.dataversePublisher.chooseCollection.collectionLabel"))).xy(1, 3)
					.add(choosenCollectionArea).xy(3, 3)
					.add(new JLabel(rm.get("replaydh.wizard.dataversePublisher.chooseFiles.filesLabel"))).xy(1, 5)
					.add(choosenFilesArea).xy(3, 5)
					.add(new JLabel(rm.get("replaydh.wizard.dataversePublisher.finish.metadataLabel"))).xy(1, 7)
					.add(metadataArea).xy(3, 7)
					.add(metadataNotice).xy(3, 9)
					.add(messageArea).xyw(1, 13, 3)
					.build();
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub

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

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;

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



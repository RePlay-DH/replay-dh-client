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
import com.jgoodies.forms.layout.FormLayout;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.AbstractWizardStep;
import bwfdm.replaydh.ui.helper.DocumentAdapter;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.helper.Wizard.Page;
import bwfdm.replaydh.workflow.export.dataverse.GUIElement;
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
				environment, CHOOSE_REPOSITORY, CHOOSE_COLLECTION, CHOOSE_FILES, EDIT_METADATA, FINISH);
		return wizard;
	}

	/**
	 * Context for the wizard
	 */
	public static final class DataversePublisherContext{

		final WorkflowExportInfo exportInfo;

		private RDHEnvironment environment;
		private String repositoryURL;		//e.g. "http://123.11.11.11:8080/xmlui/" or "http://123.11.11.11:8080"
											//TODO: could be also JSPUI !!!
		private String serviceDocumentURL;
		private String collectionURL;
		private Map<String, String> availableCollections;
		
		private List<File> filesToPublish;
		private DataverseRepository_v4 publicationRepository;
		private MetadataObject metadataObject;

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
	 * @author vk
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
	 * @author Florian Fritze
	 */
	private static abstract class DataversePublisherStep extends AbstractWizardStep<DataversePublisherContext> implements ActionListener {
		protected DataversePublisherStep(String titleKey, String descriptionKey) {
			super(titleKey, descriptionKey);
		}
		protected static List<String> listofkeys = null;
		protected static Map<String, JPanel> propertypanels = null;
		protected static Map<String, List<GUIElement>> elementsofproperty = null;
		protected static FormBuilder builder = null;
		protected static Map<String, Integer> panelRow = null; 
		
		
		static {
			listofkeys = new ArrayList<>();
			propertypanels = new HashMap<>();
			elementsofproperty = new HashMap<>();
			builder = FormBuilder.create();
			panelRow = new HashMap<>();
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
				        			availableCollections=publicationRepository.getUserAvailableCollectionsWithTitle();
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
					//.add(openRepositoryButton).xy(3, 7)
					.add(statusMessage).xyw(1, 9, 3)
					.build();
		}


		@Override
		public void actionPerformed(ActionEvent e) {
			// TODO Auto-generated method stub
			
		}

	};



	/**
	 * 2nd. page - choose collection, where to publish
	 */
	private static final DataversePublisherStep CHOOSE_COLLECTION = new DataversePublisherStep(
			"replaydh.wizard.dataversePublisher.chooseCollection.title",
			"replaydh.wizard.dataversePublisher.chooseCollection.description") {

		private JComboBox<CollectionEntry> collectionsComboBox;
		private JTextArea noAvailableCollectionsMessage;

		@Override
		public void refresh(RDHEnvironment environment, DataversePublisherContext context) {
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
		public Page<DataversePublisherContext> next(RDHEnvironment environment, DataversePublisherContext context) {
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
		private GUIElement ePublicationYear;
		private GUIElement eIdentifier;
		private GUIElement ePublisher;
		private GUIElement eResourceType;
		private GUIElement eCreator;
		private GUIElement eTitle;
		private GUIElement eDescription;
		private GUIElement eSubjects;
		
		@Override
		public void refresh(RDHEnvironment environment, DataversePublisherContext context) {
			super.refresh(environment, context); //call parent "refresh"
			// Creator
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
			ePublicationYear.getTextfield().setText(String.valueOf(year));
			
			refreshNextEnabled();

			//TODO: remove previous metadata when the page is opened again. Now previous metadata is kept. Se todo with mdObject above

		};

		@Override
		public Page<DataversePublisherContext> next(RDHEnvironment environment, DataversePublisherContext context) {

			ResourceManager rm = ResourceManager.getInstance();
			List<String> elements = null;

			// Store metadata
			context.metadataObject = new MetadataObject();
			context.metadataObject.mapDublinCoreToMetadata = new HashMap<>();
			context.metadataObject.mapDublinCoreToLabel = new HashMap<>();

			// Title
			elements = new ArrayList<>();
			elements.add(eTitle.getTextfield().getText());
			context.metadataObject.mapDublinCoreToMetadata.put("title", elements);
			context.metadataObject.mapDublinCoreToLabel.put("title", rm.get("replaydh.wizard.dataversePublisher.editMetadata.titleLabel"));

			// Description
			elements = new ArrayList<>();
			elements.add(eDescription.getTextfield().getText());
			context.metadataObject.mapDublinCoreToMetadata.put("description", elements);
			context.metadataObject.mapDublinCoreToLabel.put("description", rm.get("replaydh.wizard.dataversePublisher.editMetadata.descriptionLabel"));

			// Creator
			elements = new ArrayList<>();
			for (String property : getValuesOfProperty("creator")) {
				elements.add(property);
			}
			context.metadataObject.mapDublinCoreToMetadata.put("creator", elements);
			context.metadataObject.mapDublinCoreToLabel.put("creator", rm.get("replaydh.wizard.dataversePublisher.editMetadata.creatorLabel"));

			// Publication year
			elements = new ArrayList<>();
			elements.add(ePublicationYear.getTextfield().getText());
			context.metadataObject.mapDublinCoreToMetadata.put("issued", elements);
			context.metadataObject.mapDublinCoreToLabel.put("issued", rm.get("replaydh.wizard.dataversePublisher.editMetadata.publicationYearLabel"));

			// Not used (reserved) metadata fields
			elements = new ArrayList<>();
			elements.add(eIdentifier.getTextfield().getText());
			context.metadataObject.mapDublinCoreToMetadata.put("identifier", elements);
			context.metadataObject.mapDublinCoreToLabel.put("identifier", rm.get("replaydh.wizard.dataversePublisher.editMetadata.identifierLabel"));
			
			elements = new ArrayList<>();
			for (String property : getValuesOfProperty("publisher")) {
				elements.add(property);
			}
			context.metadataObject.mapDublinCoreToMetadata.put("publisher", elements);
			context.metadataObject.mapDublinCoreToLabel.put("publisher", rm.get("replaydh.wizard.dataversePublisher.editMetadata.publisherLabel"));
			
			elements = new ArrayList<>();
			elements.add(eResourceType.getTextfield().getText());
			context.metadataObject.mapDublinCoreToMetadata.put("type", elements);
			context.metadataObject.mapDublinCoreToLabel.put("type", rm.get("replaydh.wizard.dataversePublisher.editMetadata.resourceTypeLabel"));
			
			elements = new ArrayList<>();
			for (String property : getValuesOfProperty("subject")) {
				elements.add(property);
			}
			context.metadataObject.mapDublinCoreToMetadata.put("subject", elements);
			context.metadataObject.mapDublinCoreToLabel.put("subject", rm.get("replaydh.wizard.dataversePublisher.editMetadata.subjectLabel"));

			return FINISH;
		}

		private void refreshNextEnabled() {
			boolean nextEnabled = true;

			nextEnabled &= checkAndUpdateBorder(eCreator.getTextfield());
			nextEnabled &= checkAndUpdateBorder(eTitle.getTextfield());
			nextEnabled &= checkAndUpdateBorder(eDescription.getTextfield());
			nextEnabled &= checkAndUpdateBorder(ePublicationYear.getTextfield());
			
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
			JTextArea messageArea;

			DateFormat format;

			ResourceManager rm = ResourceManager.getInstance();
			
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
			eCreator.create();
			listofkeys.add("creator");
			List<GUIElement> elementslist = new ArrayList<>();
			elementslist.add(eCreator);
			elementsofproperty.put("creator", elementslist);
			propertypanels.put("creator", elementsofproperty.get("creator").get(0).getPanel());
			
			format = new SimpleDateFormat("YYYY");
			JFormattedTextField tfPublicationYear = new JFormattedTextField(format);
			JLabel lPubYear = new JLabel(rm.get("replaydh.wizard.dataversePublisher.editMetadata.publicationYearLabel"));
			ePublicationYear = new GUIElement();
			ePublicationYear.setTextfield(tfPublicationYear);
			ePublicationYear.setLabel(lPubYear);
			tfPublicationYear.setToolTipText("YYYY");
			ePublicationYear.create();
			listofkeys.add("year");
			
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
			ePublisher.create();
			listofkeys.add("publisher");
			elementslist = new ArrayList<>();
			elementslist.add(ePublisher);
			elementsofproperty.put("publisher", elementslist);
			propertypanels.put("publisher", elementsofproperty.get("publisher").get(0).getPanel());
			
			JLabel lSubjects = new JLabel(rm.get("replaydh.wizard.dataversePublisher.editMetadata.subjectLabel"));
			eSubjects = new GUIElement();
			eSubjects.setTextfield(new JTextField());
			eSubjects.setLabel(lSubjects);
			eSubjects.setButton(new JButton());
			eSubjects.getButton().addActionListener(this);
			eSubjects.create();
			listofkeys.add("subject");
			elementslist = new ArrayList<>();
			elementslist.add(eSubjects);
			elementsofproperty.put("subject", elementslist);
			propertypanels.put("subject", elementsofproperty.get("subject").get(0).getPanel());
			
			GuiUtils.prepareChangeableBorder(tfCreator);
			GuiUtils.prepareChangeableBorder(tfTitle);
			GuiUtils.prepareChangeableBorder(tfDescription);
			GuiUtils.prepareChangeableBorder(tfPublicationYear);
			
			messageArea = GuiUtils.createTextArea(rm.get("replaydh.wizard.dataversePublisher.editMetadata.infoMessage"));


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
			
			tfIdentifier.getDocument().addDocumentListener(adapter);
			tfPublisher.getDocument().addDocumentListener(adapter);
			tfResourceType.getDocument().addDocumentListener(adapter);
			
			
			
			builder.columns("pref:grow");
			builder.rows("pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref");
			builder.padding(Paddings.DLU4);
			builder.add(eTitle.getPanel()).xy(1, 1);
			panelRow.put("title", 1);
			builder.add(eDescription.getPanel()).xy(1, 3);
			panelRow.put("description", 3);
			builder.add(propertypanels.get("creator")).xy(1, 5);
			panelRow.put("creator", 5);
			builder.add(ePublicationYear.getPanel()).xy(1, 7);
			panelRow.put("year", 7);
			builder.add(eResourceType.getPanel()).xy(1, 9);
			panelRow.put("resourceType", 9);
			builder.add(eIdentifier.getPanel()).xy(1, 11);
			panelRow.put("identifier", 11);
			builder.add(propertypanels.get("publisher")).xy(1, 13);
			panelRow.put("publisher", 13);
			builder.add(propertypanels.get("subject")).xy(1, 15);
			panelRow.put("subject", 15);
			builder.add(messageArea).xyw(1, 17, 1);
			return builder.build();
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



			int z=0;

			for(GUIElement oneguielement : elementsofproperty.get(metadatapropertyname)) {
				
				if (z == 0) {
					oneguielement.create();
				}
				oneguielement.getButton().addActionListener(this);
				
				if (z > 0) {
					oneguielement.getMinusbutton().addActionListener(this);
				}
				
				propertybuilder.add(oneguielement.getPanel()).xy(1, (z*2)+1);
				
				propertybuilder.appendRows("$nlg, pref");
				
				z++;


			}
			if (elementsofproperty.get(metadatapropertyname).size() > 1) {
				propertybuilder.addSeparator("").xyw(1, ((z*2)+1), 1);
				z++;
			}
			builder.add(propertybuilder.build()).xy(1, panelRow.get(metadatapropertyname));
			Window parentComponent = (Window) SwingUtilities.getAncestorOfClass(Window.class, builder.getPanel());
			parentComponent.pack();
			
		}
		
		public void removeElementFromPanel(String metadatapropertyname) {
			int lastindex=elementsofproperty.get(metadatapropertyname).size()-1;
			elementsofproperty.get(metadatapropertyname).remove(lastindex);
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
						if (buttonNumber > 0) {
							minusbuttonpressed=elementsofproperty.get(propertyname).get(buttonNumber).getMinusbutton();
						}
						if (source == buttonpressed) {
							GUIElement element = createGUIElement(propertyname);
							elementsofproperty.get(propertyname).add(element);
							refreshPanel(propertyname);
							done=true;
							break;
						}
						if (source == minusbuttonpressed) {
							removeElementFromPanel(propertyname);
							done=true;
							break;
						}
					}
					if (done == true) {
						break;
					}
				}
			}
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
		private JTextArea messageArea;

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
					for (String property : entry.getValue()) {
						strMetadata += entry.getKey() + ": " + property + "\n";
					}
				}
			}
			if(!strMetadata.equals("")) {
				strMetadata = strMetadata.substring(0, strMetadata.length() - 1); //remove last "\n"
			} else {
				strMetadata = ResourceManager.getInstance().get("replaydh.wizard.dataversePublisher.finish.noDataMessage");
			}
			metadataArea.setText(strMetadata);

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
			GuiUtils.createTextArea("");
			choosenCollectionArea = GuiUtils.createTextArea("");
			choosenFilesArea = GuiUtils.createTextArea("");
			metadataArea = GuiUtils.createTextArea("");
			messageArea = GuiUtils.createTextArea("");

			//TODO: add separators
			return FormBuilder.create()
					.columns("pref, 6dlu, fill:pref:grow")
					.rows("top:pref, $nlg, top:pref, $nlg, top:pref, $nlg, top:pref, $nlg, top:pref, $nlg, top:pref")
					.padding(Paddings.DLU4)
					.add(new JLabel(rm.get("replaydh.wizard.dataversePublisher.chooseRepository.urlLabel"))).xy(1, 1)
					.add(repositoryUrlArea).xy(3, 1)
					.add(new JLabel(rm.get("replaydh.wizard.dataversePublisher.chooseCollection.collectionLabel"))).xy(1, 3)
					.add(choosenCollectionArea).xy(3, 3)
					.add(new JLabel(rm.get("replaydh.wizard.dataversePublisher.chooseFiles.filesLabel"))).xy(1, 5)
					.add(choosenFilesArea).xy(3, 5)
					.add(new JLabel(rm.get("replaydh.wizard.dataversePublisher.finish.metadataLabel"))).xy(1, 7)
					.add(metadataArea).xy(3, 7)
					.add(messageArea).xyw(1, 11, 3)
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



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
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.swing.SwingUtilities;

import org.eclipse.jgit.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.UserFolder;
import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.core.RDHMainPanel;
import bwfdm.replaydh.ui.helper.AbstractDialogWorker;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.workflow.export.ResourcePublisher;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo;
import bwfdm.replaydh.workflow.export.dspace.DSpacePublisherWizard.DSpacePublisherContext;

/**
 * @author Markus Gärtner
 * @author Volodymyr Kushnarenko
 *
 */
public class DSpacePublisher implements ResourcePublisher {

	protected static final Logger log = LoggerFactory.getLogger(DSpacePublisher.class);

	// DataCite minimum metadata fields:
	//
	// - Identifier
	// - Creator
	// - Title
	// - Publisher
	// - Publication Year
	// - Resource Type


	/**
	 * @see bwfdm.replaydh.workflow.export.ResourcePublisher#publish(bwfdm.replaydh.workflow.export.WorkflowExportInfo)
	 *
	 * For example @see bwfdm.replaydh.ui.core.RDHGui#doShowUI()
	 */
	@Override
	public void publish(WorkflowExportInfo exportInfo) {

		GuiUtils.checkNotEDT();

		RDHEnvironment environment = exportInfo.getEnvironment();
		DSpacePublisherContext context = new DSpacePublisherContext(exportInfo);

		log.info("DSpace publication, calling wizard");

		if(showDSpacePublisherWizard(null, environment, context)) {
			
			// Check if some context field are null
			if((context.getPublicationRepository() == null) || (context.getMetadataObject() == null)) {
				return;
			}

			Window activeWindow = javax.swing.FocusManager.getCurrentManager().getActiveWindow();
			DSpacePublisherWorker worker = new DSpacePublisherWorker(activeWindow, context);
			worker.start();
			
			// Open user submissions page in the browser if publication process was finished successfully 
			// and dialog was not terminated before
			if(worker.isFinishedOK()) {
				openUrlInBrowser(context.getUserSubmissionsURL());
			}
		}
		
		log.info("DSpace publication, wizard is finished");
	}

	public static void openUrlInBrowser(String url) {
		try {
	        Desktop.getDesktop().browse(new URL(url).toURI());
	    } catch (Exception ex) {
	    	log.error("Exception by openning the browser: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
	    }
	}
	
	/**
	 * @see RDHMainPanel#doChangeWorkspace()
	 */
	public static boolean showDSpacePublisherWizard(Component owner, RDHEnvironment environment, DSpacePublisherContext context) {

		boolean wizardDone = false;

		Window ancestorWindow = null;
		if(owner != null) {
			ancestorWindow = SwingUtilities.getWindowAncestor(owner);
		}

		try(Wizard<DSpacePublisherContext> wizard = DSpacePublisherWizard.getWizard(
				ancestorWindow, environment)) {

			wizard.startWizard(context);
			wizardDone = wizard.isFinished() && !wizard.isCancelled();
		}

		return wizardDone;
	}
	
	/**
	 * Dialog with the worker to publish "files+metadata" or only "metadata" to DSpace
	 * @author Volodymyr Kushnarenko
	 */
	public class DSpacePublisherWorker extends AbstractDialogWorker<Boolean, Object> {
		
		protected final Logger log = LoggerFactory.getLogger(DSpacePublisher.class);
			
		private final DSpacePublisherContext context;
		private final String rdhPrefix = "RePlay-DH_publication_"; //TODO: as property?
		private boolean finishOK;

		public DSpacePublisherWorker(Window owner, DSpacePublisherContext context) {
			super(owner, ResourceManager.getInstance().get("replaydh.dialogs.dspacePublication.title"),
					CancellationPolicy.CANCEL_INTERRUPT);

			this.context = requireNonNull(context);
			this.finishOK = false;
		}

		@Override
		protected String getMessage(MessageType messageType, Throwable t) {
			ResourceManager rm = ResourceManager.getInstance();

			switch (messageType) {
			case RUNNING:
				log.info("DSpacePublisherWorker: running");
				return rm.get("replaydh.dialogs.dspacePublication.message");
			case CANCELLED:
				log.info("DSpacePublisherWorker: cancelled");
				return rm.get("replaydh.dialogs.dspacePublication.publicationCancelled");
			case FAILED:
				log.error("DSpacePublisherWorker: failed", t);
				return rm.get("replaydh.dialogs.dspacePublication.publicationFailed", t.getMessage());
			case FINISHED:
				log.info("DSpacePublisherWorker: finished OK");
				return rm.get("replaydh.dialogs.dspacePublication.publicationDone");

			default:
				throw new IllegalArgumentException("Unknown message type: " + messageType);
			}
		}

		/**
		 * @see javax.swing.SwingWorker#doInBackground()
		 */
		@Override
		protected Boolean doInBackground() throws Exception {
			
			PublicationRepository repository = context.getPublicationRepository();
			boolean result = false;
			
			if(!context.getFilesToPublish().isEmpty()) {
				
				// Publication: files + metadata
				
				String workspacePath = context.exportInfo.getEnvironment().getWorkspacePath().toString();
				//TODO: store tmp zip archive in logs or somewhere else?
				String logFolder = context.exportInfo.getEnvironment().getClient().getUserFolder(UserFolder.LOGS).toString();//use log folder to store temporary zip-file
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
				File zipFile = new File(logFolder + FileSystems.getDefault().getSeparator() +  rdhPrefix + timeStamp + ".zip");
								
				try {
					IOUtils.packFilesToZip(context.getFilesToPublish(), zipFile, workspacePath);
				} catch (IOException ex) {
					log.error("Exception by addition of file to zip: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
				}
				
				// Start publication process
				result = repository.publishFileAndMetadata(context.getUserLogin(), context.getCollectionURL(), zipFile, context.getMetadataObject().getMapDoublinCoreToMetadata());
				
				// Delete zip-file
				try {
					FileUtils.delete(zipFile); 				
				} catch (Exception ex) {
					log.error("Exception by deleting the file: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
				}				
				
			} else {
				
				// Publication: metadata only
				
				result = repository.publishMetadata(context.getUserLogin(), context.getCollectionURL(), context.getMetadataObject().getMapDoublinCoreToMetadata());
			}
			
			return Boolean.valueOf(result);
		}
		
		@Override
		protected void doneImpl(Boolean result) {
			
			if(!isCancelled()) {
				this.finishOK = result.booleanValue();
			} else {
				this.finishOK = false;
			}
		}
		
		public boolean isFinishedOK() {
			return finishOK;
		}		
	}
	
	public static String replaceNotAllowedCharacters(final String str) {
		String output = new String(str);

		// Needed for Apache server!
		output = output.replace("\\", "/");
		output = output.replace(" ", "_"); //TODO: replace with "%20" ??
		
		return output;
	}	

}

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

import static java.util.Objects.requireNonNull;

import java.awt.Window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.helper.AbstractDialogWorker;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo;
import bwfdm.replaydh.workflow.export.WorkflowExporter;

/**
 * @author Markus Gärtner
 *
 */
public class WorkflowExportWorker extends AbstractDialogWorker<Boolean, Object> {

	private static final Logger log = LoggerFactory.getLogger(WorkflowExportWorker.class);

	private final WorkflowExporter exporter;
	private final WorkflowExportInfo exportInfo;

	public WorkflowExportWorker(Window owner, WorkflowExporter exporter, WorkflowExportInfo exportInfo) {
		super(owner, ResourceManager.getInstance().get("replaydh.dialogs.workflowExport.title"),
				CancellationPolicy.CANCEL_INTERRUPT);

		this.exporter = requireNonNull(exporter);
		this.exportInfo = requireNonNull(exportInfo);
	}

	@Override
	protected String getMessage(MessageType messageType, Throwable t) {
		ResourceManager rm = ResourceManager.getInstance();

		switch (messageType) {
		case RUNNING:
			log.info("Exporting {}", exportInfo);
			return rm.get("replaydh.dialogs.workflowExport.message");
		case CANCELLED:
			log.info("Export process cancelled by user");
			return rm.get("replaydh.dialogs.workflowExport.exportCancelled");
		case FAILED:
			log.error("Export failed", t);
			return rm.get("replaydh.dialogs.workflowExport.exportFailed", t.getMessage());
		case FINISHED:
			log.info("Export done");
			return rm.get("replaydh.dialogs.workflowExport.exportDone");

		default:
			throw new IllegalArgumentException("Unknown message type: "+messageType);
		}
	}

	/**
	 * @see javax.swing.SwingWorker#doInBackground()
	 */
	@Override
	protected Boolean doInBackground() throws Exception {

		// Now execute the actual export process
		exporter.export(exportInfo);

		return Boolean.TRUE;
	}
}

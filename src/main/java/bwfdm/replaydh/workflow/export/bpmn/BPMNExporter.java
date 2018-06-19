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
package bwfdm.replaydh.workflow.export.bpmn;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;

import java.io.IOException;
import java.io.OutputStream;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo;
import bwfdm.replaydh.workflow.export.WorkflowExporter;

/**
 * @author Markus Gärtner
 *
 */
public class BPMNExporter implements WorkflowExporter {

	/**
	 * @see bwfdm.replaydh.workflow.export.WorkflowExporter#export(bwfdm.replaydh.workflow.export.WorkflowExportInfo, java.io.Writer)
	 */
	@Override
	public void export(WorkflowExportInfo exportInfo) throws IOException {
		checkArgument("Publishing not supported", exportInfo.isExport());

		BPMN_R_Functions functions = new BPMN_R_Functions(exportInfo.getWorkflow());
		functions.showHistory(exportInfo.getTargetStep());

		// For real situations we should have a valid path + filename
		String filename = null;
		if (exportInfo.getOutputResource().getPath() != null) {
			filename = exportInfo.getOutputResource().getPath().getFileName().toString();
		}

		String format = "xml";

		try (OutputStream writer = exportInfo.createOutputStream()){
			functions.writeBpmn(writer, format);
		}
	}

}

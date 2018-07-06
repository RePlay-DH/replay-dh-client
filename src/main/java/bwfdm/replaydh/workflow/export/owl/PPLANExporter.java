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
package bwfdm.replaydh.workflow.export.owl;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;

import java.io.IOException;
import java.io.Writer;

import bwfdm.replaydh.workflow.export.WorkflowExportInfo;
import bwfdm.replaydh.workflow.export.WorkflowExporter;

/**
 * @author Markus Gärtner
 *
 */
public class PPLANExporter implements WorkflowExporter {

	private static final String FORMAT_XML = "RDF/XML";
	private static final String FORMAT_TURTLE = "TURTLE";

	/**
	 * @see bwfdm.replaydh.workflow.export.WorkflowExporter#export(bwfdm.replaydh.workflow.export.WorkflowExportInfo, java.io.Writer)
	 */
	@Override
	public void export(WorkflowExportInfo exportInfo) throws IOException {
		checkArgument("Publishing not supported", exportInfo.isExport());

		PLAN_J_Functions functions = new PLAN_J_Functions(exportInfo.getEnvironment());
		functions.iterateOverSteps(exportInfo.getWorkflow(), exportInfo.getSteps());

		// For real situations we should have a valid path + filename
		String filename = null;
		if (exportInfo.getOutputResource().getPath() != null) {
			filename = exportInfo.getOutputResource().getPath().getFileName().toString();
		}

		String format = null;

		// If possible, try to derive format from file name
		if(filename==null || filename.endsWith(".ttl")) {
			format = FORMAT_TURTLE;
		} else if(filename.endsWith(".xml")) {
			format = FORMAT_XML;
		}

		// Use XML as fallback
		if(format==null) {
			format = FORMAT_XML;
		}

		try (Writer writer = exportInfo.createWriter()){
			functions.writeOnt(writer, format);
		}
	}

}

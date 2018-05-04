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
package bwfdm.replaydh.test.workflow.export.owl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.io.resources.IOResource;
import bwfdm.replaydh.io.resources.VirtualIOResource;
import bwfdm.replaydh.test.RDHTestUtils;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowStep;
import bwfdm.replaydh.workflow.WorkflowUtils;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.Mode;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.ObjectScope;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.Type;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.WorkflowScope;
import bwfdm.replaydh.workflow.export.owl.PROVOExporter;
import bwfdm.replaydh.workflow.impl.DefaultWorkflow;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public class PROVOExporterTest {

	private PROVOExporter exporter;

	@Before
	public void prepare() {
		exporter = new PROVOExporter();
	}

	@SuppressWarnings("unused")
	private static void dumpResource(IOResource resource, PrintStream out) throws IOException {
		try(BufferedReader reader = new BufferedReader(Channels.newReader(
				resource.getReadChannel(), StandardCharsets.UTF_8.newDecoder(), 1024))) {
			String line;
			while((line = reader.readLine()) != null) {
				out.println(line);
			}
		}
	}

	@Test
	public void testSingleResourceAndStep() throws Exception {
		RDHEnvironment environment = RDHTestUtils.createTestEnvironment();
		Workflow workflow = new DefaultWorkflow(WorkflowSchema.getDefaultSchema());
		WorkflowStep step = WorkflowUtils.createStep(workflow, "TestStep-01", true, 3, true, 2);
		workflow.addWorkflowStep(step);

		IOResource resource = new VirtualIOResource(null);

		WorkflowExportInfo exportInfo = WorkflowExportInfo.newExportBuilder()
				.workflow(workflow)
				.environment(environment)
				.encoding(StandardCharsets.UTF_8)
				.workflowScope(WorkflowScope.STEP)
				.objectScope(ObjectScope.OUTPUT)
				.mode(Mode.FILE)
				.type(Type.METADATA)
				.outputResource(resource)
				.targetStep(step)
				.steps(Collections.singleton(step))
				.resources(step.getOutput())
				.build();


		exporter.export(exportInfo);

//		dumpResource(resource, System.out);
	}
}

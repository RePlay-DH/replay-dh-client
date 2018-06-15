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
package bwfdm.replaydh.experiments.bpmn;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;
import bwfdm.replaydh.core.RDHClient;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.io.resources.FileResource;
import bwfdm.replaydh.io.resources.IOResource;
import bwfdm.replaydh.utils.AccessMode;
import bwfdm.replaydh.utils.Options;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.WorkflowStep;
import bwfdm.replaydh.workflow.WorkflowUtils;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.Builder;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.Mode;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.ObjectScope;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.Type;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.WorkflowScope;
import bwfdm.replaydh.workflow.export.bpmn.BPMN_S_Functions;
import bwfdm.replaydh.workflow.impl.DefaultWorkflow;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;


public class JENAAPIPlan {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		final WorkflowSchema schema = WorkflowSchema.getDefaultSchema();

		//File file = new File("./local.owl");

		DefaultWorkflow wf = (DefaultWorkflow) WorkflowUtils.createLinearWorkflow(schema);

		//Workflow wf = WorkflowUtils.createForkedWorkflow(schema);
		wf.setTitle("My workflow");

		System.out.println("Schema: "+wf.getSchema().toString());

		Set<Resource> rs = wf.getActiveStep().getInput();

		Set<WorkflowStep> steps = wf.getNextSteps(wf.getInitialStep());

		for (WorkflowStep step : steps) {
			if (step.getOutputCount() > 0) {
				Set<Resource> set = step.getOutput();
				for(Resource resource : set) {
					System.out.println("Desc: "+resource.getDescription());
				}
			}


		}

		System.out.println("All titles");

		for (WorkflowStep step : wf.getAllSteps()) {
			System.out.println("Title: "+step.getTitle());
		}

		System.out.println("Counter: "+steps.size());

		for (Resource resource : rs) {
			System.out.println("Description: "+resource.getDescription());
		}

		Options options = new Options();
		System.getProperties().forEach((key, value) -> options.put((String) key, value));

		InputStream in = RDHClient.class.getResourceAsStream("default-properties.ini");
		Properties defaultProperties = new Properties();

		try {
			defaultProperties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}

		Builder builder = WorkflowExportInfo.newExportBuilder();
		builder.workflowScope(WorkflowScope.WORKFLOW);
		builder.workflow(wf);
		builder.objectScope(ObjectScope.OUTPUT);
		builder.mode(Mode.FILE);
		builder.type(Type.METADATA);
		RDHClient client;
		try {
			client = new RDHClient(options);
			builder.environment(new RDHEnvironment(client,defaultProperties));
		} catch (RDHLifecycleException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		builder.encoding(Charset.defaultCharset());
		Path output = Paths.get("./src/test/java/bwfdm/replaydh/experiments/bpmn/pontology.owl");
		IOResource resource = new FileResource(output, AccessMode.WRITE);
		try {
			resource.prepare();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		builder.outputResource(resource);
		builder.steps(wf.getAllSteps());
		WorkflowExportInfo exportInfo = null;
		//Set<? extends Identifiable> objects = null;
		for (WorkflowStep step : wf.getAllSteps()) {
			if (!(wf.isInitialStep(step))) {
				System.out.println("Title: "+step.getTitle());
				System.out.println(step.getOutput());
				if(step.getTitle().equals("analyze")) {
					builder.resources(step.getOutput());
					builder.targetStep(step);
					break;
				}
			} else {
				continue;
			}

		}




		exportInfo = builder.build();

		BPMN_S_Functions functions;
		functions = new BPMN_S_Functions();
		System.out.println("Yeah: "+exportInfo.getTargetStep().getTitle());
		
		try {
			functions.iterateOverSteps(exportInfo.getWorkflow(), exportInfo.getSteps());
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try (Writer writer = exportInfo.createWriter()){

			functions.writeOnt(writer,"TURTLE");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}





	}

}

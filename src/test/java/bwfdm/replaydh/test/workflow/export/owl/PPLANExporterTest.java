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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
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
import bwfdm.replaydh.workflow.export.owl.PLAN_J_Functions;
import bwfdm.replaydh.workflow.export.owl.PPLANExporter;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

public class PPLANExporterTest {
	private PPLANExporter exporter;
	
	private static OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

	@Before
	public void prepare() {
		exporter = new PPLANExporter();
	}
	
	private static int individualSize(){

        int size = 0;

        ExtendedIterator<Individual> individuals = om.listIndividuals();

        while (individuals.hasNext() == true) { 
        	
        	size++; 
        	individuals.next();
        }

        return size;
	}
	
	private static String dumpResource(IOResource resource) throws IOException {
		try(BufferedReader reader = new BufferedReader(Channels.newReader(
				resource.getReadChannel(), StandardCharsets.UTF_8.newDecoder(), 1024))) {
			String line;
			StringBuilder everything = new StringBuilder();
			while((line = reader.readLine()) != null) {
				everything.append(line);
			}
			return everything.toString();
		}
	}

	@Test
	public void testNumberOfIndividuals() throws Exception {
		RDHEnvironment environment = RDHTestUtils.createTestEnvironment();
		Workflow workflow = WorkflowUtils.createLinearWorkflow(WorkflowSchema.getDefaultSchema());
		WorkflowStep wfstep = null;
		for (WorkflowStep step : workflow.getAllSteps()) {
			if (!(workflow.isInitialStep(step))) {
				if(step.getTitle().equals("analyze")) {
					wfstep=step;
					break;
				}
			} else {
				continue;
			}

		}
		
		IOResource resource = new VirtualIOResource(null);

		WorkflowExportInfo exportInfo = WorkflowExportInfo.newExportBuilder()
				.workflow(workflow)
				.environment(environment)
				.encoding(StandardCharsets.UTF_8)
				.workflowScope(WorkflowScope.STEP)
				.objectScope(ObjectScope.OUTPUT)
				.mode(Mode.FILE)
				.type(Type.METADATA)
				.targetStep(wfstep)
				.steps(workflow.getAllSteps())
				.outputResource(resource)
				.build();


		exporter.export(exportInfo);
		
		PLAN_J_Functions functions;
		functions = new PLAN_J_Functions();
		
		try (Writer writer = exportInfo.createWriter()){

			functions.writeOnt(writer,"RDF/XML");
		}
		
		InputStream stream = new ByteArrayInputStream(dumpResource(resource).getBytes(StandardCharsets.UTF_8));
		
		om.read(stream, null);
		
		System.out.println("There should be 20 individuals in this configuration");
		
		System.out.println(individualSize()+" individuals found");
	}
}

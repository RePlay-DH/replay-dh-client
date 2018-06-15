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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.instance.DataInput;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataObject;
import org.camunda.bpm.model.bpmn.instance.DataOutput;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.InputSet;
import org.camunda.bpm.model.bpmn.instance.IoSpecification;
import org.camunda.bpm.model.bpmn.instance.OutputSet;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.UserTask;

import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowStep;
import bwfdm.replaydh.workflow.schema.IdentifierType.Uniqueness;

/**
 * 
 * @author Florian Fritze
 *
 */
public class BPMN_R_Functions extends BPMN_Basics {
	
	public BPMN_R_Functions(Workflow workflow) {
		super(nsrpdh,"replay");
		workFlow=workflow;
		process_name=workflow.getTitle().replaceAll(" ", "_");
		process = createElement(definitions, process_name, Process.class);
		om.setNsPrefixes(prefixesmap);
	}
	private Workflow workFlow = null;
	
	private final static String nsrdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private final static String nsxsd = "http://www.w3.org/2001/XMLSchema#";
	private final static String nsrdfs = "http://www.w3.org/2000/01/rdf-schema#";
	private final static String nspplan = "http://purl.org/net/p-plan#";
	private final static String nsprov = "http://www.w3.org/ns/prov#";
	private final static String nsdcterms = "http://purl.org/dc/terms/";
	private final static String nsdatacite = "http://purl.org/spar/datacite/";
	private final static String nsrpdh = "http://www.ub.uni-stuttgart.de/replay#";
	
	private final static Map<String,String> prefixesmap = new HashMap<String,String>();
	
	private final static OntModel som = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
	
	private OntModel om = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM,som);
	
	
	
	private Map<String,String> resources = new HashMap<String,String>();
	
	private Map<String,DataObject> dataObjects = new HashMap<String,DataObject>();
	
	private WorkflowStep exportWorkflowStep = null;
	
	private Process process = null;
	
	private String process_name;
	
	private boolean first_iteration = true;
	
	private boolean in_the_beginning = true;
	
	private IoSpecification ios = null;
	
	private String chosenID = null;
	
	private int numberInput = 0;
	
	private int numberOutput = 0;
	
	private int numberDataObjects = 0;
	
	private DataInput din = null;
	
	private DataOutput dout = null;
	
	private InputSet is = modelInstance.newInstance(InputSet.class);
	
	private OutputSet os = modelInstance.newInstance(OutputSet.class);
	
	private UserTask userTask = null;
	
	private UserTask userTaskBeforeEnd = null;
	
	private UserTask previousUserTask = null;
	
	private StartEvent startEvent = null;
	
	private EndEvent endEvent = null;
	
	private DataObject dao = null;
	
	private Set<Resource> inputResources = null;
	
	private Set<Resource> outputResources = null;
	
	public WorkflowStep getExportWorkflowStep() {
		return exportWorkflowStep;
	}

	public void setExportWorkflowStep(WorkflowStep exportWorkflowStep) {
		this.exportWorkflowStep = exportWorkflowStep;
	}

	static {
		prefixesmap.put("rdf", nsrdf);
		prefixesmap.put("xsd", nsxsd);
		prefixesmap.put("rdfs", nsrdfs);
		prefixesmap.put("p-plan", nspplan);
		prefixesmap.put("prov", nsprov);
		prefixesmap.put("dcterms", nsdcterms);
		prefixesmap.put("datacite", nsdatacite);
		prefixesmap.put("", nsrpdh);
	}
	
	
	/**
	 * Iterates recursively over the workflow graph from a certain workflow step
	 * @param workFlow
	 * @param workFlowStep
	 * @throws MalformedURLException 
	 */
	public void showHistory(WorkflowStep workFlowStep) throws MalformedURLException {
		if (first_iteration) {
			ios = createElement(process, "iospec_"+process_name, IoSpecification.class);
			outputResources = workFlowStep.getOutput();
			ios.getOutputSets().add(os);
			ios.getInputSets().add(is);
			for (Resource outputResource : outputResources) {
				chosenID=getBestID(outputResource.getIdentifiers());
				if ((resources.containsKey(chosenID)) == false) {
					numberOutput++;
					numberDataObjects++;
					resources.put(chosenID, "outputResource"+numberOutput);
					dout = createElement(ios, resources.get(chosenID), DataOutput.class);
					dao = createElement(process, "dataObject"+numberDataObjects, DataObject.class);
					dataObjects.put(chosenID, dao);
					for(Identifier id : outputResource.getIdentifiers()) {
						if (id.getType().getName() != null) {
							dout.setName(id.getId());
						}
					}
					os.getDataOutputRefs().add(dout);
				}
				
			}
			first_iteration=false;
			userTaskBeforeEnd = createElement(process, "Activity_"+workFlowStep.getId(), UserTask.class);
			userTaskBeforeEnd.setName(workFlowStep.getTitle());
			inputResources=workFlowStep.getInput();
			outputResources=workFlowStep.getOutput();
			showResources(workFlowStep, inputResources, outputResources, userTaskBeforeEnd);
		}
		if (workFlow.hasPreviousSteps(workFlowStep)) {
			for (WorkflowStep wfs : workFlow.getPreviousSteps(workFlowStep)) {
				showHistory(wfs);
				if (!(workFlow.isInitialStep(wfs))) {
					/*
					 * Checks if it is the beginning of the process chain, if 'yes' else will be chosen
					 */
					if (in_the_beginning == false) {
						userTask = createElement(process, "Activity_"+wfs.getId(), UserTask.class);
						userTask.setName(wfs.getTitle());
						inputResources=wfs.getInput();
						outputResources=wfs.getOutput();
						showResources(wfs, inputResources, outputResources, userTask);
						createSequenceFlow(process, previousUserTask, userTask);
						previousUserTask=userTask;
					} else {
						startEvent = createElement(process, "start", StartEvent.class);
						userTask = createElement(process, "Activity_"+wfs.getId(), UserTask.class);
						userTask.setName(wfs.getTitle());
						inputResources=wfs.getInput();
						outputResources=wfs.getOutput();
						showResources(wfs, inputResources, outputResources, userTask);
						previousUserTask=userTask;
						createSequenceFlow(process, startEvent, userTask);
						in_the_beginning=false;
					}
				} else {
					if (workFlow.hasNextSteps(wfs)) {
						for (WorkflowStep wfsinit : workFlow.getNextSteps(workFlowStep)) {
							inputResources = wfsinit.getInput();
							for (Resource inputResource : inputResources) {
								chosenID=getBestID(inputResource.getIdentifiers());
								if ((resources.containsKey(chosenID)) == false) {
									numberInput++;
									numberDataObjects++;
									resources.put(chosenID, "inputResource"+numberInput);
									din = createElement(ios, resources.get(chosenID), DataInput.class);
									dao = createElement(process, "dataObject"+numberDataObjects, DataObject.class);
									dataObjects.put(chosenID, dao);
									for(Identifier id : inputResource.getIdentifiers()) {
										if (id.getType().getName() != null) {
											din.setName(id.getId());
										}
									}
									is.getDataInputs().add(din);
								}
							}
							break;
						}
					}
				}
			}
		} 
		if (!(workFlow.hasNextSteps(workFlowStep))) {
			endEvent = createElement(process, "end", EndEvent.class);
			createSequenceFlow(process, previousUserTask, userTaskBeforeEnd);
			createSequenceFlow(process, userTaskBeforeEnd, endEvent);
		}
	}
	
	/**
	 * Writes ontology metadata of a certain workflow step
	 * @param workFlowStep
	 * @param inputResources
	 * @param outputResources
	 * @param stepcounter
	 * @throws MalformedURLException 
	 */
	public void showResources(WorkflowStep workFlowStep, Set<Resource> inputResources, Set<Resource> outputResources, UserTask userTask) throws MalformedURLException {
		String step_id=workFlowStep.getId().replaceAll(" ", "_");
		IoSpecification iosStep = createElement(userTask, "iospec_"+step_id, IoSpecification.class);
		DataInput din = null;
		DataOutput dout = null;
		InputSet isStep = modelInstance.newInstance(InputSet.class);
		OutputSet osStep = modelInstance.newInstance(OutputSet.class);
		iosStep.getOutputSets().add(osStep);
		iosStep.getInputSets().add(isStep);
		DataOutputAssociation douta = null;
		DataInputAssociation dina = null;
		/*
		 * Iterates over all available input resources
		 */
		int inputCounter = 0;
		for (Resource inputResource : inputResources) {
			inputCounter++;
			dina = createElement(userTask, "dataInAssoc_"+inputCounter+"_"+step_id, DataInputAssociation.class);
			chosenID=getBestID(inputResource.getIdentifiers());
			/*
			 * Checks if the chosenID is already known. If not, it puts the new ID to the map
			 * and assigns it a new individual
			 */
			if ((resources.containsKey(chosenID)) == false) {
				numberInput++;
				numberDataObjects++;
				resources.put(chosenID, "inputResource"+numberInput);
				din = createElement(iosStep, "inputResource"+numberInput, DataInput.class);
				dao = createElement(process, "dataObject"+numberDataObjects, DataObject.class);
				dataObjects.put(chosenID, dao);
				for(Identifier id : inputResource.getIdentifiers()) {
					din.setName(id.getId());
				}
				isStep.getDataInputs().add(din);
				dina.getSources().add(dao);
				dina.setTarget(din);
			} else {
				numberInput++;
				din = createElement(iosStep, "inputResource"+numberInput, DataInput.class);
				for(Identifier id : inputResource.getIdentifiers()) {
					din.setName(id.getId());
				}
				isStep.getDataInputs().add(din);
				dina.getSources().add(dataObjects.get(chosenID));
				dina.setTarget(din);
			}
		}
		/*
		 * Iterates over all available output resources
		 */
		int outputCounter = 0;
		for (Resource outputResource : outputResources) {
			outputCounter++;
			douta = createElement(userTask, "dataOutAssoc_"+outputCounter+"_"+step_id, DataOutputAssociation.class);
			chosenID=getBestID(outputResource.getIdentifiers());
			/*
			 * Checks if the chosenID is already known. If not, it puts the new ID to the map
			 * and assigns it a new individual
			 */
			if ((resources.containsKey(chosenID)) == false) {
				numberOutput++;
				numberDataObjects++;
				resources.put(chosenID, "outputResource"+numberOutput);
				dout = createElement(iosStep, "outputResource"+numberOutput, DataOutput.class);
				dao = createElement(process, "dataObject"+numberDataObjects, DataObject.class);
				dataObjects.put(chosenID, dao);
				for(Identifier id : outputResource.getIdentifiers()) {
					dout.setName(id.getId());
				}
				osStep.getDataOutputRefs().add(dout);
				douta.getSources().add(dout);
				douta.setTarget(dao);
			} else {
				numberOutput++;
				dout = createElement(iosStep, "outputResource"+numberOutput, DataOutput.class);
				for(Identifier id : outputResource.getIdentifiers()) {
					dout.setName(id.getId());
				}
				osStep.getDataOutputRefs().add(dout);
				douta.getSources().add(dout);
				douta.setTarget(dataObjects.get(chosenID));
			}
				
		}
	}
	
	/**
	 * Gives the ontology output to the writer
	 * @param writer
	 * @param fileending
	 * @throws IOException
	 */
	public void writeOnt(Writer writer, String fileending) throws IOException {
		Bpmn.validateModel(modelInstance);
		File file;
		file = new File("src/test/java/bwfdm/replaydh/experiments/bpmn/bpmn-model-api.bpmn");
		Bpmn.writeModelToFile(file, modelInstance);
	}
	
	/**
	 * Searches for the best identifier for an entity. 
	 * @param identifiers
	 * @return
	 * @throws MalformedURLException
	 */
	public String getBestID (Set<Identifier> identifiers) {
		Identifier chosenID=null;
		for(Identifier id : identifiers) {
			if (chosenID == null) {
				chosenID=id;
			}
			Uniqueness uniqueness = id.getType().getUniqueness();
			switch (uniqueness) {
			case GLOBALLY_UNIQUE:
				if(id.getType().isStrongerThan(chosenID.getType())) {
					chosenID=id;
					break;
				}
			case ENVIRONMENT_UNIQUE:
				if(id.getType().isStrongerThan(chosenID.getType())) {
					chosenID=id;
					break;
				}
			case LOCALLY_UNIQUE:
				if(id.getType().isStrongerThan(chosenID.getType())) {
					chosenID=id;
					break;
				}
			case AMBIGUOUS:
				if(id.getType().isStrongerThan(chosenID.getType())) {
					chosenID=id;
					break;
				}
			default:
				break;
			}
		}
		return chosenID.getId().toString();
	}
}

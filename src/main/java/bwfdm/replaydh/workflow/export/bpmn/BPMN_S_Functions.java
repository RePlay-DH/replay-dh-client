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

import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.XSD;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.Definitions;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Person;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowStep;
import bwfdm.replaydh.workflow.schema.IdentifierType.Uniqueness;

/**
 * 
 * @author Florian Fritze
 *
 */
public class BPMN_S_Functions extends BPMN_Basics {
	
	public BPMN_S_Functions() {
		super(nsrpdh);
		om.setNsPrefixes(prefixesmap);
		
	}

	//private OWLOntology o = null;
	
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
	
	/**
	 * Classes of Prov-O
	 */
	
	private final static OntClass pOEntity = som.createClass(nsprov+"Entity");
	private final static OntClass pOPerson = som.createClass(nsprov+"Person");
	private final static OntClass pOSoftwareAgent = som.createClass(nsprov+"SoftwareAgent");
	private final static OntClass pOAgent = som.createClass(nsprov+"Agent");
	private final static OntClass pOActivity = som.createClass(nsprov+"Activity");
	private final static OntClass pOAssociation = som.createClass(nsprov+"Association");
	private final static OntClass pOInfluence = som.createClass(nsprov+"Influence");
	private final static OntClass pOPlan = som.createClass(nsprov+"Plan");
	private final static OntClass pOAgentInfluence = som.createClass(nsprov+"AgentInfluence");
	
	
	
	/**
	 * Properties of Prov-O
	 */
	
	private final static ObjectProperty pOinfluenced = som.createObjectProperty(nsprov+"influenced");
	//private final static ObjectProperty pOgenerated = som.createObjectProperty(nsprov+"generated");
	private final static ObjectProperty pOwasAssociatedWith = som.createObjectProperty(nsprov+"wasAssociatedWith");
	//private final static ObjectProperty pOhadActivity = som.createObjectProperty(nsprov+"hadActivity");
	private final static ObjectProperty pOqualifiedAssociation = som.createObjectProperty(nsprov+"qualifiedAssociation");
	private final static ObjectProperty pOagent = som.createObjectProperty(nsprov+"agent");
	
	/**
	 * Classes of P-Plan
	 */
	
	
	private final static OntClass pPStep = som.createClass(nspplan+"Step");
	private final static OntClass pPVariable = som.createClass(nspplan+"Variable");
	private final static OntClass pPPlan = som.createClass(nspplan+"Plan");
	private final static OntClass pPActivity = som.createClass(nspplan+"Activity");
	
	
	/**
	 * Properties of P-Plan
	 */
	
	private final static ObjectProperty pPhasOutputVar = som.createObjectProperty(nspplan+"hasOutputVar");
	private final static ObjectProperty pPhasInputVar = som.createObjectProperty(nspplan+"hasInputVar");
	private final static ObjectProperty pPisPreceededBy = som.createObjectProperty(nspplan+"isPreceededBy");
	private final static ObjectProperty pPisStepOfPlan = som.createObjectProperty(nspplan+"isStepOfPlan");
	private final static ObjectProperty pPcorrespondsToStep = som.createObjectProperty(nspplan+"correspondsToStep");
	
	
	/**
	 * DC and DataCite Properties
	 */
	
	private final static DatatypeProperty dCIdentifier = som.createDatatypeProperty(nsdcterms+"identifier");
	private final static ObjectProperty dCOIdentifier = som.createObjectProperty(nsdcterms+"identifier");
	private final static DatatypeProperty hasDCDesc = som.createDatatypeProperty(nsdcterms+"description");
	
	
	/**
	 * Various already mentioned properties 
	 */
	
	private final static DatatypeProperty hadRole = som.createDatatypeProperty(nsprov+"hadRole");
	private final static DatatypeProperty hasInputVar = som.createDatatypeProperty(nspplan+"hasInputVar");
	private final static DatatypeProperty dcType = som.createDatatypeProperty(nsdcterms+"type");
	private final static DatatypeProperty rdfslabel = som.createDatatypeProperty(nsrdfs+"label");
	
	
	
	static {
		prefixesmap.put("rdf", nsrdf);
		prefixesmap.put("xsd", nsxsd);
		prefixesmap.put("rdfs", nsrdfs);
		prefixesmap.put("p-plan", nspplan);
		prefixesmap.put("prov", nsprov);
		prefixesmap.put("dcterms", nsdcterms);
		prefixesmap.put("datacite", nsdatacite);
		prefixesmap.put("", nsrpdh);
		pOinfluenced.addDomain(pOAgent);
		pOinfluenced.addRange(pOEntity);
		//pOgenerated.addDomain(pOActivity);
		//pOgenerated.addRange(pOEntity);
		pOwasAssociatedWith.addDomain(pOActivity);
		pOwasAssociatedWith.addRange(pOAgent);
		pOAgentInfluence.addSubClass(pOAssociation);
		//pOhadActivity.addDomain(pOInfluence);
		//pOhadActivity.addRange(pOActivity);
		pPhasOutputVar.addDomain(pPStep);
		pPhasOutputVar.addRange(pPVariable);
		pPhasInputVar.addDomain(pPStep);
		pPhasInputVar.addRange(pPVariable);
		pPisPreceededBy.addDomain(pPStep);
		pPisPreceededBy.addRange(pPStep);
		pPisStepOfPlan.addDomain(pPStep);
		pPisStepOfPlan.addRange(pPPlan);
		pPcorrespondsToStep.addDomain(pPActivity);
		pPcorrespondsToStep.addRange(pPStep);
		pOAgent.addSubClass(pOPerson);
		pOAgent.addSubClass(pOSoftwareAgent);
		pOInfluence.addSubClass(pOAgentInfluence);
		pOAgentInfluence.addSubClass(pOAssociation);
		pOEntity.addDisjointWith(pOPerson);
		hadRole.addRange(XSD.xstring);
		hasInputVar.addRange(XSD.xstring);
		rdfslabel.addRange(XSD.xstring);
		dcType.addRange(XSD.xstring);
		hasDCDesc.addRange(XSD.xstring);
		pOAgent.addSubClass(pOPerson);
		pOAgent.addSubClass(pOSoftwareAgent);
		pOqualifiedAssociation.addDomain(pOActivity);
		pOqualifiedAssociation.addRange(pOAssociation);
		pOagent.addDomain(pOAgentInfluence);
		pOagent.addRange(pOAgent);
		pOPlan.addSubClass(pPPlan);
		pPPlan.addSuperClass(pOPlan);
		pOActivity.addSubClass(pPActivity);
	}
	
	private Map<String,String> resources = new HashMap<String,String>();
	
	private Map<String,String> tools = new HashMap<String,String>();
	
	private Map<String,String> persons = new HashMap<String,String>();
	
	
	/**
	 * Iterates over a set of workflowsteps
	 * @param workFlow
	 * @param workFlowStep
	 * @throws MalformedURLException 
	 */
	public void iterateOverSteps(Workflow workFlow, Set<WorkflowStep> workFlowSteps) throws MalformedURLException {
		Process process = createElement(definitions, workFlow.getTitle(), Process.class);
		for (WorkflowStep workFlowStep : workFlowSteps) {
			if (!(workFlow.isInitialStep(workFlowStep))) {
				Set<Resource> inputResources = workFlowStep.getInput();
				Set<Resource> outputResources = workFlowStep.getOutput();
				for (WorkflowStep wfs : workFlowSteps) {
					if (!(workFlow.isInitialStep(wfs))) {
						if (workFlow.isNextStep(workFlowStep, wfs)) {
							Individual nStep = pPStep.createIndividual(nsrpdh+workFlowStep.getId());
							Individual nNextStep = pPStep.createIndividual(nsrpdh+wfs.getId());
							Individual iPPActivity = pPActivity.createIndividual(nsrpdh+"Activity_"+workFlowStep.getId());
							Individual iPPNextActivity = pPActivity.createIndividual(nsrpdh+"Activity_"+wfs.getId());
							nNextStep.addProperty(pPisPreceededBy, nStep);
							iPPActivity.addProperty(pPcorrespondsToStep, nStep);
							iPPNextActivity.addProperty(pPcorrespondsToStep, nNextStep);
						}
					}
				}
				showResources(workFlowStep, inputResources, outputResources);
			}
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
	public void showResources(WorkflowStep workFlowStep, Set<Resource> inputResources, Set<Resource> outputResources) throws MalformedURLException {
		Individual npPlan = pPPlan.createIndividual(nsrpdh+"Workflow");
		int number=0;
		String chosenID=null;
		Individual pPInVar = null;
		Individual pPOutVar = null;
		Individual personEntity = null;
		Individual toolEntity = null;
		Literal personPID = null;
		Individual ipPStep = pPStep.createIndividual(nsrpdh+workFlowStep.getId());
		Individual nIDActivity = pOActivity.createIndividual(nsrpdh+"Activity_"+workFlowStep.getId());
		Literal iDIN = null;
		Literal iDOUT = null;
		Literal activityLabel = null;
		Literal workflowDesc = null;
		Literal idType = null;
		Literal resourceType = null;
		Literal personRole = null;
		Individual association = null;
		if (workFlowStep.getTitle() != null) {
			activityLabel = om.createLiteral(workFlowStep.getTitle().toString());
			ipPStep.addLiteral(rdfslabel, activityLabel);
		}
		if (workFlowStep.getDescription() != null) {
			workflowDesc = om.createLiteral(workFlowStep.getDescription());
			ipPStep.addLiteral(hasDCDesc, workflowDesc);
		}
		/*
		 * Iterates over all available input resources
		 */
		for (Resource inputResource : inputResources) {
			chosenID=getBestID(inputResource.getIdentifiers());
			/*
			 * Checks if the chosenID is already known. If not, it puts the new ID to the map
			 * and assigns it a new individual
			 */
			if ((resources.containsKey(chosenID)) == false) {
				number=resources.size()+1;
				resources.put(chosenID, "resource"+number);
			 
				pPInVar = pPVariable.createIndividual(nsrpdh+resources.get(chosenID));
				pPInVar.addProperty(dcType, "inputResource");
				if (inputResource.getResourceType() != null) {
					resourceType = om.createLiteral(inputResource.getResourceType());
					pPInVar.addProperty(dcType, resourceType);
				}
				
				ipPStep.addOntClass(pPStep);
				ipPStep.addProperty(pPisStepOfPlan, npPlan);
				ipPStep.addProperty(pPhasInputVar, pPInVar);
				for(Identifier id : inputResource.getIdentifiers()) {
					iDIN = om.createLiteral(id.getId().toString());
					if (id.getType().getName() != null) {
						idType = om.createLiteral(id.getType().getName());
						pPInVar.addProperty(dCOIdentifier, om.createResource().addProperty(rdfslabel, iDIN)
									.addProperty(dcType, idType));
					} else {
						pPInVar.addProperty(dCIdentifier,iDIN);
					}
				}
			} else {
				pPInVar = pPVariable.createIndividual(nsrpdh+resources.get(chosenID));
				pPInVar.addProperty(dcType, "inputResource");
				ipPStep.addProperty(pPisStepOfPlan, npPlan);
				ipPStep.addProperty(pPhasInputVar, pPInVar);
			}
		}
		/*
		 * Iterates over all available output resources
		 */
		for (Resource outputResource : outputResources) {
			chosenID=getBestID(outputResource.getIdentifiers());
			/*
			 * Checks if the chosenID is already known. If not, it puts the new ID to the map
			 * and assigns it a new individual
			 */
			if ((resources.containsKey(chosenID)) == false) {
				number=resources.size()+1;
				resources.put(chosenID, "resource"+number);
			
				pPOutVar = pPVariable.createIndividual(nsrpdh+resources.get(chosenID));
				pPOutVar.addProperty(dcType,"outputResource");
				if (outputResource.getResourceType() != null) {
					resourceType = om.createLiteral(outputResource.getResourceType());
					pPOutVar.addProperty(dcType, resourceType);
				}
				ipPStep.addOntClass(pPStep);
				ipPStep.addProperty(pPisStepOfPlan, npPlan);
				ipPStep.addProperty(pPhasOutputVar, pPOutVar);
				for(Identifier id : outputResource.getIdentifiers()) {
					iDOUT = om.createLiteral(id.getId().toString());
					if (id.getType().getName() != null) {
						idType = om.createLiteral(id.getType().getName());
						pPOutVar.addProperty(dCOIdentifier, om.createResource().addProperty(rdfslabel, iDOUT)
									.addProperty(dcType, idType));
					} else {
						pPOutVar.addProperty(dCIdentifier,iDOUT);
					}
				}
			} else {
				pPOutVar = pPVariable.createIndividual(nsrpdh+resources.get(chosenID));
				pPOutVar.addProperty(dcType,"outputResource");
				ipPStep.addProperty(pPisStepOfPlan, npPlan);
				ipPStep.addProperty(pPhasOutputVar, pPOutVar);
			}
		}
		if (!(workFlowStep.getTool() == null)) {
			chosenID=getBestID(workFlowStep.getTool().getIdentifiers());
			if ((tools.containsKey(chosenID)) == false) {
				number=tools.size()+1;
				tools.put(chosenID, "tool"+number);
			
				toolEntity = pOSoftwareAgent.createIndividual(nsrpdh+tools.get(chosenID));
				association = pOAssociation.createIndividual(nsrpdh+"Association_"+tools.get(chosenID));
				nIDActivity.addProperty(pOqualifiedAssociation, association);
				toolEntity.addProperty(pOinfluenced, nIDActivity);
				association.addProperty(pOagent, toolEntity);
				if (workFlowStep.getTool().getParameters() != null) {
					Literal toolParameters = om.createLiteral(workFlowStep.getTool().getParameters());
					toolEntity.addProperty(hasInputVar, toolParameters);
				}
				if (workFlowStep.getTool().getEnvironment() != null) {
					Literal toolEnvironment = om.createLiteral(workFlowStep.getTool().getEnvironment());
					toolEntity.addProperty(hasDCDesc, toolEnvironment);
				}
				ipPStep.addProperty(pOwasAssociatedWith, toolEntity);
				for(Identifier tid : workFlowStep.getTool().getIdentifiers()) {
					Literal toolId = om.createLiteral(tid.getId());
					if (tid.getType().getName() != null) {
						idType = om.createLiteral(tid.getType().getName());
						toolEntity.addProperty(dCOIdentifier, om.createResource().addProperty(rdfslabel, toolId)
									.addProperty(dcType, idType));
					} else {
						toolEntity.addProperty(dCIdentifier,toolId);
					}
				}
			} else {
				toolEntity = pOSoftwareAgent.createIndividual(nsrpdh+tools.get(chosenID));
				toolEntity.addProperty(pOinfluenced, nIDActivity);
				ipPStep.addProperty(pOwasAssociatedWith, toolEntity);
				association = pOAssociation.createIndividual(nsrpdh+"Association_"+tools.get(chosenID));
				nIDActivity.addProperty(pOqualifiedAssociation, association);
			}
		}
		for (Person person : workFlowStep.getPersons()) {
			chosenID=getBestID(person.getIdentifiers());
			if ((persons.containsKey(chosenID)) == false) {
				number=persons.size()+1;
				persons.put(chosenID, "person"+number);
			
				personEntity = pOPerson.createIndividual(nsrpdh+persons.get(chosenID));
				if (!(person.getRole() == null)) {
					personRole = om.createLiteral(person.getRole());
				}
				ipPStep.addProperty(pOwasAssociatedWith, personEntity);
				association = pOAssociation.createIndividual(nsrpdh+"Association_"+persons.get(chosenID));
				nIDActivity.addProperty(pOqualifiedAssociation, association);
				association.addProperty(pOagent, personEntity);
				association.addProperty(hadRole, personRole);
				for(Identifier pid : person.getIdentifiers()) {
					personPID = om.createLiteral(pid.getId().toString());
					if (pid.getType().getName() != null) {
						idType = om.createLiteral(pid.getType().getName());
						personEntity.addProperty(dCOIdentifier, om.createResource().addProperty(rdfslabel, personPID)
									.addProperty(dcType, idType));
					} else {
						personEntity.addProperty(dCIdentifier,personPID);
					}
				}
			} else {
				personEntity = pOPerson.createIndividual(nsrpdh+persons.get(chosenID));
				ipPStep.addProperty(pOwasAssociatedWith, personEntity);
				association = pOAssociation.createIndividual(nsrpdh+"Association_"+persons.get(chosenID));
				nIDActivity.addProperty(pOqualifiedAssociation, association);
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
		om.write(writer,fileending);
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

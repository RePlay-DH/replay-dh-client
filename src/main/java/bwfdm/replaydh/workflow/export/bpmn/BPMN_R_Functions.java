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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.camunda.bpm.model.bpmn.instance.Process;

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
public class BPMN_R_Functions extends BPMN_Basics {
	
	public BPMN_R_Functions(Workflow workflow) {
		super(nsrpdh);
		workFlow=workflow;
		process = createElement(definitions, workflow.getTitle(), Process.class);
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
	private final static ObjectProperty pOgenerated = som.createObjectProperty(nsprov+"generated");
	private final static ObjectProperty pOwasAssociatedWith = som.createObjectProperty(nsprov+"wasAssociatedWith");
	//private final static ObjectProperty pOhadActivity = som.createObjectProperty(nsprov+"hadActivity");
	private final static ObjectProperty pOqualifiedAssociation = som.createObjectProperty(nsprov+"qualifiedAssociation");
	private final static ObjectProperty pOagent = som.createObjectProperty(nsprov+"agent");
	private final static ObjectProperty pOhadPlan = som.createObjectProperty(nsprov+"hadPlan");
	private final static ObjectProperty POwasInfluencedBy = som.createObjectProperty(nsprov+"wasInfluencedBy");
	private final static ObjectProperty POused = som.createObjectProperty(nsprov+"used");
	
	
	
	
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
	private final static DatatypeProperty dcType = som.createDatatypeProperty(nsdcterms+"type");
	private final static DatatypeProperty rdfslabel = som.createDatatypeProperty(nsrdfs+"label");
	
	
	
	
	private Map<String,String> resources = new HashMap<String,String>();
	
	private Map<String,String> tools = new HashMap<String,String>();
	
	private Map<String,String> persons = new HashMap<String,String>();
	
	private WorkflowStep exportWorkflowStep = null;
	
	private Process process = null;
	
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
		pOinfluenced.addDomain(pOAgent);
		pOinfluenced.addRange(pOEntity);
		pOgenerated.addDomain(pOActivity);
		pOgenerated.addRange(pOEntity);
		pOwasAssociatedWith.addDomain(pOActivity);
		pOwasAssociatedWith.addRange(pOAgent);
		pOAgentInfluence.addSubClass(pOAssociation);
		//pOhadActivity.addDomain(pOInfluence);
		//pOhadActivity.addRange(pOActivity);
		pOhadPlan.addDomain(pOAssociation);
		pOhadPlan.addRange(pOPlan);
		pOAgent.addSubClass(pOPerson);
		pOAgent.addSubClass(pOSoftwareAgent);
		pOInfluence.addSubClass(pOAgentInfluence);
		pOAgentInfluence.addSubClass(pOAssociation);
		pOEntity.addDisjointWith(pOPerson);
		hadRole.addRange(XSD.xstring);
		rdfslabel.addRange(XSD.xstring);
		dcType.addRange(XSD.xstring);
		hasDCDesc.addRange(XSD.xstring);
		pOAgent.addSubClass(pOPerson);
		pOAgent.addSubClass(pOSoftwareAgent);
		pOqualifiedAssociation.addDomain(pOActivity);
		pOqualifiedAssociation.addRange(pOAssociation);
		pOagent.addDomain(pOAgentInfluence);
		pOagent.addRange(pOAgent);
		POwasInfluencedBy.addDomain(pOEntity);
		POwasInfluencedBy.addRange(pOEntity);
		POused.addDomain(pOActivity);
		POused.addRange(pOEntity);
	}
	
	
	/**
	 * Iterates recursively over the workflow graph from a certain workflow step
	 * @param workFlow
	 * @param workFlowStep
	 * @throws MalformedURLException 
	 */
	public void showHistory(WorkflowStep workFlowStep) throws MalformedURLException {
		Set<Resource> inputResources = workFlowStep.getInput();
		Set<Resource> outputResources = workFlowStep.getOutput();
		if (workFlow.hasPreviousSteps(workFlowStep)) {
			for (WorkflowStep wfs : workFlow.getPreviousSteps(workFlowStep)) {
				showHistory(wfs);
				showResources(workFlowStep, inputResources, outputResources);
				if (!(workFlow.isInitialStep(wfs))) {
					Individual nActivity = pOActivity.createIndividual(nsrpdh+"Activity_"+workFlowStep.getId());
					Individual nPreviousActivity = pOActivity.createIndividual(nsrpdh+"Activity_"+wfs.getId());
					nPreviousActivity.addProperty(pOinfluenced, nActivity);
				}
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
		Individual nProvPLAN = pOPlan.createIndividual(nsrpdh+"Workflow");
		int number=0;
		String chosenID=null;
		Individual inOEntity = null;
		List<Individual> inids = new ArrayList<Individual>();
		Individual outOEntity = null;
		List<Individual> outids = new ArrayList<Individual>();
		Individual personEntity = null;
		Individual toolEntity = null;
		Literal personPID = null;
		Individual nIDActivity = pOActivity.createIndividual(nsrpdh+"Activity_"+workFlowStep.getId());
		Literal iDIN = null;
		Literal iDOUT = null;
		Literal activityLabel = null;
		Literal workflowDesc = null;
		Literal personRole = null;
		Literal idType = null;
		Literal resourceType = null;
		Individual association = null;
		if (workFlowStep.getTitle() != null) {
			activityLabel = om.createLiteral(workFlowStep.getTitle().toString());
			nIDActivity.addLiteral(rdfslabel, activityLabel);
		}
		if (workFlowStep.getDescription() != null) {
			workflowDesc = om.createLiteral(workFlowStep.getDescription());
			nIDActivity.addLiteral(hasDCDesc, workflowDesc);
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
			 
				inOEntity = pOEntity.createIndividual(nsrpdh+resources.get(chosenID));
				inOEntity.addProperty(dcType, "inputResource");
				inids.add(inOEntity);
				if (inputResource.getResourceType() != null) {
					resourceType = om.createLiteral(inputResource.getResourceType());
					inOEntity.addProperty(dcType, resourceType);
				}
				nIDActivity.addProperty(POused, inOEntity);
				for(Identifier id : inputResource.getIdentifiers()) {
					iDIN = om.createLiteral(id.getId().toString());
					if (id.getType().getName() != null) {
						idType = om.createLiteral(id.getType().getName());
						inOEntity.addProperty(dCOIdentifier, om.createResource().addProperty(rdfslabel, iDIN)
									.addProperty(dcType, idType));
					} else {
						inOEntity.addProperty(dCIdentifier,iDIN);
					}
				}
			} else {
				inOEntity = pOEntity.createIndividual(nsrpdh+resources.get(chosenID));
				inOEntity.addProperty(dcType, "inputResource");
				inids.add(inOEntity);
				nIDActivity.addProperty(POused, inOEntity);
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
			 
				outOEntity = pOEntity.createIndividual(nsrpdh+resources.get(chosenID));
				outOEntity.addProperty(dcType,"outputResource");
				outids.add(outOEntity);
				if (outputResource.getResourceType() != null) {
					resourceType = om.createLiteral(outputResource.getResourceType());
					outOEntity.addProperty(dcType, resourceType);
				}
				nIDActivity.addProperty(pOgenerated, outOEntity);
				for (Individual inid : inids) {
					outOEntity.addProperty(POwasInfluencedBy, inid);
				}
				for(Identifier id : outputResource.getIdentifiers()) {
					iDOUT = om.createLiteral(id.getId().toString());
					if (id.getType().getName() != null) {
						idType = om.createLiteral(id.getType().getName());
						outOEntity.addProperty(dCOIdentifier, om.createResource().addProperty(rdfslabel, iDOUT)
									.addProperty(dcType, idType));
					} else {
						outOEntity.addProperty(dCIdentifier,iDOUT);
					}
				}
			} else {
				outOEntity = pOEntity.createIndividual(nsrpdh+resources.get(chosenID));
				outOEntity.addProperty(dcType,"outputResource");
				outids.add(outOEntity);
				nIDActivity.addProperty(pOgenerated, outOEntity);
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
				association.addProperty(pOhadPlan, nProvPLAN);
				toolEntity.addProperty(pOinfluenced, nIDActivity);
				association.addProperty(pOagent, toolEntity);
				if (workFlowStep.getTool().getParameters() != null) {
					Literal toolParameters = om.createLiteral(workFlowStep.getTool().getParameters());
					toolEntity.addProperty(hasDCDesc, toolParameters);
				}
				if (workFlowStep.getTool().getEnvironment() != null) {
					Literal toolEnvironment = om.createLiteral(workFlowStep.getTool().getEnvironment());
					toolEntity.addProperty(hasDCDesc, toolEnvironment);
				}
				nIDActivity.addProperty(pOwasAssociatedWith, toolEntity);
				for (Individual outid : outids) {
					toolEntity.addProperty(pOgenerated, outid);
				}
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
				association = pOAssociation.createIndividual(nsrpdh+"Association_"+tools.get(chosenID));
				nIDActivity.addProperty(pOqualifiedAssociation, association);
				toolEntity = pOSoftwareAgent.createIndividual(nsrpdh+tools.get(chosenID));
				toolEntity.addProperty(pOinfluenced, nIDActivity);
				nIDActivity.addProperty(pOwasAssociatedWith, toolEntity);
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
				association = pOAssociation.createIndividual(nsrpdh+"Association_"+persons.get(chosenID));
				nIDActivity.addProperty(pOqualifiedAssociation, association);
				association.addProperty(pOhadPlan, nProvPLAN);
				for (Individual outid : outids) {
					personEntity.addProperty(pOinfluenced, outid);
					personEntity.addProperty(pOgenerated, outid);
				}
				nIDActivity.addProperty(pOwasAssociatedWith, personEntity);
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
				association = pOAssociation.createIndividual(nsrpdh+"Association_"+persons.get(chosenID));
				nIDActivity.addProperty(pOqualifiedAssociation, association);
				personEntity = pOPerson.createIndividual(nsrpdh+persons.get(chosenID));
				nIDActivity.addProperty(pOwasAssociatedWith, personEntity);
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

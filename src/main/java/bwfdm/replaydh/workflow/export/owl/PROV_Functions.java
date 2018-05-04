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

import java.io.File;
import java.util.Set;


import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Person;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowStep;
/*import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;

import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.SimpleIRIMapper;*/

public class PROV_Functions {
	
	
	/*public PROV_Functions() {
		
		try {
			o = manager.createOntology(RPDHIRI);
			manager.addAxiom(o, pOPersonSubClass);
			manager.addAxiom(o, pOSoftwareAgentSubClass);
			manager.addAxiom(o, pPPlanSubClass);
			manager.addAxiom(o, pOAgentInfluenceSubClass);
			manager.addAxiom(o, pOAssociationSubClass);
			manager.addAxiom(o, dPOinfluenced);
			manager.addAxiom(o, dPOgenerated);
			manager.addAxiom(o, dPOhadMember);
			manager.addAxiom(o, dPOwasAssociatedWith);
			manager.addAxiom(o, dPOhadRole);
			manager.addAxiom(o, dPOhadActivity);
			manager.addAxiom(o, dPOhadPlan);
			manager.addAxiom(o, rPOinfluenced);
			manager.addAxiom(o, rPOgenerated);
			manager.addAxiom(o, rPOhadMember);
			manager.addAxiom(o, rPOwasAssociatedWith);
			manager.addAxiom(o, rPOhadRole);
			manager.addAxiom(o, rPOhadActivity);
			manager.addAxiom(o, rPOhadPlan);
			manager.addAxiom(o, dPPhasOutputVar);
			manager.addAxiom(o, dPPhasInputVar);
			manager.addAxiom(o, rPPhasOutputVar);
			manager.addAxiom(o, rPPhasInputVar);
			manager.addAxiom(o, dDaCihasIdentifier);
			manager.addAxiom(o, dPPisPreceededBy);
			manager.addAxiom(o, rPPisPreceededBy);
			manager.addAxiom(o, rPPisStepOfPlan);
			ttlFormat.setPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
			ttlFormat.setPrefix("xml", "http://www.w3.org/XML/1998/namespace");
			ttlFormat.setPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
			ttlFormat.setPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
			ttlFormat.setPrefix("p-plan", "http://purl.org/net/p-plan#");
			ttlFormat.setPrefix("prov", "http://www.w3.org/ns/prov#");
			ttlFormat.setPrefix("dcterms", "http://purl.org/dc/terms/");
			ttlFormat.setPrefix("datacite", "http://purl.org/spar/datacite/");
			
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	/*private OWLOntology o = null;
	
	private OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	
	private OWLDataFactory df = manager.getOWLDataFactory();
	
	private IRI RPDHIRI = IRI.create("http://www.ub.uni-stuttgart.de/replay");
	
	private PrefixManager pmRPDH = new DefaultPrefixManager("http://www.ub.uni-stuttgart.de/replay#");
	
	private IRI rDFIRI = IRI.create("http://www.w3.org/1999/02/22-rdf-syntax-ns#");
	private IRI xMLIRI = IRI.create("http://www.w3.org/XML/1998/namespace");
	private IRI xSDIRI = IRI.create("http://www.w3.org/2001/XMLSchema#");
	private IRI rDFSIRI = IRI.create("http://www.w3.org/2000/01/rdf-schema#");
	private IRI pPLANIRI = IRI.create("http://purl.org/net/p-plan#");
	
	private IRI documentIRI = IRI.create("./rpdh.owl");
	
	private File file = new File("./src/test/java/bwfdm/replaydh/owlapi/provo/local.owl");
	
	private SimpleIRIMapper mapper = new SimpleIRIMapper(RPDHIRI, documentIRI);
	
	private OWLDataFactory factory = manager.getOWLDataFactory();
	
	private PrefixManager pmProvO = new DefaultPrefixManager(
			"http://www.w3.org/ns/prov#");
	
	private PrefixManager pmDaCi = new DefaultPrefixManager(
			"http://purl.org/spar/datacite/");
	
	private PrefixManager pmRDFS = new DefaultPrefixManager(
			"http://www.w3.org/2000/01/rdf-schema#");
	
	private OWLXMLDocumentFormat owlxmlFormat = new OWLXMLDocumentFormat();
	
	private TurtleDocumentFormat ttlFormat = new TurtleDocumentFormat();
	
	private int stepcounter = 0;
	
	/**
	 * Classes of Prov-O
	 */
	/*
	private OWLClass pOEntity = factory.getOWLClass(":Entity", pmProvO);
	private OWLClass pOPerson = factory.getOWLClass(":Person", pmProvO);
	private OWLClass pOSoftwareAgent = factory.getOWLClass(":SoftwareAgent", pmProvO);
	private OWLClass pOCollection = factory.getOWLClass(":Collection", pmProvO);
	private OWLClass pOAgent = factory.getOWLClass(":Agent", pmProvO);
	private OWLClass pOActivity = factory.getOWLClass(":Activity", pmProvO);
	private OWLClass pOAssociation = factory.getOWLClass(":Association", pmProvO);
	private OWLClass pOInfluence = factory.getOWLClass(":Influence", pmProvO);
	private OWLClass pORole = factory.getOWLClass(":Role", pmProvO);
	private OWLClass pOPlan = factory.getOWLClass(":Plan", pmProvO);
	private OWLClass pOAgentInfluence = factory.getOWLClass(":AgentInfluence", pmProvO);
	
	private OWLNamedIndividual nPerson = factory.getOWLNamedIndividual(":Person", pmProvO);
	
	
	/**
	 * Properties of Prov-O
	 */
	/*
	private OWLObjectProperty pOinfluenced = factory.getOWLObjectProperty(":influenced", pmProvO);
	private OWLObjectProperty pOgenerated = factory.getOWLObjectProperty(":generated", pmProvO);
	private OWLObjectProperty pOhadMember = factory.getOWLObjectProperty(":hadMember", pmProvO);
	private OWLObjectProperty pOwasAssociatedWith = factory.getOWLObjectProperty(":wasAssociatedWith", pmProvO);
	private OWLObjectProperty pOhadRole = factory.getOWLObjectProperty(":hadRole", pmProvO);
	private OWLObjectProperty pOhadActivity = factory.getOWLObjectProperty(":hadActivity", pmProvO);
	private OWLObjectProperty pOhadPlan = factory.getOWLObjectProperty(":hadPlan", pmProvO);
	
	/**
	 * Domains of Prov-O Classes
	 */
	/*
	private OWLObjectPropertyDomainAxiom dPOinfluenced = factory.getOWLObjectPropertyDomainAxiom(pOinfluenced, pOAgent);
	private OWLObjectPropertyDomainAxiom dPOgenerated = factory.getOWLObjectPropertyDomainAxiom(pOgenerated, pOActivity);
	private OWLObjectPropertyDomainAxiom dPOhadMember = factory.getOWLObjectPropertyDomainAxiom(pOhadMember, pOCollection);
	private OWLObjectPropertyDomainAxiom dPOwasAssociatedWith = factory.getOWLObjectPropertyDomainAxiom(pOwasAssociatedWith, pOActivity);
	private OWLObjectPropertyDomainAxiom dPOhadRole = factory.getOWLObjectPropertyDomainAxiom(pOhadRole, pOInfluence);
	private OWLObjectPropertyDomainAxiom dPOhadActivity = factory.getOWLObjectPropertyDomainAxiom(pOhadActivity, pOInfluence);
	private OWLObjectPropertyDomainAxiom dPOhadPlan = factory.getOWLObjectPropertyDomainAxiom(pOhadPlan, pOAssociation);
	
	/**
	 * Ranges of Prov-O Classes
	 */
	/*
	private OWLObjectPropertyRangeAxiom rPOinfluenced = factory.getOWLObjectPropertyRangeAxiom(pOinfluenced, pOEntity);
	private OWLObjectPropertyRangeAxiom rPOgenerated = factory.getOWLObjectPropertyRangeAxiom(pOgenerated, pOEntity);
	private OWLObjectPropertyRangeAxiom rPOhadMember = factory.getOWLObjectPropertyRangeAxiom(pOhadMember, pOEntity);
	private OWLObjectPropertyRangeAxiom rPOwasAssociatedWith = factory.getOWLObjectPropertyRangeAxiom(pOwasAssociatedWith, pOAgent);
	private OWLObjectPropertyRangeAxiom rPOhadRole = factory.getOWLObjectPropertyRangeAxiom(pOhadRole, pORole);
	private OWLObjectPropertyRangeAxiom rPOhadActivity = factory.getOWLObjectPropertyRangeAxiom(pOhadActivity, pOActivity);
	private OWLObjectPropertyRangeAxiom rPOhadPlan = factory.getOWLObjectPropertyRangeAxiom(pOhadPlan, pOPlan);
	
	
	/**
	 * Classes of P-Plan
	 */
	/*
	private PrefixManager pmPPlan = new DefaultPrefixManager(
			"http://purl.org/net/p-plan#");
	
	private OWLClass pPStep = factory.getOWLClass(":Step", pmPPlan);
	private OWLClass pPVariable = factory.getOWLClass(":Variable", pmPPlan);
	private OWLClass pPPlan = factory.getOWLClass(":Plan", pmPPlan);
	
	private OWLNamedIndividual nPPStep = factory.getOWLNamedIndividual(":Step", pmPPlan);
	private OWLNamedIndividual nPPVariable = factory.getOWLNamedIndividual(":Variable", pmPPlan);
	private OWLNamedIndividual nPPPlan = factory.getOWLNamedIndividual(":Plan", pmPPlan);
	
	
	/**
	 * Properties of P-Plan
	 */
	/*
	private OWLObjectProperty pPhasOutputVar = factory.getOWLObjectProperty(":hasOutputVar", pmPPlan);
	private OWLObjectProperty pPhasInputVar = factory.getOWLObjectProperty(":hasInputVar", pmPPlan);
	private OWLObjectProperty pPisPreceededBy = factory.getOWLObjectProperty(":isPreceededBy", pmPPlan);
	private OWLObjectProperty pPisStepOfPlan = factory.getOWLObjectProperty(":isStepOfPlan", pmPPlan);
	
	/**
	 * Domains of P-Plan Classes
	 */
	/*
	private OWLObjectPropertyDomainAxiom dPPhasOutputVar = factory.getOWLObjectPropertyDomainAxiom(pPhasOutputVar, pPStep);
	private OWLObjectPropertyDomainAxiom dPPhasInputVar = factory.getOWLObjectPropertyDomainAxiom(pPhasInputVar, pPStep);
	private OWLObjectPropertyDomainAxiom dPPisPreceededBy = factory.getOWLObjectPropertyDomainAxiom(pPisPreceededBy, pPStep);
	private OWLObjectPropertyDomainAxiom dPPisStepOfPlan = factory.getOWLObjectPropertyDomainAxiom(pPisStepOfPlan, pPStep);
	
	/**
	 * Ranges of P-Plan Classes
	 */
	/*
	private OWLObjectPropertyRangeAxiom rPPhasOutputVar = factory.getOWLObjectPropertyRangeAxiom(pPhasOutputVar, pPVariable);
	private OWLObjectPropertyRangeAxiom rPPhasInputVar = factory.getOWLObjectPropertyRangeAxiom(pPhasInputVar, pPVariable);
	private OWLObjectPropertyRangeAxiom rPPisPreceededBy = factory.getOWLObjectPropertyRangeAxiom(pPisPreceededBy, pPStep);
	private OWLObjectPropertyRangeAxiom rPPisStepOfPlan = factory.getOWLObjectPropertyRangeAxiom(pPisStepOfPlan, pPPlan);
	
	/**
	 * OWL SubClass Axioms
	 */
	/*
	OWLAxiom pOPersonSubClass = factory.getOWLSubClassOfAxiom(pOPerson, pOAgent);
	OWLAxiom pOSoftwareAgentSubClass = factory.getOWLSubClassOfAxiom(pOSoftwareAgent, pOAgent);
	OWLAxiom pPPlanSubClass = factory.getOWLSubClassOfAxiom(pPPlan, pOPlan);
	OWLAxiom pOAgentInfluenceSubClass = factory.getOWLSubClassOfAxiom(pOAgentInfluence, pOInfluence);
	OWLAxiom pOAssociationSubClass = factory.getOWLSubClassOfAxiom(pOAssociation, pOAgentInfluence);
	
	PrefixManager pmDC = new DefaultPrefixManager(
			"http://purl.org/dc/terms/");
	
	//OWLDataProperty rdfsLiteral = factory.getTopDatatype().asOWLDataProperty();
	/**
	 * DC Properties
	 */
	/*
	private OWLDataProperty dCType = factory.getOWLDataProperty(":type", pmDC);
	private OWLDataProperty dCIdentifier = factory.getOWLDataProperty(":identifier", pmDC);
	private OWLObjectProperty dCDesc = factory.getOWLObjectProperty(":description", pmDC);
	private OWLDataProperty hasDCDesc = factory.getOWLDataProperty(":description", pmDC);
	
	/**
	 * DC Domains
	 */
	
	//private OWLObjectPropertyDomainAxiom DDCType = factory.getOWLObjectPropertyDomainAxiom(DCType,POEntity);
	//private OWLObjectPropertyDomainAxiom dDCDesc = factory.getOWLObjectPropertyDomainAxiom(dCDesc,pOEntity);
		
	/**
	 * Various already mentioned properties 
	 */
	/*
	private OWLDataProperty influenced = factory.getOWLDataProperty(":influenced", pmProvO);
	private OWLDataProperty generated = factory.getOWLDataProperty(":generated", pmProvO);
	private OWLDataProperty hadMember = factory.getOWLDataProperty(":hadMember", pmProvO);
	private OWLDataProperty wasAssociatedWith = factory.getOWLDataProperty(":wasAssociatedWith", pmProvO);
	private OWLDataProperty hadRole = factory.getOWLDataProperty(":hadRole", pmProvO);
	private OWLDataProperty hadActivity = factory.getOWLDataProperty(":hadActivity", pmProvO);
	private OWLDataProperty hadPlan = factory.getOWLDataProperty(":hadPlan", pmProvO);
	private OWLDataProperty hasIdentifier = factory.getOWLDataProperty(":hasIdentifier", pmDaCi);
	private OWLDataProperty hasInputVar = factory.getOWLDataProperty(":hasInputVar", pmPPlan);
	private OWLDataProperty hasOutputVar = factory.getOWLDataProperty(":hasOutputVar", pmPPlan);
	private OWLDataProperty isStepOfPlan = factory.getOWLDataProperty(":isStepOfPlan", pmPPlan);
	
	private OWLDataProperty rdfslabel = factory.getOWLDataProperty(":label", pmRDFS);
	
	
	/**
	 * DataCite Classes
	 */
	/*
	private OWLClass daCiResourceID = factory.getOWLClass(":ResourceIdentifier", pmDaCi);
	private OWLClass daCiAgentID = factory.getOWLClass(":AgentIdentifier", pmDaCi);
	
	private OWLNamedIndividual daCiResourceIDN = factory.getOWLNamedIndividual(":ResourceIdentifier", pmDaCi);
	private OWLNamedIndividual daCiAgentIDN = factory.getOWLNamedIndividual(":AgentIdentifier", pmDaCi);
	
	/**
	 * DataCite Properties
	 */
	/*
	private OWLObjectProperty daCihasIdentifier = factory.getOWLObjectProperty(":hasIdentifier", pmDaCi);
	private OWLDataProperty dataCitehasIdentifier = factory.getOWLDataProperty(":hasIdentifier", pmDaCi);
	
	
	
	/**
	 * DataCite Domains
	 */
	/*
	private OWLObjectPropertyDomainAxiom dDaCihasIdentifier = factory.getOWLObjectPropertyDomainAxiom(daCihasIdentifier, pOEntity);
	
	/**
	 * DataCite Ranges
	 */
	/*
	private OWLObjectPropertyRangeAxiom rDaCihasIdentifier = factory.getOWLObjectPropertyRangeAxiom(daCihasIdentifier, daCiResourceID);
	
	
	private boolean found = false;
	
	private WorkflowStep exportWorkflowStep = null;
	
	public WorkflowStep getExportWorkflowStep() {
		return exportWorkflowStep;
	}

	public void setExportWorkflowStep(WorkflowStep exportWorkflowStep) {
		this.exportWorkflowStep = exportWorkflowStep;
	}

	public boolean isFound() {
		return found;
	}

	public void setFound(boolean found) {
		this.found = found;
	}
	
	
	public void showHistory(Workflow workFlow, WorkflowStep workFlowStep) {
		Set<Resource> inputResources = workFlowStep.getInput();
		Set<Resource> outputResources = workFlowStep.getOutput();
		manager.getIRIMappers().add(mapper);
		if (workFlow.hasPreviousSteps(workFlowStep)) {
			for (WorkflowStep wfs : workFlow.getPreviousSteps(workFlowStep)) {
				showHistory(workFlow,wfs);
				stepcounter++;
				showResources(workFlowStep, inputResources, outputResources, stepcounter);
				if (!(workFlow.isInitialStep(wfs))) {
					OWLNamedIndividual nStep = factory.getOWLNamedIndividual(":Activity"+stepcounter, pmRPDH);
					OWLNamedIndividual nPreviousStep = factory.getOWLNamedIndividual(":Activity"+(stepcounter-1), pmRPDH);
					OWLAxiom hasinfluenced = factory.getOWLObjectPropertyAssertionAxiom(pOinfluenced.asObjectPropertyExpression(), nPreviousStep, nStep);
					manager.addAxiom(o, hasinfluenced);
					OWLAxiom isPreceededBy = factory.getOWLObjectPropertyAssertionAxiom(pPisPreceededBy.asObjectPropertyExpression(), nStep, nPreviousStep);
					manager.addAxiom(o, isPreceededBy);
				}
				try {
					manager.saveOntology(o, ttlFormat, IRI.create(file.toURI()));
				} catch (OWLOntologyStorageException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} 
	}
	
	public void showResources(WorkflowStep workFlowStep, Set<Resource> inputResources, Set<Resource> outputResources, int stepcounter) {
		System.out.println("Title of the Step: "+workFlowStep.getTitle());
		OWLNamedIndividual nProvPLAN = factory.getOWLNamedIndividual(":Workflow",pmRPDH);
		OWLClassAssertionAxiom planAssertion = factory.getOWLClassAssertionAxiom(pOPlan, nProvPLAN);
		manager.addAxiom(o, planAssertion);
		int resourceCounter=0;
		int personCounter=0;
		int toolCounter=0;
		OWLNamedIndividual resource = null;
		OWLNamedIndividual personEntity = null;
		OWLNamedIndividual toolEntity = null;
		OWLLiteral personPID = null;
		OWLNamedIndividual nIDActivity = null;
		OWLLiteral iDIN = null;
		OWLNamedIndividual nOUTCollection = null;
		for (Resource inputResource : inputResources) {
			System.out.println("Input Resources Desc: "+inputResource.getDescription());
			System.out.println("Input Resources Type: "+inputResource.getType());
			
			OWLLiteral activityLabel = factory.getOWLLiteral(workFlowStep.getTitle().toString());
			nIDActivity = factory.getOWLNamedIndividual(":Activity"+stepcounter, pmRPDH);
			OWLAxiom hasLabelAxiom = factory.getOWLDataPropertyAssertionAxiom(rdfslabel.asDataPropertyExpression(), nIDActivity, activityLabel);
			manager.addAxiom(o, hasLabelAxiom);
			OWLLiteral workflowDesc = factory.getOWLLiteral(workFlowStep.getDescription());
			System.out.println("WF Desc:"+workFlowStep.getDescription());
			OWLAxiom hasDescription = factory.getOWLDataPropertyAssertionAxiom(hasDCDesc.asDataPropertyExpression(), nIDActivity, workflowDesc);
			manager.addAxiom(o, hasDescription);
			OWLClassAssertionAxiom activityAssertion = factory.getOWLClassAssertionAxiom(pOActivity, nIDActivity);
			manager.addAxiom(o, activityAssertion);
			resourceCounter++;
			for(Identifier id : inputResource.getIdentifiers()) {
				resource = factory.getOWLNamedIndividual(":InputResource_"+workFlowStep.getTitle().toString()+"_"+resourceCounter, pmRPDH);
				OWLClassAssertionAxiom resourceIsPPVar = factory.getOWLClassAssertionAxiom(pPVariable, resource);
				manager.addAxiom(o, resourceIsPPVar);
				iDIN = factory.getOWLLiteral(id.getId().toString());
					
				System.out.println("Input Resources ID: "+id.getId());
				OWLAxiom hadActivityAxiom = factory.getOWLObjectPropertyAssertionAxiom(pOhadActivity.asObjectPropertyExpression(), nProvPLAN, nIDActivity);
				manager.addAxiom(o, hadActivityAxiom);
				OWLClassAssertionAxiom pPlanAssertion = factory.getOWLClassAssertionAxiom(pPStep, nIDActivity);
				manager.addAxiom(o,pPlanAssertion);
				OWLAxiom isStepOfPlanAxiom = factory.getOWLObjectPropertyAssertionAxiom(pPisStepOfPlan.asObjectPropertyExpression(), nIDActivity, nProvPLAN);
				manager.addAxiom(o, isStepOfPlanAxiom);
				OWLNamedIndividual nCollection = factory.getOWLNamedIndividual(":Activity"+stepcounter+"_Input", pmRPDH);
				OWLClassAssertionAxiom collectionAssertion = factory.getOWLClassAssertionAxiom(pOCollection, nCollection);
				manager.addAxiom(o, collectionAssertion);
				OWLAxiom hasInputVarAxiom = factory.getOWLObjectPropertyAssertionAxiom(pPhasInputVar.asObjectPropertyExpression(), nIDActivity, resource);
				manager.addAxiom(o, hasInputVarAxiom);
				OWLAxiom hadMemberAxiom = factory.getOWLObjectPropertyAssertionAxiom(pOhadMember.asObjectPropertyExpression(), nCollection, resource);
				manager.addAxiom(o, hadMemberAxiom);
				OWLAxiom hasIdentifier = factory.getOWLDataPropertyAssertionAxiom(dataCitehasIdentifier.asDataPropertyExpression(), resource, iDIN);
				manager.addAxiom(o, hasIdentifier);
				
				if (!(outputResources.isEmpty())) {
					nOUTCollection = factory.getOWLNamedIndividual(":Activity"+stepcounter+"_Output", pmRPDH);
					OWLAxiom influencedAxiom = factory.getOWLObjectPropertyAssertionAxiom(pOinfluenced.asObjectPropertyExpression(), nCollection, nOUTCollection);
					manager.addAxiom(o, influencedAxiom);
				}
			}
			
			
		}
		if (!(workFlowStep.getTool() == null)) {
			toolCounter++;
			toolEntity = factory.getOWLNamedIndividual(":Tool_"+workFlowStep.getTitle().toString()+"_"+toolCounter, pmRPDH);
			for(Identifier tid : workFlowStep.getTool().getIdentifiers()) {
				OWLAxiom hasinfluenced = factory.getOWLObjectPropertyAssertionAxiom(pOinfluenced.asObjectPropertyExpression(), toolEntity, nIDActivity);
				manager.addAxiom(o, hasinfluenced);
				OWLLiteral toolParameters = factory.getOWLLiteral(workFlowStep.getTool().getParameters());
				OWLAxiom hasInputParameters = factory.getOWLDataPropertyAssertionAxiom(hasInputVar.asDataPropertyExpression(), toolEntity, toolParameters);
				manager.addAxiom(o, hasInputParameters);
				OWLLiteral toolEnvironment = factory.getOWLLiteral(workFlowStep.getTool().getEnvironment());
				OWLAxiom hasToolEnviroment = factory.getOWLDataPropertyAssertionAxiom(hasDCDesc.asDataPropertyExpression(), toolEntity, toolEnvironment);
				manager.addAxiom(o, hasToolEnviroment);
				OWLAxiom wasAssociatedWith = factory.getOWLObjectPropertyAssertionAxiom(pOwasAssociatedWith.asObjectPropertyExpression(), nIDActivity, toolEntity);
				manager.addAxiom(o, wasAssociatedWith);
				OWLLiteral toolId = factory.getOWLLiteral(tid.getId());
				OWLAxiom hasIdentifier = factory.getOWLDataPropertyAssertionAxiom(dataCitehasIdentifier.asDataPropertyExpression(), toolEntity, toolId);
				manager.addAxiom(o, hasIdentifier);
				
			}
		}
		for (Person person : workFlowStep.getPersons()) {
			personCounter++;
			personEntity = factory.getOWLNamedIndividual(":Person_"+workFlowStep.getTitle().toString()+"_"+personCounter, pmRPDH);
			for(Identifier pid : person.getIdentifiers()) {
				System.out.println("Inv Persons: "+pid.getId());
				personPID = factory.getOWLLiteral(pid.getId().toString());
				OWLAxiom hasIdentifier = factory.getOWLDataPropertyAssertionAxiom(dataCitehasIdentifier.asDataPropertyExpression(), personEntity, personPID);
				manager.addAxiom(o, hasIdentifier);
				OWLAxiom hasinfluenced = factory.getOWLObjectPropertyAssertionAxiom(pOinfluenced.asObjectPropertyExpression(), personEntity, nOUTCollection);
				manager.addAxiom(o, hasinfluenced);
				OWLAxiom wasAssociatedWith = factory.getOWLObjectPropertyAssertionAxiom(pOwasAssociatedWith.asObjectPropertyExpression(), nIDActivity, personEntity);
				manager.addAxiom(o, wasAssociatedWith);
			}
		}
		resourceCounter=0;
		personCounter=0;
		for (Resource outputResource : outputResources) {
			System.out.println("Output Resources Desc: "+outputResource.getDescription());
			System.out.println("Output Resources Type: "+outputResource.getType());
			OWLLiteral iDOUT = null;
			OWLNamedIndividual nCollection = null;
			OWLLiteral activityLabel = factory.getOWLLiteral(workFlowStep.getTitle().toString());
			nIDActivity = factory.getOWLNamedIndividual(":Activity"+stepcounter, pmRPDH);
			OWLAxiom hasLabelAxiom = factory.getOWLDataPropertyAssertionAxiom(rdfslabel.asDataPropertyExpression(), nIDActivity, activityLabel);
			manager.addAxiom(o, hasLabelAxiom);
			OWLClassAssertionAxiom activityAssertion = factory.getOWLClassAssertionAxiom(pOActivity, nIDActivity);
			manager.addAxiom(o, activityAssertion);
			resourceCounter++;
			for(Identifier id : outputResource.getIdentifiers()) {
				resource = factory.getOWLNamedIndividual(":OutputResource_"+workFlowStep.getTitle().toString()+"_"+resourceCounter, pmRPDH);
				OWLClassAssertionAxiom resourceIsPPVar = factory.getOWLClassAssertionAxiom(pPVariable, resource);
				manager.addAxiom(o, resourceIsPPVar);
				iDOUT = factory.getOWLLiteral(id.getId().toString());
				System.out.println("Output Resources ID: "+id.getId());
				nCollection = factory.getOWLNamedIndividual(":Activity"+stepcounter+"_Output", pmRPDH);
				OWLClassAssertionAxiom collectionAssertion = factory.getOWLClassAssertionAxiom(pOCollection, nCollection);
				manager.addAxiom(o, collectionAssertion);
				OWLAxiom hadActivityAxiom = factory.getOWLObjectPropertyAssertionAxiom(pOhadActivity.asObjectPropertyExpression(), nProvPLAN, nIDActivity);
				manager.addAxiom(o, hadActivityAxiom);
				OWLAxiom hadMemberAxiom = factory.getOWLObjectPropertyAssertionAxiom(pOhadMember.asObjectPropertyExpression(), nCollection, resource);
				manager.addAxiom(o, hadMemberAxiom);
				OWLAxiom hasgenerated = factory.getOWLObjectPropertyAssertionAxiom(pOgenerated.asObjectPropertyExpression(), nIDActivity, resource);
				manager.addAxiom(o, hasgenerated);
				OWLAxiom hasIdentifier = factory.getOWLDataPropertyAssertionAxiom(dataCitehasIdentifier.asDataPropertyExpression(), resource, iDOUT);
				manager.addAxiom(o, hasIdentifier);
				OWLClassAssertionAxiom pPlanAssertion = factory.getOWLClassAssertionAxiom(pPStep, nIDActivity);
				manager.addAxiom(o,pPlanAssertion);
				
				OWLAxiom isStepOfPlanAxiom = factory.getOWLObjectPropertyAssertionAxiom(pPisStepOfPlan.asObjectPropertyExpression(), nIDActivity, nProvPLAN);
				manager.addAxiom(o, isStepOfPlanAxiom);
				
				
				OWLAxiom hasOutputVarAxiom = factory.getOWLObjectPropertyAssertionAxiom(pPhasOutputVar.asObjectPropertyExpression(), nIDActivity, resource);
				manager.addAxiom(o, hasOutputVarAxiom);
				}
			}
		}*/
	}


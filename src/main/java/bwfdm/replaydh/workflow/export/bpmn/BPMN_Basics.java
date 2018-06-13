package bwfdm.replaydh.workflow.export.bpmn;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.Definitions;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

/**
 * 
 * @author Florian Fritze
 *
 */
public class BPMN_Basics {
	
	BPMN_Basics(String namespaceURI) {
		definitions.setTargetNamespace(namespaceURI);
		definitions.setId("replay");
		modelInstance.setDefinitions(definitions);
	}
	
	protected BpmnModelInstance modelInstance = Bpmn.createEmptyModel();
	protected Definitions definitions = modelInstance.newInstance(Definitions.class);
	
	/**
	 * Creates sequence flow; copied from https://docs.camunda.org/manual/7.7/user-guide/model-api/bpmn-model-api/create-a-model/
	 * @param process
	 * @param from
	 * @param to
	 * @return
	 */
	protected SequenceFlow createSequenceFlow(Process process, FlowNode from, FlowNode to) {
		  String identifier = from.getId() + "-" + to.getId();
		  SequenceFlow sequenceFlow = createElement(process, identifier, SequenceFlow.class);
		  process.addChildElement(sequenceFlow);
		  sequenceFlow.setSource(from);
		  from.getOutgoing().add(sequenceFlow);
		  sequenceFlow.setTarget(to);
		  to.getIncoming().add(sequenceFlow);
		  return sequenceFlow;
	}
	/**
	 * Creates an element in the process instance; copied from https://docs.camunda.org/manual/7.7/user-guide/model-api/bpmn-model-api/create-a-model/
	 * @param parentElement
	 * @param id
	 * @param elementClass
	 * @return
	 */
	protected <T extends BpmnModelElementInstance> T createElement(BpmnModelElementInstance parentElement, String id, Class<T> elementClass) {
		  T element = modelInstance.newInstance(elementClass);
		  element.setAttributeValue("id", id, true);
		  parentElement.addChildElement(element);
		  return element;
	}
}

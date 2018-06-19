package bwfdm.replaydh.experiments.bpmn;

import java.io.File;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.BpmnModelElementInstance;
import org.camunda.bpm.model.bpmn.instance.DataInput;
import org.camunda.bpm.model.bpmn.instance.DataInputAssociation;
import org.camunda.bpm.model.bpmn.instance.DataOutput;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.Definitions;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ExclusiveGateway;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.InputSet;
import org.camunda.bpm.model.bpmn.instance.IoSpecification;
import org.camunda.bpm.model.bpmn.instance.OutputSet;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.ServiceTask;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.bpmn.instance.Process;


public class BPMNExporter2 {
	
	static BpmnModelInstance modelInstance = Bpmn.createEmptyModel();
	
	static String namespaceUri = "https://www.ub.uni-stuttgart.de/replay/";
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Definitions definitions = modelInstance.newInstance(Definitions.class);
		definitions.setTargetNamespace("https://www.ub.uni-stuttgart.de/replay/");
		definitions.setId("replay");
		modelInstance.setDefinitions(definitions);

		// create the process
		Process process = createElement(definitions, "process-with-one-task", Process.class);

		// create start event, user task and end event
		StartEvent startEvent = createElement(process, "start", StartEvent.class);
		UserTask task1 = createElement(process, "task1", UserTask.class);
		ServiceTask task2a = createElement(process, "task2a", ServiceTask.class);
		task2a.setName("Task 2A");
		ServiceTask task2b = createElement(process, "task2b", ServiceTask.class);
		task2b.setName("Task 2B");
		task1.setName("User Task");
		EndEvent endEventa = createElement(process, "enda", EndEvent.class);
		EndEvent endEventb = createElement(process, "endb", EndEvent.class);
		
		ExclusiveGateway gateway = createElement(process, "gateway1", ExclusiveGateway.class);
		gateway.setName("Gateway 1");

		// create the connections between the elements
		createSequenceFlow(process, startEvent, task1);
		createSequenceFlow(process, task1, gateway);
		createSequenceFlow(process, gateway, task2a);
		createSequenceFlow(process, gateway, task2b);
		createSequenceFlow(process, task2a, endEventa);
		createSequenceFlow(process, task2b, endEventb);

		// properties
		IoSpecification ios = createElement(process, "ios", IoSpecification.class);
		
		DataOutputAssociation douta = createElement(task2b, "douta", DataOutputAssociation.class);
		DataInputAssociation dina = createElement(task2b, "dina", DataInputAssociation.class);
		
		InputSet is =  modelInstance.newInstance(InputSet.class);
		is.setId("is");
		OutputSet os = modelInstance.newInstance(OutputSet.class);
		os.setId("os");
		
		DataInput din = createElement(ios, "din", DataInput.class);
		DataOutput dout = createElement(ios, "dout", DataOutput.class);
		
		douta.setTarget(dout);
		douta.getSources().add(dout);
		dina.setTarget(din);
		dina.getSources().add(din);
		
		ios.getInputSets().add(is);
		ios.getOutputSets().add(os);
		
		is.getDataInputs().add(din);
		os.getDataOutputRefs().add(dout);
		
		task2b.setIoSpecification(ios);
		
		// validate and write model to file
		Bpmn.validateModel(modelInstance);
		File file;
		file = new File("src/test/java/bwfdm/replaydh/experiments/bpmn/bpmn-model-api.bpmn");
		Bpmn.writeModelToFile(file, modelInstance);
		
	}
	
	public static SequenceFlow createSequenceFlow(Process process, FlowNode from, FlowNode to) {
		  String identifier = from.getId() + "-" + to.getId();
		  SequenceFlow sequenceFlow = createElement(process, identifier, SequenceFlow.class);
		  process.addChildElement(sequenceFlow);
		  sequenceFlow.setSource(from);
		  from.getOutgoing().add(sequenceFlow);
		  sequenceFlow.setTarget(to);
		  to.getIncoming().add(sequenceFlow);
		  return sequenceFlow;
		}
	
	protected static <T extends BpmnModelElementInstance> T createElement(BpmnModelElementInstance parentElement, String id, Class<T> elementClass) {
		  T element = modelInstance.newInstance(elementClass);
		  element.setAttributeValue("id", id, true);
		  parentElement.addChildElement(element);
		  return element;
		}
}

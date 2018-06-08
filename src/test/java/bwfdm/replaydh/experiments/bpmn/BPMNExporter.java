package bwfdm.replaydh.experiments.bpmn;

import java.io.File;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.builder.ExclusiveGatewayBuilder;
import org.camunda.bpm.model.bpmn.builder.ProcessBuilder;
import org.camunda.bpm.model.bpmn.builder.ServiceTaskBuilder;


public class BPMNExporter {
	static ProcessBuilder modelInstance = Bpmn.createExecutableProcess();
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		ServiceTaskBuilder task=modelInstance.startEvent().id("start").sequenceFlowId("startflow").serviceTask().name("Process Payment").id("t0").sequenceFlowId("flow0");
				
		task=task.serviceTask().name("Task1").id("t1").sequenceFlowId("flow1");
		
		ExclusiveGatewayBuilder gateway=task.exclusiveGateway("g1").name("gateway1").id("g1").sequenceFlowId("flowg1");
		
		
		gateway=gateway.condition("Yes", "#{choice}").sequenceFlowId("flowg1y");
		
		ServiceTaskBuilder task1=gateway.serviceTask().name("Task1a").id("t1a").sequenceFlowId("flow1a");
		
		gateway=gateway.condition("No", "#{!choice}").sequenceFlowId("flowg1n");
		
		ServiceTaskBuilder task2=gateway.serviceTask().name("Task1b").id("t1b").sequenceFlowId("flow1b");
		
		task2=task2.serviceTask().name("Task2b").id("t2b").sequenceFlowId("flow2b");
		
		task2.endEvent().id("end2");
		
		task1.endEvent().id("end1");
		
		modelInstance.name("Process Payment");
		
		File file = null;
		file = new File("src/test/java/bwfdm/replaydh/experiments/bpmn/bpmn-model-api.bpmn");
		
		Bpmn.validateModel(modelInstance.done());
		Bpmn.writeModelToFile(file,modelInstance.done());

	}
}

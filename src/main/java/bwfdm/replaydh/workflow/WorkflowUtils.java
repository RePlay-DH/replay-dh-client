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
package bwfdm.replaydh.workflow;

import static java.util.Objects.requireNonNull;

import java.util.function.Predicate;

import bwfdm.replaydh.workflow.Identifiable.Type;
import bwfdm.replaydh.workflow.impl.DefaultPerson;
import bwfdm.replaydh.workflow.impl.DefaultResource;
import bwfdm.replaydh.workflow.impl.DefaultTool;
import bwfdm.replaydh.workflow.impl.DefaultWorkflow;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public class WorkflowUtils {

	// WORKFLOW HELPERS

	/**
	 * If {@code step} has exactly {@code 1} previous step, returns
	 * that one, otherwise returns {@code null}.
	 */
	public static WorkflowStep previous(WorkflowStep step) {
		Workflow workflow = step.getWorkflow();
		return workflow.getPreviousStepCount(step)==1 ?
				workflow.getPreviousStep(step, 0) : null;
	}

	/**
	 * If {@code step} has exactly {@code 1} next step, returns
	 * that one, otherwise returns {@code null}.
	 */
	public static WorkflowStep next(WorkflowStep step) {
		Workflow workflow = step.getWorkflow();
		return workflow.getNextStepCount(step)==1 ?
				workflow.getNextStep(step, 0) : null;
	}

	/**
	 * Check to see if a step can be hidden. This is the case
	 * when it has only {@code 1} previous step connected to it.
	 */
	public static boolean canHide(WorkflowStep step) {
		return !isInitial(step)
				&& step.getWorkflow().getPreviousStepCount(step)==1;
	}

	/**
	 * Check to see if the given step is a pipe, i.e.
	 * has only {@code 1} outgoing edge.
	 */
	public static boolean isPipe(WorkflowStep step) {
		return !isInitial(step)
				&& step.getWorkflow().getNextStepCount(step)==1;
	}

	public static boolean isInitial(WorkflowStep step) {
		return step.getWorkflow().isInitialStep(step);
	}

	public static boolean canCompress(WorkflowStep step) {
		return (canHide(step) && isPipe(previous(step)))
				|| (isPipe(step) && canHide(next(step)));
	}

	public static boolean isLeaf(WorkflowStep step) {
		return step.getWorkflow().getNextStepCount(step)==0;
	}

	public static boolean isBranch(WorkflowStep step) {
		return step.getWorkflow().getNextStepCount(step)>1;
	}

	public static boolean isMerge(WorkflowStep step) {
		return step.getWorkflow().getPreviousStepCount(step)>1;
	}

	public static boolean isEmpty(Workflow workflow) {
		return !workflow.hasNextSteps(workflow.getInitialStep());
	}

	// WORKFLOW TRAVERSAL

	/**
	 * Traverse the workflow graph forward until a step is found that
	 * fulfills the given {@code check}.
	 * When a step is reached that does not fulfill the check and either
	 * has {@code 0} or more than {@code 1} outgoing edge, this method
	 * returns {@code null}.
	 *
	 * @param step the workflow step to start with
	 * @param check the predicate to check for
	 * @return
	 */
	public static WorkflowStep nextUntil(WorkflowStep step, Predicate<? super WorkflowStep> check) {
		requireNonNull(step);

		final Workflow workflow = step.getWorkflow();

		while(step!=null) {
			if(check.test(step)) {
				return step;
			}

			if(workflow.getNextStepCount(step)!=1) {
				break;
			}

			step = workflow.getNextStep(step, 0);
		}

		return null;
	}

	// IDENTIFIABLE STUFF

	public static Person derivePerson(Identifiable source) {
		Person person;

		if(source.getType()==Type.PERSON) {
			person = (Person) source;
		} else {
			person = DefaultPerson.uniquePerson();
			person.copyFrom(source);
		}

		return person;
	}

	public static Resource deriveResource(Identifiable source) {
		Resource resource;

		if(source.getType()==Type.RESOURCE) {
			resource = (Resource) source;
		} else {
			resource = DefaultResource.uniqueResource();
			resource.copyFrom(source);
		}

		return resource;
	}

	public static Tool deriveTool(Identifiable source) {
		Tool tool;

		if(source.getType()==Type.TOOL) {
			tool = (Tool) source;
		} else {
			tool = DefaultTool.uniqueTool();
			tool.copyFrom(source);
		}

		return tool;
	}

	@SuppressWarnings("unchecked")
	public static <I extends Identifiable> I derive(Identifiable source, Identifiable.Type type) {
		switch (type) {
		case PERSON: return (I) derivePerson(source);
		case RESOURCE: return (I) deriveResource(source);
		case TOOL: return (I) deriveTool(source);

		default:
			throw new IllegalArgumentException("Unknown identifiable type: "+type);
		}
	}

	public static Workflow createLinearWorkflow(WorkflowSchema schema) {
		Workflow workflow = new DefaultWorkflow(schema);

		WorkflowStep step0 = createStep(workflow, "init", false, 2, false, 0);
		workflow.addWorkflowStep(workflow.getInitialStep(), step0);

		WorkflowStep step1 = createStep(workflow, "annotate", false, 2, true, 2);
		workflow.addWorkflowStep(step0, step1);

		WorkflowStep step2 = createStep(workflow, "curate", false, 1, true, 1);
		workflow.addWorkflowStep(step1, step2);

		WorkflowStep step3 = createStep(workflow, "train", true, 2, true, 0);
		workflow.addWorkflowStep(step2, step3);

		WorkflowStep step4 = createStep(workflow, "analyze", true, 2, true, 2);
		workflow.addWorkflowStep(step3, step4);

		return workflow;
	}

	public static Workflow createForkedWorkflow(WorkflowSchema schema) {
		Workflow workflow = new DefaultWorkflow(schema);

		WorkflowStep step0 = createStep(workflow, "init", false, 2, false, 0);
		workflow.addWorkflowStep(workflow.getInitialStep(), step0);

		WorkflowStep step1 = createStep(workflow, "annotate", false, 2, true, 2);
		workflow.addWorkflowStep(step0, step1);

		WorkflowStep step2 = createStep(workflow, "curate", false, 1, true, 1);
		workflow.addWorkflowStep(step1, step2);

		WorkflowStep step3 = createStep(workflow, "train", true, 2, true, 0);
		workflow.addWorkflowStep(step1, step3);

		WorkflowStep step4 = createStep(workflow, "analyze", true, 2, true, 0);
		workflow.addWorkflowStep(step3, step4);

		WorkflowStep step5 = createStep(workflow, "correct", true, 2, true, 1);
		workflow.addWorkflowStep(step4, step5);

		return workflow;
	}

	public static WorkflowStep createStep(Workflow workflow, String title, boolean useTool, int input, boolean hasOutput, int persons) {
		WorkflowStep step = workflow.createWorkflowStep();

		step.setTitle(title);
		step.setDescription("Some random description for step: "+title);

		WorkflowSchema schema = workflow.getSchema();

		if(useTool) {
			DefaultTool tool = DefaultTool.withSettings("-v -file path/to/my/dir/model.xml", System.getProperty("os.arch"));
			tool.addIdentifier(new Identifier(schema.getDefaultNameVersionIdentifierType(), "myTool-v1"));
			tool.setResourceType("software/parser");

			step.setTool(tool);
		}

		for(int i=0; i<input; i++) {
			DefaultResource resource = DefaultResource.withResourceType("dataset/model");
			resource.addIdentifier(new Identifier(schema.getDefaultNameVersionIdentifierType(), "model"+(i+1)));
			resource.addIdentifier(new Identifier(schema.getDefaultPathIdentifierType(), "path/to/model/dir/file"+(i+1)+".xml"));

			step.addInput(resource);
		}

		if(hasOutput) {
			DefaultResource output = DefaultResource.withResourceType("dataset/analysis");
			output.addIdentifier(new Identifier(schema.getDefaultPathIdentifierType(), "output/dir/result.xml"));

			step.addOutput(output);
			step.addInput(output);
		}

		for(int i=0; i<persons; i++) {
			DefaultPerson person = DefaultPerson.withRole("annotator");
			person.addIdentifier(new Identifier(schema.getDefaultNameIdentifierType(), "约翰 "+(i+1)));

			step.addPerson(person);
		}

		return step;
	}
}

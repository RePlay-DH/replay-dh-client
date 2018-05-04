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

import javax.swing.event.ChangeEvent;

/**
 * Implementation of the {@link WorkflowListener} interface that
 * does nothing, i.e. all implemented methods have empty bodies.
 *
 *
 * @author Markus Gärtner
 *
 */
public class WorkflowAdapter implements WorkflowListener {

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	public void stateChanged(ChangeEvent e) {
		// no-op
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowListener#activeWorkflowStepChanged(bwfdm.replaydh.workflow.Workflow, bwfdm.replaydh.workflow.WorkflowStep, bwfdm.replaydh.workflow.WorkflowStep)
	 */
	@Override
	public void activeWorkflowStepChanged(Workflow workflow, WorkflowStep oldActiveStep, WorkflowStep newActiveStep) {
		// no-op
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowListener#workflowStepAdded(bwfdm.replaydh.workflow.Workflow, bwfdm.replaydh.workflow.WorkflowStep)
	 */
	@Override
	public void workflowStepAdded(Workflow workflow, WorkflowStep step) {
		// no-op
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowListener#workflowStepRemoved(bwfdm.replaydh.workflow.Workflow, bwfdm.replaydh.workflow.WorkflowStep)
	 */
	@Override
	public void workflowStepRemoved(Workflow workflow, WorkflowStep step) {
		// no-op
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowListener#workflowStepChanged(bwfdm.replaydh.workflow.Workflow, bwfdm.replaydh.workflow.WorkflowStep)
	 */
	@Override
	public void workflowStepChanged(Workflow workflow, WorkflowStep step) {
		// no-op
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowListener#workflowStepPropertyChanged(bwfdm.replaydh.workflow.Workflow, bwfdm.replaydh.workflow.WorkflowStep, java.lang.String)
	 */
	@Override
	public void workflowStepPropertyChanged(Workflow workflow, WorkflowStep step, String propertyName) {
		// no-op
	}

}

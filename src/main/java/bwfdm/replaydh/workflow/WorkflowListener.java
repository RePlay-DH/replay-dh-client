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

import java.beans.PropertyChangeEvent;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public interface WorkflowListener extends ChangeListener {

	/**
	 * Fired when the workflow is closed down or loaded for the first time.
	 *
	 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
	 */
	@Override
	void stateChanged(ChangeEvent e);

	/**
	 * Fired when the {@link Workflow#getActiveStep() active step} of a workflow
	 * has been {@link Workflow#setActiveStep(WorkflowStep) changed}.
	 *
	 * @param workflow
	 * @param oldActiveStep the active step prior to the change
	 * @param newActiveStep the active step after the change
	 */
	void activeWorkflowStepChanged(Workflow workflow, WorkflowStep oldActiveStep, WorkflowStep newActiveStep);

	/**
	 * Fired after a step has been added to the workflow graph.
	 *
	 * @param workflow
	 * @param step
	 */
	void workflowStepAdded(Workflow workflow, WorkflowStep step);

	/**
	 * Fired after a step has been removed from the workflow graph.
	 *
	 * @param workflow
	 * @param step
	 */
	void workflowStepRemoved(Workflow workflow, WorkflowStep step);

	/**
	 * General unspecified change occurred for the given workflow {@code step}.
	 *
	 * @param workflow
	 * @param step
	 */
	void workflowStepChanged(Workflow workflow, WorkflowStep step);

	/**
	 * Signals a named property change. Since workflow steps have a couple of
	 * {@link Set collection} properties (e.g. {@link WorkflowStep#getInput() input files})
	 * this event notification does not carry the old and new values of the property
	 * like the {@link PropertyChangeEvent} does.
	 *
	 * @param workflow
	 * @param step
	 * @param propertyName
	 */
	void workflowStepPropertyChanged(Workflow workflow, WorkflowStep step, String propertyName);

	public static final String PROPERTY_RECORDING_TIME = "recordingTime";
	public static final String PROPERTY_INPUT = "input";
	public static final String PROPERTY_OUTPUT = "output";
	public static final String PROPERTY_PERSONS = "persons";
	public static final String PROPERTY_TOOL = "tool";
	public static final String PROPERTY_TITLE = "title";
	public static final String PROPERTY_DESCRIPTION = "description";
}

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

import java.util.Set;
import java.util.function.Consumer;

import bwfdm.replaydh.utils.LazyCollection;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * Models the DAG of a workflow. Each {@link WorkflowStep step} is treated as a
 * node that contains the state of the working directory after the step has been executed.
 *
 * @author Markus
 */
public interface Workflow extends AutoCloseable {

	public static final String ROOT_ID = "ROOT";

	/**
	 * Constant API version identifier intended to be
	 * stored together with a workflow documentation so
	 * that future client versions can make compatibility
	 * checks and/or conversions.
	 */
	public static final WorkflowVersion API_VERSION = WorkflowVersion.VERSION_1;

	//TODO add grouping of resources

	/**
	 * Returns the schema that declares the label and
	 * identifier vocabulary for this workflow.
	 * @return
	 */
	WorkflowSchema getSchema();

	String getTitle();

	String getDescription();

	/**
	 * Fetches the workflow step that is assigned the specified {@code id}.
	 *
	 * @param id
	 * @return
	 */
	WorkflowStep resolveStep(String id);

    /**
     * Returns the first step performed in this workflow.
     * Note that the first step is always the initialization of a working directory
     * and it can only contain {@link WorkflowStep#getOutput() output} resources.
     * It is not possible to delete this step from a workflow graph, but one can
     * programmatically erase all of its content (via successive use of
     * {@link WorkflowStep#removeOutput(Resource)}).
     *
     * @return
     */
    WorkflowStep getInitialStep();

    /**
     * Shorthand method to check whether the given step is the (artificial)
     * initial step of this workflow.
     *
     * @param step
     * @return
     *
     * @see #getInitialStep()
     */
    default boolean isInitialStep(WorkflowStep step) {
    	requireNonNull(step);
    	return step==getInitialStep();
    }

    /**
     * Returns the most recent step for the current state of the workspace.
     *
     * @return
     */
    WorkflowStep getActiveStep();

    /**
     * Changes the active workflow step so that after this method returns
     * {@link #getActiveStep()} will return the given {@code step}.
     * <p>
     * Note that this method can result in rather expensive background
     * operations when the underlying workspace gets changed to reflect
     * the state associated with the specified step.
     *
     * @param step
     * @return {@code true} iff the change was successful
     */
    boolean setActiveStep(WorkflowStep step) throws InterruptedException;

    /**
     * Returns the steps that represent the current <i>heads</i> of all
     * the branches in this workflow. A head is the most recent step in
     * an individual branch. Note that as long as this workflow has at
     * least one step that is not the {@link #getInitialStep() initial one}
     * the returned set will never be empty.
     *
     * @return a non-editable set of all the steps that represent heads in
     * this workflow or the empty set if this workflow is empty except for
     * the {@link #getInitialStep() initial step}.
     */
    Set<WorkflowStep> getHeadSteps();

    default boolean isHeadStep(WorkflowStep step) {
    	requireNonNull(step);
    	return step!=getInitialStep() && getHeadSteps().contains(step);
    }

    default boolean isActiveStep(WorkflowStep step) {
    	requireNonNull(step);
    	return step==getActiveStep();
    }

    /**
     * Performs the given {@code action} on each step in this workflow
     * in unspecified order.
     *
     * @param action
     */
    void forEachStep(Consumer<? super WorkflowStep> action);

    /**
     * Returns a {@link Set} view on all the steps recorded in this
     * workflow. This method should be used with care since the graph might
     * load the data related to individual steps lazily from the backend
     * storage and depending on the size of the workflow this could get
     * very expensive.
     * <p>
     *
     *
     * @return
     */
    default Set<WorkflowStep> getAllSteps() {
    	LazyCollection<WorkflowStep> result = LazyCollection.lazySet();

    	forEachStep(result);

    	return result.getAsSet();
    }

    /**
     * Returns the total number of steps in this workflow.
     * <p>
     * Note that this includes the {@link #getInitialStep() initial step},
     * so the returned value is always greater than {@code 0}!
     *
     * @return
     */
    default int getStepCount() {
    	return getAllSteps().size();
    }

    /**
     * Performs the given {@code action} for each step immediately following
     * the given {@code step}. The order in which those steps are visited
     * is unspecified.
     *
     * @param step
     * @param action
     */
    void forEachNextStep(WorkflowStep step, Consumer<? super WorkflowStep> action);

    boolean hasNextSteps(WorkflowStep step);

    /**
     * Returns all steps directly connected to and happening after
     * the given {@code step}.
     *
     * @param step
     * @return
     */
    default Set<WorkflowStep> getNextSteps(WorkflowStep step) {
    	LazyCollection<WorkflowStep> result = LazyCollection.lazySet();

    	forEachNextStep(step, result);

    	return result.getAsSet();
    }

    WorkflowStep getNextStep(WorkflowStep step, int index);

    default int getNextStepCount(WorkflowStep step) {
    	return getNextSteps(step).size();
    }

    /**
     * Returns whether {@code target} is a direct successor of {@code step}.
     *
     * @param step
     * @param target
     * @return
     */
    default boolean isNextStep(WorkflowStep step, WorkflowStep target) {
    	return getNextSteps(step).contains(target);
    }

    /**
     * Performs the given {@code action} for each step immediately preceding
     * the given {@code step}. The order in which those steps are visited
     * is unspecified.
     *
     * @param step
     * @param action
     */
    void forEachPreviousStep(WorkflowStep step, Consumer<? super WorkflowStep> action);

    boolean hasPreviousSteps(WorkflowStep step);

    /**
     * Returns all steps directly connected to and happening after
     * the given {@code step}.
     *
     * @param step
     * @return
     */
    default Set<WorkflowStep> getPreviousSteps(WorkflowStep step) {
    	LazyCollection<WorkflowStep> result = LazyCollection.lazySet();

    	forEachPreviousStep(step, result);

    	return result.getAsSet();
    }

    WorkflowStep getPreviousStep(WorkflowStep step, int index);

    default int getPreviousStepCount(WorkflowStep step) {
    	return getPreviousSteps(step).size();
    }

    /**
     * Returns whether {@code target} is a direct predecessor of {@code step}.
     *
     * @param step
     * @param target
     * @return
     */
    default boolean isPreviousStep(WorkflowStep step, WorkflowStep target) {
    	return getPreviousSteps(step).contains(target);
    }

    // LISTENER METHODS

    void addWorkflowListener(WorkflowListener listener);

    void removeWorkflowListener(WorkflowListener listener);

    // MODIFICATION METHODS

    /**
     * Creates a new step that is linked to this workflow.
     * The new step is initially empty and must be initialized
     * by client code before it can actually and officially
     * be {@link #addWorkflowStep(WorkflowStep) added} to this
     * workflow graph.
     *
     * @return the freshly created blank workflow step
     */
    WorkflowStep createWorkflowStep();

    /**
     * Adds a new step that connects to the result of the given
     * {@code source} step.
     *
     * @param source
     * @return
     *
     * @throws WorkflowException if the given {@code step} is not sufficiently
     * initialized
     * @throws UnsupportedOperationException if the implementation does not allow
     * external decisions to dictate how new steps are attached to the graph
     */
    boolean addWorkflowStep(WorkflowStep source, WorkflowStep step);

    /**
     * Adds the given {@code step} as a successor to the currently
     * {@link #getActiveStep() active} step.
     * <p>
     * This method also automatically changes the active step to
     * the {@code step} argument.
     *
     * @param step
     * @return
     */
    boolean addWorkflowStep(WorkflowStep step);

    /**
     * Attempts to delete the given workflow {@code step} and returns
     * {@code true} in case that succeeded.
     *
     * @param step
     */
    boolean deleteWorkflowStep(WorkflowStep step);

    /**
     * Opportunity for client code to artificially trigger a
     * {@link WorkflowListener#workflowStepChanged(Workflow, WorkflowStep)}
     * event for registered listeners.
     *
     * @param step
     */
    void workflowStepChanged(WorkflowStep step);

    /**
     * Disconnects this workflow graph from any underlying data and releases
     * all resources.
     * <p>
     * After being closed a workflow shouldn't allow invocations of any other
     * methods besides {@link #close()} (this method should be implemented in
     * an idempotent way).
     *
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    void close();

    boolean isClosed();

	// Transaction API

	void beginUpdate();

	void endUpdate();

	boolean isUpdating();

	String attachMarker(WorkflowStep step, String marker, String label);

}

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
package bwfdm.replaydh.workflow.impl;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.event.ChangeEvent;

import bwfdm.replaydh.utils.IdentityHashSet;
import bwfdm.replaydh.utils.Transaction;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowListener;
import bwfdm.replaydh.workflow.WorkflowStep;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * This implementation is not thread-safe!
 *
 * @author Markus Gärtner
 *
 */
public class DefaultWorkflow implements Workflow {

	private final ChangeEvent changeEvent = new ChangeEvent(this);

	private final List<WorkflowListener> listeners = new CopyOnWriteArrayList<>();

	private final Map<WorkflowStep, Node<WorkflowStep>> graph = new IdentityHashMap<>();

	private final Map<String, WorkflowStep> idLookup = new HashMap<>();

	static final String UNSET_ID = "UNSET";

	private final WorkflowStep initialStep;

	private volatile WorkflowStep activeStep;

	private final Set<WorkflowStep> heads = new IdentityHashSet<>();

	private final WorkflowSchema schema;

	private String title;

	private String description;

	private final Transaction transaction = Transaction.withEndCallback(this::endTransaction);

	private AtomicBoolean closed = new AtomicBoolean(false);

	private AtomicInteger idGenerator = new AtomicInteger(1);

	public DefaultWorkflow(WorkflowSchema schema) {
		this.schema = requireNonNull(schema);

		initialStep = createWorkflowStep();
		initialStep.setId(ROOT_ID);

		init();
	}

	@Override
	public String getTitle() {
		checkState("Title not set yet", title!=null);
		return title;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public void setTitle(String title) {
		this.title = requireNonNull(title);
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#resolveStep(java.lang.String)
	 */
	@Override
	public WorkflowStep resolveStep(String id) {
		WorkflowStep step = idLookup.get(requireNonNull(id));
		checkArgument("No such step: "+id, step!=null);
		return step;
	}

	/**
	 * Changes the internal id generator so that the provided
	 * {@code value} will be used for the numerical part when
	 * creating the next id.
	 *
	 * @param id
	 */
	public void setNextStepNumber(int value) {
		checkArgument("New number must not be negative", value>0);
		idGenerator.set(value);
	}

	public int getNextStepNumber() {
		return idGenerator.get();
	}

	private void init() {
		node(initialStep, true);

		activeStep = initialStep;
	}

	/**
	 * Helper method for {@link DefaultWorkflowStep}
	 */
	String acceptOrCreateNewId(String id) {
		if(id==null) {
			// Search first legal id we can use
			do {
				id = "step_"+idGenerator.getAndIncrement();
			} while(idLookup.containsKey(id));
		} else {
			checkUniqueId(id);
		}

		return id;
	}

	void checkUniqueId(String id) {
		if(idLookup.containsKey(id))
			throw new IllegalArgumentException("Duplicate id: "+id);
	}

	/**
	 * Helper method for {@link DefaultWorkflowStep}
	 */
	void idChanged(WorkflowStep step, String oldId, String newId) {
		if(oldId!=null) {
			idLookup.remove(oldId);
		}

		idLookup.put(newId, step);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#getSchema()
	 */
	@Override
	public WorkflowSchema getSchema() {
		return schema;
	}

	public void reset() {
		clear();
		init();
	}

	private void clear() {

		listeners.clear();

		graph.values().forEach(n -> n.dispose());
		graph.clear();

		idLookup.clear();
	}

	/**
	 * Removes all registered listeners, {@link Node#dispose() disposes} of any
	 * nodes and then clears the internal graph map entirely.
	 * <p>
	 * Subclasses should make sure to include a call to {@code super.close()} when
	 * overriding this method, preferably <b>after</b> custom cleanup actions have
	 * been performed.
	 *
	 * @see bwfdm.replaydh.workflow.Workflow#close()
	 */
	@Override
	public void close() {
		if(closed.compareAndSet(false, true)) {

			// Notify listeners about close event
			fireStateChanged();

			clear();
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#isClosed()
	 */
	@Override
	public boolean isClosed() {
		return closed.get();
	}

	public void fireStateChanged() {
		if(listeners.isEmpty()) {
			return;
		}

		for(WorkflowListener listener : listeners) {
			listener.stateChanged(changeEvent);
		}
	}

	public void fireWorkflowStepPropertyChanged(WorkflowStep step, String propertyName) {
		if(listeners.isEmpty() || !step.isAdded()) {
			return;
		}

		for(WorkflowListener listener : listeners) {
			listener.workflowStepPropertyChanged(this, step, propertyName);
		}
	}

	public void fireWorkflowStepChanged(WorkflowStep step) {
		if(listeners.isEmpty() || !step.isAdded()) {
			return;
		}

		for(WorkflowListener listener : listeners) {
			listener.workflowStepChanged(this, step);
		}
	}

	public void fireWorkflowStepAdded(WorkflowStep step) {
		if(listeners.isEmpty()) {
			return;
		}

		for(WorkflowListener listener : listeners) {
			listener.workflowStepAdded(this, step);
		}
	}

	public void fireWorkflowStepRemoved(WorkflowStep step) {
		if(listeners.isEmpty()) {
			return;
		}

		for(WorkflowListener listener : listeners) {
			listener.workflowStepRemoved(this, step);
		}
	}

	public void fireActiveWorkflowStepChanged(WorkflowStep oldActiveStep, WorkflowStep newActiveStep) {
		if(listeners.isEmpty()) {
			return;
		}

		for(WorkflowListener listener : listeners) {
			listener.activeWorkflowStepChanged(this, oldActiveStep, newActiveStep);
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#getInitialStep()
	 */
	@Override
	public WorkflowStep getInitialStep() {
		return ensureWorkflowStepData(initialStep);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#getHeadSteps()
	 */
	@Override
	public Set<WorkflowStep> getHeadSteps() {
		return Collections.unmodifiableSet(heads);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#isHeadStep(bwfdm.replaydh.workflow.WorkflowStep)
	 */
	@Override
	public boolean isHeadStep(WorkflowStep step) {
		requireNonNull(step);
		return step!=initialStep && heads.contains(step);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#getActiveStep()
	 */
	@Override
	public WorkflowStep getActiveStep() {
		checkState("No active step", activeStep!=null);

		return ensureWorkflowStepData(activeStep);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#setActiveStep(bwfdm.replaydh.workflow.WorkflowStep)
	 */
	@Override
	public boolean setActiveStep(WorkflowStep step) throws InterruptedException {
		requireNonNull(step);
		checkArgument("Foreign workflow step", step.getWorkflow()==this);

		WorkflowStep oldActiveStep = activeStep;

		boolean result = setActiveStepImpl(step);

		if(result) {
			fireActiveWorkflowStepChanged(oldActiveStep, step);
		}

		return result;
	}

	protected boolean setActiveStepImpl(WorkflowStep step) {

		boolean canChange = activeStep!=step;

		if(canChange) {
			activeStep = step;
		}

		return canChange;
	}

	protected WorkflowStep getInitialStepUnchecked() {
		return initialStep;
	}

	/**
	 * Callback for subclasses that implement lazy loading of
	 * external resources. This method signals that the general
	 * workflow data should be loaded.
	 */
	protected void ensureFullWorkflowData() {
		// no-op
	}

	/**
	 * Callback for subclasses that implement lazy loading of
	 * external resources. This method signals that data directly
	 * associated with the given {@code step} should be loaded.
	 * <p>
	 * The default implementation delegates to {@link #ensureFullWorkflowData()}
	 */
	protected WorkflowStep ensureWorkflowStepData(WorkflowStep step) {
		ensureFullWorkflowData();
		return step;
	}

	/**
	 * Callback for subclasses that implement lazy loading of
	 * external resources. This method signals that data directly
	 * associated with the given {@code step} and directly attached
	 * links (the direction of which is specified by the {@code incoming}
	 * parameter) should be loaded.
	 * <p>
	 * The default implementation delegates to {@link #ensureFullWorkflowData()}
	 */
	protected void ensureWorkflowStepLinks(WorkflowStep step, boolean incoming) {
		ensureFullWorkflowData();
	}

	protected Consumer<? super WorkflowStep> checkedConsumer(Consumer<? super WorkflowStep> action) {
		Consumer<WorkflowStep> check = this::ensureWorkflowStepData;

		return check.andThen(action);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#getStepCount()
	 */
	@Override
	public int getStepCount() {
		return graph.size();
	}

	/**
	 * {@inheritDoc}
	 * @see bwfdm.replaydh.workflow.Workflow#forEachStep(java.util.function.Consumer)
	 */
	@Override
	public void forEachStep(Consumer<? super WorkflowStep> action) {
		requireNonNull(action);
		ensureFullWorkflowData();

		graph.keySet().forEach(checkedConsumer(action));
	}

	/**
	 * {@inheritDoc}
	 * @see bwfdm.replaydh.workflow.Workflow#forEachNextStep(bwfdm.replaydh.workflow.WorkflowStep, java.util.function.Consumer)
	 */
	@Override
	public void forEachNextStep(WorkflowStep step, Consumer<? super WorkflowStep> action) {
		requireNonNull(step);
		requireNonNull(action);

		ensureWorkflowStepLinks(step, false);

		node(step, false).forEachOutgoing(checkedConsumer(action));
	}

	/**
	 * {@inheritDoc}
	 * @see bwfdm.replaydh.workflow.Workflow#getNextStepCount(bwfdm.replaydh.workflow.WorkflowStep)
	 */
	@Override
	public int getNextStepCount(WorkflowStep step) {
		requireNonNull(step);

		ensureWorkflowStepLinks(step, false);

		return node(step, false).outgoingCount();
	}

	/**
	 * {@inheritDoc}
	 * @see bwfdm.replaydh.workflow.Workflow#getNextStep(bwfdm.replaydh.workflow.WorkflowStep, int)
	 */
	@Override
	public WorkflowStep getNextStep(WorkflowStep step, int index) {
		requireNonNull(step);

		ensureWorkflowStepLinks(step, false);

		return ensureWorkflowStepData(node(step, false).outgoing().get(index));
	}

	/**
	 * {@inheritDoc}
	 * @see bwfdm.replaydh.workflow.Workflow#hasNextSteps(bwfdm.replaydh.workflow.WorkflowStep)
	 */
	@Override
	public boolean hasNextSteps(WorkflowStep step) {
		requireNonNull(step);

		ensureWorkflowStepLinks(step, false);

		return node(step, false).outgoingCount()>0;
	}

	/**
	 * {@inheritDoc}
	 * @see bwfdm.replaydh.workflow.Workflow#isNextStep(bwfdm.replaydh.workflow.WorkflowStep, bwfdm.replaydh.workflow.WorkflowStep)
	 */
	@Override
	public boolean isNextStep(WorkflowStep step, WorkflowStep target) {
		requireNonNull(step);
		requireNonNull(target);

		ensureWorkflowStepLinks(step, false);

		return node(step, false, true).hasLink(target);
	}

	/**
	 * {@inheritDoc}
	 * @see bwfdm.replaydh.workflow.Workflow#hasPreviousSteps(bwfdm.replaydh.workflow.WorkflowStep)
	 */
	@Override
	public boolean hasPreviousSteps(WorkflowStep step) {
		requireNonNull(step);

		ensureWorkflowStepLinks(step, true);

		return node(step, false).incomingCount()>0;
	}

	/**
	 * {@inheritDoc}
	 * @see bwfdm.replaydh.workflow.Workflow#forEachPreviousStep(bwfdm.replaydh.workflow.WorkflowStep, java.util.function.Consumer)
	 */
	@Override
	public void forEachPreviousStep(WorkflowStep step, Consumer<? super WorkflowStep> action) {
		requireNonNull(step);
		requireNonNull(action);

		ensureWorkflowStepLinks(step, true);

		node(step, false).forEachIncoming(checkedConsumer(action));
	}

	/**
	 * {@inheritDoc}
	 * @see bwfdm.replaydh.workflow.Workflow#getPreviousStepCount(bwfdm.replaydh.workflow.WorkflowStep)
	 */
	@Override
	public int getPreviousStepCount(WorkflowStep step) {
		requireNonNull(step);

		ensureWorkflowStepLinks(step, true);

		return node(step, false).incomingCount();
	}

	/**
	 * {@inheritDoc}
	 * @see bwfdm.replaydh.workflow.Workflow#getPreviousStep(bwfdm.replaydh.workflow.WorkflowStep, int)
	 */
	@Override
	public WorkflowStep getPreviousStep(WorkflowStep step, int index) {
		requireNonNull(step);

		ensureWorkflowStepLinks(step, true);

		return ensureWorkflowStepData(node(step, false).incoming().get(index));
	}

	/**
	 * {@inheritDoc}
	 * @see bwfdm.replaydh.workflow.Workflow#isPreviousStep(bwfdm.replaydh.workflow.WorkflowStep, bwfdm.replaydh.workflow.WorkflowStep)
	 */
	@Override
	public boolean isPreviousStep(WorkflowStep step, WorkflowStep target) {
		requireNonNull(step);
		requireNonNull(target);

		ensureWorkflowStepLinks(step, true);

		return node(target, false, true).hasLink(step);
	}

	/**
	 * {@inheritDoc}
	 * @see bwfdm.replaydh.workflow.Workflow#addWorkflowListener(bwfdm.replaydh.workflow.WorkflowListener)
	 */
	@Override
	public void addWorkflowListener(WorkflowListener listener) {
		requireNonNull(listener);

		listeners.add(listener);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#removeWorkflowListener(bwfdm.replaydh.workflow.WorkflowListener)
	 */
	@Override
	public void removeWorkflowListener(WorkflowListener listener) {
		requireNonNull(listener);

		listeners.remove(listener);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#addWorkflowStep(bwfdm.replaydh.workflow.WorkflowStep)
	 */
	@Override
	public boolean addWorkflowStep(WorkflowStep source, WorkflowStep target) {
		requireNonNull(source);
		requireNonNull(target);

		boolean result = addWorkflowStepImpl(source, target);

		fireWorkflowStepAdded(target);

		return result;
	}

	protected final void setHead(WorkflowStep step, boolean isHead) {
		if(step==initialStep) {
			return;
		}

		if(isHead) {
			heads.add(step);
		} else {
			heads.remove(step);
		}

//		System.out.printf("step=%d head=%b\n",step.hashCode(), isHead);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#addWorkflowStep(bwfdm.replaydh.workflow.WorkflowStep)
	 */
	@Override
	public boolean addWorkflowStep(WorkflowStep target) {
		requireNonNull(target);

		boolean result = addWorkflowStepImpl(getActiveStep(), target);

		if(result) {
			fireWorkflowStepAdded(target);

			if(isAutoAssignActiveStepOnAdd()) {

				WorkflowStep oldActiveStep = getActiveStep();

				setActiveStepImpl(target);

				fireActiveWorkflowStepChanged(oldActiveStep, target);
			}
		}

		return result;
	}

	protected boolean addWorkflowStepImpl(WorkflowStep source, WorkflowStep target) {
		// Make sure we already know about the source node
		node(source, false);

		setHead(source, false);

//		boolean isTargetKnown = node(target, false, false)!=null;

		// Now link the two nodes in our lookup
		addLink(source, target);

//		if(!isTargetKnown) {
			target.addNotify();

			setHead(target, getNextStepCount(target)==0);

			String id = target.getId();

			// Id not initialized yet -> create a fresh one
			if(id==null || UNSET_ID.equals(id)) {
				if(isEnsureUniqueStepIdOnAdd()) {
					ensureUniqueStepId(target);
				}
			} else {
				// If id has been set already, we expect it to be unique!
				checkUniqueId(id);
				idChanged(target, null, id);
			}
//		}

		return true;
	}

	protected boolean isEnsureUniqueStepIdOnAdd() {
		return true;
	}

	protected void ensureUniqueStepId(WorkflowStep step) {

		String id = step.getId();

		// Id not initialized yet -> create a fresh one
		if(id==null || UNSET_ID.equals(id)) {
			id = acceptOrCreateNewId(null);
			step.setId(id);
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#deleteWorkflowStep(bwfdm.replaydh.workflow.WorkflowStep)
	 */
	@Override
	public boolean deleteWorkflowStep(WorkflowStep step) {
		requireNonNull(step);
		checkArgument("Cannot delete initial step", step!=initialStep);

		boolean result = deleteWorkflowStepImpl(step);

		if(result) {
			fireWorkflowStepRemoved(step);
		}

		return result;
	}

	protected boolean deleteWorkflowStepImpl(WorkflowStep step) {

		Node<WorkflowStep> node = node(step, false, false);

		// Only allow us to delete leafs
		boolean canDelete = node!=null && node.outgoingCount()==0;

		if(canDelete) {
			// Delete all links for incoming steps
			node.forEachIncoming(source -> {
				Node<WorkflowStep> previous = node(source, false);
				List<WorkflowStep> out = previous.outgoing();
				// Delete link
				out.remove(step);
				// And update heads if needed
				setHead(previous.content(), out.isEmpty());

			});
			// Erase internal node data
			node.dispose();

			// Finally delete node mapping itself
			graph.remove(step);

			// Additional unmapping of head state
			setHead(step, false);

			// Finally notify the step itself
			step.removeNotify();

			idLookup.remove(step.getId());
		}

		return canDelete;
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#attachMarker(bwfdm.replaydh.workflow.WorkflowStep, java.lang.String, java.lang.String)
	 */
	@Override
	public String attachMarker(WorkflowStep step, String marker, String label) {
		return (String) node(step).setProperty(marker, label);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#createWorkflowStep()
	 */
	@Override
	public WorkflowStep createWorkflowStep() {
		return new DefaultWorkflowStep(this);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#workflowStepChanged(bwfdm.replaydh.workflow.WorkflowStep)
	 */
	@Override
	public void workflowStepChanged(WorkflowStep step) {
		fireWorkflowStepChanged(step);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#beginUpdate()
	 */
	@Override
	public void beginUpdate() {
		transaction.beginUpdate();
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#endUpdate()
	 */
	@Override
	public void endUpdate() {
		transaction.endUpdate();
	}

	/**
	 * @see bwfdm.replaydh.workflow.Workflow#isUpdating()
	 */
	@Override
	public boolean isUpdating() {
		return transaction.isTransactionInProgress();
	}

	/**
	 * Callback to persist changes made during the current transaction.
	 * <p>
	 * This method is used as the {@code callback} argument when initially
	 * creating the {@link Transaction} helper object for this workflow.
	 */
	protected void endTransaction() {
		// no-op
	}

	/**
	 * Hook for subclasses to signal whether or not the default implementation
	 * should automatically assign a newly added workflow step as the currently
	 * {@link #getActiveStep() active} step. If this method returns {@code false}
	 * the subclass implementation is responsible for correctly setting the active
	 * step whenever it deems a change necessary.
	 *
	 * @return
	 */
	protected boolean isAutoAssignActiveStepOnAdd() {
		return true;
	}

	// FILL METHODS

	/**
	 * Wraps the given {@link WorkflowStep step} into a new {@link Node}.
	 * Subclasses can override this method to either change the actual node
	 * class used or to perform additional setup work.
	 */
	protected Node<WorkflowStep> newNode(WorkflowStep content) {
		return new Node<>(content);
	}

	/**
	 * Fetches (and creates if missing depending on the {@code createIfMissing} argument)
	 * a node, failing if it does not exist and the {@code requirePresent} argument is {@code true}.
	 */
	public Node<WorkflowStep> node(WorkflowStep content, boolean createIfMissing, boolean requirePresent) {
		Node<WorkflowStep> node = graph.get(content);
		if(node==null && createIfMissing) {
			node = newNode(content);
			graph.put(content, node);
		}
		if(node==null && requirePresent)
			throw new IllegalStateException("Missing internal buffer for step: "+content);

		return node;
	}

	/**
	 * Fetches (and creates if missing depending on the {@code createIfMissing} argument)
	 * a node, failing if it does not exist.
	 */
	public Node<WorkflowStep> node(WorkflowStep content, boolean createIfMissing) {
		return node(content, createIfMissing, true);
	}

	/**
	 * Fetches a node, failing of does not exist already
	 */
	public Node<WorkflowStep> node(WorkflowStep content) {
		return node(content, false, true);
	}

	/**
	 * Establishes a link between the nodes for arguments
	 * {@code from} and {@code to} in that order.
	 * <p>
	 * This method does not notify listeners.
	 */
	protected void addLink(WorkflowStep from, WorkflowStep to) {
		requireNonNull(from);
		requireNonNull(to);

		node(from, true).addOutgoing(to);
		node(to, true).addIncoming(from);
	}

	public void setFlagForNodes(int flag, boolean active) {
		for(Node<WorkflowStep> node : graph.values()) {
			node.setFlag(flag, active);
		}
	}


	/**
	 * Traverses this graph, visiting nodes in an unspecified order.
	 * At each node the given {@code visitor} is {@link BiPredicate#test(Object, Object) called}
	 * with this graph instance and the node in question as arguments.
	 * Outgoing links for a node are only being followed if the visitor
	 * returned {@code true} for above call. No node will be visited more
	 * than once.
	 *
	 * @param startingNodes
	 * @param visitor
	 */
	public void walkGraph(Set<WorkflowStep> startingNodes, BiPredicate<Workflow, ? super WorkflowStep> visitor) {
		requireNonNull(visitor);
		requireNonNull(startingNodes);
		checkArgument("No starting nodes", !startingNodes.isEmpty());

		ensureFullWorkflowData();

		// Reset VISITED flag for entire graph
		setFlagForNodes(FLAG_VISITED, false);

		// Grab nodes for starting points
		Stack<Node<WorkflowStep>> buffer = new Stack<>();
		for(WorkflowStep node : startingNodes) {
			buffer.add(node(node, false, true));
		}

		while(!buffer.isEmpty()) {
			Node<WorkflowStep> node = buffer.pop();

			boolean doContinue = visitor.test(this, node.content());

			// Mark node as VISITED
			node.setFlag(FLAG_VISITED, true);

			// Only if visitor allows it will we follow outgoing links
			if(doContinue) {
				List<WorkflowStep> outgoing = node.outgoing();
				if(outgoing!=null) {
					for(WorkflowStep out : outgoing) {
						Node<WorkflowStep> nodeOut = node(out, false, true);
						if(!nodeOut.flagSet(FLAG_VISITED)) {
							buffer.add(nodeOut);
						}
					}
				}
			}
		}
	}

	public void walkGraph(Set<WorkflowStep> startingNodes, Predicate<? super WorkflowStep> visitor) {
		requireNonNull(visitor);
		requireNonNull(startingNodes);
		checkArgument("No starting nodes", !startingNodes.isEmpty());

		ensureFullWorkflowData();

		// Reset VISITED flag for entire graph
		setFlagForNodes(FLAG_VISITED, false);

		// Grab nodes for starting points
		Stack<Node<WorkflowStep>> buffer = new Stack<>();
		for(WorkflowStep node : startingNodes) {
			buffer.add(node(node, false, true));
		}

		while(!buffer.isEmpty()) {
			Node<WorkflowStep> node = buffer.pop();

			boolean doContinue = visitor.test(node.content());

			// Mark node as VISITED
			node.setFlag(FLAG_VISITED, true);

			// Only if visitor allows it will we follow outgoing links
			if(doContinue) {
				List<WorkflowStep> outgoing = node.outgoing();
				if(outgoing!=null) {
					for(WorkflowStep out : outgoing) {
						Node<WorkflowStep> nodeOut = node(out, false, true);
						if(!nodeOut.flagSet(FLAG_VISITED)) {
							buffer.add(nodeOut);
						}
					}
				}
			}
		}
	}

	// Facility to allow subclasses to create compatible additional flags
	private final AtomicInteger flagPow = new AtomicInteger(0);

	/**
	 * Creates a new flag or throws an exception if the value space of a single
	 * integer (32 bit) has been exhausted.
	 */
	protected final int createFlag() {
		int exp = flagPow.getAndIncrement();
		if(exp>31)
			throw new InternalError("Too many flags - reached limit of 32 int mask");
		return (1<<exp);
	}

	/**
	 * Used by our filler or builder code to check if we already processed
	 * a given node,
	 */
	protected final int FLAG_FILLED = createFlag();

	/**
	 * Used by walker or visitor code to check if a given node has been
	 * visited previously.
	 */
	protected final int FLAG_VISITED = createFlag();

	/**
	 * Used to limit the overhead in lazy initialization of linked
	 * content in workflow steps.
	 */
	protected final int FLAG_LOADED = createFlag();
}

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
package bwfdm.replaydh.ui.workflow.graph;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static bwfdm.replaydh.utils.RDHUtils.checkState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.mxgraph.model.mxCell;

import bwfdm.replaydh.workflow.WorkflowStep;

/**
 * @author Markus Gärtner
 *
 */
public class WorkflowNode extends mxCell {

	private static final long serialVersionUID = 5028110644293342558L;

	private static final AtomicInteger flagSlot = new AtomicInteger(0);

	public static int createFlag() {
		int slot = flagSlot.getAndIncrement();
		checkState("Flag slots exhausted", slot<32);
		return 1<<slot;
	}

	/**
	 * Lazily populated buffer holding workflow steps associated with
	 * this node but currently not visible. This is to directly support
	 * collapsing of "simple" parts of a workflow graph.
	 */
	private List<WorkflowStep> hiddenSteps;

	private int flags;

	public boolean hasHiddenSteps() {
		return hiddenSteps!=null && !hiddenSteps.isEmpty();
	}

	/**
	 * Lazily creates and returns the list of
	 * hidden workflow steps.
	 */
	private List<WorkflowStep> hiddenSteps(boolean createIfMissing, boolean expectNonNull) {
		if(hiddenSteps==null && createIfMissing) {
			hiddenSteps = new ArrayList<>(4);
		}

		if(expectNonNull && hiddenSteps==null)
			throw new IllegalStateException("Missing buffer list for hidden workflow steps");

		return hiddenSteps;
	}

	public void addHiddenStep(WorkflowStep step) {
		hiddenSteps(true, true).add(step);
	}

	public void removeHiddenStep(WorkflowStep step) {
		hiddenSteps(false, true).remove(step);
	}

	public int getHiddenStepCount() {
		return hiddenSteps==null ? 0 : hiddenSteps.size();
	}

	public List<WorkflowStep> getHiddenSteps() {
		List<WorkflowStep> list = hiddenSteps(false, false);
		if(list==null) {
			return Collections.emptyList();
		} else {
			return new ArrayList<>(list);
		}
	}

	public void forEachHiddenSetep(Consumer<? super WorkflowStep> action) {
		List<WorkflowStep> list = hiddenSteps(false, false);
		if(list!=null) {
			list.forEach(action);
		}
	}

	public void setVisibleStep(WorkflowStep step) {
		setValue(step);
	}

	public WorkflowStep getVisibleStep() {
		return (WorkflowStep) getValue();
	}

	/**
	 * Check if specified {@code flag} has been set
	 */
	public boolean isFlagSet(int flag) {
		return (flags & flag) == flag;
	}

	/**
	 * Sets specified {@code flag} to specified {@code active} state
	 * and returns whether or not it was set before.
	 */
	public boolean setFlag(int flag, boolean active) {
		boolean wasSet = (flags & flag) == flag;

		if(active) {
			flags |= flag;
		} else {
			flags &= ~flag;
		}

		return wasSet;
	}

	/**
	 * @see com.mxgraph.model.mxCell#setValue(java.lang.Object)
	 */
	@Override
	public void setValue(Object value) {
		checkArgument("Not a workflow step", value==null || value instanceof WorkflowStep);
		super.setValue(value);
	}
}

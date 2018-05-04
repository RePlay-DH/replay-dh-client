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

import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.awt.Rectangle;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxICell;
import com.mxgraph.view.mxGraph;

import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowAdapter;
import bwfdm.replaydh.workflow.WorkflowStep;

/**
 * @author Markus Gärtner
 *
 */
public abstract class WorkflowGraphLayout extends WorkflowAdapter {

	private WorkflowGraph graph;

	private Map<WorkflowStep, mxCell> nodeLookup = new IdentityHashMap<>();

	// Setup methods for host graph controller

	final void install(WorkflowGraph workflowGraph) {
		this.graph = requireNonNull(workflowGraph);
		doInstall();
	}

	/**
	 * Hook for subclasses to do setup work for a new graph
	 */
	protected void doInstall() {
		// no-op
	}

	final void uninstall() {
		try {
			doUninstall();
		} finally {
			graph = null;
		}
	}

	/**
	 * Hook for subclasses to do cleanup work for a new graph
	 */
	protected void doUninstall() {
		// no-op
	}

	public final boolean isInstalled() {
		return graph != null;
	}

	// Delegate methods to acquire graph resources

	public mxGraph getGraph() {
		return graph.getGraph();
	}

	public mxGraphModel getModel() {
		return (mxGraphModel) getGraph().getModel();
	}

	public Workflow getWorkflow() {
		return graph.getWorkflow();
	}

	public WorkflowGraph getWorkflowGraph() {
		return graph;
	}

	// Layout methods

	public abstract void doLayout();

	protected void mapStep(WorkflowStep step, mxCell node) {
		nodeLookup.put(step, node);
	}

	protected void unmapStep(WorkflowStep step) {
		nodeLookup.remove(step);
	}

	protected void unmapAllSteps() {
		nodeLookup.clear();
	}

	protected boolean isMapped(WorkflowStep step) {
		return nodeLookup.containsKey(step);
	}

	@SuppressWarnings("unchecked")
	public <C extends mxCell> C getNode(WorkflowStep step) {
		return (C) nodeLookup.get(step);
	}


	public WorkflowStep getStep(Object cell) {
		return (WorkflowStep) getModel().getValue(cell);
	}

	public boolean canSelect(Object cell) {
		return cell instanceof WorkflowNode;
	}

	public boolean canCompress(Object cell) {
		return false;
	}

	public boolean canExpand(Object cell) {
		if(cell instanceof WorkflowNode) {
			return ((WorkflowNode)cell).hasHiddenSteps();
		}
		return false;
	}

	public Object compress(Object cell) {
		return null;
	}

	public Object expand(Object cell) {
		return null;
	}

	public Object toggle(Object cell) {
		if(canCompress(cell)) {
			return compress(cell);
		} else if(canExpand(cell)) {
			return expand(cell);
		} else {
			return null;
		}
	}

	public void refreshNodeStyle(mxICell cell) {
		Workflow workflow = getWorkflow();
		WorkflowStep step = (WorkflowStep) cell.getValue();
		String style = WorkflowGraph.STYLE_VERTEX;
		boolean styleFinished = false;

		// Fetch basic style based on the main step
		if(workflow.isInitialStep(step)) {
			style = WorkflowGraph.STYLE_INITIAL;
			styleFinished = true;
		}

		// Add decoration
		if(workflow.isActiveStep(step)) {
			style += ";"+WorkflowGraph.STYLE_ACTIVE;
			styleFinished = true;
		} else if(workflow.isHeadStep(step)) {
			style += ";"+WorkflowGraph.STYLE_HEAD;
		}

		// If needed, go traverse hidden steps to compute style
		if(!styleFinished && cell instanceof WorkflowNode) {
			for(WorkflowStep hiddenStep : ((WorkflowNode)cell).getHiddenSteps()) {
				// NO need to check for initial step here, that one can't be compressed anyway
				if(workflow.isActiveStep(hiddenStep)) {
					style += ";"+WorkflowGraph.STYLE_ACTIVE;
					styleFinished = true;
				} else if(workflow.isHeadStep(hiddenStep)) {
					style += ";"+WorkflowGraph.STYLE_HEAD;
				}

				if(styleFinished) {
					break;
				}
			}
		}

		cell.setStyle(style);
	}

	/**
	 * Create new node, add to graph and map to given step.
	 * Return new cell or {@code null} if step already mapped.
	 */
	@SuppressWarnings("unchecked")
	protected <C extends mxCell> C defaultMakeNode(Object parent, WorkflowStep step) {

		// Make sure we don't attempt redundant node creation
		if(isMapped(step)) {
			return null;
		}

		Rectangle size = WorkflowStepShape.getPreferredCellSize(step);

		mxCell cell = createNode(step, new mxGeometry(0, 0, size.width, size.height));
		cell.setVertex(true);

		refreshNodeStyle(cell);

		mapStep(step, cell);

		int index = getModel().getChildCount(parent);
		getModel().add(parent, cell, index);

		return (C) cell;
	}

	protected mxCell createNode(WorkflowStep step, mxGeometry geometry) {
		return new mxCell(step, geometry, null);
	}

	/**
	 * Create new link between given nodes if {@code previous} is not {@code null}
	 * and add to graph.
	 */
	protected void defaultMakeLink(Object parent, WorkflowStep previous, WorkflowStep next) {

		if(previous!=null) {
			mxCell sourceNode = getNode(previous);
			mxCell targetNode = getNode(next);

			checkState("Missing source node", sourceNode!=null);
			checkState("Missing target node", targetNode!=null);

			defaultLinkCells(parent, sourceNode, targetNode);
		}
	}

	protected void defaultLinkCells(Object parent, mxCell source, mxCell target) {

		// Check that we don't create redundant nodes
		for(int i=0; i<source.getEdgeCount();i++) {
			mxICell edge = source.getEdgeAt(i);
			if(edge.getTerminal(false)==target) {
				return;
			}
		}

		mxCell edge = new mxCell(null, new mxGeometry(), null);
		edge.getGeometry().setRelative(true);
		edge.setEdge(true);

		mxGraphModel model = getModel();
		int index = model.getChildCount(parent);
		model.add(parent, edge, index);

		model.setTerminal(edge, source, true);
		model.setTerminal(edge, target, false);
	}

	protected void forEachNode(Consumer<? super mxCell> action) {
		nodeLookup.values().forEach(action);
	}
}

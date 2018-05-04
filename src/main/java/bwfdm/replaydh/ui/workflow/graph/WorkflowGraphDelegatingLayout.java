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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.swing.SwingConstants;

import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.view.mxGraph;

import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowStep;
import bwfdm.replaydh.workflow.WorkflowUtils;

/**
 * @author Markus Gärtner
 *
 */
public class WorkflowGraphDelegatingLayout extends WorkflowGraphLayout {

	public static WorkflowGraphDelegatingLayout newInstance() {
		return new WorkflowGraphDelegatingLayout(true, null);
	}

	public static WorkflowGraphDelegatingLayout newInstance(mxGraphLayout layout) {
		return new WorkflowGraphDelegatingLayout(false, layout);
	}


	/**
	 * Flag to signal that consecutive pipe nodes, i.e. nodes with
	 * exactly one incoming and outgoing edge each, should
	 * be hidden by default and get compressed into a single one.
	 */
	private boolean hidePipes = false;

	private final boolean autoCreateLayout;

	private mxGraphLayout layout;

	private WorkflowGraphDelegatingLayout(boolean autoCreateLayout, mxGraphLayout layout) {
		this.autoCreateLayout = autoCreateLayout;
		if(!autoCreateLayout) {
			setLayout(layout);
		}
	}

	/**
	 * @see bwfdm.replaydh.ui.workflow.graph.WorkflowGraphLayout#doInstall()
	 */
	@Override
	protected void doInstall() {
		super.doInstall();

		if(autoCreateLayout) {
			setLayout(createDefaultLayout());
		}
	}

	private mxGraphLayout createDefaultLayout() {
		mxHierarchicalLayout layout = new mxHierarchicalLayout(getGraph(), SwingConstants.WEST);
//		layout.setResizeParent(false);
		return layout;
	}

	/**
	 * @see bwfdm.replaydh.ui.workflow.graph.WorkflowGraphLayout#doUninstall()
	 */
	@Override
	protected void doUninstall() {
		if(autoCreateLayout) {
			layout = null;
		}

		super.doUninstall();
	}

	/**
	 * @return the layout
	 */
	public mxGraphLayout getLayout() {
		return layout;
	}

	/**
	 * @param layout the layout to set
	 */
	public void setLayout(mxGraphLayout layout) {
		this.layout = requireNonNull(layout);
	}

	@Override
	public void doLayout() {
		mxGraphModel model = getModel();

		model.beginUpdate();
		try {
			unmapAllSteps();
			model.clear();

			Workflow workflow = getWorkflow();
			// If we have a valid workflow, transform steps into graph nodes
			if(workflow!=null && !workflow.isClosed()) {
				workflow2Graph(workflow);
			}

		} finally {
			model.endUpdate();
		}
	}

	/**
	 * <pre>
	 * Segment formation rules:
	 *
	 * For any node N do:
	 *
	 * 1. if no active segment then start segment with N
	 * 2. if incoming(N)<=1 then add to current segment
	 * 3. if outgoing(N)>1 or incoming(N)>1 then end active segment with N
	 *
	 * Scenarios:
	 *
	 * 1.      N xx   =>   create new segment
	 *
	 * 2. s -- N --   =>   add N to existing segment
	 *
	 * 3. s -- N --   =>   end s, place N in singleton segment
	 *      __/
	 *
	 * 4. s -- N --   =>   end s, place N in singleton segment
	 *          \__
	 *
	 *
	 *
	 * </pre>
	 */

	private static final int FLAG_PIPE = WorkflowNode.createFlag();

	protected void workflow2Graph(final Workflow workflow) {
		// Pending steps
		Stack<WorkflowStep> steps = new Stack<>();

		// Start with initial step
		steps.add(workflow.getInitialStep());

		final Object parent = getGraph().getDefaultParent();

		final Set<WorkflowNode> segments = new HashSet<>();

		// First pass: map all steps to segments
		while(!steps.isEmpty()) {
			WorkflowStep step = steps.pop();

			// No processing nodes again
			if(isMapped(step)) {
				continue;
			}

			boolean stepHidden = false;
			boolean canHide = WorkflowUtils.canHide(step);

			WorkflowNode node = null;

			// If we are allowed, try to compress into existing segment
			if(hidePipes && canHide) {
				// Note that here the single previous step is guaranteed to be processed already
				WorkflowStep previous = WorkflowUtils.previous(step);
				if(previous!=workflow.getInitialStep()) {
					node = getNode(previous);

					if(node.isFlagSet(FLAG_PIPE)) {
						node.addHiddenStep(step);

						// Map step to the segment that hides it
						mapStep(step, node);

						refreshNodeStyle(node);

						stepHidden = true;

						segments.add(node);
					}
				}
			}

			if(!stepHidden) {
				node = defaultMakeNode(parent, step);
			}

			node.setFlag(FLAG_PIPE, WorkflowUtils.isPipe(step));

			// Add all outgoing steps for subsequent processing
			steps.addAll(workflow.getNextSteps(step));
		}

		// Second pass: now perform the linking
		forEachNode(node -> {
			WorkflowStep step = (WorkflowStep) node.getValue();
			workflow.forEachPreviousStep(step, previous -> {
				// The linking method will ensure not to create redundant edges
				defaultMakeLink(parent, previous, step);
			});
		});

		applyLayout(null);

		for(WorkflowNode segment : segments) {
			addHiddenStepCountOverlay(segment);
		}
	}

	/**
	 * @see bwfdm.replaydh.ui.workflow.graph.WorkflowGraphLayout#createNode(bwfdm.replaydh.workflow.WorkflowStep, com.mxgraph.model.mxGeometry, java.lang.String)
	 */
	@Override
	protected WorkflowNode createNode(WorkflowStep step, mxGeometry geometry) {
		WorkflowNode node = new WorkflowNode();
		node.setValue(step);
		node.setGeometry(geometry);
		return node;
	}

	/**
	 * @return the hidePipes
	 */
	public boolean isHidePipes() {
		return hidePipes;
	}

	/**
	 * @param hidePipes the hidePipes to set
	 */
	public void setHidePipes(boolean hidePipes) {
		this.hidePipes = hidePipes;
	}

	/**
	 * @see bwfdm.replaydh.ui.workflow.graph.WorkflowGraphLayout#canCompress(java.lang.Object)
	 */
	@Override
	public boolean canCompress(Object cell) {
		if(!getModel().isVertex(cell) || !(cell instanceof WorkflowNode)) {
			return false;
		}

		WorkflowNode node = (WorkflowNode) cell;
		if(node.hasHiddenSteps()) {
			return false;
		}

		WorkflowStep step = getStep(cell);

		if(getWorkflow().isInitialStep(step)) {
			return false;
		}

		return WorkflowUtils.canCompress(step);
	}

	@Override
	public Object compress(Object cell) {
		final mxGraphModel model = getModel();
		if(!model.isVertex(cell)) {
			return null;
		}

		final WorkflowNode node = (WorkflowNode) cell;
		if(node.hasHiddenSteps()) {
			return null;
		}

		// The step initiating the algorithm
		final WorkflowStep step = getStep(cell);

		// Try to fail early
		if(!WorkflowUtils.canHide(step) && !WorkflowUtils.isPipe(step)) {
			return null;
		}

		final Workflow workflow = getWorkflow();
		if(workflow.isInitialStep(step)) {
			return null;
		}

		model.beginUpdate();
		try {

			// Ordered collection of the steps we can hide in this compression cycle
			List<WorkflowStep> stepsToHide = new ArrayList<>();

			// Pointing to the step which is going to hide all subsequent ones
			WorkflowStep first = step;
			// Collect steps backwards as far as possible
			while(WorkflowUtils.canHide(first)) {
				WorkflowStep previous = WorkflowUtils.previous(first);
				if(workflow.isInitialStep(previous) || !WorkflowUtils.isPipe(previous)) {
					break;
				}
				stepsToHide.add(first);
				first = previous;
			}
			// We went backwards, so now reverse the list
			Collections.reverse(stepsToHide);

			// Now collect forward
			WorkflowStep last = step;
			while(WorkflowUtils.isPipe(last)) {
				WorkflowStep next = WorkflowUtils.next(last);
				if(!WorkflowUtils.canHide(next)) {
					break;
				}
				stepsToHide.add(next); // Adding next, since this loop is a 1-lookahead
				last = next;
			}

			if(stepsToHide.isEmpty()) {
				return false;
			}

			// This is the truly last step in the segment
			last = stepsToHide.get(stepsToHide.size()-1);

			WorkflowNode segment = getNode(first);
			segment.setFlag(FLAG_PIPE, WorkflowUtils.isPipe(last));

			// Now remove all nodes and links for the intermediary steps
			for(WorkflowStep midStep : stepsToHide) {
				segment.addHiddenStep(midStep);
				Object midNode = getNode(midStep);
				for(int i=model.getEdgeCount(midNode)-1; i>=0; i--) {
					model.remove(model.getEdgeAt(midNode, i));
				}
				model.remove(midNode);
			}

			Object parent = getGraph().getDefaultParent();
			for(WorkflowStep next : workflow.getNextSteps(last)) {
				defaultMakeLink(parent, first, next);
			}

			refreshNodeStyle(segment);

			applyLayout(null);

			// Add overlay
			addHiddenStepCountOverlay(segment);

			return segment;
		} finally {
			model.endUpdate();
		}
	}

	/**
	 * @see bwfdm.replaydh.ui.workflow.graph.WorkflowGraphLayout#canExpand(java.lang.Object)
	 */
	@Override
	public boolean canExpand(Object cell) {
		if(!getModel().isVertex(cell) || !(cell instanceof WorkflowNode)) {
			return false;
		}
		WorkflowNode node = (WorkflowNode) cell;
		return node.hasHiddenSteps();
	}

	@Override
	public Object expand(Object cell) {
		mxGraphModel model = getModel();
		if(!model.isVertex(cell)) {
			return null;
		}

		// The node to be expanded
		final WorkflowNode node = (WorkflowNode) cell;
		if(!node.hasHiddenSteps()) {
			return null;
		}

		Workflow workflow = getWorkflow();

		model.beginUpdate();
		try {

			// Delete old links
			for(Object edge : mxGraphModel.getOutgoingEdges(model, cell)) {
				model.remove(edge);
			}

			final Object parent = getGraph().getDefaultParent();

			Object lastNode = null;

			// Introduce new nodes
			final List<WorkflowStep> hiddenSteps = node.getHiddenSteps();
			for(WorkflowStep step : hiddenSteps) {
				unmapStep(step);
				WorkflowNode newNode = defaultMakeNode(parent, step);
				newNode.setFlag(FLAG_PIPE, WorkflowUtils.isPipe(step));
				node.removeHiddenStep(step);

				lastNode = newNode;
			}

			// Refresh info on the original node
			final WorkflowStep originalStep = getStep(node);
			node.setFlag(FLAG_PIPE, WorkflowUtils.isPipe(originalStep));
			defaultMakeLink(parent, originalStep, hiddenSteps.get(0));

			// Finally link all the new nodes
			for(WorkflowStep step : hiddenSteps) {
				for(WorkflowStep next : workflow.getNextSteps(step)) {
					defaultMakeLink(parent, step, next);
				}
			}

			// Update style on the old cell
			refreshNodeStyle(node);

			removeOverlays(node, OVERLAY_HIDDEN);

			applyLayout(null);

			return lastNode;
		} finally {
			model.endUpdate();
		}
	}

	private static final String OVERLAY_HIDDEN = "hidden";

	private void addHiddenStepCountOverlay(WorkflowNode segment) {
		String label = "+"+String.valueOf(segment.getHiddenStepCount());
		String tooltip = ResourceManager.getInstance().get(
				"replaydh.ui.core.workflowGraph.labels.hiddenSteps", segment.getHiddenStepCount());
		Object overlay = overlay()
				.value(label)
				.type(OVERLAY_HIDDEN)
				.size(20, 14)
				.style(WorkflowGraph.STYLE_OVERLAY)
				.addStyle(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RECTANGLE)
				.addStyle(mxConstants.STYLE_ROUNDED, true)
				.addStyle(WorkflowGraph.STYLE_TOOLTIP, tooltip)
				.addStyle(mxConstants.STYLE_AUTOSIZE, 1)
				.position(1.0, 0.0)
				.offset(-4.0, -7.0)
				.build();

		addOverlay(segment, overlay);

		// To account for very long pipes we need to automatically adjust the overlay's size
		getGraph().updateCellSize(overlay);
	}

	private void applyLayout(Object parent) {
		mxGraph graph = getGraph();

		if(parent==null) {
			parent = graph.getDefaultParent();
		}

		boolean cellsMovable = graph.isCellsMovable();
		graph.setCellsMovable(true);
		try {
			layout.execute(parent);
		} finally {
			graph.setCellsMovable(cellsMovable);
		}
	}

	private void removeOverlays(Object cell) {
		mxGraphModel model = getModel();
		for(Object child : mxGraphModel.getChildCells(model, cell, true, false)) {
			model.remove(child);
		}
	}

	private void removeOverlays(Object cell, String type) {
		mxGraphModel model = getModel();
		for(int i=model.getChildCount(cell)-1; i>=0; i--) {
			Object child = model.getChildAt(cell, i);
			if(model.isVertex(child) && child instanceof mxCell
					&& type.equals(((mxCell)child).getId())) {
				model.remove(child);
				return;
			}
		}
	}

	private void addOverlay(Object cell, Object overlay) {
		mxGraphModel model = getModel();
		model.add(cell, overlay, model.getChildCount(cell));
	}

	public static OverlayBuilder overlay() {
		return new OverlayBuilder();
	}

	public static class OverlayBuilder {
		private final mxCell cell;

		private OverlayBuilder() {
			cell = new mxCell();
			cell.setVertex(true);

			mxGeometry geo = new mxGeometry();
			geo.setRelative(true);
			cell.setGeometry(geo);
		}

		public OverlayBuilder type(String type) {
			cell.setId(type);
			return this;
		}

		public OverlayBuilder value(Object value) {
			cell.setValue(value);
			return this;
		}

		public OverlayBuilder size(double width, double height) {
			cell.getGeometry().setWidth(width);
			cell.getGeometry().setHeight(height);
			return this;
		}

		public OverlayBuilder position(double x, double y) {
			cell.getGeometry().setX(x);
			cell.getGeometry().setY(y);
			return this;
		}

		public OverlayBuilder offset(double x, double y) {
			cell.getGeometry().setOffset(new mxPoint(x, y));
			return this;
		}

		public OverlayBuilder addStyle(String key, Object value) {
			return addStyle(key+"="+value);
		}

		public OverlayBuilder addStyle(String style) {
			String oldStyle = cell.getStyle();
			if(oldStyle != null) {
				style = oldStyle
						+ (oldStyle.endsWith(";") ? "" : ";")
						+ style;
			}
			cell.setStyle(style);
			return this;
		}

		public OverlayBuilder style(String style) {
			cell.setStyle(style);
			return this;
		}

		public mxCell build() {
			return cell;
		}
	}
}

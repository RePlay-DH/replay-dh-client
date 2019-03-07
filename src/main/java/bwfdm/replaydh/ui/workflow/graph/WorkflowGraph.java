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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;

import org.java.plugin.registry.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxRubberband;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;

import bwfdm.replaydh.core.PluginEngine;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.git.GitRemoteUpdater;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.stats.StatEntry;
import bwfdm.replaydh.stats.StatType;
import bwfdm.replaydh.ui.GuiStats;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.actions.ActionManager;
import bwfdm.replaydh.ui.actions.ActionManager.ActionMapper;
import bwfdm.replaydh.ui.helper.AbstractDialogWorker;
import bwfdm.replaydh.ui.helper.AbstractDialogWorker.CancellationPolicy;
import bwfdm.replaydh.ui.helper.CloseableUI;
import bwfdm.replaydh.ui.workflow.WorkflowExportWizard.WorkflowExportContext;
import bwfdm.replaydh.utils.AbstractPropertyChangeSource;
import bwfdm.replaydh.utils.Options;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Person;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowListener;
import bwfdm.replaydh.workflow.WorkflowStep;
import bwfdm.replaydh.workflow.WorkflowUtils;
import bwfdm.replaydh.workflow.export.ExportException;
import bwfdm.replaydh.workflow.export.ExportUtils;
import bwfdm.replaydh.workflow.export.ResourcePublisher;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.ObjectScope;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.Type;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.WorkflowScope;
import bwfdm.replaydh.workflow.impl.DefaultWorkflow;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public class WorkflowGraph extends AbstractPropertyChangeSource implements CloseableUI {

	private static final Logger log = LoggerFactory.getLogger(WorkflowGraph.class);

	private static volatile ActionManager sharedActionManager;

	private static ActionManager getSharedActionManager() {
		GuiUtils.checkEDT();

		ActionManager actionManager = sharedActionManager;
		if(actionManager==null) {
			actionManager = ActionManager.globalManager().derive();
			try {
				actionManager.loadActions(WorkflowGraph.class.getResource("workflow-graph-actions.xml"));
			} catch (IOException e) {
				throw new RDHException("Failed to load actions for"+WorkflowGraph.class, e);
			}

			sharedActionManager = actionManager;
		}

		return actionManager;
	}

	private final JPanel panel;

	private final JLabel lWorkflowTitle;
	private final JToolBar toolBar;

	private final ActionManager actionManager;
	private final ActionMapper actionMapper;

	private final mxGraphComponent graphComponent;
	private final mxRubberband rubberband;

	private mxGraph graph;

	private mxGraphModel graphModel;

	private JPopupMenu popupMenu;

	private WorkflowGraphLayout layout;

	private boolean wheelZoomEnabled = true;

	private Workflow workflow;

	private final Handler handler;

	private final CallbackHandler callbackHandler;

	private final RDHEnvironment environment;

	public static final String SHAPE_WORKFLOW_STEP = "step";

	public static final String PROPERTY_WORKFLOW = "workflow";
	public static final String PROPERTY_LAYOUT = "layout";

	private final AtomicBoolean isBuilding = new AtomicBoolean(false);

	// Register our custom renderer for workflow step nodes
	static {
		mxGraphics2DCanvas.putShape(SHAPE_WORKFLOW_STEP, new WorkflowStepShape());
	}

	public WorkflowGraph(RDHEnvironment environment) {

		this.environment = requireNonNull(environment);

		handler = new Handler();

		callbackHandler = new CallbackHandler();

		panel = new JPanel(new BorderLayout());

		actionManager = getSharedActionManager().derive();
		actionMapper = actionManager.mapper(this);

		lWorkflowTitle = new JLabel();
		toolBar = createToolBar();

		graphModel = createGraphModel();

		graph = createGraph();

		graphComponent = createGraphComponent(graph);
		rubberband = new mxRubberband(graphComponent);

		setLayout(createGraphLayout());

		panel.add(graphComponent, BorderLayout.CENTER);
		panel.add(toolBar, BorderLayout.NORTH);

		registerActions();

		refreshWorkflowTitle();
	}

	private void logStat(StatEntry entry) {
		environment.getClient().getStatLog().log(entry);
	}

	private JToolBar createToolBar() {

		final Options options = new Options();

		PluginEngine pluginEngine = environment.getClient().getPluginEngine();
		List<Extension> extensions = pluginEngine.getExtensions(PluginEngine.CORE_PLUGIN_ID,
				ExportUtils.RESORUCE_PUBLISHER_EXTENSION_POINT_ID);
		extensions.sort(PluginEngine.LOCALIZABLE_ORDER);

		JMenuItem[] menuItems = GuiUtils.toMenuItems(pluginEngine, extensions, callbackHandler::publishResources);
		options.put("publisherExtensions", menuItems);
		options.put("workflowTitle", lWorkflowTitle);

		return actionManager.createToolBar("replaydh.ui.core.workflowGraph.toolBarList", options);
	}

	public static final String STYLE_VERTEX = "defaultVertex";
	public static final String STYLE_OVERLAY = "overlay";
	public static final String STYLE_ACTIVE = "active";
	public static final String STYLE_HEAD = "head";
	public static final String STYLE_INITIAL = "initial";

	public static final String STYLE_LABEL = "label";
	public static final String STYLE_TOOLTIP = "tooltip";

	private mxStylesheet createStylesheet() {
		mxStylesheet stylesheet = new mxStylesheet();

		StyleBuilder builder = StyleBuilder.forStylesheet(stylesheet);

		builder.modifyDefaultVertexStyle()
			.newEntry(mxConstants.STYLE_FILLCOLOR, mxConstants.NONE)
			.newEntry(mxConstants.STYLE_STROKECOLOR, "black")
//			.newEntry(mxConstants.STYLE_SHADOW, true)
//			.newEntry(mxConstants.STYLE_FILLCOLOR, "red")
			.newEntry(mxConstants.STYLE_SHAPE, SHAPE_WORKFLOW_STEP)
//			.newEntry(mxConstants.STYLE_GLASS, 1)
			.newEntry(mxConstants.STYLE_STROKEWIDTH, 1.5F)
			.newEntry(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_TOP)
			.newEntry(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_BOTTOM)
			.newEntry(mxConstants.STYLE_FOLDABLE, "0")
			.done();

		builder.newStyle(STYLE_ACTIVE)
			.newEntry(mxConstants.STYLE_STROKECOLOR, "red")
			.newEntry(mxConstants.STYLE_STROKEWIDTH, 2)
			.commit();

		builder.newStyle(STYLE_HEAD)
			.newEntry(mxConstants.STYLE_STROKECOLOR, "blue")
			.newEntry(mxConstants.STYLE_STROKEWIDTH, 2)
			.commit();

		builder.newStyle(STYLE_INITIAL)
			.newEntry(mxConstants.STYLE_FILLCOLOR, mxConstants.NONE)
			.newEntry(mxConstants.STYLE_STROKECOLOR, "blue")
			.newEntry(mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE)
			.newEntry(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_MIDDLE)
			.newEntry(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE)
			.newEntry(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER)
			.newEntry(mxConstants.STYLE_LABEL_POSITION, mxConstants.ALIGN_CENTER)
			.commit();


		builder.newStyle(STYLE_OVERLAY)
			.newEntry(mxConstants.STYLE_FILLCOLOR, "#EDED43")
			.newEntry(mxConstants.STYLE_FONTCOLOR, "#000000")
			.newEntry(mxConstants.STYLE_STROKECOLOR, "#000000")
			.newEntry(mxConstants.STYLE_STROKEWIDTH, 0.5F)
			.newEntry(mxConstants.STYLE_VERTICAL_LABEL_POSITION, mxConstants.ALIGN_MIDDLE)
			.newEntry(mxConstants.STYLE_VERTICAL_ALIGN, mxConstants.ALIGN_MIDDLE)
			.newEntry(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_CENTER)
			.newEntry(mxConstants.STYLE_LABEL_POSITION, mxConstants.ALIGN_CENTER)
			.newEntry(mxConstants.STYLE_FOLDABLE, "0")
			.commit();

		return stylesheet;
	}

	private mxGraphModel createGraphModel() {
		return new mxGraphModel();
	}

	private mxGraph createGraph() {
		mxGraph graph = new mxGraph(graphModel){
			/**
			 * @see com.mxgraph.view.mxGraph#convertValueToString(java.lang.Object)
			 */
			@Override
			public String convertValueToString(Object cell) {
				String label = createLabel(cell);
				return label!=null ? label : super.convertValueToString(cell);
			}

			/**
			 * @see com.mxgraph.view.mxGraph#getToolTipForCell(java.lang.Object)
			 */
			@Override
			public String getToolTipForCell(Object cell) {
				String tooltip = createTooltip(cell);
				return tooltip!=null ? tooltip : super.getToolTipForCell(cell);
			}

			/**
			 * Prevent edges from ever being selected
			 *
			 * @see com.mxgraph.view.mxGraph#isCellSelectable(java.lang.Object)
			 */
			@Override
			public boolean isCellSelectable(Object cell) {
				return super.isCellSelectable(cell)
						&& getModel().isVertex(cell)
						&& getLayout().canSelect(cell);
			}
		};
		graph.setAllowDanglingEdges(false);
		graph.setAutoOrigin(true);
		graph.setAutoSizeCells(false);
		graph.setCellsCloneable(false);
		graph.setCellsEditable(false);
		graph.setCellsMovable(true);
		graph.setCellsResizable(false);
		graph.setCellsSelectable(true);
		graph.setCellsDisconnectable(false);
		graph.setConnectableEdges(false);
		graph.setDropEnabled(false);
		graph.setEdgeLabelsMovable(false);
		graph.setGridEnabled(true);
		graph.setKeepEdgesInBackground(true);
		graph.setLabelsVisible(true);
		graph.setLabelsClipped(false);
		graph.setBorder(30);
		graph.setStylesheet(createStylesheet());
		graph.setHtmlLabels(false);

		return graph;
	}

	private mxGraphComponent createGraphComponent(mxGraph graph) {
		mxGraphComponent graphComponent = new mxGraphComponent(graph);
		graphComponent.setAutoExtend(true);
		graphComponent.setAntiAlias(true);
		graphComponent.setTextAntiAlias(true);
		graphComponent.setImportEnabled(false);
		graphComponent.setFoldingEnabled(false);
		graphComponent.setToolTips(true);
		graphComponent.setConnectable(false);
		graphComponent.setDragEnabled(false);
		graphComponent.setKeepSelectionVisibleOnZoom(true);

		graphComponent.getGraphControl().addMouseListener(handler);
		graphComponent.getGraphControl().addMouseWheelListener(handler);

		graph.getSelectionModel().addListener(null, handler);

		return graphComponent;
	}

	private WorkflowGraphLayout createGraphLayout() {

//		mxCompactTreeLayout layout = new mxCompactTreeLayout(graph, true);
//		layout.setEdgeRouting(true);
//		layout.setMoveTree(true);

//		mxParallelEdgeLayout layout = new mxParallelEdgeLayout(graph);

//		mxPartitionLayout layout = new mxPartitionLayout(graph, true);

//		mxStackLayout layout = new mxStackLayout(graph, true);

//		mxHierarchicalLayout layout = new mxHierarchicalLayout(graph, SwingConstants.WEST);

		return WorkflowGraphDelegatingLayout.newInstance();
	}

	private void registerActions() {
		actionMapper.mapTask("replaydh.ui.core.workflowGraph.refreshGraph", callbackHandler::refreshGraph);
		actionMapper.mapTask("replaydh.ui.core.workflowGraph.loadDummyGraph", callbackHandler::loadDummyWorkflow);

		actionMapper.mapTask("replaydh.ui.core.workflowGraph.focusActiveStep", callbackHandler::focusActiveStep);
		actionMapper.mapTask("replaydh.ui.core.workflowGraph.changeActiveStep", callbackHandler::changeActiveStep);

		actionMapper.mapTask("replaydh.ui.core.workflowGraph.compressStep", callbackHandler::compressStep);
		actionMapper.mapTask("replaydh.ui.core.workflowGraph.expandStep", callbackHandler::expandStep);
		actionMapper.mapToggle("replaydh.ui.core.workflowGraph.toggleCompressPipes", callbackHandler::toggleCompressPipes);

		actionMapper.mapTask("replaydh.ui.core.workflowGraph.exportStepMetadata", callbackHandler::exportStepMetadata);
		actionMapper.mapTask("replaydh.ui.core.workflowGraph.exportStepResources", callbackHandler::exportStepResources);
		actionMapper.mapTask("replaydh.ui.core.workflowGraph.exportMetadata", callbackHandler::exportMetadata);
		actionMapper.mapTask("replaydh.ui.core.workflowGraph.exportResources", callbackHandler::exportResources);
		actionMapper.mapTask("replaydh.ui.core.workflowGraph.updateRepository", callbackHandler::updateRepository);

		/*
		 *  Do NOT register the publish action, since that one is only there
		 *  to initiate the popup menu for selecting the actual exporter!
		 */
//		actionMapper.mapAction("replaydh.ui.core.workflowGraph.publishResources", callbackHandler::publishResources);
	}

	private void refreshActions() {
		final Object selectedCell = getSelectedCell();
		final WorkflowStep selectedStep = getSelectedStep();
		final WorkflowStep activeStep = workflow.getActiveStep();
		final WorkflowStep initialStep = workflow.getInitialStep();

		final int selectedStepCount = graph.getSelectionCount();

		boolean hasSelectedSteps = selectedStepCount>0;
		boolean isSingleSelectedStep = selectedStepCount==1;
		boolean canCompressStep = layout.canCompress(selectedCell);
		boolean canExpandStep = layout.canExpand(selectedCell);

//		boolean isCompresseStep = isSingleSelectedStep
//				&& selectedCell instanceof WorkflowNode
//				&& ((WorkflowNode) selectedCell).hasHiddenSteps();

		boolean isSingleUncompressedStep = isSingleSelectedStep && !canExpandStep;

		boolean isWorkflowEmpty = WorkflowUtils.isEmpty(getWorkflow());

		boolean activeStepIsLeaf = WorkflowUtils.isLeaf(activeStep);

		actionManager.setEnabled(isSingleSelectedStep && canCompressStep,
				"replaydh.ui.core.workflowGraph.compressStep");
		actionManager.setEnabled(isSingleSelectedStep && canExpandStep,
				"replaydh.ui.core.workflowGraph.expandStep");

		actionManager.setEnabled(isSingleUncompressedStep && selectedStep!=activeStep,
				"replaydh.ui.core.workflowGraph.changeActiveStep");

		actionManager.setEnabled(isSingleUncompressedStep && selectedStep!=initialStep,
				"replaydh.ui.core.workflowGraph.exportStepMetadata");
		actionManager.setEnabled(isSingleUncompressedStep,
				"replaydh.ui.core.workflowGraph.exportStepResources");
		actionManager.setEnabled(!isWorkflowEmpty,
				"replaydh.ui.core.workflowGraph.exportMetadata",
				"replaydh.ui.core.workflowGraph.exportResources",
				"replaydh.ui.core.workflowGraph.publishResources");
		actionManager.setEnabled(activeStepIsLeaf,
				"replaydh.ui.core.workflowGraph.updateRepository");
	}

	private String createLabel(Object cell) {

		String label = null;

		Object value = graphModel.getValue(cell);
		if(value instanceof WorkflowStep) {
			WorkflowStep step = (WorkflowStep) value;

			if(WorkflowUtils.isInitial(step)) {
				label = ResourceManager.getInstance().get("replaydh.ui.core.workflowGraph.labels.initialStep");
			} else {
				label = step.getTitle();
			}
		}

		if(label==null) {
			label = getStyleValue(cell, STYLE_LABEL);
		}

		return label;
	}

	private String getStyleValue(Object cell, String key) {
		mxCellState state = graph.getView().getState(cell);
		if(state!=null) {
			Map<String, Object> style = state.getStyle();
			if(style!=null) {
				Object value = style.get(key);
				if(value!=null) {
					return String.valueOf(value);
				}
			}
		}

		return null;
	}

	/**
	 * Creates a tabular outline of the workflow step
	 * if the specified cell is a node.
	 */
	private String createTooltip(Object cell) {
		String tooltip = null;

		Object value = graphModel.getValue(cell);
		if(value instanceof WorkflowStep) {
			ResourceManager rm = ResourceManager.getInstance();
			WorkflowStep step = (WorkflowStep) value;
			StringBuilder sb = new StringBuilder("<html>");

			sb.append("<table valign=\"top\">");

			// TITLE
			sb.append("<tr><td>").append(rm.get("replaydh.labels.title")).append(":</td><td>");
			sb.append(step.getTitle());
			sb.append("</td></tr>");

			// ID
			sb.append("<tr><td>").append(rm.get("replaydh.labels.id")).append(":</td><td>");
			sb.append(step.getId());
			sb.append("</td></tr>");

			// DATE
			sb.append("<tr><td>").append(rm.get("replaydh.labels.date")).append(":</td><td>");
			sb.append(step.getRecordingTime());
			sb.append("</td></tr>");

			// DESCRIPTION
			sb.append("<tr><td>").append(rm.get("replaydh.labels.description")).append(":</td><td>");
			convertLineBreaks(step.getDescription(), sb);
			sb.append("</td></tr>");

			// TOOL
			if(step.getTool()!=null) {
				sb.append("<tr><td>").append(rm.get("replaydh.labels.tool")).append(":</td><td>").append(getDisplayLabel(step.getTool())).append("</td></tr>");
			}

			// PERSONS
			if(step.getPersonsCount()>0) {
				sb.append("<tr><td>").append(rm.get("replaydh.labels.persons")).append(":</td><td>");
				for(Iterator<Person> it = step.getPersons().iterator(); it.hasNext();) {
					sb.append(getDisplayLabel(it.next()));
					if(it.hasNext()) {
						sb.append("<br>");
					}
				}
				sb.append("</td></tr>");
			}

			// INPUT
			if(step.getInputCount()>0) {
				sb.append("<tr><td>").append(rm.get("replaydh.labels.input")).append(":</td><td>");
				for(Iterator<Resource> it = step.getInput().iterator(); it.hasNext();) {
					sb.append(getDisplayLabel(it.next()));
					if(it.hasNext()) {
						sb.append("<br>");
					}
				}
				sb.append("</td></tr>");
			}

			// OUTPUT
			if(step.getOutputCount()>0) {
				sb.append("<tr><td>").append(rm.get("replaydh.labels.output")).append(":</td><td>");
				for(Iterator<Resource> it = step.getOutput().iterator(); it.hasNext();) {
					sb.append(getDisplayLabel(it.next()));
					if(it.hasNext()) {
						sb.append("<br>");
					}
				}
				sb.append("</td></tr>");
			}

			sb.append("</table>");

			//DEBUG

			String internalStuff = step.getProperty(WorkflowStep.PROPERTY_INTERNAL_INFO);

			if(internalStuff != null) {
				// GIT stuff
				sb.append("<br>");
				sb.append(GuiUtils.toUnwrappedSwingTooltip(internalStuff, false));
			}


			tooltip = sb.toString();
		}

		if(tooltip==null) {
			tooltip = getStyleValue(cell, STYLE_TOOLTIP);
		}

		return tooltip;
	}

	private static void convertLineBreaks(String s, StringBuilder sb) {
		String[] lines = s.split("\r\n|\r|\n");
		for(int i=0; i<lines.length; i++) {
			sb.append(lines[i]);
			if(i<lines.length-1) {
				sb.append("<br>");
			}
		}
	}

	private String getLabel(Identifiable source, Identifier identifier) {
		if(identifier==null) {
			return ResourceManager.getInstance().get("replaydh.labels.unnamed", source.getType());
		} else {
			return identifier.getType().getName()+": "+identifier.getId();
		}
	}

	private String getDisplayLabel(Identifiable identifiable) {
		return getLabel(identifiable, Identifiable.getBestIdentifier(identifiable));
	}

	private void refreshWorkflowTitle() {
		String text = ResourceManager.getInstance().get("replaydh.panels.workflowGraph.title");
		String tooltip = null;

		if(workflow!=null) {
			String title = workflow.getTitle();

			text = text+" - "+title;
			tooltip = GuiUtils.toSwingTooltip(workflow.getDescription());
		}

		lWorkflowTitle.setText(text);
		lWorkflowTitle.setToolTipText(tooltip);
	}

	public JPanel getPanel() {
		return panel;
	}

	public Component getGraphComponent() {
		return graphComponent;
	}

	/**
	 * @return the graph
	 */
	public mxGraph getGraph() {
		return graph;
	}

	public boolean isWheelZoomEnabled() {
		return wheelZoomEnabled;
	}

	public void setWheelZoomEnabled(boolean wheelZoomEnabled) {
		boolean oldValue = this.wheelZoomEnabled;
		this.wheelZoomEnabled = wheelZoomEnabled;

		firePropertyChange("wheelZoomEnabled", oldValue, wheelZoomEnabled); //$NON-NLS-1$
	}

	public WorkflowGraphLayout getLayout() {
		return layout;
	}

	public void setLayout(WorkflowGraphLayout layout) {
		requireNonNull(layout);

		WorkflowGraphLayout oldValue = this.layout;

		if(this.layout!=null) {
			this.layout.uninstall();
		}

		this.layout = layout;

		this.layout.install(this);

		firePropertyChange(PROPERTY_LAYOUT, oldValue, layout);
	}

	private void focusCell(final Object cell, final boolean refreshGraph) {
		if(cell==null) {
			return;
		}

		GuiUtils.invokeEDT(() -> {
			if(refreshGraph) {
				graph.refresh();
			}
			graphComponent.scrollCellToVisible(cell, true);
		});
	}

	public void focusStep(WorkflowStep step) {
		Object cell = layout.getNode(step);

		// If unknown step, it will be silently ignored
		focusCell(cell, false);
	}

	public void rebuildGraph() {
		GuiUtils.checkEDT();

		refreshWorkflowTitle();

		if(isBuilding.compareAndSet(false, true)) {
			try {
				layout.doLayout();
			} finally {
				isBuilding.set(false);
			}
			graphComponent.repaint();

			if(workflow!=null && !workflow.isClosed()) {
				GuiUtils.invokeEDTLater(() -> focusStep(workflow.getActiveStep()));
			}
		}

		refreshActions();
	}

	public void setWorkflow(Workflow workflow) {
		Workflow oldValue = this.workflow;

		if(this.workflow!=null) {
			this.workflow.removeWorkflowListener(handler);
		}

		this.workflow = workflow;

		if(this.workflow!=null) {
			this.workflow.addWorkflowListener(handler);
		}

		firePropertyChange(PROPERTY_WORKFLOW, oldValue, workflow);

		initRebuild();

		if(this.workflow!=null) {
			focusStep(workflow.getActiveStep());
		}
	}

	private WorkflowStep getSelectedStep() {
		Object selectedCell = graph.getSelectionCell();
		if(selectedCell==null) {
			return null;
		}
		return layout.getStep(selectedCell);
	}

	private Set<WorkflowStep> getSelectedSteps() {
		if(graph.getSelectionCount()==0) {
			return Collections.emptySet();
		}

		Object[] selectedCells = graph.getSelectionCells();
		Set<WorkflowStep> steps = new HashSet<>();

		for(Object cell : selectedCells) {
			steps.add(layout.getStep(cell));
		}

		return steps;
	}

	private boolean findExportableResources(Collection<? extends WorkflowStep> steps, Consumer<? super WorkflowStep> action) {
		int count = 0;
		for(WorkflowStep step : steps) {

		}
		return count>0;
	}

	private Object getSelectedCell() {
		return graph.getSelectionCell();
	}

	/**
	 * Returns the workflow that is rendered by this graph
	 *
	 * @return
	 */
	public Workflow getWorkflow() {
		return workflow;
	}

	private void refreshCells(Object... cells) {
		for(Object cell : cells) {
			if(cell instanceof mxICell) {
				layout.refreshNodeStyle((mxICell) cell);
			}
		}
	}

	private void initRebuild() {
		GuiUtils.invokeEDT(this::rebuildGraph);
	}

	private static WorkflowStep createDummyStep(Workflow workflow, int index) {
		WorkflowStep step = workflow.createWorkflowStep();
		step.setRecordingTime(LocalDateTime.now());
		step.setTitle("Step_"+index);
		return step;
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.CloseableUI#close()
	 */
	@Override
	public void close() {
		setWorkflow(null);
	}

	private JPopupMenu createPopupMenu() {
		return actionManager.createPopupMenu("replaydh.ui.core.workflowGraph.popupMenuList", null);
	}

	private void showPopup(MouseEvent trigger) {
		if(popupMenu==null) {
			// Create new popup menu

			popupMenu = createPopupMenu();

			if(popupMenu!=null) {
				popupMenu.pack();
			} else {
				log.error("Unable to create popup menu");
			}
		}

		if(popupMenu!=null) {
			// Ensure popup shows the latest state of actions
			refreshActions();

			popupMenu.show(graphComponent.getGraphControl(), trigger.getX(), trigger.getY());
		}
	}

	private void doExport(final WorkflowExportContext context) {
		final Component owner = getPanel();

		boolean doExport = ExportUtils.showExportWizard(context, owner);

		if(doExport) {
			ExportUtils.performExport(context, owner);
		}
	}

	private class CallbackHandler {

		private WorkflowExportContext initExportContext() {
			WorkflowExportContext context = WorkflowExportContext.create(getWorkflow(), environment);
			context.encoding(StandardCharsets.UTF_8);

			return context;
		}

		private void focusActiveStep() {
			focusStep(workflow.getActiveStep());
		}

		private void compressStep() {
			Object cell = getSelectedCell();
			if(cell==null) {
				return;
			}

			Object focus = layout.compress(cell);
			focusCell(focus, true);

			logStat(StatEntry.ofType(StatType.UI_ACTION, GuiStats.GRAPH_COLLAPSE));
		}

		private void expandStep() {
			Object cell = getSelectedCell();
			if(cell==null) {
				return;
			}

			Object focus = layout.expand(cell);
			focusCell(focus, true);

			logStat(StatEntry.ofType(StatType.UI_ACTION, GuiStats.GRAPH_EXPAND));
		}

		/**
		 * Export metadata for the selected step.
		 */
		private void exportStepMetadata() {
			WorkflowStep step = getSelectedStep();
			if(step==null) {
				return;
			}

			WorkflowExportContext context = initExportContext();
			context.workflowScope(WorkflowScope.STEP);
			context.type(Type.METADATA);
			context.targetStep(step);
			context.steps(Collections.singleton(step));

			doExport(context);
		}

		/**
		 * Export resources from the selected step.
		 */
		private void exportStepResources() {
			WorkflowStep step = getSelectedStep();
			if(step==null) {
				return;
			}

			WorkflowExportContext context = initExportContext();
			context.workflowScope(WorkflowScope.STEP);
			context.type(Type.OBJECT);
			context.targetStep(step);
			context.steps(Collections.singleton(step));
			context.objectScope(ObjectScope.WORKSPACE);

			doExport(context);
		}

		/**
		 * Export metadata for the entire workflow or a selected subset
		 * of steps.
		 */
		private void exportMetadata() {
			WorkflowStep step = getWorkflow().getActiveStep();
			if(step==null) {
				return;
			}

			Set<WorkflowStep> steps = getSelectedSteps();
			if(steps.isEmpty()) {
				steps = getWorkflow().getAllSteps();
			}

			WorkflowExportContext context = initExportContext();
			context.workflowScope(WorkflowScope.PART);
			context.type(Type.METADATA);
			context.targetStep(step);
			context.objectScope(ObjectScope.WORKSPACE);
			context.steps(steps);

			doExport(context);
		}

		/**
		 * Export objects from the currently active step.
		 */
		private void exportResources() {
			WorkflowStep step = getWorkflow().getActiveStep();
			if(step==null) {
				return;
			}

			WorkflowExportContext context = initExportContext();
			context.workflowScope(WorkflowScope.STEP);
			context.type(Type.OBJECT);
			context.targetStep(step);
			context.objectScope(ObjectScope.WORKSPACE);
			context.steps(Collections.singleton(step));

			doExport(context);
		}

		/**
		 * Pull changes from a remote repository to synchronize our local
		 * workspace.
		 */
		private void updateRepository() {
			environment.execute(() -> new GitRemoteUpdater(environment).update(panel));
		}

		/**
		 * Initiate the publication process for the currently active
		 * step.
		 * <p>
		 * Needs the {@code ActionEvent} to figure out what publisher plugin to use.
		 */
		private void publishResources(ActionEvent ae) {

			JComponent menuItem = (JComponent) ae.getSource();
			PluginEngine pluginEngine = environment.getClient().getPluginEngine();
			Extension extension = GuiUtils.resolveExtension(menuItem, pluginEngine);

			// Fetch new publisher instance
			ResourcePublisher publisher;
			try {
				publisher = pluginEngine.instantiate(extension);
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
				log.error("Failed to instantiate resource publisher: {}", extension.getUniqueId(), e);
				GuiUtils.showErrorDialog(panel, e);
				GuiUtils.beep();
				return;
			}

			// Switch execution to background thread
			environment.execute(() -> {

				// Wrap together all relevant info
				final WorkflowExportInfo exportInfo = WorkflowExportInfo.newPublicationBuilder()
						.environment(environment)
						.workflow(workflow)
						.targetStep(workflow.getActiveStep())
						.objectScope(ObjectScope.WORKSPACE)
						.steps(workflow.getAllSteps())
						.workflowScope(WorkflowScope.WORKFLOW)
						.build();

				Exception exceptionToReport = null;

				try {
					// Delegate actual publication work
					publisher.publish(exportInfo);
				} catch (IOException e) {
					// Directly use provided exception (not much else we can do here)
					exceptionToReport = e;
				} catch (ExportException e) {
					// Unwrap exception
					exceptionToReport = (Exception) e.getCause();
				} catch (InterruptedException e) {
					log.info("Publication cancelled by user");
					return;
				}

				if(exceptionToReport!=null) {
					log.error("Initiating publication process failed", exceptionToReport);

					final Throwable throwable = exceptionToReport;

					GuiUtils.invokeEDTLater(() -> GuiUtils.showErrorDialog(panel,
							ResourceManager.getInstance().get("replaydh.ui.core.workflowGraph.publishResources.errorTitle"),
							ResourceManager.getInstance().get("replaydh.ui.core.workflowGraph.publishResources.errorMessage"),
							throwable));

					return;
				}

				//TODO add a marker tag to the commit when we published it!
			});

		}

		/**
		 * Set flag for compressing pipes and then rebuild graph.
		 */
		private void toggleCompressPipes(boolean value) {
			if(layout instanceof WorkflowGraphDelegatingLayout) {
				((WorkflowGraphDelegatingLayout)layout).setHidePipes(value);
				rebuildGraph();
			}
		}

		/**
		 * Rebuild graph.
		 */
		private void refreshGraph() {
			initRebuild();
		}

		/**
		 * Move active step to the single selected one.
		 */
		private void changeActiveStep() {
			final Object cell = getSelectedCell();
			// Ignore request if no cells selected or only selected cell has hidden steps
			if(cell==null || (cell instanceof WorkflowNode
					&& ((WorkflowNode)cell).hasHiddenSteps())) {
				return;
			}

			final WorkflowStep newActiveStep = layout.getStep(cell);
			final WorkflowStep oldActiveStep = workflow.getActiveStep();

			if(newActiveStep==oldActiveStep) {
				return;
			}

			final Object oldCell = layout.getNode(oldActiveStep);

			new AbstractDialogWorker<Boolean, Object>(SwingUtilities.getWindowAncestor(panel),
					ResourceManager.getInstance().get("replaydh.dialogs.changeActiveStep.title")
					, CancellationPolicy.NO_CANCEL) {
				@Override
				protected Boolean doInBackground() throws Exception {
					workflow.setActiveStep(newActiveStep);
					return true;
				}
				@Override
				protected String getMessage(MessageType messageType, Throwable t) {
					ResourceManager rm = ResourceManager.getInstance();

					switch (messageType) {
					case RUNNING:
						log.info("Changing active step from {} to {}", oldActiveStep.getTitle(), newActiveStep.getTitle());
						return rm.get("replaydh.dialogs.changeActiveStep.message",
								oldActiveStep.getTitle(), newActiveStep.getTitle());
					case FAILED:
						log.error("Changing active workflow step to {} failed", newActiveStep.getTitle(), t);
						return rm.get("replaydh.dialogs.changeActiveStep.failed",
								newActiveStep.getTitle(), t.getMessage());
					case FINISHED:
						log.info("Finished changing active workflow step to {}", newActiveStep.getTitle());
						return rm.get("replaydh.dialogs.changeActiveStep.done", newActiveStep.getTitle());

					default:
						throw new IllegalArgumentException("Unknown or unsupported message type: "+messageType);
					}
				}
				@Override
				protected void doneImpl(Boolean result) {
					refreshCells(cell, oldCell);
					focusStep(newActiveStep);
				};
			}.start();
		}

		private void loadDummyWorkflow() {
			Workflow workflow = new DefaultWorkflow(WorkflowSchema.getDefaultSchema());

			WorkflowStep[] steps = new WorkflowStep[10];
			for(int i=0; i<steps.length; i++) {
				steps[i] = createDummyStep(workflow, i);
			}

			// Linear workflow
//			workflow.addWorkflowStep(steps[0]);
//			workflow.addWorkflowStep(steps[1]);
//			workflow.addWorkflowStep(steps[2]);
//			workflow.addWorkflowStep(steps[3]);

			// Branched unmerged workflow
			workflow.addWorkflowStep(steps[0]);
			workflow.addWorkflowStep(steps[1]);
			workflow.addWorkflowStep(steps[2]);
			workflow.addWorkflowStep(steps[3]);
			try {
				workflow.setActiveStep(steps[0]);
			} catch (InterruptedException e) {
				throw new RDHException("Shouldn't have happened", e);
			}
			workflow.addWorkflowStep(steps[4]);
			workflow.addWorkflowStep(steps[5]);
			workflow.addWorkflowStep(steps[6]);

			setWorkflow(workflow);
		}
	}

	private class Handler extends MouseAdapter implements WorkflowListener, mxIEventListener {

		/**
		 * Check to make sure that we only ever react to
		 * events from the workflow we should be rendering.
		 */
		private boolean isRelevantWorkflow(Workflow workflow) {
			return workflow == getWorkflow();
		}

		/**
		 * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseClicked(MouseEvent e) {
			if(!e.isConsumed() && e.getClickCount()==2) {
				Point p = e.getPoint();
				Object cell = graphComponent.getCellAt(p.x, p.y);
				if(cell!=null) {
					Object focus = layout.toggle(cell);
					focusCell(focus, true);
				}
			}
		}

		protected void maybeShowPopup(MouseEvent e) {
			if(!e.isConsumed() && e.isPopupTrigger()) {
				showPopup(e);
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			if(!isWheelZoomEnabled()) {
				return;
			}

			if (e.isControlDown()) {
				try {
					if (e.getPreciseWheelRotation() < 0) {
						graphComponent.zoomIn();
					} else {
						graphComponent.zoomOut();
					}
				} catch(Exception ex) {
					log.error("Failed to handle wheel-zoom command for event: {}", e, ex); //$NON-NLS-1$
				}

				e.consume();
			}
		}

		private void maybeInitRebuild(Workflow workflow) {
			if(isRelevantWorkflow(workflow)) {
				WorkflowGraph.this.initRebuild();
			}
		}

		/**
		 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
		 */
		@Override
		public void stateChanged(ChangeEvent e) {
			maybeInitRebuild((Workflow) e.getSource());
		}

		/**
		 * @see bwfdm.replaydh.workflow.WorkflowListener#workflowStepAdded(bwfdm.replaydh.workflow.Workflow, bwfdm.replaydh.workflow.WorkflowStep)
		 */
		@Override
		public void workflowStepAdded(Workflow workflow, WorkflowStep step) {
			maybeInitRebuild(workflow);
		}

		/**
		 * @see bwfdm.replaydh.workflow.WorkflowListener#workflowStepRemoved(bwfdm.replaydh.workflow.Workflow, bwfdm.replaydh.workflow.WorkflowStep)
		 */
		@Override
		public void workflowStepRemoved(Workflow workflow, WorkflowStep step) {
			maybeInitRebuild(workflow);
		}

		/**
		 * @see bwfdm.replaydh.workflow.WorkflowListener#workflowStepChanged(bwfdm.replaydh.workflow.Workflow, bwfdm.replaydh.workflow.WorkflowStep)
		 */
		@Override
		public void workflowStepChanged(Workflow workflow, WorkflowStep step) {
			//TODO make it so that we don't rebuild the entire graph for changes on a single step
			maybeInitRebuild(workflow);
		}

		/**
		 * @see bwfdm.replaydh.workflow.WorkflowListener#workflowStepPropertyChanged(bwfdm.replaydh.workflow.Workflow, bwfdm.replaydh.workflow.WorkflowStep, java.lang.String)
		 */
		@Override
		public void workflowStepPropertyChanged(Workflow workflow, WorkflowStep step, String propertyName) {
			maybeInitRebuild(workflow);
		}

		/**
		 * @see bwfdm.replaydh.workflow.WorkflowListener#activeWorkflowStepChanged(bwfdm.replaydh.workflow.Workflow, bwfdm.replaydh.workflow.WorkflowStep, bwfdm.replaydh.workflow.WorkflowStep)
		 */
		@Override
		public void activeWorkflowStepChanged(Workflow workflow, WorkflowStep oldActiveStep,
				WorkflowStep newActiveStep) {
			maybeInitRebuild(workflow);
		}

		/**
		 * @see com.mxgraph.util.mxEventSource.mxIEventListener#invoke(java.lang.Object, com.mxgraph.util.mxEventObject)
		 */
		@Override
		public void invoke(Object sender, mxEventObject evt) {
			if(mxEvent.CHANGE.equals(evt.getName())) {
				refreshActions();
			}
		}

	}
}

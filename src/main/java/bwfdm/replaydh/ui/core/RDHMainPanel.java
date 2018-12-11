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
package bwfdm.replaydh.ui.core;

import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Forms;
import com.jgoodies.forms.factories.Paddings;

import bwfdm.replaydh.core.RDHClient;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.core.Workspace;
import bwfdm.replaydh.git.JGitAdapter;
import bwfdm.replaydh.io.FileTracker;
import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.io.LocalFileObject;
import bwfdm.replaydh.io.TrackerException;
import bwfdm.replaydh.io.TrackerListener;
import bwfdm.replaydh.io.TrackingAction;
import bwfdm.replaydh.io.TrackingStatus;
import bwfdm.replaydh.io.resources.FileResource;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.stats.Interval;
import bwfdm.replaydh.stats.StatEntry;
import bwfdm.replaydh.stats.StatType;
import bwfdm.replaydh.ui.GuiStats;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.actions.ActionManager;
import bwfdm.replaydh.ui.config.PreferencesDialog;
import bwfdm.replaydh.ui.core.RDHChangeWorkspaceWizard.ChangeWorkspaceContext;
import bwfdm.replaydh.ui.core.ResourceDragController.Mode;
import bwfdm.replaydh.ui.helper.CloseableUI;
import bwfdm.replaydh.ui.helper.JMenuBarSource;
import bwfdm.replaydh.ui.helper.PassiveTextArea;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.ui.icons.Resolution;
import bwfdm.replaydh.ui.workflow.AddWorkflowSchemaWizard;
import bwfdm.replaydh.ui.workflow.AddWorkflowSchemaWizard.AddWorkflowSchemaContext;
import bwfdm.replaydh.ui.workflow.FileIgnoreEditor;
import bwfdm.replaydh.ui.workflow.FileIgnoreEditor.FileIgnoreConfiguration;
import bwfdm.replaydh.ui.workflow.WorkflowStepUIEditor;
import bwfdm.replaydh.ui.workflow.WorkflowUIUtils;
import bwfdm.replaydh.ui.workflow.WorkspaceTrackerPanel;
import bwfdm.replaydh.ui.workflow.graph.WorkflowGraph;
import bwfdm.replaydh.ui.workflow.graph.WorkflowStepShape;
import bwfdm.replaydh.utils.RDHUtils;
import bwfdm.replaydh.utils.annotation.Experimental;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifiable.Role;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.ResourceCache;
import bwfdm.replaydh.workflow.ResourceCache.CacheEntry;
import bwfdm.replaydh.workflow.ResourceCache.CacheListener;
import bwfdm.replaydh.workflow.Tool;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowStep;
import bwfdm.replaydh.workflow.impl.DefaultResource;
import bwfdm.replaydh.workflow.resolver.IdentifiableResolver;
import bwfdm.replaydh.workflow.schema.IdentifierType;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public class RDHMainPanel extends JPanel implements CloseableUI, JMenuBarSource {

	private static final long serialVersionUID = 7399427219955511287L;

	private static final Logger log = LoggerFactory.getLogger(RDHMainPanel.class);

	/**
	 * Per default assume 1 minute intervals.
	 * <p>
	 * Value is in seconds.
	 */
	private static final int DEFAULT_REFRESH_INTERVAL = 60;

	private static final boolean DEFAULT_TRACKER_ACTIVE = false;

	private static volatile ActionManager sharedActionManager;

	private static ActionManager getSharedActionManager() {
		GuiUtils.checkEDT();

		ActionManager actionManager = sharedActionManager;
		if(actionManager==null) {
			actionManager = ActionManager.globalManager().derive();
			try {
				actionManager.loadActions(RDHMainPanel.class.getResource("main-panel-actions.xml"));
			} catch (IOException e) {
				throw new RDHException("Failed to load actions for"+RDHMainPanel.class, e);
			}

			sharedActionManager = actionManager;
		}

		return actionManager;
	}

	private final WorkflowGraph workflowGraph;

	private final WorkspaceTrackerPanel workspaceTrackerPanel;

	private final ActionManager actionManager;
	private final ActionManager.ActionMapper actionMapper;

	private final RDHEnvironment environment;

	private final Supplier<Workflow> workflowSource;
	private final FileTracker fileTracker;
	private final ResourceCache resourceCache;

//	private final JToolBar toolBar;

	private final JTabbedPane tabbedPane;

	private final JPanel controlPanel;
	private final JPanel detailPanel;
	private final FileTrackerPanel fileTrackerPanel;
	private final FileCachePanel fileCachePanel;

	private final AbstractButton toggleDetailsButton;
	private final Icon expandIcon, collapseIcon;

	private boolean isExpanded = false;

	private final Handler handler;

	private final PropertyChangeListener fileTrackerChangeListener;
	private final PropertyChangeListener environmentChangeListener;

	/**
	 * Link to current background updater task.
	 * Access needs to be synchronized externally.
	 */
	private volatile ScheduledFuture<?> updateTask;
	private volatile boolean executingUpdate = false;

	/**
	 * Link to current commit preparation task.
	 * Access only on the EDT, so no additional
	 * synchronization required.
	 */
	private volatile InteractiveCommitTask activeCommitTask;

	/**
	 * Shared point of synchronization of all critical resources
	 * this class manages internally, such as the background
	 * update task.
	 */
	private final Object lock = new Object();

	public RDHMainPanel(RDHEnvironment environment) {
		super(new BorderLayout());

		this.environment = requireNonNull(environment);
		RDHClient client = environment.getClient();

		handler = new Handler();
		fileTrackerChangeListener = handler::onFileTrackerPropertyChange;
		environmentChangeListener = handler::onEnvironmentPropertyChange;

		workflowSource = client.getWorkflowSource();
		fileTracker = client.getFileTracker();
		fileTracker.addPropertyChangeListener(JGitAdapter.NAME_WORKFLOW, fileTrackerChangeListener);

		environment.addPropertyChangeListener(RDHProperty.CLIENT_UI_ALWAYS_ON_TOP, environmentChangeListener);

		resourceCache = client.getResourceCache();
		resourceCache.addCacheListener(handler);

		// Load our basic actions
		actionManager = getSharedActionManager().derive();
		actionMapper = actionManager.mapper(this);


		/**
		 * <pre>
		 * +---------------------------------------------+
		 * |			   toolbar                       |
		 * +--------+------------------------+-----------+
		 * |        |                        | +-++-++-+ |
		 * |        |                        | |A||B||C| |
		 * |        |                        | +-++-++-+ |
		 * |        |                        | +------+  |
		 * |        |                        | |  D   |  |
		 * |        |    content panel       | +------+  |
		 * |        |                        | +------+  |
		 * |        |                        | |  E   |  |
		 * |        |                        | +------+  |
		 * |        |                        | +------+  |
		 * |        |                        | |  F   |  |
		 * |        |                        | +------+  |
		 * +--------+------------------------+-----------+
		 * |			   footer                        |  <- needed?
		 * +---------------------------------------------+
		 * </pre>
		 *
		 * A/B/C  Buttons to collapse/expand main window and control
		 * D  Button to start workflow step recording (dialog)
		 * E  Outline for small status display of file tracker
		 * F  Drop area for file cache (preliminary storage of input resource descriptions)
		 */

		ResourceManager rm = ResourceManager.getInstance();
		IconRegistry ir = IconRegistry.getGlobalRegistry();

		workflowGraph = new WorkflowGraph(environment);
		workspaceTrackerPanel = new WorkspaceTrackerPanel(environment);

		tabbedPane = new JTabbedPane();

		tabbedPane.insertTab(
				rm.get("replaydh.ui.core.mainPanel.tabs.workflow.name"),
				ir.getIcon("Data-Workflow-icon.png", Resolution.forSize(32)),
				workflowGraph.getPanel(),
				rm.get("replaydh.ui.core.mainPanel.tabs.workflow.description"),
				0);

		tabbedPane.insertTab(
				rm.get("replaydh.ui.core.mainPanel.tabs.fileTracker.name"),
				ir.getIcon("data-monitor-icon.png", Resolution.forSize(32)),
				workspaceTrackerPanel,
				rm.get("replaydh.ui.core.mainPanel.tabs.fileTracker.description"),
				1);

		// Not the cleanest way, but ensure we don't overgrow
		tabbedPane.setPreferredSize(new Dimension(700, 500));

		fileTrackerPanel = new FileTrackerPanel();

		fileCachePanel = new FileCachePanel();

		collapseIcon = ir.getIcon("right.png", Resolution.forSize(32));
		expandIcon = ir.getIcon("left.png", Resolution.forSize(32));

		AbstractButton updateStatusButton = actionManager.createButton("replaydh.ui.core.mainPanel.updateStatus");
		AbstractButton cancelUpdateButton = actionManager.createButton("replaydh.ui.core.mainPanel.cancelUpdate");
		AbstractButton toggleTrackerButton = actionManager.createButton("replaydh.ui.core.mainPanel.toggleTrackerActive");
		toggleDetailsButton = actionManager.createButton("replaydh.ui.core.mainPanel.toggleDetails");

		AbstractButton addStepButton = actionManager.createButton("replaydh.ui.core.mainPanel.addStep");
		addStepButton.setHorizontalTextPosition(SwingConstants.LEFT);
		addStepButton.setFont(GuiUtils.defaultLargeInfoFont.deriveFont(24f));
		addStepButton.setText(rm.get("replaydh.ui.core.mainPanel.addStep.name"));

		AbstractButton openWorkspaceButton = actionManager.createButton("replaydh.ui.core.mainPanel.openWorkspaceFolder");
		AbstractButton clearResourceCacheButton = actionManager.createButton("replaydh.ui.core.mainPanel.clearResourceCache");

		controlPanel = FormBuilder.create()
				.columns("pref:grow:fill, 3dlu, pref")
				.rows("pref, 12dlu, pref, 6dlu, pref, max(50dlu;pref), max(pref;30dlu), max(pref;30dlu):grow:fill, 6dlu")
				.padding(Paddings.DLU14)
				.add(Forms.buttonBar(toggleDetailsButton, toggleTrackerButton, updateStatusButton, cancelUpdateButton)).xyw(1, 1, 3)
				.add(addStepButton).xyw(1, 3, 3)
				.addSeparator(rm.get("replaydh.panels.workspaceTracker.title")).xy(1, 5)
				.add(openWorkspaceButton).xy(3, 5)
				.add(fileTrackerPanel).xyw(1, 6, 3)
				.addSeparator(rm.get("replaydh.panels.fileCache.title")).xy(1, 7)
				.add(clearResourceCacheButton).xy(3, 7)
				.add(fileCachePanel).xyw(1, 8, 3)
				.build();

		detailPanel = new JPanel(new BorderLayout());
		detailPanel.add(tabbedPane, BorderLayout.CENTER);

		toggleSize(false);

		//TODO add left outline and footer ?

		registerActions();

		// If required start performing periodic updates of file tracker status data
		boolean trackerActive = environment.getBoolean(RDHProperty.CLIENT_WORKSPACE_TRACKER_ACTIVE, DEFAULT_TRACKER_ACTIVE);
		if(trackerActive) {
			if(environment.getClient().isVerbose()) {
				log.info("Starting periodic update for file tracker status info");
			}
			handler.schedulePeriodicFileTrackerUpdate();
		}

		addHierarchyListener(handler);

		showInitialOutline();
	}

	private void logStat(StatEntry entry) {
		environment.getClient().getStatLog().log(entry);
	}

	private boolean isVerbose() {
		return environment.getClient().isVerbose();
	}

	private void toggleSize(boolean expand) {
		if((expand && isExpanded) || (!expand && !isExpanded && controlPanel.getParent()==this)) {
			return;
		}

		Point anchor = null;

		Window window = SwingUtilities.getWindowAncestor(this);
		boolean isRDHFrame = window instanceof RDHFrame;
		if(window!=null) {
			anchor = window.getLocationOnScreen();
			anchor.x += window.getWidth();
		}

		boolean needPacking = true;

		// Show both the control panel and the detail outline to the left
		if(expand) {
			remove(controlPanel);

			// Init and arrange other components
			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, detailPanel, controlPanel);
			splitPane.setResizeWeight(1);
			splitPane.setOneTouchExpandable(true);

			add(splitPane, BorderLayout.CENTER);

			if(isRDHFrame) {
				needPacking = !((RDHFrame)window).restoreSize();
			}
		} else {
			// Shrink down to control panel

			if(isRDHFrame) {
				((RDHFrame)window).saveSize();
			}

			Container splitPane = SwingUtilities.getAncestorOfClass(JSplitPane.class, controlPanel);
			if(splitPane!=null) {
				splitPane.removeAll();
				remove(splitPane);
			}

			add(controlPanel, BorderLayout.CENTER);
		}

		// Refresh internal (visual) states
		isExpanded = expand;
		toggleDetailsButton.setIcon(expand ? collapseIcon : expandIcon);

		// Update window and keep top-right anchor
		if(window!=null) {

			// Only pack if we haven't been bale to restore previous size
			if(needPacking) {
				window.pack();
			}
			anchor.x -= window.getWidth();
			window.setLocation(anchor);
		}
	}

	private void showInitialOutline() {
		workflowGraph.setWorkflow(workflowSource.get());

		showPanel(workflowGraph.getPanel());

		// If the client doesn't have workspace set up yet -> invoke the wizard
		Workspace workspace = environment.getWorkspace();
		if(workspace==null) {
			if(environment.getClient().isVerbose()) {
				log.info("No workspace selected - initiating workspace change wizard");
			}
			// Show workspace change wizard, but exit client if user cancels the dialog
			GuiUtils.invokeEDT(() -> handler.doChangeWorkspace(true));
		}
	}

	/**
	 * Initiates loading the new workspace in a background thread.
	 */
	private void loadWorkspace(final ChangeWorkspaceContext context) {
		requireNonNull(context);

		String message = ResourceManager.getInstance().get("replaydh.ui.core.mainPanel.changeInProgress.message");
		String title = ResourceManager.getInstance().get("replaydh.ui.core.mainPanel.changeInProgress.title");

		JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.CANCEL_OPTION);
		JDialog dialog = pane.createDialog(title);

		final SwingWorker<Workspace, Object> worker = new SwingWorker<Workspace, Object>() {

			@Override
			protected Workspace doInBackground() throws Exception {
				RDHClient client = environment.getClient();
				Workspace workspace = null;

				/*
				 *  Depending on the amount of information either create
				 *  a new workspace or expect a fully functional and properly
				 *  set up git repository with our config file at the
				 *  specified location.
				 */
				if(context.getSchema()==null) {
					workspace = client.loadWorkspace(context.getWorkspacePath());
				} else {
					workspace = client.createWorkspace(
							context.getWorkspacePath(),
							context.getSchema(),
							context.getTitle(),
							context.getDescription());
				}

				return workspace;
			}

			@Override
			protected void done() {
				dialog.dispose();
			}
		};

		worker.execute();
		dialog.setVisible(true);

		Object value = pane.getValue();
		if(value instanceof Integer && ((Integer)value).intValue()==JOptionPane.CANCEL_OPTION) {
			worker.cancel(true);
		}
	}

	private void showPanel(Component component) {
		GuiUtils.checkEDT();

		tabbedPane.setSelectedComponent(component);

		refreshActions();
	}

	@Override
	public JMenuBar createJMenuBar() {
		return actionManager.createMenuBar("replaydh.ui.core.mainPanel.menuBarList", null);
	}

	private void registerActions() {

		actionMapper.mapTask("replaydh.ui.core.mainPanel.addStep", handler::addWorkflowStep);
		actionMapper.mapTask("replaydh.ui.core.mainPanel.exit", handler::exitClient);
		actionMapper.mapTask("replaydh.ui.core.mainPanel.restart", handler::restartClient);
		actionMapper.mapTask("replaydh.ui.core.mainPanel.about", handler::showAboutDialog);
		actionMapper.mapTask("replaydh.ui.core.mainPanel.preferences", handler::showPreferencesDialog);

		actionMapper.mapTask("replaydh.ui.core.mainPanel.openWorkspaceFolder", handler::openWorkspaceFolder);
		actionMapper.mapTask("replaydh.ui.core.mainPanel.clearResourceCache", handler::clearResourceCache);

		actionMapper.mapTask("replaydh.ui.core.mainPanel.exportStatistics", handler::exportStatistics);
		actionMapper.mapTask("replaydh.ui.core.mainPanel.resetStatistics", handler::resetStatistics);

		actionMapper.mapTask("replaydh.ui.core.mainPanel.updateStatus", handler::scheduleSingleFileTrackerUpdate);
		actionMapper.mapTask("replaydh.ui.core.mainPanel.cancelUpdate", handler::cancelFileTrackerUpdate);
		actionMapper.mapTask("replaydh.ui.core.mainPanel.toggleDetails", handler::toggleDetails);
		actionMapper.mapToggle("replaydh.ui.core.mainPanel.toggleAlwaysOnTop", handler::toggleAlwaysOnTop);
		actionMapper.mapToggle("replaydh.ui.core.mainPanel.toggleTrackerActive", handler::toggleTrackerStatus);
		actionMapper.mapTask("replaydh.ui.core.mainPanel.changeWorkspace", handler::changeWorkspace);
		actionMapper.mapTask("replaydh.ui.core.mainPanel.importWorkflowSchema", handler::importWorkflowSchema);
	}

	private ScheduledFuture<?> getOrClearUpdateTask() {
		synchronized (lock) {
			ScheduledFuture<?> task = updateTask;
			if(task!=null && task.isDone()) {
				updateTask = null;
			}

			return updateTask;
		}
	}

	private boolean fileTrackerHasFiles() {
		try {
			return fileTracker.hasStatusInfo()
					&& (fileTracker.hasFilesForStatus(TrackingStatus.MISSING)
					|| fileTracker.hasFilesForStatus(TrackingStatus.MODIFIED)
					|| fileTracker.hasFilesForStatus(TrackingStatus.UNKNOWN));
		} catch(TrackerException e) {
			log.error("Failed to ask file tracker for file statuses", e);
			return false;
		}
	}

	private void refreshActions() {
		GuiUtils.checkEDT();

		Workflow workflow = workflowSource.get();

		boolean canAddStep = workflow!=null && !workflow.isUpdating();
				/*&& (activeCommitTask==null || activeCommitTask.isDone())
				&& fileTrackerHasFiles();*/

		actionManager.setEnabled(canAddStep, "replaydh.ui.core.mainPanel.addStep");

		ScheduledFuture<?> task = getOrClearUpdateTask();
		boolean taskRunning = task!=null && !task.isDone();
		boolean canUpdate = !taskRunning;
		boolean canCancel = taskRunning && executingUpdate;
		boolean timerActive = taskRunning && task.getDelay(TimeUnit.MILLISECONDS)>0L;

		boolean canOpenWorkspace = Desktop.isDesktopSupported() && environment.getWorkspace()!=null;

		boolean canClearResourceCache = !resourceCache.isEmpty();

		actionManager.setEnabled(canUpdate, "replaydh.ui.core.mainPanel.updateStatus");
		actionManager.setEnabled(canCancel, "replaydh.ui.core.mainPanel.cancelUpdate");
		actionManager.setSelected(timerActive, "replaydh.ui.core.mainPanel.toggleTrackerActive");

		actionManager.setEnabled(canOpenWorkspace, "replaydh.ui.core.mainPanel.openWorkspaceFolder");
		actionManager.setEnabled(canClearResourceCache, "replaydh.ui.core.mainPanel.clearResourceCache");
	}

	private void refreshWorkflowFromSource() {
		if(workflowGraph!=null) {
			workflowGraph.setWorkflow(workflowSource.get());
		}

		refreshActions();
	}

	private boolean setAlwaysOnTop(boolean alwaysOnTop) {
		Window window = SwingUtilities.getWindowAncestor(RDHMainPanel.this);
		if(window!=null) {
			window.setAlwaysOnTop(alwaysOnTop);
		}

		return window!=null;
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.CloseableUI#close()
	 */
	@Override
	public void close() {
		// Close default internals
		CloseableUI.tryClose(workflowGraph, workspaceTrackerPanel, fileTrackerPanel, fileCachePanel);
		actionMapper.dispose();

		removeHierarchyListener(handler);
		fileTracker.removePropertyChangeListener(JGitAdapter.NAME_WORKFLOW, fileTrackerChangeListener);
		environment.removePropertyChangeListener(RDHProperty.CLIENT_UI_ALWAYS_ON_TOP, environmentChangeListener);

		// Finally make sure our background task gets canceled
		handler.cancelFileTrackerUpdate();
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.CloseableUI#canClose()
	 */
	@Override
	public boolean canClose() {
		return CloseableUI.canClose(workflowGraph, workspaceTrackerPanel, fileTrackerPanel, fileCachePanel);
	}

	/**
	 * For testing purposes commits all currently pending files
	 * and file changes as a new generic workflow step.
	 * @throws TrackerException
	 */
	@Experimental
	private void debugCommitAllFiles() throws TrackerException {
		if(!fileTracker.hasStatusInfo()) {
			return;
		}

		Workflow workflow = workflowSource.get();
		WorkflowSchema schema = workflow.getSchema();

		workflow.beginUpdate();
		try {
			WorkflowStep newStep = workflow.createWorkflowStep();

			newStep.setTitle("Random step "+System.currentTimeMillis()%100);
			newStep.setRecordingTime(LocalDateTime.now());

			Set<Path> filesToAdd = null;
			Set<Path> filesToRemove = null;

			if(fileTracker.hasFilesForStatus(TrackingStatus.UNKNOWN)) {
				filesToAdd = fileTracker.getFilesForStatus(TrackingStatus.UNKNOWN);
				for(Path file : filesToAdd) {
					//TODO if real commit, try to resolve identifiables here
					Resource resource = DefaultResource.withResourceType("other");
					resource.addIdentifier(new Identifier(schema.getDefaultPathIdentifierType(),
							file.toString(), environment.getWorkspacePath().toString()));
					newStep.addOutput(resource);
				}
			}

			if(fileTracker.hasFilesForStatus(TrackingStatus.MISSING)) {
				filesToRemove = fileTracker.getFilesForStatus(TrackingStatus.MISSING);
			}

			if(filesToAdd!=null && !filesToAdd.isEmpty()) {
				fileTracker.applyTrackingAction(filesToAdd, TrackingAction.ADD);

			}

			if(filesToRemove!=null && !filesToRemove.isEmpty()) {
				fileTracker.applyTrackingAction(filesToRemove, TrackingAction.REMOVE);
			}

			workflow.addWorkflowStep(newStep);

		} finally {
			workflow.endUpdate();
		}
	}

	/**
	 * Present the user with an editor for managing the content of
	 * a new workflow step.
	 */
	private boolean interactiveCommit(WorkflowStep newStep) {
		GuiUtils.checkEDT();

		// Delegate actual user interaction to editor implementation
		WorkflowStepUIEditor editor = new WorkflowStepUIEditor(environment);
		editor.setEditingItem(newStep);

		Frame frame = GuiUtils.getFrame(this);
		boolean opSuccess = false;
		String title = ResourceManager.getInstance().get("replaydh.ui.core.mainPanel.addStep.name");

		Interval uptime = new Interval().start();
		logStat(StatEntry.withData(StatType.UI_OPEN, GuiStats.DIALOG_ADD_STEP, newStep.getId()));
		try {
			opSuccess = GuiUtils.showEditorDialogWithControl(frame, editor, title, true);
		} catch(Exception e) {
			GuiUtils.beep();
			GuiUtils.showErrorDialog(frame,
					"replaydh.panels.workflow.dialogs.errorTitle",
					"replaydh.panels.workflow.dialogs.addWorkflowStepFailed", e);
		} finally {
			logStat(StatEntry.withData(StatType.UI_CLOSE, GuiStats.DIALOG_ADD_STEP,
					newStep.getId(), String.valueOf(opSuccess),
					uptime.stop().asDurationString()));
		}

		return opSuccess;
	}

	private boolean interactiveFilterFiles(FileIgnoreConfiguration configuration) {

		FileIgnoreEditor editor = new FileIgnoreEditor();

		editor.setEditingItem(configuration);

		Frame frame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, this);
		String title = ResourceManager.getInstance().get("replaydh.ui.editor.ignoreFiles.title");

		return GuiUtils.showEditorDialogWithControl(frame, editor, title, true);
	}

	/**
	 * Compact outline for file tracker status in the right control area.
	 *
	 * @author Markus Gärtner
	 *
	 */
	private class FileTrackerPanel extends JPanel implements TrackerListener, CloseableUI {

		private static final long serialVersionUID = 3544350331097979750L;

		private final JLabel timeLabel, newFilesLabel, missingFilesLabel, modifiedFilesLabel, corruptedFilesLabel;

		private final JTextArea header;
		private final JLabel iconHeader;
		private final JPanel outlinePanel;

		private static final String NA = "-";

		private FileTrackerPanel() {
			super(new BorderLayout());

			setBorder(Paddings.DLU2);

			ResourceManager rm = ResourceManager.getInstance();
			iconHeader = new JLabel();

			header = new PassiveTextArea();

			timeLabel = createLabel(null);
			newFilesLabel = createLabel(null);
			missingFilesLabel = createLabel(null);
			modifiedFilesLabel = createLabel(null);
			corruptedFilesLabel = createLabel(null);

			outlinePanel = FormBuilder.create()
					.columns("pref, 5dlu, fill:pref:grow")
					.rows("pref, 3dlu, pref, $nlg, pref, $nlg, pref, $nlg, pref")
					.add(createLabel(rm.get("replaydh.ui.core.mainPanel.lastUpdateTime")+":")).xy(1, 1)
						.add(timeLabel).xy(3, 1)
					.add(createLabel(rm.get("replaydh.trackingStatus.unknown")+":")).xy(1, 3)
						.add(newFilesLabel).xy(3, 3)
					.add(createLabel(rm.get("replaydh.trackingStatus.missing")+":")).xy(1, 5)
						.add(missingFilesLabel).xy(3, 5)
					.add(createLabel(rm.get("replaydh.trackingStatus.modified")+":")).xy(1, 7)
						.add(modifiedFilesLabel).xy(3, 7)
					.add(createLabel(rm.get("replaydh.trackingStatus.corrupted")+":")).xy(1, 9)
						.add(corruptedFilesLabel).xy(3, 9)
					.build();

			JPanel headerPanel = FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("pref, pref")
					.add(header).xy(1, 1)
					.add(iconHeader).xy(1, 2)
					.build();

			add(headerPanel, BorderLayout.NORTH);
			add(outlinePanel, BorderLayout.CENTER);

			reset();

			fileTracker.addTrackerListener(this);
		}

		private JLabel createLabel(String text) {
			JLabel label = new JLabel(text);
			label.setFont(GuiUtils.defaultSmallInfoFont);
			label.setHorizontalAlignment(SwingConstants.LEFT);
			return label;
		}

		public void reset() {
			refresh("replaydh.panels.workspaceTracker.unknownStatus", false, false);
		}

		@Override
		public void close() {
			fileTracker.removeTrackerListener(this);
		}

		private void refresh(String headerKey, boolean showLoadingIcon, boolean showOutline) {

			// Attempt to display outline first so we can prevent redundant file count queries
			if(showOutline) {
				LocalDateTime lastUpdateTime = fileTracker.getLastUpdateTime();
				String timeText = lastUpdateTime==null ? NA : GuiUtils.getTimeFormatter().format(lastUpdateTime);
				timeLabel.setText(timeText);

				int totalFileCount = 0;
				totalFileCount += updateLabel(newFilesLabel, TrackingStatus.UNKNOWN);
				totalFileCount += updateLabel(missingFilesLabel, TrackingStatus.MISSING);
				totalFileCount += updateLabel(modifiedFilesLabel, TrackingStatus.MODIFIED);
				totalFileCount += updateLabel(corruptedFilesLabel, TrackingStatus.CORRUPTED);

				if(totalFileCount==0) {
					showOutline = false;
					headerKey = "replaydh.panels.workspaceTracker.unchangedState";
				}
			}
			outlinePanel.setVisible(showOutline);

			// Configure the header label now
			String text = null;
			if(headerKey!=null) {
				text = ResourceManager.getInstance().get(headerKey);
			}
			header.setText(text);
			header.setVisible(text!=null);

			Icon icon = null;
			if(showLoadingIcon) {
				icon = IconRegistry.getGlobalRegistry().getIcon("loading-64.gif", Resolution.forSize(64));
			}
			iconHeader.setIcon(icon);
			iconHeader.setVisible(icon!=null);
		}

		private int fileCountForStatus(TrackingStatus status) {
			try {
				return fileTracker.getFileCountForStatus(status);
			} catch (TrackerException e) {
				log.error("Failed to query file tracker for files with status: "+status, e);
				return -1;
			}
		}

		private int updateLabel(JLabel label, TrackingStatus status) {
			int fileCount = fileCountForStatus(status);
			String text = fileCount<0 ? NA : String.valueOf(fileCount);

			label.setText(text);

			return fileCount;
		}

		/**
		 * @see bwfdm.replaydh.io.TrackerListener#refreshStarted(bwfdm.replaydh.io.FileTracker)
		 */
		@Override
		public void refreshStarted(FileTracker tracker) {
			refresh("replaydh.panels.workspaceTracker.updateRunning", true, false);
		}

		/**
		 * @see bwfdm.replaydh.io.TrackerListener#refreshFailed(bwfdm.replaydh.io.FileTracker, java.lang.Exception)
		 */
		@Override
		public void refreshFailed(FileTracker tracker, Exception e) {
			refresh("replaydh.panels.workspaceTracker.updateFailed", false, false);
		}

		/**
		 * @see bwfdm.replaydh.io.TrackerListener#refreshDone(bwfdm.replaydh.io.FileTracker, boolean)
		 */
		@Override
		public void refreshDone(FileTracker tracker, boolean canceled) {
			refresh(null, false, true);
		}

		/**
		 * @see bwfdm.replaydh.io.TrackerListener#trackingStatusChanged(bwfdm.replaydh.io.FileTracker, java.util.Set, bwfdm.replaydh.io.TrackingAction)
		 */
		@Override
		public void trackingStatusChanged(FileTracker tracker, Set<Path> files, TrackingAction action) {
			refresh("replaydh.panels.workspaceTracker.unknownStatus", false, false);
		}

		/**
		 * @see bwfdm.replaydh.io.TrackerListener#statusInfoChanged(bwfdm.replaydh.io.FileTracker)
		 */
		@Override
		public void statusInfoChanged(FileTracker tracker) {
			if(!tracker.hasStatusInfo()) {
				refresh("replaydh.panels.workspaceTracker.unknownStatus", false, false);
			}
		}
	}

	private class FileCachePanel extends JPanel implements CloseableUI, CacheListener {

		private static final long serialVersionUID = 7605085636637412408L;

		private final JTextArea textArea;

		private final DropLabel dropInput, dropOutput, dropTool;

		/**
		 * Buffer to hold the drop label which the current
		 * drop action is "aimed" at
		 */
		private DropLabel dropTarget;

		private final EmptyBorder defaultBorder;

		private final Border activeDragBorder;

		private final ResourceDragController dragController;

		/**
		 * <pre>
		 * +---------+
		 * |  LABEL  |
		 * |         |
		 * |  DROP-  |
		 * |  AREA   |
		 * +---------+
		 * </pre>
		 */

		private FileCachePanel() {

			ResourceManager rm = ResourceManager.getInstance();

			textArea = new PassiveTextArea();
			textArea.setBorder(Paddings.DLU2);

			defaultBorder = Paddings.DLU2;

			@SuppressWarnings("serial")
			Border padding = new AbstractBorder() {
				@Override
				public Insets getBorderInsets(Component c, Insets insets) {
					defaultBorder.getBorderInsets(c, insets);
					insets.top--;
					insets.bottom--;
					insets.left--;
					insets.right--;
					return insets;
				}
			};
			Border coloredBorder = new LineBorder(Color.green.darker());

			activeDragBorder = BorderFactory.createCompoundBorder(coloredBorder, padding);

			dropInput = new DropLabel(Role.INPUT,
					WorkflowStepShape.ICON_RESOURCE_INPUT,
					rm.get("replaydh.panels.fileCache.input.label"),
					rm.get("replaydh.panels.fileCache.input.tooltip"));
			dropOutput = new DropLabel(Role.OUTPUT,
					WorkflowStepShape.ICON_RESOURCE_INPUT,
					rm.get("replaydh.panels.fileCache.output.label"),
					rm.get("replaydh.panels.fileCache.output.tooltip"));
			dropTool = new DropLabel(Role.TOOL,
					WorkflowStepShape.ICON_TOOL,
					rm.get("replaydh.panels.fileCache.tool.label"),
					rm.get("replaydh.panels.fileCache.tool.tooltip"));

			FormBuilder.create()
					.columns("pref:grow:fill")
					.rows("pref:grow:fill, pref")
					.panel(this)
					.add(textArea).xy(1, 1)
					.add(Forms.buttonBar(dropInput, dropTool, dropOutput)).xy(1, 2)
					.padding(defaultBorder)
					.build();

			dragController = new ResourceDragController(Mode.FILES_AND_URLS) {
				@Override
				protected void refreshUI() {
					Point dropLocation = dragController.getDropLocation();
					dropTarget = findTarget(dropLocation);
					refresh();
				}

				@Override
				protected void handleFileDrag(List<Path> files) {

					final Role role = getRole();
					new FileResolutionWorker(environment, files) {
						@Override
						protected void finished(boolean finishedWithErrors) {
							dragController.cleanupDrop();
							showCacheDialog(role, getFileObjects());
						};
					}.execute();
				}

				@Override
				protected void handleURLDrag(URI url) {
					showCacheDialog(getRole(), url);
				}
			};
			dragController.install(this);
			dragController.install(textArea);

			refresh();

			resourceCache.addCacheListener(this);
		}

		/**
		 * @see bwfdm.replaydh.ui.helper.CloseableUI#close()
		 */
		@Override
		public void close() {
			resourceCache.removeCacheListener(this);
		}

		/**
		 * @see bwfdm.replaydh.ui.helper.CloseableUI#canClose()
		 */
		@Override
		public boolean canClose() {
			return !dragController.isDragProcessing();
		}

		private void refresh() {
			String text = null;
			Border border = defaultBorder;

			refreshDropLabel(dropInput);
			refreshDropLabel(dropOutput);
			refreshDropLabel(dropTool);

			if(dragController.isDragActive()) {
				text = ResourceManager.getInstance().get("replaydh.panels.fileCache.dropInfo");
				border = activeDragBorder;
			} else {
				boolean cacheEmpty = resourceCache.isEmpty();

				if(cacheEmpty) {
					text = ResourceManager.getInstance().get("replaydh.panels.fileCache.empty");
				} else {
					int numFiles = resourceCache.getEntryCount();
					text = ResourceManager.getInstance().get("replaydh.panels.fileCache.summary", numFiles);
				}
			}

			setBorder(border);
			textArea.setText(text);
		}

		private void refreshDropLabel(DropLabel dropLabel) {
			dropLabel.refresh(dragController.isDragActive(), dropLabel==dropTarget);
		}

		private DropLabel findTarget(Point location) {
			if(location==null) {
				return null;
			}

			if(dropInput.contains(SwingUtilities.convertPoint(this, location, dropInput))) {
				return dropInput;
			} else if(dropOutput.contains(SwingUtilities.convertPoint(this, location, dropOutput))) {
				return dropOutput;
			} else if(dropTool.contains(SwingUtilities.convertPoint(this, location, dropTool))) {
				return dropTool;
			}

			return null;
		}

		/**
		 * Returns the entry type for the current drag operation.
		 */
		private Role getRole() {
			DropLabel dropLabel = dropTarget;

			return dropLabel==null ? Role.INPUT : dropLabel.role;
		}

		private void showCacheDialog(Role role, List<LocalFileObject> files) {
			Map<Resource,Path> resourceMap = WorkflowUIUtils.extractResources(files, role.asIdentifiableType());

			WorkflowUIUtils.showFileResourceDialog(
					workflowSource.get().getSchema(), false, role, resourceMap);

			if(resourceMap.isEmpty()) {
				return;
			}

			for(Map.Entry<Resource, Path> entry : resourceMap.entrySet()) {
				resourceCache.add(entry.getValue(), role, entry.getKey());
			}
		}

		private void showCacheDialog(Role role, URI uri) {
			//TODO implement actual dialog
			GuiUtils.showDefaultInfo(null, "Dialog for describing URL resource");
		}

		@Override
		public void entryAdded(CacheEntry entry) {
			GuiUtils.invokeEDT(this::refresh);
		}

		@Override
		public void entryRemoved(CacheEntry entry) {
			GuiUtils.invokeEDT(this::refresh);
		}

		@Override
		public void cacheCleared() {
			GuiUtils.invokeEDT(this::refresh);
		}
	}

	private static class DropLabel extends JLabel {

		private static final long serialVersionUID = 3705934022726363610L;

		private final Icon icon;
		private final String label;
		private final String tooltip;

		private final Role role;

		private static final Icon EMPTY_ICON = GuiUtils.createBlankIcon(32, 32);

		private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);
		private static final Border ACTIVE_BORDER = BorderFactory.createLineBorder(Color.GREEN.darker(), 2);

		public DropLabel(Role role, Icon icon, String label, String tooltip) {
			this.role = requireNonNull(role);
			this.icon = requireNonNull(icon);
			this.label = requireNonNull(label);
			this.tooltip = tooltip;

			setHorizontalAlignment(SwingConstants.CENTER);

			setVerticalTextPosition(SwingConstants.BOTTOM);
			setHorizontalTextPosition(SwingConstants.CENTER);

			setBorder(EMPTY_BORDER);
		}

		public void refresh(boolean dragActive, boolean isTarget) {
			if(dragActive) {
				setIcon(icon);
				setText(label);
				setToolTipText(tooltip);

				setBorder(isTarget ? ACTIVE_BORDER : EMPTY_BORDER);
			} else {
				setIcon(EMPTY_ICON);
				setText("    ");
				setToolTipText(null);
				setBorder(EMPTY_BORDER);
			}
		}
	}

	private class Handler implements HierarchyListener, CacheListener {

		private final Interval fileTrackerUptime = new Interval();

		/**
		 * @see java.awt.event.HierarchyListener#hierarchyChanged(java.awt.event.HierarchyEvent)
		 */
		@Override
		public void hierarchyChanged(HierarchyEvent e) {
			if(e.getID()!=HierarchyEvent.HIERARCHY_CHANGED) {
				return;
			}

			if((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
				boolean alwaysOnTop = environment.getBoolean(RDHProperty.CLIENT_UI_ALWAYS_ON_TOP);
				actionManager.setSelected(alwaysOnTop, "replaydh.ui.core.mainPanel.toggleAlwaysOnTop");
				setAlwaysOnTop(alwaysOnTop);
			}
		}

		private void onFileTrackerPropertyChange(PropertyChangeEvent evt) {

			switch (evt.getPropertyName()) {
			case JGitAdapter.NAME_WORKFLOW:
				GuiUtils.invokeEDT(RDHMainPanel.this::refreshWorkflowFromSource);
				break;

			default:
				break;
			}
		}

		private void onEnvironmentPropertyChange(PropertyChangeEvent evt) {
			String propertyName = RDHEnvironment.unpackPropertyName(evt.getPropertyName());

			if(RDHProperty.CLIENT_UI_ALWAYS_ON_TOP.getKey().equals(propertyName)) {
				boolean newAlwaysOnTop = Boolean.parseBoolean((String) evt.getNewValue());
				setAlwaysOnTop(newAlwaysOnTop);
			}
		}

		private void exitClient() {
			if(isVerbose()) {
				log.info("User requested client shutdown");
			}

			environment.getClient().getGui().invokeShutdown(false);
		}

		private void restartClient() {
			if(isVerbose()) {
				log.info("User requested client restart");
			}

			environment.getClient().getGui().invokeShutdown(true);
		}

		private void showAboutDialog() {
			AboutDialog.showDialog(SwingUtilities.getWindowAncestor(controlPanel));
		}

		private void showPreferencesDialog() {
			PreferencesDialog.showDialog(environment, RDHMainPanel.this);
		}

		private void openWorkspaceFolder() {
			Workspace workspace = environment.getWorkspace();

			if(workspace==null) {
				return;
			}

			if(!Desktop.isDesktopSupported()) {
				if(isVerbose()) {
					log.info("Ignoring request to open workspace folder in file browser - java.awt.Desktop not supported");
				}
				return;
			}

			logStat(StatEntry.ofType(StatType.UI_ACTION, GuiStats.OPEN_WORKSPACE));

			try {
				Desktop.getDesktop().open(workspace.getFolder().toFile());
			} catch (IOException e) {
				log.error("Failed to show workspace folder in file browser", e);
				GuiUtils.beep();
				GuiUtils.showErrorDialog(RDHMainPanel.this, e);
			}
		}

		private void clearResourceCache() {

			if(resourceCache.isEmpty()) {
				return;
			}

			ResourceManager rm = ResourceManager.getInstance();
			Collection<CacheEntry> entries = resourceCache.getCacheEntries();

			String message = rm.get("replaydh.ui.core.mainPanel.clearCache.message", entries.size());
			String title = rm.get("replaydh.ui.core.mainPanel.clearCache.title");
			if(JOptionPane.showConfirmDialog(getRootPane(), message, title,
					JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {

				logStat(StatEntry.ofType(StatType.UI_ACTION, GuiStats.CLEAR_CACHE));

				resourceCache.clear();
			}
		}

		private void exportStatistics() {

			ResourceManager rm = ResourceManager.getInstance();

			JFileChooser fileChooser = new JFileChooser();
			fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fileChooser.setAcceptAllFileFilterUsed(true);
			fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);

			if(fileChooser.showDialog(getRootPane(), rm.get("replaydh.labels.select"))
					==JFileChooser.APPROVE_OPTION) {

				final File desto = fileChooser.getSelectedFile();
				if(desto==null) {
					return;
				}

				new SwingWorker<Object, Object>() {

					@Override
					protected Object doInBackground() throws Exception {
						environment.getClient().getStatLog().export(new FileResource(desto.toPath()));
						return null;
					}

					@Override
					protected void done() {
						boolean success = false;

						try {
							get();
							success = true;
						} catch (InterruptedException e) {
							// ignore
						} catch (ExecutionException e) {
							GuiUtils.beep();
							GuiUtils.showErrorDialog(getRootPane(), e.getCause());
						}

						if(success) {
							JOptionPane.showMessageDialog(getRootPane(),
									rm.get("replaydh.ui.core.mainPanel.exportStatistics.success",
											RDHUtils.toPathString(desto.toPath(), 45)),
									rm.get("replaydh.ui.core.mainPanel.exportStatistics.title"),
									JOptionPane.INFORMATION_MESSAGE);
						}
					};
				}.execute();
			}
		}

		private void resetStatistics() {

			ResourceManager rm = ResourceManager.getInstance();

			String message = rm.get("replaydh.ui.core.mainPanel.resetStatistics.message");
			String title = rm.get("replaydh.ui.core.mainPanel.resetStatistics.title");
			if(JOptionPane.showConfirmDialog(getRootPane(), message, title,
					JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {

				new SwingWorker<Object, Object>() {

					@Override
					protected Object doInBackground() throws Exception {
						environment.getClient().getStatLog().reset();
						return null;
					}

					@Override
					protected void done() {
						try {
							get();
						} catch (InterruptedException e) {
							// ignore
						} catch (ExecutionException e) {
							GuiUtils.beep();
							GuiUtils.showErrorDialog(getRootPane(), e.getCause());
						}
					};
				}.execute();
			}
		}

		private void addWorkflowStep() {
			checkState("Commit task already running",
					activeCommitTask==null || activeCommitTask.isDone());

			if(isVerbose()) {
				log.info("Initiating recording of new workflow step");
			}

			InteractiveCommitTask newCommitTask = new InteractiveCommitTask();
			activeCommitTask = newCommitTask;
			newCommitTask.execute();

			refreshActions();
		}


		private void scheduleSingleFileTrackerUpdate() {
			synchronized (lock) {
				// Update already in progress
				if(getOrClearUpdateTask()!=null) {
					return;
				}

				logStat(StatEntry.ofType(StatType.UI_ACTION, GuiStats.UPDATE_TRACKER));

				// Schedule to refresh task once
				updateTask = environment.getClient().getExecutorService().schedule(
						this::executeFileTrackerUpdate,
						0, TimeUnit.MILLISECONDS);
			}

			refreshActions();
		}

		/**
		 * Tries to stop the period update of file tracker status info.
		 * Does nothing if no periodic background task has been scheduled.
		 */
		private void cancelFileTrackerUpdate() {
			GuiUtils.checkEDT();

			synchronized (lock) {

				ScheduledFuture<?> updateTask = getOrClearUpdateTask();
				// No update to cancel
				if(updateTask==null) {
					return;
				}

				log.debug("Stopping file tracker update task");

				logStat(StatEntry.withData(StatType.INTERNAL_END, GuiStats.UPDATE_TRACKER,
						fileTrackerUptime.stop().asDurationString()));

				fileTrackerUptime.reset();

				updateTask.cancel(true);
			}

			refreshActions();
		}

		private void toggleDetails() {
			logStat(StatEntry.ofType(StatType.UI_ACTION,
					isExpanded ? GuiStats.WINDOW_COLLAPSE : GuiStats.WINDOW_EXPAND));
			toggleSize(!isExpanded);
		}

		private void toggleAlwaysOnTop(boolean alwaysOnTop) {
			environment.setProperty(RDHProperty.CLIENT_UI_ALWAYS_ON_TOP, Boolean.toString(alwaysOnTop));
		}

		private void changeWorkspace() {
			doChangeWorkspace(false);
		}

		/**
		 * Initiates a dialog for changing the current workspace.
		 * If the {@code exitOnCancel} parameter is {@code true}, then
		 * aborting the dialog will result in the client shutting down.
		 *
		 * @param exitOnCancel
		 *
		 * @see RDHGui#invokeShutdown()
		 * @see RDHClient#shutdown()
		 */
		private void doChangeWorkspace(boolean exitOnCancel) {
			String history = environment.getProperty(RDHProperty.CLIENT_WORKSPACE_HISTORY);

			ChangeWorkspaceContext context = history==null ?
					ChangeWorkspaceContext.blank() :
					ChangeWorkspaceContext.withWorkspaces(history);
			boolean wizardDone = false;

			// Show the wizard for selecting type and location of target resource
			try(Wizard<ChangeWorkspaceContext> wizard = RDHChangeWorkspaceWizard.getWizard(
					SwingUtilities.getWindowAncestor(RDHMainPanel.this),
					environment)) {

				wizard.startWizard(context);

				wizardDone = wizard.isFinished() && !wizard.isCancelled();
			}

			/*
			 * Protocol for deciding final action:
			 *
			 * If user went through with the wizard and chose
			 * a new workspace, then proceed to load it.
			 *
			 * If user cancelled the wizard and 'exitOnCancel'
			 * parameter has been set, exit the entire client.
			 *
			 * Otherwise just return and do nothing (let outer
			 * code decide).
			 */
			if(wizardDone) {
				loadWorkspace(context);
			} else if(exitOnCancel) {
				SwingUtilities.invokeLater(() -> environment.getClient().getGui().invokeShutdown(false));
			}
		}

		private void importWorkflowSchema() {

			final AddWorkflowSchemaContext context = AddWorkflowSchemaContext.blank();
			boolean wizardDone = false;

			WorkflowSchema schema;

			try(Wizard<AddWorkflowSchemaContext> wizard = AddWorkflowSchemaWizard.getWizard(
					SwingUtilities.getWindowAncestor(RDHMainPanel.this),
					environment)) {
				wizard.startWizard(context);

				wizardDone = wizard.isFinished() && !wizard.isCancelled();

				schema = context.getSchema();
			}

			if(wizardDone) {
				SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

					@Override
					protected Boolean doInBackground() throws Exception {

						// Step 1: copy file physically
						Files.copy(context.getSourceFile(), context.getTargetFile());

						// Step 2: register schema
						environment.getClient().getSchemaManager().addSchema(schema);

						return Boolean.TRUE;
					}

					@Override
					protected void done() {
						boolean cleanupTargetFile = false;

						try {
							if(!isCancelled()) {
								get();
							}
						} catch (InterruptedException e) {
							// ignore cancellation?
						} catch (ExecutionException e) {
							log.error("Failed to copy new schema file {} into schema folder {}",
									context.getSourceFile(), context.getTargetFile(), e.getCause());

							ResourceManager rm = ResourceManager.getInstance();
							GuiUtils.showErrorDialog(
									fileCachePanel,
									null,
									rm.get("replaydh.ui.core.mainPanel.importWorkflowSchema.importFailed"),
									e);

							cleanupTargetFile = true;
						}

						if(cleanupTargetFile) {
							log.info("Cleaning up schema file: {}", context.getTargetFile());
							try {
								Files.deleteIfExists(context.getTargetFile());
							} catch (IOException e) {
								log.error("Failed to delete schema file: {}", context.getTargetFile(), e);
								GuiUtils.showErrorDialog(fileCachePanel, e);
							}
						}
					}

				};

				worker.execute();
			}
		}

		private void toggleTrackerStatus(boolean active) {
			if(active) {
				schedulePeriodicFileTrackerUpdate();
			} else {
				cancelFileTrackerUpdate();
			}
		}

		/**
		 * If not already present creates a new worker to asynchronously
		 * update the file tracker and then refresh the UI afterwards.
		 */
		private void schedulePeriodicFileTrackerUpdate() {
			synchronized (lock) {
				// Update already in progress
				if(getOrClearUpdateTask()!=null) {
					return;
				}

				// Fetch most up2date interval settings
				int period = environment.getInteger(RDHProperty.CLIENT_WORKSPACE_TRACKER_INTERVAL,
						DEFAULT_REFRESH_INTERVAL);

				log.debug("Starting file tracker update task with period of {} seconds", period);

				fileTrackerUptime.start();
				logStat(StatEntry.ofType(StatType.INTERNAL_BEGIN, GuiStats.UPDATE_TRACKER));

				// Schedule to refresh task to run on a non-gui thread
				updateTask = environment.getClient().getExecutorService().scheduleAtFixedRate(
						this::executeFileTrackerUpdate,
						1, period, TimeUnit.SECONDS);
			}

			refreshActions();
		}

		/**
		 * Actually executes the update request on the file tracker.
		 * <p>
		 * Must <b>not</b> be called on the EDT!
		 */
		private void executeFileTrackerUpdate() {
			// Make sure this doesn't end up on the EDT by accident
			GuiUtils.checkNotEDT();

			executingUpdate = true;
			try {
				// Ensure proper update of the 'cancel' action
				GuiUtils.invokeEDTAndWait(RDHMainPanel.this::refreshActions);

				fileTracker.refreshStatusInfo();

				//TODO track subsequent failures and disable periodic update when too many fails happened
			} catch (TrackerException e) {
				log.error("Executing file tracker update failed", e);
			} finally {
				executingUpdate = false;
				// Clear our active task if needed
				getOrClearUpdateTask();

				// And switch to EDT for UI related notifications
				GuiUtils.invokeEDT(RDHMainPanel.this::refreshActions);
			}
		}

		private void anyCacheChange() {
			GuiUtils.invokeEDT(RDHMainPanel.this::refreshActions);
		}

		/**
		 * @see bwfdm.replaydh.workflow.ResourceCache.CacheListener#entryAdded(bwfdm.replaydh.workflow.ResourceCache.CacheEntry)
		 */
		@Override
		public void entryAdded(CacheEntry entry) {
			anyCacheChange();
		}

		/**
		 * @see bwfdm.replaydh.workflow.ResourceCache.CacheListener#entryRemoved(bwfdm.replaydh.workflow.ResourceCache.CacheEntry)
		 */
		@Override
		public void entryRemoved(CacheEntry entry) {
			anyCacheChange();
		}

		/**
		 * @see bwfdm.replaydh.workflow.ResourceCache.CacheListener#cacheCleared()
		 */
		@Override
		public void cacheCleared() {
			anyCacheChange();
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	private class InteractiveCommitTask extends SwingWorker<Boolean, LocalFileObject> {

		private final Set<LocalFileObject> filesToAdd = new HashSet<>();
		private final Set<LocalFileObject> filesToRemove = new HashSet<>();
		private final Set<LocalFileObject> modifiedFiles = new HashSet<>();

		private final Set<LocalFileObject> newFilesToIgnore = new HashSet<>();
		private final Set<LocalFileObject> modifiedFilesToIgnore = new HashSet<>();

		/**
		 * The newly created workflow step.
		 * Guaranteed to be non-null if the task finishes
		 * without errors.
		 */
		private WorkflowStep newStep;

		InteractiveCommitTask() {
			//TODO init the UI dialog?
		}

		/**
		 * Collects from the file tracker all files that should be considered as
		 * output of the new workflow step.
		 * @throws TrackerException
		 */
		private boolean collectFiles() throws IOException, InterruptedException, TrackerException {
			if(!fileTracker.refreshStatusInfo()) {
				return false;
			}

			if(fileTracker.hasFilesForStatus(TrackingStatus.UNKNOWN)) {
				resolveFiles(TrackingStatus.UNKNOWN, filesToAdd);
			}

			if(fileTracker.hasFilesForStatus(TrackingStatus.MISSING)) {
				resolveFiles(TrackingStatus.MISSING, filesToRemove);
			}

			if(fileTracker.hasFilesForStatus(TrackingStatus.MODIFIED)) {
				resolveFiles(TrackingStatus.MODIFIED, modifiedFiles);
			}

			return true;
		}

		/**
		 * Creates a fresh new workflow step and initializes basic fields with
		 * default values.
		 */
		private WorkflowStep constructStep(Workflow workflow) {
			WorkflowStep newStep = workflow.createWorkflowStep();

			newStep.setTitle("<Unnamed step>");
			newStep.setRecordingTime(LocalDateTime.now());

			return newStep;
		}

		private void applyCachedResources(WorkflowStep step, Predicate<? super Identifiable> filter) {
			ResourceCache cache = environment.getClient().getResourceCache();

			Collection<CacheEntry> cachedResources = cache.getCacheEntries();
			for(CacheEntry entry : cachedResources) {
				switch (entry.getRole()) {
				case INPUT:
					step.addInput(entry.getResource());
					break;

				case OUTPUT:

					if (!filter.test(entry.getResource())) {
						step.addOutput(entry.getResource());
					}
					break;

				case TOOL:
					step.setTool((Tool) entry.getResource());
					break;

				default:
					break;
				}
			}
		}

		/**
		 * Converts all previously collected files as output resources
		 * to the new workflow step and returns {@link Identifiable}
		 * objects for all the previously unknown files.
		 */
		private Set<Identifiable> addFilesAsOutput(WorkflowStep newStep) {

			// Buffer for previously unknown resources -> will be added to resolver after user confirmed
			Set<Identifiable> newResources = new HashSet<>();

			// Consider all modified or new files as "output" of the step
			Set<LocalFileObject> outputFiles = new HashSet<>();
			if(filesToAdd!=null) {
				outputFiles.addAll(filesToAdd);
			}
			if(modifiedFiles!=null) {
				outputFiles.addAll(modifiedFiles);
			}

			// Ensure each such file is represented by an Identifiable
			if(!outputFiles.isEmpty()) {
				for(LocalFileObject fileObject : outputFiles) {

					Resource resource = fileObject.getResource();

					// Otherwise create a new resource and only accept resources as "output"

					if(resource==null) {
						resource = DefaultResource.uniqueResource();
						fileObject.getIdentifiers().forEach(resource::addIdentifier);
					}
					//TODO verify if we need to check whether or not above identifiers are already/not present

					newResources.add(resource);

					newStep.addOutput(resource);
				}
			}

			return newResources;
		}

		/**
		 * Makes sure that related info in git and the identifier cache
		 * gets updated after the workflow step has been added.
		 * @throws TrackerException
		 */
		private void persistAssociatedChanges(Set<Identifiable> newResources) throws TrackerException {
			// Adjust file collections based on revised ignore decisions
			filesToAdd.removeAll(newFilesToIgnore);

			if(!filesToAdd.isEmpty()) {
				fileTracker.applyTrackingAction(LocalFileObject.extractFiles(filesToAdd), TrackingAction.ADD);
			}

			if(!filesToRemove.isEmpty()) {
				fileTracker.applyTrackingAction(LocalFileObject.extractFiles(filesToRemove), TrackingAction.REMOVE);
			}

			if(!newFilesToIgnore.isEmpty()) {
				fileTracker.applyTrackingAction(LocalFileObject.extractFiles(newFilesToIgnore), TrackingAction.IGNORE);
			}

			if(!modifiedFilesToIgnore.isEmpty()) {
				fileTracker.applyTrackingAction(LocalFileObject.extractFiles(modifiedFilesToIgnore), TrackingAction.IGNORE);
			}

			// Add new resources to the resolver
			if(!newResources.isEmpty()) {
				IdentifiableResolver resolver = environment.getClient().getResourceResolver();
				resolver.lock();
				try {
					resolver.register(newResources);
				} finally {
					resolver.unlock();
				}
			}
		}

		private void filterFiles(Set<LocalFileObject> files, long sizeLimit, Set<LocalFileObject> buffer) {

			if(files!=null && !files.isEmpty()) {
				for(LocalFileObject file :files) {
					long size;

					// We use the checksum facility for keeping size info storage closely to the file object
					try {
						size = Files.size(file.getFile());
					} catch (IOException e) {
						log.error("Failed to obtain size for file {}", file.getFile(), e);
						continue;
					}

					if(size>=sizeLimit) {
						buffer.add(file);
					}
				}
			}
		}

		private void filterLargeFiles() {
			// Fetch size limit from current settings
			long sizeLimit = IOUtils.parseSize(environment.getProperty(RDHProperty.GIT_MAX_FILESIZE));

			// Special case: if limit is 0 we will never automatically ignore files
			if(sizeLimit==0) {
				return;
			}

			// Check new and modified files separately
			filterFiles(filesToAdd, sizeLimit, newFilesToIgnore);
			filterFiles(modifiedFiles, sizeLimit, modifiedFilesToIgnore);
		}

		/**
		 * @see javax.swing.SwingWorker#doInBackground()
		 */
		@Override
		protected Boolean doInBackground() throws Exception {

			// Flag signaling user confirmation
			boolean opSuccess = false;
			Interval runtime = new Interval().start();

			try {
				logStat(StatEntry.ofType(StatType.INTERNAL_BEGIN, GuiStats.DIALOG_ADD_STEP));

				if(!collectFiles()) {
					return Boolean.FALSE;
				}

				filterLargeFiles();

				// If necessary allow the user to revise automatically filtered files
				if(!newFilesToIgnore.isEmpty() || !modifiedFilesToIgnore.isEmpty()) {
					FileIgnoreConfiguration configuration = FileIgnoreEditor.newConfiguration(
							newFilesToIgnore, modifiedFilesToIgnore);

					// BEGIN EDT
					boolean doContinue = GuiUtils.invokeEDTAndWait(
							RDHMainPanel.this::interactiveFilterFiles, configuration);
					// END EDT

					// If user canceled here we gonna stop immediately
					if(!doContinue) {
						return false;
					}
				}


				Workflow workflow = workflowSource.get();

				workflow.beginUpdate();
				try {
					// Start a fresh new workflow step
					newStep = constructStep(workflow);

					// Add all files and collect new Identifiable instances
					Set<Identifiable> newResources = addFilesAsOutput(newStep);

					Set<Identifier> usedPaths = newResources.stream()
							.map(i -> i.getIdentifier(IdentifierType.PATH))
							.collect(Collectors.toSet());

					Predicate<Identifiable> filter = identifiable -> {
						Identifier path = identifiable.getIdentifier(IdentifierType.PATH);
						return path!=null && usedPaths.contains(path);
					};

					//TODO use cached resources to filter automatically detected outputs (as cache can have more metadata already entered)

					// Add all the previously cached resources
					applyCachedResources(newStep, filter);

					/*
					 *  Start the part where the user gets involved.
					 *  We do this synchronously on the event dispatch thread
					 *  and block here until the GUI part is finished.
					 */
					// BEGIN EDT
					opSuccess = GuiUtils.invokeEDTAndWait(RDHMainPanel.this::interactiveCommit, newStep);
					// END EDT

					// Only if the user confirmed the dialog do we actually add the step and commit git changes!
					if(opSuccess) {

						// First perform actions that are easily undoable
						//TODO implement a rollback of these changes in case the next steps fails
						persistAssociatedChanges(newResources);

						// If this operation goes through, the commit is persistent
						workflow.addWorkflowStep(newStep);

						//TODO remove all resources from cache that have been used here
					}
				} finally {
					workflow.endUpdate();
				}

				return opSuccess;
			} finally {
				String label = newStep==null ? null : newStep.getId();
				logStat(StatEntry.withData(
						StatType.INTERNAL_END, GuiStats.DIALOG_ADD_STEP, label,
						String.valueOf(opSuccess), runtime.stop().asDurationString()));
			}
		}

		private void resolveFiles(TrackingStatus trackingStatus, Set<LocalFileObject> buffer) throws IOException, InterruptedException, TrackerException {
			Set<Path> files = fileTracker.getFilesForStatus(trackingStatus);

			if(files==null || files.isEmpty()) {
				return;
			}

			final IdentifiableResolver resolver = environment.getClient().getResourceResolver();

			resolver.lock();
			try {
				for(Path file : files) {
					if(Thread.interrupted())
						throw new InterruptedException();

					file = RDHUtils.normalize(file);

					// Wrap file
					LocalFileObject fileObject = new LocalFileObject(file, trackingStatus);

					// Attempt to create identifiers and resolve file to resource and metadata record
					if(LocalFileObject.ensureOrRefreshRecord(fileObject, environment)) {
						publish(fileObject);
					}
					buffer.add(fileObject);
				}
			} finally {
				resolver.unlock();
			}
		}

		/**
		 * @see javax.swing.SwingWorker#process(java.util.List)
		 */
		@Override
		protected void process(List<LocalFileObject> chunks) {
			for(LocalFileObject fileObject : chunks) {
				//FIXME copy files into workflow step
			}
		}

		/**
		 * @see javax.swing.SwingWorker#done()
		 */
		@Override
		protected void done() {
			GuiUtils.checkEDT();

			//TODO hide our progress dialog
			boolean opSuccess = false;

			try {
				opSuccess = get();

				// Special handling in case we succeeded
				if(opSuccess) {

					String newStepName = newStep.getTitle();

					// Log a "deep" dump of the new step for completeness
					log.info("Added new workflow step {}: {}", newStepName, newStep.toString());

					// Show an info popup with name of new workflow step
					ResourceManager rm = ResourceManager.getInstance();
					String message = rm.get("replaydh.ui.core.mainPanel.addStep.message", newStepName);
					String title = rm.get("replaydh.ui.core.mainPanel.addStep.title");
					JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);

					// Discard tracking status
					environment.execute(fileTracker::clearStatusInfo);
				}

			} catch (InterruptedException e) {
				// properly cancelled by user
			} catch (ExecutionException e) {
				log.error("Failed to prepare resources for new workflow step", e);

				GuiUtils.showErrorDialog(RDHMainPanel.this, e.getCause());

				//TODO ensure a proper roll-back of already committed changes?
			} finally {

				// Make sure to cleanup our task reference asap
				if(activeCommitTask==this) {
					activeCommitTask = null;
				}

				refreshActions();
			}
		}
	}
}

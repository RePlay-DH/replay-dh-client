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
package bwfdm.replaydh.ui.workflow;

import static java.util.Objects.requireNonNull;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.io.FileTracker;
import bwfdm.replaydh.io.LocalFileObject;
import bwfdm.replaydh.io.TrackerException;
import bwfdm.replaydh.io.TrackerListener;
import bwfdm.replaydh.io.TrackingAction;
import bwfdm.replaydh.io.TrackingStatus;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.actions.ActionManager;
import bwfdm.replaydh.ui.actions.ActionManager.ActionMapper;
import bwfdm.replaydh.ui.helper.CloseableUI;
import bwfdm.replaydh.ui.helper.ScrollablePanel;
import bwfdm.replaydh.ui.helper.ScrollablePanel.ScrollableSizeHint;
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.utils.LazyCollection;
import bwfdm.replaydh.utils.annotation.Experimental;

/**
 * @author Markus Gärtner
 *
 */
public class WorkspaceTrackerPanel extends JPanel implements CloseableUI {

	private static final long serialVersionUID = 5100311840353055034L;

	private static final Logger log = LoggerFactory.getLogger(WorkspaceTrackerPanel.class);

	private final RDHEnvironment environment;

	private final ActionManager actionManager;
	private final ActionMapper actionMapper;

	/**
	 * Container for the fast changing content
	 */
	private final ScrollablePanel contentPanel;

	private final JToolBar toolBar;

	private final Map<TrackingStatus, FileOutlinePanel> panels = new EnumMap<>(TrackingStatus.class);
	private final JLabel contentHeader;

	/**
	 * Interface to the file tracker facility (usually GIT)
	 */
	private final FileTracker fileTracker;

	private final Handler handler;

	/**
	 * Flag to prevent redundant refresh operations when the file tracker
	 * posts both events via the {@link PropertyChangeListener} and
	 * {@link TrackerListener} interfaces. Switching this field to {@code true}
	 * will result in the next property change event to be ignored.
	 */
	private boolean ignoreNextStatusChange = false;

	public WorkspaceTrackerPanel(RDHEnvironment environment) {
		super(new BorderLayout());

		this.environment = requireNonNull(environment);

		handler = new Handler();

		actionManager = environment.getClient().getGui().getActionManager().derive();
		try {
			actionManager.loadActions(WorkspaceTrackerPanel.class.getResource("workspace-tracker-panel-actions.xml"));
		} catch (IOException e) {
			throw new RDHException("Failed to load actions for"+WorkspaceTrackerPanel.class, e);
		}
		actionMapper = actionManager.mapper(this);
		toolBar = actionManager.createToolBar("replaydh.ui.core.workspaceTrackerPanel.toolBarList", null);

		fileTracker = environment.getClient().getFileTracker();
		fileTracker.addTrackerListener(handler);

		contentHeader = (JLabel) GuiUtils.createInfoComponent("", false, null);

		for(TrackingStatus status : TrackingStatus.values()) {
			panels.put(status, new FileOutlinePanel(environment, actionManager, status));
		}

		contentPanel = new ScrollablePanel();
		contentPanel.setScrollableWidth(ScrollableSizeHint.FIT);

		FormBuilder.create()
				.columns("fill:pref:grow")
				.rows("pref, 5dlu, pref, pref, pref, pref, pref")
				.panel(contentPanel)
				.add(contentHeader)		.xy(1, 1)
				.add(panel(TrackingStatus.CORRUPTED))	.xy(1, 3)
				.add(panel(TrackingStatus.UNKNOWN))		.xy(1, 4)
				.add(panel(TrackingStatus.MISSING))		.xy(1, 5)
				.add(panel(TrackingStatus.MODIFIED))	.xy(1, 6)
				.add(panel(TrackingStatus.TRACKED))		.xy(1, 7)
				.build();

		environment.addPropertyChangeListener(RDHEnvironment.NAME_WORKSPACE, handler);

		JScrollPane scrollPane = new JScrollPane(contentPanel);

		add(toolBar, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);

		registerActions();

		refreshActions();

		update();
	}

	private FileOutlinePanel panel(TrackingStatus status) {
		FileOutlinePanel panel = panels.get(status);
		if(panel==null)
			throw new IllegalStateException("No outline panel for tracking status: "+status);
		return panel;
	}

	private void registerActions() {
		actionMapper.mapTask("replaydh.ui.core.workspaceTrackerPanel.createDummyFile", handler::createDummyFile);
		actionMapper.mapTask("replaydh.ui.core.workspaceTrackerPanel.editDummyFile", handler::editDummyFile);
		actionMapper.mapTask("replaydh.ui.core.workspaceTrackerPanel.deleteDummyFile", handler::deleteDummyFile);
	}

	private void refreshActions() {
		// no-op
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.CloseableUI#close()
	 */
	@Override
	public void close() {
		environment.removePropertyChangeListener(RDHEnvironment.NAME_WORKSPACE, handler);

		fileTracker.removeTrackerListener(handler);

		actionMapper.dispose();
	}

	private void checkDevMode() {
		if(!environment.getClient().isDevMode())
			throw new IllegalStateException("Cannot execute method outside of dev mode!!!");
	}

	public void update() {
		TrackerStateHint hint = fileTracker.hasStatusInfo() ?
				TrackerStateHint.UPDATE_DONE : TrackerStateHint.UNKNOWN;

		showFileTrackerState(hint);
	}

	private LocalFileObject wrapFile(Path file, TrackingStatus trackingStatus) {
		return new LocalFileObject(file.toAbsolutePath(), trackingStatus);
	}

	private boolean hasFiles(TrackingStatus trackingStatus) {
		try {
			return fileTracker.hasFilesForStatus(trackingStatus);
		} catch (TrackerException e) {
			log.error("Failed to query file tracker for state on "+trackingStatus, e);
			return false;
		}
	}

	private Set<Path> getFiles(TrackingStatus trackingStatus) {
		try {
			return fileTracker.getFilesForStatus(trackingStatus);
		} catch (TrackerException e) {
			log.error("Failed to query file tracker for files on "+trackingStatus, e);
			return Collections.emptySet();
		}
	}

	private Set<LocalFileObject> wrapFilesFromTracker(TrackingStatus trackingStatus) {
		Set<Path> files = getFiles(trackingStatus);
		LazyCollection<LocalFileObject> result = LazyCollection.lazySet();

		files.forEach(file -> result.add(wrapFile(file, trackingStatus)));

		return result.getAsSet();
	}

	private Set<LocalFileObject> extractFilesForUpdate(
			Set<LocalFileObject> filesToUpdate, Set<LocalFileObject> files) {

		for(LocalFileObject fileObject : files) {
			// Schedule all files for update if not inked to a known resource definition
			if(fileObject.getResource()==null) {
				filesToUpdate.add(fileObject);
			}
		}

		return files;
	}

	private void showFileTrackerState(final TrackerStateHint hint) {

		// Switch over to EDT if needed
		if(!SwingUtilities.isEventDispatchThread()) {
			GuiUtils.invokeEDT(() -> showFileTrackerState(hint));
			return;
		}

//		System.out.println(hint);

		String textKey = null;
		Icon icon = null;

		Set<LocalFileObject> filesToUpdate = new HashSet<>();

		boolean showCorruptedPanel = false;

		boolean showNewPanel = false;
		boolean showMissingPanel = false;
		boolean showModifiedPanel = false;
		boolean showTrackedPanel = false;

		switch (requireNonNull(hint)) {

		case UPDATE_DONE: {
			boolean hasNew = hasFiles(TrackingStatus.UNKNOWN);
			boolean hasMissing = hasFiles(TrackingStatus.MISSING);
			boolean hasModified = hasFiles(TrackingStatus.MODIFIED);
			boolean hasCorrupted = hasFiles(TrackingStatus.CORRUPTED);

			boolean hasTracked = hasFiles(TrackingStatus.TRACKED);

			if(!hasNew && !hasMissing && !hasModified  && !hasCorrupted) {
				textKey = "replaydh.panels.workspaceTracker.unchangedState";
			} else {
				textKey = "replaydh.panels.workspaceTracker.validState";

				if(hasCorrupted) {
					Set<LocalFileObject> files = wrapFilesFromTracker(TrackingStatus.CORRUPTED);
					panel(TrackingStatus.CORRUPTED).setFiles(extractFilesForUpdate(filesToUpdate, files));
				}
				if(hasNew) {
					Set<LocalFileObject> files = wrapFilesFromTracker(TrackingStatus.UNKNOWN);
					panel(TrackingStatus.UNKNOWN).setFiles(extractFilesForUpdate(filesToUpdate, files));
				}
				if(hasMissing) {
					Set<LocalFileObject> files = wrapFilesFromTracker(TrackingStatus.MISSING);
					panel(TrackingStatus.MISSING).setFiles(extractFilesForUpdate(filesToUpdate, files));
				}
				if(hasModified) {
					Set<LocalFileObject> files = wrapFilesFromTracker(TrackingStatus.MODIFIED);
					panel(TrackingStatus.MODIFIED).setFiles(extractFilesForUpdate(filesToUpdate, files));
				}
				if(hasTracked) {
					Set<LocalFileObject> files = wrapFilesFromTracker(TrackingStatus.TRACKED);
					panel(TrackingStatus.TRACKED).setFiles(extractFilesForUpdate(filesToUpdate, files));
				}

				showCorruptedPanel = hasCorrupted;
				showNewPanel = hasNew;
				showMissingPanel = hasMissing;
				showModifiedPanel = hasModified;
				showTrackedPanel = hasTracked;
			}
		} break;

		case UPDATE_CANCELLED:
			textKey = "replaydh.panels.workspaceTracker.updateCanceled";
			break;

		case UPDATE_FAILED:
			textKey = "replaydh.panels.workspaceTracker.updateFailed";
			break;

		case UPDATING:
			textKey = "replaydh.panels.workspaceTracker.updateRunning";
			icon = IconRegistry.getGlobalRegistry().getIcon("loading-128.gif");
			break;

		// Default state, let's just show generic "welcome" screen
		case UNKNOWN:
		default:
			textKey = "replaydh.panels.workspaceTracker.unknownStatus";
			break;
		}

		panel(TrackingStatus.CORRUPTED).setVisible(showCorruptedPanel);
		panel(TrackingStatus.UNKNOWN).setVisible(showNewPanel);
		panel(TrackingStatus.MISSING).setVisible(showMissingPanel);
		panel(TrackingStatus.MODIFIED).setVisible(showModifiedPanel);
		panel(TrackingStatus.TRACKED).setVisible(showTrackedPanel);

		String text = null;
		if(textKey!=null) {
			text = ResourceManager.getInstance().get(textKey);
		}

		contentHeader.setText(GuiUtils.toSwingTooltip(text));
		contentHeader.setIcon(icon);

		contentHeader.setVisible(text!=null || icon!=null);

		//TODO schedule update

		revalidate();
		repaint();

		refreshActions();
	}

	private FileOutlinePanel getFileOutlinePanel(TrackingStatus trackingStatus) {
		switch (trackingStatus) {
		case IGNORED: return null;

		default:
			return panel(trackingStatus);
		}
	}

	private void updateFilePanel(LocalFileObject file) {
		FileOutlinePanel fileOutlinePanel = getFileOutlinePanel(file.getTrackingStatus());
		if(fileOutlinePanel!=null) {
			fileOutlinePanel.updateFilePanel(file);
		}
	}

	private enum TrackerStateHint {
		UNKNOWN,
		UPDATING,
		UPDATE_FAILED,
		UPDATE_CANCELLED,
		UPDATE_DONE,
		;
	}

	private class Handler implements PropertyChangeListener, TrackerListener {

		@Experimental
		private final Random random = new Random(System.currentTimeMillis());

		/**
		 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
		 */
		@Override
		public void propertyChange(PropertyChangeEvent evt) {

			//TODO handle workspace change
		}

		/**
		 * @see bwfdm.replaydh.io.TrackerListener#statusInfoChanged(bwfdm.replaydh.io.FileTracker)
		 */
		@Override
		public void statusInfoChanged(FileTracker tracker) {
			if(!ignoreNextStatusChange) {
				TrackerStateHint hint = fileTracker.hasStatusInfo() ?
						TrackerStateHint.UPDATE_DONE : TrackerStateHint.UNKNOWN;
				showFileTrackerState(hint);
			}
			ignoreNextStatusChange = false;
		}

		/**
		 * @see bwfdm.replaydh.io.TrackerListener#refreshStarted(bwfdm.replaydh.io.FileTracker)
		 */
		@Override
		public void refreshStarted(FileTracker tracker) {
			showFileTrackerState(TrackerStateHint.UPDATING);
		}

		/**
		 * @see bwfdm.replaydh.io.TrackerListener#refreshFailed(bwfdm.replaydh.io.FileTracker, java.lang.Exception)
		 */
		@Override
		public void refreshFailed(FileTracker tracker, Exception e) {
			showFileTrackerState(TrackerStateHint.UPDATE_FAILED);
		}

		/**
		 * @see bwfdm.replaydh.io.TrackerListener#refreshDone(bwfdm.replaydh.io.FileTracker, boolean)
		 */
		@Override
		public void refreshDone(FileTracker tracker, boolean canceled) {
			TrackerStateHint hint = canceled ?
					TrackerStateHint.UPDATE_CANCELLED : TrackerStateHint.UPDATE_DONE;
			showFileTrackerState(hint);
		}

		/**
		 * @see bwfdm.replaydh.io.TrackerListener#trackingStatusChanged(bwfdm.replaydh.io.FileTracker, java.util.Set, bwfdm.replaydh.io.TrackingAction)
		 */
		@Override
		public void trackingStatusChanged(FileTracker tracker, Set<Path> files, TrackingAction action) {
			update();
		}

		@Experimental
		private Path getNewFile() {
			Path workspace = fileTracker.getTrackedFolder();
			Path file;
			do {
				String fileName = "test"+String.valueOf(1+random.nextInt(200))+".txt";
				file = workspace.resolve(fileName);
			} while(Files.exists(file, LinkOption.NOFOLLOW_LINKS));

			return file;
		}

		@Experimental
		private void createDummyFile() {
			checkDevMode();

			Path file = getNewFile();
			try {
				Files.createFile(file);
				appendToDummyFile(file);
			} catch (IOException e) {
				log.error("Failed to create dummy file: "+file, e);
				GuiUtils.beep();
			}
		}

		@Experimental
		private Path getRandomFile() {
			try {
				Path[] files = Files.list(fileTracker.getTrackedFolder())
						.filter(path ->
						path.getFileName().toString().startsWith("test"))
						.sorted()
						.toArray(size -> new Path[size]);

				int num = files.length;

				return num>0 ? files[random.nextInt(num)] : null;
			} catch (IOException e) {
				log.error("Failed to pick random file in folder", e);
				GuiUtils.beep();
				return null;
			}
		}

		@Experimental
		private void editDummyFile() {
			checkDevMode();

			Path file = getRandomFile();
			if(file!=null) {
				appendToDummyFile(file);
			}
		}

		private void appendToDummyFile(Path file) {
			try(Writer w = Files.newBufferedWriter(file, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
				w.append(LocalDateTime.now().toString());
				w.append(System.lineSeparator());
			} catch (IOException e) {
				log.error("Failed to edit random file {}", file, e);
			}
		}

		@Experimental
		private void deleteDummyFile() {
			checkDevMode();

			Path file = getRandomFile();
			if(file!=null) {
				try {
					Files.delete(file);
				} catch (IOException e) {
					log.error("Failed to delete random file {}", file, e);
				}
			}
		}
	}
}

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

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static java.util.Objects.requireNonNull;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.WeakHashMap;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.FormsSetup;
import com.jgoodies.forms.factories.ComponentFactory;
import com.jgoodies.forms.factories.Paddings;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.io.LocalFileObject;
import bwfdm.replaydh.io.TrackingStatus;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.actions.ActionManager;
import bwfdm.replaydh.ui.actions.ActionManager.ActionMapper;
import bwfdm.replaydh.ui.helper.AbstractDialogWorker;
import bwfdm.replaydh.ui.helper.AbstractDialogWorker.CancellationPolicy;
import bwfdm.replaydh.ui.helper.CloseableUI;
import bwfdm.replaydh.ui.helper.WrapLayout;
import bwfdm.replaydh.utils.Options;
import bwfdm.replaydh.utils.RDHUtils;

/**
 * @author Markus Gärtner
 *
 */
public class FileOutlinePanel extends JPanel implements CloseableUI {

	private static final Logger log = LoggerFactory.getLogger(FileOutlinePanel.class);

	private static final long serialVersionUID = -8510120755340580508L;

	private final RDHEnvironment environment;

	/**
	 * Files reported by the tracker for associated
	 * status.
	 */
	private Set<LocalFileObject> files;

	/**
	 * Maximum number of files visible without expanding the outline.
	 */
	private int filesVisible = 5;

	private int displayablePathLength = 40;

	/**
	 * Status files in this outline are associated with.
	 */
	private final TrackingStatus trackingStatus;

	private final List<FilePanel> activePanels = new ArrayList<>();

	private final Stack<FilePanel> cachedPanels = new Stack<>();

	private final Map<LocalFileObject, FilePanel> fileToPanelMapping = new WeakHashMap<>();

	private final JPanel contentPanel;

	private final JPopupMenu popupMenu;

	private final ActionManager actionManager;
	private final ActionMapper actionMapper;

	private final Handler handler;

	private static final ComponentFactory COMPONENT_FACTORY = FormsSetup.getComponentFactoryDefault();

	public FileOutlinePanel(RDHEnvironment environment, ActionManager actionManager, TrackingStatus trackingStatus) {
		super(new BorderLayout(0, 10));

		this.environment = requireNonNull(environment);
		this.trackingStatus = requireNonNull(trackingStatus);
		this.actionManager = requireNonNull(actionManager).derive();
		actionMapper = this.actionManager.mapper(this);

		handler = new Handler();

		setBorder(Paddings.DLU4);

		WrapLayout layout = new WrapLayout(FlowLayout.RIGHT, 5, 5);
		layout.setAlignOnBaseline(true);
		contentPanel = new JPanel(layout);

		add(COMPONENT_FACTORY.createSeparator(RDHUtils.getTitle(trackingStatus),
				SwingConstants.LEFT), BorderLayout.NORTH);
		add(contentPanel, BorderLayout.CENTER);

		switch (trackingStatus) {
		case CORRUPTED:
			popupMenu = this.actionManager.createPopupMenu(
					"replaydh.ui.core.workspaceTrackerPanel.conflictedFilesList",
					Options.emptyOptions);
			break;

		default:
			popupMenu = null;
			break;
		}

		registerActions();
	}

	private void registerActions() {
		actionMapper.mapTask("replaydh.ui.core.workspaceTrackerPanel.markFileResolved", handler::markFileResolved);
	}

	private void unregisterActions() {
		actionMapper.dispose();
	}

	private void showPopupMenu(FilePanel filePanel, MouseEvent e) {
		if(popupMenu==null) {
			return;
		}

		refreshActions(filePanel);
		popupMenu.pack();
		popupMenu.show(filePanel, e.getX(), e.getY());
	}

	private void refreshActions(FilePanel filePanel) {
		boolean isConflictOutline = trackingStatus==TrackingStatus.CORRUPTED;
		actionManager.setEnabled(isConflictOutline,
				"replaydh.ui.core.workspaceTrackerPanel.markFileResolved");
	}

	private FilePanel getPopupOwner() {
		Component invoker = popupMenu.getInvoker();
		return invoker instanceof FilePanel ? (FilePanel) invoker : null;
	}

	public Set<LocalFileObject> getFiles() {
		return files==null ? Collections.emptySet() : files;
	}


	public TrackingStatus getTrackingStatus() {
		return trackingStatus;
	}


	public int getFilesVisible() {
		return filesVisible;
	}


	public void setFiles(Set<LocalFileObject> files) {
		this.files = files;
		refreshUI();
	}


	public void setFilesVisible(int filesVisible) {
		checkArgument("Value must be positive", filesVisible>0);
		this.filesVisible = filesVisible;
		refreshUI();
	}

	private FilePanel getCachedOrCreateFilePanel(LocalFileObject file) {
		FilePanel panel;
		if(cachedPanels.isEmpty()) {
			panel = new FilePanel(file);
		} else {
			panel = cachedPanels.pop();
			panel.setFileObject(file);
		}
		panel.addMouseListener(handler);
		return panel;
	}

	private void cachePanel(FilePanel panel) {
		panel.removeMouseListener(handler);
		cachedPanels.add(panel);
	}

	private void refreshUI() {
		Set<LocalFileObject> files = getFiles();

		// Nothing to change if panel is empty and no new files have been set
//		if(activePanels.isEmpty() && files.isEmpty()) {
//			return;
//		}

		// Clear current state
		activePanels.forEach(panel -> {
			panel.close();
			cachePanel(panel);
		});
		activePanels.clear();
		fileToPanelMapping.clear();
		contentPanel.removeAll();

		// Get ordered list of all the files for this panel
		final List<LocalFileObject> orderedFiles = new ArrayList<>(files);
		Collections.sort(orderedFiles);

		for(LocalFileObject fileObject : orderedFiles) {

			FilePanel filePanel = getCachedOrCreateFilePanel(fileObject);
			contentPanel.add(filePanel);
			activePanels.add(filePanel);

			filePanel.refreshUI();

			fileToPanelMapping.put(fileObject, filePanel);
		}

		revalidate();
	}

	public void updateFilePanel(LocalFileObject file) {
		FilePanel panel = fileToPanelMapping.get(file);

		if(panel!=null) {
			panel.refreshUI();
		}
	}

	@Override
	public void close() {
		activePanels.forEach(FilePanel::close);

		activePanels.clear();
		cachedPanels.clear();

		fileToPanelMapping.clear();

		unregisterActions();
		//TODO
	}

	private static String shortenFileName(Path file, int limit) {
		//FIXME
		return file.toString().substring(0, limit);
	}

	private class Handler extends MouseAdapter {


		@Override
		public void mousePressed(MouseEvent e) {
			maybeShowPopupMenu(e);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			maybeShowPopupMenu(e);
		}

		private void maybeShowPopupMenu(MouseEvent e) {
			if(!e.isPopupTrigger()) {
				return;
			}

			if(e.getComponent() instanceof FilePanel) {
				FilePanel panel = (FilePanel) e.getComponent();
				showPopupMenu(panel, e);
			}
		}

		private void markFileResolved() {
			FilePanel filePanel = getPopupOwner();
			if(filePanel==null) {
				return;
			}

			LocalFileObject fileObject = filePanel.fileObject;
			if(fileObject==null) {
				return;
			}

			Path file = fileObject.getFile();

			ResourceManager rm = ResourceManager.getInstance();

			String message = rm.get("replaydh.panels.fileOutline.markFileResolved.message",file);
			String title = rm.get("replaydh.panels.fileOutline.resolvingFile.title");
			if(JOptionPane.showConfirmDialog(getRootPane(), message, title,
					JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {

				new AbstractDialogWorker<Boolean, Object>(
						SwingUtilities.getWindowAncestor(getRootPane()),
						ResourceManager.getInstance().get("replaydh.dialogs.checkWorkspaceForImport.title"),
						CancellationPolicy.NO_CANCEL) {

							@Override
							protected String getMessage(MessageType messageType, Throwable t) {
								ResourceManager rm = ResourceManager.getInstance();
								switch (messageType) {
								case RUNNING:
									log.info("Marking as resolved: {}", file);
									return rm.get("replaydh.panels.fileOutline.resolvingFile.active", file);
								case FAILED:
									log.error("Failed to mark file resolved: {}", file, t);
									return rm.get("replaydh.panels.fileOutline.resolvingFile.failed", t.getMessage());
								case FINISHED:
									log.info("File marked as resolved: {}", file);
									return rm.get("replaydh.panels.fileOutline.resolvingFile.done", file);

								default:
									throw new IllegalArgumentException("Unknown or unsupported message type: "+messageType);
								}
							}

							@Override
							protected Boolean doInBackground() throws Exception {
								return environment.getClient().getFileTracker()
										.markConflictResolved(Collections.singleton(file));
							}

				}.start();
			}
		}
	}

	private static final Border defaultBorder = BorderFactory.createCompoundBorder(
			new LineBorder(Color.BLACK), new EmptyBorder(1, 1, 1, 1));

	private static final Border excludedBorder = BorderFactory.createCompoundBorder(
			new LineBorder(Color.BLUE), new EmptyBorder(1, 1, 1, 1));

	private class FilePanel extends JPanel implements CloseableUI {

		private static final long serialVersionUID = 4664367218908329084L;

		private LocalFileObject fileObject;

		private final JLabel label;
		private final JTextArea textArea;

		public FilePanel(LocalFileObject fileObject) {
			super(new BorderLayout());

			setBorder(defaultBorder);

			label = new JLabel();
			label.setBorder(Paddings.DLU2);
			label.setHorizontalAlignment(SwingConstants.LEFT);

			textArea = new JTextArea();
			textArea.setEditable(false);
			textArea.setBackground(label.getBackground());
			textArea.setForeground(label.getForeground());
			textArea.setFont(label.getFont());

			//TODO if we add more components make sure that the label is properly assigned correct location
			add(label, BorderLayout.NORTH);
			add(textArea, BorderLayout.CENTER);

			setFileObject(fileObject);
		}

		/**
		 * @see javax.swing.JComponent#getBaseline(int, int)
		 */
		@Override
		public int getBaseline(int width, int height) {
			return 0;
		}

		public void setFileObject(LocalFileObject file) {
			requireNonNull(file);

			this.fileObject = file;

//			refreshUI();
		}

		private void refreshUI() {
			Path file = fileObject.getFile();
			String title = fileObject==null ? "<no fileObject>" : file.getFileName().toString();
			String tooltip = null;
			if(title.length()>displayablePathLength) {
				tooltip = title;

				title = shortenFileName(fileObject.getFile(), displayablePathLength);
			}

			label.setText(title);
			label.setToolTipText(GuiUtils.toSwingTooltip(tooltip));

			setBorder(RDHUtils.getBasicIgnoreFilter(environment).test(file) ?
					excludedBorder : defaultBorder);

			// More detailed outline

			StringBuilder buffer = new StringBuilder();

			if(Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
				buffer.append(ResourceManager.getInstance().get(
						"replaydh.panels.fileOutline.size")+": ").append(IOUtils.readableSize(file)).append('\n');
			}

			//TODO add some more information
//			buffer.append("TODO: metadata");

			textArea.setText(buffer.toString());
		}

		@Override
		public void close() {
			fileObject = null;
		}
	}
}

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
package bwfdm.replaydh.ui.metadata;

import static java.util.Objects.requireNonNull;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.io.FileTracker;
import bwfdm.replaydh.io.TrackerListener;
import bwfdm.replaydh.io.TrackingAction;
import bwfdm.replaydh.metadata.MetadataBuilder;
import bwfdm.replaydh.metadata.MetadataEditor;
import bwfdm.replaydh.metadata.MetadataListener;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRecord.Target;
import bwfdm.replaydh.metadata.MetadataRepository;
import bwfdm.replaydh.metadata.MetadataSchema;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.actions.ActionManager;
import bwfdm.replaydh.ui.actions.ActionManager.ActionMapper;
import bwfdm.replaydh.ui.helper.CloseableUI;
import bwfdm.replaydh.ui.helper.Editor;
import bwfdm.replaydh.ui.helper.TableColumnAdjuster;
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.ui.icons.Resolution;
import bwfdm.replaydh.ui.tree.AbstractTreeCellRendererPanel;
import bwfdm.replaydh.utils.Options;

/**
 * @author Markus Gärtner
 *
 */
public class MetadataManagerPanel extends JPanel implements CloseableUI {

	private static final long serialVersionUID = 3903692114413022857L;

	private static final Logger log = LoggerFactory.getLogger(MetadataManagerPanel.class);

	private static volatile ActionManager sharedActionManager;

	private static ActionManager getSharedActionManager() {
		GuiUtils.checkEDT();

		ActionManager actionManager = sharedActionManager;
		if(actionManager==null) {
			actionManager = ActionManager.globalManager().derive();
			try {
				actionManager.loadActions(MetadataManagerPanel.class.getResource("metadata-manager-panel-actions.xml"));
			} catch (IOException e) {
				throw new RDHException("Failed to load actions for"+MetadataManagerPanel.class, e);
			}

			sharedActionManager = actionManager;
		}

		return actionManager;
	}

	private final JToolBar workspaceToolBar;

	private final RDHEnvironment environment;

	private final ActionManager actionManager;
	private final ActionMapper actionMapper;
	private final WorkspaceTreeModel workspaceTreeModel;

	private final JTree workspaceTree;
//	private final JTabbedPane recordTabs;
	private final RecordPanel recordPanel;

	private final MetadataRepository repository;

	private final Handler handler;

	public MetadataManagerPanel(RDHEnvironment environment) {
		super(new BorderLayout());

		handler = new Handler();

		this.environment = requireNonNull(environment);

		actionManager = getSharedActionManager().derive();
		actionMapper = actionManager.mapper(this);

		repository = environment.getClient().getLocalMetadataRepository();
		repository.addMetadataListener(handler);
		environment.addPropertyChangeListener(RDHEnvironment.NAME_WORKSPACE, handler);
		environment.getClient().getFileTracker().addTrackerListener(handler);

		/**
		 * New layout
		 * <pre>
		 * +---------------------------------------------+
		 * |         TOOLBAR (filter + control           |
		 * +--------------+------------------------------+
		 * |              |     RECORD TABS              |
		 * |  WORKSPACE   +------------------------------+
		 * |   OUTLINE    |  TOOLBAR  [ADD/EDIT/REMOVE]  |
		 * |              +------------------------------+
		 * |              |         RECORD               |
		 * |              |     OUTLINE/EDITOR           |
		 * +--------------+                              |
		 * |              |                              |
		 * |   ORPHANED   |                              |
		 * |   RECORDS    |                              |
		 * +--------------+------------------------------+
		 * </pre>
		 */

		// HEADER

		// LEFT AREA

		workspaceTreeModel = new WorkspaceTreeModel();
		workspaceTree = new JTree(workspaceTreeModel) {

			private static final long serialVersionUID = 3315499753160202967L;

			@Override
			public boolean getScrollableTracksViewportWidth() {
				return true;
			}
		};
		workspaceTree.setLargeModel(true);
		workspaceTree.setRootVisible(false);
		workspaceTree.setRowHeight(0);
		workspaceTree.expandRow(0);
		TreeSelectionModel treeSelectionModel = new DefaultTreeSelectionModel();
		treeSelectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		workspaceTree.setSelectionModel(treeSelectionModel);
		workspaceTree.addTreeSelectionListener(handler);
		workspaceTree.setCellRenderer(new TargetRenderer());

		JScrollPane leftScrollPane = new JScrollPane(workspaceTree);
		leftScrollPane.setBorder(GuiUtils.emptyBorder);

		Options tbOptions = new Options();
		tbOptions.put("selector", null); //TODO add filter outline
		workspaceToolBar = actionManager.createToolBar(
				"replaydh.ui.core.metadataManagerPanel.workspaceToolBarList", tbOptions);

		JPanel leftPanel = new JPanel(new BorderLayout());

		leftPanel.add(workspaceToolBar, BorderLayout.NORTH);
		leftPanel.add(leftScrollPane, BorderLayout.CENTER);
		//TODO add area for orphaned records

		// RIGHT AREA

//		recordTabs = new JTabbedPane();
//		recordTabs.addChangeListener(handler);

		recordPanel = new RecordPanel();

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
				leftPanel, recordPanel);
		splitPane.setResizeWeight(0); // Assign all empty space to the right outline
		splitPane.setDividerLocation(200);
		add(splitPane, BorderLayout.CENTER);

		environment.getClient().getGui().registerHelp(this, "replaydh.ui.core.metadataManagerPanel");

		registerActions();

		reset();
	}

	private void registerActions() {
		//TODO
	}

	private void refreshActions() {
		actionMapper.mapAction("replaydh.ui.core.metadataManagerPanel.refreshWorkspaceInfo", handler::refreshWorkspaceInfo);
		actionMapper.mapAction("replaydh.ui.core.metadataManagerPanel.expandAllFolders", handler::expandAllFolders);
		actionMapper.mapAction("replaydh.ui.core.metadataManagerPanel.collapseAllFolders", handler::collapseAllFolders);
	}

	private void reset() {
		reset(environment.getWorkspacePath());
	}

	private void reset(Path path) {
		workspaceTreeModel.setRootFolder(path);
		recordPanel.reset(null);

		refreshActions();
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.CloseableUI#close()
	 */
	@Override
	public void close() {
		repository.removeMetadataListener(handler);
		environment.removePropertyChangeListener(RDHEnvironment.NAME_WORKSPACE, handler);
		environment.getClient().getFileTracker().removeTrackerListener(handler);
		actionMapper.dispose();
		workspaceTreeModel.setRootFolder(null);
	}

	private Target getTarget(TreePath treePath) {
		Path path = treePath==null ? null : (Path) treePath.getLastPathComponent();
		if(path==null || Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
			return null;
		}
		return new Target(environment.getWorkspacePath(), path);
	}

	private Editor<MetadataBuilder> createEditorForBuild() {
		return new MetadataUIBuilder();
	}

	private Editor<MetadataEditor> createEditorForEdit() {
		return new MetadataUIEditor();
	}

//	private static class TargetContainer extends JPanel {
//
//		private static final long serialVersionUID = -6651019691586199973L;
//
//		private final Target target;
//
//		TargetContainer(Target target) {
//			super(new BorderLayout());
//
//			this.target = requireNonNull(target);
//		}
//
//		public Target getTarget() {
//			return target;
//		}
//	}

	private static final Object NO_RECORD_PRESENT = new Object() {
		@Override
		public String toString() {
			return ResourceManager.getInstance().get("replaydh.ui.core.metadataManagerPanel.noRecords");
		}
	};

	private class RecordPanel extends JPanel implements CloseableUI {

		private static final long serialVersionUID = -8270015752516385805L;

		private final MetadataRecordTableModel recordTableModel;
		private final JComboBox<Object> schemaSelect;
		private final JToolBar recordToolBar;

		private final JTable entryTable;
		private final TableColumnAdjuster columnAdjuster;

		private final JLabel targetLabel;

		private Target target;

		private List<MetadataRecord> records = new ArrayList<>();
		private MetadataRecord currentRecord;

		public RecordPanel() {
			super(new BorderLayout());

			recordTableModel = new MetadataRecordTableModel();
			entryTable = new JTable(recordTableModel, recordTableModel.getColumnModel());
			entryTable.setColumnSelectionAllowed(false);
			entryTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			entryTable.setFillsViewportHeight(true);
			entryTable.setIntercellSpacing(new Dimension(3, 3));
			entryTable.setDefaultRenderer(String.class, new MetadataRecordTableCellRenderer());
			entryTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

			columnAdjuster = new TableColumnAdjuster(entryTable);
			columnAdjuster.setColumnDataIncluded(true);
			columnAdjuster.setColumnHeaderIncluded(true);
			columnAdjuster.setOnlyAdjustLarger(true);

			JScrollPane scrollPane = new JScrollPane(entryTable);
			scrollPane.setBorder(GuiUtils.bottomLineBorder);

			schemaSelect = new JComboBox<>(new DefaultComboBoxModel<>()); // just to make sure we know what kind of model
			schemaSelect.setEditable(false);
			schemaSelect.addActionListener(this::onSchemaSelected);

			targetLabel = new JLabel();
			targetLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

			Options recordTbOptions = new Options();
			recordTbOptions.put("target", targetLabel);
			recordTbOptions.put("selector", schemaSelect);
			recordToolBar = actionManager.createToolBar(
					"replaydh.ui.core.metadataManagerPanel.recordToolBarList", recordTbOptions);

			add(recordToolBar, BorderLayout.NORTH);
			add(scrollPane, BorderLayout.CENTER);

			registerActions();

			reset(null);
		}

		private void registerActions() {
			actionMapper.mapAction("replaydh.ui.core.metadataManagerPanel.addRecord", this::addRecord);
			actionMapper.mapAction("replaydh.ui.core.metadataManagerPanel.editRecord", this::editRecord);
			actionMapper.mapAction("replaydh.ui.core.metadataManagerPanel.removeRecord", this::removeRecord);
			actionMapper.mapAction("replaydh.ui.core.metadataManagerPanel.copyRecordText", this::copyRecordText);
		}

		private void refreshActions() {
			boolean hasTarget = target!=null;
			boolean hasRecord = hasTarget && currentRecord!=null;

			boolean canAddRecord = hasTarget && records.size()<repository.getAvailableSchemaCount();

			actionManager.setEnabled(canAddRecord,
					"replaydh.ui.core.metadataManagerPanel.addRecord");
			actionManager.setEnabled(hasRecord,
					"replaydh.ui.core.metadataManagerPanel.editRecord",
					"replaydh.ui.core.metadataManagerPanel.removeRecord");
		}

		@Override
		public void close() {
			records.clear();
		}

		public void update() {
			reset(target);
		}

		public void reset(Target target) {

			records.clear();
			currentRecord = null;

			if(target!=null) {
				records.addAll(repository.getRecords(target));

				targetLabel.setText(Paths.get(target.getPath()).getFileName().toString());
				targetLabel.setToolTipText(target.getPath());
			} else {
				targetLabel.setText("-");
				targetLabel.setToolTipText(null);
			}
			this.target = target;

			DefaultComboBoxModel<Object> schemaList = (DefaultComboBoxModel<Object>) schemaSelect.getModel();
			schemaList.removeAllElements();
			records.stream()
				.map(MetadataRecord::getSchemaId)
				.sorted()
				.map(repository::lookupSchema)
				.forEach(schemaList::addElement);

			if(schemaList.getSize()==0) {
				schemaList.addElement(NO_RECORD_PRESENT);
			}

			recordTableModel.update(null);

			schemaSelect.setSelectedIndex(0);

			refreshActions();
		}

//		private Target currentTarget() {
//			TargetContainer container = (TargetContainer) SwingUtilities.getAncestorOfClass(
//					TargetContainer.class, this);
//			return container==null ? null : container.getTarget();
//		}

		private MetadataSchema currentSchema() {
			Object schema = schemaSelect.getSelectedItem();
			return schema instanceof MetadataSchema ? (MetadataSchema) schema : null;
		}

		private void onSchemaSelected(ActionEvent ae) {
			refreshActions();

			MetadataSchema schema = currentSchema();
			if(schema==null) {
				return;
			}

			currentRecord = repository.getRecord(target, schema.getId());
			recordTableModel.update(currentRecord);
		}

		private void addRecord(ActionEvent ae) {
			if(target==null) {
				return;
			}

			Set<String> tmp = repository.getAvailableSchemaIds();
			records.forEach(r -> tmp.remove(r.getSchemaId()));
			List<String> schemaIds = new ArrayList<>(tmp);
			Collections.sort(schemaIds);

			JPopupMenu popup = new JPopupMenu();
			for(String schemaId : schemaIds) {
				popup.add(new JMenuItem(schemaId)).addActionListener(
						e -> GuiUtils.invokeEDTLater(
								() -> addRecord(schemaId)));
			}
			Component source = (Component) ae.getSource();
			GuiUtils.invokeEDTLater(() -> popup.show(source, 0, source.getHeight()));
		}

		private void addRecord(String schemaId) {
			if(target==null) {
				return;
			}
			MetadataSchema schema = repository.lookupSchema(schemaId);

			// Fetch raw editor
			MetadataBuilder metadataBuilder = repository.createBuilder(target, schema.getId());
			metadataBuilder.start();

			// Wrap into GUI-based editor
			Editor<MetadataBuilder> editor = createEditorForBuild();
			editor.setEditingItem(metadataBuilder);

			// Let user decide
			boolean applied = GuiUtils.showEditorDialogWithControl(
					GuiUtils.getFrame(getRootPane()),
					editor,
					"replaydh.ui.core.metadataManagerPanel.dialogs.addRecord.title", true);

			// Depending on user choice either cancel or finish build
			if(applied) {
				// Switch execution to background thread for storing the final record (might involve I/O work)
				environment.execute(() -> {
					repository.beginUpdate();
					try {
						MetadataRecord record = metadataBuilder.build();
						repository.addRecord(record);
						log.info("Finished adding metadata record: "+target);
					} catch(Exception e) {
						// Go back to EDT for displaying a dialog
						GuiUtils.invokeEDT(() -> GuiUtils.showErrorDialog(getRootPane(), e));
					} finally {
						repository.endUpdate();
					}
				});
			} else {
				metadataBuilder.cancel();
			}
		}

		private void editRecord(ActionEvent ae) {
			if(currentRecord==null) {
				return;
			}

			// Fetch raw editor
			MetadataEditor metadataEditor = repository.createEditor(currentRecord);
			metadataEditor.start();

			// Wrap into GUI-based editor
			Editor<MetadataEditor> editor = createEditorForEdit();
			editor.setEditingItem(metadataEditor);

			// Let user decide
			boolean applied = GuiUtils.showEditorDialogWithControl(
					GuiUtils.getFrame(getRootPane()),
					editor,
					"replaydh.ui.core.metadataManagerPanel.dialogs.editRecord.title", true);

			// Depending on user choice either commit or discard changes
			if(applied) {
				// Switch execution to background thread for committing (might involve I/O work)
				environment.execute(() -> {
					repository.beginUpdate();
					try {
						metadataEditor.commit();
						log.info("Finished editing metadata record: "+currentRecord.getTarget());
					} catch(Exception e) {
						// Go back to EDT for displaying a dialog
						GuiUtils.invokeEDT(() -> GuiUtils.showErrorDialog(getRootPane(), e));
					} finally {
						repository.endUpdate();
					}
				});
			} else {
				metadataEditor.discard();
			}
		}

		private void removeRecord(ActionEvent ae) {
			if(currentRecord==null) {
				return;
			}

			//TODO change to a custom dialog in GuiUtils? (to get rid of the default YES_NO option locale
			if(JOptionPane.showConfirmDialog(
					getRootPane(),
					ResourceManager.getInstance().get("replaydh.ui.core.metadataManagerPanel.dialogs.removeRecord.message"),
					ResourceManager.getInstance().get("replaydh.ui.core.metadataManagerPanel.dialogs.removeRecord.title"),
					JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) {

				// Switch execution to background thread (might involve I/O work)
				environment.execute(() -> {
					repository.beginUpdate();
					try {
						repository.removeRecord(currentRecord);
					} catch(Exception e) {
						// Go back to EDT for displaying a dialog
						GuiUtils.invokeEDT(() -> GuiUtils.showErrorDialog(getRootPane(), e));
					} finally {
						repository.endUpdate();
					}
				});
			}
		}

		private void copyRecordText(ActionEvent ae) {
			if(currentRecord==null) {
				return;
			}

			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			if(clipboard==null) {
				GuiUtils.beep();
				return;
			}

			String text = repository.toSimpleText(currentRecord);
			if(text==null || text.isEmpty()) {
				return;
			}

			StringSelection data = new StringSelection(text);

			try {
				clipboard.setContents(data, data);
			} catch (IllegalStateException e) {
				GuiUtils.beep();
			}
		}
	}

	private class Handler implements TreeSelectionListener, PropertyChangeListener, MetadataListener, TrackerListener {

		/**
		 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
		 */
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			GuiUtils.invokeEDTLater(() -> reset(environment.getWorkspacePath()));
		}

		/**
		 * @see javax.swing.event.TreeSelectionListener#valueChanged(javax.swing.event.TreeSelectionEvent)
		 */
		@Override
		public void valueChanged(TreeSelectionEvent e) {
			Target target = getTarget(e.getPath());
			if(target!=null) {
				recordPanel.reset(target);
			}
		}

		private void refreshWorkspaceInfo(ActionEvent ae) {
			reset();
		}

		private void expandAllFolders(ActionEvent ae) {
			GuiUtils.expandAll(workspaceTree, true);
		}

		private void collapseAllFolders(ActionEvent ae) {
			GuiUtils.expandAll(workspaceTree, false);
		}

		/**
		 * @see bwfdm.replaydh.io.TrackerListener#refreshStarted(bwfdm.replaydh.io.FileTracker)
		 */
		@Override
		public void refreshStarted(FileTracker tracker) {
			// TODO Auto-generated method stub

		}

		/**
		 * @see bwfdm.replaydh.io.TrackerListener#refreshFailed(bwfdm.replaydh.io.FileTracker, java.lang.Exception)
		 */
		@Override
		public void refreshFailed(FileTracker tracker, Exception e) {
			// TODO Auto-generated method stub

		}

		/**
		 * @see bwfdm.replaydh.io.TrackerListener#refreshDone(bwfdm.replaydh.io.FileTracker, boolean)
		 */
		@Override
		public void refreshDone(FileTracker tracker, boolean canceled) {
			// TODO Auto-generated method stub

		}

		/**
		 * @see bwfdm.replaydh.io.TrackerListener#trackingStatusChanged(bwfdm.replaydh.io.FileTracker, java.util.Set, bwfdm.replaydh.io.TrackingAction)
		 */
		@Override
		public void trackingStatusChanged(FileTracker tracker, Set<Path> files, TrackingAction action) {
			// TODO Auto-generated method stub

		}

		/**
		 * @see bwfdm.replaydh.io.TrackerListener#statusInfoChanged(bwfdm.replaydh.io.FileTracker)
		 */
		@Override
		public void statusInfoChanged(FileTracker tracker) {
			// TODO Auto-generated method stub

		}

		private void onRecordChange() {
			recordPanel.update();
			workspaceTree.revalidate();
			workspaceTree.repaint();
		}

		/**
		 * @see bwfdm.replaydh.metadata.MetadataListener#metadataRecordAdded(bwfdm.replaydh.metadata.MetadataRepository, bwfdm.replaydh.metadata.MetadataRecord)
		 */
		@Override
		public void metadataRecordAdded(MetadataRepository repository, MetadataRecord record) {
			onRecordChange();
		}

		/**
		 * @see bwfdm.replaydh.metadata.MetadataListener#metadataRecordRemoved(bwfdm.replaydh.metadata.MetadataRepository, bwfdm.replaydh.metadata.MetadataRecord)
		 */
		@Override
		public void metadataRecordRemoved(MetadataRepository repository, MetadataRecord record) {
			onRecordChange();
		}

		/**
		 * @see bwfdm.replaydh.metadata.MetadataListener#metadataRecordChanged(bwfdm.replaydh.metadata.MetadataRepository, bwfdm.replaydh.metadata.MetadataRecord)
		 */
		@Override
		public void metadataRecordChanged(MetadataRepository repository, MetadataRecord record) {
			onRecordChange();
		}
	}

	private class TargetRenderer extends AbstractTreeCellRendererPanel {

		private static final long serialVersionUID = -1328710338763028623L;

		private final JFileChooser fc = new JFileChooser();

		private final JLabel fileLabel;
		private final JLabel metadataLabel;

		TargetRenderer() {
			fileLabel = new JLabel("XXXXXXXXXXX");
			fileLabel.setIconTextGap(4);

			metadataLabel = new JLabel(IconRegistry.getGlobalRegistry().getIcon(
					"icons8-Ok-48.png", Resolution.forSize(16)));

			FormBuilder.create()
				.columns("max(100;pref):grow:fill, 2dlu, pref")
				.rows("max(18;pref)")
				.panel(this)
				.add(fileLabel).xy(1, 1, "left, center")
				.add(metadataLabel).xy(3, 1, "right, center")
				.build();

			setOpaque(true);
		}

		/**
		 * @see bwfdm.replaydh.ui.tree.AbstractTreeCellRendererPanel#prepareRenderer(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int, boolean)
		 */
		@Override
		protected void prepareRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf,
				int row, boolean hasFocus) {

			Icon icon = null;
			String text = null;
			String tooltip = null;

			if(value instanceof Path) {
				 Path file = (Path) value;
				 text = file.getFileName().toString();
				 tooltip = file.toString();
				 icon = fc.getUI().getFileView(fc).getIcon(file.toFile());

				 Target target = new Target(environment.getWorkspacePath(), file);
				 metadataLabel.setVisible(repository.hasRecords(target));
			}

			fileLabel.setIcon(icon);
			fileLabel.setText(text);
			fileLabel.setToolTipText(tooltip);

			setMinimumSize(getPreferredSize());
		}
	}
}

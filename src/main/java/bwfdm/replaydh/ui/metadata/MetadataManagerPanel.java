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
import javax.swing.JComboBox;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.metadata.MetadataBuilder;
import bwfdm.replaydh.metadata.MetadataEditor;
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
		environment.addPropertyChangeListener(RDHEnvironment.NAME_WORKSPACE, handler);

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
		workspaceTree = new JTree(workspaceTreeModel);
		workspaceTree.setRootVisible(false);
		workspaceTree.expandRow(0);
		TreeSelectionModel treeSelectionModel = new DefaultTreeSelectionModel();
		treeSelectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		workspaceTree.setSelectionModel(treeSelectionModel);
		workspaceTree.addTreeSelectionListener(handler);
		//TODO set listeners and renderer for tree!!!

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

		reset(environment.getWorkspacePath());
	}

	private void registerActions() {
		//TODO
	}

	private void refreshActions() {
		//TODO
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
		actionMapper.dispose();
		workspaceTreeModel.setRootFolder(null);
		environment.removePropertyChangeListener(RDHEnvironment.NAME_WORKSPACE, handler);
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

		public void reset(Target target) {
//			System.out.println(target);

			records.clear();
			currentRecord = null;
			this.target = target;

			if(target!=null) {
				records.addAll(repository.getRecords(target));

				targetLabel.setText(Paths.get(target.getPath()).getFileName().toString());
				targetLabel.setToolTipText(target.getPath());
			} else {
				targetLabel.setText("-");
				targetLabel.setToolTipText(null);
			}

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

		private MetadataRecord currentRecord() {
//			Target target = currentTarget();
			if(target==null) {
				return null;
			}

			MetadataSchema schema = currentSchema();
			if(schema==null) {
				return null;
			}

			return repository.getRecord(target, schema.getId());
		}

		private void onSchemaSelected(ActionEvent ae) {
			refreshActions();
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
			MetadataRecord record = currentRecord();
			if(record==null) {
				return;
			}

			// Fetch raw editor
			MetadataEditor metadataEditor = repository.createEditor(record);
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
						log.info("Finished editing metadata record: "+record.getTarget());
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
			MetadataRecord record = currentRecord();
			if(record==null) {
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
						repository.removeRecord(record);
					} catch(Exception e) {
						// Go back to EDT for displaying a dialog
						GuiUtils.invokeEDT(() -> GuiUtils.showErrorDialog(getRootPane(), e));
					} finally {
						repository.endUpdate();
					}
				});
			}
		}
	}

	private class Handler implements TreeSelectionListener, PropertyChangeListener, ChangeListener {

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

		/**
		 * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
		 */
		@Override
		public void stateChanged(ChangeEvent e) {
//			int tabIndex = recordTabs.
		}

	}
}

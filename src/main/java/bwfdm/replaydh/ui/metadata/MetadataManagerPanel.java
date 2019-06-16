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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.factories.Forms;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.core.RDHTool;
import bwfdm.replaydh.core.ToolLifecycleListener;
import bwfdm.replaydh.core.ToolLifecycleState;
import bwfdm.replaydh.metadata.MetadataBuilder;
import bwfdm.replaydh.metadata.MetadataEditor;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRecord.Target;
import bwfdm.replaydh.metadata.MetadataRepository;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.actions.ActionManager;
import bwfdm.replaydh.ui.actions.ActionManager.ActionMapper;
import bwfdm.replaydh.ui.helper.CloseableUI;
import bwfdm.replaydh.ui.helper.Editor;
import bwfdm.replaydh.ui.helper.TableColumnAdjuster;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.metadata.MetadataAddRecordWizard.AddRecordContext;
import bwfdm.replaydh.utils.Options;
import bwfdm.replaydh.workflow.Identifiable;

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

	private final JToolBar toolBar;

	private final RDHEnvironment environment;

	private final ActionManager actionManager;
	private final ActionMapper actionMapper;

	private final DefaultComboBoxModel<MetadataRepository> repositorySelectorModel;

	private final MetadataRecordListModel recordListModel;
	private final MetadataRecordTableModel recordTableModel;

	private final JList<Target> recordList;
	private final JTable entryTable;
	private final TableColumnAdjuster columnAdjuster;

	private MetadataRepository activeRepository;

	private final Handler handler;

	public MetadataManagerPanel(RDHEnvironment environment) {
		super(new BorderLayout());

		handler = new Handler();

		this.environment = requireNonNull(environment);
		environment.getClient().addToolLifecycleListener(handler);

		actionManager = getSharedActionManager().derive();
		actionMapper = actionManager.mapper(this);


		/*
		 * +---------------------------------------------+
		 * |                  TOOLBAR                    |
		 * +--------------+------------------------------+
		 * |              |                              |
		 * |              |                              |
		 * |              |                              |
		 * |              |         RECORD               |
		 * |   RECORD     |         OUTLINE              |
		 * |    LIST      |                              |
		 * |              |                              |
		 * |              |                              |
		 * |              +------------------------------+
		 * |              |    ADD    EDIT   REMOVE      |
		 * +--------------+------------------------------+
		 */

		// HEADER

		repositorySelectorModel = new DefaultComboBoxModel<>();
		JComboBox<MetadataRepository> comboBox = new JComboBox<>(repositorySelectorModel);
		comboBox.setEditable(false);
		comboBox.setRenderer(new MetadataRepositoryListCellRenderer());
		comboBox.addActionListener(handler);

		Options toolBaroptions = new Options();
		toolBaroptions.put("selector", comboBox);
		toolBar = actionManager.createToolBar("replaydh.ui.core.metadataManagerPanel.toolBarList", toolBaroptions);
		add(toolBar, BorderLayout.NORTH);

		// LEFT AREA

		recordListModel = new MetadataRecordListModel();
		recordListModel.addListDataListener(handler);
		recordList = new JList<>(recordListModel);
		recordList.setCellRenderer(new MetadataRecordListCellRenderer());
		recordList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		recordList.addListSelectionListener(handler);

		JScrollPane leftScrollPane = new JScrollPane(recordList);
		leftScrollPane.setBorder(GuiUtils.emptyBorder);

		// RIGHT AREA

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

		JScrollPane rightScrollPane = new JScrollPane(entryTable);
		rightScrollPane.setBorder(GuiUtils.bottomLineBorder);

		JButton addRecordButton = (JButton) actionManager.createButton("replaydh.ui.core.metadataManagerPanel.addRecord");
		JButton editRecordButton = (JButton) actionManager.createButton("replaydh.ui.core.metadataManagerPanel.editRecord");
		JButton removeRecordButton = (JButton) actionManager.createButton("replaydh.ui.core.metadataManagerPanel.removeRecord");

		JPanel rightPanel = new JPanel(new BorderLayout());
		rightPanel.add(rightScrollPane, BorderLayout.CENTER);
		rightPanel.add(Forms.buttonBar((JComponent)Box.createHorizontalGlue(),
				addRecordButton, editRecordButton, removeRecordButton,
				(JComponent)Box.createHorizontalGlue()), BorderLayout.SOUTH);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, leftScrollPane, rightPanel);
		splitPane.setResizeWeight(0); // Assign all empty space to the right outline
		splitPane.setDividerLocation(200);
		add(splitPane, BorderLayout.CENTER);

		environment.getClient().getGui().registerHelp(this, "replaydh.ui.core.metadataManagerPanel");

		registerActions();

		refreshActions();

		MetadataRepository localRepository = environment.getClient().getLocalMetadataRepository();
		repositorySelectorModel.addElement(localRepository);
		repositorySelectorModel.setSelectedItem(localRepository);
	}

	private void registerActions() {
		actionMapper.mapAction("replaydh.ui.core.metadataManagerPanel.addRecord", handler::addRecord);
		actionMapper.mapAction("replaydh.ui.core.metadataManagerPanel.editRecord", handler::editRecord);
		actionMapper.mapAction("replaydh.ui.core.metadataManagerPanel.removeRecord", handler::removeRecord);
	}

	private void refreshActions() {
		boolean hasRepository = repositorySelectorModel.getSelectedItem()!=null;
		boolean hasSelectedRecord = hasRepository && recordList.getSelectedIndex()!=-1;

		actionManager.setEnabled(hasRepository,
				"replaydh.ui.core.metadataManagerPanel.addRecord");
		actionManager.setEnabled(hasSelectedRecord,
				"replaydh.ui.core.metadataManagerPanel.editRecord",
				"replaydh.ui.core.metadataManagerPanel.removeRecord");
	}

	private void refreshActiveRepository() {
		GuiUtils.checkEDT();

		MetadataRepository selectedRepository = (MetadataRepository) repositorySelectorModel.getSelectedItem();

		// Nothing to do if nothing has changed
		if(selectedRepository==activeRepository) {
			return;
		}

		activeRepository = selectedRepository;

		recordListModel.setRepository(activeRepository);
		recordList.clearSelection();

		refreshActions();
	}

	private void refreshSelectedRecord() {
		MetadataRecord record = null;

		/*
		 *  Only try to fetch actual record when we have both an active
		 *  repository and the repository confirms that it still has a
		 *  record for the selected target.
		 */
		if(activeRepository!=null) {
			Target target = recordList.getSelectedValue();
			if(target!=null && activeRepository.hasRecords(target)) {
				record = activeRepository.getRecord(target, null); //TODO
			}
		}

		recordTableModel.update(record);
//		columnAdjuster.adjustColumn(1);
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.CloseableUI#canClose()
	 */
	@Override
	public boolean canClose() {
		// TODO Auto-generated method stub
		return CloseableUI.super.canClose();
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.CloseableUI#close()
	 */
	@Override
	public void close() {
		actionMapper.dispose();
		recordListModel.setRepository(null);
		recordListModel.removeListDataListener(handler);
		environment.getClient().removeToolLifecycleListener(handler);
	}

	private MetadataRecord getSelectedRecord() {
		if(activeRepository==null) {
			return null;
		}

		Target target = recordList.getSelectedValue();
		if(target==null) {
			return null;
		}

		return activeRepository.getRecord(target, null); //TODO
	}

	private Editor<MetadataBuilder> createEditorForBuild() {
		return new MetadataUIBuilder();
	}

	private Editor<MetadataEditor> createEditorForEdit() {
		return new MetadataUIEditor();
	}

	private class Handler implements ActionListener, ToolLifecycleListener, ListSelectionListener, ListDataListener {

		private void addRecord(ActionEvent ae) {
			AddRecordContext context = AddRecordContext.blank(activeRepository, environment);
			boolean wizardDone = false;

			// Show the wizard for selecting type and location of target resource
			try(Wizard<AddRecordContext> wizard = MetadataAddRecordWizard.getWizard(
					SwingUtilities.getWindowAncestor(MetadataManagerPanel.this),
					environment)) {

				wizard.startWizard(context);

				wizardDone = wizard.isFinished() && !wizard.isCancelled();
			}

			if(wizardDone) {
				if(context.getRecord()!=null) {
					editRecord(context.getRepository(), context.getRecord());
				} else if(context.getIdentifiable()!=null) {
					buildRecord(context.getRepository(), context.getIdentifiable());
				}
				//TODO handle inconsistent context in case neither resource nor record is available
			}

//			GuiUtils.showDefaultInfo(MetadataManagerPanel.this, null);
		}

		private void editRecord(MetadataRepository repository, MetadataRecord record) {
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
						log.info("Finished editing metadata record: "+repository.getDisplayName(record));
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

		private void buildRecord(MetadataRepository repository, Identifiable resource) {
			// Fetch raw editor
			MetadataBuilder metadataBuilder = repository.createBuilder(null, null); //TODO
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
						log.info("Finished adding metadata record: "+repository.getDisplayName(record));
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
			MetadataRecord record = getSelectedRecord();
			if(record==null) {
				return;
			}

			MetadataRepository repository = activeRepository;

			editRecord(repository, record);
		}

		private void removeRecord(ActionEvent ae) {

			MetadataRecord record = getSelectedRecord();
			if(record==null) {
				return;
			}

			MetadataRepository repository = activeRepository;

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

		/**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			refreshActiveRepository();
		}

		/**
		 * @see bwfdm.replaydh.core.ToolLifecycleListener#toolLifecycleStateChanged(bwfdm.replaydh.core.RDHTool, bwfdm.replaydh.core.ToolLifecycleState, bwfdm.replaydh.core.ToolLifecycleState)
		 */
		@Override
		public void toolLifecycleStateChanged(RDHTool tool, ToolLifecycleState oldState, ToolLifecycleState newState) {
			// TODO check if tool is a MetadataRepository instance and refresh repositorySelectorModel accordingly
		}

		/**
		 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
		 */
		@Override
		public void valueChanged(ListSelectionEvent e) {
			refreshSelectedRecord();

			refreshActions();
		}

		private void onListDataChanged() {
			GuiUtils.invokeEDT(MetadataManagerPanel.this::refreshSelectedRecord);
		}

		/**
		 * @see javax.swing.event.ListDataListener#intervalAdded(javax.swing.event.ListDataEvent)
		 */
		@Override
		public void intervalAdded(ListDataEvent e) {
			onListDataChanged();
		}

		/**
		 * @see javax.swing.event.ListDataListener#intervalRemoved(javax.swing.event.ListDataEvent)
		 */
		@Override
		public void intervalRemoved(ListDataEvent e) {
			onListDataChanged();
		}

		/**
		 * @see javax.swing.event.ListDataListener#contentsChanged(javax.swing.event.ListDataEvent)
		 */
		@Override
		public void contentsChanged(ListDataEvent e) {
			onListDataChanged();
		}
	}
}

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
package bwfdm.replaydh.ui.config;

import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.TreePath;

import org.java.plugin.registry.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.factories.Forms;

import bwfdm.replaydh.core.PluginEngine;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.stats.Interval;
import bwfdm.replaydh.stats.StatEntry;
import bwfdm.replaydh.stats.StatType;
import bwfdm.replaydh.ui.GuiStats;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.config.PreferencesTab.TabResult;
import bwfdm.replaydh.ui.config.PreferencesTab.UiOptions;
import bwfdm.replaydh.ui.config.PreferencesTreeModel.Node;

/**
 * @author Markus Gärtner
 *
 */
public class PreferencesDialog extends JDialog {

	public static void showDialog(RDHEnvironment environment, Component owner) {
		Interval uptime = new Interval();
		uptime.start();
		environment.getClient().getStatLog().log(StatEntry.ofType(StatType.UI_OPEN, GuiStats.DIALOG_PREFERENCES));

		try {
			PreferencesDialog dialog = new PreferencesDialog(environment);

			dialog.setLocationRelativeTo(owner);
			dialog.setVisible(true);

			dialog.dispose();
		} finally {
			uptime.stop();
			environment.getClient().getStatLog().log(StatEntry.withData(StatType.UI_CLOSE,
					GuiStats.DIALOG_PREFERENCES, uptime.asDurationString()));
		}
	}

	private static final long serialVersionUID = -1343410647244513683L;

	private static final Logger log = LoggerFactory.getLogger(PreferencesDialog.class);

	private final JTree tabTree;

	private final JScrollPane tabPane;
	private final JComponent tabButtons;

	private final JButton bApply, bReset, bApplyAndClose, bClose;

	private final PreferencesTreeModel treeModel;

	private final RDHEnvironment environment;
	private final PluginEngine pluginEngine;

	private final Map<Extension, PreferencesTab> tabs = new HashMap<>();

	private PreferencesTab activeTab;

	private final ChangeListener tabListener = this::onTabChange;

	public PreferencesDialog(RDHEnvironment environment) {

		setModalityType(ModalityType.APPLICATION_MODAL);

		JPanel panel = new JPanel(new BorderLayout());

		this.environment = requireNonNull(environment);

		pluginEngine = environment.getClient().getPluginEngine();

		treeModel = new PreferencesTreeModel(pluginEngine);

		tabTree = new JTree(treeModel);
		tabTree.addTreeSelectionListener(this::onTabSelect);
		tabTree.setCellRenderer(new PreferencesTreeCellRenderer());
		tabTree.setEditable(false);
		tabTree.setRootVisible(false);
		tabTree.setPreferredSize(new Dimension(200, 300));

		ResourceManager rm = ResourceManager.getInstance();

		setTitle(rm.get("replaydh.dialogs.preferences.title"));

		bReset = new JButton(rm.get("replaydh.dialogs.preferences.resetDefaults"));
		bReset.addActionListener(ae -> resetTab());

		bApply = new JButton(rm.get("replaydh.dialogs.preferences.apply"));
		bApply.addActionListener(ae -> {
			if(applyTab())
				restart();
		});

		bApplyAndClose = new JButton(rm.get("replaydh.dialogs.preferences.applyAndClose"));
		bApplyAndClose.addActionListener(ae -> applyTabAndClose());

		bClose = new JButton(rm.get("replaydh.dialogs.preferences.close"));
		bClose.addActionListener(ae -> close());

		tabButtons = Forms.buttonBar((JComponent)Box.createHorizontalGlue(), bReset, bApply);
		tabButtons.setBorder(GuiUtils.topLineBorder);

		tabPane = new JScrollPane();
		tabPane.setBorder(null);
		tabPane.setMinimumSize(new Dimension(350, 400));
		GuiUtils.defaultSetUnitIncrement(tabPane);

		JPanel pRight = new JPanel(new BorderLayout());
		pRight.add(tabPane, BorderLayout.CENTER);
		pRight.add(tabButtons, BorderLayout.SOUTH);

		JScrollPane spLeft = new JScrollPane(tabTree);
		GuiUtils.defaultSetUnitIncrement(spLeft);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, spLeft, pRight);
		splitPane.setResizeWeight(0);

		/**
		 * <pre>
		 * +-------+--------------------+
		 * |       |                    |
		 * |       |                    |
		 * |  TAB  |        TAB         |
		 * | TREE  |       CONTENT      |
		 * |       |                    |
		 * |       +-------+------+-----+
		 * |       |       | DEF  | APP |
		 * +-------+-------+---+--+-----+
		 * |       | APP+CLOSE | CANCEL |
		 * +-------+-----------+--------+
		 * </pre>
		 */

		panel.add(splitPane, BorderLayout.CENTER);

		panel.add(Forms.buttonBar((JComponent)Box.createHorizontalGlue(),
				bApplyAndClose, bClose), BorderLayout.SOUTH);

		add(panel);
		pack();

		displayProxy(false);
	}

	// LISTENER CALLBACKS

	private void onTabSelect(TreeSelectionEvent tse) {
		TreePath path = tse.getPath();
		if(path==null) {
			return;
		}

		Node node = (Node) path.getLastPathComponent();

		switch (node.getType()) {
		case PROXY:
		case ROOT:
			displayProxy(false);
			break;

		case TAB:
			displayTab(node.getExtension());
			break;

		default:
			throw new IllegalStateException("Unsupported node type: "+node.getType());
		}
	}

	private void onTabChange(ChangeEvent ce) {
		GuiUtils.checkEDT();
		checkState("Corrupted UI state - cannot handle change event without active tab", activeTab!=null);

		refreshControls();
	}

	// GUI METHODS

	private void refreshControls() {
		boolean canApply = activeTab!=null
				&& activeTab.hasPendingChanges();

		bApply.setEnabled(canApply);
		bApplyAndClose.setEnabled(canApply);
	}

	private void displayProxy(boolean error) {

		closeActiveTab();

		String key = error ? "replaydh.dialogs.preferences.errorMessage"
				: "replaydh.dialogs.preferences.proxyMessage";
		JTextArea textArea = GuiUtils.createTextArea(
				ResourceManager.getInstance().get(key));

		changeTabContent(textArea);
	}

	private void closeActiveTab() {
		if(activeTab!=null) {
			activeTab.removeChangeListener(tabListener);

			activeTab.close();

			activeTab = null;
		}
	}

	private void displayTab(Extension extension) {
		GuiUtils.checkEDT();

		PreferencesTab tab = tabs.get(extension);
		if(tab==null) {
			try {
				@SuppressWarnings("rawtypes")
				final Class[] signature = {RDHEnvironment.class};
				final Object[] params = {environment};
				tab = pluginEngine.instantiate(extension, signature, params);
				tabs.put(extension, tab);
			} catch (InstantiationException | IllegalAccessException | ClassNotFoundException
					| NoSuchMethodException | SecurityException | IllegalArgumentException
					| InvocationTargetException e) {
				displayProxy(true);

				log.error("Failed to instantiate preferences tab: {}", extension, e);
				return;
			}
		}

		if(activeTab!=null && tab==activeTab) {
			return;
		}

		closeActiveTab();

		activeTab = tab;

		tab.addChangeListener(tabListener);

		tab.update();

		Set<UiOptions> options = tab.getOptions();
		tabButtons.setVisible(!options.contains(UiOptions.SYNCHRONOUS));

		changeTabContent(tab.getPreferencesComponent());
	}

	private void changeTabContent(Component content) {
		tabPane.setViewportView(content);

		refreshControls();
	}

	private void applyTabAndClose() {
		boolean doRestart = applyTab();

		close();

		if(doRestart) {
			restart();
		}

	}

	private void close() {
		//TODO check for changes and let user confirm closing?

		closeActiveTab();

		setVisible(false);
	}

	private void restart() {
		// Postpone actual restart till the current GUI event queue is done
		GuiUtils.invokeEDT(() -> environment.getClient().restart());
	}

	/**
	 * Apply all changes and return {@code true} iff a restart is required
	 * for the changes to take effect.
	 */
	private boolean applyTab() {
		// Switch to signal that we should invoke a restart afterwards
		boolean doRestart = false;

		if(activeTab!=null) {
			// Let the tab itself persist the changes
			TabResult result = activeTab.apply();

			switch (result) {
			case REQUIRES_RESTART: {
				ResourceManager rm = ResourceManager.getInstance();
				String title = rm.get("replaydh.dialogs.preferences.restart");
				String message = rm.get("replaydh.dialogs.preferences.restartMessage");

				/*
				 *  Ask user for confirmation and restart client.
				 *
				 *  If user declines restart request he will have to manually close and start
				 *  or directly restart the client for the changes to take effect.
				 */
				if(JOptionPane.OK_OPTION==JOptionPane.showOptionDialog(
						tabPane, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
						null, null, null)) {
					doRestart = true;
				}
			} break;

			case FAILED:
				//TODO report issues to user
				break;

			// default case is "DONE"
			case DONE:
			default:
				// do nothing
				break;
			}

			refreshControls();
		}

		return doRestart;
	}

	private void resetTab() {
		if(activeTab!=null) {
			activeTab.resetDefaults();

			refreshControls();
		}
	}
}

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

import static java.util.Objects.requireNonNull;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.factories.Forms;

import bwfdm.replaydh.core.PluginEngine;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;

/**
 * @author Markus Gärtner
 *
 */
public class PreferencesDialog extends JPanel {

	private static final long serialVersionUID = -1343410647244513683L;

	private static final Logger log = LoggerFactory.getLogger(PreferencesDialog.class);

	private final JTree tabTree;

	private final JScrollPane tabPane;
	private final JComponent tabButtons;

	private final PreferencesTreeModel treeModel;

	private final RDHEnvironment environment;

	public PreferencesDialog(RDHEnvironment environment) {
		super(new BorderLayout());

		this.environment = requireNonNull(environment);

		final PluginEngine pluginEngine = environment.getClient().getPluginEngine();

		treeModel = new PreferencesTreeModel(pluginEngine);

		tabTree = new JTree(treeModel);
		tabTree.addTreeSelectionListener(this::onTabSelect);
		tabTree.setCellRenderer(new PreferencesTreeCellRenderer());
		tabTree.setEditable(false);

		ResourceManager rm = ResourceManager.getInstance();

		JButton bReset = new JButton(rm.get("replaydh.dialogs.preferences.resetDefaults"));
		bReset.addActionListener(ae -> resetTab());

		JButton bApply = new JButton(rm.get("replaydh.dialogs.preferences.apply"));
		bApply.addActionListener(ae -> applyTab());

		tabButtons = Forms.buttonBar((JComponent)Box.createHorizontalGlue(), bReset, bApply);
		tabButtons.setBorder(GuiUtils.topLineBorder);

		tabPane = new JScrollPane();
		GuiUtils.defaultSetUnitIncrement(tabPane);

		JPanel pRight = new JPanel(new BorderLayout());
		pRight.add(tabPane, BorderLayout.CENTER);
		pRight.add(tabButtons, BorderLayout.SOUTH);

		JScrollPane spLeft = new JScrollPane(tabTree);
		GuiUtils.defaultSetUnitIncrement(spLeft);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, spLeft, pRight);

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

		add(splitPane, BorderLayout.CENTER);
	}

	private void onTabSelect(TreeSelectionEvent tse) {
		//TODO
	}

	private void applyTab() {
		//TODO
	}

	private void resetTab() {
		//TODO
	}

	public void showDialog(Component parent) {
		String title = ResourceManager.getInstance().get("replaydh.dialogs.preferences.title");
		int result = JOptionPane.showConfirmDialog(parent, this, title, JOptionPane.OK_CANCEL_OPTION);

		if(result==JOptionPane.OK_OPTION) {
			applyTab();
		}

		//TODO rewrite to manually manage the apply+close and close buttons
	}
}

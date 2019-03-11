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
package bwfdm.replaydh.git;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;

/**
 * @author Markus Gärtner
 *
 */
public class GitDialogs {

	/**
	 * Ask user if he wants to mark the conflicting files as resolved.
	 *
	 * @param git
	 * @param conflictingPaths
	 * @return {@code true} iff all the files should be marked resolved
	 */
	public static boolean showConflictingFilesList(Set<String> conflictingPaths) {

		ResourceManager rm = ResourceManager.getInstance();
		String title = rm.get("replaydh.dialogs.markConflictsResolved.title");

		String header = rm.get("replaydh.dialogs.markConflictsResolved.message");
		String footer = rm.get("replaydh.dialogs.markConflictsResolved.footer");

		StringBuilder sb = new StringBuilder(conflictingPaths.size()*20);
		new ArrayList<>(conflictingPaths)
			.stream()
			.sorted()
			.forEach(path -> sb.append(path).append('\n'));

		JTextArea taPaths = GuiUtils.createTextArea(sb.toString());
		taPaths.setEditable(true);
		taPaths.setFocusable(true);
		taPaths.setLineWrap(false);
		JScrollPane spPaths = new JScrollPane(taPaths,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		spPaths.setPreferredSize(new Dimension(400, 200));
		JTextArea taHeader = GuiUtils.createTextArea(GuiUtils.wrapText(header));
		JTextArea taFooter = GuiUtils.createTextArea(GuiUtils.wrapText(footer));

		Object[] message = {
			taHeader,
			spPaths,
			taFooter,
		};

		Object[] options = {
				rm.get("replaydh.labels.yes"),
				rm.get("replaydh.labels.no"),
		};

		JOptionPane optionPane = new JOptionPane(message,
				JOptionPane.PLAIN_MESSAGE,
				JOptionPane.DEFAULT_OPTION,
				null, options, options[0]);
		JDialog dialog = optionPane.createDialog(title);
		dialog.setResizable(true);
		dialog.setVisible(true);

		return optionPane.getValue()==options[0];
	}
}

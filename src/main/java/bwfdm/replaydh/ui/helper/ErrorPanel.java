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
package bwfdm.replaydh.ui.helper;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;

/**
 * @author Markus Gärtner
 *
 */
public class ErrorPanel extends JPanel {

	private static final long serialVersionUID = 3719519611493002960L;

	private final JCheckBox cbShowStackTrace;
	private final JTextArea taOutput;

	private Throwable throwable;

	public ErrorPanel() {
		ResourceManager rm = ResourceManager.getInstance();

		cbShowStackTrace = new JCheckBox();
		cbShowStackTrace.setText(rm.get("replaydh.panels.error.showStackTrace.label"));
		cbShowStackTrace.setToolTipText(GuiUtils.toSwingTooltip(
				rm.get("replaydh.panels.error.showStackTrace.tooltip")));

		cbShowStackTrace.addActionListener(ae -> refreshContent());

		taOutput = GuiUtils.createTextArea("");
		taOutput.setColumns(60);
		taOutput.setRows(15);
		taOutput.setEditable(true);
		taOutput.setFocusable(true);
		taOutput.setLineWrap(false);
		taOutput.setBorder(GuiUtils.defaultContentBorder);

		FormBuilder.create()
			.columns("pref")
			.rows("pref, 4dlu, pref")
			.panel(this)
			.add(cbShowStackTrace).xy(1, 1, "right, center")
			.addScrolled(taOutput).xy(1, 3, "fill, top");

	}

	public ErrorPanel setThrowable(Throwable throwable) {
		this.throwable = throwable;

		cbShowStackTrace.setVisible(true);
		cbShowStackTrace.setEnabled(throwable!=null);

		refreshContent();

		return this;
	}

	public ErrorPanel refreshContent() {
		if(throwable==null) {
			taOutput.setText(null);
		} else if(cbShowStackTrace.isVisible()) {
			taOutput.setText(GuiUtils.errorText(throwable, cbShowStackTrace.isSelected()));
		}
		// nothing to do otherwise

		return this;
	}

	/**
	 * Overwrites the current text with the supplied one
	 * and hides the check box for toggling stack trace
	 * mode.
	 *
	 * @return
	 */
	public ErrorPanel setText(String text) {
		taOutput.setText(text);
		cbShowStackTrace.setVisible(false);
		return this;
	}
}

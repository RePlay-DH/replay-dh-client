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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.Border;

import bwfdm.replaydh.ui.GuiUtils;

/**
 * @author Markus Gärtner
 *
 */
public class PassiveTextArea extends JTextArea {

	private static final long serialVersionUID = -7415799770154666741L;


	public PassiveTextArea(String text) {
		this();

		setText(text);
	}

	public PassiveTextArea() {
		setFont(GuiUtils.defaultSmallInfoFont);
		setBackground(UIManager.getColor("Label.background"));
		setForeground(UIManager.getColor("Label.foreground"));
		setEditable(false);
		setLineWrap(true);
		setWrapStyleWord(true);
	}

	/**
	 * @see javax.swing.JTextArea#getScrollableTracksViewportWidth()
	 */
	@Override
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	/**
	 * @see javax.swing.JTextArea#getPreferredSize()
	 */
	@Override
	public Dimension getPreferredSize() {
		Dimension size = super.getPreferredSize();

		Component parent = getParent();
		if(parent!=null && !(parent instanceof JScrollPane)) {

			/*
			 * Fetch parent's current width so we can make sure to be smaller.
			 *
			 * FIXME halving the width here is an ugly workaround
			 */
			int hostWidth = parent.getWidth()/2;

			// Subtract border insets if applicable
			Border border = parent instanceof JComponent ? ((JComponent)parent).getBorder() : null;
			if(border!=null) {
				Insets insets = border.getBorderInsets(parent);
				hostWidth -= insets.left + insets.right;
			}

			size.width = Math.min(hostWidth, size.width);
		}
		return size;
	}
}

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
package bwfdm.replaydh.ui.list;

import java.awt.Color;
import java.awt.Component;
import java.awt.LayoutManager;

import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;

import bwfdm.replaydh.ui.helper.AbstractRendererPanel;

/**
 * @author Markus Gärtner
 *
 */
public abstract class AbstractListCellRendererPanel<E extends Object> extends AbstractRendererPanel implements ListCellRenderer<E> {

	private static final long serialVersionUID = -2696641572818669178L;

	public AbstractListCellRendererPanel() {
		super();
	}

	public AbstractListCellRendererPanel(boolean isDoubleBuffered) {
		super(isDoubleBuffered);
	}

	public AbstractListCellRendererPanel(LayoutManager layout, boolean isDoubleBuffered) {
		super(layout, isDoubleBuffered);
	}

	public AbstractListCellRendererPanel(LayoutManager layout) {
		super(layout);
	}

	/**
	 * @see javax.swing.ListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
	 */
	@Override
	public Component getListCellRendererComponent(JList<? extends E> list, E value,
			int index, boolean isSelected, boolean cellHasFocus) {

		isSelected = prepareColor(list, value, index, isSelected, cellHasFocus);

        prepareBorder(list, value, index, isSelected, cellHasFocus);

        setEnabled(list.isEnabled());

        prepareRenderer(list, value, index, isSelected, cellHasFocus);

//        System.out.printf("refreshing: value=%s index=%d bg=%s fg=%s\n", value, index, getBackground(), getForeground());

		return this;
	}

	protected boolean prepareColor(JList<? extends E> list, E value,
			int index, boolean isSelected, boolean cellHasFocus) {

        Color bg = null;
        Color fg = null;

        JList.DropLocation dropLocation = list.getDropLocation();
        if (dropLocation != null
                && !dropLocation.isInsert()
                && dropLocation.getIndex() == index) {

            bg = UIManager.getColor("List.dropCellBackground");
            fg = UIManager.getColor("List.dropCellForeground");

            isSelected = true;
        }

        if (isSelected) {
            bg = (bg == null ? list.getSelectionBackground() : bg);
            fg = (fg == null ? list.getSelectionForeground() : fg);
        }
        else {
            bg = list.getBackground();
            fg = list.getForeground();
        }

       setBackground(bg);
       setForeground(fg);

       return isSelected;
	}

	protected void prepareBorder(JList<? extends E> list, E value,
			int index, boolean isSelected, boolean cellHasFocus) {

        Border border = null;
        if (cellHasFocus) {
            if (isSelected) {
                border = UIManager.getBorder("List.focusSelectedCellHighlightBorder"); //$NON-NLS-1$
            }
            if (border == null) {
                border = UIManager.getBorder("List.focusCellHighlightBorder"); //$NON-NLS-1$
            }
        } else {
            border = noFocusBorder;
        }
        setBorder(border);
	}

	protected abstract void prepareRenderer(JList<? extends E> list, E value,
			int index, boolean isSelected, boolean cellHasFocus);

}

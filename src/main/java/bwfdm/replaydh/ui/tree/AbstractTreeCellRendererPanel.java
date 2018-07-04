/*
 *  ICARUS -  Interactive platform for Corpus Analysis and Research tools, University of Stuttgart
 *  Copyright (C) 2012-2013 Markus Gärtner and Gregor Thiele
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses.

 * $Revision: 244 $
 * $Date: 2014-04-10 14:09:12 +0200 (Do, 10 Apr 2014) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus/core/de.ims.icarus/source/de/ims/icarus/ui/tree/AbstractTreeCellRendererPanel.java $
 *
 * $LastChangedDate: 2014-04-10 14:09:12 +0200 (Do, 10 Apr 2014) $
 * $LastChangedRevision: 244 $
 * $LastChangedBy: mcgaerty $
 */
package bwfdm.replaydh.ui.tree;

import java.awt.Color;
import java.awt.Component;
import java.awt.LayoutManager;

import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.tree.TreeCellRenderer;

import bwfdm.replaydh.ui.helper.AbstractRendererPanel;

/**
 * @author Markus Gärtner
 * @version $Id: AbstractTreeCellRendererPanel.java 244 2014-04-10 12:09:12Z mcgaerty $
 *
 */
public abstract class AbstractTreeCellRendererPanel extends AbstractRendererPanel implements TreeCellRenderer {

	private static final long serialVersionUID = -1661016190973599562L;

	public AbstractTreeCellRendererPanel() {
		super();
	}

	public AbstractTreeCellRendererPanel(boolean isDoubleBuffered) {
		super(isDoubleBuffered);
	}

	public AbstractTreeCellRendererPanel(LayoutManager layout,
			boolean isDoubleBuffered) {
		super(layout, isDoubleBuffered);
	}

	public AbstractTreeCellRendererPanel(LayoutManager layout) {
		super(layout);
	}

	/**
	 * @see javax.swing.tree.TreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int, boolean)
	 */
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean selected, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {

		selected = prepareColor(tree, value, selected, expanded, leaf, row, hasFocus);

        prepareBorder(tree, value, selected, expanded, leaf, row, hasFocus);

        setEnabled(tree.isEnabled());

        prepareRenderer(tree, value, selected, expanded, leaf, row, hasFocus);

//        System.out.printf("refreshing: value=%s bg=%s fg=%s\n", value, getBackground(), getForeground());

		return this;
	}

	protected boolean prepareColor(JTree tree, Object value,
			boolean selected, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {

        Color bg = null;
        Color fg = null;

        JTree.DropLocation dropLocation = tree.getDropLocation();
        if (dropLocation != null
                && dropLocation.getChildIndex() == -1
                && tree.getRowForPath(dropLocation.getPath()) == row) {

            bg = UIManager.getColor("Tree.dropCellBackground"); //$NON-NLS-1$
            fg = UIManager.getColor("Tree.dropCellForeground"); //$NON-NLS-1$

            selected = true;
        }

        if (selected) {
            bg = (bg == null ? UIManager.getColor("Tree.selectionBackground") : bg); //$NON-NLS-1$
            fg = (fg == null ? UIManager.getColor("Tree.selectionForeground") : fg); //$NON-NLS-1$
        }
        else {
            bg = tree.getBackground();
            fg = tree.getForeground();
        }

       setBackground(bg);
       setForeground(fg);

       return selected;
	}

	protected void prepareBorder(JTree tree, Object value,
			boolean selected, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {

        Border border = null;
        if (hasFocus) {
            if (selected) {
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

	protected abstract void prepareRenderer(JTree tree, Object value,
			boolean selected, boolean expanded, boolean leaf, int row,
			boolean hasFocus);
}

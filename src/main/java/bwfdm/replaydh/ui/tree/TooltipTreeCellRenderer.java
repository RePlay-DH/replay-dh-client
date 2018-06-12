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
 *
 * $Revision: 244 $
 * $Date: 2014-04-10 14:09:12 +0200 (Do, 10 Apr 2014) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus/core/de.ims.icarus/source/de/ims/icarus/ui/tree/TooltipTreeCellRenderer.java $
 *
 * $LastChangedDate: 2014-04-10 14:09:12 +0200 (Do, 10 Apr 2014) $
 * $LastChangedRevision: 244 $
 * $LastChangedBy: mcgaerty $
 */
package bwfdm.replaydh.ui.tree;

import java.awt.Component;
import java.awt.FontMetrics;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.id.Identity;


/**
 * @author Markus Gärtner
 * @version $Id: TooltipTreeCellRenderer.java 244 2014-04-10 12:09:12Z mcgaerty $
 *
 */
public class TooltipTreeCellRenderer extends DefaultTreeCellRenderer {

	private static final long serialVersionUID = -28033820708371349L;

	public TooltipTreeCellRenderer() {
		// no-op
	}

	/**
	 * @see javax.swing.tree.DefaultTreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int, boolean)
	 */
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value,
			boolean sel, boolean expanded, boolean leaf, int row,
			boolean hasFocus) {

		String tooltip = null;
		Icon icon = null;

		if(value instanceof Identity) {
			Identity identity = (Identity) value;
			value = identity.getName();
			tooltip = identity.getDescription();
			icon = identity.getIcon();
		}

		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf,
				row, hasFocus);

		int columnWidth = tree.getWidth();
		if(icon!=null) {
			columnWidth -= icon.getIconWidth()+getIconTextGap();
		}
		int textWidth = 0;

		if(tooltip==null) {
			tooltip = getText();
		}

		if(tooltip!=null && !tooltip.isEmpty()) {
			FontMetrics fm = getFontMetrics(getFont());
			textWidth = fm.stringWidth(tooltip);
		}

		if(textWidth<=columnWidth) {
			tooltip = null;
		}

		setIcon(icon);

		setToolTipText(GuiUtils.toUnwrappedSwingTooltip(tooltip));

		return this;
	}
}

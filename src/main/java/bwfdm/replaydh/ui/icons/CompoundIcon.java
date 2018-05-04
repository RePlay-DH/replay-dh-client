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
package bwfdm.replaydh.ui.icons;

import static java.util.Objects.requireNonNull;

import java.awt.Component;
import java.awt.Graphics;
import java.util.Arrays;

import javax.swing.Icon;


/**
 * @author Markus Gärtner
 * @version $Id: CompoundIcon.java 458 2016-05-02 15:24:40Z mcgaerty $
 *
 */
public class CompoundIcon implements Icon {

	public static final int TOP_LEFT = 0;
	public static final int TOP_RIGHT = 1;
	public static final int BOTTOM_LEFT = 2;
	public static final int BOTTOM_RIGHT = 3;

	private final Icon baseIcon;

	private Icon[] overlayIcons;

	/**
	 * @param baseIcon
	 */
	public CompoundIcon(Icon baseIcon) {
		this.baseIcon = requireNonNull(baseIcon);
	}

	/**
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
	 */
	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		baseIcon.paintIcon(c, g, x, y);

		if(overlayIcons==null)
			return;

		Icon overlay;

		if((overlay=overlayIcons[TOP_LEFT])!=null) {
			overlay.paintIcon(c, g, x, y);
		}

		if((overlay=overlayIcons[TOP_RIGHT])!=null) {
			overlay.paintIcon(c, g, x+getIconWidth()-overlay.getIconWidth(), y);
		}

		if((overlay=overlayIcons[BOTTOM_LEFT])!=null) {
			overlay.paintIcon(c, g, x, y+getIconHeight()-overlay.getIconHeight());
		}

		if((overlay=overlayIcons[BOTTOM_RIGHT])!=null) {
			overlay.paintIcon(c, g, x+getIconWidth()-overlay.getIconWidth(),
					y+getIconHeight()-overlay.getIconHeight());
		}
	}

	public void removeOverlays() {
		if(overlayIcons!=null) {
			Arrays.fill(overlayIcons, null);
		}
	}

	public void setTopLeftOverlay(Icon icon) {
		setOverlay(TOP_LEFT, icon);
	}

	public void setTopRightOverlay(Icon icon) {
		setOverlay(TOP_RIGHT, icon);
	}

	public void setBottomLeftOverlay(Icon icon) {
		setOverlay(BOTTOM_LEFT, icon);
	}

	public void setBottomRightOverlay(Icon icon) {
		setOverlay(BOTTOM_RIGHT, icon);
	}

	public void setOverlay(int corner, Icon icon) {
		if(corner<0 || corner>3)
			throw new NullPointerException("Invalid corner for overlay icon: "+corner); //$NON-NLS-1$

		if(overlayIcons==null)
			overlayIcons = new Icon[4];

		overlayIcons[corner] = icon;
	}

	/**
	 * Returns the {@code width} of the {@code baseIcon}
	 * @see javax.swing.Icon#getIconWidth()
	 */
	@Override
	public int getIconWidth() {
		return baseIcon.getIconWidth();
	}

	/**
	 * Returns the {@code height} of the {@code baseIcon}
	 * @see javax.swing.Icon#getIconHeight()
	 */
	@Override
	public int getIconHeight() {
		return baseIcon.getIconHeight();
	}
}

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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.SwingConstants;


/**
 * Implements an {@code Icon} that can have arbitrary decorative
 * icons attached to it. The decorations are grouped into 3 layers
 * that are painted on top of each other so that icons in the higher
 * layers obscure those in lower ones.
 * <p>
 * Note that this class is not thread-safe since all modifications should
 * occur on the event dispatch thread only!
 *
 * @author Markus Gärtner
 * @version $Id: DecoratedIcon.java 192 2013-12-10 14:19:43Z mcgaerty $
 *
 */
public class DecoratedIcon implements Icon {

	public static final int BACKGROUND_LAYER = 0;
	public static final int DEFAULT_LAYER = 1;
	public static final int OVERLAY_LAYER = 2;

	private Icon baseIcon;
	private Dimension maxSize;

	private Decoration[][] decorations;
	private int[] decorationCounts;

	private BufferedImage buffer;

	public DecoratedIcon(Icon baseIcon) {
		this(baseIcon, 64, 32);
	}

	public DecoratedIcon(Icon baseIcon, int width, int height) {
		this.baseIcon = requireNonNull(baseIcon);
		this.maxSize = new Dimension(width, height);
		buffer = null;
	}

	public void setBaseIcon(Icon baseIcon) {
		requireNonNull(baseIcon);

		if(baseIcon.equals(this.baseIcon)) {
			return;
		}

		this.baseIcon = baseIcon;

		rebuildBuffer();
	}

	public void setMaxSize(int width, int height) {
		if(maxSize==null) {
			maxSize = new Dimension(width, height);
		} else {
			maxSize.setSize(width, height);
		}
	}

	private void rebuildBuffer() {
		if(decorations==null || decorationCounts==null) {
			return;
		}

		int width = baseIcon.getIconWidth();
		int height = baseIcon.getIconHeight();
		BufferedImage image = new BufferedImage(maxSize.width, maxSize.height, BufferedImage.TYPE_INT_ARGB);

		Graphics g = image.createGraphics();

		baseIcon.paintIcon(null, g, 0, 0);

		loop_layers : for(int layer=0; layer<decorationCounts.length; layer++) {
			if(decorations[layer]==null) {
				continue loop_layers;
			}

			int count = decorationCounts[layer];
			loop_decorations : for(int i=0; i<count; i++) {
				Decoration deco = decorations[layer][i];
				if(deco==null) {
					continue loop_decorations;
				}

				deco.icon.paintIcon(null, g, deco.x, deco.y);

				width = Math.max(width, deco.x+deco.icon.getIconWidth());
				height = Math.max(height, deco.y+deco.icon.getIconHeight());
			}
		}

		buffer = image.getSubimage(0, 0, width, height);
	}

	public void addBackgroundDecoration(Icon icon, int x, int y) {
		addDecoration(BACKGROUND_LAYER, icon, x, y);
	}

	public void addOverlayDecoration(Icon icon, int x, int y) {
		addDecoration(OVERLAY_LAYER, icon, x, y);
	}

	public void addDecoration(Icon icon, int x, int y) {
		addDecoration(DEFAULT_LAYER, icon, x, y);
	}

	public void addDecoration(Icon icon, int alignment) {
		int x, y;
		int w = getIconWidth();
		int h = getIconHeight();
		int wi = icon.getIconWidth();
		int hi = icon.getIconHeight();

		switch (alignment) {
		case SwingConstants.NORTH:
			x = w/2 - wi/2;
			y = 0;
			break;

		case SwingConstants.NORTH_EAST:
			x = w-wi;
			y = 0;
			break;

		case SwingConstants.NORTH_WEST:
			x = 0;
			y = 0;
			break;

		case SwingConstants.SOUTH:
			x = w/2 - wi/2;
			y = h-hi;
			break;

		case SwingConstants.SOUTH_EAST:
			x = w-wi;
			y = h-hi;
			break;

		case SwingConstants.SOUTH_WEST:
			x = 0;
			y = h-hi;
			break;

		case SwingConstants.EAST:
			x = w-wi;
			y = h/2 - hi/2;
			break;

		case SwingConstants.WEST:
			x = 0;
			y = h/2 - hi/2;
			break;

		case SwingConstants.CENTER:
			x = w/2 - wi/2;
			y = h/2 - hi/2;
			break;

		default:
			throw new IllegalArgumentException("Not a recognized alignment value: "+alignment); //$NON-NLS-1$
		}

		addDecoration(DEFAULT_LAYER, icon, x, y);
	}

	public void addDecoration(int layer, Icon icon, int x, int y) {
		requireNonNull(icon);

		if(layer<0 || layer>2)
			throw new NullPointerException("Invalid layer: "+layer); //$NON-NLS-1$

		if(decorations==null) {
			decorations = new Decoration[3][];
		}

		if(decorationCounts==null) {
			decorationCounts = new int[]{0, 0, 0};
		}

		if(decorations[layer]==null) {
			decorations[layer] = new Decoration[5];
		}

		Decoration[] layerItems = decorations[layer];
		int index = decorationCounts[layer];
		if(index>=layerItems.length) {
			Decoration[] newList = new Decoration[index*2];
			System.arraycopy(layerItems, 0, newList, 0, layerItems.length);
			decorations[layer] = newList;
			layerItems = newList;
		}

		layerItems[index] = new Decoration(icon, x, y);
		decorationCounts[layer]++;

		rebuildBuffer();
	}

	public void removeDecoration(Icon icon, int alignment) {
		int x, y;
		int w = getIconWidth();
		int h = getIconHeight();
		int wi = icon.getIconWidth();
		int hi = icon.getIconHeight();

		switch (alignment) {
		case SwingConstants.NORTH:
			x = w/2 - wi/2;
			y = 0;
			break;

		case SwingConstants.NORTH_EAST:
			x = w-wi;
			y = 0;
			break;

		case SwingConstants.NORTH_WEST:
			x = 0;
			y = 0;
			break;

		case SwingConstants.SOUTH:
			x = w/2 - wi/2;
			y = h-hi;
			break;

		case SwingConstants.SOUTH_EAST:
			x = w-wi;
			y = h-hi;
			break;

		case SwingConstants.SOUTH_WEST:
			x = 0;
			y = h-hi;
			break;

		case SwingConstants.EAST:
			x = w-wi;
			y = h/2 - hi/2;
			break;

		case SwingConstants.WEST:
			x = 0;
			y = h/2 - hi/2;
			break;

		case SwingConstants.CENTER:
			x = w/2 - wi/2;
			y = h/2 - hi/2;
			break;

		default:
			throw new IllegalArgumentException("Not a recognized alignment value: "+alignment); //$NON-NLS-1$
		}

		removeDecoration(DEFAULT_LAYER, icon, x, y);
	}

	public void removeDecoration(int layer, Icon icon, int x, int y) {
		requireNonNull(icon);

		if(layer<0 || layer>2)
			throw new NullPointerException("Invalid layer: "+layer); //$NON-NLS-1$

		if(decorations==null || decorations[layer]==null || decorationCounts==null) {
			return;
		}

		int count = decorationCounts[layer];
		Decoration[] layerItems = decorations[layer];
		for(int i=0; i<count; i++) {
			if(layerItems[i].icon==icon && layerItems[i].x==x && layerItems[i].y==y) {
				count--;
				for(int j=i; j<count; j++)
					layerItems[j] = layerItems[j+1];
			}
		}
		decorationCounts[layer] = count;

		rebuildBuffer();
	}

	public void removeDecorations(int layer) {
		if(layer<0 || layer>2)
			throw new NullPointerException("Invalid layer: "+layer); //$NON-NLS-1$

		if(decorations==null || decorations[layer]==null || decorationCounts==null) {
			return;
		}

		decorations[layer] = null;
		decorationCounts[layer] = 0;

		rebuildBuffer();
	}

	public void removeDecorations() {
		decorations = null;
		decorationCounts = null;
		buffer = null;
	}

	/**
	 * @see javax.swing.Icon#paintIcon(java.awt.Component, java.awt.Graphics, int, int)
	 */
	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		if(buffer==null) {
			baseIcon.paintIcon(c, g, x, y);
		} else {
			g.drawImage(buffer, x, y, buffer.getWidth(), buffer.getHeight(), c);
		}
	}

	/**
	 * @see javax.swing.Icon#getIconWidth()
	 */
	@Override
	public int getIconWidth() {
		return buffer==null ? baseIcon.getIconWidth() : buffer.getWidth();
	}

	/**
	 * @see javax.swing.Icon#getIconHeight()
	 */
	@Override
	public int getIconHeight() {
		return buffer==null ? baseIcon.getIconHeight() : buffer.getHeight();
	}

	private static class Decoration {
		private final Icon icon;
		private final int x;
		private final int y;

		Decoration(Icon icon, int x, int y) {
			this.icon = icon;
			this.x = x;
			this.y = y;
		}
	}
}

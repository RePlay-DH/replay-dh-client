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
package bwfdm.replaydh.ui.metadata;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * @author Markus Gärtner
 *
 */
public class MetadataRecordTableCellRenderer extends DefaultTableCellRenderer {

	private static final long serialVersionUID = 5283739048246211775L;

	private final EmptyLabelCellProxy emptyLabelCellProxy = new EmptyLabelCellProxy();

	/**
	 * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
	 */
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		//FIXME move away from hardcoded column value
		if(column==1 && value==null) {
			emptyLabelCellProxy.setBackground(getBackground());
			emptyLabelCellProxy.setBorder(getBorder());

			return emptyLabelCellProxy.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		} else {
			return this;
		}
	}

	private static class EmptyLabelCellProxy extends JComponent implements TableCellRenderer {

		private static final long serialVersionUID = 6800058254824253931L;

		/**
		 * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {

			return this;
		}

		private static final Stroke dashedStroke = new BasicStroke((float) 1, BasicStroke.CAP_BUTT,
				BasicStroke.JOIN_MITER, 10.0f, new float[]{1f, 1f}, 0.0f);

		/**
		 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
		 */
		@Override
		protected void paintComponent(Graphics g) {

			Graphics2D g2d = (Graphics2D)g;

			// Fetch bounds
			int w = getWidth();
			int h = getHeight();

			// Save relevant state of graphics
			Color c = g2d.getColor();
			Stroke s = g2d.getStroke();

			// Paint background
			g2d.setColor(getBackground());
			g2d.fillRect(0, 0, w, h);

			// Paint elbow line
			int x = Math.min(w/2, 20);
			int y = h/2;
			g2d.setColor(Color.gray);
			g2d.setStroke(dashedStroke);
			g2d.drawLine(x, 0, x, y);
			g2d.drawLine(x, y, w, y);

			// Reset graphics state
			g2d.setColor(c);
			g2d.setStroke(s);
		}
	}
}

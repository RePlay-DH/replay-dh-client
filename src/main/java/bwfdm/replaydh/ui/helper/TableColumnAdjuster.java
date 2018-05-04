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
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;


/**
 * @author Markus Gärtner
 *
 */
public class TableColumnAdjuster implements PropertyChangeListener, TableModelListener
{
	private JTable table;
	private int spacing;
	private boolean isColumnHeaderIncluded;
	private boolean isColumnDataIncluded;
	private boolean isOnlyAdjustLarger;
	private boolean isDynamicAdjustment;
	private Map<TableColumn, Integer> columnSizes = new HashMap<TableColumn, Integer>();

	/*
	 *  Specify the table and use default spacing
	 */
	public TableColumnAdjuster(JTable table)
	{
		this(table, 6);
	}

	/*
	 *  Specify the table and spacing
	 */
	public TableColumnAdjuster(JTable table, int spacing)
	{
		this.table = table;
		this.spacing = spacing;
		setColumnHeaderIncluded( true );
		setColumnDataIncluded( true );
		setOnlyAdjustLarger( true );
		setDynamicAdjustment( false );
		installActions();
	}

	/*
	 *  Adjust the widths of all the columns in the table
	 */
	public void adjustColumns()
	{
		TableColumnModel tcm = table.getColumnModel();

		for (int i = 0; i < tcm.getColumnCount(); i++)
		{
			adjustColumn(i);
		}
	}

	/*
	 *  Adjust the width of the specified column in the table
	 */
	public void adjustColumn(final int column)
	{
		TableColumn tableColumn = table.getColumnModel().getColumn(column);

		if (! tableColumn.getResizable()) return;

		int columnHeaderWidth = getColumnHeaderWidth( column );
		int columnDataWidth   = getColumnDataWidth( column );
		int preferredWidth    = Math.max(columnHeaderWidth, columnDataWidth);

		updateTableColumn(column, preferredWidth);
	}

	/*
	 *  Calculated the width based on the column name
	 */
	private int getColumnHeaderWidth(int column)
	{
		if (! isColumnHeaderIncluded) return 0;

		TableColumn tableColumn = table.getColumnModel().getColumn(column);
		Object value = tableColumn.getHeaderValue();
		TableCellRenderer renderer = tableColumn.getHeaderRenderer();

		if (renderer == null)
		{
			renderer = table.getTableHeader().getDefaultRenderer();
		}

		Component c = renderer.getTableCellRendererComponent(table, value, false, false, -1, column);
		return c.getPreferredSize().width;
	}

	/*
	 *  Calculate the width based on the widest cell renderer for the
	 *  given column.
	 */
	private int getColumnDataWidth(int column)
	{
		if (! isColumnDataIncluded) return 0;

		int preferredWidth = 0;
		int maxWidth = table.getColumnModel().getColumn(column).getMaxWidth();

		for (int row = 0; row < table.getRowCount(); row++)
		{
    		preferredWidth = Math.max(preferredWidth, getCellDataWidth(row, column));

			//  We've exceeded the maximum width, no need to check other rows

			if (preferredWidth >= maxWidth)
			    break;
		}

		return preferredWidth;
	}

	/*
	 *  Get the preferred width for the specified cell
	 */
	private int getCellDataWidth(int row, int column)
	{
		//  Inovke the renderer for the cell to calculate the preferred width

		TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
		Component c = table.prepareRenderer(cellRenderer, row, column);
		int width = c.getPreferredSize().width + table.getIntercellSpacing().width;

		return width;
	}

	/*
	 *  Update the TableColumn with the newly calculated width
	 */
	private void updateTableColumn(int column, int width)
	{
		final TableColumn tableColumn = table.getColumnModel().getColumn(column);

		if (! tableColumn.getResizable()) return;

		width += spacing;

		//  Don't shrink the column width

		if (isOnlyAdjustLarger)
		{
			width = Math.max(width, tableColumn.getPreferredWidth());
		}

		columnSizes.put(tableColumn, new Integer(tableColumn.getWidth()));
		table.getTableHeader().setResizingColumn(tableColumn);
		tableColumn.setWidth(width);
	}

	/*
	 *  Restore the widths of the columns in the table to its previous width
	 */
	public void restoreColumns()
	{
		TableColumnModel tcm = table.getColumnModel();

		for (int i = 0; i < tcm.getColumnCount(); i++)
		{
			restoreColumn(i);
		}
	}

	/*
	 *  Restore the width of the specified column to its previous width
	 */
	private void restoreColumn(int column)
	{
		TableColumn tableColumn = table.getColumnModel().getColumn(column);
		Integer width = columnSizes.get(tableColumn);

		if (width != null)
		{
			table.getTableHeader().setResizingColumn(tableColumn);
			tableColumn.setWidth( width.intValue() );
		}
	}

	/*
	 *	Indicates whether to include the header in the width calculation
	 */
	public void setColumnHeaderIncluded(boolean isColumnHeaderIncluded)
	{
		this.isColumnHeaderIncluded = isColumnHeaderIncluded;
	}

	/*
	 *	Indicates whether to include the model data in the width calculation
	 */
	public void setColumnDataIncluded(boolean isColumnDataIncluded)
	{
		this.isColumnDataIncluded = isColumnDataIncluded;
	}

	/*
	 *	Indicates whether columns can only be increased in size
	 */
	public void setOnlyAdjustLarger(boolean isOnlyAdjustLarger)
	{
		this.isOnlyAdjustLarger = isOnlyAdjustLarger;
	}

	/*
	 *  Indicate whether changes to the model should cause the width to be
	 *  dynamically recalculated.
	 */
	public void setDynamicAdjustment(boolean isDynamicAdjustment)
	{
		//  May need to add or remove the TableModelListener when changed

		if (this.isDynamicAdjustment != isDynamicAdjustment)
		{
			if (isDynamicAdjustment)
			{
				table.addPropertyChangeListener( this );
				table.getModel().addTableModelListener( this );
			}
			else
			{
				table.removePropertyChangeListener( this );
				table.getModel().removeTableModelListener( this );
			}
		}

		this.isDynamicAdjustment = isDynamicAdjustment;
	}
//
//  Implement the PropertyChangeListener
//
	@Override
	public void propertyChange(PropertyChangeEvent e)
	{
		//  When the TableModel changes we need to update the listeners
		//  and column widths

		if ("model".equals(e.getPropertyName())) //$NON-NLS-1$
		{
			TableModel model = (TableModel)e.getOldValue();
			model.removeTableModelListener( this );

			model = (TableModel)e.getNewValue();
			model.addTableModelListener( this );
			adjustColumns();
		}
	}
//
//  Implement the TableModelListener
//
	@Override
	public void tableChanged(TableModelEvent e)
	{
		if (! isColumnDataIncluded) return;

		//  A cell has been updated

		if (e.getType() == TableModelEvent.UPDATE && e.getColumn()>=0)
		{
			int column = table.convertColumnIndexToView(e.getColumn());

			//  Only need to worry about an increase in width for this cell

			if (isOnlyAdjustLarger)
			{
				int	row = e.getFirstRow();
				TableColumn tableColumn = table.getColumnModel().getColumn(column);

				if (tableColumn.getResizable())
				{
					int width =	getCellDataWidth(row, column);
					updateTableColumn(column, width);
				}
			}

			//	Could be an increase of decrease so check all rows

			else
			{
				adjustColumn( column );
			}
		}

		//  The update affected more than one column so adjust all columns

		else
		{
			adjustColumns();
		}
	}

	/*
	 *  Install Actions to give user control of certain functionality.
	 */
	private void installActions()
	{
		installColumnAction(true,  true,  "adjustColumn",   "control ADD"); //$NON-NLS-1$ //$NON-NLS-2$
		installColumnAction(false, true,  "adjustColumns",  "control shift ADD"); //$NON-NLS-1$ //$NON-NLS-2$
		installColumnAction(true,  false, "restoreColumn",  "control SUBTRACT"); //$NON-NLS-1$ //$NON-NLS-2$
		installColumnAction(false, false, "restoreColumns", "control shift SUBTRACT"); //$NON-NLS-1$ //$NON-NLS-2$

		installToggleAction(true,  false, "toggleDynamic",  "control MULTIPLY"); //$NON-NLS-1$ //$NON-NLS-2$
		installToggleAction(false, true,  "toggleLarger",   "control DIVIDE"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 *  Update the input and action maps with a new ColumnAction
	 */
	private void installColumnAction(
		boolean isSelectedColumn, boolean isAdjust, String key, String keyStroke)
	{
		Action action = new ColumnAction(isSelectedColumn, isAdjust);
		KeyStroke ks = KeyStroke.getKeyStroke( keyStroke );
		table.getInputMap().put(ks, key);
		table.getActionMap().put(key, action);
	}

	/*
	 *  Update the input and action maps with new ToggleAction
	 */
	private void installToggleAction(
		boolean isToggleDynamic, boolean isToggleLarger, String key, String keyStroke)
	{
		Action action = new ToggleAction(isToggleDynamic, isToggleLarger);
		KeyStroke ks = KeyStroke.getKeyStroke( keyStroke );
		table.getInputMap().put(ks, key);
		table.getActionMap().put(key, action);
	}

	/*
	 *  Action to adjust or restore the width of a single column or all columns
	 */
	class ColumnAction extends AbstractAction
	{
		private static final long serialVersionUID = -6161284824421440777L;

		private boolean isSelectedColumn;
    	private boolean isAdjust;

		public ColumnAction(boolean isSelectedColumn, boolean isAdjust)
		{
			this.isSelectedColumn = isSelectedColumn;
			this.isAdjust = isAdjust;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			//  Handle selected column(s) width change actions

			if (isSelectedColumn)
			{
				int[] columns = table.getSelectedColumns();

				for (int i = 0; i < columns.length; i++)
				{
					if (isAdjust)
						adjustColumn(columns[i]);
					else
						restoreColumn(columns[i]);
				}
			}
			else
			{
				if (isAdjust)
					adjustColumns();
				else
					restoreColumns();
			}
		}
	}

	/*
	 *  Toggle properties of the TableColumnAdjuster so the user can
	 *  customize the functionality to their preferences
	 */
	class ToggleAction extends AbstractAction
	{
		private static final long serialVersionUID = 1187697176223549706L;

		private boolean isToggleDynamic;
		private boolean isToggleLarger;

		public ToggleAction(boolean isToggleDynamic, boolean isToggleLarger)
		{
			this.isToggleDynamic = isToggleDynamic;
			this.isToggleLarger = isToggleLarger;
		}

		@Override
		public void actionPerformed(ActionEvent e)
		{
			if (isToggleDynamic)
			{
				setDynamicAdjustment(! isDynamicAdjustment);
				return;
			}

			if (isToggleLarger)
			{
				setOnlyAdjustLarger(! isOnlyAdjustLarger);
				return;
			}
		}
	}
}

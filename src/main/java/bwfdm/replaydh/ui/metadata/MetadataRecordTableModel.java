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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.resources.ResourceManager;

/**
 * @author Markus Gärtner
 *
 */
public class MetadataRecordTableModel extends AbstractTableModel {

	private static final long serialVersionUID = -5079304353128647063L;

	private final List<String> names = new ArrayList<>();
	private final List<String> values = new ArrayList<>();
	private final BitSet mainLabels = new BitSet(100);

	private final TableColumnModel columnModel;

	public MetadataRecordTableModel() {
		DefaultTableColumnModel columnModel = new DefaultTableColumnModel();
		columnModel.addColumn(createColumn(0, 20, 25, 50, "index"));
		columnModel.addColumn(createColumn(1, 50, 150, -1, "key"));
		columnModel.addColumn(createColumn(2, 50, -1, -1, "value"));

		this.columnModel = columnModel;
	}

	private static TableColumn createColumn(int index, int min, int width, int max, Object identifier) {
		TableColumn column = new TableColumn(index);
		column.setHeaderValue(ResourceManager.getInstance().get("replaydh.columns."+identifier));
		column.setIdentifier(identifier);
		if(min>0) {
			column.setMinWidth(min);
		}
		if(max>0) {
			column.setMaxWidth(max);
		}
		if(width>0) {
			column.setWidth(width);
			column.setPreferredWidth(width);
		}
		column.setResizable(true);

		return column;
	}

	public TableColumnModel getColumnModel() {
		return columnModel;
	}

	public void update(MetadataRecord record) {
		names.clear();
		values.clear();
		mainLabels.clear();

		if(record!=null && record.getEntryCount()>0) {
			List<String> rawKeys = new ArrayList<>(record.getEntryNames());
			Collections.sort(rawKeys);

			List<String> rawValues = new ArrayList<>();

			for(String name : rawKeys) {
				record.forEachEntry(name, e -> rawValues.add(e.getValue()));

				if(!rawValues.isEmpty()) {
					mainLabels.set(names.size());
					Collections.sort(rawValues);

					for(String value : rawValues) {
						names.add(name);
						values.add(value);
					}

					rawValues.clear();
				}
			}
		}


		fireTableDataChanged();
	}

	public boolean hasData() {
		return !names.isEmpty();
	}

	/**
	 * @see javax.swing.table.TableModel#getRowCount()
	 */
	@Override
	public int getRowCount() {
		return names.size();
	}

	/**
	 * @see javax.swing.table.TableModel#getColumnCount()
	 */
	@Override
	public int getColumnCount() {
		return 3;
	}

	/**
	 * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
	 */
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	/**
	 * @see javax.swing.table.TableModel#getValueAt(int, int)
	 */
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		switch (columnIndex) {
		case 0: return String.valueOf(rowIndex+1);
		case 1: return mainLabels.get(rowIndex) ? names.get(rowIndex) : null;
		case 2: return values.get(rowIndex);

		default:
			throw new IllegalArgumentException();
		}
	}

}

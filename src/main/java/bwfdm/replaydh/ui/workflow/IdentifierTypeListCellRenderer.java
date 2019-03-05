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
package bwfdm.replaydh.ui.workflow;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.workflow.schema.IdentifierType;

/**
 * @author Markus Gärtner
 *
 */
public class IdentifierTypeListCellRenderer extends DefaultListCellRenderer {

	private static final long serialVersionUID = -7124372580874578086L;

	/**
	 * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
	 */
	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {

		String tooltip = null;

		if(value instanceof IdentifierTypeProxy) {
			IdentifierTypeProxy proxy = (IdentifierTypeProxy) value;
			if(proxy.identifierType!=null) {
				value = proxy.identifierType;
			} else {
				value = proxy.label;
			}
		}

		if(value instanceof IdentifierType) {
			IdentifierType identifierType = (IdentifierType) value;

			value = identifierType.getName();
			tooltip = identifierType.getDescription();
		}

		super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		setToolTipText(GuiUtils.toSwingTooltip(tooltip));

		return this;
	}
}

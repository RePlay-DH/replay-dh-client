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

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class GUIElement {

		private JLabel label = null;
		private JTextField textfield = null;
		private JButton button = null;
		private JButton minusbutton = null;

		public JButton getMinusbutton() {
			return minusbutton;
		}
		public void setMinusbutton(JButton minusbutton) {
			this.minusbutton = minusbutton;
		}
		public JLabel getLabel() {
			return label;
		}
		public void setLabel(JLabel label) {
			this.label = label;
		}
		public JTextField getTextfield() {
			return textfield;
		}
		public void setTextfield(JTextField textfield) {
			this.textfield = textfield;
		}
		public JButton getButton() {
			return button;
		}
		public void setButton(JButton button) {
			this.button = button;
		}

		GUIElement(JLabel label, JTextField textfield, JButton button, JButton minusbutton) {

			this.label = label;
			this.textfield = textfield;
			this.button = button;
			this.minusbutton = minusbutton;
		}

}

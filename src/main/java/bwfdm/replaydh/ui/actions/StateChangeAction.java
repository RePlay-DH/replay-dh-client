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
package bwfdm.replaydh.ui.actions;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author Markus Gärtner
 * @version $Id: StateChangeAction.java 123 2013-07-31 15:22:01Z mcgaerty $
 * 
 */
public class StateChangeAction extends DelegateAction implements ItemListener {

	private static final long serialVersionUID = 1994858029114851771L;

	private boolean selected = false;

	public StateChangeAction() {
		// no-op
	}

	public boolean isSelected() {
		return selected;
	}

	public synchronized void setSelected(boolean newValue) {
		boolean oldValue = this.selected;
		if (oldValue != newValue) {
			this.selected = newValue;
			firePropertyChange("selected", Boolean.valueOf(oldValue), //$NON-NLS-1$
					Boolean.valueOf(newValue));
		}
	}

	public synchronized void addItemListener(ItemListener listener) {
		getEventListenerList().add(ItemListener.class, listener);
	}

	public synchronized void removeItemListener(ItemListener listener) {
		getEventListenerList().remove(ItemListener.class, listener);
	}

	public synchronized ItemListener[] getItemListeners() {
		return getEventListenerList().getListeners(ItemListener.class);
	}

	public void itemStateChanged(ItemEvent evt) {
		boolean newValue = evt.getStateChange()==ItemEvent.SELECTED;
		boolean oldValue = this.selected;

		if (oldValue != newValue) {
			setSelected(newValue);

	        Object[] listeners = getEventListenerList().getListenerList();
	        
	        for (int i = listeners.length-2; i>=0; i-=2) {
	            if (listeners[i]==ItemListener.class) {
	                ((ItemListener)listeners[i+1]).itemStateChanged(evt);
	            }
	        }
		}
	}
}

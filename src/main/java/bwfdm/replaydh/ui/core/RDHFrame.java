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
package bwfdm.replaydh.ui.core;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

/**
 * @author Markus Gärtner
 *
 */
public class RDHFrame extends JFrame {

	private static final long serialVersionUID = -8656279270742321894L;

	private Dimension storedSize;

	private final Map<String, Object> clientProperties = new HashMap<>();

	public Dimension getStoredSize() {
		return storedSize;
	}

	public void setStoredSize(Dimension storedSize) {
		this.storedSize = storedSize;
	}

	public void saveSize() {
		storedSize = getSize();
	}

	public boolean restoreSize() {
		boolean canRestore = storedSize!=null;
		if(canRestore) {
			setSize(storedSize);
		}
		storedSize = null;
		return canRestore;
	}

	public final void putClientProperty(String key, Object value) {
        Object oldValue;
		synchronized (clientProperties) {
            oldValue = clientProperties.get(key);
            if (value != null) {
                clientProperties.put(key, value);
            } else if (oldValue != null) {
                clientProperties.remove(key);
            } else {
                // old == new == null
                return;
            }
		}
        firePropertyChange(key, oldValue, value);
	}

	@SuppressWarnings("unchecked")
	public final <T extends Object> T getClientProperty(String key) {
		synchronized (clientProperties) {
			return (T) clientProperties.get(key);
		}
	}
}

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

/**
 * @author Markus Gärtner
 *
 */
public interface Editor<T extends Object> {

	/**
	 * Returns the {@code Component} this editor uses to
	 * present its user interface. This method must not return
	 * {@code null} values.
	 * <p>
	 * Note that it is not required for an editor to always return
	 * the same {@code Component}! Implementations using an editor are
	 * advised to retrieve the latest component used by an editor whenever
	 * they intend to display it.
	 */
	Component getEditorComponent();

	/**
	 * Optional method for editors that wish to interact with
	 * the surrounding control components. This will usually be
	 * buttons that present the suer with direct access to the
	 * {@link #applyEdit()} and {@link #resetEdit()} operations.
	 *
	 * @param control
	 */
	default void setControl(EditorControl control) {
		// no-op
	}

	/**
	 * Resets the editor to use the supplied {@code item}. It is legal to
	 * provide {@code null} values in which case the editor should simply
	 * clear its interface. If the supplied {@code item} is not of a supported
	 * type then the editor should throw an {@link IllegalArgumentException}.
	 */
	void setEditingItem(T item);

	/**
	 * Returns the object last set by {@link #setEditingItem(Object)} or
	 * {@code null} if this editor has not been assigned any items yet.
	 */
	T getEditingItem();

	/**
	 * Discards all user input and reloads the appearance based on the
	 * data last set via {@link #setEditingItem(Object)}. If no data is
	 * set to be edited then the editor should present a "blank" interface.
	 */
	void resetEdit();

	/**
	 * Applies the changes made by the user to the underlying object to
	 * be edited.
	 */
	void applyEdit();

	/**
	 * Compares the current <i>presented state</i> (including potential
	 * user input) with the object last set via {@link #setEditingItem(Object)}
	 * and returns {@code true} if and only if there is a difference between
	 * those two. If no object has been set for being edited then this method
	 * should return {@code false}.
	 */
	boolean hasChanges();

	/**
	 * Tells the editor to release all resources held by it and to
	 * unregister all listeners. After an editor has been closed it is
	 * no longer considered to be usable.
	 * <p>
	 * Note that this also includes releasing any reference to a previously
	 * set {@link EditorControl} instance if present!
	 */
	void close();

	/**
	 *
	 * @author Markus Gärtner
	 *
	 * @param <T>
	 */
	public interface TableEditor<T extends Object> extends Editor<T> {
		// no-op
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 * @param <T>
	 */
	public interface GraphEditor<T extends Object> extends Editor<T> {
		// no-op
	}
}

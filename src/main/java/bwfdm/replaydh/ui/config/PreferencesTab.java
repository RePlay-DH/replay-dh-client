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
package bwfdm.replaydh.ui.config;

import java.awt.Component;
import java.util.Collections;
import java.util.Set;

/**
 * @author Markus Gärtner
 *
 */
public interface PreferencesTab extends AutoCloseable {

	/**
	 * Returns the component used to display the associated UI of
	 * this tab. This method will be called at most once during the
	 * lifecycle of a {@link PreferencesTab} instance.
	 *
	 * @return
	 */
	Component getPreferencesComponent();

	/**
	 * Compares the UI content with the underlying settings and returns
	 * {@code true} iff there are mismatches.
	 *
	 * @return
	 */
	boolean hasPendingChanges();

	/**
	 * If the tab has {@link #hasPendingChanges() pending changes}
	 * then persist those in the underlying settings.
	 */
	void apply();

	/**
	 * Resets the underlying settings back to their system defaults
	 * and updates the UI accordingly.
	 */
	void resetDefaults();

	/**
	 * Allows the tab to customize the surrounding user interface by
	 * returning respective options.
	 * <p>
	 * The default implementation returns an empty set.
	 *
	 * @return
	 */
	default Set<UiOptions> getOptions() {
		return Collections.emptySet();
	}

	public enum LifecycleState {
		BLANK,
		BEFORE_SHOWING,
		SHOWING,
		BEFORE_HIDING,
		IDLE,
		;
	}

	public enum UiOptions {
		/**
		 * Signals that the tab doesn't require the optional "reset defaults"
		 * and "apply" buttons since all the changes are applied synchronously
		 * upon user interactions.
		 */
		SYNCHRONOUS,
		;
	}
}

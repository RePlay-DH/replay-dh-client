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
package bwfdm.replaydh.ui.help;

import static bwfdm.replaydh.utils.RDHUtils.checkState;

import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Markus Gärtner
 *
 */
public class HelpStore {

	private static final Logger log = LoggerFactory.getLogger(HelpStore.class);

	/** Stores the anchor ids for all registered components */
	private final Map<JComponent, String> componentAnchors = new WeakHashMap<>();

	public void register(JComponent component, String anchor) {
		checkState("Component already registered", !componentAnchors.containsKey(component));
		componentAnchors.put(component, anchor);
	}

	public void unregister(JComponent component) {
		componentAnchors.remove(component);
	}

	public void close() {
		hideHelp();
		componentAnchors.clear();
	}

	public void showHelp() {
		log.info("Showing global help markers");
		//TODO for all registered components that are visible, make sure to show the overlay hints
	}

	public void hideHelp() {
		log.info("Hiding global help markers");
		//TODO hide all the previously displayed help overlays
	}
}

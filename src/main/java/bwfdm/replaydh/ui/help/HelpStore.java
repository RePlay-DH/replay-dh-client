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

import java.awt.Dimension;
import java.awt.Point;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRootPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.ui.icons.IconRegistry;

/**
 * @author Markus Gärtner, Florian Fritze
 *
 */
public class HelpStore {

	public HelpStore() {
		panel = new JPanel();
		panel.setLayout(null);
	}

	private static final Logger log = LoggerFactory.getLogger(HelpStore.class);
	
	/** Stores the anchor ids for all registered components */
	private final Map<JComponent, String> componentAnchors = new WeakHashMap<>();
	
	private JPanel panel;
	
	private IconRegistry ir = IconRegistry.getGlobalRegistry();
	
	private Icon helpHint = ir.getIcon("icons8-Help-48-small.png");
	

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
		for(JComponent comp : componentAnchors.keySet()) {
			JRootPane root = comp.getRootPane();
			Dimension dim = root.getSize();
			panel.setSize(dim);
			JButton buttonLabel = new JButton(helpHint);
			Point location = comp.getLocation();
			System.out.println(location);
			buttonLabel.setBounds((comp.getWidth()/2)+location.x, location.y+20, 40, 40);
			buttonLabel.setToolTipText("Click me for help");
			panel.add(buttonLabel);
			root.setGlassPane(panel);
			panel.setVisible(true);
			panel.setOpaque(false);
		}
	}

	public void hideHelp() {
		log.info("Hiding global help markers");
		//TODO hide all the previously displayed help overlays
		panel.removeAll();
		panel.setVisible(false);
	}
	
}

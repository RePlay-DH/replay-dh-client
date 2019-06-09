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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.ui.icons.Resolution;
import bwfdm.replaydh.utils.WeakHashSet;

/**
 * @author Markus Gärtner, Florian Fritze
 *
 */
public class HelpStore extends WindowAdapter implements ComponentListener, ActionListener {

	private static final Logger log = LoggerFactory.getLogger(HelpStore.class);

	/** Stores the anchor ids for all registered components */
	private final Map<JComponent, String> componentAnchors = new WeakHashMap<>();

	private final HTMLHelpDisplay helpDisplay;

	private final Set<Container> observedContainers = new WeakHashSet<>();

	private final Set<Window> observedWindows = new WeakHashSet<>();

	private final Set<RootPaneContainer> roots = new WeakHashSet<>();

	private final Map<JComponent, JButton> hints = new WeakHashMap<>();

	/** Global help mode is on/off */
	private boolean helpShowing = false;

	/** Keeps track of the actual help window */
	private boolean helpWindowActive = false;

	public HelpStore() {
		helpDisplay = new HTMLHelpDisplay();
		helpDisplay.readHelpFile();
	}

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

	private void observe(Container container) {
		if(observedContainers.add(container)) {
			container.addComponentListener(this);

			Container parent = container.getParent();
			if(parent != null) {
				observe(parent);
			}
		}
	}

	private void observe(Window window) {
		if(observedWindows.add(window)) {
			window.addWindowListener(this);
		}
	}

	private void unobserveAll() {
		for(Container container : observedContainers) {
			container.removeComponentListener(this);
		}
		observedContainers.clear();

		for(Window window : observedWindows) {
			window.removeWindowListener(this);
		}
		observedWindows.clear();
	}

	public void showHelp() {
		if(helpShowing) {
			return;
		}

		log.info("Showing global help markers");

		for(Entry<JComponent, String> entry : componentAnchors.entrySet()) {
			JComponent target = entry.getKey();

			// Fetch container responsible for glass pane
			RootPaneContainer root = (RootPaneContainer) SwingUtilities.getAncestorOfClass(
					RootPaneContainer.class, target);

			// Ignore components outside of valid UI hierarchy
			if(root == null) {
				continue;
			}

			if(roots.add(root)) {
				// Make sure we're in control of any glass pane ever set!
				JPanel glassPane = new JPanel(null);
				glassPane.setOpaque(false);
				root.setGlassPane(glassPane);
				/*
				 *  Typical RootPaneContainer implementations try to maintain the old
				 *  visibility state when changing glass panes, so we need to manually
				 *  override it!
				 */
				glassPane.setVisible(true);
			}

			hints.put(target, createHint(entry.getValue()));

			// Register listeners to keep hints up2date
			if(root instanceof Window) {
				observe((Window) root);
			}
			observe(target);
		}

		positionHints();

		helpShowing = true;
	}

	private JButton createHint(String anchor) {
		Icon icon = IconRegistry.getGlobalRegistry().getIcon("icons8-Help-48.png", Resolution.forSize(16));
		JButton hint = new JButton(icon);
		hint.setFocusable(false);
		hint.setName(anchor);
		hint.setToolTipText(ResourceManager.getInstance().get("replaydh.display.help.toolTip"));
		hint.setBorder(null);
		hint.addActionListener(this);
		return hint;
	}

	private void positionHints() {
		for(Entry<JComponent, JButton> entry : hints.entrySet()) {
			JComponent target = entry.getKey();
			JButton hint = entry.getValue();

			hint.setVisible(target.isShowing());
			if(!hint.isVisible()) {
				continue;
			}

			RootPaneContainer root = (RootPaneContainer) SwingUtilities.getAncestorOfClass(
					RootPaneContainer.class, target);
			Container glassPane = (Container) root.getGlassPane();

			Point loc = SwingUtilities.convertPoint(target, 0, 0, glassPane);
			Dimension dim = hint.getPreferredSize();

			int newX = loc.x + (target.getWidth() - dim.width)/2;
			int newY = loc.y - dim.height/2;

			if(newY < 0) {
				newY = 0;
			}

			// If we haven't done so previously, finally add the hint to glass pane
			if(hint.getParent()!=glassPane) {
				glassPane.add(hint);
			}
			hint.setBounds(newX, newY, dim.width, dim.height);

//			System.out.printf("anchor=%s bounds=%s%n", hint.getName(), hint.getBounds());
		}
	}

	public void hideHelp() {
		if(!helpShowing) {
			return;
		}

		log.info("Hiding global help markers");

		hints.values().forEach(hint -> hint.removeActionListener(this));

		roots.stream()
			.map(RootPaneContainer::getGlassPane)
			.forEach(glassPane -> {
				((Container)glassPane).removeAll();
				glassPane.setVisible(false);
			});

		hints.clear();
		roots.clear();

		unobserveAll();

		helpShowing = false;
	}

	@Override
	public void componentResized(ComponentEvent e) {
		positionHints();
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		positionHints();
	}

	@Override
	public void componentShown(ComponentEvent e) {
		positionHints();
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		positionHints();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(helpWindowActive) {
			log.info("Ignoring request to show help window - window already active");
			return;
		}

		JButton button = (JButton) e.getSource();
		JFrame helpFrame = helpDisplay.showHelpSection(button.getName());
		helpWindowActive = true;
		helpFrame.addWindowListener(this);
		helpFrame.setName(button.getName()); //TODO change to something sensible!
	}

	@Override
	public void windowClosing(WindowEvent e) {
		helpWindowActive = false;
	}
}

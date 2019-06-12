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

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
public class HelpStore {

	private static final Logger log = LoggerFactory.getLogger(HelpStore.class);

	/** Stores the anchor ids for all registered components */
	private final Map<Component, String> componentAnchors = new WeakHashMap<>();

	/** Holds pending target components outside of UI hierarchy */
	private final Set<Component> headlessComponents = new WeakHashSet<>();

	/** Link to the actual help window manager */
	private final HTMLHelpDisplay helpDisplay;

	/** All the containers we need to keep track of for hint placement */
	private final Set<Container> observedContainers = new WeakHashSet<>();

	/** All the roots above registered and actually displayed components */
	private final Set<RootPaneContainer> roots = new WeakHashSet<>();

	/** Lookup to find the hints responsible for individual components */
	private final Map<Component, JLabel> hints = new WeakHashMap<>();

	/** Global help mode is on/off */
	private boolean helpShowing = false;

	/** Keeps track of the actual help window */
	private boolean helpWindowActive = false;

	private final MouseListener mouseListener;
	private final ComponentListener componentListener;
	private final WindowListener windowListener;

	private final Icon smallIcon, mediumIcon, largeIcon;

	public HelpStore() {
		helpDisplay = new HTMLHelpDisplay();
		helpDisplay.readHelpFile();

		IconRegistry ir = IconRegistry.getGlobalRegistry();
		smallIcon = ir.getIcon("icons8-Help-48.png", Resolution.forSize(16));
		mediumIcon = ir.getIcon("icons8-Help-48.png", Resolution.forSize(36));
		largeIcon = ir.getIcon("icons8-Help-48.png", Resolution.forSize(48));

		mouseListener = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getClickCount()==1 && !e.isPopupTrigger()) {
					JLabel hint = (JLabel)e.getComponent();
					showHelpForHint(hint);
				}
			}
		};

		componentListener = new ComponentListener() {

			private void onAnyComponentEvent(ComponentEvent e) {
				scheduleUpdate();
			}

			@Override
			public void componentShown(ComponentEvent e) {
				onAnyComponentEvent(e);
			}

			@Override
			public void componentResized(ComponentEvent e) {
				onAnyComponentEvent(e);
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				onAnyComponentEvent(e);
			}

			@Override
			public void componentHidden(ComponentEvent e) {
				onAnyComponentEvent(e);
			}
		};

		windowListener = new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// We're only ever monitoring the help window itself

				helpWindowActive = false;
				e.getWindow().removeWindowListener(this);
			}
		};
	}

	public void register(Component component, String anchor) {
		checkState("Component already registered", !componentAnchors.containsKey(component));
		componentAnchors.put(component, anchor);

//		System.out.printf("reg: anchor=%s help=%b%n", anchor, helpShowing);
		// If global help mode is already on, make sure the new component gets a proper hint
		if(helpShowing) {
			SwingUtilities.invokeLater(() -> {
				setupHint(component, anchor);
				scheduleUpdate();
			});
		}
	}

	public void unregister(Component component) {
		componentAnchors.remove(component);

		/*
		 *  Theoretically we should unregister listeners here, but we don't keep track of
		 *  the parent chains for observed containers. As such we cannot know if a container
		 *  is still needed for another target component and therefore we simply wait till
		 *  the next unobserveAll() call unregisters all listeners.
		 */
	}

	public void close() {
		hideHelp();
		componentAnchors.clear();
	}

	private void observe(Container container) {
		if(observedContainers.add(container)) {
			container.addComponentListener(componentListener);

			Container parent = container.getParent();
			if(parent != null) {
				observe(parent);
			}
		}
	}

	private void unobserveAll() {
		for(Container container : observedContainers) {
			container.removeComponentListener(componentListener);
		}
		observedContainers.clear();
	}

	public void showHelp() {
		if(helpShowing) {
			return;
		}

		log.info("Showing global help markers");

		for(Entry<Component, String> entry : componentAnchors.entrySet()) {
			// Try to create hint and mark target as headless if it fails
			if(!setupHint(entry.getKey(), entry.getValue())) {
				headlessComponents.add(entry.getKey());
			}
		}

		helpShowing = true;

		scheduleUpdate();
	}

	private boolean setupHint(Component target, String anchor) {

		// Fetch container responsible for glass pane
		RootPaneContainer root = (RootPaneContainer) SwingUtilities.getAncestorOfClass(
				RootPaneContainer.class, target);

		// Ignore components outside of valid UI hierarchy
		if(root == null) {
			return false;
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

		/* At this point we're blindly creating hints for all components.
		 * The positionHints() method will take care of only displaying the
		 * ones that belong to components what ate 'showing'.
		 */
		hints.put(target, createHint(anchor));

		// Register listeners to keep hints up2date
		if(root instanceof Window) {
			observe((Window) root);
		}
		if(target instanceof Container) {
			observe((Container) target);
		}

		return true;
	}

	private JLabel createHint(String anchor) {
		JLabel hint = new JLabel();
		hint.setFocusable(false);
		hint.setName(anchor);
		hint.setToolTipText(ResourceManager.getInstance().get("replaydh.display.help.toolTip"));
		hint.setBorder(null);
		hint.addMouseListener(mouseListener);
		return hint;
	}

	private final AtomicBoolean updateScheduled = new AtomicBoolean(false);

	private void scheduleUpdate() {
		if(updateScheduled.compareAndSet(false, true)) {
			SwingUtilities.invokeLater(() -> {
				updateScheduled.set(false);
				processHeadlessTargets();
				positionHints();
			});
		}
	}

	private void processHeadlessTargets() {
		if(headlessComponents.isEmpty()) {
			return;
		}

		for(Iterator<Component> it = headlessComponents.iterator(); it.hasNext();) {
			Component target = it.next();
			String anchor = componentAnchors.get(target);
			if(anchor==null || setupHint(target, anchor)) {
				// Remove pending components if we now have a hint or they are no longer needed
				it.remove();
			}
		}
	}

	private void positionHints() {
		if(!helpShowing) {
			return;
		}

		for(Entry<Component, JLabel> entry : hints.entrySet()) {
			Component target = entry.getKey();
			JLabel hint = entry.getValue();

			/*
			 *  Make sure that only hints for currently visible components are considered.
			 *  This also increases the chance of target having been assigned proper bounds.
			 */
			hint.setVisible(target.isShowing());
			if(!hint.isVisible()) {
				continue;
			}

			RootPaneContainer root = (RootPaneContainer) SwingUtilities.getAncestorOfClass(
					RootPaneContainer.class, target);
			Container glassPane = (Container) root.getGlassPane();

			int targetWidth = target.getWidth();
			int targetHeight = target.getHeight();
			hint.setIcon(getHintIcon(targetWidth, targetHeight));

			// Desired position is centered at the top, with a slight northwards offset
			Point targetLoc = SwingUtilities.convertPoint(target, 0, 0, glassPane);
			// Need to use preferred size as hint might not have been placed previously
			Dimension hintSize = hint.getPreferredSize();

			// Fetch hint location based on type and size of target
			Point hintLoc = getHintLocation(target, targetLoc, targetWidth, targetHeight,
					hintSize.width, hintSize.height);

			// If we haven't done so previously, finally add the hint to glass pane
			if(hint.getParent()!=glassPane) {
				glassPane.add(hint);
			}

			// Now position the hint
			hint.setBounds(hintLoc.x, hintLoc.y, hintSize.width, hintSize.height);
		}
	}

	/**
	 * Fetches the correct icon for hints based on size of target.
	 */
	private Icon getHintIcon(int targetWidth, int targetHeight) {
		int min = Math.min(targetWidth, targetHeight);
		Icon icon = smallIcon;
		if(min>150) {
			icon = largeIcon;
		} else if(min>70) {
			icon = mediumIcon;
		}
		return icon;
	}

	/**
	 * Calculates a good spot for displaying the hint overlay.
	 */
	private Point getHintLocation(Component target, Point targetLoc, int targetWidth, int targetHeight,
			int hintWidth, int hintHeight) {
		Point loc = new Point(0, 0);

		// For buttons or similar control elements put hint centered on top
		if(target instanceof AbstractButton) {
			loc.x = targetLoc.x + (targetWidth - hintWidth)/2;
			loc.y = targetLoc.y - hintHeight/2;
		} else {
			// For any other component (panel, graph, etc...) put in center of target
			loc.x = targetLoc.x + (targetWidth - hintWidth)/2;
			loc.y = targetLoc.y + (targetHeight - hintHeight)/2;
		}

		// Sanity check against overlapping the window/frame borders
		if(loc.y < 0) {
			loc.y = 0;
		}
		return loc;
	}

	public void hideHelp() {
		if(!helpShowing) {
			return;
		}

		log.info("Hiding global help markers");

		// Stop listening to changes early, otherwise we might get into nasty loops
		unobserveAll();

		// Unregister action listeners
		hints.values().forEach(hint -> hint.removeMouseListener(mouseListener));
		hints.clear();

		// Clear glass panes
		roots.stream()
			.map(RootPaneContainer::getGlassPane)
			.forEach(glassPane -> {
				((Container)glassPane).removeAll();
				glassPane.setVisible(false);
			});
		roots.clear();

		headlessComponents.clear();

		helpShowing = false;
	}

	private void showHelpForHint(JLabel hint) {
		if(helpWindowActive) {
			log.info("Ignoring request to show help window - window already active");
			return;
		}

		JFrame helpFrame = helpDisplay.showHelpSection(hint.getName());
		helpWindowActive = true;
		helpFrame.addWindowListener(windowListener);
		helpFrame.setName(hint.getName()); //TODO change to something sensible!

	}
}

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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRootPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.icons.IconRegistry;

/**
 * @author Markus Gärtner, Florian Fritze
 *
 */
public class HelpStore implements ComponentListener, ActionListener, WindowListener {

	public HelpStore() {
		glass = new GlassPaneWindow();
		helpDisplay = new HTMLHelpDisplay();
		helpDisplay.readHelpFile();
	}

	private static final Logger log = LoggerFactory.getLogger(HelpStore.class);
	
	/** Stores the anchor ids for all registered components */
	private final Map<JComponent, String> componentAnchors = new WeakHashMap<>();
	
	private final static IconRegistry ir = IconRegistry.getGlobalRegistry();
	
	private Icon helpHint = ir.getIcon("icons8-Help-48-very-small.png");
	
	private int standardWidth = 0;
	
	private Container contentPane;
	
	private boolean helpShowed;
	
	private boolean registered=false;
	
	private GlassPaneWindow glass;
	
	private HTMLHelpDisplay helpDisplay;
	
	private List<JButton> registeredButtons = new ArrayList<>();
	
	private boolean shownHelp;
	

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
		helpShowed=true;
		final int addY = 2;
		int xLocation = 0;
		log.info("Showing global help markers");
		Predicate<Component> isButtonReference = component -> componentAnchors.get(component)
				.equals("replaydh.ui.core.mainPanel.addStep.name");
		Component c = componentAnchors.keySet().stream().filter(isButtonReference).findFirst().get();
		Consumer<Component> receiveStandardWidth = component -> standardWidth = component.getParent().getWidth();
		Predicate<Component> isPanel = component -> component instanceof JPanel && component != null;
		Set<Component> components = new HashSet<>(Arrays.asList(c.getParent().getComponents()));
		components.stream().filter(isPanel).forEach(receiveStandardWidth);
		for(JComponent comp : componentAnchors.keySet()) {
			String areaCategory=componentAnchors.get(comp);
			int yLocation = 0;
			JRootPane root = comp.getRootPane();
			Dimension dim;
			if(root != null) {
				dim = root.getSize();
				contentPane = root.getContentPane();
				glass.setSize(dim);
				if(registered == false) {
					contentPane.addComponentListener(this);
					registered=true;
				}
				JButton buttonLabel = new JButton(helpHint);
				if (areaCategory.indexOf("workflowGraph") != -1 && root.getWidth() > standardWidth) {
					xLocation=(comp.getWidth()/2)+(comp.getX())+2;
					yLocation = comp.getY()+43+addY;
				} else if(areaCategory.indexOf("fileTracker") != -1 && root.getWidth() > standardWidth) {
					xLocation=(comp.getWidth()/2)+(comp.getX())+98;
					yLocation = comp.getY()-10;
				} else {
					xLocation=root.getWidth()+(comp.getWidth()/2)+(comp.getX())-standardWidth+10;
					yLocation = comp.getY()+35;
				}
				buttonLabel.setBounds(xLocation, yLocation, 20, 20);
				buttonLabel.setBorder(null);
				buttonLabel.setToolTipText(ResourceManager.getInstance().get("replaydh.display.help.toolTip"));
				buttonLabel.addActionListener(this);
				buttonLabel.setName(componentAnchors.get(comp));
				registeredButtons.add(buttonLabel);
				glass.add(buttonLabel);
				root.setGlassPane(glass);
				glass.setVisible(true);
				glass.setOpaque(false);
			}
		}
	}

	public void hideHelp() {
		log.info("Hiding global help markers");
		glass.setVisible(false);
		glass.removeAll();
		for(JButton button : registeredButtons) {
			button.removeActionListener(this);
		}
		helpShowed=false;
	}

	@Override
	public void componentResized(ComponentEvent e) {
		if(helpShowed) {
			hideHelp();
			showHelp();
		}
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentShown(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	private class GlassPaneWindow extends JPanel {

		/**
		 * 
		 */
		private static final long serialVersionUID = 6793726559660910536L;

		public GlassPaneWindow() {
			this.setLayout(null);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		for(JButton button : registeredButtons) {
			if(source == button) {
				if(shownHelp == false) {
					JFrame helpFrame = helpDisplay.showHelpSection(button.getName());
					helpFrame.addWindowListener(this);
					helpFrame.setName(button.getName());
					shownHelp=true;
				}
			}
		}
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent e) {
		shownHelp=false;
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub
		
	}
	
	
}

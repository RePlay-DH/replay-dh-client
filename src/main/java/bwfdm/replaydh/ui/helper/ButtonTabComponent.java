/*
 *  ICARUS -  Interactive platform for Corpus Analysis and Research tools, University of Stuttgart
 *  Copyright (C) 2012-2013 Markus Gärtner and Gregor Thiele
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * $Revision: 160 $
 * $Date: 2013-11-07 16:56:45 +0100 (Do, 07 Nov 2013) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus/core/de.ims.icarus/source/de/ims/icarus/ui/tab/ButtonTabComponent.java $
 *
 * $LastChangedDate: 2013-11-07 16:56:45 +0100 (Do, 07 Nov 2013) $
 * $LastChangedRevision: 160 $
 * $LastChangedBy: mcgaerty $
 */
package bwfdm.replaydh.ui.helper;

import static java.util.Objects.requireNonNull;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SingleSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicButtonUI;

import bwfdm.replaydh.resources.ResourceManager;


/**
 * Component to be used as tabComponent; Contains a JLabel to show the text and
 * a JButton to close the tab it belongs to
 *
 * @author Markus Gärtner
 *
 */
public class ButtonTabComponent extends JPanel implements ChangeListener,
		PropertyChangeListener, ContainerListener {

	private static final long serialVersionUID = -5642829173325085218L;
	private final JTabbedPane pane;
	private final TabButton tabButton;

	public ButtonTabComponent(final JTabbedPane pane) {
		// unset default FlowLayout' gaps
		super(new FlowLayout(FlowLayout.LEFT, 0, 0));

		this.pane = requireNonNull(pane);
		setOpaque(false);
		setFocusable(false);

		pane.addPropertyChangeListener("model", this); //$NON-NLS-1$
		pane.addContainerListener(this);
		pane.getModel().addChangeListener(this);

		// make JLabel read titles from JTabbedPane
		JLabel label = new JLabel() {
			private static final long serialVersionUID = -101575414816694292L;

			@Override
			public String getText() {
				int i = pane.indexOfTabComponent(ButtonTabComponent.this);
				return i==-1 ? null : pane.getTitleAt(i);
			}

			@Override
			public Icon getIcon() {
				int i = pane.indexOfTabComponent(ButtonTabComponent.this);
				return i==-1 ? null : pane.getIconAt(i);
			}
		};

		add(label);
		// add more space between the label and the button
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
		// tab button
		tabButton = new TabButton();
		add(tabButton);
		// add more space to the top of the component
		setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
	}

	protected void checkTabButton() {
		int index = pane.indexOfTabComponent(this);
		tabButton.setVisible(pane.getSelectedIndex() == index);
	}

	@Override
	public void componentAdded(ContainerEvent e) {
		// no-op
	}

	@Override
	public void componentRemoved(ContainerEvent e) {
		checkTabButton();
	};

	@Override
	public void stateChanged(ChangeEvent e) {
		checkTabButton();
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		((SingleSelectionModel) evt.getOldValue()).removeChangeListener(this);
		((SingleSelectionModel) evt.getNewValue()).addChangeListener(this);
	}

	private class TabButton extends JButton implements ActionListener, MouseListener {

		private static final long serialVersionUID = 7781685681499233316L;

		public TabButton() {
			int size = 17;
			setPreferredSize(new Dimension(size, size));

			setText(ResourceManager.getInstance().get("labels.close"));
			// Make the button looks the same for all Laf's
			setUI(new BasicButtonUI());
			// Make it transparent
			setContentAreaFilled(false);
			// No need to be focusable
			setFocusable(false);
			setBorder(BorderFactory.createEtchedBorder());
			setBorderPainted(false);
			setRolloverEnabled(true);

			addMouseListener(this);
			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			int i = pane.indexOfTabComponent(ButtonTabComponent.this);
			if (i != -1) {
				Component comp = pane.getComponentAt(i);

				TabController tabController = (TabController) SwingUtilities.getAncestorOfClass(
						TabController.class, comp);

				if(tabController!=null) {
					// Let tab controller handle it
					tabController.closeTab(i);
				} else {

					// Just remove
					pane.remove(i);
				}
			}
		}

		// We don't want to update UI for this button
		@Override
		public void updateUI() {
			// no-op
		}

		// Paint the cross
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g.create();
			// shift the image for pressed buttons
			if (getModel().isPressed()) {
				g2.translate(1, 1);
			}
			g2.setStroke(new BasicStroke(2));
			g2.setColor(Color.BLACK);
			if (getModel().isRollover()) {
				g2.setColor(Color.MAGENTA);
			}
			int delta = 6;
			g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight()
					- delta - 1);
			g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight()
					- delta - 1);
			g2.dispose();
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			// not needed
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			setBorderPainted(true);
		}

		@Override
		public void mouseExited(MouseEvent e) {
			setBorderPainted(false);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// not needed
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// not needed
		}
	}
}

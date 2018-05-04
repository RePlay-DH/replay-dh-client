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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

import org.java.plugin.registry.PluginDescriptor;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Forms;
import com.jgoodies.forms.factories.Paddings;

import bwfdm.replaydh.core.AppProperty;
import bwfdm.replaydh.core.RDHClient;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;

/**
 * @author Markus Gärtner
 *
 */
public class AboutDialog extends JDialog {

	private static final long serialVersionUID = -5298221959344314967L;

	public static void showDialog(Window owner) {
		GuiUtils.checkEDT();
		AboutDialog dialog = new AboutDialog(owner);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}

	private static boolean isBrowsingSupported() {
		if (!Desktop.isDesktopSupported()) {
			return false;
		}
		boolean result = false;
		Desktop desktop = Desktop.getDesktop();
		if (desktop.isSupported(Desktop.Action.BROWSE)) {
			result = true;
		}
		return result;

	}

	private static final Object LINK_KEY = new Object();

	private static void linkify(JLabel label) {
		String s = label.getText();
		label.putClientProperty(LINK_KEY, s);
		label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		s = "<html><a href=\""+s+"\">"+s+"</a></html>";

		label.setText(s);

		label.addMouseListener(new LinkMouseListener());
	}

	private static class LinkMouseListener extends MouseAdapter {

		@Override
		public void mouseClicked(MouseEvent evt) {
			JLabel l = (JLabel) evt.getSource();
			String link = (String)l.getClientProperty(LINK_KEY);
			try {
				Desktop desktop = Desktop.getDesktop();
				URI uri = new URI(link);
				desktop.browse(uri);
			} catch (URISyntaxException use) {
				throw new AssertionError(use);
			} catch (IOException ioe) {
				GuiUtils.showErrorDialog(l, ioe);
			}
		}
	}

	private AboutDialog(Window owner) {
		super(owner);

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		GuiUtils.decorateWindow(this);

		final RDHClient client = RDHClient.client();

		final ResourceManager rm = ResourceManager.getInstance();
		setTitle(rm.get("replaydh.app.about"));
		setModalityType(DEFAULT_MODALITY_TYPE);


		Image image;
		try {
			image = ImageIO.read(AboutDialog.class.getResource("/bwfdm/replaydh/ui/core/Splash_footer.png"));
		} catch (IOException e) {
			throw new InternalError("Unable to load splash footer image for about dialog", e);
		}
		final JLabel lIcon = new JLabel(new ImageIcon(image));
		lIcon.setBorder(BorderFactory.createLineBorder(new Color(41, 79, 157)));
		lIcon.setPreferredSize(new Dimension(image.getWidth(null)+2, image.getHeight(null)+2));

		final JLabel lContact = new JLabel(client.getAppInfo(AppProperty.CONTACT));
		final JLabel lUrl = new JLabel(client.getAppInfo(AppProperty.URL));

		if(isBrowsingSupported()) {
			linkify(lUrl);
			linkify(lContact);
		}

		final JButton bClose = new JButton(rm.get("replaydh.labels.close"));
		bClose.addActionListener(ae -> {
			AboutDialog.this.setVisible(false);
			AboutDialog.this.dispose();
		});

		final JButton bShowLicense = new JButton(rm.get("replaydh.app.about.showLicense"));
		bShowLicense.addActionListener(ae -> AboutDialog.this.showLicense(client.getLicense()));
		bShowLicense.setFocusable(false);

		Vector<String> plugins = new Vector<>();
		for(PluginDescriptor descriptor : client.getPluginEngine().getPluginRegistry().getPluginDescriptors()) {
			plugins.add(descriptor.getId()+" ("+descriptor.getVersion()+")");
		}
		Collections.sort(plugins);

		final JList<String> pluginList = new JList<>(plugins);
		pluginList.setFocusable(false);
		pluginList.setVisibleRowCount(8);

		/**
		 * <pre>
		 * +----------------------------------+
		 * |                                  |
		 * |            LOGO                  |
		 * +----------------------------------+
		 * | Version:   xxxxxxx               |
		 * | Authors:   xxxxxxxx              |
		 * | Contact:   <link>                |
		 * | Visit:     <link>                |
 		 * +----------------------------------+
		 * | Plugins:   PLUGINS               |
		 * |                                  |
		 * |            LICENSE BUTTON        |
		 * +----------------------------------+
		 * |               CLOSE              |
		 * +----------------------------------+
		 * </pre>
		 */

		JPanel panel = FormBuilder.create()
				.columns("pref, 15dlu, pref:grow:fill")
				.rows("pref, 6dlu, pref, 6dlu, " // header rows 1 to 3
						+ "pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, 6dlu, " // content rows 5 to 13
						+ "max(pref;30dlu), $nlg, pref, 6dlu, pref, 6dlu, " // plugins rows 15 to 19
						+ "pref") // footer with close button row 19
				// header
				.add(lIcon).xyw(1, 1, 3)
				.add(new JSeparator(SwingConstants.HORIZONTAL)).xyw(1, 3, 3)
				// content
				.addLabel(rm.get("replaydh.app.about.version")+":").xy(1, 5)
					.addLabel(client.getAppInfo(AppProperty.VERSION)).xy(3, 5)
				.addLabel(rm.get("replaydh.app.about.authors")+":").xy(1, 7)
					.addLabel(client.getAppInfo(AppProperty.AUTHORS)).xy(3, 7)
				.addLabel(rm.get("replaydh.app.about.contact")+":").xy(1, 9)
					.add(lContact).xy(3, 9)
				.addLabel(rm.get("replaydh.app.about.url")+":").xy(1, 11)
					.add(lUrl).xy(3, 11)

				.add(new JSeparator(SwingConstants.HORIZONTAL)).xyw(1, 13, 3)
				// plugins + license
				.addLabel(rm.get("replaydh.app.about.plugins")+":").xy(1, 15, "left, top")
					.addScrolled(pluginList).xy(3, 15)
				.add(bShowLicense).xy(3, 17, "left, center")
				.add(new JSeparator(SwingConstants.HORIZONTAL)).xyw(1, 19, 3)
				// footer
				.add(bClose).xyw(1, 21, 3, "center, center")
				.padding(Paddings.DIALOG)
				.build();

		getContentPane().add(panel);

		pack();
		setResizable(false);
	}

	private void showLicense(String license) {

		ResourceManager rm = ResourceManager.getInstance();

		JTextArea ta = new JTextArea(license, 25, 70);
		ta.setEditable(false);
		ta.setLineWrap(true);
		ta.setWrapStyleWord(true);

		JScrollPane jsp = new JScrollPane(ta);
		jsp.setMinimumSize(new Dimension (600,400));
		jsp.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		GuiUtils.defaultSetUnitIncrement(jsp);

		JButton bClose = new JButton(rm.get("replaydh.labels.close"));

		JPanel panel = FormBuilder.create()
				.columns("pref")
				.rows("pref:grow:fill, 6dlu, pref, 6dlu, pref")
				.add(jsp).xy(1, 1, "fill, fill")
				.add(new JSeparator(SwingConstants.HORIZONTAL)).xy(1, 3, "fill, center")
				.add(Forms.buttonBar(bClose)).xy(1, 5, "center, center")
				.padding(Paddings.DIALOG)
				.build();

		final JDialog dialog = new JDialog(getOwner());
		dialog.setTitle(rm.get("replaydh.app.about.license"));
		dialog.setModalityType(DEFAULT_MODALITY_TYPE);
		dialog.getContentPane().add(panel);
		dialog.pack();
		dialog.setLocationRelativeTo(null);
		dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		GuiUtils.decorateWindow(dialog);

		bClose.addActionListener(ae -> {
			dialog.setVisible(false);
			dialog.dispose();
		});

		dialog.setVisible(true);
	}
}

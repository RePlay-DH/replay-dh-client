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
package bwfdm.replaydh.ui;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static java.util.Objects.requireNonNull;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

import org.java.plugin.registry.Extension;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;

import bwfdm.replaydh.core.PluginEngine;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.helper.Editor;
import bwfdm.replaydh.ui.helper.EditorControl;
import bwfdm.replaydh.ui.icons.Resolution;
import bwfdm.replaydh.utils.xml.HtmlUtils;

/**
 * @author Markus Gärtner
 *
 */
public class GuiUtils {

	private static final List<Image> defaultRDHIcons = new ArrayList<>();
	public static final Image APP_ICON_64;
	public static final Image APP_ICON_32;
	public static final Image APP_ICON_18;
	static {
		//TODO load default icons from package into list
		try {
			APP_ICON_64 = ImageIO.read(GuiUtils.class.getResource(
					"/bwfdm/replaydh/ui/core/replay-icon-64.png"));
			APP_ICON_32 = ImageIO.read(GuiUtils.class.getResource(
					"/bwfdm/replaydh/ui/core/replay-icon-32.png"));
			APP_ICON_18 = ImageIO.read(GuiUtils.class.getResource(
					"/bwfdm/replaydh/ui/core/replay-icon-18.png"));

			defaultRDHIcons.add(APP_ICON_64);
			defaultRDHIcons.add(APP_ICON_32);
			defaultRDHIcons.add(APP_ICON_18);
		} catch (IOException e) {
			throw new InternalError("Failed to access default icon resources", e);
		}
	}

	public static void decorateWindow(Window window) {

		window.setIconImages(defaultRDHIcons);
	}


	/**
	 * Sets the default icons to be used for the given {@code frame};
	 * @param frame
	 */
	public static void decorateFrame(JFrame frame) {

		frame.setIconImages(defaultRDHIcons);

		frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE); //TODO really needed that way?
	}

	/**
	 * Makes sure that the given task will be executed on the
	 * event dispatch thread (EDT). If the current thread is the EDT
	 * the given task is executed immediately. Otherwise it will
	 * be {@link SwingUtilities#invokeLater(Runnable) invoked later}
	 * on the EDT.
	 * <p>
	 * Note that this method is <b>not</b> suitable for executing
	 * code that is meant to be scheduled <b>after</b> the current
	 * queue of events on the event dispatch thread is processed.
	 * The provided {@code taks} will be executed immediately when
	 * called on the event dispatch thread.
	 *
	 * @param task
	 */
	public static void invokeEDT(Runnable task) {
		if(SwingUtilities.isEventDispatchThread()) {
			task.run();
		} else {
			invokeEDTLater(task);
		}
	}
	public static void invokeEDTLater(Runnable task) {
		SwingUtilities.invokeLater(task);
	}

	public static void invokeEDTAndWait(Runnable task) {
		if(SwingUtilities.isEventDispatchThread()) {
			task.run();
		} else {
			try {
				SwingUtilities.invokeAndWait(task);
			} catch (InvocationTargetException | InterruptedException e) {
				Throwable t = e;
				if(t instanceof InvocationTargetException) {
					t = e.getCause();
				}
				throw new RDHException("Failed to switch execution to EDT", t);
			}
		}
	}

	/**
	 * Throws an {@link RDHException} in case the current thread is <b>not</b>
	 * the <i>event-dispatch-thread</i>
	 */
	public static void checkEDT() {
		if(!SwingUtilities.isEventDispatchThread())
			throw new RDHException("Illegal thread context - Event Dispatch Thread required!");
	}

	/**
	 * Throws an {@link RDHException} in case the current thread <b>is</b>
	 * the <i>event-dispatch-thread</i>
	 */
	public static void checkNotEDT() {
		if(SwingUtilities.isEventDispatchThread())
			throw new RDHException("Illegal thread context - Event Dispatch Thread not allowed!");
	}

	/**
	 * Used to forward locale changes to UI components that
	 * make use of this information.
	 *
	 * @param newLocale
	 */
	public static void updateLocaleSensitiveUI(Locale newLocale) {
		JComponent.setDefaultLocale(newLocale);
	}

	/**
	 *
	 * @param root The component to start with, must be a container
	 * @param visitor the action to be executed for every component in the tree.
	 * Return value defines if further tree traversal should continue.
	 */
	public static void walkGUI(Container root, Function<? super Component, GuiVisitResult> visitor) {
		requireNonNull(root);
		requireNonNull(visitor);

		Stack<Component> pending = new Stack<>();
		pending.push(root);

		while(!pending.isEmpty()) {
			Component comp = pending.pop();

			// Apply action
			GuiVisitResult result = visitor.apply(comp);

			// If requested, stop
			if(result==GuiVisitResult.STOP_TRAVERSAL) {
				break;
			}

			// Add all children
			if(result!=GuiVisitResult.IGNORE_CHILDREN && comp instanceof Container) {
				Container container = (Container) comp;
				synchronized (container.getTreeLock()) {
					int count = container.getComponentCount();
					for(int i=0; i<count; i++) {
						pending.add(container.getComponent(i));
					}
				}
			}
		}
	}

	public enum GuiVisitResult {
		STOP_TRAVERSAL,
		IGNORE_CHILDREN,
		CONTINUE,
		;
	}

	private static volatile Map<String, Icon> blankIcons;

	public static Icon getBlankIcon(int width, int height) {
		String key = width+"x"+height; //$NON-NLS-1$
		if(blankIcons==null) {
			blankIcons = new HashMap<>();
		}

		// No need for synchronization here, since we're not concerned about duplicates
		Icon icon = blankIcons.get(key);
		if(icon==null) {
			icon = createBlankIcon(width, height);
			blankIcons.put(key, icon);
		}
		return icon;
	}

	public static Icon createBlankIcon(final int width, final int height) {
		return new Icon() {

			@Override
			public void paintIcon(Component c, Graphics g, int x, int y) {
				// no-op
			}

			@Override
			public int getIconWidth() {
				return width;
			}

			@Override
			public int getIconHeight() {
				return height;
			}
		};
	}

	private static String tryExpandResource(String s) {
		String original = s;
		if(s.startsWith("replaydh.")) {
			s = ResourceManager.getInstance().get(s);
		}

		if(s==null) {
			s = original;
		}

		return s;
	}

	public static void showDefaultInfo(Component parent, String message) {
		List<String> options = new ArrayList<>();
		options.add("Soon™");
		if(message!=null) {
			options.add(message);
		}

		JOptionPane.showOptionDialog(parent, "Soon™", "Message", JOptionPane.DEFAULT_OPTION,
				JOptionPane.INFORMATION_MESSAGE, null, new Object[]{"Ok"}, null);
	}

	public static void showInfo(Component parent, String message) {
		showDialog(parent, "replaydh.labels.message", message, JOptionPane.INFORMATION_MESSAGE);
	}

	public static void showWarning(Component parent, String message) {
		showDialog(parent, "replaydh.labels.warning", message, JOptionPane.WARNING_MESSAGE);
	}

	public static void showError(Component parent, String message) {
		showDialog(parent, "replaydh.labels.error", message, JOptionPane.ERROR_MESSAGE);
	}

	public static void showInfo(Component parent, String title, String message) {
		showDialog(parent, title, message, JOptionPane.INFORMATION_MESSAGE);
	}

	public static void showWarning(Component parent, String title, String message) {
		showDialog(parent, title, message, JOptionPane.WARNING_MESSAGE);
	}

	public static void showError(Component parent, String title, String message) {
		showDialog(parent, title, message, JOptionPane.ERROR_MESSAGE);
	}

	private static final Matcher NEWLINE_REGEX = Pattern.compile("[\\r\\n]+").matcher("");

	private static void showDialog(Component parent, String title, String message, int messageType) {
		title = tryExpandResource(title);
		message = tryExpandResource(message);

		Object messageObject = message;

		synchronized (NEWLINE_REGEX) {
			if(NEWLINE_REGEX.reset(message).find()) {
				JTextArea textArea = createTextArea(message);

				//FIXME at some point change this for a better solutions in preserving minimal size of the message component
				textArea.setLineWrap(false);
				textArea.setMinimumSize(textArea.getPreferredSize());
				// END

				messageObject = textArea;
			}
		}

		JOptionPane.showOptionDialog(parent, messageObject,
				title, JOptionPane.DEFAULT_OPTION, messageType, null,
				new Object[]{ResourceManager.getInstance().get("replaydh.labels.ok")}, null);
	}

	public static void showErrorDialog(Component parent, Throwable t) {
		showErrorDialog(parent, null, null, t);
	}

	public static void showErrorDialog(Component parent, String title, String message, Throwable t) {

		if(title==null) {
			title = "replaydh.dialogs.guiError.title";
		}
		if(message==null) {
			message = "replaydh.dialogs.guiError.message";
		}

		title = tryExpandResource(title);
		message = tryExpandResource(message);

		/*
		 *  +-----------------------------------------+
		 *  |                  TITLE                  |
		 *  +-----------------------------------------+
		 *  |                                         |
		 *  |   ICON          MESSAGE                 |
		 *  |                                         |
		 *  +-----------------------------------------+
		 *  |                                         |
		 *  |                                         |
		 *  |                                         |
		 *  |                 TEXTAREA                |
		 *  |                                         |
		 *  |                                         |
		 *  |                                         |
		 *  +-----------------------------------------+
		 *  |                  <OK>                   |
		 *  +-----------------------------------------+
		 */

		JLabel infoLabel = (JLabel) createInfoComponent(message, true, UIManager.getIcon("OptionPane.errorIcon"));
		infoLabel.setVerticalTextPosition(SwingConstants.CENTER);
		infoLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
		infoLabel.setIconTextGap(30);

		FormBuilder builder = FormBuilder.create()
				.columns("left:pref:grow, 4dlu, pref, 4dlu, center:pref:grow")
				.rows("pref")
				.padding(Paddings.DIALOG)
				.add(infoLabel).xyw(1, 1, 5);

		if(t!=null) {
			JTextArea textArea = new JTextArea();
			textArea.setEditable(false);
			textArea.setFont(UIManager.getFont("Label.font")); //$NON-NLS-1$
			textArea.setForeground(UIManager.getColor("Label.foreground")); //$NON-NLS-1$
			textArea.setBackground(UIManager.getColor("Label.background")); //$NON-NLS-1$
			textArea.setBorder(defaultContentBorder);
//			textArea.setLineWrap(true);
//			textArea.setWrapStyleWord(true);

			StringWriter sw = new StringWriter(250);
			try(PrintWriter pw = new PrintWriter(sw)) {
				t.printStackTrace(pw);
			}
			textArea.setText(sw.toString());

			JScrollPane scrollPane = new JScrollPane(textArea);
			scrollPane.setPreferredSize(new Dimension(400, 300));
			scrollPane.setBorder(defaultAreaBorder);

			builder.appendRows("$nlg, fill:pref:grow").add(scrollPane).xyw(1, 3, 5);
		}

		Frame owner = getFrame(parent);
		JDialog dialog = new JDialog(owner, title, true);

		JButton okButton = new JButton(ResourceManager.getInstance().get("replaydh.labels.ok"));
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dialog.setVisible(false);
			}
		});
		builder.appendRows("$nlg, pref")
			.add(okButton).xy(3, t!=null ? 5 : 3);

		dialog.add(builder.build());
		dialog.setModal(true);
		dialog.setResizable(t!=null);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.pack();
		dialog.setLocationRelativeTo(parent);

		dialog.setVisible(true);
		dialog.dispose();
	}

	public static Frame getFrame(Component comp) {
		return (Frame) SwingUtilities.getAncestorOfClass(Frame.class, comp);
	}

	public static <E extends Object> boolean showEditorDialogCompact(Editor<E> editor) {

		int result = JOptionPane.showOptionDialog(null, null, "Test",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
				new Object[]{editor.getEditorComponent()}, null);

		boolean apply = result==JOptionPane.OK_OPTION;

		if(apply) {
			editor.applyEdit();
		} else {
			editor.resetEdit();
		}

		return apply;
	}

	public static <E extends Object> boolean showEditorDialogWithControl(Editor<E> editor) {
		return showEditorDialogWithControl(null, editor, false);
	}

	public static <E extends Object> boolean showEditorDialogWithControl(Frame frame, Editor<E> editor, boolean modal) {
		return showEditorDialogWithControl(frame, editor,  null, modal);
	}

	public static <E extends Object> boolean showEditorDialogWithControl(Frame frame, Editor<E> editor, String title, boolean modal) {
		if(title==null) {
			title = "Test Dialog";
		}

		title = tryExpandResource(title);

		ResourceManager rm = ResourceManager.getInstance();

		final JButton okButton = new JButton(rm.get("replaydh.labels.ok"));
		final JButton cancelButton = new JButton(rm.get("replaydh.labels.cancel"));

//		final JPanel mainPanel = FormBuilder.create()
//				.columns("5dlu:grow, pref, 5dlu, pref, 5dlu:grow")
//				.rows("pref, 8dlu, pref, 5dlu")
//				.padding(Paddings.DIALOG)
//				.add(editor.getEditorComponent()).xyw(1, 1, 5)
//				.add(okButton).xy(2, 3)
//				.add(cancelButton).xy(4, 3)
//				.build();

		final JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(editor.getEditorComponent(), BorderLayout.CENTER);
		mainPanel.add(ButtonBarBuilder.create()
				.addGlue()
				.addButton(okButton, cancelButton)
				.addGlue()
				.build(), BorderLayout.SOUTH);
		mainPanel.setBorder(Paddings.DIALOG);

		final JDialog dialog = new JDialog(frame, modal);
		dialog.setLayout(new BorderLayout());
		dialog.add(mainPanel, BorderLayout.CENTER);
		dialog.setTitle(title);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.pack();
		dialog.setLocationRelativeTo(frame);

		// TODO: think about resizing of the dialog.
		//dialog.setMinimumSize(new Dimension(0, dialog.getPreferredSize().height));

		final AtomicBoolean result = new AtomicBoolean(false);

		final ActionListener actionListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Object source = e.getSource();

				if(source==okButton) {
					result.set(editor.hasChanges() || true); //FIXME for debug reasons we ignore change state
					editor.applyEdit();
				} else if(source==cancelButton) {
					editor.resetEdit();
				}
				dialog.setVisible(false);
			}
		};
		okButton.addActionListener(actionListener);
		cancelButton.addActionListener(actionListener);

		// Allow the editor control over our buttons
		final EditorControl control = new EditorControl() {

			@Override
			public void setResetEnabled(boolean enabled) {
//				cancelButton.setEnabled(enabled);
			}

			@Override
			public void setApplyEnabled(boolean enabled) {
				okButton.setEnabled(enabled);
			}

			@Override
			public boolean isResetEnabled() {
//				return cancelButton.isEnabled();
				return true;
			}

			@Override
			public boolean isApplyEnabled() {
				return okButton.isEnabled();
			}
		};

		editor.setControl(control);

		dialog.setVisible(true);

		//editor.setControl(null);

		return result.get();
	}

	public static <E extends Object> boolean showEditorDialogWithFullControl(Frame frame, Editor<E> editor, String title, boolean modal) {
		if(title==null) {
			title = "Test Dialog";
		}

		title = tryExpandResource(title);

		ResourceManager rm = ResourceManager.getInstance();

		final JButton applyButton = new JButton(rm.get("replaydh.labels.apply"));
		final JButton resetButton = new JButton(rm.get("replaydh.labels.reset"));
		final JButton closeButton = new JButton(rm.get("replaydh.labels.close"));
//		final JButton cancelButton = new JButton(rm.get("replaydh.labels.cancel"));

		final JPanel mainPanel = FormBuilder.create()
				.columns("5dlu:grow, pref, 5dlu, pref, 5dlu, pref, 5dlu:grow")
				.rows("pref, 8dlu, pref, 5dlu")
				.padding(Paddings.DIALOG)
				.add(editor.getEditorComponent()).xyw(1, 1, 7)
				.add(applyButton).xy(2, 3)
				.add(resetButton).xy(4, 3)
				.add(closeButton).xy(6, 3)
				.build();

		final JDialog dialog = new JDialog(frame, modal);
		dialog.setLayout(new BorderLayout());
		dialog.add(mainPanel, BorderLayout.CENTER);
		dialog.setTitle(title);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.pack();
		dialog.setLocationRelativeTo(frame);

		// TODO: think about resizing of the dialog.
		//dialog.setMinimumSize(new Dimension(0, dialog.getPreferredSize().height));

		final AtomicBoolean changesApplied = new AtomicBoolean(false);

		final ActionListener actionListener = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Object source = e.getSource();

				if(source==applyButton) {
					changesApplied.set(editor.hasChanges());
					editor.applyEdit();
				} else if(source==resetButton) {
					editor.resetEdit();
				} else if(source==closeButton) {
					dialog.setVisible(false);
				}
			}
		};
		applyButton.addActionListener(actionListener);
		resetButton.addActionListener(actionListener);
		closeButton.addActionListener(actionListener);

		// Allow the editor control over our buttons
		final EditorControl control = new EditorControl() {

			@Override
			public void setResetEnabled(boolean enabled) {
				resetButton.setEnabled(enabled);
			}

			@Override
			public void setApplyEnabled(boolean enabled) {
				applyButton.setEnabled(enabled);
			}

			@Override
			public boolean isResetEnabled() {
				return resetButton.isEnabled();
			}

			@Override
			public boolean isApplyEnabled() {
				return applyButton.isEnabled();
			}
		};

		editor.setControl(control);

		dialog.setVisible(true);

		//editor.setControl(null);

		return changesApplied.get();
	}

    /**
     * Create a basic multi row panel in the format "label -- text field"
     * based on the FormBuilder from JGoodies framework
     * @param labels
     * @param textFields
     * @return
     */
    public static Component createMultiRowPanel(List<JLabel> labels, List<JTextField> textFields){

            //Compare size of the labels and text fields arrays
            if(labels.size() != textFields.size()){
                throw new RuntimeException("Not identical size of the labels and text fields arrays.");
            }

            FormBuilder builder = FormBuilder.create()
                            .columns("pref, 5dlu, pref")
                            .rows("pref");

            builder.padding(new EmptyBorder(10, 10, 10, 10)); //set border-padding
            for(int i=0; i < labels.size(); i++){
                    builder.add(labels.get(i))      .xy(1, (i*2)+1);
                    builder.add(textFields.get(i))  .xy(3, (i*2)+1);
                    if(i < labels.size()-1) {
                        builder.appendRows("$lg, pref");
                    }
            }

            return builder.build(); //"JPanel" type
    }

    /**
     * Show a simple frame with some "component" inside (e.g. JPanel)
     * @param content
     */
    public static void showFrame(Component content) {
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Exit on Close
            frame.add(content);
            frame.pack();
            frame.setVisible(true);
    }

	private static final String HTML_TAG = "<html>";

	public static String toUnwrappedSwingTooltip(String tooltip) {
		return toUnwrappedSwingTooltip(tooltip, true);
	}

	public static String toUnwrappedSwingTooltip(String tooltip, boolean prependHTML) {
		if(tooltip==null || tooltip.isEmpty()) {
			return null;
		}
		if(tooltip.startsWith(HTML_TAG)) {
			return tooltip;
		}

		String convertedTooltip = HtmlUtils.escapeHTML(tooltip);
		convertedTooltip = convertedTooltip.replaceAll(
				"\\n\\r|\\r\\n|\\n|\\r", "<br>");
		if(prependHTML && convertedTooltip.length()!=tooltip.length()) {
			tooltip = "<html>"+convertedTooltip;
		}

		return tooltip;
	}

    public static Component createInfoComponent(String text, boolean center, Icon icon) {
    	JLabel label = new JLabel();

    	label.setText(toUnwrappedSwingTooltip(text));

    	label.setFont(label.getFont().deriveFont(14f));

    	if(icon!=null) {
    		label.setIcon(icon);
    		label.setVerticalTextPosition(SwingConstants.BOTTOM);
    		label.setHorizontalTextPosition(SwingConstants.CENTER);
    	}

    	if(center) {
    		label.setHorizontalAlignment(SwingConstants.CENTER);
    		label.setVerticalAlignment(SwingConstants.CENTER);
    	}

    	return label;
    }

    /**
     * Create a list of textFields, which has the same size as a list of labels
     * @param labelsList
     * @param textFieldSize
     * @return a list of textFields
     */
    public static List<JTextField> createTextFieldsListForLabels(List<JLabel> labelsList, int textFieldSize){

        List<JTextField> textFieldsList = new ArrayList<JTextField>();
        for(JLabel lab : labelsList){
                textFieldsList.add(new JTextField(textFieldSize));
        }
        return textFieldsList;
    }

    public static Component createInfoDisplay(String message, Throwable t, boolean displayFullStack) {
    	checkArgument("Must provide either a message or exception with a valid message",
    			message!=null || (t!=null && t.getMessage()!=null));

    	//TODO create textarea, scrollpane, etc (for full stack trace with nested exceptions use list?)

    	throw new UnsupportedOperationException("needs implementation");
    }

    public static void beep() {
		try {
			Toolkit.getDefaultToolkit().beep();
		} catch(Exception e) {
			// ignore
		}
    }

    public static <C extends JTextComponent> C autoSelectFullContent(C comp) {
    	comp.addFocusListener(new FocusAdapter() {
    		@Override
    		public void focusGained(FocusEvent e) {
    			SwingUtilities.invokeLater(comp::selectAll);
    		}
		});
    	return comp;
    }

    public static final String EXTENSION_KEY = "extension";

    public static Extension resolveExtension(JComponent component, PluginEngine pluginEngine) {
    	String uid = (String) component.getClientProperty(EXTENSION_KEY);
    	checkArgument("Component is not linked to an extension", uid!=null);

    	return pluginEngine.getExtension(uid);
    }

    /**
     * Translates the given {@code extensions} into {@link JMenuItem menu items},
     * using the localization information from the {@link Extension} objects and
     * also storing the {@link Extension#getUniqueId() unique ids} of those extensions
     * as {@link JComponent#putClientProperty(Object, Object) client property} for
     * later {@link #resolveExtension(JComponent, PluginEngine) resolution}.
     *
     *
     * @param engine
     * @param extensions
     * @param actionListener
     * @return
     */
    public static JMenuItem[] toMenuItems(PluginEngine engine, List<Extension> extensions, ActionListener actionListener) {
    	ResourceManager rm = ResourceManager.getInstance();

    	final JMenuItem[] result = new JMenuItem[extensions.size()];

    	for(int i=0; i<result.length; i++) {
    		Extension extension = extensions.get(i);

    		String name = PluginEngine.getParam(extension, PluginEngine.PARAM_NAME, null);
    		if(name!=null) {
    			name = rm.get(name);
    		}

    		String description = PluginEngine.getParam(extension, PluginEngine.PARAM_DESCRIPTION, null);
    		if(description!=null) {
    			description = rm.get(description);
    		}

    		String iconKey = PluginEngine.getParam(extension, PluginEngine.PARAM_ICON, null);
    		Icon icon = null;
    		if(iconKey!=null) {
    			icon = engine.getIconRegistry(extension.getDeclaringPluginDescriptor())
    					.getIcon(iconKey, Resolution.forSize(16));
    		}

    		JMenuItem item = new JMenuItem(name);
    		item.setToolTipText(description);
    		item.setIcon(icon);

    		item.putClientProperty(EXTENSION_KEY, extension.getUniqueId());
    		item.addActionListener(actionListener);

    		result[i] = item;
    	}

    	return result;
    }

    public static final String DEFAULT_BORDER_PROPERTY = "defaultBorder";
    public static final String ERROR_BORDER_PROPERTY = "errorBorder";

    private static final Border defaultErrorBorder = BorderFactory.createLineBorder(Color.red);

    public static void prepareChangeableBorder(JComponent comp) {
    	Border defaultBorder = comp.getBorder();
    	if(defaultBorder==null) {
    		defaultBorder = BorderFactory.createEmptyBorder();
    	}

    	Border errorBorder = BorderFactory.createCompoundBorder(defaultErrorBorder, defaultBorder);

    	prepareChangeableBorder(comp, defaultBorder, errorBorder);
    }

    public static void prepareChangeableBorder(JComponent comp, Border defaultBorder, Border errorBorder) {
    	comp.putClientProperty(DEFAULT_BORDER_PROPERTY, defaultBorder);
    	comp.putClientProperty(ERROR_BORDER_PROPERTY, errorBorder);
    }

    public static void toggleChangeableBorder(JComponent comp, boolean isError) {
    	Border border = null;

    	if(isError) {
    		border = (Border) comp.getClientProperty(ERROR_BORDER_PROPERTY);
    	} else {
    		border = (Border) comp.getClientProperty(DEFAULT_BORDER_PROPERTY);
    	}

    	comp.setBorder(border);
    }

	private static class UndecoratedTabbedPaneUI extends BasicTabbedPaneUI {
		@Override
		protected void installDefaults() {
			super.installDefaults();
			contentBorderInsets = new Insets(0, 0, 0, 0);
			tabAreaInsets = new Insets(2, 2, 2, 2);
		}
	}

	public static void defaultHideTabbedPaneDecoration(JTabbedPane tabbedPane) {
		tabbedPane.setUI(new UndecoratedTabbedPaneUI());
		tabbedPane.setBorder(defaultAreaBorder);
		// Prevent focus border on tabs being drawn
		tabbedPane.setFocusable(false);
	}

	private static class UndecoratedSplitPaneUI extends BasicSplitPaneUI {
		@Override
		public BasicSplitPaneDivider createDefaultDivider() {
			return new BasicSplitPaneDivider(this) {

				private static final long serialVersionUID = -9149206851193508390L;

				@Override
				public void setBorder(Border b) {
					// Undecorated split pane should not draw a
					// divider border
				}
			};
		}
	}

	public static void defaultHideSplitPaneDecoration(JSplitPane splitPane) {
		splitPane.setUI(new UndecoratedSplitPaneUI());
		splitPane.setDividerSize(4);
		splitPane.setBorder(null);
	}

	public static void disableHtml(Object item) {
		if(item instanceof JComponent) {
			JComponent comp = (JComponent) item;
			comp.putClientProperty("html.disable", Boolean.TRUE); //$NON-NLS-1$
		}
	}

	public static void disableCaretScroll(JTextComponent comp) {
		Caret caret = comp.getCaret();
		if(caret instanceof DefaultCaret) {
			((DefaultCaret) caret).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		}
	}

	public static final int DEFAULT_SCROLL_UNIT_INCREMENT = 16;

	public static final void defaultSetUnitIncrement(Object obj) {
		if(obj instanceof JScrollPane) {
			((JScrollPane)obj).getHorizontalScrollBar().setUnitIncrement(DEFAULT_SCROLL_UNIT_INCREMENT);
			((JScrollPane)obj).getVerticalScrollBar().setUnitIncrement(DEFAULT_SCROLL_UNIT_INCREMENT);
		} else if(obj instanceof JScrollBar) {
			((JScrollBar)obj).setUnitIncrement(DEFAULT_SCROLL_UNIT_INCREMENT);
		}
	}

    private static final JLabel dummyLabel = new JLabel();

    public static JTextArea createTextArea(String text) {
    	JTextArea textArea = new JTextArea();
    	textArea.setFont(dummyLabel.getFont().deriveFont(Font.PLAIN, 12));
    	textArea.setForeground(dummyLabel.getForeground());
    	textArea.setBackground(dummyLabel.getBackground());
    	textArea.setWrapStyleWord(true);
    	textArea.setLineWrap(true);
    	textArea.setEditable(false);
		textArea.setFocusable(false);
		textArea.setText(text);
		textArea.setBorder(null);
		return textArea;
    }

    private static DateTimeFormatter dateTimeFormatter;
    private static DateTimeFormatter timeFormatter;

    public static DateTimeFormatter getDateTimeFormatter() {
    	if(dateTimeFormatter==null) {
    		dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    	}
    	return dateTimeFormatter;
    }

	public static DateTimeFormatter getTimeFormatter() {
    	if(timeFormatter==null) {
    		timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT);
    	}
		return timeFormatter;
	}


	public static final Font defaultLargeInfoFont = UIManager.getFont("Label.font").deriveFont(14f);
	public static final Font defaultSmallInfoFont = UIManager.getFont("Label.font").deriveFont(12f);

	public static final Color defaultBorderColor = new Color(128, 128, 128);

	/**
	 * Rounded line border in gray
	 */
	public static final Border defaultAreaBorder = BorderFactory.createLineBorder(defaultBorderColor, 1, true);

	/**
	 * Lined border in gray
	 */
	public static final Border defaultBoxBorder = BorderFactory.createLineBorder(defaultBorderColor, 1);

	public static final Border topLineBorder = new SeparatingBorder(true, false, false, false);
	public static final Border bottomLineBorder = new SeparatingBorder(false, false, true, false);
	public static final Border rightLineBorder = new SeparatingBorder(false, true, false, false);
	public static final Border leftLineBorder = new SeparatingBorder(false, false, false, true);

	/**
	 * Empty border
	 */
	public static final Border emptyBorder = new EmptyBorder(0, 0, 0, 0);

	/**
	 * Empty border (1, 3, 1, 3)
	 */
	public static final Border defaultContentBorder = new EmptyBorder(1, 3, 1, 3);

	public static class SeparatingBorder implements Border {

		private final Insets insets;

		public SeparatingBorder(boolean top, boolean right, boolean bottom, boolean left) {
			insets = new Insets(
					top ? 1 : 0, left ? 1 : 0,
					bottom ? 1 : 0, right ? 1 :0);
		}

		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width,
				int height) {
			g.setColor(defaultBorderColor);

			if(insets.top>0)
				g.drawLine(x+1, y, x+width-2, y);
			if(insets.right>0)
				g.drawLine(x+width-1, y+1, x+width-1, y+height-2);
			if(insets.left>0)
				g.drawLine(x, y+1, x, y+height-2);
			if(insets.bottom>0)
				g.drawLine(x+1, y+height-1, x+width-2, y+height-1);
		}

		@Override
		public boolean isBorderOpaque() {
			return false;
		}

		@Override
		public Insets getBorderInsets(Component c) {
			return insets;
		}
	};
}

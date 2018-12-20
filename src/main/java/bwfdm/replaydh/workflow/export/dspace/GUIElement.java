package bwfdm.replaydh.workflow.export.dspace;

import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import bwfdm.replaydh.ui.icons.IconRegistry;

public class GUIElement {
	
	private FormBuilder propertybuilder = FormBuilder.create();
	private static final String columns = "pref, 6dlu, pref:grow, 3dlu, pref, 3dlu, pref";
	private static final String rows = "pref";
	private JTextField textfield = new JTextField();
	private JTextArea description;
	private JScrollPane scroll;
	private JLabel label = new JLabel();
	private JButton button = null;
	private JButton minusbutton = null;
	private JButton resetButton = null;
	private JPanel panel = new JPanel();
	private static final FormLayout layout = new FormLayout(columns,rows);
	private static final JLabel shadowlabelfirst = new JLabel();
	private static final JLabel shadowlabelsecond = new JLabel();
	private static final Dimension preferredSize = new Dimension(17,17);
	private Dimension buttonSize = textfield.getPreferredSize();
	
	private final static IconRegistry ir = IconRegistry.getGlobalRegistry();
	
	private final static Icon iidel = ir.getIcon("list-remove-5.png");
	private final static Icon iiadd = ir.getIcon("list-add.png");
	
	
	public void create() {
		propertybuilder.columns(columns);
		propertybuilder.rows(rows);
		propertybuilder.panel(panel);
		panel.setLayout(layout);
		propertybuilder.add(label).xy(1, 1);
		if (description != null) {
			propertybuilder.add(scroll).xy(3, 1);
		} else if (textfield != null) {
			propertybuilder.add(textfield).xy(3, 1);
		}
		if(minusbutton != null) {
			minusbutton.setName("minus");
			propertybuilder.add(minusbutton).xy(5, 1);
		} else {
			shadowlabelfirst.setPreferredSize(preferredSize);
			propertybuilder.add(shadowlabelfirst).xy(5, 1);
		}
		if(button != null) {
			button.setName("plus");
			propertybuilder.add(button).xy(7, 1);
		} else {
			shadowlabelsecond.setPreferredSize(preferredSize);
			propertybuilder.add(shadowlabelsecond).xy(7, 1);
		}
	}
	
	public void createResetButton(String resetLabel) {
		propertybuilder.columns(columns);
		propertybuilder.rows(rows);
		propertybuilder.panel(panel);
		panel.setLayout(layout);
		label.setVisible(false);
		resetButton = new JButton(resetLabel);
		resetButton.setPreferredSize(buttonSize);
		propertybuilder.add(label).xy(1, 1);
		propertybuilder.add(resetButton).xy(3, 1);
		shadowlabelfirst.setPreferredSize(preferredSize);
		propertybuilder.add(shadowlabelfirst).xy(5, 1);
		shadowlabelsecond.setPreferredSize(preferredSize);
		propertybuilder.add(shadowlabelsecond).xy(7, 1);
	}
	public JPanel getPanel() {
		return panel;
	}
	public void setPanel(JPanel panel) {
		this.panel = panel;
	}
	public JTextField getTextfield() {
		return textfield;
	}
	public void setTextfield(JTextField textfield) {
		this.textfield = textfield;
	}
	public JButton getButton() {
		return button;
	}
	public void setButton(JButton button) {
		this.button = button;
		this.button.setIcon(iiadd);
		this.button.setPreferredSize(preferredSize);
	}
	public JButton getMinusbutton() {
		return minusbutton;
	}
	public void setMinusbutton(JButton minusbutton) {
		this.minusbutton = minusbutton;
		this.minusbutton.setIcon(iidel);
		this.minusbutton.setPreferredSize(preferredSize);
	}
	public JLabel getLabel() {
		return label;
	}
	public void setLabel(JLabel label) {
		this.label = label;
	}
	public JButton getResetButton() {
		return resetButton;
	}

	public void setResetButton(JButton resetButton) {
		this.resetButton = resetButton;
	}
	public JTextArea getDescription() {
		return description;
	}
	public void setDescription(JTextArea description) {
		this.description = description;
		scroll = new JScrollPane(this.description);
	}
}

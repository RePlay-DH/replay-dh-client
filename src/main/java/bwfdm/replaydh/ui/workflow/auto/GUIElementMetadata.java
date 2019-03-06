package bwfdm.replaydh.ui.workflow.auto;

import java.awt.Dimension;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.workflow.schema.CompoundLabel;


public class GUIElementMetadata {
	
	private FormBuilder propertybuilder = FormBuilder.create();
	private static final String columns = "left:max(120dlu;pref), 6dlu, max(180dlu;min), 3dlu, pref, 3dlu, pref";
	private static final String rows = "pref";
	private JTextField textfield = new JTextField();
	private JComboBox<CompoundLabel> keysDropdown = new JComboBox<>();
	private JButton button = null;
	private JButton minusbutton = null;
	private JButton extraButton = null;
	private JTextArea description;
	private JScrollPane scroll;
	private JPanel panel = new JPanel();
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
		propertybuilder.add(keysDropdown).xy(1, 1);
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
	
	public void createExtraButton(String resetLabel) {
		propertybuilder.columns(columns);
		propertybuilder.rows(rows);
		propertybuilder.panel(panel);
		keysDropdown.setVisible(false);
		extraButton = new JButton(resetLabel);
		extraButton.setPreferredSize(buttonSize);
		propertybuilder.add(new JLabel()).xy(1, 1);
		propertybuilder.add(extraButton).xy(3, 1);
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
	public JButton getExtraButton() {
		return extraButton;
	}
	public JComboBox<CompoundLabel> getKeysDropdown() {
		return keysDropdown;
	}
	public void setKeysDropdown(JComboBox<CompoundLabel> keysDropdown) {
		this.keysDropdown = keysDropdown;
	}
	public void setResetButton(JButton resetButton) {
		this.extraButton = resetButton;
	}
	public JTextArea getDescription() {
		return description;
	}
	public void setDescription(JTextArea description) {
		this.description = description;
		scroll = new JScrollPane(this.description);
	}
}

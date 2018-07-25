package bwfdm.replaydh.workflow.export.dataverse;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class GUIElement {
	private FormBuilder propertybuilder = FormBuilder.create();
	private String columns = "fill:pref:grow, 6dlu, pref, 6dlu, pref";
	private String rows = "pref";
	private JTextField textfield = new JTextField();
	private JButton button = new JButton();
	private JButton minusbutton = new JButton();
	private JPanel panel = new JPanel();
	private FormLayout layout = new FormLayout(columns,rows);
	
	
	
	public JPanel getPanel() {
		propertybuilder.columns(columns);
		propertybuilder.rows(rows);
		propertybuilder.panel(panel);
		panel.setLayout(layout);
		propertybuilder.add(textfield).xy(1, 1);
		propertybuilder.build();
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
	}
	public JButton getMinusbutton() {
		return minusbutton;
	}
	public void setMinusbutton(JButton minusbutton) {
		this.minusbutton = minusbutton;
	}
}

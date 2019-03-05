package bwfdm.replaydh.ui.workflow.auto;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;

import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.workflow.auto.GUIElement;

public class AutoCompletionWizardWorkflowStep implements ActionListener {
	
	public AutoCompletionWizardWorkflowStep() {
		
	}

	private JDialog wizardWindow;
	private Frame frame = null;
	private ResourceManager rm = ResourceManager.getInstance();
	
	private Map<String, List<GUIElement>> elementsofproperty;
	private Map<String, JPanel> propertypanels;
	private Map<String, Integer> panelRow;
	private List<String> listofkeys;
	
	private FormBuilder builderWizard;
	
	public void createWizard() {
		wizardWindow = new JDialog();
		wizardWindow.setModal(true);
		wizardWindow.add(this.createWizardPanel());
		wizardWindow.pack();
		wizardWindow.setTitle(rm.get("replaydh.wizard.metadataAutoWizard.title"));
		wizardWindow.setLocationRelativeTo(null);
		wizardWindow.setVisible(true);
		
	}
	
	public JPanel createWizardPanel() {
		
		builderWizard = FormBuilder.create();
		panelRow = new HashMap<>();
		
		listofkeys = new ArrayList<>();
		propertypanels = new HashMap<>();
		elementsofproperty = new HashMap<>();
		
		JLabel simpleResourceLabel = new JLabel(rm.get("replaydh.wizard.metadataAutoWizard.simpleSearch"));
		GUIElement simpleSearch = createGUIElement();
		simpleSearch.setLabel(simpleResourceLabel);
		
		builderWizard.columns("pref:grow");
		builderWizard.rows("pref, $nlg, pref");
		builderWizard.padding(Paddings.DLU4);
		builderWizard.add(simpleSearch.getPanel()).xy(1, 1);
		return builderWizard.build();
		
	}
	
	/**
	 * Creates one GUI element and
	 * @return it
	 */
	public GUIElement createGUIElement() {
		GUIElement elementToAdd = new GUIElement();
		JTextField textfield = new JTextField();
		elementToAdd.setTextfield(textfield);
		JButton button = new JButton();
		button.addActionListener(this);
		elementToAdd.setButton(button);
		JButton minusbutton = new JButton();
		minusbutton.addActionListener(this);
		elementToAdd.setMinusbutton(minusbutton);
		elementToAdd.create();
		return elementToAdd;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}
}

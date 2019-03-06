package bwfdm.replaydh.ui.workflow.auto;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;
import com.jgoodies.forms.layout.FormLayout;

import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.workflow.auto.GUIElement;
import bwfdm.replaydh.ui.workflow.auto.GUIElementMetadata;
import bwfdm.replaydh.ui.helper.DocumentAdapter;
import bwfdm.replaydh.ui.GuiUtils;

public class AutoCompletionWizardWorkflowStep implements ActionListener {
	
	public AutoCompletionWizardWorkflowStep() {
		
	}

	private JDialog wizardWindow;
	private ResourceManager rm = ResourceManager.getInstance();
	
	private Map<String, List<GUIElementMetadata>> elementsofproperty;
	private Map<String, JPanel> propertypanels;
	//private Map<String, Integer> panelRow;
	private List<String> listofkeys;
	
	private JPanel mainPanelWizard;
	//private DocumentAdapter adapter;
	private List<GUIElementMetadata> dd = new ArrayList<>();
	
	private FormBuilder builderWizard;
	
	public void createWizard() {
		wizardWindow = new JDialog();
		wizardWindow.setModal(true);
		mainPanelWizard=this.createWizardPanel();
		wizardWindow.add(mainPanelWizard);
		wizardWindow.pack();
		wizardWindow.setTitle(rm.get("replaydh.wizard.metadataAutoWizard.title"));
		wizardWindow.setLocationRelativeTo(null);
		wizardWindow.setVisible(true);
		
	}
	
	public JPanel createWizardPanel() {
		
		builderWizard = FormBuilder.create();
		//panelRow = new HashMap<>();
		
		listofkeys = new ArrayList<>();
		propertypanels = new HashMap<>();
		elementsofproperty = new HashMap<>();
		
		GUIElement simpleSearch = createGUIElement();
		simpleSearch.getLabel().setText(rm.get("replaydh.wizard.metadataAutoWizard.simpleSearch"));
		
		GUIElementMetadata chooseProperties = createGUIElement("keys");
		dd.add(chooseProperties);
		propertypanels.put("defaultdd", chooseProperties.getPanel());
		
		builderWizard.columns("pref:grow");
		builderWizard.rows("pref, $nlg, pref");
		builderWizard.padding(Paddings.DLU4);
		builderWizard.add(simpleSearch.getPanel()).xy(1, 1);
		listofkeys.add("gsearch");
		builderWizard.add(chooseProperties.getPanel()).xy(1, 3);
		listofkeys.add("defaultdd");
		elementsofproperty.put("defaultdd", dd);
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
		Object source = e.getSource();
		JButton buttonpressed = null;
		JButton minusbuttonpressed = null;
		boolean done=false;
		for (String propertyname : listofkeys) {
			if (elementsofproperty.get(propertyname) != null) {
				for (int buttonNumber = 0; buttonNumber < elementsofproperty.get(propertyname).size(); buttonNumber++) {
					buttonpressed=elementsofproperty.get(propertyname).get(buttonNumber).getButton();
					minusbuttonpressed=elementsofproperty.get(propertyname).get(buttonNumber).getMinusbutton();
					if (source == buttonpressed) {
						GUIElementMetadata element = createGUIElement(propertyname);
						elementsofproperty.get(propertyname).add(element);
						if (propertyname.equals("creator")) {
							//element.getTextfield().getDocument().addDocumentListener(adapter);
							GuiUtils.prepareChangeableBorder(element.getTextfield());
							refreshBorder(elementsofproperty.get(propertyname));
						}
						refreshPanel(propertyname);
						done=true;
						break;
					}
					if (source == minusbuttonpressed) {
						if (elementsofproperty.get(propertyname).size() > 1) {
							if (propertyname.equals("creator")) {
								//elementsofproperty.get(propertyname).get(buttonNumber).getTextfield().getDocument().removeDocumentListener(adapter);
								elementsofproperty.get(propertyname).get(buttonNumber).getButton().removeActionListener(this);
								elementsofproperty.get(propertyname).get(buttonNumber).getMinusbutton().removeActionListener(this);
							}
							removeElementFromPanel(propertyname,buttonNumber);
						} else {
							elementsofproperty.get(propertyname).get(0).getTextfield().setText("");
						}
						if (propertyname.equals("creator")) {
							refreshBorder(elementsofproperty.get(propertyname));
						}
						done=true;
						break;
					}
				}
				if (done == true) {
					break;
				}
			}
		}
	}
	
	/**
	 * Refreshes one JPanel according to the specified metadata property and its position (index) in the main
	 * panelbuilder (builder)
	 * @param metadatapropertyname
	 */
	public void refreshPanel(String metadatapropertyname) {
		String columns="pref:grow";
		String rows="pref";

		FormLayout layout = new FormLayout(columns,rows);


		JPanel onepropertypanel = propertypanels.get(metadatapropertyname);

		onepropertypanel.removeAll();

		onepropertypanel.setLayout(layout);



		JPanel newpropertypanel = new JPanel();


		FormBuilder propertybuilder = FormBuilder.create();
		propertybuilder.columns(columns);
		propertybuilder.rows(rows);



		propertybuilder.panel(newpropertypanel);

		propertypanels.put(metadatapropertyname, newpropertypanel);
		onepropertypanel.removeAll();
		onepropertypanel.setLayout(layout);

		int numberOfElements=elementsofproperty.get(metadatapropertyname).size();

		int z=0;

		for(GUIElementMetadata oneguielement : elementsofproperty.get(metadatapropertyname)) {

			if (z == 0) {
				oneguielement.create();
				if(oneguielement.getButton().getActionListeners().length == 0) {
					oneguielement.getButton().addActionListener(this);
				}
				if(oneguielement.getMinusbutton().getActionListeners().length == 0) {
					oneguielement.getMinusbutton().addActionListener(this);
				}
				/*if (oneguielement.getLabel().getText().equals("")) {
					switch (metadatapropertyname) {
					case "creator":
						oneguielement.getLabel().setText(rm.get("darus.wizard.dataversePublisher.editMetadata.creatorLabel"));
						break;
					case "publisher":
						oneguielement.getLabel().setText(rm.get("darus.wizard.dataversePublisher.editMetadata.publisherLabel"));
						break;
					case "subject":
						oneguielement.getLabel().setText(rm.get("darus.wizard.dataversePublisher.editMetadata.subjectLabel"));
						break;
					case "sources":
						oneguielement.getLabel().setText(rm.get("darus.wizard.dataversePublisher.editMetadata.sourcesLabel"));
						break;
					}
				}*/
			}

			propertybuilder.add(oneguielement.getPanel()).xy(1, (z*2)+1);

			if (numberOfElements > 1) {
				propertybuilder.appendRows("$nlg, pref");
			}

			z++;


		}
		if (elementsofproperty.get(metadatapropertyname).size() > 1) {
			propertybuilder.addSeparator("").xyw(1, ((z*2)+1), 1);
			z++;
		}
		builderWizard.add(propertybuilder.build()).xy(1, 3);
		Window parentComponent = (Window) SwingUtilities.getAncestorOfClass(Window.class, mainPanelWizard);
		if (parentComponent != null) {
			parentComponent.pack();
		}
	}
	
	public boolean refreshBorder(List<GUIElementMetadata> propertylist) {
		boolean allEmpty=true;
		for (GUIElementMetadata checkElement : propertylist) {
			if (!(checkElement.getTextfield().getText().isEmpty())) {
				allEmpty=false;
				break;
			}
		}
		if (!(allEmpty)) {
			for (GUIElementMetadata changeElement : propertylist) {
				GuiUtils.toggleChangeableBorder(changeElement.getTextfield(),false);
			}
		} else {
			for (GUIElementMetadata changeElement : propertylist) {
				GuiUtils.toggleChangeableBorder(changeElement.getTextfield(),true);
			}
		}
		return !allEmpty;
	}
	
	public GUIElementMetadata createGUIElement(String metadataproperty) {
		GUIElementMetadata elementToAdd = new GUIElementMetadata();
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
	
	private void removeElementFromPanel(String metadatapropertyname, int buttonNumber) {
		elementsofproperty.get(metadatapropertyname).remove(buttonNumber);
		refreshPanel(metadatapropertyname);
	}
}

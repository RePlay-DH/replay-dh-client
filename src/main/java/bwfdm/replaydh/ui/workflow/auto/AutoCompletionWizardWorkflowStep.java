package bwfdm.replaydh.ui.workflow.auto;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;
import com.jgoodies.forms.layout.FormLayout;

import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.workflow.WorkflowUIUtils;
import bwfdm.replaydh.ui.workflow.auto.GUIElement;
import bwfdm.replaydh.ui.workflow.auto.GUIElementMetadata;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifiable.Type;
import bwfdm.replaydh.workflow.schema.CompoundLabel;
import bwfdm.replaydh.workflow.schema.LabelSchema;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;
import bwfdm.replaydh.ui.GuiUtils;

public class AutoCompletionWizardWorkflowStep implements ActionListener {
	
	private JDialog wizardWindow;
	private ResourceManager rm = ResourceManager.getInstance();
	
	private Map<String, List<GUIElementMetadata>> elementsofproperty;
	private Map<String, JPanel> propertypanels;
	private List<String> listofkeys;
	
	private JPanel mainPanelWizard;
	private List<GUIElementMetadata> dd = new ArrayList<>();
	
	private JComboBox<CompoundLabel> cbRoleType = null;
	private CompoundLabel defaultTypeOrRole = null;
	
	private Identifiable.Type type = null;
	private WorkflowSchema schema = null;
	
	private GUIElementMetadata resetButton = null;
	private GUIElementMetadata searchButton = null;
	
	private FormBuilder builderWizard;
	
	public void createWizard(WorkflowSchema schema, Identifiable.Type type) {
		this.schema=schema;
		this.type=type;
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
		
		listofkeys = new ArrayList<>();
		propertypanels = new HashMap<>();
		elementsofproperty = new HashMap<>();
		
		GUIElement simpleSearch = createGUIElement();
		simpleSearch.getLabel().setText(rm.get("replaydh.wizard.metadataAutoWizard.simpleSearch"));
		
		GUIElementMetadata chooseProperties = createGUIElement("keys");
		cbRoleType = WorkflowUIUtils.createLabelComboBox(getLabelSchema());
		defaultTypeOrRole = WorkflowUIUtils.createDefaultLabel(getLabelSchema());
		cbRoleType.setSelectedItem(defaultTypeOrRole);
		cbRoleType.addActionListener(this);
		chooseProperties.getKeysDropdown().setModel(cbRoleType.getModel());
		dd.add(chooseProperties);
		propertypanels.put("defaultdd", chooseProperties.getPanel());
		
		searchButton = new GUIElementMetadata();
		searchButton.createExtraButton(rm.get("replaydh.wizard.metadataAutoWizard.search"));
		searchButton.getExtraButton().addActionListener(this);
		resetButton = new GUIElementMetadata();
		resetButton.createExtraButton(rm.get("replaydh.wizard.metadataAutoWizard.reset"));
		resetButton.getExtraButton().addActionListener(this);
		
		builderWizard.columns("pref:grow");
		builderWizard.rows("pref, $nlg, pref, $nlg, pref, $nlg, pref");
		builderWizard.padding(Paddings.DLU4);
		builderWizard.add(simpleSearch.getPanel()).xy(1, 1);
		listofkeys.add("gsearch");
		builderWizard.add(chooseProperties.getPanel()).xy(1, 3);
		listofkeys.add("defaultdd");
		builderWizard.add(resetButton.getPanel()).xy(1, 5);
		builderWizard.add(searchButton.getPanel()).xy(1, 7);
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
						refreshPanel(propertyname);
						done=true;
						break;
					}
					if (source == minusbuttonpressed) {
						if (elementsofproperty.get(propertyname).size() > 1) {
							removeElementFromPanel(propertyname,buttonNumber);
						} else {
							elementsofproperty.get(propertyname).get(0).getTextfield().setText("");
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
		if (source == resetButton.getExtraButton()) {
			int size = elementsofproperty.get("defaultdd").size();
			for (int i=size-1; i > 0; i--) {
				elementsofproperty.get("defaultdd").remove(i);
				refreshPanel("defaultdd");
			}
		}
		if (source == searchButton.getExtraButton()) {
			System.out.println("Search!");
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
				if(oneguielement.getKeysDropdown().getActionListeners().length == 0) {
					oneguielement.getKeysDropdown().addActionListener(this);
				}
			}
			
			oneguielement.getKeysDropdown().setModel(cbRoleType.getModel());
			oneguielement.getKeysDropdown().addActionListener(this);

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
	
	private LabelSchema getLabelSchema() {
		return isPersonEditor() ? schema.getRoleSchema() : schema.getResourceTypeSchema();
	}
	
	public boolean isPersonEditor() {
		return type==Type.PERSON;
	}
}

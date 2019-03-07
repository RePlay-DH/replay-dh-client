package bwfdm.replaydh.ui.workflow.auto;

import static java.util.Objects.requireNonNull;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.Border;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;
import com.jgoodies.forms.layout.FormLayout;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.io.LocalFileObject;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.workflow.IdentifiableEditor;
import bwfdm.replaydh.ui.workflow.WorkflowUIUtils;
import bwfdm.replaydh.ui.workflow.auto.GUIElement;
import bwfdm.replaydh.ui.workflow.auto.GUIElementMetadata;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Person;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.Tool;
import bwfdm.replaydh.workflow.Identifiable.Role;
import bwfdm.replaydh.workflow.Identifiable.Type;
import bwfdm.replaydh.workflow.catalog.MetadataCatalogTestImpl;
import bwfdm.replaydh.workflow.catalog.MetadataCatalog.Constraint;
import bwfdm.replaydh.workflow.catalog.MetadataCatalog.QuerySettings;
import bwfdm.replaydh.workflow.catalog.MetadataCatalog.Result;
import bwfdm.replaydh.workflow.schema.IdentifierSchema;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.core.FileResolutionWorker;
import bwfdm.replaydh.ui.core.ResourceDragController;
import bwfdm.replaydh.ui.core.ResourceDragController.Mode;
import bwfdm.replaydh.ui.helper.ScrollablePanel;
import bwfdm.replaydh.ui.helper.WrapLayout;
import bwfdm.replaydh.ui.helper.ScrollablePanel.ScrollableSizeHint;
import bwfdm.replaydh.ui.icons.IconRegistry;

/**
 * 
 * @author Florian Fritze
 *
 */
public class AutoCompletionWizardWorkflowStep implements ActionListener {
	
	public AutoCompletionWizardWorkflowStep(WorkflowSchema schema, RDHEnvironment environment) {
		this.schema=schema;
		this.environment=environment;
		// Create Lists of IdentifiableEditorElements
      	personEditorElementsList = new ArrayList<IdentifiableEditorElement<Person>>();
    	inputResourceEditorElementsList = new ArrayList<IdentifiableEditorElement<Resource>>();
    	outputResourceEditorElementsList = new ArrayList<IdentifiableEditorElement<Resource>>();
    	toolEditorElementsList = new ArrayList<IdentifiableEditorElement<Tool>>();

    	sortedPersons = new ArrayList<Person>();
    	sortedTools = new ArrayList<Tool>();
    	sortedInputs = new ArrayList<Resource>();
    	sortedOutputs = new ArrayList<Resource>();


    	// General action listener for each propertyGroupElement
        actionListenerIdentifiableEditorElement = this;

        // Add new identifiable (Person/Resource/Tool)
        actionListenerAddIdentifiable = this;
        
        personsPanel = new CategoryPanel(Role.PERSON, btnAddPerson, btnAddAutoPerson);
    	toolPanel = new CategoryPanel(Role.TOOL, btnAddTool, btnAddAutoTool);
    	inputResourcesPanel = new CategoryPanel(Role.INPUT, btnAddInputResource, btnAddAutoInputResource);
    	outputResourcesPanel = new CategoryPanel(Role.OUTPUT, btnAddOutputResource, btnAddAutoOutputResource);

    	// Buttons to add new identifiable
        btnAddPerson = createButton(toolTipAddPerson,iconAdd,buttonBigPreferredSize,actionListenerAddIdentifiable);
    	btnAddTool = createButton(toolTipAddTool,iconAdd,buttonBigPreferredSize,actionListenerAddIdentifiable);
    	btnAddInputResource = createButton(toolTipAddInputResource,iconAdd,buttonBigPreferredSize,actionListenerAddIdentifiable);
    	btnAddOutputResource = createButton(toolTipAddOutputResource,iconAdd,buttonBigPreferredSize,actionListenerAddIdentifiable);
    	
    	// Buttons to add new identifiable
        btnAddAutoPerson = createButton(toolTipAddPerson,iconAddAuto,buttonBigPreferredSize,actionListenerAddIdentifiable);
    	btnAddAutoTool = createButton(toolTipAddTool,iconAddAuto,buttonBigPreferredSize,actionListenerAddIdentifiable);
    	btnAddAutoInputResource = createButton(toolTipAddInputResource,iconAddAuto,buttonBigPreferredSize,actionListenerAddIdentifiable);
    	btnAddAutoOutputResource = createButton(toolTipAddOutputResource,iconAddAuto,buttonBigPreferredSize,actionListenerAddIdentifiable);
    	
    	scrollablePanel = new ScrollablePanel();
    	scrollablePanel.setScrollableWidth(ScrollableSizeHint.FIT);
	}

	private JDialog wizardWindow;
	private ResourceManager rm = ResourceManager.getInstance();
	
	private Map<String, List<GUIElementMetadata>> elementsofproperty;
	private Map<String, JPanel> propertypanels;
	private List<String> listofkeys;
	
	private JPanel mainPanelWizard;
	private List<GUIElementMetadata> dd = new ArrayList<>();
	
	private JComboBox<String> ddKeys = new JComboBox<>();
	
	private RDHEnvironment environment;
	private WorkflowSchema schema;
	
	private Identifiable.Type type = null;
	
	private GUIElementMetadata resetButton = null;
	private GUIElementMetadata searchButton = null;
	
	private FormBuilder builderWizard;
	
	// Scrollable panel with all persons/tool/resources
    private ScrollablePanel scrollablePanel;
	
	// Panels with all persons, tool, input/output resources
    private CategoryPanel personsPanel;
    private CategoryPanel toolPanel;
    private CategoryPanel inputResourcesPanel;
    private CategoryPanel outputResourcesPanel;
	
	private List<IdentifiableEditorElement<Person>> personEditorElementsList;
    private List<IdentifiableEditorElement<Tool>> toolEditorElementsList;
    private List<IdentifiableEditorElement<Resource>> inputResourceEditorElementsList;
    private List<IdentifiableEditorElement<Resource>> outputResourceEditorElementsList;
    
    List<Person> sortedPersons;
    List<Resource> sortedInputs;
    List<Resource> sortedOutputs;
    List<Tool> sortedTools;
    
    Dimension buttonPreferredSize = new Dimension(16,16);
    Dimension buttonBigPreferredSize = new Dimension(32,32);
    
    // Icons
    private Icon iconRemove = IconRegistry.getGlobalRegistry().getIcon("list-remove-5.png");
    private Icon iconAdd = IconRegistry.getGlobalRegistry().getIcon("list-add.png");
    private Icon iconAddAuto = IconRegistry.getGlobalRegistry().getIcon("document-import-2.png");
    private Icon iconExpanded = IconRegistry.getGlobalRegistry().getIcon("right.png");
    private Icon iconCollapsed = IconRegistry.getGlobalRegistry().getIcon("left.png");

    // Button ToolTip text.
    private final String toolTipRemovePerson = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.toolTips.removePerson");
    private final String toolTipRemoveInputResource = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.toolTips.removeInput");
    private final String toolTipRemoveOutputResource = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.toolTips.removeOutput");
    private final String toolTipRemoveTool = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.toolTips.removeTool");

    private final String toolTipExpandedView = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.toolTips.expandedView");
    private final String toolTipCollapsedView = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.toolTips.collapsedView");

    private final String toolTipAddPerson = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.toolTips.addPerson");
    private final String toolTipAddInputResource = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.toolTips.addInput");
    private final String toolTipAddOutputResource = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.toolTips.addOutput");
    private final String toolTipAddTool = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.toolTips.addTool");

    
    private final String personElementHeader = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.labels.personElementHeader");
    private final String toolElementHeader = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.labels.toolElementHeader");
    private final String inputResourceElementHeader = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.labels.inputElementHeader");
    private final String outputResourceElementHeader = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.labels.outputElementHeader");

    private ActionListener actionListenerIdentifiableEditorElement;		//for group buttons: remove, edit, more/less
    private ActionListener actionListenerAddIdentifiable;	//for "add new identifiable" button
    
    private Border emptyBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
    private Border lineBorder = BorderFactory.createLineBorder(Color.BLACK, 1, false);
    private Border redBorder = BorderFactory.createLineBorder(Color.RED, 2, false);
    private Border defaultBorder;
    
 // Add buttons
    private JButton btnAddPerson = null;
    private JButton btnAddInputResource = null;
    private JButton btnAddOutputResource = null;
    private JButton btnAddTool = null;
    
    // Add buttons
    private JButton btnAddAutoPerson = null;
    private JButton btnAddAutoInputResource = null;
    private JButton btnAddAutoOutputResource = null;
    private JButton btnAddAutoTool = null;
    
	public void createWizard(WorkflowSchema schema, Identifiable.Type type) {
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
		simpleSearch.getButton().setVisible(false);
		simpleSearch.getMinusbutton().setVisible(false);
		
		GUIElementMetadata chooseProperties = createGUIElement("keys");
		if(this.environment.getLocale().getLanguage().equals(new Locale("de").getLanguage())) {
			ddKeys = new JComboBox(ddTypesDe.values());
		} else {
			ddKeys = new JComboBox(ddTypesEng.values());
		}
		ddKeys.addActionListener(this);
		chooseProperties.getKeysDropdown().setModel(ddKeys.getModel());
		dd.add(chooseProperties);
		propertypanels.put("defaultdd", chooseProperties.getPanel());
		
		searchButton = new GUIElementMetadata();
		searchButton.createExtraButton(rm.get("replaydh.wizard.metadataAutoWizard.search"));
		searchButton.getExtraButton().addActionListener(this);
		resetButton = new GUIElementMetadata();
		resetButton.createExtraButton(rm.get("replaydh.wizard.metadataAutoWizard.reset"));
		resetButton.getExtraButton().addActionListener(this);
		
		builderWizard.columns("pref:grow");
		builderWizard.rows("pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref");
		builderWizard.padding(Paddings.DLU4);
		builderWizard.add(simpleSearch.getPanel()).xy(1, 1);
		listofkeys.add("gsearch");
		builderWizard.add(chooseProperties.getPanel()).xy(1, 3);
		listofkeys.add("defaultdd");
		builderWizard.add(resetButton.getPanel()).xy(1, 5);
		builderWizard.add(searchButton.getPanel()).xy(1, 7);
		elementsofproperty.put("defaultdd", dd);
		builderWizard.add(scrollablePanel).xy(1, 9);
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
			boolean empty=true;
			List<Constraint> constraints = new ArrayList<>();
			for(Iterator<GUIElementMetadata> elements = elementsofproperty.get("defaultdd").iterator();elements.hasNext();) {
				GUIElementMetadata element=elements.next();
				if(!(element.getTextfield().getText().isEmpty())) {
					empty=false;
					Constraint constraint;
					if(this.environment.getLocale().getLanguage().equals(new Locale("de").getLanguage())) {
						String key = getKeyddTypesDe(element.getKeysDropdown().getSelectedItem().toString());
						constraint = new Constraint(key,element.getTextfield().getText());
					} else {
						String key = getKeyddTypesEng(element.getKeysDropdown().getSelectedItem().toString());
						constraint = new Constraint(key,element.getTextfield().getText());
					}
					constraints.add(constraint);
				}
			}
			if(empty == false) {
				QuerySettings settings = new QuerySettings();
				settings.setSchema(schema);
				this.searchWithConstraints(settings, constraints);
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
				if(oneguielement.getKeysDropdown().getActionListeners().length == 0) {
					oneguielement.getKeysDropdown().addActionListener(this);
				}
			}
			
			if(this.environment.getLocale().getLanguage().equals(new Locale("de").getLanguage())) {
				ddKeys = new JComboBox(ddTypesDe.values());
			} else {
				ddKeys = new JComboBox(ddTypesEng.values());
			}
			ddKeys.addActionListener(this);
			
			oneguielement.getKeysDropdown().setModel(ddKeys.getModel());
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
	
	
	public boolean isPersonEditor() {
		return type==Type.PERSON;
	}
	
	public enum ddTypesEng {
		Title,
		Name,
		Type,
		Identifier,
		Parameter,
		Nameversion,
		Environment
	}
	
	public enum ddTypesDe {
		Titel,
		Name,
		Typ,
		Identifier,
		Parameter,
		Namensversion,
		Environment
	}
	
	public static String getKeyddTypesEng(String type) {
		String returnType = null;
		switch(type) {
		case "Title":
			returnType="title";
			break;
		case "Name":
			returnType="name";
			break;
		case "Type":
			returnType="type";
			break;
		case "Identifier":
			returnType="id";
			break;
		case "Parameter":
			returnType="parameter";
			break;
		case "Nameversion":
			returnType="namever";
			break;
		case "Environment":
			returnType="env";
			break;
		}
		return returnType;
	}
	
	public static String getKeyddTypesDe(String type) {
		String returnType = null;
		switch(type) {
		case "Titel":
			returnType="title";
			break;
		case "Name":
			returnType="name";
			break;
		case "Typ":
			returnType="type";
			break;
		case "Identifier":
			returnType="id";
			break;
		case "Parameter":
			returnType="parameter";
			break;
		case "Namensversion":
			returnType="namever";
			break;
		case "Environment":
			returnType="env";
			break;
		}
		return returnType;
	}
	
	private void searchWithConstraints(QuerySettings settings, List<Constraint> constraints) {
		
		SwingWorker<Boolean, Object> worker = new SwingWorker<Boolean, Object>(){

			boolean success=true;
			MetadataCatalogTestImpl search = new MetadataCatalogTestImpl(settings.getSchema());
			Result results = null;
			@Override
			protected Boolean doInBackground() throws Exception {
				search.createObjects();
				results=search.query(settings, constraints);
				if(results.isEmpty()) {
					success=false;
				}
				return success;
			}
			@Override
			protected void done() {
				if(success) {
					for(Identifiable result : results) {
						System.out.println(result.getSystemId());
					}
				}
			}
		};
		worker.execute();
	}
	
	private void addIdentifiable(Identifiable identifiable, Role role) {
    	switch (role) {
		case PERSON:
        	sortedPersons.add((Person) identifiable);
			break;

		case TOOL:
			sortedTools.add((Tool) identifiable);
			break;

		case INPUT:
			sortedInputs.add((Resource) identifiable);
			break;

		case OUTPUT:
			sortedOutputs.add((Resource) identifiable);
			//contentChanged = true;
			break;

		default:
			break;
		}

		//contentChanged = true;
		updateIdentifiableEditorElements();
		updateEditorView();
    }
	
	/**
	 * Create/Update the whole model of the editor.
	 * @param workflowstep
	 */
	private void updateIdentifiableEditorElements(){

		//TODO: delete workflowStep as input parameter

		// Store all identifiable editor elements lists to restore later the expanded view if needed
        List<IdentifiableEditorElement<Person>> oldPersonEditorElelemtsList = new ArrayList<IdentifiableEditorElement<Person>>(personEditorElementsList);
        List<IdentifiableEditorElement<Tool>> oldToolEditorElementsList = new ArrayList<IdentifiableEditorElement<Tool>>(toolEditorElementsList);
        List<IdentifiableEditorElement<Resource>> oldInputResourceEditorElementsList = new ArrayList<IdentifiableEditorElement<Resource>>(inputResourceEditorElementsList);
        List<IdentifiableEditorElement<Resource>> oldOutputResourceEditorElementsList = new ArrayList<IdentifiableEditorElement<Resource>>(outputResourceEditorElementsList);

	    // Clear all element lists (to avoid duplications)
	    this.personEditorElementsList.clear();
	    this.inputResourceEditorElementsList.clear();
	    this.outputResourceEditorElementsList.clear();
	    this.toolEditorElementsList.clear();

	    int i;

	    // Persons
	    i = 0;
	    for(Person person: this.sortedPersons){
			i++;
	    	IdentifiableEditorElement<Person> personElement =
	    			new IdentifiableEditorElement<Person>(
	    					personElementHeader + " " + i,
							person,
							this.schema,
							createButton(toolTipRemovePerson, iconRemove, buttonPreferredSize, actionListenerIdentifiableEditorElement), //"remove" button
				    		createButton(toolTipExpandedView, iconExpanded, buttonPreferredSize, actionListenerIdentifiableEditorElement)  //"more info" button
				    		);
	    	this.personEditorElementsList.add(personElement);
	    }

	    // Input resources
	    i = 0;
	    for(Resource inputResource: this.sortedInputs){
	    	i++;
	    	IdentifiableEditorElement<Resource> inputResourceElement =
	    			new IdentifiableEditorElement<Resource>(
	    					inputResourceElementHeader + " " + i,
							inputResource,
							this.schema,
							createButton(toolTipRemoveInputResource, iconRemove, buttonPreferredSize, actionListenerIdentifiableEditorElement), //"remove" button
				    		createButton(toolTipExpandedView, iconExpanded, buttonPreferredSize, actionListenerIdentifiableEditorElement)  //"more info" button
				    		);
	    	this.inputResourceEditorElementsList.add(inputResourceElement);
	    }

	    // Output resources
	    i = 0;
	    for(Resource outputResource: this.sortedOutputs){
	    	i++;
	    	IdentifiableEditorElement<Resource> outputResourceElement =
	    			new IdentifiableEditorElement<Resource>(
	    					outputResourceElementHeader + " " + i,
							outputResource,
							this.schema,
							createButton(toolTipRemoveOutputResource, iconRemove, buttonPreferredSize, actionListenerIdentifiableEditorElement), //"remove" button
				    		createButton(toolTipExpandedView, iconExpanded, buttonPreferredSize, actionListenerIdentifiableEditorElement)  //"more info" button
				    		);
	    	this.outputResourceEditorElementsList.add(outputResourceElement);
	    }

	    // Tool
	    for(Tool tool: this.sortedTools){
		    if (tool != null){
		    	IdentifiableEditorElement<Tool> toolGroupElement =
		    			new IdentifiableEditorElement<Tool>(
		    					toolElementHeader,
								tool,
								this.schema,
								createButton(toolTipRemoveTool, iconRemove, buttonPreferredSize, actionListenerIdentifiableEditorElement), //"remove" button
					    		createButton(toolTipExpandedView, iconExpanded, buttonPreferredSize, actionListenerIdentifiableEditorElement) //"more info" button
					    		);
		    	this.toolEditorElementsList.add(toolGroupElement); //as array of 1 element, for the future
		    }
	    }

	    // Restore expanded view mode for all editorElememnts if needed
        restoreExpandedViewMode(personEditorElementsList,oldPersonEditorElelemtsList);
        restoreExpandedViewMode(toolEditorElementsList, oldToolEditorElementsList);
        restoreExpandedViewMode(inputResourceEditorElementsList, oldInputResourceEditorElementsList);
        restoreExpandedViewMode(outputResourceEditorElementsList, oldOutputResourceEditorElementsList);
	}
	
	/**
     * Restore an expanded view mode of each person/tool/resource editor according to the old state
     *
     * @param newEditorElements - array of PropertyGroupElements which will be changed
     * @param oldEditorElements - old state, array of PropertyGroupElements, will NOT be changed
     */
    private <T extends Identifiable> void restoreExpandedViewMode(List<IdentifiableEditorElement<T>> newEditorElements, List<IdentifiableEditorElement<T>> oldEditorElements){

    	for(IdentifiableEditorElement<T> newElement : newEditorElements){
        	for(IdentifiableEditorElement<T> oldElement : oldEditorElements){
				if(oldElement.getIdentifiableObject().getSystemId() == newElement.getIdentifiableObject().getSystemId()){
					if (oldElement.isExpanded() == true){
						newElement.setExpanded(true);
						// Change button "show expanded view" to "show collapsed view"
				    	for(JButton btn: newElement.getButtons()){
				    		if(btn.getName() == toolTipExpandedView){
				    			btn.setName(toolTipCollapsedView);
				    			btn.setIcon(iconCollapsed);
				    			break;
				    		}
				    	}
					}
				}
        	}
    	}
    }
	
    /**
     * Class to group all properties of each identifiable element,
     * e.g. 1 person, 1 resource, 1 tool...
     *
     * @author vk
     *
     */
    private class IdentifiableEditorElement<T extends Identifiable> {

    	private String headerString;
    	private T identifiable;					//instance of the Identifiable-object (e.g. concrete resource, tool, person)
    	private WorkflowSchema workflowSchema;	//workflow schema
    	private boolean expandedView; 			//expanded (true) or collapsed (false) view mode
    	private List<JButton> buttons; 			//buttons like "more info", "edit", "remove"
       	private IdentifiableEditor editor;  	//local editor

       	private final JPanel panel;

    	/**
    	 * Constructor
    	 * @param header - name of the element
    	 * @param workflowSchema
    	 * @param identifiable - identifiable object(Person/Tool/Resource)
    	 * @param buttons - Array of buttons in the header (e.g "remove element"/"collapsed-expanded view"...)
    	 */
    	public IdentifiableEditorElement(String header, T identifiable, WorkflowSchema workflowSchema, JButton... buttons){

    		this.headerString = header;
    		this.identifiable = identifiable;
    		this.workflowSchema = workflowSchema;
    		this.expandedView = false;
    		this.buttons = new ArrayList<>();

    		// Set buttons (which will be used in header)
    		for (JButton btn : buttons){
				this.buttons.add(btn);
			}

    		// Set identifiable editor
    		editor = createIdentifiableEditor(workflowSchema, this.identifiable.getType());
        	editor.setEditingItem(IdentifiableEditor.wrap(Collections.singleton(this.identifiable)));

        	/*
        	 * To work well with FlowLayout, we need a way to signal the upper
        	 * border of the components to be used as baseline.
        	 * Unfortunately this is only possible via subclassing.
        	 */
        	panel = new JPanel() {

    			private static final long serialVersionUID = 7443352565640825604L;

    			@Override
    			public int getBaseline(int width, int height) {
    				return 0;
    			}
    		};
    		panel.setBorder(BorderFactory.createCompoundBorder(lineBorder, emptyBorder));
    	}

    	//Check type of the identifiable: person, tool, resource
    	public boolean isPersonEditor() {
    		return identifiable.getType()==Type.PERSON;
    	}

    	private IdentifierSchema getIdentifierSchema() {
    		return isPersonEditor() ? workflowSchema.getPersonIdentifierSchema() : workflowSchema.getResourceIdentifierSchema();
    	}

    	public void setHeaderString(String header) {
    		this.headerString = header;
    	}

    	public IdentifiableEditor getEditor() {
    		return this.editor;
    	}

    	/**
    	 * Get a header panel of the editor element -> header + buttons
    	 * @return JPanel
    	 */
    	private JPanel getHeaderPanel() {

    		// Create extra panel for the control buttons (remove|collapsed-expanded|...)
    		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    		for (JButton btn : this.buttons){
				buttonPanel.add(btn);
			}

    		// Return header panel (header + buttons)
    		FormBuilder headerBuilder = FormBuilder.create()
    					.columns("left:pref:grow, 3dlu, pref")
    					.rows("pref")
    					.addSeparator(this.headerString).xyw(1,1,2)
    					.add(buttonPanel).xy(3,1);
    		return headerBuilder.build();
    	}


    	/**
    	 * Get collapsed view of the editor element
    	 *
    	 * @return JPanel
    	 */
    	private JPanel getCollapsedViewPanel() {

    		panel.removeAll();
        	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        	panel.add(getHeaderPanel()); //Add header panel (Separator + buttons)

        	//--- SHOW IDENTIFIERS

    		List<Identifier> printedIdentifiersList = new ArrayList<Identifier>();

			// Add default identifier if already exists (e.g. NAME for the person)
    		for(Identifier identifier: identifiable.getIdentifiers()) {
    			if(identifier.getType().equals(getIdentifierSchema().getDefaultIdentifierType())) {
    				printedIdentifiersList.add(identifier);
    				break;
    			}
    		}

    		// As an addition to the default identifier, print some other hard predefined identifiers
    		String[] predefinedTypes = new String[0];
    		if(identifiable instanceof Person) {
				predefinedTypes = new String[] {
						ResourceManager.getInstance().get("replaydh.workflowSchema.default.name.name"),
						ResourceManager.getInstance().get("replaydh.workflowSchema.default.firstnameLastname.name"),
						ResourceManager.getInstance().get("replaydh.workflowSchema.default.alias.name")
						}; //Person predefined types to print

			} else if(identifiable instanceof Tool) {
				predefinedTypes = new String[] {
						ResourceManager.getInstance().get("replaydh.workflowSchema.default.nameVersion.name"),
						ResourceManager.getInstance().get("replaydh.workflowSchema.default.path.name"),
						ResourceManager.getInstance().get("replaydh.workflowSchema.default.url.name")
						}; //Tool predefined types to print

			} else if (identifiable instanceof Resource) {
				predefinedTypes = new String[] {
						ResourceManager.getInstance().get("replaydh.workflowSchema.default.name.name"),
						ResourceManager.getInstance().get("replaydh.workflowSchema.default.path.name"),
						ResourceManager.getInstance().get("replaydh.workflowSchema.default.url.name")
						}; //Input|Output resource predefined types to print
			}
    		for(String predefinedType: predefinedTypes) {
    			for(Identifier identifier: identifiable.getIdentifiers()) {
    				if((identifier.getType().getName().equals(predefinedType))
    						&& (!identifier.getType().equals(getIdentifierSchema().getDefaultIdentifierType()))) {

    					printedIdentifiersList.add(identifier); // add identifier to the list of printed identifiers
    				}
    			}
    		}

    		// Create a FormBuilder for the identifiers and standard properties
    		FormBuilder bodyPanelBuilder = FormBuilder.create()
								.columns("left:pref:grow")
								.rows("pref");
			bodyPanelBuilder.border(BorderFactory.createEmptyBorder(2, 0, 2, 0));
			int rowIndex = 0; //for the "bodyPanelBuilder"

			// Show printedIdetnifiers
			// TODO: make URLs clickable
    		for(Identifier printedIdentifier: printedIdentifiersList) {
    			if(printedIdentifier.getId()!=null) {
    				bodyPanelBuilder
    						.add(getTrimmedLabel(250, printedIdentifier.getType().getName() + ": " + printedIdentifier.getId()))
    						.xy(1, (rowIndex*2)+1);
        			bodyPanelBuilder.appendRows("$lg, pref");
    				rowIndex++;
    			}
    		}

    		// Show fixed properties
    		if(identifiable instanceof Person) {
    			if(((Person)identifiable).getRole() != null){
	    			bodyPanelBuilder
	    					.add(getTrimmedLabel(250, ResourceManager.getInstance().get("replaydh.labels.role") + ": " + ((Person)identifiable).getRole()))
	    					.xy(1, (rowIndex*2)+1);
	        		bodyPanelBuilder.appendRows("$lg, pref");
	        		rowIndex++;
    			}
    		} else if (identifiable instanceof Tool) {
    			// Do not print "Environment" (even if "tool" has it)

    		} else if (identifiable instanceof Resource) {
    			if(((Resource)identifiable).getResourceType() != null) {
	    			bodyPanelBuilder
	    					.add(getTrimmedLabel(250, ResourceManager.getInstance().get("replaydh.labels.type") + ": " + ((Resource)identifiable).getResourceType()))
	    					.xy(1, (rowIndex*2)+1);
	        		bodyPanelBuilder.appendRows("$lg, pref");
	        		rowIndex++;
    			}
    		}

    		// --- FINISH to create the collapsedViewPanel

    		// Add bodyPanelBuilder to the collapsedViewPanel
    		panel.add(bodyPanelBuilder.build());

        	return panel;
    	}


    	/**
    	 * Get expanded view of the editorElement
    	 * @return
    	 */
    	private JPanel getExpandedViewPanel() {

    		panel.removeAll();
        	panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        	// Add header panel (Separator + buttons)
        	panel.add(getHeaderPanel());

        	// Add editor as an expanded view
        	panel.add(editor.getEditorComponent());

        	return panel;
    	}

    	/**
    	 * Get collapsed or expanded view panel of the IdentifierElement
    	 * @return JPanel
    	 */
    	public JPanel getViewPanel() {
    		if(isExpanded()) {
    			return getExpandedViewPanel();
    		} else {
    			return getCollapsedViewPanel();
    		}
    	}

    	public void setExpanded(boolean expanded){
    		this.expandedView = expanded;
    	}

    	public boolean isExpanded(){
    		return this.expandedView;
    	}

		public T getIdentifiableObject() {
			return identifiable;
		}

		public JButton[] getButtons(){
			return buttons.toArray(new JButton[0]);
		}

	}
    
    /**
     * Create a button with name, icon, preferred size and action listener
     *
     * @param name
     * @param icon
     * @param preferredSize
     * @param actionListener
     * @return
     */
    private JButton createButton(String name, Icon icon, Dimension preferredSize, ActionListener actionListener){

        JButton button = new JButton();
        String toolTipText = name;

        button.setName(name);

        if(icon != null){
        	button.setIcon(icon);
        } else {
        	button.setText(name);
        }
        button.setToolTipText(toolTipText);
        button.setPreferredSize(preferredSize);
        button.addActionListener(actionListener);
        return button;
    }
    
    /**
     * Create new JLabel and trim it according to the maxWidth if needed
     * @param maxWidth
     * @param text
     * @return
     */
    public static JLabel getTrimmedLabel(int maxWidth, String text) {
    	JLabel label = new JLabel(text);
    	Dimension dim = label.getPreferredSize();
    	if(dim.width > maxWidth) {
    		label.setPreferredSize(new Dimension(maxWidth, dim.height));
    	}
    	return label;
    }
    
    private IdentifiableEditor createIdentifiableEditor(WorkflowSchema schema, Identifiable.Type type) {
    	return IdentifiableEditor.newBuilder()
    			.schema(schema)
    			.type(type)
    			.useDefaultTitleSelector(true)
    			.titleEditable(true)
    			.build();
    }
    
    /**
     * Create/update a panel of the editor. The main GUI method of the editor.
     *
     * @param editorComponent - a panel which must be the same variable
     * all the time because the panel will be always revalidated at the end.
     * That's why the best option is to use a private variable of the class "WorkflowStepUIEditor"
     *
     */
    private void updateEditorView(){

    	updatePanel(personsPanel, personEditorElementsList, personElementHeader);

    	//--- TOOL

    	updatePanel(toolPanel, toolEditorElementsList, toolElementHeader);

    	//--- INPUT RESOURCES

    	updatePanel(inputResourcesPanel, inputResourceEditorElementsList, inputResourceElementHeader);

    	//--- OUTPUT RESOURCES

    	updatePanel(outputResourcesPanel, outputResourceEditorElementsList, outputResourceElementHeader);


    	Window window = SwingUtilities.getWindowAncestor(scrollablePanel);
    	if(window!=null) {
    		window.revalidate();
    		window.repaint();
    	}

    	scrollablePanel.requestFocusInWindow(); //put focus somewhere in Window, just to remove the focus from other JTextComponents
    }
    
    /*
     * -------- Other classes and methods ------------------------
     */

    private class CategoryPanel extends JPanel {

		private static final long serialVersionUID = 1277621550940063732L;

		private final JLabel infoLabel;
    	private final JPanel contentPanel;

    	private final Role role;

    	private final Border defaultBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);
    	private final Border activeDragBorder = BorderFactory.createLineBorder(Color.green, 1);

    	private final ResourceDragController dragController;

    	CategoryPanel(Role role, JButton addButton, JButton addAutoButton) {

    		this.role = requireNonNull(role);

    		ResourceManager rm = ResourceManager.getInstance();

    		String header;
    		String info = rm.get("replaydh.ui.editor.workflowStep.labels.dragInfo");

    		switch (role) {

			case PERSON:
				header = rm.get("replaydh.ui.editor.workflowStep.labels.personsBorder");
				break;

			case TOOL:
				header = rm.get("replaydh.ui.editor.workflowStep.labels.toolsBorder");
				break;

			case INPUT:
				header = rm.get("replaydh.ui.editor.workflowStep.labels.inputBorder");
				break;

			case OUTPUT:
				header = rm.get("replaydh.ui.editor.workflowStep.labels.outputBorder");
				break;

			default:
				throw new IllegalArgumentException("Unknown entry type: "+role);
			}

    		infoLabel = new JLabel(info);
    		infoLabel.setVerticalAlignment(SwingConstants.TOP);
    		infoLabel.setVisible(false);

        	FlowLayout layout = new WrapLayout(FlowLayout.LEFT, 5, 5);
        	layout.setAlignOnBaseline(true);

        	contentPanel = new JPanel(layout);
        	contentPanel.setBorder(BorderFactory.createEmptyBorder(5, 2, 2, 2));

        	/**
        	 * <pre>
        	 * +---------------------+-----+
        	 * |TITLE                |     |
        	 * +---------------------+ BTN |
        	 * |INFO                 |     |
        	 * +---------------------+-----+
        	 * |                           |
        	 * |          CONTENT          |
        	 * |                           |
        	 * |                           |
        	 * +---------------------------+
        	 * </pre>
        	 */
    		FormBuilder.create()
    			.columns("pref:grow:fill, 3dlu, pref, 3dlu, pref")
    			.rows("pref, pref, 7dlu, pref:grow")
    			.panel(this)
    			.addSeparator(header).xy(1, 1, "fill, center")
    			.add(addAutoButton).xywh(3, 1, 1, 2, "right, top")
    			.add(addButton).xywh(5, 1, 1, 2, "right, top")
    			.add(infoLabel).xy(1, 2, "left, bottom")
    			.add(contentPanel).xyw(1, 4, 3, "fill, fill")
    			.build();

    		Mode mode = role==Role.PERSON ? Mode.URLS_ONLY : Mode.FILES_AND_URLS;

    		dragController = new ResourceDragController(mode) {

				@Override
				protected void refreshUI() {
					refresh();
				}

				@Override
				protected void handleURLDrag(URI url) {
					showResourceDialog(role, url);
				}

				@Override
				protected void handleFileDrag(List<Path> files) {

					new FileResolutionWorker(environment, files) {
						@Override
						protected void finished(boolean finishedWithErrors) {
							dragController.cleanupDrop();
							showResourceDialog(role, getFileObjects());
						};
					}.execute();
				}
			};

			dragController.install(this);

			refresh();
    	}

    	private void refresh() {
			setBorder(dragController.isDragActive() ? activeDragBorder : defaultBorder);
			infoLabel.setVisible(dragController.isDragActive());
    	}

    	/**
    	 * Returns the panel where all the actual components for identifiables
    	 * should be placed.
		 */
		public JPanel getContentPanel() {
			return contentPanel;
		}

		/**
		 * @return the role
		 */
		public Role getRole() {
			return role;
		}
    }
    
    private <O extends Identifiable> void updatePanel(CategoryPanel panel, List<IdentifiableEditorElement<O>> elements, String header) {

    	JPanel contentPanel = panel.getContentPanel();
    	contentPanel.removeAll();

    	for(int i=0; i<elements.size(); i++) {
    		IdentifiableEditorElement<O> element = elements.get(i);
    		element.setHeaderString(header+" "+String.valueOf(i+1));
    		contentPanel.add(element.getViewPanel());
    	}

    	panel.revalidate();
    }
    
    private void showResourceDialog(Role role, List<LocalFileObject> files) {
		IdentifiableEditor editor = createIdentifiableEditor(schema, role.asIdentifiableType());
		Map<Resource,Path> resources = WorkflowUIUtils.extractResources(files, role.asIdentifiableType());
		WorkflowUIUtils.showFileResourceDialog(editor, role, resources);

		if(resources.isEmpty()) {
			return;
		}

		resources.keySet().forEach(r -> addIdentifiable(r, role));
	}

	private void showResourceDialog(Role role, URI uri) {
		//TODO implement actual dialog
		GuiUtils.showDefaultInfo(null, "Dialog for describing URL resource");
	}
}

package bwfdm.replaydh.ui.workflow.auto;

import static java.util.Objects.requireNonNull;

import java.awt.Color;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;
import com.jgoodies.forms.layout.FormLayout;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.io.LocalFileObject;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.workflow.IdentifiableEditor;
import bwfdm.replaydh.ui.workflow.WorkflowStepUIEditor;
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
import bwfdm.replaydh.workflow.catalog.MetadataCatalog;
import bwfdm.replaydh.workflow.catalog.MetadataCatalog.Constraint;
import bwfdm.replaydh.workflow.catalog.MetadataCatalog.QuerySettings;
import bwfdm.replaydh.workflow.catalog.MetadataCatalog.Result;
import bwfdm.replaydh.workflow.schema.IdentifierSchema;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.core.FileResolutionWorker;
import bwfdm.replaydh.ui.core.ResourceDragController;
import bwfdm.replaydh.ui.core.ResourceDragController.Mode;
import bwfdm.replaydh.ui.helper.WrapLayout;
import bwfdm.replaydh.ui.icons.IconRegistry;

/**
 * 
 * @author Florian Fritze
 *
 */
public class AutoCompletionWizardWorkflowStep implements ActionListener, DocumentListener {
	
	public AutoCompletionWizardWorkflowStep(WorkflowSchema schema, RDHEnvironment environment, WorkflowStepUIEditor wfseditor) {
		this.schema=schema;
		this.environment=environment;
		this.wfseditor=wfseditor;
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
    	
    	//scrollablePanel = new ScrollablePanel();
    	//scrollablePanel.setScrollableWidth(ScrollableSizeHint.FIT);
    	objectsPanel = new JPanel();
    	
    	search = this.environment.getClient().getMetadataCatalog();
    	
    	collectionEntries = new HashMap<>();
    	//search.createObjects();
	}

	private MetadataCatalog search;
	
	private JDialog wizardWindow;
	
	private JScrollPane scrollPaneObjects;
	
	private ResourceManager rm = ResourceManager.getInstance();
	
	private Map<String, List<GUIElementMetadata>> elementsofproperty;
	private Map<String, JPanel> propertypanels;
	private List<String> listofkeys;
	
	private JPanel mainPanelWizard;
	private List<GUIElementMetadata> dd = new ArrayList<>();
	
	private Map<String, String> collectionEntries;
	
	private GUIElement simpleSearch;
	
	private JComboBox<String> ddKeys = new JComboBox<>();
	
	private RDHEnvironment environment;
	private WorkflowSchema schema;
	
	private Identifiable.Type type = null;
	private Identifiable.Role role = null;
	
	private GUIElementMetadata resetButton = null;
	private GUIElementMetadata searchButton = null;
	
	private FormBuilder builderWizard;
	private FormBuilder panelWizard;
	
	// Scrollable panel with all persons/tool/resources
    private JPanel objectsPanel;
    
    private WorkflowStepUIEditor wfseditor = null;
	
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
    
	public void createWizard(WorkflowSchema schema, Identifiable.Role role, Identifiable.Type type) {
		this.type=type;
		this.role=role;
		wizardWindow = new JDialog();
		wizardWindow.setModalityType(ModalityType.APPLICATION_MODAL);
		mainPanelWizard=this.createWizardPanel();
		wizardWindow.add(mainPanelWizard);
		scrollPaneObjects.setVisible(false);
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
		
		simpleSearch = createGUIElement();
		simpleSearch.getLabel().setText(rm.get("replaydh.wizard.metadataAutoWizard.simpleSearch"));
		simpleSearch.getButton().setVisible(false);
		simpleSearch.getMinusbutton().setVisible(false);
		simpleSearch.getTextfield().getDocument().addDocumentListener(this);
		
		GUIElementMetadata chooseProperties = createGUIElement("keys");
		chooseProperties.getTextfield().getDocument().addDocumentListener(this);
		ddKeys = new JComboBox();
		for (MetadataKeys value : MetadataKeys.values()) {
			String item = value.getDisplayLabel(value.getLocaString());
			collectionEntries.put(item, value.getKey());
			ddKeys.addItem(item);
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
		
		panelWizard = FormBuilder.create();
    	panelWizard.columns("pref:grow");
    	panelWizard.rows("pref, $nlg, pref, $nlg, pref, $nlg, pref");
    	panelWizard.padding(Paddings.DLU4);
    	panelWizard.add(personsPanel).xy(1, 1);
    	panelWizard.add(toolPanel).xy(1, 3);
    	panelWizard.add(inputResourcesPanel).xy(1, 5);
    	panelWizard.add(outputResourcesPanel).xy(1, 7);
    	objectsPanel=panelWizard.build();
		
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
		scrollPaneObjects = new JScrollPane(objectsPanel,ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,  
				   ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPaneObjects.setPreferredSize(new Dimension(700,400));
		builderWizard.add(scrollPaneObjects).xy(1, 9);
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
			elementsofproperty.get("defaultdd").get(0).getTextfield().setText("");
			simpleSearch.getTextfield().setText("");
			sortedPersons.clear();
			sortedTools.clear();
			sortedInputs.clear();
			sortedOutputs.clear();
			updateIdentifiableEditorElements();
			updateEditorView();
			scrollPaneObjects.setVisible(false);
		}
		if (source == searchButton.getExtraButton()) {
			MetadataKeys keys;
			boolean empty=true;
			List<Constraint> constraints = new ArrayList<>();
			for(Iterator<GUIElementMetadata> elements = elementsofproperty.get("defaultdd").iterator();elements.hasNext();) {
				GUIElementMetadata element=elements.next();
				if(!(element.getTextfield().getText().isEmpty())) {
					empty=false;
					Constraint constraint;
					String key = collectionEntries.get(element.getKeysDropdown().getSelectedItem().toString());
					constraint = new Constraint(key, element.getTextfield().getText());
					constraints.add(constraint);
				}
			}
			if(empty == false) {
				QuerySettings settings = new QuerySettings();
				settings.setSchema(schema);
				this.searchWithConstraints(settings, constraints, this);
			}
			if((!(simpleSearch.getTextfield().getText().isEmpty())) && (empty == true)) {
				QuerySettings settings = new QuerySettings();
				settings.setSchema(schema);
				this.globalSearch(settings, simpleSearch.getTextfield().getText(), this);
			}
		}
		
		List<IdentifiableEditorElement> collectedEditorElements = new ArrayList<>();
        collectedEditorElements.addAll(personEditorElementsList);
        collectedEditorElements.addAll(inputResourceEditorElementsList);
        collectedEditorElements.addAll(outputResourceEditorElementsList);
        collectedEditorElements.addAll(toolEditorElementsList);

        for (IdentifiableEditorElement<Identifiable> element: collectedEditorElements){
        	if (Arrays.asList(element.getButtons()).contains(source)){
        		// Analyze the pushed button
                for (JButton btn : element.getButtons()){
            		if (btn == source){

            			// Remove identifiable
            			if ((btn.getName() == toolTipRemovePerson)
                    			|| (btn.getName() == toolTipRemoveInputResource)
                    			|| (btn.getName() == toolTipRemoveOutputResource)
                    			|| (btn.getName() == toolTipRemoveTool)){
                    		processRemoveButtonPush(element, btn);

                    	// Expanded view
            			} else if (btn.getName() == toolTipExpandedView){
                    		processExpandedViewButtonPush(element);

                    	// Collapsed view
            			} else if (btn.getName() == toolTipCollapsedView){
                        	processCollapsedViewButtonPush(element);
                    	} else if ((btn.getName() == toolTipAddPerson)
                    			|| (btn.getName() == toolTipAddInputResource)
                    			|| (btn.getName() == toolTipAddOutputResource)
                    			|| (btn.getName() == toolTipAddTool)){
                    		processAddButtonPush(element, btn);
            			}
            		}
            	}
        		break;
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
			
			for (MetadataKeys value : MetadataKeys.values()) {
				String item = value.getDisplayLabel(value.getLocaString());
				oneguielement.getKeysDropdown().addItem(item);
			}
			
			oneguielement.getKeysDropdown().addActionListener(this);
			
			oneguielement.getTextfield().getDocument().addDocumentListener(this);

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
		simpleSearch.getTextfield().setEnabled(true);
		int size = elementsofproperty.get("defaultdd").size();
		for (int i=0; i < size; i++) {
			if(!(elementsofproperty.get("defaultdd").get(i).getTextfield().getText().isEmpty())) {
				simpleSearch.getTextfield().setEnabled(false);
				break;
			}
		}
		refreshPanel(metadatapropertyname);
	}
	
	
	public boolean isPersonEditor() {
		return type==Type.PERSON;
	}
	
	
	public enum MetadataKeys {
		TITLE_KEY("title","replaydh.wizard.metadataAutoWizard.title"),
		DESCRIPTION_KEY("description","replaydh.wizard.metadataAutoWizard.description"),
		ROLE_KEY("role","replaydh.wizard.metadataAutoWizard.role"),
		TYPE_KEY("type","replaydh.wizard.metadataAutoWizard.type"),
		ENVIRONMENT_KEY("environment","replaydh.wizard.metadataAutoWizard.environment"),
		PARAMETERS_KEY("parameters","replaydh.wizard.metadataAutoWizard.parameters");
		
		String key;
		String locaString;
		
		private MetadataKeys(String key, String locaString) {
	        this.key = key;
	        this.locaString=locaString;
	    }

	    public String getLocaString() {
	        return locaString;
	    }
	    
	    public String getKey() {
	    	return key;
	    }
		
		public String getDisplayLabel(String key) {
			return ResourceManager.getInstance().get(key);
		}
	}
	
	
	private void searchWithConstraints(QuerySettings settings, List<Constraint> constraints, AutoCompletionWizardWorkflowStep wizard) {
		
		SwingWorker<Boolean, Object> worker = new SwingWorker<Boolean, Object>(){

			boolean success=true;
			Result results = null;
			@Override
			protected Boolean doInBackground() throws Exception {
				results=search.query(settings, constraints);
				if(results.isEmpty()) {
					success=false;
				}
				System.out.println("Defined!");
				return success;
			}
			@Override
			protected void done() {
				if(success) {
					for(Identifiable result : results) {
						switch (role) {
						case PERSON:
							toolPanel.setVisible(false);
							inputResourcesPanel.setVisible(false);
							outputResourcesPanel.setVisible(false);
							if(result.getType().equals(Type.PERSON)) {
								wizard.addIdentifiable(result, Role.PERSON);
							}
							break;

						case TOOL:
							personsPanel.setVisible(false);
							inputResourcesPanel.setVisible(false);
							outputResourcesPanel.setVisible(false);
							if(result.getType().equals(Type.TOOL)) {
								wizard.addIdentifiable(result, Role.TOOL);
							}
							break;

						case INPUT:
							toolPanel.setVisible(false);
							personsPanel.setVisible(false);
							outputResourcesPanel.setVisible(false);
							if(result.getType().equals(Type.RESOURCE)) {
								wizard.addIdentifiable(result, Role.INPUT);
							}
							break;

						case OUTPUT:
							toolPanel.setVisible(false);
							personsPanel.setVisible(false);
							inputResourcesPanel.setVisible(false);
							if(result.getType().equals(Type.RESOURCE)) {
								wizard.addIdentifiable(result, Role.OUTPUT);
							}
							break; 
						}
					}
				}
			}
		};
		worker.execute();
	}
	
	private void globalSearch(QuerySettings settings, String fragment, AutoCompletionWizardWorkflowStep wizard) {

		SwingWorker<Boolean, Object> worker = new SwingWorker<Boolean, Object>() {

			boolean success = true;
			Result results = null;

			@Override
			protected Boolean doInBackground() throws Exception {
				results = search.query(settings, fragment);
				if (results.isEmpty()) {
					success = false;
				}
				System.out.println("Global!");
				return success;
			}

			@Override
			protected void done() {
				if(success) {
					for(Identifiable result : results) {
						switch (role) {
						case PERSON:
							toolPanel.setVisible(false);
							inputResourcesPanel.setVisible(false);
							outputResourcesPanel.setVisible(false);
							if(result.getType().equals(Type.PERSON)) {
								wizard.addIdentifiable(result, Role.PERSON);
							}
							break;

						case TOOL:
							personsPanel.setVisible(false);
							inputResourcesPanel.setVisible(false);
							outputResourcesPanel.setVisible(false);
							if(result.getType().equals(Type.TOOL)) {
								wizard.addIdentifiable(result, Role.TOOL);
							}
							break;

						case INPUT:
							toolPanel.setVisible(false);
							personsPanel.setVisible(false);
							outputResourcesPanel.setVisible(false);
							if(result.getType().equals(Type.RESOURCE)) {
								wizard.addIdentifiable(result, Role.INPUT);
							}
							break;

						case OUTPUT:
							toolPanel.setVisible(false);
							personsPanel.setVisible(false);
							inputResourcesPanel.setVisible(false);
							if(result.getType().equals(Type.RESOURCE)) {
								wizard.addIdentifiable(result, Role.OUTPUT);
							}
							break; 
						}
					}
				}
			}
		};
		worker.execute();
	}
		
	
	private void addIdentifiable(Identifiable identifiable, Role role) {
		boolean found=false;
		switch (role) {
		case PERSON:
			toolPanel.setVisible(false);
			if(!(sortedPersons.isEmpty())) {
				for(Identifiable object : sortedPersons) {
					if(object.equals(identifiable)) {
						found=true;
					}
				}
			}
			if (found == false) {
	        	sortedPersons.add((Person) identifiable);
			}
			break;

		case TOOL:
			personsPanel.setVisible(false);
			if(!(sortedTools.isEmpty())) {
				for(Identifiable object : sortedTools) {
					if(object.equals(identifiable)) {
						found=true;
					}
				}
			}
			if (found == false) {
				sortedTools.add((Tool) identifiable);
			}
			break;

		case INPUT:
			if(!(sortedInputs.isEmpty())) {
				for(Identifiable object : sortedInputs) {
					if(object.equals(identifiable)) {
						found=true;
					}
				}
			}
			if (found == false) {
				sortedInputs.add((Resource) identifiable);
			}
			break;

		case OUTPUT:
			if(!(sortedOutputs.isEmpty())) {
				for(Identifiable object : sortedOutputs) {
					if(object.equals(identifiable)) {
						found=true;
					}
				}
			}
			if (found == false) {
				sortedOutputs.add((Resource) identifiable);
			}
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
							createButton(toolTipAddPerson, iconAdd, buttonPreferredSize, actionListenerIdentifiableEditorElement), //"add" button
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
							createButton(toolTipAddInputResource, iconAdd, buttonPreferredSize, actionListenerIdentifiableEditorElement), //"add" button
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
							createButton(toolTipAddOutputResource, iconAdd, buttonPreferredSize, actionListenerIdentifiableEditorElement), //"add" button
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
								createButton(toolTipAddTool, iconAdd, buttonPreferredSize, actionListenerIdentifiableEditorElement), //"add" button
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


    	/*Window window = SwingUtilities.getWindowAncestor(scrollablePanel);
    	if(window!=null) {
    		window.revalidate();
    		window.repaint();
    	}
    	scrollablePanel.requestFocusInWindow(); //put focus somewhere in Window, just to remove the focus from other JTextComponents*/
    	
    	
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
    	scrollPaneObjects.setVisible(true);
    	objectsPanel.repaint();
    	objectsPanel.revalidate();
    	Window parentComponent = (Window) SwingUtilities.getAncestorOfClass(Window.class, mainPanelWizard);
		if (parentComponent != null) {
			parentComponent.pack();
		}
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
	
	/**
     * Process "remove button"
     */
    private <T extends Identifiable> void processRemoveButtonPush(IdentifiableEditorElement<T> element, JButton removeButton){

    	if(JOptionPane.showOptionDialog(wizardWindow,
    			ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.dialogs.removeElement.message"),
    			ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.dialogs.removeElement.title"),
    			JOptionPane.YES_NO_OPTION,
    			JOptionPane.PLAIN_MESSAGE,
    			null,
    			new String[]{ResourceManager.getInstance().get("replaydh.labels.yes"), ResourceManager.getInstance().get("replaydh.labels.no")},
    			"Default") == JOptionPane.YES_OPTION){

	    	if(removeButton.getName() == toolTipRemovePerson){
				//applyLocalEditors(personEditorElementsList); //apply local editors
				//workflowStepEditable.removePerson((Person)element.getIdentifiableObject());
	    		sortedPersons.remove((Person)element.getIdentifiableObject());
	    	} else if (removeButton.getName() == toolTipRemoveTool){
				//applyLocalEditors(toolEditorElementsList); //apply local editors
				//workflowStepEditable.setTool(null);
				sortedTools.remove((Tool)element.getIdentifiableObject());
			} else if (removeButton.getName() == toolTipRemoveInputResource){
				//applyLocalEditors(inputResourceEditorElementsList); //apply local editors
				//workflowStepEditable.removeInput((Resource)element.getIdentifiableObject());
				sortedInputs.remove((Resource)element.getIdentifiableObject());
			} else if (removeButton.getName() == toolTipRemoveOutputResource){
				//applyLocalEditors(outputResourceEditorElementsList); //apply local editors
				//workflowStepEditable.removeOutput((Resource)element.getIdentifiableObject());
				sortedOutputs.remove((Resource)element.getIdentifiableObject());
			}

	    	//contentChanged = true;

	    	updateIdentifiableEditorElements();
			updateEditorView();
    	}
    }
    
    /**
     * Process "add button"
     */
    private <T extends Identifiable> void processAddButtonPush(IdentifiableEditorElement<T> element, JButton addButton){

		if (addButton.getName() == toolTipAddPerson) {
			// applyLocalEditors(personEditorElementsList); //apply local editors
			// workflowStepEditable.removePerson((Person)element.getIdentifiableObject());
			//sortedPersons.remove((Person) element.getIdentifiableObject());
			this.wfseditor.addIdentifiable((Person) element.getIdentifiableObject(), role);
		} else if (addButton.getName() == toolTipAddTool) {
			// applyLocalEditors(toolEditorElementsList); //apply local editors
			// workflowStepEditable.setTool(null);
			//sortedTools.remove((Tool) element.getIdentifiableObject());
			this.wfseditor.addIdentifiable((Tool) element.getIdentifiableObject(), role);
		} else if (addButton.getName() == toolTipAddInputResource) {
			// applyLocalEditors(inputResourceEditorElementsList); //apply local editors
			// workflowStepEditable.removeInput((Resource)element.getIdentifiableObject());
			//sortedInputs.remove((Resource) element.getIdentifiableObject());
			this.wfseditor.addIdentifiable((Resource) element.getIdentifiableObject(), role);
		} else if (addButton.getName() == toolTipAddOutputResource) {
			// applyLocalEditors(outputResourceEditorElementsList); //apply local editors
			// workflowStepEditable.removeOutput((Resource)element.getIdentifiableObject());
			//sortedOutputs.remove((Resource) element.getIdentifiableObject());
			this.wfseditor.addIdentifiable((Resource) element.getIdentifiableObject(), role);
		}

		// contentChanged = true;
		this.wfseditor.updateIdentifiableEditorElements(this.wfseditor.getEditingItem());
		this.wfseditor.updateEditorView();
    }
    
    /**
     * Process "expanded view button"
     */
    private <T extends Identifiable> void processExpandedViewButtonPush(IdentifiableEditorElement<T> element){

    	element.setExpanded(true);

    	// Change button "show more elements" to "show less elements"
    	for(JButton btn: element.getButtons()){
    		if(btn.getName() == toolTipExpandedView){
    			btn.setName(toolTipCollapsedView);
    			btn.setToolTipText(toolTipCollapsedView);
    			btn.setIcon(iconCollapsed);
    			break;
    		}
    	}
    	updateEditorView();
    }


    /**
     * Process "collapsed view button"
     */
    private <T extends Identifiable> void processCollapsedViewButtonPush(IdentifiableEditorElement<T> element){

    	element.setExpanded(false);

    	// Change button "show more elements" to "show less elements"
    	for(JButton btn: element.getButtons()){
    		if(btn.getName() == toolTipCollapsedView){
    			btn.setName(toolTipExpandedView);
    			btn.setToolTipText(toolTipExpandedView);
    			btn.setIcon(iconExpanded);
    			break;
    		}
    	}
    	updateEditorView();
	}

	@Override
	public void insertUpdate(DocumentEvent e) {
		Object source = e.getDocument();
		int size = elementsofproperty.get("defaultdd").size();
		if(simpleSearch.getTextfield().getDocument() == source) {
			if(!(simpleSearch.getTextfield().getText().isEmpty())) {
				for (int i=0; i < size; i++) {
					elementsofproperty.get("defaultdd").get(i).getTextfield().setEnabled(false);
				}
			} else {
				for (int i=0; i < size; i++) {
					elementsofproperty.get("defaultdd").get(i).getTextfield().setEnabled(true);
				}
			}
		} else {
			simpleSearch.getTextfield().setEnabled(true);
			for (int i=0; i < size; i++) {
				if(!(elementsofproperty.get("defaultdd").get(i).getTextfield().getText().isEmpty())) {
					simpleSearch.getTextfield().setEnabled(false);
					break;
				}
			}
		}
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		// TODO Auto-generated method stub
		Object source = e.getDocument();
		int size = elementsofproperty.get("defaultdd").size();
		if(simpleSearch.getTextfield().getDocument() == source) {
			if(!(simpleSearch.getTextfield().getText().isEmpty())) {
				for (int i=0; i < size; i++) {
					elementsofproperty.get("defaultdd").get(i).getTextfield().setEnabled(false);
				}
			} else {
				for (int i=0; i < size; i++) {
					elementsofproperty.get("defaultdd").get(i).getTextfield().setEnabled(true);
				}
			}
		} else {
			simpleSearch.getTextfield().setEnabled(true);
			for (int i=0; i < size; i++) {
				if(!(elementsofproperty.get("defaultdd").get(i).getTextfield().getText().isEmpty())) {
					simpleSearch.getTextfield().setEnabled(false);
					break;
				}
			}
		}
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		// TODO Auto-generated method stub
		
	}
}

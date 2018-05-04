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
package bwfdm.replaydh.ui.workflow;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.Editor;
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Person;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.Tool;
import bwfdm.replaydh.workflow.WorkflowStep;
import bwfdm.replaydh.workflow.impl.DefaultPerson;
import bwfdm.replaydh.workflow.impl.DefaultResource;
import bwfdm.replaydh.workflow.impl.DefaultTool;
import bwfdm.replaydh.workflow.schema.IdentifierType;

/**
 * An editor for the workflow step properties
 *
 * @author Volodymyr Kushnarenko
 *
 */
public class WorkflowStepUIEditorOld implements Editor<WorkflowStep>{

    private final String titleKey = "title";
    private final String descriptionKey = "description";

    Dimension buttonPreferredSize = new Dimension(16,16);
    Dimension buttonBigPreferredSize = new Dimension(32,32);
    
    //Icons
    private Icon iconRemove = IconRegistry.getGlobalRegistry().getIcon("list-remove-5.png");
    private Icon iconAdd = IconRegistry.getGlobalRegistry().getIcon("list-add.png");
    private Icon iconExpanded = IconRegistry.getGlobalRegistry().getIcon("right.png");
    private Icon iconCollapsed = IconRegistry.getGlobalRegistry().getIcon("left.png");
    private Icon iconEdit = IconRegistry.getGlobalRegistry().getIcon("list-edit.png");

    //Button names
    private final String buttonNameAdd = "+";
    private final String buttonNameAddIdentifier = "add identifier";
    private final String buttonNameRemoveIdentifier = "remove identifier";

    private final String buttonNameRemovePerson = "remove person";
    private final String buttonNameRemoveInputResource = "remove input resource";
    private final String buttonNameRemoveOutputResource = "remove output resource";
    private final String buttonNameRemoveTool = "remove tool";

    private final String buttonNameExpandedView = "show expanded view";
    private final String buttonNameCollapsedView = "show collapsed view";
    private final String buttonNameEdit = "edit";

    private final String buttonNameAddPerson = "add person";
    private final String buttonNameAddInputResource = "add input resource";
    private final String buttonNameAddOutputResource = "add output resorce";
    private final String buttonNameAddTool = "add tool";

    //Add buttons
    private JButton btnAddPerson = null;
    private JButton btnAddInputResource = null;
    private JButton btnAddOutputResource = null;
    private JButton btnAddTool = null;

    //Set of persons/tool/resources
    Set<Person> persons;
    Set<Resource> input;
    Set<Resource> output;
    Set<Resource> tools;

    //TODO: think about sorting
    List<Person> sortedPersons;
    List<Resource> sortedInputs;
    List<Resource> sortedOutputs;
    List<Resource> sortedTools;

    //Workflow step
    private WorkflowStep workflowStep;
    private WorkflowStep workflowStepOrig;

    //Editor panel, the main element, should be always returned as the same element.
    private JPanel editorPanel;

    //Scrollable panel with all persons/tool/resources
    private JPanel scrollablePanel;

    //Panels with all persons, tool, input/output resources
    private JPanel personTabPanel;
    private JPanel toolTabPanel;
    private JPanel inputResourceTabPanel;
    private JPanel outputResourceTabPanel;

    private PropertyLine titlePropertyLine;
    private PropertyLine descriptionPropertyLine;
    private List<PropertyGroupElement<Person>> personPropertyGroupList;
    private List<PropertyGroupElement<Tool>> toolPropertyGroupList;
    private List<PropertyGroupElement<Resource>> inputResourcePropertyGroupList;
    private List<PropertyGroupElement<Resource>> outputResourcePropertyGroupList;

    private Border emptyBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
    private Border lineBorder = BorderFactory.createLineBorder(Color.BLACK, 1, false);
    private FlowLayout flowLayoutLeft = new FlowLayout(FlowLayout.LEFT);


    private final String layoutColumnsCollapsed = ""
    									+ "left:pref";

    private final String layoutColumn = ""
    									+ "left:pref, " //labelComponent
								    	+ "3dlu, "
								    	+ "pref:grow, "	//propertyComponent
								    	+ "3dlu, "
								    	+ "left:pref, " //button 1
								    	+ "3dlu, "
								    	+ "left:pref, " //button 2
								    	+ "3dlu, "
								    	+ "left:pref, " //button 3
								    	+ "3dlu, "
								    	+ "left:pref"; //button 4
    private final String layoutRows = "pref";

    private ActionListener actionListenerGroupElement;		//for group buttons: remove, edit, more/less
    private ActionListener actionListenerAddIdentifiable;	//for "add new identifiable" button

    private final String defaultInputString = "<default value>"; //"<Please change it>"

    private final List<String> illegalInput = new ArrayList<String>(Arrays.asList(
										    		defaultInputString,
										    		"",
										    		"<Unnamed step>")
										    		);

    //Enum for the person standard properties
    private enum PersonStandardProperties{
//    	NAME(DefaultPersonIdentifierType.NAME.getLabel()),
//    	FIRSTNAME_LASTNAME(DefaultPersonIdentifierType.FIRSTNAME_LASTNAME.getLabel()),
//    	ALIAS(DefaultPersonIdentifierType.ALIAS.getLabel()),
    	ROLE("role"),
    	DESCRIPTION("description");
    	private String label;

    	private PersonStandardProperties(String propertyName){
    		this.label = propertyName;
    	}

    	public String getLabel(){
    		return this.label;
    	}
    }

    //Enum for the resource standard properties
    private enum ResourceStandardProperties{
//    	NAME_VERSION(DefaultResourceIdentifierType.NAME_VERSION.getLabel()),
    	RESOURCETYPE("type"),
    	DESCRIPTION("description");
//    	PATH(DefaultResourceIdentifierType.PATH.getLabel()),
//    	URL(DefaultResourceIdentifierType.URL.getLabel());

    	private String label;

    	private ResourceStandardProperties(String propertyName){
    		this.label = propertyName;
    	}

    	public String getLabel(){
    		return this.label;
    	}
    }

    // Enum for the tool standard properties
    private enum ToolStandardProperties{
//    	NAME_VERSION(DefaultResourceIdentifierType.NAME_VERSION.getLabel()),
    	DESCRIPTION("description"),
    	PARAMETERS("parameters"),
    	ENVIRONMENT("environment");

    	private String label;

    	private ToolStandardProperties(String propertyName){
    		this.label = propertyName;
    	}

    	public String getLabel(){
    		return this.label;
    	}
    }

    // Enum for the tool standard properties
    private enum IgnoredIdentifiers{
//    	RDH_ID(DefaultResourceIdentifierType.RDH_ID.getLabel()),
//    	CHECKSUM(DefaultResourceIdentifierType.CHECKSUM.getLabel());
    	//for test
    	ENVIRONMENT("environment");

    	private String label;

    	private IgnoredIdentifiers(String identifierName){
    		this.label = identifierName;
    	}

    	public String getLabel(){
    		return this.label;
    	}
    }


    /**
     * Constructor, WorkflowStepUIEditor
     */
    public WorkflowStepUIEditorOld(){

    	// Create Lists of PropertyGroupElements
      	personPropertyGroupList = new ArrayList<PropertyGroupElement<Person>>();
    	inputResourcePropertyGroupList = new ArrayList<PropertyGroupElement<Resource>>();
    	outputResourcePropertyGroupList = new ArrayList<PropertyGroupElement<Resource>>();
    	toolPropertyGroupList = new ArrayList<PropertyGroupElement<Tool>>();

    	editorPanel = new JPanel();

    	personTabPanel = new JPanel();
    	toolTabPanel = new JPanel();
    	inputResourceTabPanel = new JPanel();
    	outputResourceTabPanel = new JPanel();

    	scrollablePanel = new JPanel();

    	//General action listener for each propertyGroupElement
        actionListenerGroupElement = new ActionListener() {

        	@Override
            public void actionPerformed(ActionEvent e) {
                Object source = e.getSource();
                //Create a List of all propertyGroups to iterate later only through 1 list
                List<PropertyGroupElement> collectedGroups = new ArrayList<>();
                collectedGroups.addAll(personPropertyGroupList);
                collectedGroups.addAll(inputResourcePropertyGroupList);
                collectedGroups.addAll(outputResourcePropertyGroupList);
                collectedGroups.addAll(toolPropertyGroupList);

                for (PropertyGroupElement<Identifiable> element: collectedGroups){
                	if (Arrays.asList(element.getButtons()).contains(source)){
                		//Analyze the pushed button
                        for (JButton btn : element.getButtons()){
                    		if (btn == source){
                           		if ((btn.getName() == buttonNameRemovePerson)
                            			|| (btn.getName() == buttonNameRemoveInputResource)
                            			|| (btn.getName() == buttonNameRemoveOutputResource)
                            			|| (btn.getName() == buttonNameRemoveTool)){
                            		processRemoveButtonPush(element, btn);
                            	} else if (btn.getName() == buttonNameExpandedView){
                            		processExpandedViewButtonPush(element);
                            	} else if (btn.getName() == buttonNameCollapsedView){
                                	processCollapsedViewButtonPush(element);
                            	}
                    		}
                    	}
                		break;
                	}

                	//Button "add identifier"
                	if(element.getButtonAddIdentifier() == source){
                		processAddIdentifierButtonPush(element);
                		break;
                	}

                	//Button "remove identifier"
                	//PropertyLine customIdentifierLineToRemove = null;
                	boolean toRemove = false;
                	for(PropertyLine propertyLine: element.getCustomIdentifierLinesView()){
                		for(JButton btn: propertyLine.getButtons()){
                			if (btn==source){
                				toRemove = true;
                				processRemoveIdentifierButtonPush(element, propertyLine);
                				break;
                			}
                		}
                		if (toRemove){
                			break;
                		}
                	}

                }
            }//actionPerformed
        };//ActionListener

        //Add new identifiable (Person/Resource/Tool)
        actionListenerAddIdentifiable = new ActionListener(){

        	@Override
			public void actionPerformed(ActionEvent e) {

				Object source = e.getSource();

				if(source == btnAddPerson){
					addIdentifiableToPropertyGroupList(personPropertyGroupList,createNewPerson(),btnAddPerson);
				} else if (source == btnAddTool){
					addIdentifiableToPropertyGroupList(toolPropertyGroupList,createNewTool(),btnAddTool);
				} else if (source == btnAddInputResource){
					addIdentifiableToPropertyGroupList(inputResourcePropertyGroupList,createNewResource(),btnAddInputResource);
				} else if (source == btnAddOutputResource){
					addIdentifiableToPropertyGroupList(outputResourcePropertyGroupList,createNewResource(),btnAddOutputResource);
				}

				updateEditorView(editorPanel);

			}//actionPerformed
        };//ActionListener

        // Buttons to add new identifiable
        btnAddPerson = createButton(buttonNameAddPerson,iconAdd,buttonBigPreferredSize,actionListenerAddIdentifiable);
    	btnAddTool = createButton(buttonNameAddTool,iconAdd,buttonBigPreferredSize,actionListenerAddIdentifiable);
    	btnAddInputResource = createButton(buttonNameAddInputResource,iconAdd,buttonBigPreferredSize,actionListenerAddIdentifiable);
    	btnAddOutputResource = createButton(buttonNameAddOutputResource,iconAdd,buttonBigPreferredSize,actionListenerAddIdentifiable);

    }


    /**
     * Get viewport position of the scrollPane. Will be used for updating of the editor panel.
     *
     * @param panel
     * @return
     */
    private Point getViewportPosition(JPanel panel){

    	Point viewportPosition = new Point(0, 0);
    	for(Component compPanel : panel.getComponents()){
    		if (compPanel instanceof JPanel){
    			for(Component compScroll: ((JPanel) compPanel).getComponents()){
	    			if(compScroll instanceof JScrollPane){
	        			viewportPosition = ((JScrollPane) compScroll).getViewport().getViewPosition();
	        		}
    			}
    		}
    	}
    	return viewportPosition;
    }

    /**
     * Set viewport position. Used to restore the viewport position of the scroll pane after update of the panel.
     *
     * @param panel
     * @param viewportPosition
     */
    private void setViewportPosition(JPanel panel, Point viewportPosition){
    	for(Component compPanel : panel.getComponents()){
    		if (compPanel instanceof JPanel){
    			for(Component compScroll: ((JPanel) compPanel).getComponents()){
	    			if(compScroll instanceof JScrollPane){
	        			((JScrollPane) compScroll).getViewport().setViewPosition(viewportPosition);
	        		}
    			}
    		}
    	}
    }

    /**
     * Get a JPanel with collapsed view of the PropertyGroupElement
     *
     * @param groupElement - PropertyGroupElement, which must be "collapsed"
     * @param elementTypeName - a border name of the PropertyGroupElement
     * @return JPanel
     */
    private <T extends Identifiable> JPanel getCollapsedViewEditorPanel(PropertyGroupElement<T> groupElement, String elementTypeName){

    	JPanel elementPanel;
    	FormBuilder groupBuilder = FormBuilder.create()
								.columns(layoutColumn)
								.rows("pref");
    	HashMap<String, String> collectedProperties = groupElement.getPropertiesAndIdentifiersViewAsMap();

    	int rowIndex = 0;

    	//Add buttons (little bit hacky, combination of separator and header)
    	groupBuilder.addSeparator(elementTypeName).xyw(1, 1, 3);
    	rowIndex = addHeaderToFormBuilder("", groupBuilder, rowIndex, groupElement.getButtons());
    	//Separate header via spaces
    	for (int i=0; i<1; i++) {
    		rowIndex++;
        	groupBuilder.appendRows("$lg, pref").appendRows("$lg, pref");
    	}

    	//--- Person
    	if (groupElement.getIdentifiableObject() instanceof Person){

//UNCOMMENT    		
    		//Add Name or Firstname-Lastname or Alias
//        	if(collectedProperties.containsKey(PersonStandardProperties.NAME.getLabel())){
//        		groupBuilder.add(new JLabel(PersonStandardProperties.NAME.getLabel() + ": " + collectedProperties.get(PersonStandardProperties.NAME.getLabel()))).xy(1, (rowIndex*2)+1);
//        		groupBuilder.appendRows("$lg, pref");	rowIndex++;
//        	} else if (collectedProperties.containsKey(PersonStandardProperties.FIRSTNAME_LASTNAME.getLabel())){
//        		groupBuilder.add(new JLabel(PersonStandardProperties.FIRSTNAME_LASTNAME.getLabel() + ": " + collectedProperties.get(PersonStandardProperties.FIRSTNAME_LASTNAME.getLabel()))).xy(1, (rowIndex*2)+1);
//        		groupBuilder.appendRows("$lg, pref"); rowIndex++;
//        	} else if (collectedProperties.containsKey(PersonStandardProperties.ALIAS.getLabel())){
//        		groupBuilder.add(new JLabel(PersonStandardProperties.ALIAS.getLabel() + ": " + collectedProperties.get(PersonStandardProperties.ALIAS.getLabel()))).xy(1, (rowIndex*2)+1);
//        		groupBuilder.appendRows("$lg, pref");	rowIndex++;
//        	}
    		//Add Role
    		if(collectedProperties.containsKey(PersonStandardProperties.ROLE.getLabel())){
        		groupBuilder.add(new JLabel(PersonStandardProperties.ROLE.getLabel() + ": " + collectedProperties.get(PersonStandardProperties.ROLE.getLabel()))).xy(1, (rowIndex*2)+1);
        		groupBuilder.appendRows("$lg, pref");
        		rowIndex++;
        	}
    	}

    	//--- Tool
    	else if (groupElement.getIdentifiableObject() instanceof Tool){

//UNCOMMENT 
    		//Add Name
//        	if(collectedProperties.containsKey(ToolStandardProperties.NAME_VERSION.getLabel())){
//        		groupBuilder.add(new JLabel(ToolStandardProperties.NAME_VERSION.getLabel() + ": " + collectedProperties.get(ToolStandardProperties.NAME_VERSION.getLabel()))).xy(1, (rowIndex*2)+1);
//        		groupBuilder.appendRows("$lg, pref");	rowIndex++;
//        	}
//    		//Add Path or URL
//    		if(collectedProperties.containsKey(DefaultResourceIdentifierType.PATH.getLabel())){
//        		groupBuilder.add(new JLabel(DefaultResourceIdentifierType.PATH.getLabel() + ": " + collectedProperties.get(DefaultResourceIdentifierType.PATH.getLabel()))).xy(1, (rowIndex*2)+1);
//        		groupBuilder.appendRows("$lg, pref");
//        		rowIndex++;
//        	} else if(collectedProperties.containsKey(DefaultResourceIdentifierType.URL.getLabel())){
//        		groupBuilder.add(new JLabel(DefaultResourceIdentifierType.URL.getLabel() + ": " + collectedProperties.get(DefaultResourceIdentifierType.URL.getLabel()))).xy(1, (rowIndex*2)+1);
//        		groupBuilder.appendRows("$lg, pref");
//        		rowIndex++;
//        	}
    	}

    	//--- Resource (input/output)
    	else if (groupElement.getIdentifiableObject() instanceof Resource){

//UNCOMMENT    		
    		//Add Name
//        	if(collectedProperties.containsKey(ResourceStandardProperties.NAME_VERSION.getLabel())){
//        		groupBuilder.add(new JLabel(ResourceStandardProperties.NAME_VERSION.getLabel() + ": " + collectedProperties.get(ToolStandardProperties.NAME_VERSION.getLabel()))).xy(1, (rowIndex*2)+1);
//        		groupBuilder.appendRows("$lg, pref");	rowIndex++;
//        	}
//    		//Add Path
//    		if(collectedProperties.containsKey(ResourceStandardProperties.PATH.getLabel())){
//        		groupBuilder.add(new JLabel(ResourceStandardProperties.PATH.getLabel() + ": " + collectedProperties.get(ResourceStandardProperties.PATH.getLabel()))).xy(1, (rowIndex*2)+1);
//        		groupBuilder.appendRows("$lg, pref");
//        		rowIndex++;
//        	}
    		//Add ResourceType
    		if(collectedProperties.containsKey(ResourceStandardProperties.RESOURCETYPE.getLabel())){
        		groupBuilder.add(new JLabel(ResourceStandardProperties.RESOURCETYPE.getLabel() + ": " + collectedProperties.get(ResourceStandardProperties.RESOURCETYPE.getLabel()))).xy(1, (rowIndex*2)+1);
        		groupBuilder.appendRows("$lg, pref");
        		rowIndex++;
        	}
    	}

		elementPanel = groupBuilder.build();

		//Create insets via EmptyBorder
		elementPanel.setBorder(emptyBorder);
		//elementPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), elementTypeName, TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION));

		//Set line Border
		JPanel panelLineBorder = new JPanel();
		panelLineBorder.setBorder(lineBorder);
		panelLineBorder.add(elementPanel);

    	return panelLineBorder;//elementPanel;
    }

    /**
     * Get an extended view of the PropertyGroupElement
     *
     * @param groupElement - PropertyGroupElment which should be extended
     * @param elementTypeName - name of the border
     * @return
     */
    public <T extends Identifiable> JPanel getExpandedViewEditorPanel(PropertyGroupElement<T> groupElement, String elementTypeName){

		JPanel expandedEditorPanel;

    	//Extra builder for every GroupElement element
		FormBuilder groupBuilder = FormBuilder.create()
				.columns(layoutColumn)
				.rows("pref");

		//Add standard properties
		int rowIndex = 0;

		//Add buttons (little bit hacky, combination of separator and header)
    	groupBuilder.addSeparator(elementTypeName).xyw(1, 1, 3);
    	rowIndex = addHeaderToFormBuilder("", groupBuilder, rowIndex, groupElement.getButtons());
    	//Separate header via spaces
    	for (int i=0; i<1; i++) {
    		rowIndex++;
        	groupBuilder.appendRows("$lg, pref").appendRows("$lg, pref");
    	}

    	for (PropertyLine propertyLine: groupElement.getStandardPropertyLinesView()){
    		rowIndex = addPropertyLineToFormBuilder(propertyLine, groupBuilder, rowIndex);
       	}

		//Add custom identifiers
    	groupBuilder.addSeparator("").xy(1, ((rowIndex)*2)+1).appendRows("$lg, pref"); rowIndex++;
        groupBuilder.addSeparator("Custom identifiers").xyw(1, ((rowIndex)*2)+1, 3);
        rowIndex = addHeaderToFormBuilder("",
        							groupBuilder,
        							rowIndex,
        							groupElement.getButtonAddIdentifier()
        							);
    	for (PropertyLine propertyLine: groupElement.getCustomIdentifierLinesView()){
    		rowIndex = addPropertyLineToFormBuilder(propertyLine, groupBuilder, rowIndex);
       	}

    	//Build a panel
    	expandedEditorPanel = groupBuilder.build();
    	//Create insets via EmptyBorder
    	expandedEditorPanel.setBorder(emptyBorder);
    	//expandedEditorPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), elementTypeName, TitledBorder.LEFT,TitledBorder.DEFAULT_POSITION));

		//Set line Border
		JPanel panelLineBorder = new JPanel();
		panelLineBorder.setBorder(lineBorder);
		panelLineBorder.add(expandedEditorPanel);

    	return panelLineBorder;//return expandedEditorPanel;
    }


    /**
     * Add new identifiable object to the list of propertyGroupElement-s
     * @param propertyGroupsArray - array of PropertyGroupElement-s
     * @param identifiableObject
     * @param btnAdd
     */
    private <T extends Identifiable> void  addIdentifiableToPropertyGroupList(List<PropertyGroupElement<T>> propertyGroupsArray, T identifiableObject, JButton btnAdd){

    	JButton btnRemove = null;
    	JButton btnExpandedView = createButton(buttonNameExpandedView, iconExpanded, buttonPreferredSize, actionListenerGroupElement);

    	if(btnAdd == btnAddPerson){
    		btnRemove = createButton(buttonNameRemovePerson, iconRemove, buttonPreferredSize, actionListenerGroupElement); //"remove" button
    	} else if(btnAdd == btnAddTool){
    		btnRemove = createButton(buttonNameRemoveTool, iconRemove, buttonPreferredSize, actionListenerGroupElement); //"remove" button
    	} else if(btnAdd == btnAddInputResource){
    		btnRemove = createButton(buttonNameRemoveInputResource, iconRemove, buttonPreferredSize, actionListenerGroupElement); //"remove" button
    	} else if(btnAdd == btnAddOutputResource){
    		btnRemove = createButton(buttonNameRemoveOutputResource, iconRemove, buttonPreferredSize, actionListenerGroupElement); //"remove" button
    	}

		PropertyGroupElement<T> newElement =
			new PropertyGroupElement<T>(
					"",
					identifiableObject,
					btnRemove,
					btnExpandedView
					);

		//TODO: remove, because it is better to check this by applying

//		//Check if identifiable already exists
//		boolean isNewElement = true;
//		for(PropertyGroupElement<T> groupElement: propertyGroupsArray){
//    		if(newElement.getIdentifiableObject().equals(groupElement.getIdentifiableObject())){
//    			isNewElement = false;
//    			break;
//    		}
//		}
//        if(isNewElement){
//        	propertyGroupsArray.add(newElement);
//        }

		propertyGroupsArray.add(newElement);

    }


    /**
	 * Create/Update the whole model of the editor.
	 * @param workflowstep
	 */
	private void updateEditorModel(WorkflowStep workflowStep){

		//Store property groups to know, which of them used collapsed/expanded view
        List<PropertyGroupElement<Person>> oldPersonsPropertyGroup = new ArrayList<PropertyGroupElement<Person>>(personPropertyGroupList);
        List<PropertyGroupElement<Tool>> oldToolsPropertyGroup = new ArrayList<PropertyGroupElement<Tool>>(toolPropertyGroupList);
        List<PropertyGroupElement<Resource>> oldInputResourcesPropertyGroup = new ArrayList<PropertyGroupElement<Resource>>(inputResourcePropertyGroupList);
        List<PropertyGroupElement<Resource>> oldOutputResourcesPropertyGroup = new ArrayList<PropertyGroupElement<Resource>>(outputResourcePropertyGroupList);

	    // Clear all groups (to avoid duplications)
	    this.personPropertyGroupList.clear();
	    this.inputResourcePropertyGroupList.clear();
	    this.outputResourcePropertyGroupList.clear();
	    this.toolPropertyGroupList.clear();

	    int i;

	    //Title
	    if(workflowStep.getTitle() == null){ //replace to empty string in case of null
	    	workflowStep.setTitle("");
	    }
	    this.titlePropertyLine = new PropertyLine(
	    		new JLabel(titleKey),
	    		new JTextField(workflowStep.getTitle())
	    		);

	    //Description
	    if(workflowStep.getDescription() == null){ //replace to empty string in case of null
	    	workflowStep.setDescription("");
	    }
	    this.descriptionPropertyLine = new PropertyLine(
	    		new JLabel(descriptionKey),
	    		new JScrollPane(new JTextArea(workflowStep.getDescription(), 4, 10))
	    		);

	    //Persons
	    i = 0;
	    for(Person person: workflowStep.getPersons()){
	    	i++;
	    	PropertyGroupElement<Person> personGroupElement =
	    			new PropertyGroupElement<Person>(
	    					"Person " + i,
							person,
							createButton(buttonNameRemovePerson, iconRemove, buttonPreferredSize, actionListenerGroupElement), //"remove" button
				    		createButton(buttonNameExpandedView, iconExpanded, buttonPreferredSize, actionListenerGroupElement)  //"more info" button
				    		);
	    	this.personPropertyGroupList.add(personGroupElement);
	    }

	    //Input resources
	    i = 0;
	    for(Resource inputResource: workflowStep.getInput()){
	    	i++;
	    	PropertyGroupElement<Resource> inputResourceGroupElement =
	    			new PropertyGroupElement<Resource>(
	    					"Input " + i,
							inputResource,
							createButton(buttonNameRemoveInputResource, iconRemove, buttonPreferredSize, actionListenerGroupElement), //"remove" button
				    		createButton(buttonNameExpandedView, iconExpanded, buttonPreferredSize, actionListenerGroupElement)  //"more info" button
				    		);
	    	this.inputResourcePropertyGroupList.add(inputResourceGroupElement);
	    }

	    //Output resources
	    i = 0;
	    for(Resource outputResource: workflowStep.getOutput()){
	    	i++;
	    	PropertyGroupElement<Resource> outputResourceGroupElement =
	    			new PropertyGroupElement<Resource>(
	    					"Output " + i,
							outputResource,
							createButton(buttonNameRemoveOutputResource, iconRemove, buttonPreferredSize, actionListenerGroupElement), //"remove" button
				    		createButton(buttonNameExpandedView, iconExpanded, buttonPreferredSize, actionListenerGroupElement)  //"more info" button
				    		);
	    	this.outputResourcePropertyGroupList.add(outputResourceGroupElement);
	    }

	    //Tool
	    //
	    //TODO: think about possibility to use more than 1 tool (as array) -> see persons and resources above
	    Tool tool = workflowStep.getTool();
	    if (tool != null){
	    	PropertyGroupElement<Tool> toolGroupElement =
	    			new PropertyGroupElement<Tool>(
	    					"Tool",
							tool,
							createButton(buttonNameRemoveTool, iconRemove, buttonPreferredSize, actionListenerGroupElement), //"remove" button
				    		createButton(buttonNameExpandedView, iconExpanded, buttonPreferredSize, actionListenerGroupElement) //"more info" button
				    		);
	    	this.toolPropertyGroupList.add(toolGroupElement); //as array of 1 element, for the future
	    }

	    //Restore old view mode for all propertyGroups (collapsed/expanded)
        restoreViewModeCollapsedExpanded(personPropertyGroupList,oldPersonsPropertyGroup);
        restoreViewModeCollapsedExpanded(toolPropertyGroupList, oldToolsPropertyGroup);
        restoreViewModeCollapsedExpanded(inputResourcePropertyGroupList, oldInputResourcesPropertyGroup);
        restoreViewModeCollapsedExpanded(outputResourcePropertyGroupList, oldOutputResourcesPropertyGroup);

	}


	/**
     * Create/update a panel of the editor. The main GUI method of the editor.
     *
     * @param editorPanel - a panel which must be the same variable
     * all the time because the panel will be always revalidated at the end.
     * That's why the best option is to use a private variable of the class "WorkflowStepUIEditor"
     *
     */
    private void updateEditorView(JPanel editorPanel){

    	//Variables for view
    	int index;
    	GridBagConstraints gbc = new GridBagConstraints();
    	gbc.anchor = GridBagConstraints.FIRST_LINE_START;
    	gbc.insets = new Insets(5, 5, 5, 5);

    	Border compoundBorder = BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

    	//Save a viewport position of the ScrollPane
    	Point viewportPosScrollablePanel = getViewportPosition(scrollablePanel);

    	//Remove all components from all panes and panels
    	editorPanel.removeAll();     //main panel of the editor
    	scrollablePanel.removeAll(); //panel for persons/tool/resources that has a scroll
    	personTabPanel.removeAll();
    	toolTabPanel.removeAll();
    	inputResourceTabPanel.removeAll();
    	outputResourceTabPanel.removeAll();


    	//Set BoxLayout (to put all editor components vertically)
    	editorPanel.setLayout(new BoxLayout(editorPanel, BoxLayout.Y_AXIS));

    	scrollablePanel.setLayout(new BoxLayout(scrollablePanel, BoxLayout.Y_AXIS));
    	scrollablePanel.setBorder(emptyBorder);//setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));


    	//--- Title + Description
    	FormBuilder tittleGroupBuilder = FormBuilder.create()
					    							.columns("left:pref, 3dlu, pref:grow")
					    							.rows(layoutRows);
    	addPropertyLineToFormBuilder(titlePropertyLine, tittleGroupBuilder, 0);
    	addPropertyLineToFormBuilder(descriptionPropertyLine, tittleGroupBuilder, 1);
    	JPanel tittlePanel = tittleGroupBuilder.build();
    	tittlePanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
    	editorPanel.add(tittlePanel);
    	//editorPanel.add(tittleGroupBuilder.build());


        //scrollablePanel.setBorder(BorderFactory.createTitledBorder(compoundBorder, "Persons/Tool/Resources"));

    	//editorPanel.add(separatorPanel);

    	//--- Persons

    	JPanel personGridBagPanel = new JPanel(new GridBagLayout());
    	index = 0;
    	for (PropertyGroupElement<Person> element: personPropertyGroupList){
        	index++;
    		if(!element.isExpanded()){
    			 personGridBagPanel.add(getCollapsedViewEditorPanel(element, "PERSON " + index),gbc);
    		} else {
    			 personGridBagPanel.add(getExpandedViewEditorPanel(element, "PERSON " + index),gbc);
    		}
        }
    	personGridBagPanel.add(btnAddPerson,gbc);

    	//Set flow layout and add gridbag panel to the peson panel
    	personTabPanel.setLayout(flowLayoutLeft);
    	personTabPanel.add(personGridBagPanel);


    	//--- Tool

    	JPanel toolGridBagPanel = new JPanel(new GridBagLayout());
    	index = 0;
    	if(toolPropertyGroupList.size()==0) {
    		toolGridBagPanel.add(btnAddTool,gbc);
    	} else {
	    	for (PropertyGroupElement<Tool> element: toolPropertyGroupList){
	        	index++;
	    		if(!element.isExpanded()){
	    			 toolGridBagPanel.add(getCollapsedViewEditorPanel(element, "TOOL"),gbc);
	    		} else {
	    			 toolGridBagPanel.add(getExpandedViewEditorPanel(element, "TOOL"),gbc);
	    		}
	        }
    	}

    	//Set layout and add the panel
    	toolTabPanel.setLayout(flowLayoutLeft);
    	toolTabPanel.add(toolGridBagPanel);

    	//--- Input resources

    	JPanel inputResourcesGridBagPanel = new JPanel(new GridBagLayout());
    	index = 0;
    	for (PropertyGroupElement<Resource> inputElement: inputResourcePropertyGroupList){
        	index++;
    		if(!inputElement.isExpanded()){
    			 inputResourcesGridBagPanel.add(getCollapsedViewEditorPanel(inputElement, "INPUT " + index),gbc);
    		} else {
    			 inputResourcesGridBagPanel.add(getExpandedViewEditorPanel(inputElement, "INPUT " + index),gbc);
    		}
        }
    	inputResourcesGridBagPanel.add(btnAddInputResource,gbc);

    	//Set layout and add the panel
    	inputResourceTabPanel.setLayout(flowLayoutLeft);
    	inputResourceTabPanel.add(inputResourcesGridBagPanel);

    	//--- Output resources

    	JPanel outputResourcesGridBagPanel = new JPanel(new GridBagLayout());
    	index = 0;
    	for (PropertyGroupElement<Resource> outputElement: outputResourcePropertyGroupList){
        	index++;
    		if(!outputElement.isExpanded()){
    			 outputResourcesGridBagPanel.add(getCollapsedViewEditorPanel(outputElement, "OUTPUT " + index), gbc);
    		} else {
    			 outputResourcesGridBagPanel.add(getExpandedViewEditorPanel(outputElement, "OUTPUT " + index),gbc);
    		}
        }
    	outputResourcesGridBagPanel.add(btnAddOutputResource,gbc);

    	//Set layout and add the panel
    	outputResourceTabPanel.setLayout(flowLayoutLeft);
    	outputResourceTabPanel.add(outputResourcesGridBagPanel);
    	//outputResourceTabPanel.setSize(new Dimension(300, 100));


    	//--- All together


    	personTabPanel.setBorder(BorderFactory.createTitledBorder(compoundBorder, "PERSONS"));
    	toolTabPanel.setBorder(BorderFactory.createTitledBorder(compoundBorder, "TOOL"));
    	inputResourceTabPanel.setBorder(BorderFactory.createTitledBorder(compoundBorder, "INPUT resources"));
    	outputResourceTabPanel.setBorder(BorderFactory.createTitledBorder(compoundBorder, "OUTPUT resources"));



//    	personTabPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Persons", TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION));
//    	toolTabPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Tool", TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION));
//    	inputResourceTabPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Input resources", TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION));
//    	outputResourceTabPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.black), "Output resources", TitledBorder.LEFT, TitledBorder.DEFAULT_POSITION));

    	scrollablePanel.setLayout(new BoxLayout(scrollablePanel, BoxLayout.Y_AXIS));
    	scrollablePanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

    	scrollablePanel.add(personTabPanel);
    	scrollablePanel.add(toolTabPanel);
    	scrollablePanel.add(inputResourceTabPanel);
    	scrollablePanel.add(outputResourceTabPanel);

//    	editorPanel.add(personTabPanel);
//    	editorPanel.add(toolTabPanel);
//    	editorPanel.add(inputResourceTabPanel);
//    	editorPanel.add(outputResourceTabPanel);

    	//Restore viewport position of scrollable panel



//    	editorPanel.add(getScrollPane(personTabPanel));
//    	editorPanel.add(getScrollPane(toolTabPanel));
//    	editorPanel.add(getScrollPane(inputResourceTabPanel));
//    	editorPanel.add(getScrollPane(outputResourceTabPanel));

    	setViewportPosition(scrollablePanel, viewportPosScrollablePanel);
    	JScrollPane scrollPane = getScrollPane(scrollablePanel);
    	scrollPane.setPreferredSize(new Dimension(800, 700));
    	scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.GRAY)); //border on top and bottom


    	//scrollPane.setBorder(BorderFactory.createTitledBorder(compoundBorder, "Editable parameters"));

    	//editorPanel.add(new JSeparator(JSeparator.HORIZONTAL));
    	editorPanel.add(scrollPane);

    	//editorPanel.add(getScrollPane(scrollablePanel));
    	//editorPanel.add(getVerticalScrollPane(scrollablePanel, new Dimension(200, 200)));
    	//editorPanel.add(getPanelWithScrollMinSize(scrollablePanel, 600, 700));
    	//editorPanel.add(getScrollPane(scrollablePanel));
    	//editorPanel.add(getScrollPaneMinSize(scrollablePanel, new Dimension(400,600)));



    	//editorPanel.add(getPanelWithScrollMinSize(editorPanel, 600, 800));

    	//editorPanel.setSize(new Dimension(300, 700));



    	editorPanel.revalidate();
//        	Window win = SwingUtilities.getWindowAncestor(editorPanel);
//        	if (win != null){
//        		win.revalidate();
//        		win.pack();
//        	}

    }



    /**
     * OLD: Horizontal TAB-layout. As Backup.
     */
/*
    private void updateTabEditorView(JPanel editorPanel){

	    	int index;
	    	GridBagConstraints gbc = new GridBagConstraints();
	    	gbc.anchor = GridBagConstraints.NORTH;

	    	//Save a viewport position of the ScrollPane
	    	Point viewportPosEditorPanel = getViewportPosition(editorPanel);
	    	Point viewportPosPersonTabbedPanel = getViewportPosition(personTabPanel);
	    	Point viewportPosToolTabbedPanel = getViewportPosition(toolTabPanel);
	    	Point viewportPosInputResourceTabbedPanel = getViewportPosition(inputResourceTabPanel);
	    	Point viewportPosOutputResourceTabbedPanel = getViewportPosition(outputResourceTabPanel);

	    	//Save active panel of the tabbedEditorPanel
	    	int selectedTabIndex = 0;
	    	if(tabbedEditorPane.getSelectedIndex() >= 0){
	    		selectedTabIndex = tabbedEditorPane.getSelectedIndex();
	    	}

	    	//Remove all components from all panes and panels
	    	editorPanel.removeAll();
	    	tabbedEditorPane.removeAll();
	    	personTabPanel.removeAll();
	    	toolTabPanel.removeAll();
	    	inputResourceTabPanel.removeAll();
	    	outputResourceTabPanel.removeAll();

	    	//Set BoxLayout (to put all components vertically)
	    	editorPanel.setLayout(new BoxLayout(editorPanel, BoxLayout.Y_AXIS));

	    	//Title + Description
	    	FormBuilder tittleGroupBuilder = FormBuilder.create()
	    							.columns("left:pref, 3dlu, pref:grow")
	    							.rows(layoutRows);
	    	addPropertyLineToFormBuilder(titlePropertyLine, tittleGroupBuilder, 0);
	    	addPropertyLineToFormBuilder(descriptionPropertyLine, tittleGroupBuilder, 1);
	    	editorPanel.add(tittleGroupBuilder.build());

	    	//--- Persons

	    	JPanel personGridBagPanel = new JPanel(new GridBagLayout());
	    	index = 0;
	    	for (PropertyGroupElement<Person> element: personPropertyGroupList){
	        	index++;
	    		if(!element.isExpanded()){
	    			 personGridBagPanel.add(getCollapsedViewEditorPanel(element, "Person " + index),gbc);
	    		} else {
	    			 personGridBagPanel.add(getExpandedViewEditorPanel(element, "Person " + index),gbc);
	    		}
	        }

	    	FormBuilder personTabBuilder = FormBuilder.create()
											.columns("left:pref")
											.rows("pref")
											.padding(Paddings.DLU14)
											.add(btnAddPerson).xy(1,1).appendRows("$lg, pref")
											.add(personGridBagPanel).xy(1,3).appendRows("$lg, pref");
	    	//Set layout and add the panel
	    	personTabPanel.setLayout(new BorderLayout());
	    	personTabPanel.add(personTabBuilder.build());

	    	//--- Tool

	    	JPanel toolGridBagPanel = new JPanel(new GridBagLayout());
	    	index = 0;
	    	for (PropertyGroupElement<Tool> element: toolPropertyGroupList){
	        	index++;
	    		if(!element.isExpanded()){
	    			 toolGridBagPanel.add(getCollapsedViewEditorPanel(element, "Tool"),gbc);
	    		} else {
	    			 toolGridBagPanel.add(getExpandedViewEditorPanel(element, "Tool"),gbc);
	    		}
	        }

	    	FormBuilder toolTabBuilder = FormBuilder.create()
											.columns("left:pref")
											.rows("pref")
											.padding(Paddings.DLU14);
	    	if(toolPropertyGroupList.size()==0){
	    		toolTabBuilder.add(btnAddTool).xy(1,1).appendRows("$lg, pref");
	    	} else {
	    		toolTabBuilder.add(toolGridBagPanel).xy(1,1).appendRows("$lg, pref");
	    	}
	    	//Set layout and add the panel
	    	toolTabPanel.setLayout(new BorderLayout());
	    	toolTabPanel.add(toolTabBuilder.build());

	    	//--- Input resources

	    	JPanel inputResourcesGridBagPanel = new JPanel(new GridBagLayout());
	    	index = 0;
	    	for (PropertyGroupElement<Resource> inputElement: inputResourcePropertyGroupList){
	        	index++;
	    		if(!inputElement.isExpanded()){
	    			 inputResourcesGridBagPanel.add(getCollapsedViewEditorPanel(inputElement, "Resource " + index),gbc);
	    		} else {
	    			 inputResourcesGridBagPanel.add(getExpandedViewEditorPanel(inputElement, "Resource " + index),gbc);
	    		}
	        }

	    	FormBuilder inputResourcesTabBuilder = FormBuilder.create()
											.columns("left:pref")
											.rows("pref")
											.padding(Paddings.DLU14)
											.add(btnAddInputResource).xy(1,1).appendRows("$lg, pref")
											.add(inputResourcesGridBagPanel).xy(1,3).appendRows("$lg, pref");
	    	//Set layout and add the panel
	    	inputResourceTabPanel.setLayout(new BorderLayout());
	    	inputResourceTabPanel.add(inputResourcesTabBuilder.build());

	    	//--- Output resources

	    	JPanel outputResourcesGridBagPanel = new JPanel(new GridBagLayout());
	    	index = 0;
	    	for (PropertyGroupElement<Resource> outputElement: outputResourcePropertyGroupList){
	        	index++;
	    		if(!outputElement.isExpanded()){
	    			 outputResourcesGridBagPanel.add(getCollapsedViewEditorPanel(outputElement, "Resource " + index),gbc);
	    		} else {
	    			 outputResourcesGridBagPanel.add(getExpandedViewEditorPanel(outputElement, "Resource " + index),gbc);
	    		}
	        }

	    	FormBuilder outputResourcesTabBuilder = FormBuilder.create()
											.columns("left:pref")
											.rows("pref")
											.padding(Paddings.DLU14)
											.add(btnAddOutputResource).xy(1,1).appendRows("$lg, pref")
											.add(outputResourcesGridBagPanel).xy(1,3).appendRows("$lg, pref");
	    	//Set layout and add the panel
	    	outputResourceTabPanel.setLayout(new BorderLayout());
	    	outputResourceTabPanel.add(outputResourcesTabBuilder.build());

	    	//--- All together

	    	//Insert all tabs to the tabbedEditorPane
	    	tabbedEditorPane.insertTab("PERSONS", null, getScrollPane(personTabPanel), "Edit persons", 0);
	    	tabbedEditorPane.insertTab("TOOL", null, getScrollPane(toolTabPanel), "Edit tool", 1);
	    	tabbedEditorPane.insertTab("INPUT Resources", null, getScrollPane(inputResourceTabPanel), "Edit input resources", 2);
	    	tabbedEditorPane.insertTab("OUTPUT Resources", null, getScrollPane(outputResourceTabPanel), "Edit output resources", 3);

	    	//Restore selected tab index
	    	tabbedEditorPane.setSelectedIndex(selectedTabIndex);

	    	//Set perf.size: not the cleverest way, but should work
	    	tabbedEditorPane.setPreferredSize(new Dimension(700, 500));

	    	//Restore viewport position of each TabPanel
	    	setViewportPosition(personTabPanel, viewportPosPersonTabbedPanel);
	    	setViewportPosition(toolTabPanel, viewportPosToolTabbedPanel);
	    	setViewportPosition(inputResourceTabPanel, viewportPosInputResourceTabbedPanel);
	    	setViewportPosition(outputResourceTabPanel, viewportPosOutputResourceTabbedPanel);

	    	//Add tabbed panel to the main editor panel
	    	editorPanel.add(tabbedEditorPane);

	    	editorPanel.revalidate();
	//        	Window win = SwingUtilities.getWindowAncestor(editorPanel);
	//        	if (win != null){
	//        		win.revalidate();
	//        		win.pack();
	//        	}

	}
*/


    /**
     * Create new Person with standard parameters
     *
     * @return DefaultPerson
     */
    private DefaultPerson createNewPerson(){

    	DefaultPerson person = DefaultPerson.uniquePerson();
//UNCOMMENT        
//    	person.addIdentifier(new Identifier(DefaultPersonIdentifierType.NAME, this.defaultInputString, ""));
        //person.addIdentifier(new Identifier(DefaultPersonIdentifierType.FIRSTNAME_LASTNAME, DefaultPersonIdentifierType.FIRSTNAME_LASTNAME.getLabel(), ""));
        //person.addIdentifier(new Identifier(DefaultPersonIdentifierType.ORCID, DefaultPersonIdentifierType.ORCID.getLabel(), ""));
        person.setDescription(this.defaultInputString);
//        person.setRole(DefaultPersonRole.EDITOR.getLabel());
    	return person ;
    }

    /**
     * Create new Tool with standard parameters
     *
     * @return DefaultTool
     */
    private DefaultTool createNewTool(){

    	DefaultTool tool = DefaultTool.uniqueTool();
//UNCOMMENT
//    	tool.addIdentifier(new Identifier(DefaultResourceIdentifierType.NAME_VERSION, this.defaultInputString, ""));
        tool.setDescription(this.defaultInputString);
        tool.setEnvironment(this.defaultInputString);
        tool.setParameters(this.defaultInputString);
        tool.setResourceType(this.defaultInputString);
        return tool;
    }

    /**
     * Create new Resource with standard parameters
     *
     * @return DefaultResource
     */
    private DefaultResource createNewResource(){

    	DefaultResource resource = DefaultResource.uniqueResource();
//UNCOMMENT
//    	resource.addIdentifier(new Identifier(DefaultResourceIdentifierType.NAME_VERSION, this.defaultInputString, ""));
//        resource.addIdentifier(new Identifier(DefaultResourceIdentifierType.PATH, this.defaultInputString, ""));
        resource.setDescription(this.defaultInputString);
        resource.setResourceType(this.defaultInputString);
        return resource;
    }


    /**
     * Process pushing of the "add button"
     */
    private <T extends Identifiable> void processAddIdentifierButtonPush(PropertyGroupElement<T> element){

    	//Prepare role combobox
		List<String> identifierTypeList = new ArrayList<>();

		//Person
		if(element.getIdentifiableObject() instanceof Person){
			//Create identifierTypeList basing on DefaultPersonIdentifierType
//UNCOMMENT			
//			for(DefaultPersonIdentifierType identifierType: Arrays.asList(DefaultPersonIdentifierType.class.getEnumConstants())){
//				boolean isStandardProperty = false;
//				//Check if standard property
//				for(PersonStandardProperties standardProperty: Arrays.asList(PersonStandardProperties.class.getEnumConstants())){
//					if(identifierType.getLabel() == standardProperty.getLabel()){
//						isStandardProperty = true;
//						break;
//					}
//				}
//				//is ignored identifier?
//				for(IgnoredIdentifiers ignoredIdent: Arrays.asList(IgnoredIdentifiers.class.getEnumConstants())){
//					if(identifierType.getLabel() == ignoredIdent.getLabel() ){
//						isStandardProperty = true;
//						break;
//					}
//				}
//				//if not standard property (and not ignored identifier), than add it to the list
//				if(!isStandardProperty){
//					if(!element.getCustomIdentifiersViewAsMap().containsKey(identifierType.getLabel())){
//						//TODO: think about "other" -> implemnt "other" as "kex/value" table
//						// || (identifierType==DefaultPersonIdentifierType.OTHER)){ //you can use more than 1 "other"
//
//						identifierTypeList.add(identifierType.getLabel()); //add identifier type to the list
//					}
//				}
//			}

		//Tool
		} else if (element.getIdentifiableObject() instanceof Tool){
			//Create identifierTypeList basing on DefaultResourceIdentifierType
//UNCOMMENT			
//			for(DefaultResourceIdentifierType identifierType: Arrays.asList(DefaultResourceIdentifierType.class.getEnumConstants())){
//				boolean isStandardProperty = false;
//				//Check if standard property
//				for(ToolStandardProperties standardProperty: Arrays.asList(ToolStandardProperties.class.getEnumConstants())){
//					if(identifierType.getLabel() == standardProperty.getLabel()){
//						isStandardProperty = true;
//						break;
//					}
//				}
//				//is ignored identifier?
//				for(IgnoredIdentifiers ignoredIdent: Arrays.asList(IgnoredIdentifiers.class.getEnumConstants())){
//					if(identifierType.getLabel() == ignoredIdent.getLabel() ){
//						isStandardProperty = true;
//						break;
//					}
//				}
//				//if not standard property (and not ignored identifier), than add it to the list
//				if(!isStandardProperty){
//					if(!element.getCustomIdentifiersViewAsMap().containsKey(identifierType.getLabel())){
//						//TODO: think about "other" -> implemnt "other" as "key/value" table
//						// || (identifierType==DefaultPersonIdentifierType.OTHER)){ //you can use more than 1 "other"
//
//						identifierTypeList.add(identifierType.getLabel()); //add identifier type to the list
//					}
//				}
//			}

		//Resource
		} else if (element.getIdentifiableObject() instanceof Resource){
			//Create identifierTypeList basing on DefaultResourceIdentifierType
//UNCOMMENT			
//			for(DefaultResourceIdentifierType identifierType: Arrays.asList(DefaultResourceIdentifierType.class.getEnumConstants())){
//				boolean isStandardProperty = false;
//				//Check if standard property
//				for(ResourceStandardProperties standardProperty: Arrays.asList(ResourceStandardProperties.class.getEnumConstants())){
//					if(identifierType.getLabel() == standardProperty.getLabel()){
//						isStandardProperty = true;
//						break;
//					}
//				}
//				//is ignored identifier?
//				for(IgnoredIdentifiers ignoredIdent: Arrays.asList(IgnoredIdentifiers.class.getEnumConstants())){
//					if(identifierType.getLabel() == ignoredIdent.getLabel() ){
//						isStandardProperty = true;
//						break;
//					}
//				}
//				//if not standard property (and not ignored identifier), than add it to the list
//				if(!isStandardProperty){
//					if(!element.getCustomIdentifiersViewAsMap().containsKey(identifierType.getLabel())){
//						//TODO: think about "other" -> implemnt "other" as "key/value" table
//						// || (identifierType==DefaultPersonIdentifierType.OTHER)){ //you can use more than 1 "other"
//
//						identifierTypeList.add(identifierType.getLabel()); //add identifier type to the list
//					}
//				}
//			}
		}

		//Return if the identifier type list is empty
		if(identifierTypeList.isEmpty()){
			GuiUtils.showInfo(null, "Sorry, you have already used all possible identifiers.");
			//TODO: think about "add button" - disable it or not?
			//element.btnAddIdentifier.setEnabled(false);
			return;
		}

		JComboBox typeCombo = new JComboBox(identifierTypeList.toArray());

		//Create new property line for the dialog
		PropertyLine identifierPropertyLine = new PropertyLine(
													typeCombo,
													new JTextField(this.defaultInputString)
													);
		FormBuilder builder = FormBuilder.create()
									.columns(layoutColumn)
									.rows(layoutRows);
    	addPropertyLineToFormBuilder(identifierPropertyLine, builder, 0);

        // TODO: discuss, what type of dialog to use (Confirm or Option -> do we need a language adaptation?)
//        if(JOptionPane.showConfirmDialog(null,
//    			builder.build(),
//    			"Add new identifier",
//    			JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
//
        if(JOptionPane.showOptionDialog(null,
	        			builder.build(),
	        			"Add new identifier",
	        			JOptionPane.YES_NO_OPTION,
	        			JOptionPane.PLAIN_MESSAGE,
	        			null,
	        			new String[]{"OK", "Cancel"},
	        			"Default") == JOptionPane.YES_OPTION){
        	//TODO: add input checking directly to the EditorComponent field, real time checking (see MetadataEditor)
        	if((element.getCustomIdentifiersViewAsMap().containsKey(identifierPropertyLine.getLabelComponentAsString()))
        		&& (element.getCustomIdentifiersViewAsMap().containsValue(identifierPropertyLine.getEditComponentAsString()))){

        		GuiUtils.showInfo(null, "Sorry, the same identifier already exists. Please try again.");//not used if "other" can be used only 1 time
        	} else {
        		element.addCustomIdentifierLineView(new PropertyLine(
									new JLabel(identifierPropertyLine.getLabelComponentAsString()),
									new JTextField(identifierPropertyLine.getEditComponentAsString()),
									createButton(buttonNameRemoveIdentifier, iconRemove, buttonPreferredSize, actionListenerGroupElement)
									));
        	}
        }

        updateEditorView(editorPanel);

    }


    private <T extends Identifiable> void processRemoveIdentifierButtonPush(PropertyGroupElement<T> element, PropertyLine customIdentifierLine){
    	element.removeCustomIdentifierLineView(customIdentifierLine);
    	updateEditorView(editorPanel);
    }

    /**
     * Process "remove button"
     */
    private <T extends Identifiable> void processRemoveButtonPush(PropertyGroupElement<T> element, JButton removeButton){

    	//TODO: put message-text to the ResourceManager, see ui/metadata/MetadataManagerPanel

//    	if(JOptionPane.showConfirmDialog(null,
//    			"Do you really want to remove this element? All not applied changes will be lost.",
//    			"Remove element",
//    			JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
    	if(JOptionPane.showOptionDialog(null,
    			"Do you really want to remove this element? All not applied changes will be lost.",
    			"Remove element",
    			JOptionPane.YES_NO_OPTION,
    			JOptionPane.PLAIN_MESSAGE,
    			null,
    			new String[]{"OK", "Cancel"},
    			"Default") == JOptionPane.YES_OPTION){

	    	if(removeButton.getName() == buttonNameRemovePerson){
				personPropertyGroupList.remove(element);
			} else if (removeButton.getName() == buttonNameRemoveTool){
				toolPropertyGroupList.remove(element);
			} else if (removeButton.getName() == buttonNameRemoveInputResource){
				inputResourcePropertyGroupList.remove(element);
			} else if (removeButton.getName() == buttonNameRemoveOutputResource){
				outputResourcePropertyGroupList.remove(element);
			}

			updateEditorView(editorPanel);
    	}
    }


    /**
     * Process "expanded view button"
     */
    private <T extends Identifiable> void processExpandedViewButtonPush(PropertyGroupElement<T> element){

    	element.setExpanded(true);

    	//change button "show more elements" to "show less elements"
    	for(JButton btn: element.getButtons()){
    		if(btn.getName() == buttonNameExpandedView){
    			btn.setName(buttonNameCollapsedView);
    			btn.setToolTipText(buttonNameCollapsedView);
    			btn.setIcon(iconCollapsed);
    			break;
    		}
    	}
    	updateEditorView(this.editorPanel);
    }


    /**
     * Process "collapsed view button"
     */
    private <T extends Identifiable> void processCollapsedViewButtonPush(PropertyGroupElement<T> element){

    	element.setExpanded(false);

    	//change button "show more elements" to "show less elements"
    	for(JButton btn: element.getButtons()){
    		if(btn.getName() == buttonNameCollapsedView){
    			btn.setName(buttonNameExpandedView);
    			btn.setToolTipText(buttonNameExpandedView);
    			btn.setIcon(iconExpanded);
    			break;
    		}
    	}
    	updateEditorView(this.editorPanel);
	}



    /**
     * Pack a header (String) with possible buttons into the defined row of the FormBuilder.
     *
     * Attention! rowIndex will be increased automatically and returned as a return value.
     *
     *
     * @param headers
     * @param builder
     * @param rowIndex
     * @param buttons (if needed)
     *
     * @return updated rowIndex, which is ready for the next iteration
     */
    private int addHeaderToFormBuilder(String header, FormBuilder builder, int rowIndex, JButton... buttons){

    	// Add a header using right-alignment (via extra FormBuilder)
    	FormBuilder labelBuilder = FormBuilder.create()
				  						.columns("right:pref:grow")
				  						.rows("pref");
    	labelBuilder.add(header).xy(1,1);
    	builder.add(labelBuilder.build()).xy(3,(rowIndex*2)+1);

    	// Add buttons
    	int xpos = 5;
		for (JButton btn: buttons){
			if (btn != null){
		  		builder.add(btn).xy(xpos,(rowIndex*2)+1);
		  		xpos+=2;
		  	}
		}

		// Add new line, increment the rowIndes
		builder.appendRows("$lg, pref");
	  	rowIndex++;

	  	return rowIndex;
    }

    /**
     * Pack a property line to the FormBuilder into the defined row.
     *
     * Attention! rowIndex will be increased automatically and returned as a return value.
     *
     * Usage example:
     * 		row = packPropertyLineToFormBuilderByRow(propertyLine, builder, row);
     *
     * @param collectedPropertyLines
     * @param builder
     * @param rowIndex
     *
     * @return updated rowIndex, which is ready for the next iteration
     */
    private int addPropertyLineToFormBuilder(PropertyLine propertyLine, FormBuilder builder, int rowIndex){

		// Add label and editable component
    	builder.add(propertyLine.getLabelComponent()).xy(1,(rowIndex*2)+1); //Standard label for property, with editable component
		builder.add(propertyLine.getEditComponent()).xy(3,(rowIndex*2)+1); 		//Editable component

		// Add buttons if possible
		int xpos = 5;
		for (JButton btn: propertyLine.getButtons()){
			if (btn != null){
		  		builder.add(btn).xy(xpos,(rowIndex*2)+1);
		  		xpos+=2;
		  	}
		}

		// Add new line and increment the rowIndex
	  	builder.appendRows("$lg, pref");
	  	rowIndex++;

	  	return rowIndex;
    }

    /**
     * Get a Panel with an integrated ScrollPane with defined minimum size
     *
     * @param panelOrig
     * @param minScrollWidth
     * @param minScrollHight
     * @return JPanel with integrated JScrollPane
     */
    private JPanel getPanelWithScrollMinSize(JPanel panelOrig, int minScrollWidth, int minScrollHight){

		FormBuilder scrollPanelBuilder = FormBuilder.create()
													.columns("pref:grow")
													.rows("pref:grow");
		JScrollPane scrollPane = new JScrollPane(panelOrig);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(20);

		// FIXME: do resizing more clever!
		Dimension currentSize = scrollPane.getPreferredSize();
		if((currentSize.width > minScrollWidth) || (currentSize.height > minScrollHight)){
			scrollPane.setPreferredSize(new Dimension(minScrollWidth,minScrollHight));
		}
		//scrollPane.setPreferredSize(new Dimension(minScrollWidth,minScrollHight));
		scrollPanelBuilder.add(scrollPane).xy(1, 1);
    	return scrollPanelBuilder.build();
    }

    /**
     * Pack a Panel to a ScrollPane without min size
     *
     * @param panelOrig
     * @return JPanel with integrated JScrollPane
     */
    private JScrollPane getScrollPane(JPanel panelOrig){

		JScrollPane scrollPane = new JScrollPane(panelOrig);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(20);

    	return scrollPane;
    }

    /**
     * Pack a Panel to a ScrollPane without min size
     *
     * @param panelOrig
     * @return JPanel with integrated JScrollPane
     */
    private JScrollPane getScrollPaneMinSize(JPanel panelOrig, Dimension dimension){

		JScrollPane scrollPane = new JScrollPane(panelOrig);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
		scrollPane.setMinimumSize(dimension);

    	return scrollPane;
    }

    /**
     * Pack a Panel to a ScrollPane with only vertical scrollbar, without min size
     *
     * @param panelOrig
     * @return JPanel with integrated JScrollPane
     */
    private JScrollPane getVerticalScrollPane(JPanel panelOrig){

		JScrollPane scrollPane = new JScrollPane(panelOrig,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
	            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);

    	return scrollPane;
    }

    /**
     * Pack a Panel to a ScrollPane with only vertical scrollbar, with min size
     *
     * @param panelOrig
     * @return JPanel with integrated JScrollPane
     */
    private JScrollPane getVerticalScrollPane(JPanel panelOrig, Dimension minSize){

		JScrollPane scrollPane = new JScrollPane(panelOrig,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
	            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(20);
		scrollPane.setMinimumSize(minSize);

    	return scrollPane;
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
     * Get an editor component (e.g. JPanel in case of this editor).
     * A GUI part of the editor.
     *
     * @see bwfdm.​replaydh.​ui.​helper#getEditorComponent()
     * @return Component
     */
    @Override
    public Component getEditorComponent() {

        /*
         * Check if an editorItem was added.
         * If not - return a JPanel with an error message.
         */
    	//TODO: remove it?
        if (getEditingItem() == null){
            return new JLabel("Error. Please add an editing item (WorkflowStep).");
        }

        updateEditorModel(getEditingItem());
        updateEditorView(this.editorPanel);
        return this.editorPanel;
    }

    /**
     * Set editing item to the editor
     *
     * @see bwfdm.replaydh.ui.helper.Editor#setEditingItem(java.lang.Object)
     */
    @Override
    public void setEditingItem(WorkflowStep item) {

    	//Set the item
    	this.workflowStep = item; 		//editable workflowstep

    	if(this.workflowStepOrig == null){
    		this.workflowStepOrig = item;	//original not editable workflowstep (for reset option)
    	}

    	//Update the model of the editor
    	//updateEditorModel(getEditingItem());
    }


    /**
     * Get editing item from the editor
     *
     * @see bwfdm.replaydh.ui.helper.Editor#getEditingItem()
     */
    @Override
    public WorkflowStep getEditingItem() {
        return this.workflowStep;
    }


    /**
     * Reset not saved (not applied) properties in the editor
     *
     * @see bwfdm.replaydh.ui.helper.Editor#resetEdit()
     */
    @Override
    public void resetEdit() {
//    	if(hasChanges() && (JOptionPane.showConfirmDialog(null,
//    			"Do you really want to reset all not applied values?",
//    			"Reset values",
//    			JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) ){
        if(hasChanges() && (JOptionPane.showOptionDialog(null,
        			"Do you really want to reset all not applied values?",
        			"Reset values",
        			JOptionPane.YES_NO_OPTION,
        			JOptionPane.PLAIN_MESSAGE,
        			null,
        			new String[]{"OK", "Cancel"},
        			"Default") == JOptionPane.YES_OPTION) ){

	    	//Update editor model (with workflowStep).
	    	//FYI: workflowStepOrig does not play a role, because it is only a link, not a real copy of the object.
	    	updateEditorModel(getEditingItem());

	    	//Update editor panel
	        updateEditorView(editorPanel);
    	}
    }



    /**
     * Restore a view mode (collapsed/expanded) of each person/tool/resource editor according to the old state
     *
     * @param newPropertyGroups - array of PropertyGroupElements which will be changed
     * @param oldPropertyGroups - old state, array of PropertyGroupElements, will NOT be changed
     */
    private <T extends Identifiable> void restoreViewModeCollapsedExpanded(List<PropertyGroupElement<T>> newPropertyGroups, List<PropertyGroupElement<T>> oldPropertyGroups){

    	for(PropertyGroupElement<T> newPropertyGroupElement : newPropertyGroups){
        	for(PropertyGroupElement<T> oldPropertyGroupElement : oldPropertyGroups){
				if(oldPropertyGroupElement.getIdentifiableObject().getSystemId() == newPropertyGroupElement.getIdentifiableObject().getSystemId()){
					if (oldPropertyGroupElement.isExpanded() == true){
						newPropertyGroupElement.setExpanded(true);
						//change button "show expanded view" to "show collapsed view"
				    	for(JButton btn: newPropertyGroupElement.getButtons()){
				    		if(btn.getName() == buttonNameExpandedView){
				    			btn.setName(buttonNameCollapsedView);
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
     * Apply changes of the properties.
     * Important: it changes variables "workflowStep" and "workflowStepOrig"
     *
     * @see bwfdm.replaydh.ui.helper.Editor#applyEdit()
     */
    @Override
    public void applyEdit() {

    	if(!isInputCorrect()){
    		//System.out.println("input not correct");
    		return;
    	}

        //Title
    	getEditingItem().setTitle(titlePropertyLine.getEditComponentAsString());

        //Description
    	getEditingItem().setDescription(descriptionPropertyLine.getEditComponentAsString());

        //Persons

    	// TODO: can we add a new person with the same properties+identifiers??
    	// Example - many clicks on "+" button and then "apply" without editing. Shell we ignore UUID in the "equals" method?

    	// 1 - check if there are new persons
        for(PropertyGroupElement<Person> personGroupElement: this.personPropertyGroupList){
        	boolean isNew = true;
        	for(Person person: getEditingItem().getPersons()){
        		//if(person.getSystemId().equals(personGroupElement.getIdentifiableObject().getSystemId())){
        		if(person.equals((Person)personGroupElement.getIdentifiableObject())){
        			isNew = false;
        			break;
        		}
        	}
        	if(isNew){
        		getEditingItem().addPerson((Person)personGroupElement.getIdentifiableObject());
        	}
        }
        // 2 - check if there are removed person
        for(Person person: getEditingItem().getPersons()){
        	boolean isRemoved = true;
        	for(PropertyGroupElement<Person> personGroupElement: this.personPropertyGroupList){
        		//if(person.getSystemId().equals(personGroupElement.getIdentifiableObject().getSystemId())){
        		if(person.equals((Person)personGroupElement.getIdentifiableObject())){
        			isRemoved = false;
        			break;
        		}
        	}
        	if(isRemoved){
        		getEditingItem().removePerson(person);
        	}
        }
        //Apply changes from the "person-View" to the "person-Model"
        for(PropertyGroupElement<Person> personGroupElement: this.personPropertyGroupList){
            personGroupElement.applyStandardPersonPropertiesToIdentifiable();
            personGroupElement.applyCustomIdentifiersToIdentifiable();
        }


        //Tool

        //check if tool was removed/added
        if(toolPropertyGroupList.size()!=0){
        	getEditingItem().setTool((Tool)toolPropertyGroupList.get(0).getIdentifiableObject());
        } else {
        	getEditingItem().setTool(null);
        }

        //Apply changes from the "tool-View" to the "tool-Model"
        for(PropertyGroupElement<Tool> toolGroupElement: this.toolPropertyGroupList){
            toolGroupElement.applyStandardToolPropertiesToIdentifiable();
            toolGroupElement.applyCustomIdentifiersToIdentifiable();
        }


        //Input resources


        // 1 - check if there are new input resources
        for(PropertyGroupElement<Resource> inputResourceGroupElement: this.inputResourcePropertyGroupList){
        	boolean isNew = true;
        	for(Resource resource: getEditingItem().getInput()){
        		//if(person.getSystemId().equals(personGroupElement.getIdentifiableObject().getSystemId())){
        		if(resource.equals((Resource)inputResourceGroupElement.getIdentifiableObject())){
        			isNew = false;
        			break;
        		}
        	}
        	if(isNew){
        		getEditingItem().addInput((Resource)inputResourceGroupElement.getIdentifiableObject());
        	}
        }
        // 2 - check if there are removed output resources
        for(Resource resource: getEditingItem().getInput()){
        	boolean isRemoved = true;
        	for(PropertyGroupElement<Resource> inputResourceGroupElement: this.inputResourcePropertyGroupList){
        		//if(person.getSystemId().equals(personGroupElement.getIdentifiableObject().getSystemId())){
        		if(resource.equals((Resource)inputResourceGroupElement.getIdentifiableObject())){
        			isRemoved = false;
        			break;
        		}
        	}
        	if(isRemoved){
        		getEditingItem().removeInput(resource);
        	}
        }
        //Apply changes from the "input-View" to the "input-Model"
        for(PropertyGroupElement<Resource> inputResourceGroupElement: this.inputResourcePropertyGroupList){
            inputResourceGroupElement.applyStandardResourcePropertiesToIdentifiable();
            inputResourceGroupElement.applyCustomIdentifiersToIdentifiable();
        }


        //Output resources


        //TODO: pack all methods into 1 method!

        // 1 - check if there are new output resources
        for(PropertyGroupElement<Resource> outputResourceGroupElement: this.outputResourcePropertyGroupList){
        	boolean isNew = true;
        	for(Resource resource: getEditingItem().getOutput()){
        		//if(resource.getSystemId().equals(outputResourceGroupElement.getIdentifiableObject().getSystemId())){
        		if(resource.equals((Resource)outputResourceGroupElement.getIdentifiableObject())){
        			isNew = false;
        			break;
        		}
        	}
        	if(isNew){
        		getEditingItem().addOutput((Resource)outputResourceGroupElement.getIdentifiableObject());
        	}
        }
        // 2 - check if there are removed output resources
        for(Resource resource: getEditingItem().getOutput()){
        	boolean isRemoved = true;
        	for(PropertyGroupElement<Resource> outputResourceGroupElement: this.outputResourcePropertyGroupList){
        		//if(resource.getSystemId().equals(outputResourceGroupElement.getIdentifiableObject().getSystemId())){
        		if(resource.equals((Resource)outputResourceGroupElement.getIdentifiableObject())){
        			isRemoved = false;
        			break;
        		}
        	}
        	if(isRemoved){
        		getEditingItem().removeOutput(resource);
        	}
        }
        //Apply changes from the "output-View" to the "output-Model"
        for(PropertyGroupElement<Resource> outputResourceGroupElement: this.outputResourcePropertyGroupList){
            outputResourceGroupElement.applyStandardResourcePropertiesToIdentifiable();
            outputResourceGroupElement.applyCustomIdentifiersToIdentifiable();
        }

        //Common

        //Update the editor's model
    	updateEditorModel(getEditingItem());

        //Update editorPanel
        updateEditorView(editorPanel);
    }


    /**
     * Check if there are some changes of the the properties
     * true  -> yes, editor has changes
     * false -> no, editor does not have changes
     *
     * @see bwfdm.replaydh.ui.helper.Editor#hasChanges()
     */
    @Override
    public boolean hasChanges() {

    	WorkflowStep workflowStep = this.getEditingItem();

    	//Title
    	if (!workflowStep.getTitle().equals(titlePropertyLine.getEditComponentAsString())){
    		return true;
    	}

    	//Description
    	if (!workflowStep.getDescription().equals(descriptionPropertyLine.getEditComponentAsString())){
    		return true;
    	}

    	//TODO: does addition of new elements mean "hasChanges"?
    	//If not -> remove check with the workflowStep.get.. from "if" bellow

    	//Persons
        for(PropertyGroupElement<Person> personGroupElement: this.personPropertyGroupList){
        	if (personGroupElement.hasChanges() || !workflowStep.getPersons().contains(personGroupElement.getIdentifiableObject())){
        		return true;
        	}
        }

        //Tool
        for(PropertyGroupElement<Tool> toolGroupElement: this.toolPropertyGroupList){
        	if (toolGroupElement.hasChanges() || workflowStep.getTool()==null){
        		return true;
        	}
        }

        //Input resources
        for(PropertyGroupElement<Resource> inputResourceGroupElement: this.inputResourcePropertyGroupList){
        	if (inputResourceGroupElement.hasChanges() || !workflowStep.getInput().contains(inputResourceGroupElement.getIdentifiableObject())){
        		return true;
        	}
        }

        //Output resources
        for(PropertyGroupElement<Resource> outputResourceGroupElement: this.outputResourcePropertyGroupList){
        	if (outputResourceGroupElement.hasChanges() || !workflowStep.getOutput().contains(outputResourceGroupElement.getIdentifiableObject())){
        		return true;
        	}
        }

    	//There are no changes
    	return false;
    }


    /**
     * Close the editor without applying the changes
     *
     * @see bwfdm.replaydh.ui.helper.Editor#close()
     */
    @Override
    public void close() {

//    	if(JOptionPane.showConfirmDialog(null,
//    			"Do you really want to close the editor? All not applied changes will be lost?",
//    			"Exit",
//    			JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION){
		if(JOptionPane.showOptionDialog(null,
        			"Do you really want to close the editor? All not applied changes will be lost?",
        			"Exit",
        			JOptionPane.YES_NO_OPTION,
        			JOptionPane.PLAIN_MESSAGE,
        			null,
        			new String[]{"OK", "Cancel"},
        			"Default") == JOptionPane.YES_OPTION){

	    	return;
	    	//updateEditorModel(getEditingItem());
    	}
    }

    /**
     * Check if all input parameters do not contain illegal values
     * (for now - just checking of empty strings)
     *
     * @return
     */
    private boolean isInputCorrect(){
    	//TODO: remake it more beautiful and universal!

    	//Title
    	if (illegalInput.contains(titlePropertyLine.getEditComponentAsString())){
    		GuiUtils.showInfo(null, "Not correct parameters: " + titlePropertyLine.getLabelComponentAsString() + " -- " + titlePropertyLine.getEditComponentAsString());
    		return false;
    	}

    	//Description
    	if (illegalInput.contains(descriptionPropertyLine.getEditComponentAsString())){
    		GuiUtils.showInfo(null, "Not correct parameters: " + descriptionPropertyLine.getLabelComponentAsString() + " -- " + descriptionPropertyLine.getEditComponentAsString());
    		return false;
    	}

    	//Persons
        for(PropertyGroupElement<Person> personGroupElement: this.personPropertyGroupList){
        	for(PropertyLine propLine: personGroupElement.getStandardPropertyLinesView()){
        		if (illegalInput.contains(propLine.getEditComponentAsString())){
        			GuiUtils.showInfo(null, "Not correct person parameters: " + propLine.getLabelComponentAsString() + " -- " + propLine.getEditComponentAsString());
            		return false;
        		}
        	}
        	for(PropertyLine propLine: personGroupElement.getCustomIdentifierLinesView()){
        		if (illegalInput.contains(propLine.getEditComponentAsString())){
        			GuiUtils.showInfo(null, "Not correct person parameters: " + propLine.getLabelComponentAsString() + " -- " + propLine.getEditComponentAsString());
            		return false;
        		}
        	}
        }

        //Tool
        for(PropertyGroupElement<Tool> toolGroupElement: this.toolPropertyGroupList){
        	for(PropertyLine propLine: toolGroupElement.getStandardPropertyLinesView()){
        		if (illegalInput.contains(propLine.getEditComponentAsString())){
        			GuiUtils.showInfo(null, "Not correct tool parameters: " + propLine.getLabelComponentAsString() + " -- " + propLine.getEditComponentAsString());
            		return false;
        		}
        	}
        	for(PropertyLine propLine: toolGroupElement.getCustomIdentifierLinesView()){
        		if (illegalInput.contains(propLine.getEditComponentAsString())){
        			GuiUtils.showInfo(null, "Not correct tool parameters: " + propLine.getLabelComponentAsString() + " -- " + propLine.getEditComponentAsString());
            		return false;
        		}
        	}
        }

        //Input resources
        for(PropertyGroupElement<Resource> inputResourceGroupElement: this.inputResourcePropertyGroupList){
        	for(PropertyLine propLine: inputResourceGroupElement.getStandardPropertyLinesView()){
        		if (illegalInput.contains(propLine.getEditComponentAsString())){
        			GuiUtils.showInfo(null, "Not correct input resource parameters: " + propLine.getLabelComponentAsString() + " -- " + propLine.getEditComponentAsString());
            		return false;
        		}
        	}
        	for(PropertyLine propLine: inputResourceGroupElement.getCustomIdentifierLinesView()){
        		if (illegalInput.contains(propLine.getEditComponentAsString())){
        			GuiUtils.showInfo(null, "Not correct input resource parameters: " + propLine.getLabelComponentAsString() + " -- " + propLine.getEditComponentAsString());
            		return false;
        		}
        	}
        }

        //Output resources
        for(PropertyGroupElement<Resource> outputResourceGroupElement: this.outputResourcePropertyGroupList){
        	for(PropertyLine propLine: outputResourceGroupElement.getStandardPropertyLinesView()){
        		if (illegalInput.contains(propLine.getEditComponentAsString())){
        			GuiUtils.showInfo(null, "Not correct output resource parameters: " + propLine.getLabelComponentAsString() + " -- " + propLine.getEditComponentAsString());
            		return false;
        		}
        	}
        	for(PropertyLine propLine: outputResourceGroupElement.getCustomIdentifierLinesView()){
        		if (illegalInput.contains(propLine.getEditComponentAsString())){
        			GuiUtils.showInfo(null, "Not correct output resource parameters: " + propLine.getLabelComponentAsString() + " -- " + propLine.getEditComponentAsString());
            		return false;
        		}
        	}
        }

        return true;
    }



    /**
     * Class to group a whole property element,
     * e.g. 1 person, 1 resource, 1 tool...
     *
     * @author vk
     *
     */
    private class PropertyGroupElement<T extends Identifiable>{

    	private String header;
    	private T identifiableObject;				//instance of the Identifiable-object (e.g. concrete resource, tool, person)
    	private boolean expanded; 					//expanded (true) or collapsed (false) view mode
    	private JButton btnAddIdentifier;			//button to add new identifier
    	private List<PropertyLine> standardPropertyLines; 	//every property (packed in form as a line) of the Identifiable object
    	private List<PropertyLine> customIdentifierLines;	//list of all identifier packed in UI-form
    	private List<JButton> buttons; 				//buttons like "more info", "edit", "remove"

    	public PropertyGroupElement(String header, T propertyObject, JButton... buttons){

    		this.header = header;
    		this.identifiableObject = propertyObject;
    		this.expanded = false;
    		this.standardPropertyLines = new ArrayList<>();
    		this.customIdentifierLines = new ArrayList<>();
    		this.btnAddIdentifier = createButton(buttonNameAddIdentifier, iconAdd, buttonPreferredSize, actionListenerGroupElement);
    		this.buttons = new ArrayList<>();
    		for (JButton btn : buttons){
				this.buttons.add(btn);
			}

    		if(identifiableObject instanceof Person){
    			updatePersonStandardPropertyLines();
    			updateCustomIdentifierLines();
    		}
    		else if(identifiableObject instanceof Tool){
    			updateToolStandardPropertyLines();
    			updateCustomIdentifierLines();
    		}
    		else if(identifiableObject instanceof Resource){
    			updateResourceStandardPropertyLines();
    			updateCustomIdentifierLines();
    		}
    	}

    	public void setExpanded(boolean expanded){
    		this.expanded = expanded;
    	}

    	public boolean isExpanded(){
    		return this.expanded;
    	}

    	public List<PropertyLine> getStandardPropertyLinesView() {
			return standardPropertyLines;
		}

    	public List<PropertyLine> getCustomIdentifierLinesView() {
			return customIdentifierLines;
		}

    	public void addCustomIdentifierLineView(PropertyLine customIdentifierLine){
    		this.customIdentifierLines.add(customIdentifierLine);
    	}

    	public void removeCustomIdentifierLineView(PropertyLine customIdentifierLine){
    		for(PropertyLine line: this.customIdentifierLines){
    			if((line.getLabelComponentAsString().equals(customIdentifierLine.getLabelComponentAsString())) //){
    					&&(line.getEditComponentAsString().equals(customIdentifierLine.getEditComponentAsString())) ){
    				this.customIdentifierLines.remove(line);
    				break;
    			}
    		}
    	}

		public T getIdentifiableObject() {
			return identifiableObject;
		}

		public JButton[] getButtons(){
			return buttons.toArray(new JButton[0]);
		}

		public String getHeader() {
			return header;
		}

		public JButton getButtonAddIdentifier(){
			return btnAddIdentifier;
		}

		/**
		 * Get all standard properties and custom identifiers together as a HashMap
		 * @return
		 */
		public HashMap<String, String> getPropertiesAndIdentifiersViewAsMap(){
			HashMap<String, String> collectedProperties = new HashMap<String, String>();
			collectedProperties.putAll(getStandardPropertiesViewAsMap());
			collectedProperties.putAll(getCustomIdentifiersViewAsMap());
			return collectedProperties;
		}

		/**
		 * Represent all standard properties as a HashMap
		 * @return
		 */
		public HashMap<String, String> getStandardPropertiesViewAsMap(){
			HashMap<String, String> standardProperties = new HashMap<String, String>();
			for(PropertyLine propertyLine: this.standardPropertyLines){
				standardProperties.put(propertyLine.getLabelComponentAsString(), propertyLine.getEditComponentAsString());
			}
			return standardProperties;
		}

		/**
		 * Represent all custom identifiers as a HashMap
		 * @return
		 */
		public HashMap<String, String> getCustomIdentifiersViewAsMap(){
			HashMap<String, String> collectedIdentifiersMap = new HashMap<String, String>();
			for(PropertyLine identifierLine: this.customIdentifierLines){
				collectedIdentifiersMap.put(identifierLine.getLabelComponentAsString(), identifierLine.getEditComponentAsString());
			}
			return collectedIdentifiersMap;
		}


		/**
	     * Represent all properties and identifiers of the identifiable MODEL as a HashMap
	     * @return
	     */
	    public HashMap<String, String> getPropertiesAndIdentifiersModelAsMap(){

	    	HashMap<String, String> collectedProperties = new HashMap<String, String>();

	    	//Person specific properties
	    	if(identifiableObject instanceof Person){
		    	if(((Person)identifiableObject).getRole() != null){
		    		collectedProperties.put(PersonStandardProperties.ROLE.getLabel(), ((Person)identifiableObject).getRole());
		    	}
		    	if(((Person)identifiableObject).getDescription() != null){
		    		collectedProperties.put(PersonStandardProperties.DESCRIPTION.getLabel(), ((Person)identifiableObject).getDescription());
		    	}
	    	}
	    	//Tool specific properties
	    	else if(identifiableObject instanceof Tool){
		    	if(((Tool)identifiableObject).getDescription() != null){
		    		collectedProperties.put(ToolStandardProperties.DESCRIPTION.getLabel(), ((Tool)identifiableObject).getDescription());
		    	}
		    	if(((Tool)identifiableObject).getEnvironment() != null){
		    		collectedProperties.put(ToolStandardProperties.ENVIRONMENT.getLabel(), ((Tool)identifiableObject).getEnvironment());
		    	}
		    	if(((Tool)identifiableObject).getParameters() != null){
		    		collectedProperties.put(ToolStandardProperties.PARAMETERS.getLabel(), ((Tool)identifiableObject).getParameters());
		    	}
	    	}
	    	//Resource specific properties
	    	else if(identifiableObject instanceof Resource){

	    		if(((Resource)identifiableObject).getResourceType() != null){
		    		collectedProperties.put(ResourceStandardProperties.RESOURCETYPE.getLabel(), ((Resource)identifiableObject).getResourceType());
		    	}
	    		if(((Resource)identifiableObject).getDescription() != null){
		    		collectedProperties.put(ResourceStandardProperties.DESCRIPTION.getLabel(), ((Resource)identifiableObject).getDescription());
		    	}
	    	}

	    	//Add other identifiers of the identifiable object
	    	collectedProperties.putAll(getCustomIdentifiersModelAsMap());

	    	return collectedProperties;
	    }


	    /**
	     * Represent all custom identifiers of the identifiable object MODEL as a HashMap
	     * @return
	     */
	    public HashMap<String, String> getCustomIdentifiersModelAsMap(){
			HashMap<String, String> collectedIdentifiers = new HashMap<String, String>();
			for(Identifier identifier: identifiableObject.getIdentifiers()){
				boolean isIgnored = false;
				for(IgnoredIdentifiers ignoredIdent: Arrays.asList(IgnoredIdentifiers.class.getEnumConstants())){
					if(identifier.getType().getStringValue() == ignoredIdent.getLabel() ){
						isIgnored = true;
						break;
					}
				}
				if(!isIgnored){
					collectedIdentifiers.put(identifier.getType().getStringValue(), identifier.getId());
				}
			}
			return collectedIdentifiers;
		}


		/**
		 * Prepare person standard properties for drawing
		 */
		private void updatePersonStandardPropertyLines(){

			Person person = (Person)identifiableObject;
			standardPropertyLines.clear();
			
			String[] nameIdentifiers = {
//UNCOMMMENT					
//					PersonStandardProperties.NAME.getLabel(),
//					PersonStandardProperties.FIRSTNAME_LASTNAME.getLabel(),
//					PersonStandardProperties.ALIAS.getLabel()
					};

			JComboBox nameCombo = new JComboBox(nameIdentifiers);
//UNCOMMENT			
//			nameCombo.setSelectedItem(PersonStandardProperties.NAME.getLabel());
			String nameComboValue = "";

			//Prepare name-identifiers for the combobox (Name/Firstname-Lastname/Alias)
			boolean firstFound=true;
			for(String name: nameIdentifiers){
				for(Identifier identifier: person.getIdentifiers()){
					if(identifier.getType().getStringValue() == name){
						if(firstFound){			//save only the first identifier, all other must be removed (for model-view correlation)
							nameCombo.setSelectedItem(name);
				    		nameComboValue = person.getIdentifier(name).getId();
				    		firstFound = false;
						} else {				//only 1 identifier from the list must be here, all other must be removed
							person.removeIdentifier(identifier);
						}
					}
				}
			}

			//Prepare role combobox
			List<String> roleValues = new ArrayList<>();
//UNCOMMENT			
//			for(DefaultPersonRole role: Arrays.asList(DefaultPersonRole.class.getEnumConstants())){
//				roleValues.add(role.getLabel());
//			}
			JComboBox roleCombo = new JComboBox(roleValues.toArray());
			if(((Person)identifiableObject).getRole() != null){
				roleCombo.setSelectedItem(((Person)identifiableObject).getRole());
			}
//			else {
//				//TODO: should we apply default Role if the original one == null ?
//				//((Person)identifiableObject).setRole(roleCombo.getSelectedItem().toString());
//				System.out.println(((Person)identifiableObject).getRole());
//			}

			//Create new property lines
			standardPropertyLines.add(new PropertyLine(
					nameCombo,
					new JTextField(nameComboValue)
					));

			standardPropertyLines.add(new PropertyLine(
					new JLabel(PersonStandardProperties.ROLE.getLabel()),
					roleCombo
					//new JTextField(((Person)identifiableObject).getRole())
					));

	    	standardPropertyLines.add(new PropertyLine(
					new JLabel(PersonStandardProperties.DESCRIPTION.getLabel()),
					new JScrollPane(new JTextArea(((Person)identifiableObject).getDescription(), 4, 10))
					));
	    }

		//TODO: check
		/**
		 * Prepare tool standard properties for drawing
		 */
		private void updateToolStandardPropertyLines(){

			standardPropertyLines.clear();

			String nameVersionValue = "";

			//Parse all identifiers to save a standard property value.
			for(Identifier identifier: ((Tool)identifiableObject).getIdentifiers()){
//UNCOMMENT				
//				if(identifier.getType().getStringValue() == ToolStandardProperties.NAME_VERSION.getLabel()){
//					nameVersionValue = identifier.getId();
//				}
			}

			//Create new property lines
//UNCOMMENT			
//			standardPropertyLines.add(new PropertyLine(
//						new JLabel(ToolStandardProperties.NAME_VERSION.getLabel()),
//						new JTextField(nameVersionValue)
//						));

			standardPropertyLines.add(new PropertyLine(
						new JLabel(PersonStandardProperties.DESCRIPTION.getLabel()),
						new JScrollPane(new JTextArea(((Tool)identifiableObject).getDescription(), 4, 10))
						));

			standardPropertyLines.add(new PropertyLine(
						new JLabel(ToolStandardProperties.ENVIRONMENT.getLabel()),
						new JTextField(((Tool)identifiableObject).getEnvironment())
						));

			standardPropertyLines.add(new PropertyLine(
						new JLabel(ToolStandardProperties.PARAMETERS.getLabel()),
						new JTextField(((Tool)identifiableObject).getParameters())
						));
		}


		/**
		 * Prepare resource (input/output) standard properties for drawing
		 */
		private void updateResourceStandardPropertyLines(){

			standardPropertyLines.clear();
			String nameVersionValue = null;
			String pathValue = null;
			String urlValue = null;

			//TODO: check case when property/identifier was not defined before!
			//TODO: check not null values -> not needed, because after "apply" null values will be saved correct!

			//Parse all identifiers to save a standard property value.
			for(Identifier identifier: ((Resource)identifiableObject).getIdentifiers()){
//UNCOMMENT
//				if(identifier.getType().getStringValue() == ResourceStandardProperties.NAME_VERSION.getLabel()){
//					nameVersionValue = identifier.getId();
//				} else if(identifier.getType().getStringValue() == ResourceStandardProperties.PATH.getLabel()){
//					pathValue = identifier.getId();
//				} else if(identifier.getType().getStringValue() == ResourceStandardProperties.URL.getLabel()){
//					urlValue = identifier.getId();
//				}
			}

			//Prepare type combobox
			List<String> typeValues = new ArrayList<>();
//UNCOMMENT			
//			for(DefaultResourceType resourceType: Arrays.asList(DefaultResourceType.class.getEnumConstants())){
//				typeValues.add(resourceType.getDataCiteName());
//			}
			JComboBox typeCombo = new JComboBox(typeValues.toArray());
			if(((Resource)identifiableObject).getResourceType() != null){
				typeCombo.setSelectedItem(((Resource)identifiableObject).getResourceType());
			}
//			else {
//				//TODO: should we apply default type if the original one == null?
//
//				// why? because there are problems during "remove identifiable"
//				//((Resource)identifiableObject).setResourceType(typeCombo.getSelectedItem().toString());
//				System.out.println(((DefaultResource)identifiableObject).getResourceType());
//			}

			//Create new property lines, even if parameters == null --> after "apply" values will be saved properly!
//UNCOMMENT			
//			standardPropertyLines.add(new PropertyLine(
//						new JLabel(ResourceStandardProperties.NAME_VERSION.getLabel()),
//						new JTextField(nameVersionValue)
//						));

			standardPropertyLines.add(new PropertyLine(
						new JLabel(ResourceStandardProperties.RESOURCETYPE.getLabel()),
						typeCombo
						));

			standardPropertyLines.add(new PropertyLine(
						new JLabel(ResourceStandardProperties.DESCRIPTION.getLabel()),
						new JScrollPane(new JTextArea(((Resource)identifiableObject).getDescription(), 4, 10))
						));

//UNCOMMENT			
//			standardPropertyLines.add(new PropertyLine(
//						new JLabel(ResourceStandardProperties.PATH.getLabel()),
//						new JTextField(pathValue)
//						));
//
//			standardPropertyLines.add(new PropertyLine(
//						new JLabel(ResourceStandardProperties.URL.getLabel()),
//						new JTextField(urlValue)
//						));
		}


		/**
		 * Prepare custom identifiers for drawing
		 */
		private void updateCustomIdentifierLines(){

			T element = identifiableObject;
			customIdentifierLines.clear();

			for (Identifier identifier: element.getIdentifiers()){
				boolean isStandardProperty = false;

				//is standard property for person?
				if (element instanceof Person){
					for(PersonStandardProperties prop: Arrays.asList(PersonStandardProperties.class.getEnumConstants())){
						if(identifier.getType().getStringValue() == prop.getLabel() ){
							isStandardProperty = true;
							break;
						}
					}

				//is standard property for tool?
				} else if (element instanceof Tool){
					for(ToolStandardProperties prop: Arrays.asList(ToolStandardProperties.class.getEnumConstants())){
						if(identifier.getType().getStringValue() == prop.getLabel() ){
							isStandardProperty = true;
							break;
						}
					}

				//is standard property for resource?
				} else if (element instanceof Resource){
					for(ResourceStandardProperties prop: Arrays.asList(ResourceStandardProperties.class.getEnumConstants())){
						if(identifier.getType().getStringValue() == prop.getLabel() ){
							isStandardProperty = true;
							break;
						}
					}
				}

				//is ignored identifier?
				for(IgnoredIdentifiers ignoredIdent: Arrays.asList(IgnoredIdentifiers.class.getEnumConstants())){
					if(identifier.getType().getStringValue() == ignoredIdent.getLabel() ){
						isStandardProperty = true; //use the same flag, because meaning is the same
						break;
					}
				}

				//if not a standard property (and not ignored identifier), then update it
				if(!isStandardProperty){
					customIdentifierLines.add(new PropertyLine(
							new JLabel(identifier.getType().getStringValue()),
							new JTextField(identifier.getId()),
							createButton(buttonNameRemoveIdentifier, iconRemove, buttonPreferredSize, actionListenerGroupElement)
							));
				}
			}
	    }


		/**
		 * Apply standard properties from the GUI-forms to the identifiable (person/tool/resources)
		 */
		public void applyStandardPersonPropertiesToIdentifiable(){

			HashMap<String,String> collectedStandardProperties = this.getStandardPropertiesViewAsMap();
	    	Person person = (Person)this.identifiableObject;

	    	//Remove Name/Alias/Firstname-Lastname identifiers (to use only one of them)
	    	for(Identifier identifier: person.getIdentifiers()){
//UNCOMMENT    			
//	    		if((identifier.getType().getStringValue() == PersonStandardProperties.NAME.getLabel())
//	    			|| (identifier.getType().getStringValue() == PersonStandardProperties.ALIAS.getLabel())
//	    			|| (identifier.getType().getStringValue() == PersonStandardProperties.FIRSTNAME_LASTNAME.getLabel())){
//
//    				person.removeIdentifier(identifier);
//        		}
    		}

	    	//Iterate through the property map and check names
	    	for (String propertyName: collectedStandardProperties.keySet()){

	    		if(propertyName == PersonStandardProperties.ROLE.getLabel()){
	    			person.setRole(collectedStandardProperties.get(propertyName));
	    		}
	    		else if(propertyName == PersonStandardProperties.DESCRIPTION.getLabel()){
	    			person.setDescription(collectedStandardProperties.get(propertyName));
	    		}
//UNCOMMENT	    		
//	    		else if(propertyName == PersonStandardProperties.NAME.getLabel()){
//	    			person.addIdentifier(new Identifier(DefaultPersonIdentifierType.NAME,
//	    							collectedStandardProperties.get(propertyName),
//			    					"")); //with empty context
//	    		}
//	    		else if(propertyName == PersonStandardProperties.ALIAS.getLabel()){
//	    			person.addIdentifier(new Identifier(DefaultPersonIdentifierType.ALIAS,
//	    							collectedStandardProperties.get(propertyName),
//			    					"")); //with empty context
//	    		}
//	    		else if(propertyName == PersonStandardProperties.FIRSTNAME_LASTNAME.getLabel()){
//	    			person.addIdentifier(new Identifier(DefaultPersonIdentifierType.FIRSTNAME_LASTNAME,
//	    							collectedStandardProperties.get(propertyName),
//			    					"")); //with empty context
//	    		}
	    	}
	    	updatePersonStandardPropertyLines();
	    }

		/**
		 * Apply standard tool properties to the tool object
		 */
		public void applyStandardToolPropertiesToIdentifiable(){

			HashMap<String,String> collectedStandardProperties = this.getStandardPropertiesViewAsMap();
	    	DefaultTool tool = (DefaultTool)this.identifiableObject;

	    	//Remove all identifiers, which are used as standard property
	    	for(ToolStandardProperties standardProperty: Arrays.asList(ToolStandardProperties.class.getEnumConstants())){
	    		for(Identifier identifier: tool.getIdentifiers()){
	    			if(identifier.getType().getStringValue() == standardProperty.getLabel()){
	        			tool.removeIdentifier(identifier);
	        		}
	    		}
			}

	    	//Iterate through the property map and check names
	    	for (String propertyName: collectedStandardProperties.keySet()){

//UNCOMMENT	    		
//	    		if(propertyName == ToolStandardProperties.NAME_VERSION.getLabel()){
//	    			tool.addIdentifier(new Identifier(DefaultResourceIdentifierType.NAME_VERSION,
//									collectedStandardProperties.get(propertyName),
//			    					"")); //with empty context
//	    		}
//	    		else 
	    		if(propertyName == ToolStandardProperties.DESCRIPTION.getLabel()){
	    			tool.setDescription(collectedStandardProperties.get(propertyName));
	    		}
	    		else if(propertyName == ToolStandardProperties.ENVIRONMENT.getLabel()){
	    			tool.setEnvironment(collectedStandardProperties.get(propertyName));
	    		}
	    		else if(propertyName == ToolStandardProperties.PARAMETERS.getLabel()){
	    			tool.setParameters(collectedStandardProperties.get(propertyName));
	    		}
	    	}
	    	updateToolStandardPropertyLines();
		}


		/**
		 * Apply standard resource properties to the resource object
		 */
		public void applyStandardResourcePropertiesToIdentifiable(){

			HashMap<String,String> collectedStandardProperties = this.getStandardPropertiesViewAsMap();
	    	DefaultResource resource = (DefaultResource)this.identifiableObject;

	    	//Remove all identifiers, which are used as standard property
	    	for(ResourceStandardProperties standardProperty: Arrays.asList(ResourceStandardProperties.class.getEnumConstants())){
	    		for(Identifier identifier: resource.getIdentifiers()){
	    			if(identifier.getType().getStringValue() == standardProperty.getLabel()){
	        			resource.removeIdentifier(identifier);
	        		}
	    		}
			}

	    	//Iterate through the property map and check names
	    	for (String propertyName: collectedStandardProperties.keySet()){

//UNCOMMENT	    		
//	    		if(propertyName == ResourceStandardProperties.NAME_VERSION.getLabel()){
//	    			resource.addIdentifier(new Identifier(DefaultResourceIdentifierType.NAME_VERSION,
//									collectedStandardProperties.get(propertyName),
//			    					"")); //with empty context
//	    		}
//	    		else 
	    		if(propertyName == ResourceStandardProperties.RESOURCETYPE.getLabel()){
	    			resource.setResourceType(collectedStandardProperties.get(propertyName));
	    		}
	    		else if(propertyName == ResourceStandardProperties.DESCRIPTION.getLabel()){
	    			resource.setDescription(collectedStandardProperties.get(propertyName));
	    		}
//UNCOMMENT	    		
//	    		else if(propertyName == ResourceStandardProperties.PATH.getLabel()){
//	    			resource.addIdentifier(new Identifier(DefaultResourceIdentifierType.PATH,
//							collectedStandardProperties.get(propertyName),
//	    					"")); //with empty context
//	    		}
//	    		else if(propertyName == ResourceStandardProperties.URL.getLabel()){
//	    			resource.addIdentifier(new Identifier(DefaultResourceIdentifierType.URL,
//							collectedStandardProperties.get(propertyName),
//	    					"")); //with empty context
//	    		}
	    	}
	    	updateResourceStandardPropertyLines();
		}


		/**
		 * Apply custom identifiers from GUI-forms to the identifiable (person/tool/resources)
		 */
    	public void applyCustomIdentifiersToIdentifiable(){

			HashMap<String,String> collectedIdentifiers = this.getCustomIdentifiersViewAsMap();
	    	T element = this.identifiableObject;

	    	//remove all custom identifiers of the identifiable
	    	for(Identifier identifier: element.getIdentifiers()){

	    		boolean isStandardProperty = false;
				//is standard property for person?
				if (element instanceof Person){
					for(PersonStandardProperties prop: Arrays.asList(PersonStandardProperties.class.getEnumConstants())){
						if(identifier.getType().getStringValue() == prop.getLabel() ){
							isStandardProperty = true;
							break;
						}
					}
				//is standard property for tool?
				} else if (element instanceof Tool){
					for(ToolStandardProperties prop: Arrays.asList(ToolStandardProperties.class.getEnumConstants())){
						if(identifier.getType().getStringValue() == prop.getLabel() ){
							isStandardProperty = true;
							break;
						}
					}
				//is standard property for resource?
				} else if (element instanceof Resource){
					for(ResourceStandardProperties prop: Arrays.asList(ResourceStandardProperties.class.getEnumConstants())){
						if(identifier.getType().getStringValue() == prop.getLabel() ){
							isStandardProperty = true;
							break;
						}
					}
				}
				//is ignored identifier?
				for(IgnoredIdentifiers ignoredIdent: Arrays.asList(IgnoredIdentifiers.class.getEnumConstants())){
					if(identifier.getType().getStringValue() == ignoredIdent.getLabel() ){
						isStandardProperty = true;
						break;
					}
				}
				//if not standard property (and not ignored identifier), than remove it
				if(!isStandardProperty){
					element.removeIdentifier(identifier);
				}
	    	}

	    	//Create new identifiers based on the "view" - collectedIdentifiers
	    	for (String propertyName: collectedIdentifiers.keySet()){
        			String id = collectedIdentifiers.get(propertyName);
        			IdentifierType type = this.getIdentifierTypeByString(propertyName);
        			element.addIdentifier(new Identifier(type, id, "")); //with empty context
	       	}

	    	updateCustomIdentifierLines();
    	}

    	/**
    	 * Get type of the identifier as IdentifierType basing on the String value of the type
    	 *
    	 * @param typeName - String value of the identifier type
    	 * @return
    	 */
    	private IdentifierType getIdentifierTypeByString(String typeName){

    		IdentifierType identifierType = null;
    		T element = this.identifiableObject;

//UNCOMMENT    		
//    		//Person
//    		if(element instanceof Person){
//    			for(DefaultPersonIdentifierType allowedIdentifierType: Arrays.asList(DefaultPersonIdentifierType.class.getEnumConstants())){
//    				if(typeName == allowedIdentifierType.getLabel()){
//    					identifierType = allowedIdentifierType;
//    				}
//    			}
//    		//Resource (Tool, Input, Output)
//			} else if (element instanceof Resource){
//				for(DefaultResourceIdentifierType allowedIdentifierType: Arrays.asList(DefaultResourceIdentifierType.class.getEnumConstants())){
//    				if(typeName == allowedIdentifierType.getLabel()){
//    					identifierType = allowedIdentifierType;
//    				}
//    			}
//			}

    		return identifierType;
    	}


    	/**
         * Check, if property group element has changes between model and view
         *
         * @return true if there are some changes, otherwise - false
         */
        public boolean hasChanges(){
        	Map<String, String> collectedPropertiesView = this.getPropertiesAndIdentifiersViewAsMap();
            Map<String, String> collectedPropertiesModel = this.getPropertiesAndIdentifiersModelAsMap();
            if (!collectedPropertiesModel.equals(collectedPropertiesView)){
            	return true;
            }
        	return false;
        }

	}



    /**
     * One line of the editor. To edit a concrete property/identifier.
     * Can include: label-component, edit-component, 0-4 buttons (option).
     * Buttons:
     * - add (add resource)
     * - remove (remove resource)
     * - ...
     *
     * @author vk
     */
    private class PropertyLine {

    	private JComponent labelComponent;
    	private JComponent editComponent;
        private List<JButton> buttons;

        public PropertyLine( 	JComponent labelComponent,
								JComponent editComponent,
								JButton... buttons
								){
			this.labelComponent = labelComponent;
			this.editComponent = editComponent;
			this.buttons = new ArrayList<>();
			for (JButton btn : buttons){
				this.buttons.add(btn);
			}
		}

		/**
		 * Get value of the component as a string
		 * @return
		 */
		public String getEditComponentAsString(){

			String output = null;

			//JTextFiled
			if (editComponent instanceof JTextField){
				output = ((JTextField)editComponent).getText();

			//JComboBox
			} else if(editComponent instanceof JComboBox) {
				//"toString()" works, because there are ONLY strings inside the combobox
				Object selectedItem = ((JComboBox)editComponent).getSelectedItem();
				if(selectedItem!=null) {
					output = selectedItem.toString();
				}

			//JTextArea
			} else if (editComponent instanceof JTextArea) {
				output = ((JTextArea)editComponent).getText();

			//JScrollPane
			} else if (editComponent instanceof JScrollPane){
				for(Component comp: editComponent.getComponents()){
					if (comp instanceof JViewport) {
						Component viewComp = ((JViewport)comp).getView();
						if (viewComp instanceof JTextArea){
							output = ((JTextArea)viewComp).getText();
						}
					}
				}
			}
			return output;
		}

		/**
		 * Represent a label component (JLabel, JComboBox) as a String
		 * @return String
		 */
		public String getLabelComponentAsString(){

			String output = null;
			//JLabel
			if(getLabelComponent() instanceof JLabel){
				output = ((JLabel)getLabelComponent()).getText();
			//JComboBox
			} else if(getLabelComponent() instanceof JComboBox) {
				//"toString()" works, because there are ONLY strings inside the combobox
//UNCOMMENT				
//				output = ((JComboBox)getLabelComponent()).getSelectedItem().toString();
			}
			return output;
		}

		/**
		 * Get LabelComponent
		 * @return
		 */
       	public JComponent getLabelComponent() {
			return labelComponent;
		}

       	/**
       	 * Get EditComponent
       	 * @return
       	 */
		public JComponent getEditComponent() {
			return editComponent;
		}

		/**
		 * Get array of Buttons
		 * @return
		 */
		public JButton[] getButtons(){
			return buttons.toArray(new JButton[0]);
		}

    }
}

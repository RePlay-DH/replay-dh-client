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

import static java.util.Objects.requireNonNull;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.text.JTextComponent;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.io.LocalFileObject;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.core.FileResolutionWorker;
import bwfdm.replaydh.ui.core.ResourceDragController;
import bwfdm.replaydh.ui.core.ResourceDragController.Mode;
import bwfdm.replaydh.ui.helper.DocumentAdapter;
import bwfdm.replaydh.ui.helper.Editor;
import bwfdm.replaydh.ui.helper.EditorControl;
import bwfdm.replaydh.ui.helper.ScrollablePanel;
import bwfdm.replaydh.ui.helper.ScrollablePanel.ScrollableSizeHint;
import bwfdm.replaydh.ui.helper.WrapLayout;
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.ui.workflow.auto.AutoCompletionWizardWorkflowStep;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifiable.Role;
import bwfdm.replaydh.workflow.Identifiable.Type;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Person;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.Tool;
import bwfdm.replaydh.workflow.WorkflowStep;
import bwfdm.replaydh.workflow.catalog.MetadataCatalog;
import bwfdm.replaydh.workflow.catalog.MetadataCatalog.QuerySettings;
import bwfdm.replaydh.workflow.impl.DefaultPerson;
import bwfdm.replaydh.workflow.impl.DefaultResource;
import bwfdm.replaydh.workflow.impl.DefaultTool;
import bwfdm.replaydh.workflow.impl.DefaultWorkflow;
import bwfdm.replaydh.workflow.impl.DefaultWorkflowStep;
import bwfdm.replaydh.workflow.schema.IdentifierSchema;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * An editor for the workflow step properties
 *
 * @author Volodymyr Kushnarenko
 *
 */
public class WorkflowStepUIEditor implements Editor<WorkflowStep>, ActionListener {

	// Different labels
//	private final String titleKey = ResourceManager.getInstance().get("replaydh.labels.title");
//    private final String descriptionKey = ResourceManager.getInstance().get("replaydh.labels.description");
//
//    private final String personsBorderTitle = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.labels.personsBorder");
//    private final String toolBorderTitle = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.labels.toolsBorder");
//    private final String inputResourcesBorderTitle = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.labels.inputBorder");
//    private final String outputResourcesBorderTitle = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.labels.outputBorder");

    private final String personElementHeader = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.labels.personElementHeader");
    private final String toolElementHeader = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.labels.toolElementHeader");
    private final String inputResourceElementHeader = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.labels.inputElementHeader");
    private final String outputResourceElementHeader = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.labels.outputElementHeader");

    // Input text fields
    private JTextField titleTextField;
	private JTextArea descriptionTextArea;
    Dimension buttonPreferredSize = new Dimension(16,16);
    Dimension buttonBigPreferredSize = new Dimension(32,32);

    private MetadataCatalog search;

    private JPopupMenu popupTitle;
    private JPopupMenu popupDescription;

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

    List<Person> sortedPersons;
    List<Resource> sortedInputs;
    List<Resource> sortedOutputs;
    List<Tool> sortedTools;

    // Workflow step
    private WorkflowStep workflowStepEditable;
    private WorkflowStep workflowStepOrig;

    private WorkflowSchema schema;

    // Editor panel, the main element, should be always returned as the same element.
    private JComponent editorComponent;

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

    private Border emptyBorder = BorderFactory.createEmptyBorder(2, 2, 2, 2);
    private Border lineBorder = BorderFactory.createLineBorder(Color.BLACK, 1, false);

    private ActionListener actionListenerIdentifiableEditorElement;		//for group buttons: remove, edit, more/less
    private ActionListener actionListenerAddIdentifiable;	//for "add new identifiable" button

    private final String defaultInputString = ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.labels.hintInput");

    private boolean contentChanged;
    private JScrollPane descriptionScrollPane;

    private EditorControl editorControl;
    private boolean inputCorrect;

    private AutoCompletionWizardWorkflowStep autoWizard;


    private Timer waitingTimer;

	private ActionListener taskPerformer = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent evt) {
			QuerySettings settings = new QuerySettings();
			settings.setSchema(schema);
			if (titleTextField.hasFocus()) {
				suggestSearch(settings, null, "title", titleTextField.getText());
			} else if (descriptionTextArea.hasFocus()) {
				suggestSearch(settings, null, "description", descriptionTextArea.getText());
			}
		}
	};


    private FocusListener focusListener = new FocusListener() {

		@Override
		public void focusLost(FocusEvent e) {
			if((e.getComponent()instanceof JTextComponent)
						&& (((JTextComponent)e.getComponent()).getText().equals(""))) {

				((JTextComponent)e.getComponent()).setText(defaultInputString); //set predefined string instead of empty string
			}
			waitingTimer.stop();
		}

		@Override
		public void focusGained(FocusEvent e) {
			if((e.getComponent()instanceof JTextComponent)
						&& (((JTextComponent)e.getComponent()).getText().equals(defaultInputString))) {

				((JTextComponent)e.getComponent()).setText(""); //set empty string instead of predefined string
			}
			waitingTimer.stop();
		}
	};

    private final RDHEnvironment environment;

    /**
     * Constructor, WorkflowStepUIEditor
     */
    public WorkflowStepUIEditor(RDHEnvironment environment){

    	this.environment = requireNonNull(environment);

    	ResourceManager rm = ResourceManager.getInstance();

    	search = this.environment.getClient().getMetadataCatalog();

    	// Set correct schema in the "setEdititngItem" method
    	schema = environment.getWorkspace().getSchema();//set workflow schema

    	contentChanged = false;
    	inputCorrect = true;

    	// Create Lists of IdentifiableEditorElements
      	personEditorElementsList = new ArrayList<IdentifiableEditorElement<Person>>();
    	inputResourceEditorElementsList = new ArrayList<IdentifiableEditorElement<Resource>>();
    	outputResourceEditorElementsList = new ArrayList<IdentifiableEditorElement<Resource>>();
    	toolEditorElementsList = new ArrayList<IdentifiableEditorElement<Tool>>(1);

    	sortedPersons = new ArrayList<Person>();
    	sortedTools = new ArrayList<Tool>();
    	sortedInputs = new ArrayList<Resource>();
    	sortedOutputs = new ArrayList<Resource>();

    	titleTextField = new JTextField(defaultInputString);
    	titleTextField.getDocument().addDocumentListener(new DocumentAdapter() {
			@Override
			public void anyUpdate(DocumentEvent e) {
				onDocumentChange(titleTextField);
			}
		});
    	titleTextField.addFocusListener(focusListener);
    	GuiUtils.prepareChangeableBorder(titleTextField);

    	popupTitle = new JPopupMenu();
    	titleTextField.add(popupTitle);
    	titleTextField.setComponentPopupMenu(popupTitle);

    	descriptionTextArea = new JTextArea(defaultInputString);
    	descriptionTextArea.getDocument().addDocumentListener(new DocumentAdapter() {
			@Override
			public void anyUpdate(DocumentEvent e) {
				onDocumentChange(descriptionTextArea);
			}
		});
    	descriptionTextArea.addFocusListener(focusListener);
    	descriptionTextArea.setRows(4);
    	descriptionTextArea.setColumns(10);
    	GuiUtils.prepareChangeableBorder(descriptionTextArea);
    	descriptionScrollPane = new JScrollPane(descriptionTextArea);
    	popupDescription = new JPopupMenu();
    	titleTextField.add(popupDescription);
    	titleTextField.setComponentPopupMenu(popupDescription);

    	// General action listener for each propertyGroupElement
        actionListenerIdentifiableEditorElement = this;

        // Add new identifiable (Person/Resource/Tool)
        actionListenerAddIdentifiable = this;

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

    	personsPanel = new CategoryPanel(Role.PERSON, btnAddPerson, btnAddAutoPerson);
    	toolPanel = new CategoryPanel(Role.TOOL, btnAddTool, btnAddAutoTool);
    	inputResourcesPanel = new CategoryPanel(Role.INPUT, btnAddInputResource, btnAddAutoInputResource);
    	outputResourcesPanel = new CategoryPanel(Role.OUTPUT, btnAddOutputResource, btnAddAutoOutputResource);

    	/**
    	 * <pre>
    	 * +-----------------------------+
    	 * |TITLE:        _____________  |
    	 * |DESC :        _____________  |
    	 * |              _____________  |
    	 * +-----------------------------+
    	 * | PERSONS                ADD  |
    	 * +-----------------------------+
    	 * | TOOL                   ADD  |
    	 * +-----------------------------+
    	 * | INPUT                  ADD  |
    	 * +-----------------------------+
    	 * | OUTPUT                 ADD  |
    	 * +-----------------------------+
    	 * </pre>
    	 */

    	// Build main panel

    	scrollablePanel = new ScrollablePanel();
    	scrollablePanel.setScrollableWidth(ScrollableSizeHint.FIT);

    	FormBuilder.create()
    		.columns("pref, 6dlu, pref:grow:fill, 2dlu, pref")
    		.rows("pref, $nlg, pref, $nlg, pref, pref, pref, pref")
    		.panel(scrollablePanel)
    		.padding(Paddings.DLU4)

    		.addLabel(rm.get("replaydh.labels.title")).xy(1, 1)
    		.add(titleTextField).xyw(3, 1, 3)

    		.addLabel(rm.get("replaydh.labels.description")).xy(1, 3)
    		.add(descriptionScrollPane).xyw(3, 3, 3)

//    		.addSeparator(rm.get("replaydh.ui.editor.workflowStep.labels.personsBorder")).xyw(1, 5, 3)
//    		.add(btnAddPerson).xy(5, 5)
    		.add(personsPanel).xyw(1, 5, 5)

//    		.addSeparator(rm.get("replaydh.ui.editor.workflowStep.labels.toolsBorder")).xyw(1, 7, 3)
//    		.add(btnAddTool).xy(5, 7)
    		.add(toolPanel).xyw(1, 6, 5)

//    		.addSeparator(rm.get("replaydh.ui.editor.workflowStep.labels.inputBorder")).xyw(1, 9, 3)
//    		.add(btnAddInputResource).xy(5, 9)
    		.add(inputResourcesPanel).xyw(1, 7, 5)

//    		.addSeparator(rm.get("replaydh.ui.editor.workflowStep.labels.outputBorder")).xyw(1, 11, 3)
//    		.add(btnAddOutputResource).xy(5, 11)
    		.add(outputResourcesPanel).xyw(1, 8, 5)

    		.build();

    	JScrollPane scrollPane = new JScrollPane(scrollablePanel);
    	scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

//    	editorComponent = new JPanel(new BorderLayout()); //the main panel with all components, it will be sent as output in "getEditorComponent()" method
//    	editorComponent.add(scrollPane, BorderLayout.CENTER);

    	editorComponent = scrollPane;
    	editorComponent.setPreferredSize(new Dimension(600, 600));
    	editorComponent.revalidate();
    	editorComponent.setMinimumSize(new Dimension(600, 600));

    	waitingTimer = new Timer(1000, taskPerformer);
    	waitingTimer.setRepeats(false);

    }

    private void scheduleInputVerification() {
    	GuiUtils.invokeEDTLater(() -> verifyEntireInput(false));
    }

    private boolean isValidText(String s) {
    	return s!=null
    			&& !s.trim().isEmpty()
    			&& !s.equals(defaultInputString);
    }

    private void onDocumentChange(JTextComponent comp) {
    	boolean valid = verifyInput(comp);
    	GuiUtils.toggleChangeableBorder(comp, !valid);
    	if(valid) {
    		updateTimer(comp.isFocusOwner());
    	}
    	scheduleInputVerification();
    }

    private void updateTimer(boolean compHasFocus) {
		if (compHasFocus) {
			if (waitingTimer.isRunning()) {
				waitingTimer.restart();
			} else {
				waitingTimer.start();
			}
		} else {
			waitingTimer.stop();
		}
    }

    private <I extends Identifiable> boolean verifyIdentifiables(List<IdentifiableEditorElement<I>> elements) {
    	boolean valid = true;
    	for(IdentifiableEditorElement<I> element : elements) {
    		valid &= element.isContentValid();
    		if(!valid) {
    			break;
    		}
    	}
    	return valid;
    }

    /**
     * Verifies content of given text component
     */
    private boolean verifyInput(JTextComponent comp) {
    	return isValidText(comp.getText());
    }

    /**
     * Verification of the input in all fields. Set red border to each input field and deactivate "OK" button in case of error.
     */
    private void verifyEntireInput(boolean force) {
    	inputCorrect = true;

    	// Title
    	if(inputCorrect || force) {
    		inputCorrect &= verifyInput(titleTextField);
    	}

    	// Description
    	if(inputCorrect || force) {
    		inputCorrect &= verifyInput(descriptionTextArea);
    	}

    	if(inputCorrect || force) {
    		inputCorrect &= verifyIdentifiables(personEditorElementsList);
    	}
    	if(inputCorrect || force) {
    		inputCorrect &= verifyIdentifiables(inputResourceEditorElementsList);
    	}
    	if(inputCorrect || force) {
    		inputCorrect &= verifyIdentifiables(outputResourceEditorElementsList);
    	}
    	if(inputCorrect || force) {
    		inputCorrect &= verifyIdentifiables(toolEditorElementsList);
    	}

    	updateControl();
    }

    private void updateControl() {
    	// Disable or enable an "Apply" button based on the "inputCorrect" state
    	if(editorControl!=null) {
    		editorControl.setApplyEnabled(inputCorrect);
    	}
    }


    /**
     * Set {@link EditorControl} object to manipulate the Apply-button of the editor
     */
    @Override
    public void setControl(EditorControl control) {

    	editorControl = control;

    	// Verify input immediately to allow "editorControl.setApplyEnable(false)" in case of incorrect input
    	verifyEntireInput(true);
    }

    /**
     * Analyze button clicks
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        // Add new identifiable (person, tool, resource)
        if(source == btnAddPerson){
        	applyLocalEditors(personEditorElementsList); //Apply local editors to avoid possible conflicts
        	DefaultPerson person = createNewPerson();
        	if(callIdentifiableEditorDialogue(this.schema, Type.PERSON, person)) {
				addIdentifiable(person, Role.PERSON);
        	}
        	return;

		} else if (source == btnAddTool){
			applyLocalEditors(toolEditorElementsList); //Apply local editors to avoid possible conflicts
        	DefaultTool tool = createNewTool();
			if(callIdentifiableEditorDialogue(this.schema, Type.TOOL, tool)) {
				addIdentifiable(tool, Role.TOOL);
			}
			return;

		} else if (source == btnAddInputResource){
			applyLocalEditors(inputResourceEditorElementsList); //Apply local editors to avoid possible conflicts
        	DefaultResource input = createNewResource();
			if(callIdentifiableEditorDialogue(this.schema, Type.RESOURCE, input)) {
				addIdentifiable(input, Role.INPUT);
			}
			return;

		} else if (source == btnAddOutputResource){
			applyLocalEditors(outputResourceEditorElementsList); //Apply local editors to avoid possible conflicts
        	DefaultResource output = createNewResource();
			if(callIdentifiableEditorDialogue(this.schema, Type.RESOURCE, output)) {
				addIdentifiable(output, Role.OUTPUT);
			}
			return;
		}



        if(source == btnAddAutoPerson){
        	autoWizard = new AutoCompletionWizardWorkflowStep(this.schema, environment, this);
        	autoWizard.createWizard(this.schema, Role.PERSON, Type.PERSON);
        	return;

		} else if (source == btnAddAutoTool){
			autoWizard = new AutoCompletionWizardWorkflowStep(this.schema, environment, this);
			autoWizard.createWizard(this.schema, Role.TOOL, Type.TOOL);
			return;

		} else if (source == btnAddAutoInputResource){
			autoWizard = new AutoCompletionWizardWorkflowStep(this.schema, environment, this);
			autoWizard.createWizard(this.schema, Role.INPUT, Type.RESOURCE);
			return;

		} else if (source == btnAddAutoOutputResource){
			autoWizard = new AutoCompletionWizardWorkflowStep(this.schema, environment, this);
			autoWizard.createWizard(this.schema, Role.OUTPUT, Type.RESOURCE);
			return;
		}

        // Create a List of all propertyGroups to iterate later only through 1 list
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
                    	}
            		}
            	}
        		break;
        	}
        }


    }//actionPerformed

    public void addIdentifiable(Identifiable identifiable, Role role) {
    	boolean found=false;
    	switch (role) {
		case PERSON:
			if(!(sortedPersons.isEmpty())) {
				for(Identifiable object : sortedPersons) {
					if(object.equals(identifiable)) {
						found=true;
					}
				}
			}
			if (found == false) {
				workflowStepEditable.addPerson((Person) identifiable);
	        	sortedPersons.add((Person) identifiable);
			}
			break;

		case TOOL:
			if(!(sortedTools.isEmpty())) {
				for(Identifiable object : sortedTools) {
					if(object.equals(identifiable)) {
						found=true;
					}
				}
			}
			if (found == false) {
				workflowStepEditable.setTool((Tool) identifiable);
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
				workflowStepEditable.addInput((Resource) identifiable);
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
				workflowStepEditable.addOutput((Resource) identifiable);
				sortedOutputs.add((Resource) identifiable);
			}
			contentChanged = true;
			break;

		default:
			break;
		}

		contentChanged = true;
		updateIdentifiableEditorElements(workflowStepEditable);
		updateEditorView();
    }

    /**
     * Apply possible changes in every local editor
     */
    private <T extends Identifiable> void applyLocalEditors(List<IdentifiableEditorElement<T>> identifiableEditorElementsList) {

    	for(IdentifiableEditorElement<T> element: identifiableEditorElementsList) {
    		element.getEditor().applyEdit();
    	}
    }


    /**
	 * Create/Update the whole model of the editor.
	 * @param workflowstep
	 */
	public void updateIdentifiableEditorElements(WorkflowStep workflowStep){

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
				if(oldElement.getIdentifiableObject() == newElement.getIdentifiableObject()){
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
	 * Update the workflowStep model: title, description and all IdentifibleEditorElements
	 *
	 * @param workflowStep
	 */
    private void updateEditorModel(WorkflowStep workflowStep) {

    	updateText(titleTextField, workflowStep.getTitle());
    	updateText(descriptionTextArea, workflowStep.getDescription());
    	updateIdentifiableEditorElements(workflowStep); //TODO: remove workflowStep as input
    }

    private void updateText(JTextComponent comp, String text) {
    	if(text==null || text.trim().isEmpty()) {
    		text = defaultInputString;
    	}
    	comp.setText(text);
    	boolean valid = verifyInput(comp);
    	GuiUtils.toggleChangeableBorder(comp, !valid);
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

	/**
     * Create/update a panel of the editor. The main GUI method of the editor.
     *
     * @param editorComponent - a panel which must be the same variable
     * all the time because the panel will be always revalidated at the end.
     * That's why the best option is to use a private variable of the class "WorkflowStepUIEditor"
     *
     */
    public void updateEditorView(){

    	updatePanel(personsPanel, personEditorElementsList, personElementHeader);

    	updatePanel(toolPanel, toolEditorElementsList, toolElementHeader);

    	updatePanel(inputResourcesPanel, inputResourceEditorElementsList, inputResourceElementHeader);

    	updatePanel(outputResourcesPanel, outputResourceEditorElementsList, outputResourceElementHeader);

    	btnAddTool.setEnabled(toolEditorElementsList.size()<1);
    	Window window = SwingUtilities.getWindowAncestor(scrollablePanel);
    	if(window!=null) {
    		window.revalidate();
    		window.repaint();
    	}

    	scrollablePanel.requestFocusInWindow(); //put focus somewhere in Window, just to remove the focus from other JTextComponents
    }

    private IdentifiableEditor createIdentifiableEditor(WorkflowSchema schema, Identifiable.Type type) {
    	return IdentifiableEditor.newBuilder()
    			.environment(environment)
    			.schema(schema)
    			.type(type)
    			.useDefaultTitleSelector(true)
    			.titleEditable(true)
    			.build();
    }

    /**
     * Call {@link IdentifiableEditor} for some {@link Identifiable} as an extra modal dialogue
     *
     * @param schema
     * @param type
     * @param identifiableObject
     * @param frame
     * @return boolean, true="OK-button was pushed / or there are some changes", false="Cancel-button was pushed"
     */
    private <T extends Identifiable> boolean callIdentifiableEditorDialogue(WorkflowSchema schema, Identifiable.Type type, T identifiableObject) {

    	boolean result = false;
    	String title = type.getLabel();

    	IdentifiableEditor editor = createIdentifiableEditor(schema, type);
    	editor.setEditingItem(IdentifiableEditor.wrap(Collections.singleton(identifiableObject)));
    	Frame window = null;//GuiUtils.getFrame(this.editorPanel);
    	try {
			result = GuiUtils.showEditorDialogWithControl(window, editor, title, true);
		} catch(Exception e) {
			GuiUtils.beep();
			GuiUtils.showErrorDialog(window,
					"replaydh.ui.editor.workflowStep.dialogs.errorIdentifiableEditor.title",
					"replaydh.ui.editor.workflowStep.dialogs.errorIdentifiableEditor.message", e);
		}
    	return result;
    }


    /**
     * Create new Person with standard parameters
     *
     * @return DefaultPerson
     */
    private DefaultPerson createNewPerson(){

    	DefaultPerson person = DefaultPerson.uniquePerson();
    	return person;
    }

    /**
     * Create new Tool with standard parameters
     *
     * @return DefaultTool
     */
    private DefaultTool createNewTool(){

    	DefaultTool tool = DefaultTool.uniqueTool();
        return tool;
    }

    /**
     * Create new Resource with standard parameters
     *
     * @return DefaultResource
     */
    private DefaultResource createNewResource(){

    	DefaultResource resource = DefaultResource.uniqueResource();
    	return resource;
    }


    /**
     * Process "remove button"
     */
    private <T extends Identifiable> void processRemoveButtonPush(IdentifiableEditorElement<T> element, JButton removeButton){

    	if(JOptionPane.showOptionDialog(null,
    			ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.dialogs.removeElement.message"),
    			ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.dialogs.removeElement.title"),
    			JOptionPane.YES_NO_OPTION,
    			JOptionPane.PLAIN_MESSAGE,
    			null,
    			new String[]{ResourceManager.getInstance().get("replaydh.labels.yes"), ResourceManager.getInstance().get("replaydh.labels.no")},
    			"Default") == JOptionPane.YES_OPTION){

	    	if(removeButton.getName() == toolTipRemovePerson){
				applyLocalEditors(personEditorElementsList); //apply local editors
				workflowStepEditable.removePerson((Person)element.getIdentifiableObject());
	    		sortedPersons.remove((Person)element.getIdentifiableObject());
	    	} else if (removeButton.getName() == toolTipRemoveTool){
				applyLocalEditors(toolEditorElementsList); //apply local editors
				workflowStepEditable.setTool(null);
				sortedTools.remove((Tool)element.getIdentifiableObject());
			} else if (removeButton.getName() == toolTipRemoveInputResource){
				applyLocalEditors(inputResourceEditorElementsList); //apply local editors
				workflowStepEditable.removeInput((Resource)element.getIdentifiableObject());
				sortedInputs.remove((Resource)element.getIdentifiableObject());
			} else if (removeButton.getName() == toolTipRemoveOutputResource){
				applyLocalEditors(outputResourceEditorElementsList); //apply local editors
				workflowStepEditable.removeOutput((Resource)element.getIdentifiableObject());
				sortedOutputs.remove((Resource)element.getIdentifiableObject());
			}

	    	contentChanged = true;

	    	updateIdentifiableEditorElements(workflowStepEditable);
			updateEditorView();
    	}
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

        // Apply local editors to avoid possible conflicts
		applyLocalEditors(personEditorElementsList);
		applyLocalEditors(toolEditorElementsList);
		applyLocalEditors(inputResourceEditorElementsList);
		applyLocalEditors(outputResourceEditorElementsList);

		updateEditorModel(workflowStepEditable);
        updateEditorView();
        return editorComponent;
    }

    /**
     * Set editing item to the editor
     *
     * @see bwfdm.replaydh.ui.helper.Editor#setEditingItem(java.lang.Object)
     */
    @Override
    public void setEditingItem(WorkflowStep item) {

    	// Set a workflow schema
    	schema = item.getWorkflow().getSchema();

    	// Save original workflow step
    	workflowStepOrig = item;

    	// Create a new workflow step, which will be edited in this editor
    	//FIXME wtf is this?
    	workflowStepEditable = new DefaultWorkflowStep(new DefaultWorkflow(schema));

    	// Replace the main parameters of editable workflow step with parameters of the original workflow step.
    	copyWorkflowStepParameters(workflowStepOrig, workflowStepEditable);

    	sortedPersons.clear();
    	if(workflowStepEditable.getPersons() != null) {
    		sortedPersons.addAll(workflowStepEditable.getPersons());
    	}
    	sortedTools.clear();
    	if(workflowStepEditable.getTool() != null) {
    		sortedTools.add(workflowStepEditable.getTool());
    	}
    	sortedInputs.clear();
    	if(workflowStepEditable.getInput() != null) {
    		sortedInputs.addAll(workflowStepEditable.getInput());
    	}
    	sortedOutputs.clear();
    	if(workflowStepEditable.getOutput() != null) {
    		sortedOutputs.addAll(workflowStepEditable.getOutput());
    	}

    	updateEditorModel(workflowStepEditable);
    }


    /**
     * Replace parameters of the workflowStepDestination with properties/identifiers of the workflowStepSource.
     * All original parameters as persons, tool, input/output resources of the workflowStepDestination
     * will be removed and replaced with the same parameters of the workflowStepSource.
     *
     * The "system ID" will not be copied from the source to destination.
     *
     * @param workflowStepSource
     * @param workflowStepDestination
     */
    private void copyWorkflowStepParameters(final WorkflowStep workflowStepSource, WorkflowStep workflowStepDestination) {

    	workflowStepDestination.copyFrom(workflowStepSource);
    }


    /**
     * Get editing item from the editor
     *
     * @see bwfdm.replaydh.ui.helper.Editor#getEditingItem()
     */
    @Override
    public WorkflowStep getEditingItem() {
        return this.workflowStepOrig;
    }


    /**
     * Reset not saved (not applied) properties in the editor
     *
     * @see bwfdm.replaydh.ui.helper.Editor#resetEdit()
     */
    @Override
    public void resetEdit() {

    	// TODO: remove this dialog "Are you sure that you want to reset all changes?"
//    	if(hasChanges() && (JOptionPane.showOptionDialog(null,
//        			ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.dialogs.resetEditor.message"),
//        			ResourceManager.getInstance().get("replaydh.ui.editor.workflowStep.dialogs.resetEditor.title"),
//        			JOptionPane.YES_NO_OPTION,
//        			JOptionPane.PLAIN_MESSAGE,
//        			null,
//        			new String[]{ResourceManager.getInstance().get("replaydh.labels.yes"), ResourceManager.getInstance().get("replaydh.labels.no")},
//        			"Default") == JOptionPane.YES_OPTION) ){

        	// Apply local editors to avoid possible conflicts
    		applyLocalEditors(personEditorElementsList);
    		applyLocalEditors(toolEditorElementsList);
    		applyLocalEditors(inputResourceEditorElementsList);
    		applyLocalEditors(outputResourceEditorElementsList);

        	// Reset workflow to the original version
        	copyWorkflowStepParameters(workflowStepOrig, workflowStepEditable);

        	sortedPersons.clear();
        	sortedPersons.addAll(workflowStepEditable.getPersons());
        	sortedTools.clear();
        	sortedTools.add(workflowStepEditable.getTool());
        	sortedInputs.clear();
        	sortedInputs.addAll(workflowStepEditable.getInput());
        	sortedOutputs.clear();
        	sortedOutputs.addAll(workflowStepEditable.getOutput());

        	contentChanged = false;

        	updateEditorModel(workflowStepEditable);
	    	updateEditorView();
//    	}
    }


    /**
     * Apply changes of the properties.
     * Important: it changes variables "workflowStep" and "workflowStepOrig"
     *
     * @see bwfdm.replaydh.ui.helper.Editor#applyEdit()
     */
    @Override
    public void applyEdit() {

    	// TODO: think about "isInputCorrect" before apply. Could be, that "editorControl" is not activated...

    	workflowStepEditable.setTitle(titleTextField.getText());
    	workflowStepEditable.setDescription(descriptionTextArea.getText());

    	// Apply local editors to avoid possible conflicts
		applyLocalEditors(personEditorElementsList);
		applyLocalEditors(toolEditorElementsList);
		applyLocalEditors(inputResourceEditorElementsList);
		applyLocalEditors(outputResourceEditorElementsList);

    	// Replace original wfStep with the editable wfStep
    	copyWorkflowStepParameters(workflowStepEditable, workflowStepOrig);

    	contentChanged = false;
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

    	if(contentChanged) {
    		return true;
    	}

    	// Title
    	if(!workflowStepEditable.getTitle().equals(titleTextField.getText())) {
    		contentChanged = true;
    		return true;
    	}
    	// Description
    	if(!workflowStepEditable.getDescription().equals(descriptionTextArea.getText())) {
    		contentChanged = true;
    		return true;
    	}
    	// Persons
    	for(IdentifiableEditorElement<Person> element: personEditorElementsList) {
    		if(element.getEditor().hasChanges()) {
    			contentChanged = true;
    			return true;
    		}
    	}
    	// Tool
    	for(IdentifiableEditorElement<Tool> element: toolEditorElementsList) {
    		if(element.getEditor().hasChanges()) {
    			contentChanged = true;
    			return true;
    		}
    	}
    	// Input resources
    	for(IdentifiableEditorElement<Resource> element: inputResourceEditorElementsList) {
    		if(element.getEditor().hasChanges()) {
    			contentChanged = true;
    			return true;
    		}
    	}
    	// Output resources
    	for(IdentifiableEditorElement<Resource> element: outputResourceEditorElementsList) {
    		if(element.getEditor().hasChanges()) {
    			contentChanged = true;
    			return true;
    		}
    	}

    	// Default - there are no changes
    	return false;
    }


    /**
     * Close the editor without applying the changes
     *
     * @see bwfdm.replaydh.ui.helper.Editor#close()
     */
    @Override
    public void close() {

		// Remove important variables
		workflowStepEditable = null;
		workflowStepOrig = null;
		personEditorElementsList.clear();
		toolEditorElementsList.clear();
		inputResourceEditorElementsList.clear();
		outputResourceEditorElementsList.clear();

		sortedPersons.clear();
		sortedTools.clear();
		sortedInputs.clear();
		sortedOutputs.clear();

		popupTitle.setVisible(false);
		popupDescription.setVisible(false);

		return;

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
    }

    /**
     * Class to group all properties of each identifiable element,
     * e.g. 1 person, 1 resource, 1 tool...
     *
     * @author vk
     *
     */
    private class IdentifiableEditorElement<T extends Identifiable> implements EditorControl {

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
    		GuiUtils.prepareChangeableBorder(panel);

    		// Set identifiable editor
    		editor = createIdentifiableEditor(workflowSchema, this.identifiable.getType());
    		editor.setControl(this);
        	editor.setEditingItem(IdentifiableEditor.wrap(Collections.singleton(this.identifiable)));
    	}

    	public boolean isContentValid() {
    		return editor.isContentValid();
    	}

    	@Override
		public boolean isApplyEnabled() {
			return true;
		}

		@Override
		public boolean isResetEnabled() {
			return true;
		}

		@Override
		public void setApplyEnabled(boolean enabled) {
			GuiUtils.toggleChangeableBorder(panel, !enabled);

	    	scheduleInputVerification();
		}

		@Override
		public void setResetEnabled(boolean enabled) {
			// no-op
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


	private void suggestSearch(QuerySettings settings, Identifiable context, String key, String valuePrefix) {

		SwingWorker<Boolean, Object> worker = new SwingWorker<Boolean, Object>() {

			boolean success = true;
			List<String> results = null;

			@Override
			protected Boolean doInBackground() throws Exception {
				results = search.suggest(settings, null, key, valuePrefix);
				if (results.isEmpty()) {
					success = false;
				}
				return success;
			}

			@Override
			protected void done() {
				if (success) {
					switch(key) {
					case MetadataCatalog.TITLE_KEY:
						popupTitle.removeAll();
						for(String value: results) {
							JMenuItem item = new JMenuItem(value);
							item.addActionListener(new ActionListener() {
							    @Override
								public void actionPerformed(java.awt.event.ActionEvent evt) {
							    	titleTextField.setText(item.getText());
							    }
							});
							popupTitle.add(item);
						}
						popupTitle.show(titleTextField, 1, titleTextField.getHeight());
						break;
					case MetadataCatalog.DESCRIPTION_KEY:
						popupDescription.removeAll();
						for(String value: results) {
							JMenuItem item = new JMenuItem(value);
							item.addActionListener(new ActionListener() {
							    @Override
								public void actionPerformed(java.awt.event.ActionEvent evt) {
							    	descriptionTextArea.setText(item.getText());
							    }
							});
							popupDescription.add(item);
						}
						popupDescription.show(descriptionTextArea, 1, descriptionTextArea.getHeight());
						break;
					}
				}
			}
		};
		worker.execute();
	}
}

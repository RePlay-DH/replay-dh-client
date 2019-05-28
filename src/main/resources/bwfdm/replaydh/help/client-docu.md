---
abstract: 'This is an end user documentation of the RePlay-DH client.'
author:
- Florian Fritze, Markus Gärtner, Uli Hahn, Sibylle Hermann, Volodymyr Kushnarenko
title: 'End user documentation of the RePlay-DH client'
---
<!-- TOC -->

- [End user documentation of the RePlay-DH client](#end-user-documentation-of-the-replay-dh-client)
    - [Configuration dialogue of the RePlay-DH client](#configuration-dialogue-of-the-replay-dh-client)
        - [Workspace validation](#workspace-validation)
        - [Choose a workflow schema](#choose-a-workflow-schema)
    - [Basic functions of the RePlay-DH client](#basic-functions-of-the-replay-dh-client)
        - [Drag and Drop feature](#drag-and-drop-feature)
        - [Create a new workflow step](#create-a-new-workflow-step)
    - [The extended view of the client](#the-extended-view-of-the-client)
    - [Additional workflow graph functions of the client](#additional-workflow-graph-functions-of-the-client)
        - [The workflow step editor](#the-workflow-step-editor)
        - [Export of workflow metadata](#export-of-workflow-metadata)
        - [Archive Exporter](#archive-exporter)
        - [DSpace Exporter](#dspace-exporter)
        - [Set Active Workflow Step](#set-active-workflow-step)
        - [Focus the active step in the graph](#focus-the-active-step-in-the-graph)
        - [Compress the active branch](#compress-the-active-branch)
        - [Rebuild the graph visualization](#rebuild-the-graph-visualization)
    - [The file tracker](#the-file-tracker)
        - [The metadata editor for the object metadata](#the-metadata-editor-for-the-object-metadata)
            - [Creating an object metadata entry](#creating-an-object-metadata-entry)
        - [Editing object metadata](#editing-object-metadata)
        - [Adding an additional metadata property](#adding-an-additional-metadata-property)
    - [Remarks](#remarks)
    - [Glossary](#glossary)

<!-- /TOC -->
# End user documentation of the RePlay-DH client

## Configuration dialogue of the RePlay-DH client 

When the RePlay-DH client (client) is started for the first time the configuration dialogue appears. This dialogue is intended as guidance to the basic setup of the client.

![Introduction of the configuration dialogue](Introduction.png)

In this dialogue it is asked for the user name and the user's home organisation. This information can later be reused by the client, for example when referring to the persons involved in a workflow. If more than one person is involved into a workflow and the involved persons belong to more than one organization the identifier of the organization can help to clarify the dependencies between persons and their organizations. For example person A one belongs to organization B.

![Credentials of the user](Identity.png)

It is recommended to enter the given and family name and the research facility. In the next step it is asked for the workspace.

![Create and choose a workspace](Workspace.png)

Git is tracking the changes on files and directories in this folder and stores its metadata in it. By clicking the “Start Validation” button Git creates and initializes a repository in this workspace or imports an already existing Git repository so that the client can work in this environment.

### Workspace validation

![Validate the workspace](Validation.png) 

The client detects whether the workspace folder is empty and whether it is already managed by Git.

![No empty workspace folder](Folder_not_empty.png)

If the folder is not empty but the repository is already managed by the client it can successfully be proceeded with the setup. If the Git repository is not managed by the client it is not possible to continue with the workspace setup.

![Workspace setup failed](No_Replay_Git_repo.png)

In this case manually deleting the hidden ".git" folder in this directory can solve the issue. But be aware that this action will lead to complete loss of all git repository information. If one decides to do this after that it can be continued with a clean new RePlay-DH git repository which can now be created in this folder. In addition to that it is also possible to keep all the files and directories in this workspace except the ".git" folder.

![Validation is OK](ValidationOK.png)

If there are no problems with the previously initialized Git repository the configuration setup asks for the workflow schema used for the workflow metadata, the title of the workspace and its description. The title is the name of the workspace also the name of the research process. The description is a free textfield for a detailed explanation and information about the research process, for instance *"train a linguistic tool"*, etc.

![Set a workspace description](Workspace_Description.png)

### Choose a workflow schema

After that one can set the own workflow schema or choose the client's default schema. The workflow schema can define controlled vocabulary and custom workflow metadata properties but must successfully validate against the process metadata schema which is shipped with the client and which cannot be replaced. The JSON schema is designed to allow custom metadata properties for the process metadata. For instance it is allowed to integrate a metadata property which is not mandatory by the client's default state but can be necessary in the workflow context. This could be the parameter of a research device which is modified while doing iterative workflow steps. It is also possible to specify controlled vocabulary for certain process metadata properties. For instance the allowed roles that persons can have while doing research.

![Choose a worfklow schema](Select_Schema.png)


![Startup window of the client](Standard-Start.png)

This is the default view of the client which provides basic features of the client to support a researcher's workflow. There it can be decided to switch on automatic worfklow tracking (which is a main benefit of Git) or to track the specified workspace on demand (which means manually by clicking the refresh button with the arrows forming a circle). It is also possible to open the specified worflow directory.

## Basic functions of the RePlay-DH client 

### Drag and Drop feature

<div id="drag_and_drop" image="Client-ausgeklappt_1.png">It is possible to drag and drop files for a workflow step in the red marked area.</div>

![Drag and Drop area of the client](Client-ausgeklappt_1.png)

For instance one wants to specify a file in the workspace as input file one can open the workspace by clicking on the folder symbol on the right and simply drag and drop in the red marked area.

![Example for Drag and Drop](Drag_and_Drop.png)

There are three possible file types: *Input files* are used as input for a specific tool. *Tools* are  software which is used to process the input files and generate the *Output files*. Output files are the results of a specific method. One can choose between these three file types. After that the client shows that one file is registered for the next workflow step.

After that one can already add basic metadata to this new registered object: The path of the object is already inferred by the drag and drop functionality. But one can also give a textual description of this file or can choose what type of file it is. The controlled vocabulary shown in this dropdown menu is retrieved from the workflow schema.

![Add basic object metadata](Add_file_metadata.png)

By clicking on the "+" sign one can choose an identifier for the added object. For instance if the object has a Digital Object Identifier (DOI) one can now set this here. Identifiers are important for making the research objects accessible and identifiable in various domains, for instance in a research data repository. Identifiers are also important for making research objects distinguishable from each other. A DOI for example is a unique identifier and can provide persistent access to a research object.

![Adding identifier](Choose_id.png)

If one has registered a file as a tool. There is a slightly different metadata dialogue:

![Adding tool metadata](Add_tool_metadata.png)

There the parameters of this tool and the execution environment can be specified. For instance parameters can vary in different workflow steps because of adjusting the behaviour of the tool to generate different output.

![One object registered](Registered_files.png)

### Create a new workflow step

<div id="record_step" title="Create a new workflow step">In order to create a new workflow step because the research has reached a certain point click on the "Record Step" button.</div> 

A new window appears. In Git terminology a step is a certain commit in the Git history. The client stores then the current research state in the Git repository and writes a commit message in the JSON format. The information for the commit message will be retrieved from the workflow step editor metadata properties.

![Create workflow step](Record_Step.png)

There are 6 different main properties with corresponding sub-properties to describe the workflow step. The step name and description are mandatory. Moreover the involved persons, the tools and input and output files can be described. These 6 main properties are specified in the standard worflow metadata schema of the RePlay-DH client. But it is also possible to add custom metadata properties to the workflow metadata. The workflow metadata schema shipped with the client is flexible enough to allow this in the future. In addition to that the workflow schema has to be set at the beginning of the client setup (see configuration dialogue above).

By clicking on the "+" button a corresponding dialogue will appear. For the "PERSONS" column the dialogue example is provided below. One can describe there the name and the role of the involved person (e.g. creator, editor, operator and other).    

![Create workflow step: add new person](Record_Step_add_person.png)

Other identifiers for a new person are selectable by clicking on the "+" button:
  
![Create workflow step: add identifiers to the new person](Record_Step_add_person_identifiers.png)

When editing process is ready, the new metadata appears in the corresponding column: 

![Create workflow step: new person in the "PERSONS" column](Record_Step_add_person_result.png)

It is always possible to edit the parameters or remove some entries from the list. Please click "arrow" or "-" buttons on the upper right corner of each person correspondent:

![Create workflow step: editing of the new person](Record_Step_add_person_edit.png)

The same procedure could be applied for the "TOOL", "INPUT resources" and "OUTPUT resources" columns.

![Create workflow step: add a tool](Record_Step_add_tool.png)

The required identifiers for the tool are colored in red. They can be chosen by clicking on the "+" button:

![Create workflow step: add identifiers to the tool](Record_Step_add_tool_identifier.png)  

![Create workflow step: add new resource](Record_Step_add_resource.png)  

To make the working process with the client easy and intuitive, new resources can be added by dragging and dropping new files (tool, input or output resources) to the specific area of the editor. A corresponding dialogue will appear automatically.

When description of the current workflow step is finished, the "Ok" button stores the results:

![Create workflow step: description is finished, the workflow step is ready to be stored](Record_Step_finish.png)   

When the recording operation is finished the RePlay-DH client will provide a short status message:  

![Create workflow step: status message](Record_Step_finish_message.png)

## The extended view of the client

By clicking in the main window on the left arrows, the client will open and show more functionalities. There are three different tabs which hold the extended functions of the client. The "Workflow" tab holds the visualization of the workflow graph and its manipulation and extraction functionalities. The "File Tracker" shows information of the current state of the workspace directory and the "Metadata Manager" tab gives the possibility to add object metadata to the items or research objects in the workspace directory. The "Workflow" tab and the "File Tracker" tab are showing an abstract view on the underlying Git repository which is managed by the client. The "Metadata Manager" is an additional feature of the client which allows to assign the research objects with object metadata, for instance the [DataCite Metadata Schema](https://schema.datacite.org/) which holds different properties to describe research data objects. By default the client is shipped with the DataCite Metadata Schema for object metadata. In RePlay-DH terms objects can be resources (input or output files) or tools (programs or scripts). With the DataCite Metadata Schema one can connect the metadata with the research objects used or created in the workflow. In the feature it will be possible to use also other metadata schemas for the research objects. The user will be able to select an own object metadata schema to work with the object metadata editor.

![RePlay-DH client main window](Client_extended.png)

## Additional workflow graph functions of the client

### The workflow step editor

![The workflow step editor](Workflow_Editor.png)

The workflow step editor provides mighty functions. First the current workflow graph is displayed. It all begins with the start or init commit represented by the "Start" circle. After that the workflow graph can follow certain user created branches. It is possible to switch between these branches. Every end of a certain branch is coloured blue.

The current selected workflow step which refers to an underlying commit is coloured red. By selecting a step the selected workflow step gets a green mark as shown in the picture below. There one can see the selected step with blue and green color which means that one has pointed the current step and has selected it with its cursor.

![The selected step](WFE_Selected_Branch.png)

### Export of workflow metadata

![Export of workflow metadata](Workflow_Editor_1.png)

<div id="export_workflow_metadata" title="Export of workflow metadata" image="Workflow_Editor_1.png">If one clicks on the red marked icon the RePlay-DH client gives you the possibility to export a set or the whole metadata of the workflow graph.</div> 

Yet one can choose between the [P-Plan](http://www.opmw.org/model/p-plan/) ontology and the [Prov-O](https://www.w3.org/TR/prov-o/) ontology. The former is intended to describe the plan which was followed during the research process with the client while the latter exports the already executed and saved workflow graph history. Both ontologies are supported and can be chosen.

![The metadata exporter](Workflow_Metadata_Export.png)

If one has chosen an ontology one can furthermore choose the file format. The ontology file formats [TURTLE](https://www.w3.org/TR/turtle/) and [RDF/XML](https://www.w3.org/TR/rdf-syntax-grammar/) are supported.
In the following dialogue one can choose a file to which the metadata is exported.

![Select output file](WFSE_Select_Output.png)

A status message will popup to give feedback whether the export was successfully

![Export successfully](Export_Successfully.png)

### Archive Exporter

<div id="archive_exporter" title="Archive Exporter" image="Workflow_Editor_2.png">By clicking on the exporter icon, there will be the possibility to export the whole workspace as a compressed archive.</div> This is useful if one wants to make a backup with all resources and tools that belong to a client managed Git repository.

![The archive exporter](Workflow_Editor_2.png)

![Select an archive exporter](Select_Archive_Exporter.png)

### DSpace Exporter

<div id="dspace_exporter" title="DSpace Exporter" image="Workflow_Editor3.png">By clicking the publication symbol it is possible to export resources and tools of a certain workflow step within the workspace directory to some publication repository.</div>

This option is currently in experimental phase. Currently one has to add the link to the repository manually. The manual provides somes examples with the test DSpace-based publication repository, which is accessible for every user via the link: http://demo.dspace.org    

![Export to the publication repository](Workflow_Editor_3.png)

It is planed to choose between different instances of publication repositories. Currently only DSpace-based repositories are supported but there will be a Dataverse support in the near future.

![Selection of the type of the publication repository](DSpace_type.png)

After choosing a publication repository and entering login and password one has to push the "Check login" button. If login and password are correct, the "Next" button will be activated. **IMPORTANT:** the login and password will be sent direct to the publication repository and will not be stored inside the RePlay-DH client. 

![Enter login and password](DSpace_login.png)

A collection for the publication can be selected.

![Selection of the collection](DSpace_collection.png)

The next step is to choose the files for publication. One can skip this step and make a metadata-only publication. Adding and discharge of the files are also possible.  

![Selection of files for publication](DSpace_files.png)

Extension of the publication with metadata is an important feature of the RePlay-DH client. To make the input procedure easier, the client tries to fill in some default data automatically based on the already provided information. 

![Input of metadata](DSpace_metadata.png)

After that a short summary will be displayed to show what will be exactly published. Clicking on the "Finish" button will start the publicaton process in background.  

![Publication summary](DSpace_summary.png)

At the end of the process the RePlay-DH client will provide a status message. In case of success there will be an automatic redirection to the publication repository web-page via browser to check all the sent data and finish the publication procedure there. 

![Redirection to the publication repository](DSpace_redirection.png)

The RePlay-DH client only starts the publication process and redirects to the publication repository for the final publication procedure. Provided approach makes the client flexible for different publication repositories, which usually have different terms of publication.   

### Set Active Workflow Step 

<div id="active_workflowstep" title="Set Active Workflow Step" image="Workflow_Editor_4.png">The blue "down arrow" selects the active workflow step in the workflow graph.</div> This is useful if one wants to step back to previous commit. By recording a new step to a previously recorded step which has already a proceeding workflow step a new branch will be created. This feature is useful for following different research process aims or strategies.

![Set active workflow step](Workflow_Editor_4.png)

### Focus the active step in the graph

<div id="focus_active_step" title="Focus the active step in the graph" image="Workflow_Editor_5.png">When a certain step is selected and the graph is really big one can focus the selected step with this button.</div>

![Focus the active step](Workflow_Editor_5.png)

### Compress the active branch

<div id="compress_graph" title="Compress the active branch" image="Workflow_Editor_6.png">If a branch becomes longer the marked button can compress the branch from one branching point to another. This gives a much better overview of the whole workflow graph.</div>

![Compress the active branch](Workflow_Editor_6.png)

### Rebuild the graph visualization

<div id="rebuild_graph" title="Rebuild the graph visualization" image="Workflow_Editor_7.png">By clicking this button the visualization of the graph will be completely rebuilt.</div>

![Rebuild visual graph](Workflow_Editor_7.png)


## The file tracker

![The file tracker](File_Tracker.png)

<div id="file_tracker" title="The file tracker" image="File_Tracker.png">The file tracker tab window demonstrates one of the built-in benefits of the underlying git repository infrastructure. Like all git repositories, git notices immediately if the tracked files of the workspace have changed or new files are added. If so these altered or new files are presented in the file tracker tab window.</div>

![The file tracker with an added object](File_Tracker_added.png) 

### The metadata editor for the object metadata 

![Select metadata builder](MetadataManager_1.png)

On the left one can choose a research object (input, output file or tool) for assigning object metadata to it. In this example the file is named "Hello". According to the chosen metadata repository which can hold different object metadata schemas one can decide to build, change or delete an object metadata entry for a research data object (here: file "Hello"). On the right side of the manager one can see all the used keys of the metadata schema and its set values. These values can later be changed via the object metadata editor.

#### Creating an object metadata entry

The red marked icon will open the metadata builder<div id="metadata_builder" title="Creating an object metadata entry" image="MetadataManager_1.png"></div> 

![Metadata builder intro window](Metadata_Builder_Intro.png)

The metadata can be linked to a local file or a remote resource.

If one chooses a local file the next dialogue asks for the target.

![Choose target for a metadata object](Select_Target.png)

![Verification of the target](Builder_Verification.png)

After choosing "Next" the client checks if the selected resource is already known to the client. If not a new object metadata record will be created.

![Create object metadata record](Create_Object_Metadata_Record.png)

The red marked textfields are mandatory metadata fields while the rest is optional.


In the RePlay-DH terminology objects are an abstract term for resources and tools. Generally resources are text files or the research data which are changed or created during the research life cycle. Tools are programs or scripts which are used to process these resources. For example a program reads a text file and creates automatically annotations within this text file. New research data is then created. These objects can be selected in the object metadata editor and enriched with metadata which makes these objects more understandable and reusable.

![Metadata editor](MetadataManager.png)

### Editing object metadata 

<div id="metadata_editor" title="Editing object metadata" image="MetadataManager.png">When selecting an arbitrary resource the corresponding metadata will be shown in the window. One can then click on the middle button to open the object metadata editor to alter or add some metadata to the corresponding resource or object.</div>

![Editing existing object metadata record](EditExistingRecord.png)

In the editor view more textfields can be added to the corresponding metadata property by clicking on the plus button. The plus button only appears when the metadata schema allows more than one entry for this property. For example it is possible to add more than one creator to a resouce but only one identifier. By clicking on the minus button the textfields are deleted until there is only one for the property. By clicking on the red cross button the content of the last text field is deleted. The metadata editor checks the input for every property and is preventing from entering no schema compliant metadata. On the bottom of the object metadata editor window there are two buttons: The "OK"-Button saves all the changes of the metadata of a specific resource or tool. The "Cancel"-Button closes the metadata editor without saving the changes.

### Adding an additional metadata property 

On the bottom of the editor view there is the possibility to add an additional metadata property which does not exist in the undelying schema. <div id="custom_object_metadata_property" title="Adding an additional metadata property" image="Add_additional_metadata_property.png">By clicking on the “add additional metadata property” button a pop up window appears which allows to enter the name of the metadata property which should be created.</div> Also in this case there is a validation in the background to prevent the input of unallowed metadata property names.

![Adding an additional metadata property](Add_additional_metadata_property.png)

## Remarks

We hope that this manual can help using our client. Feel free to contact us if something seems to be unclear or not explained yet. 
The RePlay-DH team.

## Glossary
resources: Resources are research data which are produced or used during the research lifecycle.

tool: A tool is a piece of software that is used to produce resources. A tool can also have resources as input.

(research) objects: "Objects" is the general term for resources and tools.

persons: A person is a researcher who is involved in the research lifecycle and who is producing research data for instance with a tool.

object metadata: Object metadata is metadata that belongs or describes an object.

process metadata: Process metadata is the metadata for the research process itself. It describes the steps conducted by the researcher to produce research data.

Git: Git is version control system which tracks a specific workspace directory for changes and helps to handle different versions of objects.

workspace: A workspace is specific directory which is managed by Git and holds all the objects which belong to a research conducted by a person or researcher.





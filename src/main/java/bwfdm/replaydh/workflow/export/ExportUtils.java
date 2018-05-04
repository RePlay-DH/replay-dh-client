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
package bwfdm.replaydh.workflow.export;

import java.awt.Component;
import java.util.Collection;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.java.plugin.registry.Extension;
import org.java.plugin.registry.Extension.Parameter;

import bwfdm.replaydh.core.PluginEngine;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.FileEndingFilter;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.workflow.WorkflowExportWizard;
import bwfdm.replaydh.ui.workflow.WorkflowExportWizard.WorkflowExportContext;
import bwfdm.replaydh.ui.workflow.WorkflowExportWorker;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.Mode;

/**
 * @author Markus Gärtner
 *
 */
public class ExportUtils {

	/**
	 * Abstract base extension point for modules that support exporting
	 * parts of a workflow as metdata or resources.
	 * <p>
	 * Possible values:
	 * processMetadata|objectMetadata|object
	 */
	public static final String PARAM_TYPE = "type";

	/**
	 * Hint for the client on what scopes the exporter supports on
	 * the level of workflow process-metadata. Will be used to
	 * adjust the user interface that lets the user choose an
	 * exporter based on the currently selected parts of a workflow graph.
	 * <p>
	 * Possible values: workflow|step|path|part|all
	 */
	public static final String PARAM_WORKFLOW_SCOPE = "workflowScope";

	/**
	 * Hint for the client on what types of objects within the
	 * workflow the exporter is actually able to export.
	 * <p>
	 * Possible values:
	 * input|output|person|tool|all
	 */
	public static final String PARAM_OBJECT_SCOPE = "objectScope";

	/**
	 * Actual implementation, must be fully qualified class name.
	 */
	public static final String PARAM_CLASS = "class";

	/**
	 * Hint for the client on what kinds of output the exporter supports.
	 * <p>
	 * Possible vlaues:
	 * file|folder
	 */
	public static final String PARAM_MODE = "mode";

	/**
	 * Opportunity for an extension to define its own format info
	 * that will be used to customize UI components such as a
	 * file chooser dialog.
	 * <p>
	 * Sub-parameters: {@link #SUBPARAM_DESCRIPTION}, {@link #SUBPARAM_EXTENSION}
	 */
	public static final String PARAM_FORMAT = "format";

	/**
	 * The actual file name ending for a format, including the last dot.
	 */
	public static final String SUBPARAM_EXTENSION = "extension";

	/**
	 * A human readable description for a format, usually provided
	 * as a resource key for lookup.
	 */
	public static final String SUBPARAM_DESCRIPTION = "description";

	public static void configureFromExtension(WorkflowExportInfo.Builder builder, Extension extension) {
		//TODO
	}

	public static void configureFromExtension(JFileChooser fileChooser, Extension extension) {
		Mode mode = PluginEngine.parseParam(extension, PARAM_MODE, Mode::valueOf, Mode.FILE);
		fileChooser.setFileSelectionMode(mode==Mode.FOLDER ?
				JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);

		ResourceManager rm = ResourceManager.getInstance();

		Collection<Parameter> formats = extension.getParameters(PARAM_FORMAT);
		if(formats!=null) {
			fileChooser.resetChoosableFileFilters();

			for(Parameter format : formats) {
				String description = format.getSubParameter(SUBPARAM_DESCRIPTION).rawValue();
				String fileEnding = format.getSubParameter(SUBPARAM_EXTENSION).rawValue();

				description = rm.get(description);

				FileFilter filter = new FileEndingFilter(description, fileEnding);
				fileChooser.addChoosableFileFilter(filter);
			}
		}
	}

	public static boolean showExportWizard(WorkflowExportContext context, Component owner) {
		GuiUtils.checkEDT();

		boolean wizardDone = false;

		try(Wizard<WorkflowExportContext> wizard = WorkflowExportWizard.getWizard(
				SwingUtilities.getWindowAncestor(owner), context.getEnvironment())) {

			wizard.startWizard(context);

			wizardDone = wizard.isFinished() && !wizard.isCancelled();
		}

		return wizardDone;
	}

	public static void performExport(WorkflowExportContext context, Component owner) {
		GuiUtils.checkEDT();

		WorkflowExportInfo exportInfo = context.build();
		WorkflowExporter exporter = context.getExporter();

		new WorkflowExportWorker(SwingUtilities.getWindowAncestor(owner),
				exporter, exportInfo).start();
	}

	public static final String WORKFLOW_EXPORTER_EXTENSION_POINT_ID = "WorkflowExporter";
	public static final String RESORUCE_PUBLISHER_EXTENSION_POINT_ID = "ResourcePublisher";
}

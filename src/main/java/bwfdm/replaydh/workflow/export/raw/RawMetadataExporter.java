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
package bwfdm.replaydh.workflow.export.raw;

import java.awt.Component;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JTextArea;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;

import bwfdm.replaydh.json.JsonWorkflowStepWriter;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.ScrollablePanel;
import bwfdm.replaydh.ui.helper.ScrollablePanel.ScrollableSizeHint;
import bwfdm.replaydh.utils.Options;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowAnonymiser;
import bwfdm.replaydh.workflow.WorkflowStep;
import bwfdm.replaydh.workflow.export.ExportException;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo;
import bwfdm.replaydh.workflow.export.WorkflowExporter;

/**
 * @author Markus Gärtner
 *
 */
public class RawMetadataExporter implements WorkflowExporter {

	private final JCheckBox cbAnonymise = new JCheckBox();

	private boolean doAnonymise = false;

	/**
	 * @see bwfdm.replaydh.ui.helper.ConfigurableHelper#isConfigurable()
	 */
	@Override
	public boolean isConfigurable() {
		return true;
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.ConfigurableHelper#getDialogComponent()
	 */
	@Override
	public Component getDialogComponent() {
		ResourceManager rm = ResourceManager.getInstance();

		cbAnonymise.setText(rm.get("replaydh.plugins.rawMetadataExporter.anonymise.label"));

		JTextArea taInfo = GuiUtils.createTextArea(
				rm.get("replaydh.plugins.rawMetadataExporter.anonymise.message"));

		ScrollablePanel panel = new ScrollablePanel();
		panel.setScrollableWidth(ScrollableSizeHint.FIT);

		return FormBuilder.create()
				.columns("fill:pref:grow")
				.rows("pref, 4dlu, pref")
				.panel(panel)
				.add(taInfo).xy(1, 1)
				.add(cbAnonymise).xy(1, 3)
				.padding(Paddings.DIALOG)
				.build();
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.ConfigurableHelper#configure()
	 */
	@Override
	public void configure() {
		doAnonymise = cbAnonymise.isSelected();
	}

	/**
	 * @see bwfdm.replaydh.workflow.export.WorkflowExporter#export(bwfdm.replaydh.workflow.export.WorkflowExportInfo)
	 */
	@Override
	public void export(WorkflowExportInfo exportInfo) throws IOException, ExportException, InterruptedException {

		Workflow workflow = exportInfo.getWorkflow();
		Set<WorkflowStep> steps = exportInfo.getSteps();

		if(doAnonymise) {
			WorkflowAnonymiser anonymiser = new WorkflowAnonymiser(workflow);

			anonymiser.anonymise();

			workflow = anonymiser.getTarget();
			steps = anonymiser.map(steps);
		}

		try(Writer writer = exportInfo.createWriter();
				JsonWorkflowStepWriter workflowStepWriter = new JsonWorkflowStepWriter()) {

			Options options = new Options();
			options.put(JsonWorkflowStepWriter.PRETTY, Boolean.TRUE);
			workflowStepWriter.init(writer, options);

			workflowStepWriter.writeWorkflow(workflow, steps);
		}
	}

}

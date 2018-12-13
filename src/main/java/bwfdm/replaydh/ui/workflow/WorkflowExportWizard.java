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

import static bwfdm.replaydh.utils.RDHUtils.checkState;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.filechooser.FileFilter;

import org.java.plugin.registry.Extension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.PluginEngine;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.io.resources.FileResource;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.AbstractWizardStep;
import bwfdm.replaydh.ui.helper.ConfigurableHelper;
import bwfdm.replaydh.ui.helper.DocumentAdapter;
import bwfdm.replaydh.ui.helper.ExtensionListCellRenderer;
import bwfdm.replaydh.ui.helper.FileEndingFilter;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.helper.Wizard.Page;
import bwfdm.replaydh.ui.icons.Resolution;
import bwfdm.replaydh.utils.AccessMode;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.export.ExportUtils;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.Mode;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.ObjectScope;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.Type;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.WorkflowScope;
import bwfdm.replaydh.workflow.export.WorkflowExporter;

/**
 * @author Markus Gärtner
 *
 */
public class WorkflowExportWizard {

	private static final Logger log = LoggerFactory.getLogger(WorkflowExportWizard.class);

	public static Wizard<WorkflowExportContext> getWizard(Window parent, RDHEnvironment environment) {
		@SuppressWarnings("unchecked")
		Wizard<WorkflowExportContext> wizard = new Wizard<>(
				parent, "workflowExport", ResourceManager.getInstance().get("replaydh.wizard.workflowExport.title"),
				environment, SELECT_EXPORTER, SELECT_OUTPUT, CONFIGURE_EXPORTER, FINISH);

		return wizard;
	}

	public static final class WorkflowExportContext extends WorkflowExportInfo.Builder {

		public static WorkflowExportContext create(Workflow workflow, RDHEnvironment environment) {
			return (WorkflowExportContext) new WorkflowExportContext()
					.workflow(workflow)
					.environment(environment);
		}

		WorkflowExportContext() {
			super(true, true);
		}

		private Extension extension;

		private WorkflowExporter exporter;

		private List<Extension> extensions;

		public RDHEnvironment getEnvironment() {
			return info.getEnvironment();
		}

		Extension extension() {
			checkState("Extension not selected yet", extension!=null);
			return extension;
		}

		public WorkflowExporter getExporter() {
			if(exporter==null) {
				PluginEngine pluginEngine = getEnvironment().getClient().getPluginEngine();
				try {
					exporter = pluginEngine.instantiate(extension());
				} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
					throw new RDHException("Failed to instantiate exporter class for extension: "+extension(), e);
				}
			}
			return exporter;
		}

		List<Extension> extensions() {
			if(extensions==null) {
				PluginEngine pluginEngine = getEnvironment().getClient().getPluginEngine();
				extensions = pluginEngine.getExtensions(PluginEngine.CORE_PLUGIN_ID,
						ExportUtils.WORKFLOW_EXPORTER_EXTENSION_POINT_ID);
				extensions.sort(PluginEngine.LOCALIZABLE_ORDER);
			}
			return extensions;
		}

		WorkflowExportInfo info() {
			return info;
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	private static abstract class WorkflowExportStep extends AbstractWizardStep<WorkflowExportContext> {
		protected WorkflowExportStep(String id, String titleKey, String descriptionKey) {
			super(id, titleKey, descriptionKey);
		}
	}

	/**
	 * Initial selection of the exporter to be used
	 */
	private static final WorkflowExportStep SELECT_EXPORTER = new WorkflowExportStep(
			"selectExporter",
			"replaydh.wizard.workflowExport.selectExporter.title",
			"replaydh.wizard.workflowExport.selectExporter.description") {

		private JComboBox<Extension> jbExporter;

		private ComboBoxModel<Extension> EMPTY_MODEL;

		private void refreshNextEnabled() {
			setNextEnabled(jbExporter.getSelectedItem()!=null);
		}

		private void onExporterSelectionChanged(ActionEvent ae) {
			refreshNextEnabled();
		}

		@Override
		protected JPanel createPanel() {

			EMPTY_MODEL = new DefaultComboBoxModel<>();

			jbExporter = new JComboBox<>(EMPTY_MODEL);
			jbExporter.setEditable(false);
			jbExporter.addActionListener(this::onExporterSelectionChanged);

			ResourceManager rm = ResourceManager.getInstance();

			return FormBuilder.create()
					.columns("pref:grow:fill")
					.rows("pref, 6dlu, pref, 6dlu, pref")
					.add(GuiUtils.createTextArea(rm.get("replaydh.wizard.workflowExport.selectExporter.message"))).xy(1, 1)	//TODO
					.add(jbExporter).xy(1, 3)
					.add(GuiUtils.createTextArea(rm.get("replaydh.wizard.workflowExport.selectExporter.message2"))).xy(1, 5)
					.build();
		}

		@Override
		public void refresh(RDHEnvironment environment, WorkflowExportContext context) {

			// Lazy initialization of the actual extensions model
			if(jbExporter.getModel().getSize()==0) {
				jbExporter.setRenderer(new ExtensionListCellRenderer(
						environment.getClient().getPluginEngine(),
						Resolution.forSize(16)));

				/*
				 *  Make sure to only present those exporters that can handle the
				 *  scopes defined in the context. Due to the existence of our
				 *  GitArchiveExporter we should be pretty much able to always
				 *  offer an exporter here.
				 *
				 *  TODO maybe do a sanity check against empty list and present some info?
				 */
				jbExporter.setModel(new DefaultComboBoxModel<>(filterExtensions(context)));
			} else if(context.extension!=null) {
				jbExporter.setSelectedItem(context.extension);
			}

			refreshNextEnabled();
		};

		/**
		 * Filters and returns all the {@link Extension extensions} for exporters that are
		 * capable of exporting for the {@link WorkflowScope} and {@link ObjectScope} given
		 * by the specified {@code context}.
		 */
		private Vector<Extension> filterExtensions(WorkflowExportContext context) {
			WorkflowExportInfo info = context.info();
			final Type type = info.getType();
			checkState("Type not set", type!=null);

			final WorkflowScope workflowScope = info.getWorkflowScope();
			checkState("Workflow scope not set", workflowScope!=null);

			final ObjectScope objectScope = info.getObjectScope();
			if(type==Type.OBJECT) {
				checkState("Object scope not set", objectScope!=null);
			}

			Vector<Extension> result = new Vector<>();

			for(Extension extension : context.extensions()) {
				Type declaredType = PluginEngine.parseParam(extension, ExportUtils.PARAM_TYPE, Type::valueOf, null);

				if(declaredType!=type) {
					continue;
				}

				Set<WorkflowScope> declaredWorkflowScopes = PluginEngine.parseParams(
						extension, ExportUtils.PARAM_WORKFLOW_SCOPE, WorkflowScope::valueOf, new HashSet<>());

				if(!declaredWorkflowScopes.contains(workflowScope)
						&& !declaredWorkflowScopes.contains(WorkflowScope.ALL)) {
					continue;
				}

				if(type==Type.OBJECT) {
					Set<ObjectScope> declaredObjectScopes = PluginEngine.parseParams(
							extension, ExportUtils.PARAM_OBJECT_SCOPE, ObjectScope::valueOf, new HashSet<>());

					if(!declaredObjectScopes.contains(objectScope)
							&& !declaredObjectScopes.contains(ObjectScope.ALL)) {
						continue;
					}
				}

				result.addElement(extension);
			}

			return result;
		}

		@Override
		public Page<WorkflowExportContext> next(RDHEnvironment environment, WorkflowExportContext context) {
			context.extension = (Extension) jbExporter.getSelectedItem();
			context.mode(PluginEngine.parseParam(context.extension, ExportUtils.PARAM_MODE, Mode::valueOf, Mode.FILE));

			return SELECT_OUTPUT;
		}

		@Override
		public boolean close() {
			jbExporter.setModel(EMPTY_MODEL);
			return true;
		};
	};

	/**
	 * Initial selection of the exporter to be used
	 */
	private static final WorkflowExportStep SELECT_OUTPUT = new WorkflowExportStep(
			"selectOutput",
			"replaydh.wizard.workflowExport.selectOutput.title",
			"replaydh.wizard.workflowExport.selectOutput.description") {


		private JTextField tfPath;
		private JButton bFileChooser;
		private JFileChooser fileChooser;

		private void onButtonPressed(ActionEvent ae) {
			if(fileChooser.showDialog(getPageComponent(), null)==JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				if(file!=null) {
					tfPath.setText(adjustFileEnding(file.getAbsoluteFile().toPath()).toString());
				}
			}

			updateNextEnabled();
		}

		private void onDocumentChanged(DocumentEvent de) {
			updateNextEnabled();
		}

		private void updateNextEnabled() {
			setNextEnabled(getPathIfValid()!=null);
		}

		private boolean isRequireFolder() {
			return fileChooser.getFileSelectionMode()==JFileChooser.DIRECTORIES_ONLY;
		}

		private Path getPathIfValid() {
			String text = tfPath.getText();
			if(text==null || text.isEmpty()) {
				return null;
			}

			Path path = Paths.get(text);

			boolean requiresFolder = isRequireFolder();

			Path folderToCheck = requiresFolder ? path : path.getParent();

			if(folderToCheck==null || !Files.exists(folderToCheck) || !Files.isDirectory(folderToCheck)) {
				path = null;
			}

			return path;
		}

		@Override
		protected JPanel createPanel() {

			ResourceManager rm = ResourceManager.getInstance();

			bFileChooser = new JButton(rm.get("replaydh.labels.select"));
			bFileChooser.addActionListener(this::onButtonPressed);

			tfPath = new JTextField();
			tfPath.getDocument().addDocumentListener(new DocumentAdapter() {
				@Override
				public void anyUpdate(DocumentEvent e) {
					onDocumentChanged(e);
				}
			});

			fileChooser = new JFileChooser();
			fileChooser.setApproveButtonText(rm.get("replaydh.labels.select"));
			fileChooser.setDialogTitle(rm.get("replaydh.wizard.workflowExport.selectOutput.fileChooserTitle"));
			fileChooser.setAcceptAllFileFilterUsed(false);

			return FormBuilder.create()
					.columns("pref:grow:fill, 8dlu, pref, 4dlu")
					.rows("pref, 6dlu, pref, 6dlu, pref")
					.add(GuiUtils.createTextArea(rm.get("replaydh.wizard.workflowExport.selectOutput.message"))).xyw(1, 1, 4)	//TODO
					.add(tfPath).xy(1, 3)
					.add(bFileChooser).xy(3, 3)
					.add(GuiUtils.createTextArea(rm.get(""))).xyw(1, 5, 4)	//TODO add further info text
					.build();
		}

		@Override
		public void cancel(RDHEnvironment environment, WorkflowExportContext context) {
			context.exporter = null;
		};

		@Override
		public void refresh(RDHEnvironment environment, WorkflowExportContext context) {

			Extension extension = context.extension();

			ExportUtils.configureFromExtension(fileChooser, extension);

			updateNextEnabled();
		};

		private Path adjustFileEnding(Path path) {
			if(isRequireFolder()) {
				return path;
			}

			String fileName = path.getFileName().toString();
			Path rest = path.getParent();

			FileFilter fileFilter = fileChooser.getFileFilter();
			if(fileFilter instanceof FileEndingFilter) {
				FileEndingFilter fileEndingFilter = (FileEndingFilter) fileFilter;
				String extension = fileEndingFilter.getExtension();
				if(!fileName.toLowerCase().endsWith(extension.toLowerCase())) {
					fileName += extension;
					path = rest.resolve(fileName);
				}
			}

			return path;
		}

		@Override
		public Page<WorkflowExportContext> next(RDHEnvironment environment, WorkflowExportContext context) {
			WorkflowExporter exporter = context.getExporter();

			Path file = adjustFileEnding(getPathIfValid());
			context.outputResource(new FileResource(file, AccessMode.WRITE));
			//TODO clear text field and reset file chooser?

			return exporter.isConfigurable() ? CONFIGURE_EXPORTER : FINISH;
		}
	};

	/**
	 * If exporter allows {@link ConfigurableHelper#isConfigurable() configuration}
	 * then here we show its {@link ConfigurableHelper#getDialogComponent() dialog component}.
	 * Otherwise the user will be presented a default message stating that there is
	 * nothing to be configured here.
	 */
	private static final WorkflowExportStep CONFIGURE_EXPORTER = new WorkflowExportStep(
			"configureExporter",
			"replaydh.wizard.workflowExport.configureExporter.title",
			"replaydh.wizard.workflowExport.configureExporter.description") {

		@Override
		protected JPanel createPanel() {

			return new JPanel(new BorderLayout());
		}

		@Override
		public void refresh(RDHEnvironment environment, WorkflowExportContext context) {

			JComponent panel = getPageComponent();
			panel.removeAll();

			boolean dialogComponentAdded = false;

			WorkflowExporter exporter = context.getExporter();
			if(exporter.isConfigurable()) {
				panel.add(exporter.getDialogComponent(), BorderLayout.CENTER);
				dialogComponentAdded = true;
			}

			if(!dialogComponentAdded) {
				panel.add(GuiUtils.createInfoComponent(ResourceManager.getInstance()
						.get("replaydh.wizard.workflowExport.configureExporter.notConfigurable"),
						true, null), BorderLayout.CENTER);
			}
		};

		@Override
		public Page<WorkflowExportContext> next(RDHEnvironment environment, WorkflowExportContext context) {

			WorkflowExporter exporter = context.getExporter();
			if(exporter.isConfigurable()) {
				exporter.configure();
			}

			return FINISH;
		}
	};

	/**
	 *
	 */
	private static final WorkflowExportStep FINISH = new WorkflowExportStep(
			"finish",
			"replaydh.wizard.workflowExport.finish.title",
			"replaydh.wizard.workflowExport.finish.description") {

		@Override
		protected JPanel createPanel() {
			JPanel panel = new JPanel(new BorderLayout());
			panel.add(GuiUtils.createTextArea(ResourceManager.getInstance().get(
					"replaydh.wizard.workflowExport.finish.message")), BorderLayout.CENTER);
			return panel;
		}

		/**
		 * Final page!
		 */
		@Override
		public Page<WorkflowExportContext> next(RDHEnvironment environment, WorkflowExportContext context) {

			return null;
		}
	};
}

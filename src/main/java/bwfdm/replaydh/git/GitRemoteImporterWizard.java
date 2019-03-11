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
package bwfdm.replaydh.git;

import java.awt.Window;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.Workspace;
import bwfdm.replaydh.git.GitRemoteWizard.ChooseRemoteStep;
import bwfdm.replaydh.git.GitRemoteWizard.GitRemoteContext;
import bwfdm.replaydh.git.GitRemoteWizard.GitRemoteStep;
import bwfdm.replaydh.git.GitRemoteWizard.GitWorker;
import bwfdm.replaydh.git.GitRemoteWizard.PerformOperationStep;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.ErrorPanel;
import bwfdm.replaydh.ui.helper.FilePanel;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.helper.Wizard.Page;
import bwfdm.replaydh.ui.icons.IconRegistry;

/**
 * @author Markus Gärtner
 *
 */
public class GitRemoteImporterWizard {

	private static final Logger log = LoggerFactory.getLogger(GitRemoteImporterWizard.class);

	public static Wizard<GitRemoteImporterContext> getWizard(Window parent, RDHEnvironment environment) {
		@SuppressWarnings("unchecked")
		Wizard<GitRemoteImporterContext> wizard = new Wizard<>(
				parent, "gitRemoteImporterWizard",
				ResourceManager.getInstance().get("replaydh.wizard.gitRemoteImporter.title"),
				environment,
				CHOOSE_REMOTE, SELECT_DIRECTORY, CLONE, FINISH);
		return wizard;
	}


	/**
	 * Context for the wizard
	 */
	public static final class GitRemoteImporterContext extends GitRemoteContext<Git> {

		/** Destination where to clone the repo */
		public Path directory;

		public GitRemoteImporterContext(Git git) {
			super(git);
		}

		public GitRemoteImporterContext(RDHEnvironment environment) {
			super(environment);
		}

	}

	/**
	 * First step: Let user provide or select a remote repository URL
	 */
	private static final ChooseRemoteStep<Git, GitRemoteImporterContext> CHOOSE_REMOTE
		= new ChooseRemoteStep<Git, GitRemoteImporterContext>(
			"chooseRemote",
			"replaydh.wizard.gitRemoteUpdater.chooseRemote.title",
			"replaydh.wizard.gitRemoteUpdater.chooseRemote.description",
			null,
			"replaydh.wizard.gitRemoteUpdater.chooseRemote.description",
			null) {

		@Override
		protected boolean checkLegalRemoteName(String name) {
			// Empty or unset name means we don't need to save the remote
			if(name==null || name.isEmpty()) {
				return true;
			}

			if(GitUtils.TEMP_RDH_REMOTE.equals(name)) {
				return false;
			}

			// Remote names are part of a ref string
			return Repository.isValidRefName(name);
		}

		@Override
		protected List<Object> getRemotes(RDHEnvironment environment, GitRemoteImporterContext context) {
			RemoteConfig rc = context.remoteConfig;
			if(rc!=null) {
				return rc.getURIs()
						.stream()
						.map(URIish::toString)
						.collect(Collectors.toList());
			} else {
				return Collections.emptyList();
			}
		};

		@Override
		protected boolean isAllowRemoteName() {
			return true;
		};

		@Override
		public Page<GitRemoteImporterContext> next(RDHEnvironment environment,
				GitRemoteImporterContext context) {
			if(!defaultProcessNext(environment, context)) {
				return null;
			}

			return SELECT_DIRECTORY;
		}
	};

	/**
	 * Let user provide the file that should be loaded
	 */
	private static final GitRemoteStep<Git, GitRemoteImporterContext> SELECT_DIRECTORY
		= new GitRemoteStep<Git, GitRemoteImporterContext>(
			"selectDirectory",
			"replaydh.wizard.gitRemoteUpdater.selectDirectory.title",
			"replaydh.wizard.gitRemoteUpdater.selectDirectory.description") {

		private FilePanel filePanel;

		private void configureFileChooser(JFileChooser fileChooser) {
			//TODO add title for chooser
		}

		private void onFilePanelChange(ChangeEvent evt) {
			Path file = filePanel.getFile();
			setNextEnabled(file!=null && Files.exists(file, LinkOption.NOFOLLOW_LINKS)
					&& Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS));
		}

		@Override
		protected JPanel createPanel() {

			ResourceManager rm = ResourceManager.getInstance();

			filePanel = FilePanel.newBuilder()
					.acceptedFileType(JFileChooser.DIRECTORIES_ONLY)
					.fileLimit(1)
					.fileChooserSetup(this::configureFileChooser)
					.build();

			filePanel.addChangeListener(this::onFilePanelChange);

			return FormBuilder.create()
					.columns("pref:grow:fill")
					.rows("pref, 6dlu, pref, 6dlu, pref")
					.add(GuiUtils.createTextArea(rm.get(
							"replaydh.wizard.gitRemoteUpdater.selectDirectory.message"))).xy(1, 1)	//TODO
					.add(filePanel).xy(1, 3)
					.add(GuiUtils.createTextArea(rm.get(""))).xy(1, 5)	//TODO add further info text
					.build();
		}

		@Override
		public void refresh(RDHEnvironment environment, GitRemoteImporterContext context) {
			filePanel.setFile(context.directory);
		};

		@Override
		public Page<GitRemoteImporterContext> next(RDHEnvironment environment, GitRemoteImporterContext context) {
			context.directory = filePanel.getFile();

			return CLONE;
		}
	};

	/**
	 * Let user run the clone command
	 */
	private static final PerformOperationStep<Git, CloneCommand, GitRemoteImporterContext> CLONE
		= new PerformOperationStep<Git, CloneCommand, GitRemoteImporterContext>(
			"clone",
			"replaydh.wizard.gitRemoteImporter.clone.title",
			"replaydh.wizard.gitRemoteImporter.clone.description") {

		@Override
		public void refresh(RDHEnvironment environment, GitRemoteImporterContext context) {
			setNextEnabled(false);
			Path directory = context.directory;

			boolean notEmpty;
			try {
				notEmpty = Files.list(directory).findAny().isPresent();
			} catch (IOException e) {
				log.error("Failed to check for empty directory on {}", directory, e);
				displayText(ResourceManager.getInstance().get(
						"replaydh.wizard.gitRemoteImporter.clone.dirCheckFailed"));
				return;
			}

			if(notEmpty) {
				displayText(ResourceManager.getInstance().get(
						"replaydh.wizard.gitRemoteImporter.clone.dirNotEmpty"));
			} else {
				super.refresh(environment, context);
			}
		};

		@Override
		protected CloneCommand createGitCommand(
				GitWorker<Git,CloneCommand,GitRemoteImporterContext> worker) throws GitException {
			GitRemoteImporterContext context = worker.context;
			CloneCommand command = Git.cloneRepository();

			if(!configureTransportCommand(command, context)) {
				return null;
			}

			command.setRemote(context.getRemote());
			command.setDirectory(context.directory.toFile());
			command.setCloneAllBranches(true);
			command.setURI(context.remoteConfig.getURIs().get(0).toString());

			return command;
		}

		@Override
		protected void handleWorkerResult(GitWorker<Git,CloneCommand,GitRemoteImporterContext> worker) {
			super.handleWorkerResult(worker);
			control.invokeNext(FINISH);
		};


		@Override
		public Page<GitRemoteImporterContext> next(RDHEnvironment environment,
				GitRemoteImporterContext context) {
			return FINISH;
		}
	};

	/**
	 * Let user provide the file that should be loaded
	 */
	private static final GitRemoteStep<Git, GitRemoteImporterContext> FINISH
		= new GitRemoteStep<Git, GitRemoteImporterContext>(
			"finish",
			"replaydh.wizard.gitRemoteImporter.finish.title",
			"replaydh.wizard.gitRemoteImporter.finish.description") {

		private JTextArea taHeader;
		private JLabel lIcon;
		private ErrorPanel epInfo;

		@Override
		protected JPanel createPanel() {

			taHeader = GuiUtils.createTextArea("");

			lIcon = new JLabel();
			lIcon.setIcon(IconRegistry.getGlobalRegistry().getIcon("loading-64.gif"));

			epInfo = new ErrorPanel();
			epInfo.setVisible(false);

			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("pref, 6dlu, pref, pref")
					.add(taHeader).xy(1, 1)
					.add(lIcon).xy(1, 3, "center, center")
					.add(epInfo).xy(1, 4, "center, center")
					.build();
		}

		@Override
		public void refresh(RDHEnvironment environment, GitRemoteImporterContext context) {
			setPreviousEnabled(false);
			lIcon.setVisible(false);

			SwingWorker<?, ?> worker = new SwingWorker<Workspace, Runnable>() {

				@Override
				protected Workspace doInBackground() throws Exception {
					return JGitAdapter.checkClonedRepo(context.directory, environment);
				}

				@Override
				protected void process(List<Runnable> chunks) {
					chunks.forEach(Runnable::run);
				};

				@Override
				protected void done() {
					lIcon.setVisible(false);
					epInfo.setVisible(false);

					Workspace workspace = null;

					try {
						workspace = get();
					} catch (InterruptedException | CancellationException e) {
						// Operation cancelled (no idea how)
						log.info("Clone verification cancelled");
					} catch (ExecutionException e) {
						log.error("Error during clone verification", e.getCause());
						context.error = e.getCause();
					}

					ResourceManager rm = ResourceManager.getInstance();

					if(context.error!=null) {
						// Major problem
						taHeader.setText(rm.get("replaydh.wizard.gitRemoteImporter.finish.verificationFailed"));
						epInfo.setThrowable(context.error);
						epInfo.setVisible(true);
					} else if(workspace!=null) {
						// Everything went well
						taHeader.setText(rm.get("replaydh.wizard.gitRemoteImporter.finish.compatibleRepo"));
					} else {
						// Cloned repo is not a valid RDH managed repo!
						taHeader.setText(rm.get("replaydh.wizard.gitRemoteImporter.finish.incompatibleRepo"));
					}
				};

			};

			worker.execute();
		};

		@Override
		public Page<GitRemoteImporterContext> next(RDHEnvironment environment, GitRemoteImporterContext context) {
			return null;
		}
	};
}

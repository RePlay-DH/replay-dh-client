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
package bwfdm.replaydh.workflow.git;

import java.awt.Window;
import java.net.URISyntaxException;

import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.git.GitException;
import bwfdm.replaydh.git.GitUtils;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.helper.Wizard.Page;

/**
 * @author Markus Gärtner
 *
 */
public class GitRemoteUpdateWizard extends GitRemoteWizard {

	public static Wizard<GitRemoteUpdaterContext> getWizard(Window parent, RDHEnvironment environment) {
		@SuppressWarnings("unchecked")
		Wizard<GitRemoteUpdaterContext> wizard = new Wizard<>(
				parent, "gitRemoteUpdaterWizard",
				ResourceManager.getInstance().get("replaydh.wizard.gitRemoteUpdater.title"),
				environment,
				CHOOSE_REMOTE, UPDATE, FINISH);
		return wizard;
	}

	/**
	 * Context for the wizard
	 */
	public static final class GitRemoteUpdaterContext extends GitRemoteContext<PullResult> {

		/** Backup pointer to head before the pull attempt */
		RevCommit currentHead;

		public GitRemoteUpdaterContext(Git git) {
			super(git);
		}

		public GitRemoteUpdaterContext(RDHEnvironment environment) {
			super(environment);
		}
	}

	/**
	 * First step: Let user provide or select a remote repository URL
	 */
	private static final ChooseRemoteStep<PullResult, GitRemoteUpdaterContext> CHOOSE_REMOTE
		= new ChooseRemoteStep<PullResult, GitRemoteUpdaterContext>(
			"chooseRemote",
			"replaydh.wizard.gitRemoteUpdater.chooseRemote.title",
			"replaydh.wizard.gitRemoteUpdater.chooseRemote.description",
			null,
			"replaydh.wizard.gitRemoteUpdater.chooseRemote.description",
			null) {

		@Override
		public Page<GitRemoteUpdaterContext> next(RDHEnvironment environment,
				GitRemoteUpdaterContext context) {
			return defaultProcessNext(environment, context) ? UPDATE : null;
		}
	};

	/**
	 *
	 */
	private static final PerformOperationStep<PullResult, PullCommand, GitRemoteUpdaterContext> UPDATE
			= new PerformOperationStep<PullResult, PullCommand, GitRemoteUpdaterContext>(
			"update",
			"replaydh.wizard.gitRemoteUpdater.update.title",
			"replaydh.wizard.gitRemoteUpdater.update.description") {

		@Override
		protected PullCommand createGitCommand(GitRemoteUpdaterContext context)
					throws GitException, URISyntaxException {
			PullCommand command = context.git.pull();

			URIish remoteUri = null;
			String remoteName = null;

			if(context.remoteConfig!=null) {
				remoteName = context.remoteConfig.getName();
				remoteUri = getUsablePushUri(context.remoteConfig);
				command.setRemote(remoteName);
			} else {
				remoteUri = context.remoteUrl;
				command.setRemote(remoteUri.toString());
			}

			if(!GitUtils.prepareTransportCredentials(command, remoteUri, remoteName,
					(username, password) -> {
						context.credentials = new UsernamePasswordCredentialsProvider(username, password);
						return context.credentials;
					})) {
				return null;
			}

			return command;
		};

		@Override
		protected GitMonitoringWorker<PullResult,PullCommand,GitRemoteUpdaterContext> createWorker(
				RDHEnvironment environment, PullCommand command, GitRemoteUpdaterContext context) {
			return new GitMonitoringWorker<>(this, context, command);
		};

		@Override
		public Page<GitRemoteUpdaterContext> next(RDHEnvironment environment,
				GitRemoteUpdaterContext context) {
			// TODO is all the needed info already stored in the context?
			return FINISH;
		}
	};

	/**
	 *
	 */
	private static final GitRemoteStep<PullResult, GitRemoteUpdaterContext> FINISH
		= new GitRemoteStep<PullResult, GitRemoteUpdaterContext>(
			"finish",
			"replaydh.wizard.gitRemoteUpdater.finish.title",
			"replaydh.wizard.gitRemoteUpdater.finish.description") {

		private JTextArea taHeader;
		private JTextArea taInfo;

		@Override
		protected JPanel createPanel() {

			taHeader = GuiUtils.createTextArea("");

			taInfo = GuiUtils.createTextArea("");
			taInfo.setColumns(60);
			taInfo.setRows(15);
			taInfo.setEditable(true);
			taInfo.setLineWrap(false);
			taInfo.setBorder(GuiUtils.defaultContentBorder);

			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("pref, 6dlu, pref, $nlg, pref")
					.add(taHeader).xy(1, 1)
					.addScrolled(taInfo).xy(1, 3, "center, center")
					.build();
		}

		@Override
		public void refresh(RDHEnvironment environment, GitRemoteUpdaterContext context) {
			ResourceManager rm = ResourceManager.getInstance();

			if(context.error!=null) {
				taInfo.setText(GuiUtils.errorText(context.error));
				taHeader.setText(rm.get("replaydh.wizard.gitRemoteUpdater.finish.headerError"));
			} else if(context.result!=null) {
				taInfo.setText(context.result.toString());
			} else {
				taHeader.setText(rm.get("replaydh.wizard.gitRemoteUpdater.finish.headerMissingInfo"));
				taInfo.setText(context.finalMessage);
			}

			setPreviousEnabled(false);
		};

		@Override
		public Page<GitRemoteUpdaterContext> next(RDHEnvironment environment,
				GitRemoteUpdaterContext context) {
			return null;
		}
	};
}

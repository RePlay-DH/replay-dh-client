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
package bwfdm.replaydh.workflow.export.git;

import static java.util.Objects.requireNonNull;

import java.awt.Component;
import java.awt.Window;
import java.net.URISyntaxException;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.RemoteRefUpdate.Status;
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
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo;
import bwfdm.replaydh.workflow.git.GitRemoteWizard;

/**
 * @author Markus Gärtner
 *
 */
public class GitRemotePublisherWizard extends GitRemoteWizard {

	public static Wizard<GitRemotePublisherContext> getWizard(Window parent, RDHEnvironment environment) {
		@SuppressWarnings("unchecked")
		Wizard<GitRemotePublisherContext> wizard = new Wizard<>(
				parent, "gitRemotePublisherWizard",
				ResourceManager.getInstance().get("replaydh.wizard.gitRemotePublisher.title"),
				environment,
				CHOOSE_REMOTE, EXPORT, FINISH);
		return wizard;
	}

	/**
	 * Context for the wizard
	 */
	public static final class GitRemotePublisherContext extends GitRemoteContext<Iterable<PushResult>> {

		final WorkflowExportInfo exportInfo;

		public GitRemotePublisherContext(WorkflowExportInfo exportInfo, Git git) {
			super(git);
			this.exportInfo = requireNonNull(exportInfo);
		}
	}

	/**
	 * First step: Let user provide or select a remote repository URL
	 */
	private static final ChooseRemoteStep<Iterable<PushResult>, GitRemotePublisherContext> CHOOSE_REMOTE
		= new ChooseRemoteStep<Iterable<PushResult>, GitRemotePublisherContext>(
			"chooseRemote",
			"replaydh.wizard.gitRemotePublisher.chooseRemote.title",
			"replaydh.wizard.gitRemotePublisher.chooseRemote.description",
			null,
			"replaydh.wizard.gitRemotePublisher.chooseRemote.middle",
			null) {

		@Override
		public Page<GitRemotePublisherContext> next(RDHEnvironment environment,
				GitRemotePublisherContext context) {
			return defaultProcessNext(environment, context) ? EXPORT : null;
		}
	};

	/**
	 *
	 */
	private static final PerformOperationStep<Iterable<PushResult>, PushCommand, GitRemotePublisherContext> EXPORT
			= new PerformOperationStep<Iterable<PushResult>, PushCommand, GitRemotePublisherContext>(
			"export",
			"replaydh.wizard.gitRemotePublisher.export.title",
			"replaydh.wizard.gitRemotePublisher.export.description") {

		@Override
		protected PushCommand createGitCommand(GitRemotePublisherContext context)
					throws GitException, URISyntaxException {
			PushCommand command = context.git.push();

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

			command.setPushAll();
			command.setPushTags();
			command.setAtomic(true);

			return command;
		};

		@Override
		protected GitMonitoringWorker<Iterable<PushResult>,PushCommand,GitRemotePublisherContext> createWorker(
				RDHEnvironment environment, PushCommand command, GitRemotePublisherContext context) {
			return new GitMonitoringWorker<>(this, context, command);
		};

		@Override
		public Page<GitRemotePublisherContext> next(RDHEnvironment environment,
				GitRemotePublisherContext context) {
			// TODO is all the needed info already stored in the context?
			return FINISH;
		}
	};

	/**
	 *
	 */
	private static final GitRemoteStep<Iterable<PushResult>, GitRemotePublisherContext> FINISH
		= new GitRemoteStep<Iterable<PushResult>, GitRemotePublisherContext>(
			"finish",
			"replaydh.wizard.gitRemotePublisher.finish.title",
			"replaydh.wizard.gitRemotePublisher.finish.description") {

		private JComboBox<PushResult> cbResults;

		private JTextArea taHeader;
		private JTextArea taInfo;

		@Override
		protected JPanel createPanel() {

			cbResults = new JComboBox<>();
			cbResults.setEditable(false);
			cbResults.setRenderer(new PushResultListeCellRenderer());
			cbResults.addActionListener(ae -> displaySelectedResult());

			taHeader = GuiUtils.createTextArea("");

			taInfo = new JTextArea();
			taInfo.setColumns(60);
			taInfo.setRows(15);
			taInfo.setEditable(true);
			taInfo.setLineWrap(false);
			taInfo.setBorder(GuiUtils.defaultContentBorder);

			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("pref, 6dlu, pref, $nlg, pref")
					.add(taHeader).xy(1, 1)
					.add(cbResults).xy(1, 3)
					.addScrolled(taInfo).xy(1, 5, "center, center")
					.build();
		}

		private void displaySelectedResult() {
			PushResult pushResult = (PushResult) cbResults.getSelectedItem();

			if(pushResult==null) {
				taInfo.setText(null);
			} else {
				//TODO properly process the displayed data for non-technical users
				StringBuilder sb = new StringBuilder();

				sb.append(pushResult.getMessages());

				String LB = "\n\n";

				pushResult.getRemoteUpdates()
					.stream()
					.sorted((u1, u2) -> u1.getRemoteName().compareTo(u2.getRemoteName()))
					.forEach(refUpdate -> sb.append(LB).append(refUpdate.toString()));  //TODO ugly, as this is not localized

				taInfo.setText(sb.toString());
			}
		}

		@Override
		public void refresh(RDHEnvironment environment, GitRemotePublisherContext context) {
			ResourceManager rm = ResourceManager.getInstance();

			cbResults.removeAllItems();

			if(context.error!=null) {
				cbResults.setVisible(false);
				taInfo.setText(GuiUtils.errorText(context.error));
				taHeader.setText(rm.get("replaydh.wizard.gitRemotePublisher.finish.headerError"));
			} else if(context.result!=null) {
				context.result.forEach(cbResults::addItem);
				if(cbResults.getItemCount()>0) {
					GuiUtils.invokeEDTLater(() -> cbResults.setSelectedIndex(0));

					boolean accepted = true;
					for(PushResult pushResult : context.result) {
						if(isRejectedPushResult(pushResult)) {
							accepted = false;
							break;
						}
					}

					if(accepted) {
						taHeader.setText(rm.get("replaydh.wizard.gitRemotePublisher.finish.headerAccepted"));
					} else {
						taHeader.setText(rm.get("replaydh.wizard.gitRemotePublisher.finish.headerRejected"));
					}
				} else {
					taHeader.setText(rm.get("replaydh.wizard.gitRemotePublisher.finish.headerMissingInfo"));
				}
			} else {
				taHeader.setText(rm.get("replaydh.wizard.gitRemotePublisher.finish.headerMissingInfo"));
				taInfo.setText(context.finalMessage);
			}

			setPreviousEnabled(false);
		};

		@Override
		public boolean close() {
			cbResults.removeAllItems();

			return true;
		};

		@Override
		public Page<GitRemotePublisherContext> next(RDHEnvironment environment,
				GitRemotePublisherContext context) {
			return null;
		}
	};

	private static boolean isRejectedPushResult(PushResult pushResult) {
		for(RemoteRefUpdate refUpdate : pushResult.getRemoteUpdates()) {
			Status status = refUpdate.getStatus();
			if(status!=Status.OK && status!=Status.UP_TO_DATE) {
				return true;
			}
		}
		return false;
	}

	private static class PushResultListeCellRenderer implements ListCellRenderer<PushResult> {

		private final DefaultListCellRenderer delegate = new DefaultListCellRenderer();

		private Icon ERROR_ICON = IconRegistry.getGlobalRegistry().getIcon("delete_obj.gif");
		private Icon DEFAULT_ICON = GuiUtils.getBlankIcon(16, 16);

		@Override
		public Component getListCellRendererComponent(JList<? extends PushResult> list,
				PushResult value, int index, boolean isSelected,
				boolean cellHasFocus) {

			String tooltip = null;
			String text = value.getURI().toString();
			Icon icon = isRejectedPushResult(value) ? ERROR_ICON : DEFAULT_ICON;

			delegate.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);

			delegate.setToolTipText(tooltip);
			delegate.setIcon(icon);

			return delegate;
		}
	}
}

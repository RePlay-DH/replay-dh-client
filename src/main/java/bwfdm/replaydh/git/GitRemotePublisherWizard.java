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

import static java.util.Objects.requireNonNull;

import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.ErrorPanel;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.helper.Wizard.Page;
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo;

/**
 * @author Markus Gärtner
 *
 */
public class GitRemotePublisherWizard extends GitRemoteWizard {

	private static final Logger log = LoggerFactory.getLogger(GitRemotePublisherWizard.class);

	public static Wizard<GitRemotePublisherContext> getWizard(Window parent, RDHEnvironment environment) {
		@SuppressWarnings("unchecked")
		Wizard<GitRemotePublisherContext> wizard = new Wizard<>(
				parent, "gitRemotePublisherWizard",
				ResourceManager.getInstance().get("replaydh.wizard.gitRemotePublisher.title"),
				environment,
				CHOOSE_REMOTE, SELECT_SCOPE, EXPORT, FINISH);
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

	private static final EnumSet<Status> UNCHANGED = EnumSet.of(
			Status.NOT_ATTEMPTED, Status.UP_TO_DATE);

	private static final EnumSet<Status> CHANGED = EnumSet.of(
			Status.OK);

	private static final EnumSet<Status> FAILED;
	static {
		EnumSet<Status> failed = EnumSet.allOf(Status.class);
		failed.removeAll(CHANGED);
		failed.removeAll(UNCHANGED);
		FAILED = failed;
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
			if(!defaultProcessNext(environment, context)) {
				return null;
			}

			// TODO figure out if we have multiple branches

			return SELECT_SCOPE;
		}
	};

	/**
	 * Let user decide if we should only update current branch or entire repository
	 */
	private static final SelectScopeStep<Iterable<PushResult>, GitRemotePublisherContext> SELECT_SCOPE
		= new SelectScopeStep<Iterable<PushResult>, GitRemotePublisherContext>(
			"selectScope",
			"replaydh.wizard.gitRemotePublisher.selectScope.title",
			"replaydh.wizard.gitRemotePublisher.selectScope.description",
			"replaydh.wizard.gitRemotePublisher.selectScope.header",
			"replaydh.wizard.gitRemotePublisher.selectScope.workspaceScope",
			"replaydh.wizard.gitRemotePublisher.selectScope.workflowScope") {

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
		protected PushCommand createGitCommand(
				GitWorker<Iterable<PushResult>,PushCommand,GitRemotePublisherContext> worker) throws GitException {
			GitRemotePublisherContext context = worker.context;
			PushCommand command = context.git.push();

			if(!configureTransportCommand(command, context)) {
				return null;
			}

			// Only if specifically requested will we push all branches
			if(context.scope==Scope.WORKFLOW) {
				command.setPushAll();
			}

			command.setRemote(context.getRemote());
			command.setPushTags();
			command.setAtomic(true);

			return command;
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
		private ErrorPanel epInfo;

		@Override
		protected JPanel createPanel() {

			cbResults = new JComboBox<>();
			cbResults.setEditable(false);
			cbResults.setRenderer(new PushResultListeCellRenderer());
			cbResults.addActionListener(ae -> displaySelectedResult());

			taHeader = GuiUtils.createTextArea("");

			epInfo = new ErrorPanel();

			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("pref, 6dlu, pref, $nlg, pref")
					.add(taHeader).xy(1, 1)
					.add(cbResults).xy(1, 3)
					.add(epInfo).xy(1, 5, "center, center")
					.build();
		}

		private void displaySelectedResult() {
			PushResult pushResult = (PushResult) cbResults.getSelectedItem();

			if(pushResult==null) {
				epInfo.setText(null);
			} else {

				//Create a more comfortable result lookup
				Map<Status, List<RemoteRefUpdate>> updatesByStatus
						= getUpdatesByStatus(pushResult);

				String headerKey;

				/*
				 *  Depending on what every RemoteRefUpdate reports, we need
				 *  to inform the user appropriately.
				 */
				if(hasFailed(updatesByStatus.keySet())) {
					// Push rejected - nothing updated on remote
					headerKey = "replaydh.wizard.gitRemotePublisher.finish.headerRejected";
				} else if(hasChanged(updatesByStatus.keySet())) {
					// Push succeeded - remote updated
					headerKey = "replaydh.wizard.gitRemotePublisher.finish.headerAccepted";
				} else {
					// Remote already up2date
					headerKey = "replaydh.wizard.gitRemotePublisher.finish.headerNoChanges";
				}

				taHeader.setText(ResourceManager.getInstance().get(headerKey));

				showRawResult(pushResult);
			}
		}

		private void showRawResult(PushResult pushResult) {
			//TODO properly process the displayed data for non-technical users
			StringBuilder sb = new StringBuilder();

			sb.append(pushResult.getMessages());

			String LB = "\n\n";

			pushResult.getRemoteUpdates()
				.stream()
				.sorted((u1, u2) -> u1.getRemoteName().compareTo(u2.getRemoteName()))
				.forEach(refUpdate -> sb.append(LB).append(refUpdate.toString()));  //TODO ugly, as this is not localized

			epInfo.setText(sb.toString());
		}

		@Override
		public void refresh(RDHEnvironment environment, GitRemotePublisherContext context) {
			ResourceManager rm = ResourceManager.getInstance();

			cbResults.removeAllItems();

			if(context.error!=null) {
				// Major error prevented execution of the command
				cbResults.setVisible(false);
				epInfo.setThrowable(context.error);
				taHeader.setText(rm.get("replaydh.wizard.gitRemotePublisher.finish.headerError"));
			} else if(context.result!=null) {
				// We got a result which may still indicate an error

				for(PushResult pushResult : context.result) {
					cbResults.addItem(pushResult);

					log.info("Raw result of pushing to {}: {}",
							pushResult.getURI(), pushResult);
				}

				context.result.forEach(cbResults::addItem);
				if(cbResults.getItemCount()>0) {
					GuiUtils.invokeEDTLater(() -> cbResults.setSelectedIndex(0));

					// Switch to detect if at least one ref got rejected
					boolean accepted = true;
					for(PushResult pushResult : context.result) {
						if(isRejectedPushResult(pushResult)) {
							accepted = false;
							break;
						}
					}

					if(accepted) {
						// Everything was
						taHeader.setText(rm.get("replaydh.wizard.gitRemotePublisher.finish.headerAccepted"));
					} else {
						taHeader.setText(rm.get("replaydh.wizard.gitRemotePublisher.finish.headerRejected"));
					}
				} else {
					taHeader.setText(rm.get("replaydh.wizard.gitRemotePublisher.finish.headerMissingInfo"));
				}
			} else {
				// Something weird happened, possibly an issue with the dialog flow
				taHeader.setText(rm.get("replaydh.wizard.gitRemotePublisher.finish.headerMissingInfo"));
				epInfo.setText(context.finalMessage);
			}

			setPreviousEnabled(false);
		};

		private Map<Status, List<RemoteRefUpdate>> getUpdatesByStatus(PushResult pushResult) {
			Map<Status, List<RemoteRefUpdate>> result = new HashMap<>();

			pushResult.getRemoteUpdates().forEach(update ->
					result.computeIfAbsent(update.getStatus(), r -> new ArrayList<>()).add(update));

			return result;
		}

		/**
		 * Check if the given set of occurred results contains any indicating a fail.
		 */
		private boolean hasFailed(Set<Status> results) {
			return results.stream().anyMatch(FAILED::contains);
		}

		/**
		 * Check if the given set of occurred results contains any indicating a
		 * successful physical change.
		 */
		private boolean hasChanged(Set<Status> results) {
			return results.stream().anyMatch(CHANGED::contains);
		}

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

			delegate.setToolTipText(GuiUtils.toSwingTooltip(tooltip));
			delegate.setIcon(icon);

			return delegate;
		}
	}
}

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ThreeWayMerger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.git.GitException;
import bwfdm.replaydh.git.GitUtils;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.ErrorPanel;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.helper.Wizard.Page;
import bwfdm.replaydh.ui.icons.IconRegistry;

/**
 * @author Markus Gärtner
 *
 */
public class GitRemoteUpdateWizard extends GitRemoteWizard {

	private static final Logger log = LoggerFactory.getLogger(GitRemoteUpdateWizard.class);

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
	public static final class GitRemoteUpdaterContext extends GitRemoteContext<FetchResult> {

		/** Backup pointer to head before the pull attempt */
		public RevCommit currentHead;

		public MergeResult mergeDryRunResult;

		public MergeResult mergeResult;

		public GitRemoteUpdaterContext(Git git) {
			super(git);
		}

		public GitRemoteUpdaterContext(RDHEnvironment environment) {
			super(environment);
		}
	}

	private static final EnumSet<Result> UNCHANGED = EnumSet.of(
			Result.NO_CHANGE, Result.NOT_ATTEMPTED);

	private static final EnumSet<Result> CHANGED = EnumSet.of(
			Result.FAST_FORWARD, Result.FORCED, Result.NEW, Result.RENAMED);

	private static final EnumSet<Result> FAILED;
	static {
		EnumSet<Result> failed = EnumSet.allOf(Result.class);
		failed.removeAll(CHANGED);
		failed.removeAll(UNCHANGED);
		FAILED = failed;
	}

	/**
	 * First step: Let user provide or select a remote repository URL
	 */
	private static final ChooseRemoteStep<FetchResult, GitRemoteUpdaterContext> CHOOSE_REMOTE
		= new ChooseRemoteStep<FetchResult, GitRemoteUpdaterContext>(
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
	private static final PerformOperationStep<FetchResult, FetchCommand, GitRemoteUpdaterContext> UPDATE
			= new PerformOperationStep<FetchResult, FetchCommand, GitRemoteUpdaterContext>(
			"update",
			"replaydh.wizard.gitRemoteUpdater.update.title",
			"replaydh.wizard.gitRemoteUpdater.update.description") {

		@Override
		protected FetchCommand createGitCommand(GitRemoteUpdaterContext context)
					throws GitException, URISyntaxException {
			FetchCommand command = context.git.fetch();

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

			command.setCheckFetchedObjects(true);
//			command.isRemoveDeletedRefs(); //TODO for now we don't allow deleting local refs. Needs tochange when we add deeper functionality

			return command;
		};

		@Override
		protected GitMonitoringWorker<FetchResult,FetchCommand,GitRemoteUpdaterContext> createWorker(
				RDHEnvironment environment, FetchCommand command, GitRemoteUpdaterContext context) {
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
	private static final GitRemoteStep<FetchResult, GitRemoteUpdaterContext> FINISH
		= new GitRemoteStep<FetchResult, GitRemoteUpdaterContext>(
			"finish",
			"replaydh.wizard.gitRemoteUpdater.finish.title",
			"replaydh.wizard.gitRemoteUpdater.finish.description") {

		private JTextArea taHeader;
		private ErrorPanel epInfo;
		private JLabel lIcon;

		@Override
		protected JPanel createPanel() {

			taHeader = GuiUtils.createTextArea("");

			lIcon = new JLabel();
			lIcon.setIcon(IconRegistry.getGlobalRegistry().getIcon("loading-16.gif"));

			epInfo = new ErrorPanel();

			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("pref, 6dlu, pref, pref")
					.add(taHeader).xy(1, 1)
					.add(lIcon).xy(1, 3, "center, center")
					.add(epInfo).xy(1, 4, "center, center")
					.build();
		}

		@Override
		public void refresh(RDHEnvironment environment, GitRemoteUpdaterContext context) {
			ResourceManager rm = ResourceManager.getInstance();

			lIcon.setVisible(false);
			epInfo.setVisible(true);

			if(context.error!=null) {
				// Operation failed with an exception, so don't bother post-processing
				epInfo.setThrowable(context.error);
				taHeader.setText(rm.get("replaydh.wizard.gitRemoteUpdater.finish.headerError"));
			} else if(context.result!=null) {
				final FetchResult fetchResult = context.result;
				log.info("Raw result of fetching from {}: {}",
						context.getRemote(), fetchResult);

				//Create a more comfortable result lookup
				Map<Result, List<TrackingRefUpdate>> updatesByResultType
						= getUpdatesByResultType(fetchResult);

				/*
				 *  Depending on what every RefUpdate reports, we need
				 *  to abort, merge or do nothing.
				 */
				if(hasFailed(updatesByResultType.keySet())) {
					// Fetching failed for at least one ref
					String headerKey = "replaydh.wizard.gitRemoteUpdater.finish.headerUpdateFailed";
					if(updatesByResultType.containsKey(Result.LOCK_FAILURE)) {
						// Local concurrency issues might be resolvable with another try
						headerKey = "replaydh.wizard.gitRemoteUpdater.finish.headerConcurrentProcess";
					} else if(updatesByResultType.containsKey(Result.IO_FAILURE)) {
						// Same for I/O stuff, if user can fix access rights or network issues
						headerKey = "replaydh.wizard.gitRemoteUpdater.finish.headerIoProblem";
					}

					taHeader.setText(rm.get(headerKey));
					epInfo.setText(fetchResult.toString());
				} else if(hasChanged(updatesByResultType.keySet())) {
					// Local refs changed and we need to merge (or at least try...)
					List<TrackingRefUpdate> updatedRefs = getUpdatedRefs(updatesByResultType);
					doDryRun(environment, context, updatedRefs);
				} else {
					// Nothing changed
					taHeader.setText(rm.get("replaydh.wizard.gitRemoteUpdater.finish.headerNoChanges"));
					epInfo.setVisible(false);
				}

			} else {
				// Something weird happened and we don't have anything substantial to report
				taHeader.setText(rm.get("replaydh.wizard.gitRemoteUpdater.finish.headerMissingInfo"));
				epInfo.setText(context.finalMessage);
			}

			setPreviousEnabled(false);
		};

		private Map<Result, List<TrackingRefUpdate>> getUpdatesByResultType(FetchResult fetchResult) {
			Map<Result, List<TrackingRefUpdate>> result = new HashMap<>();

			fetchResult.getTrackingRefUpdates().forEach(update ->
					result.computeIfAbsent(update.getResult(), r -> new ArrayList<>()).add(update));

			return result;
		}

		/**
		 * Check if the given set of occurred results contains any indicating a fail.
		 */
		private boolean hasFailed(Set<Result> results) {
			return results.stream().anyMatch(FAILED::contains);
		}

		/**
		 * Check if the given set of occurred results contains any indicating a
		 * successful physical change.
		 */
		private boolean hasChanged(Set<Result> results) {
			return results.stream().anyMatch(CHANGED::contains);
		}

		/**
		 * Extract all the {@link TrackingRefUpdate} that indicate changed data.
		 * @param updatesByResultType
		 * @return
		 */
		private List<TrackingRefUpdate> getUpdatedRefs(Map<Result, List<TrackingRefUpdate>> updatesByResultType) {
			List<TrackingRefUpdate> updatedRefs = new ArrayList<>();

			CHANGED.forEach(result -> updatedRefs.addAll(updatesByResultType.getOrDefault(
					result, Collections.emptyList())));

			return updatedRefs;
		}

		private void doDryRun(RDHEnvironment environment, GitRemoteUpdaterContext context,
				List<TrackingRefUpdate> updatedRefs) {
			ResourceManager rm = ResourceManager.getInstance();

			taHeader.setText(rm.get("replaydh.wizard.gitRemoteUpdater.finish.dryRunMessage"));
			lIcon.setText(rm.get("replaydh.wizard.gitRemoteUpdater.finish.dryRunActive"));
			lIcon.setVisible(true);
			epInfo.setVisible(false);

			SwingWorker<MergeResult, String> worker = new SwingWorker<MergeResult, String>() {

				@Override
				protected MergeResult doInBackground() throws Exception {
					Repository repository = context.git.getRepository();

					for(TrackingRefUpdate refUpdate : updatedRefs) {
						ThreeWayMerger merger = MergeStrategy.RECURSIVE.newMerger(repository, true);
						boolean noProblems = merger.merge(refUpdate.getOldObjectId(), refUpdate.getNewObjectId());
					}

					FetchResult fetchResult = context.result;
					RevCommit headCommit = context.currentHead;

					RevCommit fetchedCommit = null;

					ThreeWayMerger merger = MergeStrategy.RECURSIVE.newMerger( repository, true );
					merger.merge( headCommit, fetchedCommit );

					// TODO Auto-generated method stub
					return null;
				}

				@Override
				protected void done() {
					//TODO handle result of merge dry run and display info or do a real merge
				};
			};

			worker.execute();
		}

		@Override
		public Page<GitRemoteUpdaterContext> next(RDHEnvironment environment,
				GitRemoteUpdaterContext context) {
			return null;
		}
	};
}

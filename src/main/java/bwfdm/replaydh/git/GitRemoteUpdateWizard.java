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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.diff.Sequence;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ResolveMerger;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.merge.ThreeWayMerger;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.eclipse.jgit.transport.Transport;
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
				CHOOSE_REMOTE, SELECT_SCOPE, UPDATE, CHECK_MERGE, RESOLVE_CONFLICTS, FINISH);
		return wizard;
	}

	/**
	 * Context for the wizard
	 */
	public static final class GitRemoteUpdaterContext extends GitRemoteContext<FetchResult> {

		/** Backup pointer to head before the pull attempt */
		public RevCommit currentHead;

		Map<String, MergeDryRunResult> mergeDryRunResults = new HashMap<>();

		public MergeResult mergeResult;

		public GitRemoteUpdaterContext(Git git) {
			super(git);
		}

		public GitRemoteUpdaterContext(RDHEnvironment environment) {
			super(environment);
		}

		Mergable getWorstDryRunResult() {
			// If we haven't done a dry run, we assume everything is ok
			Mergable worstResult = mergeDryRunResults.isEmpty() ?
					Mergable.OK : Mergable.UNKNOWN;

			for(MergeDryRunResult dryRunResult : mergeDryRunResults.values()) {
				if(dryRunResult.mergable.compareTo(worstResult)>0) {
					worstResult = dryRunResult.mergable;
				}
			}
			return worstResult;
		}
	}

	static class MergeDryRunResult {
		String localBranch;
		String remoteBranch;
		ObjectId localId;
		ObjectId remoteId;
		Mergable mergable = Mergable.UNKNOWN;

		// Low-level fields that are not part of toString()

		Throwable error;
		Conflict conflict;

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("%s -> %s %s [%s / %s]",
					localBranch, remoteBranch, mergable, localId, remoteId);
		}
	}

	static class Conflict {
		/**
		 * @see ResolveMerger#getCommitNames()
		 */
		final String commitNames[];

		/**
		 * @see ResolveMerger#getUnmergedPaths()
		 */
		final List<String> unmergedPaths;

		/**
		 * @see ResolveMerger#getModifiedFiles()
		 */
		final List<String> modifiedFiles;

		/**
		 * @see ResolveMerger#getToBeCheckedOut()
		 */
		final Map<String, DirCacheEntry> toBeCheckedOut;

		/**
		 * @see ResolveMerger#getMergeResults()
		 */
		final Map<String, org.eclipse.jgit.merge.MergeResult<? extends Sequence>> mergeResults;

		/**
		 * @see ResolveMerger#getFailingPaths()
		 */
		final Map<String, MergeFailureReason> failingPaths;

		Conflict(ResolveMerger merger) {
			commitNames = merger.getCommitNames();
			unmergedPaths = merger.getUnmergedPaths();
			modifiedFiles = merger.getModifiedFiles();
			toBeCheckedOut = merger.getToBeCheckedOut();
			mergeResults = merger.getMergeResults();
			failingPaths = merger.getFailingPaths();
		}
	}

	static enum Mergable {
		/** Initial state */
		UNKNOWN,
		/** Merge possible */
		OK,
		/** Merging failed with conflicts */
		CONFLICTING,
		/** Merging failed for other reasons (see 'error' */
		FAILED,
		;
	}

	static enum DryRunState {
		/** An error occurred (no conflicting files) causing the dry run to fail */
		FAILED_OTHER_REASON(true, false, false),
		/** Dry run went well, but the real merge yielded a result indicating a fail */
		FAILED_MERGE_ATTEMPT(true, true, false),
		/** All refs seem to be in a state suitable for a fast-forward merge */
		OK_FF(false, false, false),
		/** Dry run finished, but a conflict was detected */
		OK_CONFLICTING(false, true, true),
		/** Dry run finished, went well and we already managed to do a real merge */
		OK_MERGED(false, true, false),
		;

		public final boolean failed;
		public final boolean mergeAttempted;
		public final boolean conflicting;

		private DryRunState(boolean failed, boolean mergeAttempted, boolean conflicting) {
			this.failed = failed;
			this.mergeAttempted = mergeAttempted;
			this.conflicting = conflicting;
		}
	}

	/**
	 * Unchanged (up2date or not-attempted)
	 */
	private static final EnumSet<Result> UNCHANGED = EnumSet.of(
			Result.NO_CHANGE, Result.NOT_ATTEMPTED);

	/**
	 * Updated, forcefully overwritten, new or renamed
	 */
	private static final EnumSet<Result> CHANGED = EnumSet.of(
			Result.FAST_FORWARD, Result.FORCED, Result.NEW, Result.RENAMED);

	/**
	 * Rejected or caused by I/O or locking issues
	 */
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
	private static final SelectScopeStep<FetchResult, GitRemoteUpdaterContext> SELECT_SCOPE
		= new SelectScopeStep<FetchResult, GitRemoteUpdaterContext>(
			"selectScope",
			"replaydh.wizard.gitRemoteUpdater.selectScope.title",
			"replaydh.wizard.gitRemoteUpdater.selectScope.description",
			"replaydh.wizard.gitRemoteUpdater.selectScope.header",
			"replaydh.wizard.gitRemoteUpdater.selectScope.workspaceScope",
			"replaydh.wizard.gitRemoteUpdater.selectScope.workflowScope") {

		@Override
		public Page<GitRemoteUpdaterContext> next(RDHEnvironment environment,
				GitRemoteUpdaterContext context) {
			return defaultProcessNext(environment, context) ? UPDATE : null;
		}
	};

	/**
	 * Let user run the fetch command
	 */
	private static final PerformOperationStep<FetchResult, FetchCommand, GitRemoteUpdaterContext> UPDATE
			= new PerformOperationStep<FetchResult, FetchCommand, GitRemoteUpdaterContext>(
			"update",
			"replaydh.wizard.gitRemoteUpdater.update.title",
			"replaydh.wizard.gitRemoteUpdater.update.description") {

		@Override
		protected FetchCommand createGitCommand(
				GitWorker<FetchResult,FetchCommand,GitRemoteUpdaterContext> worker) throws GitException {
			GitRemoteUpdaterContext context = worker.context;
			FetchCommand command = context.git.fetch();

			if(!configureTransportCommand(command, context)) {
				return null;
			}

			command.setRemote(context.getRemote());

			getCurrentBranch(context);

			// If desired, we have to restrict updates to the current branch
			if(context.scope==Scope.WORKSPACE) {
				command.setRefSpecs(new RefSpec(Constants.R_HEADS+context.branch));
			}

			command.setCheckFetchedObjects(true);
			command.isRemoveDeletedRefs(); // Needed to keep track of our counter branch

			return command;
		}

		@Override
		protected void handleWorkerResult(GitWorker<FetchResult,FetchCommand,GitRemoteUpdaterContext> worker) {
			super.handleWorkerResult(worker);
			control.invokeNext(CHECK_MERGE);
		};


		@Override
		public Page<GitRemoteUpdaterContext> next(RDHEnvironment environment,
				GitRemoteUpdaterContext context) {
			// TODO is all the needed info already stored in the context?
			return CHECK_MERGE;
		}
	};

	/**
	 * Analyze the FetchResult and propose ways to fix any issues.
	 */
	private static final GitRemoteStep<FetchResult, GitRemoteUpdaterContext> CHECK_MERGE
		= new GitRemoteStep<FetchResult, GitRemoteUpdaterContext>(
			"checkMerge",
			"replaydh.wizard.gitRemoteUpdater.checkMerge.title",
			"replaydh.wizard.gitRemoteUpdater.checkMerge.description") {

		private JTextArea taHeader;
		private ErrorPanel epInfo;
		private JLabel lIcon;

		@Override
		protected JPanel createPanel() {

			taHeader = GuiUtils.createTextArea("");

			lIcon = new JLabel();
			lIcon.setIcon(IconRegistry.getGlobalRegistry().getIcon("loading-64.gif"));

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
				taHeader.setText(rm.get("replaydh.wizard.gitRemoteUpdater.checkMerge.headerError"));
			} else if(context.result!=null) {
				// We got a result, which can still signal a failure
				final FetchResult fetchResult = context.result;
				log.info("Raw result of fetching from {}: {}",
						context.getRemote(), fetchResult);

				//Create a more comfortable result lookup
				Map<Result, List<TrackingRefUpdate>> updatesByResultType
						= getUpdatesByResultType(fetchResult);

				boolean canTryMerge = false;

				/*
				 *  Depending on what every RefUpdate reports, we need
				 *  to abort, merge or do nothing.
				 */
				if(hasFailed(updatesByResultType.keySet())) {
					// Fetching failed for at least one ref
					String headerKey = "replaydh.wizard.gitRemoteUpdater.checkMerge.headerUpdateFailed";
					if(updatesByResultType.containsKey(Result.LOCK_FAILURE)) {
						// Local concurrency issues might be resolvable with another try
						headerKey = "replaydh.wizard.gitRemoteUpdater.checkMerge.headerConcurrentProcess";
					} else if(updatesByResultType.containsKey(Result.IO_FAILURE)) {
						// Same for I/O stuff, if user can fix access rights or network issues
						headerKey = "replaydh.wizard.gitRemoteUpdater.checkMerge.headerIoProblem";
					}

					taHeader.setText(rm.get(headerKey));
					epInfo.setText(fetchResult.toString()); // display raw info
				} else if(hasChanged(updatesByResultType.keySet())) {
					// Local refs changed and we need to merge (or at least try...)
					taHeader.setText(rm.get("replaydh.wizard.gitRemoteUpdater.checkMerge.headerChanged"));
					epInfo.setVisible(false);
					canTryMerge = true;
				} else {
					// Nothing changed
					taHeader.setText(rm.get("replaydh.wizard.gitRemoteUpdater.checkMerge.headerNoChanges"));
					epInfo.setVisible(false);
					canTryMerge = true;
				}

				// Check if we can merge and if not provide the user with options
				if(canTryMerge) {
					doDryRun(environment, context, updatesByResultType);
				}

			} else {
				// Something weird happened and we don't have anything substantial to report
				taHeader.setText(rm.get("replaydh.wizard.gitRemoteUpdater.checkMerge.headerMissingInfo"));
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
		 * Checks whether we can merge all the updated refs
		 */
		private void doDryRun(RDHEnvironment environment, GitRemoteUpdaterContext context,
				Map<Result, List<TrackingRefUpdate>> updatesByResultType) {
			ResourceManager rm = ResourceManager.getInstance();



			SwingWorker<DryRunState, Runnable> worker = new SwingWorker<DryRunState, Runnable>() {

				@Override
				protected DryRunState doInBackground() throws Exception {
					log.info("Merge dry run started - checking refs");

					// Tell GUI we're busy
					publish(() -> {
						lIcon.setText(rm.get("replaydh.wizard.gitRemoteUpdater.checkMerge.dryRunActive"));
						lIcon.setVisible(true);
						epInfo.setVisible(false);
					});

					final Repository repo = context.git.getRepository();

					// Figure out IF we need to merge

					// Back to square 1: we might have artifacts from an earlier fetch
					List<RefSpec> refSpecs;
					if(context.scope==Scope.WORKSPACE) {
						refSpecs = Arrays.asList(new RefSpec(context.branch));
					} else {
						refSpecs = context.remoteConfig.getFetchRefSpecs();
					}

//					refSpecs.forEach(System.out::println);

					/*
					 *  All the refs we might potentially have to merge.
					 *  Maps from remote ref to local branch names
					 */
					final List<RemoteRefUpdate> refUpdates =
							Transport.findRemoteRefUpdatesFor(repo, refSpecs, null)
								.stream()
								// Ignore all marker branches for merging (as they don't point to movable commits)
								.filter(refUpdate -> !GitUtils.isRdhMarkerBranch(refUpdate.getSrcRef()))
								.collect(Collectors.toList());

					try(RevWalk rw = new RevWalk(repo)) {
						for(RemoteRefUpdate refUpdate : refUpdates) {
							ObjectId srcId = repo.resolve(refUpdate.getSrcRef());
							ObjectId remoteId = repo.resolve(refUpdate.getRemoteName());

							// Ignore full deletions or creation of new files
							if(srcId==ObjectId.zeroId() || remoteId==ObjectId.zeroId()) {
								continue;
							}

							RevCommit ours = rw.parseCommit(srcId);
							RevCommit theirs = rw.parseCommit(remoteId);

							if(rw.isMergedInto(ours, theirs)) {
								continue;
							}

							MergeDryRunResult dryRunResult = new MergeDryRunResult();
							dryRunResult.localBranch = refUpdate.getSrcRef();
							dryRunResult.remoteBranch = refUpdate.getRemoteName();
							dryRunResult.localId = srcId;
							dryRunResult.remoteId = remoteId;

							context.mergeDryRunResults.put(dryRunResult.localBranch, dryRunResult);
						}
						rw.dispose();
					}

					//TODO debug -> remove
					context.mergeDryRunResults.values().forEach(System.out::println);

					if(context.mergeDryRunResults.isEmpty()) {
						log.info("Nothing to merge - aborting dry run");
						return DryRunState.OK_FF;
					}

					// Tell User we're doing a merge dry run
					publish(() -> {
						String existingText = taHeader.getText();
						taHeader.setText(existingText+"\n\n"
								+rm.get("replaydh.wizard.gitRemoteUpdater.checkMerge.dryRunMessage"));
					});

					/*
					 * Scenarios:
					 *
					 * 1. Everything can be merged -> run a normal MergeCommand
					 *
					 * 2. Conflicts detected - present options to user:
					 *     a)  Insert conflict markers into files (and give him an example)
					 *     b)  If conflict is in current branch: allow to create the "remote"
					 *         version of conflicted file so the user has both and can merge
					 *         manually.
					 *     c)  If conflicts are in another branch, only merge/resolve the current
					 *         branch and ask user to switch to the conflicting branch later.
					 */

					for(MergeDryRunResult dryRunResult : context.mergeDryRunResults.values()) {
						// In-memory merge dry run
						ThreeWayMerger merger = MergeStrategy.RECURSIVE.newMerger(repo, true);

						if(merger instanceof ResolveMerger) {
							((ResolveMerger)merger).setCommitNames(new String[] {
									"BASE", "HEAD", dryRunResult.remoteBranch });
						}

						try {
							boolean noProblems = merger.merge(dryRunResult.localId, dryRunResult.remoteId);

							if(noProblems) {
								// All went well, rejoice
								dryRunResult.mergable = Mergable.OK;
							} else if(merger instanceof ResolveMerger) {
								// We failed to merge, but inspecting the ResolveMerger should give additional hints
								ResolveMerger resolveMerger = (ResolveMerger) merger;
								dryRunResult.conflict = new Conflict(resolveMerger);
								if(resolveMerger.failed()) {
									// Non-conflicting errors
									dryRunResult.mergable = Mergable.FAILED;
								} else {
									// Content conflicts, from this we can recover via help from user
									dryRunResult.mergable = Mergable.CONFLICTING;
								}
							} else {
								// We failed and we have no damn clue as to why...
								dryRunResult.mergable = Mergable.FAILED;
							}
						} catch(IOException e) {
							// Serious issue: merger couldn't even complete its job
							log.error("Error during merge dry run on {}", dryRunResult, e);
							dryRunResult.error = e;
							dryRunResult.mergable = Mergable.FAILED;
						}
					}

					Mergable worstResult = context.getWorstDryRunResult();

					// In case everything went well we'll directly do the real merge
					if (worstResult==Mergable.OK) {
						// Tell User we're attempting the real merge now
						publish(() -> {
							String existingText = taHeader.getText();
							taHeader.setText(existingText+"\n\n"
									+rm.get("replaydh.wizard.gitRemoteUpdater.checkMerge.mergeMessage"));
						});

						// Grab the dry run that used our current branch
						String mergeHead = null;
						MergeDryRunResult dryRun = context.mergeDryRunResults.get(context.branch);
						if(dryRun!=null) {
							mergeHead = dryRun.remoteBranch;
						}
						if(mergeHead==null) {
							String b = Repository.shortenRefName(context.branch);
							mergeHead = Constants.R_REMOTES+context.getRemote()+'/'+b;
						}

						// Run a real merge with settings identical to the dry run
						log.info("merging {} into {}", mergeHead, context.branch);
						context.mergeResult = context.git.merge()
								.setStrategy(MergeStrategy.RECURSIVE)
								.include(repo.exactRef(mergeHead))
								.setCommit(false) // We want to be able to decide on the specifics of the commit
								.call();

						// Merge attempt finished without a question, so either succeeded or experienced an unexpected fail
						return context.mergeResult.getMergeStatus().isSuccessful() ?
								DryRunState.OK_MERGED : DryRunState.FAILED_MERGE_ATTEMPT;
					}

					/*
					 *  At this point we had no major errors and performed a dry run, but did not
					 *  continue to do a real merge, so we either have conflicting files or other
					 *  non-exception errors.
					 */
					return worstResult==Mergable.CONFLICTING ?
							DryRunState.OK_CONFLICTING : DryRunState.FAILED_OTHER_REASON;
				}

				@Override
				protected void process(List<Runnable> chunks) {
					chunks.forEach(Runnable::run);
				};

				@Override
				protected void done() {
					lIcon.setVisible(false);

					boolean canContinue = true;

					try {
						//TODO use the DryRunState to decide what to do next and what to display to the user
						DryRunState state = get();
					} catch (InterruptedException | CancellationException e) {
						// Operation cancelled (no idea how)
						log.info("Merge dry run cancelled");
						canContinue = false;
					} catch (ExecutionException e) {
						log.error("Error during merge dry run", e.getCause());

						context.error = e.getCause();
						canContinue = false;
					}

					Throwable errorToDisplay = null;

					if(context.error!=null) {
						// Something went horribly wrong
						errorToDisplay = context.error;
					} else if(context.mergeResult!=null) {
						// We already performed a real merge, which SHOULD have succeeded
						canContinue = context.mergeResult.getMergeStatus().isSuccessful();
					} else if(!context.mergeDryRunResults.isEmpty()) {
						// Dry run finished, but we didn't do a succesful real merge

						//TODO
					} else {
						// Nothing to check for merging
					}

					setNextEnabled(canContinue);
				};
			};

			worker.execute();
		}

		private void displayResults(RDHEnvironment environment,
				GitRemoteUpdaterContext context) {
			//TODO
		}

		@Override
		public Page<GitRemoteUpdaterContext> next(RDHEnvironment environment,
				GitRemoteUpdaterContext context) {
			//TODO
			return null;
		}
	};

	/**
	 * Analyze the FetchResult and propose ways to fix any issues.
	 */
	private static final GitRemoteStep<FetchResult, GitRemoteUpdaterContext> RESOLVE_CONFLICTS
		= new GitRemoteStep<FetchResult, GitRemoteUpdaterContext>(
			"resolveConflicts",
			"replaydh.wizard.gitRemoteUpdater.resolveConflicts.title",
			"replaydh.wizard.gitRemoteUpdater.resolveConflicts.description") {

		@Override
		protected JPanel createPanel() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Page<GitRemoteUpdaterContext> next(RDHEnvironment environment, GitRemoteUpdaterContext context) {
			// TODO Auto-generated method stub
			return null;
		}

	};

	/**
	 * Analyze the FetchResult and propose ways to fix any issues.
	 */
	private static final GitRemoteStep<FetchResult, GitRemoteUpdaterContext> FINISH
		= new GitRemoteStep<FetchResult, GitRemoteUpdaterContext>(
			"finish",
			"replaydh.wizard.gitRemoteUpdater.finish.title",
			"replaydh.wizard.gitRemoteUpdater.finish.description") {

		@Override
		protected JPanel createPanel() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Page<GitRemoteUpdaterContext> next(RDHEnvironment environment, GitRemoteUpdaterContext context) {
			// TODO Auto-generated method stub
			return null;
		}

	};
}

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

import java.awt.Window;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.diff.Sequence;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.Merger;
import org.eclipse.jgit.merge.ResolveMerger;
import org.eclipse.jgit.merge.ResolveMerger.MergeFailureReason;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.OrTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
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
				CHOOSE_REMOTE, SELECT_SCOPE, UPDATE, CHECK_MERGE, RESOLVE_CONFLICTS, MERGE);
		return wizard;
	}

	/**
	 * Context for the wizard
	 */
	public static final class GitRemoteUpdaterContext extends GitRemoteContext<FetchResult> {

		/** Backup pointer to head before the pull attempt */
		public RevCommit currentHead;

		/** Maps shortened ref names to the results of merge dry runs */
		Map<String, MergeDryRunResult> mergeDryRunResults = new HashMap<>();

		/** If we actualyl attempt a merge, this holds the result */
		public MergeResult mergeResult;

		/** Branch to merge our current HEAD with */
		public String remoteBranch;

		/** In case of conflicts, keep track of user's strategy choice */
		public ConflictStrategy strategy;

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

		MergeDryRunResult getActiveBranchDryRunResult() {
			return mergeDryRunResults.get(branch);
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
		/** All refs are already up to date */
		UP_TO_DATE(false, false, false),
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

	static enum ConflictStrategy {
		/**
		 * Normal merging
		 */
		MARKER("marker"),
		/**
		 * For every conflicting file, create a {@code <name>.other.<filetype>}
		 * with the content of the remote version.
		 */
		DUPLICATE("duplicate"),
		/**
		 * Ignore all remote changes
		 */
//		OURS("ours"),
		/**
		 * Ignore all local changes
		 */
//		THEIRS("theirs"),
		;

		private final String key;

		private ConflictStrategy(String key) {
			this.key = requireNonNull(key);
		}

		public String getDescription() {
			return ResourceManager.getInstance().get(
					"replaydh.wizard.gitRemoteUpdater.resolveConflicts.strategy."+key+".description");
		}

		@Override
		public String toString() {
			return ResourceManager.getInstance().get(
					"replaydh.wizard.gitRemoteUpdater.resolveConflicts.strategy."+key+".label");
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

	private static final MergeStrategy MERGE_STRATEGY = MergeStrategy.RECURSIVE;

	/**
	 * First step: Let user provide or select a remote repository URL
	 */
	private static final ChooseRemoteStep<FetchResult, GitRemoteUpdaterContext> CHOOSE_REMOTE
		= new ChooseRemoteStep<FetchResult, GitRemoteUpdaterContext>(
			"chooseRemote",
			"replaydh.wizard.gitRemoteUpdater.chooseRemote.title",
			"replaydh.wizard.gitRemoteUpdater.chooseRemote.description",
			null,
			"replaydh.wizard.gitRemoteUpdater.chooseRemote.middle",
			null) {

		@Override
		public Page<GitRemoteUpdaterContext> next(RDHEnvironment environment,
				GitRemoteUpdaterContext context) {
			if(!defaultProcessNext(environment, context)) {
				return null;
			}

			/*
			 *  We could check for multiple branches and decide to skip the scope
			 *  selection, but we rly can't predict if there are new remote branches.
			 */

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
						refSpecs = Arrays.asList(new RefSpec(
								Constants.R_HEADS+context.branch+":"+Constants.R_REMOTES+context.getRemote()+'/'+context.branch));
					} else {
						refSpecs = context.remoteConfig.getFetchRefSpecs();
					}

//					refSpecs.forEach(System.out::println);

					/*
					 *  All the refs we might potentially have to merge.
					 *  Maps from remote ref to local branch names
					 */
					final List<RemoteRefUpdate> refUpdates =
							Transport.findRemoteRefUpdatesFor(repo, refSpecs, Collections.emptyList())
								.stream()
								// Ignore all marker branches for merging (as they don't point to movable commits)
								.filter(refUpdate -> !GitUtils.isRdhMarkerBranch(refUpdate.getSrcRef()))
								.collect(Collectors.toList());

					try(RevWalk rw = new RevWalk(repo)) {
						for(RemoteRefUpdate refUpdate : refUpdates) {
							if(context.branch.equals(Repository.shortenRefName(refUpdate.getSrcRef()))) {
								context.remoteBranch = refUpdate.getRemoteName();
							}

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

							context.mergeDryRunResults.put(
									Repository.shortenRefName(dryRunResult.localBranch),
									dryRunResult);
						}
						rw.dispose();
					}

//					context.mergeDryRunResults.values().forEach(System.out::println);

					/*
					 *  Fail-fast check: If we can't even locate our target
					 *  for the active branch, there's no real reason to attempt
					 *  further.
					 */
					if(context.remoteBranch==null && !refUpdates.isEmpty()) {
						log.error("Unable to merge - failed to obtain remote ref for current branch");
						return DryRunState.FAILED_OTHER_REASON;
					}

					if(context.mergeDryRunResults.isEmpty()) {
						log.info("Nothing to merge - aborting dry run and doing a real fast-forward merge");
						context.mergeResult = context.git.merge()
							.setFastForward(FastForwardMode.FF_ONLY)
							.include(repo.exactRef(context.remoteBranch))
							.call();

						MergeStatus mergeStatus = context.mergeResult.getMergeStatus();

						if(!mergeStatus.isSuccessful()) {
							return DryRunState.FAILED_MERGE_ATTEMPT;
						}

						switch (mergeStatus) {
						case ALREADY_UP_TO_DATE: return DryRunState.UP_TO_DATE;
						case FAST_FORWARD: return DryRunState.OK_FF;

						default:
							return DryRunState.OK_MERGED;
						}
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
						Merger merger = MERGE_STRATEGY.newMerger(repo, true);

						if(merger instanceof ResolveMerger) {
							((ResolveMerger)merger).setCommitNames(new String[] {
									"BASE", "HEAD", Repository.shortenRefName(dryRunResult.remoteBranch) });
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
								.setStrategy(MERGE_STRATEGY)
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

					DryRunState dryRunState;

					try {
						dryRunState = get();
					} catch (InterruptedException | CancellationException e) {
						// Operation cancelled (no idea how)
						log.info("Merge dry run cancelled");
						dryRunState = DryRunState.FAILED_OTHER_REASON; // not the best option, maybe add CANCELLED
					} catch (ExecutionException e) {
						log.error("Error during merge dry run", e.getCause());

						context.error = e.getCause();
						dryRunState = DryRunState.FAILED_OTHER_REASON;
					}

					displayResults(environment, context, dryRunState);
				};
			};

			worker.execute();
		}

		private void displayResults(RDHEnvironment environment,
				GitRemoteUpdaterContext context, DryRunState dryRunState) {

			ResourceManager rm = ResourceManager.getInstance();
			boolean canContinue = false;
			boolean markFinished = false;
			Throwable errorToDisplay = context.error;
			String infoText;

			switch (dryRunState) {
			case UP_TO_DATE: {
				// Nothing to merge was found
				markFinished = true;
				infoText = rm.get("replaydh.wizard.gitRemoteUpdater.checkMerge.mergeDoneTemplate");
			} break;

			case OK_FF: {
				// Nothing to merge was found
				markFinished = true;
				infoText = rm.get("replaydh.wizard.gitRemoteUpdater.checkMerge.noMergeNeeded");
			} break;

			case OK_MERGED: {
				// We already performed a successful real merge
				markFinished = true;
				infoText = rm.get("replaydh.wizard.gitRemoteUpdater.checkMerge.fullMergeDone");
			} break;

			case OK_CONFLICTING: {
				// Merge dry run found conflicts - need to check if they affect current branch
				MergeDryRunResult dryRunResult = context.mergeDryRunResults.get(context.branch);

				if(dryRunResult.mergable==Mergable.CONFLICTING) {
					// User intervention only required for current branch
					canContinue = true;
					infoText = rm.get("replaydh.wizard.gitRemoteUpdater.checkMerge.activeBranchConflict");
				} else {
					markFinished = true;
					infoText = rm.get("replaydh.wizard.gitRemoteUpdater.checkMerge.otherBranchConflict");
				}
			} break;

			case FAILED_MERGE_ATTEMPT: {
				// Error during merge attempt
				String reason = null;
				MergeResult mergeResult = context.mergeResult;
				if(mergeResult!=null) {
					reason = mergeResult.toString();
				}
				infoText = rm.get("replaydh.wizard.gitRemoteUpdater.checkMerge.mergeAttemptFailed", reason);
			} break;

			case FAILED_OTHER_REASON: {
				// Something went horribly wrong
				infoText = rm.get("replaydh.wizard.gitRemoteUpdater.checkMerge.mergeDryRunFailed");
			} break;

			default:
				infoText = rm.get("replaydh.wizard.gitRemoteUpdater.checkMerge.undefinedResult");
				break;
			}

			if(errorToDisplay!=null) {
				epInfo.setThrowable(errorToDisplay);
				taHeader.setText(infoText);
				epInfo.setVisible(true);
			} else {
				epInfo.setText(infoText);
				epInfo.setVisible(infoText!=null);
			}

			if(markFinished) {
				markFinished();
			} else {
				setNextEnabled(canContinue);
			}
		}

		@Override
		public Page<GitRemoteUpdaterContext> next(RDHEnvironment environment,
				GitRemoteUpdaterContext context) {
			return RESOLVE_CONFLICTS;
		}
	};

	/**
	 * List conflicting files and propose ways to solve it.
	 */
	private static final GitRemoteStep<FetchResult, GitRemoteUpdaterContext> RESOLVE_CONFLICTS
		= new GitRemoteStep<FetchResult, GitRemoteUpdaterContext>(
			"resolveConflicts",
			"replaydh.wizard.gitRemoteUpdater.resolveConflicts.title",
			"replaydh.wizard.gitRemoteUpdater.resolveConflicts.description") {

		private JComboBox<ConflictStrategy> cbStrategy;

		private JTextArea taInfo;

		@Override
		protected JPanel createPanel() {
			ResourceManager rm = ResourceManager.getInstance();

			cbStrategy = new JComboBox<>(ConflictStrategy.values());
			cbStrategy.addActionListener(ae -> onStrategySelected());

			taInfo = GuiUtils.createTextArea("");

			onStrategySelected();

			return FormBuilder.create()
					.columns("pref, 4dlu, fill:pref:grow")
					.rows("pref, 6dlu, pref, 6dlu, pref")
					.add(GuiUtils.createTextArea(rm.get("replaydh.wizard.gitRemoteUpdater.resolveConflicts.header"))).xyw(1, 1, 3)

					.addLabel(rm.get("replaydh.wizard.gitRemoteUpdater.resolveConflicts.strategy.label")+":").xy(1, 3)
					.add(cbStrategy).xy(3, 3)
					.add(taInfo).xyw(1, 5, 3)

					.build();
		}

		private void onStrategySelected() {
			ConflictStrategy strategy = (ConflictStrategy) cbStrategy.getSelectedItem();
			taInfo.setText(strategy==null ? "???" : strategy.getDescription());
		}

		@Override
		public void refresh(RDHEnvironment environment, GitRemoteUpdaterContext context) {
			if(context.strategy!=null) {
				cbStrategy.setSelectedItem(context.strategy);
			}
		};

		@Override
		public Page<GitRemoteUpdaterContext> next(RDHEnvironment environment, GitRemoteUpdaterContext context) {
			context.strategy = (ConflictStrategy)cbStrategy.getSelectedItem();

			return MERGE;
		}

	};

	/**
	 * Analyze the FetchResult and propose ways to fix any issues.
	 */
	private static final GitRemoteStep<FetchResult, GitRemoteUpdaterContext> MERGE
		= new GitRemoteStep<FetchResult, GitRemoteUpdaterContext>(
			"finish",
			"replaydh.wizard.gitRemoteUpdater.finish.title",
			"replaydh.wizard.gitRemoteUpdater.finish.description") {

		private JTextArea taHeader;
		private JLabel lIcon;
		private ErrorPanel epInfo;

		private SwingWorker<?, ?> worker;

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
		public void refresh(RDHEnvironment environment, GitRemoteUpdaterContext context) {
			setPreviousEnabled(false);
			lIcon.setVisible(false);

			// Reset previous errors
			context.error = null;

			switch (context.strategy) {
			case MARKER:
				postConflictMarkers(environment, context);
				break;

			case DUPLICATE:
				createDuplicates(environment, context);
				break;

			default:
				taHeader.setText("Unable to handle selected strategy: "+context.strategy);
				break;
			}
		};

		private void postConflictMarkers(RDHEnvironment environment, GitRemoteUpdaterContext context) {
			worker = new SwingWorker<MergeResult, Runnable>() {

				@Override
				protected MergeResult doInBackground() throws Exception {
					publish(() -> {
						lIcon.setVisible(true);
						taHeader.setText("replaydh.wizard.gitRemoteUpdater.finish.mergeActive");
					});

					return context.git.merge()
							.setStrategy(MERGE_STRATEGY)
							.setCommit(false)
							.include(context.git.getRepository().exactRef(context.remoteBranch))
							.call();
				}

				@Override
				protected void process(List<Runnable> chunks) {
					chunks.forEach(Runnable::run);
				};

				@Override
				protected void done() {
					lIcon.setVisible(false);

					MergeResult mergeResult = null;

					try {
						mergeResult = get();
					} catch (InterruptedException | CancellationException e) {
						// Operation cancelled (no idea how)
						log.info("Merge run cancelled");
					} catch (ExecutionException e) {
						log.error("Error during merge", e.getCause());

						context.error = e.getCause();
					}

					ResourceManager rm = ResourceManager.getInstance();

					if(context.error!=null) {
						// Major fail
						taHeader.setText(rm.get("replaydh.wizard.gitRemoteUpdater.finish.mergeFailed"));
						epInfo.setThrowable(context.error);
					} else if(mergeResult!=null && mergeResult.getMergeStatus()==MergeStatus.CONFLICTING) {
						// Success (we expected conflicts after all)
						taHeader.setText(rm.get("replaydh.wizard.gitRemoteUpdater.finish.mergeDone"));

						StringBuilder sb = new StringBuilder();
						mergeResult.getConflicts().keySet()
							.stream()
							.sorted()
							.forEach(path -> sb.append(path).append('\n'));
						epInfo.setText(sb.toString());
					} else {
						// Merge finished with internal problems
						taHeader.setText(rm.get("replaydh.wizard.gitRemoteUpdater.finish.mergeFailed"));
						epInfo.setText(mergeResult.toString());
					}
					epInfo.setVisible(true);
				};
			};
			worker.execute();
		}

		private void createDuplicates(RDHEnvironment environment, GitRemoteUpdaterContext context) {
			worker = new SwingWorker<List<String>, Runnable>() {

				@Override
				protected List<String> doInBackground() throws Exception {
					publish(() -> {
						lIcon.setVisible(true);
						taHeader.setText("replaydh.wizard.gitRemoteUpdater.finish.duplicationActive");
					});

					MergeDryRunResult dryRunResult = context.getActiveBranchDryRunResult();
					if(dryRunResult==null || dryRunResult.conflict==null)
						throw new IllegalStateException("Expected valid dry run result and conflict information for active branch");

					List<String> duplicates = new ArrayList<>();

					final Repository repo = context.git.getRepository();
					final Path root = repo.getWorkTree().toPath();

					List<TreeFilter> pathFilters = dryRunResult.conflict.unmergedPaths
							.stream()
							.map(PathFilter::create)
							.collect(Collectors.toList());

					if(pathFilters.isEmpty())
						throw new IllegalStateException("Expected conflicting file definitions");

					TreeFilter filter = pathFilters.size()==1 ?
							pathFilters.get(0) : OrTreeFilter.create(pathFilters);

					try(TreeWalk tw = new TreeWalk(repo)) {
						RevCommit commit = repo.parseCommit(dryRunResult.remoteId);
						tw.reset(commit.getTree());
						tw.setRecursive(true);
						tw.setFilter(filter);

						while(tw.next()) {
							String path = tw.getPathString();

							Path origPath = Paths.get(path);
							Path parent = origPath.getParent();
							String name = origPath.getFileName().toString();

							int dot = name.indexOf('.');
							if(dot==-1) {
								name = name+GitUtils.DUPLICATE_MARKER;
							} else {
								String properName = name.substring(0, dot);
								String ending = name.substring(dot);
								name = properName+GitUtils.DUPLICATE_MARKER+ending;
							}

							Path newPath = root;
							if(parent!=null) {
								newPath = newPath.resolve(parent);
							}
							newPath = newPath.resolve(name);

							if(Files.exists(newPath, LinkOption.NOFOLLOW_LINKS))
								throw new FileAlreadyExistsException(newPath.toString());

							ObjectId id = tw.getObjectId(0);
							if(id==ObjectId.zeroId())
								throw new IllegalStateException("Expected file for "+origPath);

							ObjectLoader loader = repo.open(id);
							try(OutputStream out = Files.newOutputStream(newPath)) {
								loader.copyTo(out);
							}

							duplicates.add(name);
						}
					}

					return duplicates;
				}

				@Override
				protected void process(List<Runnable> chunks) {
					chunks.forEach(Runnable::run);
				};

				@Override
				protected void done() {
					lIcon.setVisible(false);

					List<String> duplicates = null;

					try {
						duplicates = get();
					} catch (InterruptedException | CancellationException e) {
						// Operation cancelled (no idea how)
						log.info("Duplication run cancelled");
					} catch (ExecutionException e) {
						log.error("Error during duplication", e.getCause());

						context.error = e.getCause();
					}

					ResourceManager rm = ResourceManager.getInstance();

					if(context.error!=null) {
						// Major fail
						taHeader.setText(rm.get("replaydh.wizard.gitRemoteUpdater.finish.duplicationFailed"));
						epInfo.setThrowable(context.error);
					} else if(duplicates!=null && !duplicates.isEmpty()) {
						// Success
						taHeader.setText(rm.get("replaydh.wizard.gitRemoteUpdater.finish.duplicationDone"));

						StringBuilder sb = new StringBuilder();
						duplicates.stream()
							.sorted()
							.forEach(path -> sb.append(path).append('\n'));
						epInfo.setText(sb.toString());
					} else {
						// Duplication finished with internal problems
						taHeader.setText(rm.get("replaydh.wizard.gitRemoteUpdater.finish.duplicationFailed"));
					}
					epInfo.setVisible(true);
				};
			};

			worker.execute();
		}

		@Override
		public boolean canCancel() {
			return worker==null || !worker.isDone();
		};

		@Override
		public boolean close() {
			worker = null;

			return true;
		};

		@Override
		public Page<GitRemoteUpdaterContext> next(RDHEnvironment environment, GitRemoteUpdaterContext context) {
			return null;
		}

	};
}

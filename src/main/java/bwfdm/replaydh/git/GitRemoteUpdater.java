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
import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;

import javax.swing.SwingUtilities;

import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.transport.FetchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.io.TrackingStatus;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.AbstractDialogWorker;
import bwfdm.replaydh.ui.helper.AbstractDialogWorker.CancellationPolicy;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.utils.RDHUtils;
import bwfdm.replaydh.workflow.git.GitRemoteUpdateWizard;
import bwfdm.replaydh.workflow.git.GitRemoteUpdateWizard.GitRemoteUpdaterContext;

/**
 * @author Markus Gärtner
 *
 */
public class GitRemoteUpdater {

	private static final Logger log = LoggerFactory.getLogger(GitRemoteUpdater.class);

	private final RDHEnvironment environment;

	public GitRemoteUpdater(RDHEnvironment environment) {
		this.environment = requireNonNull(environment);
	}

	public void update(Component parent) {
		GuiUtils.checkNotEDT();

		log.info("Initiated update of local workspace");

		final JGitAdapter gitAdapter = JGitAdapter.fromClient(environment.getClient());

		// Check that we don't have any unsaved changes
		new AbstractDialogWorker<Boolean, Object>(SwingUtilities.getWindowAncestor(parent),
				ResourceManager.getInstance().get("replaydh.dialogs.checkWorkspaceForImport.title"),
				CancellationPolicy.NO_CANCEL) {

			final EnumMap<TrackingStatus, Integer> fileCountForStatus = new EnumMap<>(TrackingStatus.class);

			@Override
			protected Boolean doInBackground() throws Exception {
				gitAdapter.refreshStatusInfo();

				/*
				 * Files with status UNKNOWN are included here as we want to make
				 * users aware of the risks of having files in the local workspace
				 * that are not part of the Git index.
				 */
				for(TrackingStatus trackingStatus : Arrays.asList(
								TrackingStatus.MODIFIED,
								TrackingStatus.CORRUPTED,
								TrackingStatus.MISSING,
								TrackingStatus.UNKNOWN)) {
					int count = gitAdapter.getFileCountForStatus(trackingStatus);
					if(count>0) {
						fileCountForStatus.put(trackingStatus, Integer.valueOf(count));
					}
				}

				return !fileCountForStatus.isEmpty();
			}
			@Override
			protected String getMessage(MessageType messageType, Throwable t) {
				ResourceManager rm = ResourceManager.getInstance();

				switch (messageType) {
				case RUNNING:
					log.info("Running pre-import workspace check");
					return rm.get("replaydh.dialogs.checkWorkspaceForImport.message");
				case FAILED:
					log.error("Error during pre-import check of local workspace", t);
					return rm.get("replaydh.dialogs.checkWorkspaceForImport.failed", t.getMessage());
				case FINISHED:
					log.info("Pre-import workspace check done: {}", fileCountForStatus);
					if(fileCountForStatus.isEmpty()) {
						return rm.get("replaydh.dialogs.checkWorkspaceForImport.done");
					} else {
						StringBuilder sb = new StringBuilder();

						fileCountForStatus.entrySet()
						.forEach(entry -> sb.append(sb.length()>0 ? "\n" : "")
								.append(RDHUtils.getTitle(entry.getKey()))
								.append(": ")
								.append(String.valueOf(entry.getValue().intValue())));

						return rm.get("replaydh.dialogs.checkWorkspaceForImport.notPossible", sb.toString());
					}

				default:
					throw new IllegalArgumentException("Unknown or unsupported message type: "+messageType);
				}
			}
			@Override
			protected void doneImpl(Boolean result) {

				// If we have changes blocking our import capability, we need to directly stop!
				if(result.booleanValue()) {
					log.info("Updating local workspace aborted due to uncommitted local changes");
					return;
				}

				end();

				// Workspace is ready for import, now show the real dialog
				GuiUtils.invokeEDTLater(() -> {
					GitRemoteUpdaterContext context;
					try {
						context = createUpdaterContext(gitAdapter);
					} catch (IOException e) {
						log.error("Failed to create context for wizard", e);
						GuiUtils.showErrorDialog(parent,
								ResourceManager.getInstance().get("replaydh.wizard.gitRemoteUpdater.prepare.failedTitle"),
								ResourceManager.getInstance().get("replaydh.wizard.gitRemoteUpdater.prepare.localGitError"),
								e);
						GuiUtils.beep();

						return;
					}

					// Here be user interaction
					if(showWizard(parent, context)) {
						log.info("Finished updating local workspace");

						// Post-processing: refresh state of our git adapter based on pull result
//						if(context.commandCompleted) {
//							environment.execute(() -> handlePullResult(gitAdapter, context.result));
//						}
						//TODO do we actually need cleanup or maintenance work if the wizard itself takes care already?
					} else {
						log.info("Updating local workspace cancelled");
					}
				});

			};
		}.start();
	}

	private GitRemoteUpdaterContext createUpdaterContext(JGitAdapter gitAdapter) throws IOException {
		GitRemoteUpdaterContext context = new GitRemoteUpdaterContext(gitAdapter.getGit());
		context.currentHead = gitAdapter.head();

		return context;
	}

	private boolean showWizard(Component owner, GitRemoteUpdaterContext context) {

		boolean wizardDone = false;

		Window ancestorWindow = null;
		if(owner != null) {
			ancestorWindow = SwingUtilities.getWindowAncestor(owner);
		}

		try(Wizard<GitRemoteUpdaterContext> wizard = GitRemoteUpdateWizard.getWizard(
				ancestorWindow, environment)) {

			wizard.startWizard(context);
			wizardDone = wizard.isFinished() && !wizard.isCancelled();
		}

		return wizardDone;
	}

	private void handlePullResult(JGitAdapter gitAdapter, PullResult pullResult) {

		FetchResult fetchResult = pullResult.getFetchResult();
		MergeResult mergeResult = pullResult.getMergeResult();

		//TODO check the PullResult and make the gitAdapter refresh data
	}
}

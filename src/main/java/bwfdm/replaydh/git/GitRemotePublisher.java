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

import java.awt.Component;
import java.awt.Window;
import java.io.IOException;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.workflow.export.ExportException;
import bwfdm.replaydh.workflow.export.ResourcePublisher;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo;
import bwfdm.replaydh.workflow.export.git.GitRemotePublisherWizard;
import bwfdm.replaydh.workflow.export.git.GitRemotePublisherWizard.GitRemotePublisherContext;

/**
 * @author Markus Gärtner
 *
 */
public class GitRemotePublisher implements ResourcePublisher {

	protected static final Logger log = LoggerFactory.getLogger(GitRemotePublisher.class);

	/**
	 * @see bwfdm.replaydh.workflow.export.ResourcePublisher#publish(bwfdm.replaydh.workflow.export.WorkflowExportInfo)
	 */
	@Override
	public void publish(WorkflowExportInfo exportInfo)
			throws IOException, ExportException, InterruptedException {

		GuiUtils.checkNotEDT();

		log.info("Publication to remote Git initiated");

		RDHEnvironment environment = exportInfo.getEnvironment();
		JGitAdapter gitAdapter = JGitAdapter.fromClient(environment.getClient());
		GitRemotePublisherContext context = new GitRemotePublisherContext(exportInfo, gitAdapter.getGit());

		if(showWizard(null, environment, context)) {
			log.info("Finished publishing to remote Git: {}", context.getRemote());
		}
	}

	private static boolean showWizard(Component owner, RDHEnvironment environment, GitRemotePublisherContext context) {

		boolean wizardDone = false;

		Window ancestorWindow = null;
		if(owner != null) {
			ancestorWindow = SwingUtilities.getWindowAncestor(owner);
		}

		try {
			try(Wizard<GitRemotePublisherContext> wizard = GitRemotePublisherWizard.getWizard(
					ancestorWindow, environment)) {

				wizard.startWizard(context);
				wizardDone = wizard.isFinished() && !wizard.isCancelled();
			}
		}catch (Throwable e) {
			e.printStackTrace();
		}

		return wizardDone;
	}
}

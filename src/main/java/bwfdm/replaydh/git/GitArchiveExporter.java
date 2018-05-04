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

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jgit.api.ArchiveCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.archive.TarFormat;
import org.eclipse.jgit.archive.Tbz2Format;
import org.eclipse.jgit.archive.TgzFormat;
import org.eclipse.jgit.archive.TxzFormat;
import org.eclipse.jgit.archive.ZipFormat;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.core.RDHClient;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHTool;
import bwfdm.replaydh.workflow.export.ExportException;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.Type;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo.WorkflowScope;
import bwfdm.replaydh.workflow.export.WorkflowExporter;

/**
 * @author Markus Gärtner
 *
 */
public class GitArchiveExporter implements WorkflowExporter {

	private static final Logger log = LoggerFactory.getLogger(GitArchiveExporter.class);

//	// UI part
//
//	private JPanel panel;
//
//	private ButtonGroup selection;

//	// SETTINGS part
//
//	private String format;

	// Setup stuff

	/**
	 * Maps format names to their default file ending
	 */
	private static final Map<String, String> formats = new LinkedHashMap<>();

	private static final void register(String name, String fileEnding, ArchiveCommand.Format<?> fmt) {
		formats.put(name, fileEnding);
		ArchiveCommand.registerFormat(name, fmt);
	}

	private static final String DEFAULT_FORMAT = "zip";

	static {
		register("zip", "zip", new ZipFormat());
		register("tar", "tar", new TarFormat());
		register("tgz", "tar.gz", new TgzFormat());
		register("tbz2", "tar.bz2", new Tbz2Format());
		register("txz", "tar.xz", new TxzFormat());
	}

	/**
	 * @see bwfdm.replaydh.workflow.export.WorkflowExporter#export(bwfdm.replaydh.workflow.export.WorkflowExportInfo)
	 */
	@Override
	public void export(WorkflowExportInfo exportInfo) throws IOException, ExportException, InterruptedException {
		requireNonNull(exportInfo);
		checkArgument("Can only archive state for individual steps", exportInfo.getWorkflowScope()==WorkflowScope.STEP);
		checkArgument("Can only archive objects", exportInfo.getType()==Type.OBJECT);

		final JGitAdapter adapter = getAdapter(exportInfo.getEnvironment());
		final Git git = adapter.getGit();

		synchronized (adapter.getGitLock()) {
			RevCommit commit = adapter.loadId(exportInfo.getTargetStep());

			final Path path = exportInfo.getOutputResource().getPath();

			String fileName = path.getFileName().toString();
			String format = null;

			/*
			 *  Try to find out what format to use based on file ending.
			 *  If the suggested file endings from our plugin declaration
			 *  are used, this should always be successful in finding the
			 *  correct format identifier.
			 */
			for(Entry<String, String> entry : formats.entrySet()) {
				if(fileName.toLowerCase().endsWith(entry.getValue().toLowerCase())) {
					format = entry.getKey();
				}
			}

			if(format==null) {
				format = DEFAULT_FORMAT;
				log.warn("No supported format specified - defaulting to {} for exporting to {}", format, path);
			}

			try {
				@SuppressWarnings("unused")
				OutputStream out = git.archive()
					.setTree(commit)
					.setFormat(format)
					.setOutputStream(exportInfo.createOutputStream())
					.call();
				log.info("Exported {} as {} to {}", commit, format, path);
			} catch (GitAPIException e) {
				throw new ExportException("Internal git exception while attempting to archive", e);
			}
		}
	}

	private JGitAdapter getAdapter(RDHEnvironment environment) {
		RDHClient client = environment.getClient();
		RDHTool fileTracker = client.getFileTracker();
		if(fileTracker instanceof JGitAdapter) {
			return (JGitAdapter) fileTracker;
		} else
			throw new IllegalStateException("No JGitAdapter available");
	}
}

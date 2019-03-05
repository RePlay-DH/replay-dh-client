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
package bwfdm.replaydh.test.git;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * @author Markus Gärtner
 *
 */
public abstract class RepositoryTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	protected Git createGit(String name, boolean bare) throws GitAPIException, IOException {
		File repoDir = folder.newFolder(name);
		return Git.init().setDirectory(repoDir).setBare(bare).call();
	}

	public static final String LOCAL_NAME = "local";
	public static final String REMOTE_NAME = "remote";
	public static final String OTHER_NAME = "other";

	protected Git createLocalGit() throws GitAPIException, IOException {
		return createGit(LOCAL_NAME, false);
	}

	protected Git createRemoteGit() throws GitAPIException, IOException {
		return createGit(REMOTE_NAME, true);
	}

	protected Git createOtherGit() throws GitAPIException, IOException {
		return createGit(OTHER_NAME, false);
	}

	protected File makeFile(String parent, String name, String content) throws IOException {
		File dir = new File(folder.getRoot(), parent);
		File file = new File(dir, name);
		try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
			writer.write(content);
		}
		return file;
	}

	protected File makeLocalFile(String name, String content) throws IOException {
		return makeFile(LOCAL_NAME, name, content);
	}

	protected File makeOtherFile(String name, String content) throws IOException {
		return makeFile(OTHER_NAME, name, content);
	}

	protected void addRemote(Git git, Git remote) throws MalformedURLException, GitAPIException {
		RemoteAddCommand cmd = git.remoteAdd();
		cmd.setName(Constants.DEFAULT_REMOTE_NAME);
		cmd.setUri(new URIish(remote.getRepository().getDirectory().toURI().toURL()));
		cmd.call();
	}

	protected String readFile(Git git, ObjectId oid) throws IOException {
		ObjectLoader loader = git.getRepository().open(oid);

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();

		loader.copyTo(buffer);

		return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
	}

	protected String readFile(Git git, RevCommit commit, String path) throws IOException {
		try (TreeWalk tw = new TreeWalk(git.getRepository())) {
			tw.reset(commit.getTree());
			tw.setRecursive(true);
			tw.setFilter(PathFilter.create(path));

			assertTrue(tw.next());
			assertEquals(path, tw.getPathString());

			return readFile(git, tw.getObjectId(0));
		}
	}
}

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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.junit.Test;

/**
 * @author Markus Gärtner
 *
 */
public class GeneralGitTest extends RepositoryTest {

	@Test
	public void testInit() throws Exception {
		try(Git git = createLocalGit()) {
			assertNotNull(git.getRepository());
		}
	}

	@Test
	public void testCommit() throws Exception {
		try(Git git = createLocalGit()) {
			makeLocalFile("test", "abc");

			RevCommit commit = git.commit().setMessage("abc").call();

			assertEquals(commit, git.getRepository().resolve(Constants.HEAD));
		}
	}

	@Test
	public void testAccessCommittedFile() throws Exception {
		try(Git git = createLocalGit()) {
			File file = makeLocalFile("test", "abc");

			git.add().addFilepattern("test").call();
			RevCommit commit = git.commit().setMessage("abc").call();

			assertEquals(commit, git.getRepository().resolve(Constants.HEAD));

			Repository repo = git.getRepository();

			try(TreeWalk tw = new TreeWalk(repo)) {
				tw.reset(commit.getTree());
				tw.setRecursive(true);
				tw.setFilter(PathFilter.create("test"));

				assertTrue(tw.next());
				assertEquals(file.getName(), tw.getPathString());

				ObjectLoader loader = repo.open(tw.getObjectId(0));

				ByteArrayOutputStream buffer = new ByteArrayOutputStream();

				loader.copyTo(buffer);

				assertEquals("abc", new String(buffer.toByteArray(), StandardCharsets.UTF_8));
			}
		}
	}
}

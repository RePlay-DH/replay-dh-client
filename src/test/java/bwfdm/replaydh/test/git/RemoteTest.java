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

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.FetchResult;
import org.junit.Test;

/**
 * @author Markus Gärtner
 *
 */
public class RemoteTest extends RepositoryTest {

	private void printRefs(String label, Git git) throws GitAPIException {
		System.out.println("===== "+label+" =====");
		git.branchList().setListMode(ListMode.ALL).call().forEach(System.out::println);
	}

	@Test
	public void testFetchOfMultipleBranches() throws Exception {
		try(Git local = createLocalGit();
				Git other = createOtherGit();
				Git remote = createRemoteGit()) {

			addRemote(local, remote);
			addRemote(other, remote);

			printRefs("local blank", local);

			other.commit().setMessage("init").call();

			other.branchCreate().setName("b1").call();
			other.branchCreate().setName("b2").call();
			other.branchCreate().setName("sub/test").call();

			printRefs("other filled", other);

			other.push().setPushAll().call();

			System.out.println("===== remote available =====");
			local.lsRemote().setHeads(true).call().forEach(System.out::println);

			FetchResult fetchResult = local.fetch().call();

			System.out.println("===== tracking updates =====");
			fetchResult.getTrackingRefUpdates().forEach(System.out::println);

			printRefs("local fetched", local);
		}
	}
}

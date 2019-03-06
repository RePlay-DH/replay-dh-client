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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.RefUpdate.Result;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.merge.ResolveMerger;
import org.eclipse.jgit.merge.ThreeWayMerger;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.TrackingRefUpdate;
import org.junit.Test;

/**
 * @author Markus Gärtner
 *
 */
public class ConflictTest extends RepositoryTest {

	@Test
	public void testConflictingMergeDuringPull() throws Exception {
		try(Git local = createLocalGit();
				Git other = createOtherGit();
				Git remote = createRemoteGit()) {

			addRemote(local, remote);
			addRemote(other, remote);

			makeLocalFile("a", "a\nb\nc");
			local.add().addFilepattern("a").call();
			local.commit().setMessage("a1").call();

			makeOtherFile("a", "a\nbc\nc");
			other.add().addFilepattern("a").call();
			other.commit().setMessage("a").call();

			other.push().setPushAll().call();

			PullResult pullResult = local.pull().call();

			assertEquals(MergeStatus.CONFLICTING, pullResult.getMergeResult().getMergeStatus());

			assertTrue(readLocalFile("a").startsWith("a\n<<<<<<< HEAD"));
		}
	}

	@Test
	public void testConflictingFetchWithMergeDryRun() throws Exception {
		try(Git local = createLocalGit();
				Git other = createOtherGit();
				Git remote = createRemoteGit()) {

			addRemote(local, remote);
			addRemote(other, remote);

			makeLocalFile("a", "a\nb\nc");
			local.add().addFilepattern("a").call();
			local.commit().setMessage("a1").call();

			makeOtherFile("a", "a\nbc\nc");
			other.add().addFilepattern("a").call();
			other.commit().setMessage("a").call();

			other.push().setPushAll().call();

			FetchResult fr = local.fetch().call();

			TrackingRefUpdate refUpdate = fr.getTrackingRefUpdate("refs/remotes/origin/master");

			Repository repo = local.getRepository();

			ThreeWayMerger merger = MergeStrategy.RECURSIVE.newMerger(repo, true);
			ObjectId localHead = repo.resolve(Constants.HEAD);
			ObjectId fetchHead = refUpdate.getNewObjectId();

			assertEquals("a\nb\nc", readFile(local, repo.parseCommit(localHead), "a"));
			assertEquals("a\nbc\nc", readFile(local, repo.parseCommit(fetchHead), "a"));
			assertEquals("a\nb\nc", readLocalFile("a"));

			boolean noProblems = merger.merge(
					localHead, fetchHead);

			assertFalse(noProblems);
		}
	}

	@Test
	public void testDetectingConflictingFilesAndLinesDuringMergeDryRun() throws Exception {
		try(Git local = createLocalGit();
				Git other = createOtherGit();
				Git remote = createRemoteGit()) {

			addRemote(local, remote);
			addRemote(other, remote);

			makeLocalFile("a", "a\nb\nc");
			local.add().addFilepattern("a").call();
			local.commit().setMessage("a1").call();

			makeOtherFile("a", "a\nbc\nc");
			other.add().addFilepattern("a").call();
			other.commit().setMessage("a").call();

			other.push().setPushAll().call();

			FetchResult fr = local.fetch().call();

			TrackingRefUpdate refUpdate = fr.getTrackingRefUpdate("refs/remotes/origin/master");
			assertEquals(Result.NEW, refUpdate.getResult());

			Repository repo = local.getRepository();

			ThreeWayMerger merger = MergeStrategy.RECURSIVE.newMerger(repo, true);
			ObjectId localHead = repo.resolve(Constants.HEAD);
			ObjectId fetchHead = repo.resolve(Constants.FETCH_HEAD);

			assertEquals("a\nb\nc", readFile(local, repo.parseCommit(localHead), "a"));
			assertEquals("a\nbc\nc", readFile(local, repo.parseCommit(fetchHead), "a"));
			assertEquals("a\nb\nc", readLocalFile("a"));

			assertTrue(merger instanceof ResolveMerger);

			ResolveMerger resolveMerger = (ResolveMerger) merger;
			boolean noProblems = resolveMerger.merge(
					localHead, fetchHead);

			assertFalse(noProblems);
			// Make sure it really was a virtual merge and the local file remained unchanged
			assertEquals("a\nb\nc", readLocalFile("a"));

			System.out.println("Commit Names: "+Arrays.toString(resolveMerger.getCommitNames()));
			System.out.println("Modified Files: "+resolveMerger.getModifiedFiles());
			System.out.println("Unmerged Paths: "+resolveMerger.getUnmergedPaths());

			System.out.println("Failing Paths:");
			Optional.ofNullable(resolveMerger.getFailingPaths()).orElse(Collections.emptyMap())
				.forEach((path, reason) -> System.out.printf("    %s -> %s%n", path, reason));

			System.out.println("Newly Staged:");
			resolveMerger.getToBeCheckedOut()
				.forEach((path, entry) -> System.out.printf("    %s -> %s%n", path, entry.getObjectId()));

			System.out.println("Merge Results:");
			resolveMerger.getMergeResults()
				.forEach((path, result) -> System.out.printf("    %s -> conflict=%b chunks=%s%n",
						path, result.containsConflicts(),
						StreamSupport.stream(result.spliterator(), false)
							.map(chunk -> String.format("idx=%d begin=%d end=%d state=%s",
									chunk.getSequenceIndex(), chunk.getBegin(), chunk.getEnd(), chunk.getConflictState()))
							.collect(Collectors.toList())));
		}
	}
}

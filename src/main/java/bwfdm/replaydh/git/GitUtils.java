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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.function.Function;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.util.FS;

/**
 * @author Markus Gärtner
 *
 */
public class GitUtils {

	/**
	 * Default name of the git folder within our working directory
	 */
	static final String DEFAULT_GIT_DIR_NAME = Constants.DOT_GIT;

	/**
	 * Name of the default ignore file for the repository
	 */
	static final String DEFAULT_IGNORE_FILE_NAME = Constants.DOT_GIT_IGNORE;

	/**
	 * Default name of the configuration file that stores our
	 * schema and other internal information within the
	 * repository git-directory
	 */
	static final String RDH_CLIENT_INFO_FILENAME = "replaydh.info";

	/**
	 * General namespace prefix for all refs we generate through
	 * our client.
	 */
	static final String RDH_NAMESPACE = "rdh/";

	/**
	 * Marker prefix for all our branches and tags to show users
	 * that those were created by the RePlay-DH client.
	 */
	static final String RDH_PREFIX = "rdh_";

	/**
	 * Prefix to use for our branches that represent users trying out
	 * alternatives in their workflow
	 */
	static final String BRANCH_PREFIX = RDH_NAMESPACE+RDH_PREFIX+"alternative_";

	static final String BRANCH_NAMESPACE = Constants.R_HEADS+RDH_NAMESPACE;

	static String createNewBranchName(int counter) {
		return BRANCH_PREFIX+String.valueOf(counter);
	}

	static final String BRANCH_COUNTER_PREFIX = RDH_NAMESPACE+RDH_PREFIX+"counter/"+RDH_PREFIX+"count_";

	static int parseCounter(String name) {
		int idx = name.lastIndexOf('_');
		if(idx==-1)
			throw new IllegalArgumentException("Not a valid counter branch name: "+name);
		return Integer.parseInt(name.substring(idx+1));
	}

	/**
	 * Namespace for the sole branch that we use to keep track of
	 * the number of branches we created, so that we can keep a
	 * continuously increasing counter as branch suffix.
	 */
	static final String BRANCH_COUNTER_NAMESPACE = BRANCH_NAMESPACE+RDH_PREFIX+"counter/";

	/**
	 * Namespace for tags that we use to mark special commits for our
	 * workflow.
	 */
	static final String TAG_NAMESPACE = Constants.R_TAGS+RDH_NAMESPACE;

	static final String TAG_PREFIX = RDH_NAMESPACE+RDH_PREFIX;

	/**
	 * Whenever we reset to a previous commit as a result of changing the
	 * active workflow step in the graph, we need to make sure that the
	 * branch we were currently in is still fully reachable. We therefore
	 * tag the HEAD of a branch with a special "keep_alive" marker to
	 * prevent git from gc'ing the commits in case they are no longer
	 * reachable.
	 */
	static final String TAG_KEEP_ALIVE_SUFFIX = "_keep_alive";

	static final String TAG_KEEP_ALIVE_NAMESPACE = TAG_NAMESPACE+"ka/";

	/**
	 * The proxy user for special commits made by the adapter
	 */
	static final String JGIT_USER = "RePlay-DH JGit Adapter";

	/**
	 * The email for the {@link #JGIT_USER}.
	 */
	static final String JGIT_EMAIL = "";

	static PersonIdent createJGitUser() {
		return new PersonIdent(JGIT_USER, JGIT_EMAIL);
	}

	static final String INITIAL_COMMIT_HEADER = "Storing info for JGit adapter";

	/**
	 * Marker for the commit that denotes the artificial initial
	 * commit performed automatically be the client.
	 */
	static final String TAG_SOURCE = TAG_PREFIX+"source";

	/**
	 * Maps from system dependent file name to git convention
	 * which uses '/' as separator
	 */
	private static final Function<String, String> normalizer;

	/**
	 * Maps from git convention ('/' separator) to the system
	 * default.
	 */
	private static final Function<String, String> denormalizer;

	public static final char GIT_SEPARATOR_CHAR = '/';

	static {
		if(File.separatorChar==GIT_SEPARATOR_CHAR) {
			normalizer = denormalizer = s -> s;
		} else {
			normalizer = s -> {
				return s.replace(File.separatorChar, GIT_SEPARATOR_CHAR);
			};

			denormalizer = s -> {
				return s.replace(GIT_SEPARATOR_CHAR, File.separatorChar);
			};
		}
	}

	public static String systemToGitPath(String path) {
		return normalizer.apply(path);
	}

	public static String gitToSystemPath(String path) {
		return denormalizer.apply(path);
	}

	public static boolean isGitRepository(Path dir) {
		return FileKey.isGitRepository(dir.toFile(), FS.DETECTED);
	}

	public static boolean isRDHRepository(Path gitDir) {

		Path infoFile = gitDir.resolve(RDH_CLIENT_INFO_FILENAME);

		return Files.exists(infoFile, LinkOption.NOFOLLOW_LINKS);
	}
}

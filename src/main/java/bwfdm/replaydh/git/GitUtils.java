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
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.RemoteRemoveCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache.FileKey;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.TransportProtocol;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.Session;

import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.utils.Mutable.MutableObject;

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

	public static boolean isRdhBranch(String branch) {
		branch = Repository.shortenRefName(branch);
		return branch.startsWith(RDH_NAMESPACE);
	}

	public static boolean isRdhMarkerBranch(String branch) {
		branch = Repository.shortenRefName(branch);
		return branch.startsWith(BRANCH_COUNTER_PREFIX);
	}

	public static boolean isGitRepository(Path dir) {
		return FileKey.isGitRepository(dir.toFile(), FS.DETECTED);
	}

	public static boolean isGitRelatedFile(Path file) {
		return isGitIndex(file) || file.endsWith(DEFAULT_IGNORE_FILE_NAME);
	}

	public static boolean isGitIndex(Path file) {
		return file.endsWith(DEFAULT_GIT_DIR_NAME);
	}

	public static boolean isRDHRepository(Path gitDir) {

		Path infoFile = gitDir.resolve(RDH_CLIENT_INFO_FILENAME);

		return Files.exists(infoFile, LinkOption.NOFOLLOW_LINKS);
	}

	public static final String DUPLICATE_MARKER = ".rdh-other";

	public static boolean isSpecialFile(Path file) {
		return file.getFileName().toString().contains(DUPLICATE_MARKER);
	}

	/**
	 * Name of a special remote config that we use for temporary
	 * storage of push or fetch info.
	 */
	public static final String TEMP_RDH_REMOTE = "tmp-rdh-remote";

	public static boolean isTemporaryRemote(RemoteConfig rc) {
		return TEMP_RDH_REMOTE.equals(rc.getName());
	}

	public static void cleanupTempRemote(Git git) throws GitAPIException {
		if(git==null) {
			return;
		}
		RemoteRemoveCommand cmd = git.remoteRemove();
		cmd.setName(TEMP_RDH_REMOTE);
		cmd.call();
	}

	public static RemoteConfig createTempRemote(Git git, URIish uri) throws GitAPIException {
		RemoteAddCommand cmd = git.remoteAdd();
		cmd.setName(TEMP_RDH_REMOTE);
		cmd.setUri(uri);
		return cmd.call();
	}

	/**
	 * Figure out what {@link TransportProtocol} to use for the given remote {@code uri}.
	 *
	 * @param uri
	 * @param repo
	 * @param remoteName
	 * @return
	 * @throws GitException
	 */
	public static TransportProtocol getProtocol(
			URIish uri, Repository repo, String remoteName) throws GitException {
		return Transport.getTransportProtocols()
				.stream()
				.filter(p -> p.canHandle(uri, repo, remoteName))
				.findFirst()
				.orElseThrow(() -> new GitException("No transport protocol found for uri: "+uri));
	}

	/**
	 * Configures the given {@code command} with credentials befitting the
	 * protocol specified by the {@code uri} scheme.
	 * <p>
	 * If the configuration requires additional user credentials, this method
	 * will present the user with a corresponding dialog and after successful
	 * elicitation of the credentials data call the {@code credentialsFactory}
	 * callback. This way client code is in control over constructing the
	 * actual {@link CredentialsProvider} instance and also can keep that
	 * reference for later cleanup work.
	 * <p>
	 * The method returns {@code true} if configuration was successful and
	 * {@code false} if at any point involving user interactions the user
	 * decided to abort.
	 *
	 * @param command
	 * @param uri
	 * @param remoteName
	 * @param credentialsFactory
	 * @return
	 * @throws GitException
	 */
	public static boolean prepareTransportCredentials(
			TransportCommand<?, ?> command,
			URIish uri, String remoteName,
			BiFunction<String, char[], CredentialsProvider> credentialsFactory) throws GitException {
		TransportProtocol protocol = getProtocol(uri, command.getRepository(), remoteName);

		Set<String> schemes = protocol.getSchemes();

		//TODO we need a better recognition strategy than checking the scheme set...

		// Regular http or https access
		if(schemes.contains("https")) {
			return GuiUtils.showCredentialsDialog(null,
					ResourceManager.getInstance().get("replaydh.dialogs.credentials.loginCredentialsTitle"),
					ResourceManager.getInstance().get("replaydh.dialogs.credentials.loginCredentialsMessage"),
					(username, password) -> command.setCredentialsProvider(
							credentialsFactory.apply(username, password)));
		}

		// ssh key with optional password
		if(schemes.contains("ssh")) {

			// Ask user for optional password
			MutableObject<char[]> password = new MutableObject<>();
			if(!GuiUtils.showPasswordDialog(null,
					ResourceManager.getInstance().get("replaydh.dialogs.credentials.sshPasswordTitle"),
					ResourceManager.getInstance().get("replaydh.dialogs.credentials.sshPasswordMessage"),
					password::set)) {
				return false;
			}

			// Configure SSH session to use password if needed
			SshSessionFactory sshSessionFactory = new JschConfigSessionFactory() {
				@Override
				protected void configure(Host host, Session session) {
				    // If available, do set the user password for the SSH key
					password.asOptional().ifPresent(chars -> session.setPassword(new String(chars)));
				}
			};

			// Inject callback to configure transport
			command.setTransportConfigCallback(new TransportConfigCallback() {
				@Override
				public void configure(Transport transport) {
					SshTransport sshTransport = (SshTransport) transport;
					sshTransport.setSshSessionFactory(sshSessionFactory);
				}
			});

			return true;
		}

		// For local or anonymous git repositories we don't need any credentials configuration
		if(schemes.contains("git") || schemes.contains("file")) {
			return true;
		}

		throw new GitException("Protocol not supported: "+protocol.getName());
	}
}

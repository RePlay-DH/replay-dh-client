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

import static bwfdm.replaydh.git.GitUtils.gitToSystemPath;
import static bwfdm.replaydh.git.GitUtils.systemToGitPath;
import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.RmCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.ignore.IgnoreNode;
import org.eclipse.jgit.ignore.IgnoreNode.MatchResult;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.RefDatabase;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.core.AbstractRDHTool;
import bwfdm.replaydh.core.RDHClient;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.core.RDHTool;
import bwfdm.replaydh.core.Workspace;
import bwfdm.replaydh.io.FileTracker;
import bwfdm.replaydh.io.TrackerException;
import bwfdm.replaydh.io.TrackerListener;
import bwfdm.replaydh.io.TrackingAction;
import bwfdm.replaydh.io.TrackingStatus;
import bwfdm.replaydh.json.JsonWorkflowStepReader;
import bwfdm.replaydh.json.JsonWorkflowStepWriter;
import bwfdm.replaydh.utils.LazyCollection;
import bwfdm.replaydh.utils.Options;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowException;
import bwfdm.replaydh.workflow.WorkflowStep;
import bwfdm.replaydh.workflow.WorkflowUtils;
import bwfdm.replaydh.workflow.impl.DefaultWorkflow;
import bwfdm.replaydh.workflow.impl.Node;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public class JGitAdapter extends AbstractRDHTool implements RDHTool, FileTracker/*, PropertyChangeListener*/ {

	private static final Logger log = LoggerFactory.getLogger(JGitAdapter.class);

	private static final JGitAdapterVersion VERSION = JGitAdapterVersion.VERSION_1;

	public static final String NAME_WORKFLOW = "workflow";

	/**
	 * If set in the environment's {@link RDHEnvironment#getProperty(String) properties}
	 * this will denote the location where all git repos should be placed.
	 *
	 * Note that JGit allows working directory and index to be located at different areas
	 * of a file system.
	 */
	public static final String CENTRAL_GIT_FOLDER_PROPERTY = ""; //TODO provide proper name here

	private static final String NODE_PROPERTY_COMMIT_ID = "commitID";

	private static final String NODE_PROPERTY_BRANCH_ID = "branch";

	/**
	 *
	 */
	private static final long DEFAULT_STATUS_INFO_EXPIRATION_TIME_MILLIS = 1000;

	private static final Properties EMPTY_CONFIG = new Properties();
	static {
		EMPTY_CONFIG.setProperty(RDHInfoProperty.TITLE, "<no title>");
		EMPTY_CONFIG.setProperty(RDHInfoProperty.DESCRIPTION, "<no description>");
	}

	/**
	 * Link to git porcelain API in JGit
	 */
	private Git git;

	/**
	 * The single workflow representing the content of our git working directory.
	 */
	private DelegatingWorkflow workflow;

	/**
	 * Our access to lower level git content/functionality.
	 * This walk is used through the entire life-cycle of the
	 * adapter from {@link #connectGitAndStoreWorkspace(Workspace) creating}
	 * or {@link #connectGitAndLoadWorkspace(Path) loading} a repository
	 * to {@link #stop(RDHEnvironment) stopping} the adapter.
	 */
	private RevWalk revWalk;

	/**
	 * Maps commit ids (hashes) to their associated workflow step instances.
	 */
	private Map<String, WorkflowStep> commitToStepLookup = new HashMap<>();

	/**
	 * Flag to indicate if the workflow (skeleton) has been loaded
	 */
	private volatile boolean workflowLoaded = false;

	/**
	 * The latest step added to the workflow.
	 * <p>
	 * Note that we can have at most one pending step per transaction!
	 */
	private volatile WorkflowStep pendingStep = null;

	private volatile Status lastStatus = null;
	private volatile LocalDateTime lastStatusUpdateTime = null;

	/**
	 * Lock to synchronize all git operations on.
	 * <p>
	 * Package-private so that the {@link GitArchiveExporter}
	 * can use it for interaction with git.
	 */
	private final Object gitLock = new Object();

	private final List<TrackerListener> trackerListeners = new CopyOnWriteArrayList<>();

	/**
	 * Package-private so that the {@link GitArchiveExporter}
	 * can use it for interaction with git.
	 */
	Git getGit() {
		return git;
	}

	/**
	 * Package-private so that the {@link GitArchiveExporter}
	 * can use it for interaction with git.
	 */
	Object getGitLock() {
		return git;
	}

	/**
	 * @see bwfdm.replaydh.core.AbstractRDHTool#start(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public boolean start(RDHEnvironment environment) throws RDHLifecycleException {
		if(!super.start(environment)) {
			return false;
		}

		return true;
	}

	/**
	 * @see bwfdm.replaydh.core.AbstractRDHTool#stop(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public void stop(RDHEnvironment environment) throws RDHLifecycleException {
		disconnectGit();

		super.stop(environment);
	}

	/**
	 * Connects to the git associated with the specified workspace folder
	 * and loads the configuration and schema into a new {@link Workspace}
	 * object.
	 *
	 * @throws IOException if accessing any file resource fails
	 * @throws GitException if the specified git repository is not valid of if
	 * any of the stored configuration data is corrupted or incompatible
	 * @param workspacePath
	 * @return
	 */
	public Workspace connectGitAndLoadWorkspace(Path workspacePath) throws IOException, GitException {
		synchronized (gitLock) {
			Path gitDir = getGitDir(workspacePath);

			// Special precaution against repeatedly getting called to connect to repository
			if(isCurrentGit(gitDir)) {
				return getEnvironment().getWorkspace();
			}

			disconnectGit();

			final Git existingGit = openOrCreate(gitDir.toFile(), false);

			final Path infoFile = getInfoFile(gitDir);

			// Try to read our configuration
			final Properties config = loadConfig(infoFile);

			// Sanity check against tampering with config
			checkConfig(config);

			// Convert config into proper workspace base (also verifies integrity)
			final Workspace workspace = createWorkspace(workspacePath, config);

			verifyGit(existingGit, workspace, false);

			// Finally start adjusting fields (nothing from here on should fail)
			initInternals(existingGit, workspace, config);

			// Fields 'pendingStep' and 'workflowLoaded' should still be in their initial states

			return workspace;
		}
	}

	private void checkConfig(Properties config) {
		//TODO verify stored adapter version etc...
	}

	private void initInternals(Git newGit, Workspace workspace, Properties config) {
		git = newGit;
		revWalk = new RevWalk(newGit.getRepository());
		// Start with an initially empty workflow
		workflow = new DelegatingWorkflow(workspace.getSchema());
		workflow.setTitle(config.getProperty(RDHInfoProperty.TITLE));
		workflow.setDescription(config.getProperty(RDHInfoProperty.DESCRIPTION));

		String nextStepId = config.getProperty(RDHInfoProperty.NEXT_STEP_ID);
		if(nextStepId!=null) {
			workflow.setNextStepNumber(Integer.parseInt(nextStepId));
		}

		getPropertyChangeSupport().firePropertyChange(NAME_WORKFLOW, null, workflow);
	}

	/**
	 * Creates a new git repository at the specified location and
	 * stores the workspace configuration.
	 *
	 * @param workspace
	 * @throws GitException
	 * @throws IOException
	 */
	public void connectGitAndStoreWorkspace(Workspace workspace) throws IOException, GitException {
		synchronized (gitLock) {
			Path gitDir = getGitDir(workspace.getFolder());

			if(isCurrentGit(gitDir)) {
				return;
			}

			disconnectGit();

			// Try to open or create git repo
			final Git newGit = openOrCreate(gitDir.toFile(), true);

			verifyGit(newGit, workspace, true);

			final Properties config = createDefaultConfig(workspace);
			saveConfig(getInfoFile(gitDir), config);

			initInternals(newGit, workspace, config);
		}
	}

	/**
	 * Returns the file location we should use for reporting the
	 * given repository to the user.
	 * <p>
	 * Currently we report the .git metadata folder, but in the future
	 * we might want to change that to the working directory?
	 */
	private File reportLocation(Repository repo) {
		return repo.getDirectory();
	}

	private void verifyGit(final Git git, final Workspace workspace, boolean createSource)
			throws IOException, GitException {

		final Repository repo = git.getRepository();

		try(RevWalk revWalk = new RevWalk(repo)) {

			// Snapshot of current info situation
			final Properties currentInfo = createSourceCommitInfo(workspace);

			// Try to fetch our marker tag
			final Ref sourceRef = git.getRepository().findRef(GitUtils.TAG_SOURCE);

			// If commit already exists, do sanity checks
			if(sourceRef!=null) {
				// Fetch actual commit target
				ObjectId id = getTarget(sourceRef, repo);
				RevCommit commit = repo.parseCommit(id);

				// Sanity check against somebody tampering with our tag
				if(commit.getParentCount()>0)
					throw new GitException(String.format(
							"Git repo corrupted - %s does not point to initial commit, but %s in repo %s",
							GitUtils.TAG_SOURCE, commit, reportLocation(repo)));

				// Get  info from commit
				final Properties storedInfo = new Properties();
				try(StringReader reader = new StringReader(commit.getFullMessage())) {
					storedInfo.load(reader);
				}

				// Sanity check against outdated repo
				if(!storedInfo.equals(currentInfo))
					throw new GitException(String.format(
							"Git repo corrutped - stored info expected to be %s, but was %s in commit %s in repo %s",
							currentInfo, storedInfo, commit, reportLocation(repo)));
			} else if(createSource) {
				// Check if repo is truly empty
				Map<String, Ref> refs = git.getRepository().getRefDatabase().getRefs(RefDatabase.ALL);
				if(!refs.isEmpty())
					throw new GitException(String.format(
							"Repository %s is not empty", reportLocation(repo)));

				// Artificial proxy user denoting the JGit adapter itself
				final PersonIdent proxyUser = GitUtils.createJGitUser();

				// The serialized info to be used as git commit message
				final String message = serializeInfo(currentInfo);

				// If required, include an empty .gitignore file in the initial commit
				ensureGitignoreFile(git);

				RevCommit commit;

				// Make our initial commit and store the info
				try {
					commit = git.commit()
							.setMessage(message)
							.setReflogComment("initial RePlay-DH marker commit")
							.setAllowEmpty(true)
							.setNoVerify(true)
							.setCommitter(proxyUser)
							.call();

					log.info("Created initial commit {} in repo {}", commit, reportLocation(repo));
				} catch (GitAPIException e) {
					throw new GitException(String.format(
							"Failed to create initial commit for repo %s",
							reportLocation(repo)), e);
				}

				// Finally tag the commit with our marker
				try {
					Ref tag = git.tag()
							.setName(GitUtils.TAG_SOURCE)
							.setTagger(proxyUser)
							.setObjectId(commit)
							.call();

					log.info("Created marker tag {} for repo {}", tag, reportLocation(repo));
				} catch (GitAPIException e) {
					throw new GitException(String.format(
							"Failed to create marker tag {} for commit {} in repo {}",
							GitUtils.TAG_SOURCE, commit, reportLocation(repo)), e);
				}
			} else
				// No marked commit and not allowed to create one -> complain
				throw new GitException(String.format(
						"Marker tag %s not found in repo",
						GitUtils.TAG_SOURCE, reportLocation(repo)));
		}
	}

	/**
	 * Wrap workspace info and current API and adapter versions into a
	 * {@link Properties} object for storage in the initial commit.
	 */
	private static Properties createSourceCommitInfo(Workspace workspace) {
		Properties properties = new Properties(EMPTY_CONFIG);

		properties.setProperty(RDHInfoProperty.ADAPTER_VERSION, VERSION.label);
		properties.setProperty(RDHInfoProperty.API_VERSION, Workflow.API_VERSION.label);
		properties.setProperty(RDHInfoProperty.SCHEMA_ID, workspace.getSchema().getId());
		copyProperty(properties, workspace, RDHInfoProperty.TITLE);
		copyProperty(properties, workspace, RDHInfoProperty.DESCRIPTION);

		return properties;
	}

	private static void copyProperty(Properties target, Workspace source, String key) {
		String value = source.getProperty(key);
		if(value!=null) {
			target.setProperty(key, value);
		}
	}

	private static void copyProperty(Workspace target, Properties source, String key) {
		String value = source.getProperty(key);
		if(value!=null) {
			target.setProperty(key, value);
		}
	}

	/**
	 * Unpeel a ref if needed and then obtain its target {@link ObjectId}.
	 */
	private static ObjectId getTarget(Ref ref, Repository repo) {
		if(!ref.isPeeled())
			ref = repo.peel(ref);
		ObjectId id = ref.getPeeledObjectId();
		if(id==null)
			id = ref.getObjectId();
		return id;
	}

	/**
	 * Transform the given {@link Properties} into a String object.
	 */
	private static String serializeInfo(Properties info) {
		try(StringWriter tmp = new StringWriter()) {
			info.store(tmp, GitUtils.INITIAL_COMMIT_HEADER);
			return tmp.toString();
		} catch (IOException e) {
			throw new RDHException("Unexpected error from StringWriter", e);
		}
	}

	/**
	 * From given {@code config} obtain property for {@code key}, potentially
	 * applying {@code defaultValue}. If no entry for key is found and no default
	 * value is defined, throw {@link GitException} with info of the config file
	 * missing an entry.
	 * If non-null value was obtained, unmap key in the config and return value.
	 */
	private static String getAndRemoveProperty(Path workspacePath, Properties config, String key, String defaultValue) throws GitException {
		String value = config.getProperty(key, defaultValue);
		if(value == null)
			throw new GitException(String.format("Missing property '%s' in config file: %s",
					key, workspacePath));

		config.remove(key);

		return value;
	}

	/**
	 * Read config and create a new {@link Workspace} instance.
	 * <p>
	 * Needs valid schema id stored in config, otherwise {@link GitException}
	 * will be thrown.
	 */
	private Workspace createWorkspace(Path workspacePath, Properties config) throws GitException {

		String schemaId = getAndRemoveProperty(workspacePath, config, RDHInfoProperty.SCHEMA_ID, null);

		final RDHClient client = getEnvironment().getClient();

		WorkflowSchema schema = client.getSchemaManager().lookupSchema(schemaId);
		if(schema==null)
			throw new GitException("Unknown schema id: "+schemaId);

		Workspace workspace = client.createBasicWorkspace(workspacePath, schema);

		copyProperty(workspace, config, RDHInfoProperty.TITLE);
		copyProperty(workspace, config, RDHInfoProperty.DESCRIPTION);

		//TODO process rest of config and store unknown configuration information in the workspace object

		return workspace;
	}

	/**
	 * Returns the location of the git folder for the supplied
	 * workspace. Usually this is the hidden {@code .git} directory
	 * within the same folder, but depending on the client configuration
	 * this can also be a folder in some centralized repository
	 * directory.
	 *
	 * @param workspacePath
	 * @return
	 */
	private Path getGitDir(Path workspacePath) {
		// Assume default git directory name and try to access repo at that location
		return workspacePath.resolve(GitUtils.DEFAULT_GIT_DIR_NAME);
	}

	/**
	 * Tries to open a git repository at the specified location or creates a
	 * new repository there if none could be found.
	 *
	 * @param gitDir
	 * @return
	 * @throws IOException if accessing the existing git repository failed
	 * @throws GitAPIException if creating a new git repository failed
	 * @throws GitException if an existing repository was expected but not found
	 */
	private Git openOrCreate(File gitDir, boolean mayCreateNew) throws IOException, GitException {
		Git git;
		FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();

		// Try to find existing gitDir at specified location
		repositoryBuilder.addCeilingDirectory(gitDir);
		repositoryBuilder.findGitDir(gitDir);

		boolean repoExists = repositoryBuilder.getGitDir() != null;

		if(repoExists) {
			// Use existing repo
			log.info("Using existing git repository "+repositoryBuilder.getGitDir());
			git = new Git(repositoryBuilder.build());
		} else if(mayCreateNew) {
			// Create new repo if allowed to
			log.info("Creating new git repository in folder "+gitDir.getParent());
			try {
				git = Git.init().setDirectory(gitDir.getParentFile()).call();
			} catch (IllegalStateException | GitAPIException e) {
				throw new GitException("Failed to init git repository at location: "+gitDir, e);
			}
		} else
			throw new GitException("No git repository at provided location: "+gitDir);


		// Always check validity of underlying git
		if(!isValidGit(git))
			throw new GitException("Failed to create a valid repository at location: "+gitDir);

		return git;
	}

	/**
	 * Checks whether the given {@code git} repository exists and is valid.
	 * This check only verifies that the repository is not {@link Repository#isBare() bare}.
	 *
	 * @param git
	 * @return
	 */
	private boolean isValidGit(Git git) {
		// Git is considered valid if its repository is not bare
		return git!=null && !git.getRepository().isBare();
	}

	/**
	 * Returns {@code true} if given {@link Path} points to current
	 * metadata folder of repository.
	 */
	private boolean isCurrentGit(Path newGitDir) {
		return git!=null
				&& git.getRepository().getDirectory()!=null
				&& newGitDir.equals(git.getRepository().getDirectory().toPath());
	}

	/**
	 * Returns the location of the internal info file within
	 * the specified git repository.
	 *
	 * @param gitDir
	 * @return
	 */
	private static Path getInfoFile(Path gitDir) {
		return gitDir.resolve(GitUtils.RDH_CLIENT_INFO_FILENAME);
	}

	/**
	 * Wraps information for given workspace into {@link Properties}
	 * object.
	 * This includes information regarding API and adapter version and
	 * id of schema used for workspace.
	 */
	private static Properties createDefaultConfig(Workspace workspace) {
		Properties properties = new Properties(EMPTY_CONFIG);

		// Store default values based on supplied workspace
		properties.setProperty(RDHInfoProperty.API_VERSION, Workflow.API_VERSION.label);
		properties.setProperty(RDHInfoProperty.SCHEMA_ID, workspace.getSchema().getId());
		copyProperty(properties, workspace, RDHInfoProperty.TITLE);
		copyProperty(properties, workspace, RDHInfoProperty.DESCRIPTION);

		// Store other defaults
		properties.setProperty(RDHInfoProperty.ADAPTER_VERSION, VERSION.label);

		return properties;
	}

	/**
	 * Read new config from given file.
	 */
	private Properties loadConfig(Path configFile) throws IOException {
		Properties properties = new Properties(EMPTY_CONFIG);

		try(Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}

		return properties;
	}

	private static final String INFO_FILE_COMMENT = "Automatically created file - do not modify!";

	/**
	 * Store given config to provided file location.
	 */
	private void saveConfig(Path configPath, Properties config) throws IOException {
		try(Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
			config.store(writer, INFO_FILE_COMMENT);
		}
	}

	/**
	 * Attempts to close the given git if it is not {@code null}.
	 * Note that {@link Git#close()} delegates to {@link Repository#close()}
	 * which just decrements the repository's use counter and when it reaches
	 * {@code 0} as a result, only then it gets actually closed. So in theory
	 * we have no real control over the final release of resources but since
	 * we do not plan on revealing the {@code git} instance to outside code
	 * we can be pretty sure about this being the only code that deals with
	 * closing the repository.
	 *
	 * @param git
	 */
	private static void close(Git git) {
		if(git!=null) {
			log.info("Closing connection to git repository");
			git.close();
		}
	}

	/**
	 * Close git if available.
	 * Dispose current revWalk if present.
	 * Close current workflow if present.
	 * <p>
	 * Fires {@value #NAME_WORKFLOW} if a workflow was present
	 * and has been closed a result of this method call.
	 *
	 */
	public void disconnectGit() {
		synchronized (gitLock) {
			log.info("Disconnecting from git: {}", git==null ? "<none>" : reportLocation(git.getRepository()));

			try {
				// Update the info file
				// (currently we only do this to store the updated step number)
				if(git!=null && workflow!=null && workflow.getNextStepNumber()!=1) {
					int nextStepId = workflow.getNextStepNumber();

					Path gitDir = git.getRepository().getDirectory().toPath();
					Path infoFile = getInfoFile(gitDir);
					Properties config = loadConfig(infoFile);

					config.setProperty(RDHInfoProperty.NEXT_STEP_ID, String.valueOf(nextStepId));

					saveConfig(infoFile, config);
				}
			} catch(IOException e) {
				log.error("Failed to update git info file", e);
			} finally {
				// Now proceed to shutdown all resources
				close(git);
				git = null;
				if(revWalk!=null) {
					revWalk.dispose();
					revWalk = null;
				}
				if(workflow!=null) {
					workflow.close();
					Workflow oldValue = workflow;
					workflow = null;
					getPropertyChangeSupport().firePropertyChange(NAME_WORKFLOW, oldValue, null);
				}

				pendingStep = null;
				workflowLoaded = false;
				clearStatusInfo();
			}
		}
	}

	/**
	 * The workflow graph will be backed by this git such that each transaction
	 * (c.f. {@link Workflow#beginUpdate()} and {@link Workflow#endUpdate()}) will
	 * trigger synchronization with the git via the appropriate git commands being
	 * invoked and the (modified) process metadata being stored as payload in each
	 * commit's text message.
	 *
	 * @return
	 */
	public Workflow getWorkflow() {
		return workflow;
	}

	/**
	 * @see bwfdm.replaydh.io.FileTracker#getTrackedFolder()
	 */
	@Override
	public Path getTrackedFolder() {
		synchronized (gitLock) {
			if(git==null) {
				return null;
			} else {
				return getRootFolder(git);
			}
		}
	}

	/**
	 * Returns the absolute path to the specified git repository.
	 */
	private Path getRootFolder(Git git) {
		return git.getRepository().getWorkTree().toPath().toAbsolutePath();
	}

	/**
	 * Returns location of the repository specific default file that contains
	 * git rules for ignoring files.
	 */
	private Path getGitignoreFile(Git git) {
		return getRootFolder(git).resolve(GitUtils.DEFAULT_IGNORE_FILE_NAME);
	}

	/**
	 * Checks whether the default .gitignore file is already present in the
	 * repository, creating it if necessary.
	 * @return {@code true} iff the .gitignore file had to be created.
	 * @throws IOException in case any of the underlying file operations fail
	 * @throws GitException
	 */
	private void ensureGitignoreFile(Git git) throws IOException, GitException {
		Path ignoreFile = getGitignoreFile(git);
		boolean shouldCreate = Files.notExists(ignoreFile, LinkOption.NOFOLLOW_LINKS);

		if(shouldCreate) {
			Files.createFile(ignoreFile);
			try(Writer writer = Files.newBufferedWriter(ignoreFile, StandardCharsets.UTF_8))  {
				writer.write("########################################################");
				writer.write("# This file contains ignore rules for the underlying   #");
				writer.write("# Git repository. Entries are managed by the RePlay-DH #");
				writer.write("# client automatically depending on user actions via   #");
				writer.write("# the graphical interface.                             #");
				writer.write("#                                                      #");
				writer.write("#   Do NOT modify this file manually unless you know   #");
				writer.write("#              EXACTLY what you're doing!              #");
				writer.write("########################################################");
			}
		}

		final Repository repo = git.getRepository();

		try {
			Path root = getRootFolder(git);
			git.add()
				.addFilepattern(toRelativeGitPath(ignoreFile, root))
				.call();

			log.info("Created default ignore file {} in repo {}", ignoreFile, reportLocation(repo));
		} catch(GitAPIException e) {
			throw new GitException(String.format(
					"Failed to create default ignore file %s for repo %s",
					ignoreFile, reportLocation(repo)), e);
		}
	}

	/**
	 * @see bwfdm.replaydh.io.FileTracker#addTrackerListener(bwfdm.replaydh.io.TrackerListener)
	 */
	@Override
	public void addTrackerListener(TrackerListener listener) {
		trackerListeners.add(listener);
	}

	/**
	 * @see bwfdm.replaydh.io.FileTracker#removeTrackerListener(bwfdm.replaydh.io.TrackerListener)
	 */
	@Override
	public void removeTrackerListener(TrackerListener listener) {
		trackerListeners.remove(listener);
	}

	@Override
	public boolean hasStatusInfo() {
		return lastStatus!=null;
	}

	protected void fireRefreshStarted() {
		if(trackerListeners.isEmpty()) {
			return;
		}

		for(TrackerListener listener : trackerListeners) {
			listener.refreshStarted(this);
		}
	}

	protected void fireRefreshFailed(Exception e) {
		if(trackerListeners.isEmpty()) {
			return;
		}

		for(TrackerListener listener : trackerListeners) {
			listener.refreshFailed(this, e);
		}
	}

	protected void fireRefreshDone(boolean canceled) {
		if(trackerListeners.isEmpty()) {
			return;
		}

		for(TrackerListener listener : trackerListeners) {
			listener.refreshDone(this, canceled);
		}
	}

	protected void fireTrackingStatusChanged(Set<Path> files, TrackingAction action) {
		if(trackerListeners.isEmpty()) {
			return;
		}

		for(TrackerListener listener : trackerListeners) {
			listener.trackingStatusChanged(this, files, action);
		}
	}

	protected void fireStatusInfoChanged() {
		if(trackerListeners.isEmpty()) {
			return;
		}

		for(TrackerListener listener : trackerListeners) {
			listener.statusInfoChanged(this);
		}
	}

	private boolean refreshStatusInfo0() {

		if(isVerbose()) {
			log.trace("Refreshing status info for git repo: {}", git.getRepository().getWorkTree());
		}

		synchronized (gitLock) {
			lastStatus = null;

			fireRefreshStarted();

			// Signal cleared status
			fireStatusInfoChanged();

			ExecutionResult<Status> status = executeCommand(git.status());
			if(!status.hasFailed()) {
				if(isVerbose()) {
					log.info("Updated git status");
				}

				lastStatus = status.result;
				lastStatusUpdateTime = LocalDateTime.now();

				fireRefreshDone(false);

				// Only if we succeeded should listeners be informed
				fireStatusInfoChanged();
			} else if(isVerbose()) {
				log.info("Failed to update git status", status.exception);
				fireRefreshFailed(status.exception);
			}

			return hasStatusInfo();
		}
	}

	private boolean maybeRefreshStatusInfo(long cacheExpirationTimeMillis) {
		LocalDateTime lastUpdate = lastStatusUpdateTime;
		if(lastUpdate==null || lastUpdate.plusNanos(
				cacheExpirationTimeMillis).compareTo(LocalDateTime.now())>0) {
			return refreshStatusInfo0();
		}

		return hasStatusInfo();
	}

	/**
	 * Attempts to refresh the internal {@link Status git status} cache
	 * and returns {@code true} if successful.
	 *
	 * @return
	 */
	@Override
	public boolean refreshStatusInfo() {
		return refreshStatusInfo0();
	}

	/**
	 * @see bwfdm.replaydh.io.FileTracker#getLastUpdateTime()
	 */
	@Override
	public LocalDateTime getLastUpdateTime() {
		return lastStatusUpdateTime;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see bwfdm.replaydh.io.FileTracker#clearStatusInfo()
	 */
	@Override
	public void clearStatusInfo() {
		synchronized (gitLock) {
			if(lastStatus!=null) {
				lastStatus = null;
				fireStatusInfoChanged();
			}
		}
	}

	@Override
	public Set<Path> getFilesForStatus(TrackingStatus status) throws TrackerException {
		LazyCollection<Path> result = LazyCollection.lazySet();
		synchronized (gitLock) {
			Status gitStatus = lastStatus;

			if(gitStatus!=null) {
				final Path root = getRootFolder(git);
				final Consumer<String> resolver = path -> {
					result.add(root.resolve(gitToSystemPath(path)));
				};

				collectFilesForStatus(gitStatus, status, resolver);
			}
		}

		return result.getAsSet();
	}

	/**
	 * Need to be called under {@code gitLock} lock!
	 * @param gitStatus
	 * @param status
	 * @param action
	 * @throws TrackerException
	 */
	private void collectFilesForStatus(Status gitStatus, TrackingStatus status, Consumer<? super String> action)
			throws TrackerException {

		switch (status) {
		case IGNORED:
			gitStatus.getIgnoredNotInIndex().forEach(action);
			break;

		case UNKNOWN:
			gitStatus.getUntracked().forEach(action);
			break;

		case MISSING:
			gitStatus.getMissing().forEach(action);
			break;

		case MODIFIED:
			gitStatus.getModified().forEach(action);
			gitStatus.getChanged().forEach(action);
			break;

		case TRACKED: {
			try(TreeWalk treeWalk = prepareTreeWalk(Constants.HEAD)) {
				while(treeWalk.next()) {
					if(treeWalk.isSubtree()) {
						treeWalk.enterSubtree();
					} else {
						action.accept(treeWalk.getPathString());
					}
				}
			} catch(IOException e) {
				throw new TrackerException("Failed to access JGit TreeWalk to count tracked files", e);
			}
		} break;

		default:
			throw new TrackerException("Unknown or unsupported tracking status: "+status);
		}
	}

	/**
	 * Needs to be called under {@code gitLock} lock!
	 * @param str
	 * @return
	 * @throws IOException
	 */
	private TreeWalk prepareTreeWalk(String str) throws IOException {
		RevCommit commit = resolve(str);
		RevTree tree = commit.getTree();
		if(tree==null) {
			return null;
		}

		TreeWalk treeWalk = new TreeWalk(git.getRepository());
		treeWalk.addTree(tree);
		treeWalk.setRecursive(false);

		return treeWalk;
	}

	/**
	 * @throws TrackerException
	 * @see bwfdm.replaydh.io.FileTracker#hasFilesForStatus(bwfdm.replaydh.io.TrackingStatus)
	 */
	@Override
	public boolean hasFilesForStatus(TrackingStatus...statuses) throws TrackerException {
		if(!hasStatusInfo()) {
			return false;
		}

		EnumSet<TrackingStatus> ss = EnumSet.noneOf(TrackingStatus.class);
		Collections.addAll(ss, statuses);

		boolean hasFiles = false;

		synchronized (gitLock) {
			Status gitStatus = lastStatus;
			if(gitStatus!=null) {

				for(TrackingStatus status : ss) {
					switch (status) {
					case IGNORED: hasFiles |= !gitStatus.getIgnoredNotInIndex().isEmpty(); break;
					case UNKNOWN: hasFiles |= !gitStatus.getUntracked().isEmpty(); break;
					case MISSING: hasFiles |= !gitStatus.getMissing().isEmpty(); break;
					case MODIFIED: hasFiles |= !gitStatus.getModified().isEmpty() || !gitStatus.getChanged().isEmpty(); break;
					case CORRUPTED: hasFiles |= !gitStatus.getConflicting().isEmpty(); break;

					case TRACKED: {
						try(TreeWalk treeWalk = prepareTreeWalk(Constants.HEAD)) {
							walk : while(treeWalk.next()) {
								if(treeWalk.isSubtree()) {
									treeWalk.enterSubtree();
								} else {
									hasFiles = true;
									break walk;
								}
							}
						} catch(IOException e) {
							throw new TrackerException("Failed to access JGit TreeWalk to check for tracked files", e);
						}
					} break;

					default:
						throw new TrackerException("Unknown or unsupported tracking status: "+status);
					}

					if(hasFiles) {
						break;
					}
				}
			}

		}

		return hasFiles;
	}

	/**
	 * @throws TrackerException
	 * @see bwfdm.replaydh.io.FileTracker#getFileCountForStatus(bwfdm.replaydh.io.TrackingStatus[])
	 */
	@Override
	public int getFileCountForStatus(TrackingStatus...statuses) throws TrackerException {
		if(!hasStatusInfo()) {
			return -1;
		}

		EnumSet<TrackingStatus> ss = EnumSet.noneOf(TrackingStatus.class);
		Collections.addAll(ss, statuses);

		int fileCount = 0;

		synchronized (gitLock) {
			Status gitStatus = lastStatus;
			if(gitStatus!=null) {

				for(TrackingStatus status : ss) {
					switch (status) {
					case IGNORED: fileCount += gitStatus.getIgnoredNotInIndex().size(); break;
					case UNKNOWN: fileCount += gitStatus.getUntracked().size(); break;
					case MISSING: fileCount += gitStatus.getMissing().size(); break;
					case MODIFIED: fileCount += gitStatus.getModified().size() + gitStatus.getChanged().size(); break;
					case CORRUPTED: fileCount += gitStatus.getConflicting().size(); break;

					case TRACKED: {
						try(TreeWalk treeWalk = prepareTreeWalk(Constants.HEAD)) {
							int count = 0;
							while(treeWalk.next()) {
								if(treeWalk.isSubtree()) {
									treeWalk.enterSubtree();
								} else {
									count++;
								}
							}
							fileCount += count;
						} catch(IOException e) {
							throw new TrackerException("Failed to access JGit TreeWalk to count tracked files", e);
						}
					} break;

					default:
						throw new TrackerException("Unknown or unsupported tracking status: "+status);
					}
				}
			}

		}

		return fileCount;
	}

	@Override
	public Map<Path, TrackingStatus> getFilesForStatus(Set<TrackingStatus> statuses) throws TrackerException {
		Map<Path, TrackingStatus> result = new HashMap<>();
		synchronized (gitLock) {
			Status gitStatus = lastStatus;

			if(gitStatus!=null) {
				final Path root = getRootFolder(git);

				for(TrackingStatus status : statuses) {
					final Consumer<String> resolver = path -> {
						result.put(root.resolve(gitToSystemPath(path)), status);
					};

					collectFilesForStatus(gitStatus, status, resolver);
				}

			}
		}

		return result;
	}

	@Override
	public Set<Path> applyTrackingAction(Set<Path> files, TrackingAction action) throws TrackerException {
		requireNonNull(files);
		requireNonNull(action);
		checkArgument("Set of files is empty", !files.isEmpty());

		synchronized (gitLock) {
			switch (action) {
			case ADD:
				return startTrackingFiles(files);

			case REMOVE:
				return stopTrackingFiles(files, true);

			case IGNORE:
				return ignoreFiles(files);

			case NONE:
				return resetFileTracking(files);

			default:
				throw new TrackerException("Unknown or unsupported tracking action: "+action);
			}
		}
	}

	/**
	 * Converts given {@link Path files} into Strings usable for git commands
	 * and calls the given {@link Consumer gitPathAction}. For any file that
	 * resides outside the repository, the {@code leftoverAction} is performed.
	 * <p>
	 * Needs to be called under {@code gitLock} lock!
	 *
	 * @param files
	 * @param gitPathAction
	 * @param leftoverAction
	 */
	private void prepareFiles(Set<Path> files, BiConsumer<? super Path, ? super String> gitPathAction,
			Consumer<? super Path> leftoverAction) {

		final Path root = getRootFolder(git);

		for(Path file : files) {
			String gitPath = toRelativeGitPath(file, root);
			if(gitPath==null) {
				leftoverAction.accept(file);
			} else {
				gitPathAction.accept(file, gitPath);
			}
		}
	}

	private String toRelativeGitPath(Path file, Path root) {
		Path absoluteFile = file.toAbsolutePath();
		if(!absoluteFile.startsWith(root)) {
			return null;
		} else {
			// Only use the "relative" path portion, starting from working directory
			Path relativeFile = root.relativize(absoluteFile);
			return systemToGitPath(relativeFile.toString());
		}

	}

	private Set<Path> resetFileTracking(Set<Path> files) {
		//TODO implement: remove tracking or revoke ignore status for the given files
		throw new UnsupportedOperationException();
	}

	private Set<Path> startTrackingFiles(Set<Path> files) {
		requireNonNull(files);
		checkArgument("Set of files is empty", !files.isEmpty());

		LazyCollection<Path> leftovers = LazyCollection.lazySet(files.size());

		AddCommand command = git.add();

		// Collect all file paths that are valid for our workspace
		prepareFiles(files, (file, gitPath) -> command.addFilepattern(gitPath), leftovers);

		if(command!=null) {
			ExecutionResult<DirCache> result = executeCommand(command);
			if(result.hasFailed()) {
				// In case of failure we consider all files to be leftovers
				leftovers.addAll(files);
			}
		}

		return leftovers.getAsSet();
	}

	private Set<Path> stopTrackingFiles(Set<Path> files, boolean delete) {
		requireNonNull(files);
		checkArgument("Set of files is empty", !files.isEmpty());

		LazyCollection<Path> leftovers = LazyCollection.lazySet(files.size());

		RmCommand command = git.rm();

		// If required also force deletion on the working directory
		command.setCached(!delete);

		// Collect all file paths that are valid for our workspace
		prepareFiles(files, (file, gitPath) -> command.addFilepattern(gitPath), leftovers);


		if(command!=null) {
			ExecutionResult<DirCache> result = executeCommand(command);
			if(result.hasFailed()) {
				// In case of failure we consider all files to be leftovers
				leftovers.addAll(files);
			}
		}

		return leftovers.getAsSet();
	}

	/**
	 * Needs to be called under {@code gitLock} lock!
	 * @param files
	 * @return
	 * @throws TrackerException
	 */
	private Set<Path> ignoreFiles(Set<Path> files) throws TrackerException {
		requireNonNull(files);
		checkArgument("Set of files is empty", !files.isEmpty());

		// Need to check current status to know if files are added, already ignored or new/unknown
		EnumMap<TrackingStatus, Set<Path>> statusBreakdown = getStatusBreakdown(files);

		// If files are already ignored, just don't bother with them
		files.removeAll(statusBreakdown.get(TrackingStatus.IGNORED));

		// Rare case, but worth exiting here, if we have nothing left to ignore
		if(files.isEmpty()) {
			return Collections.emptySet();
		}

		/*
		 *  We need to remove from index all files that are currently
		 *  either missing, modified or simply under version control already.
		 *  Therefore collect all files for those three categories and
		 *  delegate to deletion commend.
		 */
		Set<Path> filesToRemove = getFilesFromStatusBreakdown(statusBreakdown,
				TrackingStatus.TRACKED,
				TrackingStatus.MISSING,
				TrackingStatus.MODIFIED);

		// Remove all already tracked files from version control
		if(!filesToRemove.isEmpty()) {
			// Make sure to not delete the working copies (that's left to the user if desired!)
			stopTrackingFiles(filesToRemove, false);
		}

		// From here on we'll only interact with the ignore file

		// We're assuming one global ignore file that only our client is manipulating
		IgnoreNode ignoreNode;
		try {
			ignoreNode = getDefaultIgnoreRules();
		} catch (IOException e) {
			throw new TrackerException("Failed to load ignore rules", e);
		}
		final Path root = getRootFolder(git);
		// Files that failed to be converted into local git paths or couldn't be added to ignore file
		LazyCollection<Path> leftovers = LazyCollection.lazySet(files.size());
		// Properly transformed local git paths that haven't been ignored previously
		List<String> pathsToIgnore = new ArrayList<>();

		// Transform all paths into proper git syntax and remove the ones that are already ignored
		for(Path file : files) {
			String path = toRelativeGitPath(file, root);
			if(path==null) {
				leftovers.add(file);
				continue;
			}

			boolean isDirectory = Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS);

			if((ignoreNode.isIgnored(path, isDirectory))==MatchResult.NOT_IGNORED) {
				if(isDirectory && !path.endsWith("/")) {
					path = path+"/";
				}

				pathsToIgnore.add(path);
			}
		}

		// Now append new entries to .gitignore file
		if(!pathsToIgnore.isEmpty()) {
			try {
				appendIgnoreRules(pathsToIgnore);
			} catch (IOException e) {
				log.error("Failed to append ignore rules for files : {}", pathsToIgnore, e);
				leftovers.addAll(files);
			}
		}

		// Finally report files that failed to be ignored
		return leftovers.getAsSet();
	}

	private Set<Path> getFilesFromStatusBreakdown(Map<TrackingStatus, Set<Path>> statusBreakdown, TrackingStatus...statuses) {
		LazyCollection<Path> result = LazyCollection.lazySet();

		for(TrackingStatus status : statuses) {
			result.addAll(statusBreakdown.get(status));
		}

		return result.getAsSet();
	}

	/**
	 * Needs to be called under {@code gitLock} lock!
	 * @param pathsToIgnore
	 * @throws IOException
	 */
	private void appendIgnoreRules(List<String> pathsToIgnore) throws IOException {
		checkArgument("List of paths to ignroe must not be empty", !pathsToIgnore.isEmpty());

		Collections.sort(pathsToIgnore);

		Path ignoreFile = getGitignoreFile(git);

		try(Writer writer = Files.newBufferedWriter(ignoreFile, Constants.CHARSET,
				StandardOpenOption.WRITE,
				StandardOpenOption.CREATE,
				StandardOpenOption.APPEND)) {

			writer.append(System.lineSeparator());
			writer.append("# ignored on "+LocalDateTime.now());
			writer.append(System.lineSeparator());

			for(String path : pathsToIgnore) {
				writer.append(path);
				writer.append(System.lineSeparator());
			}
		}
	}

	private IgnoreNode getDefaultIgnoreRules() throws IOException {
		synchronized (gitLock) {
			Path ignoreFile = getGitignoreFile(git);
			IgnoreNode ignoreNode = new IgnoreNode();
			try(InputStream in = Files.newInputStream(ignoreFile)) {
				ignoreNode.parse(in);
			}

			return ignoreNode;
		}
	}

	private void rollbackTrackingChanges() {
		//TODO facility to revert changes made since last successful commit?
	}

	private Status ensureUpdatedStatus() throws TrackerException {
		Status status;
		if(!maybeRefreshStatusInfo(DEFAULT_STATUS_INFO_EXPIRATION_TIME_MILLIS) || ((status=lastStatus)==null))
			throw new TrackerException("No status info available to determine tracking status");
		return status;
	}

	private static TrackingStatus lookupFileStatus(Status status, String path) {
		checkArgument("Path must not be null", path!=null);

		if(status.getUntracked().contains(path)) {
			return TrackingStatus.UNKNOWN;
		} else if(status.getIgnoredNotInIndex().contains(path)) {
			return TrackingStatus.IGNORED;
		} else if(status.getMissing().contains(path)) {
			return TrackingStatus.MISSING;
		} else if(status.getChanged().contains(path)
				|| status.getModified().contains(path)) {
			return TrackingStatus.MODIFIED;
		} else if(status.getConflicting().contains(path)) {
			return TrackingStatus.CORRUPTED;
		} else {
			return TrackingStatus.TRACKED;
		}
	}

	/**
	 * @throws TrackerException
	 * @see bwfdm.replaydh.io.FileTracker#getStatusForFile(java.nio.file.Path)
	 */
	@Override
	public TrackingStatus getStatusForFile(Path file) throws TrackerException {
		synchronized (gitLock) {
			final Status status = ensureUpdatedStatus();
			final Path root = getRootFolder(git);
			final String path = systemToGitPath(root.relativize(file).toString());

			return lookupFileStatus(status, path);
		}
	}

	/**
	 * @throws TrackerException
	 * @see bwfdm.replaydh.io.FileTracker#getSatusForFiles(java.util.Set)
	 */
	@Override
	public Map<Path, TrackingStatus> getSatusForFiles(Set<Path> files) throws TrackerException {
		final Map<Path, TrackingStatus> result = new HashMap<>(files.size());

		synchronized (gitLock) {

			final Status status = ensureUpdatedStatus();
			final Path root = getRootFolder(git);

			for(Path file : files) {
				String path = toRelativeGitPath(file, root);

				result.put(file, lookupFileStatus(status, path));
			}
		}

		return result;
	}

	/**
	 * @throws TrackerException
	 * @see bwfdm.replaydh.io.FileTracker#getStatusBreakdown(java.util.Set)
	 */
	@Override
	public EnumMap<TrackingStatus, Set<Path>> getStatusBreakdown(Set<Path> files) throws TrackerException {
		final EnumMap<TrackingStatus, Set<Path>> result = new EnumMap<>(TrackingStatus.class);

		synchronized (gitLock) {
			final Status status = ensureUpdatedStatus();
			final Path root = getRootFolder(git);

			for(Path file : files) {
				String path = toRelativeGitPath(file, root);
				TrackingStatus s = lookupFileStatus(status, path);

				Set<Path> buffer = result.get(s);
				if(buffer==null) {
					buffer = new HashSet<>();
					result.put(s, buffer);
				}
				buffer.add(file);
			}
		}

		for(TrackingStatus s : TrackingStatus.values()) {
			result.putIfAbsent(s, Collections.emptySet());
		}

		return result;
	}

	/**
	 * {@link Callable#call() executes} the given {@link GitCommand} and wraps it result in
	 * a nullable {@link Optional}. A return value of {@code null} indicates a failure in
	 * executing the commend.
	 * <p>
	 * This method is the central place to
	 *
	 * @param command
	 * @return
	 */
	private <O extends Object> ExecutionResult<O> executeCommand(GitCommand<O> command) {
		try {
			O result = command.call();
			return new ExecutionResult<O>(command, result);
		} catch(GitAPIException e) {
			log.error("Failed to execute git command {}", command.getClass().getSimpleName(), e);
			return new ExecutionResult<>(command, e);
		}
	}



	/**
	 * Saves the given {@code step} as the one to be associated with
	 * the next git commit action.
	 *
	 * @throws WorkflowException in case there's already been a pending step set
	 */
	private void setPendingStep(WorkflowStep step) {
		requireNonNull(step);

		synchronized (gitLock) {
			if(pendingStep!=null)
				throw new WorkflowException("Pending workflow step already set. At most one pending step supported per transaction!");

			pendingStep = step;
		}
	}

	public WorkflowStep getPendingStep() {
		synchronized (gitLock) {
			return pendingStep;
		}
	}

	private Ref createNewBranch(RevCommit startPoint) throws IOException, GitException {
		synchronized (gitLock) {
			Ref counterBranch = getSpecialRef(GitUtils.BRANCH_COUNTER_NAMESPACE);

			// Get counter for new branch
			int counter = 1;
			if(counterBranch!=null) {
				counter = GitUtils.parseCounter(counterBranch.getName())+1;
			}

			final String newBranchName = GitUtils.createNewBranchName(counter);
			final String newCounterBranchName = GitUtils.BRANCH_COUNTER_PREFIX+String.valueOf(counter);

			// Update the counter branch
			if(counterBranch==null) {
				try {
					counterBranch = git.branchCreate()
							.setName(newCounterBranchName)
							.setStartPoint(startPoint)
							.call();
					log.info("Created counter branch {}", counterBranch);
				} catch (GitAPIException e) {
					throw new GitException("Failed to create counter branch: "+newCounterBranchName, e);
				}
			} else {
				try {
					counterBranch = git.branchRename()
							.setOldName(counterBranch.getName())
							.setNewName(newCounterBranchName)
							.call();
					log.info("Updating counter branch to {}", counterBranch);
				} catch (GitAPIException e) {
					throw new GitException("Failed to rename counter branch into: "+newCounterBranchName, e);
				}
			}

			// Finally create our new branch
			Ref newBranch;

			try {
				newBranch = git.branchCreate()
						.setName(newBranchName)
						.setStartPoint(startPoint)
						.call();
				log.info("Created new branch {}", newBranch);
			} catch (GitAPIException e) {
				throw new GitException("Failed to create new branch "+newBranchName, e);
			}

			// The ref to our new branch after successfully switching
			Ref result;

			// Switch to new branch
			try {
				// Should never fail since the branch has no content that could be conflicting
				result = git.checkout().setName(newBranch.getName()).call();
				log.info("Switched to new branch {}", result);
			} catch (GitAPIException e) {
				throw new GitException("Failed to switch to new branch "+newBranch, e);
			}

			// Apply stash to new branch if anything is stashed
			try {
				if(!git.stashList().call().isEmpty()) {
					ObjectId id = git.stashApply().call();
					log.info("Applied stashed commit {}", id);
				}
			} catch (GitAPIException e) {
				throw new GitException("Failed to apply stashed changes", e);
			}

			return result;
		}
	}

	private void deleteBranch(Ref branch) throws GitException {
		synchronized (gitLock) {
			List<String> names;
			try {
				names = git.branchDelete().setBranchNames(branch.getName()).call();
			} catch (GitAPIException e) {
				throw new GitException("Failed to delete branch "+branch, e);
			}
			for(String name : names) {
				log.info("Deleted branch {}", name);
			}
		}
	}

	/**
	 * If no step has been assigned as 'pending' this method simply returns.
	 * <p>
	 * Otherwise executes a git commit action and links the resulting commit id
	 * with the currently pending workflow step.
	 * Uses the serialized {@code JSON} form of the step as commit message.
	 * @throws IOException
	 * @throws GitException
	 */
	private void commitPendingStep() throws IOException, GitException {
		synchronized (gitLock) {
			checkState("No active workflow", workflow!=null);

			WorkflowStep step = pendingStep;
			if(step!=null) {
				try {

					// Serialize step and create metadata payload
					String message = createCommitMessage(step);

					// New branch if we need to create one
					Ref newBranch = null;
					final WorkflowStep previous = WorkflowUtils.previous(pendingStep);
					// If preceding step already has outgoing actions, we need to branch
					if(workflow.getNextStepCount(previous)>0) {
						newBranch = createNewBranch(head());
					}

					// We assume all files to have been added to the index already
					CommitCommand command = git.commit()
							.setMessage(message)
							.setAll(true);

					ExecutionResult<RevCommit> result = executeCommand(command);
					if(result.hasFailed()) {
						// Make sure we clean up "temporary" branch in case commit failed
						if(newBranch!=null) {
							deleteBranch(newBranch);
						}
						throw new GitException("Failed to commit pending step: "+pendingStep.getId(), result.exception);
					} else {
						saveId(step, result.result);

						// Tell listeners that "something" has changed with the step
						workflow.fireWorkflowStepChanged(step);
					}

					// Make sure other entities get informed of the changes
					clearStatusInfo();
				} finally {
					pendingStep = null;
				}
			}
		}
	}

	private void storeNodeProperty(WorkflowStep step, String property, String value) {
		workflow.node(step, true).setProperty(property, value);
	}

	private void saveId(WorkflowStep step, RevCommit commit) {
		// Save SHA-1 of commit id as lowercase hexadecimal string
		storeNodeProperty(step, NODE_PROPERTY_COMMIT_ID, commit.name());

		commitToStepLookup.put(commit.name(), step);
	}

	/**
	 * Package-private so that the {@link GitArchiveExporter} can use it
	 * for commit lookup.
	 */
	RevCommit loadId(WorkflowStep step) throws IOException {
		Node<WorkflowStep> node = workflow.node(step);
		String id = (String)node.getProperty(NODE_PROPERTY_COMMIT_ID);
		if(id==null) {
			return null;
		}
		ObjectId objectId = ObjectId.fromString(id);
		return revWalk.parseCommit(objectId);
	}

	private String getLabel(WorkflowStep step) {
		Node<WorkflowStep> node = workflow.node(step);
		String id = (String)node.getProperty(NODE_PROPERTY_COMMIT_ID);
		if(id==null) {
			id = "Unnamed Step";
		}
		return id;
	}

	private Ref getTag(ObjectId id, String prefix) throws IOException {
		Repository repository = git.getRepository();
		Map<String, Ref> tags = repository.getRefDatabase().getRefs(prefix);
		for(Ref tag : tags.values()) {
			ObjectId objectId = getTarget(tag, repository);

			if(id.equals(objectId)) {
				return tag;
			}
		}

		return null;
	}

	private Ref getSpecialRef(String prefix) throws IOException, GitException {
		Repository repository = git.getRepository();
		Map<String, Ref> refs = repository.getRefDatabase().getRefs(prefix);
		if(refs.isEmpty()) {
			return null;
		}
		if(refs.size()>1)
			throw new GitException("More than 1 ref for prefix "+prefix+": "+refs);

		return refs.values().iterator().next();
	}

	private void ensureReachable(RevCommit commit) throws GitException, IOException {
		synchronized (gitLock) {

			// Grab step and progress till a leaf is found
			final WorkflowStep step = lookupStep(commit);
			if(step==null)
				throw new GitException("Inconsistent workflow graph - missing step for commit "+commit);
			final WorkflowStep leaf = WorkflowUtils.nextUntil(step, WorkflowUtils::isLeaf);

			/*
			 *  If step is null here we must have reached a branching point,
			 *  which means that at an earlier point we already ensured
			 *  reachability of those branches!
			 */
			if(leaf==null) {
				return;
			}

			// Load commit for that leaf step
			commit = loadId(leaf);

			// Try to resolve a tag
			Ref ref = getTag(commit, GitUtils.TAG_KEEP_ALIVE_NAMESPACE);

			// If branch is already secured, simply exit
			if(ref!=null) {
				return;
			}


		}
	}

	private void checkout(WorkflowStep step) throws IOException, GitException {
		synchronized (gitLock) {
			// Fetch the commit the target step is pointing to
			RevCommit commit = loadId(step);
			// Fetch the current head
			RevCommit head = head();

			/*
			 *  Exit early in case we are already at the right commit.
			 *  Usually this only happens during the initial phase of
			 *  constructing a new workflow from the git commit graph.
			 */
			if(commit.equals(head)) {
				return;
			}

			//TODO check if we have a clean working dir state and ask user to clean it?

//			ensureReachable(head);

			CheckoutCommand cmd = git.checkout().setName(commit.name());
			ExecutionResult<Ref> result = executeCommand(cmd);
			if(result.hasFailed()) {
				throw new GitException("Failed to checkout existing commit "+commit, result.exception);
			}

			// Sanity check to make sure HEAD is switched correctly
			RevCommit newHead = head();
			if(!commit.equals(newHead)) {
				log.error("Failed to properly checkout commit {} - ended up at {}", commit, newHead);
			}
		}
	}

	private RevCommit head() throws IOException {
		return resolve(Constants.HEAD);
	}

	private WorkflowStep lookupStep(RevCommit commit) {
		return commitToStepLookup.get(commit.name());
	}

	private void resetStepLookup() {
		commitToStepLookup.clear();
	}

	private boolean hasSavedId(WorkflowStep step) {
		Node<WorkflowStep> node = workflow.node(step);
		return node.getProperty(NODE_PROPERTY_COMMIT_ID)!=null;
	}

	private static final String DEFAULT_MESSAGE = "Generic unnamed step";

	/**
	 * Serializes the given {@code step} into JSON and uses the
	 * {@link WorkflowStep#getDescription() description} of it
	 * as header field. If no description has been set for the
	 * step a {@link #DEFAULT_MESSAGE default message} will be used.
	 */
	private static String createCommitMessage(WorkflowStep step) {

		// Use title of workflow step as header
		String title = step.getTitle();
		if(title!=null && !title.isEmpty()) {

			// Prevent initial line-break from resulting in empty line
			title = title.trim();

			// Restrict to first line only
			int lineBreakIndex = title.indexOf('\n');
			if(lineBreakIndex>-1) {
				title = title.substring(0, lineBreakIndex).trim();
			}
		}

		// Final check to ensure we didn't get an empty string from above pre-processing
		if(title==null || title.isEmpty()) {
			title = DEFAULT_MESSAGE;
		}

		String id = step.getId();
		if(id!=null) {
			title = id+": "+title;
		}

		// The writeStep() method will ensure not to start the header with a '{' symbol
		Options options = new Options();
		options.put(JsonWorkflowStepWriter.HEADER, title);
		options.put(JsonWorkflowStepWriter.PRETTY, true);
		return JsonWorkflowStepWriter.writeStep(step, options);
	}

//	private Map<RevCommit, String> getBranchPointers() throws IOException {
//		synchronized (gitLock) {
//			Map<RevCommit, String> result = new HashMap<>();
//
//			Map<String, Ref> refs = git.getRepository().getRefDatabase().getRefs(ALL);
//			for (Entry<String, Ref> entry : refs.entrySet()) {
//				ObjectId objectId = getTarget(entry.getValue(), git.getRepository());
//				RevCommit commit = null;
//				try {
//					commit = revWalk.parseCommit(objectId);
//				} catch (MissingObjectException e) {
//					// ignore: the ref points to an object that does not exist;
//				} catch (IncorrectObjectTypeException e) {
//					// ignore: the ref points to an object that is not a commit
//					// (e.g. a tree or a blob);
//				}
//				if (commit != null)
//					result.put(commit, entry.getKey());
//			}
//
//			return result;
//		}
//	}

	/**
	 * Load the entire commit history of the underlying git and create the skeleton workflow steps.
	 * @throws GitException if accessing any git related resources failed
	 */
	public void loadWorkflow() throws GitException {
		synchronized (gitLock) {
			// Avoid loading the git twice
			if(workflowLoaded) {
				return;
			}

			workflow.setIgnoreEventRequests(true);
			final Repository repo = git.getRepository();

			try(RevWalk revWalk = new RevWalk(repo)) {

				Collection<Ref> allRefs = repo.getRefDatabase().getRefs(Constants.R_HEADS).values();
                for( Ref ref : allRefs ) {
                    revWalk.markStart(revWalk.parseCommit(getTarget(ref, repo)));
                }

//				Iterable<RevCommit> commits = git.log().all().call();

				buildWorkflowGraph(revWalk, Constants.HEAD);

				// Finally switch flag so we don't ever attempt to load the workflow a second time
				workflowLoaded = true;

			} catch (IOException e) {
				throw new GitException("General I/O issue when trying to access log of git repository", e);
			} finally {

				workflow.setIgnoreEventRequests(false);
			}
		}

		// Notify listeners
		workflow.fireStateChanged();
	}

	private void buildWorkflowGraph(Iterable<RevCommit> source, String active)
				throws GitException, IOException {

		resetStepLookup();

		/*
		 *  The 'source' will usually be an instance of RevWalk
		 *  and cannot be used more than once for iterating.
		 *  So we kinda have to duplicate the storage for our
		 *  2-phase strategy.
		 */
		final Set<RevCommit> commits = new HashSet<>();
		source.forEach(commits::add);

		// Treat our initial commit special
		final RevCommit initialCommit = resolve(GitUtils.TAG_SOURCE);

//		// Lookup to see which commits start a new branch
//		final Map<RevCommit, String> branchPointers = getBranchPointers();

		// Phase 1: create mapping for all commits
		for(RevCommit commit : commits) {

			// Sanity check against duplicate step creation (should normally never happen)
			if(lookupStep(commit)!=null) {
				continue;
			}

			WorkflowStep step;

			if(commit.equals(initialCommit)) {
				step = workflow.getInitialStepDirect();
			} else {
				step = workflow.createWorkflowStep();
			}

			/*
			 *  We only save the commit id here and leave subsequent loading to
			 *  a later time. The workflow implementation will make sure to load
			 *  the process metadata stored inside a commit's message prior to the
			 *  associated workflow step being accessed.
			 */
			saveId(step, commit);

//			// Mark step as beginning of a new branch
//			String branch = branchPointers.get(commit);
//			if(branch!=null) {
//				step.setProperty(WorkflowStep.PROPERTY_INTERNAL_INFO, "Branch: "+branch);
//				storeNodeProperty(step, NODE_PROPERTY_BRANCH_ID, branch);
//			}
		}

		// Phase 2: actually add the steps and links to workflow
		for(RevCommit commit : commits) {
			WorkflowStep step = lookupStep(commit);

			if(workflow.isInitialStep(step)) {
				continue;
			}

			// Only if we have an actual "child" commit do we need to create a new step+node
			if(commit.getParentCount()>0) {
				// Establish links to parent steps
				for(RevCommit parentCommit : commit.getParents()) {
					WorkflowStep parentStep = lookupStep(parentCommit);

					// Our initial sorting should make sure that this check never fails
					if(parentStep==null)
						throw new GitException(String.format(
								"Inconsistent rev revWalk - node for parent '%s' of commit '%s' is missing",
								parentCommit.name(), commit.name()));

					// Introduce the actual graph structure via links between steps
					workflow.addWorkflowStepDirect(parentStep, step);
				}
			} else {
				// Commits without parents are considered to be linked to the virtual root step
				workflow.addWorkflowStepDirect(workflow.getInitialStep(), step);
			}
		}


		// Fetch and assign the "active" step in the workflow
		RevCommit activeCommit = resolve(active);
		if(activeCommit!=null) {
			WorkflowStep activeStep = lookupStep(activeCommit);
			if(activeStep==null)
				throw new GitException("Inconsistent repo data - no active workflow step for object-id: "+active);

			workflow.setActiveStepDirect(activeStep);
		}
	}

	/**
	 * Returns the RevCommit for the supplied id or {@code null}
	 * if that id cannot be resolved.
	 *
	 * @return
	 * @throws GitException if parsing the head commit failed
	 */
	private RevCommit resolve(String str) throws IOException {
		ObjectId id = git.getRepository().resolve(str);
		return id==null ? null : revWalk.parseCommit(id);
	}

	/**
	 *
	 * @param step
	 * @throws GitException if loading the git commit data for the specified step failed
	 */
	private void loadWorkflowStep(final WorkflowStep step) throws GitException {
		synchronized (gitLock) {
			workflow.setIgnoreEventRequests(true);
			try {
				RevCommit commit = null;

				try {
					commit = loadId(step);
				} catch (IOException e) {
					throw new GitException("Failed to resolve commit id", e);
				}

				// A pending workflow step won't have any commit assigned to it
				if(commit==null) {
					return;
				}

				String message = commit.getFullMessage();

				if(workflow.getInitialStepDirect()==step) {
					step.setTitle(GitUtils.INITIAL_COMMIT_HEADER);
					step.setDescription(message);
					step.setRecordingTime(LocalDateTime.ofInstant(
							Instant.ofEpochSecond(commit.getCommitTime()),
							ZoneId.systemDefault()));
				} else {
					Options options = new Options();
					options.put(JsonWorkflowStepReader.SKIP_HEADER, true);

					try {
						JsonWorkflowStepReader.parseStep(workflow.getSchema(), () -> step, message, options);
					} catch (Exception e) {
						if(getEnvironment().getBoolean(RDHProperty.GIT_IGNORE_MISSING_METADATA, false)) {
							// If we're prevented from throwing an exception, at least log it for future info
							log.warn("Failed to read process metadata from commit for step {}", step.getId(), e);
						} else
							throw new GitException("Failed to read process metadata from commit message for "+commit, e);
					}

					getEnvironment().getClient().getResourceResolver().update(step);
				}

			} finally {
				workflow.setIgnoreEventRequests(false);
			}
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 * @param <T> result of the {@link GitCommand command's} {@link GitCommand#call()} method
	 */
	private static class ExecutionResult<T extends Object> {

		ExecutionResult(GitCommand<T> command, T result) {
			this.command = requireNonNull(command);
			this.result = result;
			exception = null;
		}

		ExecutionResult(GitCommand<T> command, Exception exception) {
			this.command = requireNonNull(command);
			result = null;
			this.exception = exception;
		}

		@SuppressWarnings("unused")
		public final GitCommand<T> command;
		public final T result;
		public final Exception exception;

		public boolean hasFailed() {
			return exception!=null;
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	private class DelegatingWorkflow extends DefaultWorkflow {

		private final AtomicBoolean skeletonLoaded = new AtomicBoolean(false);

		private boolean ignoreEventRequests = false;

		DelegatingWorkflow(WorkflowSchema schema) {
			super(schema);
		}

		void setIgnoreEventRequests(boolean value) {
			ignoreEventRequests = value;
		}

		/**
		 * @see bwfdm.replaydh.workflow.impl.DefaultWorkflow#fireWorkflowStepAdded(bwfdm.replaydh.workflow.WorkflowStep)
		 */
		@Override
		public void fireWorkflowStepAdded(WorkflowStep step) {
			if(!ignoreEventRequests)
				super.fireWorkflowStepAdded(step);
		}

		/**
		 * @see bwfdm.replaydh.workflow.impl.DefaultWorkflow#fireWorkflowStepChanged(bwfdm.replaydh.workflow.WorkflowStep)
		 */
		@Override
		public void fireWorkflowStepChanged(WorkflowStep step) {
			if(!ignoreEventRequests)
				super.fireWorkflowStepChanged(step);
		}

		/**
		 * @see bwfdm.replaydh.workflow.impl.DefaultWorkflow#fireWorkflowStepPropertyChanged(bwfdm.replaydh.workflow.WorkflowStep, java.lang.String)
		 */
		@Override
		public void fireWorkflowStepPropertyChanged(WorkflowStep step, String propertyName) {
			if(!ignoreEventRequests)
				super.fireWorkflowStepPropertyChanged(step, propertyName);
		}

		/**
		 * Shortcut method for internal use by {@link JGitAdapter} only.
		 * <p>
		 * Exposes a proxy to {@link #addWorkflowStepImpl(WorkflowStep, WorkflowStep)}
		 * for package private access.
		 */
		boolean addWorkflowStepDirect(WorkflowStep source, WorkflowStep target) {
			return super.addWorkflowStepImpl(source, target);
		}

		/**
		 * @see bwfdm.replaydh.workflow.impl.DefaultWorkflow#fireWorkflowStepRemoved(bwfdm.replaydh.workflow.WorkflowStep)
		 */
		@Override
		public void fireWorkflowStepRemoved(WorkflowStep step) {
			if(!ignoreEventRequests)
				super.fireWorkflowStepRemoved(step);
		}

		/**
		 * Shortcut method for internal use by {@link JGitAdapter} only.
		 * Delegates to {@link #setActiveStepImpl(WorkflowStep)}
		 *
		 * @param step
		 */
		void setActiveStepDirect(WorkflowStep step) {
			setActiveStepImpl(step);
		}

		/**
		 * Shortcut method for internal use by {@link JGitAdapter} only.
		 * Exposes a proxy to {@link #getInitialStepUnchecked()}
		 * for package private access.
		 */
		WorkflowStep getInitialStepDirect() {
			return getInitialStepUnchecked();
		}

		/**
		 * @see bwfdm.replaydh.workflow.impl.DefaultWorkflow#setActiveStepImpl(bwfdm.replaydh.workflow.WorkflowStep)
		 */
		@Override
		protected boolean setActiveStepImpl(WorkflowStep step) {

			// Fail early in case the new step can't be set
			if(step==getActiveStep()) {
				return false;
			}

			// Perform Git level actions
			try {
				checkout(step);
			} catch (IOException | GitException e) {
				log.error("Failed to checkout state of step: {}", step.getId(), e);
				return false;
			}

			// Only at the end do we perform the change on model level
			return super.setActiveStepImpl(step);
		}

		/**
		 * We manage the assignment of new active step in the {@link #endTransaction()}
		 * method, so prevent super class from automatically changing it during regular
		 * method calls.
		 *
		 * @see bwfdm.replaydh.workflow.impl.DefaultWorkflow#autoAssignActiveStepOnAdd()
		 */
		@Override
		protected boolean autoAssignActiveStepOnAdd() {
			return false;
		}

		/**
		 * Calls the super method and then delegates to the {@link JGitAdapter} to
		 * commit pending changes.
		 *
		 * @see bwfdm.replaydh.workflow.impl.DefaultWorkflow#endTransaction()
		 */
		@Override
		protected void endTransaction() {

			final WorkflowStep pendingStep = getPendingStep();

			// If transaction got cancelled, we have nothing to do here
			if(pendingStep!=null) {
				boolean success = false;

				// Perform the git action(s)
				try {
					commitPendingStep();

					success = true;
				} catch (IOException | GitException e) {
					throw new RDHException("Failed to end transaction", e);
				} finally {
					// If we failed on the git level, make sure to revert addition of the step
					if(!success) {
						deleteWorkflowStepImpl(pendingStep);
					}
				}

				// Now finally perform the adjustment of the workflow's active step
				final WorkflowStep oldActiveStep = getActiveStep();

				setActiveStepImpl(pendingStep);

				fireActiveWorkflowStepChanged(oldActiveStep, pendingStep);
			}
		}

		/**
		 * @see bwfdm.replaydh.workflow.impl.DefaultWorkflow#addWorkflowStep(bwfdm.replaydh.workflow.WorkflowStep, bwfdm.replaydh.workflow.WorkflowStep)
		 */
		@Override
		public boolean addWorkflowStep(WorkflowStep source, WorkflowStep target) {
			throw new UnsupportedOperationException();
		}

		/**
		 * @see bwfdm.replaydh.workflow.impl.DefaultWorkflow#addWorkflowStepImpl(bwfdm.replaydh.workflow.WorkflowStep, bwfdm.replaydh.workflow.WorkflowStep)
		 */
		@Override
		protected boolean addWorkflowStepImpl(WorkflowStep source, WorkflowStep target) {

			setPendingStep(target);

			return super.addWorkflowStepImpl(source, target);
		}

		/**
		 * @see bwfdm.replaydh.workflow.impl.DefaultWorkflow#ensureFullWorkflowData()
		 */
		@Override
		protected void ensureFullWorkflowData() {
			if(skeletonLoaded.compareAndSet(false, true)) {
				// Load the skeleton process metadata (this includes links)
				try {
					loadWorkflow();
				} catch (GitException e) {
					throw new RDHException("Failed to load skeleton commit graph information", e);
				}
			}
		}

		/**
		 * @see bwfdm.replaydh.workflow.impl.DefaultWorkflow#ensureWorkflowStepData(bwfdm.replaydh.workflow.WorkflowStep)
		 */
		@Override
		protected WorkflowStep ensureWorkflowStepData(WorkflowStep step) {

			// Make sure we got the workflow skeleton loaded
			ensureFullWorkflowData();

			// Ensure that the commit message is parsed into actual step data
			synchronized (step) {
				Node<WorkflowStep> node = node(step);
				if(!node.flagSet(FLAG_LOADED)) {
					try {
						loadWorkflowStep(step);
					} catch (GitException e) {
						throw new RDHException("Failed to load data for workflow step: "+getLabel(step), e);
					}
					node.setFlag(FLAG_LOADED, true);
				}
			}

			return step;
		}
	}
}

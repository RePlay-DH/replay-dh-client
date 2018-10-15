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
package bwfdm.replaydh.core;

import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.LogManager;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.git.GitException;
import bwfdm.replaydh.git.JGitAdapter;
import bwfdm.replaydh.git.RDHInfoProperty;
import bwfdm.replaydh.io.FileTracker;
import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.io.resources.FileResource;
import bwfdm.replaydh.io.resources.FileResourceProvider;
import bwfdm.replaydh.metadata.MetadataBuilder;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRepository;
import bwfdm.replaydh.metadata.basic.file.FileMetadataRepository;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.stats.Interval;
import bwfdm.replaydh.stats.StatEntry;
import bwfdm.replaydh.stats.StatLog;
import bwfdm.replaydh.stats.StatType;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.core.RDHGui;
import bwfdm.replaydh.utils.Label;
import bwfdm.replaydh.utils.Lazy;
import bwfdm.replaydh.utils.Options;
import bwfdm.replaydh.utils.StringResource;
import bwfdm.replaydh.utils.annotation.Experimental;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.ResourceCache;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.impl.DefaultResource;
import bwfdm.replaydh.workflow.resolver.IdentifiableResolver;
import bwfdm.replaydh.workflow.resolver.LocalIdentifiableResolver;
import bwfdm.replaydh.workflow.schema.SchemaManager;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;
import bwfdm.replaydh.workflow.schema.impl.LocalSchemaManager;

/**
 *
 * Command line parameters supported by the client:
 * <table border="1">
 * <tr><th>Option</th><th>Type</th><th>Variants</th><th>Description</th></tr>
 * <tr><td>-v</td><td>flag</td><td>-Dintern.verbose=true</td><td>Causes the client to write additional logging information.</td></tr>
 * <tr><td>-dev</td><td>flag</td><td>-Dintern.debug=true</td><td>Starts the client in developer mode, making available additional debugging functions.</td></tr>
 * <tr><td>-config &lt;name&gt;</td><td>string</td><td>-</td><td>Name of the file containing additional settings. This file will also be used to save changes made by the user.</td></tr>
 * <tr><td>-dir &lt;file&gt;</td><td>path string</td><td>-Duser.folder=&lt;file&gt;</td><td>Path to the client folder where settings and other client internal informations get stored.</td></tr>
 * <tr><td>-workspace &lt;file&gt;</td><td>path string</td><td>-Dclient.workspace.path=&lt;file&gt;</td><td>Path to the currently active working directory.</td></tr>
 * <tr><td>-D&lt;key&gt;=&lt;value&gt;</td><td>strings</td><td>-</td><td>Allows to override client settings (this is a basic java command line feature and listed here simply for the sake of completeness, as the client reads in the full set of currently set Java properties as base of the internal settings, with higher priority that the read in config file).</td></tr>
 * </table>
 *
 * @author Markus Gärtner
 *
 */
public class RDHClient {

	private static final Logger log = LoggerFactory.getLogger(RDHClient.class);

	private static RDHClient client;

	private static final UncaughtExceptionHandler uncaughtExceptionHandler = new ErrorLogger();
	static {
		Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);

		// Does EDT exception handling still require this workaround to catch exceptions when in modal dialog?
		System.setProperty("sun.awt.exception.handler",	uncaughtExceptionHandler.getClass().getName());
	}

	private static final AtomicBoolean mainMethodInvoked = new AtomicBoolean(false);

	public static void main(String[] args) {

		// Make sure our client can only be started once per session
		if(!mainMethodInvoked.compareAndSet(false, true))
			throw new IllegalAccessError("Not allowed to invoke main method more than once per JVM session");

		try {
			client = new RDHClient(args);

			// If the boot sequence finishes without errors we're good to show UI
			client.boot();

			// Switch to GUI mode (will invoke a switch to EDT soonish)
			client.getGui().showUI();

			// Block the app thread till user decides to close the client
			client.awaitShutdown();

			// Now perform an orderly shutdown
			client.shutdownImpl();
		} catch(RDHLifecycleException e) {
			// RDHLifecycleException is just a wrapper, so use its message and cause for logging
			log.error("Error in default client lifecycle: {}", e.getMessage(), e.getCause());
		} catch(Exception e) {
			log.error("Unexpected error during client lifecycle", e);
		}

		//TODO show errors as dialog!
	}

	public static RDHClient client() {
		RDHClient client = RDHClient.client;
		checkState("No client instance available", client!=null);
		return client;
	}

	public static boolean hasClient() {
		return client!=null;
	}

	private final Map<RDHTool, ToolLifecycleState> tools = new IdentityHashMap<>();

	private final List<ToolLifecycleListener> toolLifecycleListeners = new CopyOnWriteArrayList<>();

	/**
	 * Startup options (saved so we can use them for shutdown as well)
	 */
	private final Options options;

	private final String[] args;

	private final Lazy<JGitAdapter> gitAdapter = Lazy.create(this::createGitAdapter, true);;

	private final Lazy<MetadataRepository> localMetadataRepository = Lazy.create(this::createLocalMetadataRepository, true);

	/**
	 * Dynamic client settings, workspace and locale
	 */
	private final Lazy<RDHEnvironment> environment = Lazy.create(this::createEnvironment, true);

	private ResourceManager resourceManager;

	private final Lazy<ScheduledExecutorService> executorService = Lazy.create(this::createExecutorService, true);

	private final Lazy<IdentifiableResolver> resourceResolver = Lazy.create(this::createResourceResolver, true);

	private final Lazy<RDHGui> gui = Lazy.create(this::createGui, true);

	private final Lazy<SchemaManager> schemaManager = Lazy.create(this::createSchemaManager, true);

	private final Lazy<ResourceCache> resourceCache = Lazy.create(this::createResourceCache, true);

	private final Lazy<PluginEngine> pluginEngine = Lazy.create(this::createPluginEngine, true);

	private final Lazy<StatLog> statLog = Lazy.create(this::createStatLog, true);

	private final boolean verbose;

	private final boolean debug;

	/**
	 * Root folder to store config files and other such information.
	 */
	private final Path userFolder;
	private final boolean createUserFolderIfMissing;

	private final Path clientFolder;

	private final EnvironmentObserver environmentObserver = new EnvironmentObserver();

	private final Object lock = new Object();

	private volatile boolean active = false;

	/**
	 * Flag indicating a requested restart.
	 * Note that this value can only ever change from
	 * {@code false} to {@code true}!
	 */
	private volatile boolean doRestart = false;

	private final String configFilename;

	private final Object terminationLock = new Object();

	private final Properties appInfo;

	private final Interval clientRuntime = new Interval();

	/**
	 * Creates a new  client using provided options.
	 *
	 * @param options
	 * @throws RDHLifecycleException
	 */
	public RDHClient(String[] args) throws RDHLifecycleException {

		this.args = args.clone();
		this.options = new Options();

		// Copy all system properties into our settings
		System.getProperties().forEach((key, value) -> options.put((String) key, value));

		readArguments(args, options);

		appInfo = new Properties();
		try(InputStream in = RDHClient.class.getResourceAsStream("app.ini");
				Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
			appInfo.load(reader);
		} catch (IOException e) {
			throw new RDHLifecycleException("Unable to read internal app info file", e);
		}

		configFilename = options.get(RDHProperty.INTERN_CONFIG_FILE.getKey(), "config.ini");

		verbose = options.getBoolean(RDHProperty.INTERN_VERBOSE.getKey());

		debug = options.getBoolean(RDHProperty.INTERN_DEBUG.getKey());

		String clientPath = (String) options.get(RDHProperty.INTERN_CLIENT_FOLDER.getKey());
		if(clientPath==null) {
			URL source = getClass().getProtectionDomain().getCodeSource().getLocation();
			if(source==null)
				throw new RDHLifecycleException("Unable to obtain client folder - no code source for client jar-archive available");

			try {
				clientFolder = Paths.get(source.toURI()).getParent();
			} catch (URISyntaxException e) {
				throw new RDHLifecycleException("Unable to convert path of code source to usable client fddler: "+source, e);
			}
		} else {
			clientFolder = Paths.get(clientPath);
		}

		String userPath = (String) options.get(RDHProperty.INTERN_USER_FOLDER.getKey());
		if(userPath==null) {
			userFolder = Paths.get((String)options.get("user.home"),
					"."+getAppInfo(AppProperty.NAME), getAppInfo(AppProperty.VERSION));
			createUserFolderIfMissing = true;
		} else {
			userFolder = Paths.get(userPath);
			createUserFolderIfMissing = false;
		}
	}

	private static void readArguments(String[] args, Options options) {
		for(int i=0; i<args.length; i++) {
			switch (args[i]) {
			case "-v":
				addOption(options, RDHProperty.INTERN_VERBOSE, Boolean.TRUE);
				break;

			case "-dev":
				addOption(options, RDHProperty.INTERN_DEBUG, Boolean.TRUE);
				break;

			case "-config":
				addOption(options, RDHProperty.INTERN_CONFIG_FILE, args[++i]);
				break;

			case "-dir":
				addOption(options, RDHProperty.INTERN_USER_FOLDER, args[++i]);
				break;

			case "-workspace":
				addOption(options, RDHProperty.CLIENT_WORKSPACE_PATH, args[++i]);
				break;

			default: {
				String arg = args[i];
				if(arg.startsWith("-D")) {
					int eqIndex = arg.indexOf('=', 3);
					if(eqIndex!=-1 && eqIndex<arg.length()-1) {
						options.put(arg.substring(2, eqIndex), arg.substring(eqIndex+1));
					}
					break;
				}

				throw new RDHException("Unknown paramater or malformed option: "+args[i]);
			}
			}
		}
	}

	private static void addOption(Options options, RDHProperty property, Object value) {
		options.put(property.getKey(), value.toString());
	}

	private void delegateLogOutput() throws RDHLifecycleException {

		// Grab raw config

		Properties logConfig = new Properties();
		try {
			logConfig.load(new InputStreamReader(
					RDHClient.class.getResourceAsStream("log.properties"),
					StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new RDHLifecycleException("", e);
		}

		Path logFolder;
		try {
			logFolder = ensureUserFolder(UserFolder.LOGS);
		} catch (IOException e) {
			throw new RDHLifecycleException("Unable to create default log folder", e);
		}

		// Adjust config to current environment
		logConfig.setProperty("java.util.logging.FileHandler.pattern",
				logFolder.toString()+"/rdh-client-%g-%u.log");

		// Now serialize the config again with the default charset required by the loading code.
		// Has to use ISO 8859-1 encoding to ensure compatibility!
		ByteArrayOutputStream bOut = new ByteArrayOutputStream(300);
		try {
			logConfig.store(new OutputStreamWriter(bOut, StandardCharsets.ISO_8859_1), null);
		} catch (IOException e) {
			throw new RDHLifecycleException("Failed to adjust logger configuration", e);
		}

		// Finally delegate LogManager to read our adjusted configuration
		try {
			LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(bOut.toByteArray()));
		} catch (SecurityException e) {
			throw new RDHLifecycleException("Not allowed to configure log manager.", e);
		} catch (IOException e) {
			throw new RDHLifecycleException("Unexpected I/O error while configuring log manager.", e);
		}
	}

	private Path getUserConfigFile() {
		return userFolder.resolve(configFilename);
	}

	public String getAppInfo(String key) {
		requireNonNull(key);
		return requireNonNull(appInfo.getProperty(key), "Missing "+key+" in app.info!");
	}

	public String getLicense() {
		try(InputStream in = getClass().getResourceAsStream("license-client.txt")) {
			return IOUtils.readStream(in, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RDHException("Failed to load license.txt", e);
		}
	}

	/**
	 * Returns a {@link Path} denoting a file or sub-folder in the user
	 * specific folder for this client. Tools can use this to store their
	 * own settings.
	 * <p>
	 * Will fail if the client internal config.ini file is requested
	 * this way.
	 */
	public final Path getUserFile(String name) {
		if(configFilename.equals(name))
			throw new RDHException("Not allowed to request access to config.ini file!");

		return userFolder.resolve(name);
	}

	private Path ensureUserFile(String name) throws IOException {
		Path file = getUserFile(name);
		if(!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
			Files.createFile(file);
		}

		return file;
	}

	private Path ensureUserFolder(UserFolder folder) throws IOException {
		Path file = getUserFile(folder.folderName);
		if(isVerbose()) {
			log.info("Ensuring user folder: "+file);
		}

		if(!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
			Files.createDirectories(file);
		}

		return file;
	}

	/**
	 *
	 * @return the currently active environment for this client.
	 */
	public RDHEnvironment getEnvironment() {
		return environment.value();
	}

	/**
	 * Returns the location of the designated default folder
	 * within the current user folder designated to the client.
	 *
	 * @param folder
	 * @return
	 */
	public Path getUserFolder(UserFolder folder) {
		return userFolder.resolve(requireNonNull(folder).folderName);
	}

	public Path  getClientFolder(ClientFolder folder) {
		return clientFolder.resolve(requireNonNull(folder).folderName);
	}

	/**
	 * Returns the file tracker that is currently being used to monitor
	 * changes to files in the workspace.
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends RDHTool & FileTracker> T getFileTracker() {
		return (T) gitAdapter.value();
	}

	/**
	 * Returns the default repository usable for local storage of object
	 * metadata. Depending on the client configuration this can either
	 * be an isolated repository for each client instance of a shared
	 * repository that can be accessed by clients via a common file system
	 * such as that of an institute.
	 *
	 * @return
	 */
	public MetadataRepository getLocalMetadataRepository() {
		return localMetadataRepository.value();
	}

	/**
	 * Returns the lookup facility that caches and maps from sets of
	 * {@link Identifier identifiers} to known resources, tools or persons.
	 *
	 * @return
	 */
	public IdentifiableResolver getResourceResolver() {
		return resourceResolver.value();
	}

	/**
	 * Returns a delegate method for fetching the currently active workflow
	 * for this client.
	 * @return
	 */
	public Supplier<Workflow> getWorkflowSource() {
		return gitAdapter.value()::getWorkflow;
	}

	/**
	 * @return the resourceCache
	 */
	public ResourceCache getResourceCache() {
		return resourceCache.value();
	}

	public PluginEngine getPluginEngine() {
		return pluginEngine.value();
	}

	public SchemaManager getSchemaManager() {
		return schemaManager.value();
	}

	public StatLog getStatLog() {
		return statLog.value();
	}

	/**
	 * Returns the client component responsible for managing the
	 * graphical user interface (GUI) of this client.
	 * @return
	 */
	public RDHGui getGui() {
		return gui.value();
	}

	/**
	 * @return whether or not this client is in verbose mode, meaning
	 * it will output a lot of additional information into the logs
	 */
	public boolean isVerbose() {
		return verbose;
	}

	public boolean isDevMode() {
		return debug;
	}

	public void resetWorkspace() {
		JGitAdapter git = gitAdapter.value();

		if(isVerbose()) {
			log.info("Resetting workspace");
		}

		git.disconnectGit();
	}

	/**
	 * Expects an existing workspace managed by the RePlay-DH client to
	 * be located at the specified {@link Path}.
	 *
	 * @param workspacePath
	 * @return
	 */
	public Workspace loadWorkspace(Path workspacePath) {
		JGitAdapter git = gitAdapter.value();

		if(isVerbose()) {
			log.info("Loading workspace at location: {}", workspacePath);
		}

		Workspace workspace = null;
		try {
			// Delegate the I/O work to our git wrapper
			workspace = git.connectGitAndLoadWorkspace(workspacePath);
		} catch (GitException | IOException e) {
			throw new RDHException("Failed to load workspace from location: "+workspacePath, e);
		}
		if(workspace==null)
			throw new RDHException("No workspace at location: "+workspacePath);

		// Finally publish the new workspace
		getEnvironment().setWorkspace(workspace);

		return workspace;
	}

	public Workspace createWorkspace(Path workspacePath, WorkflowSchema schema,
			String title, String description) {
		JGitAdapter git = gitAdapter.value();

		if(isVerbose()) {
			log.info("Creating workspace at location '{}' with schema {}",workspacePath, schema.getId());
		}

		// This takes care of basic default configurations in the workspace
		Workspace workspace = createBasicWorkspace(workspacePath, schema);
		workspace.setProperty(RDHInfoProperty.TITLE, title);
		workspace.setProperty(RDHInfoProperty.DESCRIPTION, description);
		try {
			// Delegate the I/O work to our git wrapper
			git.connectGitAndStoreWorkspace(workspace);
		} catch (IOException | GitException e) {
			throw new RDHException("Failed to create workspace at location: "+workspacePath, e);
		}

		// Finally publish the new workspace
		getEnvironment().setWorkspace(workspace);

		return workspace;
	}

	public Workspace createBasicWorkspace(Path workspacePath, WorkflowSchema schema) {
		return new Workspace(workspacePath, schema);
	}

	private void boot() throws RDHLifecycleException {

		synchronized (lock) {

			clientRuntime.start();

			// Make sure our user folder structure is valid and existing
			if(!Files.exists(userFolder)) {
				if(createUserFolderIfMissing) {
					// If allowed we need to create the entire folder structure in one go
					try {
						Files.createDirectories(userFolder);
					} catch (IOException e) {
						throw new RDHLifecycleException("Failed to ensure default user folder: "+userFolder, e);
					}
				} else
					throw new RDHLifecycleException("Missing custom user folder: "+userFolder);
			}

			// Before anything verbose is started we need to delegate logs to custom files
			delegateLogOutput();

			if(verbose) {
				log.info("User folder: {}", userFolder);
				log.info("Client options: {}", options);
			}

			// Load localization (until here the resource manager will default to system settings)
			loadResourceManager();

			// Try to collect tools (via plugins, etc?)
			try {
				collectTools();
			} catch(Exception e) {
				throw new RDHLifecycleException("Failed to collect tools", e);
			}

			// Try to actually start tools (in no particular order)
			try {
				int failedToolCount = startAllTools();
				if(failedToolCount>0) {
					log.error("Failed to start {} tools", failedToolCount);
					return;
				}
			} catch(Exception e) {
				throw new RDHLifecycleException("Failed to start tools", e);
			}

			getStatLog().log(StatEntry.ofType(StatType.INTERNAL_BEGIN, ClientStats.SESSION));

			// Publish info about started tools (this way tools can get to know each other)
			try {
				publishStartedTools();
			} catch(Exception e) {
				throw new RDHLifecycleException("Failed to publish initial tool startups", e);
			}

			//TODO DEBUG
			getPluginEngine();

			// At the very end mark us as active
			active = true;
		}
	}

	private RDHEnvironment createEnvironment() {

		synchronized (lock) {
			Properties defaultProperties = new Properties();

			try {

				// Load default properties
				InputStream in = RDHClient.class.getResourceAsStream("default-properties.ini");
				defaultProperties.load(new InputStreamReader(in, StandardCharsets.UTF_8));

				// Load user properties
				Path configFile = getUserConfigFile();
				if(Files.exists(configFile, LinkOption.NOFOLLOW_LINKS)) {
					Properties properties = new Properties(defaultProperties);
					try(Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
						properties.load(reader);
					}

					// Remove all entries that are duplicates of the default settings (potentially expensive, need to verify necessity)
					filterDefaultProperties(properties, defaultProperties);

					defaultProperties = properties;
				}
			} catch(IOException e) {
				log.error("Error while creating environment", e);

				throw new InternalError("Failed to lazily create environment", e);
			}

			// We forward the client options directly as properties (overrides internal settings!)
			defaultProperties.putAll(options);

			// Create blank environment
			RDHEnvironment environment = new RDHEnvironment(this, defaultProperties);

			// Process existing settings
			defaultPreprocessSettings(environment);

			environment.addPropertyChangeListener(environmentObserver);

			return environment;
		}
	}

	private void filterDefaultProperties(Properties properties, Properties defaultProperties) {
		for(Iterator<Entry<Object, Object>> it = properties.entrySet().iterator(); it.hasNext();) {
			Entry<Object, Object> entry = it.next();

			Object defaultValue = defaultProperties.get(entry.getKey());
			if(Objects.equals(defaultValue, entry.getValue())) {
				it.remove();
			}
		}
	}

	/**
	 * Convert textual settings into actual fields or other forms of
	 * "higher" properties.
	 *
	 * @param environment
	 */
	private void defaultPreprocessSettings(RDHEnvironment environment) {

		// If available directly set the designated workspace directory
//		String savedWorkspace = environment.getProperty(RDHProperty.CLIENT_WORKSPACE_PATH);
//		if(savedWorkspace!=null) {
//			environment.setWorkspacePath(Paths.get(savedWorkspace));
//		}
	}

	private IdentifiableResolver createResourceResolver() {

		synchronized (lock) {
			Path rootFolder = null;

			String savedRootFolder = getEnvironment().getProperty(RDHProperty.CLIENT_IDENTIFIER_CACHE_ROOT_FOLDER);
			if(savedRootFolder!=null) {
				rootFolder = Paths.get(savedRootFolder);
			}

			if(rootFolder==null) {
				try {
					rootFolder = ensureUserFolder(UserFolder.IDENTIFIERS);
				} catch (IOException e) {
					throw new RDHException("Unable to create default directory for identifier cache", e);
				}
			}

			if(!Files.isDirectory(rootFolder, LinkOption.NOFOLLOW_LINKS))
				throw new RDHException("Identifier cache root folder must point to a directory: "+rootFolder);

			IdentifiableResolver resolver = LocalIdentifiableResolver.newBuilder()
					.autoPerformCacheSerialization(true)
					.resourceProvider(FileResourceProvider.getSharedInstance())
					.folder(rootFolder)
					.useDefaultSerialization()
					.build();

			return addAndStartTool(resolver);
		}
	}

	private MetadataRepository createLocalMetadataRepository() {

		synchronized (lock) {
			Path rootFolder = null;

			String savedRootFolder = getEnvironment().getProperty(RDHProperty.CLIENT_METADATA_ROOT_FOLDER);
			if(savedRootFolder!=null) {
				rootFolder = Paths.get(savedRootFolder);
			}

			if(rootFolder==null) {
				try {
					rootFolder = ensureUserFolder(UserFolder.METADATA);
				} catch (IOException e) {
					throw new RDHException("Unable to create default directory for local metadata repository", e);
				}
			}

			if(!Files.isDirectory(rootFolder, LinkOption.NOFOLLOW_LINKS))
				throw new RDHException("Metadata root folder must point to a directory: "+rootFolder);

			FileMetadataRepository.Builder builder = FileMetadataRepository.newBuilder();

			builder.rootFolder(rootFolder);
			builder.useDublinCore();
			builder.useDublinCoreNameGenerator();
			builder.useDefaultCacheAndSerialization();
			builder.useVirtualUIDStorage();

			MetadataRepository repo = builder.build();

			repo = addAndStartTool(repo);

			// BEGIN DEBUG
			if(isDevMode() && !repo.hasRecords()) {
				repo.beginUpdate();
				try {
					for(int i=0; i<10; i++) {
						addDummyRecord(repo, i);
					}
				} finally {
					repo.endUpdate();
				}
			}
			// END DEBUG

			return repo;
		}
	}

	@Experimental
	private static void addDummyRecord(MetadataRepository repository, int index) {
		repository.beginUpdate();
		try {
			Resource resource = DefaultResource.uniqueResource();
			MetadataBuilder builder = repository.createBuilder(resource);
			builder.start();

			for(Label name : builder.getRequiredNames()) {
				if("title".equals(name.getLabel()) || "date".equals(name.getLabel())) {
					continue;
				}

				builder.addEntry(name.getLabel(), "xxxxxxxx");
			}

			builder.addEntry("title", "Resource "+String.valueOf(index+1));
			builder.addEntry("date", LocalDate.now().withDayOfMonth((index*2+1)%30).toString());

			MetadataRecord record = builder.build();

			repository.addRecord(record);

		} finally {
			repository.endUpdate();
		}
	}

	private JGitAdapter createGitAdapter() {

		synchronized (lock) {
			return addAndStartTool(new JGitAdapter());
		}
	}

	private RDHGui createGui() {

		synchronized (lock) {
			return addAndStartTool(new RDHGui());
		}
	}

	private SchemaManager createSchemaManager() {
		synchronized (lock) {

			LocalSchemaManager.Builder builder = LocalSchemaManager.newBuilder();
			builder.setIncludeSharedDefaultSchema(true);

			// We try to ensure user folder related lookup first, since that one can fail
			try {
				builder.addFolder(ensureUserFolder(UserFolder.SCHEMAS));
			} catch (IOException e) {
				throw new RDHException("Unable to create default directory for local schema manager", e);
			}

			// Now add the static client centric folder
			builder.addFolder(getClientFolder(ClientFolder.SCHEMAS));

			SchemaManager schemaManager = builder.build();

			return addAndStartTool(schemaManager);
		}
	}

	private ResourceCache createResourceCache() {
		synchronized (lock) {
			return addAndStartTool(new ResourceCache());
		}
	}

	private PluginEngine createPluginEngine() {
		synchronized (lock) {
			Path pluginFolder = getClientFolder(ClientFolder.PLUGINS);

			if(!Files.exists(pluginFolder, LinkOption.NOFOLLOW_LINKS)) {
				log.warn("No plugin folder found at path: {}", pluginFolder);
			} else if(!Files.isDirectory(pluginFolder, LinkOption.NOFOLLOW_LINKS))
				throw new RDHException("Plugins folder must point to a directory: "+pluginFolder);

			return addAndStartTool(new PluginEngine(pluginFolder, FileResourceProvider.getSharedInstance()));
		}
	}

	private StatLog createStatLog() {
		synchronized (lock) {
			Path folder;
			try {
				folder = ensureUserFolder(UserFolder.STATS);
			} catch (IOException e) {
				throw new RDHException("Unable to create default directory for usage statistics", e);
			}
//			String baseName = "usagestats-"+LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
			String baseName = "usage_stats.txt";
			Path logFile = folder.resolve(baseName);

			return addAndStartTool(new StatLog(new FileResource(logFile)));
		}
	}

	private void loadResourceManager() {
		final RDHEnvironment environment = getEnvironment();

		boolean notifyMissingResource = environment.getBoolean(RDHProperty.INTERN_RESOURCES_REPORT_MISSING, false);
		boolean returnKeyIfAbsent = environment.getBoolean(RDHProperty.INTERN_RESOURCES_RETURN_ABSENT_KEYS, false);
		Locale locale = environment.getLocale();

		ResourceManager resourceManager = new ResourceManager(
				locale, // use our stored locale
				environment::getProperty, // allow referencing of environmental properties
				notifyMissingResource, // log keys of missing resources if needed
				returnKeyIfAbsent // use missing keys as return value -> easier visual recognition
				);
		resourceManager.addManagedResource("bwfdm.replaydh.resources.localization");

		ResourceManager.setInstance(resourceManager);

		this.resourceManager = resourceManager;
	}

	private void collectTools() {
		//TODO collect and instantiate additional tools
	}

	/**
	 * Tries to start all tools and returns the number of
	 * tools for which this failed.
	 */
	private int startAllTools() {
		int failedToolCount = 0;

		RDHEnvironment environment = getEnvironment();

		synchronized (tools) {
			for(Iterator<Entry<RDHTool, ToolLifecycleState>> it = tools.entrySet().iterator(); it.hasNext();) {
				Entry<RDHTool, ToolLifecycleState> entry = it.next();
				RDHTool tool = entry.getKey();
				ToolLifecycleState state = entry.getValue();

				// We're not firing lifecycle events in this phase!
				if(state==ToolLifecycleState.INACTIVE) {
					boolean startFailed = false;

					try {
						entry.setValue(ToolLifecycleState.STARTING);
						tool.start(environment);
					} catch (Exception e) {
						log.error("Failed to start tool {}",
								tool.getClass(), e);
						failedToolCount++;
						startFailed = true;
					} finally {
						entry.setValue(startFailed ? ToolLifecycleState.FAILED : ToolLifecycleState.ACTIVE);
					}

				} else {
					log.error("Tool state currupted for {} - expected {}, got {}",
							tool.getClass(), ToolLifecycleState.INACTIVE, state);
					failedToolCount++;
				}
			}
		}

		return failedToolCount;
	}

	private void publishStartedTools() {
		synchronized (tools) {
			for(Entry<RDHTool, ToolLifecycleState> entry : tools.entrySet()) {
				if(entry.getValue()==ToolLifecycleState.ACTIVE) {
					fireToolLifecycleChanged(entry.getKey(), ToolLifecycleState.INACTIVE, ToolLifecycleState.ACTIVE);
				}
			}
		}
	}

	/**
	 * Convert certain "higher" forms of properties back into
	 * textual settings for serialization.
	 *
	 * @param environment
	 */
	private void defaultPostprocessSettings(RDHEnvironment environment) {
		Path workspace = environment.getWorkspacePath();
		String workspaceString = workspace==null ? null : workspace.toAbsolutePath().toString();
		environment.setProperty(RDHProperty.CLIENT_WORKSPACE_PATH, workspaceString);

		System.getProperties().keySet().forEach(key -> environment.setProperty((String) key, null));
	}

	private static final String SETTINGS_FILE_COMMENT = "RePlay-DH client settings, do NOT modify manually!";

	private void saveEnvironment() throws IOException {
		RDHEnvironment environment = getEnvironment();

		// Make sure the properties reflect current client state
		defaultPostprocessSettings(environment);

		Properties properties = environment.getProperties();

		// If no deviation from defaults the properties map will be empty and we don't need to save anything
		if(properties.isEmpty()) {
			return;
		}

		Path configFile = getUserConfigFile();

		try(Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
			properties.store(writer, SETTINGS_FILE_COMMENT);
		}
	}

	private void executeShutdownHooks() {
		if(!environment.created()) {
			return;
		}

		Set<Runnable> shutdownHooks = getEnvironment().getShutdownHooks();

		for(Runnable task : shutdownHooks) {
			try {
				task.run();
			} catch(Exception e) {
				if(task instanceof StringResource) {
					String title = ((StringResource)task).getStringValue();

					log.error("Failed to execute named shutdown hook: {}", title, e);
				} else {
					log.error("Failed to execute anonymous shutdown hook", e);
				}
			}
		}
	}

	/**
	 * Causes the client to terminate by continuing the shutdown section
	 * of the main method.
	 * <p>
	 * Note that shutdown of the client is done asynchronously and this
	 * method will return immediately. Due to the shutdown procedure being
	 * implemented outside the influence of our executor service, there
	 * are no real restrictions on the nature of threads calling this
	 * method.
	 */
	public void shutdown() {
		synchronized (terminationLock) {
			terminationLock.notifyAll();
		}
	}

	public void restart() {
		doRestart = true;
		shutdown();
	}

	/**
	 * Causes the calling thread to wait for the client to terminate.
	 * We use this from the thread that executed the main method, to
	 * ensure that no complications with the executor service can arise.
	 *
	 * @throws RDHLifecycleException
	 */
	private void awaitShutdown() throws RDHLifecycleException {
		synchronized (terminationLock) {
			try {
				terminationLock.wait();
			} catch (InterruptedException e) {
				throw new RDHLifecycleException("Unexpected wake-up by interruption while waiting for client termination", e);
			}
		}
	}

	/**
	 * Initiates an orderly shutdown of the client.
	 */
	private void shutdownImpl() {
		synchronized (lock) {
			checkState("Client already shut down", active);

			log.info("Client shutdown initiated. Restart requested: {}", doRestart ? "yes" : "no");

			getStatLog().log(StatEntry.withData(StatType.INTERNAL_END, ClientStats.SESSION,
					clientRuntime.stop().asDurationString()));

			List<ErrorDescription> errors = new ArrayList<>();

			RDHEnvironment environment = getEnvironment();

			// Make sure we don't get stuck in notification loops here
			environment.removePropertyChangeListener(environmentObserver);

			synchronized (tools) {
				for(Entry<RDHTool, ToolLifecycleState> entry : tools.entrySet()) {
					RDHTool tool = entry.getKey();
					ToolLifecycleState state = entry.getValue();

					if(state==ToolLifecycleState.ACTIVE) {
						entry.setValue(ToolLifecycleState.STOPPING);
						setStateUnsafe(tool, ToolLifecycleState.STOPPING, true);
						boolean stopFailed = false;

						try {
							tool.stop(environment);
						} catch(Exception e) {
							stopFailed = true;

							errors.add(new ErrorDescription(e, "Failed to stop tool: "+tool.getClass()));
						} finally {
							ToolLifecycleState newState = stopFailed ? ToolLifecycleState.FAILED : ToolLifecycleState.INACTIVE;
							entry.setValue(newState);
							setStateUnsafe(tool, newState, true);
						}
					}
				}

				// Forcefully erase current tool2state mapping
				tools.clear();
			}

			// Execute and clear registered shutdown hooks after tools stopped
			executeShutdownHooks();

			/*
			 *  Store new or updated settings.
			 *
			 *  We do this AFTER stopping all the tools to give them a chance
			 *  to store their individual settings in the environment.
			 */
			try {
				saveEnvironment();
			} catch (IOException e) {
				errors.add(new ErrorDescription(e, "Failed to save environment settings"));
			}

			// If we have an active executor service let it terminate in an orderly fashion
			if(executorService.created()) {
				ExecutorService executorService = getExecutorService();
				executorService.shutdown();
				if(!executorService.isTerminated()) {
					try {
						log.info("Terminating executor service (this may take a while)");
						// Multiple log entries are an indicator for problematic pending tasks
						while(!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
							log.warn("Termination of executor service timed out...");
						}
					} catch (InterruptedException e) {
						errors.add(new ErrorDescription(e, "Termination of executor service interrupted"));
					}
				}
			}

			// From here on the environment will be unusable
			environment.dispose();

			/*
			 * Finally show an error dialog in case we encountered any issues so far.
			 * Note that from this point on any exception that also kills the UI
			 * means that the client can finish shutting down in an "orderly" fashion
			 * as no additional threads keep the VM alive (at least once the last window
			 * closes...).
			 */
			if(!errors.isEmpty()) {
				log.info("Client shutdown encountered {} errors", errors.size());

				//TODO change to gui version (make a nice dialog and block since we aren't on the EDT)
				for(ErrorDescription ed : errors) {
					log.error(ed.description, ed.error);
				}
			}

			/*
			 *  Clear and reset resource management.
			 *  We're waiting for this till after displaying errors, so we can at
			 *  least provide the user a nice localized visual info ;)
			 */
			resourceManager.close();
			resourceManager = null;
			// From here on any localization will default back to system default
			ResourceManager.setInstance(null);

			/*
			 *  No need to manually shutdown the VM. From here on
			 *  all settings are securely stored, all UI elements are
			 *  disposed and other associated resources have been released.
			 */
			log.info("Client shutdown finished");

			// Maybe move this flag switch up a bit?
			active = false;

			/*
			 *  If we are instructed to restart the client, now is
			 *  the time to create a new process and let the current one end.
			 */
			if(doRestart) {
				try {
					invokeRestart();
				} catch (IOException e) {
					log.error("Failed to restart client", e);

					//TODO maybe also show a notification dialog?
				}
			}
		}
	}

	private void invokeRestart() throws IOException {

		log.info("Initiating restart...");

		RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

		Path jarFile = IOUtils.getJarFile();
		boolean isJar = jarFile.getFileName().toString().endsWith(".jar");

		List<String> command = new ArrayList<>();

		command.add("java");

		String classpath = System.getProperty("java.class.path");
		if(!isJar) {
			classpath += System.getProperty("path.separator")+jarFile.toString();
		}

		Collections.addAll(command, "-cp", classpath);

		// Add all VM arguments prior to the client parameters
		if(isJar) {
			command.addAll(runtime.getInputArguments());
		}

		// Add jar target if applicable
		if(isJar) {
			String jarName = jarFile.getFileName().toString();
			Collections.addAll(command, "-jar", jarName);
		} else {
			// If no jar available (run in an IDE maybe?) directly start the client class
			command.add(RDHClient.class.getName());
		}

		// Now add the client arguments
		Collections.addAll(command, args);

		Process process = new ProcessBuilder(command)
				.inheritIO()
				.directory(null) // inherit current working directory
				.start();

		try {
			// Give the new process a little time for startup
			process.waitFor(2, TimeUnit.SECONDS);
		} catch (InterruptedException ignore) {
			// no-op
		}

		log.info("New client process started, alive={} ... exiting current session", process.isAlive());

		// From here on the current process will end gracefully
	}

	public static class ErrorDescription {
		ErrorDescription(Throwable error, String description) {
			this.error = requireNonNull(error);
			this.description = requireNonNull(description);
		}

		public final Throwable error;
		public final String description;
	}

	private void fireToolLifecycleChanged(RDHTool tool, ToolLifecycleState oldState, ToolLifecycleState newState) {
		/*
		 *  Exiting early means we can only miss concurrent addition of a new listener.
		 *  Removal of listeners after this check doesn't really matter.
		 */
		if(toolLifecycleListeners.isEmpty()) {
			return;
		}

		for(ToolLifecycleListener listener : toolLifecycleListeners) {
			listener.toolLifecycleStateChanged(tool, oldState, newState);
		}
	}

	public void addToolLifecycleListener(ToolLifecycleListener listener) {
		toolLifecycleListeners.add(listener);
	}

	public void removeToolLifecycleListener(ToolLifecycleListener listener) {
		toolLifecycleListeners.remove(listener);
	}

	/**
	 * Adds a new tool in {@link ToolLifecycleState#RESOLVED} state.
	 *
	 * @param tool
	 */
	public void addTool(RDHTool tool) {
		requireNonNull(tool);

		addTool0(tool);
	}

	public void addTool(Class<? extends RDHTool> clazz) {
		requireNonNull(clazz);

		RDHTool tool = null;

		try {
			tool = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RDHException("Failed to instantiate new tool for class: "+clazz, e);
		}

		addTool0(tool);
	}

	private <T extends RDHTool> T addTool0(T tool) {
		if(!trySetState(tool, ToolLifecycleState.UNKNOWN, ToolLifecycleState.INACTIVE))
			throw new RDHException("Tool already registered: "+tool.getClass());

		return tool;
	}

	private <T extends RDHTool> T addAndStartTool(T tool) {
		addTool0(tool);

		startTool(tool);

		return tool;
	}

	public void stopTool(RDHTool tool) {
		requireNonNull(tool);

		synchronized (tools) {
			ToolLifecycleState state = getStateUnsafe(tool);
			if(state!=ToolLifecycleState.ACTIVE)
				throw new RDHException("Tool not in a stoppable state: "+tool.getClass()+" - "+state);

			setStateUnsafe(tool, ToolLifecycleState.STOPPING, true);

			try {
				tool.stop(getEnvironment());
			} catch(RDHLifecycleException e) {
				setStateUnsafe(tool, ToolLifecycleState.FAILED, true);
				throw new RDHException("Failed to stop tool: "+tool.getClass(), e);
			}

			setStateUnsafe(tool, ToolLifecycleState.INACTIVE, true);
		}
	}

	public void startTool(RDHTool tool) {
		requireNonNull(tool);

		synchronized (tools) {
			ToolLifecycleState state = getStateUnsafe(tool);
			if(state!=ToolLifecycleState.INACTIVE)
				throw new RDHException("Tool not in a startable state: "+tool.getClass()+" - "+state);

			setStateUnsafe(tool, ToolLifecycleState.STARTING, true);

			try {
				tool.start(getEnvironment());
			} catch(RDHLifecycleException e) {
				setStateUnsafe(tool, ToolLifecycleState.FAILED, true);
				throw new RDHException("Failed to start tool: "+tool.getClass(), e);
			}

			setStateUnsafe(tool, ToolLifecycleState.ACTIVE, true);
		}
	}

	private ToolLifecycleState getStateUnsafe(RDHTool tool) {
		ToolLifecycleState state = tools.get(tool);
		return state==null ? ToolLifecycleState.UNKNOWN : state;
	}

	private ToolLifecycleState setStateUnsafe(RDHTool tool, ToolLifecycleState newState, boolean publish) {
		ToolLifecycleState oldState = getStateUnsafe(tool);
		tools.put(tool, newState);
		if(publish) {
			fireToolLifecycleChanged(tool, oldState, newState);
		}

		return oldState;
	}

	public ToolLifecycleState getState(RDHTool tool) {
		synchronized (tools) {
			return getStateUnsafe(tool);
		}
	}

	/**
	 * Implements CAS-semantics for a tool's state as seen by the client.
	 * <p>
	 * Attempts to change the state of a given tool to {@code newState} with
	 * the expectation that the current state matches {@code expectedState}.
	 * Returns {@code true} if that condition was met and {@code false} otherwise,
	 * in which case no changes are performed.
	 */
	private boolean trySetState(RDHTool tool, ToolLifecycleState expectedState, ToolLifecycleState newState) {
		synchronized (tools) {
			ToolLifecycleState currentState = getStateUnsafe(tool);

			boolean isExpectedState = currentState==expectedState;

			if(isExpectedState) {
				setStateUnsafe(tool, newState, true);
			}

			return isExpectedState;
		}
	}

	public void defaultHandleError(Throwable t) {
		final boolean isEDT = SwingUtilities.isEventDispatchThread();

		GuiUtils.invokeEDT(() -> defaultShowErrorDialog(t, isEDT));
	}

	private void defaultShowErrorDialog(Throwable t, boolean isEDT) {
		GuiUtils.checkEDT();

		String title = isEDT ?
				"replaydh.dialogs.guiError.title"
				: "replaydh.dialogs.unknownError.title";
		String message = isEDT ?
				"replaydh.dialogs.guiError.message"
				: "replaydh.dialogs.unknownError.message";

		GuiUtils.showErrorDialog(null, title, message, t);
	}

	public static class ErrorLogger implements UncaughtExceptionHandler {

		/**
		 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
		 */
		@Override
		public void uncaughtException(Thread thread, Throwable e) {
			// Synchronize on class to allow concurrent logging across multiple instances
			synchronized (ErrorLogger.class) {
				log.error("Uncaught exception in thread {}: {}",  thread, e.getMessage(), e);

				RDHClient client = RDHClient.client;
				if(client!=null) {
					client.defaultHandleError(e);
				}
			}
		}

	}

	public ScheduledExecutorService getExecutorService() {
		return executorService.value();
	}

	private ScheduledExecutorService createExecutorService() {
		final AtomicInteger threadCounter = new AtomicInteger(0);

		final ThreadFactory threadFactory = new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				// Per default we just use a base name and append thread counter
				Thread t = new Thread(r, "replay-dh-worker-"+threadCounter.incrementAndGet());
				t.setUncaughtExceptionHandler(uncaughtExceptionHandler);
				return t;
			}
		};

		RDHEnvironment environment = getEnvironment();

		// Settings can specify a custom limit on parallelism or thread pool size
		int threadLimit = environment.getInteger(RDHProperty.INTERN_EXECUTOR_MAX_THREADS, -1);

		// If no limit is set we can always restrict it based on processor count
		if(threadLimit==-1 && environment.getBoolean(RDHProperty.INTERN_EXECUTOR_LIMIT_TO_CORES, false)) {
			threadLimit = Runtime.getRuntime().availableProcessors();
		}

		// Use "unlimited" threads if limit set to negative or 0 (preventing dumb settings)
		if(threadLimit<=0) {
			threadLimit = 200;
		}

		return Executors.newScheduledThreadPool(threadLimit, threadFactory);
	}

	private class EnvironmentObserver implements PropertyChangeListener {

		/**
		 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
		 */
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			switch (evt.getPropertyName()) {

			default:
				// do nothing for the rest
				break;
			}
		}

	}
}

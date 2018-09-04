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

import static java.util.Objects.requireNonNull;

import java.awt.SystemTray;
import java.nio.file.Path;

import bwfdm.replaydh.workflow.Tool;

/**
 * Collection of property keys usable for stored setting.
 *
 * @author Markus Gärtner
 *
 */
public enum RDHProperty {

	// Client properties to store actual user settings

	/**
	 * String form of the workspace {@link Path path} object.
	 */
	CLIENT_WORKSPACE_PATH("client.workspace.path"),

	/**
	 * semicolon-separated list of previous workspace paths.
	 *
	 * TODO use platform-dependent path separator char!
	 */
	CLIENT_WORKSPACE_HISTORY("client.workspace.history"),

	CLIENT_WORKSPACE_TRACKER_ACTIVE("client.workspace.tracker.active", false),
	CLIENT_WORKSPACE_TRACKER_INTERVAL("client.workspace.tracker.interval", 60),

	/**
	 * Flag to indicate whether the UI should hide additional
	 * or redundant help information that's usually required
	 * for new or unexperienced users.
	 */
	CLIENT_EXPERT_MODE("client.expertMode", false),

	/**
	 * Flag to indicate that the application should not use the
	 * {@link SystemTray tray area} even when it is supported
	 * on the operating system.
	 */
	CLIENT_UI_TRAY_DISABLED("client.ui.trayDisabled", false),

	/**
	 * Flag to indicate that the client's main window should
	 * always stay o ntop of other windows when not minimized.
	 */
	CLIENT_UI_ALWAYS_ON_TOP("client.ui.alwaysOnTop", false),

	/**
	 * Language setting for the client
	 */
	CLIENT_LOCALE("client.locale", "en"),

	/**
	 * Optional root folder for the centralized storage of
	 * local object metadata.
	 */
	CLIENT_METADATA_ROOT_FOLDER("client.metadata.rootFolder"),

	/**
	 * Optional root folder for the centralized storage of
	 * locally cached identifier data.
	 */
	CLIENT_IDENTIFIER_CACHE_ROOT_FOLDER("client.identifierCache.rootFolder"),

	/**
	 * The "namespace" this client is used in.
	 */
	CLIENT_ORGANIZATION("client.organization"),

	/**
	 * An (ideally) unique name within the provided
	 * {@link #CLIENT_ORGANIZATION} to identify the current
	 * user.
	 */
	CLIENT_USERNAME("client.username"),

	// Internal properties used to setup localization, logging, etc...

	INTERN_RESOURCES_REPORT_MISSING("intern.resources.reportMissing", true),

	INTERN_RESOURCES_RETURN_ABSENT_KEYS("intern.resources.returnAbsentKeys", true),

	INTERN_EXECUTOR_MAX_THREADS("intern.executor.maxThreads"),
	INTERN_EXECUTOR_LIMIT_TO_CORES("intern.executor.limitToCores", false),

	INTERN_VERBOSE("intern.verbose", false),

	/**
	 * Boolean flag to start the client in debug or developer mode.
	 * <p>
	 * The default value for this property is {@code false}.
	 */
	INTERN_DEBUG("intern.debug", false),
	/**
	 * Name of the configurations file inside the designated client folder.
	 */
	INTERN_CONFIG_FILE("intern.config.file"),
	/**
	 * The folder to store user related data such as logs, metadata, etc...
	 */
	INTERN_USER_FOLDER("user.folder"),
	/**
	 * The folder to read static environmental things such as plugins
	 */
	INTERN_CLIENT_FOLDER("client.folder"),

	INTERN_FORCE_WELCOME_DIALOG("intern.forceWelcomeDialog", false),

	// Git properties used for interaction with JGit
	//TODO make JGitAdapter use these properties

	/**
	 * Central folder for git repositories. Can be used when
	 * the user doesn't want the git repositories to be located
	 * in the individual workspaces.
	 */
	GIT_CENTRAL_FOLDER("git.centralFolder"),

	/**
	 * Alternative location for the local git repository.
	 */
	GIT_LOCAL_REPOSITORY("git.localRepository"),

	/**
	 * Maximum size of a file to be included in version control
	 * by git. Files above this threshold will be ignored automatically.
	 * A value of 0, no matter the unit used, will result in no automatic
	 * size checks be performed!
	 * <p>
	 * Format: xxx&lt;unit&gt; with unit being one of <code>MB, GB, TB</code>
	 * <p>
	 * The default value for this property is {@code 25MB}.
	 */
	GIT_MAX_FILESIZE("git.maxFileSize", "25MB"),

	/**
	 * Boolean flag to indicate whether or not the git adapter
	 * should attach additional internal information to the
	 * workflow steps it creates out of git commits.
	 * <p>
	 * The default value for this property is {@code false}.
	 */
	GIT_ATTACH_INFO_TO_STEPS("git.attachInfoToSteps", false),

	/**
	 * Boolean flag to indicate that the client should not report
	 * the missing of process metadate entries in commit messages.
	 * <p>
	 * The default value for this property is {@code false}.
	 */
	GIT_IGNORE_MISSING_METADATA("git.ignoreMissingMetadata", false),

	// Properties defining elicitation of process metadata

	/**
	 * Flag to indicate whether or not the client should automatically
	 * record hardware information for {@link Tool} entries.
	 */
	WORKFLOW_RECORD_HARDWARE("workflow.metadata.recordHardware"),

	/**
	 * Pointer to the location of the optional external schema definition
	 * that provides a custom vocabulary for certain parts of the
	 * workflow metadata.
	 */
	WORKFLOW_SCHEMA_LOCATION("workflow.schema.location"),

	// Properties defining settings and repositories for the DSpace adapter

	DSPACE_REPOSITORY_URL("dspace.repository.url"),
	DSPACE_REPOSITORY_NAME("dspace.repository.name"),
	
	DATAVERSE_REPOSITORY_URL("dataverse.repository.url"),
	DATAVERSE_REPOSITORY_NAME("dataverse.repository.name"),
	;

	private final String key;
	private final Object defaultValue;

	private RDHProperty(String key) {
		this(key, null);
	}

	private RDHProperty(String key, Object defaultValue) {
		this.key = requireNonNull(key);
		this.defaultValue = defaultValue;
	}

	@SuppressWarnings("unchecked")
	public <T extends Object> T getDefaultValue() {
		return (T) defaultValue;
	}

	public String getKey() {
		return key;
	}

	/**
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() {
		return getKey();
	}
}

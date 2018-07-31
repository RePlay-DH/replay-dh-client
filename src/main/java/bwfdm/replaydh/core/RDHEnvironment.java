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

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Locale.Category;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.utils.PropertyChangeSource;
import bwfdm.replaydh.utils.StringResource;

/**
 * Models the storage for settings and other environmental variables
 * used by the RDH-client and gives access to the active client instance.
 * <p>
 * Client code can register {@link PropertyChangeListener} instances to be notified
 * about certain changes to this environment. The following names for properties are
 * directly supported:
 * <table border="1">
 * <tr><th>Name</th><th>Type</th><th>Description</th></tr>
 * <tr><td>workflow</td><td>{@link Path}</td><td>Working directory for the client has changed</td></tr>
 * <tr><td>properties</td><td>{@link Set Set&lt;String&gt;}</td><td>A bigger portion of the internal properties has changed.
 * The set contains all keys that were used in performing the change and will be passed to listener methods as {@code newValue} argument.</td></tr>
 * <tr><td>property:&lt;id&gt;</td><td>String</td><td>A single property has changed. Note that the property name reported to the listener method is the original property key prefixed by {@code "property:"}</td></tr>
 * </table>
 *
 * @author Markus Gärtner
 */
public class RDHEnvironment implements PropertyChangeSource {

	/**
	 * Name indicating a <i>workspace</i> change, i.e. the
	 * {@link #getWorkspace() workspace} of this environment
	 * has been modified.
	 * <p>
	 * The value type of the associated property is {@link Workspace}
	 */
	public static final String NAME_WORKSPACE = "workspace";

//	/**
//	 * Name indicating a <i>workspace</i> change, i.e. the
//	 * {@link #getWorkspacePath() workspace path} of this environment
//	 * has been modified.
//	 * <p>
//	 * The value type of the associated property is {@link Path}
//	 */
//	public static final String NAME_WORKSPACE_PATH = "workspacePath";

	/**
	 * Indicating that the entire set of properties of this
	 * environment has been changed.
	 */
	public static final String NAME_PROPERTIES = "properties";

	/**
	 * Prefix used for events to indicate that a single property
	 * has been changed.
	 * <p>
	 * The value type of the associated property is {@link String}
	 */
	public static final String PROPERTY_PREFIX = "property:";

	private static String prefixedPropertyKey(String key) {
		return PROPERTY_PREFIX+key;
	}

	/**
	 * Checks whether or not the given property {@code name} is prefixed
	 * by {@value #PROPERTY_PREFIX}. If it is then the original property
	 * name without the artificial prefix is returned, otherwise the result
	 * will be {@code null}.
	 *
	 * @param name
	 * @return
	 */
	public static String unpackPropertyName(String name) {
		if(name.startsWith(PROPERTY_PREFIX)) {
			return name.substring(PROPERTY_PREFIX.length());
		} else {
			return null;
		}
	}

	private static final Logger log = LoggerFactory.getLogger(RDHEnvironment.class);

	/**
	 * The 2real" workspace object encapsulating the physical location,
	 * schema and additional configuration data.
	 */
	private Workspace workspace;

//	/**
//	 * Weak link to the current workspace location.
//	 */
//	private Path workspacePath;

	private Locale locale;

	private final Properties properties;

	private final PropertyChangeSupport changeSupport;

	private final RDHClient client;

	private final Set<Runnable> shutdownHooks = new HashSet<>();

	public RDHEnvironment(RDHClient client, Properties defaultProperties) {
		requireNonNull(defaultProperties);
		requireNonNull(client);

		this.client = client;

		changeSupport = new PropertyChangeSupport(this);
		properties = defaultProperties;
	}

	// Read methods

	public RDHClient getClient() {
		return client;
	}

	private void firePropertyChange(RDHProperty property, Object oldValue, Object newValue) {
		changeSupport.firePropertyChange(property.getKey(), oldValue, newValue);
	}

	/**
	 * Locale loading mechanism:
	 * <ol>
	 * <li>Read property {@link RDHProperty#PROPERTY_CLIENT_LOCALE}</li>
	 * <li>If above property is set, try interpreting it as as language tag using
	 * {@link Locale#forLanguageTag(String)}</li>
	 * <li>As fall-back use the system {@link Locale#getDefault(Category) default}
	 * for {@link Category#DISPLAY displaying}</li>
	 * </ol>
	 *
	 * @return
	 */
	public synchronized Locale getLocale() {
		if(locale==null) {
			Locale lc = null;

			String savedLocale = properties.getProperty(RDHProperty.CLIENT_LOCALE.getKey());
			if(savedLocale!=null) {
				lc = Locale.forLanguageTag(savedLocale);
			}

			if(lc==null) {
				lc = Locale.getDefault(Category.DISPLAY);
			}

			// Persist the locale "changes" within this JVM session
			Locale.setDefault(lc);
			GuiUtils.updateLocaleSensitiveUI(lc);

			locale = lc;
		}

		return locale;
	}

	public synchronized Workspace getWorkspace() {
		return workspace;
	}

	public synchronized Path getWorkspacePath() {
		Workspace workspace = getWorkspace();
		return workspace==null ? null : workspace.getFolder();
	}

//	public synchronized Path getWorkspacePath() {
//		return workspacePath;
//	}

	public synchronized String getProperty(String key) {
		return getProperty(key, null);
	}

	public synchronized String getProperty(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	public synchronized String getProperty(RDHProperty property) {
		return getProperty(property.getKey(), property.getDefaultValue());
	}

	public synchronized String getProperty(RDHProperty property, String defaultValue) {
		return properties.getProperty(property.getKey(), defaultValue);
	}

	public synchronized boolean getBoolean(RDHProperty property, boolean defaultValue) {
		return getBoolean(property.getKey(), defaultValue);
	}

	public synchronized boolean getBoolean(RDHProperty property) {
		Object defaultValue = property.getDefaultValue();
		return getBoolean(property.getKey(), defaultValue==null ? false : (boolean)defaultValue);
	}

	public synchronized boolean getBoolean(String key, boolean defaultValue) {
		String value = getProperty(key);
		return value!=null ? Boolean.parseBoolean(value) : defaultValue;
	}

	public synchronized int getInteger(RDHProperty property, int defaultValue) {
		return getInteger(property.getKey(), defaultValue);
	}

	public synchronized int getInteger(RDHProperty property) {
		Object defaultValue = property.getDefaultValue();
		return getInteger(property.getKey(), defaultValue==null ? 0 : (int)defaultValue);
	}

	public synchronized int getInteger(String key, int defaultValue) {
		String value = getProperty(key);
		return value!=null ? Integer.parseInt(value) : defaultValue;
	}

	public synchronized double getDouble(RDHProperty property, double defaultValue) {
		return getDouble(property.getKey(), defaultValue);
	}

	public synchronized double getDouble(RDHProperty property) {
		Object defaultValue = property.getDefaultValue();
		return getDouble(property.getKey(), defaultValue==null ? 0D : (double)defaultValue);
	}

	public synchronized double getDouble(String key, double defaultValue) {
		String value = getProperty(key);
		return value!=null ? Double.parseDouble(value) : defaultValue;
	}

	/**
	 * Package-private exposure of registered shutdown hooks
	 * so the client can use them when actually shutting down.
	 * <p>
	 * Implementation details:
	 * This method was changed to only return a copy of the current
	 * set of shutdown hooks. This way we don't need to worry about
	 * synchronization issues when for some reason new hooks get
	 * registered while processing the existing shutdown hooks. The
	 * original collection of shutdown hooks will be {@link Set#clear() cleared}.
	 *
	 * @return
	 */
	synchronized Set<Runnable> getShutdownHooks() {
		Set<Runnable> result = new HashSet<>(shutdownHooks);
		shutdownHooks.clear();
		return result;
	}

	// Write methods

	public synchronized void addShutdownHook(Runnable task) {
		shutdownHooks.add(task);
	}

	public synchronized void removeShutdownHook(Runnable task) {
		shutdownHooks.remove(task);
	}

	public synchronized void addShutdownHook(Runnable task, String title) {
		addShutdownHook(new NamedRunnable(task, title));
	}

	public synchronized void removeShutdownHook(String title) {
		requireNonNull(title);

		for(Iterator<Runnable> it = shutdownHooks.iterator(); it.hasNext();) {
			Runnable r = it.next();
			if(r instanceof StringResource) {
				String t = ((StringResource)r).getStringValue();
				if(title.equals(t)) {
					it.remove();
					return;
				}
			}
		}
	}

//	/**
//	 * Initiate a workspace change by pointing the workspace path
//	 * to a new location.
//	 *
//	 * @param workspacePath
//	 */
//	public synchronized void setWorkspacePath(Path workspacePath) {
//
//		if(Objects.equals(this.workspacePath, workspacePath)) {
//			return;
//		}
//
//		Path oldWorkspacePath = this.workspacePath;
//		this.workspacePath = workspacePath;
//
//		// Notify listeners about the general change
//		changeSupport.firePropertyChange(NAME_WORKSPACE_PATH, oldWorkspacePath, workspacePath);
//	}

	synchronized void setWorkspace(Workspace workspace) {

		if(Objects.equals(this.workspace, workspace)) {
			return;
		}

//		if(!Objects.equals(workspacePath, workspace.getFolder()))
//			throw new RDHException("Cannot change workspace instance to a new location before changing the worpsace path");

		Workspace oldWorkspace = this.workspace;
		this.workspace = workspace;

		// Notify listeners about the general change
		changeSupport.firePropertyChange(NAME_WORKSPACE, oldWorkspace, workspace);

		// Now synchronize the workspace history
		Path path = workspace.getFolder();
		String newEntry = path.toString();
		String history = getProperty(RDHProperty.CLIENT_WORKSPACE_HISTORY);
		String newHistory = null;
		if(history!=null) {
			if(!history.contains(newEntry)) {

				newHistory = history+";"+newEntry;

				if(client.isVerbose()) {
					log.info("Added {} to workspace history", newEntry);
				}
			}
		} else {
			newHistory = newEntry;

			if(client.isVerbose()) {
				log.info("Initialized workspace history with path {}", newEntry);
			}
		}

		// If history changed, fire notification
		if(newHistory!=null) {
			setProperty(RDHProperty.CLIENT_WORKSPACE_HISTORY, newHistory);
		}
	}

	public synchronized String setProperty(RDHProperty property, String value) {
		return setProperty(property.getKey(), value);
	}

	public synchronized void removeProperty(RDHProperty property) {
		removeProperty(property.getKey());
	}

	public synchronized String setProperty(String key, String value) {
//		System.out.printf("setting property key=%s value=%s\n", key, value);

		String oldValue;
		if(value==null) {
			oldValue = (String) properties.remove(key);
		} else {
			oldValue = (String) properties.setProperty(key, value);
		}
		if(!Objects.equals(oldValue, value)) {
			changeSupport.firePropertyChange(prefixedPropertyKey(key), oldValue, value);
		}
		return oldValue;
	}

	public synchronized void setProperties(Map<String, String> properties) {
		//TODO change this so that we only copy over "new" properties
		this.properties.putAll(properties);
		// Special case: we report the entirety of out properties to have changed
		Set<String> keys = new HashSet<>(properties.keySet());
		changeSupport.firePropertyChange(NAME_PROPERTIES, null, keys);
	}

	public synchronized void removeProperty(String key) {
		String oldValue = (String) properties.remove(key);
		if(oldValue!=null) {
			changeSupport.firePropertyChange(prefixedPropertyKey(key), oldValue, null);
		}
	}

	/**
	 * Package-private exposure of our internal properties so
	 * the client can save them during shutdown
	 */
	synchronized Properties getProperties() {
		return properties;
	}

	/**
	 * Executes a task through the client's {@link RDHClient#getExecutorService() executor service}.
	 */
	public synchronized void execute(Runnable task) {
		getClient().getExecutorService().execute(task);
	}

	// Listener stuff

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(listener);
	}

	@Override
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(propertyName, listener);
	}

	@Override
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(propertyName, listener);
	}

	/**
	 * Clears the list of registered listeners and empties the current properties map
	 */
	void dispose() {
		properties.clear();

		PropertyChangeListener[] listeners = changeSupport.getPropertyChangeListeners();
		for(PropertyChangeListener listener : listeners) {
			changeSupport.removePropertyChangeListener(listener);
		}
	}

	private static class NamedRunnable implements Runnable, StringResource {

		private final Runnable task;
		private final String title;

		NamedRunnable(Runnable task, String title) {
			this.task = requireNonNull(task);
			this.title = requireNonNull(title);
		}

		/**
		 * @see bwfdm.replaydh.utils.StringResource#getStringValue()
		 */
		@Override
		public String getStringValue() {
			return title;
		}

		/**
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			task.run();
		}
	}
}

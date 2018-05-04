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
package bwfdm.replaydh.workflow;

import static java.util.Objects.requireNonNull;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import bwfdm.replaydh.core.AbstractRDHTool;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.utils.RDHUtils;
import bwfdm.replaydh.workflow.Identifiable.Role;

/**
 * @author Markus Gärtner
 *
 */
public class ResourceCache extends AbstractRDHTool implements PropertyChangeListener {

	//TODO needs check and restriction for Roles (should not allow PERSON)

	/**
	 * Actual cache content, also used for synchronization
	 */
	private final Map<Object, CacheEntry> cache = new LinkedHashMap<>();

	private final List<CacheListener> listeners = new CopyOnWriteArrayList<>();

	private static Path normalize(Path path) {
		return RDHUtils.normalize(path);
	}

	/**
	 * @throws RDHLifecycleException
	 * @see bwfdm.replaydh.core.AbstractRDHTool#start(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public boolean start(RDHEnvironment environment) throws RDHLifecycleException {
		if(!super.start(environment)) {
			return false;
		}

		environment.addPropertyChangeListener(RDHEnvironment.NAME_WORKSPACE, this);

		return true;
	}

	/**
	 * @throws RDHLifecycleException
	 * @see bwfdm.replaydh.core.AbstractRDHTool#stop(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public void stop(RDHEnvironment environment) throws RDHLifecycleException {

		environment.removePropertyChangeListener(RDHEnvironment.NAME_WORKSPACE, this);

		super.stop(environment);
	}

	/**
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if(RDHEnvironment.NAME_WORKSPACE.equals(evt.getPropertyName())) {
			clear();
		}
	}

	/**
	 * Adds a resource object for the specified file to the cache
	 * if such a mapping has not been present before.
	 * Returns the new {@link CacheEntry entry} iff successful.
	 */
	public CacheEntry add(Path file, Role role, Resource resource) {
		requireNonNull(file);
		requireNonNull(role);

		// Ensure we're always dealing with absolute paths in the cache
		file = normalize(file);

		return add0(key(role, file), new CacheEntry(file, role, resource, true));
	}

	/**
	 * Adds a resource object for the specified remote location to the cache
	 * if such a mapping has not been present before.
	 * Returns the new {@link CacheEntry entry} iff successful.
	 */
	public CacheEntry add(URI uri, Role role, Resource resource) {
		requireNonNull(uri);
		requireNonNull(role);

		return add0(key(role, uri), new CacheEntry(uri, role, resource, false));
	}

	public void clear() {
		cache.clear();

		fireCacheCleared();
	}

	private static String key(Role role, Path source) {
		return role+":"+source.toString();
	}

	private static String key(Role role, URI source) {
		return role+":"+source.toString();
	}

	private CacheEntry add0(String key, CacheEntry entry) {

		boolean isNew;
		synchronized (cache) {
			isNew = cache.putIfAbsent(key, entry)==null;
		}
		if(isNew) {
			fireEntryAdded(entry);
		}
		return isNew ? entry : null;
	}

	private void fireEntryAdded(CacheEntry entry) {
		if(listeners.isEmpty()) {
			return;
		}

		for(CacheListener listener : listeners) {
			listener.entryAdded(entry);
		}
	}

	private void fireEntryRemoved(CacheEntry entry) {
		if(listeners.isEmpty()) {
			return;
		}

		for(CacheListener listener : listeners) {
			listener.entryRemoved(entry);
		}
	}

	private void fireCacheCleared() {
		if(listeners.isEmpty()) {
			return;
		}

		for(CacheListener listener : listeners) {
			listener.cacheCleared();
		}
	}

	public boolean isEmpty() {
		synchronized (cache) {
			return cache.isEmpty();
		}
	}

	public int getEntryCount() {
		synchronized (cache) {
			return cache.size();
		}
	}

	public void addCacheListener(CacheListener listener) {
		requireNonNull(listener);
		listeners.add(listener);
	}

	public void removeCacheListener(CacheListener listener) {
		requireNonNull(listener);
		listeners.remove(listener);
	}

	public interface CacheListener {

		void entryAdded(CacheEntry entry);
		void entryRemoved(CacheEntry entry);

		void cacheCleared();
	}

	/**
	 * Returns all cache entries applied
	 * @return
	 */
	public Collection<CacheEntry> getCacheEntries() {
		return Collections.unmodifiableCollection(cache.values());
	}

	public static class CacheEntry {

		/**
		 * URI or Path object
		 */
		private final Object source;

		/**
		 * Role indicator to save user decision
		 */
		private final Role role;

		/**
		 * Filled out resource instance associated with the source.
		 */
		private final Resource resource;

		/**
		 * Flag to indicate whether the source is to
		 * be interpreted as a Path or URI.
		 */
		private final boolean isFile;

		private CacheEntry(Object source, Role role, Resource resource, boolean isFile) {
			this.source = requireNonNull(source);
			this.role = requireNonNull(role);
			this.resource = requireNonNull(resource);
			this.isFile = isFile;
		}

		public Path getFile() {
			if(!isFile)
				throw new IllegalStateException("Soruce is not a file");

			return (Path) source;
		}

		public URI getURI() {
			if(isFile)
				throw new IllegalStateException("Soruce is not a URI");

			return (URI) source;
		}

		public Resource getResource() {
			return resource;
		}

		public boolean isFile() {
			return isFile;
		}

		public Role getRole() {
			return role;
		}
	}
}

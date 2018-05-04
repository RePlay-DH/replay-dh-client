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
package bwfdm.replaydh.io.resources;

import static bwfdm.replaydh.utils.RDHUtils.checkState;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Markus Gärtner
 *
 */
public class VirtualResourceProvider implements ResourceProvider {

	private final Map<Path, VirtualIOResource> resources = new HashMap<>();

	private final Set<Path> directories = new HashSet<>();

	private final Map<Path, Lock> locks = new WeakHashMap<>();

	protected VirtualIOResource createResource(Path path) {
		return new VirtualIOResource(path);
	}

	/**
	 * @see bwfdm.replaydh.io.resources.ResourceProvider#exists(java.nio.file.Path)
	 */
	@Override
	public boolean exists(Path path) {
		return resources.containsKey(path) || isDirectory(path);
	}

	/**
	 * @see bwfdm.replaydh.io.resources.ResourceProvider#create(java.nio.file.Path)
	 */
	@Override
	public boolean create(Path path) throws IOException {
		boolean hasResource = resources.containsKey(path);
		if(!hasResource) {
			resources.put(path, createResource(path));
		}

		return !hasResource;
	}

	/**
	 * @see bwfdm.replaydh.io.resources.ResourceProvider#getResource(java.nio.file.Path)
	 */
	@Override
	public IOResource getResource(Path path) throws IOException {
		checkState("No resoruce registered for path: "+path, resources.containsKey(path));
		return resources.get(path);
	}

	public void clear() {
		directories.clear();
		resources.values().forEach(VirtualIOResource::delete);
	}

	public void addDirectory(Path path) {
		directories.add(path);
	}

	public Collection<VirtualIOResource> getResources() {
		return Collections.unmodifiableCollection(resources.values());
	}

	public Set<Path> getPaths() {
		return Collections.unmodifiableSet(resources.keySet());
	}

	/**
	 * @see bwfdm.replaydh.io.resources.ResourceProvider#isDirectory(java.nio.file.Path)
	 */
	@Override
	public boolean isDirectory(Path path) {
		return directories.contains(path);
	}

	/**
	 * @see bwfdm.replaydh.io.resources.ResourceProvider#getLock(java.nio.file.Path)
	 */
	@Override
	public Lock getLock(Path path) {
		synchronized (locks) {
			Lock lock = locks.get(path);
			if(lock==null) {
				lock = new ReentrantLock();
				locks.put(path, lock);
			}
			return lock;
		}
	}

	/**
	 * @see bwfdm.replaydh.io.resources.ResourceProvider#children(java.nio.file.Path)
	 */
	@Override
	public DirectoryStream<Path> children(Path folder, String glob) throws IOException {
		VirtualDirectoryStream stream = new VirtualDirectoryStream();

		Matcher matcher = null;
		if(!"*".equals(glob)) {
			String regex = Globs.toWindowsRegexPattern(glob);

			matcher = Pattern.compile(regex).matcher("");
		}

		for(Path path : resources.keySet()) {
			if(path.equals(folder)) {
				continue;
			}

			if(path.getParent().equals(folder)) {
				if(matcher==null) {
					stream.add(path);
					continue;
				}

				matcher.reset(path.getFileName().toString());
				if(matcher.matches()) {
					stream.add(path);
				}
			}
		}

		return stream;
	}

	private static class VirtualDirectoryStream extends ArrayList<Path> implements DirectoryStream<Path> {

		private static final long serialVersionUID = -1L;

		private volatile boolean closed = false;

		/**
		 * @see java.util.ArrayList#iterator()
		 */
		@Override
		public Iterator<Path> iterator() {
			checkState("Stream closed", !closed);
			return super.iterator();
		}

		/**
		 * @see java.io.Closeable#close()
		 */
		@Override
		public void close() throws IOException {
			closed = true;
			clear();
		}

	}
}

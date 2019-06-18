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
package bwfdm.replaydh.metadata;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import bwfdm.replaydh.utils.LazyCollection;

/**
 * Models a single entry in a metadata repository
 *
 * @author Markus Gärtner
 *
 */
public interface MetadataRecord {

	/**
	 * Returns an implementation specific identifier object that can
	 * be used to uniquely identify or address this metadata record.
	 * <p>
	 * Note that this identifier is created by the {@link MetadataRepository repository}
	 * that manages this record and therefore is only really usable for
	 * this specific repository.
	 *
	 * @return
	 */
	Target getTarget();

	String getSchemaId();

	int getEntryCount();

	/**
	 * Returns a set view of all the names used for entries in
	 * this record. The returned set may or may not be linked to
	 * this record and it should always be considered read-only!
	 *
	 * @return
	 */
	Set<String> getEntryNames();

	default Set<Entry> getEntries() {
		if(getEntryCount()==0) {
			return Collections.emptySet();
		}

		LazyCollection<Entry> collector = LazyCollection.lazySet();

		forEachEntry(collector);

		return collector.getAsSet();
	}

	/**
	 * Executes given {@code action} for all entries in this record.
	 *
	 * @param action
	 */
	void forEachEntry(Consumer<? super Entry> action);

	Entry getEntry(String name);

	int getEntryCount(String name);

	default boolean hasEntries(String name) {
		return getEntryCount(name)>0;
	}

	default boolean hasMultipleEntries(String name) {
		return getEntryCount(name)>1;
	}

	default Set<Entry> getEntries(String name) {
		int entryCount = getEntryCount(name);
		switch (entryCount) {
		case 0: return Collections.emptySet();
		case 1: return Collections.singleton(getEntry(name));

		default:
			LazyCollection<Entry> collector = LazyCollection.lazySet();

			forEachEntry(name, collector);

			return collector.getAsSet();
		}

	}

	/**
	 * Executes given {@code action} for every entry that uses the
	 * specified {@code name}.
	 *
	 * @param name
	 * @param action
	 */
	void forEachEntry(String name, Consumer<? super Entry> action);

	/**
	 * A single key-value pair in a metadata record.
	 * <p>
	 * Implementation note: The {@link Object#equals(Object) equals()}
	 * contract for objects implementing this interface is as such
	 * that two entries {@code e1} and {@code e2} are considered equal
	 * when<br>
	 * {@code e1.getName().equals(e2.getName()) && e1.getValue().equals(e2.getValue())}
	 *
	 * @author Markus Gärtner
	 *
	 */
	public interface Entry {
		/**
		 * Key part
		 */
		String getName();

		/**
		 * Value part
		 */
		String getValue();
	}

	/**
	 * Pointer to a physical object as target for a metadata record in a given repository.
	 *
	 * @author Markus Gärtner
	 *
	 */
	public static final class Target implements Cloneable {
		private final String workspace, path;

		public Target(Path workspace, Path path) {
			requireNonNull(workspace);
			requireNonNull(path);
			this.workspace = workspace.toString();
			this.path = workspace.relativize(path).toString();
		}

		public Target(String workspace, String path) {
			requireNonNull(workspace);
			requireNonNull(path);
			this.workspace = workspace;
			if(path.startsWith(workspace)) {
				path = path.substring(workspace.length());
			}
			this.path = requireNonNull(path);
		}

		/**
		 * Returns the workspace path the resource associated with this record is located in.
		 * <p>
		 * The returned value might be empty in case the record has been manually created and
		 * is not associated with any physical resource yet.
		 *
		 * @return
		 */
		public String getWorkspace() {
			return workspace;
		}

		/**
		 * Returns the relative path to the resource associated with this record within
		 * its host workspace.
		 * @return
		 */
		public String getPath() {
			return path;
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if(obj==this) {
				return true;
			} else if(obj instanceof Target) {
				Target other = (Target)obj;
				return workspace.equals(other.workspace)
						&& path.equals(other.path);
			}
			return false;
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return Objects.hash(workspace, path);
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return (workspace.isEmpty() ? "<none>" : workspace) + File.separatorChar + path;
		}

		/**
		 * @see java.lang.Object#clone()
		 */
		@Override
		public Target clone() {
			try {
				return (Target) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new InternalError("Not supposed to happen", e);
			}
		}
	}
}

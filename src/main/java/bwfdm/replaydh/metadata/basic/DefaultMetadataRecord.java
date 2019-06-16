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
package bwfdm.replaydh.metadata.basic;

import static java.util.Objects.requireNonNull;

import java.rmi.server.UID;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import bwfdm.replaydh.metadata.MetadataException;
import bwfdm.replaydh.metadata.MetadataRecord;

/**
 * @author Markus Gärtner
 *
 */
public class DefaultMetadataRecord implements MutableMetadataRecord {

	private final Map<String, Set<Entry>> entries = new HashMap<>();
	private final Target target;
	private final String schemaId;

	private boolean changed = false;

	private int totalEntryCount = 0;

	/**
	 * Creates a fresh empty record and associates it with the given {@link UID target}.
	 *
	 * @param target
	 */
	public DefaultMetadataRecord(Target target, String schemaId) {
		this.target = requireNonNull(target);
		this.schemaId = requireNonNull(schemaId);
	}

	/**
	 * Copy constructor to effectively "clone" the given {@code source} record.
	 * This constructor will copy over all {@link Entry entries} from the given
	 * record and also uses its assigned {@link UID target}.
	 *
	 * @param source
	 */
	public DefaultMetadataRecord(MetadataRecord source) {
		requireNonNull(source);

		target = source.getTarget();
		schemaId = source.getSchemaId();

		copyContent(source);
	}

	private Set<Entry> entriesForName(String name, boolean createIfMissing, boolean expectNonNull) {
		Set<Entry> result = entries.get(name);

		if(result==null && createIfMissing) {
			result = new HashSet<>();
			entries.put(name, result);
		}

		if(result==null && expectNonNull)
			throw new MetadataException("No entries available for name: "+name);

		return result;
	}

	private void change() {
		changed = true;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRecord#getTarget()
	 */
	@Override
	public Target getTarget() {
		return target;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRecord#getSchemaId()
	 */
	@Override
	public String getSchemaId() {
		return schemaId;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRecord#getEntryCount()
	 */
	@Override
	public int getEntryCount() {
		//TODO verify thread-safety and whether we need a validation mechanism to keep that count up2date
		return totalEntryCount;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRecord#getEntryNames()
	 */
	@Override
	public Set<String> getEntryNames() {
		return Collections.unmodifiableSet(entries.keySet());
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRecord#forEachEntry(java.util.function.Consumer)
	 */
	@Override
	public void forEachEntry(Consumer<? super Entry> action) {
		for(Set<Entry> entriesForName : entries.values()) {
			entriesForName.forEach(action);
		}
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRecord#getEntry(java.lang.String)
	 */
	@Override
	public Entry getEntry(String name) {
		Set<Entry> entries = entriesForName(name, false, true);
		if(entries.size()>1)
			throw new MetadataException("More than 1 entry assigned for name: "+name);

		// No other way to get at the sole element of a Set than via its iterator
		Iterator<Entry> it = entries.iterator();

		return it.hasNext() ? it.next() : null;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRecord#getEntryCount(java.lang.String)
	 */
	@Override
	public int getEntryCount(String name) {
		Set<Entry> entries = entriesForName(name, false, false);
		return entries==null ? 0 : entries.size();
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRecord#forEachEntry(java.lang.String, java.util.function.Consumer)
	 */
	@Override
	public void forEachEntry(String name, Consumer<? super Entry> action) {
		Set<Entry> entries = entriesForName(name, false, false);
		if(entries!=null) {
			entries.forEach(action);
		}
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRecord#hasEntries(java.lang.String)
	 */
	@Override
	public boolean hasEntries(String name) {
		Set<Entry> entries = entriesForName(name, false, false);
		return entries==null ? false : !entries.isEmpty();
	}

	// MODIFICATION METHODS

	@Override
	public void addEntry(Entry entry) {
		requireNonNull(entry);

		addEntry0(entry);
	}

	@Override
	public void addEntry(String name, String value) {
		addEntry0(new DefaultMetadataEntry(name, value));
	}

	private void addEntry0(Entry entry) {
		if(!entriesForName(entry.getName(), true, true).add(entry))
			throw new MetadataException("Duplicate entry: "+entry);

		totalEntryCount++;
		change();
	}

	@Override
	public void removeEntry(Entry entry) {
		requireNonNull(entry);

		removeEntry0(entry);
	}

	private void removeEntry0(Entry entry) {
		String name = entry.getName();
		Set<Entry> entries = entriesForName(name, false, true);

		if(!entries.remove(entry))
			throw new MetadataException("Unknown entry: "+entry);

		totalEntryCount--;

		// Clear up our global map if the buffer for a certain name is no longer needed
		if(entries.isEmpty()) {
			this.entries.remove(name);
		}
		change();
	}

	@Override
	public void removeAllEntries() {
		entries.clear();
		totalEntryCount = 0;
		change();
	}

	@Override
	public void removeAllEntries(String name) {
		requireNonNull(name);

		Set<Entry> entries = entriesForName(name, false, true);
		this.entries.remove(name);
		totalEntryCount -= entries.size();
		change();
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return target.hashCode();
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MetadataRecord@"+target;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		} else if(obj instanceof MetadataRecord) {
			MetadataRecord other = (MetadataRecord) obj;
			return target.equals(other.getTarget());
		}
		return false;
	}

	@Override
	public boolean hasChanged() {
		return changed;
	}

	@Override
	public void markUnchanged() {
		changed = true;
	}
}

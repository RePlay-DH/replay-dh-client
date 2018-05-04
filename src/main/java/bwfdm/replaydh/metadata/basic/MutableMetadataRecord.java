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

import java.util.Collection;

import bwfdm.replaydh.metadata.MetadataBuilder;
import bwfdm.replaydh.metadata.MetadataRecord;

/**
 * Extends the basic {@link MetadataRecord} interface with methods to
 * allow external modifications without making other components like
 * {@link MetadataBuilder builders} too dependent on the default
 * {@link DefaultMetadataRecord} implementation.
 *
 * @author Markus Gärtner
 *
 */
public interface MutableMetadataRecord extends MetadataRecord {

	void removeAllEntries(String name);

	void removeAllEntries();

	default void removeAllEntries(Collection<? extends Entry> entries) {
		requireNonNull(entries);

		entries.forEach(this::removeEntry);
	}

	void removeEntry(Entry entry);

	default void addAllEntries(Collection<? extends Entry> newEntries) {
		requireNonNull(newEntries);

		newEntries.forEach(this::addEntry);
	}

	default void addEntry(String name, String value) {
		addEntry(new DefaultMetadataEntry(name, value));
	}

	void addEntry(Entry entry);

	/**
	 * Clears this record and then copies all entries from the given
	 * {@code source}.
	 * @param source
	 */
	//TODO check if we rly need the removeAllEntries() call or if we should leave that decision to client code
	default void copyContent(MetadataRecord source) {
		requireNonNull(source);

		removeAllEntries();

		source.forEachEntry(this::addEntry);
	}

}

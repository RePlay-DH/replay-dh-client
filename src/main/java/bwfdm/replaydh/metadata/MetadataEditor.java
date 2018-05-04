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

import java.util.Collection;

import bwfdm.replaydh.metadata.MetadataRecord.Entry;

/**
 * Models the interface a metadata repository provides for manipulating
 * existing entries.
 *
 * @author Markus Gärtner
 *
 */
public interface MetadataEditor extends MetadataSchema {

	/**
	 * Notifies the editor that client code wishes to actually
	 * start editing metadata entries for the linked resource.
	 */
	MetadataEditor start();

	/**
	 * @param name
	 * @param value
	 * @return
	 *
	 * @throws MetadataException if either {@code name} or {@code value}
	 * denotes a (currently) not allowed String value.
	 */
	MetadataEditor addEntry(String name, String value);

	MetadataEditor addEntry(Entry entry);

	MetadataEditor removeEntry(Entry entry);

	/**
	 * Complete clears the underlying record.
	 *
	 * @return
	 */
	MetadataEditor removeAllEntries();

	/**
	 * Removes the given collection of entries from the underlying record.
	 *
	 * @param entries
	 * @return
	 */
	default MetadataEditor removeAllEntries(Collection<Entry> entries) {
		entries.forEach(this::removeEntry);
		return this;
	}

	/**
	 * Removes all entries for the given name.
	 *
	 * @param name
	 * @return
	 */
	MetadataEditor removeAllEntries(String name);

	/**
	 * Returns the current state of the record under modification.
	 * Note that this does <b>not</b> necessarily return the original
	 * {@link MetadataRecord record} that was passed to the editor.
	 * Implementations are free to use temporary record instances
	 * for the modification process and only copy over the changes
	 * to the original record when the process is {@link #commit() committed}.
	 *
	 * @return
	 */
	MetadataRecord getMetadataRecord();

	/**
	 * Reverts all the changes made so far and puts the record back
	 * in its original state.
	 * <p>
	 * An editor shouldn't be used further after invoking this method.
	 */
	void discard();

	/**
	 * Persists the changes made so far so that they are reflected
	 * in the original record this editor has been created for.
	 * <p>
	 * An editor shouldn't be used further after invoking this method.
	 */
	void commit();
}

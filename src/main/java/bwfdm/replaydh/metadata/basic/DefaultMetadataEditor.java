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

import bwfdm.replaydh.metadata.MetadataEditor;
import bwfdm.replaydh.metadata.MetadataException;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRecord.Entry;
import bwfdm.replaydh.metadata.MetadataSchema;

/**
 * This implementation uses a "working copy" of the original record
 * on which all the intermediary manipulations take place. Only after
 * {@link #commit() committing} will the modifications carry over to
 * the original record.
 *
 * @author Markus Gärtner
 *
 */
public class DefaultMetadataEditor extends DelegatingMetadataSchema implements MetadataEditor {

	/**
	 * The original record to be modified
	 */
	private final MutableMetadataRecord record;

	/**
	 * The "working copy" of the original record.
	 * All modifications will be first performed
	 * on this copy and only when {@link #commit()}
	 * is called will the changes carry over.
	 */
	private final MutableMetadataRecord mutex;

	private volatile boolean started = false;

	public DefaultMetadataEditor(MetadataSchema verifier, MutableMetadataRecord record) {
		super(verifier);
		requireNonNull(record);

		this.record = record;
		mutex = new DefaultMetadataRecord(record);
	}

	/**
	 * Callback for subclasses, invoked at the beginning of a
	 * {@link #start()} call.
	 */
	protected void beforeStart() {
		// no-op
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataEditor#start()
	 */
	@Override
	public MetadataEditor start() {
		beforeStart();

		started = true;

		return this;
	}

	protected void checkStarted() {
		if(!started)
			throw new MetadataException("Builing process not started for resource: "+getOriginalMetadataRecord().getTarget());
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataEditor#addEntry(java.lang.String, java.lang.String)
	 */
	@Override
	public MetadataEditor addEntry(String name, String value) {
		checkStarted();
		MetadataSchema.checkCanAdd(this, mutex, name, value);

		mutex.addEntry(name, value);

		return this;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataEditor#addEntry(bwfdm.replaydh.metadata.MetadataRecord.Entry)
	 */
	@Override
	public MetadataEditor addEntry(Entry entry) {
		requireNonNull(entry);
		checkStarted();

		MetadataSchema.checkCanAdd(this, mutex, entry.getName(), entry.getValue());

		mutex.addEntry(entry);

		return this;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataEditor#removeEntry(bwfdm.replaydh.metadata.MetadataRecord.Entry)
	 */
	@Override
	public MetadataEditor removeEntry(Entry entry) {
		requireNonNull(entry);
		checkStarted();

		mutex.removeEntry(entry);

		return this;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataEditor#removeAllEntries()
	 */
	@Override
	public MetadataEditor removeAllEntries() {
		checkStarted();
		mutex.removeAllEntries();

		return this;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataEditor#removeAllEntries(java.lang.String)
	 */
	@Override
	public MetadataEditor removeAllEntries(String name) {
		requireNonNull(name);
		checkStarted();

		mutex.removeAllEntries(name);

		return this;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataEditor#getMetadataRecord()
	 */
	@Override
	public MetadataRecord getMetadataRecord() {
		return mutex;
	}

	protected MetadataRecord getOriginalMetadataRecord() {
		return record;
	}

	/**
	 * Callback for subclasses, invoked at the beginning of a
	 * {@link #discard()} or {@link #commit()} call.
	 */
	protected void beforeEndEdit(boolean discard) {
		// no-op
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataEditor#discard()
	 */
	@Override
	public void discard() {
		checkStarted();

		beforeEndEdit(true);

		// Copy over the initial data from mutex record (this removes all edits performed in the meantime)
		mutex.copyContent(record);

		afterEndEdit(true);
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataEditor#commit()
	 */
	@Override
	public void commit() {
		checkStarted();

		beforeEndEdit(false);

		// Will fail if record does not confine to minimal requirements anymore
		MetadataSchema.checkIsComplete(this, mutex);

		// Everything verified and ok -> copy content to original record
		record.copyContent(mutex);

		afterEndEdit(false);
	}


	/**
	 * Callback for subclasses, invoked at the end of a
	 * {@link #discard()} or {@link #commit()} call.
	 */
	protected void afterEndEdit(boolean discard) {
		// no-op
	}
}

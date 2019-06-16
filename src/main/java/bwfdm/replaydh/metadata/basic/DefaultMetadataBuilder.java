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

import bwfdm.replaydh.metadata.MetadataBuilder;
import bwfdm.replaydh.metadata.MetadataException;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRecord.Entry;
import bwfdm.replaydh.metadata.MetadataSchema;

/**
 * @author Markus Gärtner
 *
 */
public class DefaultMetadataBuilder extends DelegatingMetadataSchema implements MetadataBuilder {


	private final MutableMetadataRecord record;

	private volatile boolean started = false;

	public DefaultMetadataBuilder(MetadataSchema verifier, MutableMetadataRecord record) {
		super(verifier);
		requireNonNull(record);

		this.record = record;
	}

	/**
	 * Callback for subclasses, invoked at the beginning of a
	 * {@link #start()} call.
	 */
	protected void beforeStartBuild() {
		// no-op
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataBuilder#start()
	 */
	@Override
	public MetadataBuilder start() {
		beforeStartBuild();

		started = true;

		return this;
	}

	protected void checkStarted() {
		if(!started)
			throw new MetadataException("Builing process not started for resource: "+getRecord().getTarget());
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataBuilder#addEntry(bwfdm.replaydh.metadata.MetadataRecord.Entry)
	 */
	@Override
	public MetadataBuilder addEntry(Entry entry) {
		requireNonNull(entry);
		checkStarted();

		MetadataSchema.checkCanAdd(this, record, entry.getName(), entry.getValue());

		record.addEntry(entry);

		return this;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataBuilder#newEntry(java.lang.String, java.lang.String)
	 */
	@Override
	public MetadataBuilder addEntry(String name, String value) {
		checkStarted();
		MetadataSchema.checkCanAdd(this, record, name, value);

		record.addEntry(name, value);

		return this;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataBuilder#reset()
	 */
	@Override
	public void reset() {
		record.removeAllEntries();
	}

	protected MutableMetadataRecord getRecord() {
		return record;
	}

	/**
	 * Callback for subclasses, invoked at the beginning of a
	 * {@link #build()} or {@link #cancel()} call.
	 */
	protected void beforeEndBuild(boolean cancel) {
		// no-op
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataBuilder#build()
	 */
	@Override
	public MetadataRecord build() {
		checkStarted();
		beforeEndBuild(false);

		MetadataSchema.checkIsComplete(this, record);

		return record;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataBuilder#cancel()
	 */
	@Override
	public void cancel() {
		checkStarted();
		beforeEndBuild(true);

		record.removeAllEntries();
	}
}

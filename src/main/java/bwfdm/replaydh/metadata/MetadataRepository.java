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

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.core.RDHTool;
import bwfdm.replaydh.metadata.MetadataRecord.Target;
import bwfdm.replaydh.utils.SchemaManager;

/**
 * @author Markus Gärtner
 *
 */
public interface MetadataRepository extends RDHTool, SchemaManager<MetadataSchema> {

	String getDisplayName();

	// Lifecycle API

	/**
	 * Initializes this repository adapter so that subsequent calls to read methods
	 * can access its data.
	 *
	 * @param environment
	 */
	@Override
	boolean start(RDHEnvironment environment) throws RDHLifecycleException;

	/**
	 * Shuts down this repository adapter and releases all currently loaded records.
	 */
	@Override
	void stop(RDHEnvironment environment) throws RDHLifecycleException;

	// Transaction API

	void beginUpdate();

	void endUpdate();

	// Read API

	boolean hasRecords(Target target);

	/**
	 * Returns a human-readable {@code String} that is suitable for displaying
	 * the given record.
	 * <p>
	 * Responsibility of this task has been moved to the repository implementations
	 * due to them having knowledge of the particular metadata schemes. This way
	 * they can more efficiently decide on a field in the given record to be used
	 * as <i>display name</i>.
	 *
	 * @param record
	 * @return
	 */
	String getDisplayName(MetadataRecord record);

	RecordIterator getAvailableRecords();

	/**
	 * Fetches the actual set of metadata associated with a given {@code target}
	 * based on a specific metadata schema.
	 *
	 * @param target
	 * @return
	 */
	MetadataRecord getRecord(Target target, String schemaId);

	/**
	 * Fetches all records for the given {@code target}. The returned collection will
	 * either be empty or contain {@code 1} record for each metadata schema that was
	 * used to create metadata for the {@code target}.
	 *
	 * @param target
	 * @return
	 */
	Collection<MetadataRecord> getRecords(Target target);

	void addRecord(MetadataRecord record);

	void removeRecord(MetadataRecord record);

	// Listener API

	void addMetadataListener(MetadataListener listener);

	void removeMetadataListener(MetadataListener listener);


	// Build & Modification API

	/**
	 * Checks whether or not the given record is allowed to
	 * be {@link #createEditor(MetadataRecord) edited} by client
	 * code or the user.
	 * <p>
	 * Per default all records are editable.
	 *
	 * @param record
	 * @return
	 */
	default boolean isRecordEditable(MetadataRecord record) {
		return true;
	}

	/**
	 * Creates a new {@link MetadataBuilder} instance suitable for creating
	 * a {@link MetadataRecord} for the given {@code target}.
	 *
	 * @param target
	 * @return
	 *
	 * @throws MissingIdentifierException if the resource does not provide enough identifier
	 * information for this repository to construct a valid {@link Target}
	 * @throws MetadataException if the {@code resource} is already registered with this repository
	 *
	 * @see #createTarget(Resource)
	 */
	MetadataBuilder createBuilder(Target target, String schemaId);

	/**
	 * Creates a new {@link MetadataEditor} instance that can be used
	 * to change the content of the given {@link MetadataRecord}.
	 *
	 * @param record
	 * @return
	 */
	MetadataEditor createEditor(MetadataRecord record);

	String toSimpleText(MetadataRecord record);

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	public interface RecordIterator extends Iterator<Target>, AutoCloseable {
		/**
		 * @see java.lang.AutoCloseable#close()
		 */
		@Override
		void close() throws IOException;
	}
}

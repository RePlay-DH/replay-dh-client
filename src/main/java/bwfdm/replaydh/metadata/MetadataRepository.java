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
import java.util.List;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.core.RDHTool;
import bwfdm.replaydh.metadata.MetadataRecord.UID;
import bwfdm.replaydh.utils.LookupResult;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Resource;

/**
 * @author Markus Gärtner
 *
 */
public interface MetadataRepository extends RDHTool{

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

	/**
	 * Equivalent of a collection {@link Collection#isEmpty() isEmpty()} method
	 * that just tells the client code whether or not there are <i>any</i>
	 * records already contained in this repository.
	 *
	 * @return
	 */
	boolean hasRecords();

	/**
	 * Checks whether or not this repository contains a record for the given
	 * {@link Identifiable}.
	 * <p>
	 * The default implementation just delegates to {@link #getUID(Identifiable)}
	 * and checks the returned value to not be {@code null}.
	 *
	 * @param resource
	 * @return
	 */
	default boolean hasRecord(Identifiable resource) {
		return getUID(resource)!=null;
	}

	boolean hasRecord(UID uid);

	/**
	 * Optional method to create a (human readable) unique name associated
	 * with the given {@link UID}. If the returned value is {@code non-null}
	 * then it is guaranteed to uniquely identify the specified {@code uid}
	 * and can for example be used as a file name when storing metadata records.
	 * <p>
	 * The default implementation returns {@code null}.
	 *
	 * @param uid
	 * @return
	 */
	@Deprecated
	default String getUniqueName(UID uid) {
		return null;
	}

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

	/**
	 * Returns the unique {@link UID} that is associated with the given {@code resource}
	 * or {@code null} if the resource is unknown to this repository.
	 *
	 * @param resource
	 * @return
	 */
	UID getUID(Identifiable resource);

	default List<LookupResult<UID, Identifiable>> resolve(Identifiable resource, int candidateLimit) {
		//TODO
		throw new UnsupportedOperationException("Not implemented");
	}

	//TODO add some sort of query mechanism to filter the set of returned record UIDs
	RecordIterator getAvailableRecords();

	/**
	 * Fetches the actual set of metadata associated with a given {@code uid}.
	 *
	 * @param uid
	 * @return
	 */
	MetadataRecord getRecord(UID uid);

	/**
	 * Tries to fetch the {@link MetadataRecord record} for a given {@link Identifiable}
	 * and returns it. If the given {@code identifiable} is currently unknown to the
	 * repository, then {@code null} is returned.
	 *
	 * @param resource
	 * @return
	 */
	default MetadataRecord getRecord(Identifiable resource) {
		UID uid = getUID(resource);
		return uid==null ? null : getRecord(uid);
	}

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
	 * @param record
	 * @return
	 */
	default boolean isRecordEditable(MetadataRecord record) {
		return true;
	}

	/**
	 * Creates a new {@link MetadataBuilder} instance suitable for creating
	 * a {@link MetadataRecord} for the given {@code resource}.
	 * <p>
	 * Note that certain repository implementations
	 *
	 * @param resource
	 * @return
	 *
	 * @throws MissingIdentifierException if the resource does not provide enough identifier
	 * information for this repository to construct a valid {@link UID}
	 * @throws MetadataException if the {@code resource} is already registered with this repository
	 *
	 * @see #createUID(Resource)
	 */
	MetadataBuilder createBuilder(Identifiable resource);

	/**
	 * Creates a new {@link MetadataEditor} instance that can be used
	 * to change the content of the given {@link MetadataRecord}.
	 *
	 * @param record
	 * @return
	 */
	MetadataEditor createEditor(MetadataRecord record);

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	public interface RecordIterator extends Iterator<UID>, AutoCloseable {
		/**
		 * @see java.lang.AutoCloseable#close()
		 */
		@Override
		void close() throws IOException;
	}
}

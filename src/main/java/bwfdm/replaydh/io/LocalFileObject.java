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
package bwfdm.replaydh.io;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import bwfdm.replaydh.core.RDHClient;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.io.resources.FileResource;
import bwfdm.replaydh.io.resources.IOResource;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.utils.LazyCollection;
import bwfdm.replaydh.workflow.Checksum;
import bwfdm.replaydh.workflow.Checksums;
import bwfdm.replaydh.workflow.Checksums.ChecksumType;
import bwfdm.replaydh.workflow.Checksums.ChecksumValidationResult;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.impl.DefaultResource;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public class LocalFileObject implements Comparable<LocalFileObject> {

	private static WorkflowSchema getSchema(RDHEnvironment environment) {
		WorkflowSchema schema = environment.getClient().getWorkflowSchemaManager().getDefaultSchema();
		if(schema==null) {
			//FIXME should not be necessary
			schema = WorkflowSchema.getDefaultSchema();
		}
		return schema;
	}

	/**
	 * Converts a collection of file object wrappers back into regular file path instances.
	 */
	public static Set<Path> extractFiles(Collection<LocalFileObject> fileObjects) {
		LazyCollection<Path> result = LazyCollection.lazySet();

		for(LocalFileObject fileObject : fileObjects) {
			result.add(fileObject.getFile());
		}

		return result.getAsSet();
	}

	/**
	 * Tries to create a new {@link ChecksumType#MD5 MD5} checksum for the
	 * specified file object if needed.
	 *
	 * @param fileObject
	 * @return {@code true} iff a fresh checksum had to be calculated
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static boolean ensureOrValidateChecksum(LocalFileObject fileObject)
			throws IOException, InterruptedException {
		requireNonNull(fileObject);

		// Nothing to do here when we have no actual physical file
		if(!Files.exists(fileObject.file, LinkOption.NOFOLLOW_LINKS)) {
			return false;
		}

		boolean needsNewChecksum;

		synchronized (fileObject.lock) {
			fileObject.startUpdate();

			try {
				IOResource resource = null;

				needsNewChecksum = fileObject.checksum==null;

				if(!needsNewChecksum) {
					resource = new FileResource(fileObject.file);
					needsNewChecksum = Checksums.validateChecksum(resource, fileObject.checksum)!=ChecksumValidationResult.VALID;
				}

				if(needsNewChecksum) {
					if(resource==null) {
						resource = new FileResource(fileObject.file);
					}

					fileObject.checksum = Checksums.createChecksum(resource, ChecksumType.MD5);
				}

			} finally {
				fileObject.endUpdate();
			}
		}

		return needsNewChecksum;
	}

	/**
	 * Tries to create and add new {@link Identifier identifiers} of type
	 * {@link DefaultResourceIdentifierType#PATH} and {@link DefaultResourceIdentifierType#CHECKSUM}
	 * if needed.
	 *
	 * @param fileObject
	 * @param environment
	 * @return {@code true} iff the internal set of identifiers has been freshly loaded
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static boolean ensureOrRefreshIdentifiers(LocalFileObject fileObject, RDHEnvironment environment)
			throws IOException, InterruptedException {
		requireNonNull(fileObject);
		requireNonNull(environment);

		boolean needsNewIdentifiers;

		synchronized (fileObject.lock) {
			fileObject.startUpdate();
			try {
				needsNewIdentifiers= ensureOrValidateChecksum(fileObject)
						|| fileObject.identifiers.isEmpty();

				if(needsNewIdentifiers) {
					fileObject.identifiers.clear();

					Path file = fileObject.file;
					String disambiguationContext = null;

					// Make sure we can handle files outside current workspace
					Path workspace = environment.getWorkspace().getFolder();
					if(file.startsWith(workspace)) {
						file = IOUtils.relativize(workspace, file);
						disambiguationContext = workspace.toString();
					}

					WorkflowSchema schema = getSchema(environment);

					// Add PATH identifier
					fileObject.identifiers.add(new Identifier(schema.getDefaultPathIdentifierType(),
							file.toString(), disambiguationContext));

					if(fileObject.checksum!=null) {
						// Add CHECKSUM identifier
						fileObject.identifiers.add(new Identifier(schema.getDefaultChecksumIdentifierType(),
								fileObject.checksum.toString()));
					}
				}
			} finally {
				fileObject.endUpdate();
			}
		}

		return needsNewIdentifiers;
	}

	/**
	 * Tries to refresh the {@link Identifiable resource} for the specified file object from
	 * the {@link RDHClient#getResourceResolver() resource resolver} using the identifiers
	 * created in {@link #ensureOrRefreshIdentifiers(LocalFileObject, RDHEnvironment)}.
	 *
	 * @param fileObject
	 * @param environment
	 * @return {@code true} iff the resource associated with this file has been freshly loaded
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static boolean ensureOrRefreshResource(LocalFileObject fileObject, RDHEnvironment environment)
			throws IOException, InterruptedException {

		requireNonNull(fileObject);
		requireNonNull(environment);

		boolean needsNewResource;

		synchronized (fileObject.lock) {
			fileObject.startUpdate();
			try {
				needsNewResource = ensureOrRefreshIdentifiers(fileObject, environment)
						|| fileObject.resource==null;

				if(needsNewResource) {
					fileObject.resource = DefaultResource.withIdentifiers(fileObject.identifiers);
				}
			} finally {
				fileObject.endUpdate();
			}
		}

		return needsNewResource;
	}

	/**
	 * Tries to refresh the {@link MetadataRecord record} for the specified file object from the
	 * {@link RDHClient#getLocalMetadataRepository() local repository} if needed.
	 *
	 * @param fileObject
	 * @param environment
	 * @return {@code true} iff the associated metadata record has been freshly loaded
	 * @throws IOException
	 * @throws InterruptedException
	 */
//	public static boolean ensureOrRefreshRecord(LocalFileObject fileObject, RDHEnvironment environment)
//			throws IOException, InterruptedException {
//
//		requireNonNull(fileObject);
//		requireNonNull(environment);
//
//		boolean needsNewRecord;
//
//		synchronized (fileObject.lock) {
//			fileObject.startUpdate();
//			try {
//				needsNewRecord = ensureOrRefreshResource(fileObject, environment)
//						|| fileObject.record==null;
//
//				if(needsNewRecord) {
//					fileObject.record = null;
//
//					if(fileObject.resource!=null) {
//						MetadataRepository repository = environment.getClient().getLocalMetadataRepository();
//						String workspace = environment.getWorkspacePath().toString();
//
//						Target target = new Target(workspace, fileObject.file.toString());
//
//						fileObject.record = repository.getRecord(target);
//					}
//				}
//			} finally {
//				fileObject.endUpdate();
//			}
//		}
//
//		return needsNewRecord;
//	}

	/**
	 * The local resource described by this wrapper object.
	 */
	private final Path file;

	/**
	 * Used for synchronization between the static modification methods.
	 */
	private final Object lock = new Object();

	/**
	 * The identifiers computed from locally available information.
	 */
	private final Set<Identifier> identifiers;

	/**
	 * The status associated with the file
	 */
	private final TrackingStatus trackingStatus;

	/**
	 * Checksum, to be computed lazily
	 */
	private Checksum checksum;

	/**
	 * Resource the file has been resolved to. Either
	 * an existing registered resource or a freshly
	 * created instance.
	 */
	private Resource resource;

	/**
	 * Associated set of metadata for this file.
	 */
	private MetadataRecord record;

	private State state = State.UNKNOWN;

//	/**
//	 * Result of the attempt to resolve the local file to an existing
//	 * resource by means of an available {@link IdentifiableResolver}.
//	 */
//	private LookupResult<Identifiable, Set<Identifier>> lookupResult;

	public LocalFileObject(Path file) {
		this(file, TrackingStatus.UNKNOWN);
	}

	public LocalFileObject(Path file, TrackingStatus trackingStatus) {
		this.file = requireNonNull(file);
		this.trackingStatus = requireNonNull(trackingStatus);

		identifiers = new HashSet<>();
	}

	private void startUpdate() {
		state = State.LOADING;
	}

	private void endUpdate() {
		state = State.FINISHED;
	}

	public boolean isLoading() {
		return state == State.LOADING;
	}

	public Path getFile() {
		return file;
	}

	public Set<Identifier> getIdentifiers() {
		return Collections.unmodifiableSet(identifiers);
	}

	public TrackingStatus getTrackingStatus() {
		return trackingStatus;
	}

	public Checksum getChecksum() {
		return checksum;
	}

	public Resource getResource() {
		return resource;
	}

//	public LookupResult<Identifiable, Set<Identifier>> getLookupResult() {
//		return lookupResult;
//	}

//	public void setChecksum(Checksum checksum) {
//		checkState("Checksum already set", this.checksum!=null);
//
//		this.checksum = requireNonNull(checksum);
//	}

//	public void setLookupResult(LookupResult<Identifiable, Set<Identifier>> lookupResult) {
//		checkState("Lookup result already set", this.lookupResult!=null);
//
//		this.lookupResult = requireNonNull(lookupResult);
//	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(LocalFileObject other) {
		return file.compareTo(other.file);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return file.toString();
	}

	public enum State {
		UNKNOWN,
		LOADING,
		FINISHED,
		;
	}
}

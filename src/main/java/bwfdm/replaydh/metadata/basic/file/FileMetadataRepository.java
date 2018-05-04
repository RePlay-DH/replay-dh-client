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
package bwfdm.replaydh.metadata.basic.file;

import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.io.resources.FileResourceProvider;
import bwfdm.replaydh.io.resources.IOResource;
import bwfdm.replaydh.io.resources.IOWorker;
import bwfdm.replaydh.io.resources.ResourceProvider;
import bwfdm.replaydh.metadata.MetadataException;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRecord.UID;
import bwfdm.replaydh.metadata.MetadataRepository;
import bwfdm.replaydh.metadata.MetadataSchema;
import bwfdm.replaydh.metadata.ValueRestriction;
import bwfdm.replaydh.metadata.basic.AbstractMetadataRespository;
import bwfdm.replaydh.metadata.basic.DefaultMetadataSchema;
import bwfdm.replaydh.metadata.basic.DefaultUID;
import bwfdm.replaydh.metadata.basic.DublinCoreField;
import bwfdm.replaydh.metadata.basic.DublinCoreSchema11;
import bwfdm.replaydh.metadata.basic.MetadataRecordCache;
import bwfdm.replaydh.metadata.basic.MutableMetadataRecord;
import bwfdm.replaydh.metadata.basic.TypedUID;
import bwfdm.replaydh.metadata.basic.UIDStorage;
import bwfdm.replaydh.metadata.basic.VirtualUIDStorage;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.utils.RDHUtils;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.schema.IdentifierType;
import bwfdm.replaydh.workflow.schema.IdentifierType.Uniqueness;

/**
 * Implements a {@link MetadataRepository repository} that stores records
 * as local text files inside a given folder.
 * <p>
 * TODO
 *
 * @author Markus Gärtner
 *
 */
public class FileMetadataRepository extends AbstractMetadataRespository {

	public static Builder newBuilder() {
		return new Builder();
	}

	private static final Logger log = LoggerFactory.getLogger(FileMetadataRepository.class);

	private final Path rootFolder;

	private final MetadataRecordCache cache;

	private final IOWorker<? super MutableMetadataRecord> reader;
	private final IOWorker<? super MetadataRecord> writer;

	private final UIDStorage uidStorage;

	private final ResourceProvider resourceProvider;

	private final Function<MetadataRecord, String> nameGenerator;

	public static final String DEFAULT_RECORD_FILE_ENDING = ".mdr.xml";

	private static final String UID_OWNER_ID = "RDH-Local";

//	private final ChangeHandler changeHandler;

	/**
	 * Specialized schema that requires strings for names and values to be
	 * at least 2 characters in length and not contain the assignment symbol '='.
	 */
	private static final MetadataSchema sharedVerifier;
	static {
		String pattern = "^[^=]+$";
		ValueRestriction restriction = ValueRestriction.forPattern(pattern, 2);

		DefaultMetadataSchema verifier = new DefaultMetadataSchema();
		verifier.setNameRestriction(restriction);
		verifier.setValueRestriction(restriction);

		sharedVerifier = verifier;
	}

	/**
	 * Alternative source of new {@link MetadataSchema} instances for edit purposes.
	 */
	private final Function<MetadataRecord, MetadataSchema> editVerifierSource;

	/**
	 * Alternative source of new {@link MetadataSchema} instances for build purposes.
	 */
	private final Function<Identifiable, MetadataSchema> buildVerifierSource;

	protected FileMetadataRepository(Builder builder) {

		// Redundant check just to be sure
		builder.validate();

		rootFolder = builder.getRootFolder();
		editVerifierSource = builder.getEditVerifierSource();
		buildVerifierSource = builder.getBuildVerifierSource();
		cache = builder.getCache();
		reader = builder.getReader();
		writer = builder.getWriter();
		uidStorage = builder.getUidStorage();
		resourceProvider = builder.getResourceProvider();

		Function<MetadataRecord, String> nameGenerator = builder.getNameGenerator();
		if(nameGenerator==null) {
			nameGenerator = record -> record.getUID().toString();
		}

		this.nameGenerator = nameGenerator;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		return ResourceManager.getInstance().get("replaydh.app.localMetadataRepository.displayName");
	}

	/**
	 * @throws RDHLifecycleException
	 * @see bwfdm.replaydh.metadata.MetadataRepository#start(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public boolean start(RDHEnvironment environment) throws RDHLifecycleException {
		if(!super.start(environment)) {
			return false;
		}

		try {
			resourceProvider.create(rootFolder);
		} catch (IOException e) {
			log.error("Failed to create storage directory for metadata records: {}",  rootFolder);
			return false;
		}

		// Make sure we only count as valid if the designated file actually denotes a directory
		return resourceProvider.isDirectory(rootFolder);
	}

	/**
	 * @throws RDHLifecycleException
	 * @see bwfdm.replaydh.metadata.MetadataRepository#stop(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public void stop(RDHEnvironment environment) throws RDHLifecycleException {
		if(isTransactionInProgress())
			throw new MetadataException("Cannot stop repository with an active transaction in progress");

		// Make sure we have no pending builds or edits
		cleanupPendingBuildsAndEdits();

		// Remove all data from live cache
		cache.clear();

		// TODO shut down other caches/resources

		super.stop(environment);
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#hasRecords()
	 */
	@Override
	public boolean hasRecords() {

		// Easiest case: some records already loaded into live cache
		if(!cache.isEmpty()) {
			return true;
		}

		// Some known mappings for resources already exist
		if(!uidStorage.isEmpty()) {
			return true;
		}

		// Use stream to check if there's at least 1 record file
		try(DirectoryStream<Path> files = resourceProvider.children(rootFolder, "*"+DEFAULT_RECORD_FILE_ENDING)) {

			if(files.iterator().hasNext()) {
				return true;
			}
		} catch (IOException e) {
			log.error("Failed to check directory {} for record files", rootFolder, e);
		}

		// Nothing in cache and no record files in root folder
		return false;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#hasRecord(bwfdm.replaydh.metadata.MetadataRecord.UID)
	 */
	@Override
	public boolean hasRecord(UID uid) {
		checkUID(uid);
		Path file = getFile(uid);

		return resourceProvider.exists(file);
	}

	public void clearCache() {
		cache.clear();
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#getDisplayName(bwfdm.replaydh.metadata.MetadataRecord)
	 */
	@Override
	public String getDisplayName(MetadataRecord record) {
		return nameGenerator.apply(record);
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#getUID(bwfdm.replaydh.workflow.Identifiable)
	 */
	@Override
	public UID getUID(Identifiable resource) {
		// This assumes that the UID storage is kept in sync with our underlying record storage
		UID uid = uidStorage.getUID(resource);

		/*
		 * If the UID storage does not know about the resource,
		 * let's try to construct a new UID and check if there
		 * is a corresponding file. In that case we can safely
		 * return the "temporary" UID.
		 */
		if(uid==null) {
			try {
				// Fetch "temporary" UID and matching file
				uid = createUID(resource);
				Path file = getFile(uid);

				// If the file does not exist, we assume the resource is unknown to us
				if(!resourceProvider.exists(file)) {
					uid = null;
				}
			} catch(RDHException e) {
				// No real error (resource can simple have no strong enough identifier yet), but for completeness sake we log it
				log.info("Failed to check for metadata file existance of resource: {}", resource, e);
			}
		}

		return uid;
	}

	private UID getUID(Path path) {
		String fileName = path.getFileName().toString();
		int split = fileName.indexOf('.');
		String systemIdPart = fileName.substring(0, split);
		UUID systemId = UUID.fromString(systemIdPart);

		return new DefaultUID(UID_OWNER_ID, systemId);
	}

	/**
	 * This implementation picks the {@link Identifier} provided by the given
	 * {@link Identifiable} that has the strongest {@link Uniqueness} associated
	 * with it. If such an identifier is available it will then try to create
	 * the required {@link RDHUtils#createDisambiguationContext(Identifier, RDHEnvironment) disambiguation context}
	 * so that the returned {@link TypedUID} provides a meaningful globally unique
	 * way of identification.
	 *
	 * @see bwfdm.replaydh.metadata.basic.AbstractMetadataRespository#createUID(bwfdm.replaydh.workflow.Identifiable)
	 *
	 * @throws RDHException if the resource provides no sufficiently unique identifier
	 */
	@Override
	protected DefaultUID createUID(Identifiable resource) {
		requireNonNull(resource);

//		// Try to find the best identifier for the resource
//		Identifier identifier = getRelevantIdentifier(resource, Uniqueness.LOCALLY_UNIQUE);
//
//		// No way of creating a "weak" UID
//		if(identifier==null)
//			throw new RDHException("No identifier with sufficient uniqueness provided for resource: "
//						+RDHUtils.getCompleteDisplayName(resource));
//
//		String disambiguationContext = RDHUtils.createDisambiguationContext(identifier, getEnvironment());
//		if(disambiguationContext==null) {
//			disambiguationContext = TypedUID.NULL_CONTEXT;
//		}
//
//		return new TypedUID(UID_OWNER_ID, identifier.getType(), identifier.getId(), disambiguationContext);

		return new DefaultUID(UID_OWNER_ID, resource.getSystemId());
	}

	/**
	 * Tries to find the {@link Identifier} for the given {@code resource} that
	 * provides the highest level of {@link Uniqueness}.
	 * Returns {@code null} if either none of the identifiers available for
	 * the resource are known to the framework, the resource has no identifiers
	 * or none of them provides a meaningful uniqueness.
	 *
	 * @param resource
	 * @return
	 */
	private Identifier getRelevantIdentifier(Identifiable resource, Uniqueness minimalUniqueness) {
		Set<Identifier> identifiers = resource.getIdentifiers();
		if(identifiers.isEmpty()) {
			return null;
		}

		Identifier strongestIdentifier = null;
		IdentifierType strongestType = null;

		for(Identifier identifier : identifiers) {
			IdentifierType type = identifier.getType();

			// Ignore identifiers that we have no measure of uniqueness for
			if(type==null) {
				continue;
			}

			// Ignore types with insufficient uniqueness for our specified minimum
			if(minimalUniqueness.compareTo(type.getUniqueness())>0) {
				continue;
			}

			// Check if current identifier provides stronger uniqueness
			if(strongestType==null || type.isStrongerThan(strongestType)) {
				strongestType = type;
				strongestIdentifier = identifier;
			}

			// Stop once we reached global uniqueness (no point in trying further)
			if(strongestType.getUniqueness()==Uniqueness.GLOBALLY_UNIQUE) {
				break;
			}
		}

		return strongestIdentifier;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#getAvailableRecords()
	 */
	@Override
	public RecordIterator getAvailableRecords() {

		try {
			return new RecordIteratorImpl(resourceProvider.children(rootFolder, "*"+DEFAULT_RECORD_FILE_ENDING));
		} catch (IOException e) {
			throw new MetadataException("Failed to access directory stream", e);
		}
	}

	protected DefaultUID checkUID(UID uid) {
		if(!DefaultUID.class.isInstance(uid))
			throw new MetadataException("Unsupported UID implementation: "+uid.getClass());

		DefaultUID tuid = (DefaultUID)uid;
		if(!UID_OWNER_ID.equals(tuid.getOwnerId()))
			throw new MetadataException("Foreign owner ID on given UID: "+tuid.getOwnerId());

		return tuid;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#addRecord(bwfdm.replaydh.metadata.MetadataRecord)
	 */
	@Override
	public void addRecord(MetadataRecord record) {
		requireNonNull(record);

		// Verify the record is mapped to one of our UIDs
		checkUID(record.getUID());

		// Chances are the record might be needed again soonish, so use the cache
		cache.addRecord(record);

		// Persist the record to file storage
		saveRecordToFile(record);

		fireMetadataRecordAdded(record);
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#removeRecord(bwfdm.replaydh.metadata.MetadataRecord)
	 */
	@Override
	public void removeRecord(MetadataRecord record) {
		requireNonNull(record);

		// Verify the record is mapped to one of our UIDs
		checkUID(record.getUID());

		// Make sure we don't keep the record in cache
		cache.removeRecord(record);

		// Remove record from file storage
		deleteRecordFile(record);

		fireMetadataRecordRemoved(record);
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#getRecord(bwfdm.replaydh.metadata.MetadataRecord.UID)
	 */
	@Override
	public MetadataRecord getRecord(UID uid) {
		requireNonNull(uid);

		/*
		 *  After this check we can assume that the associated record has at least been
		 *  known to us at some earlier point since otherwise no external entity could
		 *  have gained access to the UID.
		 *
		 *  So if from this point on the loading process fails we know it has to be
		 *  something outside of our control!
		 */
		checkUID(uid);

		// Always make sure we check cache first
		MetadataRecord record = cache.getRecord(uid);

		// If we got a cache miss try loading it
		if(record==null) {

			// Create fresh buffer and read content from file
			record = newRecord(uid);
			loadRecordFromFile((MutableMetadataRecord) record); // Safe cast since we just created it

			cache.addRecord(record);
		}
		return record;
	}

//	/**
//	 * Replaces all characters normally reserved for file paths
//	 * with the underscore ({@code '_'}) symbol.
//	 */
//	private static String normalizeForFilename(String s) {
//		return s==null ? null : s.replaceAll("[\\.:\\\\/]+", "_");
//	}

//	private static void appendOrReplace(StringBuilder sb, String text, boolean mayIgnore, boolean prependSeparator) {
//		if(mayIgnore && (text==null || text.isEmpty())) {
//			return;
//		}
//
//		text = normalizeForFilename(text);
//
//		if(text==null || text.isEmpty()) {
//			text = "x";
//		} else {
//			prependSeparator &= text.charAt(0)!='_';
//		}
//
//		if(prependSeparator) {
//			sb.append('_');
//		}
//
//		sb.append(text);
//	}

//	/**
//	 * @see bwfdm.replaydh.metadata.MetadataRepository#getUniqueName(bwfdm.replaydh.metadata.MetadataRecord.UID)
//	 */
//	@Override
//	public String getUniqueName(UID uid) {
//		TypedUID tuid = checkUID(uid);
//		StringBuilder sb = new StringBuilder();
//
//		appendOrReplace(sb, tuid.getOwnerId(), false, false);
//		appendOrReplace(sb, tuid.getType(), false, true);
//		appendOrReplace(sb, tuid.getContext(), true, true);
//		appendOrReplace(sb, tuid.getId(), false, true);
//
//		return sb.toString();
//	}

	private Path getFile(UID uid) {
//		String uniqueName = getUniqueName(uid);
//		if(uniqueName==null) {
//			//TODO implement our own scheme?
//		}

		DefaultUID duid = checkUID(uid);
		UUID systemId = duid.getSystemId();

		return rootFolder.resolve(systemId.toString()+DEFAULT_RECORD_FILE_ENDING);
	}

//	private Path getFile(Identifiable resource) {
//		UUID systemId = resource.getSystemId();
//
//		return rootFolder.resolve(systemId.toString()+DEFAULT_RECORD_FILE_ENDING);
//	}

	private void saveRecordToFile(MetadataRecord record) {
		Path file = getFile(record.getUID());

		//TODO ensure proper locking for writing to the file

		try {
			// Make sure the resource is there
			resourceProvider.create(file);

			IOResource resource = resourceProvider.getResource(file);
			resource.prepare();

			writer.transform(resource, record);
		} catch (ExecutionException e) {
			throw new MetadataException(String.format(
					"Failed to save record %s to file %s", record.getUID(), file), e.getCause());
		} catch (IOException e) {
			throw new MetadataException(String.format(
					"Failed to access file %s for record %s", file, record.getUID()), e);
		}
	}

	private void loadRecordFromFile(MutableMetadataRecord record) {
		Path file = getFile(record.getUID());

		if(!resourceProvider.exists(file))
			throw new MetadataException(String.format(
					"Unable to load record %s: missing file %s", record.getUID(), file));

		//TODO ensure proper locking for reading from the file
		try {
			IOResource resource = resourceProvider.getResource(file);
			resource.prepare();
			reader.transform(resource, record);
		} catch (ExecutionException e) {
			throw new MetadataException(String.format(
					"Failed to load record %s from file %s", record.getUID(), file), e.getCause());
		} catch (IOException e) {
			throw new MetadataException(String.format(
					"Failed to access file %s for record %s", file, record.getUID()), e);
		}
	}

	private void deleteRecordFile(MetadataRecord record) {
		Path file = getFile(record.getUID());

		if(!resourceProvider.exists(file)) {
			return;
		}

		try {
			resourceProvider.getResource(file).delete();
		} catch (IOException e) {
			throw new MetadataException(String.format(
					"Failed to delete file %s for record %s", file, record.getUID()), e);
		}
	}

	/**
	 * @see bwfdm.replaydh.metadata.basic.AbstractMetadataRespository#createVerifierForBuild(bwfdm.replaydh.workflow.Identifiable)
	 */
	@Override
	protected MetadataSchema createVerifierForBuild(Identifiable resource) {
		return buildVerifierSource!=null ? buildVerifierSource.apply(resource) : sharedVerifier;
	}

	/**
	 * @see bwfdm.replaydh.metadata.basic.AbstractMetadataRespository#createVerifierForEdit(bwfdm.replaydh.metadata.MetadataRecord)
	 */
	@Override
	protected MetadataSchema createVerifierForEdit(MetadataRecord record) {
		return editVerifierSource!=null ? editVerifierSource.apply(record) : sharedVerifier;
	}

	/**
	 * @see bwfdm.replaydh.metadata.basic.AbstractMetadataRespository#afterEndEdit(bwfdm.replaydh.metadata.MetadataRecord, boolean)
	 */
	@Override
	protected void afterEndEdit(MetadataRecord record, boolean discard) {

		// Allow notification of listeners
		super.afterEndEdit(record, discard);

		// After publishing the change event, persist record to file
		saveRecordToFile(record);
	}

	protected class RecordIteratorImpl implements RecordIterator {

		private final DirectoryStream<Path> stream;
		private volatile Iterator<Path> pathIterator;

		public RecordIteratorImpl(DirectoryStream<Path> stream) {
			this.stream = requireNonNull(stream);
		}

		private void checkRepositoryActive() {
			if(!hasEnvironment())
				throw new IllegalStateException("Repository associated with record iterator is closed");
		}

		private Iterator<Path> pathIterator() {
			if(pathIterator==null) {
				pathIterator = stream.iterator();
			}
			return pathIterator;
		}

		/**
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			checkRepositoryActive();
			return pathIterator().hasNext();
		}

		/**
		 * @see java.util.Iterator#next()
		 */
		@Override
		public UID next() {
			checkRepositoryActive();
			Path path = pathIterator().next();
			return getUID(path);
		}

		/**
		 * @throws IOException
		 * @see java.lang.AutoCloseable#close()
		 */
		@Override
		public void close() throws IOException {
			stream.close();
		}

	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	public static class Builder {

		private Path rootFolder;

		private Function<MetadataRecord, MetadataSchema> editVerifierSource;
		private Function<Identifiable, MetadataSchema> buildVerifierSource;

		private Function<MetadataRecord, String> nameGenerator;

		private IOWorker<? super MutableMetadataRecord> reader;
		private IOWorker<? super MetadataRecord> writer;

		private MetadataRecordCache cache;

		private ResourceProvider resourceProvider;

		private UIDStorage uidStorage;

		/**
		 * Prevents public instantiation outside of {@link FileMetadataRepository#newBuilder()}.
		 */
		Builder() {
			// no-op
		}

		public Builder nameGenerator(Function<MetadataRecord, String> nameGenerator) {
			requireNonNull(nameGenerator);
			checkState("Name generator already set", this.nameGenerator==null);

			this.nameGenerator = nameGenerator;

			return this;
		}

		public Builder uidStorage(UIDStorage uidStorage) {
			requireNonNull(uidStorage);
			checkState("UID storage already set", this.uidStorage==null);

			this.uidStorage = uidStorage;

			return this;
		}

		public Builder resourceProvider(ResourceProvider resourceProvider) {
			requireNonNull(resourceProvider);
			checkState("Resource provider already set", this.resourceProvider==null);

			this.resourceProvider = resourceProvider;

			return this;
		}

		public Builder reader(IOWorker<? super MutableMetadataRecord> reader) {
			requireNonNull(reader);
			checkState("Reader already set", this.reader==null);

			this.reader = reader;

			return this;
		}

		public Builder writer(IOWorker<? super MetadataRecord> writer) {
			requireNonNull(writer);
			checkState("Writer already set", this.writer==null);

			this.writer = writer;

			return this;
		}

		public Builder rootFolder(Path rootFolder) {
			requireNonNull(rootFolder);
			checkState("Root folder already set", this.rootFolder==null);

			this.rootFolder = rootFolder;

			return this;
		}

		public Builder cache(MetadataRecordCache cache) {
			requireNonNull(cache);
			checkState("Cache already set", this.cache==null);

			this.cache = cache;

			return this;
		}

		public Builder editVerifierSource(Function<MetadataRecord, MetadataSchema> editVerifierSource) {
			requireNonNull(editVerifierSource);
			checkState("Source for edit schema already set", this.editVerifierSource==null);

			this.editVerifierSource = editVerifierSource;

			return this;
		}

		public Builder buildVerifierSource(Function<Identifiable, MetadataSchema> buildVerifierSource) {
			requireNonNull(buildVerifierSource);
			checkState("Source for build schema already set", this.buildVerifierSource==null);

			this.buildVerifierSource = buildVerifierSource;

			return this;
		}

		public Path getRootFolder() {
			return rootFolder;
		}

		public Function<MetadataRecord, String> getNameGenerator() {
			return nameGenerator;
		}

		public Function<MetadataRecord, MetadataSchema> getEditVerifierSource() {
			return editVerifierSource;
		}

		public Function<Identifiable, MetadataSchema> getBuildVerifierSource() {
			return buildVerifierSource;
		}

		public MetadataRecordCache getCache() {
			return cache;
		}

		public IOWorker<? super MutableMetadataRecord> getReader() {
			return reader;
		}

		public IOWorker<? super MetadataRecord> getWriter() {
			return writer;
		}

		public UIDStorage getUidStorage() {
			return uidStorage;
		}

		public ResourceProvider getResourceProvider() {
			return resourceProvider;
		}

		// Utility parts

		/**
		 * Make the repository use the {@link DublinCoreField#TITLE} field
		 * for generating a {@link MetadataRepository#getDisplayName(MetadataRecord) display name}
		 * for a given {@link MetadataRecord record}.
		 *
		 * @return
		 */
		public Builder useDublinCoreNameGenerator() {

			if(nameGenerator==null) {
				nameGenerator = record -> DublinCoreField.TITLE.getValue(record);
			}

			return this;
		}

		/**
		 * Make the repository use a {@link DublinCoreSchema11#isStrict() non-strict} version
		 * of the {@link DublinCoreSchema11}.
		 * @return
		 */
		public Builder useDublinCore() {

			useDublinCore(false);

			return this;
		}

		/**
		 * Make the repository use a {@link DublinCoreSchema11#isStrict() strict} version
		 * of the {@link DublinCoreSchema11}.
		 * @return
		 */
		public Builder useStrictDublinCore() {

			useDublinCore(true);

			return this;
		}

		private void useDublinCore(boolean strict) {

			final MetadataSchema verifier = new DublinCoreSchema11(strict);

			editVerifierSource = record -> verifier;
			buildVerifierSource = id -> verifier;

			if(strict) {
				useDublinCoreNameGenerator();
			}
		}

		public Builder useVirtualUIDStorage() {
			uidStorage = new VirtualUIDStorage();

			return this;
		}

		/**
		 * Assigns default implementations to required I/O-related
		 * fields other than the {@link #getRootFolder() root folder}
		 * if those fields have not been set yet.
		 *
		 * @return
		 */
		public Builder useDefaultCacheAndSerialization() {
			if(resourceProvider==null) {
				resourceProvider = new FileResourceProvider();
			}

			if(cache==null) {
				cache = new MetadataRecordCache(1000, 10_000);
			}

			if(reader==null) {
				reader = FileMetadataXml.reader();
			}

			if(writer==null) {
				writer = FileMetadataXml.writer();
			}

			//TODO init default UIDStorage (use DB ?)

			return this;
		}

		protected void validate() {
			checkState("Missing root folder", rootFolder!=null);
			checkState("Missing cache", cache!=null);
			checkState("Missing reader", reader!=null);
			checkState("Missing writer", writer!=null);
			checkState("Missing UID-storage", uidStorage!=null);
			checkState("Missing resource provider", resourceProvider!=null);

			//TODO
		}

		public FileMetadataRepository build() {
			validate();

			return new FileMetadataRepository(this);
		}
	}
}

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
package bwfdm.replaydh.metadata.db;

import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.db.DBUtils;
import bwfdm.replaydh.io.resources.FileResource;
import bwfdm.replaydh.io.resources.FileResourceProvider;
import bwfdm.replaydh.io.resources.IOResource;
import bwfdm.replaydh.io.resources.ResourceProvider;
import bwfdm.replaydh.metadata.MetadataException;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRecord.Target;
import bwfdm.replaydh.metadata.MetadataRepository;
import bwfdm.replaydh.metadata.MetadataSchema;
import bwfdm.replaydh.metadata.ValueRestriction;
import bwfdm.replaydh.metadata.basic.AbstractMetadataRespository;
import bwfdm.replaydh.metadata.basic.DefaultMetadataRecord;
import bwfdm.replaydh.metadata.basic.DefaultMetadataSchema;
import bwfdm.replaydh.metadata.basic.DublinCoreField;
import bwfdm.replaydh.metadata.basic.DublinCoreSchema11;
import bwfdm.replaydh.metadata.basic.MetadataRecordCache;
import bwfdm.replaydh.metadata.basic.MutableMetadataRecord;
import bwfdm.replaydh.metadata.xml.MetadataSchemaXml;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.utils.AccessMode;
import bwfdm.replaydh.utils.MutablePrimitives.MutableInteger;

/**
 * Implements a {@link MetadataRepository repository} that stores records
 * as local text files inside a given folder.
 * <p>
 * TODO
 *
 * @author Markus Gärtner
 *
 */
public class MetadataDB extends AbstractMetadataRespository {

	public static Builder newBuilder() {
		return new Builder();
	}

	private static final Logger log = LoggerFactory.getLogger(MetadataDB.class);

	private final Path rootFolder;

	private final MetadataRecordCache cache;

	private final ResourceProvider resourceProvider;

	private final Function<MetadataRecord, String> nameGenerator;

	private final boolean memory;

	public static final String DEFAULT_DB_FILE = "metadata.db";

	private final boolean verbose;

	private Connection connection;

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

	protected MetadataDB(Builder builder) {

		// Redundant check just to be sure
		builder.validate();

		rootFolder = builder.getRootFolder();
		cache = builder.getCache();
		resourceProvider = builder.getResourceProvider();
		memory = builder.isMemory();
		verbose = builder.isVerbose();

		setDefaultSchema(builder.getDefaultSchema());

		Function<MetadataRecord, String> nameGenerator = builder.getNameGenerator();
		if(nameGenerator==null) {
			nameGenerator = record -> record.getTarget().toString();
		}

		this.nameGenerator = nameGenerator;
	}

	/**
	 * @see bwfdm.replaydh.core.AbstractRDHTool#isVerbose()
	 */
	@Override
	protected boolean isVerbose() {
		return verbose;
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

		synchronized (lock) {
			if(resourceProvider!=null && rootFolder!=null) {
				scanFolder(rootFolder);
			}
		}

		// Driver loaded, now connect database
		if(memory) {
			try {
				connection = DBUtils.connect("jdbc:sqlite::memory:");
			} catch (SQLException e) {
				log.error("Unable to create in-memory database", e);
			}
		} else {
			Path path = rootFolder.resolve(DEFAULT_DB_FILE);

			try {
				resourceProvider.create(path);
			} catch (IOException e) {
				log.error("Failed to create database file {}", path, e);
				return false;
			}

			try {
				connection = DBUtils.connect("jdbc:sqlite:"+path);
			} catch (SQLException e) {
				log.error("Unable to create file database {}", path, e);
			}
		}

		try {
			setupDB();
		} catch (SQLException e) {
			log.error("Failed to setup database", e);
			return false;
		}

		return connection!=null;
	}

	private void scanFolder(Path folder) {
		if(!resourceProvider.exists(folder)) {
			return;
		}

		// We assume all XML files in the target folder to be schema definitions
		try(DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.oms.xml")) {
			for(Path file : stream) {
				scanFile(file);
			}
		} catch (IOException e) {
			log.error("Failed to scan folder: {}", folder, e);
		}
	}

	private void scanFile(Path file) {
		IOResource resource = new FileResource(file, AccessMode.READ);
		MetadataSchema schema = null;
		try {
			schema = MetadataSchemaXml.readSchema(resource);
		} catch (ExecutionException e) {
			log.error("Failed to read schema file: {}", file, e.getCause());
		}

		if(schema!=null) {
			addSchema(schema);
		}
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

		if(connection!=null) {
			DBUtils.closeSilently(connection);
		}

		super.stop(environment);
	}

	private static final String PREFIX = "rdh_";
	private static final String TBL_RECORD = PREFIX+"record";
	private static final String TBL_ENTRY = PREFIX+"entry";

	private static final String COL_ID = "id";
	private static final String COL_WORKSPACE = "workspace";
	private static final String COL_PATH = "path";
	private static final String COL_SCHEMA = "schema";

	private static final String COL_RECORD_ID = "record_id";
	private static final String COL_PROPERTY = "property";
	private static final String COL_VALUE = "value";

	private static final int NO_ID = -1;

	private void setupDB() throws SQLException {
		try(Statement stmt = connection.createStatement()) {

			/*
			 * Make sure we can use triggers on foreign keys.
			 * We need this for not having to do multiple queries
			 * when deleting records.
			 */
			stmt.execute("PRAGMA foreign_keys = ON");

			/*
			 *  CREATE TABLE rdh_record (
				    id        INTEGER PRIMARY KEY AUTOINCREMENT,
				    workspace TEXT    NOT NULL,
				    path      TEXT    NOT NULL,
				    schema    TEXT    NOT NULL,
				    UNIQUE (
				        workspace COLLATE NOCASE,
				        path COLLATE NOCASE
				    )
				    ON CONFLICT ABORT
				);
			 */
			stmt.execute(maybeLogQuery(
					"CREATE TABLE IF NOT EXISTS "+TBL_RECORD+" (\n" +
					"    "+COL_ID+"        INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
					"    "+COL_WORKSPACE+" TEXT    NOT NULL,\n" +
					"    "+COL_PATH+"      TEXT    NOT NULL,\n" +
					"    "+COL_SCHEMA+"    TEXT    NOT NULL,\n" +
					"    UNIQUE (\n" +
					"        "+COL_WORKSPACE+" COLLATE NOCASE,\n" +
					"        "+COL_PATH+" COLLATE NOCASE,\n" +
					"        "+COL_SCHEMA+" COLLATE NOCASE\n" +
					"    )\n" +
					"    ON CONFLICT ABORT\n" +
					");"));

			/*
			 *  CREATE INDEX "" ON rdh_record (
				    workspace,
				    path
				);
			 */
			stmt.execute(maybeLogQuery(
					"CREATE INDEX IF NOT EXISTS \"path_idx\" ON "+TBL_RECORD+" (\n" +
					"    "+COL_WORKSPACE+",\n" +
					"    "+COL_PATH+"\n" +
					");"));

			/*
			 *  CREATE TABLE rdh_entry (
				    record_id INTEGER REFERENCES rdh_record (id) ON DELETE CASCADE,
				    property  TEXT    NOT NULL,
				    value     TEXT    NOT NULL
				);
			 */
			stmt.execute(maybeLogQuery(
					"CREATE TABLE IF NOT EXISTS "+TBL_ENTRY+" (\n" +
					"    "+COL_RECORD_ID+" INTEGER REFERENCES "+TBL_RECORD+" ("+COL_ID+") ON DELETE CASCADE,\n" +
					"    "+COL_PROPERTY+"  TEXT    NOT NULL,\n" +
					"    "+COL_VALUE+"     TEXT    NOT NULL\n" +
					");"));
		}
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#hasRecords(bwfdm.replaydh.metadata.MetadataRecord.Target)
	 */
	@Override
	public boolean hasRecords(Target target) {

		if(cache.hasRecords(target)) {
			return true;
		}

		try(Statement stmt = connection.createStatement(
				ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
			String workspace = target.getWorkspace();
			String path = target.getPath();
			String query = maybeLogQuery(
					"SELECT count() FROM  (" +
					"    SELECT * FROM "+TBL_RECORD+" " +
					"    WHERE "+COL_WORKSPACE+"=\""+workspace+"\"\n" +
					"        AND "+COL_PATH+"=\""+path+"\" LIMIT 1)");
			ResultSet rs = stmt.executeQuery(query);

			return asInt(rs) > 0;
		} catch (SQLException e) {
			log.error("Failed to query database", e);
		}

		return false;
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
	 * @see bwfdm.replaydh.metadata.MetadataRepository#getAvailableRecords()
	 */
	@Override
	public RecordIterator getAvailableRecords() {

		return new RecordIteratorImpl();
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#addRecord(bwfdm.replaydh.metadata.MetadataRecord)
	 */
	@Override
	public void addRecord(MetadataRecord record) {
		requireNonNull(record);

		// Chances are the record might be needed again soonish, so use the cache early on
		cache.addRecord(record);

		// Persist the record to db storage
		saveRecordToDb(record);

		fireMetadataRecordAdded(record);
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#removeRecord(bwfdm.replaydh.metadata.MetadataRecord)
	 */
	@Override
	public void removeRecord(MetadataRecord record) {
		requireNonNull(record);

		// Make sure we don't keep the record in cache
		cache.removeRecord(record);

		// Remove record from db storage
		deleteRecordFromDb(record);

		fireMetadataRecordRemoved(record);
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#getRecord(Target, String)
	 */
	@Override
	public MetadataRecord getRecord(Target target, String schemaId) {
		requireNonNull(target);

		// Always make sure we check cache first
		MetadataRecord record = cache.getRecord(target, schemaId);

		// If we got a cache miss try loading it
		if(record==null) {

			// Create fresh buffer and read content from db
			record = loadRecordFromDb(target, schemaId);

			// Cache result if one is available
			if(record!=null) {
				cache.addRecord(record);
			}
		}
		return record;
	}

	private String maybeLogQuery(String query) {

		if(isVerbose()) {
			log.info("Executing query:\n===== BEGIN =====\n{}\n=====", query);
		}

		return query;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#getRecords(bwfdm.replaydh.metadata.MetadataRecord.Target)
	 */
	@Override
	public Collection<MetadataRecord> getRecords(Target target) {
		requireNonNull(target);

		List<MetadataRecord> records = cache.getRecords(target);

		if(records.isEmpty()) {
			records = new ArrayList<>();

			try(Statement stmt = connection.createStatement(
					ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
				String workspace = target.getWorkspace();
				String path = target.getPath();
				String query = maybeLogQuery(
						"SELECT r."+COL_ID+", r."+COL_SCHEMA+", e."+COL_PROPERTY+", e."+COL_VALUE+"\n" +
						"FROM "+TBL_ENTRY+" AS e\n" +
						"INNER JOIN "+TBL_RECORD+" AS r ON r."+COL_ID+" = e."+COL_RECORD_ID+"\n" +
						"WHERE r."+COL_WORKSPACE+" = \""+workspace+"\"\n" +
						"    AND r."+COL_PATH+" = \""+path+"\"\n" +
						"ORDER BY r."+COL_ID);
				ResultSet rs = stmt.executeQuery(query);

				int id = -1;
				DefaultMetadataRecord record = null;
				while(rs.next()) {
					int newId = rs.getInt(1);
					String schemaId = rs.getString(2);

					if(record==null || newId != id) {
						id = newId;
						record = new DefaultMetadataRecord(target, schemaId);
						records.add(record);
					}
					record.addEntry(rs.getString(3), rs.getString(4));
				}
			} catch (SQLException e) {
				log.error("Failed to query database", e);
			}

			if(!records.isEmpty()) {
				cache.addRecords(records);
			}
		}

		return records;
	}

	private void saveRecordToDb(MetadataRecord record) {
		int id = NO_ID;
		/*
		 *  If record existed, we need to erase content.
		 *  The 'entry' table is essentially a triple store,
		 *  so managing the dynamic content for a single record
		 *  constantly would result in some coding overhead and
		 *  simply erasing old data and inserting new one shouldn't
		 *  be too painful on the performance side...
		 */
		try(Statement stmt = connection.createStatement()) {
			id = getRecordId(stmt, record);
			if(id != NO_ID) {
				// Old record -> delete all the entries, but keep the "record" itself
				String query = maybeLogQuery(
						"DELETE\n" +
						"FROM "+TBL_ENTRY+" AS e\n" +
						"WHERE e."+COL_RECORD_ID+" = "+id);
				stmt.execute(query);
			} else {
				// New record -> create entry in records table
				Target target = record.getTarget();
				String query = maybeLogQuery("INSERT INTO "+TBL_RECORD+" ("+COL_WORKSPACE+", "+COL_PATH+", "+COL_SCHEMA+")\n" +
						"VALUES (\""+target.getWorkspace()+"\", \""+target.getPath()+"\", \""+record.getSchemaId()+"\")");
				stmt.execute(query);

				// Fetch record id
				try(ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
					id = asInt(rs);
				}
			}


			/*
			 * New record or previous data erased.
			 * So either way, just run a batch insert of the rows
			 * for our record.
			 */
			StringBuilder sb = new StringBuilder(50 + record.getEntryCount()*30);
			sb.append("INSERT INTO "+TBL_ENTRY+" ("+COL_RECORD_ID+", "+COL_PROPERTY+", "+COL_VALUE+")\n VALUES ");
			MutableInteger rows = new MutableInteger();
			final int recordId = id;
			record.forEachEntry(entry -> {
				if(rows.getAndIncrement()>0) {
					sb.append(",");
				}
				sb.append("\n(").append(recordId)
					.append(", \"").append(entry.getName())
					.append("\", \"").append(entry.getValue())
					.append("\")");
			});

			String query = maybeLogQuery(sb.toString());
			stmt.execute(query);
		} catch (SQLException e) {
			log.error("Failed to query database", e);
			throw new MetadataException("Error while contacting database", e);
		}
	}

	private MetadataRecord loadRecordFromDb(Target target, String schemaId) {
		try(Statement stmt = connection.createStatement()) {
			String workspace = target.getWorkspace();
			String path = target.getPath();
			String query = maybeLogQuery(
					"SELECT e."+COL_PROPERTY+", e."+COL_VALUE+" \n" +
					"FROM "+TBL_ENTRY+" AS e, "+TBL_RECORD+" AS r\n" +
					"WHERE r."+COL_WORKSPACE+" = \""+workspace+"\" \n" +
					"    AND r."+COL_PATH+" = \""+path+"\"\n" +
					"    AND r."+COL_SCHEMA+" = \""+schemaId+"\"\n" +
					"    AND e."+COL_RECORD_ID+" = r."+COL_ID+"");
			ResultSet rs = stmt.executeQuery(query);

			DefaultMetadataRecord record = null;

			while(rs.next()) {
				// Lazily create the record if the query yielded actual results
				if(record==null) {
					record = new DefaultMetadataRecord(target, schemaId);
				}
				record.addEntry(rs.getString(1), rs.getString(2));
			}

			return record;
		} catch (SQLException e) {
			log.error("Failed to query database", e);
			return null;
		}
	}

	private void deleteRecordFromDb(MetadataRecord record) {
		try(Statement stmt = connection.createStatement()) {
			Target target = record.getTarget();
			String workspace = target.getWorkspace();
			String path = target.getPath();
			String schemaId = record.getSchemaId();
			String query = maybeLogQuery(
					"DELETE\n" +
					"FROM "+TBL_RECORD+" AS r\n" +
					"WHERE r."+COL_WORKSPACE+" = \""+workspace+"\" \n" +
					"    AND r."+COL_PATH+" = \""+path+"\"\n" +
					"    AND r."+COL_SCHEMA+" = \""+schemaId+"\"");
			stmt.execute(query);
		} catch (SQLException e) {
			log.error("Failed to query database", e);
			throw new MetadataException("Error while contacting database", e);
		}
	}

	private int getRecordId(Statement stmt, MetadataRecord record) throws SQLException {
		int id =  record instanceof DbMetadataRecord ? ((DbMetadataRecord)record).getId() : NO_ID;
		if(id != NO_ID) {
			return id;
		}

		Target target = record.getTarget();
		String query = maybeLogQuery(
				"SELECT r."+COL_ID+"\n" +
				"FROM "+TBL_RECORD+" AS r\n" +
				"WHERE r."+COL_WORKSPACE+" = \""+target.getWorkspace()+"\"\n" +
				"    AND r."+COL_PATH+" = \""+target.getPath()+"\"\n" +
				"    AND r."+COL_SCHEMA+" = \""+record.getSchemaId()+"\"");
		try(ResultSet rs = stmt.executeQuery(query)) {
			id = asInt(rs);
		}

		if(record instanceof DbMetadataRecord) {
			((DbMetadataRecord)record).setId(id);
		}

		return id;
	}

	private int asInt(ResultSet rs) throws SQLException {
		if(rs.next()) {
			return rs.getInt(1);
		} else {
			return NO_ID;
		}
	}

	/**
	 * @see bwfdm.replaydh.metadata.basic.AbstractMetadataRespository#newRecord(bwfdm.replaydh.metadata.MetadataRecord.Target, java.lang.String)
	 */
	@Override
	public MutableMetadataRecord newRecord(Target target, String schemaId) {
		if(cache.getRecord(target, schemaId)!=null)
			throw new IllegalStateException("Record already cached for target: "+target);

		return new DbMetadataRecord(target, schemaId);
	}

	/**
	 * @see bwfdm.replaydh.metadata.basic.AbstractMetadataRespository#afterEndEdit(bwfdm.replaydh.metadata.MetadataRecord, boolean)
	 */
	@Override
	protected void afterEndEdit(MetadataRecord record, boolean discard) {

		if(!discard) {
			// After publishing the change event, persist record to db
			saveRecordToDb(record);
		}

		// Allow notification of listeners
		super.afterEndEdit(record, discard);
	}

	/**
	 * @see bwfdm.replaydh.utils.AbstractSchemaManager#getFallbackSchema()
	 */
	@Override
	protected MetadataSchema getFallbackSchema() {
		return MetadataSchema.EMPTY_SCHEMA;
	}

	private String adjustPath(String s) {
		return s==null ? null : s.replace('\\', '/');
	}

	/**
	 * Special class to keep track of the id used on the database side.
	 *
	 * @author Markus Gärtner
	 *
	 */
	protected static class DbMetadataRecord extends DefaultMetadataRecord {

		private int id = NO_ID;

		public DbMetadataRecord(Target target, String schemaId) {
			super(target, schemaId);
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public boolean isIdSet() {
			return id != NO_ID;
		}
	}

	/**
	 * Iterator that wraps around a {@link ResultSet} which is generated
	 * dynamically and released once no longer needed.
	 *
	 * @author Markus Gärtner
	 *
	 */
	protected class RecordIteratorImpl implements RecordIterator {

		private Statement stmt;
		private ResultSet rs;
		private Target nextTarget;
		private boolean loaded = false;

		/**
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			if(nextTarget != null) {
				return true;
			}

			if(stmt == null && !loaded) {
				try {
					stmt = connection.createStatement();
					String query = maybeLogQuery(
							"SELECT r."+COL_WORKSPACE+", r."+COL_PATH+" \n" +
							"FROM "+TBL_RECORD+" AS r");
					rs = stmt.executeQuery(query);
				} catch (SQLException e) {
					throw new MetadataException("Error while contacting database", e);
				}
			}

			if(rs == null) {
				return false;
			}

			try {
				if(!rs.next()) {
					close();
					return false;
				}

				nextTarget = new Target(adjustPath(rs.getString(1)),
						adjustPath(rs.getString(2)));
			} catch (SQLException e) {
				throw new MetadataException("Error while contacting database", e);
			}

			return nextTarget != null;
		}

		/**
		 * @see java.util.Iterator#next()
		 */
		@Override
		public Target next() {
			Target target = nextTarget;
			if(target == null)
				throw new NoSuchElementException();

			nextTarget = null;

			return target;
		}

		/**
		 * @see java.lang.AutoCloseable#close()
		 */
		@Override
		public void close() {
			DBUtils.closeSilently(stmt);
			stmt = null;
			rs = null;
		}

	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	public static class Builder {

		public static final boolean DEFAULT_MEMORY = false;

		public static final boolean DEFAULT_VERBOSE = false;

		private Path rootFolder;

		private Function<MetadataRecord, String> nameGenerator;

		private MetadataRecordCache cache;

		private ResourceProvider resourceProvider;

		private Boolean memory;

		private Boolean verbose;

		private MetadataSchema defaultSchema;

		/**
		 * Prevents public instantiation outside of {@link MetadataDB#newBuilder()}.
		 */
		Builder() {
			// no-op
		}

		public Builder defaultSchema(MetadataSchema defaultSchema) {
			requireNonNull(defaultSchema);
			checkState("Default schema already set", this.defaultSchema==null);

			this.defaultSchema = defaultSchema;

			return this;
		}

		public Builder nameGenerator(Function<MetadataRecord, String> nameGenerator) {
			requireNonNull(nameGenerator);
			checkState("Name generator already set", this.nameGenerator==null);

			this.nameGenerator = nameGenerator;

			return this;
		}

		public Builder resourceProvider(ResourceProvider resourceProvider) {
			requireNonNull(resourceProvider);
			checkState("Resource provider already set", this.resourceProvider==null);

			this.resourceProvider = resourceProvider;

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

		public Builder memory(boolean memory) {
			checkState("Memory flag set", this.memory==null);

			this.memory = Boolean.valueOf(memory);

			return this;
		}

		public Builder verbose(boolean verbose) {
			checkState("Verbose flag set", this.verbose==null);

			this.verbose = Boolean.valueOf(verbose);

			return this;
		}

		public MetadataSchema getDefaultSchema() {
			return defaultSchema;
		}

		public Path getRootFolder() {
			return rootFolder;
		}

		public Function<MetadataRecord, String> getNameGenerator() {
			return nameGenerator;
		}

		public MetadataRecordCache getCache() {
			return cache;
		}

		public ResourceProvider getResourceProvider() {
			return resourceProvider;
		}

		public boolean isMemory() {
			return memory==null ? DEFAULT_MEMORY : memory.booleanValue();
		}

		public boolean isVerbose() {
			return verbose==null ? DEFAULT_VERBOSE : verbose.booleanValue();
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
			defaultSchema(new DublinCoreSchema11(strict));

			if(strict) {
				useDublinCoreNameGenerator();
			}
		}

		/**
		 * Assigns default implementations to required I/O-related
		 * fields other than the {@link #getRootFolder() root folder}
		 * if those fields have not been set yet.
		 *
		 * @return
		 */
		public Builder useDefaultCacheAndLocationProvider() {
			if(resourceProvider==null) {
				resourceProvider = new FileResourceProvider();
			}

			if(cache==null) {
				cache = new MetadataRecordCache(1000, 10_000);
			}

			return this;
		}

		protected void validate() {
			checkState("Missing cache", cache!=null);

			if(!isMemory()) {
				checkState("Missing root folder", rootFolder!=null);
				checkState("Missing resource provider", resourceProvider!=null);
			}
		}

		public MetadataDB build() {
			validate();

			return new MetadataDB(this);
		}
	}
}

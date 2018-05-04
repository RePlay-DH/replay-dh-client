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
package bwfdm.replaydh.workflow.resolver;

import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import bwfdm.replaydh.core.AbstractRDHTool;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.db.DBUtils;
import bwfdm.replaydh.io.resources.ResourceProvider;
import bwfdm.replaydh.metadata.MetadataException;
import bwfdm.replaydh.utils.LookupResult;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifiable.Type;
import bwfdm.replaydh.workflow.Identifier;

/**
 * @author Markus Gärtner
 *
 */
public class DBIdentifiableResolver extends AbstractRDHTool implements IdentifiableResolver {

	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Name of the file inside the root rootFolder where shared cache data is being kept.
	 */
	private static final String DEFAULT_SHARED_DB_FILE = "shared-cache.db";

	private static final String DEFAULT_LOCAL_CACHE_FILENAME = "local-cache";
	private static final String DEFAULT_LOCAL_CACHE_FILEENDING = ".id.ini";

	/**
	 * The source rootFolder where resources for persistent storage
	 * of the cache data are kept.
	 */
	private final Path rootFolder;

	/**
	 * File system abstraction
	 */
	private final ResourceProvider resourceProvider;

	/**
	 * Flag to indicate how the cache data should be accessed:
	 * <p>
	 * A value of {@code true} means that a shared sqlite database
	 * file in the target rootFolder should be used. Upon starting this
	 * implementation will ensure that the content of a plain cache
	 * file will be transfered to the database, if present.
	 * <p>
	 * A value of {@code false} will cause the implementation to
	 * completely ignore the shared sqlite database file and only
	 * access the plain cache file. In this case the content of the
	 * cache will be loaded into an in-memory database. When the
	 * resolver is closed, said cache data will be written back
	 * again.
	 */
	private final boolean useSharedDatabase;

	/**
	 * Flag to indicate whether or not the database connection should
	 * be closed again after each batch of requests has been processed.
	 * <p>
	 * Only relevant if the {@link #useSharedDatabase} flag is active!
	 */
	private final boolean autoCloseIdleConnection;

	/**
	 * Lock for synchronizing access to the db.
	 */
	private final Lock dbLock = new ReentrantLock();

	/**
	 * Models access to our lookup db. This will either be an
	 * in-memory db or a direct connection to the shared cache.
	 * <p>
	 * Any interaction with this connection must be synchronized
	 * via the {@link #dbLock}!
	 */
	private volatile transient Connection connection;

	/**
	 * Location of local cache data.
	 */
	private volatile Path cacheFile;

	private volatile Lock fileLock;

	protected DBIdentifiableResolver(Builder builder) {
		requireNonNull(builder);

		rootFolder = builder.getRootFolder();
		resourceProvider = builder.getResourceProvider();
		useSharedDatabase = builder.isUseSharedDatabase();
		autoCloseIdleConnection = builder.isAutoCloseIdleConnection();
	}

	private static Connection open(String path) {
		try {
			return DriverManager.getConnection("jdbc:sqlite:"+path);
		} catch (SQLException e) {
			throw new RDHException("Failed to open SQLite database for path: "+path, e);
		}
	}

	private static Connection openVirtual() {
		try {
			return DriverManager.getConnection("jdbc:sqlite::memory");
		} catch (SQLException e) {
			throw new RDHException("Failed to open in-memory SQLite database", e);
		}

	}

	/**
	 * @throws RDHLifecycleException
	 * @see bwfdm.replaydh.core.AbstractRDHTool#start(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public boolean start(RDHEnvironment environment) throws RDHLifecycleException {
		if(!super.start(environment)) {
			return false;
		}

		//FIXME ensure proper uniqueness of our local file! (maybe based off of some internal setting/id stored as property?)
		String localCacheName = DEFAULT_LOCAL_CACHE_FILENAME+DEFAULT_LOCAL_CACHE_FILENAME;
		cacheFile = rootFolder.resolve(localCacheName);

		return true;
	}

	/**
	 * @throws RDHLifecycleException
	 * @see bwfdm.replaydh.core.AbstractRDHTool#stop(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public void stop(RDHEnvironment environment) throws RDHLifecycleException {
		cacheFile = null;

		DBUtils.closeSilently(connection);

		if(fileLock!=null) {
			try {
				// An exception here could prevent the super.stop() method from ever being called. Issue?
				fileLock.unlock();
			} finally {
				fileLock = null;
			}
		}

		super.stop(environment);
	}

	/**
	 * To be called under dbLock synchronization!
	 */
	private Connection openConnection() {
		checkStarted();

		if(useSharedDatabase) {
			return open(getSharedDBFile().toString());
		} else {
			Connection result = openVirtual();

			//TODO read in the cacheFile content and store in virtual DB!

			return result;
		}
	}

	private Connection getConnection() {
		synchronized (dbLock) {
			if(connection==null) {
				connection = openConnection();
				initDB(connection);
			}

			return connection;
		}
	}

	private void initDB(Connection connection) {
		Statement statement = null;
		try {
			statement = connection.createStatement();

			statement.executeQuery(
					"CREATE TABLE IF NOT EXISTS identifiers ("
					+ "uuid text NOT NULL,"
					+ "type text NOT NULL,"
					+ "context text NOT NULL,"
					+ "id text NOT NULL"
					+ ")");
		} catch (SQLException e) {
			throw new MetadataException("Failed to create default tables", e);
		} finally {
			DBUtils.closeSilently(statement);
		}
	}

	private Path getSharedDBFile() {
		return rootFolder.resolve(DEFAULT_SHARED_DB_FILE);
	}

	private Path getFileForLocking() {
		checkStarted();

		return useSharedDatabase ? getSharedDBFile() : cacheFile;
	}

	private void checkLock() {
		checkState("No update lock acquired", fileLock!=null);
	}

	@Override
	public void lock() {
		checkStarted();

		if(!useSharedDatabase) {
			return;
		}

		synchronized (rootFolder) {
			if(fileLock==null) {
				fileLock = resourceProvider.getLock(getSharedDBFile());
			}

			fileLock.lock();
		}
	}

	@Override
	public void unlock() {
		checkStarted();

		if(!useSharedDatabase) {
			return;
		}

		synchronized (rootFolder) {
			checkLock();

			try {
				fileLock.unlock();
			} finally {
				fileLock = null;
			}
		}

		if(autoCloseIdleConnection) {
			synchronized (dbLock) {
				DBUtils.closeSilently(connection);
				connection = null;
			}
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.resolver.IdentifiableResolver#resolve(int, java.util.Set)
	 */
	@Override
	public List<LookupResult<Identifiable, Set<Identifier>>> resolve(int candidateLimit, Set<Identifier> identifiers) {

		// For non-virtual cache storage we need the locking!
		if(!useSharedDatabase) {
			checkLock();
		}

		synchronized (dbLock) {

		}


		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public void update(Set<? extends Identifiable> identifiables) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see bwfdm.replaydh.workflow.resolver.IdentifiableResolver#lookup(java.util.UUID)
	 */
	@Override
	public <I extends Identifiable> I lookup(UUID systemId) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public void register(Set<? extends Identifiable> identifiables) {
		throw new UnsupportedOperationException("not implemented");
	}

	@Override
	public void unregister(Set<? extends Identifiable> identifiables) {
		throw new UnsupportedOperationException("not implemented");
	}

	/**
	 * @see bwfdm.replaydh.workflow.resolver.IdentifiableResolver#identifiablesForType(bwfdm.replaydh.workflow.Identifiable.Type)
	 */
	@Override
	public Iterator<Identifiable> identifiablesForType(Type type) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	public static class Builder {

		public static final boolean DEFAULT_USE_SHARED_DATABASE = false;
		public static final boolean DEFAULT_AUTOCLOSE_IDE_CONNECTION = false;

		private Path rootFolder;
		private ResourceProvider resourceProvider;
		private Boolean useSharedDatabase;
		private Boolean autoCloseIdleConnection;

		protected Builder() {
			// no-op
		}

		public Builder resourceProvider(ResourceProvider resourceProvider) {
			requireNonNull(resourceProvider);
			checkState("Resource provider already set", this.resourceProvider==null);

			this.resourceProvider = resourceProvider;

			return this;
		}

		public Builder folder(Path rootFolder) {
			requireNonNull(rootFolder);
			checkState("Root folder already set", this.rootFolder==null);

			this.rootFolder = rootFolder;

			return this;
		}

		public Builder autoCloseIdleConnection(boolean autoCloseIdleConnection) {
			checkState("AutoCloseIdleConnection already set", this.autoCloseIdleConnection==null);

			this.autoCloseIdleConnection = autoCloseIdleConnection==DEFAULT_AUTOCLOSE_IDE_CONNECTION ? null
					: Boolean.valueOf(autoCloseIdleConnection);

			return this;
		}

		public Builder useSharedDatabase(boolean useSharedDatabase) {
			checkState("UseSharedDatabase already set", this.useSharedDatabase==null);

			this.useSharedDatabase = useSharedDatabase==DEFAULT_USE_SHARED_DATABASE ? null
					: Boolean.valueOf(useSharedDatabase);

			return this;
		}

		public Path getRootFolder() {
			return rootFolder;
		}

		public ResourceProvider getResourceProvider() {
			return resourceProvider;
		}

		public boolean isUseSharedDatabase() {
			return useSharedDatabase==null ? DEFAULT_USE_SHARED_DATABASE : useSharedDatabase.booleanValue();
		}

		public boolean isAutoCloseIdleConnection() {
			return autoCloseIdleConnection==null ? DEFAULT_AUTOCLOSE_IDE_CONNECTION : autoCloseIdleConnection.booleanValue();
		}

		protected void validate() {
			checkState("Root folder not set", rootFolder!=null);
			checkState("Resource provider not set", resourceProvider!=null);
		}

		public DBIdentifiableResolver build() {
			validate();

			return new DBIdentifiableResolver(this);
		}
	}
}

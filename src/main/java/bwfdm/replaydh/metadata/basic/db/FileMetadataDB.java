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
package bwfdm.replaydh.metadata.basic.db;

import static java.util.Objects.requireNonNull;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;

import bwfdm.replaydh.db.DBUtils;
import bwfdm.replaydh.metadata.MetadataException;
import bwfdm.replaydh.metadata.MetadataRecord.UID;
import bwfdm.replaydh.metadata.basic.UIDStorage;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.schema.IdentifierType;

/**
 * @author Markus Gärtner
 *
 */
public class FileMetadataDB implements UIDStorage {

	/**
	 * Physical location of database file
	 */
	private final String path;

	/**
	 * Currently active connection to database
	 */
	private volatile Connection connection;

	private final Object lock = new Object();

	private static final String UID_TABLE = "uidMap";

	/**
	 * Creates a FileMetadataDB that uses the given {@code path}
	 * to locate a sqlite database.
	 */
	public FileMetadataDB(String path) {
		this.path = requireNonNull(path);
	}

	/**
	 * Creates a FileMetadataDB for testing that uses an in-memory
	 * sqlite database
	 */
	public FileMetadataDB() {
		path = null;
	}

	private Connection getConnection() {
		Connection result = connection;
		if(result==null) {
			synchronized (lock) {
				if((result = connection) != null) {

					if(path==null) {
						result = openVirtual();
					} else {
						result = open(path);
					}

					initDB(result);

					connection = result;
				}
			}
		}

		return result;
	}

	private static Connection open(String path) {
		try {
			return DriverManager.getConnection("jdbc:sqlite:"+path);
		} catch (SQLException e) {
			throw new MetadataException("Failed to open SQLite database for path: "+path, e);
		}
	}

	private static Connection openVirtual() {
		try {
			return DriverManager.getConnection("jdbc:sqlite::memory");
		} catch (SQLException e) {
			throw new MetadataException("Failed to open in-memory SQLite database", e);
		}

	}

	private void initDB(Connection connection) {
		Statement statement = null;
		try {
			statement = connection.createStatement();

			statement.executeQuery(
					"CREATE TABLE IF NOT EXISTS entities ("
					+ "id integer PRIMARY KEY,"
					+ "uniqueName text NOT NULL UNIQUE"
					+ ")");
			connection.createStatement().executeQuery(
					"CREATE TABLE IF NOT EXISTS TableName ("
					+ "identifier text NOT NULL,"
					+ "type text NOT NULL,"
					+ "type text NOT NULL,"
					+ ")");
		} catch (SQLException e) {
			throw new MetadataException("Failed to create default tables", e);
		} finally {
			DBUtils.closeSilently(statement);
		}
	}

	/**
	 * Attempts to commit pending changes and then closes the connection.
	 *
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() {
		try {
			DBUtils.commitSilently(connection);
			DBUtils.closeSilently(connection);
		} finally {
			connection = null;
		}
	}

	private void collectUsableIdentifiers(Identifiable identifiable, Consumer<? super Identifier> collector) {
		for(Identifier identifier : identifiable.getIdentifiers()) {
			IdentifierType type = identifier.getType();
		}
	}

	/**
	 * @see bwfdm.replaydh.metadata.basic.UIDStorage#hasUID(bwfdm.replaydh.workflow.Identifiable)
	 */
	@Override
	public boolean hasUID(Identifiable resource) {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * @see bwfdm.replaydh.metadata.basic.UIDStorage#getUID(bwfdm.replaydh.workflow.Identifiable)
	 */
	@Override
	public UID getUID(Identifiable resource) {
		// TODO Auto-generated method stub
		return null;
	}

//	/**
//	 * @see bwfdm.replaydh.metadata.basic.UIDStorage#createUID(bwfdm.replaydh.workflow.Identifiable)
//	 */
//	@Override
//	public UID createUID(Identifiable resource) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	/**
	 * @see bwfdm.replaydh.metadata.basic.UIDStorage#deleteUID(bwfdm.replaydh.workflow.Identifiable)
	 */
	@Override
	public void deleteUID(Identifiable resource) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see bwfdm.replaydh.metadata.basic.UIDStorage#addUID(bwfdm.replaydh.workflow.Identifiable, bwfdm.replaydh.metadata.MetadataRecord.UID)
	 */
	@Override
	public void addUID(Identifiable resource, UID uid) {
		// TODO Auto-generated method stub

	}

	/**
	 * @see bwfdm.replaydh.metadata.basic.UIDStorage#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}
}

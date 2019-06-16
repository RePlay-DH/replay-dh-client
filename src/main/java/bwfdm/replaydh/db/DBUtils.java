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
package bwfdm.replaydh.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Markus Gärtner
 *
 */
public class DBUtils {

	private static final Logger log = LoggerFactory.getLogger(DBUtils.class);

	static {
		try {
			Class.forName("org.sqlite.JDBC");
		} catch (ClassNotFoundException e) {
			log.error("Failed to load SQLite driver library", e);

			throw new InternalError("DBUtils: unable to load SQLite driver", e);
		}
	}

	public static Connection connect(String url) throws SQLException {
		return DriverManager.getConnection(url);
	}

	public static void closeSilently(Connection connection) {
		if(connection!=null) {
			try {
				connection.close();
			} catch (SQLException e) {
				log.error("Failed to close SQL connection", e);
			}
		}
	}

	public static void closeSilently(Statement statement) {
		if(statement!=null) {
			try {
				statement.close();
			} catch (SQLException e) {
				log.error("Failed to close SQL statement", e);
			}
		}
	}

	public static void closeSilently(ResultSet resultSet) {
		if(resultSet!=null) {
			try {
				resultSet.close();
			} catch (SQLException e) {
				log.error("Failed to close SQL result set", e);
			}
		}
	}

	public static void commitSilently(Connection connection) {
		if(connection!=null) {
			try {
				if(!connection.getAutoCommit()) {
					connection.commit();
				}
			} catch (SQLException e) {
				log.error("Failed to commit SQL connection", e);
			}
		}
	}

	private static final StringBuilder sb = new StringBuilder();

	public static String escape(String s) {
		synchronized (sb) {
			sb.setLength(0);

			for(int i=0; i<s.length(); i++) {
				char c = s.charAt(i);

				if(c=='\'') {
					sb.append('\'');
				}

				sb.append(c);
			}

			if(sb.length()!=s.length()) {
				s = sb.toString();
			}

			return s;
		}
	}

	public static String unescape(String s) {
		synchronized (sb) {
			sb.setLength(0);

			boolean escaped = false;

			for(int i=0; i<s.length(); i++) {
				char c = s.charAt(i);

				if(!escaped &&  c=='\'') {
					escaped = true;
				} else {
					escaped = false;
					sb.append(c);
				}
			}

			if(sb.length()!=s.length()) {
				s = sb.toString();
			}

			return s;
		}
	}
}

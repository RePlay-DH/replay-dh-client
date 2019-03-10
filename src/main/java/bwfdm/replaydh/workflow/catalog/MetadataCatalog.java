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
package bwfdm.replaydh.workflow.catalog;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * Defines the query interface for accessing entries in the
 * catalog for process metadata.
 *
 * @author Markus Gärtner
 *
 */
public interface MetadataCatalog {

	public static final String TITLE_KEY = "title";
	public static final String DESCRIPTION_KEY = "description";
	public static final String ROLE_KEY = "role";
	public static final String TYPE_KEY = "type";
	public static final String ENVIRONMENT_KEY = "environment";
	public static final String PARAMETERS_KEY = "parameters";

	/**
	 * Magic {@code key} string to signal that the {@link #suggest(QuerySettings, Identifiable, String, String) suggest}
	 * method should not restrict the search to a specific key, but instead return
	 * all candidates it can find for all stored keys.
	 */
//	public static final String ANY = new StringBuilder().toString();

	/**
	 * Simple search "google style".
	 * <p>
	 * The catalog will use the given {@code fragment} to run a full text search
	 * over value fields in all entries and return any record for which at least
	 * one entry scored a hit.
	 *
	 * @param settings
	 * @param fragment
	 * @return
	 * @throws CatalogException
	 */
	Result query(QuerySettings settings, String fragment) throws CatalogException;

	/**
	 * Complex "database" search.
	 * <p>
	 * Finds all the records for which all the specified constraints based on their
	 * respective entries are fulfilled.
	 *
	 * @param settings
	 * @param constraints
	 * @return
	 * @throws CatalogException
	 */
	Result query(QuerySettings settings, List<Constraint> constraints) throws CatalogException;

	/**
	 * Asks the catalog for suggestions usable in an auto-completion scenario.
	 *
	 * @param settings  general constraints such as a {@link WorkflowSchema} or result size limit
	 * @param context  optional hint for the catalog for which type of resource/person/tool suggestions
	 * should be gathered. If {@code null} then search will be performed solely based on {@code key}
	 * @param key  the property for which to gather values
	 * @param valuePrefix  a filter to restrict the search to only values that start with this prefix. If
	 * {@code null} or {@link String#isEmpty() empty} then arbitrary values for the specified key will be picked.
	 * @return A non-null but potentially empty list of values found for the specified property based on the parameters
	 * @throws CatalogException
	 */
	List<String> suggest(QuerySettings settings, Identifiable context, String key, String valuePrefix) throws CatalogException;

	/**
	 * An immutable instance of {@link QuerySettings} that can be shared
	 * and used as default value.
	 */
	public static final QuerySettings EMPTY_SETTINGS = new QuerySettings() {
		@Override
		boolean canEdit() {
			return false;
		}
	};

	/**
	 * Models a constraint for restricting the search to specific values of a
	 * certain attribute.
	 *
	 * @author Markus Gärtner
	 *
	 */
	public static class Constraint {
		private final String key;
		private final String value;

		private static String check(String s) {
			requireNonNull(s);
			checkArgument("Keys and values must not be empty", !s.isEmpty());

			return s.intern();
		}

		public Constraint(String key, String value) {
			this.key = check(key);
			this.value = check(value);
		}

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return key+"="+value;
		}
	}

	public static final int DEFAULT_RESULT_LIMIT = 20;

	public static QuerySettings settings() {
		return new QuerySettings();
	}

	/**
	 * Additional settings to customize the behavior of the query engine in
	 * a catalog.
	 * <p>
	 * Each instance of this class is intended to be used only for a single
	 * invocation of a query method. An exception is the global sharable
	 * {@link MetadataCatalog#EMPTY_SETTINGS} object.
	 *
	 * @author Markus Gärtner
	 *
	 */
	public static class QuerySettings {
		private WorkflowSchema schema = null;
		private int resultLimit = DEFAULT_RESULT_LIMIT;

		boolean canEdit() {
			return true;
		}

		private void checkEditable() {
			checkState("Cannot edit query settings", canEdit());
		}

		public WorkflowSchema getSchema() {
			return schema;
		}

		public QuerySettings setSchema(WorkflowSchema schema) {
			checkEditable();
			this.schema = requireNonNull(schema);
			return this;
		}

		public int getResultLimit() {
			return resultLimit;
		}

		public QuerySettings setResultLimit(int resultLimit) {
			checkEditable();
			checkArgument("Result limit must be greater than 0: "+resultLimit, resultLimit>0);
			this.resultLimit = resultLimit;
			return this;
		}


	}

	public static final Result EMPTY_RESULT = new Result() {

		@Override
		public Iterator<Identifiable> iterator() {
			return Collections.emptyIterator();
		}

		@Override
		public boolean isEmpty() {
			return true;
		}
	};

	/**
	 * Abstraction for results of querying a {@link MetadataCatalog}.
	 *
	 * @author Markus Gärtner
	 *
	 */
	interface Result extends Iterable<Identifiable> {

		/**
		 * Returns {@code true} if this result has no entries.
		 * @return
		 */
		boolean isEmpty();

		/**
		 * {@inheritDoc}
		 * <p>
		 * This method should be invoked at most once during the
		 * lifetime of a {@link Result} object!
		 *
		 * @see java.lang.Iterable#iterator()
		 */
		@Override
		Iterator<Identifiable> iterator();

		default List<Identifiable> asList() {
			if(isEmpty()) {
				return Collections.emptyList();
			} else {
				List<Identifiable> tmp = new ArrayList<>();
				for (Iterator<Identifiable> it = iterator(); it.hasNext();) {
					tmp.add(it.next());
				}
				return tmp;
			}
		}
	}
}

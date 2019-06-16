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
package bwfdm.replaydh.utils;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import bwfdm.replaydh.core.AbstractRDHTool;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public abstract class AbstractSchemaManager<S extends SchemaManager.Schema>
		extends AbstractRDHTool implements SchemaManager<S> {


	/**
	 * Currently assigned default schema.
	 * <p>
	 * Initialized with the shared {@link WorkflowSchema#getDefaultSchema() default schema}.
	 */
	private S defaultSchema = null;

	/**
	 * Maps schema ids to the respective schema instance.
	 */
	private final Map<String, S> schemas = new HashMap<>();

	/**
	 * Lock for any access to the schema map or the default schema field.
	 */
	protected final Object lock = new Object();

	/**
	 * @throws RDHLifecycleException
	 * @see bwfdm.replaydh.core.AbstractRDHTool#stop(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public void stop(RDHEnvironment environment) throws RDHLifecycleException {
		synchronized (lock) {
			schemas.clear();
			defaultSchema = null;
		}

		super.stop(environment);
	}

	protected Set<String> getExternalSchemaIds() {
		return Collections.unmodifiableSet(schemas.keySet());
	}

	@Override
	public S lookupSchema(String schemaId) {
		requireNonNull(schemaId);

		if(defaultSchema!=null && defaultSchema.getId().equals(schemaId)) {
			return defaultSchema;
		}

		synchronized (lock) {
			return schemas.get(schemaId);
		}
	}

	private String checkId(S schema) {
		String id = schema.getId();
		checkArgument("Invalid schema id - must not be null or empty", id!=null && !id.isEmpty());
		return id;
	}

	@Override
	public void addSchema(S schema) {
		requireNonNull(schema);

		synchronized (lock) {
			String key = checkId(schema);
			S oldSchema = schemas.putIfAbsent(key, schema);
			if(oldSchema!=null && oldSchema!=schema)
				throw new IllegalArgumentException("Duplicate schemas for id: "+key);
		}
	}

	@Override
	public void removeSchema(S schema) {
		requireNonNull(schema);

		synchronized (lock) {
			String key = checkId(schema);
			if(!schemas.remove(key, schema))
				throw new IllegalArgumentException("Unknown schema: "+key);
		}
	}

	@Override
	public void setDefaultSchema(S schema) {
		synchronized (lock) {
			defaultSchema = schema;
		}
	}

	/**
	 * Hook for subclasses to provide a fallback schema  in case looking up
	 * a schema by id fails.
	 * @return
	 */
	protected S getFallbackSchema() {
		return null;
	}

	@Override
	public S getDefaultSchema() {
		synchronized (lock) {
			S result = defaultSchema;
			if(result==null) {
				result = getFallbackSchema();
			}

			return result;
		}
	}

	@Override
	public Set<String> getAvailableSchemaIds() {
		Set<String> ids = new HashSet<>(getExternalSchemaIds());
		S fallback = getFallbackSchema();
		if(fallback!=null) {
			ids.add(fallback.getId());
		}
		return ids;
	}
}

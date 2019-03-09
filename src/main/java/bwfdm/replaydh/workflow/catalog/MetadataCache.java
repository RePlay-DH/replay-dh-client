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
import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowStep;

/**
 * @author Markus Gärtner
 *
 */
public class MetadataCache implements MetadataCatalog {

	//TODO currently we use the loca-independent strings for keys. but should we maybe change since we're only working on "live" data anyway?

	public static final int MAX_CACHED_TEXT_LENGTH = 150;

	private final Map<String, NavigableSet<String>> propertyCache = new HashMap<>(100);

	private final Object lock = new Object();

	public void clear() {
		synchronized (lock) {
			propertyCache.values().forEach(Collection::clear);
			propertyCache.clear();
		}

		//TODO cleanup any further storage structrues we're using
	}

	public void reload(Workflow workflow) {
		synchronized (lock) {
			clear();

			// Only access workflow data if it's still 'alive'
			if(!workflow.isClosed()) {
				workflow.forEachStep(this::addWorkflowStep0);
			}
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.catalog.MetadataCatalog#query(bwfdm.replaydh.workflow.catalog.MetadataCatalog.QuerySettings, java.lang.String)
	 */
	@Override
	public Result query(QuerySettings settings, String fragment) throws CatalogException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see bwfdm.replaydh.workflow.catalog.MetadataCatalog#query(bwfdm.replaydh.workflow.catalog.MetadataCatalog.QuerySettings, java.util.List)
	 */
	@Override
	public Result query(QuerySettings settings, List<Constraint> constraints) throws CatalogException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see bwfdm.replaydh.workflow.catalog.MetadataCatalog#suggest(bwfdm.replaydh.workflow.catalog.MetadataCatalog.QuerySettings, bwfdm.replaydh.workflow.Identifiable, java.lang.String, java.lang.String)
	 */
	@Override
	public List<String> suggest(QuerySettings settings, Identifiable context, String key, String valuePrefix)
			throws CatalogException {
		requireNonNull(settings, "Settings must not be null");
		requireNonNull(key, "Key must not be null");

		key = key.trim();
		checkArgument("Key must not be empty", !key.isEmpty());

		synchronized (lock) {
			NavigableSet<String> rawCache = propertyCache.get(key);
			if(rawCache==null)
				return Collections.emptyList();

			NavigableSet<String> source = rawCache;

			if(valuePrefix!=null && !valuePrefix.trim().isEmpty()) {
				source = rawCache.subSet(valuePrefix, true, valuePrefix+Character.MAX_VALUE, false);
			}

			int resultSize = Math.min(settings.getResultLimit(), source.size());

			if(resultSize==0)
				return Collections.emptyList();

			return source.stream()
					.limit(resultSize)
					.collect(Collectors.toList());
		}
	}

	public void addWorkflowStep(WorkflowStep step) {
		synchronized (lock) {
			addWorkflowStep0(step);
		}
	}

	public void removeWorkflowStep(WorkflowStep step) {
		synchronized (lock) {
			//TODO remove identifiables?
		}
	}

	public void updateWorkflowStep(WorkflowStep step) {
		synchronized (lock) {
			updateWorkflowStep0(step);
		}
	}

	private void addWorkflowStep0(WorkflowStep step) {
		//TODO add identifiables to storage

		updateWorkflowStep0(step);
	}

	private void updateWorkflowStep0(WorkflowStep step) {
		storeProperty(TITLE_KEY, step.getTitle());
		storeProperty(DESCRIPTION_KEY, step.getDescription());

		step.forEachIdentifiable(this::storeIdentifiable);
	}

	private void storeIdentifiable(Identifiable identifiable) {
		storeProperty(DESCRIPTION_KEY, identifiable.getDescription());
		identifiable.forEachIdentifier(this::storeIdentifier);
	}

	private void storeIdentifier(Identifier identifier) {
		storeProperty(identifier.getType().getLabel(), identifier.getId());
	}

	private void storeProperty(String key, String value) {
		if(value==null) {
			return;
		}

		if(value.length()>MAX_CACHED_TEXT_LENGTH) {
			return;
		}

		propertyCache.computeIfAbsent(key, k -> new TreeSet<>()).add(value);
	}
}

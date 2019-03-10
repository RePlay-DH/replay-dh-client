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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import bwfdm.replaydh.utils.IdentityHashSet;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Person;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.Tool;
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

	private final Set<Identifiable> identifiableCache = new IdentityHashSet<>(200);

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
			if(workflow!=null && !workflow.isClosed()) {
				workflow.forEachStep(this::addWorkflowStep0);
			}
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.catalog.MetadataCatalog#query(bwfdm.replaydh.workflow.catalog.MetadataCatalog.QuerySettings, java.lang.String)
	 */
	@Override
	public Result query(QuerySettings settings, String fragment) throws CatalogException {
		requireNonNull(settings, "Settings must not be null");
		requireNonNull(fragment, "Fragment must not be null");

		fragment = fragment.trim();
		checkArgument("Fragment must not be empty", !fragment.isEmpty());

		return scan(settings, fullTextScanner(fragment));
	}

	/**
	 * @see bwfdm.replaydh.workflow.catalog.MetadataCatalog#query(bwfdm.replaydh.workflow.catalog.MetadataCatalog.QuerySettings, java.util.List)
	 */
	@Override
	public Result query(QuerySettings settings, List<Constraint> constraints) throws CatalogException {
		requireNonNull(settings, "Settings must not be null");
		requireNonNull(constraints, "Constraint list must not be null");
		checkArgument("Constraint list must not be empty", !constraints.isEmpty());

		return scan(settings, fromConstraints(constraints));
	}

	private static Predicate<Identifiable> fromConstraints(List<Constraint> constraints) {
		return new ConstraintScanner(constraints);
	}

	private static Predicate<Identifiable> fullTextScanner(String value) {
		return new FullTextScanner(value);
	}

	private Result scan(QuerySettings settings, Predicate<? super Identifiable> constraint) {
		List<Identifiable> hits = identifiableCache.parallelStream() // needs testing to make sure we're not suffocating the EDT
			.filter(constraint)
			.limit(settings.getResultLimit())
			.collect(Collectors.toList());

		return new SimpleResult(hits);
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
			step.forEachIdentifiable(identifiableCache::remove);
		}
	}

	public void updateWorkflowStep(WorkflowStep step) {
		synchronized (lock) {
			updateWorkflowStep0(step);
		}
	}

	private void addWorkflowStep0(WorkflowStep step) {
		step.forEachIdentifiable(identifiableCache::add);

		updateWorkflowStep0(step);
	}

	private void updateWorkflowStep0(WorkflowStep step) {
		storeProperty(TITLE_KEY, step.getTitle());
		storeProperty(DESCRIPTION_KEY, step.getDescription());

		step.forEachIdentifiable(this::storeIdentifiable);
	}

	private void storeIdentifiable(Identifiable identifiable) {
		storeProperty(DESCRIPTION_KEY, identifiable.getDescription());

		switch (identifiable.getType()) {
		case PERSON:
			storeProperty(ROLE_KEY, ((Person)identifiable).getRole());
			break;

		case TOOL: {
			Tool tool = (Tool) identifiable;
			storeProperty(ENVIRONMENT_KEY, tool.getEnvironment());
			storeProperty(PARAMETERS_KEY, tool.getParameters());
		} // fall-through to RESOURCE for the type property

		case RESOURCE:
			storeProperty(TYPE_KEY, ((Resource)identifiable).getResourceType());
			break;

		default:
			throw new IllegalArgumentException("Identifiable type not handled yet: "+identifiable.getType());
		}

		identifiable.forEachIdentifier(this::storeIdentifier);
	}

	private void storeIdentifier(Identifier identifier) {
		storeProperty(identifier.getType().getLabel(), identifier.getId());
	}

	private void storeProperty(String key, String value) {
		if(value==null || value.isEmpty()) {
			return;
		}

		if(value.length()>MAX_CACHED_TEXT_LENGTH) {
			return;
		}

		propertyCache.computeIfAbsent(key, k -> new TreeSet<>()).add(value);
	}

	/**
	 * Change here if we ever want to adjust the matching policy into a more
	 * restrictive one compared to the contianment check.
	 *
	 * @param constraint
	 * @param target
	 * @return
	 */
	private static boolean matches(String constraint, String target) {
		return target!=null && !target.isEmpty() && target.contains(constraint);
	}

	private static class FullTextScanner implements Predicate<Identifiable> {
		private final String value;
		private final Predicate<Identifier> identifierCheck;

		FullTextScanner(String value) {
			this.value = requireNonNull(value);
			identifierCheck = this::checkIdentifier;
		}

		private boolean checkIdentifier(Identifier identifier) {
			return checkValue(identifier.getId());
		}

		private boolean checkValue(String target) {
			return matches(value, target);
		}

		private boolean checkTypeSpecific(Identifiable target) {
			switch (target.getType()) {
			case PERSON: return checkValue(((Person)target).getRole());
			case RESOURCE: return checkValue(((Resource)target).getResourceType());
			case TOOL: {
				Tool tool = (Tool)target;
				return checkValue(tool.getResourceType())
						|| checkValue(tool.getEnvironment())
						|| checkValue(tool.getParameters());
			}

			default:
				throw new IllegalArgumentException("Identifiable type not handled yet: "+target.getType());
			}
		}

		/**
		 * @see java.util.function.Predicate#test(java.lang.Object)
		 */
		@Override
		public boolean test(Identifiable target) {
			return checkTypeSpecific(target)
					|| checkValue(target.getDescription())
					|| target.hasIdentifier(identifierCheck);
		}
	}

	private static class ConstraintScanner implements Predicate<Identifiable> {
		private final Predicate<Identifiable>[] checks;

		@SuppressWarnings("unchecked")
		ConstraintScanner(List<Constraint> constraints) {
			List<Predicate<Identifiable>> tmp = new ArrayList<>();

			for(Constraint constraint : constraints) {
				switch (constraint.getKey()) {
				case ROLE_KEY:
					tmp.add(new TypeSpecificConstraint<>(constraint, Person.class, Person::getRole));
					break;
				case ENVIRONMENT_KEY:
					tmp.add(new TypeSpecificConstraint<>(constraint, Tool.class, Tool::getEnvironment));
					break;
				case PARAMETERS_KEY:
					tmp.add(new TypeSpecificConstraint<>(constraint, Tool.class, Tool::getParameters));
					break;
				case TYPE_KEY:
					tmp.add(new TypeSpecificConstraint<>(constraint, Resource.class, Resource::getResourceType));
					break;
				case DESCRIPTION_KEY:
					tmp.add(i -> matches(constraint.getValue(), i.getDescription()));
					break;

				default:
					tmp.add(new IdentifierConstraint(constraint));
					break;
				}
			}

			checks = new Predicate[tmp.size()];
			tmp.toArray(checks);
		}

		@Override
		public boolean test(Identifiable target) {
			for(Predicate<Identifiable> check : checks) {
				if(check.test(target)) {
					return true;
				}
			}
			return false;
		}
	}

	private static class IdentifierConstraint implements Predicate<Identifiable> {
		protected final String key, value;

		IdentifierConstraint(Constraint constraint) {
			requireNonNull(constraint);
			this.key = requireNonNull(constraint.getKey());
			this.value = requireNonNull(constraint.getValue());
		}

		@Override
		public boolean test(Identifiable target) {
			Identifier id = target.getIdentifier(key);
			return id!=null && matches(value, id.getId());
		}
	}

	private static class TypeSpecificConstraint<I extends Identifiable> extends IdentifierConstraint {
		private final Class<I> clazz;
		private final Function<I, String> getter;

		TypeSpecificConstraint(Constraint constraint, Class<I> clazz, Function<I, String> getter) {
			super(constraint);

			this.clazz = requireNonNull(clazz);
			this.getter = requireNonNull(getter);
		}

		@Override
		public boolean test(Identifiable target) {
			if(clazz.isInstance(target)) {
				return matches(value, getter.apply(clazz.cast(target)));
			} else {
				return super.test(target);
			}
		}

	}
}

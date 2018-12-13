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

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static java.util.Objects.requireNonNull;

import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.core.RDHTool;
import bwfdm.replaydh.utils.LookupResult;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifiable.Type;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Person;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.Tool;
import bwfdm.replaydh.workflow.WorkflowStep;

/**
 * @author Markus Gärtner
 *
 */
public interface IdentifiableResolver extends RDHTool {

	/**
	 * Directly fetches a previously registered {@link Identifiable} that
	 * is mapped to the given {@link UUID systemId}.
	 *
	 * @param systemId
	 * @return
	 *
	 * @see Identifiable#getSystemId()
	 * @see UUID
	 */
	<I extends Identifiable> I lookup(UUID systemId);

	/**
	 * Tries to find known {@link Identifiable} instances that match
	 * the given {@link Set} of {@link Identifier identifiers}. The
	 * list is allowed to be empty, indicating that the given identifiers
	 * have not previously been mapped to any known identifiable.
	 * <p>
	 * The returned list is expected to be ordered according to the
	 * relevance of its contained {@link LookupResult result} entries.
	 * The most relevant entry should be at position {@code 0}.
	 * <p>
	 * If the resolution process failed, the returned list is expected
	 * to only contain a single entry that provides the {@link LookupResult#getException() exception}
	 * that caused the failure.
	 *
	 * @param identifiers
	 * @return
	 */
	List<LookupResult<Identifiable, Set<Identifier>>> resolve(int candidateLimit, Set<Identifier> identifiers);

	/**
	 * Wraps the given {@link Identifier identifiers} into a
	 * {@link Set} and delegates to {@link #resolve(Set)}.
	 *
	 * @param identifiers
	 * @return
	 */
	default List<LookupResult<Identifiable, Set<Identifier>>> resolve(int candidateLimit, Identifier...identifiers) {
		requireNonNull(identifiers);
		checkArgument("Array of supplied identifiers must not be empty", identifiers.length>0);

		Set<Identifier> tmp;
		if(identifiers.length==1) {
			tmp = Collections.singleton(identifiers[0]);
		} else {
			tmp = new HashSet<>();
			Collections.addAll(tmp, identifiers);
		}

		return resolve(candidateLimit, tmp);
	}

	/**
	 * Establish a mapping in this resolver so that subsequent calls to
	 * {@link #resolve(int, Set)} will be able to find the identifiable
	 * instances provided to this method.
	 *
	 * @param identifiables
	 */
	void register(Set<? extends Identifiable> identifiables);

	default void register(Identifiable...identifiables) {
		requireNonNull(identifiables);
		checkArgument("Array of supplied identifiables must not be empty", identifiables.length>0);

		Set<Identifiable> tmp;
		if(identifiables.length==1) {
			tmp = Collections.singleton(identifiables[0]);
		} else {
			tmp = new HashSet<>();
			Collections.addAll(tmp, identifiables);
		}

		register(tmp);
	}

	void unregister(Set<? extends Identifiable> identifiables);

	void update(Set<? extends Identifiable> identifiables);

	default void update(WorkflowStep step) {
		Set<Resource> input = step.getInput();
		if(!input.isEmpty()) {
			register(input);
		}
		Set<Resource> output = step.getOutput();
		if(!output.isEmpty()) {
			register(output);
		}
		Set<Person> persons = step.getPersons();
		if(!persons.isEmpty()) {
			register(persons);
		}
		Tool tool = step.getTool();
		if(tool!=null) {
			register(tool);
		}
	}

	/**
	 * Signals the start of a resolution process. Implementations
	 * should start to acquire locks on shared resources and/or
	 * open related resources, so that subsequent calls to
	 * {@link #resolve(Set)} will be able to finish without expensive
	 * I/O initialization operations.
	 * <p>
	 * Note that each call to this method must be paired with a
	 * matching invocation of {@link #unlock()} to not compromise
	 * locks!
	 * A typical usage scenario would look like the following:
	 * <pre>
	 * Identifiable target = null;
	 * ...
	 * resolver.lock(); // lock resource
	 * try {
	 *   target = resolver.resolve(mySetOfIdentifiers);
	 * } finally {
	 *   resolver.unlock(); // free resource
	 * }
	 * ... // process target
	 * </pre>
	 * <p>
	 * Note that is implementation specific whether or not locking
	 * is reentrant.
	 *
	 * @see #unlock()
	 */
	void lock();

	/**
	 * Releases associated resources and locks.
	 *
	 * @see #lock()
	 */
	void unlock();

	/**
	 *
	 * @param type
	 * @return
	 */
	Iterator<Identifiable> identifiablesForType(Identifiable.Type type);

	public static final IdentifiableResolver NOOP_RESOLVER = new IdentifiableResolver() {

		@Override
		public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
			// no-op
		}

		@Override
		public void removePropertyChangeListener(PropertyChangeListener listener) {
			// no-op
		}

		@Override
		public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
			// no-op
		}

		@Override
		public void addPropertyChangeListener(PropertyChangeListener listener) {
			// no-op
		}

		@Override
		public void stop(RDHEnvironment environment) throws RDHLifecycleException {
			// no-op
		}

		@Override
		public boolean start(RDHEnvironment environment) throws RDHLifecycleException {
			return true;
		}

		@Override
		public void update(Set<? extends Identifiable> identifiables) {
			// no-op
		}

		@Override
		public void unregister(Set<? extends Identifiable> identifiables) {
			// no-op
		}

		@Override
		public void unlock() {
			// no-op
		}

		@Override
		public List<LookupResult<Identifiable, Set<Identifier>>> resolve(int candidateLimit, Set<Identifier> identifiers) {
			return Collections.emptyList();
		}

		@Override
		public void register(Set<? extends Identifiable> identifiables) {
			// no-op
		}

		@Override
		public <I extends Identifiable> I lookup(UUID systemId) {
			return null;
		}

		@Override
		public void lock() {
			// no-op
		}

		@Override
		public Iterator<Identifiable> identifiablesForType(Type type) {
			return null;
		}
	};
}

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
import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.core.AbstractRDHTool;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.io.resources.FileResourceProvider;
import bwfdm.replaydh.io.resources.IOResource;
import bwfdm.replaydh.io.resources.IOWorker;
import bwfdm.replaydh.io.resources.ResourceProvider;
import bwfdm.replaydh.utils.LookupResult;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifiable.Type;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.impl.AbstractIdentifiable;
import bwfdm.replaydh.workflow.impl.DefaultPerson;
import bwfdm.replaydh.workflow.impl.DefaultResource;
import bwfdm.replaydh.workflow.impl.DefaultTool;
import bwfdm.replaydh.workflow.schema.IdentifierSchema;
import bwfdm.replaydh.workflow.schema.IdentifierType;
import bwfdm.replaydh.workflow.schema.SchemaManager;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public class LocalIdentifiableResolver extends AbstractRDHTool implements IdentifiableResolver {

	private static final Logger log = LoggerFactory.getLogger(LocalIdentifiableResolver.class);

	public static Builder newBuilder() {
		return new Builder();
	}

	private static final String DEFAULT_LOCAL_CACHE_FILENAME = "local-cache.id.ini";

	public static final String DEFAULT_CACHE_FILE_ENDING = ".id.xml";

	public static final String NO_SCHEMA_ID = "no-schema";

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
	 * Location of local cache data.
	 */
	private final Path cacheFile;

	private final boolean autoPerformCacheSerialization;

	/**
	 * Lock for synchronizing lookup/query access
	 */
	private final ReentrantLock lock = new ReentrantLock();

	private final Map<IdentifiableProxy, IdentifierSet> target2IdLookup = new HashMap<>(1<<6);
	private final Map<IdentifierProxy, CompactSet<IdentifiableProxy>> id2TargetLookup = new HashMap<>(1<<8);

//	private static XMLOutputFactory xmlOutputFactory;
//	private static SAXParserFactory parserFactory;

	private final IOWorker<? super BiConsumer<IdentifiableProxy, IdentifierSet>> reader;
	private final IOWorker<? super Collection<Entry<IdentifiableProxy, IdentifierSet>>> writer;

	/**
	 * Flag indicating that whenever an {@link IdentifierSet} is asked to return an
	 * instance of {@link Identifiable} and has no actual live one associated yet,
	 * a new {@code Identifiable} object is created based on the existing information.
	 */
	private final boolean autoCreateKnownMissingIdentifiables;

	protected LocalIdentifiableResolver(Builder builder) {
		requireNonNull(builder);

		rootFolder = builder.getRootFolder();
		resourceProvider = builder.getResourceProvider();
		autoPerformCacheSerialization = builder.isAutoPerformCacheSerialization();
		autoCreateKnownMissingIdentifiables = builder.isAutoCreateKnownMissingIdentifiables();

		reader = builder.getReader();
		writer = builder.getWriter();

		cacheFile = rootFolder.resolve(DEFAULT_LOCAL_CACHE_FILENAME);
	}

	@Override
	public void lock() {
		checkStarted();

		lock.lock();
	}

	@Override
	public void unlock() {
		assert lock.isHeldByCurrentThread();

		checkStarted();

		lock.unlock();
	}

	/**
	 * @return the autoPerformCacheSerialization
	 */
	public boolean isAutoPerformCacheSerialization() {
		return autoPerformCacheSerialization;
	}

	/**
	 * {@inheritDoc}
	 *
	 * If this resolver has been configured to automatically perform
	 * (de)serialization of the underlying cache file, then this method
	 * will try to {@link #readCache() read} the cached data.
	 * Any errors encountered in the process will only be logged.
	 * @throws RDHLifecycleException
	 *
	 * @see bwfdm.replaydh.core.AbstractRDHTool#start(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public boolean start(RDHEnvironment environment) throws RDHLifecycleException {
		if(!super.start(environment)) {
			return false;
		}

		if(autoPerformCacheSerialization) {
			try {
				readCache();
			} catch (ExecutionException e) {
				log.error("Failed to read cache from file: "+cacheFile, e.getCause());
			}
		}

		//TODO otherwise access the current workflow and store all of its identifiable objects?

		return true;
	}

	public void readCache() throws ExecutionException {

		BiConsumer<IdentifiableProxy, IdentifierSet> action = this::mapIdentifiable;

		if(resourceProvider.exists(cacheFile)) {
			IOResource resource;
			try {
				resource = resourceProvider.getResource(cacheFile);
			} catch (IOException e) {
				throw new ExecutionException("Unable to fetch resource for file: "+cacheFile, e);
			}
			reader.transform(resource, action);
		}
	}


	/**
	 * {@inheritDoc}
	 *
	 * If this resolver has been configured to automatically perform
	 * (de)serialization of the underlying cache file, then this method
	 * will try to {@link LocalIdentifiableResolver#writeCache() write}
	 * the cached data to the cache file.
	 * Any errors encountered in the process will only be logged.
	 * @throws RDHLifecycleException
	 *
	 * @see bwfdm.replaydh.core.AbstractRDHTool#stop(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public void stop(RDHEnvironment environment) throws RDHLifecycleException {

		if(autoPerformCacheSerialization) {
			try {
				writeCache();
			} catch (ExecutionException e) {
				log.error("Failed to serialize cache data to file: "+cacheFile, e.getCause());
			}
		}

		super.stop(environment);
	}

	/**
	 * Reads the content of the local cache file and stores its content in
	 * the internal live cache, overwriting duplicate entries.
	 *
	 * @throws IOException
	 * @throws XMLStreamException
	 */
	public void writeCache() throws ExecutionException {

		if(!target2IdLookup.isEmpty()) {
			IOResource resource;
			try {
				resourceProvider.create(cacheFile);

				resource = resourceProvider.getResource(cacheFile);
			} catch (IOException e) {
				throw new ExecutionException("Failed to ensure existance of file: "+cacheFile, e);
			}

			writer.transform(resource, target2IdLookup.entrySet());
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.resolver.IdentifiableResolver#lookup(java.util.UUID)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <I extends Identifiable> I lookup(UUID systemId) {
		assert lock.isHeldByCurrentThread();

		requireNonNull(systemId);

		Identifiable result = null;

		for(Identifiable.Type type : Identifiable.Type.values()) {
			IdentifiableProxy identifiableProxy = new IdentifiableProxy(systemId, type);
			IdentifierSet identifierSet = target2IdLookup.get(identifiableProxy);
			if(identifierSet!=null) {
				result = identifierSet.getIdentifiable();
				if(result!=null) {
					break;
				}
			}
		}

		return (I) result;
	}

	/**
	 * @see bwfdm.replaydh.workflow.resolver.IdentifiableResolver#update(bwfdm.replaydh.workflow.Identifiable)
	 */
	@Override
	public void update(Set<? extends Identifiable> identifiables) {
		update0(identifiables, true);
	}

	private void mapIdentifiable(IdentifiableProxy identifiableProxy, IdentifierSet identifierSet) {
		// ids->target reverse mappings
		for(IdentifierProxy identifierProxy : identifierSet) {
			targetsForIdentifier(identifierProxy, true, true).add(identifiableProxy);
		}

		target2IdLookup.put(identifiableProxy, identifierSet);
	}

	@Override
	public void register(Set<? extends Identifiable> identifiables) {
		update0(identifiables, false);
	}

	private void update0(Set<? extends Identifiable> identifiables, boolean updateOnly) {
		assert lock.isHeldByCurrentThread();

		requireNonNull(identifiables);
		checkState("Set of identifiables must not be emtpy", !identifiables.isEmpty());

		Set<IdentifierProxy> removedIdentifiers = new HashSet<>();
		Set<IdentifierProxy> addedIdentifiers = new HashSet<>();

		for(Identifiable identifiable : identifiables) {

			// Ignore all "empty" identifiables, since they only cause issues
			if(!identifiable.hasIdentifiers()) {
				continue;
			}

			IdentifiableProxy identifiableProxy = identifiableProxy(identifiable);
			IdentifierSet identifierSet = target2IdLookup.get(identifiableProxy);

			if(identifierSet==null) {
				// Resource unknown -> full fresh mapping
				if(updateOnly)
					throw new IllegalArgumentException("Cannot update unknown identifiable: "+identifiable);

				// target->ids mapping
				identifierSet = identifierSet(identifiable);

				mapIdentifiable(identifiableProxy, identifierSet);
			} else {
				// Resource already registered -> do a check against changes

				// Collect sets of removed and newly added identifiers
				removedIdentifiers.clear();
				identifierSet.forEach(removedIdentifiers::add);

				addedIdentifiers.clear();
				identifiable.forEachIdentifier(id -> {
					IdentifierProxy proxy = identifierProxy(id);
					addedIdentifiers.add(proxy);
					removedIdentifiers.remove(proxy);
				});
				identifierSet.forEach(addedIdentifiers::remove);

				// Remove mappings for all the identifiers that got removed
				for(IdentifierProxy identifierProxy : removedIdentifiers) {
					identifierSet.remove(identifierProxy);
					targetsForIdentifier(identifierProxy, false, true).remove(identifiableProxy);
				}

				// Add new mappings for added identifiers
				for(IdentifierProxy identifierProxy : addedIdentifiers) {
					identifierSet.add(identifierProxy);
					targetsForIdentifier(identifierProxy, true, true).add(identifiableProxy);
				}
			}

			// Finally make sure we refresh the reference
			identifierSet.refreshSource(identifiable);
		}
	}

	protected CompactSet<IdentifiableProxy> targetsForIdentifier(IdentifierProxy identifierProxy, boolean createIfMissing, boolean requireNonNull) {
		CompactSet<IdentifiableProxy> result = id2TargetLookup.get(identifierProxy);
		if(result==null && createIfMissing) {
			result = new CompactSet<>();
			id2TargetLookup.put(identifierProxy, result);
		}
		if(result==null && requireNonNull)
			throw new IllegalStateException("Missing target set for identifier: "+identifierProxy);
		return result;
	}

	@Override
	public void unregister(Set<? extends Identifiable> identifiables) {
		assert lock.isHeldByCurrentThread();

		for(Identifiable identifiable : identifiables) {
			IdentifiableProxy identifiableProxy = identifiableProxy(identifiable);
			IdentifierSet identifierSet = target2IdLookup.get(identifiableProxy);

			// If the identifiable is unknown, don't bother
			if(identifierSet==null) {
				continue;
			}

			// Erase all reverse mappings
			identifierSet.forEach(identifierProxy -> {
				targetsForIdentifier(identifierProxy, false, true).remove(identifiableProxy);
			});

			// Remove the identifier mapping as well
			identifierSet.refreshSource(null);
			identifierSet.clear();
			target2IdLookup.remove(identifiableProxy);
		}
	}

	public void clearCache() {
		assert lock.isHeldByCurrentThread();

		target2IdLookup.clear();
		id2TargetLookup.clear();
	}

	private void maybeInstantiateIdentifiable(IdentifiableProxy identifiableProxy, IdentifierSet identifierSet) {
		/*
		 * If we need to, create a new Identifiable, copy over all identifiers
		 * and assign it the current source for the given IdentifierSet
		 */
		if(identifierSet.getIdentifiable()==null && autoCreateKnownMissingIdentifiables) {
			Identifiable identifiable = identifiable(identifiableProxy);
			identifierSet.forEach(identifierProxy -> identifiable.addIdentifier(
					identifier(identifiableProxy, identifierProxy)));
			identifierSet.refreshSource(identifiable);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * Implementation note: This implementation does not rely on I/O work or any other
	 * external code when trying to resolve identifiers. Therefore the returned list
	 * of results will only contain {@link LookupResult#isValid() valid} {@link LookupResult}
	 * instances or be empty, but will never contain an entry which points to an
	 * {@link LookupResult#getException() exception}!
	 *
	 * @see bwfdm.replaydh.workflow.resolver.IdentifiableResolver#resolve(int, java.util.Set)
	 */
	@Override
	public List<LookupResult<Identifiable, Set<Identifier>>> resolve(int candidateLimit, final Set<Identifier> identifiers) {
		assert lock.isHeldByCurrentThread();

		requireNonNull(identifiers);
		checkArgument("Candidate limit must be greater or equal 1 - given value is "+candidateLimit, candidateLimit>0);
		checkArgument("Set of identifiers must not be empty", !identifiers.isEmpty());

		// Convert all input identifiers into proxies
		final Set<IdentifierProxy> identifierProxies = new HashSet<>();
		identifiers.forEach(id -> identifierProxies.add(identifierProxy(id)));

		// Keep track of the candidates we already looked at
		final Set<IdentifiableProxy> processedIdentifiables = new HashSet<>(Math.max(identifiers.size()*2, 30));

		// n-best result candidates
		final PriorityQueue<LookupResult<Identifiable, Set<Identifier>>> buffer = new PriorityQueue<>();

		for(IdentifierProxy identifierProxy : identifierProxies) {

			// Fetch previously registered targets for given identifier
			CompactSet<IdentifiableProxy> targets = targetsForIdentifier(identifierProxy, false, false);

			// Nothing to do if no target available
			if(targets==null || targets.isEmpty()) {
				continue;
			}

			// Try all mapped targets
			for(IdentifiableProxy identifiableProxy : targets) {

				// Only actually process targets we haven't seen before
				if(processedIdentifiables.add(identifiableProxy)) {
					IdentifierSet identifierSet = target2IdLookup.get(identifiableProxy);

					// Calculate relevance of the candidate
					double relevance = calcRelevance(identifiableProxy, identifierSet, identifierProxies);

					// Try to fetch an existing "live" identifiable
					Identifiable identifiable = identifierSet.getIdentifiable();
					if(identifiable==null) {
						// If no "live" identifiable has been registered yet, create a blank one
						identifiable = identifiable(identifiableProxy);
					}

					// Push result into n-best buffer
					LookupResult<Identifiable, Set<Identifier>> result = new LookupResultImpl(identifiers, identifiable, relevance);
					buffer.add(result);

					// If candidate buffer exceeds limit, delete the head which is the least relevant entry
					if(buffer.size()>candidateLimit) {
						buffer.poll();
					}
				}
			}
		}

		// Transform our queue buffer into a result array
		if(buffer.isEmpty()) {
			return Collections.emptyList();
		} else {
			List<LookupResult<Identifiable, Set<Identifier>>> result = new ArrayList<>(buffer.size());
			while(!buffer.isEmpty()) {
				// Preserve order
				result.add(buffer.poll());
			}
			// Switch order to put most relevant results first
			Collections.reverse(result);
			return result;
		}
	}

	/**
	 * Estimate a relevance for the given {@link IdentifiableProxy} based on
	 * the number of common identifiers between it and the specified set of
	 * {@link IdentifierProxy}s.
	 * <p>
	 * This method is only called for candidates which have at least one
	 * identifier in common with the input set. Therefore the returned
	 * relevance value will always be in the following range:
	 * <tt>0 &lt; relevance &le; 1 </tt>
	 *
	 * @param identifiableProxy
	 * @param identifierSet
	 * @param identifiers
	 * @return
	 */
	protected double calcRelevance(IdentifiableProxy identifiableProxy,
			IdentifierSet identifierSet, Set<IdentifierProxy> identifiers) {
		int presentIdentifiers = 0;
		for(IdentifierProxy identifierProxy : identifiers) {
			if(identifierSet.contains(identifierProxy)) {
				presentIdentifiers++;
			}
		}

		return (double) presentIdentifiers / identifiers.size();
	}

	protected static class LookupResultImpl implements  LookupResult<Identifiable, Set<Identifier>> {

		private final Identifiable target;
		private final Set<Identifier> identifiers;
		private final double relevance;
		private final Exception exception;

		LookupResultImpl(Set<Identifier> identifiers, Identifiable target, double relevance) {
			checkArgument("Relevance must be in closed interval [0,1]",
					Double.compare(relevance, 0)>=0 && Double.compare(relevance, 1)<=0);

			this.identifiers = requireNonNull(identifiers);
			this.target = requireNonNull(target);
			this.relevance = relevance;
			exception = null;
		}

		LookupResultImpl(Set<Identifier> identifiers, Exception exception) {
			this.identifiers = requireNonNull(identifiers);
			this.exception = requireNonNull(exception);
			target = null;
			relevance = 0;
		}

		/**
		 * @see bwfdm.replaydh.utils.LookupResult#getInput()
		 */
		@Override
		public Set<Identifier> getInput() {
			return identifiers;
		}

		/**
		 * @see bwfdm.replaydh.utils.LookupResult#getTarget()
		 */
		@Override
		public Identifiable getTarget() {
			return target;
		}

		/**
		 * @see bwfdm.replaydh.utils.LookupResult#getRelevance()
		 */
		@Override
		public double getRelevance() {
			return relevance;
		}

		/**
		 * @see bwfdm.replaydh.utils.LookupResult#getException()
		 */
		@Override
		public Exception getException() {
			return exception;
		}

	}

	// CONVERSION HELPERS

	protected IdentifierSet identifierSet(Identifiable identifiable) {
		IdentifierSet result = new IdentifierSet();
		result.addFrom(identifiable);
		return result;
	}

	/**
	 * Translates the given identifier proxy back into a real
	 * {@link Identifier} object.
	 *
	 * @param identifiableProxy
	 * @param proxy
	 * @return
	 */
	protected Identifier identifier(IdentifiableProxy identifiableProxy, IdentifierProxy proxy) {

		/*
		 * This process is kinda expensive and also the idea
		 * of needing 2 proxies to lookup the correct identifier
		 * type information is not 100% appealing...
		 *
		 * TODO think of a cheaper/compacter way
		 */

		SchemaManager schemaManager = getEnvironment().getClient().getSchemaManager();
		WorkflowSchema schema = schemaManager.lookupSchema(proxy.schemaId);
		checkArgument("Unknown schema id: "+proxy.schemaId, schema!=null);
		IdentifierSchema identifierSchema = identifiableProxy.type==Type.PERSON ?
				schema.getPersonIdentifierSchema() : schema.getPersonIdentifierSchema();

		IdentifierType identifierType = IdentifierSchema.parseIdentifierType(identifierSchema, proxy.type);

		return new Identifier(identifierType, proxy.id, proxy.context);
	}

	/**
	 * Instantiate an actual {@link Identifiable} from the given {@code proxy}.
	 * This will preserve the proxy's {@link UUID system Id}.
	 *
	 * @param proxy
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected <I extends Identifiable> I identifiable(IdentifiableProxy proxy) {
		AbstractIdentifiable result = null;

		switch (proxy.type) {
		case PERSON:
			result = DefaultPerson.blankPerson();
			break;

		case RESOURCE:
			result = DefaultResource.blankResource();
			break;

		case TOOL:
			result = DefaultTool.blankTool();
			break;

		default:
			throw new RDHException("Unknown identifier type: "+proxy.type);
		}

		result.setSystemId(proxy.uuid);

		return (I) result;
	}

	protected static IdentifiableProxy identifiableProxy(Identifiable source) {
		return new IdentifiableProxy(source.getSystemId(), source.getType());
	}

	protected static IdentifierProxy identifierProxy(Identifier source) {
		IdentifierType identifierType = source.getType();
		WorkflowSchema schema = identifierType.getSchema();
		String schemaId = schema==null ? NO_SCHEMA_ID : schema.getId();

		return new IdentifierProxy(schemaId, identifierType.getLabel(),
				source.getId(), source.getContext());
	}

	/**
	 * @see bwfdm.replaydh.workflow.resolver.IdentifiableResolver#identifiablesForType(bwfdm.replaydh.workflow.Identifiable.Type)
	 */
	@Override
	public Iterator<Identifiable> identifiablesForType(Type type) {
		return new DelegatingIdentifiableIterator(type, target2IdLookup.entrySet().iterator());
	}

	/**
	 * Implements an iterator that only returns {@link Identifiable identifiables} of a certain
	 * type by cherry picking from a given iterator of the underlying cache structure.
	 *
	 * @author Markus Gärtner
	 *
	 */
	private class DelegatingIdentifiableIterator implements Iterator<Identifiable> {

		private final Identifiable.Type type;
		private final Iterator<Entry<IdentifiableProxy, IdentifierSet>> source;

		private Identifiable next = null;

		public DelegatingIdentifiableIterator(Type type, Iterator<Entry<IdentifiableProxy, IdentifierSet>> source) {
			this.type = requireNonNull(type);
			this.source = requireNonNull(source);
		}

		/**
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			while(next==null && source.hasNext()) {
				Entry<IdentifiableProxy, IdentifierSet> entry = source.next();
				maybeInstantiateIdentifiable(entry.getKey(), entry.getValue());

				Identifiable identifiable = entry.getValue().getIdentifiable();
				if(identifiable!=null && identifiable.getType()==type) {
					next = identifiable;
				}
			}

			return next!=null;
		}

		/**
		 * @see java.util.Iterator#next()
		 */
		@Override
		public Identifiable next() {
			Identifiable result = next;
			next = null;
			if(result==null)
				throw new NoSuchElementException();
			return result;
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 * @param <T> Type of elements stored in this set
	 */
	protected static class CompactSet<T extends Object> implements Iterable<T>, Serializable {

		private static final long serialVersionUID = -1078105408876033875L;

		private Object buffer;

		private static final int ARRAY_MAX_SIZE = 8;
		private static final int ARRAY_START_SIZE = 2;

		@SuppressWarnings("unchecked")
		void add(T item) {
			if(buffer==null) {
				buffer = new Object[ARRAY_START_SIZE];
			}

			if(buffer instanceof Object[]) {
				Object[] array = (Object[]) buffer;
				int insertIdx = -1;
				for(int i=0; i<array.length; i++) {
					Object obj = array[i];
					if(obj==null) {
						// Mark potential insertion points (only save the first one)
						if(insertIdx==-1)
							insertIdx = i;
					} else if(obj.equals(item)) {
						// Item already present -> abort
						return;
					}
				}

				if(insertIdx!=-1) {
					// Slot available
					array[insertIdx] = item;
				} else {
					// Need to expand array (double size)
					int newSize = array.length*2;
					if(newSize>ARRAY_MAX_SIZE) {
						// Grow structure into Set
						Set<T> tmp = new HashSet<>();
						for(Object obj : array) {
							tmp.add((T) obj);
						}
						buffer = tmp;
					} else {
						insertIdx = array.length;
						array = Arrays.copyOf(array, newSize);
						array[insertIdx] = item;
						buffer = array;
					}
				}
			}

			if(buffer instanceof Set) {
				((Set<T>) buffer).add(item);
			}
		}

		@SuppressWarnings("unchecked")
		void remove(T item) {
			if(buffer instanceof Set) {
				Set<T> tmp = (Set<T>) buffer;
				if(!tmp.remove(item)) {
					// Do nothing else if the item wasn't present
					return;
				}

				if(tmp.size()<=ARRAY_MAX_SIZE) {
					buffer = tmp.toArray();
				}
			}

			if(buffer instanceof Object[]) {
				Object[] array = (Object[]) buffer;
				int maxIndex = array.length-1;
				boolean shift = false;
				for(int i=0; i<=maxIndex; i++) {
					Object obj = array[i];
					if(obj==null) {
						return;
					}

					if(!shift && obj.equals(item)) {
						array[i] = null;
						shift = true;
					}

					if(shift) {
						Object next = i==maxIndex ? null : array[i+1];
						array[i] = next;
					}
				}
			}
		}

		@SuppressWarnings("unchecked")
		boolean isEmpty() {
			if(buffer==null) {
				return true;
			} else if(buffer instanceof Object[]) {
				Object[] array = (Object[]) buffer;
				return array[0]==null;
			} else {
				return ((Set<T>)buffer).isEmpty();
			}
		}

		@SuppressWarnings("unchecked")
		boolean contains(T item) {
			if(buffer==null) {
				return false;
			} else if(buffer instanceof Object[]) {
				Object[] array = (Object[]) buffer;
				for(Object obj : array) {
					if(obj==null) {
						break;
					} else if(obj.equals(item)) {
						return true;
					}
				}
				return false;
			} else {
				return ((Set<T>)buffer).contains(item);
			}
		}

		@SuppressWarnings("unchecked")
		void clear() {
			if(buffer instanceof Object[]) {
				Arrays.fill((Object[]) buffer, null);
			} else if(buffer instanceof Set) {
				((Set<T>)buffer).clear();
			}
		}

		@Override
		@SuppressWarnings("unchecked")
		public void forEach(Consumer<? super T> action) {
			if(buffer instanceof Object[]) {
				Object[] array = (Object[]) buffer;
				for(Object obj : array) {
					if(obj==null) {
						break;
					} else {
						action.accept((T) obj);
					}
				}
			} else if(buffer instanceof Set) {
				((Set<T>)buffer).forEach(action);
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public Iterator<T> iterator() {
			if(buffer==null) {
				return nullIterator();
			} else if(buffer instanceof Object[]) {
				Object[] array = (Object[]) buffer;
				if(array[0]==null) {
					return nullIterator();
				} else {
					return new ArrayIterator<>(array);
				}
			} else {
				return ((Set<T>)buffer).iterator();
			}
		}

		@SuppressWarnings("unchecked")
		protected static <T extends Object> Iterator<T> nullIterator() {
			return (Iterator<T>) NULL_ITERATOR;
		}
	}

	protected static final Iterator<Object> NULL_ITERATOR = new Iterator<Object>() {

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Object next() {
			throw new NoSuchElementException();
		}
	};

	/**
	 *
	 * @author Markus Gärtner
	 *
	 * @param <E> type of elements returned by the {@link #next()} method.
	 */
	protected static class ArrayIterator<E extends Object> implements Iterator<E> {
    	final Object[] array;
        int cursor;       // index of next element to return
        int lastRet = -1; // index of last element returned; -1 if no such

        ArrayIterator(Object[] array) {
        	this.array = requireNonNull(array);
        }

        @Override
		public boolean hasNext() {
            return cursor < array.length && array[cursor]!=null;
        }

        @Override
		@SuppressWarnings("unchecked")
        public E next() {
            if (cursor>= array.length || array[cursor]==null)
                throw new NoSuchElementException();
            lastRet = cursor;
            cursor++;
            return (E) array[lastRet];
        }

        @Override
		public void remove() {
            if (lastRet < 0)
                throw new IllegalStateException();

            array[lastRet] = null;
            for(int i=lastRet+1; i<array.length; i++) {
            	array[i-1] = array[i];
            	if(array[i]==null) {
            		break;
            	}
            }
            cursor = lastRet;
            lastRet = -1;
        }
    }

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	protected static class IdentifierSet extends CompactSet<IdentifierProxy> {

		private static final long serialVersionUID = -1392340680102043318L;

		private transient Reference<Identifiable> identifiable;

		void add(Identifier identifier) {
			add(identifierProxy(identifier));
		}

		void addAll(Collection<? extends Identifier> identifiers) {
			for(Identifier identifier : identifiers) {
				add(identifierProxy(identifier));
			}
		}

		void refreshSource(Identifiable identifiable) {
			if(identifiable==null) {
				this.identifiable = null;
			} else if(identifiable!=getIdentifiable()) {
				this.identifiable = new WeakReference<>(identifiable);
			}
		}

		void addFrom(Identifiable identifiable) {
			refreshSource(identifiable);

			identifiable.forEachIdentifier(identifier -> add(identifierProxy(identifier)));
		}

		Identifiable getIdentifiable() {
			Identifiable result = identifiable==null ? null : identifiable.get();
			if(result==null) {
				identifiable = null;
			}
			return result;
		}

		@Override
		void clear() {
			super.clear();
			identifiable = null;
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	protected static class IdentifiableProxy implements Serializable {

		private static final long serialVersionUID = 8568202453326100424L;

		final UUID uuid;
		final Identifiable.Type type;

		public IdentifiableProxy(UUID uuid, Type type) {
			this.uuid = requireNonNull(uuid);
			this.type = requireNonNull(type);
		}

		@Override
		public int hashCode() {
			return Objects.hash(uuid, type);
		}

		@Override
		public boolean equals(Object obj) {
			if(obj==this) {
				return true;
			} else if(obj instanceof IdentifiableProxy) {
				IdentifiableProxy other = (IdentifiableProxy) obj;
				return uuid.equals(other.uuid) && type==other.type;
			}
			return false;
		}

		@Override
		public String toString() {
			return type+":"+uuid;
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	protected static class IdentifierProxy implements Serializable {

		private static final long serialVersionUID = 6579996377913886477L;

		final String schemaId;
		final String type;
		final String id;
		final String context;

		public IdentifierProxy(String schemaId, String type, String id, String context) {
			this.schemaId = requireNonNull(schemaId);
			this.type = requireNonNull(type);
			this.id = requireNonNull(id);
			this.context = context;
		}

		@Override
		public int hashCode() {
			return Objects.hash(schemaId, type, id, context);
		}

		@Override
		public boolean equals(Object obj) {
			if(obj==this) {
				return true;
			} else if(obj instanceof IdentifierProxy) {
				IdentifierProxy other = (IdentifierProxy) obj;
				return schemaId.equals(other.schemaId)
						&& type.equals(other.type)
						&& id.equals(other.id)
						&& Objects.equals(context, other.context);
			}
			return false;
		}

		@Override
		public String toString() {
			return String.format("%s:%s:%s:%s", schemaId, type, id, context==null ? "<none>" : context);
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	public static class Builder {

		public static final boolean DEFAULT_AUTO_PERFORM_CACHE_SERIALIZATION = true;

		public static final boolean DEFAULT_AUTO_CREATE_KNOWN_MISSING_IDENTIFIABLES = true;

		private Path rootFolder;
		private ResourceProvider resourceProvider;

		private IOWorker<? super BiConsumer<IdentifiableProxy, IdentifierSet>> reader;
		private IOWorker<? super Collection<Entry<IdentifiableProxy, IdentifierSet>>> writer;

		private Boolean autoPerformCacheSerialization;

		private Boolean autoCreateKnownMissingIdentifiables;

		protected Builder() {
			// no-op
		}

		public Builder autoPerformCacheSerialization(boolean autoPerformCacheSerialization) {
			checkState("'autoPerformCacheSerialization' flag already set", this.autoPerformCacheSerialization==null);

			this.autoPerformCacheSerialization =
					autoPerformCacheSerialization==DEFAULT_AUTO_PERFORM_CACHE_SERIALIZATION ?
							null : Boolean.valueOf(autoPerformCacheSerialization);

			return this;
		}

		public Builder autoCreateKnownMissingIdentifiables(boolean autoCreateKnownMissingIdentifiables) {
			checkState("'autoCreateKnownMissingIdentifiables' flag already set", this.autoCreateKnownMissingIdentifiables==null);

			this.autoCreateKnownMissingIdentifiables =
					autoCreateKnownMissingIdentifiables==DEFAULT_AUTO_CREATE_KNOWN_MISSING_IDENTIFIABLES ?
							null : Boolean.valueOf(autoCreateKnownMissingIdentifiables);

			return this;
		}

		public Builder resourceProvider(ResourceProvider resourceProvider) {
			requireNonNull(resourceProvider);
			checkState("Resource provider already set", this.resourceProvider==null);

			this.resourceProvider = resourceProvider;

			return this;
		}

		public Builder reader(IOWorker<? super BiConsumer<IdentifiableProxy, IdentifierSet>> reader) {
			requireNonNull(reader);
			checkState("Reader already set", this.reader==null);

			this.reader = reader;

			return this;
		}

		public Builder writer(IOWorker<? super Collection<Entry<IdentifiableProxy, IdentifierSet>>> writer) {
			requireNonNull(writer);
			checkState("Writer already set", this.writer==null);

			this.writer = writer;

			return this;
		}

		public Builder folder(Path rootFolder) {
			requireNonNull(rootFolder);
			checkState("Root folder already set", this.rootFolder==null);

			this.rootFolder = rootFolder;

			return this;
		}

		public Path getRootFolder() {
			return rootFolder;
		}

		public ResourceProvider getResourceProvider() {
			return resourceProvider;
		}

		public IOWorker<? super BiConsumer<IdentifiableProxy, IdentifierSet>> getReader() {
			return reader;
		}

		public IOWorker<? super Collection<Entry<IdentifiableProxy, IdentifierSet>>> getWriter() {
			return writer;
		}

		public boolean isAutoPerformCacheSerialization() {
			return autoPerformCacheSerialization==null ? DEFAULT_AUTO_PERFORM_CACHE_SERIALIZATION
					: autoPerformCacheSerialization.booleanValue();
		}

		public boolean isAutoCreateKnownMissingIdentifiables() {
			return autoCreateKnownMissingIdentifiables==null ? DEFAULT_AUTO_CREATE_KNOWN_MISSING_IDENTIFIABLES
					: autoCreateKnownMissingIdentifiables.booleanValue();
		}

		/**
		 * Assigns default implementations to required fields other
		 * than the {@link #getRootFolder() root folder} if those
		 * fields have not been set yet.
		 *
		 * @return
		 */
		public Builder useDefaultSerialization() {
			if(resourceProvider==null) {
				resourceProvider = new FileResourceProvider();
			}

			if(reader==null) {
				reader = IdentifiableResolverXml.reader();
			}

			if(writer==null) {
				writer = IdentifiableResolverXml.writer();
			}

			return this;
		}

		protected void validate() {
			checkState("Root folder not set", rootFolder!=null);
			checkState("Resource provider not set", resourceProvider!=null);
			checkState("Missing reader", reader!=null);
			checkState("Missing writer", writer!=null);
		}

		public LocalIdentifiableResolver build() {
			validate();

			return new LocalIdentifiableResolver(this);
		}
	}
}

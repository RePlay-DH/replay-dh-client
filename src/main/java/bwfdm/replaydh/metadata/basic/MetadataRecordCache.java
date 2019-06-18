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
package bwfdm.replaydh.metadata.basic;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static java.util.Objects.requireNonNull;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import bwfdm.replaydh.metadata.MetadataException;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRecord.Target;
import bwfdm.replaydh.utils.UsageAware;

/**
 * Implements a cache for {@link Target} to {@link MetadataRecord} mappings.
 *
 * @author Markus Gärtner
 *
 */
public class MetadataRecordCache {

	private static class Proxy extends WeakReference<MetadataRecord> {

		final Target target;
		final String schemaId;

		public Proxy(MetadataRecord record, ReferenceQueue<? super MetadataRecord> q) {
			super(record, q);

			this.target = record.getTarget().clone();
			this.schemaId = record.getSchemaId();
		}

	}

	private static final float DEFAULT_PURGE_THRESHOLD = 0.2f;

	private final Map<Target, List<Proxy>> cache;

	private final ReferenceQueue<MetadataRecord> queue = new ReferenceQueue<>();
	private final int limit;

	public MetadataRecordCache(int initialCapacity, int limit) {
		this(initialCapacity, limit, DEFAULT_PURGE_THRESHOLD);
	}

	public MetadataRecordCache(int initialCapacity, int limit, float purgeThreshold) {
		checkArgument("Initial capacity must be greater that 0", initialCapacity>0);
		checkArgument("Purge threshold must be positive and less then 1", purgeThreshold>0 && purgeThreshold<1);

		cache = new HashMap<>(initialCapacity);

		this.limit = limit;
	}

	private void purgeStaleEntries() {
        for (Object x; (x = queue.poll()) != null; ) {
            synchronized (cache) {
            	Proxy proxy = (Proxy) x;
        		List<Proxy> proxies = cache.get(proxy.target);
            	proxies.remove(proxy);
            	if(proxies.isEmpty()) {
            		cache.remove(proxy.target);
            	}
            }
        }
	}

	public void purgeUnusedEntries() {
		purgeStaleEntries();

		synchronized (cache) {
			for(Iterator<Entry<Target, List<Proxy>>> itList = cache.entrySet().iterator(); itList.hasNext();) {
				Entry<Target, List<Proxy>> entry = itList.next();

				for(Iterator<Proxy> it = entry.getValue().iterator(); it.hasNext();) {
					Proxy proxy = it.next();
					MetadataRecord record = proxy.get();

					// Mark for purging all GC'd records
					boolean purge = record==null || proxy.isEnqueued();

					if(!purge && record instanceof UsageAware) {

						// Additionally check if we can purge entries that are no longer in active use
						UsageAware aware = (UsageAware) record;
						// Make sure we give entries time to be used at least once!
						if(aware.hasBeenUsed() && !aware.inUse()) {
							proxy.clear();
							purge = true;
						}
					}

					if(purge) {
						it.remove();
					}
				}

				if(entry.getValue().isEmpty()) {
					itList.remove();
				}
			}
		}
	}

	/**
	 * Must be called under cache lock!
	 */
	private Proxy proxyFor(Target target, String schemaId) {
		List<Proxy> proxies = cache.get(target);
		if(proxies == null || proxies.isEmpty()) {
			return null;
		}
		for(Proxy proxy : proxies) {
			if(proxy.schemaId.equals(schemaId)) {
				return proxy;
			}
		}
		return null;
	}

	public MetadataRecord getRecord(Target target, String schemaId) {
		requireNonNull(target);

		purgeStaleEntries();

		synchronized (cache) {
			Proxy proxy = proxyFor(target, schemaId);
			return proxy==null ? null : proxy.get();
		}
	}

	public List<MetadataRecord> getRecords(Target target) {
		requireNonNull(target);

		purgeStaleEntries();

		synchronized (cache) {
			List<Proxy> proxies = cache.get(target);
			if(proxies == null || proxies.isEmpty()) {
				return Collections.emptyList();
			}

			List<MetadataRecord> records = new ArrayList<>(proxies.size());
			for(Proxy proxy : proxies) {
				MetadataRecord record = proxy.get();
				if(record!=null) {
					records.add(record);
				}
			}
			return records;
		}


	}

	/**
	 * Returns {@code true} if this cache contains a {@link Reference} for the
	 * given {@link Target} that is not {@code null}, {@link Reference#isEnqueued() queued}
	 * or already {@link Reference#clear() cleared} so that it would {@link Reference#get() yield}
	 * a {@code null} target.
	 *
	 * @param target
	 * @return
	 */
	public boolean hasRecords(Target target) {
		requireNonNull(target);

		purgeStaleEntries();

		synchronized (cache) {
			List<Proxy> proxies = cache.get(target);
			return proxies!=null && !proxies.isEmpty();
		}
	}

	/**
	 * Must be called under cache lock!
	 */
	private List<Proxy> proxyList(Target target, boolean createIfMissing) {
		List<Proxy> proxies = cache.get(target);
		if(proxies==null && createIfMissing) {
			proxies = new ArrayList<>();
			cache.put(target, proxies);
		}
		return proxies;
	}

	public void addRecord(MetadataRecord record) {
		requireNonNull(record);

		purgeStaleEntries();

		synchronized (cache) {
			addRecord0(record);
		}
	}

	/**
	 * Must be called under cache lock!
	 */
	private void addRecord0(MetadataRecord record) {
		Target target = requireNonNull(record.getTarget());
		Proxy proxy = proxyFor(target, record.getSchemaId());

		if(proxy!=null) {
			MetadataRecord existing = proxy.get();

			// If the ref got cleared
			if(existing!=null) {

				// If the exact same record is already registered, do nothing
				if(existing==record) {
					return;
				} else // Otherwise report inconsistency
					throw new MetadataException("Cache corrupted - foreign record already registered for target: "+target);
			}
		}

		// Here we either have a ref that got GC'd or a blank new entry
		proxyList(target, true).add(new Proxy(record, queue));
	}

	public void addRecords(Collection<MetadataRecord> records) {
		requireNonNull(records);

		purgeStaleEntries();

		synchronized (cache) {
			records.forEach(this::addRecord0);
		}
	}

	public void removeRecord(MetadataRecord record) {
		requireNonNull(record);

		purgeStaleEntries();

		Target target = requireNonNull(record.getTarget());

		synchronized (cache) {
			List<Proxy> proxies = cache.get(target);
			if(proxies == null || proxies.isEmpty())
				throw new MetadataException("No metadata record present in cache for target: "+target);

			for(Iterator<Proxy> it = proxies.iterator(); it.hasNext();) {
				Proxy proxy = it.next();
				if(proxy.schemaId.equals(record.getSchemaId())) {
					proxy.clear();
					it.remove();
				}
			}
		}
	}

	/**
	 * Force removal of all entries from cache.
	 */
	public void clear() {
		// Initial removal of queued refs (no need to purge since cache will get cleared)
		while(queue.poll()!=null);

		synchronized (cache) {
			cache.clear();
		}

		// Clearing the cache might have caused additional GC runs, so we need to free up more queued refs
		while(queue.poll()!=null);
	}

	public boolean isEmpty() {
		purgeStaleEntries();

		synchronized (cache) {
			return cache.isEmpty();
		}
	}
}

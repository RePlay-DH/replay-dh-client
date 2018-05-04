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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import bwfdm.replaydh.metadata.MetadataException;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRecord.UID;
import bwfdm.replaydh.utils.UsageAware;

/**
 * Implements a cache for {@link UID} to {@link MetadataRecord} mappings.
 *
 * @author Markus Gärtner
 *
 */
public class MetadataRecordCache {

	private static class Proxy extends WeakReference<MetadataRecord> {

		private final UID uid;

		public Proxy(MetadataRecord record, ReferenceQueue<? super MetadataRecord> q) {
			super(record, q);

			this.uid = record.getUID();
		}

	}

	private static final float DEFAULT_PURGE_THRESHOLD = 0.2f;

	private final Map<UID, Proxy> cache;

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
            	cache.remove(proxy.uid);
            }
        }
	}

	public void purgeUnusedEntries() {
		purgeStaleEntries();

		synchronized (cache) {
			for(Iterator<Entry<UID, Proxy>> it = cache.entrySet().iterator(); it.hasNext();) {
				Entry<UID, Proxy> entry = it.next();
				Reference<MetadataRecord> ref = entry.getValue();
				MetadataRecord record = ref.get();

				// Mark for purging all GC'd records
				boolean purge = record==null || ref.isEnqueued();

				if(!purge && record instanceof UsageAware) {

					// Additionally check if we can purge entries that are no longer in active use
					UsageAware aware = (UsageAware) record;
					// Make sure we give entries time to be used at least once!
					if(aware.hasBeenUsed() && !aware.inUse()) {
						ref.clear();
						purge = true;
					}
				}

				if(purge) {
					it.remove();
				}
			}
		}
	}

	public MetadataRecord getRecord(UID uid) {
		requireNonNull(uid);

		purgeStaleEntries();

		synchronized (cache) {
			Reference<MetadataRecord> ref = cache.get(uid);
			if(ref==null) {
				return null;
			}

			MetadataRecord record = ref.get();
			if(record==null) {
				cache.remove(uid);
			}

			return record;
		}
	}

	/**
	 * Returns {@code true} if this cache contains a {@link Reference} for the
	 * given {@link UID} that is not {@code null}, {@link Reference#isEnqueued() queued}
	 * or already {@link Reference#clear() cleared} so that it would {@link Reference#get() yield}
	 * a {@code null} target.
	 *
	 * @param uid
	 * @return
	 */
	public boolean hasRecord(UID uid) {
		requireNonNull(uid);

		purgeStaleEntries();

		synchronized (cache) {
			Reference<MetadataRecord> ref = cache.get(uid);
			return ref!=null && !ref.isEnqueued() && ref.get()!=null;
		}
	}

	public void addRecord(MetadataRecord record) {
		requireNonNull(record);

		purgeStaleEntries();

		UID uid = requireNonNull(record.getUID());

		synchronized (cache) {
			Reference<MetadataRecord> ref = cache.get(uid);

			if(ref!=null) {
				MetadataRecord existing = ref.get();

				// If the ref got cleared
				if(existing!=null) {

					// If the exact same record is already registered, do nothing
					if(existing==record) {
						return;
					} else // Otherwise report inconsistency
						throw new MetadataException("Cache corrupted - foreign record already registered for UID: "+uid);
				}
			}

			// Here we either have a ref that got GC'd or a blank new entry
			cache.put(uid, new Proxy(record, queue));
		}
	}

	public void removeRecord(MetadataRecord record) {
		requireNonNull(record);

		purgeStaleEntries();

		UID uid = requireNonNull(record.getUID());

		synchronized (cache) {
			Reference<MetadataRecord> ref = cache.remove(uid);

			if(ref==null)
				throw new MetadataException("No metadata record present in cache for UID: "+uid);

			ref.clear();
		}
	}

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

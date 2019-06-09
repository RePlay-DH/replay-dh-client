/*
 * $Revision: 244 $
 * $Date: 2014-04-10 14:09:12 +0200 (Do, 10 Apr 2014) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus/core/de.ims.icarus/source/de/ims/icarus/util/collections/WeakHashSet.java $
 *
 * $LastChangedDate: 2014-04-10 14:09:12 +0200 (Do, 10 Apr 2014) $
 * $LastChangedRevision: 244 $
 * $LastChangedBy: mcgaerty $
 */
package bwfdm.replaydh.utils;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.WeakHashMap;

/**
 * @author Markus Gärtner
 * @version $Id: WeakHashSet.java 244 2014-04-10 12:09:12Z mcgaerty $
 *
 */
public class WeakHashSet<E extends Object> extends AbstractSet<E> {

	private static final Object dummy = "DUMMY"; //$NON-NLS-1$

	private WeakHashMap<E, Object> store = new WeakHashMap<>();

	public WeakHashSet() {
		// no-op
	}

	public WeakHashSet(Collection<? extends E> c) {
		addAll(c);
	}

	/**
	 * @see java.util.AbstractCollection#iterator()
	 */
	@Override
	public Iterator<E> iterator() {
		return store.keySet().iterator();
	}

	/**
	 * @see java.util.AbstractCollection#size()
	 */
	@Override
	public int size() {
		return store.size();
	}

	@Override
	public boolean contains(Object o) {
		return store.containsKey(o);
	}

	@Override
	public Object[] toArray() {
		return store.keySet().toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return store.keySet().toArray(a);
	}

	@Override
	public boolean add(E e) {
		if(store.containsKey(e)) {
			return false;
		} else {
			store.put(e, dummy);
			return true;
		}
	}

	@Override
	public boolean remove(Object o) {
		return store.remove(o) != null;
	}

	@Override
	public void clear() {
		store.clear();
	}

}

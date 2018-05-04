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

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Markus Gärtner
 * @version $Id: IdentityHashSet.java 244 2014-04-10 12:09:12Z mcgaerty $
 *
 */
public class IdentityHashSet<E extends Object> extends AbstractSet<E> {

	/**
	 * The hash table data.
	 */
	private transient Entry table[];

	/**
	 * The total number of entries in the hash table.
	 */
	private transient int count;

	/**
	 * The table is rehashed when its size exceeds this threshold. (The value of
	 * this field is (int)(capacity * loadFactor).)
	 *
	 * @serial
	 */
	private int threshold;

	/**
	 * The load factor for the hash-table.
	 *
	 * @serial
	 */
	private float loadFactor;

	/**
	 * Inner class that acts as a data structure to create a new entry in the
	 * table.
	 */
	private static class Entry {
		int hash;
		Object value;
		Entry next;

		/**
		 * Create a new entry with the given values.
		 *
		 * @param hash The hash used to enter this in the table
		 * @param value The value for this key
		 * @param next A reference to the next entry in the table
		 */
		protected Entry(int hash, Object value, Entry next) {
			this.hash = hash;
			this.value = value;
			this.next = next;
		}
	}

	public IdentityHashSet() {
		this(20, 0.75f);
	}

	public IdentityHashSet(int initialCapacity) {
		this(initialCapacity, 0.75f);
	}

	public IdentityHashSet(int initialCapacity, float loadFactor) {

		if (initialCapacity < 0)
			throw new IllegalArgumentException("Illegal capacity (negative): " //$NON-NLS-1$
					+ initialCapacity);
		if (loadFactor <= 0)
			throw new IllegalArgumentException("Illegal load-factor (zero or less): " + loadFactor); //$NON-NLS-1$

		if (initialCapacity == 0) {
			initialCapacity = 1;
		}

		this.loadFactor = loadFactor;
		table = new Entry[initialCapacity];
		threshold = (int) (initialCapacity * loadFactor);
	}

	/**
	 * @see java.util.AbstractCollection#iterator()
	 */
	@Override
	public Iterator<E> iterator() {
		return new HashIterator();
	}

	@Override
	public int size() {
		return count;
	}

	@Override
	public boolean isEmpty() {
		return count == 0;
	}

	private int hash(Object value) {
		return System.identityHashCode(value);
	}

	@Override
	public boolean contains(Object value) {
		if (value == null)
			throw new NullPointerException("Invalid value"); //$NON-NLS-1$
		Entry tab[] = table;
		int hash = hash(value);
		int index = (hash & 0x7FFFFFFF) % tab.length;
		for (Entry e = tab[index]; e != null; e = e.next) {
			if (e.value == value) {
				return true;
			}
		}
		return false;
	}

	public boolean containsEquals(Object value) {
		if (value == null)
			throw new NullPointerException("Invalid value"); //$NON-NLS-1$
		Entry tab[] = table;
		int hash = hash(value);
		int index = (hash & 0x7FFFFFFF) % tab.length;
		for (Entry e = tab[index]; e != null; e = e.next) {
			if (value.equals(e.value)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Increases the capacity of and internally reorganizes this hash-table, in
	 * order to accommodate and access its entries more efficiently.
	 *
	 * This method is called automatically when the number of keys in the
	 * hash-table exceeds this hash-table's capacity and load factor.
	 */
	protected void rehash() {
		int oldCapacity = table.length;
		Entry oldMap[] = table;

		int newCapacity = (oldCapacity * 2) + 1;
		Entry newMap[] = new Entry[newCapacity];

		threshold = (int) (newCapacity * loadFactor);
		table = newMap;

		for (int i = oldCapacity; i-- > 0;) {
			for (Entry old = oldMap[i]; old != null;) {
				Entry e = old;
				old = old.next;

				int index = (e.hash & 0x7FFFFFFF) % newCapacity;
				e.next = newMap[index];
				newMap[index] = e;
			}
		}
	}

	@Override
	public boolean add(E value) {
		if (value == null)
			throw new NullPointerException("Invalid value"); //$NON-NLS-1$
		// Makes sure the key is not already in the hash-table.
		Entry tab[] = table;
		int hash = hash(value);
		int index = (hash & 0x7FFFFFFF) % tab.length;
		for (Entry e = tab[index]; e != null; e = e.next) {
			if (e.value == value) {
				return false;
			}
		}

		if (count >= threshold) {
			// Rehash the table if the threshold is exceeded
			rehash();

			tab = table;
			index = (hash & 0x7FFFFFFF) % tab.length;
		}

		// Creates the new entry.
		Entry e = new Entry(hash, value, tab[index]);
		tab[index] = e;
		count++;
		return true;
	}

	public boolean addEquals(E value) {
		if (value == null)
			throw new NullPointerException("Invalid value"); //$NON-NLS-1$
		// Makes sure the key is not already in the hash-table.
		Entry tab[] = table;
		int hash = hash(value);
		int index = (hash & 0x7FFFFFFF) % tab.length;
		for (Entry e = tab[index]; e != null; e = e.next) {
			if (value.equals(e.value)) {
				return false;
			}
		}

		if (count >= threshold) {
			// Rehash the table if the threshold is exceeded
			rehash();

			tab = table;
			index = (hash & 0x7FFFFFFF) % tab.length;
		}

		// Creates the new entry.
		Entry e = new Entry(hash, value, tab[index]);
		tab[index] = e;
		count++;
		return true;
	}

	@Override
	public boolean remove(Object value) {
		if (value == null)
			throw new NullPointerException("Invalid value"); //$NON-NLS-1$
		Entry tab[] = table;
		int hash = hash(value);
		int index = (hash & 0x7FFFFFFF) % tab.length;
		for (Entry e = tab[index], prev = null; e != null; prev = e, e = e.next) {
			if (e.value == value) {
				if (prev != null) {
					prev.next = e.next;
				} else {
					tab[index] = e.next;
				}
				count--;
				e.value = null;
				return true;
			}
		}
		return false;
	}

	/**
	 * Clears this hash-table so that it contains no keys.
	 */
	@Override
	public synchronized void clear() {
		Entry tab[] = table;
		for (int index = tab.length; --index >= 0;) {
			tab[index] = null;
		}
		count = 0;
	}

	class HashIterator implements Iterator<E> {
		Entry next; // next entry to return
		Entry current; // current entry
		int index; // current slot

		HashIterator() {
			Entry[] t = table;
			current = next = null;
			index = 0;
			if (t != null && count > 0) { // advance to first entry
				do {
				} while (index < t.length && (next = t[index++]) == null);
			}
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public void remove() {
			Entry p = current;
			if (p == null)
				throw new IllegalStateException();
			current = null;
			Object item = p.value;
			IdentityHashSet.this.remove(item);
		}

		/**
		 * @see java.util.Iterator#next()
		 */
		@SuppressWarnings("unchecked")
		@Override
		public E next() {
			Entry[] t;
			Entry e = next;
			if (e == null)
				throw new NoSuchElementException();
			if ((next = (current = e).next) == null && (t = table) != null) {
				do {
				} while (index < t.length && (next = t[index++]) == null);
			}
			return (E) e.value;
		}
	}
}

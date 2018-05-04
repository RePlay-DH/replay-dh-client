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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Not thread-safe!
 *
 * @author Markus Gärtner
 *
 */
public class LazyCollection<E extends Object> implements Consumer<E> {

	public static <E extends Object> LazyCollection<E> lazyList() {
		return new LazyCollection<>(ArrayList::new);
	}

	public static <E extends Object> LazyCollection<E> lazyList(final int capacity) {
		return new LazyCollection<>(() -> new ArrayList<>(capacity));
	}

	public static <E extends Object> LazyCollection<E> lazyLinkedList() {
		return new LazyCollection<>(LinkedList::new);
	}

	public static <E extends Object> LazyCollection<E> lazySet() {
		return new LazyCollection<>(HashSet::new);
	}

	public static <E extends Object> LazyCollection<E> lazyLinkedSet() {
		return new LazyCollection<>(LinkedHashSet::new);
	}

	public static <E extends Object> LazyCollection<E> lazySet(final int capacity) {
		return new LazyCollection<>(() -> new HashSet<>(capacity));
	}

	public static <E extends Object> LazyCollection<E> lazyLinkedSet(final int capacity) {
		return new LazyCollection<>(() -> new LinkedHashSet<>(capacity));
	}

	private final Supplier<Collection<E>> supplier;

	private Collection<E> buffer;

	public LazyCollection(Supplier<Collection<E>> supplier) {
		requireNonNull(supplier);

		this.supplier = supplier;
	}

	@Override
	public void accept(E t) {
		add(t);
	}

	public void add(E item) {
		if(item==null) {
			return;
		}

		if(buffer==null) {
			buffer = supplier.get();
		}

		buffer.add(item);
	}

	public void addAll(Collection<? extends E> items) {
		if(items==null || items.isEmpty()) {
			return;
		}

		if(buffer==null) {
			buffer = supplier.get();
		}

		buffer.addAll(items);
	}

	public <C extends Collection<E>> C get() {
		@SuppressWarnings("unchecked")
		C result = (C) buffer;

		return result;
	}

	@SuppressWarnings("unchecked")
	public <L extends List<E>> L getAsList() {
		L result = (L) buffer;

		if(result==null) {
			result = (L) Collections.emptyList();
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	public <L extends Set<E>> L getAsSet() {
		L result = (L) buffer;

		if(result==null) {
			result = (L) Collections.emptySet();
		}

		return result;
	}

	public Object[] getAsArray() {
		Collection<E> c = buffer;

		return (c==null || c.isEmpty()) ? new Object[0] : c.toArray();
	}

	@SuppressWarnings("unchecked")
	public <T extends Object> T[] getAsArray(T[] array) {
		Collection<E> c = buffer;

		return (c==null) ? (T[]) new Object[0] : c.toArray(array);
	}

	public boolean isEmpty() {
		Collection<E> c = buffer;
		return c==null || c.isEmpty();
	}

	public int size() {
		Collection<E> c = buffer;
		return c==null ? 0 : c.size();
	}
}

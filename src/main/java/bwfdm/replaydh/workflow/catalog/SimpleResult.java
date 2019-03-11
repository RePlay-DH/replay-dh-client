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

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.catalog.MetadataCatalog.Result;

/**
 * Implements a {@link MetadataCatalog.Result} that is backed by a
 * {@link List} and which can be modified.
 *
 * @author Markus Gärtner
 *
 */
public class SimpleResult implements MetadataCatalog.Result {

	private final List<Identifiable> identifiables;

	public SimpleResult() {
		identifiables = new ArrayList<>();
	}

	public SimpleResult(int capacity) {
		identifiables = new ArrayList<>(capacity);
	}

	/**
	 * Wraps around the given list of identifiables, resulting
	 * in a {@link Result} that can't be modified further. Later changes
	 * to the provided list by external sources will however be reflected
	 * in this result.
	 * @param identifiables
	 */
	public SimpleResult(List<Identifiable> identifiables) {
		this.identifiables = Collections.unmodifiableList(identifiables);
	}

	/**
	 * @see bwfdm.replaydh.workflow.catalog.MetadataCatalog.Result#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return identifiables.isEmpty();
	}

	/**
	 * @see bwfdm.replaydh.workflow.catalog.MetadataCatalog.Result#iterator()
	 */
	@Override
	public Iterator<Identifiable> iterator() {
		return identifiables.iterator();
	}

	/**
	 * @see bwfdm.replaydh.workflow.catalog.MetadataCatalog.Result#asList()
	 */
	@Override
	public List<Identifiable> asList() {
		return Collections.unmodifiableList(identifiables);
	}

	public void add(Identifiable identifiable) {
		requireNonNull(identifiable);
		identifiables.add(identifiable);
	}

	public void clear() {
		identifiables.clear();
	}
}

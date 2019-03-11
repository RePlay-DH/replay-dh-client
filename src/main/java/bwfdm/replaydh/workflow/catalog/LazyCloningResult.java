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

import java.util.Iterator;
import java.util.List;

import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.WorkflowUtils;
import bwfdm.replaydh.workflow.catalog.MetadataCatalog.Result;

/**
 * @author Markus Gärtner
 *
 */
public class LazyCloningResult implements Result {

	private final List<Identifiable> rawResult;

	public LazyCloningResult(List<Identifiable> rawResult) {
		this.rawResult = requireNonNull(rawResult);
	}

	/**
	 * @see bwfdm.replaydh.workflow.catalog.MetadataCatalog.Result#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return rawResult.isEmpty();
	}

	/**
	 * @see bwfdm.replaydh.workflow.catalog.MetadataCatalog.Result#iterator()
	 */
	@Override
	public Iterator<Identifiable> iterator() {
		return new LazyCloningIterator(rawResult.iterator());
	}

	private static class LazyCloningIterator implements Iterator<Identifiable> {
		private final Iterator<Identifiable> rawIterator;

		public LazyCloningIterator(Iterator<Identifiable> rawIterator) {
			this.rawIterator = requireNonNull(rawIterator);
		}

		/**
		 * @see java.util.Iterator#hasNext()
		 */
		@Override
		public boolean hasNext() {
			return rawIterator.hasNext();
		}

		/**
		 * @see java.util.Iterator#next()
		 */
		@Override
		public Identifiable next() {
			return WorkflowUtils.clone(rawIterator.next());
		}
	}
}

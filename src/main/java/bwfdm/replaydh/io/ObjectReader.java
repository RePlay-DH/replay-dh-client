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
package bwfdm.replaydh.io;

import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.function.Consumer;

import bwfdm.replaydh.utils.LazyCollection;
import bwfdm.replaydh.utils.Options;

/**
 * @author Markus Gärtner
 *
 */
public interface ObjectReader<E extends Object> extends AutoCloseable {

	void init(Reader input, Options options);

	boolean hasMoreData() throws IOException, InterruptedException;

	E read() throws IOException, InterruptedException;

	default int readAll(Consumer<? super E> action) throws IOException, InterruptedException {
		int counter = 0;
		while(hasMoreData()) {
			action.accept(read());
		}
		return counter;
	}

	default List<E> readAll() throws IOException, InterruptedException {
		LazyCollection<E> buffer = LazyCollection.lazyList();
		readAll(buffer);
		return buffer.getAsList();
	}

	@Override
	void close() throws IOException;
}

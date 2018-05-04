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
import java.io.Writer;
import java.util.function.Supplier;

import bwfdm.replaydh.utils.Options;

/**
 * @author Markus Gärtner
 *
 */
public interface ObjectWriter<E extends Object> extends AutoCloseable {

	void init(Writer output, Options options);

	/**
	 * Write the optional header section.
	 *
	 * @throws IOException
	 */
	default void writeHeader() throws IOException {
		// no-op
	}

	/**
	 * Write a single element to the underlying character stream.
	 *
	 * @param element
	 * @throws IOException
	 * @throws InterruptedException
	 */
	void write(E element) throws IOException, InterruptedException;

	/**
	 * Write an unspecified number of elements to the underlying character stream.
	 * This call wraps the write process inside the proper {@link #writeHeader()}
	 * and {@link #writeFooter()} invocations.
	 *
	 * @param source
	 * @throws IOException
	 * @throws InterruptedException
	 */
	default void writeAll(Supplier<? extends E> source) throws IOException, InterruptedException {
		writeHeader();

		E element;
		while((element=source.get())!=null) {
			write(element);
		}

		writeFooter();
	}

	default void writeAll(Iterable<? extends E> source) throws IOException, InterruptedException {
		writeHeader();

		for(E element : source) {
			write(element);
		}

		writeFooter();
	}

	/**
	 * Flush pending buffered element data and write the optional footer section.
	 *
	 * @throws IOException
	 */
	default void writeFooter() throws IOException {
		// no-op
	}

	@Override
	void close() throws IOException;
}

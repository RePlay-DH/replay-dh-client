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
package bwfdm.replaydh.metadata;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import bwfdm.replaydh.metadata.MetadataRecord.Entry;

/**
 * Utility class for the construction of new metadata records.
 * It provides all the methods for incrementally adding new
 * key-value pairs and to verify if names or values are legal
 * in general or in a given combination.
 * <p>
 * An instance of this class can also be used for informative
 * purposes when creating a dynamic graphical interface for
 * creating or editing metadata records.
 *
 * @author Markus Gärtner
 *
 */
public interface MetadataBuilder extends MetadataSchema, BiConsumer<String, String>, Consumer<Entry> {

	//TODO move dependency on MetadataSchema to a compositional approach

	/**
	 * Notifies the builder that client code wishes to actually
	 * start building metadata entries for the linked resource.
	 */
	MetadataBuilder start();

	/**
	 * Creates and adds a new {@link Entry entry} to this builder.
	 *
	 * @param name
	 * @param value
	 * @return
	 *
	 * @throws MetadataException if either {@code name} or {@code value}
	 * denotes a (currently) not allowed String value.
	 */
	MetadataBuilder addEntry(String name, String value);

	MetadataBuilder addEntry(Entry entry);

	/**
	 * @see java.util.function.BiConsumer#accept(java.lang.Object, java.lang.Object)
	 */
	@Override
	default void accept(String name, String value) {
		addEntry(name, value);
	}

	/**
	 * @see java.util.function.Consumer#accept(java.lang.Object)
	 */
	@Override
	default void accept(Entry entry) {
		addEntry(entry);
	}

	/**
	 * Clear any building progress in this builder that has been made so far.
	 * After this method completes the builder should behave exactly like a
	 * freshly {@link MetadataRepository#newBuilder() created} one.
	 */
	void reset();

	/**
	 * Wraps the current content of this builder into a new {@link MetadataRecord} and then
	 * {@link #reset() resets} this builder.
	 *
	 * @return
	 */
	MetadataRecord build();

	/**
	 * Stop the current building process and discard all information.
	 */
	void cancel();
}

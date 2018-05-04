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

import bwfdm.replaydh.metadata.MetadataRecord.UID;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Resource;

public interface UIDStorage extends AutoCloseable {

	boolean isEmpty();

	/**
	 * Checks whether or not there exists an entry for the given resource.
	 * A return value of {@code true} will guarantee {@link #getUID(Resource)}
	 * to return a non-null value until the mapping gets {@link #deleteUID(Resource) deleted}.
	 *
	 * @param resource
	 * @return
	 */
	boolean hasUID(Identifiable resource);

	/**
	 * Returns the stored uid for the given {@code resource} if available.
	 * Otherwise this method should return {@code null}.
	 *
	 * @param resource
	 * @return
	 */
	UID getUID(Identifiable resource);

//	/**
//	 * Creates a new {@link UID} instance based on the identifiers provided
//	 * by the given {@code resource}. If the information provided there is
//	 * insufficient, this method should throw an exception.
//	 *
//	 * @param resource
//	 * @return
//	 */
//	UID createUID(Identifiable resource);

	/**
	 * Removes any information stored for the given resource.
	 * After this method has completed, a call to {@link #hasUID(Resource)}
	 * with the same {@code resource} argument will yield {@code false}
	 * until a new UID has been {@link #createUID(Resource) created}.
	 *
	 * @param resource
	 */
	void deleteUID(Identifiable resource);

	void addUID(Identifiable resource, UID uid);

	@Override
	void close();
}

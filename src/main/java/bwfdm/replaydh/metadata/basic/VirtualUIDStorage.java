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

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import bwfdm.replaydh.metadata.MetadataRecord.UID;
import bwfdm.replaydh.workflow.Identifiable;

/**
 * @author Markus Gärtner
 *
 */
public class VirtualUIDStorage implements UIDStorage {

	/**
	 * Mechanism to  work around mutable {@link Identifiable} objects.
	 */
	private final Map<Identifiable, UID> hardMap = new IdentityHashMap<>();

	/**
	 * Mechanism to allow for {@link Object#equals(Object) equal} resources
	 * to point to the same {@link UID}.
	 */
	private final Map<Identifiable, UID> softMap = new HashMap<>();

	/**
	 * @see bwfdm.replaydh.metadata.basic.UIDStorage#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return hardMap.isEmpty() && softMap.isEmpty();
	}

	/**
	 * @see bwfdm.replaydh.metadata.basic.UIDStorage#hasUID(bwfdm.replaydh.workflow.Identifiable)
	 */
	@Override
	public boolean hasUID(Identifiable resource) {
		return softMap.containsKey(resource) || hardMap.containsKey(resource);
	}

	/**
	 * @see bwfdm.replaydh.metadata.basic.UIDStorage#getUID(bwfdm.replaydh.workflow.Identifiable)
	 */
	@Override
	public UID getUID(Identifiable resource) {
		UID result = softMap.get(resource);
		if(result==null) {
			result = hardMap.get(resource);
		}

		return result;
	}

	/**
	 * @see bwfdm.replaydh.metadata.basic.UIDStorage#deleteUID(bwfdm.replaydh.workflow.Identifiable)
	 */
	@Override
	public void deleteUID(Identifiable resource) {
		hardMap.remove(resource);
		softMap.remove(resource);

		//TODO throw exception if no map contained a mapping for given resource?
	}

	/**
	 * @see bwfdm.replaydh.metadata.basic.UIDStorage#close()
	 */
	@Override
	public void close() {
		// no-op
	}

	/**
	 * @see bwfdm.replaydh.metadata.basic.UIDStorage#addUID(bwfdm.replaydh.workflow.Identifiable, bwfdm.replaydh.metadata.MetadataRecord.UID)
	 */
	@Override
	public void addUID(Identifiable resource, UID uid) {
		hardMap.put(resource, uid);
		softMap.put(resource, uid);

		//TODO throw exception if resource already mapped to an(other) UID?
	}
}

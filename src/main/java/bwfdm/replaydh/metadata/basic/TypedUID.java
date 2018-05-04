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

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import bwfdm.replaydh.metadata.MetadataRecord.UID;
import bwfdm.replaydh.workflow.Identifier;

/**
 * @author Markus Gärtner
 *
 */
public class TypedUID implements UID {

	/**
	 * Context indicating global uniqueness of associated identifier.
	 */
	public static final String NULL_CONTEXT = "";

	/**
	 * ID of the entity that created this UID instance.
	 */
	private final String ownerId;

	/**
	 * Type of this UID, typically the type definition used for a matching
	 * {@link Identifier}.
	 */
	private final String type;

	/**
	 * The actual 'id' content, which might be a relative file path, URL or
	 * a person's name.
	 */
	private final String id;

	/**
	 * Disambiguation context for this UID.
	 */
	private final String context; //TODO use this as basis for a unique-name ??

	public TypedUID(String ownerId, String type, String id, String context) {
		this.ownerId = requireNonNull(ownerId);
		this.type = requireNonNull(type);
		this.id = requireNonNull(id);
		this.context = requireNonNull(context);
	}

	public String getOwnerId() {
		return ownerId;
	}

	public String getType() {
		return type;
	}

	public String getId() {
		return id;
	}

	public String getContext() {
		return context;
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		} else if(obj instanceof TypedUID) {
			TypedUID other = (TypedUID) obj;
			return ownerId.equals(other.ownerId)
					&& type.equals(other.type)
					&& id.equals(other.id)
					&& context.equals(other.context);
		}
		return false;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(ownerId, type, id, context);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("UID@[ownerId=%s type=%s id=%s context=%s]", ownerId, type, id, context);
	}


}

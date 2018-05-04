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
package bwfdm.replaydh.workflow;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import java.util.Objects;

import bwfdm.replaydh.workflow.schema.IdentifierType;

/**
 *
 * @author Markus
 */
public class Identifier implements Comparable<Identifier> {

	private final String id;
	private final IdentifierType type;
	private final String context;

	public Identifier(IdentifierType type, String id) {
		this(type, id, null);
	}

	public Identifier(IdentifierType type, String id, String context) {
		this.type = requireNonNull(type);
		this.id = requireNonNull(id);
		this.context = context;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Identifier other) {
		return getType().compareTo(other.getType());
	}

    /**
     * Returns the type information for this identifier.
     *
     * The set of possible types and their semantics is depending on the
     * context of the identifier (e.g. the DataCite Kernel for resources).
     *
     * @return
     */
	public IdentifierType getType() {
		return type;
	}

    /**
     * Returns the actual id for this identifier.
     *
     * Example: "John Smith" for an identifier used for {@link Person} objects.
     *
     * @return
     */
	public String getId() {
		return id;
	}

	/**
	 * @return the context
	 */
	public String getContext() {
		return context;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return hash(type.getLabel(), id, context);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		} else if(obj instanceof Identifier) {
			Identifier other = (Identifier) obj;

			return type.getLabel().equals(other.type.getLabel())
					&& id.equals(other.id)
					&& Objects.deepEquals(context, other.context);
		}
		return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(id);
		if(context!=null) {
			sb.append('[').append(context).append(']');
		}
		sb.append('@').append(type);

		return sb.toString();
	}
}

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
package bwfdm.replaydh.ui.workflow;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import bwfdm.replaydh.workflow.schema.IdentifierType;

/**
 * @author Markus Gärtner
 *
 */
public class IdentifierTypeProxy implements Comparable<IdentifierTypeProxy> {

	public final IdentifierType identifierType;

	public final String label;

	public IdentifierTypeProxy(IdentifierType identifierType) {
		this(requireNonNull(identifierType), null);
	}

	public IdentifierTypeProxy(String label) {
		this(null, requireNonNull(label));
	}

	private IdentifierTypeProxy(IdentifierType identifierType, String label) {
		this.identifierType = identifierType;
		this.label = label;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String s = identifierType==null ? null : identifierType.getName();
		if(s==null) {
			s = label;
		}
		return s;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(label, identifierType);
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		} else if(obj instanceof IdentifierTypeProxy) {
			IdentifierTypeProxy other = (IdentifierTypeProxy) obj;
			return Objects.equals(identifierType, other.identifierType)
					&& Objects.equals(label, other.label);
		}
		return false;
	}

	public IdentifierTypeProxy valueOf(String s) {

		if(s==null || s.isEmpty()) {
			return null;
		}

		if(Objects.equals(s, toString())) {
			return this;
		}

		return new IdentifierTypeProxy(s);
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(IdentifierTypeProxy o) {
		return toString().compareTo(o.toString());
	}
}

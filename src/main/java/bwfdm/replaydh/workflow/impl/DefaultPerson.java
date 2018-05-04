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
package bwfdm.replaydh.workflow.impl;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import java.util.Objects;

import bwfdm.replaydh.workflow.Person;

public class DefaultPerson extends AbstractIdentifiable implements Person {

	public static DefaultPerson blankPerson() {
		return new DefaultPerson(false);
	}

	public static DefaultPerson uniquePerson() {
		return new DefaultPerson(true);
	}

	public static DefaultPerson withRole(String role) {
		DefaultPerson person = uniquePerson();
		person.setRole(role);
		return person;
	}

	private String role;

	protected DefaultPerson(boolean autoCreateSystemId) {
		super(autoCreateSystemId);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Identifiable#getType()
	 */
	@Override
	public Type getType() {
		return Type.PERSON;
	}

	/**
	 * @see bwfdm.replaydh.workflow.Person#getRole()
	 */
	@Override
	public String getRole() {
		return role;
	}

	/**
	 * @param role
	 *            the role to set
	 */
	@Override
	public void setRole(String role) {
		requireNonNull(role);
		this.role = role;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return hash(role, identifiers());
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof Person) {
			Person other = (Person) obj;

			return Objects.equals(role, other.getRole()) && super.equals(other);
		}
		return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Person@[identifiers=%s role=%s]", identifiers(), role);
	}
}

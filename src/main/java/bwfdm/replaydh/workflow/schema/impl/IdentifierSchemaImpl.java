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
package bwfdm.replaydh.workflow.schema.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import bwfdm.replaydh.workflow.schema.IdentifierSchema;
import bwfdm.replaydh.workflow.schema.IdentifierType;

/**
 * @author Markus Gärtner
 *
 */
public class IdentifierSchemaImpl implements IdentifierSchema {

	private final Set<IdentifierType> identifierTypes = new HashSet<>();

	private boolean allowCustomIdentifierTypes = false;

	private IdentifierType defaultIdentifierType;

	/**
	 * @see bwfdm.replaydh.workflow.schema.IdentifierSchema#getIdentifierTypes()
	 */
	@Override
	public Set<IdentifierType> getIdentifierTypes() {
		return Collections.unmodifiableSet(identifierTypes);
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.IdentifierSchema#forEachIdentifierType(java.util.function.Consumer)
	 */
	@Override
	public void forEachIdentifierType(Consumer<? super IdentifierType> action) {
		identifierTypes.forEach(action);
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.IdentifierSchema#allowCustomIdentifierTypes()
	 */
	@Override
	public boolean allowCustomIdentifierTypes() {
		return allowCustomIdentifierTypes;
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.IdentifierSchema#getDefaultIdentifierType()
	 */
	@Override
	public IdentifierType getDefaultIdentifierType() {
		return defaultIdentifierType;
	}

	public boolean isAllowCustomIdentifierTypes() {
		return allowCustomIdentifierTypes;
	}

	public IdentifierSchemaImpl setAllowCustomIdentifierTypes(boolean allowCustomIdentifierTypes) {
		this.allowCustomIdentifierTypes = allowCustomIdentifierTypes;
		return this;
	}

	public IdentifierSchemaImpl setDefaultIdentifierType(IdentifierType defaultIdentifierType) {
		this.defaultIdentifierType = defaultIdentifierType;
		return this;
	}

	public IdentifierSchemaImpl addIdentifierType(IdentifierType identifierType) {
		if(!identifierTypes.add(identifierType))
			throw new IllegalArgumentException("Identifier type already present: "+identifierType);
		return this;
	}
}

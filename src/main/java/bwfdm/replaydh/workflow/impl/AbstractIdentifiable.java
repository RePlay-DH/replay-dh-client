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

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;

public abstract class AbstractIdentifiable implements Identifiable {

	private final transient Map<String, Identifier> identifiers = new HashMap<>();

	private String description;

	protected AbstractIdentifiable(boolean autoCreateSystemId) {
		// no-op
	}

	protected Map<String, Identifier> identifiers() {
		return identifiers;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @see bwfdm.replaydh.workflow.Identifiable#forEachIdentifier(java.util.function.Consumer)
	 */
	@Override
	public void forEachIdentifier(Consumer<? super Identifier> action) {
		identifiers.values().forEach(action);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Identifiable#hasIdentifier(java.util.function.Predicate)
	 */
	@Override
	public boolean hasIdentifier(Predicate<? super Identifier> predicate) {
		for(Identifier identifier : identifiers.values()) {
			if(predicate.test(identifier)) {
				return true;
			}
		}
		return false;
	}

	private static final String key(Identifier identifier) {
		return identifier.getType().getStringValue();
	}

	@Override
	public void addIdentifier(Identifier identifier) {
		requireNonNull(identifier);

		Map<String, Identifier> identifiers = this.identifiers;
		if(identifiers.putIfAbsent(key(identifier), identifier)!=null)
			throw new IllegalArgumentException("Duplicate identifier for type: "+identifier);
	}

	@Override
	public void removeIdentifier(Identifier identifier) {
		requireNonNull(identifier);

		Map<String, Identifier> identifiers = this.identifiers;
		if(identifiers.remove(key(identifier))!=identifier)
			throw new IllegalArgumentException("Unknown identifier: "+identifier);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Identifiable#removeIdentifier(java.lang.String)
	 */
	@Override
	public void removeIdentifier(String type) {
		requireNonNull(type);

		Map<String, Identifier> identifiers = this.identifiers;
		if(identifiers.remove(type)==null)
			throw new IllegalArgumentException("No identifier registered for type: "+type);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Identifiable#hasIdentifier(bwfdm.replaydh.workflow.Identifier)
	 */
	@Override
	public boolean hasIdentifier(Identifier identifier) {
		Map<String, Identifier> identifiers = this.identifiers;
		return identifiers.containsKey(key(identifier));
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		} else if(obj instanceof Identifiable) {
			Identifiable other = (Identifiable) obj;

			Set<String> localKeys = identifiers.keySet();
			Set<String> foreignKeys = other.getIdentifierTypes();

			if(!Objects.equals(localKeys, foreignKeys)) {
				return false;
			}

			for(String key : localKeys) {
				Identifier localId = identifiers.get(key);
				Identifier foreignId = other.getIdentifier(key);

				if(!Objects.equals(localId, foreignId)) {
					return false;
				}
			}

			return true;
		}
		return false;
	}

	/**
	 * @see bwfdm.replaydh.workflow.Identifiable#getIdentifier(java.lang.String)
	 */
	@Override
	public Identifier getIdentifier(String type) {
		return identifiers.get(type);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Identifiable#getIdentifierTypes()
	 */
	@Override
	public Set<String> getIdentifierTypes() {
		return Collections.unmodifiableSet(identifiers.keySet());
	}

	/**
	 * @see bwfdm.replaydh.workflow.Identifiable#hasIdentifiers()
	 */
	@Override
	public boolean hasIdentifiers() {
		return !identifiers.isEmpty();
	}

    /**
     * Copies content from the given {@code source} identifiable.
     * The basic version of this method only copies the {@link #getDescription() description}
     * and available {@link #getIdentifiers() identifiers}.
     *
     * @param source
     */
    @Override
	public void copyFrom(Identifiable source) {
    	description = source.getDescription();
    	source.forEachIdentifier(this::addIdentifier); // Identifier is a value object class, so we can reuse instances
    }
}

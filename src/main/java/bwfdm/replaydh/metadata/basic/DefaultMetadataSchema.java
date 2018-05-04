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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import bwfdm.replaydh.metadata.MetadataSchema;
import bwfdm.replaydh.metadata.ValueRestriction;
import bwfdm.replaydh.utils.Label;
import bwfdm.replaydh.utils.Multiplicity;

/**
 * Defines a customizable
 *
 * @author Markus Gärtner
 *
 */
public class DefaultMetadataSchema implements MetadataSchema {

	/**
	 * Basic restrictions applicable to names only
	 */
	private ValueRestriction nameRestriction = ValueRestriction.EMPTY_RESTRICTION;

	/**
	 * Basic restrictions applicable to values only
	 */
	private ValueRestriction valueRestriction = ValueRestriction.EMPTY_RESTRICTION;

	/**
	 * Globally defined default names
	 */
	private final Set<Label> allowedNames = new HashSet<>();

//	/**
//	 * Globally defined default values
//	 */
//	private final Set<Label> allowedValues = new HashSet<>();

	/**
	 * Flag to signal that only names contained in {@link #allowedNames} are to be considered legal.
	 */
	private boolean namesLimited = false;

//	/**
//	 * Flag to signal that only values contained in {@link #allowedValues} are to be considered legal.
//	 */
//	private boolean valuesLimited = false;

	/**
	 * Predefined legal values for certain names
	 */
	private final Map<String, EntryInfo> infosByName = new HashMap<>();

	/**
	 * Minimal set of names that must have entries in a given record
	 * for the record to be considered valid.
	 */
	private final Set<Label> requiredNames = new HashSet<>();

	private static <E extends Object> void replace(Set<E> buffer, Collection<? extends E> c) {
		buffer.clear();
		buffer.addAll(c);
	}

	public void setNameRestriction(ValueRestriction nameRestriction) {
		requireNonNull(nameRestriction);

		this.nameRestriction = nameRestriction;
	}

	public void setValueRestriction(ValueRestriction valueRestriction) {
		requireNonNull(valueRestriction);

		this.valueRestriction = valueRestriction;
	}

	public void setNamesLimited(boolean namesLimited) {
		this.namesLimited = namesLimited;
	}

//	public void setValuesLimited(boolean valuesLimited) {
//		this.valuesLimited = valuesLimited;
//	}

	public void setAllowedNames(Collection<? extends Label> c) {
		requireNonNull(c);

		replace(allowedNames, c);
	}

//	public void setAllowedValues(Collection<? extends Label> c) {
//		requireNonNull(c);
//
//		replace(allowedValues, c);
//	}

	public void setRequiredNames(Collection<? extends Label> c) {
		requireNonNull(c);

		replace(requiredNames, c);
	}

	public void setValuesLimited(String name, boolean valuesLimited) {
		requireNonNull(name);

		info(name, true, false).valuesLimited = valuesLimited;
	}

	public void setMultiplicity(String name, Multiplicity multiplicity) {
		requireNonNull(name);
		requireNonNull(multiplicity);

		info(name, true, false).multiplicity = multiplicity;
	}

	public void setAllowedValues(String name, Collection<? extends Label> c) {
		requireNonNull(name);
		requireNonNull(c);

		replace(info(name, true, false).allowedValues, c);
	}

	public void setValueRestriction(String name, ValueRestriction valueRestriction) {
		requireNonNull(name);

		info(name, true, false).valueRestriction = valueRestriction;
	}

	/**
	 * Read-only shared fallback info so methods can easily return default
	 * values.
	 */
	private static final EntryInfo SHARED_FALLBACK_INFO = new EntryInfo();

	private EntryInfo info(String name, boolean createIfMissing, boolean ensureNonNull) {
		EntryInfo info = infosByName.get(name);
		if(info==null && createIfMissing) {
			info = new EntryInfo();
			infosByName.put(name, info);
		}
		if(info==null && ensureNonNull) {
			info = SHARED_FALLBACK_INFO;
		}

		return info;
	}

	private Label find0(Collection<? extends Label> c, String label) {
		for(Label l : c) {
			if(label.equals(l.getLabel())) {
				return l;
			}
		}
		return null;
	}

	@Override
	public Label getName(String name) {
		return find0(allowedNames, name);
	}

//	@Override
//	public Label getValue(String value) {
//		return find0(allowedValues, value);
//	}

	@Override
	public Label getValue(String name, String value) {
		return find0(info(name, false, true).allowedValues, value);
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#getMultiplicity(java.lang.String)
	 */
	@Override
	public Multiplicity getMultiplicity(String name) {
		return info(name, false, true).multiplicity;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#getAllowedValues(java.lang.String)
	 */
	@Override
	public Set<Label> getAllowedValues(String name) {
		return Collections.unmodifiableSet(info(name, false, true).allowedValues);
	}

//	/**
//	 * @see bwfdm.replaydh.metadata.MetadataSchema#getAllowedValues()
//	 */
//	@Override
//	public Set<Label> getAllowedValues() {
//		return Collections.unmodifiableSet(allowedValues);
//	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#getAllowedNames()
	 */
	@Override
	public Set<Label> getAllowedNames() {
		return Collections.unmodifiableSet(allowedNames);
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#getRequiredNames()
	 */
	@Override
	public Set<Label> getRequiredNames() {
		return Collections.unmodifiableSet(requiredNames);
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#isNamesLimited()
	 */
	@Override
	public boolean isNamesLimited() {
		return namesLimited;
	}

//	/**
//	 * @see bwfdm.replaydh.metadata.MetadataSchema#isValuesLimited()
//	 */
//	@Override
//	public boolean isValuesLimited() {
//		return valuesLimited;
//	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#isValuesLimited(java.lang.String)
	 */
	@Override
	public boolean isValuesLimited(String name) {
		return info(name, false, true).valuesLimited;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#getRestrictionForNames()
	 */
	@Override
	public ValueRestriction getRestrictionForNames() {
		return nameRestriction;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#getRestrictionForValues()
	 */
	@Override
	public ValueRestriction getRestrictionForValues() {
		return valueRestriction;
	}

	@Override
	public ValueRestriction getRestrictionForEntry(String name) {
		return info(name, false, true).valueRestriction;
	}

	@Override
	public boolean hasRequiredNames() {
		return !requiredNames.isEmpty();
	}

	private static class EntryInfo {
		Multiplicity multiplicity = Multiplicity.ANY;
		final Set<Label> allowedValues = new HashSet<>();
		boolean valuesLimited = false;
		ValueRestriction valueRestriction;
	}
}

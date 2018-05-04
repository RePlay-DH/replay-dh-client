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

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import bwfdm.replaydh.metadata.MetadataRecord.Entry;
import bwfdm.replaydh.utils.Label;
import bwfdm.replaydh.utils.LazyCollection;
import bwfdm.replaydh.utils.Multiplicity;

/**
 * Base interface for builders and editors of metadata records
 * to provide methods that allow client code to check if certain
 * {@link Entry#getName() name} and {@link Entry#getValue() value}
 * Strings are allowed or to fetch predefined sets of legal values.
 *
 * @author Markus Gärtner
 *
 */
public interface MetadataSchema {


	/**
	 * Checks whether {@code name} is a legal String value usable
	 * as {@link Entry#getName() name} of a new entry.
	 *
	 * @param name
	 * @return
	 */
	default boolean isAllowedName(String name) {
		requireNonNull(name);

		if(!getRestrictionForNames().isValid(name)) {
			return false;
		}

		if(isNamesLimited() && getName(name)==null) {
			return false;
		}

		return true;
	}

	/**
	 * Checks whether {@code value} is a legal String value usable
	 * as {@link Entry#getValue() value} of a new entry assuming the
	 * entry's {@link Entry#getName() name} is equal to the given
	 * {@code name} parameter.
	 *
	 * @param name
	 * @param value
	 * @return
	 */
	default boolean isAllowedValue(String name, String value) {
		requireNonNull(name);
		requireNonNull(value);

		ValueRestriction valueRestriction = getRestrictionForEntry(name);

		// Use entry specific restrictions if available
		if(valueRestriction!=null) {
			if(!valueRestriction.isValid(value)) {
				return false;
			}
	    } else if(!getRestrictionForValues().isValid(value)) {
			return false;
		}

		if(isValuesLimited(name)) {
			if(getValue(name, value)==null) {
				return false;
			}
		}
//		else if(isValuesLimited()) {
//			if(getValue(value)==null) {
//				return false;
//			}
//		}

		return true;
	}

	/**
	 * Returns the multiplicity that defines how many {@link Entry entries} for a
	 * given {@code name} the repository expects or supports.
	 *
	 * @param name
	 * @return
	 */
	Multiplicity getMultiplicity(String name);

	/**
	 * Fetches the entry description mapped to the specified
	 * {@code name} or {@link null} if no such mapping exists.
	 *
	 * @param name
	 * @return
	 */
	Label getName(String name);

//	Label getValue(String value);

	Label getValue(String name, String value);

	/**
	 * Returns a set of legal String values usable as {@link Entry#getValue() values}
	 * for a new entry, assuming the entry's {@link Entry#getName()} is equal to the
	 * {@code name} parameter.
	 * <p>
	 * Note that this is a highly context sensitive method and client code should expect
	 * the returned set to be different even for repeated invocations when using the
	 * same {@code name} argument.
	 *
	 * @param name
	 * @return
	 */
	Set<Label> getAllowedValues(String name);

//	/**
//	 * Returns a global set of legal {@link Entry#getValue() value} Strings.
//	 * <p>
//	 *
//	 *
//	 * @return
//	 */
//	Set<Label> getAllowedValues();

	/**
	 * Returns a global set of legal {@link Entry#getName() name} Strings.
	 *
	 * @return
	 */
	Set<Label> getAllowedNames();

	/**
	 * Returns a static minimal set of {@link Entry#getName() name} Strings
	 * that valid {@link MetadataRecord records} <b>must</b> have legal entries
	 * for.
	 *
	 * @return
	 */
	Set<Label> getRequiredNames();

	default boolean hasRequiredNames() {
		return !getRequiredNames().isEmpty();
	}

	/**
	 * Returns whether or not the vocabulary for {@link Entry#getName() name}
	 * Strings is (currently) limited. If this method returns {@code true} then
	 * only Strings contained in the set returned by {@link #getAllowedNames()}
	 * are to be considered legal. A return value of {@code false} means that
	 * only {@link #getRestrictionForNames() general restrictions} apply
	 *
	 * @return
	 */
	boolean isNamesLimited();

//	boolean isValuesLimited();

	/**
	 * Returns whether or not the vocabulary for {@link Entry#getName() value}
	 * Strings is (currently) limited. If this method returns {@code true} then
	 * only Strings contained in the set returned by {@link #getAllowedValues(String)}
	 * are to be considered legal. A return value of {@code false} means that
	 * only general restrictions from {@link #getRestrictionForValues()}
	 * or {@link #getRestrictionForEntry(String)} apply.
	 *
	 * @return
	 */
	boolean isValuesLimited(String name);

	/**
	 * Returns a restriction on legal {@link Entry#getName() name} Strings.
	 * This method should never return {@code null}. In case no restrictions
	 * on name values are imposed the <i>empty</i> {@link ValueRestriction#EMPTY_RESTRICTION}
	 * is to be returned.
	 *
	 * @return
	 */
	ValueRestriction getRestrictionForNames();

	/**
	 * Returns a restriction on legal {@link Entry#getValue() value} Strings.
	 * This method should never return {@code null}. In case no restrictions
	 * on value Strings are imposed the <i>empty</i> {@link ValueRestriction#EMPTY_RESTRICTION}
	 * is to be returned.
	 *
	 * @return
	 */
	ValueRestriction getRestrictionForValues();

	/**
	 * Returns a restriction on legal {@link Entry#getValue() value} Strings for
	 * the {@link Entry} specified by {@code name}.
	 * <p>
	 * If the underlying metadata schema doesn't define a dedicated restriction
	 * for the given entry other than the general one obtained via {@link #getRestrictionForValues()}
	 * then this method should return {@code null}.
	 *
	 * @param name
	 * @return
	 */
	ValueRestriction getRestrictionForEntry(String name);

	public static void checkCanAdd(MetadataSchema verifier, MetadataRecord record, String name, String value) {
		requireNonNull(verifier);
		requireNonNull(name);
		requireNonNull(value);

		if(!verifier.isAllowedName(name))
			throw new MetadataException("Unsupported entry name: "+name);

		if(!verifier.isAllowedValue(name, value))
			throw new MetadataException("Unsupported value for entry name '"+name+"': "+value);

		Multiplicity multiplicity = verifier.getMultiplicity(name);
		int present = record.getEntryCount(name);
		// Check if we're at a legal value after adding 1
		if(!multiplicity.isLegalCount(present+1))
			throw new MetadataException(String.format("Invalid number of values for multiplicity '%s' at name '%s': %d",
					multiplicity, name, present+1));
	}

	public static void checkIsComplete(MetadataSchema verifier, MetadataRecord record) {
		requireNonNull(verifier);
		requireNonNull(record);

		if(verifier.hasRequiredNames()) {
			Set<Label> requiredNames = verifier.getRequiredNames();

			LazyCollection<Label> missingNames = LazyCollection.lazySet();

			for(Label label : requiredNames) {
				if(!record.hasEntries(label.getLabel())) {
					missingNames.add(label);
				}
			}

			if(!missingNames.isEmpty())
				throw new MetadataException("Missing required entries in record '"
							+record.getUID()+"': "+Arrays.toString(missingNames.getAsArray()));
		}
	}

	/**
	 * A shared state-less schema implementation that does not pose
	 * any restrictions on the structure or composition of metadata records.
	 */
	public static final MetadataSchema EMPTY_VERIFIER = new MetadataSchema() {

		@Override
		public boolean isValuesLimited(String name) {
			return false;
		}

//		@Override
//		public boolean isValuesLimited() {
//			return false;
//		}

		@Override
		public boolean isNamesLimited() {
			return false;
		}

		@Override
		public ValueRestriction getRestrictionForValues() {
			return ValueRestriction.EMPTY_RESTRICTION;
		}

		@Override
		public ValueRestriction getRestrictionForNames() {
			return ValueRestriction.EMPTY_RESTRICTION;
		}

		@Override
		public ValueRestriction getRestrictionForEntry(String name) {
			return null;
		}

		@Override
		public Set<Label> getRequiredNames() {
			return Collections.emptySet();
		}

		@Override
		public Multiplicity getMultiplicity(String name) {
			return Multiplicity.ANY;
		}

//		@Override
//		public Set<Label> getAllowedValues() {
//			return Collections.emptySet();
//		}

		@Override
		public Set<Label> getAllowedValues(String name) {
			return Collections.emptySet();
		}

		@Override
		public Set<Label> getAllowedNames() {
			return Collections.emptySet();
		}

		@Override
		public Label getName(String name) {
			return null;
		}

//		@Override
//		public Label getValue(String value) {
//			return null;
//		}

		@Override
		public Label getValue(String name, String value) {
			return null;
		}
	};
}

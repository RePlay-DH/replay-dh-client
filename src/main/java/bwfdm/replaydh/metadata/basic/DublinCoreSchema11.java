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

import java.util.Collections;
import java.util.Set;

import bwfdm.replaydh.metadata.MetadataException;
import bwfdm.replaydh.metadata.MetadataSchema;
import bwfdm.replaydh.metadata.ValueRestriction;
import bwfdm.replaydh.utils.Label;
import bwfdm.replaydh.utils.Multiplicity;

/**
 * Verifier implementing the specification of the Dublin Core Metadata DublinCoreField Set, Version 1.1
 * as listed at <a href="http://dublincore.org/documents/dces/">http://dublincore.org/documents/dces/</a>.
 *
 *
 * @author Markus Gärtner
 * @see <a href="http://dublincore.org/documents/dces/">http://dublincore.org/documents/dces/</a>
 */
public class DublinCoreSchema11 implements MetadataSchema {

	public static final DublinCoreSchema11 SHARED_INSTANCE = new DublinCoreSchema11(false);

	/*
    Title
    Creator
    Subject
    Description
    Publisher
    Contributor
    Date
    Type
    Format
    Identifier
    Source
    Language
    Relation
    Coverage
    Rights

	 */

	private static final Multiplicity DEFAULT_MULTIPLICITY = Multiplicity.ANY;
	private static final String DEFAULT_NAME = null;
	private static final String DEFAULT_DESCRIPTION = "";
	private static final String DEFAULT_URI = null;

	public static final String ID = "dublin-core-v1.1";

	private final boolean strict;

	public DublinCoreSchema11(boolean strict) {
		this.strict = strict;
	}

	public DublinCoreSchema11() {
		this(false);
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#getId()
	 */
	@Override
	public String getId() {
		return ID;
	}

	/**
	 * @return the strict
	 */
	public boolean isStrict() {
		return strict;
	}

	private DublinCoreField getField(String name) {
		requireNonNull(name);
		DublinCoreField element = DublinCoreField.forKey(name);
		if(element==null) {
			if(strict)
				throw new MetadataException("Unknown metadata key: "+name);
		}

		return element;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#getMultiplicity(java.lang.String)
	 */
	@Override
	public Multiplicity getMultiplicity(String name) {
		DublinCoreField field = getField(name);
		return field==null ? DEFAULT_MULTIPLICITY : field.getMultiplicity();
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#getAllowedValues(java.lang.String)
	 */
	@Override
	public Set<Label> getAllowedValues(String name) {
		return Collections.emptySet();
	}

//	/**
//	 * @see bwfdm.replaydh.metadata.MetadataSchema#getAllowedValues()
//	 */
//	@Override
//	public Set<Label> getAllowedValues() {
//		return Collections.emptySet();
//	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#getAllowedNames()
	 */
	@Override
	public Set<Label> getAllowedNames() {
		return (Set<Label>)DublinCoreField.getAvailableFields();
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#getRequiredNames()
	 */
	@Override
	public Set<Label> getRequiredNames() {
		return DublinCoreField.getRequiredFields();
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#isNamesLimited()
	 */
	@Override
	public boolean isNamesLimited() {
		return strict;
	}

//	/**
//	 * @see bwfdm.replaydh.metadata.MetadataSchema#isValuesLimited()
//	 */
//	@Override
//	public boolean isValuesLimited() {
//		return false;
//	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#isValuesLimited(java.lang.String)
	 */
	@Override
	public boolean isValuesLimited(String name) {
		return false;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#getRestrictionForNames()
	 */
	@Override
	public ValueRestriction getRestrictionForNames() {
		return ValueRestriction.EMPTY_RESTRICTION;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#getRestrictionForValues()
	 */
	@Override
	public ValueRestriction getRestrictionForValues() {
		return ValueRestriction.EMPTY_RESTRICTION;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#getRestrictionForEntry(java.lang.String)
	 */
	@Override
	public ValueRestriction getRestrictionForEntry(String name) {
		return ValueRestriction.EMPTY_RESTRICTION;
	}

	@Override
	public Label getName(String name) {
		return DublinCoreField.forKey(name);
	}

//	@Override
//	public Label getValue(String value) {
//		return null;
//	}

	@Override
	public Label getValue(String name, String value) {
		return null;
	}


}

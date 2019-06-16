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

import java.util.Set;

import bwfdm.replaydh.metadata.MetadataSchema;
import bwfdm.replaydh.metadata.ValueRestriction;
import bwfdm.replaydh.utils.Label;
import bwfdm.replaydh.utils.Multiplicity;

/**
 * An implementation of {@link MetadataSchema} that does not add any
 * actual verification logic but instead delegates all method calls to
 * another schema specified at construction time.
 * <p>
 * This class exists as skeleton base for new implementations that wish
 * to only make minor custom adjustments.
 *
 * @author Markus Gärtner
 *
 */
public class DelegatingMetadataSchema implements MetadataSchema {

	protected final MetadataSchema schema;

	public DelegatingMetadataSchema(MetadataSchema schema) {
		requireNonNull(schema);

		this.schema = schema;
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataSchema#getId()
	 */
	@Override
	public String getId() {
		return schema.getId();
	}

	@Override
	public boolean isAllowedName(String name) {
		return schema.isAllowedName(name);
	}

	@Override
	public boolean isAllowedValue(String name, String value) {
		return schema.isAllowedValue(name, value);
	}

	@Override
	public Multiplicity getMultiplicity(String name) {
		return schema.getMultiplicity(name);
	}

	@Override
	public Label getName(String name) {
		return schema.getName(name);
	}

//	@Override
//	public Label getValue(String value) {
//		return getValue(value);
//	}

	@Override
	public Label getValue(String name, String value) {
		return getValue(name, value);
	}

	@Override
	public Set<Label> getAllowedValues(String name) {
		return schema.getAllowedValues(name);
	}

//	@Override
//	public Set<Label> getAllowedValues() {
//		return schema.getAllowedValues();
//	}

	@Override
	public Set<Label> getAllowedNames() {
		return schema.getAllowedNames();
	}

	@Override
	public Set<Label> getRequiredNames() {
		return schema.getRequiredNames();
	}

	@Override
	public boolean isNamesLimited() {
		return schema.isNamesLimited();
	}

	@Override
	public boolean isValuesLimited(String name) {
		return schema.isValuesLimited(name);
	}

	@Override
	public ValueRestriction getRestrictionForNames() {
		return schema.getRestrictionForNames();
	}

	@Override
	public ValueRestriction getRestrictionForValues() {
		return schema.getRestrictionForValues();
	}

	@Override
	public ValueRestriction getRestrictionForEntry(String name) {
		return schema.getRestrictionForEntry(name);
	}

	@Override
	public boolean hasRequiredNames() {
		return schema.hasRequiredNames();
	}

//	@Override
//	public boolean isValuesLimited() {
//		return schema.isValuesLimited();
//	}
}

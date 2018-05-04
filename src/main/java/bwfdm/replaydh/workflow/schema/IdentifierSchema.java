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
package bwfdm.replaydh.workflow.schema;

import static java.util.Objects.requireNonNull;

import java.util.Set;
import java.util.function.Consumer;

import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.workflow.schema.IdentifierType.Uniqueness;
import bwfdm.replaydh.workflow.schema.impl.IdentifierTypeImpl;

/**
 * Models the identifier types associated with a certain context
 * and specified whether or not additional identifier types are
 * allowed or if there is a preferred one to be used as default.
 *
 * @author Markus Gärtner
 *
 */
public interface IdentifierSchema {

	/**
	 * Returns the basic identifier types supported by this
	 * schema.
	 * <p>
	 * Note that a schema <b>must</b> provide at least one
	 * identifier type!
	 *
	 * @return
	 */
	Set<IdentifierType> getIdentifierTypes();

	default void forEachIdentifierType(Consumer<? super IdentifierType> action) {
		getIdentifierTypes().forEach(action);
	}

	/**
	 * Returns whether or not the schema allows the usage of
	 * arbitrary new identifier types. If the return value is
	 * {@code false}, then only the identifier types provided
	 * by the {@link #getIdentifierTypes()} method can be used.
	 * @return
	 */
	boolean allowCustomIdentifierTypes();

	/**
	 * Returns the {@link IdentifierType} that should be preferred
	 * or {@code null} if this schema does not include any preferences.
	 *
	 * @return
	 */
	IdentifierType getDefaultIdentifierType();

	default IdentifierType findIdentifierType(String s) {
		requireNonNull(s);

		for(IdentifierType type : getIdentifierTypes()) {
			if(type.getLabel().equals(s)) {
				return type;
			}
		}

		return null;
	}

	public static IdentifierType parseIdentifierType(IdentifierSchema schema, String s) {
		IdentifierType identifierType = schema.findIdentifierType(s);
		if(identifierType==null && schema.allowCustomIdentifierTypes()) {
			identifierType = (IdentifierType) new IdentifierTypeImpl()
					.setUniqueness(Uniqueness.AMBIGUOUS)
					.setDescription(ResourceManager.getInstance().get("replaydh.workflowSchema.customIdentifierType"))
					.setName(s)
					.setLabel(s);
		}
		return identifierType;
	}
}

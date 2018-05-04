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

import static java.util.Objects.requireNonNull;

import bwfdm.replaydh.utils.LabelImpl;
import bwfdm.replaydh.workflow.schema.IdentifierType;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public class IdentifierTypeImpl extends LabelImpl implements IdentifierType {

	private Uniqueness uniqueness;
	private WorkflowSchema schema;

	/**
	 * @see bwfdm.replaydh.workflow.schema.IdentifierType#getUniqueness()
	 */
	@Override
	public Uniqueness getUniqueness() {
		return uniqueness;
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.IdentifierType#getSchema()
	 */
	@Override
	public WorkflowSchema getSchema() {
		return schema;
	}

	/**
	 * @param uniqueness the uniqueness to set
	 */
	public IdentifierTypeImpl setUniqueness(Uniqueness uniqueness) {
		this.uniqueness = requireNonNull(uniqueness);
		return this;
	}

	/**
	 * @param schema the schema to set
	 */
	public IdentifierTypeImpl setSchema(WorkflowSchema schema) {
		this.schema = requireNonNull(schema);
		return this;
	}

	/**
	 * @see bwfdm.replaydh.utils.LabelImpl#toString()
	 */
	@Override
	public String toString() {
		return "IdentifierType@"+getLabel();
	}
}

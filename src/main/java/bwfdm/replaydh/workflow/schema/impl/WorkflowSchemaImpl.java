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

import bwfdm.replaydh.metadata.MetadataSchema;
import bwfdm.replaydh.workflow.schema.IdentifierSchema;
import bwfdm.replaydh.workflow.schema.LabelSchema;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public class WorkflowSchemaImpl implements WorkflowSchema {

	private final String id;
	private final String description;

	private final IdentifierSchema personIdentifierSchema;
	private final IdentifierSchema resourceIdentifierSchema;

	private final LabelSchema roleSchema;
	private final LabelSchema resourceTypeSchema;

	private final MetadataSchema propertiesSchema;

	public WorkflowSchemaImpl(String id, String description, IdentifierSchema personIdentifierSchema, IdentifierSchema resourceIdentifierSchema,
			LabelSchema roleSchema, LabelSchema resourceTypeSchema, MetadataSchema propertiesSchema) {
		this.id = requireNonNull(id);
		this.description = description;

		this.personIdentifierSchema = requireNonNull(personIdentifierSchema);
		this.resourceIdentifierSchema = requireNonNull(resourceIdentifierSchema);
		this.roleSchema = requireNonNull(roleSchema);
		this.resourceTypeSchema = requireNonNull(resourceTypeSchema);

		this.propertiesSchema = propertiesSchema;
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.WorkflowSchema#getPersonIdentifierSchema()
	 */
	@Override
	public IdentifierSchema getPersonIdentifierSchema() {
		return personIdentifierSchema;
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.WorkflowSchema#getResourceIdentifierSchema()
	 */
	@Override
	public IdentifierSchema getResourceIdentifierSchema() {
		return resourceIdentifierSchema;
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.WorkflowSchema#getRoleSchema()
	 */
	@Override
	public LabelSchema getRoleSchema() {
		return roleSchema;
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.WorkflowSchema#getResourceTypeSchema()
	 */
	@Override
	public LabelSchema getResourceTypeSchema() {
		return resourceTypeSchema;
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.WorkflowSchema#getId()
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.WorkflowSchema#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.WorkflowSchema#getPropertiesSchema()
	 */
	@Override
	public MetadataSchema getPropertiesSchema() {
		return propertiesSchema;
	}
}

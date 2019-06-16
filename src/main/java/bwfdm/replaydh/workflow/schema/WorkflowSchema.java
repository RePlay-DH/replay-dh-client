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

import bwfdm.replaydh.metadata.MetadataSchema;
import bwfdm.replaydh.utils.SchemaManager;
import bwfdm.replaydh.workflow.Person;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.Tool;
import bwfdm.replaydh.workflow.WorkflowStep;

/**
 * Models a way to customize the vocabulary used for defining
 * resource types, roles, etc that is customized to the field
 * of research our client is used for.
 *
 * @author Markus Gärtner
 *
 */
public interface WorkflowSchema extends SchemaManager.Schema {

	/**
	 * Returns the globally unique identifier of this schema.
	 *
	 * @return
	 */
	@Override
	String getId();

	/**
	 * Returns an explanatory description of the schema in a
	 * human readable format.
	 *
	 * @return
	 */
	String getDescription();

	/**
	 * Returns the schema used for identifying {@link Person}
	 * instances in the workflow.
	 *
	 * @return
	 */
	IdentifierSchema getPersonIdentifierSchema();

	/**
	 * Returns the schema used for identifying {@link Resource}
	 * and {@link Tool} instances in the workflow.
	 *
	 * @return
	 */
	IdentifierSchema getResourceIdentifierSchema();

	/**
	 * Returns the schema used for describing a person's
	 * {@link Person#getRole() role} in a workflow.
	 * @return
	 */
	LabelSchema getRoleSchema();

	/**
	 * Returns the schema used for describing the {@link Resource#getResourceType() type}
	 * of {@link Resource} and {@link Tool} instances in
	 * a workflow.
	 * @return
	 */
	LabelSchema getResourceTypeSchema();

	/**
	 * Allows a workflow schema to also formalize the vocabulary of allowed
	 * {@link WorkflowStep#getProperties() custom properties}.
	 * <p>
	 * This is an optional method.
	 *
	 * @return
	 */
	MetadataSchema getPropertiesSchema();

	/**
	 * Read the default schema definition from the
	 * {@code bwfdm/replaydh/workflow/schema/default-schema.xml} file.
	 *
	 * @return
	 */
	public static WorkflowSchema getDefaultSchema() {
		return DefaultSchemaProxy.getDefaultSchema();
	}

	default IdentifierType getDefaultPathIdentifierType() {
		IdentifierType identifierType = getResourceIdentifierSchema().findIdentifierType(IdentifierType.PATH);
		if(identifierType==null)
			throw new IllegalStateException("Schema is missing default identifier type for '"+IdentifierType.PATH+"'");
		return identifierType;
	}

	default IdentifierType getDefaultChecksumIdentifierType() {
		IdentifierType identifierType = getResourceIdentifierSchema().findIdentifierType(IdentifierType.CHECKSUM);
		if(identifierType==null)
			throw new IllegalStateException("Schema is missing default identifier type for '"+IdentifierType.CHECKSUM+"'");
		return identifierType;
	}

	default IdentifierType getDefaultURLIdentifierType() {
		IdentifierType identifierType = getResourceIdentifierSchema().findIdentifierType(IdentifierType.URL);
		if(identifierType==null)
			throw new IllegalStateException("Schema is missing default identifier type for '"+IdentifierType.URL+"'");
		return identifierType;
	}

	default IdentifierType getDefaultNameIdentifierType() {
		IdentifierType identifierType = getPersonIdentifierSchema().findIdentifierType(IdentifierType.NAME);
		if(identifierType==null)
			throw new IllegalStateException("Schema is missing default identifier type for '"+IdentifierType.NAME+"'");
		return identifierType;
	}

	default IdentifierType getDefaultNameVersionIdentifierType() {
		IdentifierType identifierType = getResourceIdentifierSchema().findIdentifierType(IdentifierType.NAME_VERSION);
		if(identifierType==null)
			throw new IllegalStateException("Schema is missing default identifier type for '"+IdentifierType.NAME_VERSION+"'");
		return identifierType;
	}
}

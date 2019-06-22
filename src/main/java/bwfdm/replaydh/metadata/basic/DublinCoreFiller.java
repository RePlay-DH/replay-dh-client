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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.fill.ResourceMetadataFiller;
import bwfdm.replaydh.workflow.schema.IdentifierType;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public class DublinCoreFiller implements ResourceMetadataFiller {

	@SuppressWarnings("unused")
	private boolean specialTreatment = false;

	// Unused
	private Map<String, String> dc2resource = new HashMap<>();
	// Unused
	private Map<String, String> resource2dc = new HashMap<>();

	private boolean settingsLoaded = false;

	private void loadSettings(RDHEnvironment environment) {
		if(settingsLoaded) {
			return;
		}

		settingsLoaded = true;

		specialTreatment = environment.getBoolean(RDHProperty.METADATA_AUTOFILL_DC_SPECIAL_TREATMENT);
		for(DublinCoreField field : DublinCoreField.values()) {
			String dcKey = field.getLabel();
			String resourceKey = environment.getProperty(field);

			if(resourceKey==null || resourceKey.trim().isEmpty()) {
				continue;
			}

			if(dc2resource.containsKey(dcKey))
				throw new IllegalStateException(String.format(
						"DC key '%s' already mapped to '%s'", dcKey, dc2resource.get(dcKey)));

			if(resource2dc.containsKey(resourceKey))
				throw new IllegalStateException(String.format(
						"Resource key '%s' already mapped to '%s'", resourceKey, resource2dc.get(dcKey)));

			dc2resource.put(dcKey, resourceKey);
			resource2dc.put(resourceKey, dcKey);
		}
	}

	/** Makes sure the given record is following Dublin Core schema */
	private void checkRecord(MetadataRecord record) {
		if(!DublinCoreSchema11.ID.equals(record.getSchemaId()))
			throw new IllegalArgumentException("Incompatible schema: "+record.getSchemaId());
	}

	private boolean isNullOrEmpty(String s) {
		return s==null || s.trim().isEmpty();
	}

	private boolean mapFlat(String value, MutableMetadataRecord target, DublinCoreField field) {
		if(isNullOrEmpty(value)) {
			return false;
		}

		// Fetch actual dc key for entry
		String dcKey = field.getLabel();

		// Ignore if at least 1 entry already set
		if(target.getEntryCount(dcKey)>0) {
			return false;
		}

		target.addEntry(dcKey, value);

		return true;
	}

	private boolean mapIdentifier(IdentifierType identifierType, Resource source,
			MutableMetadataRecord target, DublinCoreField field) {
		if(identifierType==null) {
			return false;
		}

		Identifier identifier = source.getIdentifier(identifierType);
		if(identifier==null) {
			return false;
		}

		String dcKey = field.getLabel();
		if(target.getEntryCount(dcKey)>0) {
			return false;
		}

		target.addEntry(dcKey, identifier.getId());

		return true;
	}

	/**
	 * @see bwfdm.replaydh.workflow.fill.ResourceMetadataFiller#fillRecord(RDHEnvironment, WorkflowSchema, bwfdm.replaydh.workflow.Resource, MutableMetadataRecord)
	 */
	@Override
	public boolean fillRecord(RDHEnvironment environment, WorkflowSchema schema, Resource source, MutableMetadataRecord target) {
		requireNonNull(source);
		requireNonNull(target);

		checkRecord(target);

		loadSettings(environment);

		boolean changed = false;

		// Simple fields
		changed |= mapFlat(source.getDescription(), target, DublinCoreField.DESCRIPTION);
		changed |= mapFlat(source.getResourceType(), target, DublinCoreField.TYPE);

		// Complex fields
		changed |= mapIdentifier(schema.getDefaultNameVersionIdentifierType(), source, target, DublinCoreField.TITLE);
		changed |= mapIdentifier(schema.getDefaultURLIdentifierType(), source, target, DublinCoreField.IDENTIFIER);


		return changed;
	}

	private boolean mapFlat(DublinCoreField field, MetadataRecord source, Consumer<String> setter, String oldValue) {
		// Fetch actual dc key for entry
		String dcKey = field.getLabel();

		// Ignore situations without exactly 1 vale to read
		if(source.getEntryCount(dcKey)!=1) {
			return false;
		}

		String value = source.getEntry(dcKey).getValue();
		if(isNullOrEmpty(value)) {
			return false;
		}

		// Check against existing value
		if(oldValue!=null && oldValue.equals(value)) {
			return false;
		}

		setter.accept(value);

		return true;
	}

	private boolean mapIdentifier(IdentifierType identifierType, MetadataRecord source,
			Resource target, DublinCoreField field) {
		if(identifierType==null) {
			return false;
		}

		// Ignore existing identifiers
		Identifier identifier = target.getIdentifier(identifierType);
		if(identifier!=null) {
			return false;
		}

		// Ignore situations without exactly 1 vale to read
		String dcKey = field.getLabel();
		if(source.getEntryCount(dcKey)!=1) {
			return false;
		}

		String value = source.getEntry(dcKey).getValue();
		if(isNullOrEmpty(value)) {
			return false;
		}

		target.addIdentifier(new Identifier(identifierType, value));

		return true;
	}

	/**
	 * @see bwfdm.replaydh.workflow.fill.ResourceMetadataFiller#fillResource(RDHEnvironment, WorkflowSchema, bwfdm.replaydh.metadata.MetadataRecord, bwfdm.replaydh.workflow.Resource)
	 */
	@Override
	public boolean fillResource(RDHEnvironment environment, WorkflowSchema schema, MetadataRecord source, Resource target) {
		requireNonNull(source);
		requireNonNull(target);

		checkRecord(source);

		loadSettings(environment);

		boolean changed = false;

		changed |= mapFlat(DublinCoreField.DESCRIPTION, source, target::setDescription, target.getDescription());
		changed |= mapFlat(DublinCoreField.TYPE, source, target::setResourceType, target.getResourceType());

		changed |= mapIdentifier(schema.getDefaultNameVersionIdentifierType(), source, target, DublinCoreField.TITLE);
		changed |= mapIdentifier(schema.getDefaultURLIdentifierType(), source, target, DublinCoreField.IDENTIFIER);

		return changed;
	}

}

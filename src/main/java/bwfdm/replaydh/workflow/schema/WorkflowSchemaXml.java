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

import java.io.IOException;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.io.resources.IOResource;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.utils.Label;
import bwfdm.replaydh.utils.LabelImpl;
import bwfdm.replaydh.utils.Lazy;
import bwfdm.replaydh.utils.annotation.XmlFacility;
import bwfdm.replaydh.utils.xml.RDHXml;
import bwfdm.replaydh.utils.xml.XmlParserHandler;
import bwfdm.replaydh.workflow.schema.IdentifierType.Uniqueness;
import bwfdm.replaydh.workflow.schema.impl.IdentifierSchemaImpl;
import bwfdm.replaydh.workflow.schema.impl.IdentifierTypeImpl;
import bwfdm.replaydh.workflow.schema.impl.LabelSchemaImpl;
import bwfdm.replaydh.workflow.schema.impl.WorkflowSchemaImpl;

/**
 * @author Markus Gärtner
 *
 */
@XmlFacility
public class WorkflowSchemaXml {

	private WorkflowSchemaXml() {
		// prevent instantiation
	}

	private static final String NS = RDHXml.RDH_NS;

	private static final String TAG_SCHEMA = NS+':'+"workflowSchema";
	private static final String TAG_PERSON_IDENTIFIERS = NS+':'+"personIdentifiers";
	private static final String TAG_RESORUCE_IDENTIFIERS = NS+':'+"resourceIdentifiers";
	private static final String TAG_RESOURCE_TYPES = NS+':'+"resourceTypes";
	private static final String TAG_ROLES = NS+':'+"roles";
	private static final String TAG_IDENTIFER_TYPE = NS+':'+"identifierType";
	private static final String TAG_LABEL = NS+':'+"label";
	private static final String TAG_SUB_LABELS = NS+':'+"subLabels";
	private static final String TAG_SUB_LABEL = NS+':'+"subLabel";
	private static final String TAG_NAME = NS+':'+"name";
	private static final String TAG_DESCRIPTION = NS+':'+"description";

	private static final String ATTR_LOCALIZE = "localize";
	private static final String ATTR_USE_DEFAULT_LOACLIZATION_SUFFIX = "useDefaultLocalizationSuffixes";
	private static final String ATTR_LOCALIZATION_ROOT = "localizationRoot";
	private static final String ATTR_DEFAULT_IDENTIFIER_TYPE = "defaultIdentifierType";
	private static final String ATTR_ALLOW_CUSTOM_IDENTIFIER_TYPES = "allowCustomIdentifierTypes";
	private static final String ATTR_DEFAULT_LABEL = "defaultLabel";
	private static final String ATTR_ALLOW_CUSTOM_LABELS = "allowCustomLabels";
	private static final String ATTR_ALLOW_COMPOUND_LABELS = "allowCompoundLabels";
	private static final String ATTR_COMPOUND_SEPARATOR = "compoundSeparator";
	private static final String ATTR_ALLOW_COMPOUNDS = "allowCompounds";
	private static final String ATTR_UNIQUENESS = "uniqueness";
	private static final String ATTR_ID = "id";
	private static final String ATTR_DESCRIPTION = "description";
	private static final String ATTR_LOCALIZATION_BASE = "localizationBase";

	private static final char LOCA_SEP = '.';

	private static SAXParserFactory parserFactory;

	private static final boolean DEFAULT_LOCALIZE = false;
	private static final boolean DEFAULT_USE_DEFAULT_LOCALIZATION_SUFFIX = false;
	private static final boolean DEFAULT_ALLOW_CUSTOM_IDENTIFIER_TYPES = false;
	private static final boolean DEFAULT_ALLOW_CUSTOM_LABELS = false;
	private static final boolean DEFAULT_ALLOW_COMPOUNDS = false;

	private static final String NAMESPACE = "Workflow";
	private static final String SCHEMA_NAME = "Workflow.xsd";

	private static final Lazy<Schema> schema = RDHXml.createShareableSchemaSource(
			WorkflowSchemaXml.class.getResource(SCHEMA_NAME));

	public static Schema getSchema() {
		return schema.value();
	}

	private static String suffix(String s) {
		return LOCA_SEP+s;
	}

	private static final String NAME_SUFFIX = "name";
	private static final String DESCRIPTION_SUFFIX = "description";

	public static WorkflowSchema readSchema(IOResource resource) throws ExecutionException {
		try(ReadableByteChannel channel = resource.getReadChannel()) {
			try(Reader r = Channels.newReader(channel, StandardCharsets.UTF_8.newDecoder(), IOUtils.BUFFER_LENGTH)) {
				if(parserFactory==null) {
					SAXParserFactory factory = SAXParserFactory.newInstance();

					factory.setNamespaceAware(true);
					factory.setValidating(false);

					factory.setSchema(getSchema());

					parserFactory = factory;
				}


				SAXParser parser = null;
				try {
					parser = parserFactory.newSAXParser();
				} catch (ParserConfigurationException e) {
					throw new ExecutionException("Parser creation failed", e);
				}

				XMLReader reader = parser.getXMLReader();

				ContentHandler contentHandler = new ContentHandler();

				reader.setContentHandler(contentHandler);

				reader.parse(new InputSource(r));

				return contentHandler.schema;
			} catch (SAXException e) {
				throw new ExecutionException(e);
			}
		} catch (IOException e) {
			throw new ExecutionException(e);
		}
	}

//	public static void writeSchema(IOResource resource, WorkflowSchema schema) throws ExecutionException {
//		try(WritableByteChannel channel = resource.getWriteChannel(true)) {
//			try(Writer w = Channels.newWriter(channel, StandardCharsets.UTF_8.newEncoder(), IOUtils.BUFFER_LENGTH)) {
//				if(xmlOutputFactory==null) {
//					xmlOutputFactory = XMLOutputFactory.newFactory();
//				}
//
//				XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(w);
//
//				writer.writeStartDocument("utf-8", "1.0");
//				writer.writeCharacters(LINE_BREAK);
//
//				// Write header part
//				writer.writeStartElement(TAG_SCHEMA);
//				//???
//				writer.writeCharacters(LINE_BREAK);
//
//				//??? write schema elements
//
//				writer.writeEndElement();
//				writer.writeEndDocument();
//			} catch (XMLStreamException e) {
//				throw new ExecutionException(e);
//			}
//		} catch (IOException e) {
//			throw new ExecutionException(e);
//		}
//	}

	private static void linkIdentifierTypes(WorkflowSchema schema) {
		linkIdentifierTypes(schema, schema.getResourceIdentifierSchema());
		linkIdentifierTypes(schema, schema.getPersonIdentifierSchema());
	}

	private static void linkIdentifierTypes(WorkflowSchema workflowSchema, IdentifierSchema schema) {
		for(IdentifierType identifierType : schema.getIdentifierTypes()) {
			linkIdentiferType(workflowSchema, identifierType);
		}
		linkIdentiferType(workflowSchema, schema.getDefaultIdentifierType());
	}

	private static void linkIdentiferType(WorkflowSchema schema, IdentifierType identifierType) {
		if(identifierType instanceof IdentifierTypeImpl) {
			((IdentifierTypeImpl)identifierType).setSchema(schema);
		}
	}

	private static class ContentHandler extends XmlParserHandler {

		private WorkflowSchema schema;

		// TOP LEVEL SCHEMAS
		private IdentifierSchemaImpl personIdentifierSchema;
		private IdentifierSchemaImpl resourceIdentifierSchema;
		private LabelSchemaImpl roleSchema;
		private LabelSchemaImpl resourceTypeSchema;

		private String schemaId;
		private String schemaDescription;

		private IdentifierSchemaImpl activeIdentifierSchema;
		private LabelSchemaImpl activeLabelSchema;

		private LabelImpl activeLabel;
		private LabelImpl activeSubLabel;
		private IdentifierTypeImpl activeIdentifierType;

		private String localizationRoot;
		private boolean localize;
		private boolean useDefaultLocalizationSuffixes;

		private String defaultLabel;

		private String maybeLocalize(String label, String suffix) {
			if(label==null) {
				return null;
			}

			if(!localize) {
				return label;
			}

			if(localizationRoot!=null) {
				label = localizationRoot + LOCA_SEP + label;
			}

			if(useDefaultLocalizationSuffixes && suffix!=null) {
				label = label + LOCA_SEP + suffix;
			}

			return ResourceManager.getInstance().get(label);
		}

		private void tryLocalize(LabelImpl label, Attributes attr) {
			String localizationBase = attr.getValue(ATTR_LOCALIZATION_BASE);
			if(localizationBase==null) {
				return;
			}

			label.setName(maybeLocalize(localizationBase, NAME_SUFFIX));
			label.setDescription(maybeLocalize(localizationBase, DESCRIPTION_SUFFIX));
		}

		private LabelImpl activeLabel() {
			if(activeIdentifierType!=null) {
				return activeIdentifierType;
			} else if(activeLabel!=null) {
				return activeLabel;
			} else
				throw new IllegalStateException("No active label instance");
		}

		private boolean isDefault(Label label) {
			return defaultLabel!=null && defaultLabel.equals(label.getLabel());
		}

		/**
		 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {

			switch (qName) {

			case TAG_SCHEMA: {
				schemaId = attributes.getValue(ATTR_ID);
				schemaDescription = attributes.getValue(ATTR_DESCRIPTION);
				localizationRoot = attributes.getValue(ATTR_LOCALIZATION_ROOT);
				localize = flag(attributes, ATTR_LOCALIZE, DEFAULT_LOCALIZE);
				useDefaultLocalizationSuffixes = flag(attributes,
						ATTR_USE_DEFAULT_LOACLIZATION_SUFFIX, DEFAULT_USE_DEFAULT_LOCALIZATION_SUFFIX);
			} break;

			case TAG_RESORUCE_IDENTIFIERS:
			case TAG_PERSON_IDENTIFIERS: {
				activeIdentifierSchema = new IdentifierSchemaImpl();

				activeIdentifierSchema.setAllowCustomIdentifierTypes(flag(attributes,
						ATTR_ALLOW_CUSTOM_IDENTIFIER_TYPES, DEFAULT_ALLOW_CUSTOM_IDENTIFIER_TYPES));
				defaultLabel = normalize(attributes.getValue(ATTR_DEFAULT_IDENTIFIER_TYPE));
			} break;

			case TAG_ROLES:
			case TAG_RESOURCE_TYPES: {
				activeLabelSchema = new LabelSchemaImpl();

				activeLabelSchema.setAllowCustomLabels(flag(attributes,
						ATTR_ALLOW_CUSTOM_LABELS, DEFAULT_ALLOW_CUSTOM_LABELS));
				activeLabelSchema.setAllowCompoundLabels(flag(attributes,
						ATTR_ALLOW_COMPOUND_LABELS, DEFAULT_ALLOW_COMPOUNDS));
				activeLabelSchema.setCompoundSeparator(normalize(
						attributes.getValue(ATTR_COMPOUND_SEPARATOR)));
				defaultLabel = normalize(attributes.getValue(ATTR_DEFAULT_LABEL));
			} break;

			case TAG_IDENTIFER_TYPE: {
				activeIdentifierType = new IdentifierTypeImpl();

				activeIdentifierType.setLabel(normalize(attributes.getValue(ATTR_ID)));
				activeIdentifierType.setUniqueness(Uniqueness.parseUniqueness(
						normalize(attributes.getValue(ATTR_UNIQUENESS))));

				tryLocalize(activeIdentifierType, attributes);
			} break;

			case TAG_LABEL: {
				activeLabel = new LabelImpl();

				activeLabel.setLabel(normalize(attributes.getValue(ATTR_ID)));

				activeLabelSchema.setAllowCompounds(activeLabel, flag(attributes,
						ATTR_ALLOW_COMPOUNDS, DEFAULT_ALLOW_COMPOUNDS));

				tryLocalize(activeLabel, attributes);
			} break;

			case TAG_NAME:
			case TAG_DESCRIPTION: {
				clearText();
			} break;

			case TAG_SUB_LABELS: {
				// no-op
			} break;

			case TAG_SUB_LABEL: {
				activeSubLabel = new LabelImpl();

				activeSubLabel.setLabel(normalize(attributes.getValue(ATTR_ID)));

				tryLocalize(activeSubLabel, attributes);
			} break;

			default:
				throw new SAXException("Unexpected opening tag: "+qName);
			}
		}

		/**
		 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			switch (qName) {

			case TAG_SCHEMA: {
				schema = new WorkflowSchemaImpl(
						schemaId, schemaDescription,
						personIdentifierSchema, resourceIdentifierSchema,
						roleSchema, resourceTypeSchema, null); //FIXME take property schema into account

				// Perform late binding of identifier types to their host schema
				linkIdentifierTypes(schema);
			} break;

			case TAG_RESORUCE_IDENTIFIERS: {
				resourceIdentifierSchema = activeIdentifierSchema;
				activeIdentifierSchema = null;
			} break;

			case TAG_PERSON_IDENTIFIERS: {
				personIdentifierSchema = activeIdentifierSchema;
				activeIdentifierSchema = null;
			} break;

			case TAG_ROLES: {
				roleSchema = activeLabelSchema;
				activeLabelSchema = null;
			} break;

			case TAG_RESOURCE_TYPES: {
				resourceTypeSchema = activeLabelSchema;
				activeLabelSchema = null;
			} break;

			case TAG_IDENTIFER_TYPE: {
				activeIdentifierSchema.addIdentifierType(activeIdentifierType);
				if(isDefault(activeIdentifierType)) {
					activeIdentifierSchema.setDefaultIdentifierType(activeIdentifierType);
				}
				activeIdentifierType = null;
			} break;

			case TAG_LABEL: {
				activeLabelSchema.addLabel(activeLabel);
				if(isDefault(activeLabel)) {
					activeLabelSchema.setDefaultLabel(activeLabel);
				}
				activeLabel = null;
			} break;

			case TAG_NAME: {
				activeLabel().setName(maybeLocalize(text(), NAME_SUFFIX));
			} break;

			case TAG_DESCRIPTION: {
				activeLabel().setDescription(maybeLocalize(text(), DESCRIPTION_SUFFIX));
			} break;

			case TAG_SUB_LABELS: {
				// no-op
			} break;

			case TAG_SUB_LABEL: {
				activeLabelSchema.addSubLabel(activeLabel, activeSubLabel);
				activeSubLabel = null;
			} break;

			default:
				throw new SAXException("Unexpected closing tag: "+qName);
			}
		}
	}
}

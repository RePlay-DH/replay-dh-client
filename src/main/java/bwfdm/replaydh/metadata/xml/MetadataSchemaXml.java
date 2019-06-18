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
package bwfdm.replaydh.metadata.xml;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
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
import bwfdm.replaydh.metadata.MetadataSchema;
import bwfdm.replaydh.metadata.ValueRestriction;
import bwfdm.replaydh.metadata.basic.DefaultMetadataSchema;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.utils.Label;
import bwfdm.replaydh.utils.LabelImpl;
import bwfdm.replaydh.utils.Lazy;
import bwfdm.replaydh.utils.Multiplicity;
import bwfdm.replaydh.utils.xml.RDHXml;
import bwfdm.replaydh.utils.xml.XmlParserHandler;

/**
 * @author Markus Gärtner
 *
 */
public class MetadataSchemaXml {

	private MetadataSchemaXml() {
		// no-op
	}

	private static final String NS = RDHXml.RDH_NS;

	private static final String TAG_SCHEMA = NS+':'+"metadataSchema";
	private static final String TAG_NAME_RESTRICTION = NS+':'+"nameRestriction";
	private static final String TAG_VALUE_RESTRICTION = NS+':'+"valueRestriction";
	private static final String TAG_ENTRIES = NS+':'+"entries";
	private static final String TAG_ENTRY = NS+':'+"entry";
	private static final String TAG_PATTERN = NS+':'+"pattern";
	private static final String TAG_NAME = NS+':'+"name";
	private static final String TAG_DESCRIPTION = NS+':'+"description";

	private static final String ATTR_NAMES_LIMITED = "namesLimited";
	private static final String ATTR_VALUES_LIMITED = "valuesLimited";
	private static final String ATTR_MULTIPLICITY = "multiplicity";
	private static final String ATTR_NAME = "name";

	private static final String ATTR_LOCALIZE = "localize";
	private static final String ATTR_USE_DEFAULT_LOACLIZATION_SUFFIX = "useDefaultLocalizationSuffixes";
	private static final String ATTR_LOCALIZATION_ROOT = "localizationRoot";
	private static final String ATTR_LOCALIZATION_BASE = "localizationBase";
	private static final String ATTR_ID = "id";
	private static final String ATTR_DESCRIPTION = "description";
	private static final String ATTR_REQUIRED = "required";
	private static final String ATTR_MIN = "min";
	private static final String ATTR_MAX = "max";

	private static SAXParserFactory parserFactory;

	private static final boolean DEFAULT_LOCALIZE = false;
	private static final boolean DEFAULT_USE_DEFAULT_LOCALIZATION_SUFFIX = false;

	private static final String SCHEMA_NAME = "Metadata.xsd";

	private static final Lazy<Schema> schema = RDHXml.createShareableSchemaSource(
			MetadataSchemaXml.class.getResource(SCHEMA_NAME));

	public static Schema getSchema() {
		return schema.value();
	}

	public static MetadataSchema readSchema(IOResource resource) throws ExecutionException {
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

	public static class ContentHandler extends XmlParserHandler {


		private static final String NAME_SUFFIX = "name";
		private static final String DESCRIPTION_SUFFIX = "description";

		private static final char LOCA_SEP = '.';

		private String localizationRoot;
		private boolean localize;
		private boolean useDefaultLocalizationSuffixes;

		private DefaultMetadataSchema schema;
		private LabelImpl entry;

		private int minLength, maxLength;
		private String pattern;

		private final Set<Label> allowedNames = new HashSet<>();
		private final Set<Label> requiredNames = new HashSet<>();

		protected String getLocalizationRoot() {
			return localizationRoot;
		}

		protected boolean isLocalize() {
			return localize;
		}

		protected boolean isUseDefaultLocalizationSuffixes() {
			return useDefaultLocalizationSuffixes;
		}

		public DefaultMetadataSchema getSchema() {
			return schema;
		}

		private String maybeLocalize(String label, String suffix) {
			if(label==null) {
				return null;
			}

			if(!isLocalize()) {
				return label;
			}

			if(getLocalizationRoot()!=null) {
				label = getLocalizationRoot() + LOCA_SEP + label;
			}

			if(isUseDefaultLocalizationSuffixes() && suffix!=null) {
				label = label + LOCA_SEP + suffix;
			}

			return ResourceManager.getInstance().get(label);
		}

		private void tryLocalize(LabelImpl label, Attributes attr) {
			String localizationBase = normalize(attr.getValue(ATTR_LOCALIZATION_BASE));
			if(localizationBase==null) {
				String name = normalize(attr.getValue(ATTR_NAME));
				if(name!=null) {
					label.setName(maybeLocalize(name, null));
				}
				String description = normalize(attr.getValue(ATTR_DESCRIPTION));
				if(description!=null) {
					label.setDescription(maybeLocalize(description, null));
				}
			} else {
				label.setName(maybeLocalize(localizationBase, NAME_SUFFIX));
				label.setDescription(maybeLocalize(localizationBase, DESCRIPTION_SUFFIX));
			}
		}

		/**
		 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {

			switch (qName) {
			case TAG_SCHEMA: {
				schema = new DefaultMetadataSchema();

				localizationRoot = attributes.getValue(ATTR_LOCALIZATION_ROOT);
				localize = flag(attributes, ATTR_LOCALIZE, DEFAULT_LOCALIZE);
				useDefaultLocalizationSuffixes = flag(attributes,
						ATTR_USE_DEFAULT_LOACLIZATION_SUFFIX, DEFAULT_USE_DEFAULT_LOCALIZATION_SUFFIX);

				schema.setNamesLimited(flag(attributes, ATTR_NAMES_LIMITED, false));
			} break;

			case TAG_ENTRIES: {
				// no-op
			} break;

			case TAG_ENTRY: {
				entry = new LabelImpl(requireNonNull(attributes.getValue(ATTR_ID), "Missing id for entry"));
				tryLocalize(entry, attributes);

				String multiplicity = normalize(attributes.getValue(ATTR_MULTIPLICITY));
				if(multiplicity!=null) {
					schema.setMultiplicity(entry.getLabel(), Multiplicity.parseMultiplicity(multiplicity));
				}

				if(flag(attributes, ATTR_REQUIRED, false)) {
					requiredNames.add(entry);
				}

				schema.setValuesLimited(entry.getLabel(), flag(attributes, ATTR_VALUES_LIMITED, false));
			} break;

			case TAG_NAME_RESTRICTION:
			case TAG_VALUE_RESTRICTION: {
				String min = normalize(attributes.getValue(ATTR_MIN));
				minLength = min==null ? ValueRestriction.UNDEFINED_VALUE : Integer.parseInt(min);

				String max = normalize(attributes.getValue(ATTR_MAX));
				maxLength = max==null ? ValueRestriction.UNDEFINED_VALUE : Integer.parseInt(max);
			} break;

			case TAG_PATTERN: {
				// no-op
			} break;

			case TAG_NAME:
			case TAG_DESCRIPTION: {
				clearText();
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
				// no-op
			} break;

			case TAG_ENTRIES: {
				// no-op
			} break;

			case TAG_ENTRY: {
				allowedNames.add(entry);
				entry = null;
			} break;

			case TAG_VALUE_RESTRICTION: {
				ValueRestriction valueRestriction = ValueRestriction.forPattern(pattern, minLength, maxLength);

				if(entry!=null) {
					schema.setValueRestriction(entry.getLabel(), valueRestriction);
				} else {
					schema.setValueRestriction(valueRestriction);
				}

				pattern = null;
				minLength = maxLength = ValueRestriction.UNDEFINED_VALUE;
			} break;

			case TAG_NAME_RESTRICTION: {
				ValueRestriction valueRestriction = ValueRestriction.forPattern(pattern, minLength, maxLength);
				schema.setNameRestriction(valueRestriction);

				pattern = null;
				minLength = maxLength = ValueRestriction.UNDEFINED_VALUE;
			} break;

			case TAG_PATTERN: {
				pattern = text();
			} break;

			case TAG_NAME: {
				entry.setName(maybeLocalize(text(), NAME_SUFFIX));
			} break;

			case TAG_DESCRIPTION: {
				entry.setDescription(maybeLocalize(text(), DESCRIPTION_SUFFIX));
			} break;

			default:
				throw new SAXException("Unexpected closing tag: "+qName);
			}
		}
	}
}

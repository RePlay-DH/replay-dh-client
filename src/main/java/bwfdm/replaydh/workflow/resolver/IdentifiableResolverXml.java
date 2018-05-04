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
package bwfdm.replaydh.workflow.resolver;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.io.resources.IOResource;
import bwfdm.replaydh.io.resources.IOWorker;
import bwfdm.replaydh.metadata.MetadataException;
import bwfdm.replaydh.utils.Lazy;
import bwfdm.replaydh.utils.annotation.XmlFacility;
import bwfdm.replaydh.utils.xml.HtmlUtils;
import bwfdm.replaydh.utils.xml.RDHXml;
import bwfdm.replaydh.utils.xml.XmlParserHandler;
import bwfdm.replaydh.workflow.Identifiable.Type;
import bwfdm.replaydh.workflow.resolver.LocalIdentifiableResolver.IdentifiableProxy;
import bwfdm.replaydh.workflow.resolver.LocalIdentifiableResolver.IdentifierProxy;
import bwfdm.replaydh.workflow.resolver.LocalIdentifiableResolver.IdentifierSet;

/**
 * @author Markus Gärtner
 *
 */
@XmlFacility
public class IdentifiableResolverXml {

	private static XMLOutputFactory xmlOutputFactory;
	private static SAXParserFactory parserFactory;

	private static final String NS = RDHXml.RDH_NS;

	private static final String TAG_CACHE = NS+':'+"cache";
	private static final String TAG_IDENTIFIABLE = NS+':'+"identifiable";
	private static final String TAG_IDENTIFIER = NS+':'+"identifier";
	private static final String TAG_ID = NS+':'+"id";
	private static final String TAG_CONTEXT = NS+':'+"context";

	private static final String ATTR_SYSTEM_ID = "systemId";
	private static final String ATTR_SCHEMA_ID = "schemaId";
	private static final String ATTR_TYPE = "type";
	private static final String ATTR_DATE = "date";

	private static final String NAMESPACE = "Identifiers";
	private static final String SCHEMA_NAME = "Identifiers.xsd";

	private static final String LINE_BREAK = System.lineSeparator();

	private static final Lazy<Schema> schema = RDHXml.createShareableSchemaSource(
			IdentifiableResolverXml.class.getResource(SCHEMA_NAME));

	public static Schema getSchema() {
		return schema.value();
	}

	public static void readIdentifiables(IOResource resource, BiConsumer<IdentifiableProxy, IdentifierSet> action)
			throws ParserConfigurationException, SAXException, IOException {
		try(ReadableByteChannel channel = resource.getReadChannel()) {
			try(Reader r = Channels.newReader(channel, StandardCharsets.UTF_8.newDecoder(), IOUtils.BUFFER_LENGTH)) {
				if(parserFactory==null) {
					SAXParserFactory factory = SAXParserFactory.newInstance();

					factory.setNamespaceAware(true);
					factory.setValidating(false);

					factory.setSchema(getSchema());

					parserFactory = factory;
				}


				SAXParser parser = parserFactory.newSAXParser();

				XMLReader reader = parser.getXMLReader();

				reader.setContentHandler(new ContentHandler(action));

				reader.parse(new InputSource(r));
			}
		}
	}
	private static void maybeWriteCData(XMLStreamWriter writer, String data) throws XMLStreamException {
		if(HtmlUtils.hasReservedXMLSymbols(data)) {
			writer.writeCData(data);
		} else {
			writer.writeCharacters(data);
		}
	}

	private static void writeElement(XMLStreamWriter writer, String tag, String content) throws XMLStreamException {
		if(content!=null && !content.isEmpty()) {
			writer.writeStartElement(tag);
			maybeWriteCData(writer, content);
			writer.writeEndElement();
		}
	}

	private static void writeIdentifier(XMLStreamWriter writer, IdentifierProxy identifierProxy) throws XMLStreamException {

		// Ignore invalid identifiers
		if(isValidIdentifier(identifierProxy)) {
			writer.writeStartElement(TAG_IDENTIFIER);

			// Header
			writer.writeAttribute(ATTR_SCHEMA_ID, identifierProxy.schemaId);
			writer.writeAttribute(ATTR_TYPE, identifierProxy.type);

			// ID
			writeElement(writer, TAG_ID, identifierProxy.id);

			// Context
			writeElement(writer, TAG_CONTEXT, identifierProxy.context);

			writer.writeEndElement();
			writer.writeCharacters(LINE_BREAK);
		}
	}

	/**
	 * An identifier is considered valid as long as the {@link IdentifierProxy#id id}
	 * field is not empty.
	 */
	private static boolean isValidIdentifier(IdentifierProxy identifierProxy) {
		return !identifierProxy.id.isEmpty();
	}

	/**
	 * A set of identifiers is considered valid if at least one individual identifier
	 * is {@link #isValidIdentifier(IdentifierProxy) valid}.
	 */
	private static boolean isValidIdentifierSet(IdentifierSet identifierSet) {

		for(IdentifierProxy identifierProxy : identifierSet) {
			if(isValidIdentifier(identifierProxy)) {
				return true;
			}
		}

		return false;
	}

	public static IOWorker<? super BiConsumer<IdentifiableProxy, IdentifierSet>> reader() {
		return (resource, action) -> {
			try {
				readIdentifiables(resource, action);
			} catch (ParserConfigurationException e) {
				throw new ExecutionException("Failed to configure parser", e);
			} catch (SAXException e) {
				throw new ExecutionException("Parsing XML resource failed", e);
			} catch (IOException e) {
				throw new ExecutionException("I/O error during read operation", e);
			}
		};
	}

	public static IOWorker<? super Collection<Entry<IdentifiableProxy, IdentifierSet>>> writer() {
		return (resource, item) -> {
			try {
				writeIdentifiables(resource, item);
			} catch (XMLStreamException e) {
				throw new ExecutionException("Streaming to XML failed", e);
			} catch (IOException e) {
				throw new ExecutionException("I/O error during write operation", e);
			}
		};
	}

	public static void writeIdentifiables(IOResource resource, Collection<Entry<IdentifiableProxy, IdentifierSet>> data)
			throws XMLStreamException, IOException {
		try(WritableByteChannel channel = resource.getWriteChannel(true)) {
			try(Writer w = Channels.newWriter(channel, StandardCharsets.UTF_8.newEncoder(), IOUtils.BUFFER_LENGTH)) {
				if(xmlOutputFactory==null) {
					xmlOutputFactory = XMLOutputFactory.newFactory();
				}

				XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(w);

				writer.writeStartDocument("utf-8", "1.0");
				writer.writeCharacters(LINE_BREAK);

				final boolean writeCacheTag = data.size()>1 || true; //FIXME for consistency we should always write the cache tag?

				// Write header part
				if(writeCacheTag) {
					writer.writeStartElement(TAG_CACHE);
					RDHXml.writeSchemaAttributes(writer, NAMESPACE, SCHEMA_NAME);
					writer.writeAttribute(ATTR_DATE, LocalDateTime.now().toString());
					writer.writeCharacters(LINE_BREAK);
				}

				for(Entry<IdentifiableProxy, IdentifierSet> entry : data) {
					IdentifiableProxy identifiableProxy = entry.getKey();
					IdentifierSet identifierSet = entry.getValue();

					// Only write identifiable data if the associated identifiers are valid
					if(isValidIdentifierSet(identifierSet)) {

						writer.writeStartElement(TAG_IDENTIFIABLE);
						writer.writeAttribute(ATTR_SYSTEM_ID, identifiableProxy.uuid.toString());
						writer.writeAttribute(ATTR_TYPE, identifiableProxy.type.getLabel());
						writer.writeCharacters(LINE_BREAK);

						for(IdentifierProxy identifierProxy : identifierSet) {
							writeIdentifier(writer, identifierProxy);
						}

						writer.writeEndElement();
						writer.writeCharacters(LINE_BREAK);
					}
				}

				if(writeCacheTag) {
					writer.writeEndElement();
				}

				writer.writeEndDocument();
			}
		}
	}

	private static class ContentHandler extends XmlParserHandler {

		// Helper fields for identifiers
		private String context;
		private String schemaId;
		private String id;
		private String type;

		// Helper fields for mapping
		private IdentifierSet identifierSet;
		private IdentifiableProxy identifiableProxy;

		private final BiConsumer<IdentifiableProxy, IdentifierSet> action;

		public ContentHandler(BiConsumer<IdentifiableProxy, IdentifierSet> action) {
			this.action = requireNonNull(action);
		}

		/**
		 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {

			switch (qName) {
			case TAG_IDENTIFIABLE: {
				UUID systemId = UUID.fromString(attributes.getValue(ATTR_SYSTEM_ID));
				Type type = Type.parseType(attributes.getValue(ATTR_TYPE));

				identifiableProxy = new IdentifiableProxy(systemId, type);
				identifierSet = new IdentifierSet();
			} break;

			case TAG_IDENTIFIER: {
				schemaId = attributes.getValue(ATTR_SCHEMA_ID);
				type = requireNonNull(attributes.getValue(ATTR_TYPE));
				context = null;
				id = null;
			} break;

			case TAG_ID: {
				clearText();
			} break;

			case TAG_CONTEXT: {
				clearText();
			} break;

			case TAG_CACHE:
				break;

			default:
				throw new MetadataException("Unexpected opening tag: "+qName);
			}
		}

		/**
		 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			switch (qName) {
			case TAG_IDENTIFIABLE: {
				action.accept(identifiableProxy, identifierSet);
				identifiableProxy = null;
				identifierSet = null;
			} break;

			case TAG_IDENTIFIER: {
				identifierSet.add(new IdentifierProxy(schemaId, type, id, context));
				schemaId = null;
				type = null;
				id = null;
				context = null;
			} break;

			case TAG_ID: {
				id = text();
			} break;

			case TAG_CONTEXT: {
				context = text();
			} break;

			case TAG_CACHE:
				break;

			default:
				throw new MetadataException("Unexpected closing tag: "+qName);
			}
		}
	}
}

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
package bwfdm.replaydh.metadata.basic.file;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

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
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRecord.Entry;
import bwfdm.replaydh.metadata.basic.MutableMetadataRecord;
import bwfdm.replaydh.utils.Lazy;
import bwfdm.replaydh.utils.LazyCollection;
import bwfdm.replaydh.utils.annotation.XmlFacility;
import bwfdm.replaydh.utils.xml.HtmlUtils;
import bwfdm.replaydh.utils.xml.RDHXml;
import bwfdm.replaydh.utils.xml.XmlParserHandler;

/**
 * @author Markus Gärtner
 *
 */
@XmlFacility
public class FileMetadataXml {

	private FileMetadataXml() {
		// prevent instantiation
	}

	private static final String NS = RDHXml.RDH_NS;

	private static final String TAG_RECORD = NS+':'+"record";
	private static final String TAG_ENTRY = NS+':'+"entry";

	private static final String ATTR_UID = "id";
	private static final String ATTR_NAME = "name";

	private static XMLOutputFactory xmlOutputFactory;
	private static SAXParserFactory parserFactory;

	private static final String LINE_BREAK = System.lineSeparator();

	private static final Comparator<Entry> SORT_LEX = (e1, e2) -> e1.getName().compareTo(e2.getName());

	private static final String NAMESPACE = "Record";
	private static final String SCHEMA_NAME = "Record.xsd";

	private static final Lazy<Schema> schema = RDHXml.createShareableSchemaSource(
			FileMetadataXml.class.getResource(SCHEMA_NAME));

	public static Schema getSchema() {
		return schema.value();
	}

	public static IOWorker<? super MutableMetadataRecord> reader() {
		return (resource, record) -> {
			try {
				readRecord(resource, record);
			} catch (ParserConfigurationException e) {
				throw new ExecutionException("Failed to configure parser", e);
			} catch (SAXException e) {
				throw new ExecutionException("Parsing XML resource failed", e);
			} catch (IOException e) {
				throw new ExecutionException("I/O error during read operation", e);
			}
		};
	}

	public static IOWorker<? super MetadataRecord> writer() {
		return (resource, record) -> {
			try {
				writeRecord(resource, record);
			} catch (XMLStreamException e) {
				throw new ExecutionException("Streaming to XML failed", e);
			} catch (IOException e) {
				throw new ExecutionException("I/O error during write operation", e);
			}
		};
	}

	public static void readRecord(IOResource resource, MutableMetadataRecord record)
			throws IOException, SAXException, ParserConfigurationException {
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

				reader.setContentHandler(new ContentHandler(record));

				reader.parse(new InputSource(r));
			}
		}
	}

	public static void writeRecord(IOResource resource, MetadataRecord record) throws XMLStreamException, IOException {
		try(WritableByteChannel channel = resource.getWriteChannel(true)) {
			try(Writer w = Channels.newWriter(channel, StandardCharsets.UTF_8.newEncoder(), IOUtils.BUFFER_LENGTH)) {
				if(xmlOutputFactory==null) {
					xmlOutputFactory = XMLOutputFactory.newFactory();
				}

				XMLStreamWriter writer = xmlOutputFactory.createXMLStreamWriter(w);

				writer.writeStartDocument("utf-8", "1.0");
				writer.writeCharacters(LINE_BREAK);

				// Write header part
				writer.writeStartElement(TAG_RECORD);
				RDHXml.writeSchemaAttributes(writer, NAMESPACE, SCHEMA_NAME);
				writer.writeAttribute(ATTR_UID, record.getUID().toString());
				writer.writeCharacters(LINE_BREAK);

				// Write all the entries in alphabetical order

				LazyCollection<Entry> tmp = LazyCollection.lazyList(record.getEntryCount());
				record.forEachEntry(tmp);

				List<Entry> entries = tmp.getAsList();

				entries.sort(SORT_LEX);

				for(Entry entry : entries) {
					writer.writeStartElement(TAG_ENTRY);
					writer.writeAttribute(ATTR_NAME, entry.getName());
					if(HtmlUtils.hasReservedXMLSymbols(entry.getValue())) {
						writer.writeCData(entry.getValue());
					} else {
						writer.writeCharacters(entry.getValue());
					}
					writer.writeEndElement();
					writer.writeCharacters(LINE_BREAK);
				}

				writer.writeEndElement();
				writer.writeEndDocument();
			}
		}
	}

	private static class ContentHandler extends XmlParserHandler {
		private final MutableMetadataRecord record;

		private String name;

		public ContentHandler(MutableMetadataRecord record) {
			this.record = requireNonNull(record);
		}

		/**
		 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {

			switch (qName) {
			case TAG_RECORD: {
				String uid = requireNonNull(attributes.getValue(ATTR_UID));
				String expectedUid = requireNonNull(record.getUID().toString());
				if(!expectedUid.equals(uid))
					throw new MetadataException(String.format(
							"Record corrupted: expected UID '%s' - got '%s'", expectedUid, uid));
			} break;

			case TAG_ENTRY: {
				name = requireNonNull(attributes.getValue(ATTR_NAME));
				clearText();
			} break;

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
			case TAG_RECORD:
				break;

			case TAG_ENTRY: {
				// Construct and add new name-value entry
				String value = text();
				record.addEntry(name, value);

				// Make sure we clear our name mapping when leaving scope
				name = null;
			} break;

			default:
				throw new MetadataException("Unexpected closing tag: "+qName);
			}
		}
	}
}

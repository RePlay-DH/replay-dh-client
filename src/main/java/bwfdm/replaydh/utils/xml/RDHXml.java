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
package bwfdm.replaydh.utils.xml;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.utils.Lazy;

/**
 * @author Markus Gärtner
 *
 */
public class RDHXml {

	/**
	 * The global namespace used for all xml schemas native to
	 * the RePlay-DH Client
	 */
	public static final String RDH_NS = "rdh";

	public static final String RDH_NS_URI_BASE = "https://www.ub.uni-stuttgart.de/replay/";
	public static final String XSI_NS_URI = XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;

	public static final String RDH_SCHEMA_LOC_BASE = "https://www.ub.uni-stuttgart.de/replay/";

	public static void writeSchemaAttributes(XMLStreamWriter writer, String namespace, String schemaName) throws XMLStreamException {
		writer.writeAttribute("xmlns:xsi", XSI_NS_URI);
		writer.writeAttribute("xmlns:rdh", RDH_NS_URI_BASE+namespace);
		writer.writeAttribute("xsi:schemaLocation", RDH_SCHEMA_LOC_BASE+namespace+" "+schemaName); // following rule for even number of URIs in schema lcoation
	}

	/**
	 * Maps schema locations of the form {namespace} {location} to actual
	 * physical URL locations.
	 */
	private static final Map<String, URL> schemaLocations = new HashMap<>();

	static {
		registerSchemaLocation("https://www.ub.uni-stuttgart.de/replay/CommonTypes CommonTypes.xsd",
				RDHXml.class.getResource("CommonTypes.xsd"));
	}

	public static void registerSchemaLocation(String schemaLocation, URL url) {
		requireNonNull(schemaLocation);
		requireNonNull(url);

		synchronized (schemaLocations) {
			URL mappedValue = schemaLocations.get(schemaLocation);
			if(mappedValue!=null && url!=mappedValue
					&& !url.toExternalForm().equals(mappedValue.toExternalForm()))
				throw new IllegalArgumentException(String.format(
						"Unable to map schema location '%s' to url '%s' - already mapped to url '%s'",
						schemaLocation, url, mappedValue));

			schemaLocations.put(schemaLocation, url);
		}
	}

	public static URL getSchemaLocation(String schemaLocation) {
		requireNonNull(schemaLocation);

		synchronized (schemaLocations) {
			return schemaLocations.get(schemaLocation);
		}
	}

	private static final LSResourceResolver schemaResolver = new LSResourceResolver() {

		@Override
		public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {

			if(XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(type)) {
				URL location = getSchemaLocation(systemId);

				if(location!=null) {
					try {
						return new Input(publicId, systemId, baseURI, location.openStream(), StandardCharsets.UTF_8.name());
					} catch (IOException e) {
						throw new RDHException("Unexpected I/O error while accessing linked schema file: "+location, e);
					}
				}
			}

			// If we cannot resolve schema file, default to what the basic resolver does
			return null;
		}
	};

	private static Schema createSchema(URL location) {
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		factory.setResourceResolver(schemaResolver);

		Schema schema;
		try {
			schema = factory.newSchema(location);
		} catch (SAXException e) {
			throw new RDHException("Failed to create XML schema from location: "+location, e);
		}
		return schema;
	}

	public static Lazy<Schema> createShareableSchemaSource(URL location) {
		checkArgument("Schema location must not be null", location!=null);

		Lazy<Schema> lazy = Lazy.create(() -> createSchema(location), true);

		return lazy;
	}
}

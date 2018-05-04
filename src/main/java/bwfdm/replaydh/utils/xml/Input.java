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

import java.io.InputStream;
import java.io.Reader;

import org.w3c.dom.ls.LSInput;

/**
 * Clone of the {@code com.sun.org.apache.xerces.internal.dom.DOMInputImpl}
 * implementation.
 *
 * @author Markus Gärtner
 *
 */
public class Input implements LSInput {

	//
	// Data
	//

	protected String fPublicId = null;
	protected String fSystemId = null;
	protected String fBaseSystemId = null;

	protected InputStream fByteStream = null;
	protected Reader fCharStream = null;
	protected String fData = null;

	protected String fEncoding = null;

	protected boolean fCertifiedText = false;

	/**
	 * Default Constructor, constructs an input source
	 *
	 *
	 */
	public Input() {
	}

	/**
	 * Constructs an input source from just the public and system identifiers,
	 * leaving resolution of the entity and opening of the input stream up to the
	 * caller.
	 *
	 * @param publicId
	 *            The public identifier, if known.
	 * @param systemId
	 *            The system identifier. This value should always be set, if
	 *            possible, and can be relative or absolute. If the system
	 *            identifier is relative, then the base system identifier should be
	 *            set.
	 * @param baseSystemId
	 *            The base system identifier. This value should always be set to the
	 *            fully expanded URI of the base system identifier, if possible.
	 */

	public Input(String publicId, String systemId, String baseSystemId) {

		fPublicId = publicId;
		fSystemId = systemId;
		fBaseSystemId = baseSystemId;

	} // DOMInputImpl(String,String,String)

	/**
	 * Constructs an input source from a byte stream.
	 *
	 * @param publicId
	 *            The public identifier, if known.
	 * @param systemId
	 *            The system identifier. This value should always be set, if
	 *            possible, and can be relative or absolute. If the system
	 *            identifier is relative, then the base system identifier should be
	 *            set.
	 * @param baseSystemId
	 *            The base system identifier. This value should always be set to the
	 *            fully expanded URI of the base system identifier, if possible.
	 * @param byteStream
	 *            The byte stream.
	 * @param encoding
	 *            The encoding of the byte stream, if known.
	 */

	public Input(String publicId, String systemId, String baseSystemId, InputStream byteStream, String encoding) {

		fPublicId = publicId;
		fSystemId = systemId;
		fBaseSystemId = baseSystemId;
		fByteStream = byteStream;
		fEncoding = encoding;

	} // DOMInputImpl(String,String,String,InputStream,String)

	/**
	 * Constructs an input source from a character stream.
	 *
	 * @param publicId
	 *            The public identifier, if known.
	 * @param systemId
	 *            The system identifier. This value should always be set, if
	 *            possible, and can be relative or absolute. If the system
	 *            identifier is relative, then the base system identifier should be
	 *            set.
	 * @param baseSystemId
	 *            The base system identifier. This value should always be set to the
	 *            fully expanded URI of the base system identifier, if possible.
	 * @param charStream
	 *            The character stream.
	 * @param encoding
	 *            The original encoding of the byte stream used by the reader, if
	 *            known.
	 */

	public Input(String publicId, String systemId, String baseSystemId, Reader charStream, String encoding) {

		fPublicId = publicId;
		fSystemId = systemId;
		fBaseSystemId = baseSystemId;
		fCharStream = charStream;
		fEncoding = encoding;

	} // DOMInputImpl(String,String,String,Reader,String)

	/**
	 * Constructs an input source from a String.
	 *
	 * @param publicId
	 *            The public identifier, if known.
	 * @param systemId
	 *            The system identifier. This value should always be set, if
	 *            possible, and can be relative or absolute. If the system
	 *            identifier is relative, then the base system identifier should be
	 *            set.
	 * @param baseSystemId
	 *            The base system identifier. This value should always be set to the
	 *            fully expanded URI of the base system identifier, if possible.
	 * @param data
	 *            The String Data.
	 * @param encoding
	 *            The original encoding of the byte stream used by the reader, if
	 *            known.
	 */

	public Input(String publicId, String systemId, String baseSystemId, String data, String encoding) {
		fPublicId = publicId;
		fSystemId = systemId;
		fBaseSystemId = baseSystemId;
		fData = data;
		fEncoding = encoding;
	} // DOMInputImpl(String,String,String,String,String)

	/**
	 * An attribute of a language-binding dependent type that represents a stream of
	 * bytes. <br>
	 * The parser will ignore this if there is also a character stream specified,
	 * but it will use a byte stream in preference to opening a URI connection
	 * itself. <br>
	 * If the application knows the character encoding of the byte stream, it should
	 * set the encoding property. Setting the encoding in this way will override any
	 * encoding specified in the XML declaration itself.
	 */

	@Override
	public InputStream getByteStream() {
		return fByteStream;
	}

	/**
	 * An attribute of a language-binding dependent type that represents a stream of
	 * bytes. <br>
	 * The parser will ignore this if there is also a character stream specified,
	 * but it will use a byte stream in preference to opening a URI connection
	 * itself. <br>
	 * If the application knows the character encoding of the byte stream, it should
	 * set the encoding property. Setting the encoding in this way will override any
	 * encoding specified in the XML declaration itself.
	 */

	@Override
	public void setByteStream(InputStream byteStream) {
		fByteStream = byteStream;
	}

	/**
	 * An attribute of a language-binding dependent type that represents a stream of
	 * 16-bit units. Application must encode the stream using UTF-16 (defined in and
	 * Amendment 1 of ). <br>
	 * If a character stream is specified, the parser will ignore any byte stream
	 * and will not attempt to open a URI connection to the system identifier.
	 */
	@Override
	public Reader getCharacterStream() {
		return fCharStream;
	}

	/**
	 * An attribute of a language-binding dependent type that represents a stream of
	 * 16-bit units. Application must encode the stream using UTF-16 (defined in and
	 * Amendment 1 of ). <br>
	 * If a character stream is specified, the parser will ignore any byte stream
	 * and will not attempt to open a URI connection to the system identifier.
	 */

	@Override
	public void setCharacterStream(Reader characterStream) {
		fCharStream = characterStream;
	}

	/**
	 * A string attribute that represents a sequence of 16 bit units (utf-16 encoded
	 * characters). <br>
	 * If string data is available in the input source, the parser will ignore the
	 * character stream and the byte stream and will not attempt to open a URI
	 * connection to the system identifier.
	 */
	@Override
	public String getStringData() {
		return fData;
	}

	/**
	 * A string attribute that represents a sequence of 16 bit units (utf-16 encoded
	 * characters). <br>
	 * If string data is available in the input source, the parser will ignore the
	 * character stream and the byte stream and will not attempt to open a URI
	 * connection to the system identifier.
	 */

	@Override
	public void setStringData(String stringData) {
		fData = stringData;
	}

	/**
	 * The character encoding, if known. The encoding must be a string acceptable
	 * for an XML encoding declaration ( section 4.3.3 "Character Encoding in
	 * Entities"). <br>
	 * This attribute has no effect when the application provides a character
	 * stream. For other sources of input, an encoding specified by means of this
	 * attribute will override any encoding specified in the XML claration or the
	 * Text Declaration, or an encoding obtained from a higher level protocol, such
	 * as HTTP .
	 */

	@Override
	public String getEncoding() {
		return fEncoding;
	}

	/**
	 * The character encoding, if known. The encoding must be a string acceptable
	 * for an XML encoding declaration ( section 4.3.3 "Character Encoding in
	 * Entities"). <br>
	 * This attribute has no effect when the application provides a character
	 * stream. For other sources of input, an encoding specified by means of this
	 * attribute will override any encoding specified in the XML claration or the
	 * Text Declaration, or an encoding obtained from a higher level protocol, such
	 * as HTTP .
	 */
	@Override
	public void setEncoding(String encoding) {
		fEncoding = encoding;
	}

	/**
	 * The public identifier for this input source. The public identifier is always
	 * optional: if the application writer includes one, it will be provided as part
	 * of the location information.
	 */
	@Override
	public String getPublicId() {
		return fPublicId;
	}

	/**
	 * The public identifier for this input source. The public identifier is always
	 * optional: if the application writer includes one, it will be provided as part
	 * of the location information.
	 */
	@Override
	public void setPublicId(String publicId) {
		fPublicId = publicId;
	}

	/**
	 * The system identifier, a URI reference , for this input source. The system
	 * identifier is optional if there is a byte stream or a character stream, but
	 * it is still useful to provide one, since the application can use it to
	 * resolve relative URIs and can include it in error messages and warnings (the
	 * parser will attempt to fetch the ressource identifier by the URI reference
	 * only if there is no byte stream or character stream specified). <br>
	 * If the application knows the character encoding of the object pointed to by
	 * the system identifier, it can register the encoding by setting the encoding
	 * attribute. <br>
	 * If the system ID is a relative URI reference (see section 5 in ), the
	 * behavior is implementation dependent.
	 */
	@Override
	public String getSystemId() {
		return fSystemId;
	}

	/**
	 * The system identifier, a URI reference , for this input source. The system
	 * identifier is optional if there is a byte stream or a character stream, but
	 * it is still useful to provide one, since the application can use it to
	 * resolve relative URIs and can include it in error messages and warnings (the
	 * parser will attempt to fetch the ressource identifier by the URI reference
	 * only if there is no byte stream or character stream specified). <br>
	 * If the application knows the character encoding of the object pointed to by
	 * the system identifier, it can register the encoding by setting the encoding
	 * attribute. <br>
	 * If the system ID is a relative URI reference (see section 5 in ), the
	 * behavior is implementation dependent.
	 */
	@Override
	public void setSystemId(String systemId) {
		fSystemId = systemId;
	}

	/**
	 * The base URI to be used (see section 5.1.4 in ) for resolving relative URIs
	 * to absolute URIs. If the baseURI is itself a relative URI, the behavior is
	 * implementation dependent.
	 */
	@Override
	public String getBaseURI() {
		return fBaseSystemId;
	}

	/**
	 * The base URI to be used (see section 5.1.4 in ) for resolving relative URIs
	 * to absolute URIs. If the baseURI is itself a relative URI, the behavior is
	 * implementation dependent.
	 */
	@Override
	public void setBaseURI(String baseURI) {
		fBaseSystemId = baseURI;
	}

	/**
	 * If set to true, assume that the input is certified (see section 2.13 in
	 * [<a href='http://www.w3.org/TR/2002/CR-xml11-20021015/'>XML 1.1</a>]) when
	 * parsing [<a href='http://www.w3.org/TR/2002/CR-xml11-20021015/'>XML 1.1</a>].
	 */
	@Override
	public boolean getCertifiedText() {
		return fCertifiedText;
	}

	/**
	 * If set to true, assume that the input is certified (see section 2.13 in
	 * [<a href='http://www.w3.org/TR/2002/CR-xml11-20021015/'>XML 1.1</a>]) when
	 * parsing [<a href='http://www.w3.org/TR/2002/CR-xml11-20021015/'>XML 1.1</a>].
	 */

	@Override
	public void setCertifiedText(boolean certifiedText) {
		fCertifiedText = certifiedText;
	}
}

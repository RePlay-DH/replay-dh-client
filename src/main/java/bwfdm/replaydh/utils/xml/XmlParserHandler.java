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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Markus Gärtner
 *
 */
public class XmlParserHandler extends DefaultHandler {

	private final StringBuilder buffer = new StringBuilder(50);

	/**
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		buffer.append(ch, start, length);
	}

	protected void clearText() {
		buffer.setLength(0);
	}

	protected String text() {
		// Trim front
		int start = 0;
		while(Character.isWhitespace(buffer.charAt(start))) start++;

		// Trim tail
		int end = buffer.length();
		while(Character.isWhitespace(buffer.charAt(end-1))) end--;

		// Grab remaining text content and clear buffer
		String text = buffer.substring(start, end);
		clearText();

		// Make sure we can't get an empty text back (will fail null check in surrounding code)
		if(text.isEmpty()) {
			text = null;
		}
		return text;
	}

	protected static String normalize(String s) {
		return (s==null || s.isEmpty()) ? null : s;
	}

	protected static boolean flag(Attributes attr, String key, boolean defaultValue) {
		String s = normalize(attr.getValue(key));
		if(s==null) {
			return defaultValue;
		} else {
			return Boolean.parseBoolean(s);
		}
	}
}

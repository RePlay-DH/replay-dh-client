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
package bwfdm.replaydh.utils;

/**
 * @author Markus Gärtner
 *
 */
public interface Label extends StringResource, Comparable<Label> {


	/**
	 * Delegates to {@link #getLabel()}.
	 *
	 * @see bwfdm.replaydh.utils.StringResource#getStringValue()
	 */
	@Override
	default java.lang.String getStringValue() {
		return getLabel();
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	default int compareTo(Label other) {
		return getName().compareTo(other.getName());
	}

	/**
	 * Returns the basic loca-independent String representation of
	 * this label.
	 * @return
	 */
	String getLabel();

	/**
	 * Returns a localized String representation of this label
	 * that is suitable to be used as a display string.
	 * @return
	 */
	String getName();

	/**
	 * Returns a localized String representation of a more
	 * explanatory text that describes the purpose or meaning
	 * of this label.
	 *
	 * @return
	 */
	String getDescription();
}

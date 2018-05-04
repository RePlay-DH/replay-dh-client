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

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the multiplicity of allowed (external) elements in a certain context.
 *
 * @author Markus Gärtner
 *
 */
public enum Multiplicity implements StringResource {

	/**
	 * Defines an "empty" docking point for external entities
	 */
	NONE("none", 0, 0),

	/**
	 * Docking point for at most one external entity
	 */
	NONE_OR_ONE("none-or-one", 0, 1),

	/**
	 * Requires exactly one external entity to be docked
	 */
	ONE("one", 1, 1),

	/**
	 * Requires at least one external entity to be docked but
	 * poses no upper limit.
	 */
	ONE_OR_MORE("one-or-more", 1, -1),

	/**
	 * Unrestricted docking point
	 */
	ANY("any", 0, -1),
	;

	private static final int UNRESTRICTED = -1;

	private final String xmlForm;
	private final int min, max;

	private Multiplicity(String xmlForm, int min, int max) {
		this.xmlForm = xmlForm;
		this.min = min;
		this.max = max;
	}

	/**
	 * @see java.lang.Enum#toString()
	 */
	@Override
	public String toString() {
		return name().toLowerCase();
	}

	/**
	 * @see de.ims.icarus2.util.strings.StringResource#getStringValue()
	 */
	@Override
	public String getStringValue() {
		return xmlForm;
	}

	public boolean isLegalCount(int value) {
		if(value<0) {
			return false;
		}

		if(min!=UNRESTRICTED && value<min) {
			return false;
		}

		if(max!=UNRESTRICTED && value>max) {
			return false;
		}

		return true;
	}

	public int getRequiredMinimum() {
		return min;
	}

	public int getAllowedMaximum() {
		return max;
	}

	private static Map<String, Multiplicity> xmlLookup;

	public static Multiplicity parseMultiplicity(String s) {
		if(xmlLookup==null) {
			Map<String, Multiplicity> map = new HashMap<>();
			for(Multiplicity type : values()) {
				map.put(type.xmlForm, type);
			}
			// Ignore thread-safety, since the outcome will be the same anyway
			xmlLookup = map;
		}

		return xmlLookup.get(s);
	}
}

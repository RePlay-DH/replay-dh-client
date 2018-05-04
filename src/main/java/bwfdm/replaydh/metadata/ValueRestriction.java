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
package bwfdm.replaydh.metadata;

import static java.util.Objects.requireNonNull;

import bwfdm.replaydh.metadata.MetadataRecord.Entry;

/**
 * Models a general limitation for String values that can be used
 * for either {@link Entry#getName() name} or {@link Entry#getValue() value}
 * of a metadata record's {@link Entry entry}.
 *
 * Note that this interface is not designed to model static collections of
 * legal String values, but merely general rules like limitations in terms
 * of character count or a regular expression pattern for allowed symbols.
 *
 * @author Markus Gärtner
 *
 */
public interface ValueRestriction {

	public static final int UNDEFINED_VALUE = -1;

	int getMinimumLength();

	int getMaximumLength();

	String getPattern();

	public static void checkIsValid(ValueRestriction restriction, String input) {
		requireNonNull(input);

		// Prevent unnecessary checks when we only deal with our empty restriction
		if(restriction==EMPTY_RESTRICTION) {
			return;
		}

		int minLength = restriction.getMinimumLength();
		if(minLength>UNDEFINED_VALUE && input.length()<minLength)
			throw new MetadataException("Value too short (min "+minLength+" characters): "+input);

		int maxLength = restriction.getMinimumLength();
		if(maxLength>UNDEFINED_VALUE && input.length()>maxLength)
			throw new MetadataException("Value too long (max "+maxLength+" characters): "+input);

		String pattern = restriction.getPattern();
		if(pattern!=null && !input.matches(pattern))
			throw new MetadataException("Value does not match reges pattern '"+pattern+"': "+input);
	}

	default boolean isValid(String s) {
		requireNonNull(s);

		int minLength = getMinimumLength();
		if(minLength>UNDEFINED_VALUE && s.length()<minLength) {
			return false;
		}

		int maxLength = getMaximumLength();
		if(maxLength>UNDEFINED_VALUE && s.length()>maxLength) {
			return false;
		}

		String pattern = getPattern();
		if(pattern!=null && !s.matches(pattern)) {
			return false;
		}

		return true;
	}

	/**
	 * A restriction that does not impose any limits on input Strings.
	 */
	public static final ValueRestriction EMPTY_RESTRICTION = new ValueRestriction() {

		@Override
		public String getPattern() {
			return null;
		}

		@Override
		public int getMinimumLength() {
			return UNDEFINED_VALUE;
		}

		@Override
		public int getMaximumLength() {
			return UNDEFINED_VALUE;
		}

		@Override
		public boolean isValid(String s) {
			return s!=null;
		}
	};

	public static ValueRestriction forPattern(String pattern) {
		return new StaticValueRestriction(UNDEFINED_VALUE, UNDEFINED_VALUE, pattern);
	}

	public static ValueRestriction forPattern(String pattern, int minLength) {
		return new StaticValueRestriction(minLength, UNDEFINED_VALUE, pattern);
	}

	public static ValueRestriction forPattern(String pattern, int minLength, int maxLength) {
		return new StaticValueRestriction(minLength, maxLength, pattern);
	}

	/**
	 * @author Markus Gärtner
	 *
	 */
	static class StaticValueRestriction implements ValueRestriction {

		private final int minLength, maxLength;
		private final String pattern;

		private StaticValueRestriction(int minLength, int maxLength, String pattern) {
			this.minLength = minLength;
			this.maxLength = maxLength;
			this.pattern = pattern;
		}

		/**
		 * @see bwfdm.replaydh.metadata.ValueRestriction#getMinimumLength()
		 */
		@Override
		public int getMinimumLength() {
			return minLength;
		}

		/**
		 * @see bwfdm.replaydh.metadata.ValueRestriction#getMaximumLength()
		 */
		@Override
		public int getMaximumLength() {
			return maxLength;
		}

		/**
		 * @see bwfdm.replaydh.metadata.ValueRestriction#getPattern()
		 */
		@Override
		public String getPattern() {
			return pattern;
		}
	}
}

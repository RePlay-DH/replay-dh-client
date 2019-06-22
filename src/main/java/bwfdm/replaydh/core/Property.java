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
package bwfdm.replaydh.core;

import static java.util.Objects.requireNonNull;

/**
 * Generic interface for objects that can be used to query the
 * {@link RDHEnvironment} for settings and that can provide associated
 * default values.
 *
 * @author Markus Gärtner
 *
 */
public interface Property {

	String getKey();

	<T extends Object> T getDefaultValue();

	public static final class StaticProperty implements Property {
		private final Object defaultValue;
		private final String key;

		public StaticProperty(String key, Object defaultValue) {
			this.key = requireNonNull(key);
			this.defaultValue = defaultValue;
		}

		public StaticProperty(String key) {
			this.key = requireNonNull(key);
			this.defaultValue = null;
		}

		/**
		 * @see bwfdm.replaydh.core.Property#getKey()
		 */
		@Override
		public String getKey() {
			return key;
		}
		/**
		 * @see bwfdm.replaydh.core.Property#getDefaultValue()
		 */
		@SuppressWarnings("unchecked")
		@Override
		public <T> T getDefaultValue() {
			return (T) defaultValue;
		}
	}
}

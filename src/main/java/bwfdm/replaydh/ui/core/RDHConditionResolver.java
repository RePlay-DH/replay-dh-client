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
package bwfdm.replaydh.ui.core;

import static java.util.Objects.requireNonNull;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.ui.actions.ConditionResolver;
import bwfdm.replaydh.utils.StringResource;

/**
 * @author Markus Gärtner
 *
 */
public class RDHConditionResolver implements ConditionResolver {

	public static enum Global implements StringResource {
		DEBUG("debug"),
		;

		private final String key;

		private Global(String key) {
			this.key = requireNonNull(key);
		}

		/**
		 * @return the key
		 */
		public String getKey() {
			return key;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return ConditionResolver.join(GLOBAL_PREFIX, key);
		}

		/**
		 * @see bwfdm.replaydh.utils.StringResource#getStringValue()
		 */
		@Override
		public String getStringValue() {
			return key;
		}

		public static Global parseGlobal(String s) {
			switch (s.toLowerCase()) {
			case "debug": return DEBUG;

			default:
				throw new IllegalArgumentException("Unknown global: "+s);
			}
		}
	}

	private final RDHEnvironment environment;

	public RDHConditionResolver(RDHEnvironment environment) {
		this.environment = requireNonNull(environment);
	}

	/**
	 * @see bwfdm.replaydh.ui.actions.ConditionResolver#resolveGlobal(java.lang.String)
	 */
	@Override
	public boolean resolveGlobal(String key) {
		switch (Global.parseGlobal(key)) {
		case DEBUG: return environment.getClient().isDevMode();

		default:
			throw new IllegalArgumentException("Unsupported global key: "+key);
		}
	}

	/**
	 * @see bwfdm.replaydh.ui.actions.ConditionResolver#resolve(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean resolve(String namespace, String key) {
		if(SETTINGS_PREFIX.equals(namespace)) {
			return ConditionResolver.defaultResolveValue(environment.getProperty(key));
		} else
			throw new IllegalArgumentException("Unsupported namespace: "+namespace);
	}

}

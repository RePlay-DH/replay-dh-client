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
package bwfdm.replaydh.ui.actions;

import java.util.Map;
import java.util.function.BooleanSupplier;

import bwfdm.replaydh.utils.StringResource;
import bwfdm.replaydh.utils.Wrapper;

/**
 * @author Markus Gärtner
 *
 */
public interface ConditionResolver {

	public static final String GLOBAL_PREFIX = "global";

	public static final String SETTINGS_PREFIX = "settings";

	/**
	 * Resolve a global property identified by {@code key}.
	 * How non-boolean properties are treated is completely
	 * implementation specific.
	 *
	 * @param key
	 * @return
	 */
	boolean resolveGlobal(String key);

	/**
	 * Resolve a non-global property for a specific {@code namespace}.
	 *
	 * @param namespace
	 * @param key
	 * @return
	 */
	boolean resolve(String namespace, String key);

	/**
	 * Hook to intercept the resolution of a local property provided
	 * to the building process. The default implementation simply looks
	 * up the current value in {@code properties} mapped to {@code key}
	 * and then delegates to {@link #defaultResolveValue(Object)}.
	 *
	 * @param key
	 * @param properties
	 * @return
	 */
	default boolean resolve(String key, Map<String, Object> properties) {
		return defaultResolveValue(properties.get(key));
	}

	/**
	 * Provides a default strategy of resolving arbitrary condition values:
	 *
	 * <table border="1">
	 * <tr><th>Value</th><th>Result</th></tr>
	 * <tr><td>null</td><td>{@code false}</td></tr>
	 * <tr><td>{@link Boolean}</td><td>Invoke {@link Boolean#booleanValue()}</td></tr>
	 * <tr><td>{@link String}</td><td>{@link Boolean#parseBoolean(String) parse} value to boolean, or {@code false} in case of error</td></tr>
	 * <tr><td>{@link Number}</td><td>{@link Number#doubleValue()} &gt; {@code 0}</td></tr>
	 * <tr><td>{@link BooleanSupplier}</td><td>Invoke {@link BooleanSupplier#getAsBoolean()}</td></tr>
	 * <tr><td>any other object</td><td>{@code true}</td></tr>
	 * </table>
	 *
	 * In addition, if {@code value} is a {@link StringResource} it will first
	 * be unpacked. Same goes for {@link Wrapper} values.
	 *
	 * @param value
	 * @return
	 */
	public static boolean defaultResolveValue(Object value) {
		if(value==null) {
			return false;
		}

		if(value instanceof Wrapper) {
			value = ((Wrapper<?>)value).get();
		}

		if(value.getClass()==Boolean.class) {
			return ((Boolean)value).booleanValue();
		} else if(value.getClass()==String.class) {
			return Boolean.parseBoolean((String)value);
		} else if(value.getClass()==Number.class) {
			return Double.compare(((Number)value).doubleValue(), 0.0) > 0;
		} else if(value instanceof BooleanSupplier) {
			return ((BooleanSupplier)value).getAsBoolean();
		} else {
			return true;
		}
	}

	public static String extractNamespace(String value) {
		int sep = value.indexOf(':');
		return sep==-1 ? null : value.substring(0, sep);
	}

	public static String extractValue(String value) {
		int sep = value.indexOf(':');
		return sep==-1 ? value : value.substring(sep+1);
	}

	public static String join(String namespace, String value) {
		return namespace+':'+value;
	}

	public static boolean resolve(String condition, ConditionResolver resolver, Map<String, Object> properties) {
		if(condition==null) {
			return true;
		}

		final String namespace = extractNamespace(condition);
		final String value = extractValue(condition);

		if(GLOBAL_PREFIX.equals(namespace)) {
			return resolver.resolveGlobal(value);
		} else if(namespace!=null) {
			return resolver.resolve(namespace, value);
		} else {
			return resolver.resolve(value, properties);
		}
	}

	/**
	 * Shared instance that returns {@code false} for all requests at resolving
	 * conditions {@link #resolveGlobal(String) globally} or
	 * {@link #resolve(String, String) locally}.
	 */
	public static final ConditionResolver EMPTY_RESOLVER = new ConditionResolver() {

		@Override
		public boolean resolveGlobal(String key) {
			return false;
		}

		@Override
		public boolean resolve(String namespace, String key) {
			return false;
		}

		@Override
		public boolean resolve(String key, Map<String, Object> properties) {
			return false;
		}
	};
}

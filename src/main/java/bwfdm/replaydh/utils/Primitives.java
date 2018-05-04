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

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * @author Markus Gärtner
 *
 */
public class Primitives {

	private static final Map<Class<?>, Class<?>> primitiveWrapperLookup = new IdentityHashMap<>(9);
	private static final Map<Class<?>, Class<?>> primitiveWrappers = new IdentityHashMap<>(9);

	static {
		primitiveWrapperLookup.put(Boolean.class, boolean.class);
		primitiveWrapperLookup.put(Character.class, char.class);
		primitiveWrapperLookup.put(Byte.class, byte.class);
		primitiveWrapperLookup.put(Short.class, short.class);
		primitiveWrapperLookup.put(Integer.class, int.class);
		primitiveWrapperLookup.put(Long.class, long.class);
		primitiveWrapperLookup.put(Float.class, float.class);
		primitiveWrapperLookup.put(Double.class, double.class);
		primitiveWrapperLookup.put(Void.class, void.class);
	}

	static {
		primitiveWrappers.put(boolean.class, Boolean.class);
		primitiveWrappers.put(char.class, Character.class);
		primitiveWrappers.put(byte.class, Byte.class);
		primitiveWrappers.put(short.class, Short.class);
		primitiveWrappers.put(int.class, Integer.class);
		primitiveWrappers.put(long.class, Long.class);
		primitiveWrappers.put(float.class, Float.class);
		primitiveWrappers.put(double.class, Double.class);
		primitiveWrappers.put(void.class, Void.class);
	}

	/**
	 * Unwraps wrapper types to their primitive type definition.
	 *
	 * @param clazz
	 * @return
	 */
	public static Class<?> unwrap(Class<?> clazz) {
		Class<?> primitiveClass = primitiveWrapperLookup.get(clazz);
		return primitiveClass==null ? clazz : primitiveClass;
	}

	/**
	 * Returns the wrapper type for a given class if it is a primitive type.
	 *
	 * @param clazz
	 * @return
	 */
	public static Class<?> wrap(Class<?> clazz) {
		return clazz.isPrimitive() ? primitiveWrappers.get(clazz) : clazz;
	}

	/**
	 * Returns whether or not the given class is one of the wrapper classes for
	 * primitives like {@link Integer}, etc...
	 *
	 * @param clazz
	 * @return
	 */
	public static <T extends Object> boolean isPrimitiveWrapperClass(Class<T> clazz) {
		return clazz==Long.class || clazz==Integer.class
				|| clazz==Short.class || clazz==Byte.class
				|| clazz==Float.class || clazz==Double.class
				|| clazz==Void.class || clazz==Character.class
				|| clazz==Boolean.class;
	}

	public static int cast(Integer value) {
		return value==null ? 0 : value.intValue();
	}

	public static long cast(Long value) {
		return value==null ? 0L : value.intValue();
	}

	public static double cast(Double value) {
		return value==null ? 0D : value.doubleValue();
	}

	public static float cast(Float value) {
		return value==null ? 0F : value.floatValue();
	}

	public static short cast(Short value) {
		return value==null ? 0 : value.shortValue();
	}

	public static byte cast(Byte value) {
		return value==null ? 0 : value.byteValue();
	}

	public static boolean cast(Boolean value) {
		return value==null ? false : value.booleanValue();
	}

	public static Integer _int(int value) {
		return Integer.valueOf(value);
	}

	public static Long _long(long value) {
		return Long.valueOf(value);
	}

	public static Double _double(double value) {
		return Double.valueOf(value);
	}

	public static Float _float(float value) {
		return Float.valueOf(value);
	}

	public static Short _short(short value) {
		return Short.valueOf(value);
	}

	public static Byte _byte(byte value) {
		return Byte.valueOf(value);
	}

	public static Boolean _boolean(boolean value) {
		return Boolean.valueOf(value);
	}

}

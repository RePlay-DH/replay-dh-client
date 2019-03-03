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

import java.util.Optional;

/**
 * @author Markus Gärtner
 *
 */
public interface Mutable<O extends Object> extends Wrapper<O>, Cloneable {

	void set(Object value);

	default O setAndGet(Object value) {
		set(value);
		return get();
	}

	default O getAndSet(Object value) {
		O old = get();
		set(value);
		return old;
	}

	default Optional<O> asOptional() {
		return Optional.ofNullable(get());
	}

	void clear();

	boolean isPrimitive();

	boolean isEmpty();

	public static Mutable<?> forClass(Class<?> clazz) {
		return forClass(clazz, true);
	}

	public static Mutable<?> forClass(Class<?> clazz, boolean unwrap) {

		if(unwrap && Primitives.isPrimitiveWrapperClass(clazz)) {
			clazz = Primitives.unwrap(clazz);
		}

		Mutable<?> result = null;

		if(clazz.isPrimitive()) {
			switch (clazz.getSimpleName()) {
			case "int": result = new MutablePrimitives.MutableInteger(); break;
			case "long": result = new MutablePrimitives.MutableLong(); break;
			case "byte": result = new MutablePrimitives.MutableByte(); break;
			case "short": result = new MutablePrimitives.MutableShort(); break;
			case "float": result = new MutablePrimitives.MutableFloat(); break;
			case "double": result = new MutablePrimitives.MutableDouble(); break;
			case "char": result = new MutablePrimitives.MutableChar(); break;
			case "boolean": result = new MutablePrimitives.MutableBoolean(); break;
			case "void": result = NULL; break;

			default:
				break;
			}
		} else if(Void.class.equals(clazz)) {
			result = NULL;
		} else {
			result = new Mutable.MutableObject<Object>();
		}

		if(result==null)
			throw new IllegalArgumentException("Unable to produce mutable storage for class: "+clazz);

		return result;
	}

	public static final Mutable<Void> NULL = new Mutable<Void>() {

		@Override
		public Void get() {
			return null;
		}

		@Override
		public void set(Object value) {
			throw new UnsupportedOperationException("Cannot set value for null container");
		}

		@Override
		public void clear() {
			// no-op
		}

		@Override
		public boolean isPrimitive() {
			return false;
		}

		@Override
		public boolean isEmpty() {
			return true;
		}
	};

	public static class MutableObject<O extends Object> implements Mutable<O> {

		public static final Object DEFAULT_EMPTY_VALUE = null;

		private O value;

		public MutableObject() {
			// no-op
		}

		public MutableObject(O value) {
			set(value);
		}

		/**
		 * @see de.ims.icarus2.util.Wrapper#get()
		 */
		@Override
		public O get() {
			return value;
		}

		/**
		 * @see de.ims.icarus2.util.Mutable#set(java.lang.Object)
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void set(Object value) {
			this.value = (O) value;
		}

		/**
		 * @see de.ims.icarus2.util.Mutable#clear()
		 */
		@SuppressWarnings("unchecked")
		@Override
		public void clear() {
			value = (O)DEFAULT_EMPTY_VALUE;
		}

		/**
		 * @see java.lang.Object#clone()
		 */
		@Override
		public Object clone() {
			return new MutableObject<>(value);
		}

		/**
		 * @see de.ims.icarus2.util.Mutable#isPrimitive()
		 */
		@Override
		public boolean isPrimitive() {
			return false;
		}

		/**
		 * @see de.ims.icarus2.util.Mutable#isEmpty()
		 */
		@Override
		public boolean isEmpty() {
			return value==DEFAULT_EMPTY_VALUE;
		}
	}
}

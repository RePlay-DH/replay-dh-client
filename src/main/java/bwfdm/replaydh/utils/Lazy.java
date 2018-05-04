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

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Class to lazily initialize a value based on provided Factory method or
 * class's default constructor inspired from C# Lazy&lt;T> using Lambda and
 * Method reference capabilities of Java 8. All exceptions resulting from
 * factory methods are cascaded to caller. Exceptions from default constructor
 * is wrapped as a RuntimeException. Throws NullPointerException if the factory
 * method itself is null or returns null on execution to fail fast. Available as
 * both Thread safe(default) and unsafe versions. Usage examples
 *
 * <pre>
 * public Lazy&lt;IntensiveResource> r = Lazy.create(IntensiveResource.class, false);
 *
 * public Lazy&lt;IntensiveResource> r1 = Lazy.create(IntensiveResource::buildResource);
 *
 * public Lazy&lt;IntensiveResource> r2 = Lazy.create(() -> return new IntensiveResource());
 * </pre>
 *
 * Invoking toString() will cause the object to be initialized. Accessing the
 * value of the Lazy object using Lazy.value() method causes the object to
 * initialize and execute the Factory method supplied. Values can also be
 * obtained as {@link java.util.Optional} to avoid NPEs
 *
 * @author raam
 *
 * @author Markus Gärtner
 *
 * @param <V> Type of object to be created
 */
public class Lazy<V> implements Serializable {

	/**
	 * Default UID instead of generated one, because proxy
	 */
	private static final long serialVersionUID = 1L;

	private static final String FACTORY_NULL_RETURN_MESSAGE = "Factory method returns null for Lazy value";

	private V value;

	private boolean created = false;

	protected Supplier<V> factory;

	public static <V> Lazy<V> create(Class<V> valueClass, boolean threadSafe) {
		if (threadSafe) {
			return new ThreadSafeLazy<V>(valueClass);
		} else {
			return new Lazy<V>(valueClass);
		}
	}

	public static <V> Lazy<V> create(Class<V> valueClass) {
		return create(valueClass, true);
	}

	public static <V> Lazy<V> create(Supplier<V> factoryMethod,
			boolean threadSafe) {
		if (threadSafe) {
			return new ThreadSafeLazy<V>(factoryMethod);
		} else {
			return new Lazy<V>(factoryMethod);
		}
	}

	public static <V> Lazy<V> create(Supplier<V> factoryMethod) {
		return create(factoryMethod, true);
	}

	private Lazy(Class<V> valueClass) {
		factory = () -> {
			try {
				return valueClass.newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		};
	}

	private Lazy(Supplier<V> factoryMethod) {
		if (factoryMethod == null) {
			throw new NullPointerException();
		}
		this.factory = factoryMethod;
	}

	public boolean created() {
		return created;
	}

	public V value() {
		if (!created()) {
			initialize();
			if (getValue0() == null) {
				throw new NullPointerException(FACTORY_NULL_RETURN_MESSAGE);
			}
		}
		return getValue0();
	}

	protected V getValue0() {
		return value;
	}

	public Optional<V> optional() {
		if (!created()) {
			initialize();
		}
		return Optional.ofNullable(getValue0());
	}

	protected void initialize() {
		value = factory.get();
		created = true;
	}

	@Override
	public String toString() {
		return value().toString();
	}

	private static class ThreadSafeLazy<V> extends Lazy<V> {

		private static final long serialVersionUID = 1L;

		private AtomicBoolean created = new AtomicBoolean(false);
		private AtomicReference<V> value = new AtomicReference<V>();
		private ReentrantLock writeLock = new ReentrantLock();

		private ThreadSafeLazy(Class<V> valueClass) {
			super(valueClass);
		}

		private ThreadSafeLazy(Supplier<V> factoryMethod) {
			super(factoryMethod);
		}

		@Override
		public boolean created() {
			return created.get();
		}

		@Override
		protected V getValue0() {
			return value.get();
		}

		@Override
		protected void initialize() {
			writeLock.lock();
			try {
				if (value.get() == null) {
					if (value.compareAndSet(null, factory.get())) {
						created.set(true);
					} else
						throw new InternalError("Reentrant attempt at creating value - cyclic dependency detected?");
				}
			} finally {
				writeLock.unlock();
			}
		}

	}
}

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

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Markus Gärtner
 *
 */
public final class ClassProxy {

	private static final Logger log = LoggerFactory.getLogger(ClassProxy.class);

	private final String className;
	private final ClassLoader classLoader;

	private Map<String, Object> properties;

	public ClassProxy(String className, ClassLoader classLoader) {
		requireNonNull(className);
		requireNonNull(classLoader);

		this.className = className;
		this.classLoader = classLoader;
	}

	public Object loadObject() {
		try {
			Class<?> clazz = classLoader.loadClass(className);

			return clazz.newInstance();
		} catch (ClassNotFoundException e) {
			log.error("ClassProxy: Could not find class: {}", className, e); //$NON-NLS-1$
		} catch (InstantiationException e) {
			log.error("ClassProxy: Unable to instantiate class: {}", className, e); //$NON-NLS-1$
		} catch (IllegalAccessException e) {
			log.error("ClassProxy: Unable to access default constructor: {}", className, e); //$NON-NLS-1$
		}

		return null;
	}

	public Class<?> loadClass() throws ClassNotFoundException {
		return classLoader.loadClass(className);
	}

	public Object loadObjectUnsafe() throws ClassNotFoundException, InstantiationException, IllegalAccessException  {
		Class<?> clazz = classLoader.loadClass(className);

		return clazz.newInstance();
	}

	@Override
	public String toString() {
		return "ClassProxy: "+className; //$NON-NLS-1$
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return classLoader.hashCode() * className.hashCode();
	}

	/**
	 * Two {@code ClassProxy} instances are considered equal if
	 * they both refer to the same {@code Class} as by their
	 * {@code className} field and both use the same {@code ClassLoader}
	 * to load the final object.
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(this==obj) {
			return true;
		} if(obj instanceof ClassProxy) {
			ClassProxy other = (ClassProxy) obj;
			return className.equals(other.className)
					&& classLoader.equals(other.classLoader);
		}
		return false;
	}

	public Object getProperty(String key) {
		return properties==null ? null : properties.get(key);
	}

	public void setProperty(String key, Object value) {
		if(properties==null) {
			properties = new HashMap<>();
		}

		properties.put(key, value);
	}

	/**
	 * @return the className
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @return the classLoader
	 */
	public ClassLoader getClassLoader() {
		return classLoader;
	}
}

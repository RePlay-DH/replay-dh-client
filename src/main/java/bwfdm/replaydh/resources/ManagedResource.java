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
package bwfdm.replaydh.resources;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * @author Markus Gärtner
 *
 */
public final class ManagedResource {

	private final String baseName;

	private final ResourceLoader loader;

	private ResourceBundle bundle;

	/**
	 *
	 */
	ManagedResource(String baseName, ResourceLoader loader) {
		this.baseName = baseName;
		this.loader = loader;
	}

	public synchronized void reload(Locale locale) {
		bundle = loader.loadResource(baseName, locale);
	}

	synchronized void clear() {
		bundle = null;
	}

	public String getResource(Locale locale, String key) {
		if(bundle==null) {
			reload(locale);
		}

		return bundle==null ? key : bundle.getString(key);
	}

	/**
	 * @return the name
	 */
	public String getBaseName() {
		return baseName;
	}

	/**
	 * @return the loader
	 */
	public ResourceLoader getLoader() {
		return loader;
	}

	/**
	 * @return the bundle
	 */
	public ResourceBundle getBundle() {
		return bundle;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return baseName.hashCode() *  loader.hashCode();
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		} else if(obj instanceof ManagedResource) {
			ManagedResource other = (ManagedResource) obj;
			return baseName.equals(other.baseName) && loader.equals(other.loader);
		}

		return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return baseName;
	}
}

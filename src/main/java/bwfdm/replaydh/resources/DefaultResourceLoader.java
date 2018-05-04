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

import static java.util.Objects.requireNonNull;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;


/**
 * A {@code ResourceLoader} that uses the {@code ClassLoader}
 * of a certain {@code Class} to load resources.
 *
 * @author Markus Gärtner
 *
 */
public class DefaultResourceLoader implements ResourceLoader {

	private static final Control control = new UTF8Control();

	protected ClassLoader classLoader;

	/**
	 * @param clazz
	 */
	public DefaultResourceLoader(ClassLoader classLoader) {
		setLoader(classLoader);
	}

	protected Control getControl() {
		return control;
	}

	public ClassLoader getLoader() {
		return classLoader;
	}

	public void setLoader(ClassLoader classLoader) {
		requireNonNull(classLoader);

		this.classLoader = classLoader;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		} else if(obj instanceof DefaultResourceLoader) {
			return ((DefaultResourceLoader)obj).classLoader==classLoader;
		}
		return false;
	}

	/**
	 * @see de.ims.icarus.resources.ResourceLoader#loadResource(java.lang.String, java.util.Locale)
	 */
	@Override
	public ResourceBundle loadResource(String name, Locale locale) {
		return ResourceBundle.getBundle(name, locale, getLoader(), getControl());
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return classLoader.hashCode();
	}

}

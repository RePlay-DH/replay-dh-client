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
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * A {@code ResourceLoader} is responsible for fetching
 * arbitrary resources (normally during the process of localization)
 * in a transparent way. This approach separates the management
 * of localization data from the actual loading process.
 * The {@link ResourceManager} calls {@code #loadResource(String, Locale)}
 * on certain instances of {@code ResourceLoader} whenever there is
 * the need to load new data for a given combination of {@link Locale}
 * and {@code name} where the exact semantic of {@code name} is 
 * implementation specific (it can denote a resource path in the
 * way of fully qualified resource naming or the remote location
 * of a resource bundle available over the Internet).
 * 
 * @author Markus Gärtner 
 * @version $Id: ResourceLoader.java 123 2013-07-31 15:22:01Z mcgaerty $
 *
 */
public interface ResourceLoader {

	/**
	 * Attempts to load a new {@code ResourceBundle} for the given 
	 * combination of {@code Locale} and {@code name}. Implementations
	 * should throw an {@code MissingResourceException} when encountering
	 * errors or when there is no matching resource data in the
	 * domain of this {@code ResourceLoader}. 
	 * 
	 * @param name abstract identifier for the resource in question
	 * @param locale the {@code Locale} associated with the resource
	 * in question
	 * @return the new {@code ResourceBundle} for the given combination of 
	 * {@code Locale} and {@code name}
	 * @throws MissingResourceException if the desired resource could
	 * not be found
	 */
	ResourceBundle loadResource(String name, Locale locale);
}

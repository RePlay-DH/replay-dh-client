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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.utils.StringPrimitives;


/**
 *
 *
 *
 * @author Markus Gärtner
 *
 */
public final class ResourceManager {

	private volatile static ResourceManager instance;

	public static ResourceManager getInstance() {
		ResourceManager result = instance;

		if (result == null) {
			synchronized (ResourceManager.class) {
				result = instance;

				if (result == null) {
					/*
					 * The default instance will use the systems default locale,
					 * have no accommodating environment to load properties from
					 * and will both report unknown keys and return them as values.
					 */
					result = new ResourceManager(
							Locale.getDefault(),
							key -> null,
							true,
							true);
					result.addManagedResource("bwfdm.replaydh.resources.localization");
					instance = result;
				}
			}
		}

		return result;
	}

	/**
	 *
	 * @param newInstance
	 */
	public static void setInstance(ResourceManager newInstance) {
		// do we need any verification?
		instance = newInstance;
	}

	private final List<ManagedResource> resources = new LinkedList<>();

	private final boolean notifyMissingResource;
	private final boolean returnKeyIfAbsent;

	public static final ResourceLoader DEFAULT_RESOURCE_LOADER
			= new DefaultResourceLoader(ResourceManager.class.getClassLoader());

	private static final Logger log = LoggerFactory.getLogger(ResourceManager.class);

	private final Locale locale;

	private final UnaryOperator<String> propertyLookup;

	public ResourceManager(Locale locale, UnaryOperator<String> propertyLookup,
			boolean notifyMissingResource, boolean returnKeyIfAbsent) {
		this.notifyMissingResource = notifyMissingResource;
		this.returnKeyIfAbsent = returnKeyIfAbsent;
		this.locale = requireNonNull(locale);
		this.propertyLookup = propertyLookup;
	}

	public boolean isNotifyMissingResource() {
		return notifyMissingResource;
	}

	public boolean isReturnKeyIfAbsent() {
		return returnKeyIfAbsent;
	}

	public Locale getLocale() {
		return locale;
	}

	public void close() {
		resources.clear();
	}

	// prevent cloning
	@Override
	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public ManagedResource addManagedResource(String baseName, ResourceLoader loader) {
		requireNonNull(baseName);

		if(loader==null) {
			loader = DEFAULT_RESOURCE_LOADER;
		}

		ManagedResource managedResource = null;

		synchronized (resources) {
			managedResource = getManagedResource(baseName);

			if(managedResource==null) {
				resources.add(new ManagedResource(baseName, loader));
			}
		}

		return managedResource;
	}

	public ManagedResource addManagedResource(String baseName) {
		return addManagedResource(baseName, null);
	}

	public ManagedResource getManagedResource(String baseName) {
		requireNonNull(baseName);

		synchronized (resources) {
			for(ManagedResource resource : resources) {
				if(resource.getBaseName().equals(baseName)) {
					return resource;
				}
			}
		}

		return null;
	}

	public ManagedResource removeManagedResource(String baseName) {

		synchronized (resources) {
			for(Iterator<ManagedResource> it = resources.iterator(); it.hasNext();) {
				ManagedResource resource = it.next();
				if(resource.getBaseName().equals(baseName)) {
					it.remove();
					return resource;
				}
			}
		}

		return null;
	}

	public static ResourceLoader createResourceLoader(ClassLoader classLoader) {
		if(classLoader==null || ResourceManager.class.getClassLoader()==classLoader)
			return DEFAULT_RESOURCE_LOADER;
		else
			return new DefaultResourceLoader(classLoader);
	}

	public String getFormatted(String key, Object...args) {
		return String.format(get(key), args);
	}

//	public String get(String key) {
//		return get(key, (Object[])null);
//	}

//	public String get(String key, String defaultValue) {
//		return get(key, null, defaultValue);
//	}

	public String get(String key, Object...params) {
		String value = getResource(key);

		// Quick return if we didn't find the resource and got the key back
		if(value == key) {
			return value;
		}

//		// Applies default value if required
//		if (value == null) {
//			value = defaultValue;
//		}

		// Replaces the place holders with the values in the array or performs general property expansion
		if (value != null && ((params != null && params.length > 0) || value.indexOf('{') != -1)) {
			value = format(value, params);
		}

		return value;
	}

	private String getResource(String key) {
		for(ManagedResource resource : resources) {
			try {
				return resource.getResource(getLocale(), key);
			} catch (MissingResourceException mrex) {
				// continue
			}
		}

		if(notifyMissingResource) {
			log.info("No resource entry for key '{}' in locale {}", key, getLocale());
		}

		return returnKeyIfAbsent ? key : null;
	}

	public String format(String text, Object...params) {
		requireNonNull(text);

		StringBuilder result = new StringBuilder(text.length());
//		String index = null;
//		boolean digit = false;
//
//		for (int i = 0; i < text.length(); i++) {
//			char c = text.charAt(i);
//
//			if (c == '{') {
//				index = ""; //$NON-NLS-1$
//				digit = true;
//			} else if (index != null && c == '}') {
//
//				if(digit) {
//					int tmp = Integer.parseInt(index) - 1;
//
//					if (tmp >= 0 && params!=null && tmp < params.length) {
//						result.append(params[tmp]);
//					}
//				} else {
//					String replacement = get(index);
//					if(replacement==null) {
//						replacement = index;
//					}
//					result.append(replacement);
//				}
//
//				index = null;
//				digit = false;
//			} else if (index != null) {
//				index += c;
//				digit &= Character.isDigit(c);
//			} else {
//				result.append(c);
//			}
//		}

		format0(result, text, 0, text.length(), params);

		return result.toString();
	}

	private enum MarkerType {
		/**
		 * Next index after the last used one
		 */
		EMPTY,

		/**
		 * Specific index given in the marker
		 */
		INDEX,

		/**
		 * Key of a resource to expand (can be recursive)
		 */
		RESORUCE,

		/**
		 * Environmental property expansion
		 */
		PROPERTY,
		;
	}

	private String getProperty(String key) {
		return propertyLookup==null ? null : propertyLookup.apply(key);
	}

	private static boolean isMagicCharacter(char c) {
		return c=='{' || c=='}';
	}

	private static Object getParam(String text, int index, Object[] params) {
		if(params==null || index>=params.length)
			throw new IllegalArgumentException(String.format(
					"Missing parameter for index %d in '%s'", index, text));

		return params[index];
	}

	/**
	 * Format into given buffer the result of expanding markers in the specified
	 * section of input, using given arguments.
	 *
	 * @param buffer output buffer to write into
	 * @param text raw text containing markers
	 * @param offset0 begin of section in raw text (inclusive)
	 * @param offset1 end of section in raw text (exclusive)
	 * @param params list of parameters usable for expansion
	 */
	private void format0(StringBuilder buffer, String text, int offset0, int offset1, Object...params) {
		int begin = -1, end = -1;
		boolean escaped = false;
		boolean digits = false;

		MarkerType markerType = null;
		int paramIndex = -1;

		for (int i = offset0; i < offset1; i++) {
			char c = text.charAt(i);

			// Honor escaping only outside of marker definitions
			if(escaped && markerType==null) {

				// Escaping only supported for magic characters
				if(c!='\\' && !isMagicCharacter(c)) {
					buffer.append('\\');
				}

				buffer.append(c);

				escaped = false;
				continue;
			}

			switch (c) {
			case '\\':
				escaped = true;
				break;

			case '{':
				begin = i+1;
				digits = true;
				markerType = MarkerType.EMPTY;
				break;

			case '}': {
				end = i;

				// If marker type isn't decided do a quick check between INDEX and RESOURCE
				if(end>begin && markerType!=MarkerType.PROPERTY) {
					markerType = digits ? MarkerType.INDEX : MarkerType.RESORUCE;
				}

				switch (markerType) {
				case EMPTY:
					buffer.append(String.valueOf(getParam(text, ++paramIndex, params)));
					break;

				case INDEX:
					paramIndex = StringPrimitives.parseInt(text, begin, end-1) - 1;
					buffer.append(String.valueOf(getParam(text, paramIndex, params)));
					break;

				case RESORUCE: {
					String resource = getResource(text.substring(begin, end));
					buffer.append(resource);
				} break;

				case PROPERTY: {
					String property = getProperty(text.substring(begin, end));
					buffer.append(property); // do we need to output a special hint for missing properties?
				} break;

				default:
					throw new IllegalArgumentException();
				}

				// Officially end marker section
				markerType = null;
			} break;

			case '$':
				// Only consider property symbol at the beginning of a marker section
				if(begin==i && markerType!=MarkerType.PROPERTY) {
					begin++; //make sure to skip the '$' symbol in our section definition
					markerType = MarkerType.PROPERTY;
					break;
				}
				// intended fall-through in case the $ symbol is part of the normal text

			default:
				if(markerType!=null) {
					// In active marker
					digits &= Character.isDigit(c);
				} else {
					buffer.append(c);
				}

				break;
			}
		}
	}
}

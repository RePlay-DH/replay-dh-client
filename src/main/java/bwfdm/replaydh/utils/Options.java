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

import java.util.HashMap;
import java.util.Map;

/**
 * @author Markus Gärtner
 *
 */
public class Options extends HashMap<String, Object> implements Cloneable {

	/**
	 *
	 */
	private static final long serialVersionUID = 6318648432239062316L;

	public static final Options emptyOptions = new Options() {

		private static final long serialVersionUID = -6172790615021617955L;

		@Override
		public Object put(String key, Object value) {
			return null;
		}

		@Override
		public Object remove(Object key) {
			return null;
		}
	};

	public Options() {
		// no-op
	}

	public Options(Options source) {
		putAll(source);
	}

	public Options(Map<String, ? extends Object> source) {
		putAll(source);
	}

	public Options(Object... args) {
		putAll(args);
	}

	@SuppressWarnings("unchecked")
	public <O extends Object> O get(String key, O defaultValue) {
		Object value = get(key);
		return value==null ? defaultValue : (O) value;
	}

	public Object getOptional(String key, Object defaultValue) {
		Object value = get(key);
		return value==null ? defaultValue : value;
	}

	@Override
	public Object put(String key, Object value) {
		if(value==null) {
			return remove(key);
		} else {
			return super.put(key, value);
		}
	}

	public void putAll(Object... args) {
		if (args == null || args.length % 2 != 0) {
			return;
		}

		for (int i = 0; i < args.length; i += 2) {
			put(String.valueOf(args[i]), args[i + 1]);
		}
	}

	@Override
	public void putAll(Map<? extends String, ? extends Object> m) {
		// Silently fail when argument is null
		if(m==null) {
			return;
		}

		super.putAll(m);
	}

	public void dump() {
		System.out.println("Options: "); //$NON-NLS-1$
		for(Entry<String, Object> entry : entrySet())
			System.out.printf("  -key=%s value=%s\n",  //$NON-NLS-1$
					entry.getKey(), String.valueOf(entry.getValue()));
	}

	public Object firstSet(String...keys) {
		Object value = null;

		for(String key : keys) {
			if((value = get(key)) != null) {
				break;
			}
		}

		return value;
	}

	public int getInteger(String key, int defaultValue) {
		Object result = get(key);
		if(result instanceof String) {
			try {
				result = Integer.parseInt((String) result);
			} catch(NumberFormatException e) {
				// ignore
			}
		}

		return result instanceof Number ? ((Number)result).intValue() : defaultValue;
	}

	public int getInteger(String key) {
		return getInteger(key, 0);
	}

	public long getLong(String key, long defaultValue) {
		Object result = get(key);
		if(result instanceof String) {
			try {
				result = Long.parseLong((String) result);
			} catch(NumberFormatException e) {
				// ignore
			}
		}

		return result instanceof Number ? ((Number)result).longValue() : defaultValue;
	}

	public long getLong(String key) {
		return getLong(key, 0L);
	}

	public double getDouble(String key, double defaultValue) {
		Object result = get(key);
		if(result instanceof String) {
			try {
				result = Double.parseDouble((String) result);
			} catch(NumberFormatException e) {
				// ignore
			}
		}

		return result instanceof Number ? ((Number)result).doubleValue() : defaultValue;
	}

	public double getDouble(String key) {
		return getDouble(key, 0d);
	}

	public float getFloat(String key, float defaultValue) {
		Object result = get(key);
		if(result instanceof String) {
			try {
				result = Float.parseFloat((String) result);
			} catch(NumberFormatException e) {
				// ignore
			}
		}

		return result instanceof Number ? ((Number)result).floatValue() : defaultValue;
	}

	public float getFloat(String key) {
		return getFloat(key, 0f);
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		Object result = get(key);
		if(result instanceof String) {
			try {
				result = Boolean.parseBoolean((String) result);
			} catch(NumberFormatException e) {
				// ignore
			}
		}

		return result instanceof Boolean ? ((Boolean)result).booleanValue() : defaultValue;
	}

	public boolean getBoolean(String key) {
		return getBoolean(key, false);
	}

	@Override
	public final Options clone() {
		return new Options(this);
	}

	// Collection of commonly used option keys

	public static final String NAME = "name"; //$NON-NLS-1$
	public static final String DESCRIPTION = "description"; //$NON-NLS-1$
	public static final String LABEL = "label"; //$NON-NLS-1$
	public static final String TITLE = "title"; //$NON-NLS-1$
	public static final String CONTENT_TYPE = "contentType"; //$NON-NLS-1$
	public static final String CONVERTER = "converter"; //$NON-NLS-1$
	public static final String CONTEXT = "context"; //$NON-NLS-1$
	public static final String LOCATION = "location"; //$NON-NLS-1$
	public static final String LANGUAGE = "language"; //$NON-NLS-1$
	public static final String ID = "id"; //$NON-NLS-1$
	public static final String FILTER = "filter"; //$NON-NLS-1$
	public static final String EXTENSION = "extension"; //$NON-NLS-1$
	public static final String PLUGIN = "plugin"; //$NON-NLS-1$
	public static final String DATA = "data"; //$NON-NLS-1$
	public static final String OWNER = "owner"; //$NON-NLS-1$
	public static final String INDEX = "index"; //$NON-NLS-1$
}

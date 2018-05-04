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
package bwfdm.replaydh.ui.icons;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Markus Gärtner
 *
 */
public final class Resolution {

	private final int width, height;

	private Resolution(int width, int height) {
		checkArgument("Width cannot be negative", width>0);
		checkArgument("Height cannot be negative", height>0);

		this.width = width;
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	@Override
	public int hashCode() {
		return width + 31*height;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		} else if(obj instanceof Resolution) {
			Resolution other = (Resolution) obj;
			return width==other.width && height==other.height;
		}
		return false;
	}

	@Override
	public String toString() {
		return String.format("Resolution@[width=%d, height=%d]", width, height);
	}

	private static final Map<Long, Resolution> cache = Collections.synchronizedMap(new HashMap<>());

	private static Resolution getCachedOrCreate(int width, int height, boolean mayCache) {

		long key = (long)width<<32 + height;

		Resolution res = cache.get(key);

		if(res==null) {
			res = new Resolution(width, height);

			if(mayCache) {
				cache.put(key, res);
			}
		}

		return res;
	}

	/**
	 * Returns a quadratic {@literal Resolution} with the given size.
	 * This will usually access the internal cache to not create new
	 * instances.
	 *
	 * @param size
	 * @return
	 */
	public static Resolution forSize(int size) {
		return forSize(size, size);
	}

	/**
	 * Returns a {@link Resolution} object with custom {@code width}
	 * and {@code height}.The method might returned a cached instance
	 * but will usually only do so for quadratic resolutions.
	 *
	 * @param width
	 * @param height
	 * @return
	 */
	public static Resolution forSize(int width, int height) {
		// Allow caching only for quadratic resolutions
		return getCachedOrCreate(width, height, width==height);
	}
}

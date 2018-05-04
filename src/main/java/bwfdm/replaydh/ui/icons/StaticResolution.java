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

/**
 * @author Markus Gärtner
 * @version $Id$
 *
 */
public enum StaticResolution {
	RES_SMALL(16),
	RES_30(30),
	RES_32(32),
	RES_64(64),
	RES_128(128),
	RES_256(256),
	RES_512(512),
	RES_1024(1024);

	private final int size;

	private StaticResolution(int size) {
		this.size = size;
	}

	public int getSize() {
		return size;
	}

	public static StaticResolution forSize(int size) {
		switch (size) {
			case 16: return RES_SMALL;
			case 30: return RES_30;
			case 32: return RES_32;
			case 64: return RES_64;
			case 128: return RES_128;
			case 256: return RES_256;
			case 512: return RES_512;
			case 1024: return RES_1024;
		}

		throw new IllegalArgumentException("Unknown resolution: "+size);
	}

	public static StaticResolution getNextLarger(int size) {
		if(size<=16) {
			return RES_SMALL;
		} else if(size<=30) {
			return RES_30;
		} else if(size<=32) {
			return RES_32;
		} else if(size<=64) {
			return RES_64;
		} else if(size<=128) {
			return RES_128;
		} else if(size<=256) {
			return RES_256;
		} else if(size<=512) {
			return RES_512;
		} else if(size<=1024) {
			return RES_1024;
		} else
			throw new IllegalArgumentException("Unsupported size: "+size);
	}

	public static StaticResolution getNextSmaller(int size) {
		if(size>=512) {
			return RES_512;
		} else if(size>=256) {
			return RES_256;
		} else if(size>=128) {
			return RES_128;
		} else if(size>=64) {
			return RES_64;
		} else if(size>=32) {
			return RES_32;
		} else if(size>=30) {
			return RES_30;
		} else if(size>=16) {
			return StaticResolution.RES_SMALL;
		} else
			throw new IllegalArgumentException("Unsupported size: "+size);
	}

	//TODO make a forLabel(String) method that takes constants such as "default" "small" "medium" "large"...
}

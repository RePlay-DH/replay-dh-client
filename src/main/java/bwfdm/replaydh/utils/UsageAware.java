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

/**
 * Provides a mechanism for client code to support an object in
 * tracking its usage. When client code wishes to use the object
 * over prolonged time or even store it in its own data structures
 * then it should tell it by {@link #incrementUseCounter() incrementing}
 * the internal use counter. After the object is no longer needed it
 * should then {@link #decrementUseCounter() decrement} it again.
 * <p>
 * To check whether or not an object is currently in use by <i>any</i>
 * client code the {@link #inUse()} method can be utilized.
 *
 * @author Markus Gärtner
 *
 */
public interface UsageAware {

	/**
	 * Increments the internal use counter by {@code 1} and returns
	 * {@code true} if the return value of {@link #inUse()} will be
	 * different after this method finishes.
	 *
	 * @return
	 */
	boolean incrementUseCounter();

	/**
	 * Decrements the internal use counter by {@code 1} and returns
	 * {@code true} if the return value of {@link #inUse()} will be
	 * different after this method finishes.
	 *
	 * @return
	 */
	boolean decrementUseCounter();

	/**
	 * Returns whether or not the internal use counter for this
	 * object is greater than {@code 0}.
	 *
	 * @return
	 */
	boolean inUse();

	/**
	 * Memory function that returns whether or not {@link #incrementUseCounter()}
	 * has been called at least once over the entire lifetime of this object.
	 *
	 * @return
	 */
	boolean hasBeenUsed();
}

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
 * Models the result of an identity resolution process.
 *
 * @author Markus Gärtner
 *
 */
public interface LookupResult<E extends Object, I extends Object> extends Comparable<LookupResult<E,I>> {
	/**
	 * Returns the actual result of the resolution process
	 * or {@code null} in case the process yielded no usable
	 * result.
	 *
	 * @return
	 */
	E getTarget();

	/**
	 * Returns the input to te resolution process.
	 *
	 * @return
	 */
	I getInput();

	/**
	 * Per default a lookup result is assumed to be valid if it
	 * provides a valid {@link #getTarget() target} and has no
	 * {@link #getException() exception} associated with it.
	 *
	 * @return
	 */
	default boolean isValid() {
		return getTarget()!=null && getException()==null;
	}

	/**
	 * Returns a relative evaluation for the result of the
	 * resolution process. The expected value range of this
	 * relevance is the default closed probability space
	 * {@code [0..1]}.
	 * <p>
	 * If the {@link #getTarget()} method returns
	 * {@code null} then the value obtained via this method
	 * has no meaning and is undefined.
	 *
	 * @return
	 */
	double getRelevance();

	/**
	 * In case the resolution process failed unexpectedly and
	 * the {@link #getTarget()} method returns {@code null},
	 * this method can provide the exception which caused the
	 * failure.
	 *
	 * @return
	 */
	Exception getException();

	/**
	 * Compares two lookup results based on their relevance
	 * scores and whether or not they are actually {@link #isValid() valid}.
	 * <table border="1">
	 * <tr><th>this.isValid()</th><th>other.isValid()</th><th>result</th></tr>
	 * <tr><td>true</td><td>true</td><td>{@link Double#compare(double, double)} applied to the two relevance values</td></tr>
	 * <tr><td>true</td><td>false</td><td>1</td></tr>
	 * <tr><td>false</td><td>true</td><td>-1</td></tr>
	 * <tr><td>false</td><td>false</td><td>0</td></tr>
	 * </table>
	 *
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	default int compareTo(LookupResult<E,I> other) {

		boolean valid = isValid();
		boolean otherValid = other.isValid();

		if(valid && otherValid) {
			return Double.compare(getRelevance(), other.getRelevance());
		} else if(valid) {
			return 1;
		} else if(otherValid) {
			return -1;
		} else {
			return 0;
		}
	}
}

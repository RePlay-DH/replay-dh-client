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
package bwfdm.replaydh.io;

/**
 * Models the treatment of a tracker towards a given set
 * of files.
 *
 * @author Markus Gärtner
 *
 */
public enum TrackingAction {

	/**
	 * Start tracking file.
	 * <p>
	 * This overrides the {@link #IGNORE} action:
	 * A file will be forced to be tracked via this
	 * action even if it has previously been marked
	 * to be ignored!
	 */
	ADD,

	/**
	 * Stop tracking file and delete it
	 */
	REMOVE,

	/**
	 * Do not track the file.
	 * <p>
	 * If file is currently being tracked this will imply
	 * a weak version of {@link #REMOVE}: The file will be
	 * removed from the internal index, but will <b>not</b> be
	 * deleted from disk!
	 */
	IGNORE,

	/**
	 * Completely ignore the file.
	 * <p>
	 * If the file has been previously tracked or ignored, this
	 * will effectively revert that and put the handling of the
	 * file back into a neutral state.
	 */
	NONE,
	;
}

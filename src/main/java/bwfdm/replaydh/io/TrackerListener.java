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

import java.nio.file.Path;
import java.util.Set;

/**
 * @author Markus Gärtner
 *
 */
public interface TrackerListener {

	/**
	 * An entity invoked a refresh cycle on the tracker.
	 *
	 * @param tracker
	 */
	void refreshStarted(FileTracker tracker);

	/**
	 * A refresh cycle failed and the tracker was able to catch
	 * the cause which is provided in this callback method as
	 * argument.
	 *
	 * @param tracker
	 * @param e
	 */
	void refreshFailed(FileTracker tracker, Exception e);

	/**
	 * A refresh cycle completed or was interrupted at the
	 * users decision.
	 *
	 * @param tracker
	 * @param canceled
	 */
	void refreshDone(FileTracker tracker, boolean canceled);

	void trackingStatusChanged(FileTracker tracker, Set<Path> files, TrackingAction action);

	void statusInfoChanged(FileTracker tracker);
}

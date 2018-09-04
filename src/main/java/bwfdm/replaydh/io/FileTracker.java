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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import bwfdm.replaydh.utils.PropertyChangeSource;

/**
 * Fires the following property change events to be received via
 * {@link PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)}:
 * <p>
 * <dl>
 *   <dt>{@code statusInfo}</dt>
 *     <dd>Signals the completion of a call to {@link #clearStatusInfo()} or {@link #refreshStatusInfo()}.
 *     The value of this property is highly implementation dependent (usually client code will only be able
 *     to use it for something like a check against {@code null}).
 *     Note that only the {@link PropertyChangeEvent#getNewValue() newValue} will be available and hold
 *     the most recent state of the cached status info</dd>
 * </dl>
 *
 *
 * @author Markus Gärtner
 *
 */
public interface FileTracker extends PropertyChangeSource {

	String NAME_STATUS_INFO = "statusInfo";

	Path getTrackedFolder();

	/**
	 * Signals whether or not this tracker is active.
	 * If so then the {@link #getTrackedFolder()} method
	 * will return  a {@code non-null} {@link Path} object
	 * denoting the tracked root folder.
	 *
	 * @return
	 */
	default boolean isTracking() {
		return getTrackedFolder()!=null;
	}

	/**
	 * Return whether an up-to-date status report for files
	 * managed by this tracker is available
	 */
	boolean hasStatusInfo();

	LocalDateTime getLastUpdateTime();

	/**
	 * Attempt to refresh the status report for files managed
	 * by this tracker. Returns whether or not this succeeded.
	 */
	boolean refreshStatusInfo() throws TrackerException;

	/**
	 * Tells the tracker to discard all cached status info.
	 */
	void clearStatusInfo();

	/**
	 * Returns all files matching the given {@code status}.
	 *
	 * @param status
	 * @return
	 */
	Set<Path> getFilesForStatus(TrackingStatus status) throws TrackerException;

	int getFileCountForStatus(TrackingStatus...statuses) throws TrackerException;

	/**
	 * Quick check to see if the tracker currently has any
	 * files within its scope that match any of the given {@code statuses}.
	 *
	 * @param status
	 * @return
	 * @throws TrackerException
	 */
	default boolean hasFilesForStatus(TrackingStatus...statuses) throws TrackerException {
		return getFileCountForStatus(statuses)>0;
	}

	/**
	 * Returns a map of files and their respective statuses.
	 * The {@code statuses} argument defines the set of statuses
	 * for which files should be included.
	 *
	 * @param statuses
	 * @return
	 * @throws TrackerException
	 */
	default Map<Path, TrackingStatus> getFilesForStatus(Set<TrackingStatus> statuses) throws TrackerException {
		Map<Path, TrackingStatus> result = new HashMap<>();

		for(TrackingStatus status : statuses) {
			if(hasFilesForStatus(status)) {
				for(Path file : getFilesForStatus(status)) {
					result.put(file, status);
				}
			}
		}

		return result;
	}

	/**
	 * Apply the given {@code action} to all the specified {@code files} and
	 * returns all the files for which this wasn't successful.
	 *
	 * @param files
	 * @param action
	 * @return
	 */
	Set<Path> applyTrackingAction(Set<Path> files, TrackingAction action) throws TrackerException;

	//TODO javadocs
	TrackingStatus getStatusForFile(Path file) throws TrackerException;

	default Map<Path, TrackingStatus> getSatusForFiles(Set<Path> files) throws TrackerException {
		Map<Path, TrackingStatus> result = new HashMap<>();
		for(Path file : files) {
			result.put(file, getStatusForFile(file));
		}
		return result;
	}

	/**
	 * For every type of {@code TrackingStatus} returns all the files that got
	 * added for it.
	 *
	 * @param files
	 * @return
	 * @throws TrackerException
	 */
	default EnumMap<TrackingStatus, Set<Path>> getStatusBreakdown(Set<Path> files) throws TrackerException {
		EnumMap<TrackingStatus, Set<Path>> result = new EnumMap<>(TrackingStatus.class);

		for(Path file : files) {
			TrackingStatus status = getStatusForFile(file);

			Set<Path> buffer = result.get(status);
			if(buffer==null) {
				buffer = new HashSet<>();
				result.put(status, buffer);
			}
			buffer.add(file);
		}

		for(TrackingStatus status : TrackingStatus.values()) {
			result.putIfAbsent(status, Collections.emptySet());
		}

		return result;
	}

	void addTrackerListener(TrackerListener listener);

	void removeTrackerListener(TrackerListener listener);
}

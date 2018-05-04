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
package bwfdm.replaydh.ui.core;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.io.LocalFileObject;
import bwfdm.replaydh.ui.GuiUtils;

/**
 * @author Markus Gärtner
 *
 */
public class FileResolutionWorker extends SwingWorker<Boolean, LocalFileObject> {

	private static final Logger log = LoggerFactory.getLogger(FileResolutionWorker.class);

	private final RDHEnvironment environment;
	private final List<Path> files = new ArrayList<>();
	private final List<LocalFileObject> fileObjects = new ArrayList<>();

	public FileResolutionWorker(RDHEnvironment environment, Collection<Path> files) {
		this.environment = requireNonNull(environment);
		requireNonNull(files);
		checkArgument("List of files must not be empty", !files.isEmpty());

		this.files.addAll(files);
	}

	/**
	 * @return the fileObjects
	 */
	public List<LocalFileObject> getFileObjects() {
		return fileObjects;
	}

	/**
	 * @return the files
	 */
	public List<Path> getFiles() {
		return files;
	}

	/**
	 * Process all supplied files by computing checksum and
	 * trying to resolve them against existing resources.
	 */
	@Override
	protected Boolean doInBackground() throws Exception {
		for(Path file : files) {
			LocalFileObject fileObject = new LocalFileObject(file);
			LocalFileObject.ensureOrRefreshResource(fileObject, environment);

			fileObjects.add(fileObject);
		}

		return Boolean.TRUE;
	}

	@Override
	protected void done() {

		boolean error = false;

		try {
			get();
		} catch (InterruptedException e) {
			// this one is ok, we allow canceling stuff
			return;
		} catch (ExecutionException e) {
			log.error("Failed to resolve one or more file resources", e.getCause());
			GuiUtils.showError(null, "replaydh.panels.fileCache.fileResolutionError");
			GuiUtils.beep();
			error = true;
		} finally {
			finished(error);
		}
	}

	/**
	 * Hook to perform maintenance work after the background process is done.
	 */
	protected void finished(boolean finishedWithErrors) {
		// for subclasses
	}
}

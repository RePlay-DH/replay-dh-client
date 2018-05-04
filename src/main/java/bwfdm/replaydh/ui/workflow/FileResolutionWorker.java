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
package bwfdm.replaydh.ui.workflow;

import static java.util.Objects.requireNonNull;

import java.util.Set;

import javax.swing.SwingWorker;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.io.LocalFileObject;
import bwfdm.replaydh.workflow.resolver.IdentifiableResolver;

public class FileResolutionWorker extends SwingWorker<Integer, LocalFileObject> {

	protected final RDHEnvironment environment;
	protected final Set<LocalFileObject> fileObjects;

	public FileResolutionWorker(RDHEnvironment environment, Set<LocalFileObject> fileObjects) {
		this.environment = requireNonNull(environment);
		this.fileObjects = requireNonNull(fileObjects);
	}

	/**
	 * @see javax.swing.SwingWorker#doInBackground()
	 */
	@Override
	protected Integer doInBackground() throws Exception {

		final IdentifiableResolver resolver = environment.getClient().getResourceResolver();

		int refreshedFileCount = 0;

		resolver.lock();
		try {
			if(Thread.interrupted())
				throw new InterruptedException();

			for(LocalFileObject fileObject : fileObjects) {
				if(LocalFileObject.ensureOrRefreshRecord(fileObject, environment)) {
					refreshedFileCount++;
					publish(fileObject);
				}
			}
		} finally {
			resolver.unlock();
		}

		return refreshedFileCount;
	}
}

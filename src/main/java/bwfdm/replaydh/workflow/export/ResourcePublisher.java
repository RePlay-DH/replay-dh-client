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
package bwfdm.replaydh.workflow.export;

import java.io.IOException;

/**
 * Models the ability to publish parts of a workflow or resources that are
 * part of it in the public domain. Due to the usually asynchronous nature
 * of such processes (e.g. when involving review stages) it is conceptually
 * not feasible to handle the entire process lifecycle for this type of
 * action from within the client. This interface therefore makes no detailed
 * assumption on where in the publication process responsibility is shifted
 * to an external entity.
 * Implementations are free to use whatever GUI components they require and
 * to forward the user to an external service such as the website of the
 * underlying publication platform or institutional repository as soon as
 * they deem necessary.
 *
 * @author Markus Gärtner
 *
 */
public interface ResourcePublisher {

	/**
	 * Initiates the publication process. This method is guaranteed to
	 * be called on a background thread.
	 *
	 * @param exportInfo all the information needed for the export
	 * operation bundled into a single object.
	 */
	public void publish(WorkflowExportInfo exportInfo) throws IOException, ExportException, InterruptedException;
}

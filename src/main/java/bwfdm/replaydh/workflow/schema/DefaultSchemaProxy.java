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
package bwfdm.replaydh.workflow.schema;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.io.resources.IOResource;
import bwfdm.replaydh.io.resources.ReadOnlyURLResource;
import bwfdm.replaydh.utils.Lazy;

/**
 * Package private proxy to store the global default
 * workflow schema instance.
 *
 * @author Markus Gärtner
 *
 */
final class DefaultSchemaProxy {

	private DefaultSchemaProxy() {
		throw new UnsupportedOperationException("No instantiation!");
	}

	private static final Lazy<WorkflowSchema> defaultSchema = Lazy.create(DefaultSchemaProxy::loadDefaultSchema, true);

	private static WorkflowSchema loadDefaultSchema() {
		IOResource resource = new ReadOnlyURLResource(WorkflowSchema.class.getResource("default-schema.xml"));
		try {
			resource.prepare();
		} catch (IOException e) {
			throw new RDHException("Failed to prepare resource for internald efault workflow schema", e);
		}

		try {
			return WorkflowSchemaXml.readSchema(resource);
		} catch (ExecutionException e) {
			throw new RDHException("Failed to load internal default workflow schema", e.getCause());
		}
	}

	static WorkflowSchema getDefaultSchema() {
		return defaultSchema.value();
	}
}

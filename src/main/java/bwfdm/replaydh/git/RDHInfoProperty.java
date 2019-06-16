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
package bwfdm.replaydh.git;

import bwfdm.replaydh.core.RDHClient;
import bwfdm.replaydh.utils.annotation.Experimental;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowVersion;
import bwfdm.replaydh.workflow.schema.WorkflowSchemaManager;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * Collection of properties that are used to store content in a
 * workflow's info file in the respective Git repository.
 *
 * @author Markus Gärtner
 *
 */
public class RDHInfoProperty {

	/**
	 * Id of the {@link WorkflowSchema} to be used for
	 * a given {@link Workflow}.
	 *
	 * The clients current {@link WorkflowSchemaManager} (obtained
	 * via {@link RDHClient#getWorkflowSchemaManager()}) will be
	 * used to {@link WorkflowSchemaManager#lookupSchema(String) find}
	 * the actual schema instance.
	 * <p>
	 * Associated property type is {@link String}
	 */
	public static final String SCHEMA_ID = "workflow.schema";

	/**
	 * Version of the API that was used in the workflow
	 * documentation.
	 */
	public static final String API_VERSION = "api.version";

	/**
	 * Version of the {@link JGitAdapter} expressed as label
	 * for a {@link WorkflowVersion} instance.
	 * <p>
	 * Associated property type is {@link String}
	 */
	public static final String ADAPTER_VERSION = "adapter.version";

	/**
	 * Title of the workflow as defined by the user.
	 * <p>
	 * Associated property type is {@link String}
	 */
	public static final String TITLE = "workflow.title";

	/**
	 * Description of the workflow as defined by the user.
	 * <p>
	 * Associated property type is {@link String}
	 */
	public static final String DESCRIPTION = "workflow.description";

	/**
	 * Property holding the numerical part of the next id
	 * to be assigned to a new workflow step.
	 * <p>
	 * Associated property type is {@link Integer}
	 */
	public static final String NEXT_STEP_ID = "workflow.nextStepId";

	/**
	 * Flag to indicate whether or not the client should use an
	 * internal ignore file ($GIT_DIR/info/exclude) instead of the
	 * default .gitignore file in the repository root.
	 */
	@Experimental
	public static final String USE_INTERNAL_IGNORE_FILE = "adapter.useInternalIgnoreFile";
}

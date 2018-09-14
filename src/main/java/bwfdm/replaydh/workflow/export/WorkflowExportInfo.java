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

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.util.Set;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.io.resources.IOResource;
import bwfdm.replaydh.io.resources.ResourceProvider;
import bwfdm.replaydh.utils.Options;
import bwfdm.replaydh.utils.RDHUtils;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowStep;

/**
 * Wraps all the basic information needed for an export
 * process into a single object.
 *
 * @author Markus Gärtner
 *
 */
public class WorkflowExportInfo {

	public static Builder newExportBuilder() {
		return new Builder(true, false);
	}

	public static Builder newPublicationBuilder() {
		return new Builder(false, false);
	}

	private final boolean export;

	private RDHEnvironment environment;

	private WorkflowScope workflowScope;
	private ObjectScope objectScope;
	private Mode mode;
	private Type type;

	private Workflow workflow;
	private WorkflowStep sourceStep, targetStep;
	private Set<WorkflowStep> steps;

	private ResourceProvider resourceProvider;
	private IOResource outputResource;

	private Set<Resource> resources;

	private Charset encoding;

	private final Options options = new Options();

	private WorkflowExportInfo(boolean export) {
		this.export = export;
	}

	private void checkExport(boolean expected) {
		if(export!=expected)
			throw new IllegalStateException("Operation not allowed outside of "
					+(expected ? "'export'" : "'publication'")+" mode");
	}

	public boolean isExport() {
		return export;
	}

	public RDHEnvironment getEnvironment() {
		return environment;
	}

	public WorkflowScope getWorkflowScope() {
		return workflowScope;
	}

	public ObjectScope getObjectScope() {
		return objectScope;
	}

	public Mode getMode() {
		return mode;
	}

	public Type getType() {
		return type;
	}

	public Workflow getWorkflow() {
		return workflow;
	}

	public WorkflowStep getSourceStep() {
		checkState("Source step only defined for PATH scope", getWorkflowScope()==WorkflowScope.PATH);
		return sourceStep;
	}

	public WorkflowStep getTargetStep() {
		return targetStep;
	}

	public Set<WorkflowStep> getSteps() {
		return steps;
	}

	public ResourceProvider getResourceProvider() {
		checkState("Resource provider only available for folder mode", getMode()==Mode.FOLDER);
		return resourceProvider;
	}

	public IOResource getOutputResource() {
		return outputResource;
	}

	public Set<Resource> getResources() {
		return resources;
	}

	public Charset getEncoding() {
		return encoding;
	}

	/**
	 * Helper method to create a writer for writing characters to the single target resource.
	 * Client code should use this method with the try-with-resource statement:
	 *
	 * <pre>
	 * try(Writer writer = createWriter()) {
	 *
	 *   writer.write(myContent);
	 *   ...
	 * }
	 * </pre>
	 *
	 * @return
	 * @throws IOException
	 */
	public Writer createWriter() throws IOException {
		checkState("Cannot create writer for folder mode", getMode()==Mode.FILE);
		return Channels.newWriter(getOutputResource().getWriteChannel(true), getEncoding().newEncoder(), -1);
	}

	/**
	 * Helper method to create a stream for writing bytes to the single target resource.
	 * Client code should use this method with the try-with-resource statement:
	 *
	 * <pre>
	 * try(OutputStream stream = createOutputStream()) {
	 *
	 *   stream.write(myContent);
	 *   ...
	 * }
	 * </pre>
	 *
	 * @param charset
	 * @return
	 * @throws IOException
	 */
	public OutputStream createOutputStream() throws IOException {
		checkState("Cannot create writer for folder mode", getMode()==Mode.FILE);
		return Channels.newOutputStream(getOutputResource().getWriteChannel(true));
	}

	// Configuration support

	/**
	 * Returns an {@link Options} instance that can be freely used by
	 * exporter implementations and utility code to store additional
	 * information for an ongoing export process.
	 */
	public Options getOptions() {
		return options;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(getClass().getSimpleName()).append('@').append(hashCode()).append('[');
		sb.append("isExport=").append(export);
		sb.append(", workflow=").append(workflow.getTitle());
		sb.append(", type=").append(type);
		sb.append(", workflowScope=").append(workflowScope);
		sb.append(", objectScope=").append(objectScope);
		sb.append(", mode=").append(mode);
		sb.append(", encoding=").append(encoding);
		sb.append(", sourceStep=").append(sourceStep==null ? null : sourceStep.getId());
		sb.append(", targetStep=").append(targetStep==null ? null : targetStep.getId());
		sb.append(", stepCount=").append(steps==null ? 0 : steps.size());
		sb.append(", resourceCount=").append(resources==null ? 0 : resources.size());
		sb.append(", outputResource=").append(outputResource==null ? null : outputResource.getPath());
		sb.append(", resourceProvider=").append(resourceProvider);
		sb.append(", options=").append(options);

		sb.append(']');

		return sb.toString();
	}

	//TODO

	public static enum WorkflowScope {
		/**
		 * Export data for the entire {@link WorkflowExportInfo#getWorkflow() workflow}.
		 */
		WORKFLOW,
		/**
		 * Export data for a single {@link WorkflowExportInfo#getTargetStep() step}.
		 */
		STEP,
		/**
		 * Export data for a path from {@link WorkflowExportInfo#getSourceStep() source}
		 * to {@link WorkflowExportInfo#getTargetStep() target}. All the steps including
		 * the two aforementioned ones are available via {@link WorkflowExportInfo#getSteps()}.
		 */
		PATH,
		/**
		 * Export data for a collection of {@link WorkflowExportInfo#getSteps() steps}.
		 * Note that there is no assumed order in that collection and neither an explicit
		 * {@link WorkflowExportInfo#getSourceStep() source} nor
		 * {@link WorkflowExportInfo#getTargetStep() target} is available for this scenario.
		 */
		PART,

		/**
		 * Special value not usable for regular scope definition but to signal that an
		 * exporter can cover all possible scope values.
		 */
		ALL,
		;
	}

	public static enum ObjectScope {
		INPUT,
		OUTPUT,
		PERSON,
		TOOL,

		/**
		 * Placeholder to signal that the content of the workspace as seen at a certain
		 * step should be exported.
		 */
		WORKSPACE,

		/**
		 * Special value not usable for regular scope definition but to signal that an
		 * exporter can cover all possible scope values.
		 */
		ALL,
		;
	}

	public static enum Mode {
		/**
		 * Output goes to a single "file". Note that the
		 * {@link WorkflowExportInfo#getOutputResource() output resource} can be a real file or
		 * just a temporary virtual storage that will later be displayed as text for the user
		 * to copy.
		 */
		FILE,
		/**
		 * Output goes into a specific folder on the file system.
		 * This mode is only used for exporters that create a collection of different output
		 * files or are able to export multiple resources/steps at once.
		 */
		FOLDER,
		;
	}

	public static enum Type {
		/**
		 * Export process or object metadata describing individual steps or resources.
		 */
		METADATA,

		/**
		 * Export physical resources which were part of the workflow.
		 * Only resources available locally via a file link can be exported
		 * this way.
		 */
		OBJECT,
		;
	}

	public static class Builder {

		protected final WorkflowExportInfo info;

		private final boolean allowPermanentChange;

		protected Builder(boolean export, boolean allowPermanentChange) {
			info = new WorkflowExportInfo(export);
			this.allowPermanentChange = allowPermanentChange;
		}

		private void checkState(String message, boolean condition) {
			if(allowPermanentChange) {
				return;
			}

			RDHUtils.checkState(message, condition);
		}

		public Builder environment(RDHEnvironment environment) {
			checkState("Environment already set", info.environment==null);
			requireNonNull(environment);

			info.environment = environment;

			return this;
		}

		public Builder workflowScope(WorkflowScope workflowScope) {
			checkState("Workflow scope already set", info.workflowScope==null);
			requireNonNull(workflowScope);
			checkArgument("Scope constant ALL not allowed for regular info construction", workflowScope!=WorkflowScope.ALL);

			info.workflowScope = workflowScope;

			return this;
		}

		public Builder objectScope(ObjectScope objectScope) {
			checkState("Object scope already set", info.objectScope==null);
			requireNonNull(objectScope);
			checkArgument("Scope constant ALL not allowed for regular info construction", objectScope!=ObjectScope.ALL);

			info.objectScope = objectScope;

			return this;
		}

		public Builder mode(Mode mode) {
			info.checkExport(true);
			checkState("Mode already set", info.mode==null);
			requireNonNull(mode);

			info.mode = mode;

			return this;
		}

		public Builder type(Type type) {
			checkState("Type already set", info.type==null);
			requireNonNull(type);

			info.type = type;

			return this;
		}

		public Builder workflow(Workflow workflow) {
			checkState("Workflow already set", info.workflow==null);
			requireNonNull(workflow);

			info.workflow = workflow;

			return this;
		}

		public Builder sourceStep(WorkflowStep sourceStep) {
			checkState("Source step already set", info.sourceStep==null);
			requireNonNull(sourceStep);

			info.sourceStep = sourceStep;

			return this;
		}

		public Builder targetStep(WorkflowStep targetStep) {
			checkState("Target step already set", info.targetStep==null);
			requireNonNull(targetStep);

			info.targetStep = targetStep;

			return this;
		}

		public Builder steps(Set<WorkflowStep> steps) {
			checkState("Workflow steps already set", info.steps==null);
			requireNonNull(steps);

			info.steps = steps;

			return this;
		}

		public Builder resourceProvider(ResourceProvider resourceProvider) {
			info.checkExport(true);
			checkState("Resource provider already set", info.resourceProvider==null);
			requireNonNull(resourceProvider);

			info.resourceProvider = resourceProvider;

			return this;
		}

		public Builder outputResource(IOResource outputResource) {
			info.checkExport(true);
			checkState("Output resource already set", info.outputResource==null);
			requireNonNull(outputResource);

			info.outputResource = outputResource;

			return this;
		}

		public Builder resources(Set<Resource> resources) {
			checkState("Resources already set", info.resources==null);
			requireNonNull(resources);

			info.resources = resources;

			return this;
		}

		public Builder encoding(Charset encoding) {
			info.checkExport(true);
			checkState("Encoding already set", info.encoding==null);
			requireNonNull(encoding);

			info.encoding = encoding;

			return this;
		}

		private void validate() {
			checkState("Environment not set", info.environment!=null);

			checkState("Workflow scope not set", info.workflowScope!=null);
			checkState("Object scope not set", info.objectScope!=null);

			if(info.isExport()) {
				checkState("Mode not set", info.mode!=null);
				checkState("Type not set", info.type!=null);
				checkState("Encoding not set", info.encoding!=null);
				checkState("Output resource not set", info.outputResource!=null);
			}

			checkState("Workflow not set", info.workflow!=null);
			checkState("Steps not set", info.steps!=null);

			if(info.workflowScope==WorkflowScope.PATH) {
				checkState("Source step not set", info.sourceStep!=null);
			}
			if(info.workflowScope==WorkflowScope.PATH || info.workflowScope==WorkflowScope.STEP) {
				checkState("Target step not set", info.targetStep!=null);
			}
			if(info.mode==Mode.FOLDER) {
				checkState("Resource provider not set", info.resourceProvider!=null);
			}
			if(info.isExport() && info.type!=Type.METADATA) {
				checkState("Resources not set", info.resources!=null);
			}

		}

		public WorkflowExportInfo build() {
			validate();

			return info;
		}
	}
}

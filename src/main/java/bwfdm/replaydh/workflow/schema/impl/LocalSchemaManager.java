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
package bwfdm.replaydh.workflow.schema.impl;

import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.io.resources.FileResource;
import bwfdm.replaydh.io.resources.IOResource;
import bwfdm.replaydh.utils.AbstractSchemaManager;
import bwfdm.replaydh.utils.AccessMode;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;
import bwfdm.replaydh.workflow.schema.WorkflowSchemaManager;
import bwfdm.replaydh.workflow.schema.WorkflowSchemaXml;

/**
 * @author Markus Gärtner
 *
 */
public class LocalSchemaManager extends AbstractSchemaManager<WorkflowSchema> implements WorkflowSchemaManager {

	private static final Logger log = LoggerFactory.getLogger(LocalSchemaManager.class);

	public static Builder newBuilder() {
		return new Builder();
	}

	/**
	 * Folders that will be scanned for schema files
	 */
	private final Set<Path> folders = new HashSet<>();

	/**
	 * Flag to indicate this manager should also consider the
	 * shared default schema as backup for any method that needs
	 * to lookup a schema instance.
	 */
	private final boolean includeSharedDefaultSchema;

	protected LocalSchemaManager(Builder builder) {
		requireNonNull(builder);

		includeSharedDefaultSchema = builder.isIncludeSharedDefaultSchema();
		folders.addAll(builder.getFolders());
	}

	/**
	 * @throws RDHLifecycleException
	 * @see bwfdm.replaydh.core.AbstractRDHTool#start(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public boolean start(RDHEnvironment environment) throws RDHLifecycleException {
		if(!super.start(environment)) {
			return false;
		}

		// Scan all the specified folders for additional schemas
		synchronized (lock) {
			for(Path folder : folders) {
				if(!Files.exists(folder, LinkOption.NOFOLLOW_LINKS)) {
					log.warn("Designated schema folder does not exist: {}", folder);
					continue;
				}

				scanFolder(folder);

				setDefaultSchema(WorkflowSchema.getDefaultSchema());
			}
		}

		return true;
	}

	private void scanFolder(Path folder) {
		// We assume all XML files in the target folder to be schema definitions
		try(DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.xml")) {
			for(Path file : stream) {
				scanFile(file);
			}
		} catch (IOException e) {
			log.error("Failed to scan folder: {}", folder, e);
		}
	}

	private void scanFile(Path file) {
		IOResource resource = new FileResource(file, AccessMode.READ);
		WorkflowSchema schema = null;
		try {
			schema = WorkflowSchemaXml.readSchema(resource);
		} catch (ExecutionException e) {
			log.error("Failed to read schema file: {}", file, e.getCause());
		}

		if(schema!=null) {
			addSchema(schema);
		}
	}

	protected boolean includeSharedDefaultSchema() {
		return includeSharedDefaultSchema;
	}

	/**
	 * @see bwfdm.replaydh.utils.AbstractSchemaManager#getFallbackSchema()
	 */
	@Override
	protected WorkflowSchema getFallbackSchema() {
		return includeSharedDefaultSchema() ? WorkflowSchema.getDefaultSchema() : null;
	}

	private static final boolean DEFAULT_INCLUDE_SHARED_DEFAULT_SCHEMA = true;

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	public static class Builder {

		private final Set<Path> folders = new HashSet<>();

		private Boolean includeSharedDefaultSchema;

		private Builder() {
			// prevent external instantiation
		}

		public Builder addFolder(Path folder) {
			folders.add(requireNonNull(folder));
			return this;
		}

		public Builder removeFolder(Path folder) {
			folders.remove(requireNonNull(folder));
			return this;
		}

		public Set<Path> getFolders() {
			return Collections.unmodifiableSet(folders);
		}

		public Builder setIncludeSharedDefaultSchema(boolean includeSharedDefaultSchema) {
			checkState("Flag 'includeSharedDefaultSchema' already set", this.includeSharedDefaultSchema==null);

			this.includeSharedDefaultSchema = includeSharedDefaultSchema==DEFAULT_INCLUDE_SHARED_DEFAULT_SCHEMA ?
					null : Boolean.valueOf(includeSharedDefaultSchema);
			return this;
		}

		public boolean isIncludeSharedDefaultSchema() {
			return includeSharedDefaultSchema==null ? DEFAULT_INCLUDE_SHARED_DEFAULT_SCHEMA
					: includeSharedDefaultSchema.booleanValue();
		}

		protected void validate() {

			// In case the default schema is not included we need external definitions
			if(!isIncludeSharedDefaultSchema()) {
				checkState("No folders defined", !folders.isEmpty());
			}
		}

		public LocalSchemaManager build() {
			validate();

			return new LocalSchemaManager(this);
		}
	}
}

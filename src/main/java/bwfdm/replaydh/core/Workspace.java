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
package bwfdm.replaydh.core;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * Encapsulates all data related to a workspace instance.
 *
 * @author Markus Gärtner
 *
 */
public class Workspace {

	private final Path folder;

	private final WorkflowSchema schema;

	private final Properties properties = new Properties();

	Workspace(Path folder, WorkflowSchema schema) {
		this.folder = requireNonNull(folder).toAbsolutePath();
		this.schema = requireNonNull(schema);
	}

	/**
	 * @return the folder
	 */
	public Path getFolder() {
		return folder;
	}

	/**
	 * @return the schema
	 */
	public WorkflowSchema getSchema() {
		return schema;
	}

	public void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}

	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	public String getProperty(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Workspace[folder=%s schemaId=%s]", folder, schema.getId());
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(folder, schema.getId());
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		} else if(obj instanceof Workspace) {
			Workspace other = (Workspace) obj;
			return folder.equals(other.folder)
					&& schema.getId().equals(other.schema.getId());
		}

		return false;
	}
}

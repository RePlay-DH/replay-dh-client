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
package bwfdm.replaydh.workflow.impl;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.Set;

import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Tool;

public class DefaultTool extends DefaultResource implements Tool {

	public static DefaultTool blankTool() {
		return new DefaultTool(false);
	}

	public static DefaultTool uniqueTool() {
		return new DefaultTool(true);
	}

	public static DefaultTool withSettings(String parameters, String environment) {
		DefaultTool tool = uniqueTool();
		tool.setParameters(parameters);
		tool.setEnvironment(environment);
		return tool;
	}

	public static DefaultTool withIdentifiers(Set<Identifier> identifiers) {
		DefaultTool resource = uniqueTool();
		resource.addIdentifiers(identifiers);
		return resource;
	}

	private String parameters;
	private String environment;

	protected DefaultTool(boolean autoCreateSystemId) {
		super(autoCreateSystemId);
	}

	/**
	 * @see bwfdm.replaydh.workflow.impl.DefaultResource#getType()
	 */
	@Override
	public Type getType() {
		return Type.TOOL;
	}

	/**
	 * @see bwfdm.replaydh.workflow.Tool#getParameters()
	 */
	@Override
	public String getParameters() {
		return parameters;
	}

	/**
	 * @see bwfdm.replaydh.workflow.Tool#getEnvironment()
	 */
	@Override
	public String getEnvironment() {
		return environment;
	}

	/**
	 * @param parameters the parameters to set
	 */
	@Override
	public void setParameters(String parameters) {
		requireNonNull(parameters);
		this.parameters = parameters;
	}

	/**
	 * @param environment the environment to set
	 */
	@Override
	public void setEnvironment(String environment) {
		requireNonNull(environment);
		this.environment = environment;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return hash(getResourceType(), environment, parameters, identifiers());
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		} else if(obj instanceof Tool) {
			Tool other = (Tool) obj;

			return Objects.equals(parameters, other.getParameters())
					&& Objects.equals(environment, other.getEnvironment())
					&& super.equals(other);
		}
		return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Tool@[identifiers=%s type=%s environment='%s' parameters='%S']", identifiers(), getResourceType(), environment, parameters);
	}
}

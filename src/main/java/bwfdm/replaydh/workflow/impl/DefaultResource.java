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

import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Resource;

public class DefaultResource extends AbstractIdentifiable implements Resource {

	public static DefaultResource blankResource() {
		return new DefaultResource(false);
	}

	public static DefaultResource uniqueResource() {
		return new DefaultResource(true);
	}

	public static DefaultResource withResourceType(String resourceType) {
		DefaultResource resource = uniqueResource();
		resource.setResourceType(resourceType);
		return resource;
	}

	public static DefaultResource withIdentifiers(Set<Identifier> identifiers) {
		DefaultResource resource = uniqueResource();
		resource.addIdentifiers(identifiers);
		return resource;
	}

	public static DefaultResource copyResource(Resource source) {
		DefaultResource resource = new DefaultResource(false);
		resource.copyFrom(source);
		return resource;
	}

	private String resourceType;

	protected DefaultResource(boolean autoCreateSystemId) {
		super(autoCreateSystemId);
	}

	/**
	 * @see bwfdm.replaydh.workflow.Identifiable#getType()
	 */
	@Override
	public Type getType() {
		return Type.RESOURCE;
	}

	@Override
	public void setResourceType(String resourceType) {
		requireNonNull(resourceType);

		this.resourceType = resourceType;
	}

	/**
	 * @see bwfdm.replaydh.workflow.Resource#getResourceType()
	 */
	@Override
	public String getResourceType() {
		return resourceType;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return hash(resourceType, identifiers());
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof Resource) {
			Resource other = (Resource) obj;

			return Objects.equals(resourceType, other.getResourceType())
					&& super.equals((Identifiable)other);
		}
		return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return String.format("Resource@[identifiers=%s, type=%s]", identifiers(), resourceType);
	}

    /**
     * {@inheritDoc}
     *
     * In addition to the fields copied by the super method, this implementation
     * also copies over the {@link #getResourceType() resource type} if available.
     *
     * @see bwfdm.replaydh.workflow.Identifiable#copyFrom(bwfdm.replaydh.workflow.Identifiable)
     */
    @Override
	public void copyFrom(Identifiable source) {
    	super.copyFrom(source);

    	if(source instanceof Resource) {
    		String resourceType = ((Resource)source).getResourceType();
    		if(resourceType!=null) {
    			setResourceType(resourceType);
    		}
    	}
    }
}

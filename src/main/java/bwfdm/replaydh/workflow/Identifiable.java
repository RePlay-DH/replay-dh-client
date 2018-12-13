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
package bwfdm.replaydh.workflow;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import bwfdm.replaydh.core.RDHClient;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.utils.LazyCollection;
import bwfdm.replaydh.utils.Mutable;
import bwfdm.replaydh.workflow.schema.IdentifierType;
import bwfdm.replaydh.workflow.schema.IdentifierType.Uniqueness;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public interface Identifiable {

	/**
	 * Returns the unique ID assigned to this identifiable by the
	 * framework. Since this ID is meant to persist across (de)serialization
	 * it can be used for mapping to external resources such as
	 * metadata in a repository.
	 * <p>
	 * Note that this ID is not related to the content of a resource,
	 * but rather a means to consistently identify the same object across
	 * time even when other (more direct) mechanisms are not available.
	 * <p>
	 * Implementations should <b>not</b> make use of this ID when checking for
	 * {@link #equals(Object) equality} of two identifiable objects.
	 *
	 * @return
	 */
	UUID getSystemId();

	String getDescription();

	void setDescription(String description);

	void forEachIdentifier(Consumer<? super Identifier> action);

    /**
     * Returns a {@link Set} of identifiers associated with this entity.
     * <p>
     * The returned set is not backed by this identifier and
     * client code can freely modify it in any way desired.
     *
     * @return
     */
    default Set<Identifier> getIdentifiers() {
    	LazyCollection<Identifier> result = LazyCollection.lazySet();
    	forEachIdentifier(result);
    	return result.getAsSet();
    }

    default boolean hasIdentifiers() {
    	return !getIdentifiers().isEmpty();
    }

    default Set<String> getIdentifierTypes() {
    	LazyCollection<String> result = LazyCollection.lazySet();
    	forEachIdentifier(id -> result.add(id.getType().getLabel()));
    	return result.getAsSet();
    }

    /**
     * Adds a new {@link Identifier} and fails if it is either {@code null}
     * or already contained in the set of identifiers for this object.
     *
     * @param identifier
     */
    void addIdentifier(Identifier identifier);

    default void addIdentifiers(Collection<? extends Identifier> identifiers) {
    	for(Identifier identifier : identifiers) {
    		addIdentifier(identifier);
    	}
    }

    /**
     * Adds a new {@link Identifier}, failing if it is either {@code null} or
     * has not been added to this object before.
     *
     * @param identifier
     */
    void removeIdentifier(Identifier identifier);
    void removeIdentifier(String type);

    boolean hasIdentifier(Identifier identifier);

    /**
     * Performs a lookup to find an {@link Identifier} associated with
     * this identifiable with the specified {@code type}.
     *
     * @param type
     * @return
     */
    Identifier getIdentifier(String type);

    /**
     * Performs a lookup to find a  {@link Identifier} associated
     * with a type in the given list. The first such identifier
     * that can be found will be returned.
     *
     * @param types
     * @return
     *
     * @see #getIdentifier(String)
     */
    default Identifier getIdentifier(String...types) {
    	Identifier identifier = null;

    	for(String type : types) {
    		if((identifier = getIdentifier(type)) != null) {
    			break;
    		}
    	}

    	return identifier;
    }

    default Identifier getIdentifier(IdentifierType type) {
    	return getIdentifier(type.getLabel());
    }

    /**
     * Returns the identifier with the highest associated {@link Uniqueness}.
     * If more than one identifier are competing for highest uniqueness, the
     * one that is visited first will be returned.
     * <p>
     * In case this identifiable has no identifiers assigned to it, this
     * method returns {@code null}.
     *
     * @return
     */
    public static Identifier getBestIdentifier(Identifiable identifiable, WorkflowSchema schema) {

    	if(schema!=null) {
    		IdentifierType defaultType = null;
    		switch (identifiable.getType()) {
			case RESOURCE:
			case TOOL:
				defaultType = schema.getResourceIdentifierSchema().getDefaultIdentifierType();
				break;

			case PERSON:
				defaultType = schema.getPersonIdentifierSchema().getDefaultIdentifierType();
				break;

			default:
				break;
			}

    		Identifier identifier = defaultType==null ? null : identifiable.getIdentifier(defaultType);
    		if(identifier!=null) {
    			return identifier;
    		}
    	}

    	/*
    	 * Fallback in case none of the default types was present:
    	 *
    	 * Search for the most unique identifier we can get.
    	 * Use a mutable reference wrapper, so we delegate to the
    	 * forEach-method instead of creating a new collection which
    	 * is the case when using getIdentifiers()!
    	 */
    	Mutable<Identifier> result = new Mutable.MutableObject<>();

    	identifiable.forEachIdentifier(identifier -> {
    		if(result.isEmpty() || identifier.getType().isStrongerThan(result.get().getType())) {
    			result.set(identifier);
    		}
    	});

    	return result.get();
    }

    public static Identifier getBestIdentifier(Identifiable identifiable) {
    	WorkflowSchema schema = null;

    	// If client is initialized, try to use the current schema
    	if(RDHClient.hasClient()) {
    		//FIXME introduces weird dependency on the currently active schema, do we want this?
    		schema = RDHClient.client().getSchemaManager().getDefaultSchema();
    	}

    	return getBestIdentifier(identifiable, schema);
    }

    /**
     * Returns the type of this identifiable, i.e. the kind of resource
     * identified by it.
     *
     * @return
     */
    Type getType();

    /**
     * Copies content from the given {@code source} identifiable.
     * The basic version of this method only copies the {@link #getDescription() description}
     * and available {@link #getIdentifiers() identifiers}.
     *
     * @param source
     */
    void copyFrom(Identifiable source);

    /**
     * The physical type an identifiable can represent.
     *
     * @author Markus Gärtner
     *
     */
    public enum Type {
    	RESOURCE("resource"),
    	PERSON("person"),
    	TOOL("tool"),
    	;

    	private Type(String label) {
			this.label = label;
		}

		private final String label;

		public String getLabel() {
			return label;
		}

		public String getDisplayLabel() {
			return ResourceManager.getInstance().get("replaydh.labels."+label);
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return getDisplayLabel();
		}

		public static Type parseType(String s) {
			switch (s.toLowerCase()) {
			case "resource": return RESOURCE;
			case "person": return PERSON;
			case "tool": return TOOL;

			default:
				throw new IllegalArgumentException("Unknown type label: "+s);
			}
		}
    }

    /**
     * The actual role an identifiable is assigned within the context
     * of a workflow step.
     *
     * @author Markus Gärtner
     *
     */
	public enum Role {
		PERSON,
		TOOL,
		INPUT,
		OUTPUT,
		;

		/**
		 * @return
		 */
		public Type asIdentifiableType() {
			switch (this) {
			case PERSON:
				return Type.PERSON;

			case INPUT:
			case OUTPUT:
				return Type.RESOURCE;

			case TOOL:
				return Type.TOOL;

			default:
				throw new IllegalStateException("Not a known role: "+this);
			}
		}
	}
}

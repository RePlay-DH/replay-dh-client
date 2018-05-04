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

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.nio.file.Path;

import bwfdm.replaydh.utils.Label;
import bwfdm.replaydh.utils.StringResource;
import bwfdm.replaydh.workflow.Checksum;

/**
 * @author Markus Gärtner
 *
 */
public interface IdentifierType extends Label {

	/**
	 * The id of the default identifier type that
	 * uses {@link Path} objects for identification.
	 */
	public static final String PATH = "path";

	/**
	 * The id of the default identifier type that
	 * uses {@link java.net.URL} or {@link URI} objects for
	 * identification.
	 */
	public static final String URL = "url";

	/**
	 * The id of the default identifier type that
	 * uses {@link Checksum} objects for identification.
	 */
	public static final String CHECKSUM = "checksum";

	/**
	 * The id of the default identifier type for persons.
	 */
	public static final String NAME = "name";

	/**
	 * The id of the default identifier type for
	 * versionable resources.
	 */
	public static final String NAME_VERSION = "name-version";

	public static boolean isDefaultPathIdentifierType(IdentifierType type) {
		return PATH.equals(type.getLabel());
	}

	public static boolean isDefaultURLIdentifierType(IdentifierType type) {
		return URL.equals(type.getLabel());
	}

	public static boolean isDefaultChecksumIdentifierType(IdentifierType type) {
		return CHECKSUM.equals(type.getLabel());
	}

	/**
	 * Returns an indicator for the "strength" of identifiers
	 * of this type. This defines how well they are suited
	 * for generating means of unambiguously identifying a
	 * specific resource.
	 *
	 * @return
	 */
	Uniqueness getUniqueness();

	/**
	 * Returns the schema that this identifier type
	 * originated from.
	 *
	 */
	WorkflowSchema getSchema();

	/**
	 * Returns {@code true} if this instance denotes a stronger level of
	 * uniqueness compared to {@code other}.
	 *
	 * @param other
	 * @return
	 */
	default boolean isStrongerThan(IdentifierType other) {
		return getUniqueness().compareTo(other.getUniqueness())<0;
	}

	/**
	 * Measure of how unique a given {@link IdentifierType identifier type} truly
	 * is. For identifiers that are no {@link #GLOBALLY_UNIQUE globally unique}
	 * additional contextual information can ve provided to disambiguate them to
	 * a certain level.
	 *
	 * @author Markus Gärtner
	 *
	 */
	public enum Uniqueness implements StringResource {
		/**
		 * No external disambiguation needed, identifier is
		 * unique across all instances of RePlay-DH.
		 * <p>
		 * Examples are URLs, DOI, ORCID, VLO-handle...
		 */
		GLOBALLY_UNIQUE("globally-unique"),

		/**
		 * Unique in the context of the "institute" a RePlay-DH
		 * client is configured to work in.
		 */
		ENVIRONMENT_UNIQUE("environment-unique"),

		/**
		 * Unique within a single RePlay-DH client instance.
		 * This usually denotes uniqueness in terms of the
		 * local working directory.
		 * <p>
		 * Not that this can be state-dependent, i.e. in case
		 * of local file paths when the workspace folder is
		 * changed.
		 */
		LOCALLY_UNIQUE("locally-unique"),

		/**
		 * High likelihood of the identifier being unique,
		 * but collisions <b>are</b> possibly. So really only
		 * usable as a strong indicator, not a guaranteed
		 * measure of uniqueness.
		 */
		HASH("hash"),

		/**
		 * Weak identifier, not usable for any form of
		 * disambiguation at all.
		 * <p>
		 * This is usually the case when an identifier has no
		 * a priori defined value space that could limit the level
		 * of ambiguity.
		 */
		AMBIGUOUS("ambiguous"),
		;

		private final String label;

		private Uniqueness(String label) {
			this.label = requireNonNull(label);
		}

		/**
		 * @see bwfdm.replaydh.utils.StringResource#getStringValue()
		 */
		@Override
		public String getStringValue() {
			return label;
		}

		private static final Uniqueness[] _values = values();

		public static Uniqueness parseUniqueness(String s) {
			for(Uniqueness uniqueness : _values) {
				if(uniqueness.label.equals(s)) {
					return uniqueness;
				}
			}

			throw new IllegalArgumentException("Unknown uniqueness label: "+s);
		}
	}
}

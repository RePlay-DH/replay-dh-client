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
package bwfdm.replaydh.metadata.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.utils.Label;
import bwfdm.replaydh.utils.Multiplicity;

public class DublinCoreField implements Label {

	private static final List<DublinCoreField> _values = new ArrayList<>();

	private final String label;
	private final String key;
	private final String definition;
	private final Multiplicity multiplicity;
	private final String uri;

	private DublinCoreField(String key, String label, String definition, Multiplicity multiplicity, String uri) {
		this.key = key;
		this.label = label;
		this.definition = definition;
		this.multiplicity = multiplicity;
		this.uri = uri;

		_values.add(this);
	}

	@Override
	public String getLabel() {
		return key;
	}

	@Override
	public String getName() {
		return label;
	}

	@Override
	public String getDescription() {
		return definition;
	}

	public Multiplicity getMultiplicity() {
		return multiplicity;
	}

	public String getUri() {
		return uri;
	}

	public boolean isRequired() {
		return multiplicity.getRequiredMinimum()>0;
	}

	public String getValue(MetadataRecord record) {
		return record.getEntry(key).getValue();
	}

	private static final Map<String, DublinCoreField> _keyLookup = new HashMap<>();

	private static final Set<DublinCoreField> _required;
	private static final Set<DublinCoreField> _available;

	private static final Set<String> _requiredKeys;
	private static final Set<String> _keys;

	public static DublinCoreField forKey(String key) {
		return _keyLookup.get(key);
	}

	@SuppressWarnings("unchecked")
	public static <L extends Label> Set<L> getRequiredFields() {
		return (Set<L>) _required;
	}

	@SuppressWarnings("unchecked")
	public static <L extends Label> Set<L> getAvailableFields() {
		return (Set<L>) _available;
	}

	public static Set<String> getRequiredKeys() {
		return _requiredKeys;
	}

	public static Set<String> getKeys() {
		return _keys;
	}

	public static final DublinCoreField TITLE = new DublinCoreField("title", "Title", "A name given to the resource.", Multiplicity.ONE, "http://purl.org/dc/elements/1.1/title");
	public static final DublinCoreField CREATOR = new DublinCoreField("creator", "Creator", "An entity primarily responsible for making the resource.", Multiplicity.ONE_OR_MORE, "http://purl.org/dc/elements/1.1/creator");
	public static final DublinCoreField SUBJECT = new DublinCoreField("subject", "Subject", "The topic of the resource.", Multiplicity.ONE, "http://purl.org/dc/elements/1.1/subject");
	public static final DublinCoreField DESCRIPTION = new DublinCoreField("description", "Description", "An account of the resource.", Multiplicity.ONE, "http://purl.org/dc/elements/1.1/description");
	public static final DublinCoreField PUBLISHER = new DublinCoreField("publisher", "Publisher", "An entity responsible for making the resource available.", Multiplicity.ANY, "http://purl.org/dc/elements/1.1/publisher");
	public static final DublinCoreField CONTRIBUTOR = new DublinCoreField("contributor", "Contributor", "", Multiplicity.ANY, "http://purl.org/dc/elements/1.1/contributor");
	public static final DublinCoreField DATE = new DublinCoreField("date", "Date", "A point or period of time associated with an event in the lifecycle of the resource.", Multiplicity.ONE, "http://purl.org/dc/elements/1.1/date");
	public static final DublinCoreField TYPE = new DublinCoreField("type", "Type", "The nature or genre of the resource.", Multiplicity.ONE, "http://purl.org/dc/elements/1.1/type");
	public static final DublinCoreField FORMAT = new DublinCoreField("format", "Format", "The file format, physical medium, or dimensions of the resource.", Multiplicity.NONE_OR_ONE, "http://purl.org/dc/elements/1.1/format");
	public static final DublinCoreField IDENTIFIER = new DublinCoreField("identifier", "Identifier", "An unambiguous reference to the resource within a given context.", Multiplicity.ONE, "http://purl.org/dc/elements/1.1/identifier");
	public static final DublinCoreField SOURCE = new DublinCoreField("source", "Source", "A related resource from which the described resource is derived.", Multiplicity.ANY, "http://purl.org/dc/elements/1.1/source");
	public static final DublinCoreField LANGUAGE = new DublinCoreField("language", "Language", "A language of the resource.", Multiplicity.ANY, "http://purl.org/dc/elements/1.1/language");
	public static final DublinCoreField RELATION = new DublinCoreField("relation", "Relation", "A related resource.", Multiplicity.ANY, "http://purl.org/dc/elements/1.1/relation");
	public static final DublinCoreField COVERAGE = new DublinCoreField("coverage", "Coverage", "The spatial or temporal topic of the resource, the spatial applicability of the resource, or the jurisdiction under which the resource is relevant.", Multiplicity.ANY, "http://purl.org/dc/elements/1.1/coverage");
	public static final DublinCoreField RIGHTS = new DublinCoreField("rights", "Rights", "Information about rights held in and over the resource.", Multiplicity.ANY, "http://purl.org/dc/elements/1.1/rights");


	static {
		Set<DublinCoreField> required = new HashSet<>();
		Set<DublinCoreField> available = new HashSet<>();
		Set<String> requiredKeys = new HashSet<>();

		for(DublinCoreField field : _values) {
			_keyLookup.put(field.getLabel(), field);
			available.add(field);
			if(field.isRequired()) {
				required.add(field);
				requiredKeys.add(field.getLabel());
			}
		}
		_required = Collections.unmodifiableSet(required);
		_available = Collections.unmodifiableSet(available);
		_requiredKeys = Collections.unmodifiableSet(requiredKeys);

		_keys = Collections.unmodifiableSet(_keyLookup.keySet());
	}
}

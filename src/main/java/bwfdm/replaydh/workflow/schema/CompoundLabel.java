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

import java.util.Objects;

import bwfdm.replaydh.utils.Label;

/**
 *
 * @author Markus Gärtner
 *
 */
public class CompoundLabel implements Comparable<CompoundLabel> {
	private final LabelSchema schema;

	private Object mainLabel;
	private String separator;
	private Object subLabel;

	public CompoundLabel(LabelSchema schema) {
		this.schema = requireNonNull(schema);
	}

	public CompoundLabel(LabelSchema schema, String s) {
		this(schema);

		String sep = schema.getCompoundSeparator();
		int sepIndex = s.indexOf(sep);
		if(sepIndex==-1) {
			setMainLabel(stringToLabel(s));
		} else {
			Object mainLabel = stringToLabel(s.substring(0, sepIndex));
			setMainLabel(mainLabel);
			setSeparator(sep);
			setSubLabel(stringToSubLabel(
					mainLabel instanceof Label ? (Label)mainLabel : null,
							s.substring(sepIndex+sep.length())));
		}
	}

	public CompoundLabel valueOf(String s) {

		// No valid input
		if(s==null || s.isEmpty()) {
			return null;
		}

		// No changes detected
		if(s.equals(toString())) {
			return this;
		}

		// Need to actually parse the input
		return new CompoundLabel(schema, s);
	}

	public String getLabel() {
		StringBuilder sb = new StringBuilder();

		if(mainLabel!=null) {
			sb.append(labelToString(mainLabel));
		}

		if(separator!=null) {
			sb.append(separator);
		}

		if(subLabel!=null) {
			sb.append(labelToString(subLabel));
		}

		return sb.toString();
	}

	@Override
	public String toString() {
		return getLabel();
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(CompoundLabel other) {
		int result;
		if((result = compare(getMainLabelString(), other.getMainLabelString()))!=0) {
			return result;
		}
		if((result = compare(getSeparator(), other.getSeparator()))!=0) {
			return result;
		}
		if((result = compare(getSubLabelString(), other.getSubLabelString()))!=0) {
			return result;
		}
		return result;
	}

	private static int compare(String s1, String s2) {
		if(s1==s2) {
			return 0;
		} else if(s1==null) {
			return -1;
		} else if(s2==null) {
			return 1;
		} else {
			return s1.compareTo(s2);
		}
	}

	private static String labelToString(Object label) {
		return (label instanceof Label) ? ((Label)label).getName() : label.toString();
	}

	private static String labelToDescription(Object label) {
		return (label instanceof Label) ? ((Label)label).getDescription() : null;
	}

	private Object stringToLabel(String s) {
		if(s==null || s.isEmpty()) {
			return null;
		}

		Label label = schema.findLabel(s);
		return label==null ? s : label;
	}

	private Object stringToSubLabel(Label mainLabel, String s) {
		if(s==null || s.isEmpty()) {
			return null;
		}

		Label label = mainLabel==null ? schema.findSubLabel(s) : schema.findSubLabel(mainLabel, s);
		return label==null ? s : label;
	}

	@Override
	public int hashCode() {
		return Objects.hash(schema, mainLabel, separator, subLabel);
	}

	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		} else if(obj instanceof CompoundLabel) {
			CompoundLabel other = (CompoundLabel) obj;
			return schema==other.schema
					&& Objects.equals(mainLabel, other.mainLabel)
					&& Objects.equals(separator, other.separator)
					&& Objects.equals(subLabel, other.subLabel);
		}
		return false;
	}

	public LabelSchema getSchema() {
		return schema;
	}

	public Object getMainLabel() {
		return mainLabel;
	}

	public String getMainLabelString() {
		return labelToString(mainLabel);
	}

	public String getMainLabelDescription() {
		return labelToDescription(mainLabel);
	}

	public String getSeparator() {
		return separator;
	}

	public Object getSubLabel() {
		return subLabel;
	}

	public String getSubLabelString() {
		return labelToString(subLabel);
	}

	public String getSubLabelDescription() {
		return labelToDescription(subLabel);
	}

	private static boolean isValidLabel(Object label) {
		return label==null
				|| label instanceof String
				|| label instanceof Label;
	}

	public CompoundLabel setMainLabel(Object mainLabel) {
		if(!isValidLabel(mainLabel))
			throw new IllegalArgumentException("Invalid main label: "+mainLabel);

		this.mainLabel = mainLabel;

		return this;
	}

	public CompoundLabel setSeparator(String separator) {
		this.separator = separator;

		return this;
	}

	public CompoundLabel setSubLabel(Object subLabel) {
		if(!isValidLabel(subLabel))
			throw new IllegalArgumentException("Invalid main label: "+subLabel);
		this.subLabel = subLabel;

		return this;
	}
}

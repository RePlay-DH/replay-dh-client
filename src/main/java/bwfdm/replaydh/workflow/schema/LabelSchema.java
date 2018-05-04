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

import java.util.Set;

import bwfdm.replaydh.utils.Label;

/**
 * @author Markus Gärtner
 *
 */
public interface LabelSchema {

	/**
	 * Returns the basic labels which are allowed for this schema.
	 * The returned set is never {@code null}, but might be empty.
	 *
	 * @return
	 */
	Set<Label> getLabels();

	/**
	 * Returns whether or not the schema allows the usage of
	 * arbitrary new labels. If the return value is
	 * {@code false}, then only the labels provided
	 * by the {@link #getLabels()} method can be used.
	 *
	 * @return
	 */
	boolean allowCustomLabels();

	/**
	 * Returns the {@link Label} that should be preferred
	 * or {@code null} if this schema does not include any preferences.
	 *
	 * @return
	 */
	Label getDefaultLabel();

	/**
	 * Tries to find a {@link Label} declared within this schema
	 * that is associated with the specified {@code labelString}.
	 * Returns such a label if it could be found or {@code null}
	 * otherwise.
	 * <p>
	 * Note that the provided {@code labelString} should <b>not</b>
	 * be a compound label!
	 *
	 * @param labelString
	 * @return
	 */
	default Label findLabel(String labelString) {
		requireNonNull(labelString);

		for(Label label : getLabels()) {
			if(label.getLabel().equals(labelString)) {
				return label;
			}
		}
		return null;
	}

	// OPTIONAL PART

	/**
	 * Returns whether or not this schema supports compound labels, i.e.
	 * labels that consist of a <i>main label</i> and a <i>sub label</i>
	 * part. This can be used to provide very specific descriptions beyond
	 * very general basic labels. For example a text resource could be
	 * described with just the label {@code text}, but a more specific
	 * declaration such as {@code text/code} (to specify that the text
	 * file contains source code) is more helpful.
	 *
	 * @return
	 */
	boolean allowCompoundLabels();

	/**
	 * Returns whether or not the schema allows compound labels that use
	 * the specified {@link label} as the <i>main label</i> part.
	 * <p>
	 * Note that if the more general {@link #allowCompoundLabels()} method
	 * returns {@code false} this method must never return {@code true}.
	 * It is therefore only possible to exclude certain labels from
	 * participating in compounding.
	 *
	 * @param label
	 * @return
	 */
	boolean allowCompoundLabel(Label label);

	/**
	 * Returns the symbol(s) used for separating the <i>main label</i> and
	 * <i>sub label</i> parts in a label string.
	 * <p>
	 * Note that this method always returns a non-null, non-empty string
	 * unless the schema does not {@link #allowCompoundLabels() allow}
	 * compounding.
	 *
	 * @return
	 */
	String getCompoundSeparator();

	/**
	 * Returns a collection of generally applicable labels usable
	 * as <i>sub label</i> part in label compounds.
	 * <p>
	 * The returned set is never {@code null}, but might be empty.
	 *
	 * @return
	 */
	Set<Label> getDefaultSubLabels();

	/**
	 * Returns a collection of labels usable as <i>sub label</i>
	 * part in label compounds together with the provided {@code label}
	 * as the <i>main label</i> part.
	 * <p>
	 * The returned set is never {@code null}, but might be empty.
	 *
	 * @return
	 */
	Set<Label> getDefaultSubLabels(Label label);

	/**
	 * Tries to find a {@link Label} declared within this schema
	 * for use in compound labels
	 * that is associated with the specified {@code labelString}.
	 * Returns such a label if it could be found or {@code null}
	 * otherwise.
	 * <p>
	 * Note that the provided {@code labelString} should <b>not</b>
	 * be a compound label!
	 *
	 * @param subLabelString
	 * @return
	 */
	default Label findSubLabel(String subLabelString) {
		requireNonNull(subLabelString);

		for(Label label : getDefaultSubLabels()) {
			if(label.getLabel().equals(subLabelString)) {
				return label;
			}
		}
		return null;
	}

	default Label findSubLabel(Label mainLabel, String subLabelString) {
		requireNonNull(subLabelString);

		for(Label label : getDefaultSubLabels(mainLabel)) {
			if(label.getLabel().equals(subLabelString)) {
				return label;
			}
		}
		return null;
	}

	default Label extractMainLabel(String labelString) {
		requireNonNull(labelString);
		int sepIdx = labelString.indexOf(getCompoundSeparator());
		if(sepIdx==-1) {
			return findLabel(labelString);
		} else {
			return findLabel(labelString.substring(0, sepIdx));
		}
	}

	/**
	 *
	 *
	 * @param labelString
	 * @return
	 */
	default Label extractSubLabel(String labelString) {
		requireNonNull(labelString);
		String sep = getCompoundSeparator();
		int sepIdx = labelString.indexOf(sep);
		if(sepIdx==-1) {
			return null;
		} else {
			String subLabelString = labelString.substring(sepIdx+sep.length());
			Label mainLabel = findLabel(labelString.substring(0, sepIdx));
			if(mainLabel==null) {
				return findSubLabel(subLabelString);
			} else {
				return findSubLabel(mainLabel, subLabelString);
			}
		}
	}
}

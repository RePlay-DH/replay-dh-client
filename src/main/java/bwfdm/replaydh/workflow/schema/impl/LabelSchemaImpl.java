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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import bwfdm.replaydh.utils.Label;
import bwfdm.replaydh.workflow.schema.LabelSchema;

/**
 * @author Markus Gärtner
 *
 */
public class LabelSchemaImpl implements LabelSchema {

	private boolean allowCustomLabels = false;

	private final LabelInfo globalInfo = new LabelInfo();

	private Label defaultLabel;

	private String compoundSeparator = null;

	private final Map<Label, LabelInfo> labelInfos = new HashMap<>();

	private LabelInfo labelInfo(Label label, boolean createIfMissing, boolean expectPresent) {
		LabelInfo labelInfo = labelInfos.get(label);
		if(createIfMissing && labelInfo==null) {
			 labelInfo = new LabelInfo();
			 labelInfos.put(label, labelInfo);
		}

		if(expectPresent && labelInfo==null)
			throw new IllegalArgumentException("Unknown label: "+label);

		return labelInfo;
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.LabelSchema#getLabels()
	 */
	@Override
	public Set<Label> getLabels() {
		return Collections.unmodifiableSet(labelInfos.keySet());
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.LabelSchema#allowCustomLabels()
	 */
	@Override
	public boolean allowCustomLabels() {
		return allowCustomLabels;
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.LabelSchema#getDefaultLabel()
	 */
	@Override
	public Label getDefaultLabel() {
		return defaultLabel;
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.LabelSchema#allowCompoundLabels()
	 */
	@Override
	public boolean allowCompoundLabels() {
		return globalInfo.allowCompounds;
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.LabelSchema#allowCompoundLabel(bwfdm.replaydh.utils.Label)
	 */
	@Override
	public boolean allowCompoundLabel(Label label) {
		return labelInfo(label, false, true).allowCompounds;
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.LabelSchema#getCompoundSeparator()
	 */
	@Override
	public String getCompoundSeparator() {
		return compoundSeparator;
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.LabelSchema#getDefaultSubLabels()
	 */
	@Override
	public Set<Label> getDefaultSubLabels() {
		return Collections.unmodifiableSet(globalInfo.subLabels);
	}

	/**
	 * @see bwfdm.replaydh.workflow.schema.LabelSchema#getDefaultSubLabels(bwfdm.replaydh.utils.Label)
	 */
	@Override
	public Set<Label> getDefaultSubLabels(Label label) {
		return Collections.unmodifiableSet(labelInfo(label, false, true).subLabels);
	}

	public LabelSchemaImpl setAllowCustomLabels(boolean allowCustomLabels) {
		this.allowCustomLabels = allowCustomLabels;
		return this;
	}

	public LabelSchemaImpl setDefaultLabel(Label defaultLabel) {
		this.defaultLabel = defaultLabel;
		return this;
	}

	public LabelSchemaImpl setCompoundSeparator(String compoundSeparator) {
		this.compoundSeparator = compoundSeparator;
		return this;
	}

	public LabelSchemaImpl setAllowCompoundLabels(boolean allowCompoundLabels) {
		globalInfo.allowCompounds = allowCompoundLabels;
		return this;
	}

	public LabelSchemaImpl setAllowCompounds(Label label, boolean allowCompounds) {
		labelInfo(label, true, true).allowCompounds = allowCompounds;
		return this;
	}

	public LabelSchemaImpl addLabel(Label label) {
		if(!globalInfo.subLabels.add(label))
			throw new IllegalArgumentException("Duplicate label: "+label);
		return this;
	}

	public LabelSchemaImpl addSubLabel(Label label, Label subLabel) {
		if(!labelInfo(label, true, true).subLabels.add(subLabel))
			throw new IllegalArgumentException("Duplicate sub-label: "+subLabel);
		return this;
	}

	private static class LabelInfo {
		final Set<Label> subLabels = new HashSet<>();
		boolean allowCompounds = false;
	}
}

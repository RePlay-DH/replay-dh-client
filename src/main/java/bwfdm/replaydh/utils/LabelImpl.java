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
package bwfdm.replaydh.utils;

import static java.util.Objects.requireNonNull;

/**
 * @author Markus Gärtner
 *
 */
public class LabelImpl implements Label {

	private String label;
	private String name;
	private String description;

	public LabelImpl() {
		// no-op
	}

	public LabelImpl(String label) {
		setLabel(label);
	}

	/**
	 * @see bwfdm.replaydh.utils.Label#getLabel()
	 */
	@Override
	public String getLabel() {
		return label;
	}

	/**
	 * @see bwfdm.replaydh.utils.Label#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @see bwfdm.replaydh.utils.Label#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}

	public LabelImpl setLabel(String label) {
		this.label = requireNonNull(label);
		return this;
	}

	public LabelImpl setName(String name) {
		this.name = name;
		return this;
	}

	public LabelImpl setDescription(String description) {
		this.description = description;
		return this;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Label@"+label;
	}
}

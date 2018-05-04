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
package bwfdm.replaydh.ui.workflow.graph;

import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.Map;

import com.mxgraph.view.mxStylesheet;

/**
 * @author Markus Gärtner
 *
 */
public class StyleBuilder {

	public static StyleBuilder forStylesheet(mxStylesheet stylesheet) {
		return new StyleBuilder(stylesheet);
	}

	private final mxStylesheet stylesheet;

	private Map<String, Object> style;
	private String name;
	private String key;

	private StyleBuilder(mxStylesheet stylesheet) {
		this.stylesheet = requireNonNull(stylesheet);
	}

	private void checkNoEntry() {
		checkState("Uncommited entry with key: "+key, key==null);
	}

	private void checkNoStyle() {
		checkState("Uncommited style", name==null && style==null);
	}

	private void checkEntry() {
		checkState("No entry context", key!=null);
	}

	private void checkStyle() {
		checkState("No style context", style!=null);
	}

	private void checkCommit() {
		checkNoEntry();

		checkStyle();

		checkState("No style name for commit available", name!=null);
	}

	private void set(String name, Map<String, Object> style) {
		this.name = name;
		this.style = style;
		this.key = null;
	}

	private void set(String key) {
		this.key = key;
	}

	private void clear() {
		this.name = null;
		this.style = null;
		this.key = null;
	}

	public StyleBuilder newStyle(String name) {
		requireNonNull(name);

		if(stylesheet.getStyles().containsKey(name))
			throw new IllegalArgumentException("Style already exists: "+name);

		checkNoStyle();
		checkNoEntry();

		set(name, new HashMap<>());

		return this;
	}

	public StyleBuilder newStyle(String name, String baseStyle) {
		requireNonNull(name);
		requireNonNull(baseStyle);

		if(stylesheet.getStyles().containsKey(name))
			throw new IllegalArgumentException("Style already exists: "+name);

		checkNoStyle();
		checkNoEntry();

		set(name, stylesheet.getCellStyle(baseStyle, new HashMap<>()));

		return this;
	}

	public StyleBuilder newStyle(String name, Map<String, Object> baseStyle) {
		requireNonNull(name);
		requireNonNull(baseStyle);

		if(stylesheet.getStyles().containsKey(name))
			throw new IllegalArgumentException("Style already exists: "+name);

		checkNoStyle();
		checkNoEntry();

		set(name, new HashMap<>(baseStyle));

		return this;
	}

	public StyleBuilder newEntry(String key, Object value) {
		requireNonNull(key);
		requireNonNull(value);

		checkNoEntry();
		checkStyle();

		style.put(key, value);

		return this;
	}

	public StyleBuilder key(String key) {
		requireNonNull(key);

		checkNoEntry();
		checkStyle();

		set(key);

		return this;
	}

	public StyleBuilder value(Object value) {
		requireNonNull(value);

		checkEntry();
		checkStyle();

		style.put(key, value);
		set(null);

		return this;
	}

	public StyleBuilder modifyStyle(String name) {
		requireNonNull(name);

		checkNoEntry();
		checkNoStyle();

		Map<String, Object> style = stylesheet.getStyles().get(key);

		if(style==null)
			throw new IllegalArgumentException("No such style: "+name);

		this.style = style;

		return this;
	}

	public StyleBuilder modifyDefaultVertexStyle() {
		checkNoEntry();
		checkNoStyle();

		this.style = stylesheet.getDefaultVertexStyle();

		return this;
	}

	public StyleBuilder modifyDefaultEdgeStyle() {
		checkNoEntry();
		checkNoStyle();

		this.style = stylesheet.getDefaultEdgeStyle();

		return this;
	}

	public void commit() {
		checkCommit();

		stylesheet.putCellStyle(name, style);

		clear();
	}

	public void done() {
		clear();
	}
}

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
package bwfdm.replaydh.ui;

import bwfdm.replaydh.stats.Stats;

/**
 * @author Markus Gärtner
 *
 */
public final class GuiStats implements Stats {

	public static final String WINDOW = "window";
	public static final String TRAY = "tray";
	public static final String DIALOG = "dialog";
	public static final String WIZARD = "wizard";
	public static final String GRAPH = "graph";

	public static final String TRAY_MESSAGE = "tray_message";

	public static final String OPEN_WORKSPACE = "open_workspace";
	public static final String CLEAR_CACHE = "clear_cache";
	public static final String UPDATE_TRACKER = "update_tracker";

	public static final String WINDOW_EXPAND = "window_expand";
	public static final String WINDOW_COLLAPSE = "window_collapse";

	public static final String GRAPH_EXPAND = "window_expand";
	public static final String GRAPH_COLLAPSE = "window_collapse";

	public static final String DIALOG_ADD_STEP = "dialog_add_step";
	public static final String DIALOG_PREFERENCES = "dialog_preferences";
	public static final String DIALOG_ADD_CACHED = "dialog_add_cached";
	public static final String DIALOG_ADD_SCHEMA = "dialog_add_schema";

	public static final String WIZARD_PAGE_NEXT = "wizard_page_next";
	public static final String WIZARD_PAGE_PREV = "wizard_page_prev";
	public static final String WIZARD_CANCEL = "wizard_cancel";
}

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
package bwfdm.replaydh.stats;

/**
 * @author Markus Gärtner
 *
 */
public enum StatType {

	/**
	 * User opened a certain UI component or started a dialog.
	 */
	UI_OPEN(StatConstants.UI, StatConstants.BEGIN),
	/**
	 * User closed a certain UI component or finished a dialog.
	 */
	UI_CLOSE(StatConstants.UI, StatConstants.END),

	/**
	 * Used to model individual actions in the user interface that
	 * do only affect a singular point in time.
	 */
	UI_ACTION(StatConstants.UI),
	/**
	 * Used to log UI issues that break the normal execution flow
	 */
	UI_ERROR(StatConstants.UI),

	/**
	 * An interval of interest begins
	 */
	RUNTIME_BEGIN(StatConstants.RUNTIME, StatConstants.BEGIN),
	/**
	 * An interval of interest end
	 */
	RUNTIME_END(StatConstants.RUNTIME, StatConstants.END),

	/**
	 * An internal service started
	 */
	INTERNAL_BEGIN(StatConstants.INTERNAL, StatConstants.BEGIN),
	/**
	 * An internal service is shutting down
	 */
	INTERNAL_END(StatConstants.INTERNAL, StatConstants.END),

	;

	private StatType(int...flags) {
		int f = 0;
		for(int flag : flags) {
			f |= flag;
		}

		this.flags = f;
	}

	private final int flags;

	private StatType partner;

	public int getFlags() {
		return flags;
	}

	public boolean flagsSet(int flags) {
		return (this.flags & flags) == flags;
	}

	public StatType getPartner() {
		return partner;
	}

	private static void pair(StatType first, StatType second) {
		first.partner = second;
		second.partner = first;
	}

	static {
		pair(UI_OPEN, UI_CLOSE);
		pair(RUNTIME_BEGIN, RUNTIME_END);
		pair(INTERNAL_BEGIN, INTERNAL_END);
	}
}

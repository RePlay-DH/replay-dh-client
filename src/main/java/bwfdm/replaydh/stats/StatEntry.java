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

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;

/**
 * @author Markus Gärtner
 *
 */
public class StatEntry {

	private final LocalDateTime dateTime;

	private final StatType type;

	private final String label;

	private final String[] data;

	private static final String[] NO_DATA = {};

	private static LocalDateTime now() {
		return LocalDateTime.now();
	}

	public static StatEntry ofType(StatType type, String label) {
		return new StatEntry(now(), type, label, NO_DATA);
	}

	public static StatEntry withData(StatType type, String label, String...data) {
		checkArgument("Must have at least 1 data point", data.length>0);
		return new StatEntry(now(), type, label, data);
	}

	private StatEntry(LocalDateTime dateTime, StatType type, String label, String[] data) {
		this.dateTime = LocalDateTime.now();
		this.type = requireNonNull(type);
		this.label = requireNonNull(label);
		this.data = data;
	}

	public LocalDateTime getDateTime() {
		return dateTime;
	}

	public StatType getType() {
		return type;
	}

	public String getLabel() {
		return label;
	}

	public String[] getData() {
		return data;
	}
}

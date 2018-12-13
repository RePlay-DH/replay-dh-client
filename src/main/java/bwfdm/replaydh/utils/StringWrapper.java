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

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.function.IntPredicate;
import java.util.function.ToIntFunction;

/**
 * Utility class for wrapping text based on certain measurements.
 * <p>
 * This class is <b>not</b> thread-safe!
 * <p>
 * Per default the wrapper will wrap on spaces or tabs, use a maximum line width
 * of {@link #DEFAULT_LINE_LIMIT 70}, the {@link String#length() native string length}
 * measure and {@code 1} as the average estimated length of a single character.
 * It will also use {@code \r\n} as the default linebreak character sequence.
 *
 * @author Markus Gärtner
 *
 */
public class StringWrapper {

	private static final char CR = '\r';
	private static final char LF = '\n';
	private static final char SPACE = ' ';
	private static final char TAB = '\t';
	/**
	 * A default limit for characters per line before a linebreak should be forced.
	 * Chosen to be {@code 70}.
	 */
	public static final int DEFAULT_LINE_LIMIT = 70;

	private static final ToIntFunction<CharSequence> DEFAULT_CHAR_MEASURE = CharSequence::length;

	private String linebreak = CR+""+LF;
	private int limit = DEFAULT_LINE_LIMIT;
	private ToIntFunction<CharSequence> measure = DEFAULT_CHAR_MEASURE;
	private int averageCharWidth = 1;
	private IntPredicate breakpoint = c -> c==SPACE || c==TAB;
	private String header;
	private String footer;

	public StringWrapper linebreak(String linebreak) {
		this.linebreak = requireNonNull(linebreak);
		return this;
	}

	public StringWrapper limit(int limit) {
		checkArgument("Limit must be positive", limit>0);
		this.limit = limit;
		return this;
	}

	public StringWrapper measure(ToIntFunction<CharSequence> measure) {
		this.measure = requireNonNull(measure);
		return this;
	}

	public StringWrapper averageCharWidth(int averageWidthHint) {
		checkArgument("Hint for average char width must be positive", averageWidthHint>0);
		this.averageCharWidth = averageWidthHint;
		return this;
	}

	public StringWrapper breakpoint(IntPredicate breakpoint) {
		this.breakpoint = requireNonNull(breakpoint);
		return this;
	}

	public StringWrapper header(String header) {
		this.header = requireNonNull(header);
		return this;
	}

	public StringWrapper footer(String footer) {
		this.footer = requireNonNull(footer);
		return this;
	}


	//---------------------------- PROCESSING STUFF ------------------------

	private int measure(CharSequence cs) {
		return measure.applyAsInt(cs);
	}

	private StringBuilder out;
	private StringBuilder line;

	private int estimatedCharsPerLine;
	private boolean contentFed;

	public String wrap(String text) {
		// Spare us a lot of trouble if the text already fits
		if(measure(text)<=limit) {
			// If the text contains manual linebreaks we still need to process those
			if(text.indexOf(LF)!=-1) {
				text = text.replaceAll("\\r\\n|\\n", linebreak);
			}

			return applyHeaderAndFooter(text);
		}
		// End of shortcut options

		reset(text);

		boolean expectLF = false;

		for(int i=0; i<text.length(); i++) {
			char c = text.charAt(i);

			if(expectLF && c!=LF)
				throw new IllegalArgumentException("Incomplete CR LF at index "+i);
			expectLF = false;

			switch (c) {
			case CR:
				expectLF = true;
				break;

			case LF:
				consumeLineFully();
				feedNewline();
				break;

			default:
				feedChar(c);
				break;
			}
		}

		feedLeftoverLine();

		if(header!=null) {
			out.insert(0, header);
		}

		if(footer!=null) {
			out.append(footer);
		}

		return out.toString();
	}

	private String applyHeaderAndFooter(String text) {
		if(header!=null || footer!=null) {
			reset(text);
			if(header!=null) {
				out.append(header);
			}
			out.append(text);
			if(footer!=null) {
				out.append(footer);
			}
			text = out.toString();
		}
		return text;
	}

	private void reset(String text) {
		if(out==null) {
			out = new StringBuilder(text.length());
		}
		if(line==null) {
			line = new StringBuilder(limit);
		}

		line.setLength(0);
		out.setLength(0);

		estimatedCharsPerLine = (int) Math.max(1, limit/(double)averageCharWidth);
		contentFed = false;
	}

	/**
	 * Push new char into line and check if we should actually try
	 * to wrap, doing so if current line length exceeds estimated
	 * character count per wrapped line.
	 * @param c
	 */
	private void feedChar(char c) {
		line.append(c);
		if(shouldTryConsumeLine()) {
			if(shouldWrapLine()) {
				consumeWrappableLine();
			}
		}
	}

	/**
	 * @return true if {@link String#length()} of current line
	 * exceeds estimated character count per wrapped line.
	 */
	private boolean shouldTryConsumeLine() {
		return line.length()>=estimatedCharsPerLine;
	}

	/**
	 * @return true if {@link #measure(String)} measured length of current line
	 * exceeds the {@link #limit}.
	 */
	private boolean shouldWrapLine() {
		return measure(line)>limit;
	}

	private void feedNewline() {
		out.append(linebreak);
	}

	private void feedOptionalNewline() {
		if(contentFed) {
			feedNewline();
		}
	}

	/**
	 * Scans the given sequence backwards starting from {@code index}
	 * @param s
	 * @param breakable
	 * @param start
	 * @return
	 */
	private int find(CharSequence s, boolean breakable, int start) {
		IntPredicate test = breakpoint;
		if(!breakable) {
			test = test.negate();
		}

		int index = start;

		while(index>=0) {
			if(test.test(s.charAt(index))) {
				return index;
			}
			index--;
		}

		return -1;
	}

	/**
	 * Searches backwards for the first legal breakpoint and wraps the
	 * current line on it.
	 */
	private void consumeWrappableLine() {
		int wrapStart = 0;
		int wrapEnd = line.length()-1;

		int newlineStart = line.length();

		int bRight = find(line, true, wrapEnd);

		if(bRight!=-1) {
			int bLeft = find(line, false, bRight-1);
			if(bLeft!=-1) {
				wrapEnd = bLeft;
			}
			newlineStart = bRight+1;
		}

		feedOptionalNewline();
		out.append(line, wrapStart, wrapEnd+1);
		contentFed = true;

		line.delete(0, newlineStart);
	}

	private void feedLeftoverLine() {
		if(line.length()>0) {
			feedOptionalNewline();
			out.append(line);
			line.setLength(0);
			contentFed = true;
		}
	}

	/**
	 * Push content from current line into output,
	 * wrapping whenever needed.
	 * <p>
	 * Once this method returns, the current line buffer
	 * is guaranteed to be empty.
	 */
	private void consumeLineFully() {

		// Consume wrappable parts
		while(shouldWrapLine()) {
			consumeWrappableLine();
		}

		// Push leftover line part as well
		feedLeftoverLine();
	}
}

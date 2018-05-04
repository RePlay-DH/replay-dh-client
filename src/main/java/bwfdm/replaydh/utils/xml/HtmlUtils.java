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
package bwfdm.replaydh.utils.xml;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Markus Gärtner
 * @version $Id: HtmlUtils.java 458 2016-05-02 15:24:40Z mcgaerty $
 *
 */
public final class HtmlUtils {

	private HtmlUtils() {
	}

	public static boolean hasReservedXMLSymbols(CharSequence s) {
		for(int i=0; i<s.length(); i++) {
			if(isReservedXMLSymbol(s.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	public static boolean isReservedXMLSymbol(char c) {
		switch(c) {

			case '<':
			case '>':
			case '&':
			case '"':
			case '\'':
				return true;

			default:
				return false;
		}
	}

	public static String hexString(Color color) {
		return String.format("#%02X%02X%02X",  //$NON-NLS-1$
				color.getRed(),
				color.getGreen(),
				color.getBlue());
	}

	private static String[] htmlReplacements = new String[300];
	private static Map<String, Integer> htmlEntities = new HashMap<>();

	private static void reg(int c, String s) {
		htmlReplacements[c] = s;
		htmlEntities.put(s.substring(1, s.length()-1), c);
	}
	static {
		reg(60, "&lt;"); //< //$NON-NLS-1$
		reg(62, "&gt;"); //> //$NON-NLS-1$
		reg(38, "&amp;"); //& //$NON-NLS-1$
		reg(34, "&quot;"); //" //$NON-NLS-1$
		reg(224, "&agrave;"); //à //$NON-NLS-1$
		reg(192, "&Agrave;"); //À //$NON-NLS-1$
		reg(226, "&acirc;"); //â //$NON-NLS-1$
		reg(194, "&Acirc;"); //Â //$NON-NLS-1$
		reg(228, "&auml;"); //ä //$NON-NLS-1$
		reg(196, "&Auml;"); //Ä //$NON-NLS-1$
		reg(229, "&aring;"); //å //$NON-NLS-1$
		reg(197, "&Aring;"); //Å //$NON-NLS-1$
		reg(230, "&aelig;"); //æ //$NON-NLS-1$
		reg(198, "&AElig;"); //Æ //$NON-NLS-1$
		reg(231, "&ccedil;"); //ç //$NON-NLS-1$
		reg(199, "&Ccedil;"); //Ç //$NON-NLS-1$
		reg(233, "&eacute;"); //é //$NON-NLS-1$
		reg(201, "&Eacute;"); //É //$NON-NLS-1$
		reg(232, "&egrave;"); //è //$NON-NLS-1$
		reg(200, "&Egrave;"); //È //$NON-NLS-1$
		reg(234, "&ecirc;"); //ê //$NON-NLS-1$
		reg(202, "&Ecirc;"); //Ê //$NON-NLS-1$
		reg(235, "&euml;"); //ë //$NON-NLS-1$
		reg(203, "&Euml;"); //Ë //$NON-NLS-1$
		reg(239, "&iuml;"); //ï //$NON-NLS-1$
		reg(207, "&Iuml;"); //Ï //$NON-NLS-1$
		reg(244, "&ocirc;"); //ô //$NON-NLS-1$
		reg(212, "&Ocirc;"); //Ô //$NON-NLS-1$
		reg(246, "&ouml;"); //ö //$NON-NLS-1$
		reg(214, "&Ouml;"); //Ö //$NON-NLS-1$
		reg(248, "&oslash;"); //ø //$NON-NLS-1$
		reg(216, "&Oslash;"); //Ø //$NON-NLS-1$
		reg(223, "&szlig;"); //ß //$NON-NLS-1$
		reg(249, "&ugrave;"); //ù //$NON-NLS-1$
		reg(217, "&Ugrave;"); //Ù //$NON-NLS-1$
		reg(251, "&ucirc;"); //û //$NON-NLS-1$
		reg(219, "&Ucirc;"); //Û //$NON-NLS-1$
		reg(252, "&uuml;"); //ü //$NON-NLS-1$
		reg(220, "&Uuml;"); //Ü //$NON-NLS-1$
		reg(174, "&reg;"); //® //$NON-NLS-1$
		reg(169, "&copy;"); //© //$NON-NLS-1$
//		reg(8364, "&euro;"); //€ //$NON-NLS-1$

		htmlEntities.put("euro", 8364);
	}

	public static String replace(char symbol) {
		if(symbol==8364) {
			return "&euro;";
		} else if(symbol>=0 && symbol<htmlReplacements.length) {
			return htmlReplacements[symbol];
		} else {
			return null;
		}
	}

	public static String escapeHTML(String s) {
		StringBuffer sb = new StringBuffer(s.length());
		int n = s.length();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			Object r = replace(c);

			if(r==null) {
				sb.append(c);
			} else {
				sb.append(r);
			}
		}
		return sb.toString();
	}

	public static String unescapeHTML(String s) {
		StringBuffer sb = new StringBuffer(s.length());

		// Created lazily in case we need it
		StringBuffer label = null;

		int n = s.length();
		boolean entity = false;
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);

			switch (c) {
			case '&':
				entity = true;
				if(label==null) {
					label = new StringBuffer();
				} else {
					label.setLength(0);
				}
				break;

			case ';':
				entity = false;
				sb.append((char)htmlEntities.get(label.toString()).intValue());
				break;

			default:
				if(entity) {
					label.append(c);
				} else {
					sb.append(c);
				}
				break;
			}
		}
		return sb.toString();
	}

	public static String revertBreaks(String s) {
		return s.replaceAll("<[bB][rR]>", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static abstract class HtmlBuilder {
		protected StringBuilder builder;

		protected boolean building = false;

		protected HtmlBuilder(int capacity) {
			builder = new StringBuilder(capacity);
		}

		protected HtmlBuilder() {
			this(40);
		}

		public HtmlBuilder start() {
			builder.setLength(0);
			building = true;
			start0();

			return this;
		}

		public boolean isBuilding() {
			return building;
		}

		public HtmlBuilder finish() {
			finish0();
			building = false;

			return this;
		}

		protected abstract void finish0();

		protected abstract void start0();

		public String getResult() {
			return building ? null : builder.toString();
		}
	}

	public static class HtmlTableBuilder extends HtmlBuilder {

		protected int columnCount = DEFAULT_COLUMN_COUNT;

		protected boolean leadingColon = false;
		protected boolean headerSet = false;

		public static final int DEFAULT_COLUMN_COUNT = 2;

		/**
		 *
		 */
		public HtmlTableBuilder() {
			super(100);
		}

		/**
		 * @param capacity
		 */
		public HtmlTableBuilder(int capacity) {
			super(capacity);
		}

		@Override
		public HtmlTableBuilder start() {
			return start(DEFAULT_COLUMN_COUNT, false);
		}

		public HtmlTableBuilder start(int columnCount) {
			return start(columnCount, false);
		}

		public HtmlTableBuilder start(boolean leadingColon) {
			return start(DEFAULT_COLUMN_COUNT, leadingColon);
		}

		public HtmlTableBuilder start(int columnCount, boolean leadingColon) {
			super.start();
			this.columnCount = columnCount;
			this.leadingColon = leadingColon;
			headerSet = false;

			return this;
		}

		@Override
		protected void finish0() {
			builder.append("</table></html>"); //$NON-NLS-1$
		}

		@Override
		protected void start0() {
			builder.append("<html><table width=\"100%\">"); //$NON-NLS-1$
		}

		public HtmlTableBuilder setHeader(Object...items) {
			if(headerSet || items.length!=columnCount)
				throw new IllegalArgumentException();

			builder.append("<colgroup>"); //$NON-NLS-1$

			for(Object item : items)
				builder.append("<col width=\"").append(String.valueOf(item)) //$NON-NLS-1$
						.append("\" />"); //$NON-NLS-1$

			builder.append("</colgroup>"); //$NON-NLS-1$

			return this;
		}

		public HtmlTableBuilder addRowEscaped(String...items) {
			builder.append("<tr>"); //$NON-NLS-1$

			for(int i=0; i<items.length; i++) {
				if(i>=columnCount)
					break;

				builder.append("<td>").append(escapeHTML(items[i])) //$NON-NLS-1$
						.append((i==0 && leadingColon) ? ":</td>" : "</td>"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			if(items.length<columnCount)
				for(int i=columnCount-1; i>=items.length; i--)
					builder.append("<td></td>"); //$NON-NLS-1$

			builder.append("</tr>"); //$NON-NLS-1$

			return this;
		}

		public HtmlTableBuilder addRow(String...items) {
			builder.append("<tr>"); //$NON-NLS-1$

			for(int i=0; i<items.length; i++) {
				if(i>=columnCount)
					break;

				builder.append("<td>").append(items[i]) //$NON-NLS-1$
						.append((i==0 && leadingColon) ? ":</td>" : "</td>"); //$NON-NLS-1$ //$NON-NLS-2$
			}

			if(items.length<columnCount)
				for(int i=columnCount-1; i>=items.length; i--)
					builder.append("<td></td>"); //$NON-NLS-1$

			builder.append("</tr>"); //$NON-NLS-1$

			return this;
		}

		/**
		 * @return the columnCount
		 */
		public int getColumnCount() {
			return columnCount;
		}

		/**
		 * @param columnCount the columnCount to set
		 */
		public HtmlTableBuilder setColumnCount(int columnCount) {
			if(isBuilding())
				throw new IllegalStateException();

			this.columnCount = columnCount;

			return this;
		}

		/**
		 * @return the leadingColon
		 */
		public boolean isLeadingColon() {
			return leadingColon;
		}

		/**
		 * @param leadingColon the leadingColon to set
		 */
		public HtmlTableBuilder setLeadingColon(boolean leadingColon) {
			this.leadingColon = leadingColon;

			return this;
		}
	}

	public static class HtmlLineBuilder extends HtmlBuilder {

		protected boolean omitBreakIfEmpty = true;

		/**
		 *
		 */
		public HtmlLineBuilder() {
			super();
		}

		/**
		 * @param capacity
		 */
		public HtmlLineBuilder(int capacity) {
			super(capacity);
		}

		@Override
		public HtmlLineBuilder start() {
			return start(true);
		}

		public HtmlLineBuilder start(boolean omitBreakIfEmpty) {
			super.start();
			this.omitBreakIfEmpty = omitBreakIfEmpty;

			return this;
		}

		@Override
		protected void start0() {
			builder.append("<html>"); //$NON-NLS-1$
		}

		@Override
		protected void finish0() {
			builder.append("</html>"); //$NON-NLS-1$
		}

		public HtmlLineBuilder newLine() {
			if(!omitBreakIfEmpty || builder.length()>6) // length of '<html>'
				builder.append("<br>"); //$NON-NLS-1$

			return this;
		}

		public HtmlLineBuilder appendEscaped(String item) {
			return appendEscaped(item, null);
		}

		public HtmlLineBuilder appendEscaped(String item, Color color) {
			return append(escapeHTML(item), color);
		}

		public HtmlLineBuilder append(String item) {
			return append(item, null);
		}

		public HtmlLineBuilder append(String item, Color color) {
			if(color!=null)
				builder.append("<font color=\"").append(hexString(color)).append("\">"); //$NON-NLS-1$ //$NON-NLS-2$
			builder.append(item);
			if(color!=null)
				builder.append("</font>"); //$NON-NLS-1$

			return this;
		}

		public HtmlLineBuilder appendIfNonempty(String item) {
			return appendIfNonempty(item, null);
		}

		public HtmlLineBuilder appendIfNonempty(String item, Color color) {
			if(builder.length()>6) {
				return append(item, color);
			}

			return this;
		}

		public boolean isEmpty() {
			return builder.length()<= (building ? 6 : 13);
		}

		/**
		 * @return the omitBreakIfEmpty
		 */
		public boolean isOmitBreakIfEmpty() {
			return omitBreakIfEmpty;
		}

		/**
		 * @param omitBreakIfEmpty the omitBreakIfEmpty to set
		 */
		public HtmlLineBuilder setOmitBreakIfEmpty(boolean omitBreakIfEmpty) {
			this.omitBreakIfEmpty = omitBreakIfEmpty;

			return this;
		}
	}
}

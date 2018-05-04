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


/**
 * @author Markus Gärtner
 *
 */
public class StringPrimitives {

    static NumberFormatException forInputString(CharSequence s) {
        return new NumberFormatException("For input string: \"" + s + "\""); //$NON-NLS-1$ //$NON-NLS-2$
    }

	/**
	 * @see Integer#parseInt(String, int)
	 */
    public static int parseInt(CharSequence s, int radix, int from, int to)
                throws NumberFormatException {
        if (s == null) {
            throw new NumberFormatException("null"); //$NON-NLS-1$
        }

        if (radix < Character.MIN_RADIX) {
            throw new NumberFormatException("radix " + radix + //$NON-NLS-1$
                                            " less than Character.MIN_RADIX"); //$NON-NLS-1$
        }

        if (radix > Character.MAX_RADIX) {
            throw new NumberFormatException("radix " + radix + //$NON-NLS-1$
                                            " greater than Character.MAX_RADIX"); //$NON-NLS-1$
        }

        if(to==-1) {
        	to = s.length()-1;
        }

        int result = 0;
        boolean negative = false;
        int i = from;
        int len = to-from+1;
        int limit = -Integer.MAX_VALUE;
        int multmin;
        int digit;

        if (len > 0) {
            char firstChar = s.charAt(i);
            if (firstChar < '0') { // Possible leading "+" or "-"
                if (firstChar == '-') {
                    negative = true;
                    limit = Integer.MIN_VALUE;
                } else if (firstChar != '+')
                    throw forInputString(s);

                if (len == 1) // Cannot have lone "+" or "-"
                    throw forInputString(s);
                i++;
            }
            multmin = limit / radix;
            while (i <= to) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                digit = Character.digit(s.charAt(i++),radix);
                if (digit < 0) {
                    throw forInputString(s);
                }
                if (result < multmin) {
                    throw forInputString(s);
                }
                result *= radix;
                if (result < limit + digit) {
                    throw forInputString(s);
                }
                result -= digit;
            }
        } else {
            throw forInputString(s);
        }
        return negative ? result : -result;
    }

    public static int parseInt(CharSequence s, int radix) throws NumberFormatException {
    	return parseInt(s, radix, 0, -1);
    }

    public static int parseInt(CharSequence s, int from, int to) throws NumberFormatException {
    	return parseInt(s, 10, from, to);
    }

    public static int parseInt(CharSequence s) throws NumberFormatException {
        return parseInt(s,10,0,-1);
    }

    /**
     * @see Long#parseLong(String, int)
     */
    public static long parseLong(CharSequence s, int radix, int from, int to)
              throws NumberFormatException
    {
        if (s == null) {
            throw new NumberFormatException("null"); //$NON-NLS-1$
        }

        if (radix < Character.MIN_RADIX) {
            throw new NumberFormatException("radix " + radix + //$NON-NLS-1$
                                            " less than Character.MIN_RADIX"); //$NON-NLS-1$
        }
        if (radix > Character.MAX_RADIX) {
            throw new NumberFormatException("radix " + radix + //$NON-NLS-1$
                                            " greater than Character.MAX_RADIX"); //$NON-NLS-1$
        }

        if(to==-1) {
        	to = s.length()-1;
        }

        long result = 0;
        boolean negative = false;
        int i = from;
        int len = to-from+1;
        long limit = -Long.MAX_VALUE;
        long multmin;
        int digit;

        if (len > 0) {
            char firstChar = s.charAt(i);
            if (firstChar < '0') { // Possible leading "+" or "-"
                if (firstChar == '-') {
                    negative = true;
                    limit = Long.MIN_VALUE;
                } else if (firstChar != '+')
                    throw forInputString(s);

                if (len == 1) // Cannot have lone "+" or "-"
                    throw forInputString(s);
                i++;
            }
            multmin = limit / radix;
            while (i <= to) {
                // Accumulating negatively avoids surprises near MAX_VALUE
                digit = Character.digit(s.charAt(i++),radix);
                if (digit < 0) {
                    throw forInputString(s);
                }
                if (result < multmin) {
                    throw forInputString(s);
                }
                result *= radix;
                if (result < limit + digit) {
                    throw forInputString(s);
                }
                result -= digit;
            }
        } else {
            throw forInputString(s);
        }
        return negative ? result : -result;
    }

    public static long parseLong(CharSequence s, int radix) throws NumberFormatException {
    	return parseLong(s, radix, 0, -1);
    }

    public static long parseLong(CharSequence s, int from, int to) throws NumberFormatException {
    	return parseLong(s, 10, from, to);
    }

    public static long parseLong(CharSequence s) throws NumberFormatException {
        return parseLong(s, 10, 0, -1);
    }

    /**
     * @see Short#parseShort(String, int)
     */
    public static short parseShort(CharSequence s, int radix, int from, int to)
        throws NumberFormatException {
        int i = parseInt(s, radix, from, to);
        if (i < Short.MIN_VALUE || i > Short.MAX_VALUE)
            throw new NumberFormatException(
                "Value out of range. Value:\"" + s + "\" Radix:" + radix); //$NON-NLS-1$ //$NON-NLS-2$
        return (short)i;
    }

    public static short parseShort(CharSequence s, int radix) throws NumberFormatException {
    	return parseShort(s, radix, 0, -1);
    }

    public static short parseShort(CharSequence s, int from, int to) throws NumberFormatException {
    	return parseShort(s, 10, from, to);
    }

    public static short parseShort(CharSequence s) throws NumberFormatException {
        return parseShort(s, 10);
    }

    /**
     * @see Float#parseFloat(String)
     */
    public static float parseFloat(CharSequence s) throws NumberFormatException {
        return Float.parseFloat(s.toString());
    }

    /**
     * @see Double#parseDouble(String)
     */
    public static double parseDouble(CharSequence s) throws NumberFormatException {
        return Double.parseDouble(s.toString());
    }

    /**
     * @see Boolean#parseBoolean(String)
     */
    public static boolean parseBoolean(CharSequence s) {
        return toBoolean(s, 0);
    }

    public static boolean parseBoolean(CharSequence s, int offset) {
    	return toBoolean(s, offset);
    }

    private static boolean toBoolean(CharSequence s, int offset) {
        return ((s != null)
        		&& s.length()==4
        		&& (s.charAt(0)=='T' || s.charAt(0)=='t')
        		&& (s.charAt(1)=='R' || s.charAt(1)=='r')
        		&& (s.charAt(2)=='U' || s.charAt(2)=='u')
        		&& (s.charAt(3)=='E' || s.charAt(3)=='e'));
    }
}

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
package bwfdm.replaydh.io.resources;

import java.util.regex.PatternSyntaxException;

/**
 * Copied over from the JVM internal class {@code sun.nio.fs.Globs} in
 * version {@code 8u40-b25}. This utility class is only being used by the
 * {@link VirtualResourceProvider} implementation which in turn only
 * exists for testing purposes. So no negative side effects should occur
 * by keeping this class as-is.
 */
public class Globs {
    private Globs() { }

    private static final String regexMetaChars = ".^$+{[]|()";
    private static final String globMetaChars = "\\*?[{";

    private static boolean isRegexMeta(char c) {
        return regexMetaChars.indexOf(c) != -1;
    }

    private static boolean isGlobMeta(char c) {
        return globMetaChars.indexOf(c) != -1;
    }
    private static char EOL = 0;  //TBD

    private static char next(String glob, int i) {
        if (i < glob.length()) {
            return glob.charAt(i);
        }
        return EOL;
    }

    /**
     * Creates a regex pattern from the given glob expression.
     *
     * @throws  PatternSyntaxException
     */
    private static String toRegexPattern(String globPattern, boolean isDos) {
        boolean inGroup = false;
        StringBuilder regex = new StringBuilder("^");

        int i = 0;
        while (i < globPattern.length()) {
            char c = globPattern.charAt(i++);
            switch (c) {
                case '\\':
                    // escape special characters
                    if (i == globPattern.length()) {
                        throw new PatternSyntaxException("No character to escape",
                                globPattern, i - 1);
                    }
                    char next = globPattern.charAt(i++);
                    if (isGlobMeta(next) || isRegexMeta(next)) {
                        regex.append('\\');
                    }
                    regex.append(next);
                    break;
                case '/':
                    if (isDos) {
                        regex.append("\\\\");
                    } else {
                        regex.append(c);
                    }
                    break;
                case '[':
                    // don't match name separator in class
                    if (isDos) {
                        regex.append("[[^\\\\]&&[");
                    } else {
                        regex.append("[[^/]&&[");
                    }
                    if (next(globPattern, i) == '^') {
                        // escape the regex negation char if it appears
                        regex.append("\\^");
                        i++;
                    } else {
                        // negation
                        if (next(globPattern, i) == '!') {
                            regex.append('^');
                            i++;
                        }
                        // hyphen allowed at start
                        if (next(globPattern, i) == '-') {
                            regex.append('-');
                            i++;
                        }
                    }
                    boolean hasRangeStart = false;
                    char last = 0;
                    while (i < globPattern.length()) {
                        c = globPattern.charAt(i++);
                        if (c == ']') {
                            break;
                        }
                        if (c == '/' || (isDos && c == '\\')) {
                            throw new PatternSyntaxException("Explicit 'name separator' in class",
                                    globPattern, i - 1);
                        }
                        // TBD: how to specify ']' in a class?
                        if (c == '\\' || c == '[' ||
                                c == '&' && next(globPattern, i) == '&') {
                            // escape '\', '[' or "&&" for regex class
                            regex.append('\\');
                        }
                        regex.append(c);

                        if (c == '-') {
                            if (!hasRangeStart) {
                                throw new PatternSyntaxException("Invalid range",
                                        globPattern, i - 1);
                            }
                            if ((c = next(globPattern, i++)) == EOL || c == ']') {
                                break;
                            }
                            if (c < last) {
                                throw new PatternSyntaxException("Invalid range",
                                        globPattern, i - 3);
                            }
                            regex.append(c);
                            hasRangeStart = false;
                        } else {
                            hasRangeStart = true;
                            last = c;
                        }
                    }
                    if (c != ']') {
                        throw new PatternSyntaxException("Missing ']", globPattern, i - 1);
                    }
                    regex.append("]]");
                    break;
                case '{':
                    if (inGroup) {
                        throw new PatternSyntaxException("Cannot nest groups",
                                globPattern, i - 1);
                    }
                    regex.append("(?:(?:");
                    inGroup = true;
                    break;
                case '}':
                    if (inGroup) {
                        regex.append("))");
                        inGroup = false;
                    } else {
                        regex.append('}');
                    }
                    break;
                case ',':
                    if (inGroup) {
                        regex.append(")|(?:");
                    } else {
                        regex.append(',');
                    }
                    break;
                case '*':
                    if (next(globPattern, i) == '*') {
                        // crosses directory boundaries
                        regex.append(".*");
                        i++;
                    } else {
                        // within directory boundary
                        if (isDos) {
                            regex.append("[^\\\\]*");
                        } else {
                            regex.append("[^/]*");
                        }
                    }
                    break;
                case '?':
                   if (isDos) {
                       regex.append("[^\\\\]");
                   } else {
                       regex.append("[^/]");
                   }
                   break;

                default:
                    if (isRegexMeta(c)) {
                        regex.append('\\');
                    }
                    regex.append(c);
            }
        }

        if (inGroup) {
            throw new PatternSyntaxException("Missing '}", globPattern, i - 1);
        }

        return regex.append('$').toString();
    }

    static String toUnixRegexPattern(String globPattern) {
        return toRegexPattern(globPattern, false);
    }

    static String toWindowsRegexPattern(String globPattern) {
        return toRegexPattern(globPattern, true);
    }
}

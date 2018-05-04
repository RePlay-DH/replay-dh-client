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
package bwfdm.replaydh.ui.helper;

import static java.util.Objects.requireNonNull;

import java.io.File;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * A more flexible version of {@link FileNameExtensionFilter} that is not
 * limited to only look at the "raw" file ending (i.e. the part of the file's
 * name after the last '.' character). This implementations can match arbitrary
 * complex file ending with numerous dots in them and uses a call to
 * {@link String#endsWith(String)} on the full lower-case file name against all
 * registered extensions.
 *
 * @author Markus Gärtner
 *
 */
public class FileEndingFilter extends FileFilter {
    // Description of this filter.
    private final String description;
    private final String extension;
    // Cached ext
    private final String lowerCaseExtension;

    public FileEndingFilter(String description, String extension) {
        this.description = requireNonNull(description);
        this.extension = requireNonNull(extension);
        lowerCaseExtension = extension.toLowerCase();
    }

    /**
     * Tests the specified file, returning true if the file is
     * accepted, false otherwise. True is returned if the extension
     * matches one of the file name extensions of this {@code
     * FileFilter}, or the file is a directory.
     *
     * @param f the {@code File} to test
     * @return true if the file is to be accepted, false otherwise
     */
    @Override
	public boolean accept(File f) {
        if (f != null) {
            if (f.isDirectory()) {
                return true;
            }
            String fileName = f.getName().toLowerCase();
            if (fileName.endsWith(lowerCaseExtension)) {
                   return true;
            }
        }
        return false;
    }

    @Override
	public String getDescription() {
        return description;
    }

    public String getExtension() {
    	return extension;
    }

    @Override
	public String toString() {
        return super.toString() + "[description=" + getDescription() +
            " extension=" + getExtension() + "]";
    }
}

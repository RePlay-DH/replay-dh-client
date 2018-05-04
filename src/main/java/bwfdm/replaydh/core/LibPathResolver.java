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
package bwfdm.replaydh.core;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileSystems;

import org.java.plugin.standard.StandardPathResolver;

/**
 * @author Markus Gärtner
 * @version $Id: LibPathResolver.java 236 2014-03-13 14:49:16Z mcgaerty $
 *
 */
public class LibPathResolver extends StandardPathResolver {

	public LibPathResolver() {
		// no-op
	}

	@Override
	protected URL resolvePath(URL baseUrl, String path) {
		String context = baseUrl.toExternalForm();
		if(path.startsWith("lib/") && context.endsWith("!/")) {  //$NON-NLS-1$//$NON-NLS-2$
			context = stripJarContext(context);
			int offset = context.lastIndexOf(FileSystems.getDefault().getSeparator());
			if(offset==-1) {
				offset = context.lastIndexOf('/');
			}
			context = context.substring(0, offset+1);

			try {
				//System.out.printf("resolving lib: context=%s path=%s\n", context, path); //$NON-NLS-1$
				return new URL(context+path);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		return super.resolvePath(baseUrl, path);
	}

	protected String stripJarContext(String context) {
		int beginIndex = context.startsWith("jar:") ? 4 : 0; //$NON-NLS-1$
		int endIndex = context.length();
		if(context.endsWith("!/")) { //$NON-NLS-1$
			endIndex -= 2;
		}

		return context.substring(beginIndex, endIndex);
	}
}

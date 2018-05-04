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

import java.beans.Statement;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Markus Gärtner
 * @version $Id: WeakHandler.java 123 2013-07-31 15:22:01Z mcgaerty $
 *
 */
public abstract class WeakHandler {

	private Reference<Object> target;
	private final String methodName;

	private static final Logger log = LoggerFactory.getLogger(WeakHandler.class);

	public WeakHandler(Object target, String methodName) {
		requireNonNull(target);
		requireNonNull(methodName);

		this.target = new WeakReference<Object>(target);
		this.methodName = methodName;
	}

	public Object getTarget() {
		if(target==null) {
			return null;
		}
		return target.get();
	}

	public String getMethodName() {
		return methodName;
	}

	public boolean isObsolete() {
		return getTarget()==null;
	}

	protected void dispatch(Object...args) {
		Statement statement = new Statement(getTarget(), methodName, args);
		try {
			statement.execute();
		} catch(Exception e) {
			log.error("Failed to execute handler statement: {}", Arrays.toString(args), e);
		}
	}
}

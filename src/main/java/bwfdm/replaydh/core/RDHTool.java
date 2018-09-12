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

import bwfdm.replaydh.utils.PropertyChangeSource;

/**
 * @author Markus Gärtner
 *
 */
public interface RDHTool extends PropertyChangeSource {

	/**
	 * Initializes this tool with the given environment.
	 *
	 * @param environment
	 * @return {@code true} iff this tool initialized properly
	 */
	boolean start(RDHEnvironment environment) throws RDHLifecycleException;

	/**
	 * Shuts down this tool
	 * @param environment this argument is provide to support stateless tools
	 * that do not store the {@link RDHEnvironment} instance provided to the
	 * {@link #start(RDHEnvironment)} method.
	 */
	void stop(RDHEnvironment environment) throws RDHLifecycleException;
}

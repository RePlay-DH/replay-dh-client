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

/**
 * @author Markus Gärtner
 *
 */
public enum ToolLifecycleState {

	/**
	 * Proxy state when a tool hasn't been added to the client yet.
	 */
	UNKNOWN,

	/**
	 * Tool successfully {@link RDHTool#stop() stopped} or
	 * hasn't been {@link RDHTool#start(RDHEnvironment) started} yet.
	 */
	INACTIVE,

	/**
	 * Attempt to {@link RDHTool#start(RDHEnvironment) start}
	 * a tool has started but has not been completed yet.
	 */
	STARTING,

	/**
	 * Tool successfully {@link RDHTool#start(RDHEnvironment) started}.
	 */
	ACTIVE,

	/**
	 * Attempt to {@link RDHTool#stop() stop}
	 * a tool has started but has not been completed yet.
	 */
	STOPPING,

	/**
	 * Attempt to either {@link RDHTool#start(RDHEnvironment) start} or
	 * {@link RDHTool#stop() stop} failed or the tool's class
	 * could not be resolved properly.
	 */
	FAILED,
	;
}

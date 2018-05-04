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
package bwfdm.replaydh.workflow.impl;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Copied over generic graph node class
 *
 * @author Markus Gärtner
 *
 * @param <E> type of payload {@link #content()}
 */
public class Node<E extends Object> {
	private E content;

	private List<E> incoming, outgoing;
	private int flags = 0;

	private Map<String, Object> properties;

	protected Node(E content) {
		requireNonNull(content);

		this.content = content;
	}

	E content() {
		return content;
	}

	public Object setProperty(String key, Object value) {
		if(properties==null) {
			properties = new HashMap<>();
		}

		return properties.put(key, value);
	}

	public Object getProperty(String key) {
		return properties==null ? null : properties.get(key);
	}

	protected void dispose() {
		content = null;
		incoming = null;
		outgoing = null;
		flags = 0;
	}

	private List<E> incoming(boolean createIfMissing) {
		if(incoming==null && createIfMissing) {
			incoming = new ArrayList<>();
		}
		return incoming;
	}

	private List<E> outgoing(boolean createIfMissing) {
		if(outgoing==null && createIfMissing) {
			outgoing = new ArrayList<>();
		}
		return outgoing;
	}

	public boolean flagSet(int flag) {
		return (flags & flag) == flag;
	}

	public void setFlag(int flag, boolean active) {
		if(active) {
			flags |= flag;
		} else {
			flags &= ~flag;
		}
	}

	public boolean hasLink(E target) {
		List<E> outgoing = outgoing(false);
		return outgoing!=null && outgoing.contains(target);
	}

	public void addIncoming(E incoming) {
		incoming(true).add(incoming);
	}

	public void addOutgoing(E outgoing) {
		outgoing(true).add(outgoing);
	}

	public void forEachIncoming(Consumer<? super E> action) {
		List<E> incoming = incoming(false);
		if(incoming!=null) {
			incoming.forEach(action);
		}
	}

	public void forEachOutgoing(Consumer<? super E> action) {
		List<E> outgoing = outgoing(false);
		if(outgoing!=null) {
			outgoing.forEach(action);
		}
	}

	public int incomingCount() {
		return incoming==null ? 0 : incoming.size();
	}

	public int outgoingCount() {
		return outgoing==null ? 0 : outgoing.size();
	}

	public List<E> outgoing() {
		return outgoing(false);
	}

	public List<E> incoming() {
		return incoming(false);
	}
}

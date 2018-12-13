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
package bwfdm.replaydh.ui.config;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.java.plugin.registry.Extension;

import bwfdm.replaydh.core.PluginEngine;
import bwfdm.replaydh.ui.id.ExtensionIdentity;
import bwfdm.replaydh.ui.id.Identity;
import bwfdm.replaydh.ui.tree.AbstractTreeModel;

/**
 * Implementation note: This class is not thread-safe! It is intended
 * to only be used on the Event-Dispatch-Thread, which should obsolete
 * the need for synchronization.
 *
 * @author Markus Gärtner
 *
 */
public class PreferencesTreeModel extends AbstractTreeModel {

	private static final String TAB_ID = "PreferencesTab";
	private static final String GROUP_ID = "PreferencesGroup";

	private final PluginEngine pluginEngine;

	public PreferencesTreeModel(PluginEngine pluginEngine) {
		super(new Node());

		this.pluginEngine = requireNonNull(pluginEngine);

		Collection<Extension> tabExtensions = pluginEngine.getExtensions(
				PluginEngine.CORE_PLUGIN_ID, TAB_ID);

		final Map<Extension, Node> nodes = new HashMap<>();

		for(Extension extension : tabExtensions) {

			Node parent = getRoot();
			Extension.Parameter groupDef = extension.getParameter("group");
			if(groupDef!=null) {
				parent = ensureGroup(groupDef.valueAsExtension(), nodes);
			}

			Node node = new Node(parent, extension, pluginEngine);
			parent.addElement(node);
		}
	}

	private Node ensureGroup(Extension group, Map<Extension, Node> nodes) {
		Node node = nodes.get(group);

		if(node==null) {
			Node parent = getRoot();
			Extension.Parameter parentDef = group.getParameter("parent");
			if(parentDef!=null) {
				Extension parentExt = parentDef.valueAsExtension();
				parent = ensureGroup(parentExt, nodes);
			}

			node = new Node(parent, group, pluginEngine);
			parent.addElement(node);
			nodes.put(group, node);
		}

		return node;
	}

	/**
	 * @see bwfdm.replaydh.ui.tree.AbstractTreeModel#getIndexOfChild(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int getIndexOfChild(Object parent, Object child) {
		checkNode(parent);
		checkNode(child);

		Node nChild = (Node) child;
		if(nChild.parent!=parent) {
			return -1;
		}

		Node nParent = (Node) parent;
		if(nParent.elements==null || nParent.elements.isEmpty()) {
			return -1;
		}

		for(int i=0; i<nParent.elements.size(); i++) {
			if(nParent.elements.get(i)==child) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * @see bwfdm.replaydh.ui.tree.AbstractTreeModel#getRoot()
	 */
	@Override
	public Node getRoot() {
		return (Node) super.getRoot();
	}

	private void checkNode(Object node) {
		requireNonNull(node);
		if(!Node.class.isInstance(node))
			throw new IllegalArgumentException("Invalid node class: "+node.getClass());
	}

	/**
	 * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
	 */
	@Override
	public Object getChild(Object parent, int index) {
		checkNode(parent);
		Node node = (Node) parent;
		if(node.elements==null)
			throw new IllegalStateException("Node has no children: illegal index "+index);
		return node.elements.get(index);
	}

	/**
	 * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
	 */
	@Override
	public int getChildCount(Object parent) {
		checkNode(parent);
		Node node = (Node) parent;
		return node.elements==null ? 0 : node.elements.size();
	}

	public enum NodeType {
		/**
		 * Describes the singular root node
		 */
		ROOT,
		/**
		 * Describes intermediary nodes with no own content.
		 */
		PROXY,
		/**
		 * Describes nodes that have actual tab content
		 */
		TAB,
		;
	}

	public static class Node {
		NodeType type = NodeType.PROXY;

		/**
		 * Plugin element to load identity data and the tab
		 * class from.
		 */
		final Extension extension;

		Identity label;

		/**
		 * Uplink to parent node or {@code null} if this node is the root
		 */
		final Node parent;

		/**
		 * Children
		 */
		List<Node> elements;

		Node() {
			extension = null;
			parent = null;
			type = NodeType.ROOT;
		}

		Node(Node parent, String path) {
			extension = null;
			this.parent = requireNonNull(parent);
			type = NodeType.PROXY;
		}

		Node(Node parent, Extension extension, PluginEngine pluginEngine) {
			this.parent = requireNonNull(parent);
			this.extension = requireNonNull(extension);
			type = NodeType.TAB;
			label = new ExtensionIdentity(extension, pluginEngine);
		}

		void addElement(Node node) {
			if(elements==null) {
				elements = new ArrayList<>();
			}
			elements.add(node);
		}

		boolean hasElement(Node node) {
			return elements!=null && elements.contains(node);
		}

		public Extension getExtension() {
			return extension;
		}

		public Identity getLabel() {
			return label;
		}

		public NodeType getType() {
			return type;
		}
	}
}

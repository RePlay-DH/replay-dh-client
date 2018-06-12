/*
 *  ICARUS -  Interactive platform for Corpus Analysis and Research tools, University of Stuttgart
 *  Copyright (C) 2012-2013 Markus Gärtner and Gregor Thiele
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * $Revision: 458 $
 * $Date: 2016-05-02 17:24:40 +0200 (Mo, 02 Mai 2016) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus/core/de.ims.icarus/source/de/ims/icarus/ui/tree/AbstractTreeModel.java $
 *
 * $LastChangedDate: 2016-05-02 17:24:40 +0200 (Mo, 02 Mai 2016) $
 * $LastChangedRevision: 458 $
 * $LastChangedBy: mcgaerty $
 */
package bwfdm.replaydh.ui.tree;

import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * @author Markus Gärtner
 * @version $Id: AbstractTreeModel.java 458 2016-05-02 15:24:40Z mcgaerty $
 *
 */
public abstract class AbstractTreeModel implements TreeModel {

	protected EventListenerList listeners = new EventListenerList();

	protected Object root;

	protected AbstractTreeModel(Object root) {
		if (root == null)
			throw new NullPointerException("Invalid root"); //$NON-NLS-1$

		this.root = root;
	}

	protected AbstractTreeModel() {
		this(new Root());
	}

	/**
	 * @see javax.swing.tree.TreeModel#getRoot()
	 */
	@Override
	public Object getRoot() {
		return root;
	}

	public boolean isRoot(Object node) {
		return root==node;
	}

	public int getRootChildCount() {
		return getChildCount(getRoot());
	}

	/**
	 * @see javax.swing.tree.TreeModel#isLeaf(java.lang.Object)
	 */
	@Override
	public boolean isLeaf(Object node) {
		return getChildCount(node)==0;
	}

	/**
	 * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int getIndexOfChild(Object parent, Object child) {
		return -1;
	}

	/**
	 * @see javax.swing.tree.TreeModel#valueForPathChanged(javax.swing.tree.TreePath, java.lang.Object)
	 */
	@Override
	public void valueForPathChanged(TreePath path, Object newValue) {
		// no-op
	}

	protected void fireNewRoot() {
        Object[] pairs = listeners.getListenerList();

		TreePath path = new TreePath(getRoot());

		TreeModelEvent event = null;

		for (int i = pairs.length - 2; i >= 0; i -= 2) {
			if (pairs[i] == TreeModelListener.class) {
				if (event == null) {
					event = new TreeModelEvent(this, path, null, null);
				}

				((TreeModelListener) pairs[i + 1]).treeStructureChanged(event);
			}
		}
	}

	/**
	 * Call when the entire tree structure has changed
	 */
	protected void fireStructureChanged() {
		fireTreeStructureChanged(new TreePath(getRoot()));
	}

	/**
	 * Call when a node has changed its leaf state.
	 */
	protected void firePathLeafStateChanged(TreePath path) {
		fireTreeStructureChanged(path);
	}

	/**
	 * Call when the tree structure below the path has completely changed.
	 */
	protected void fireTreeStructureChanged(TreePath parentPath) {
		Object[] pairs = listeners.getListenerList();

		TreeModelEvent event = null;

		for (int i = pairs.length - 2; i >= 0; i -= 2) {
			if (pairs[i] == TreeModelListener.class) {
				if (event == null) {
					event = new TreeModelEvent(this, parentPath, null, null);
				}

				((TreeModelListener) pairs[i + 1]).treeStructureChanged(event);
			}
		}
	}

	/**
	 * Call when the path itself has changed, but no structure changes have
	 * occurred.
	 */
	protected void firePathChanged(TreePath path) {
		Object node = path.getLastPathComponent();
		TreePath parentPath = path.getParentPath();

		if (parentPath == null) {
			fireChildrenChanged(path, null, null);
		} else {
			Object parent = parentPath.getLastPathComponent();

			fireChildChanged(parentPath, getIndexOfChild(parent, node), node);
		}
	}

	protected void fireChildAdded(TreePath parentPath, int index, Object child) {
		fireChildrenAdded(parentPath, new int[] { index }, new Object[] { child });
	}

	protected void fireChildChanged(TreePath parentPath, int index, Object child) {
		fireChildrenChanged(parentPath, new int[] { index }, new Object[] { child });
	}

	protected void fireChildRemoved(TreePath parentPath, int index, Object child) {
		fireChildrenRemoved(parentPath, new int[] { index }, new Object[] { child });
	}

	protected void fireChildrenAdded(TreePath parentPath, int[] indices, Object[] children) {
		Object[] pairs = listeners.getListenerList();

		TreeModelEvent event = null;

		for (int i = pairs.length - 2; i >= 0; i -= 2) {
			if (pairs[i] == TreeModelListener.class) {
				if (event == null) {
					event = new TreeModelEvent(this, parentPath, indices, children);
				}

				((TreeModelListener) pairs[i + 1]).treeNodesInserted(event);
			}
		}
	}

	protected void fireChildrenChanged(TreePath parentPath, int[] indices, Object[] children) {
		Object[] pairs = listeners.getListenerList();

		TreeModelEvent event = null;

		for (int i = pairs.length - 2; i >= 0; i -= 2) {
			if (pairs[i] == TreeModelListener.class) {
				if (event == null) {
					event = new TreeModelEvent(this, parentPath, indices, children);
				}

				((TreeModelListener) pairs[i + 1]).treeNodesChanged(event);
			}
		}
	}

	protected void fireChildrenRemoved(TreePath parentPath, int[] indices, Object[] children) {
		Object[] pairs = listeners.getListenerList();

		TreeModelEvent event = null;

		for (int i = pairs.length - 2; i >= 0; i -= 2) {
			if (pairs[i] == TreeModelListener.class) {
				if (event == null) {
					event = new TreeModelEvent(this, parentPath, indices, children);
				}
				((TreeModelListener) pairs[i + 1]).treeNodesRemoved(event);
			}
		}
	}

	/**
	 * @see javax.swing.tree.TreeModel#addTreeModelListener(javax.swing.event.TreeModelListener)
	 */
	@Override
	public void addTreeModelListener(TreeModelListener listener) {
		if (listener == null)
			throw new NullPointerException("Invalid listener"); //$NON-NLS-1$

		listeners.add(TreeModelListener.class, listener);
	}

	/**
	 * @see javax.swing.tree.TreeModel#removeTreeModelListener(javax.swing.event.TreeModelListener)
	 */
	@Override
	public void removeTreeModelListener(TreeModelListener listener) {
		if (listener == null)
			throw new NullPointerException("Invalid listener"); //$NON-NLS-1$

		listeners.remove(TreeModelListener.class, listener);
	}

	public static class Root {

		/**
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "root"; //$NON-NLS-1$
		}

	}
}

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
package bwfdm.replaydh.ui.actions;

import static java.util.Objects.requireNonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A logical grouping of actions identified by their 'ids'.
 *
 * @author Markus Gärtner
 * @version $Id: ActionSet.java 123 2013-07-31 15:22:01Z mcgaerty $
 *
 */
public class ActionSet {

	private final String id;
	private final WeakReference<Object> owner;
	private List<String> actionIds;
	private Map<String, String> groupMap;

	public ActionSet(String id) {
		this(id, null);
	}

	public ActionSet(String id, Object owner) {
		requireNonNull(id);

		this.id = id;

		this.owner = owner==null ? null : new WeakReference<Object>(owner);
	}

	private void checkOwner(Object owner) {
		Object realOwner = this.owner==null ? null : this.owner.get();
		if(owner==null || (realOwner!=null && realOwner!=owner))
			throw new IllegalArgumentException("Illegal access attempt - supplied owner object is invalid: "+String.valueOf(owner)); //$NON-NLS-1$
	}

	public String getId() {
		return id;
	}

	public String[] getActionIds() {
		if(actionIds==null) {
			return new String[0];
		}
		return actionIds.toArray(new String[actionIds.size()]);
	}

	public String getActionIdAt(int index) {
		return actionIds==null ? null : actionIds.get(index);
	}

	public boolean isEmpty() {
		return actionIds==null ? true : actionIds.isEmpty();
	}

	public int size() {
		return actionIds==null ? 0 : actionIds.size();
	}

	public boolean contains(String actionId) {
		return actionIds==null ? false : actionIds.contains(actionId);
	}

	void add(String actionId, String groupId) {
		requireNonNull(actionId);
		if(actionIds==null) {
			actionIds = new ArrayList<>();
		}

		actionIds.add(actionId);
		if(groupId!=null) {
			mapGroup(actionId, groupId);
		}
	}

	public void add(Object owner, String actionId, String groupId) {
		checkOwner(owner);
		add(actionId, groupId);
	}

	public String getGroupId(String actionId) {
		return groupMap==null ? null : groupMap.get(actionId);
	}

	void mapGroup(String actionId, String groupId) {
		if(groupMap==null) {
			groupMap = new HashMap<>();
		}

		if(groupId==null) {
			groupMap.remove(actionId);
		} else {
			groupMap.put(actionId, groupId);
		}
	}

	public void mapGroup(Object owner, String actionId, String groupId) {
		checkOwner(owner);
		requireNonNull(actionId);

		mapGroup(actionId, groupId);
	}
}

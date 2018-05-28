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

import java.awt.Component;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JSeparator;


/**
 * An {@code ActionList} encapsulates a collection of action related
 * objects that can be used to construct menus, tool-bars, pop-ups and
 * the like. It basically holds a list of identifier {@code Strings} or
 * {@code null} values that are each associated with a certain {@code ResourceEntryType}.
 * Each {@code ActionList} created by the {@code ActionManager} or other
 * framework elements is immutable (i.e. it was created using a {@code null}
 * owner). All attempts to modify those lists by client code outside the
 * framework will cause {@code IllegalArgumentException} being thrown.
 * If an application wants to create their own {@code ActionList} instance
 * and modify it at runtime it can use the {@link #ActionList(String, Object)}
 * constructor to pass an {@code owner} object to the list that will serve
 * as a kind of {@code key} to access methods that structurally modify the list
 * or some of its properties.
 *
 * @author Markus Gärtner
 * @version $Id: ActionList.java 389 2015-04-23 10:19:15Z mcgaerty $
 *
 */
public final class ActionList {

	private List<ListEntry> list;
	private final String id;
	private final WeakReference<Object> owner;
	private String actionId;
	private Map<String, String> groupMap;

	/**
	 * Creates an {@code ActionList} that will be immutable
	 * for code outside the framework. That means all public methods
	 * that take an {@code owner} argument will throw {@code IllegalArgumentException}
	 * on every attempt to call them regardless of the {@code Object} passed
	 * as {@code owner} to the specific call.
	 * @param id the global identifier used to address this list
	 */
	public ActionList(String id) {
		this(id, null);
	}

	/**
	 * Creates an {@code ActionList} that will be immutable
	 * for all code besides the framework and the holder of the {@code owner}
	 * object. Calls to restricted methods like {@link #add(Object, ResourceEntryType, String)}
	 * will throw {@code IllegalArgumentException} if the given {@code owner}
	 * argument does not match the initial value set in this constructor or
	 * the initial {@code owner} is {@code null}. Note that framework members
	 * can still bypass those restriction by using the package private methods!
	 *
	 * @param id the global identifier used to address this list
	 * @param owner {@code "key"} to access restricted methods on this list or
	 * {@code null} if this {@code ActionList} is meant to be immutable
	 */
	public ActionList(String id, Object owner) {
		requireNonNull(id);

		this.id = id;

		this.owner = owner==null ? null : new WeakReference<Object>(owner);
	}

	/**
	 * Returns the global identifier assigned to this list
	 */
	public String getId() {
		return id;
	}

	/**
	 * Fetches the {@code value} this list stores at the given {@code index}.
	 * Note that the meaning of this value is depending on the {@code ResourceEntryType}
	 * defined for that {@code index} and might even be {@code null}.
	 */
	public String getValueAt(int index) {
		if(list!=null) {
			ListEntry entry = list.get(index);
			return entry.getValue();
		}
		return null;
	}

	/**
	 * Fetches the {@code ResourceEntryType} that describes the actual
	 * 'content' of data stored at the given {@code index}.
	 */
	public EntryType getTypeAt(int index) {
		if(list!=null) {
			ListEntry entry = list.get(index);
			return entry.getType();
		}
		return null;
	}

	/**
	 * Fetches the {@code condition} if any that can be used to
	 * determine whether to actually build a component for the given {@code index}.
	 */
	public String getConditionAt(int index) {
		if(list!=null) {
			ListEntry entry = list.get(index);
			return entry.getCondition();
		}
		return null;
	}

	/**
	 * Returns the number of elements in this list or {@code 0}
	 * if it is empty
	 */
	public int size() {
		return list==null ? 0 : list.size();
	}

	private void checkOwner(Object owner) {
		Object realOwner = this.owner==null ? null : this.owner.get();
		if(owner==null || (realOwner!=null && realOwner!=owner))
			throw new IllegalArgumentException("Illegal access attempt - supplied owner object is invalid: "+String.valueOf(owner)); //$NON-NLS-1$
	}

	/**
	 * Replaces the {@code type} and {@code value} objects at the specified
	 * {@code index} by the given arguments.
	 * <p>
	 * Before actual modifications take place the supplied {@code owner}
	 * will be checked against the one that was set at constructor time.
	 * If this internal {@code owner} is {@code null} or the given one
	 * differs from it an {@code IllegalArgumentException} will be thrown.
	 * @param index the index the modifications will take place
	 * @param owner {@code key} to access this restricted method
	 * @param type the new {@code ResourceEntryType} or {@code null} if the type
	 * should not be changed
	 * @param value object to replace the old {@code value} or {@code null}
	 * if no changes are intended
	 */
	public void set(int index, Object owner, EntryType type, String value) {
		checkOwner(owner);
		if(list!=null) {
			ListEntry entry = list.get(index);
			entry.type = type==null ? entry.type : type;
			entry.value = value==null ? entry.value : value;
		}
	}

	void add(EntryType type, String value, String condition) {
		requireNonNull(type);

		if(list==null) {
			list = new ArrayList<>();
		}

		ListEntry entry = new ListEntry(type, value, condition);
		list.add(entry);
	}

	/**
	 * Adds the {@code type} and {@code value} objects to the end
	 * of this list.
	 * <p>
	 * Before actual modifications take place the supplied {@code owner}
	 * will be checked against the one that was set at constructor time.
	 * If this internal {@code owner} is {@code null} or the given one
	 * differs from it an {@code IllegalArgumentException} will be thrown.
	 * @param owner {@code key} to access this restricted method
	 * @param type the {@code ResourceEntryType} to be added, must not be {@code null}
	 * @param value {@code String} to be added, may be {@code null}
	 */
	public void add(Object owner, EntryType type, String value, String condition) {
		checkOwner(owner);
		add(type, value, condition);
	}

	/**
	 * Returns the identifier of an {@code Action} that is meant to
	 * be used to {@code activate} this list or {@code null} if this list
	 * is not associated with any action.
	 */
	public String getActionId() {
		return actionId;
	}

	void setActionId(String actionId) {
		this.actionId = actionId;
	}

	/**
	 *
	 * @param owner
	 * @param actionId
	 */
	public void setActionId(Object owner, String actionId) {
		checkOwner(owner);
		setActionId(actionId);
	}

	public String getGroupId(String actionId) {
		return groupMap==null ? null : groupMap.get(actionId);
	}

	void mapGroup(String actionId, String groupId) {
		if(groupMap==null)
			groupMap = new HashMap<>();

		if(groupId==null)
			groupMap.remove(actionId);
		else
			groupMap.put(actionId, groupId);
	}

	public void mapGroup(Object owner, String actionId, String groupId) {
		checkOwner(owner);
		requireNonNull(actionId);

		mapGroup(actionId, groupId);
	}

	/**
	 *
	 * @author Markus Gärtner
	 * @version $Id: ActionList.java 389 2015-04-23 10:19:15Z mcgaerty $
	 *
	 */
	private static class ListEntry implements Cloneable {
		private EntryType type;
		private String value;
		private String condition;

		/**
		 * @param type
		 * @param value
		 */
		ListEntry(EntryType type, String value, String condition) {
			this.type = type;
			this.value = value;
			this.condition = condition;
		}

		@Override
		public ListEntry clone() {
			try {
				return (ListEntry) super.clone();
			} catch (CloneNotSupportedException e) {
				throw new InternalError(e);
			}
		}

		/**
		 * @return the type
		 */
		public EntryType getType() {
			return type;
		}

		/**
		 * @return the value
		 */
		public String getValue() {
			return value;
		}

		/**
		 * @return the condition
		 */
		public String getCondition() {
			return condition;
		}

		@Override
		public String toString() {
			return String.format("type: %s value: %s condition: %s", type, value, condition); //$NON-NLS-1$
		}
	}

	/**
	 * Type definitions used for entries in an {@link ActionList}.
	 * @author Markus Gärtner
	 * @version $Id: ActionList.java 389 2015-04-23 10:19:15Z mcgaerty $
	 *
	 */
	public enum EntryType {

		/**
		 * Marks the separation of two elements in the list. Members of the
		 * framework will honor this type by using one of the various
		 * {@code addSeparator()} methods in the {@code Swing} classes or
		 * add a new instance of {@link JSeparator} depending on the {@code value}
		 * of the entry.
		 */
		SEPARATOR,

		/**
		 * Links an entry to another {@code ActionList} instance. An implementation
		 * specific component (typically a button) will be placed at the
		 * corresponding index that expands the referenced list when clicked.
		 */
		ACTION_LIST_ID,

		/**
		 * Points to a collection of {@code Action}s encapsulated in
		 * an {@code ActionSet}. Each element of this collection will be
		 * added in sequential order.
		 */
		ACTION_SET_ID,

		/**
		 * References a single action to be added.
		 */
		ACTION_ID,

		/**
		 * Makes the framework insert a {@code JLabel} that will be
		 * localized using the corresponding {@code value} as key to obtain
		 * the localized {@code String} for {@link JLabel#setText(String)}.
		 */
		LABEL,

		/**
		 * Inserts an implementation specific placeholder that typically
		 * is roughly the same size as a regular action component for the
		 * current container. It is possible to assign a specific size value
		 * that determines either the width or height of the inserted component
		 * depending on the type of action component the containing list
		 * is converted into.
		 */
		EMPTY,

		/**
		 * Inserts an implementation specific 'glue' component that consumes
		 * free space when available. Note that typically only tool-bar
		 * components support such behavior.
		 */
		GLUE,

		/**
		 * Mightiest type to assign to an entry.
		 * <p>
		 * When asked to build action based components the framework can
		 * be supplied a {@code Map} of properties. For each {@code placeholder}
		 * encountered this map will be queried with the actual {@code value}
		 * of the entry as key. When not {@code null} the result will be
		 * handled in the following way:
		 * <ul>
		 * 	<li>if it is a {@code String} then it will be handled as {@link ResourceEntryType#LABEL}</li>
		 * 	<li>if it is an {@link Action} then it will be added directly</li>
		 * 	<li>if it is an {@link ActionSet} all its elements will be added sequentially</li>
		 * 	<li>if it is an {@link ActionList} the framework will either wrap the
		 * 		list into a new implementation specific {@code Component} and add
		 * 		an {@code Action} responsible for showing this component or it will
		 * 		"inline" the list into the current construction process</li>
		 * 	<li>if it is a {@link Component} it will be added directly (some members
		 * 		of the framework might resize the component to fit their requirements</li>
		 *  <li>all other types are ignored</li>
		 * </ul>
		 */
		CUSTOM;


		public static EntryType parse(String text) {
			requireNonNull(text);
			switch(text.toLowerCase()) {
			case "separator": return SEPARATOR; //$NON-NLS-1$
			case "action-list": return ACTION_LIST_ID; //$NON-NLS-1$
			case "action-set": return ACTION_SET_ID; //$NON-NLS-1$
			case "action": return ACTION_ID; //$NON-NLS-1$
			case "label": return LABEL; //$NON-NLS-1$
			case "custom": return CUSTOM; //$NON-NLS-1$
			case "empty": return SEPARATOR; //$NON-NLS-1$
			case "glue": return GLUE; //$NON-NLS-1$
			default:
				throw new IllegalArgumentException("Unknown entry-type: "+text); //$NON-NLS-1$
			}
		}
	}
}

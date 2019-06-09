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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import bwfdm.replaydh.core.RDHClient;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.actions.ActionList.EntryType;
import bwfdm.replaydh.ui.helper.ModifiedFlowLayout;
import bwfdm.replaydh.ui.helper.WeakHandler;
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.ui.icons.Resolution;

/**
 * @author Markus Gärtner
 * @version $Id: ActionManager.java 389 2015-04-23 10:19:15Z mcgaerty $
 *
 */
public class ActionManager {

	public static final String DIRECTION_PARAMETER = "direction"; //$NON-NLS-1$
	public static final String FILL_TOOLBAR = "fillToolBar"; //$NON-NLS-1$

	public static final String RIGHT_TO_LEFT = "rightToLeft"; //$NON-NLS-1$
	public static final String LEFT_TO_RIGHT = "leftToRight"; //$NON-NLS-1$

	public static final String SEPARATOR_SMALL = "small"; //$NON-NLS-1$
	public static final String SEPARATOR_MEDIUM = "medium"; //$NON-NLS-1$
	public static final String SEPARATOR_WIDE = "wide"; //$NON-NLS-1$

	public static final String SMALL_SELECTED_ICON_KEY = "RDH_SmallSelectedIcon"; //$NON-NLS-1$
	public static final String LARGE_SELECTED_ICON_KEY = "RDH_LargeSelectedIcon"; //$NON-NLS-1$

	public static final String HELP_ANCHOR_KEY = "RDH_HelpAnchor"; //$NON-NLS-1$

	private volatile ResourceManager resourceManager;
	private volatile IconRegistry iconRegistry;
	private volatile ConditionResolver conditionResolver;

	private final ActionManager parent;

	private boolean silent = false;

	protected Map<String, Action> actionMap;
	protected Map<String, ActionSet> actionSetMap;
	protected Map<String, ActionList> actionListMap;
	protected Map<Integer, ButtonGroup> groupMap;
	protected Map<String, ActionAttributes> attributeMap;

	protected final Object lock = new Object();

	private static volatile ActionManager instance;

	private static final Logger log = LoggerFactory.getLogger(ActionManager.class);

	public static ActionManager globalManager() {
		if(instance==null) {
			synchronized (ActionManager.class) {
				if(instance==null) {
					ActionManager manager = new ActionManager(null, null, null, null);

					URL actionLocation = ActionManager.class.getResource(
							"default-actions.xml"); //$NON-NLS-1$
					if(actionLocation==null)
						throw new IllegalStateException("Missing resources: default-actions.xml"); //$NON-NLS-1$

					try {
						manager.loadActions(actionLocation);
					} catch (IOException e) {
						log.error("Failed to load actions from file: {}", actionLocation, e); //$NON-NLS-1$
					}

					instance = manager;
				}
			}
		}

		return instance;
	}

	protected ComponentHandler toolBarHandler;
	protected ComponentHandler menuHandler;
	protected ComponentHandler popupMenuHandler;
	protected ComponentHandler menuBarHandler;

	protected Set<String> loadedResources;

	/**
	 *
	 */
	public ActionManager(ActionManager parent, ResourceManager resourceManager, IconRegistry iconRegistry, ConditionResolver conditionResolver) {
		this.parent = parent;
		this.resourceManager = resourceManager;
		this.iconRegistry = iconRegistry;
		this.conditionResolver = conditionResolver;

		// Inherit silent flag from parent
		if(parent!=null) {
			setSilent(parent.isSilent());
		}
	}

	public ResourceManager getResourceManager() {
		if(resourceManager==null) {
			synchronized (this) {
				if(resourceManager==null && parent!=null)
					resourceManager = parent.getResourceManager();

				if(resourceManager==null)
					resourceManager = ResourceManager.getInstance();
			}
		}

		return resourceManager;
	}

	public IconRegistry getIconRegistry() {
		if(iconRegistry==null) {
			synchronized (this) {
				if(iconRegistry==null && parent!=null)
					iconRegistry = parent.getIconRegistry();

				if(iconRegistry==null)
					iconRegistry = IconRegistry.getGlobalRegistry();
			}
		}

		return iconRegistry;
	}

	/**
	 * @return the conditionResolver
	 */
	public ConditionResolver getConditionResolver() {
		if(conditionResolver==null) {
			synchronized (this) {
				if(conditionResolver==null && parent!=null)
					conditionResolver = parent.getConditionResolver();

				if(conditionResolver==null)
					conditionResolver = ConditionResolver.EMPTY_RESOLVER;
			}
		}

		return conditionResolver;
	}

	public ActionManager derive() {
		return new ActionManager(this, null, null, null);
	}

	public ActionManager derive(ResourceManager resourceManager) {
		return new ActionManager(this, resourceManager, null, null);
	}

	public ActionManager derive(ResourceManager resourceManager, IconRegistry iconRegistry) {
		return new ActionManager(this, resourceManager, iconRegistry, null);
	}

	public ActionManager derive(ResourceManager resourceManager, IconRegistry iconRegistry, ConditionResolver conditionResolver) {
		return new ActionManager(this, resourceManager, iconRegistry, conditionResolver);
	}

	protected boolean isSilent() {
		return silent;
	}

	protected void setSilent(boolean silent) {
		this.silent = silent;
	}

	public ActionManager getParent() {
		return parent;
	}

	protected DelegateAction getDelegateAction(String id) {
		Action a = getAction(id);
		if(a instanceof DelegateAction) {
			return (DelegateAction)a;
		}
		return null;
	}

	protected StateChangeAction getStateChangeAction(String id) {
		Action a = getAction(id);
		if(a instanceof StateChangeAction) {
			return (StateChangeAction)a;
		}
		return null;
	}

	protected void addAttributes(Attributes attrs) {
		if (attributeMap == null) {
			attributeMap = new HashMap<>();
		}
		attributeMap.put(attrs.getValue(ID_ATTRIBUTE), new ActionAttributes(attrs));
	}

	protected ActionAttributes getAttributes(String key) {
		ActionAttributes attributes = null;

		if (attributeMap != null) {
			attributes = attributeMap.get(key);
		}

		if(attributes==null && parent!=null) {
			attributes = parent.getAttributes(key);
		}

		return attributes;
	}

	public Action deriveAction(String id, String templateId) {
		requireNonNull(id);
		requireNonNull(templateId);
		if(actionMap==null) {
			actionMap = new HashMap<>();
		}

		ActionAttributes attr = getAttributes(templateId);
		if(attr==null)
			throw new IllegalArgumentException("Unknown template id: "+templateId); //$NON-NLS-1$

		return createAction(attr, id);
	}

	protected Action findAction(String id) {
		Action action = null;
		if(actionMap!=null) {
			action = actionMap.get(id);
		}

		if(action==null && parent!=null) {
			action = parent.findAction(id);
		}

		return action;
	}

	public Action getAction(String id) {
		requireNonNull(id);

		// Search for action all along the parent line
		Action action = findAction(id);

		// Action already found -> return it
		if(action!=null) {
			return action;
		}

		// Fetch attributes to create action from
		ActionAttributes attr = getAttributes(id);

		if(attr==null && !isSilent())
			throw new IllegalArgumentException("Unknown action id: "+id); //$NON-NLS-1$

		// Virtual actions are not supposed to be instantiated directly
		if(attr!=null && Boolean.parseBoolean(attr.getValue(VIRTUAL_INDEX)))
			throw new IllegalArgumentException("Cannot instantiate virtual action: "+id); //$NON-NLS-1$

		// Create new action
		return createAction(attr, null);
	}

	public void addAction(String id, Action action) {
		requireNonNull(id);
		requireNonNull(action);

		if(actionMap==null) {
			actionMap = new HashMap<>();
		}

		synchronized (lock) {
			if(actionMap.containsKey(id) && !isSilent())
				throw new IllegalArgumentException("Duplicate action id: "+id); //$NON-NLS-1$

			actionMap.put(id, action);
		}
	}

	protected Action createAction(ActionAttributes attr, String id) {
		Action action = null;
		if (attr != null) {
			// For deriving actions from a template we have to use
			// the given id instead of the template defined one
			if(id==null) {
				id = attr.getValue(ID_INDEX);
			}

			String type = attr.getValue(TYPE_INDEX);
			if ("toggle".equals(type)) { //$NON-NLS-1$
				action = new StateChangeAction();
			} else {
				action = new DelegateAction();
			}
			configureAction(action, attr, null, id);

			addAction(id, action);
		}
		return action;
	}

	private void configureAction(Action action, ActionAttributes attr, ActionAttributes orig, String id) {
		if(orig==null) {
			orig = attr;
		}

		if(attr.hasValue(TEMPLATE_INDEX)) {
			String templateId = attr.getValue(TEMPLATE_INDEX);
			ActionAttributes tplAttr = getAttributes(templateId);
			if(tplAttr==null && !silent)
				throw new IllegalArgumentException("Unknown template id: "+templateId); //$NON-NLS-1$

			if(tplAttr!=null) {
				configureAction(action, tplAttr, orig, id);
			}
		}

		action.putValue(Action.NAME, attr.getValue(NAME_INDEX));
		if(attr.hasValue(SMALL_SELECTED_ICON_INDEX)) {
			action.putValue(SMALL_SELECTED_ICON_KEY, getIconRegistry().getIcon(
					attr.getValue(SMALL_SELECTED_ICON_INDEX), Resolution.forSize(16)));
		}
		if(attr.hasValue(SMALL_ICON_INDEX)) {
			action.putValue(Action.SMALL_ICON, getIconRegistry().getIcon(
					attr.getValue(SMALL_ICON_INDEX), Resolution.forSize(16)));
		}
		if(attr.hasValue(LARGE_SELECTED_ICON_INDEX)) {
			action.putValue(LARGE_SELECTED_ICON_KEY, getIconRegistry().getIcon(
					attr.getValue(LARGE_SELECTED_ICON_INDEX)));
		}
		if(attr.hasValue(LARGE_ICON_INDEX)) {
			action.putValue(Action.LARGE_ICON_KEY, getIconRegistry().getIcon(
					attr.getValue(LARGE_ICON_INDEX)));
		}
		String command = attr.getValue(COMMAND_INDEX);
		if(command==null) {
			command = id;
		}
		if(attr.hasValue(COMMAND_INDEX)) {
			action.putValue(Action.ACTION_COMMAND_KEY, command);
		}
		if(attr.hasValue(DESC_INDEX)) {
			action.putValue(Action.SHORT_DESCRIPTION, attr.getValue(DESC_INDEX));
			action.putValue(Action.LONG_DESCRIPTION, attr.getValue(DESC_INDEX));
		}

		String mnemonic = attr.getValue(MNEMONIC_INDEX);
		if (mnemonic != null && !mnemonic.equals("")) { //$NON-NLS-1$
			action.putValue(Action.MNEMONIC_KEY,
					new Integer(mnemonic.charAt(0)));
		}
		String accel = attr.getValue(ACCEL_INDEX);
		if (accel != null && !accel.equals("")) { //$NON-NLS-1$
			action.putValue(Action.ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(accel));
		}

		String help = attr.getValue(SHOW_HELP_INDEX);
		if (help != null && Boolean.parseBoolean(help)) {
			action.putValue(HELP_ANCHOR_KEY, id);
		}

		// Finally apply localization (allows for inheritance of loca keys from templates)
		if(orig==attr) {
			String name = (String)action.getValue(Action.NAME);
			if(name!=null)
				action.putValue(Action.NAME, getResourceManager().get(name));

			String description = (String)action.getValue(Action.SHORT_DESCRIPTION);
			if(description!=null) {
				description = getResourceManager().get(description);
				description = processActionDescription(description);
				action.putValue(Action.SHORT_DESCRIPTION, description);
			}
		}
	}

	protected String processActionDescription(String description) {
		return GuiUtils.toSwingTooltip(description);
	}

	public ActionSet getActionSet(String id) {
		requireNonNull(id);
		ActionSet actionSet = null;

		if(actionSetMap!=null) {
			actionSet = actionSetMap.get(id);
		}

		if(actionSet==null && parent!=null) {
			try {
				actionSet = parent.getActionSet(id);
			} catch(IllegalArgumentException e) {
				// ignore the 'silent' setting of parent managers
			}
		}

		if(actionSet==null && !isSilent())
			throw new IllegalArgumentException("Unknown action-set id: "+id); //$NON-NLS-1$

		return actionSet;
	}

	public void addActionSet(String id, ActionSet actionSet) {
		requireNonNull(id);
		requireNonNull(actionSet);

		if(actionSetMap==null) {
			actionSetMap = new HashMap<>();
		}

		synchronized (lock) {
			if(actionSetMap.containsKey(id) && !isSilent())
				throw new IllegalArgumentException("Duplicate action-set id: "+id); //$NON-NLS-1$

			actionSetMap.put(id, actionSet);
		}
	}

	public ActionList getActionList(String id) {
		requireNonNull(id);
		ActionList actionList = null;
		if(actionListMap!=null) {
			actionList = actionListMap.get(id);
		}

		if(actionList==null && parent!=null) {
			try {
				actionList = parent.getActionList(id);
			} catch(IllegalArgumentException e) {
				// ignore the 'silent' setting of parent managers
			}
		}

		if(actionList==null && !isSilent())
			throw new IllegalArgumentException("Unknown action-list id: "+id); //$NON-NLS-1$

		return actionList;
	}

	public void addActionList(String id, ActionList actionList) {
		requireNonNull(id);
		requireNonNull(actionList);

		if(actionListMap==null) {
			actionListMap = new HashMap<>();
		}

		synchronized (lock) {
			if(actionListMap.containsKey(id) && !isSilent())
				throw new IllegalArgumentException("Duplicate action-list id: "+id); //$NON-NLS-1$

			actionListMap.put(id, actionList);
		}
	}

	public ButtonGroup getGroup(String groupId, Component comp) {
		requireNonNull(groupId);
		requireNonNull(comp);

		if(groupMap==null) {
			groupMap = new HashMap<>();
		}

		int key = groupId.hashCode() ^ comp.hashCode();

		ButtonGroup group = groupMap.get(key);
		if(group==null) {
			group = new ButtonGroup();
			groupMap.put(key, group);
		}

		return group;
	}

	public void setEnabled(boolean enabled, String...ids) {
		for(String id : ids) {
			Action action = getAction(id);
			if (action != null) {
				action.setEnabled(enabled);
			}
		}
	}

	public boolean isEnabled(String id) {
		Action action = getAction(id);
		if (action != null) {
			return action.isEnabled();
		}
		return false;
	}

	public void setSelected(boolean selected, String...ids) {
		for(String id : ids) {
			StateChangeAction action = getStateChangeAction(id);
			if (action != null) {
				action.setSelected(selected);
			}
		}
	}

	public boolean isSelected(String id) {
		StateChangeAction action = getStateChangeAction(id);
		if (action != null) {
			return action.isSelected();
		}
		return false;
	}

	public boolean isStateChangeAction(String id) {
		return (getStateChangeAction(id) != null);
	}

	/**
	 * Registers a {@code handler} object to receive notifications
	 * about events on the specified action. All invocations are
	 * targeted at the method named {@code method} with the matching
	 * signature for the invocation at hand:
	 * <p>
	 * Calls made through {@link ActionListener} interfaces will forward
	 * the provided {@link ActionEvent}.
	 * Calls coming from {@link ItemListener}s on the other hand provide
	 * the received {@code ItemEvent}.
	 * <p>
	 * It is up to the {@code handler} object to distinguish between those
	 * two cases by providing methods with different signatures or merge
	 * the handling by providing only one method with a sole parameter of
	 * type {@code Object}.
	 * <p>
	 * Note that all internal handlers store weak references to the {@code handler}
	 * objects registered as callbacks. When the target of such a reference
	 * gets garbage collected the next invocation attempt will cause the
	 * listener to be unregistered from the action. So it is strongly recommended
	 * to store a strong reference to all handler objects!
	 *
	 * @param id the unique identifier of the {@code Action} the handler should
	 * be attached to
	 * @param handler the object defining the callback method
	 * @param method method name used to find an appropriate {@code Method} when
	 * forwarding events
	 * @throws IllegalArgumentException if any of the arguments is {@code null} or
	 * if the referenced {@code Action} is not able to forward events (i.e. it is
	 * not derived of type {@link DelegateAction})
	 * @throws UnknownIdentifierException if the given {@code id} is not mapped to
	 * an {@code Action} and this manager is not configured to be silent
	 */
	public void addHandler(String id, Object handler, String method) {
		requireNonNull(handler);
		requireNonNull(method);
		Action a = getAction(id);

		if(a instanceof StateChangeAction) {
			ItemListener listener = new BooleanInvocationHandler(a, handler, method);
			((StateChangeAction)a).addItemListener(listener);
		}

		if(a instanceof DelegateAction) {
			ActionListener listener = new ActionInvocationHandler(a, handler, method);
			((DelegateAction)a).addActionListener(listener);
		} else
			throw new IllegalArgumentException("Cannot attach handler to non-delegating action: "+id);
	}

	public void removeHandler(String id, Object handler, String method) {
		requireNonNull(handler);
		requireNonNull(method);
		Action a = getAction(id);

		if(a instanceof StateChangeAction) {
			StateChangeAction sa = (StateChangeAction) a;
			ItemListener[] listeners = sa.getItemListeners();
			for(ItemListener listener : listeners) {
				if(listener instanceof WeakHandler) {
					WeakHandler wh = (WeakHandler) listener;
					if(wh.getTarget()==handler && wh.getMethodName().equals(method)) {
						sa.removeItemListener(listener);
					}
				}
			}
		}

		if(a instanceof DelegateAction) {
			DelegateAction da = (DelegateAction) a;
			ActionListener[] listeners = da.getActionListeners();
			for(ActionListener listener : listeners) {
				if(listener instanceof WeakHandler) {
					WeakHandler wh = (WeakHandler) listener;
					if(wh.getTarget()==handler && wh.getMethodName().equals(method)) {
						da.removeActionListener(listener);
					}
				}
			}
		}
	}

	public ActionMapper mapper(Object target) {
		return this.new ActionMapper(target);
	}

	/**
	 *
	 *
	 * This class is intended for exclusive use on the event-dispatch thread,
	 * therefore it is <b>not</b> designed thread-safe!
	 *
	 * @author Markus Gärtner
	 *
	 */
	public class ActionMapper {
		private Reference<Object> target;

		private final Map<String, Object> handlers = new HashMap<>();

		private ActionMapper(Object target) {
			requireNonNull(target);

			this.target = new WeakReference<>(target);
		}

		public Object getTarget() {
			if(target==null) {
				return null;
			}
			return target.get();
		}

		public boolean isObsolete() {
			return getTarget()==null;
		}

		public Runnable disposer() {
			return this::dispose;
		}

		public void dispose() {
			if(target==null) {
				return;
			}

			try {
				for(Entry<String, Object> entry : handlers.entrySet()) {
					String id = entry.getKey();
					Object handler = entry.getValue();

					Action a = getAction(id);

					if(a instanceof DelegateAction && handler instanceof ActionListener) {
						((DelegateAction)a).removeActionListener((ActionListener)handler);
					}

					if(a instanceof StateChangeAction && handler instanceof ItemListener) {
						((StateChangeAction)a).removeItemListener((ItemListener)handler);
					}
				}
			} finally {
				handlers.clear();
				target = null;
			}
		}

		@SuppressWarnings("unchecked")
		private ActionMapper map0(String id, Object handler) {
			requireNonNull(id);

			Object current = handlers.get(id);

			// Two chances to prevent redundant adding of same handler

			if(Objects.equals(handler, current)) {
				return this;
			}

			if(current instanceof Set && ((Set<?>)current).contains(handler)) {
				return this;
			}

			// Expand buffer if needed
			if(current==null) {
				handlers.put(id, handler);
			} else if(current instanceof Set) {
				((Set<Object>) current).add(handler);
			} else {
				Set<Object> set = new HashSet<>();
				Collections.addAll(set, current, handler);
				handlers.put(id, set);
			}

			// Now register handler with action

			Action a = getAction(id);

			if(a instanceof DelegateAction && handler instanceof ActionListener) {
				((DelegateAction)a).addActionListener((ActionListener)handler);
			}

			if(a instanceof StateChangeAction && handler instanceof ItemListener) {
				((StateChangeAction)a).addItemListener((ItemListener)handler);
			}

			return this;
		}

		/**
		 * Maps the specified method as an {@link ActionListener}
		 *
		 * @param id
		 * @param methodName
		 * @return
		 */
		public ActionMapper mapAction(String id, String methodName) {
			return map0(id, new ActionInvocationHandler(null, getTarget(), methodName));
		}

		/**
		 * Maps the specified method as a toggle function.
		 *
		 * @param id
		 * @param methodName
		 * @return
		 */
		public ActionMapper mapToggle(String id, String methodName) {
			return map0(id, new BooleanInvocationHandler(null, getTarget(), methodName));
		}

		/**
		 * Shorthand method for invoking {@link #mapAction(String, String)} and
		 * {@link #mapToggle(String, String)} on the same method.
		 *
		 * @param id
		 * @param methodName
		 * @return
		 */
		public ActionMapper map(String id, String methodName) {
			return map0(id, new ActionInvocationHandler(null, getTarget(), methodName))
				.map0(id, new BooleanInvocationHandler(null, getTarget(), methodName));
		}

		public ActionMapper mapAction(String id, ActionListener actionListener) {
			return map0(id, actionListener);
		}

		public ActionMapper mapToggle(String id, ItemListener itemListener) {
			return map0(id, itemListener);
		}

//		public ActionMapper mapActionConsumer(String id, Consumer<? super ActionEvent> handler) {
//			return map0(id, new ActionEventRefHandler(handler));
//		}

		public ActionMapper mapToggle(String id, Consumer<Boolean> handler) {
			return map0(id, new ToggleRefHandler(handler));
		}

		public ActionMapper mapTask(String id, Runnable task) {
			return map0(id, new TaskRefHandler(task));
		}
	}

	protected static class TaskRefHandler implements ActionListener {

		private final Runnable methodRef;

		public TaskRefHandler(Runnable methodRef) {
			this.methodRef = requireNonNull(methodRef);
		}

		/**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent evt) {
			methodRef.run();
		}

	}

	protected static class ActionEventRefHandler implements ActionListener {

		private final Consumer<? super ActionEvent> methodRef;

		public ActionEventRefHandler(Consumer<? super ActionEvent> methodRef) {
			this.methodRef = requireNonNull(methodRef);
		}

		/**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent evt) {
			methodRef.accept(evt);
		}

	}

	protected static class ToggleRefHandler implements ItemListener {

		private final Consumer<Boolean> methodRef;

		public ToggleRefHandler(Consumer<Boolean> methodRef) {
			this.methodRef = requireNonNull(methodRef);
		}

		/**
		 * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
		 */
		@Override
		public void itemStateChanged(ItemEvent evt) {
			Boolean value = Boolean.TRUE;
			if (evt.getStateChange() == ItemEvent.DESELECTED) {
				value = Boolean.FALSE;
			}

			methodRef.accept(value);
		}
	}

	protected static class WeakActionHandler extends WeakHandler {

		private final Action action;

		public WeakActionHandler(Action action, Object target, String methodName) {
			super(target, methodName);
			this.action = action;
		}

		public Action getAction() {
			return action;
		}
	}

	protected static class ActionInvocationHandler extends WeakActionHandler implements ActionListener {

		public ActionInvocationHandler(Action action, Object target, String methodName) {
			super(action, target, methodName);
		}

		/**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			if(isObsolete()) {
				DelegateAction action = (DelegateAction) getAction();
				if(action!=null)
					action.removeActionListener(this);
			} else {
				dispatch(e);
			}
		}
	}

	protected static class BooleanInvocationHandler extends WeakActionHandler implements ItemListener {

		public BooleanInvocationHandler(Action action, Object target, String methodName) {
			super(action, target, methodName);
		}

		@Override
		public void itemStateChanged(ItemEvent evt) {
			Boolean value = Boolean.TRUE;
			if (evt.getStateChange() == ItemEvent.DESELECTED) {
				value = Boolean.FALSE;
			}

			if(isObsolete()) {
				StateChangeAction action = (StateChangeAction) getAction();
				if(action!=null)
					action.removeItemListener(this);
			} else {
				dispatch(value);
			}
		}
	}

	protected void feedActionSet(final Component container,
			final ComponentHandler handler, final ActionSet actionSet, final Map<String, Object> properties) {

		int size = actionSet.size();
		int index;
		boolean rightToLeft = RIGHT_TO_LEFT.equals(properties.get(DIRECTION_PARAMETER));
		for(int i=0; i<size; i++) {
			index = rightToLeft ? size-i-1 : i;
			String actionId = actionSet.getActionIdAt(index);
			Action action = getAction(actionId);
			if(action!=null)
				handler.feedAction(container, action, actionSet.getGroupId(actionId));
			else
				log.error("Unknown action id in set '{}': '{}'",
						actionSet.getId(), actionSet.getActionIdAt(i));
		}
	}

	protected boolean checkCondition(String condition, Map<String, Object> properties) {
		return ConditionResolver.resolve(condition, getConditionResolver(), properties);
	}

	protected void feedActionList(final Component container,
			final ComponentHandler handler, final ActionList list, final Map<String, Object> properties) {

		Action action;
		ActionSet actionSet;

		int size = list.size();
		int index;
		boolean rightToLeft = RIGHT_TO_LEFT.equals(properties.get(DIRECTION_PARAMETER));
		boolean separatorAllowed = false;
		for(int i=0; i<size; i++) {
			index = rightToLeft ? size-i-1 : i;

			String condition = list.getConditionAt(index);

			// Make sure we only feed items that we are supposed to do under the current circumstances!
			if(condition!=null && !checkCondition(condition, properties)) {
				continue;
			}

			String value = list.getValueAt(index);
			switch (list.getTypeAt(index)) {
			case LABEL:
				handler.feedComponent(container, createLabel(value));
				break;

			case SEPARATOR:
				// Prevent two separators beside each other
				if(separatorAllowed) {
					handler.feedSeparator(container, value);
					separatorAllowed = false;
				}
				break;

			case EMPTY:
				handler.feedEmpty(container, value);
				break;

			case GLUE:
				handler.feedGlue(container);
				break;

			case ACTION_ID:
				action = getAction(value);
				if(action!=null)
					handler.feedAction(container, action, null);
				else
					log.warn("Unknown action id: {}", value); //$NON-NLS-1$
				break;

			case ACTION_SET_ID:
				actionSet = getActionSet(value);
				if(actionSet==null) {
					log.warn("Unknown action-set id: {}", value); //$NON-NLS-1$
					break;
				}
				feedActionSet(container, handler, actionSet, properties);
				break;

			case ACTION_LIST_ID:
				ActionList subList = getActionList(value);
				if(subList!=null)
					handler.feedList(container, subList, properties);
				else
					log.warn("Unknown action-list id: {}", value); //$NON-NLS-1$
				break;

			case CUSTOM:
				Object replacement = properties.get(value);
				if(replacement==null) {
					value = null;
				} else {
					Object[] items = replacement instanceof Object[] ?
							(Object[]) replacement : new Object[]{replacement};
					for(Object item : items) {
						if(item instanceof String) {
							handler.feedComponent(container, createLabel((String)item));
						} else if(item instanceof Action) {
							handler.feedAction(container, (Action)item, null);
						} else if(item instanceof ActionSet) {
							feedActionSet(container, handler, (ActionSet)item, properties);
						} else if(item instanceof ActionList) {
							handler.feedList(container, (ActionList)item, properties);
						} else if(item instanceof Component) {
							handler.feedComponent(container, (Component)item);
						} else if(item==EntryType.SEPARATOR) {
							// No checking for subsequent separators!
							handler.feedSeparator(container, null);
						} else if(item!=null) {
							// Null replacements are legal, unrecognizable objects are not
							log.warn("Not a valid action-list element: {}", String.valueOf(item)); //$NON-NLS-1$
						} else {
							log.info("No replacement defined for item: {}", list.getTypeAt(index)); //$NON-NLS-1$
						}
					}
				}
				break;
			}
			separatorAllowed = list.getTypeAt(i)!=EntryType.SEPARATOR && value!=null;
		}
	}

	private static Border DEFAULT_LABEL_BORDER;

	protected Component createLabel(String value) {
		JLabel label = new JLabel();

		if(value!=null && !value.isEmpty()) {
			label.setText(getResourceManager().get(value));
		} else {
			label.setText("<undefined>"); //$NON-NLS-1$
		}

		if(DEFAULT_LABEL_BORDER==null) {
			DEFAULT_LABEL_BORDER = new EmptyBorder(0, 4, 0, 3);
		}

		label.setBorder(DEFAULT_LABEL_BORDER);

		return label;
	}

	protected ComponentHandler getHandler(Class<?> containerClass) {
		if(containerClass==JMenu.class) {
			if(menuHandler==null)
				menuHandler = new MenuHandler();
			return menuHandler;
		} else if(containerClass==JPopupMenu.class) {
			if(popupMenuHandler==null)
				popupMenuHandler = new PopupMenuHandler();
			return popupMenuHandler;
		} else if(containerClass==JToolBar.class) {
			if(toolBarHandler==null)
				toolBarHandler = new ToolBarHandler();
			return toolBarHandler;
		} else if(containerClass==JMenuBar.class) {
			if(menuBarHandler==null)
				menuBarHandler = new MenuBarHandler();
			return menuBarHandler;
		} else
			throw new IllegalArgumentException("Unsupported container class: "+containerClass); //$NON-NLS-1$
	}

	protected static final Map<String, Object> EMPTY_PROPERTIES = Collections.emptyMap();

	public JMenu createMenu(String id, Map<String, Object> properties) {
		ActionList actionList = getActionList(id);

		if(actionList==null) {
			log.warn("Unknown action-list id: {}", id); //$NON-NLS-1$
			return null;
		}

		return createMenu(actionList, properties);
	}

	public JMenu createMenu(ActionList actionList, Map<String, Object> properties) {
		requireNonNull(actionList);
		if(properties==null) {
			properties = EMPTY_PROPERTIES;
		}

		Action action = null;
		if(actionList.getActionId()!=null) {
			action = getAction(actionList.getActionId());
		}

		JMenu menu = new JMenu(action);

		feedActionList(menu, getHandler(JMenu.class), actionList, properties);
		configureMenu(menu, action, properties);

		return menu;
	}

	public JMenuBar createMenuBar(String id, Map<String, Object> properties) {
		ActionList actionList = getActionList(id);

		if(actionList==null) {
			log.warn("Unknown action-list id: {}", id); //$NON-NLS-1$
			return null;
		}

		return createMenuBar(actionList, properties);
	}

	public JMenuBar createMenuBar(ActionList actionList, Map<String, Object> properties) {
		requireNonNull(actionList);
		if(properties==null) {
			properties = EMPTY_PROPERTIES;
		}

		JMenuBar menuBar = new JMenuBar();

		feedActionList(menuBar, getHandler(JMenuBar.class), actionList, properties);
		configureMenuBar(menuBar, properties);

		return menuBar;
	}

	public JToolBar createEmptyToolBar() {
		return createEmptyToolBar(EMPTY_PROPERTIES);
	}

	public JToolBar createEmptyToolBar(Map<String, Object> properties) {
		JToolBar toolBar = new JToolBar();
		configureToolBar(toolBar, properties);

		return toolBar;
	}

	public JToolBar createToolBar(String id, Map<String, Object> properties) {
		ActionList actionList = getActionList(id);

		if(actionList==null) {
			log.warn("Unknown action-list id: {}", id); //$NON-NLS-1$
			return null;
		}

		return createToolBar(actionList, properties);
	}

	public JToolBar createToolBar(ActionList actionList, Map<String, Object> properties) {
		requireNonNull(actionList);
		if(properties==null) {
			properties = EMPTY_PROPERTIES;
		}

		JToolBar toolBar = new JToolBar();

		feedActionList(toolBar, getHandler(JToolBar.class), actionList, properties);

		configureToolBar(toolBar, properties);

		return toolBar;
	}

	public void feedToolBar(String id, JToolBar toolBar, Map<String, Object> properties) {
		ActionList actionList = getActionList(id);

		if(actionList==null) {
			log.warn("Unknown action-list id: {}", id); //$NON-NLS-1$
			return;
		}

		feedToolBar(actionList, toolBar, properties);
	}

	public void feedToolBar(ActionList actionList, JToolBar toolBar, Map<String, Object> properties) {
		requireNonNull(actionList);
		if(properties==null) {
			properties = EMPTY_PROPERTIES;
		}

		feedActionList(toolBar, getHandler(JToolBar.class), actionList, properties);

		configureToolBar(toolBar, properties);
	}

	public JPopupMenu createPopupMenu(String id, Map<String, Object> properties) {
		ActionList actionList = getActionList(id);

		if(actionList==null) {
			log.warn("Unknown action-list id: {}", id); //$NON-NLS-1$
			return null;
		}

		return createPopupMenu(actionList, properties);
	}

	public JPopupMenu createPopupMenu(ActionList actionList, Map<String, Object> properties) {
		requireNonNull(actionList);
		if(properties==null) {
			properties = EMPTY_PROPERTIES;
		}

		JPopupMenu popupMenu = new JPopupMenu();

		feedActionList(popupMenu, getHandler(JPopupMenu.class), actionList, properties);
		configurePopupMenu(popupMenu, properties);

		return popupMenu;
	}

	public AbstractButton createButton(String actionId) {
		Action action = getAction(actionId);

		return createButton(action, null, null);
	}

	protected final JMenuItem createMenuItem(Action action, String groupId,	Component container) {
		JMenuItem menuItem = null;
		if (action instanceof StateChangeAction) {
			StateChangeAction sca = (StateChangeAction)action;

			menuItem = new JCheckBoxMenuItem(sca);
			menuItem.addItemListener(sca);
			menuItem.setSelected(sca.isSelected());
			if (groupId != null) {
				ButtonGroup group = getGroup(groupId, container);
				group.add(menuItem);

				action.addPropertyChangeListener(new ToggleActionPropertyChangeListener(menuItem));
			}

			configureToggleMenuItem(menuItem, sca);

			sca.addPropertyChangeListener(new ToggleActionPropertyChangeListener(menuItem));
		} else if(action!=null) {
			menuItem = new JMenuItem(action);
			configureMenuItem(menuItem, action);
		}
		return menuItem;
	}

	/**
	 * Creates a {@link JButton} or {@link JToggleButton} depending
	 * on the type of the given {@code Action} (a {@code StateChangeAction}
	 * will result in a {@code JToggleButton}). If the supplied {@code Action}
	 * is {@code null} then this will silently fail by returning {@code null}.
	 */
	protected final AbstractButton createButton(Action action, String groupId, Component container) {
		AbstractButton button = null;
		if (action instanceof StateChangeAction) {
			StateChangeAction sca = (StateChangeAction)action;
			ButtonGroup group = groupId==null ? null : getGroup(groupId, container);

			button = new JToggleButton(sca);
			button.addItemListener(sca);
			button.setSelected(sca.isSelected());
			if (group != null) {
				group.add(button);
			}
			configureToggleButton((JToggleButton)button, sca);
			sca.addPropertyChangeListener(new ToggleActionPropertyChangeListener(button));
		} else if(action!=null) {
			button = new JButton(action);
			configureButton(button, action);
		}
		return button;
	}

	// configuration callbacks

	protected void registerHelp(JComponent component, Action action) {
		Object helpAnchor = action.getValue(HELP_ANCHOR_KEY);
		if(helpAnchor != null && helpAnchor instanceof String) {
			RDHClient.client().getGui().registerHelp(component, (String) helpAnchor);

//			System.out.println("register: "+helpAnchor);
		}
	}

	protected void configureMenu(JMenu menu, Action action, Map<String, Object> properties) {
		registerHelp(menu, action);
	}

	protected void configurePopupMenu(JPopupMenu popupMenu, Map<String, Object> properties) {
		// no-op
	}

	protected void configureToolBar(JToolBar toolBar, Map<String, Object> properties) {
		toolBar.setFloatable(false);
		toolBar.setRollover(true);

		if(Boolean.parseBoolean(String.valueOf(properties.get("multiline")))) { //$NON-NLS-1$
			// FIXME ModifiedFlowLayout needs rework to support glue objects
			toolBar.setLayout(new ModifiedFlowLayout(FlowLayout.LEFT, 1, 3));
		}
	}

	protected void configureMenuBar(JMenuBar menuBar, Map<String, Object> properties) {
		// no-op
	}

	protected void configureToggleButton(JToggleButton button, Action action) {
		configureButton(button, action);

		Icon selectedIcon = (Icon) action.getValue(LARGE_SELECTED_ICON_KEY);
		if(selectedIcon==null) {
			selectedIcon = (Icon) action.getValue(SMALL_SELECTED_ICON_KEY);
		}
		button.setSelectedIcon(selectedIcon);
	}

	protected void configureButton(AbstractButton button, Action action) {
		button.setHideActionText(true);
		button.setFocusable(false);

		registerHelp(button, action);

		// TODO check for requirements of a default preferred size for buttons!
		/*Icon icon = button.getIcon();
		if(icon!=null) {
			int width = Math.max(24, icon.getIconWidth()+6);
			int height = Math.max(24, icon.getIconHeight()+6);
			button.setPreferredSize(new Dimension(width, height));
		}*/
	}

	protected void configureToggleMenuItem(JMenuItem menuItem, Action action) {
		configureMenuItem(menuItem, action);
	}

	protected void configureMenuItem(JMenuItem menuItem, Action action) {
		// no-op
	}

	/**
	 *
	 * @author Markus Gärtner
	 * @version $Id: ActionManager.java 389 2015-04-23 10:19:15Z mcgaerty $
	 *
	 */
	protected static class ToggleActionPropertyChangeListener implements PropertyChangeListener {

		private transient WeakReference<AbstractButton> target;

		public ToggleActionPropertyChangeListener(AbstractButton button) {
			this.target = new WeakReference<AbstractButton>(button);
		}

	    public AbstractButton getTarget() {
	        if (target == null) {
	            return null;
	        }
	        return this.target.get();
	    }

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			String propertyName = evt.getPropertyName();

			if (propertyName.equals("selected")) { //$NON-NLS-1$
				AbstractButton button = target.get();
				if(button==null) {
					Action action = (Action) evt.getSource();
					action.removePropertyChangeListener(this);
				} else {
					Boolean selected = (Boolean) evt.getNewValue();
					button.setSelected(selected.booleanValue());
				}
			}
		}

	    private void writeObject(ObjectOutputStream s) throws IOException {
	        s.defaultWriteObject();
	        s.writeObject(getTarget());
	    }

	    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
	        s.defaultReadObject();
	        AbstractButton target = (AbstractButton)s.readObject();
	        if (target != null) {
	        	this.target = new WeakReference<AbstractButton>(target);
	        }
	    }
	}

	/**
	 *
	 * @author Markus Gärtner
	 * @version $Id: ActionManager.java 389 2015-04-23 10:19:15Z mcgaerty $
	 *
	 */
	class ActionAttributes {

		private String[] array;

		public ActionAttributes(Attributes attrs) {
			array = new String[14];
			setValue(ID_INDEX, attrs.getValue(ID_ATTRIBUTE));
			setAttributes(attrs);
		}

		public String getValue(int index) {
			return array[index];
		}

		private String substitute(String s) {
			String id = getValue(ID_INDEX);
			if(id!=null) {
				s = s.replaceFirst("\\$\\{id\\}", id); //$NON-NLS-1$
			}
			return s;
		}

		public boolean hasValue(int index) {
			return array[index]!=null;
		}

		public void setValue(int index, String value) {
			// Do not allow 'clearing' of fields
			if(value!=null) {
				if(index!=ID_INDEX)
					value = substitute(value);

				array[index] = value;
			}
		}

		public void setAttributes(Attributes attrs) {
			setValue(TEMPLATE_INDEX, attrs.getValue(TEMPLATE_ATTRIBUTE));
			setValue(ACCEL_INDEX, attrs.getValue(ACCEL_ATTRIBUTE));
			setValue(DESC_INDEX, attrs.getValue(DESC_ATTRIBUTE));
			setValue(LARGE_ICON_INDEX, attrs.getValue(LARGE_ICON_ATTRIBUTE));
			setValue(LARGE_SELECTED_ICON_INDEX, attrs.getValue(LARGE_SELECTED_ICON_ATTRIBUTE));
			setValue(MNEMONIC_INDEX, attrs.getValue(MNEMONIC_ATTRIBUTE));
			setValue(NAME_INDEX, attrs.getValue(NAME_ATTRIBUTE));
			setValue(SMALL_ICON_INDEX, attrs.getValue(SMALL_ICON_ATTRIBUTE));
			setValue(SMALL_SELECTED_ICON_INDEX, attrs.getValue(SMALL_SELECTED_ICON_ATTRIBUTE));
			setValue(TYPE_INDEX, attrs.getValue(TYPE_ATTRIBUTE));
			setValue(VIRTUAL_INDEX, attrs.getValue(VIRTUAL_ATTRIBUTE));
			setValue(COMMAND_INDEX, attrs.getValue(COMMAND_ATTRIBUTE));
			setValue(SHOW_HELP_INDEX, attrs.getValue(SHOW_HELP_ATTRIBUTE));
		}
	}

    private final static String ACCEL_ATTRIBUTE = "accel"; //$NON-NLS-1$
    private final static String DESC_ATTRIBUTE = "desc"; //$NON-NLS-1$
    private final static String LARGE_ICON_ATTRIBUTE = "licon"; //$NON-NLS-1$
    private final static String LARGE_SELECTED_ICON_ATTRIBUTE = "slicon"; //$NON-NLS-1$
    private final static String ID_ATTRIBUTE = "id"; //$NON-NLS-1$
    private final static String IDREF_ATTRIBUTE = "idref"; //$NON-NLS-1$
    private final static String MNEMONIC_ATTRIBUTE = "mnemonic"; //$NON-NLS-1$
    private final static String NAME_ATTRIBUTE = "name"; //$NON-NLS-1$
    private final static String SMALL_ICON_ATTRIBUTE = "icon"; //$NON-NLS-1$
    private final static String SMALL_SELECTED_ICON_ATTRIBUTE = "sicon"; //$NON-NLS-1$
    private final static String TYPE_ATTRIBUTE = "type"; //$NON-NLS-1$
    private final static String VALUE_ATTRIBUTE = "value"; //$NON-NLS-1$
    private final static String VIRTUAL_ATTRIBUTE = "virtual"; //$NON-NLS-1$
    private final static String COMMAND_ATTRIBUTE = "command"; //$NON-NLS-1$
    private final static String TEMPLATE_ATTRIBUTE = "template"; //$NON-NLS-1$
    private final static String SHOW_HELP_ATTRIBUTE = "help"; //$NON-NLS-1$

    private final static String CONDITION_ATTRIBUTE = "condition"; //$NON-NLS-1$

    private final static int ACCEL_INDEX = 0;
    private final static int DESC_INDEX = 1;
    private final static int SMALL_ICON_INDEX = 2;
    private final static int SMALL_SELECTED_ICON_INDEX = 3;
    private final static int ID_INDEX = 4;
    private final static int MNEMONIC_INDEX = 5;
    private final static int NAME_INDEX = 6;
    private final static int LARGE_ICON_INDEX = 7;
    private final static int LARGE_SELECTED_ICON_INDEX = 8;
    private final static int TYPE_INDEX = 9;
    private final static int VIRTUAL_INDEX = 10;
    private final static int COMMAND_INDEX = 11;
    private final static int TEMPLATE_INDEX = 12;
    private final static int SHOW_HELP_INDEX = 13;

    private static volatile SAXParserFactory parserFactory;
    private volatile XmlActionHandler xmlHandler;

	private void parseActions(InputStream stream) throws IOException {
		SAXParserFactory factory = parserFactory;

		if (factory == null) {
			factory = SAXParserFactory.newInstance();
			factory.setValidating(true);

			parserFactory = factory;
		}

		if (xmlHandler == null) {
			xmlHandler = new XmlActionHandler();
		}

		try {
			SAXParser parser = factory.newSAXParser();
			String dtdResource = getClass().getResource("action-list.dtd").toString(); //$NON-NLS-1$

			parser.parse(stream, xmlHandler, dtdResource);
		} catch (SAXException e) {
			throw new IOException("Error parsing: " + e.getMessage()); //$NON-NLS-1$
		} catch (IOException e) {
			throw e;
		} catch (ParserConfigurationException e) {
			throw new IOException("Error configuring parser: " + e.getMessage()); //$NON-NLS-1$
		}
	}

	/**
	 * Hook for subclasses to bypass the optimization regarding
	 * redundant loading of action resources.
	 * <p>
	 * The default implementation returns {@code true}.
	 */
	protected boolean isPreventRedundantLoading() {
		return true;
	}

	/**
	 *
	 */
	public void loadActions(URL location) throws IOException {
		requireNonNull(location);

		// Fix for umlauts and such causing problems with the xerces internal URL scheme
		//FIXME spaces break the encoding when resolved back to files
//		location = IOUtil.encodeURL(location);

		if(loadedResources==null) {
			loadedResources = new HashSet<>();
		}

		// Skip redundant loading of resources
		if(isPreventRedundantLoading() && loadedResources.contains(location.toExternalForm())) {
			return;
		}

		InputStream stream = location.openStream();
		try {
			parseActions(stream);
		} finally {
			try {
				stream.close();
			} catch(IOException e) {
				log.error("Failed to close stream after reading actions from URL: {}",  location, e);
			}
		}

		loadedResources.add(location.toExternalForm());
	}

    /**
     *
     * @author Markus Gärtner
     * @version $Id: ActionManager.java 389 2015-04-23 10:19:15Z mcgaerty $
     *
     */
	protected class XmlActionHandler extends DefaultHandler {

	    private final static String ACTION_ELEMENT="action"; //$NON-NLS-1$
	    private final static String ACTION_SET_ELEMENT="action-set"; //$NON-NLS-1$
	    private final static String ACTION_LIST_ELEMENT="action-list"; //$NON-NLS-1$
	    private final static String ITEM_ELEMENT="item"; //$NON-NLS-1$
	    private final static String EMPTY_ELEMENT="empty"; //$NON-NLS-1$
	    private final static String GROUP_ELEMENT="group"; //$NON-NLS-1$
	    private final static String SEPARATOR_ELEMENT="separator"; //$NON-NLS-1$

		private Stack<ActionList> actionListStack;
		private String groupId;
		private ActionList actionList;
		private ActionSet actionSet;

		@Override
		public void startDocument() {
			actionListStack = new Stack<>();
			groupId = null;
			actionList = null;
			actionSet = null;
		}

		@Override
		public void startElement(String nameSpace, String localName,
				String name, Attributes attributes) {

			if (ACTION_SET_ELEMENT.equals(name)) {
				String id = attributes.getValue(ID_ATTRIBUTE);
				actionSet = new ActionSet(id);
				if (actionList != null) {
					String condition = attributes.getValue(CONDITION_ATTRIBUTE);
					actionList.add(EntryType.ACTION_SET_ID, id, condition);
				}
				addActionSet(id, actionSet);
			} else if (ACTION_LIST_ELEMENT.equals(name)) {
				String id = attributes.getValue(ID_ATTRIBUTE);
				String idref = attributes.getValue(IDREF_ATTRIBUTE);

				// Idref pointer overrides id
				if (idref == null) {
					idref = id;
				}
				ActionAttributes actionAtts = getAttributes(idref);
				if (actionAtts == null) {
					// Create new action for this list
					addAttributes(attributes);
				} else if(Boolean.parseBoolean(actionAtts.getValue(VIRTUAL_INDEX))) {
					// Not allowed to use a template within a list definition
					throw new RuntimeException("Cannot use template action id within list definition: "+idref); //$NON-NLS-1$
				} else {
					// Override fields
					actionAtts.setAttributes(attributes);
				}

				// Instantiate and add new list
				ActionList newList = new ActionList(id);
				newList.setActionId(idref);
				if (actionList != null) {
					String condition = attributes.getValue(CONDITION_ATTRIBUTE);
					actionList.add(EntryType.ACTION_LIST_ID, id, condition);
					actionListStack.push(actionList);
				}
				addActionList(id, newList);

				actionList = newList;
			} else if (ACTION_ELEMENT.equals(name)) {
				String id = attributes.getValue(IDREF_ATTRIBUTE);
				if (id == null) {
					id = attributes.getValue(ID_ATTRIBUTE);
				}
				ActionAttributes actionAtts = getAttributes(id);
				if (actionAtts == null) {
					// Create new action
					addAttributes(attributes);
				} else if(!Boolean.parseBoolean(actionAtts.getValue(VIRTUAL_INDEX))) {
					// Override fields only if target is not a template!
					actionAtts.setAttributes(attributes);
				}

				if(actionSet!=null) {
					actionSet.add(id, groupId);
				} else if(actionList!=null) {
					String condition = attributes.getValue(CONDITION_ATTRIBUTE);
					actionList.add(EntryType.ACTION_ID, id, condition);
					if(groupId!=null) {
						actionList.mapGroup(id, groupId);
					}
				}
			} else if (GROUP_ELEMENT.equals(name)) {
				groupId = attributes.getValue(ID_ATTRIBUTE);
			} else if (EMPTY_ELEMENT.equals(name)) {
				if (actionList != null) {
					String condition = attributes.getValue(CONDITION_ATTRIBUTE);
					actionList.add(EntryType.EMPTY, null, condition);
				}
			} else if (SEPARATOR_ELEMENT.equals(name)) {
				if (actionList != null) {
					String condition = attributes.getValue(CONDITION_ATTRIBUTE);
					actionList.add(EntryType.SEPARATOR, null, condition);
				}
			} else if(ITEM_ELEMENT.equals(name)) {
				if(actionList!=null) {
					String type = attributes.getValue(TYPE_ATTRIBUTE);
					String value = attributes.getValue(VALUE_ATTRIBUTE);
					String condition = attributes.getValue(CONDITION_ATTRIBUTE);

					actionList.add(EntryType.parse(type), value, condition);
				}
			}
		}

		@Override
		public void endElement(String nameSpace, String localName, String name) {

			if (ACTION_SET_ELEMENT.equals(name)) {
				actionSet = null;
			} else if (ACTION_LIST_ELEMENT.equals(name)) {
				if(!actionListStack.isEmpty())
					actionList = actionListStack.pop();
				else
					actionList = null;
			} else if (GROUP_ELEMENT.equals(name)) {
				groupId = null;
			}
		}

		@Override
		public void endDocument() {
			actionListStack = null;
			groupId = null;
			actionList = null;
			actionSet = null;
		}

		@Override
		public void error(SAXParseException ex) throws SAXException {
			logException(true, ex);
		}

		@Override
		public void warning(SAXParseException ex) throws SAXException {
			logException(false, ex);
		}

		@Override
		public void fatalError(SAXParseException ex) throws SAXException {
			logException(true, ex);
		}

		private void logException(boolean error, SAXParseException ex) {
			StringBuilder sb = new StringBuilder();
			sb.append(ex.getMessage()).append(":\n"); //$NON-NLS-1$
			sb.append("Message: ").append(ex.getMessage()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("Public ID: ").append(String.valueOf(ex.getPublicId())).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("System ID: ").append(String.valueOf(ex.getSystemId())).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("Line: ").append(ex.getLineNumber()).append("\n"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append("Column: ").append(ex.getColumnNumber()); //$NON-NLS-1$
			if(ex.getException()!=null)
				sb.append("\nEmbedded: ").append(ex.getException()); //$NON-NLS-1$

			if(error) {
				log.error(sb.toString(), ex);
			} else {
				log.warn(sb.toString(), ex);
			}
		}
	}

	/**
	 *
	 * @author Markus Gärtner
	 * @version $Id: ActionManager.java 389 2015-04-23 10:19:15Z mcgaerty $
	 *
	 */
	protected interface ComponentHandler {
		Component feedLabel(Component container, String label);
		Component feedList(Component container, ActionList list, Map<String, Object> properties);
		Component feedAction(Component container, Action a, String groupId);
		Component feedSeparator(Component container, String value);
		Component feedComponent(Component container, Component comp);
		Component feedEmpty(Component container, String value);
		Component feedGlue(Component container);
	}

	private static Component getLastChild(Container container) {
		return container.getComponent(container.getComponentCount()-1);
	}

	/**
	 *
	 * @author Markus Gärtner
	 * @version $Id: ActionManager.java 389 2015-04-23 10:19:15Z mcgaerty $
	 *
	 */
	protected class MenuHandler implements ComponentHandler {

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedList(java.awt.Component, de.ims.icarus.ui.actions.ActionList, java.util.Map)
		 */
		@Override
		public Component feedList(Component container, ActionList list,
				Map<String, Object> properties) {
			JMenu menu = (JMenu) container;
			JMenu subMenu = createMenu(list, properties);
			if(subMenu!=null)
				menu.add(subMenu);
			return subMenu;
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedAction(java.awt.Component, javax.swing.Action)
		 */
		@Override
		public Component feedAction(Component container, Action a, String groupId) {
			JMenu menu = (JMenu) container;
			return menu.add(createMenuItem(a, groupId, container));
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedSeparator(java.awt.Component, java.lang.String)
		 */
		@Override
		public Component feedSeparator(Component container, String value) {
			JMenu menu = (JMenu) container;
			menu.addSeparator();
			return getLastChild(menu.getPopupMenu());
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedComponent(java.awt.Component, java.awt.Component)
		 */
		@Override
		public Component feedComponent(Component container, Component comp) {
			JMenu menu = (JMenu) container;
			return menu.add(comp);
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedLabel(java.awt.Component, java.lang.String)
		 */
		@Override
		public Component feedLabel(Component container, String label) {
			JMenu menu = (JMenu) container;
			return menu.add(createLabel(label));
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedEmpty(java.awt.Component)
		 */
		@Override
		public Component feedEmpty(Component container, String value) {
			JMenu menu = (JMenu) container;
			return menu.add(value);
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedGlue(java.awt.Component)
		 */
		@Override
		public Component feedGlue(Component container) {
			// not supported
			return null;
		}

	}

	/**
	 *
	 * @author Markus Gärtner
	 * @version $Id: ActionManager.java 389 2015-04-23 10:19:15Z mcgaerty $
	 *
	 */
	protected class PopupMenuHandler implements ComponentHandler {

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedList(java.awt.Component, de.ims.icarus.ui.actions.ActionList, java.util.Map)
		 */
		@Override
		public Component feedList(Component container, ActionList list,
				Map<String, Object> properties) {
			JPopupMenu menu = (JPopupMenu) container;
			JMenu subMenu = createMenu(list, properties);
			if(subMenu!=null) {
				menu.add(subMenu);
			}
			return subMenu;
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedAction(java.awt.Component, javax.swing.Action)
		 */
		@Override
		public Component feedAction(Component container, Action a, String groupId) {
			JPopupMenu menu = (JPopupMenu) container;
			return menu.add(createMenuItem(a, groupId, container));
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedSeparator(java.awt.Component, java.lang.String)
		 */
		@Override
		public Component feedSeparator(Component container, String value) {
			JPopupMenu menu = (JPopupMenu) container;
			menu.addSeparator();
			return getLastChild(menu);
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedComponent(java.awt.Component, java.awt.Component)
		 */
		@Override
		public Component feedComponent(Component container, Component comp) {
			JPopupMenu menu = (JPopupMenu) container;
			return menu.add(comp);
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedLabel(java.awt.Component, java.lang.String)
		 */
		@Override
		public Component feedLabel(Component container, String label) {
			JPopupMenu menu = (JPopupMenu) container;
			return menu.add(createLabel(label));
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedEmpty(java.awt.Component)
		 */
		@Override
		public Component feedEmpty(Component container, String value) {
			JPopupMenu menu = (JPopupMenu) container;
			return menu.add(value);
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedGlue(java.awt.Component)
		 */
		@Override
		public Component feedGlue(Component container) {
			// not supported
			return null;
		}

	}

	protected class MenuBarHandler implements ComponentHandler {

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedLabel(java.awt.Component, java.lang.String)
		 */
		@Override
		public Component feedLabel(Component container, String label) {
			// not supported by menu bars
			return null;
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedList(java.awt.Component, de.ims.icarus.ui.actions.ActionList, java.util.Map)
		 */
		@Override
		public Component feedList(Component container, ActionList list,
				Map<String, Object> properties) {
			JMenuBar menuBar = (JMenuBar)container;

			JMenu menu = createMenu(list, properties);
			if(menu!=null) {
				menuBar.add(menu);
			}
			return menu;
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedAction(java.awt.Component, javax.swing.Action, java.lang.String)
		 */
		@Override
		public Component feedAction(Component container, Action a, String groupId) {
			JMenuBar menuBar = (JMenuBar) container;

			AbstractButton button = createButton(a, groupId, container);
			button.setHorizontalTextPosition(SwingConstants.CENTER);
	        button.setVerticalTextPosition(SwingConstants.BOTTOM);

			return menuBar.add(button);
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedSeparator(java.awt.Component, java.lang.String)
		 */
		@Override
		public Component feedSeparator(Component container, String value) {
			// not supported by menu bars
			return null;
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedComponent(java.awt.Component, java.awt.Component)
		 */
		@Override
		public Component feedComponent(Component container, Component comp) {
			// not supported by menu bars
			return null;
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedEmpty(java.awt.Component)
		 */
		@Override
		public Component feedEmpty(Component container, String value) {
			// not supported by menu bars
			return null;
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedGlue(java.awt.Component)
		 */
		@Override
		public Component feedGlue(Component container) {
			JMenuBar menuBar = (JMenuBar) container;
			return menuBar.add(Box.createHorizontalGlue());
		}

	}

	/**
	 *
	 * @author Markus Gärtner
	 * @version $Id: ActionManager.java 389 2015-04-23 10:19:15Z mcgaerty $
	 *
	 */
	protected class ToolBarHandler implements ComponentHandler {

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedList(java.awt.Component, de.ims.icarus.ui.actions.ActionList, java.util.Map)
		 */
		@Override
		public Component feedList(Component container, ActionList list,
				Map<String, Object> properties) {

			final JPopupMenu popupMenu = createPopupMenu(list, properties);
			if(popupMenu!=null) {
				Action action = null;
				if(list.getActionId()!=null) {
					action = getAction(list.getActionId());
				}

				final AbstractButton button = (AbstractButton) feedAction(container, action, null);
				button.addActionListener(ae -> {
					popupMenu.pack();
					popupMenu.show(button, 0, button.getHeight()+2);
				});
			}

			return popupMenu;
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedAction(java.awt.Component, javax.swing.Action)
		 */
		@Override
		public Component feedAction(Component container, Action a, String groupId) {
			JToolBar toolBar = (JToolBar) container;

			AbstractButton button = createButton(a, groupId, container);
			button.setHorizontalTextPosition(SwingConstants.CENTER);
	        button.setVerticalTextPosition(SwingConstants.BOTTOM);
	        //button.setFocusable(false);

			return toolBar.add(button);
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedSeparator(java.awt.Component, java.lang.String)
		 */
		@Override
		public Component feedSeparator(Component container, String value) {
			JToolBar toolBar = (JToolBar) container;
			int width = -1;

			if(value!=null) {
				switch (value) {
				case SEPARATOR_WIDE:
					width = 30;
					break;

				case SEPARATOR_MEDIUM:
					width = 20;
					break;

				case SEPARATOR_SMALL:
					width = 10;
					break;

				default:
					try {
						width = Integer.parseInt(value);
					} catch(NumberFormatException e) {
						width = -1;
					}
					break;
				}
			}

			if(width<0) {
				width = 4;
			}

			toolBar.addSeparator(new Dimension(width, 24));
			return getLastChild(toolBar);
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedComponent(java.awt.Component, java.awt.Component)
		 */
		@Override
		public Component feedComponent(Component container, Component comp) {
			JToolBar toolBar = (JToolBar) container;
			return toolBar.add(comp);
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedLabel(java.awt.Component, java.lang.String)
		 */
		@Override
		public Component feedLabel(Component container, String label) {
			JToolBar toolBar = (JToolBar) container;
			return toolBar.add(createLabel(label));
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedEmpty(java.awt.Component)
		 */
		@Override
		public Component feedEmpty(Component container, String value) {
			JToolBar toolBar = (JToolBar) container;
			int width = -1;

			if(value!=null) {
				switch (value) {
				case SEPARATOR_WIDE:
					width = 50;
					break;

				case SEPARATOR_MEDIUM:
					width = 25;
					break;

				case SEPARATOR_SMALL:
					width = 10;
					break;

				default:
					try {
						width = Integer.parseInt(value);
					} catch(NumberFormatException e) {
						width = -1;
					}
					break;
				}
			}

			if(width==-1) {
				width = 25;
			}

			return toolBar.add(Box.createHorizontalStrut(width));
		}

		/**
		 * @see de.ims.icarus.ui.actions.ActionManager.ComponentHandler#feedGlue(java.awt.Component)
		 */
		@Override
		public Component feedGlue(Component container) {
			JToolBar toolBar = (JToolBar) container;
			return toolBar.add(Box.createHorizontalGlue());
		}

	}
}

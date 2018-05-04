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

import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.utils.Options;

/**
 * @author Markus Gärtner
 *
 */
public class ActionComponentBuilder {

	private ActionManager actionManager;
	private String actionListId;
	private Options options;
	private Set<String> lockedOptions;

	private static final Logger log = LoggerFactory.getLogger(ActionComponentBuilder.class);

	public ActionComponentBuilder() {
		// no-op
	}

	public ActionComponentBuilder(ActionManager actionManager) {
		setActionManager(actionManager);
	}

	public ActionComponentBuilder(ActionManager actionManager, String actionListId) {
		setActionManager(actionManager);
		setActionListId(actionListId);
	}

	/**
	 * @return the actionManager
	 */
	public ActionManager getActionManager() {
		checkState("No action manager defined", actionManager!=null);

		return actionManager;
	}

	/**
	 * @return the actionListId
	 */
	public String getActionListId() {
		checkState("No action list id defined", actionListId!=null);

		return actionListId;
	}

	/**
	 * @param actionManager the actionManager to set
	 */
	public void setActionManager(ActionManager actionManager) {
		requireNonNull(actionManager);

		this.actionManager = actionManager;
	}

	/**
	 * @param actionListId the actionListId to set
	 */
	public void setActionListId(String actionListId) {
		requireNonNull(actionListId);

		this.actionListId = actionListId;
	}

	public Options getOptions() {
		if(options==null) {
			options = new Options();
		}
		return options;
	}

	private boolean isIgnored(String key) {
		return lockedOptions!=null && lockedOptions.contains(key);
	}

	public boolean addOption(String key, Object value) {
		requireNonNull(key);

		if(isIgnored(key)) {
			log.debug("Ignoring options key: {}", key);
			return false;
		}

		getOptions().put(key, value);

		return true;
	}

	public Set<String> addOptions(Map<String, Object> map) {
		requireNonNull(map);

		Set<String> result = null;

		for(Map.Entry<String, Object> entry : map.entrySet()) {
			if(!addOption(entry.getKey(), entry.getValue())) {
				if(result==null) {
					result = new HashSet<>();
				}
				result.add(entry.getKey());
			}
		}

		if(result==null) {
			result = Collections.emptySet();
		}

		return result;
	}

	public void lockOption(String key) {
		requireNonNull(key);

		if(lockedOptions==null) {
			lockedOptions = new HashSet<>();
		}
		lockedOptions.add(key);
	}

	public JToolBar buildToolBar() {
		return getActionManager().createToolBar(getActionListId(), getOptions());
	}

	public JPopupMenu buildPopupMenu() {
		return getActionManager().createPopupMenu(getActionListId(), getOptions());
	}

	public JMenu buildMenu() {
		return getActionManager().createMenu(getActionListId(), getOptions());
	}
}

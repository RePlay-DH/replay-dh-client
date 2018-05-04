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

import java.awt.Component;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JList;

import org.java.plugin.registry.Extension;

import bwfdm.replaydh.core.PluginEngine;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.icons.Resolution;

/**
 * Renderer to display {@link Extension} objects in a specialized way:
 * If available it will use the {@code name} parameter as label and
 * the optional {@code description} parameter as tooltip.
 * <p>
 *
 *
 * @author Markus Gärtner
 *
 */
public class ExtensionListCellRenderer extends DefaultListCellRenderer {

	private static final long serialVersionUID = -642091595775524120L;

	private final Reference<PluginEngine> ref;
	private final Resolution resolution;

	public ExtensionListCellRenderer(PluginEngine pluginEngine, Resolution resolution) {
		ref = new WeakReference<>(requireNonNull(pluginEngine));
		this.resolution = requireNonNull(resolution);
	}

	private String getIfPresent(Extension extension, String key, String defaultValue) {
		Extension.Parameter param = extension.getParameter(key);
		String label = null;
		if(param!=null) {
			label = ResourceManager.getInstance().get(param.valueAsString());
		}
		if(label==null) {
			label = defaultValue;
		}
		return label;
	}

	private Icon getIcon(Extension extension) {
		PluginEngine pluginEngine = ref.get();
		if(pluginEngine==null) {
			return null;
		}

		Extension.Parameter param = extension.getParameter("icon");
		if(param==null) {
			return null;
		}

		return pluginEngine.getIconRegistry(extension.getDeclaringPluginDescriptor())
				.getIcon(param.valueAsString(), resolution);
	}

	/**
	 * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
	 */
	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {

		String label = "???";
		String tooltip = null;
		Icon icon = null;

		if(value instanceof Extension) {
			Extension extension = (Extension) value;
			label = getIfPresent(extension, PluginEngine.PARAM_NAME, extension.getUniqueId());
			tooltip = getIfPresent(extension, PluginEngine.PARAM_DESCRIPTION, null);
			icon = getIcon(extension);
		}

		super.getListCellRendererComponent(list, label, index, isSelected, cellHasFocus);

		setToolTipText(GuiUtils.toUnwrappedSwingTooltip(tooltip));
		setIcon(icon);

		return this;
	}
}

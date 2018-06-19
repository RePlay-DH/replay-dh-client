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
 * $Revision: 160 $
 * $Date: 2013-11-07 16:56:45 +0100 (Do, 07 Nov 2013) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus/core/de.ims.icarus/source/de/ims/icarus/util/id/ExtensionIdentity.java $
 *
 * $LastChangedDate: 2013-11-07 16:56:45 +0100 (Do, 07 Nov 2013) $
 * $LastChangedRevision: 160 $
 * $LastChangedBy: mcgaerty $
 */
package bwfdm.replaydh.ui.id;

import static java.util.Objects.requireNonNull;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.java.plugin.registry.Extension;

import bwfdm.replaydh.core.PluginEngine;
import bwfdm.replaydh.resources.DefaultResourceLoader;
import bwfdm.replaydh.resources.ManagedResource;
import bwfdm.replaydh.resources.ResourceLoader;
import bwfdm.replaydh.resources.ResourceManager;

/**
 * @author Markus Gärtner
 * @version $Id: ExtensionIdentity.java 160 2013-11-07 15:56:45Z mcgaerty $
 *
 */
public class ExtensionIdentity implements Identity {

	protected final PluginEngine pluginEngine;
	protected final Extension extension;

	protected ManagedResource resources;
	protected Icon icon = null;

	/**
	 *
	 */
	public ExtensionIdentity(Extension extension, PluginEngine pluginEngine) {

		this.pluginEngine = requireNonNull(pluginEngine);
		this.extension = requireNonNull(extension);

		Extension.Parameter param = extension.getParameter("resources"); //$NON-NLS-1$
		if(param!=null) {
			ClassLoader classLoader = pluginEngine.getClassLoader(extension);
			String basename = param.valueAsString();
			ResourceLoader loader = new DefaultResourceLoader(classLoader);
			resources = ResourceManager.getInstance().addManagedResource(basename, loader);
		}
	}

	/**
	 * @see de.ims.icarus.util.id.Identity#getId()
	 */
	@Override
	public String getId() {
		Extension.Parameter param = extension.getParameter("id"); //$NON-NLS-1$

		return param==null ? extension.getId() : param.valueAsString();
	}

	private String getResource(String key) {
		return resources==null ? key
				: resources.getResource(ResourceManager.getInstance().getLocale(), key);
	}

	/**
	 * @see de.ims.icarus.util.id.Identity#getName()
	 */
	@Override
	public String getName() {
		Extension.Parameter param = extension.getParameter("name"); //$NON-NLS-1$

		if(param!=null) {
			String name = param.valueAsString();
			return getResource(name);
		}

		return getId();
	}

	/**
	 * @see de.ims.icarus.util.id.Identity#getDescription()
	 */
	@Override
	public String getDescription() {
		Extension.Parameter param = extension.getParameter("description"); //$NON-NLS-1$

		if(param!=null) {
			String desc = param.valueAsString();
			return getResource(desc);
		}

		return null;
	}

	/**
	 * @see de.ims.icarus.util.id.Identity#getIcon()
	 */
	@Override
	public Icon getIcon() {
		Extension.Parameter param = extension.getParameter("icon"); //$NON-NLS-1$
		if(icon==null && param!=null) {
			ClassLoader loader = pluginEngine.getClassLoader(extension);
			URL iconLocation = loader.getResource(param.valueAsString());

			if(iconLocation!=null) {
				icon = new ImageIcon(iconLocation);
			}
		}

		return icon;
	}

//	/**
//	 * @see de.ims.icarus.util.id.Identity#getOwner()
//	 */
//	@Override
//	public Object getOwner() {
//		return extension;
//	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Identity) {
			Identity other = (Identity)obj;
			return getId().equals(other.getId());
		}
		return false;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getId();
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getId().hashCode();
	}
}

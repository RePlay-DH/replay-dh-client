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
 * $Revision: 288 $
 * $Date: 2014-08-13 13:38:45 +0200 (Mi, 13 Aug 2014) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus/core/de.ims.icarus/source/de/ims/icarus/util/id/DefaultIdentity.java $
 *
 * $LastChangedDate: 2014-08-13 13:38:45 +0200 (Mi, 13 Aug 2014) $
 * $LastChangedRevision: 288 $
 * $LastChangedBy: mcgaerty $
 */
package bwfdm.replaydh.ui.id;

import static java.util.Objects.requireNonNull;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * @author Markus Gärtner
 * @version $Id: DefaultIdentity.java 288 2014-08-13 11:38:45Z mcgaerty $
 *
 */
public class DefaultIdentity implements MutableIdentity {

	protected final String id;
	protected String name;
	protected String description;
	protected URL iconLocation;
	protected Icon icon;

	public DefaultIdentity(String id) {
		this.id = requireNonNull(id);
	}

	public DefaultIdentity(String id, String description, Icon icon) {
		this.id = requireNonNull(id);
		this.description = description;
		this.icon = icon;
	}

	/**
	 * @see de.ims.icarus.util.id.Identity#getId()
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * @see de.ims.icarus.util.id.Identity#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @see de.ims.icarus.util.id.Identity#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * @see de.ims.icarus.util.id.Identity#getIcon()
	 */
	@Override
	public Icon getIcon() {
		Icon icon = this.icon;

		if(icon==null && iconLocation!=null) {
			icon = new ImageIcon(iconLocation);
		}

		return icon;
	}

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
	 * @return the iconLocation
	 */
	public URL getIconLocation() {
		return iconLocation;
	}

	/**
	 * @param name the name to set
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param description the description to set
	 */
	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @param iconLocation the iconLocation to set
	 */
	public void setIconLocation(URL iconLocation) {
		this.iconLocation = iconLocation;
	}

	/**
	 * @param icon the icon to set
	 */
	public void setIcon(Icon icon) {
		this.icon = icon;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return id.hashCode();
	}

}

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
 * $Revision: 263 $
 * $Date: 2014-06-16 15:43:09 +0200 (Mo, 16 Jun 2014) $
 * $URL: https://subversion.assembla.com/svn/icarusplatform/trunk/Icarus/core/de.ims.icarus/source/de/ims/icarus/util/id/Identity.java $
 *
 * $LastChangedDate: 2014-06-16 15:43:09 +0200 (Mo, 16 Jun 2014) $
 * $LastChangedRevision: 263 $
 * $LastChangedBy: mcgaerty $
 */
package bwfdm.replaydh.ui.id;

import java.util.Comparator;

import javax.swing.Icon;

/**
 * @author Markus Gärtner
 * @version $Id: Identity.java 263 2014-06-16 13:43:09Z mcgaerty $
 *
 */
public interface Identity {

	String getId();

	String getName();

	String getDescription();

	Icon getIcon();

//	Object getOwner();

	/**
	 * Name based comparator
	 */
	public static final Comparator<Identity> COMPARATOR = new Comparator<Identity>() {

		@Override
		public int compare(Identity i1, Identity i2) {
			String name1 = i1.getName();
			String name2 = i2.getName();
			if(name1!=null && name2!=null) {
				return name1.compareTo(name2);
			} else {
				return i1.getId().compareTo(i2.getId());
			}
		}

	};
}

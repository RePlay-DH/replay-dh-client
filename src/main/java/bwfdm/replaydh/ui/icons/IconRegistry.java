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
package bwfdm.replaydh.ui.icons;

import static java.util.Objects.requireNonNull;

import java.io.ObjectStreamException;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.core.RDHClient;


/**
 * @author Markus Gärtner
 * @version $Id: IconRegistry.java 389 2015-04-23 10:19:15Z mcgaerty $
 *
 */
public final class IconRegistry {

	private final Map<String, IconInfo> icons = new HashMap<>();

	private final List<Entry<ClassLoader, String>> loaders = new ArrayList<>();

	private final IconRegistry parent;

	private static volatile IconRegistry globalRegistry;

	private static final Logger log = LoggerFactory.getLogger(IconRegistry.class);

	public static IconRegistry getGlobalRegistry() {
		if(globalRegistry==null) {
			synchronized (IconRegistry.class) {
				if(globalRegistry==null) {
					IconRegistry newGlobalRegistry= new IconRegistry(null);
					newGlobalRegistry.addSearchPath("bwfdm/replaydh/ui/icons/"); //$NON-NLS-1$

					globalRegistry = newGlobalRegistry;
				}
			}
		}

		return globalRegistry;
	}

	public static IconRegistry newRegistry(IconRegistry parent) {
		// TODO maybe force globalRegistry to be fallback parent?
		return new IconRegistry(parent);
	}

	private IconRegistry(IconRegistry parent) {
		this.parent = parent;
	}

	// prevent multiple deserialization
	private Object readResolve() throws ObjectStreamException {
		return getGlobalRegistry();
	}

	// prevent cloning
	@Override
	protected Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	public void addSearchPath(String prefix) {
		addSearchPath(null, prefix);
	}

	public void addSearchPath(ClassLoader loader, String prefix) {
		if(loader==null && prefix==null)
			throw new IllegalArgumentException("Either loader or prefix has to be defined!"); //$NON-NLS-1$

		if(loader==null)
			loader = RDHClient.class.getClassLoader(); // Use "root" loader

		if(prefix==null)
			prefix = ""; //$NON-NLS-1$

		synchronized (loaders) {

			// check for duplicates
			for(int i=0; i<loaders.size(); i++) {
				Entry<ClassLoader, String> entry = loaders.get(i);
				if(entry.getKey().equals(loader) && entry.getValue().equals(prefix)) {
					return;
				}
			}

			// not present yet -> add new entry
			Entry<ClassLoader, String> entry = new AbstractMap.SimpleEntry<>(loader, prefix);
			loaders.add(entry);
		}
	}

	public void removeSearchPath(ClassLoader loader, String prefix) {
		requireNonNull(loader);
		if(prefix==null)
			prefix = ""; //$NON-NLS-1$

		synchronized (loaders) {
			for(int i=0; i<loaders.size(); i++) {
				Entry<ClassLoader, String> entry = loaders.get(i);
				if(entry.getKey().equals(loader) && entry.getValue().equals(prefix)) {
					loaders.remove(i);
					break;
				}
			}
		}
	}

	private IconInfo lookupIcon(String name) {
		// Check cache first
		IconInfo iconInfo = icons.get(requireNonNull(name));

		// Delegate to parent if missing
		if(iconInfo==null && parent!=null)
			iconInfo = parent.lookupIcon(name);

		return iconInfo;
	}

	private IconInfo findIcon(String name) {
		IconInfo iconInfo = null;

		// Try local search paths first
		synchronized (loaders) {
			for(Entry<ClassLoader, String> entry : loaders) {
				try {
					String prefix = entry.getValue();
					ClassLoader loader = entry.getKey();

					// apply prefix
					String path = prefix+name;

					// try to locate resource
					URL location = loader.getResource(path);

					// create new icon
					if(location!=null) {
						iconInfo = new IconInfo(name, location);
					}

					if(iconInfo!=null) {
						break;
					}

				} catch(Exception e) {
					log.error("Error while loading icon: {}", name, e);
				}
			}
		}

		// Save icon for future calls or delegate search
		// to parent which will save loaded icons in its own
		// map so that calls to lookupIcon(String) will search
		// the correct maps
		if(iconInfo!=null) {
			icons.put(name, iconInfo);
		} else if(parent!=null) {
			iconInfo = parent.findIcon(name);
		}

		return iconInfo;
	}

	public IconInfo getIconInfo(String name) {
		IconInfo iconInfo = lookupIcon(name);
		if(iconInfo==null) {
			iconInfo = findIcon(name);
		}

		if(iconInfo==null)
			throw new IllegalArgumentException("Unknown icon name: "+name);

		return iconInfo;
	}

	public URL getIconLocation(String name) {
		return getIconInfo(name).url;
	}

	/**
	 * Returns the icon mapped to the given name in its raw size.
	 *
	 * @param name
	 * @return
	 */
	public Icon getIcon(String name) {
		return getIconInfo(name).ensureIcon();
	}

	/**
	 * Returns the icon mapped to the given name resized to the
	 * provided custom resolution.
	 *
	 * @param name
	 * @param resolution
	 * @return
	 */
	public Icon getIcon(String name, Resolution resolution) {
		return getIconInfo(name).getIcons().getIcon(resolution);
	}

	public final static class IconInfo {
		public final String name;
		public final URL url;
		private volatile IconSet<ImageIcon> icons;

		private IconInfo(String name, URL url) {
			this.name = requireNonNull(name);
			this.url = requireNonNull(url);
		}

		public ImageIcon ensureIcon() {
			IconSet<ImageIcon> icons = this.icons;
			if(icons==null) {
				synchronized (this) {
					if((icons=this.icons)==null) {
						ImageIcon icon = new ImageIcon(url);
						icons = new IconSet<>(icon);
						this.icons = icons;
					}
				}
			}

			return icons.getRawIcon();
		}

		public IconSet<ImageIcon> getIcons() {
			ensureIcon();
			return icons;
		}
	}
}

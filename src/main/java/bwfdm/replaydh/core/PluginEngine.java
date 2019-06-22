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
package bwfdm.replaydh.core;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.file.DirectoryStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.java.plugin.JpfException;
import org.java.plugin.ObjectFactory;
import org.java.plugin.PathResolver;
import org.java.plugin.PluginLifecycleException;
import org.java.plugin.PluginManager;
import org.java.plugin.PluginManager.PluginLocation;
import org.java.plugin.registry.Extension;
import org.java.plugin.registry.Extension.Parameter;
import org.java.plugin.registry.ExtensionPoint;
import org.java.plugin.registry.IntegrityCheckReport;
import org.java.plugin.registry.PluginDescriptor;
import org.java.plugin.registry.PluginElement;
import org.java.plugin.registry.PluginRegistry;
import org.java.plugin.standard.StandardPluginLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.io.resources.ResourceProvider;
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.utils.annotation.Experimental;

/**
 * @author Markus Gärtner
 *
 */
public class PluginEngine extends AbstractRDHTool {

	public static final String CORE_PLUGIN_ID = "bwfdm.replaydh.core";

	private static final Logger log = LoggerFactory.getLogger(PluginEngine.class);

	private PluginRegistry pluginRegistry;
	private PathResolver pathResolver;
	private PluginManager pluginManager;

	private final Path pluginFolder;
	private final ResourceProvider resourceProvider;

	public PluginEngine(Path pluginFolder, ResourceProvider resourceProvider) {
		this.pluginFolder = requireNonNull(pluginFolder);
		this.resourceProvider = requireNonNull(resourceProvider);
	}

	public PluginRegistry getPluginRegistry() {
		checkStarted();
		return pluginRegistry;
	}

	public PathResolver getPathResolver() {
		checkStarted();
		return pathResolver;
	}

	public PluginManager getPluginManager() {
		checkStarted();
		return pluginManager;
	}

	/**
	 * @return the pluginFolder
	 */
	public Path getPluginFolder() {
		return pluginFolder;
	}

	/**
	 * @throws RDHLifecycleException
	 * @see bwfdm.replaydh.core.AbstractRDHTool#start(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public boolean start(RDHEnvironment environment) throws RDHLifecycleException {
		if(!super.start(environment)) {
			return false;
		}

		initPluginEnvironment();

		collectPlugins();

		return true;
	}

	private void initPluginEnvironment() {

		// Init plug-in management objects
		ObjectFactory objectFactory = ObjectFactory.newInstance();
		log.info("Using object factory: {}", objectFactory.getClass());

		PluginRegistry newPluginRegistry = objectFactory.createRegistry();
		log.info("Using plugin registry: {}", newPluginRegistry.getClass());

		//pathResolver = objectFactory.createPathResolver();
		PathResolver newPathResolver = new LibPathResolver();
		log.info("Using path resolver: {}", newPathResolver.getClass());

		PluginManager newPluginManager = objectFactory.createManager(newPluginRegistry, newPathResolver);
		log.info("Using plugin manager: {}", newPluginManager.getClass());

		pluginRegistry = newPluginRegistry;
		pluginManager = newPluginManager;
		pathResolver = newPathResolver;
	}

	private PluginLocation locateCorePlugin() throws RDHLifecycleException {
		Path location;
		try {
			location = IOUtils.getJarFile();
		} catch (NoSuchFileException e) {
			throw new RDHLifecycleException("Failed to obtain location of client Jar file", e);
		}

		PluginLocation pluginLocation;

		if(resourceProvider.isDirectory(location)
				|| location.getFileName().toString().endsWith(".jar")) {

			try {
				pluginLocation = StandardPluginLocation.create(location.toFile());
			} catch (MalformedURLException e) {
				throw new RDHLifecycleException("Failed to create PluginLocation for: "+location, e);
			}

		} else
			throw new RDHLifecycleException("Unfamiliar source location: "+location);

		return pluginLocation;
	}

	private void collectPlugins() throws RDHLifecycleException {
		List<PluginLocation> pluginLocations = new LinkedList<>();
		log.info("Collecting plug-ins");
		pluginLocations.add(locateCorePlugin());
		processFolder(pluginFolder, pluginLocations);
		log.info("Collected {} plug-ins", pluginLocations.size());

		try {
			log.info("Publishing plug-ins");
			getPluginManager().publishPlugins(pluginLocations.toArray(new PluginLocation[pluginLocations.size()]));
		} catch (JpfException e) {
			log.error("Failed to publish plug-ins", e);
			throw new RDHLifecycleException("JpfException encountered: "+e.getMessage(), e);
		}

		if(log.isInfoEnabled()) {
			IntegrityCheckReport report = getPluginRegistry().getRegistrationReport();
			log.info(integrityCheckReport2str("Registration report", report));
		}

		// check plug-ins integrity
		log.info("Checking plug-ins set integrity");
		IntegrityCheckReport report = getPluginRegistry().checkIntegrity( getPathResolver());
		log.info("Integrity check done: {} errors, {} warnings",
				report.countErrors(), report.countWarnings());
		// output report
		if(report.countErrors()>0 || report.countWarnings()>0) {
			log.warn(integrityCheckReport2str("Integrity check report", report));
		} else if(log.isInfoEnabled()) {
			log.info(integrityCheckReport2str("Integrity check report", report));
		}

		// in case of errors simply exit launcher completely
		if(report.countErrors()>0) {
			throw new RDHLifecycleException("Integrity check failed - check log");
		}
	}

    private String integrityCheckReport2str(final String header, final IntegrityCheckReport report) {
        StringBuilder buf = new StringBuilder();
        buf.append(header).append(":\r\n");
        buf.append("-------------- REPORT BEGIN -----------------\r\n");
        for (IntegrityCheckReport.ReportItem item : report.getItems()) {
            buf.append("\tSeverity=").append(item.getSeverity())
                .append("; Code=").append(item.getCode())
                .append("; Message=").append(item.getMessage())
                .append("; Source=").append(item.getSource())
                .append("\n");
        }
        buf.append("-------------- REPORT END -----------------");
        return buf.toString();
    }

    private void processFolder(final Path folder, final List<PluginLocation> result) {
    	if(!resourceProvider.exists(folder)) {
    		return;
    	}

    	log.info("processing folder - {}", folder);
        try {
            PluginLocation pluginLocation = StandardPluginLocation.create(folder.toFile());
            if (pluginLocation != null) {
                result.add(pluginLocation);
                return;
            }
        } catch (MalformedURLException e) {
        	log.warn("Failed collecting plug-in folder {}", folder, e);
            return;
        }

        // Recursively traverse content of folder till we find plug-ins
        try(DirectoryStream<Path> stream = resourceProvider.children(folder, "*")) {
        	for(Path path : stream) {
        		if(resourceProvider.isDirectory(path)) {
                    processFolder(path, result);
        		} else if(resourceProvider.isRegularFile(path)) {
                    processFile(path, result);
        		}
        	}
        } catch (IOException e) {
        	log.error("Failed traversing files in plug-in folder {}", folder, e);
		}
    }

    private void processFile(final Path file, final List<PluginLocation> result) {
    	log.info("processing file - {}", file);
        try {
            PluginLocation pluginLocation = StandardPluginLocation.create(file.toFile());
            if (pluginLocation != null) {
                result.add(pluginLocation);
            }
        } catch (MalformedURLException e) {
        	log.warn("Failed collecting plug-in file {}", file , e);
        }
    }

	public ClassLoader getClassLoader(PluginElement<?> element) {
		return getPluginManager().getPluginClassLoader(element.getDeclaringPluginDescriptor());
	}

	public ClassLoader getClassLoader(String pluginId) {
		return getPluginManager().getPluginClassLoader(getPluginRegistry().getPluginDescriptor(pluginId));
	}

	public PluginDescriptor getCorePlugin() {
		return getPluginRegistry().getPluginDescriptor(CORE_PLUGIN_ID);
	}

	@Experimental
	public IconRegistry getIconRegistry(PluginDescriptor plugin) {
		if(!CORE_PLUGIN_ID.equals(plugin.getId()))
			throw new UnsupportedOperationException(
					"Icon lookup for plugins other than the core plugin not yet implemented!");
		return IconRegistry.getGlobalRegistry();
	}

	public Extension getExtension(String uid) {
		String pluginId = getPluginRegistry().extractPluginId(uid);
		String elementId = getPluginRegistry().extractId(uid);

		if(pluginId==null || elementId==null) {
			return null;
		}

		return getPluginRegistry().getPluginDescriptor(pluginId).getExtension(elementId);
	}

	public List<Extension> getExtensions(String pluginId, String extensionPointId) {
		PluginDescriptor descriptor = getPluginRegistry().getPluginDescriptor(pluginId);
		ExtensionPoint extensionPoint = descriptor.getExtensionPoint(extensionPointId);
		if(extensionPoint==null)
			throw new IllegalArgumentException("No such extension point: "+extensionPointId);
		return new ArrayList<>(extensionPoint.getConnectedExtensions());
	}

	public Path getPluginFolder(PluginElement<?> element) {
		PluginDescriptor descriptor = element.getDeclaringPluginDescriptor();
		return pluginFolder.resolve(descriptor.getId());
	}

	public void activatePlugin(PluginElement<?> element) throws PluginLifecycleException {
		PluginDescriptor descriptor = element.getDeclaringPluginDescriptor();
		if(!getPluginManager().isPluginActivated(descriptor)
				&& !getPluginManager().isPluginActivating(descriptor)) {
			getPluginManager().activatePlugin(descriptor.getId());
		}
	}

	public List<Extension> find(String pluginId, String extensionPointId, Predicate<Extension> filter) {
		requireNonNull(pluginId);
		requireNonNull(extensionPointId);
		requireNonNull(filter);

		return getExtensions(pluginId, extensionPointId).stream()
			.filter(filter)
			.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	public <T extends Object> T instantiate(Extension extension) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException {
		if(extension==null)
			throw new NullPointerException("Invalid extension");

		try {
			activatePlugin(extension);
		} catch (PluginLifecycleException e) {
			log.error("Failed to activate plug-in: {}", extension.getDeclaringPluginDescriptor().getId(), e);

			throw new IllegalStateException("Plug-in not active for extension: "+extension.getUniqueId());
		}

		Extension.Parameter param = extension.getParameter("class");
		if(param==null)
			throw new IllegalArgumentException("Extension does not declare class parameter: "+extension.getUniqueId());

		ClassLoader loader = getClassLoader(extension);
		Class<?> clazz = loader.loadClass(param.valueAsString());
		return (T) clazz.newInstance();
	}

	@SuppressWarnings("unchecked")
	public <T extends Object> T instantiate(Extension extension, Class<?>[] signature, Object[] params) throws InstantiationException,
			IllegalAccessException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalArgumentException, InvocationTargetException {
		if(extension==null)
			throw new NullPointerException("Invalid extension");
		if (signature == null)
			throw new NullPointerException("Invalid signature");
		if (params == null)
			throw new NullPointerException("Invalid params");

		try {
			activatePlugin(extension);
		} catch (PluginLifecycleException e) {
			log.error("Failed to activate plug-in: {}" ,extension.getDeclaringPluginDescriptor().getId(), e);

			throw new IllegalStateException("Plug-in not active for extension: "+extension.getUniqueId());
		}

		Extension.Parameter param = extension.getParameter("class");
		if(param==null)
			throw new IllegalArgumentException("Extension does not declare class parameter: "+extension.getUniqueId());

		ClassLoader loader = getClassLoader(extension);
		Class<?> clazz = loader.loadClass(param.valueAsString());

		Constructor<?> constructor = clazz.getConstructor(signature);

		return (T) constructor.newInstance(params);
	}

	public Class<?> loadClass(Extension extension) throws ClassNotFoundException {
		if(extension==null)
			throw new NullPointerException("Invalid extension");

		try {
			activatePlugin(extension);
		} catch (PluginLifecycleException e) {
			log.error("Failed to activate plug-in: {}", extension.getDeclaringPluginDescriptor().getId(), e);

			throw new IllegalStateException("Plug-in not active for extension: "+extension.getUniqueId());
		}

		Extension.Parameter param = extension.getParameter("class");
		if(param==null)
			throw new IllegalArgumentException("Extension does not declare class parameter: "+extension.getUniqueId());

		ClassLoader loader = getClassLoader(extension);
		return loader.loadClass(param.valueAsString());
	}

	public Class<?> loadClass(Extension.Parameter param) throws ClassNotFoundException {
		if(param==null)
			throw new NullPointerException("Invalid parameter");

		Extension extension = param.getDeclaringExtension();

		try {
			activatePlugin(extension);
		} catch (PluginLifecycleException e) {
			log.error("Failed to activate plug-in: {}", extension.getDeclaringPluginDescriptor().getId(), e);

			throw new IllegalStateException("Plug-in not active for extension: "+extension.getUniqueId());
		}

		ClassLoader loader = getClassLoader(extension);
		return loader.loadClass(param.valueAsString());
	}

	public boolean isInstance(Extension extension, Object data) {
		if(extension==null)
			throw new NullPointerException("Invalid extension");
		if(data==null)
			throw new NullPointerException("Invalid data");

		Class<?> clazz = data instanceof Class ? (Class<?>)data : data.getClass();

		try {
			Extension.Parameter param = extension.getParameter("class");

			return clazz.getName().equals(param.valueAsString());
		} catch(Exception e) {
			return false;
		}
	}

	public static String getParam(Extension extension, String name, String defaultValue) {
		String value = null;
		Extension.Parameter param = extension.getParameter(name);
		if(param!=null) {
			value = param.rawValue();
		}
		if(value==null || value.isEmpty()) {
			value = defaultValue;
		}
		return value;
	}

	public static <T extends Object> T parseParam(Extension extension, String name,
			Function<String, T> parser, T defaultValue) {

		T result = null;
		String value = getParam(extension, name, null);
		if(value!=null) {
			result = parser.apply(value.toUpperCase());
		}
		if(result==null) {
			result = defaultValue;
		}
		return result;
	}

	public static <T extends Object, C extends Collection<T>> C parseParams(Extension extension, String name,
			Function<String, T> parser, C buffer) {

		for(Parameter param : extension.getParameters(name)) {
			String value = param.rawValue();
			T item = parser.apply(value.toUpperCase());
			if(item!=null) {
				buffer.add(item);
			}
		}

		return buffer;
	}

	public static final String PARAM_NAME = "name";
	public static final String PARAM_DESCRIPTION = "description";
	public static final String PARAM_ICON = "icon";

	public static final Comparator<Extension> LOCALIZABLE_ORDER = (e1, e2) -> {
		String name1 = e1.getParameter(PARAM_NAME).valueAsString();
		String name2 = e2.getParameter(PARAM_NAME).valueAsString();

		return name1.compareTo(name2);
	};
}

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
package bwfdm.replaydh.ui.metadata;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.swing.Icon;
import javax.swing.tree.TreePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.git.GitUtils;
import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.ui.id.Identity;
import bwfdm.replaydh.ui.tree.AbstractTreeModel;

/**
 * @author Markus Gärtner
 *
 */
public class WorkspaceTreeModel extends AbstractTreeModel {

	private static final Logger log = LoggerFactory.getLogger(WorkspaceTreeModel.class);

	private Path rootFolder;

	private boolean showHiddenFiles = false;
	private boolean showHiddenFolders = false;

	private Predicate<? super Path> filter;
	private Matcher matcher;

	private final Map<Path, List<Path>> tree = new HashMap<>();

	private Order order = Order.FOLDER_FIRST;

	private boolean include(Path file) {
		//ALWAYS ignore our git files!!!
		if(GitUtils.isGitRelatedFile(file)) {
			return false;
		}

		// Use those checks that are cheap but can fail with exception first
		try {
			if(!showHiddenFolders && Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS) && Files.isHidden(file)) {
				return false;
			} else if(!showHiddenFiles && Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) && Files.isHidden(file)) {
				return false;
			}
		} catch (IOException e) {
			log.error("Failed to read hidden flag of file: {}", file, e); // maybe a bad idea to log at that frequency?
			// In any case, exception means we ignore the file
			return false;
		}

		// Regex check might be _somewhat_ expensive
		if(matcher!=null && !matcher.reset().find()) {
			return false;
		}

		// Give explicit filter a chance last, as it might be expensive
		if(filter!=null && !filter.test(file)) {
			return false;
		}

		// No filter discarded the file, so accept it
		return true;
	}

	private List<Path> children(Path file) {
		if(file==null || !Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS)) {
			return Collections.emptyList();
		}

		List<Path> items = tree.get(file);

		if(items==null) {
			// Grab all the filtered files and folders
			try(DirectoryStream<Path> stream = Files.newDirectoryStream(file, this::include)) {
				items = StreamSupport.stream(stream.spliterator(), false)
					.filter(this::include)
					.sorted(order)
					.collect(Collectors.toList());
			} catch (IOException e) {
				log.error("Failed to read files for directory: {}", file, e);
			}

			tree.put(file, Optional.ofNullable(items).orElse(Collections.emptyList()));
		}

		return items;
	}

	private Path path(Object node) {
		return node==getRoot() ? rootFolder : (Path) node;
	}

	/**
	 * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
	 */
	@Override
	public Object getChild(Object parent, int index) {
		return children(path(parent)).get(index);
	}

	/**
	 * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
	 */
	@Override
	public int getChildCount(Object parent) {
		return children(path(parent)).size();
	}

	public Path getRootFolder() {
		return rootFolder;
	}

	public boolean isShowHiddenFiles() {
		return showHiddenFiles;
	}

	public boolean isShowHiddenFolders() {
		return showHiddenFolders;
	}

	public Predicate<? super Path> getFilter() {
		return filter;
	}

	public String getPattern() {
		return matcher==null ? null : matcher.pattern().pattern();
	}

	public Order getOrder() {
		return order;
	}

	public void setRootFolder(Path rootFolder) {
		this.rootFolder = rootFolder;
		tree.clear();
		fireStructureChanged();
	}

	public void setShowHiddenFiles(boolean showHiddenFiles) {
		this.showHiddenFiles = showHiddenFiles;
	}

	public void setShowHiddenFolders(boolean showHiddenFolders) {
		this.showHiddenFolders = showHiddenFolders;
	}

	public void setFilter(Predicate<? super Path> filter) {
		this.filter = filter;
	}

	public void setPattern(String pattern) {
		this.matcher = pattern==null ? null : Pattern.compile(pattern).matcher("");
	}

	public void setOrder(Order order) {
		this.order = requireNonNull(order);
	}

	public TreePath toTreePath(Path path) {
		List<Path> elements = new ArrayList<>();
		do {
			elements.add(path);
		} while ((path = path.getParent()) !=null && !rootFolder.equals(path));

		Collections.reverse(elements);

		return new TreePath(elements.toArray());
	}

	public void pathChanged(Path path) {
		Path relative = IOUtils.relativize(rootFolder, path);
		if(relative==null) {
			return;
		}
		firePathChanged(toTreePath(relative));
	}

	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	public enum Order implements Comparator<Path>, Identity {
		NAME_ASC("name_asc"){
			@Override
			public int compare(Path p1, Path p2) {
				return p1.compareTo(p2);
			}
		},
		NAME_DESC("name_desc"){
			@Override
			public int compare(Path p1, Path p2) {
				return -p1.compareTo(p2);
			}
		},
		FILES_FIRST("files_first") {
			@Override
			public int compare(Path p1, Path p2) {
				boolean isFile1 = Files.isRegularFile(p1, LinkOption.NOFOLLOW_LINKS);
				boolean isFile2 = Files.isRegularFile(p2, LinkOption.NOFOLLOW_LINKS);

				if(isFile1 && isFile2) {
					return NAME_ASC.compare(p1, p2);
				} else if(isFile1) {
					return -1;
				} else {
					return 1;
				}
			}
		},
		FOLDER_FIRST("folder_first") {
			@Override
			public int compare(Path p1, Path p2) {
				boolean isFolder1 = Files.isDirectory(p1, LinkOption.NOFOLLOW_LINKS);
				boolean isFolder2 = Files.isDirectory(p2, LinkOption.NOFOLLOW_LINKS);

				if(isFolder1 && isFolder2) {
					return NAME_ASC.compare(p1, p2);
				} else if(isFolder1) {
					return -1;
				} else {
					return 1;
				}
			}
		}
		;

		private Order(String key) {
			this.key = key;
		}

		private final String key;

		/**
		 * @see bwfdm.replaydh.ui.id.Identity#getId()
		 */
		@Override
		public String getId() {
			return key;
		}

		/**
		 * @see bwfdm.replaydh.ui.id.Identity#getName()
		 */
		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * @see bwfdm.replaydh.ui.id.Identity#getDescription()
		 */
		@Override
		public String getDescription() {
			// TODO Auto-generated method stub
			return null;
		}

		/**
		 * @see bwfdm.replaydh.ui.id.Identity#getIcon()
		 */
		@Override
		public Icon getIcon() {
			return null;
		}
	}
}

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
package bwfdm.replaydh.ui.workflow;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.swing.AbstractListModel;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListModel;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;

import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.io.LocalFileObject;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.Editor;
import bwfdm.replaydh.ui.helper.PassiveTextArea;
import bwfdm.replaydh.ui.list.AbstractListCellRendererPanel;

/**
 * @author Markus Gärtner
 *
 */
public class FileIgnoreEditor implements Editor<FileIgnoreEditor.FileIgnoreConfiguration> {

	public static class FileIgnoreConfiguration {
		/**
		 * Files that have been unknown to the client so far
		 * and which should be ignored.
		 */
		private final Set<LocalFileObject> newFilesToIgnore = new HashSet<>();
		/**
		 * Files that have been tracked by the client already
		 * and which should now be ignored.
		 */
		private final Set<LocalFileObject> modifiedFilesToIgnore = new HashSet<>();
		/**
		 * Files that have been unknown to the client so far
		 * and which should be kept.
		 */
		private final Set<LocalFileObject> newFilesToKeep = new HashSet<>();
		/**
		 * Files that have been tracked by the client already
		 * and which should now be kept.
		 */
		private final Set<LocalFileObject> modifiedFilesToKeep = new HashSet<>();

		public Set<LocalFileObject> getNewFilesToIgnore() {
			return newFilesToIgnore;
		}
		public Set<LocalFileObject> getModifiedFilesToIgnore() {
			return modifiedFilesToIgnore;
		}
		public Set<LocalFileObject> getNewFilesToKeep() {
			return newFilesToKeep;
		}
		public Set<LocalFileObject> getModifiedFilesToKeep() {
			return modifiedFilesToKeep;
		}

		public void clear() {
			modifiedFilesToIgnore.clear();
			modifiedFilesToKeep.clear();
			newFilesToIgnore.clear();
			newFilesToKeep.clear();
		}

		public void copyFrom(FileIgnoreConfiguration other) {
			clear();

			if(other!=null) {
				modifiedFilesToIgnore.addAll(other.modifiedFilesToIgnore);
				modifiedFilesToKeep.addAll(other.modifiedFilesToKeep);
				newFilesToIgnore.addAll(other.newFilesToIgnore);
				newFilesToKeep.addAll(other.newFilesToKeep);
			}
		}

		@Override
		public FileIgnoreConfiguration clone() {
			FileIgnoreConfiguration conf = new FileIgnoreConfiguration();
			conf.copyFrom(this);
			return conf;
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if(obj==this) {
				return true;
			} else if(obj instanceof FileIgnoreConfiguration) {
				FileIgnoreConfiguration other = (FileIgnoreConfiguration) obj;
				return Objects.equals(modifiedFilesToIgnore, other.modifiedFilesToIgnore)
						&& Objects.equals(modifiedFilesToKeep, other.modifiedFilesToKeep)
						&& Objects.equals(newFilesToIgnore, other.newFilesToIgnore)
						&& Objects.equals(newFilesToKeep, other.newFilesToKeep);
			}
			return false;
		}

		/**
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return Objects.hash(modifiedFilesToIgnore, modifiedFilesToKeep, newFilesToIgnore, newFilesToKeep);
		}
	}

	public static FileIgnoreConfiguration newConfiguration(Set<LocalFileObject> newFilesToIgnore,
			Set<LocalFileObject> modifiedFilesToIgnore) {
		FileIgnoreConfiguration configuration = new FileIgnoreConfiguration();
		configuration.newFilesToIgnore.addAll(newFilesToIgnore);
		configuration.modifiedFilesToIgnore.addAll(modifiedFilesToIgnore);
		return configuration;
	}

	private final JPanel panel;
	private final FileIgnoreListModel newFilesModel, modifiedFilesModel;

	private final FileIgnoreConfiguration backupConf = new FileIgnoreConfiguration();
	private FileIgnoreConfiguration workConf;

	private final JComponent newFilesPanel, modifiedFilesPanel;

	private final MouseListener toggleAdapter = new MouseAdapter() {
		/**
		 * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
		 */
		@Override
		public void mouseClicked(MouseEvent e) {
			if(!e.isPopupTrigger() && e.getClickCount()==2) {
				JList<?> list = (JList<?>) e.getComponent();
				int row = list.locationToIndex(e.getPoint());
				if(row!=-1 && list.getCellBounds(row, row).contains(e.getPoint())) {
					((FileIgnoreListModel)list.getModel()).toggleIgnoreFile(row);
				}
			}
		}
	};

	public FileIgnoreEditor() {

		newFilesModel = new FileIgnoreListModel();
		modifiedFilesModel = new FileIgnoreListModel();

		ResourceManager rm = ResourceManager.getInstance();

		newFilesPanel = createListSection(newFilesModel, true);
		modifiedFilesPanel = createListSection(modifiedFilesModel, false);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true,
				newFilesPanel, modifiedFilesPanel);
		GuiUtils.defaultHideSplitPaneDecoration(splitPane);
		splitPane.setOneTouchExpandable(true);
		splitPane.setResizeWeight(0.5);

		JTextArea header = new PassiveTextArea(rm.get("replaydh.ui.editor.ignoreFiles.header"));

		/**
		 * <pre>
		 * +----------------------------------------+
		 * |     DESC                               |
		 * +----------------------------------------+
		 * |  NEW                                   |
		 * |+--------------------------------------+|
		 * || PATH LIST                            ||
		 * ||                                      ||
		 * |+--------------------------------------+|
		 * +----------------------------------------+
		 * |  MOD                                   |
		 * |+--------------------------------------+|
		 * || PATH LIST                            ||
		 * ||                                      ||
		 * |+--------------------------------------+|
		 * +----------------------------------------+
		 * </pre>
		 */

		panel = FormBuilder.create()
				.columns("fill:pref:grow")
				.rows("pref, 6dlu, fill:pref:grow")
				.add(header).xy(1, 1)
				.add(splitPane).xy(1, 3)
				.padding(Paddings.DLU4)
				.build();
	}

	private JComponent createListSection(FileIgnoreListModel model, boolean isNewFilesPanel) {

		ResourceManager rm = ResourceManager.getInstance();

		JList<FileIgnoreInfo> list = createList(model);

		JCheckBox checkBox = new JCheckBox();
		checkBox.setToolTipText(rm.get("replaydh.ui.editor.ignoreFiles.toggleAll"));
		checkBox.addItemListener(ie -> model.setAll(checkBox.isSelected()));

//		ScrollablePanel panel = new ScrollablePanel();
//		panel.setScrollableHeight(ScrollableSizeHint.STRETCH);

		String header = isNewFilesPanel ?
				rm.get("replaydh.ui.editor.ignoreFiles.newFiles")
				: rm.get("replaydh.ui.editor.ignoreFiles.modifiedFiles");

		/**
		 * <pre>
		 * +----------------------------------------+
		 * |  <HEADER>                           |X||
		 * |+--------------------------------------+|
		 * || PATH LIST                            ||
		 * ||                                      ||
		 * |+--------------------------------------+|
		 * +----------------------------------------+
		 * </pre>
		 */

		return FormBuilder.create()
				.columns("fill:pref:grow, 6dlu, pref, 2dlu")
				.rows("pref, 4dlu, fill:pref:grow")
				.addSeparator(header).xy(1, 1)
				.add(checkBox).xy(3, 1)
				.addScrolled(list).xyw(1, 3, 4, "fill, fill")
				.build();
	}

	private JList<FileIgnoreInfo> createList(FileIgnoreListModel model) {
		JList<FileIgnoreInfo> list = new JList<>(model);
		list.setCellRenderer(new FileIgnoreListCellRenderer());
		list.setVisibleRowCount(3);
		list.addMouseListener(toggleAdapter);
		return list;
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.Editor#getEditorComponent()
	 */
	@Override
	public Component getEditorComponent() {
		return panel;
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.Editor#setEditingItem(java.lang.Object)
	 */
	@Override
	public void setEditingItem(FileIgnoreConfiguration item) {
		workConf = item;
		backupConf.copyFrom(workConf);

		updateUI();
	}

	private static final FileIgnoreConfiguration EMPTY_CONF = new FileIgnoreConfiguration();

	private void updateUI() {
		FileIgnoreConfiguration conf = workConf;
		if(conf==null) {
			conf = EMPTY_CONF;
		}

		modifiedFilesModel.update(conf.modifiedFilesToIgnore, conf.modifiedFilesToKeep);
		newFilesModel.update(conf.newFilesToIgnore, conf.newFilesToKeep);

		modifiedFilesPanel.setVisible(!modifiedFilesModel.isEmpty());
		newFilesPanel.setVisible(!newFilesModel.isEmpty());
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.Editor#getEditingItem()
	 */
	@Override
	public FileIgnoreConfiguration getEditingItem() {
		return workConf;
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.Editor#resetEdit()
	 */
	@Override
	public void resetEdit() {
		if(workConf==null) {
			return;
		}

		workConf.copyFrom(backupConf);

		updateUI();
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.Editor#applyEdit()
	 */
	@Override
	public void applyEdit() {
		if(workConf==null) {
			return;
		}

		viewToModel(workConf);
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.Editor#hasChanges()
	 */
	@Override
	public boolean hasChanges() {
		FileIgnoreConfiguration tmp = new FileIgnoreConfiguration();
		viewToModel(tmp);
		return tmp.equals(backupConf);
	}

	/**
	 * @see bwfdm.replaydh.ui.helper.Editor#close()
	 */
	@Override
	public void close() {
		// nothing to do
	}

	private void viewToModel(FileIgnoreConfiguration conf) {
		conf.clear();
		filterModel(modifiedFilesModel, conf.modifiedFilesToIgnore, conf.modifiedFilesToKeep);
		filterModel(newFilesModel, conf.newFilesToIgnore, conf.newFilesToKeep);
	}

	private void filterModel(ListModel<FileIgnoreInfo> model,
			Set<LocalFileObject> ignore, Set<LocalFileObject> keep) {

	}

	private static class FileIgnoreInfo implements Comparable<FileIgnoreInfo> {
		public final LocalFileObject fileObject;
		public boolean doIgnore = true;

		public FileIgnoreInfo(LocalFileObject fileObject) {
			this.fileObject = fileObject;
		}

		public FileIgnoreInfo(LocalFileObject fileObject, boolean doIgnore) {
			this.fileObject = fileObject;
			this.doIgnore = doIgnore;
		}

		/**
		 * @see java.lang.Comparable#compareTo(java.lang.Object)
		 */
		@Override
		public int compareTo(FileIgnoreInfo o) {
			return fileObject.compareTo(o.fileObject);
		}
	}

	private static class FileIgnoreListModel extends AbstractListModel<FileIgnoreInfo> {

		private static final long serialVersionUID = -982812892460223524L;

		private final List<FileIgnoreInfo> entries = new ArrayList<>();

		public void clear() {
			int oldSize = entries.size();

			if(oldSize>0) {
				entries.clear();
				fireIntervalRemoved(this, 0, oldSize-1);
			}
		}

		public void update(Set<LocalFileObject> ignore,
			Set<LocalFileObject> keep) {

			clear();

			for(LocalFileObject file : ignore) {
				entries.add(new FileIgnoreInfo(file));
			}

			for(LocalFileObject file : keep) {
				entries.add(new FileIgnoreInfo(file, false));
			}

			Collections.sort(entries);

			int newSize = entries.size();

			if(newSize>0) {
				fireIntervalAdded(this, 0, newSize-1);
			}
		}

		/**
		 * @see javax.swing.ListModel#getSize()
		 */
		@Override
		public int getSize() {
			return entries.size();
		}

		/**
		 * @see javax.swing.ListModel#getElementAt(int)
		 */
		@Override
		public FileIgnoreInfo getElementAt(int index) {
			return entries.get(index);
		}

		public void toggleIgnoreFile(int index) {
			FileIgnoreInfo info = getElementAt(index);
			info.doIgnore = !info.doIgnore;
			fireContentsChanged(this, index, index);
		}

		public void setAll(boolean value) {
			if(entries.isEmpty()) {
				return;
			}

			for(FileIgnoreInfo info : entries) {
				info.doIgnore = value;
			}

			fireContentsChanged(this, 0, getSize()-1);
		}

		public boolean isEmpty() {
			return entries.isEmpty();
		}
	}

	private static class FileIgnoreListCellRenderer extends AbstractListCellRendererPanel<FileIgnoreInfo> {

		private static final long serialVersionUID = 1723380140504734496L;

		private final JLabel lPath;
		private final JLabel lSize;
		private final JLabel lMetadata;
		private final JCheckBox cbIgnore;

		public FileIgnoreListCellRenderer() {
			lPath = new JLabel();
			lPath.setFont(GuiUtils.defaultLargeInfoFont);
			lSize = new JLabel();
			lMetadata = new JLabel();

			cbIgnore = new JCheckBox();
			cbIgnore.setOpaque(false);

			FormBuilder.create()
				.columns("2dlu, 8dlu, pref, 4dlu, fill:pref:grow, 4dlu, pref, 1dlu")
				.rows("1dlu, pref, $lg, pref, 1dlu")
				.panel(this)
				.add(lPath).xyw(2, 2, 4, "fill, center")
				.add(lSize).xy(3, 4)
				.add(lMetadata).xy(5, 4)
				.add(cbIgnore).xywh(7, 2, 1, 3)
				.build();
		}

		/**
		 * @see bwfdm.replaydh.ui.list.AbstractListCellRendererPanel#prepareRenderer(javax.swing.JList, java.lang.Object, int, boolean, boolean)
		 */
		@Override
		protected void prepareRenderer(JList<? extends FileIgnoreInfo> list, FileIgnoreInfo info, int index,
				boolean isSelected, boolean cellHasFocus) {
			Path path = info.fileObject.getFile();
			lPath.setText(path.getFileName().toString());
			setToolTipText(path.toString());

			lSize.setText(IOUtils.readableSize(path));

			//TODO fill metadata label

			cbIgnore.setSelected(info.doIgnore);
		}

	}
}

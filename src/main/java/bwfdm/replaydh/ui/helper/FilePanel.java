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

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Forms;

import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.ui.icons.Resolution;

/**
 * @author Markus Gärtner
 *
 */
public class FilePanel extends JPanel {

	private static final long serialVersionUID = 6605339170077720531L;

	public static void main(String[] args) {
		FilePanel filePanel = newBuilder()
				.fileLimit(1)
				.build();
		JFrame frame = new JFrame();
		frame.setLayout(new BorderLayout());
		frame.add(filePanel, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();

		frame.setVisible(true);
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	private static final Logger log = LoggerFactory.getLogger(FilePanel.class);

	private final JTextField tfPath;
	private final JList<Path> fileList;
	private final JButton bAdd, bRemove, bClear;
	private final FileDropPanel fileDropPanel;
	private final int fileLimit;
	private final FileFilter fileFilter;
	private final int acceptedFileType;
	private final Consumer<? super JFileChooser> fileChooserSetup;

	private final List<Path> files = new ArrayList<>();

	private JFileChooser fileChooser;

	private final Handler handler;

	private ChangeEvent changeEvent;

	FilePanel(Builder builder) {
		requireNonNull(builder);

		fileLimit = builder.getFileLimit();
		fileFilter = builder.getFileFilter();
		acceptedFileType = builder.getAcceptedFileType();
		fileChooserSetup = builder.getFileChooserSetup();

		handler = new Handler();

		ResourceManager rm = ResourceManager.getInstance();

		fileDropPanel = new FileDropPanel(builder.getDropPanelLabel());
		bAdd = new JButton(builder.getSelectButtonLabel());
		bAdd.addActionListener(handler);

		bClear = new JButton(rm.get("replaydh.labels.clear"));
		bClear.addActionListener(handler);

		if(fileLimit>1) {

			tfPath = null;
			fileList = new JList<>(new DefaultListModel<>());
			fileList.setVisibleRowCount(5);
			fileList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			fileList.getModel().addListDataListener(handler);
			fileList.getSelectionModel().addListSelectionListener(handler);
			fileList.setLayoutOrientation(JList.VERTICAL);

			bRemove = new JButton(rm.get("replaydh.labels.remove"));
			bRemove.addActionListener(handler);

			/**
			 * <pre>
			 * +-----------------------------+
			 * |                             |
			 * |         DROP AREA           |
			 * |                             |
			 * +-----+-----+-----+-----+-----+
			 * |     | ADD | RMV | CLR |     |
			 * +-----+-----+-----+-----+-----+
			 * |                             |
			 * |         FILE LIST           |
			 * |                             |
			 * +-----------------------------+
			 * </pre>
			 */
//			FormBuilder.create()
//			.columns("pref:grow")
//			.rows("fill:pref:grow, 1dlu, pref, 1dlu, pref")
//			.panel(this)
//			.add(fileDropPanel).xy(1, 1, "fill, fill")
//			.add(Forms.buttonBar(bAdd, bRemove, bClear)).xy(1, 3, "center, center")
//			.addScrolled(fileList).xy(1, 5, "fill, fill")
//			.build();

			JScrollPane listScrollPane = new JScrollPane(fileList);

			JComponent buttonBar = Forms.buttonBar(bAdd, bRemove, bClear);
			JComponent topComponent = fileDropPanel;
			JComponent bottomComponent = listScrollPane;

			JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
			splitPane.setResizeWeight(0.2);

			boolean addedDecoration = false;

//			SplitPaneUI ui = splitPane.getUI();
//			if(ui instanceof BasicSplitPaneUI) {
//				BasicSplitPaneDivider divider = null;
//				synchronized (splitPane.getTreeLock()) {
//					for(int i=0; i<splitPane.getComponentCount(); i++) {
//						Component comp = splitPane.getComponent(i);
//						if(comp instanceof BasicSplitPaneDivider) {
//							divider = (BasicSplitPaneDivider) comp;
//							break;
//						}
//					}
//				}
//
//				if(divider!=null) {
//					divider.setLayout(new BorderLayout());
//					divider.add(buttonBar, BorderLayout.CENTER);
//					splitPane.setDividerSize(buttonBar.getPreferredSize().height+4);
//					addedDecoration = true;
//				}
//			}

			if(!addedDecoration) {
				bottomComponent = FormBuilder.create()
						.columns("fill:pref:grow")
						.rows("2dlu, pref, 1dlu, fill:pref:grow")
						.add(buttonBar).xy(1, 2, "center, center")
						.add(listScrollPane).xy(1, 4)
						.build();
			}

			splitPane.setTopComponent(topComponent);
			splitPane.setBottomComponent(bottomComponent);

			setLayout(new BorderLayout());
			add(splitPane, BorderLayout.CENTER);
		} else {
			tfPath = new JTextField();
			fileList = null;
			bRemove = null;

			/**
			 * <pre>
			 * +-----------------------------+
			 * |                             |
			 * |         DROP AREA           |
			 * |                             |
			 * +---------------------+---+---+
			 * | TEXTFIELD           |ADD|CLR|
			 * +---------------------+---+---+
			 * </pre>
			 */
			FormBuilder.create()
				.columns("fill:pref:grow, 2dlu, pref, 2dlu, pref")
				.rows("fill:pref:grow, 1dlu, pref")
				.panel(this)
				.add(fileDropPanel).xyw(1, 1, 5)
				.add(tfPath).xy(1, 3)
				.add(bAdd).xy(3, 3)
				.add(bClear).xy(5, 3)
				.build();
		}

		refreshButtons();
		refreshTextField();
	}

	public void addChangeListener(ChangeListener listener) {
		listenerList.add(ChangeListener.class, listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		listenerList.remove(ChangeListener.class, listener);
	}

	private void showFileChooser() {
		getFileChooser();

		// Reset file chooser selection
		fileChooser.setSelectedFiles(null);

		// Let user select files
		if(fileChooser.showDialog(this, null)==JFileChooser.APPROVE_OPTION) {
			if(fileChooser.isMultiSelectionEnabled()) {
				File[] files = fileChooser.getSelectedFiles();
				addFiles(files);
			} else {
				File file = fileChooser.getSelectedFile();
				setFile(file);
			}
		}
	}

	private void configureFileChooser(JFileChooser fileChooser) {
		fileChooser.setApproveButtonText(bAdd.getText());
		fileChooser.setMultiSelectionEnabled(isAllowMultipleFiles());
		fileChooser.setFileFilter(fileFilter);
		fileChooser.setFileSelectionMode(acceptedFileType);

		if(fileChooserSetup!=null) {
			fileChooserSetup.accept(fileChooser);
		}
	}

	public JFileChooser getFileChooser() {
		if(fileChooser==null) {
			fileChooser = new JFileChooser();
			configureFileChooser(fileChooser);
		}
		return fileChooser;
	}

	public boolean isFileAccepted(File file) {
		return fileFilter == null || fileFilter.accept(file);
	}

	public boolean isFileAccepted(Path file) {
		return isFileAccepted(file.toFile());
	}

	public boolean isAllowMultipleFiles() {
		return fileLimit>1;
	}

	private void refreshTextField() {
		if(tfPath!=null) {
			String text = files.isEmpty() ? null : files.get(0).toString();
			tfPath.setText(text);
		}
	}

	private void fireChangeEvent() {
		ChangeListener[] listeners = listenerList.getListeners(ChangeListener.class);
		for(ChangeListener listener : listeners) {
			if(changeEvent==null) {
				changeEvent = new ChangeEvent(this);
			}
			listener.stateChanged(changeEvent);
		}
	}

	private void refreshFileFromTextField() {

	}

	private void setFile0(Path path) {
		files.clear();

		if (path != null) {
			files.add(path);
		}

		refreshButtons();
		refreshTextField();

		fireChangeEvent();
	}

	public void setFile(Path path) {
		setFile0(path);
	}

	public void setFile(File file) {
		Path path = file==null ? null : file.toPath();
		setFile0(path);
	}

	public void setFile(String filename) {
		Path path = filename==null ? null : Paths.get(filename);
		setFile0(path);
	}

	public void addFiles(Path... files) {
		if(files==null || files.length==0) {
			return;
		}

		checkState("Too many files", canAddFiles(files.length));

		if(fileList!=null) {
			DefaultListModel<Path> listModel = (DefaultListModel<Path>) fileList.getModel();

			for (Path file : files) {
				if (isFileAccepted(file)) {
					listModel.addElement(file);
				}
			}
		}

		Collections.addAll(this.files, files);

		refreshTextField();
		refreshButtons();

		fireChangeEvent();
	}

	public void addFiles(File... files) {
		if(files==null || files.length==0) {
			return;
		}

		checkState("Too many files", canAddFiles(files.length));

		if(fileList!=null) {
			DefaultListModel<Path> listModel = (DefaultListModel<Path>) fileList.getModel();

			for (File file : files) {
				Path path = file.toPath();
				if (isFileAccepted(path)) {
					listModel.addElement(path);
					this.files.add(path);
				}
			}
		} else {
			for (File file : files) {
				this.files.add(file.toPath());
			}
		}

		refreshTextField();
		refreshButtons();

		fireChangeEvent();
	}

	private boolean isValidFiles(Collection<? extends File> files) {
		for (File file : files) {
			if (!isFileAccepted(file)) {
				return false;
			}
		}

		return true;
	}

	private boolean canAddFiles(int numNewFiles) {
		return getFileCount()+numNewFiles<=fileLimit;
	}

	private int getFileCount() {
		return files.size();
	}

	public Path[] getFiles() {
		if(files.isEmpty()) {
			return new Path[0];
		} else {
			Path[] result = new Path[files.size()];
			return files.toArray(result);
		}
	}

	public Path getFile() {
		checkState("more than 1 file added", files.size()<=1);
		Path file = files.isEmpty() ? null : files.get(0);

		if(file==null && tfPath!=null) {
			String text = tfPath.getText();
			if(text!=null && !text.isEmpty()) {
				file = Paths.get(text);
			}
		}

		return file;
	}

	public String getFilePath() {
		checkState("more than 1 file added", files.size()<=1);
		Path file = files.isEmpty() ? null : files.get(0);

		if(file!=null) {
			return file.toString();
		} else if(tfPath!=null) {
			return tfPath.getText();
		}

		return null;
	}

	public void removeAllFiles() {
		files.clear();
		if(fileList!=null) {
			DefaultListModel<Path> listModel = (DefaultListModel<Path>) fileList.getModel();
			listModel.removeAllElements();
		}

		refreshTextField();
		refreshButtons();

		fireChangeEvent();
	}

	private void refreshButtons() {

		boolean canClear = !files.isEmpty();

		bClear.setEnabled(canClear);

		if(bRemove!=null) {
			boolean canRemove = !fileList.getSelectionModel().isSelectionEmpty();
			bRemove.setEnabled(canRemove);
		}
	}

	private class Handler implements ActionListener, ListSelectionListener, ListDataListener, DocumentListener {

		/**
		 * @see javax.swing.event.ListDataListener#intervalAdded(javax.swing.event.ListDataEvent)
		 */
		@Override
		public void intervalAdded(ListDataEvent e) {
			refreshButtons();
		}

		/**
		 * @see javax.swing.event.ListDataListener#intervalRemoved(javax.swing.event.ListDataEvent)
		 */
		@Override
		public void intervalRemoved(ListDataEvent e) {
			refreshButtons();
		}

		/**
		 * @see javax.swing.event.ListDataListener#contentsChanged(javax.swing.event.ListDataEvent)
		 */
		@Override
		public void contentsChanged(ListDataEvent e) {
			refreshButtons();
		}

		/**
		 * @see javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
		 */
		@Override
		public void valueChanged(ListSelectionEvent e) {
			refreshButtons();
		}

		/**
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getSource()==bAdd) {
				showFileChooser();
			} else if(e.getSource()==bRemove) {
				int[] selectedIndices = fileList.getSelectedIndices();
				if(selectedIndices.length>0) {
					DefaultListModel<Path> listModel = (DefaultListModel<Path>) fileList.getModel();
					for(int i=selectedIndices.length-1; i>=0; i--) {
						Path p = listModel.remove(selectedIndices[i]);
						files.remove(p); //TODO not the most performant way, but should be sufficient
					}
				}
			} else if(e.getSource()==bClear) {
				removeAllFiles();
			}
		}

		/**
		 * @see javax.swing.event.DocumentListener#insertUpdate(javax.swing.event.DocumentEvent)
		 */
		@Override
		public void insertUpdate(DocumentEvent e) {
			refreshFileFromTextField();
		}

		/**
		 * @see javax.swing.event.DocumentListener#removeUpdate(javax.swing.event.DocumentEvent)
		 */
		@Override
		public void removeUpdate(DocumentEvent e) {
			refreshFileFromTextField();
		}

		/**
		 * @see javax.swing.event.DocumentListener#changedUpdate(javax.swing.event.DocumentEvent)
		 */
		@Override
		public void changedUpdate(DocumentEvent e) {
			refreshFileFromTextField();
		}

	}

	private static final Color VALID_DRAG_BACKGROUND = new Color(0, 255, 0, 64);
	private static final Color INVALID_DRAG_BACKGROUND = new Color(249, 51, 51);

	private class FileDropPanel extends JLabel implements DropTargetListener {

		private static final long serialVersionUID = 3280984907756763096L;

		/**
		 * Location of last drag event
		 */
		private Point dragPoint;

		/**
		 * Flag to signal that drag operation is occurring within component
		 */
		private boolean dragOver = false;

		/**
		 * Result of the latest check on dragged files
		 */
		private FileDragState fileDragState = FileDragState.NO_DRAG;

		private final Icon actionIcon = IconRegistry.getGlobalRegistry().getIcon("add.png", Resolution.forSize(16));

		public FileDropPanel(String text) {
			setText(text);
			setHorizontalAlignment(SwingConstants.CENTER);
			setVerticalAlignment(SwingConstants.CENTER);
			setOpaque(false);
			setFocusable(false);

			setDropTarget(new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this));
			setMinimumSize(new Dimension(200, 80));
			setPreferredSize(getMinimumSize());
		}

		private void paintDropDecoration(Graphics2D g) {
			// if (dragPoint != null && actionIcon != null) {
			// int x = dragPoint.x-actionIcon.getIconWidth();
			// int y = dragPoint.y-actionIcon.getIconHeight();
			// actionIcon.paintIcon(this, g, x, y);
			// }
		}

		private Color getColorForDrag() {
			if(dragOver) {
				switch (fileDragState) {
				case NO_DRAG: return getBackground();
				case VALID_FILES: return VALID_DRAG_BACKGROUND;

				default:
					return INVALID_DRAG_BACKGROUND;
				}
			} else {
				return getBackground();
			}
		}

		/**
		 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
		 */
		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2d = (Graphics2D) g.create();
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			int w = getWidth();
			int h = getHeight();

			Color bg = getColorForDrag();

			// Fill entire drawing area with background
			g2d.setColor(bg);
			g2d.fillRect(0, 0, w, h);

			g2d.setColor(Color.black);

			// Inlined border
			g2d.drawRect(1, 1, w - 2, h - 2);
			// 2 diagonal lines
			g2d.drawLine(3, 3, w - 4, h - 4);
			g2d.drawLine(3, h - 4, w - 4, 3);

			String text = getText();
			if (text != null) {
				FontMetrics fm = g2d.getFontMetrics();
				int fh = fm.getHeight() + 2;
				int fw = fm.stringWidth(text);

				g2d.setColor(bg);
				g2d.fillRect(w / 2 - fw / 2, h / 2 - fh / 2, fw, fh);
			}

			super.paintComponent(g);

			if (dragOver) {
				paintDropDecoration(g2d);
			}

			g2d.dispose();
		}

		private boolean isDataFlavorSupported(DropTargetEvent dte, DataFlavor df) {
			if(dte instanceof DropTargetDragEvent) {
				return ((DropTargetDragEvent)dte).isDataFlavorSupported(df);
			} else if(dte instanceof DropTargetDropEvent) {
				return ((DropTargetDropEvent)dte).isDataFlavorSupported(df);
			} else {
				return false;
			}
		}

		private Transferable getTransferable(DropTargetEvent dte) {
			if(dte instanceof DropTargetDragEvent) {
				return ((DropTargetDragEvent)dte).getTransferable();
			} else if(dte instanceof DropTargetDropEvent) {
				return ((DropTargetDropEvent)dte).getTransferable();
			} else
				throw new IllegalArgumentException("Not a valid event associated with a transferable object: "+dte);
		}

		private FileDragState processDrag(boolean dragOver, DropTargetEvent dte) {

			if (dragOver && dte!=null) {
				boolean acceptDrag = false;

				if (isDataFlavorSupported(dte, DataFlavor.javaFileListFlavor)) {
					Transferable transferable = getTransferable(dte);
					// Kinda redundant, but just to make sure
					if(transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
						// Switch to check for exceptions during validation of transferable data
						boolean dndError = true;

						try {
							/*
							 *  Native drop sources need this method call or we will get a
							 *  "java.awt.dnd.InvalidDnDOperationException: No drop current"
							 *  exception when trying to get the transferable data.
							 */
							if(dte instanceof DropTargetDropEvent) {
								((DropTargetDropEvent)dte).acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
							}

							@SuppressWarnings("unchecked")
							List<File> transferData = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
							if(transferData.isEmpty()) {
								fileDragState = FileDragState.NO_DRAG;
							} else if((fileLimit==1 && transferData.size()>1) || (fileLimit>1 && !canAddFiles(transferData.size()))) {
								fileDragState = FileDragState.MULTIPLE_FILES;
							} else if(!isValidFiles(transferData)) {
								fileDragState = FileDragState.INVALID_FILE;
							} else {
								fileDragState = FileDragState.VALID_FILES;
								acceptDrag = true;
							}

							dndError = false;
						} catch (UnsupportedFlavorException e) {
							log.error("Unsupported data flavor in file drag operation", e);
						} catch (IOException e) {
							log.error("Failed to obtain transferable content of file drag operation", e);
						}

						if(dndError) {
							fileDragState = FileDragState.DND_ERROR;
						}
					} else {
						fileDragState = FileDragState.INVALID_FLAVOR;
					}
				}

				if(dte instanceof DropTargetDragEvent) {
					DropTargetDragEvent dtde = (DropTargetDragEvent)dte;
					if(acceptDrag) {
						dtde.acceptDrag(DnDConstants.ACTION_COPY);
					} else {
						dtde.rejectDrag();
					}
				}
			} else {
				fileDragState = FileDragState.NO_DRAG;
			}

			// Make sure our UI gets adjusted to the new drag situation
			final Point dragPoint = (dragOver && dte instanceof DropTargetDragEvent) ?
					((DropTargetDragEvent)dte).getLocation() : null;
			SwingUtilities.invokeLater(() -> {
				FileDropPanel.this.dragOver = dragOver;
				FileDropPanel.this.dragPoint = dragPoint;
				FileDropPanel.this.repaint();
			});
			repaint();

//			System.out.println(fileDragState);

			return fileDragState;
		}

		@Override
		public void dragEnter(DropTargetDragEvent dtde) {
			processDrag(true, dtde);
		}

		@Override
		public void dragOver(DropTargetDragEvent dtde) {
			processDrag(true, dtde);
		}

		@Override
		public void dropActionChanged(DropTargetDragEvent dtde) {
		}

		@Override
		public void dragExit(DropTargetEvent dte) {
			processDrag(false, null);
		}

		private void importFiles(List<File> files) {
			Path[] filePathes = new Path[files.size()];
			for (int i = 0; i < files.size(); i++) {
				filePathes[i] = files.get(i).toPath();
			}

			if(fileLimit==1 && filePathes.length==1) {
				setFile(filePathes[0]);
			} else {
				addFiles(filePathes);
			}
		}

		@Override
		public void drop(DropTargetDropEvent dtde) {

			// Process the drag event still as a "within component" type
			FileDragState fileDragState = processDrag(true, dtde);

			Transferable transferable = dtde.getTransferable();

			// Only try to finish the drag if we got green light from the preprocessing method
			if (fileDragState==FileDragState.VALID_FILES) {
				dtde.acceptDrop(dtde.getDropAction());
				try {

					@SuppressWarnings("unchecked")
					List<File> transferData = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
					importFiles(transferData);
					dtde.dropComplete(true);

				} catch (IOException | UnsupportedFlavorException e) {
					// Theoretically we shouldn't get any exceptions, since the processDrag() method checked them already
					log.error("Failed to import file list drag", e);
				}
			} else {
				dtde.rejectDrop();
			}

			// Now after drag is done reset to empty state
			processDrag(false, null);
		}
	}

	private enum FileDragState {
		/**
		 * No drag going on, show default info
		 */
		NO_DRAG,

		/**
		 * Some unexpected error outside the influence of our panel
		 * occurred during the drag operation.
		 */
		DND_ERROR,

		/**
		 * User attempting to drag something other than files here
		 */
		INVALID_FLAVOR,

		/**
		 * Everything fine, user dragged valid files of supported count
		 */
		VALID_FILES,

		/**
		 * User tried to drag an invalid file, e.g. a folder when only normal
		 * files are supported.
		 */
		INVALID_FILE,

		/**
		 * User tried to drag multiple files when only 1 file is allowed or
		 * tried to drag another file when a file has already been set.
		 */
		MULTIPLE_FILES,;
	}

	public static class DirectoryFileFilter extends FileFilter {

		/**
		 * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
		 */
		@Override
		public boolean accept(File f) {
			return f.isDirectory();
		}

		/**
		 * @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		@Override
		public String getDescription() {
			return ResourceManager.getInstance().get("replaydh.labels.directoryFileFilter");
		}

	}

	public static final FileFilter SHARED_DIRECTORY_FILE_FILTER = new DirectoryFileFilter();


	/**
	 *
	 * @author Markus Gärtner
	 *
	 */
	public static class Builder {
		private static final int DEFAULT_FILE_LIMIT = 1;
		private static final int DEFAULT_ACCEPTED_FILE_TYPE = JFileChooser.FILES_AND_DIRECTORIES;

		private Integer fileLimit;
		private FileFilter fileFilter;
		private String selectButtonLabel;
		private String dropPanelLabel;
		private Integer acceptedFileType;

		private Consumer<? super JFileChooser> fileChooserSetup;

		protected Builder() {
			// no-op
		}

		public Builder fileLimit(int fileLimit) {
			checkArgument("'fileLimit' must be positive", fileLimit>0);
			checkState("'fileLimit' already set", this.fileLimit==null);
			this.fileLimit = fileLimit==DEFAULT_FILE_LIMIT ?
					null : Integer.valueOf(fileLimit);

			return this;
		}

		public int getFileLimit() {
			return fileLimit==null ? DEFAULT_FILE_LIMIT : fileLimit.intValue();
		}

		public Builder fileFilter(FileFilter fileFilter) {
			requireNonNull(fileFilter);
			checkState("File filter already set", this.fileFilter==null);

			this.fileFilter = fileFilter;

			return this;
		}

		public FileFilter getFileFilter() {
			return fileFilter;
		}

		public Builder selectButtonLabel(String selectButtonLabel) {
			requireNonNull(selectButtonLabel);
			checkState("Select-Button label already set", this.selectButtonLabel==null);

			this.selectButtonLabel = selectButtonLabel;

			return this;
		}

		public String getSelectButtonLabel() {
			if(selectButtonLabel!=null) {
				return getSelectButtonLabel();
			}

			ResourceManager rm = ResourceManager.getInstance();

			return getFileLimit()>1 ? rm.get("replaydh.labels.add") : rm.get("replaydh.labels.select");
		}

		public Builder dropPanelLabel(String dropPanelLabel) {
			requireNonNull(dropPanelLabel);
			checkState("Drop-Panel label already set", this.dropPanelLabel==null);

			this.dropPanelLabel = dropPanelLabel;

			return this;
		}

		public String getDropPanelLabel() {
			if(dropPanelLabel!=null) {
				return dropPanelLabel;
			}

			ResourceManager rm = ResourceManager.getInstance();

			boolean multifile = getFileLimit()>1;
			boolean acceptFiles = acceptedFileType==JFileChooser.FILES_ONLY || acceptedFileType==JFileChooser.FILES_AND_DIRECTORIES;
			boolean acceptFolders = acceptedFileType==JFileChooser.DIRECTORIES_ONLY || acceptedFileType==JFileChooser.FILES_AND_DIRECTORIES;

			String files = null, folders = null;

			if(acceptFiles) {
				files = multifile ? rm.get("replaydh.hints.files") : rm.get("replaydh.hints.file");
			}

			if(acceptFolders) {
				folders = multifile ? rm.get("replaydh.hints.folders") : rm.get("replaydh.hints.folder");
			}

			String target;

			if(files==null) {
				target = folders;
			} else if(folders==null) {
				target = files;
			} else if(multifile) {
				target = rm.get("replaydh.hints.conjunction", files, folders);
			} else {
				target = rm.get("replaydh.hints.disjunction", files, folders);
			}

			return rm.get("replaydh.hints.drag", target);
		}

		public Builder acceptedFileType(int acceptedFileType) {
			checkArgument("invalid value for 'acceptedFileType'",
					acceptedFileType==JFileChooser.FILES_AND_DIRECTORIES
					|| acceptedFileType==JFileChooser.DIRECTORIES_ONLY
					|| acceptedFileType==JFileChooser.FILES_ONLY);
			checkState("'acceptedFileType' already set", this.acceptedFileType==null);
			this.acceptedFileType = acceptedFileType==DEFAULT_ACCEPTED_FILE_TYPE ?
					null : Integer.valueOf(acceptedFileType);

			return this;
		}

		public int getAcceptedFileType() {
			return acceptedFileType==null ? DEFAULT_ACCEPTED_FILE_TYPE : acceptedFileType.intValue();
		}

		public Builder fileChooserSetup(Consumer<? super JFileChooser> fileChooserSetup) {
			requireNonNull(fileChooserSetup);
			checkState("Setup routine for file chooser already set", this.fileChooserSetup==null);

			this.fileChooserSetup = fileChooserSetup;

			return this;
		}

		public Consumer<? super JFileChooser> getFileChooserSetup() {
			return fileChooserSetup;
		}

		private void validate() {
			//TODO
		}

		public FilePanel build() {
			validate();
			return new FilePanel(this);
		}
	}
}

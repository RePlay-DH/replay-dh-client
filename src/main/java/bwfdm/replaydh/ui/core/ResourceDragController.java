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
package bwfdm.replaydh.ui.core;

import static java.util.Objects.requireNonNull;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.ui.GuiUtils;

public abstract class ResourceDragController implements DropTargetListener {


	private static final Logger log = LoggerFactory.getLogger(ResourceDragController.class);

	public enum Mode {
		FILES_ONLY(true, false),
		URLS_ONLY(false, true),
		FILES_AND_URLS (true, true),
		;

		private final boolean allowFiles, allowUrls;

		private Mode(boolean allowFiles, boolean allowUrls) {
			this.allowFiles = allowFiles;
			this.allowUrls = allowUrls;
		}

		public boolean isAllowFiles() {
			return allowFiles;
		}

		public boolean isAllowUrls() {
			return allowUrls;
		}
	}

	private final Mode mode;

	/**
	 * Flag to indicate that the user is currently performing
	 * a drag action.
	 */
	private boolean dragActive = false;

	/**
	 * Flag to indicate that the client is currently processing
	 * the content of a previous drag operation in a background
	 * thread and no new drags should be accepted.
	 */
	private boolean dragProcessing = false;

	private Point dropLocation;

	public ResourceDragController(Mode mode) {
		this.mode = requireNonNull(mode);
	}

	public void install(JComponent comp) {

		comp.setDropTarget(new DropTarget(comp,
				DnDConstants.ACTION_COPY_OR_MOVE, this, true));
	}

	/**
	 * @return the dragActive
	 */
	public boolean isDragActive() {
		return dragActive;
	}

	/**
	 * @return the dragProcessing
	 */
	public boolean isDragProcessing() {
		return dragProcessing;
	}

	/**
	 * @return the dropLocation
	 */
	public Point getDropLocation() {
		return dropLocation;
	}

	protected abstract void refreshUI();

	private void refreshDrop(boolean dropActive, Point dropLocation) {
		this.dragActive = dropActive;
		this.dropLocation = dropLocation;

		refreshUI();
	}

	private boolean handleDrag(DropTargetDragEvent dtde) {
		/*
		 * Strategy:
		 *
		 * We try any valid data flavor and/or content and
		 * exit early if we found something valid.
		 * Otherwise we reject the drag at the end of this method.
		 */

		// Check if it's a file list
		if(mode.isAllowFiles() && dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			return true;
		}

		// Check if it's a URL
		if(mode.isAllowUrls() && dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			try {
				String content = (String) dtde.getTransferable().getTransferData(DataFlavor.stringFlavor);
				// Verify URL syntax
				new URL(content);
				return true;
			} catch(IOException | UnsupportedFlavorException e) { // for transferable
				// no-op
			}
		}

		// If we got here, all checks failed and we shouldn't accept whatever is being dragged
		dtde.rejectDrag();

		return false;
	}

	public void cleanupDrop() {
		dragProcessing = false;

		refreshDrop(false, null);
	}

	public Mode getMode() {
		return mode;
	}

	/**
	 * @see java.awt.dnd.DropTargetListener#dragEnter(java.awt.dnd.DropTargetDragEvent)
	 */
	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		refreshDrop(handleDrag(dtde), dtde.getLocation());
	}

	/**
	 * @see java.awt.dnd.DropTargetListener#dragOver(java.awt.dnd.DropTargetDragEvent)
	 */
	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		refreshDrop(handleDrag(dtde), dtde.getLocation());
	}

	/**
	 * @see java.awt.dnd.DropTargetListener#dropActionChanged(java.awt.dnd.DropTargetDragEvent)
	 */
	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
		refreshDrop(handleDrag(dtde), dtde.getLocation());
	}

	/**
	 * @see java.awt.dnd.DropTargetListener#dragExit(java.awt.dnd.DropTargetEvent)
	 */
	@Override
	public void dragExit(DropTargetEvent dte) {
		refreshDrop(false, null);
	}

	protected void handleFileDrag(List<Path> files) {
		// hook for subclasses
	}

	protected void handleURLDrag(URI url) {
		// hook for subclasses
	}

	/**
	 * @see java.awt.dnd.DropTargetListener#drop(java.awt.dnd.DropTargetDropEvent)
	 */
	@Override
	public void drop(DropTargetDropEvent dtde) {

		try {
			// Call needed for non-native transferable contents to be accessible
			dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

			if(mode.isAllowFiles() && dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
				try {
					@SuppressWarnings("unchecked")
					final List<File> rawFiles = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

					final List<Path> files = new ArrayList<>(rawFiles.size());
					rawFiles.forEach(f -> files.add(f.toPath()));

					handleFileDrag(files);

					dragProcessing = true;
				} catch (UnsupportedFlavorException | IOException e) {
					log.error("Failed to obtain \"fileList\" flavor from transferable", e);
					GuiUtils.showError(null, "replaydh.panels.fileCache.dragFailed");
					GuiUtils.beep();
				}
			} else if(mode.isAllowUrls() && dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				try {
					String text = (String) dtde.getTransferable().getTransferData(DataFlavor.stringFlavor);

					URI uri = new URI(text);

					handleURLDrag(uri);
				} catch (UnsupportedFlavorException | IOException e) {
					log.error("Failed to obtain \"string\" flavor from transferable", e);
					GuiUtils.showError(null, "replaydh.panels.fileCache.dragFailed");
					GuiUtils.beep();
				} catch (URISyntaxException e) {
					GuiUtils.showError(null, "replaydh.panels.fileCache.invalidURL");
					GuiUtils.beep();
				}
			}
		} finally {
			// Make sure we clean up the visuals after drag completed
			cleanupDrop();
		}
	}
}

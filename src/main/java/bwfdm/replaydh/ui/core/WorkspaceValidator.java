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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Stack;

import javax.swing.SwingWorker;

import bwfdm.replaydh.git.GitUtils;

public class WorkspaceValidator extends SwingWorker<WorkspaceValidator.WorkspaceState, Path>
			implements DirectoryStream.Filter<Path> {

		public enum WorkspaceState {

		/**
		 * Designated workspace folder does not exist, will be created.
		 */
		MISSING_FOLDER,

		/**
		 * Nothing in designated workspace folder, we're good to go
		 */
		EMPTY_FOLDER,

		/**
		 * Files in designated workspace folder, but no git repo.
		 * Only warn user.
		 */
		USED_FOLDER,

		/**
		 * Previously configured and usable RDH workspace
		 */
		RDH_REPO,

		/**
		 * Git repo found in workspace folder that has not been
		 * created by RDH-Client, so abort setup!
		 * <p>
		 * This is also used to signal inconsistent states such as
		 * more than one git directory within a folder.
		 */
		FOREIGN_REPO,
		;
	}

	public static class StateProxy {
		private WorkspaceState state;

		StateProxy(WorkspaceState state) {
			setState(state);
		}

		public void setState(WorkspaceState state) {
			this.state = requireNonNull(state);
		}

		public void swapIfGreater(WorkspaceState other) {
			if(other.compareTo(state)>0) {
				state = other;
			}
		}

		public WorkspaceState getState() {
			return state;
		}

		public boolean isAtLeast(WorkspaceState other) {
			return state.compareTo(other)>=0;
		}

		public boolean isAtMost(WorkspaceState other) {
			return state.compareTo(other)<=0;
		}
	}

		/**
		 * Main folder to validate
		 */
		private final Path root;

		/**
		 * Folder currently being processed
		 */
		private Path currentFolder;

		/**
		 * Number of sub-folders in current folder,
		 * not including potential git repositories
		 */
		private volatile int folderCount;

		/**
		 * Number of regular files and/or links in the current folder
		 */
		private volatile int fileCount;

		/**
		 * Folders that contain a git repository.
		 */
		private final Stack<Path> gitFolders = new Stack<>();

		/**
		 * Folders to be processed next
		 */
		private final Stack<Path> pendingFolders = new Stack<>();

		private final WorkspaceValidator.StateProxy state = new WorkspaceValidator.StateProxy(WorkspaceValidator.WorkspaceState.EMPTY_FOLDER);

		public WorkspaceValidator(Path root) {
			this.root = requireNonNull(root);
		}

		/**
		 * @see java.nio.file.DirectoryStream.Filter#accept(java.lang.Object)
		 */
		@Override
		public boolean accept(Path entry) throws IOException {
			boolean valid = false;

			if(Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)) {
				if(GitUtils.isGitRepository(entry)) {
					gitFolders.add(entry);
//					System.out.println("git repo: "+entry);
				} else {
					valid = true;
					folderCount++;
//					System.out.println("no git: "+entry);
				}
			} else {
				fileCount++;
			}

			return valid;
		}

		private void scanFolder(Path folder) throws IOException {
			currentFolder = folder;

			publish(folder);

			fileCount = 0;
			folderCount = 0;
			gitFolders.clear();

			try(DirectoryStream<Path> stream = Files.newDirectoryStream(folder, this)) {
				stream.forEach(pendingFolders::add);
			}
		}

		private void refreshState() {
			boolean isRoot = currentFolder==root;
			boolean hasGit = !gitFolders.isEmpty();
			boolean isEmpty = fileCount==0 && folderCount==0;

			if(isRoot) {
				if(hasGit) {
					if (gitFolders.size()==1 && GitUtils.isRDHRepository(gitFolders.peek())) {
						state.swapIfGreater(WorkspaceValidator.WorkspaceState.RDH_REPO);
					} else {
						state.swapIfGreater(WorkspaceValidator.WorkspaceState.FOREIGN_REPO);
					}
				} else if(!isEmpty) {
					state.swapIfGreater(WorkspaceValidator.WorkspaceState.USED_FOLDER);
				}
			} else if(hasGit) {
				state.swapIfGreater(WorkspaceValidator.WorkspaceState.FOREIGN_REPO);
			} else if(!isEmpty) {
				state.swapIfGreater(WorkspaceValidator.WorkspaceState.USED_FOLDER);
			}
		}

		/**
		 * @see javax.swing.SwingWorker#doInBackground()
		 */
		@Override
		protected WorkspaceValidator.WorkspaceState doInBackground() throws Exception {
			pendingFolders.add(root);
			while(!pendingFolders.isEmpty()) {
				if(Thread.interrupted())
					throw new InterruptedException();
				// Now add all remaining subfolders, scan for git directories and count files
				scanFolder(pendingFolders.pop());

				refreshState();

				// Now check if we can already stop scanning
				if(state.isAtLeast(WorkspaceValidator.WorkspaceState.FOREIGN_REPO)) {
					break;
				}
			}

			return state.getState();
		}

		/**
		 * @return the currentFolder
		 */
		public Path getCurrentFolder() {
			return currentFolder;
		}

		public WorkspaceValidator.WorkspaceState getWorkspaceState() {
			return state.getState();
		}
	}

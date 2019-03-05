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
package bwfdm.replaydh.workflow.git;

import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.GitCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.TransportCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.RDHClient;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.git.GitException;
import bwfdm.replaydh.git.JGitAdapter;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.AbstractWizardStep;
import bwfdm.replaydh.ui.icons.IconRegistry;

/**
 * @author Markus Gärtner
 *
 */
public abstract class GitRemoteWizard {


	private static final Logger log = LoggerFactory.getLogger(GitRemoteWizard.class);


	/**
	 * Context for the wizard
	 */
	public static class GitRemoteContext<T> implements AutoCloseable {
		public final Git git;

		/** Actual URL for the remote if provided by user */
		public URIish remoteUrl;

		/** Registered config for the remote if selected by user */
		public RemoteConfig remoteConfig;

		/** Label for registering the remote */
		public String refName;

		/** The last message created while performing the remote operation */
		public String finalMessage;

		/** If invoking the remote operation failed store the exception here */
		public Throwable error;

		/** Raw result of calling the remote operation */
		public T result;

		/** Store credentials provider here so that we can properly discard its data afterwards */
		public UsernamePasswordCredentialsProvider credentials;

		public GitRemoteContext(Git git) {
			this.git = requireNonNull(git);
		}

		public GitRemoteContext(RDHEnvironment environment) {
			this.git = ((JGitAdapter)environment.getClient().getFileTracker()).getGit();
		}

		public String getRemote() {
			String url = null;
			if(remoteUrl!=null) {
				url = remoteUrl.toString();
			}
			if(url==null && remoteConfig!=null) {
				url = remoteConfig.getName();
			}
			return url;
		}

		/**
		 * @see java.lang.AutoCloseable#close()
		 */
		@Override
		public void close() {
			if(credentials!=null) {
				credentials.clear();
				credentials = null;
			}
		}
	}

	protected static URIish getUsablePushUri(RemoteConfig config) {
		List<URIish> pushUris = config.getPushURIs();
		if(!pushUris.isEmpty()) {
			return pushUris.get(0);
		}

		List<URIish> rawUris = config.getURIs();
		if(!rawUris.isEmpty()) {
			return rawUris.get(0);
		}

		return null;
	}

	/**
	 * Abstract class for the wizard page
	 */
	protected static abstract class GitRemoteStep<T, C extends GitRemoteContext<T>> extends AbstractWizardStep<C> {
		protected GitRemoteStep(String id, String titleKey, String descriptionKey) {
			super(id, titleKey, descriptionKey);
		}
	}

	/**
	 * Marker string to signal that the user should be allowed to enter
	 * a new repository URL.
	 */
	protected static final String CHOOSE_NEW_REPO = "";

	/**
	 * First step: Let user provide or select a remote repository URL
	 */
	protected abstract static class ChooseRemoteStep<T, C extends GitRemoteContext<T>> extends GitRemoteStep<T,C> {

		private static String DEFAULT_HEADER_SECTION_KEY = "replaydh.wizard.gitRemote.chooseRemote.header";
		private static String DEFAULT_MIDDLE_SECTION_KEY = "replaydh.wizard.gitRemote.chooseRemote.middle";
		private static String DEFAULT_FOOTER_SECTION_KEY = "replaydh.wizard.gitRemote.chooseRemote.footer";

		public ChooseRemoteStep(String id, String titleKey, String descriptionKey,
				String headerSectionKey, String middleSectionKey, String footerSectionKey) {
			super(id, titleKey, descriptionKey);

			this.headerSectionKey = headerSectionKey!=null ? headerSectionKey : DEFAULT_HEADER_SECTION_KEY;
			this.middleSectionKey = middleSectionKey!=null ? middleSectionKey : DEFAULT_MIDDLE_SECTION_KEY;
			this.footerSectionKey = footerSectionKey!=null ? footerSectionKey : DEFAULT_FOOTER_SECTION_KEY;
		}

		private final String headerSectionKey;
		private final String middleSectionKey;
		private final String footerSectionKey;

		private JComboBox<Object> cbRemote;
		private JTextField tfRemoteName;

		private final Matcher legalRemoteNameMatcher = Pattern.compile("[a-zA-Z]+").matcher("");

		@Override
		protected JPanel createPanel() {

			ResourceManager rm = ResourceManager.getInstance();

			cbRemote = new JComboBox<>();
			cbRemote.addActionListener(this::onRemoteAction);
			cbRemote.setRenderer(new RemoteListeCellRenderer());
			GuiUtils.prepareChangeableBorder(cbRemote);

			tfRemoteName = new JTextField(15);
			/*
			 *  https://stackoverflow.com/questions/41461152/which-characters-are-illegal-within-a-git-remote-name
			 *  See answer #3:
			 *  Maybe we need to consider additional support and/or hints to ensure users can't
			 *  accidentally create ref names that might become ambiguous when considering future
			 *  abbreviations of git hashes.
			 */
//			tfRemoteName.setToolTipText(rm.get("replaydh.wizard.gitRemote.chooseRemote.remoteNameHints")); //TODO enable later
			GuiUtils.addErrorFeedback(tfRemoteName, this::checkLegalRemoteName);

			return FormBuilder.create()
					.columns("pref, 4dlu, fill:pref:grow")
					.rows("pref, 6dlu, pref, 6dlu, pref, 6dlu, pref, 6dlu, pref")
					.add(GuiUtils.createTextArea(rm.get(headerSectionKey))).xyw(1, 1, 3)

					.addLabel(rm.get("replaydh.wizard.gitRemote.chooseRemote.location")+":").xy(1, 3)
					.add(cbRemote).xy(3, 3)
					.add(GuiUtils.createTextArea(rm.get(middleSectionKey))).xyw(1, 5, 3)

					.addLabel(rm.get("replaydh.wizard.gitRemote.chooseRemote.remoteName")+":").xy(1, 7)
					.add(tfRemoteName).xy(3, 7, "left, center")
					.add(GuiUtils.createTextArea(rm.get(footerSectionKey))).xyw(1, 9, 3)

					.build();
		}

		private boolean checkLegalRemoteName(String name) {
			// Empty or unset name means we don't need to save the remote
			if(name==null || name.isEmpty()) {
				return true;
			}

			// Syntax check
			if(!legalRemoteNameMatcher.reset(name).matches()) {
				return false;
			}

			boolean result = true;

			// Make sure we don't allow duplicates
			//TODO problematic: we need access to the Git instance here
			Git git = ((JGitAdapter)RDHClient.client().getFileTracker()).getGit();
			Repository repository = git.getRepository();
			try {
				for(RemoteConfig config : RemoteConfig.getAllRemoteConfigs(repository.getConfig())) {
					if(config.getName().equals(name)) {
						result = false;
						break;
					}
				}
			} catch (URISyntaxException e) {
				log.error("Unable to access list of remote configs for current repository.", e);
				GuiUtils.beep();
				GuiUtils.showErrorDialog(panel(), e);

				result = false;
			}

			return result;
		}

		@Override
		public void refresh(RDHEnvironment environment, C context) {

			cbRemote.removeAllItems();
			cbRemote.addItem(CHOOSE_NEW_REPO);

			if(context.remoteUrl!=null) {
				cbRemote.addItem(context.remoteUrl);
			}

			Repository repo = context.git.getRepository();

			try {
				RemoteConfig.getAllRemoteConfigs(repo.getConfig()).forEach(cbRemote::addItem);
			} catch (URISyntaxException e) {
				log.error("Failed to read existing remote entries", e);
				GuiUtils.showErrorDialog(getPageComponent(), e);
			}

			refreshRemoteEditable();
			refreshRemoteNameEditable();
			refreshNextEnabled();
		};

		private void onRemoteAction(ActionEvent ae) {

			refreshRemoteEditable();
			refreshRemoteNameEditable();
			refreshNextEnabled();
		}

		private void refreshRemoteNameEditable() {
			Object item = cbRemote.getSelectedItem();
			tfRemoteName.setEditable(!(item instanceof RemoteConfig));
		}

		private void refreshRemoteEditable() {

			Object item = cbRemote.getSelectedItem();
			cbRemote.setEditable(item==null || item instanceof String);
		}

		private void refreshNextEnabled() {
			Object item = cbRemote.getSelectedItem();
			boolean nextEnabled = false;

			if(item instanceof RemoteConfig) {
				// Directly selecting a registered remote is always valid
				nextEnabled = true;
			} else if(item instanceof String) {
				String s = (String) item;

				if(!s.isEmpty() && !CHOOSE_NEW_REPO.equals(s)) {
					try {
						// Kind of ugly, but easiest way of determining validity of URI
						new URIish(s);
						nextEnabled = true;
					} catch(URISyntaxException e) {
						// ignore
					}
				}
			}

			// If user is allowed to edit remote name we need to check its validity as well
			if(tfRemoteName.isEditable()) {
				nextEnabled &= !GuiUtils.isErrorBorderActive(tfRemoteName);
			}

			setNextEnabled(nextEnabled);
		}

		@Override
		public void cancel(RDHEnvironment environment, C context) {
			context.remoteConfig = null;
			context.remoteUrl = null;
		};

		/**
		 * Default processing of the current page. Will return {@code true} if no errors
		 * were encountered and the wizard can continue.
		 *
		 * @param environment
		 * @param context
		 * @return
		 */
		protected boolean defaultProcessNext(RDHEnvironment environment,
				C context) {

			// Takes care of resetting the 2 fields for remote locations
			cancel(environment, context);

			Object item = cbRemote.getSelectedItem();

			if(item instanceof RemoteConfig) {
				context.remoteConfig = (RemoteConfig) item;
			} else if(item instanceof String) {
				try {
					context.remoteUrl = new URIish((String) item);
				} catch (URISyntaxException e) {
					log.error("Invalid remote url: {}", item, e);
					GuiUtils.showErrorDialog(panel(), e);
					return false;
				}

				String remoteName = tfRemoteName.getText();
				// Additional sanity check
				if(remoteName!=null && !remoteName.isEmpty()
						&& checkLegalRemoteName(remoteName)) {
					RemoteAddCommand addCommand = context.git.remoteAdd();
					addCommand.setName(remoteName);
					addCommand.setUri(context.remoteUrl);

					// Save and directly use new remote config
					try {
						context.remoteConfig = addCommand.call();
					} catch (GitAPIException e) {
						log.error("Failed to add new remote config", e);
						GuiUtils.showErrorDialog(panel, null,
								"replaydh.wizard.gitRemote.chooseRemote.addRemoteFailed", e);
						// Failing to store the URL doesn't have to stop us from pushing
					}
				}
			}

			return true;
		}
	}

	/**
	 *
	 */
	protected static abstract class PerformOperationStep<T, G extends GitCommand<T>, C extends GitRemoteContext<T>>
			extends GitRemoteStep<T,C> {

		public PerformOperationStep(String id, String titleKey, String descriptionKey) {
			super(id, titleKey, descriptionKey);
		}

		private JButton bTransmit;
		private JTextArea taHeader;
		private JTextArea taInfo;

		private GitWorker<T, G, C> worker;

		@Override
		protected JPanel createPanel() {
			bTransmit = new JButton();
			bTransmit.addActionListener(this::onTransmitButtonClicked);

			taHeader = GuiUtils.createTextArea(null);
			taInfo = GuiUtils.createTextArea(null);

			return FormBuilder.create()
					.columns("fill:pref:grow, pref, fill:pref:grow")
					.rows("pref, 6dlu, pref, 6dlu, pref")
					.add(taHeader).xyw(1, 1, 3)
					.add(bTransmit).xy(2, 3)
					.add(taInfo).xyw(1, 5, 3)
					.build();
		}

		private void onTransmitButtonClicked(ActionEvent ae) {
			// If worker is pending we need to start it
			if(worker.getState()==StateValue.PENDING) {
				bTransmit.setText(ResourceManager.getInstance().get("replaydh.labels.cancel"));
				worker.execute();
				refreshPreviousEnabled();
			} else {
				// Otherwise it's a cancel request
				worker.cancel(false); //TODO for now we don't want to interrupt the worker thread
			}

			GuiUtils.invokeEDTLater(this::refreshButton);
		}

		private void refreshButton() {
			ResourceManager rm = ResourceManager.getInstance();
			IconRegistry ir = IconRegistry.getGlobalRegistry();

			if(workerRunning()) {
				bTransmit.setText(rm.get("replaydh.labels.cancel"));
				bTransmit.setToolTipText(GuiUtils.toSwingTooltip(
						rm.get("replaydh.wizard.gitRemote.execute.cancelTooltip")));
				bTransmit.setIcon(ir.getIcon("loading-16.gif"));
			} else {
				bTransmit.setText(rm.get("replaydh.labels.start"));
				bTransmit.setToolTipText(GuiUtils.toSwingTooltip(
						rm.get("replaydh.wizard.gitRemote.execute.startTooltip")));
				bTransmit.setIcon(null);
			}
		}

		/**
		 * Allow to continue if worker has been constructed and completed (either by
		 * finishing normally, being terminated or getting cancelled).
		 */
		private void refreshNextEnabled() {
			setNextEnabled(worker!=null && worker.isDone());
		}

		/**
		 * Allow to backtrack if worker couldn't be created, got cancelled or
		 * has never been started at all.
		 */
		private void refreshPreviousEnabled() {
			setPreviousEnabled(worker==null || worker.isCancelled() || worker.getState()==StateValue.PENDING);
		}

		private boolean workerRunning() {
			return worker!=null && !worker.isDone() && worker.getState()!=StateValue.PENDING;
		}

		protected abstract G createGitCommand(C context) throws GitException, URISyntaxException;

		protected abstract GitWorker<T, G, C> createWorker(RDHEnvironment environment, G command, C context);

		@Override
		public void refresh(RDHEnvironment environment, C context) {
			checkState("Worker for push operation already set", worker==null);

			ResourceManager rm = ResourceManager.getInstance();

			G command = null;
			try {
				command = createGitCommand(context);
			} catch (GitException | URISyntaxException e) {
				log.error("Failed to prepare git command", e);
				context.error = e;
			}

			if(command!=null) {
				worker = createWorker(environment, command, context);
			}

			bTransmit.setText(rm.get("replaydh.labels.start"));
			bTransmit.setEnabled(worker!=null);

			if(worker!=null) {
				taInfo.setText(rm.get("replaydh.wizard.gitRemote.execute.workerStartInfo"));
			} else {
				taInfo.setText(rm.get("replaydh.wizard.gitRemote.execute.workerConstructionFailed"));
			}

			refreshNextEnabled();
			refreshPreviousEnabled();
			refreshButton();
		};

		@Override
		public void cancel(RDHEnvironment environment, C context) {
			// We don't allow backtracking while worker is still active, so it's safe to cleanup here
			worker = null;
		};

		@Override
		public boolean close() {
			boolean workerAliveAndRunning = workerRunning();

			worker = null;

			return workerAliveAndRunning;
		};
	};

	protected static abstract class GitWorker<T, G extends GitCommand<T>, C extends GitRemoteContext<T>>
			extends SwingWorker<T, String> {
		private final PerformOperationStep<T, G, C> step;
		protected final C context;

		protected GitWorker(PerformOperationStep<T, G, C> step, C context) {
			this.step = step;
			this.context = context;
		}

		@Override
		protected final void done() {
			ResourceManager rm = ResourceManager.getInstance();

			try {
				context.result = get();
			} catch (InterruptedException | CancellationException e) {
				// Assumed to be user-originated cancellation
			} catch (ExecutionException e) {
				// Unwrap real error
				context.error = e.getCause();

				log.error("Failed to push to remote git: {}", e.getCause());
			}

			// Store last displayed info
			context.finalMessage = step.taInfo.getText();

			step.taInfo.setText(rm.get("replaydh.wizard.gitRemote.execute.workerFinished"));

			step.refreshNextEnabled();
			step.refreshPreviousEnabled();

			step.bTransmit.setEnabled(false);
			step.bTransmit.setIcon(null);
			step.bTransmit.setText("-");
			step.bTransmit.setToolTipText(null);
		};

		@Override
		protected void process(List<String> chunks) {
			if(!chunks.isEmpty()) {
				step.taInfo.setText(chunks.get(chunks.size()-1));
			}
		};
	}

	protected static class GitMonitoringWorker<T, G extends TransportCommand<G, T>, C extends GitRemoteContext<T>>
			extends GitWorker<T, G, C> implements ProgressMonitor {


		/** total number of tasks */
		private int totalTasks = -1;
		/** total number of expected steps in current task */
		private int totalWork = -1;
		/** total number of finished steps in current task */
		private int currentWork = -1;

		private final List<String> tasks = new ArrayList<>();

		private String task;

		private final G command;

		public GitMonitoringWorker(PerformOperationStep<T, G, C> step, C context, G command) {
			super(step, context);
			this.command = requireNonNull(command);
		}

		/**
		 * @see org.eclipse.jgit.lib.ProgressMonitor#start(int)
		 */
		@Override
		public void start(int totalTasks) {
			this.totalTasks = totalTasks;

			updateAndReportProgress();
		}

		/**
		 * @see org.eclipse.jgit.lib.ProgressMonitor#beginTask(java.lang.String, int)
		 */
		@Override
		public void beginTask(String title, int totalWork) {
			task = title;
			this.totalWork = totalWork;
			currentWork = 0;

			updateAndReportProgress();
		}

		/**
		 * @see org.eclipse.jgit.lib.ProgressMonitor#update(int)
		 */
		@Override
		public void update(int completed) {
			currentWork += completed;

			updateAndReportProgress();
		}

		/**
		 * @see org.eclipse.jgit.lib.ProgressMonitor#endTask()
		 */
		@Override
		public void endTask() {
			totalWork = -1;
			currentWork = -1;
			tasks.add(task);
			task = null;

			updateAndReportProgress();
		}

		private void updateAndReportProgress() {
			StringBuilder sb = new StringBuilder();

			ResourceManager rm = ResourceManager.getInstance();

			if(totalTasks==-1 && tasks.isEmpty() && task==null) {
				sb.append("-");
			} else {
				int taskCount = tasks.size();
				if(task!=null) {
					taskCount++;
				}

				sb.append(rm.get("replaydh.wizard.gitRemote.execute.tasks",
						taskCount, totalTasks==-1 ? "?" : totalTasks));

				for(String task : tasks) {
					sb.append("\r\n").append(task);
				}

				if(task!=null) {
					sb.append("\r\n").append(rm.get("replaydh.wizard.gitRemote.execute.taskActive",
							task, currentWork+1, totalWork==0 ? "?" : totalWork));
				}
			}

			publish(sb.toString());
		}

		protected void attachProgressMonitor() {
			try {
				command.getClass().getMethod("setProgressMonitor", ProgressMonitor.class).invoke(command, this);
			} catch (IllegalAccessException | IllegalArgumentException | SecurityException e) {
				log.error("Unable to access method to attach progress monitor", e);
			} catch (InvocationTargetException e) {
				log.error("Unexpected error while attaching progress monitor", e.getCause());
			} catch (NoSuchMethodException e) {
				log.error("Command is missing method to attach progress monitor: {}", command.getClass(), e);
			}
		}

		/**
		 * @see javax.swing.SwingWorker#doInBackground()
		 */
		@Override
		protected T doInBackground() throws Exception {
			attachProgressMonitor();

			return command.call();
		}
	}

	private static class RemoteListeCellRenderer extends DefaultListCellRenderer {

		private static final long serialVersionUID = -4653383382806680013L;

		/**
		 * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
		 */
		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {

			String tooltip = null;

			// Entries are either Strings or RemoteConfig instances
			if(value instanceof RemoteConfig) {
				RemoteConfig config = (RemoteConfig) value;
				URIish uri = getUsablePushUri(config);
				if(uri!=null) {
					value = String.format("%s - %s", config.getName(), uri.toString());
					tooltip = uri.toString();
				}
			} else if(CHOOSE_NEW_REPO.equals(value)) {
				value = ResourceManager.getInstance().get("replaydh.wizard.gitRemote.chooseRemote.addNewUrl");
			} else if(value!=null) {
				tooltip = value.toString();
			}

			// Let the default implementation do the real work
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			setToolTipText(GuiUtils.toSwingTooltip(tooltip));

			return this;
		}
	};


}

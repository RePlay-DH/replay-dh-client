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
package bwfdm.replaydh.git;

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
import java.util.function.Consumer;
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

		/** Flag to signal the command finished without 'error' but may still have failed */
		public boolean commandCompleted;

		/** The scope on which the operation should take effect */
		public Scope scope;

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
	 * Let user provide or select a remote repository URL
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

	public enum Scope {
		/**
		 * Only consider changes related to the current workspace (branch)
		 */
		WORKSPACE("workspace"),
		/**
		 * Push or fetch everything for the entire workflow (repository)
		 */
		WORKFLOW("workflow"),
		;
		private final String key;

		private Scope(String key) {
			this.key = "replaydh.wizard.gitRemote.scope."+key;
		}

		/**
		 * @see java.lang.Enum#toString()
		 */
		@Override
		public String toString() {
			return ResourceManager.getInstance().get(key);
		}
	}

	/**
	 * Let user provide or select a remote repository URL
	 */
	protected abstract static class SelectScopeStep<T, C extends GitRemoteContext<T>> extends GitRemoteStep<T,C> {

		private static String DEFAULT_HEADER_SECTION_KEY = "replaydh.wizard.gitRemote.selectScope.header";
		private static String DEFAULT_WORKSPACE_SCOPE_KEY = "replaydh.wizard.gitRemote.selectScope.workspaceScope";
		private static String DEFAULT_WORKFLOW_SCOPE_KEY = "replaydh.wizard.gitRemote.selectScope.workflowScope";

		private static final Scope DEFAULT_SCOPE = Scope.WORKSPACE;

		public SelectScopeStep(String id, String titleKey, String descriptionKey,
				String headerSectionKey, String workspaceScopeKey, String workflowScopeKey) {
			super(id, titleKey, descriptionKey);

			this.headerSectionKey = headerSectionKey!=null ? headerSectionKey : DEFAULT_HEADER_SECTION_KEY;
			this.workspaceScopeKey = workspaceScopeKey!=null ? workspaceScopeKey : DEFAULT_WORKSPACE_SCOPE_KEY;
			this.workflowScopeKey = workflowScopeKey!=null ? workflowScopeKey : DEFAULT_WORKFLOW_SCOPE_KEY;
		}

		private final String headerSectionKey;
		private final String workspaceScopeKey;
		private final String workflowScopeKey;

		private JComboBox<Scope> cbScope;
		private JTextArea taInfo;

		@Override
		protected JPanel createPanel() {

			ResourceManager rm = ResourceManager.getInstance();

			cbScope = new JComboBox<>(Scope.values());
			cbScope.addActionListener(ae -> onScopeSelected());

			taInfo = GuiUtils.createTextArea("");

			return FormBuilder.create()
					.columns("pref, 4dlu, fill:pref:grow")
					.rows("pref, 6dlu, pref, 6dlu, pref")
					.add(GuiUtils.createTextArea(rm.get(headerSectionKey))).xyw(1, 1, 3)

					.addLabel(rm.get("replaydh.wizard.gitRemote.scope.label")+":").xy(1, 3)
					.add(cbScope).xy(3, 3)
					.add(taInfo).xyw(1, 5, 3)

					.build();
		}

		@Override
		public void refresh(RDHEnvironment environment, C context) {
			if(context.remoteConfig==null) {
				cbScope.setEnabled(false);
				taInfo.setText(ResourceManager.getInstance().get("replaydh.wizard.gitRemote.selectScope.noRemote"));
			} else {
				cbScope.setEnabled(true);

				Scope scope = context.scope;
				if(scope==null) {
					scope = DEFAULT_SCOPE;
				}
				cbScope.setSelectedItem(scope);
			}
		};

		private void onScopeSelected() {
			Scope scope = (Scope) cbScope.getSelectedItem();

			String infoKey;

			switch (scope) {
			case WORKSPACE:
				infoKey = workspaceScopeKey;
				break;

			case WORKFLOW:
				infoKey = workflowScopeKey;
				break;

			default:
				throw new IllegalStateException("Unknown scope: "+scope);
			}

			taInfo.setText(ResourceManager.getInstance().get(infoKey));
		}

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

			Scope scope = (Scope)cbScope.getSelectedItem();

			// Make sure we don't use the selection if situation prohibits WORKSPACE
			if(!cbScope.isEnabled()) {
				scope = null;
			}

			context.scope = scope;

			return true;
		}
	}

	/**
	 * Provide an interface for executing an arbitrary {@link GitCommand}
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
			} else {
				// Otherwise it's a cancel request
				worker.cancel(false); //TODO for now we don't want to interrupt the worker thread
			}

			GuiUtils.invokeEDTLater(() -> {
				refreshButton();
				refreshPreviousEnabled();
			});
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

		protected GitWorker<T, G, C> createWorker(RDHEnvironment environment, C context) {
			return defaultCreateWorker(environment, context);
		}

		protected final GitWorker<T, G, C> defaultCreateWorker(RDHEnvironment environment, C context) {
			return new GitWorker<>(environment, context, this::createGitCommand,
					this::handleWorkerResult, this::handleWorkerChunks);
		}

		protected abstract G createGitCommand(GitWorker<T, G, C> worker) throws GitException;

		protected void handleWorkerChunks(List<String> chunks) {
			if(!chunks.isEmpty()) {
				taInfo.setText(chunks.get(chunks.size()-1));
			}
		}

		protected void handleWorkerResult(GitWorker<T, G, C> worker) {
			ResourceManager rm = ResourceManager.getInstance();

			// Store last displayed info
			worker.context.finalMessage = taInfo.getText();

			taInfo.setText(rm.get("replaydh.wizard.gitRemote.execute.workerFinished"));

			refreshNextEnabled();
			refreshPreviousEnabled();

			bTransmit.setEnabled(false);
			bTransmit.setIcon(null);
			bTransmit.setText("-");
			bTransmit.setToolTipText(null);
		}

		@Override
		public void refresh(RDHEnvironment environment, C context) {
			checkState("Worker for push operation already set", worker==null);

			ResourceManager rm = ResourceManager.getInstance();

			worker = createWorker(environment, context);

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

	@FunctionalInterface
	public interface CommandGenerator<T, G extends GitCommand<T>, C extends GitRemoteContext<T>> {
		G createCommand(GitWorker<T, G, C> worker) throws GitException;
	}

	@FunctionalInterface
	public interface ResultHandler<T, G extends GitCommand<T>, C extends GitRemoteContext<T>> {
		void handleResult(GitWorker<T, G, C> worker);
	}

	public static class GitWorker<T, G extends GitCommand<T>, C extends GitRemoteContext<T>>
			extends SwingWorker<T, String>  implements ProgressMonitor {

		protected final CommandGenerator<T, G, C> commandGen;
		protected final ResultHandler<T, G, C> resultHandler;
		protected final Consumer<List<String>> chunkHandler;
		public final RDHEnvironment environment;
		public final C context;

		private boolean doMonitor = true;

		protected GitWorker(
				RDHEnvironment environment,
				C context,
				CommandGenerator<T, G, C> commandGen,
				ResultHandler<T, G, C> resultHandler,
				Consumer<List<String>> chunkHandler) {
			this.environment = requireNonNull(environment);
			this.context = requireNonNull(context);
			this.commandGen = requireNonNull(commandGen);
			this.resultHandler = requireNonNull(resultHandler);
			this.chunkHandler = chunkHandler;
		}

		public GitWorker<T, G, C> monitor(boolean doMonitor) {
			this.doMonitor = doMonitor;
			return this;
		}

		@Override
		protected T doInBackground() throws Exception {
			G command = commandGen.createCommand(this);

			if(command==null)
				throw new IllegalStateException("COnstruction of Git command failed");

			if(doMonitor) {
				attachProgressMonitor(command);
			}

			return command.call();
		}

		protected void attachProgressMonitor(G command) {
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

		@Override
		protected final void done() {
			try {
				context.result = get();
				context.commandCompleted = true;
			} catch (InterruptedException | CancellationException e) {
				/*
				 *  Assumed to be user-originated cancellation.
				 *  This should also shadow the CanceledException from the
				 *  Git command. If that ever surfaces, we need to handle it
				 *  here or wrap it earlier.
				 */

			} catch (ExecutionException e) {
				// Unwrap real error
				context.error = e.getCause();

				log.error("Failed executing command for remote git: {}",
						context.getRemote(), e.getCause());
			}

			resultHandler.handleResult(this);
		};

		@Override
		protected void process(List<String> chunks) {
			if(chunkHandler!=null) {
				chunkHandler.accept(chunks);
			}
		};


		// PROGRESS MONITOR STUFF

		/** total number of tasks */
		private int totalTasks = -1;
		/** total number of expected steps in current task */
		private int totalWork = -1;
		/** total number of finished steps in current task */
		private int currentWork = -1;

		private final List<String> tasks = new ArrayList<>();

		private String task;

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

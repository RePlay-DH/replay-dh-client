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
package bwfdm.replaydh.workflow.export.git;

import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.git.GitException;
import bwfdm.replaydh.git.GitUtils;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.helper.AbstractWizardStep;
import bwfdm.replaydh.ui.helper.Wizard;
import bwfdm.replaydh.ui.helper.Wizard.Page;
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.ui.icons.Resolution;
import bwfdm.replaydh.workflow.export.WorkflowExportInfo;

/**
 * @author Markus Gärtner
 *
 */
public class GitRemotePublisherWizard {


	private static final Logger log = LoggerFactory.getLogger(GitRemotePublisherWizard.class);

	public static Wizard<GitRemotePublisherContext> getWizard(Window parent, RDHEnvironment environment) {
		@SuppressWarnings("unchecked")
		Wizard<GitRemotePublisherContext> wizard = new Wizard<>(
				parent, "gitRemotePublisherWizard",
				ResourceManager.getInstance().get("replaydh.wizard.gitRemotePublisher.title"),
				environment,
				CHOOSE_REMOTE, EXPORT, FINISH);
		return wizard;
	}

	/**
	 * Context for the wizard
	 */
	public static final class GitRemotePublisherContext {

		final WorkflowExportInfo exportInfo;
		final Git git;

		RDHEnvironment environment;

		/** Actual URL for the remote if provided by user */
		String remoteUrl;

		/** Registered config for the remote if selected by user */
		RemoteConfig remoteConfig;

		/** Store remote for repeated use (will ask user for label later) */
		boolean storeRepository;

		/** Label for registering the remote */
		String refName;

		/** Raw results of calling the {@link PushCommand} */
		Iterable<PushResult> results;

		/** The last message created while calling the {@link PushCommand} */
		String finalMessage;

		/** If invoking the {@link PushCommand} store the exception here */
		Throwable error;

		/** Store credentials provider here so that we can properly discard its data afterwards */
		UsernamePasswordCredentialsProvider credentials;

		public GitRemotePublisherContext(WorkflowExportInfo exportInfo, Git git) {
			this.exportInfo = requireNonNull(exportInfo);
			this.git = requireNonNull(git);
		}

		public String getRemote() {
			String url = remoteUrl;
			if(url==null && remoteConfig!=null) {
				url = remoteConfig.getName();
			}
			return url;
		}
	}

	private static URIish getUsablePushUri(RemoteConfig config) {
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
	private static abstract class GitRemotePublisherStep extends AbstractWizardStep<GitRemotePublisherContext> {
		protected GitRemotePublisherStep(String id, String titleKey, String descriptionKey) {
			super(id, titleKey, descriptionKey);
		}
	}

	/**
	 * First step: Let user provide or select a remote repository URL
	 */
	private static final GitRemotePublisherStep CHOOSE_REMOTE = new GitRemotePublisherStep(
			"chooseRemote",
			"replaydh.wizard.gitRemotePublisher.chooseRemote.title",
			"replaydh.wizard.gitRemotePublisher.chooseRemote.description") {
		private JComboBox<Object> cbRemote;

		@Override
		protected JPanel createPanel() {

			ResourceManager rm = ResourceManager.getInstance();

			cbRemote = new JComboBox<>();
			cbRemote.addActionListener(this::onRemoteAction);
			cbRemote.setRenderer(new RemoteListeCellRenderer());

			GuiUtils.prepareChangeableBorder(cbRemote);

			return FormBuilder.create()
					.columns("pref, 4dlu, fill:pref:grow")
					.rows("pref, 6dlu, pref, 6dlu, pref")
					.add(GuiUtils.createTextArea(rm.get("replaydh.wizard.gitRemotePublisher.chooseRemote.message"))).xyw(1, 1, 3)
					.addLabel(rm.get("replaydh.wizard.gitRemotePublisher.chooseRemote.location")+":").xy(1, 3)
					.add(cbRemote).xy(3, 3)
					.add(GuiUtils.createTextArea(rm.get("replaydh.wizard.gitRemotePublisher.chooseRemote.message2"))).xyw(1, 5, 3)
					.build();
		}

		@Override
		public void refresh(RDHEnvironment environment, GitRemotePublisherContext context) {

			cbRemote.removeAllItems();
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
			refreshNextEnabled();
		};

		private void onRemoteAction(ActionEvent ae) {
			refreshRemoteEditable();
			refreshNextEnabled();
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

				if(!s.isEmpty()) {
					try {
						// Kind of ugly, but easiest way of determining validity of URI
						new URIish(s);
						nextEnabled = true;
					} catch(URISyntaxException e) {
						// ignore
					}
				}
			}

			setNextEnabled(nextEnabled);
		}

		@Override
		public void cancel(RDHEnvironment environment, GitRemotePublisherContext context) {
			context.remoteConfig = null;
			context.remoteUrl = null;
		};

		@Override
		public Page<GitRemotePublisherContext> next(RDHEnvironment environment,
				GitRemotePublisherContext context) {

			// Takes care of resetting the 2 fields for remote locations
			cancel(environment, context);

			Object item = cbRemote.getSelectedItem();

			if(item instanceof RemoteConfig) {
				context.remoteConfig = (RemoteConfig) item;
			} else if(item instanceof String) {
				context.remoteUrl = (String) item;
			}

			return EXPORT;
		}
	};

	/**
	 *
	 */
	private static final GitRemotePublisherStep EXPORT = new GitRemotePublisherStep(
			"export",
			"replaydh.wizard.gitRemotePublisher.export.title",
			"replaydh.wizard.gitRemotePublisher.export.description") {

		private JButton bTransmit;
		private JTextArea taHeader;
		private JTextArea taInfo;

		private PushWorker worker;

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
				bTransmit.setToolTipText(rm.get("replaydh.wizard.gitRemotePublisher.export.cancelTooltip"));
				bTransmit.setIcon(ir.getIcon("loading-64.gif", Resolution.forSize(24)));
			} else {
				bTransmit.setText(rm.get("replaydh.labels.start"));
				bTransmit.setToolTipText(rm.get("replaydh.wizard.gitRemotePublisher.export.pushTooltip"));
				bTransmit.setIcon(null);
			}
		}

		/**
		 * Allow to continue if worker has been constructed and completed (either by
		 * finishing normally, being terinated or getting cancelled).
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

		private PushCommand createPushCommand(GitRemotePublisherContext context) throws GitException, URISyntaxException {

			PushCommand command = context.git.push();

			URIish remoteUri = null;
			String remoteName = null;

			if(context.remoteConfig!=null) {
				remoteName = context.remoteConfig.getName();
				remoteUri = getUsablePushUri(context.remoteConfig);
				command.setRemote(remoteName);
			} else {
				command.setRemote(context.remoteUrl);
				remoteUri = new URIish(context.remoteUrl);
			}

			GitUtils.prepareTransportCredentials(command, remoteUri, remoteName,
					(username, password) -> {
						context.credentials = new UsernamePasswordCredentialsProvider(username, password);
						return context.credentials;
					});

			command.setPushAll();
			command.setPushTags();
			command.setAtomic(true);

			return command;
		}

		@Override
		public void refresh(RDHEnvironment environment, GitRemotePublisherContext context) {
			checkState("Worker for push operation already set", worker==null);

			ResourceManager rm = ResourceManager.getInstance();

			PushCommand command = null;
			try {
				command = createPushCommand(context);
			} catch (GitException | URISyntaxException e) {
				log.error("Failed to prepare push command", e);
				context.error = e;
			}

			if(command!=null) {
				worker = new PushWorker(command) {
					@Override
					protected void done() {

						try {
							context.results = get();
						} catch (InterruptedException | CancellationException e) {
							// Assumed to be user-originated cancellation
						} catch (ExecutionException e) {
							// Unwrap real error
							context.error = e.getCause();

							log.error("Failed to push to remote git: {}", e.getCause());
						}

						context.finalMessage = taInfo.getText();

						taInfo.setText(ResourceManager.getInstance().get(
								"replaydh.wizard.gitRemotePublisher.export.workerFinished"));

						refreshNextEnabled();
						refreshPreviousEnabled();

						bTransmit.setEnabled(false);
					};
					@Override
					protected void process(List<String> chunks) {
						if(!chunks.isEmpty()) {
							taInfo.setText(chunks.get(chunks.size()-1));
						}
					};
				};
			}

			bTransmit.setText(rm.get("replaydh.labels.start"));
			bTransmit.setEnabled(worker!=null);

			refreshNextEnabled();
			refreshPreviousEnabled();
			refreshButton();
		};

		@Override
		public void cancel(RDHEnvironment environment, GitRemotePublisherContext context) {
			// We don't allow backtracking while worker is still active, so it's safe to cleanup here
			worker = null;
		};

		@Override
		public boolean close() {
			boolean workerAliveAndRunning = workerRunning();

			worker = null;

			return workerAliveAndRunning;
		};

		@Override
		public Page<GitRemotePublisherContext> next(RDHEnvironment environment,
				GitRemotePublisherContext context) {
			// TODO Auto-generated method stub
			return FINISH;
		}
	};

	/**
	 *
	 * Implementation note: {@link ProgressMonitor#isCancelled()} is directly implemented by
	 * {@link SwingWorker#isCancelled()} without further addendum.
	 *
	 * @author Markus Gärtner
	 *
	 */
	private static class PushWorker extends SwingWorker<Iterable<PushResult>, String> implements ProgressMonitor {

		/** total number of tasks */
		private int totalTasks = -1;
		/** total number of expected steps in current task */
		private int totalWork = -1;
		/** total number of finished steps in current task */
		private int currentWork = -1;

		private final List<String> tasks = new ArrayList<>();

		private String task;

		private final PushCommand command;

		PushWorker(PushCommand command) {
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

				sb.append(rm.get("replaydh.wizard.gitRemotePublisher.export.tasks",
						taskCount, totalTasks==-1 ? "?" : totalTasks));

				for(String task : tasks) {
					sb.append("\r\n").append(task);
				}

				if(task!=null) {
					sb.append("\r\n").append(rm.get("replaydh.wizard.gitRemotePublisher.export.taskActive",
							task, currentWork+1, totalWork==0 ? "?" : totalWork));
				}
			}

			publish(sb.toString());
		}

		/**
		 * @see javax.swing.SwingWorker#doInBackground()
		 */
		@Override
		protected Iterable<PushResult> doInBackground() throws Exception {
			command.setProgressMonitor(this);

			return command.call();
		}

	}

	/**
	 *
	 */
	private static final GitRemotePublisherStep SAVE_REMOTE = new GitRemotePublisherStep(
			"saveRemote",
			"replaydh.wizard.gitRemotePublisher.saveRemote.title",
			"replaydh.wizard.gitRemotePublisher.saveRemote.description") {

		@Override
		protected JPanel createPanel() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Page<GitRemotePublisherContext> next(RDHEnvironment environment,
				GitRemotePublisherContext context) {
			// TODO Auto-generated method stub
			return null;
		}
	};

	/**
	 *
	 */
	private static final GitRemotePublisherStep FINISH = new GitRemotePublisherStep(
			"finish",
			"replaydh.wizard.gitRemotePublisher.finish.title",
			"replaydh.wizard.gitRemotePublisher.finish.description") {

		private JComboBox<PushResult> cbResults;

		private JTextArea taInfo;

		@Override
		protected JPanel createPanel() {

			cbResults = new JComboBox<>();
			cbResults.setEditable(false);
			cbResults.setRenderer(new PushResultListeCellRenderer());
			cbResults.addActionListener(ae -> displaySelectedResult());

			taInfo = GuiUtils.createTextArea("");
			taInfo.setLineWrap(false);
			taInfo.setBorder(GuiUtils.defaultContentBorder);

			return FormBuilder.create()
					.columns("fill:pref:grow")
					.rows("pref")
					.addScrolled(taInfo).xy(1, 1)
					.build();
		}

		private void displaySelectedResult() {
			//TODO
		}

		@Override
		public void refresh(RDHEnvironment environment, GitRemotePublisherContext context) {
			cbResults.removeAllItems();
			//TODO add some header?

			if(context.error!=null) {
				cbResults.setVisible(false);
				taInfo.setText(GuiUtils.errorText(context.error));
			} else if(context.results!=null) {
				context.results.forEach(cbResults::addItem);
				if(cbResults.getItemCount()>0) {
					cbResults.setSelectedIndex(0);
				} else {
					//TODO show info about not having any info...
				}
			} else {
				taInfo.setText(context.finalMessage);
			}

			setPreviousEnabled(false);
		};



		@Override
		public boolean close() {
			cbResults.removeAllItems();

			return true;
		};

		@Override
		public Page<GitRemotePublisherContext> next(RDHEnvironment environment,
				GitRemotePublisherContext context) {
			return null;
		}
	};

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
			} else if(value!=null) {
				tooltip = value.toString();
			}

			// Let the default implementation do the real work
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			setToolTipText(tooltip);

			return this;
		}
	}

	private static class PushResultListeCellRenderer implements ListCellRenderer<PushResult> {

		private final DefaultListCellRenderer delegate = new DefaultListCellRenderer();

		@Override
		public Component getListCellRendererComponent(JList<? extends PushResult> list,
				PushResult value, int index, boolean isSelected,
				boolean cellHasFocus) {

			String tooltip = null;
			String text = value.getURI().toString();
			Icon icon = null;

			delegate.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);

			delegate.setToolTipText(tooltip);
			delegate.setIcon(icon);

			return delegate;
		}
	}
}

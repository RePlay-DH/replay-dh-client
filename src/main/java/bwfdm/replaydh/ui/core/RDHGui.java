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

import java.awt.AWTException;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;
import javax.swing.RootPaneContainer;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.core.AbstractRDHTool;
import bwfdm.replaydh.core.RDHClient;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.io.FileTracker;
import bwfdm.replaydh.io.TrackerAdapter;
import bwfdm.replaydh.io.TrackerException;
import bwfdm.replaydh.io.TrackingStatus;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.actions.ActionManager;
import bwfdm.replaydh.ui.actions.ConditionResolver;
import bwfdm.replaydh.ui.core.RDHWelcomeWizard.WelcomeContext;
import bwfdm.replaydh.ui.helper.CloseableUI;
import bwfdm.replaydh.ui.helper.JMenuBarSource;
import bwfdm.replaydh.ui.helper.Wizard;

/**
 * @author Markus Gärtner
 *
 */
public class RDHGui extends AbstractRDHTool {

	private static final Logger log = LoggerFactory.getLogger(RDHGui.class);

	private static final Dimension MIN_WINDOW_SIZE = new Dimension(450, 300);

	/**
	 * Storage of windows that are currently showing and have been
	 * created by this class or are managed by it.
	 */
	private final Set<Window> openWindows = new HashSet<>();

	private volatile Window lastHiddenWindow = null;

	private final AtomicBoolean shutdownRequestActive = new AtomicBoolean(false);

	private final Handler handler = new Handler();

	private ActionManager actionManager;

	private TrayIcon trayIcon;

	/**
	 * Compound flag calculated from {@link SystemTray#isSupported()}
	 * and the current setting for the {@link RDHProperty#CLIENT_UI_TRAY_DISABLED}
	 * property.
	 */
	private boolean canUseSystemTray = false;

	/**
	 * Handler for keeping track of open windows and requesting
	 * a client shutdown once the last window closes.
	 */
	private final ShutdownHandler shutdownHandler = new ShutdownHandler();

	/**
	 * @throws RDHLifecycleException
	 * @see bwfdm.replaydh.core.RDHTool#start(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public boolean start(RDHEnvironment environment) throws RDHLifecycleException {
		if(!super.start(environment)) {
			return false;
		}

		ConditionResolver conditionResolver = new RDHConditionResolver(environment);
		actionManager = ActionManager.globalManager().derive(null, null, conditionResolver);

		// Honor user override if system supports tray
		canUseSystemTray = SystemTray.isSupported() && !environment.getBoolean(
				RDHProperty.CLIENT_UI_TRAY_DISABLED, false);

		FileTracker fileTracker = environment.getClient().getFileTracker();
		fileTracker.addTrackerListener(handler);

//TODO
//        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        } catch (Exception e) {
//        	//ignore
//        }

		return true;
	}

	/**
	 * @throws RDHLifecycleException
	 * @see bwfdm.replaydh.core.RDHTool#stop(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public void stop(RDHEnvironment environment) throws RDHLifecycleException {
		//TODO shutdown tray icon and such?

		FileTracker fileTracker = environment.getClient().getFileTracker();
		fileTracker.removeTrackerListener(handler);

		for(Window window : openWindows) {
			window.removeWindowListener(shutdownHandler);
			window.dispose();
		}

		openWindows.clear();

		super.stop(environment);
	}

	/**
	 * Tells whether or not using the {@link SystemTray} is supported
	 * and enabled under the current operating system and application
	 * settings.
	 */
	public boolean isCanUseSystemTray() {
		return canUseSystemTray && !getEnvironment().getBoolean(RDHProperty.CLIENT_UI_TRAY_DISABLED, false);
	}

	/**
	 * Shows either a welcome dialog and wizard or the client's {@link RDHMainPanel main panel}
	 * in a new window.
	 */
	public void showUI() {
		// Make sure everything from here runs on the EDT
		GuiUtils.invokeEDT(this::doShowUI);
	}

	/**
	 * @return the actionManager
	 */
	public ActionManager getActionManager() {
		return actionManager;
	}

	private boolean needsSetupWizard() {

		RDHEnvironment environment = getEnvironment();

		return environment.getProperty(RDHProperty.CLIENT_USERNAME)==null
				|| environment.getBoolean(RDHProperty.INTERN_FORCE_WELCOME_DIALOG, false);
	}

	private void rebuildPreviousWorkspace() {
		RDHEnvironment environment = getEnvironment();

		String previousPathString = environment.getProperty(RDHProperty.CLIENT_WORKSPACE_PATH);

		// No workspace configured, just abort
		if(previousPathString==null || previousPathString.isEmpty()) {
			return;
		}

		final Path workspacePath = Paths.get(previousPathString);

		// If we can't access previous workspace, invoke user dialog
		if(!Files.exists(workspacePath, LinkOption.NOFOLLOW_LINKS)) {
			String title = ResourceManager.getInstance().get("replaydh.dialogs.workspaceError.title");
			String message = ResourceManager.getInstance().get(
					"replaydh.dialogs.workspaceError.missingWorkspaceFolder", workspacePath);

			GuiUtils.showError(null, title, message);
			return;
		}

		try {
			environment.getClient().loadWorkspace(workspacePath);
		} catch(RDHException e) {
			log.error("Accessing previous workspace at '{}' failed", workspacePath, e);

			String title = ResourceManager.getInstance().get("replaydh.dialogs.workspaceError.title");
			String message = ResourceManager.getInstance().get(
					"replaydh.dialogs.workspaceError.accessFailed", workspacePath);

			GuiUtils.showError(null, title, message);
		}
	}

	/**
	 * Note that this method <b>must</b> be called on the <i>event-dispatch thread</i>!
	 */
	private void doShowUI() {
		GuiUtils.checkEDT();

		RDHEnvironment environment = getEnvironment();

		rebuildPreviousWorkspace();

		//Check if we have properly setup everything or if we need some wizard dialog
		if(needsSetupWizard()) {
			try (Wizard<WelcomeContext> wizard = RDHWelcomeWizard.getWizard(null, environment)) {

				if(isVerbose()) {
					log.info("Initiating welcome wizard");
				}
				WelcomeContext context = new WelcomeContext();
				// Walk user through the setup process
				wizard.startWizard(context);

				// If we can't finish setup, tell user
				if(!wizard.isFinished() || wizard.isCancelled()) {
					if(isVerbose()) {
						log.info("Welcome wizard failed - invoking client shutdown");
					}

					GuiUtils.showInfo(null, ResourceManager.getInstance().get("replaydh.canceledSetup"));
					invokeShutdown(false);
					return;
				} else {
					// Everything went fine, finalize setup
					environment.setProperty(RDHProperty.CLIENT_USERNAME, context.username);
					environment.setProperty(RDHProperty.CLIENT_ORGANIZATION, context.organization);
				}
			}
		}

		// Everything went well -> start showing the real client screen
		showMainWindow();
	}

	private void showMainWindow() {

		if(isVerbose()) {
			log.info("Showing new main window");
		}

		RDHMainPanel panel = new RDHMainPanel(getEnvironment());

		showFrame(panel, SwingConstants.WEST);
	}

	/**
	 * Wraps the given {@code content} into a new {@link JFrame} and
	 * shows it.
	 */
	private void showFrame(Component content, int screenAlignment) {

		RDHFrame frame = new RDHFrame();
		frame.setMinimumSize(MIN_WINDOW_SIZE);

		frame.setTitle(ResourceManager.getInstance().get("replaydh.app.title"));

		GuiUtils.decorateFrame(frame);

		if(content instanceof JMenuBarSource) {
			frame.setJMenuBar(((JMenuBarSource)content).createJMenuBar());
		}

		frame.add(content);
		//TODO do we need sanity checks against unwanted window sizes?
		frame.pack();

		Point location = getWindowLocation(frame.getSize(), screenAlignment);
		if(location==null) {
			frame.setLocationByPlatform(true);
		} else {
			frame.setLocation(location);
		}

		frame.addWindowListener(shutdownHandler);

		frame.setVisible(true);
	}

	private static final int defaultMargin = 5;

	private Point getWindowLocation(Dimension size, int screenAlignment) {
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

		Point location = new Point();

		switch (screenAlignment) {
		case SwingConstants.WEST:
			location.x = screen.width-size.width - defaultMargin;
			location.y = (screen.height-size.height)/2;
			break;

		default:
			throw new IllegalArgumentException("Unknown screen alignment value: "+screenAlignment);
		}

		return location;
	}

	public void invokeShutdown(boolean restart) {
		// Prevent concurrent attempts to shutdown the client
		if(shutdownRequestActive.compareAndSet(false, true)) {

			// Make sure we hide our GUI components
			if(isCanUseSystemTray()) {
				hideTrayIcon();
			}

			for(Window window : Window.getWindows()) {
				disposeGracefully(window);
			}

			lastHiddenWindow = null;

			try {
				/*
				 *  Sanity check:
				 *  If window closed as result of client shutdown, we could have
				 *  the environment "disappear" before this internal attempt of
				 *  shutting down.
				 */
				if(hasEnvironment()) {
					RDHClient client = getEnvironment().getClient();
					if(restart) {
						client.restart();
					} else {
						client.shutdown();
					}
				}
			} finally {
				// Usually this code will never be reached when the client actually shuts down
				shutdownRequestActive.set(false);
			}
		}
	}

	/**
	 * Returns {@code true} if the given {@code window} can be closed.
	 *
	 * @param window
	 * @return
	 */
	private boolean canClose(Window window) {

		boolean closeable = true;

		// Check if the window itself can be closed first
		if(window instanceof CloseableUI) {
			closeable = ((CloseableUI)window).canClose();
		} else if(window instanceof RootPaneContainer) {
			Container contentPane = ((RootPaneContainer)window).getContentPane();
			closeable = CloseableUI.canClose(contentPane);
		}

		// If window refused to be closed then don't even bother checking further
		if(!closeable) {
			return false;
		}

		// Check if it's the last active window
		if(openWindows.size()==1) {
			//TODO get user confirmation about closing down
		}

		return closeable;
	}

	private TrayIcon ensureTrayIcon() {
		if(!isCanUseSystemTray()) {
			return null;
		}

		if(trayIcon==null) {

			if(isVerbose()) {
				log.info("Initializing tray icon");
			}

			trayIcon = new TrayIcon(GuiUtils.APP_ICON_18, "RePlay-DH Client");
			trayIcon.setImageAutoSize(true);
			trayIcon.addActionListener(handler::restoreClient);

			ResourceManager rm = ResourceManager.getInstance();

			PopupMenu popupMenu = new PopupMenu();
			popupMenu.add(createMenuItem(rm.get("replaydh.systemTray.exit"), handler::exitClient));
			popupMenu.add(createMenuItem(rm.get("replaydh.systemTray.restore"), handler::restoreClient));
			trayIcon.setPopupMenu(popupMenu);
		}

		return trayIcon;
	}

	private MenuItem createMenuItem(String label, ActionListener actionListener) {
		MenuItem menuItem = new MenuItem(label);
		menuItem.addActionListener(actionListener);
		return menuItem;
	}

	private void showTrayIcon() {
		if(!isCanUseSystemTray())
			throw new UnsupportedOperationException("No tray support");

		if(isVerbose()) {
			log.info("Showing tray icon");
		}

		GuiUtils.checkEDT();

		SystemTray systemTray = SystemTray.getSystemTray();

		// Check that our tray icon isn't already showing
		if(trayIcon!=null) {
			TrayIcon[] trayIcons = systemTray.getTrayIcons();
			for(TrayIcon ti : trayIcons) {
				if(trayIcon==ti) {
					return;
				}
			}
		}

		ensureTrayIcon();

		try {
			systemTray.add(trayIcon);
		} catch (AWTException e) {
			throw new RDHException("Failed to show tray icon", e);
		}

		// Inform user about tray icon functionality
		showDefaultTrayInfo();
	}

	private void showDefaultTrayInfo() {

		if(isVerbose()) {
			log.info("Displaying default tray info");
		}

		ResourceManager rm = ResourceManager.getInstance();
		String title = rm.get("replaydh.systemTray.client.title");
		String message = rm.get("replaydh.systemTray.client.trayInfo");
		showTrayMessage(title, message, MessageType.INFO);
	}

	private void hideTrayIcon() {
		if(!isCanUseSystemTray())
			throw new UnsupportedOperationException("No tray support");

		if(isVerbose()) {
			log.info("Hiding tray icon");
		}

		GuiUtils.checkEDT();

		if(trayIcon!=null) {

			SystemTray.getSystemTray().remove(trayIcon);

			trayIcon = null;
		}
	}

	/**
	 * Closes the given window, removes it from the list
	 * of open windows and either {@link #disposeGracefully(Window) disposes}
	 * off it or "closes it to tray" if the tray area is supported and
	 * the window was the last visible main window for the client.
	 */
	private void closeWindow(Window window) {

		openWindows.remove(window);

		if(isCanUseSystemTray()) {
			lastHiddenWindow = window;
			window.setVisible(false);

			if(openWindows.isEmpty()) {
				showTrayIcon();
			}
		} else {
			//TODO ask user confirmation in case this is the last window and we would otherwise shutdown the client
			disposeGracefully(window);
		}
	}

	/**
	 * If window is an instance of {@link CloseableUI} allows it to
	 * {@link CloseableUI#close() close} itself.
	 * In any case {@link Window#dispose() disposes} of the window
	 * resource evenatually.
	 *
	 * @param window
	 */
	private void disposeGracefully(Window window) {
		if(window instanceof CloseableUI) {
			((CloseableUI)window).close();
		}
		window.dispose();
	}

	/**
	 * Tries to open the window that has been hidden (or closed to try area)
	 * most recently, if such a window exists.
	 */
	private void reopenWindow() {
		GuiUtils.checkEDT();

		if(lastHiddenWindow!=null) {
			lastHiddenWindow.setVisible(true);
			openWindows.add(lastHiddenWindow);
			lastHiddenWindow = null;
		}
	}

	public void showTrayMessage(String title, String message, MessageType messageType) {
		if(isCanUseSystemTray() && trayIcon!=null) {
			trayIcon.displayMessage(title, message, messageType);
		} else {
			//TODO find some other non-invasive way of displaying the info
		}
	}

	private class Handler extends TrackerAdapter {

		/**
		 * @see bwfdm.replaydh.io.TrackerAdapter#statusInfoChanged(bwfdm.replaydh.io.FileTracker)
		 */
		@Override
		public void statusInfoChanged(FileTracker tracker) {
			if(!tracker.hasStatusInfo()) {
				return;
			}

			boolean hasFiles = false;

			try {
				hasFiles = tracker.hasFilesForStatus(TrackingStatus.UNKNOWN, TrackingStatus.MISSING,
						TrackingStatus.MODIFIED, TrackingStatus.CORRUPTED);
			} catch (TrackerException e) {
				log.error("Failed to query file tracker for files", e);
			}

			// If we have any kind of changes detected, go and show a tray message to the user
			if(hasFiles) {
				ResourceManager rm = ResourceManager.getInstance();
				String title = rm.get("replaydh.systemTray.fileTracker.title");
				String message = rm.get("replaydh.systemTray.fileTracker.filesChanged");

				//TODO if only one type of change occurred, show a specialized message tailored to that type

				showTrayMessage(title, message, MessageType.INFO);
			}
		}

		private void exitClient(ActionEvent ae) {
			invokeShutdown(false);
		}

		private void restoreClient(ActionEvent ae) {
			/*
			 *  Invoked when user clicks the tray icon.
			 *
			 *  We gonna show the last closed window again (which caused the
			 *  switch to tray mode) and also hide the tray icon, so that main
			 *  focus of interaction is again on the "real" GUI.
			 */

			reopenWindow();

			GuiUtils.invokeEDT(RDHGui.this::hideTrayIcon);
		}
	}

	private class ShutdownHandler extends WindowAdapter {

		/**
		 * Window being shown for the first time.
		 *
		 * @see java.awt.event.WindowAdapter#windowOpened(java.awt.event.WindowEvent)
		 */
		@Override
		public void windowOpened(WindowEvent e) {
			openWindows.add(e.getWindow());
		}

		/**
		 * User is attempting to close window.
		 *
		 * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
		 */
		@Override
		public void windowClosing(WindowEvent e) {
			Window window = e.getWindow();
			if(canClose(window)) {
				closeWindow(window);
			}
		}

		/**
		 * Window has been {@link Window#dispose() disposed}.
		 *
		 * @see java.awt.event.WindowAdapter#windowClosed(java.awt.event.WindowEvent)
		 */
		@Override
		public void windowClosed(WindowEvent e) {
			if(openWindows.isEmpty()) {
				invokeShutdown(false);
			}
		}
	}
}

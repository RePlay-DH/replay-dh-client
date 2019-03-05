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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Forms;
import com.jgoodies.forms.factories.Paddings;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.stats.Interval;
import bwfdm.replaydh.stats.StatEntry;
import bwfdm.replaydh.stats.StatType;
import bwfdm.replaydh.ui.GuiStats;
import bwfdm.replaydh.ui.GuiUtils;

/**
 * @author Markus Gärtner
 *
 * @param <E> type of context to be used
 */
public class Wizard<E extends Object> extends JDialog implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(Wizard.class);

	private static final long serialVersionUID = -2324289994636994740L;

//	public static void main(String[] args) {
//		Page<Object> page1 = mock(Page.class);
//		when(page1.getTitle()).thenReturn("Step 1");
//		when(page1.getDescription()).thenReturn("Description 1");
//		when(page1.getPageComponent()).thenReturn(new JLabel("Content XX"));
//
//		Page<Object> page2 = mock(Page.class);
//		when(page2.getTitle()).thenReturn("Step 2");
//		when(page2.getDescription()).thenReturn("Description 2");
//		when(page2.getPageComponent()).thenReturn(new JLabel("Content XX v2"));
//
//		when(page1.next(any(), any())).thenReturn(page2);
//
//		Wizard<Object> wizard = new Wizard<>(null, "Test Wizard", mock(RDHEnvironment.class), page1, page2);
//		wizard.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
//
//		wizard.startWizard(new Object());
//	}

	private final RDHEnvironment environment;

	// General page control
	private final JButton previousButton, cancelButton, nextButton;

	/**
	 * Placeholder for the pages' individual content
	 */
	private final JScrollPane pageProxy;

	private final JPanel mainPanel;

	/**
	 * Static title of the entire wizard
	 */
//	private final JLabel header;

	/**
	 * Individual title of active page
	 */
	private final JLabel title;

	/**
	 * Individual description of active page
	 */
	private final JTextArea description;

	private final Handler handler;

	/**
	 * Supplied pages in original order
	 */
	private final Page<E>[] pages;

	/**
	 * Visual progress indicator
	 */
	private final JLabel[] states;

	/**
	 *  Number of visited pages, gives pointer into
	 *  {@link #trace} array for current page
	 */
	private int pagesVisited = 0;

	/**
	 * Stack of visited pages so far
	 */
	private final int[] trace;

	/**
	 * Flag to signal that the wizard process has been canceled.
	 */
	private boolean canceled = false;

	/**
	 * FLag indicating that the wizard exited "clean",
	 * i.e. either via cancellation or when the user
	 * successfully clicked the final "finish" button and
	 * all participating pages managed to persist their
	 * states.
	 */
	private boolean finished = false;

	/**
	 * Client payload used to store the process data and final
	 * results of the wizard.
	 */
	private E context;

	private static final Dimension DEFAULT_DIALOG_SIZE = new Dimension(700, 650);

	private final String statLabel;

	private final Interval wizardUptime = new Interval();

	public Wizard(Window parent, String statLabel, String wizardTitle, RDHEnvironment environment,
			@SuppressWarnings("unchecked") Page<E>...pages) {
		super(parent, wizardTitle);

		this.statLabel = requireNonNull(statLabel);
		this.environment = requireNonNull(environment);

		requireNonNull(pages);
		checkArgument("Number of pages must be greater than zero", pages.length>0);

		this.pages = pages;

		setModal(true);

		/*
		 * General setup:
		 *
		 * +------------------------------------+
		 * |               HEADER               |
		 * +------------------------------------+
		 * |     TITLE                          |
		 * |     DESCRIPTION                    |
		 * +---+--------------------------------+
		 * |T  |                                |
		 * |R  |                                |
		 * |A  |              PAGE              |
		 * |C  |                                |
		 * |E  |                                |
		 * +---+--------------------------------+
		 * |  +------+    +------+    +------+  |
		 * |  | PREV |    |CANCEL|    | NEXT |  |
		 * |  +------+    +------+    +------+  |
		 * +------------------------------------+
		 *
		 */

		// Init UI
		ResourceManager rm = ResourceManager.getInstance();

		handler = new Handler();

//		header = createLabel(24, true, SwingConstants.CENTER, SwingConstants.CENTER);
//		header.setText(wizardTitle);

		title = createLabel(16, true, SwingConstants.LEFT, SwingConstants.CENTER);

		description = GuiUtils.createTextArea(null);

		trace = new int[pages.length];
		Arrays.fill(trace, -1);

		states = new JLabel[pages.length];
		for(int i=0; i<pages.length; i++) {
			JLabel label = createLabel(11, true, SwingConstants.LEFT, SwingConstants.CENTER);
			label.setText(pages[i].getTitle());
			label.setToolTipText(GuiUtils.toSwingTooltip(
					pages[i].getDescription()));
			label.setEnabled(false);
			label.setFocusable(false);
			states[i] = label;
		}

		previousButton = new JButton(rm.get("replaydh.labels.previous"));
		previousButton.addActionListener(handler::doPrevious);

		nextButton = new JButton(rm.get("replaydh.labels.next"));
		nextButton.addActionListener(handler::doNext);

		cancelButton = new JButton(rm.get("replaydh.labels.cancel"));
		cancelButton.addActionListener(handler::doCancel);

		pageProxy = new JScrollPane(new JLabel("Nothing..."));
		pageProxy.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		pageProxy.setBorder(Paddings.DLU4);

		mainPanel = FormBuilder.create()
				.columns("pref, 2dlu, max(100dlu;pref):grow")
				.rows("pref, 3dlu, pref, 1dlu, pref, 2dlu, fill:max(70dlu;pref):grow, 3dlu, pref")
				// Header area
//				.add(header).xyw(1, 1, 3)
//				.add(new JSeparator()).xyw(1, 2, 3)
				.add(title).xywh(1, 3, 3, 1, "center, center")
				.add(description).xy(3, 5)
				.add(new JSeparator()).xyw(1, 6, 3)
				// Left outline
				.add(Forms.buttonStack(states)).xy(1, 7, "left, top")
				.add(new JSeparator(SwingConstants.VERTICAL)).xy(2, 7)
				// Content part
				.add(pageProxy).xy(3, 7, "fill, fill")
				.add(new JSeparator()).xyw(1, 8, 3)
				// Controls
				.addBar(previousButton, cancelButton, nextButton).xyw(1, 9, 3, "center, center")
				.padding(Paddings.DIALOG)
				.build();

		getContentPane().add(mainPanel);
		pack();
		setMinimumSize(new Dimension(DEFAULT_DIALOG_SIZE));
	}

	private JLabel createLabel(int fontSize, boolean bold, int hAlign, int vAlign) {
		JLabel label = new JLabel();
		Font font = label.getFont();
		if(fontSize!=-1) {
			font = font.deriveFont((float)fontSize);
		}
		if(bold) {
			font = font.deriveFont(Font.BOLD);
		}
		label.setFont(font);
		label.setHorizontalAlignment(hAlign);
		label.setVerticalAlignment(vAlign);
		return label;
	}

	/**
	 * Make sure that all pages have a properly initialized UI
	 */
	private void ensureUI() {
		for(Page<?> page : pages) {
			page.getPageComponent();
		}
	}

	public void startWizard(E context) {

		this.context = requireNonNull(context);

		ensureUI();

		setNextActivePage(0);

		setLocationRelativeTo(null);

		wizardUptime.start();
		environment.getClient().getStatLog().log(StatEntry.withData(StatType.UI_OPEN, GuiStats.WIZARD, statLabel));

		refreshPageUI();

		setVisible(true);
	}

	public E getContext() {
		checkState("No context available", context!=null);

		return context;
	}

	/**
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() {

		if(context!=null && context instanceof AutoCloseable) {
			try {
				((AutoCloseable)context).close();
			} catch (Exception e) {
				log.error("Failed to close context", e);
			}
		}
		context = null;

		// Close all pages
		for(Page<E> page : pages) {
			page.close();
		}
	}

	public boolean isCancelled() {
		return canceled;
	}

	public boolean isFinished() {
		return finished;
	}

	private int activePageIndex() {
		return pagesVisited==0 ? -1 : trace[pagesVisited-1];
	}

	private void pushPageIndex(int pageIndex) {
		trace[pagesVisited] = pageIndex;
		pagesVisited++;
	}

	private int pollPageIndex() {
		checkState("No previous page available", pagesVisited>1);
		pagesVisited--;
		int oldPageIndex = trace[pagesVisited];
		trace[pagesVisited] = -1;
		int newPageIndex = trace[pagesVisited-1];

		for(int i=oldPageIndex;i>newPageIndex; i--) {
			states[i].setEnabled(false);
		}

		return newPageIndex;
	}

	private Page<E> activePage() {
		int activePageIndex = activePageIndex();
		return activePageIndex==-1 ? null : pages[activePageIndex];
	}

	private boolean isFirstPage() {
		return pagesVisited==1;
	}

	private boolean isLastPage() {
		return activePageIndex()==pages.length-1;
	}

	private void setPage(Page<E> page) {
		requireNonNull(page);

		int newActivePageIndex = -1;

		for(int i=0; i<pages.length; i++) {
			if(pages[i]==page) {
				newActivePageIndex = i;
				break;
			}
		}

		if(newActivePageIndex==-1)
			throw new IllegalArgumentException("Unknown page: "+page.getTitle());

		setNextActivePage(newActivePageIndex);
	}

	private void setNextActivePage(int newActivePageIndex) {
		checkArgument("page index cannot be negative", newActivePageIndex>=0 && newActivePageIndex<pages.length);

		int currentActivePageIndex = activePageIndex();
		checkArgument("can only go forward", newActivePageIndex>currentActivePageIndex);

		// If this isn't the first time any page is shown then we need to do some cleanup
		if(currentActivePageIndex>-1) {
			Page<E> currentPage = pages[currentActivePageIndex];
			currentPage.setControl(null);

			// Mark current page as "visited"
			states[currentActivePageIndex].setEnabled(true);
			// Mark subsequent pages as "ignored"
			for(int i=currentActivePageIndex+1; i<newActivePageIndex; i++) {
				states[i].setEnabled(false);
			}
		}

		// Go forward once and record new index
		pushPageIndex(newActivePageIndex);

		refreshPageUI();
	}

	private void refreshPageUI() {
		ResourceManager rm = ResourceManager.getInstance();

		Page<E> currentPage = activePage();
		checkState("no active page", currentPage!=null);

		boolean lastPage = isLastPage();

		nextButton.setEnabled(true);
		previousButton.setEnabled(!isFirstPage());
		nextButton.setText(lastPage ?
				rm.get("replaydh.labels.finish")
				: rm.get("replaydh.labels.next"));
		cancelButton.setEnabled(!lastPage || currentPage.canCancel());

		int currentActivePageIndex = activePageIndex();
		states[currentActivePageIndex].setEnabled(true);

		currentPage.setControl(handler);
		currentPage.refresh(environment, context);
		title.setText(currentPage.getTitle());
		description.setText(currentPage.getDescription());

		pageProxy.setViewportView(currentPage.getPageComponent());

		growIfNeeded();
	}

	private void growIfNeeded() {

		Dimension size = getSize();
		Dimension pref = getPreferredSize();

//		System.out.printf("size=%s pref=%s main=%s\n", size, pref, mainPanel.getPreferredSize());

		boolean resize = false;

		if(pref.width>size.width) {
			resize = true;
			size.width = pref.width;
		}

		if(pref.height>size.height) {
			resize = true;
			size.height = pref.height;
		}

		if(resize) {
			setSize(size);
		}
	}

	private void setNextEnabled(boolean enabled) {
		nextButton.setEnabled(enabled && !isLastPage());
	}

	private void setPreviousEnabled(boolean enabled) {
		previousButton.setEnabled(enabled && !isFirstPage());
	}

	private void stopWizard(boolean cancel) {
		canceled = cancel;

		if(!cancel) {
			E context = this.context;
			// Persist all participating pages
			for(int i=0; i<pagesVisited; i++) {
				int pageIndex = trace[i];
				pages[pageIndex].persist(environment, context);
			}
		}

		// Close all pages
		for(Page<E> page : pages) {
			page.close();
		}

		// Only after everything else went well do we mark the wizard as finished!
		finished = true;

		// Stop showing the dialog
		setVisible(false);
		dispose();

		environment.getClient().getStatLog().log(
				StatEntry.withData(StatType.UI_CLOSE, GuiStats.WIZARD, statLabel,
						wizardUptime.stop().asDurationString()));

		wizardUptime.reset();
	}

	private class Handler implements WizardControl<E> {

		private void doNext(ActionEvent ae) {
			Page<E> currentPage = activePage();
			checkState("No active page", currentPage!=null);

			if(isLastPage()) {
				// Finish entire wizard process if this is the last page
				stopWizard(false);
			} else {
				// Let current page decide how to proceed
				Page<E> nextPage = currentPage.next(environment, context);

				// Only continue with a valid next page
				if(nextPage!=null) {

					environment.getClient().getStatLog().log(
							StatEntry.withData(StatType.UI_ACTION, GuiStats.WIZARD_PAGE_NEXT, statLabel, nextPage.getId()));

					setPage(nextPage);
				}
			}
		}

		private void doPrevious(ActionEvent ae) {

			environment.getClient().getStatLog().log(
					StatEntry.withData(StatType.UI_ACTION, GuiStats.WIZARD_PAGE_PREV, statLabel, activePage().getId()));

			activePage().cancel(environment, context);

			// Discard current page and go back once
			pollPageIndex();

			// Do a complete rebuild of the page
			refreshPageUI();
		}

		private void doCancel(ActionEvent ae) {

			environment.getClient().getStatLog().log(
					StatEntry.withData(StatType.UI_ACTION, GuiStats.WIZARD_CANCEL, statLabel));

			Wizard.this.stopWizard(true);
		}

		/**
		 * @see bwfdm.replaydh.ui.helper.Wizard.WizardControl#setNextEnabled(boolean)
		 */
		@Override
		public void setNextEnabled(boolean enabled) {
			Wizard.this.setNextEnabled(enabled);
		}

		/**
		 * @see bwfdm.replaydh.ui.helper.Wizard.WizardControl#setPreviousEnabled(boolean)
		 */
		@Override
		public void setPreviousEnabled(boolean enabled) {
			Wizard.this.setPreviousEnabled(enabled);
		}

		/**
		 * @see bwfdm.replaydh.ui.helper.Wizard.WizardControl#invokeNext(bwfdm.replaydh.ui.helper.Wizard.Page)
		 */
		@Override
		public void invokeNext(Page<E> next) {
			Wizard.this.setPage(next);
		}

	}

	public interface WizardControl<E extends Object> {
		/**
		 * Defines whether or not the button to progress to the "next"
		 * step should be enabled, allowing the user to continue.
		 *
		 * @param enabled
		 */
		void setNextEnabled(boolean enabled);

		/**
		 * Defines whether or not the button to return to the "previous"
		 * step should be enabled, allowing the user to backtrack
		 * his decisions.
		 *
		 * @param enabled
		 */
		void setPreviousEnabled(boolean enabled);

		/**
		 * Overrides the default behavior of using the "next" button
		 * to progress to the next page. This method is intended for
		 * situations where the wizard should skip pages based on the
		 * users decision (e.g. when the page presents a "Skip to XX"
		 * button/option).
		 */
		void invokeNext(Page<E> next);
	}

	public interface Page<E extends Object> {

		/**
		 * Title to identify this page/step
		 */
		String getTitle();

		/**
		 * Non-localized identifier of the page
		 */
		String getId();

		/**
		 * Short description on what to do on this page
		 */
		String getDescription();

		/**
		 *Component hosting all the content of this page
		 */
		JComponent getPageComponent();

		/**
		 * Used when a page is visited for the first time or the
		 * user decided to go back to it during an active path
		 * via the "previous" button.
		 * @param context
		 */
		void refresh(RDHEnvironment environment, E context);

		void setControl(WizardControl<E> control);

		/**
		 * Callback to process current content of the page
		 * and signal which page to visit next.
		 * <p>
		 * If something went wrong and continuing to a new
		 * page is not possible, returning {@code null} will
		 * make the wizard stay on the current page.
		 *
		 * @return
		 */
		Page<E> next(RDHEnvironment environment, E context);

		/**
		 * Called when the user decides to go back a page and
		 * discard changes to the current one.
		 */
		void cancel(RDHEnvironment environment, E context);

		/**
		 * Used to finalize the wizard process.
		 * This method will be called for each page that has been
		 * visited in the final path chosen by the user in the order
		 * they have been visited.
		 *
		 * @param context
		 */
		void persist(RDHEnvironment environment, E context);

		/**
		 * Only needed for final pages, this method indicates
		 * whether or not the page can still be cancelled at this
		 * point.
		 *
		 * @return
		 */
		default boolean canCancel() {
			return true;
		}

		/**
		 * Optional method for cleanup work. Will be invoked when
		 * the wizard is shutting down to allow all pages to free
		 * background resources.
		 * <p>
		 * Invoking this method on an already closed page should
		 * have no negative side effects.
		 * <p>
		 * If a page is unable to properly close down due to some
		 * background task or open resource that it is trying to
		 * close, this method should return {@code false}.
		 * <p>
		 * THe default implementation always returns {@code true}.
		 */
		default boolean close() {
			return true;
		}
	}
}

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

import static java.util.Objects.requireNonNull;

import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Forms;
import com.jgoodies.forms.factories.Paddings;

import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.icons.IconRegistry;
import bwfdm.replaydh.ui.icons.Resolution;

/**
 * A version of {@link SwingWorker} that maintains and displays its own
 * {@link JDialog} instance while running the background task.
 *
 * @param <T> type returned by {@link #doInBackground()} and {@link #get()}
 * @param <V> type for intermediary results send to {@link #process(java.util.List)}
 *
 * @author Markus Gärtner
 *
 */
public abstract class AbstractDialogWorker<T extends Object, V extends Object> extends SwingWorker<T, V> {

	private final JTextArea taInfo;
	private final JLabel lIcon;
	private final JDialog dialog;
	private final JButton bControl;

	private final CancellationPolicy cancellationPolicy;

	public static enum MessageType {
		RUNNING,
		FINISHED,
		FAILED,
		CANCELLED,
		;
	}

	public static enum CancellationPolicy {
		NO_CANCEL,
		CANCEL,
		CANCEL_INTERRUPT,
		;
	}

	public AbstractDialogWorker(Window owner, String title, CancellationPolicy cancellationPolicy) {
		requireNonNull(owner);
		requireNonNull(title);
		this.cancellationPolicy = requireNonNull(cancellationPolicy);

		ResourceManager rm = ResourceManager.getInstance();

		taInfo = GuiUtils.createTextArea(null);
		bControl = new JButton(rm.get("replaydh.labels.cancel"));
		bControl.addActionListener(this::onControlButtonClicked);
		bControl.setEnabled(cancellationPolicy!=CancellationPolicy.NO_CANCEL);

		lIcon = new JLabel(IconRegistry.getGlobalRegistry().getIcon("loading-64.gif"));

		JPanel panel = FormBuilder.create()
				.columns("10dlu, max(150dlu;pref):grow:fill, 10dlu")
				.rows("max(40dlu;pref), 6dlu, pref, 6dlu, pref")
				.padding(Paddings.DIALOG)
				.add(taInfo).xyw(1, 1, 3)
				.add(lIcon).xy(2, 3)
				.add(Forms.buttonBar(bControl)).xyw(1, 5, 3, "center, center")
				.build();

		dialog = new JDialog(owner, title);
		dialog.setModalityType(ModalityType.APPLICATION_MODAL);
		dialog.add(panel);
		dialog.setLocationRelativeTo(null);
		dialog.pack();
	}

	protected abstract String getMessage(MessageType messageType, Throwable t);

	private void onControlButtonClicked(ActionEvent ae) {
		if(isDone()) {
			dialog.setVisible(false);
		} else if(cancellationPolicy!=CancellationPolicy.NO_CANCEL) {
			boolean mayInterruptIfRunning = cancellationPolicy==CancellationPolicy.CANCEL_INTERRUPT;
			cancel(mayInterruptIfRunning);
		}
	}

	public void start() {
		execute();
		taInfo.setText(getMessage(MessageType.RUNNING, null));
		dialog.setVisible(true);
	}

	/**
	 * Does a best-effort attempt at returning the final result of this worker.
	 * If the worker hasn't {@link #isDone() finished} yet or the {@link #get()}
	 * method throws an error, {@code null} is returned.
	 * @return
	 */
	protected T tryGetUnblocked() {
		if(isDone()) {
			try {
				return get();
			} catch (InterruptedException | ExecutionException e) {
				// ignored
			}
		}

		return null;
	}

	/**
	 * @see javax.swing.SwingWorker#done()
	 */
	@Override
	protected final void done() {
		String message = getMessage(MessageType.FINISHED, null);
		String iconName = "icons8-Ok-48.png";

		try {
			T result = get();
			doneImpl(result);
		} catch (InterruptedException | CancellationException e) {
			// Accept user cancellation
			message = getMessage(MessageType.CANCELLED, e);
			iconName = "icons8-Error-48.png";
		} catch (ExecutionException e) {
			message = getMessage(MessageType.FAILED, e.getCause());
			iconName = "icons8-Cancel-48.png";
		}

		taInfo.setText(message);
		lIcon.setIcon(IconRegistry.getGlobalRegistry().getIcon(iconName, Resolution.forSize(24)));
		bControl.setText(ResourceManager.getInstance().get("replaydh.labels.close"));
		bControl.setEnabled(true);

		dialog.pack();
	}

	/**
	 * Hook that replaces {@link #done()} for subclasses.
	 */
	protected void doneImpl(T result) {
		// no-op
	}

	/**
	 * Hides the dialog window without the user interaction.
	 */
	protected void end() {
		dialog.setVisible(false);
		dialog.dispose();
	}
}

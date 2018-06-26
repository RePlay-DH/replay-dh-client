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
package bwfdm.replaydh.ui.config;

import static java.util.Objects.requireNonNull;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.JPanel;
import javax.swing.event.ChangeListener;

import com.jgoodies.forms.factories.Paddings;

import bwfdm.replaydh.utils.DefaultChangeSource;

/**
 * @author Markus Gärtner
 *
 */
public abstract class DelegatingPreferencesTab implements PreferencesTab {

	private final JPanel panel;

	private final List<PreferencesDelegate<?, ?>> delegates = new ArrayList<>();

	private final DefaultChangeSource changeSource;

	private final Consumer<PreferencesDelegate<?, ?>> changeBouncer;

	protected DelegatingPreferencesTab() {
		changeSource = new DefaultChangeSource(this);
		changeBouncer = d -> changeSource.fireChange();

		panel = new JPanel();
		panel.setBorder(Paddings.DLU7);
	}

	protected JPanel getPanel() {
		return panel;
	}

	protected List<PreferencesDelegate<?, ?>> getDelegates() {
		return delegates;
	}

	protected void addDelegate(PreferencesDelegate<?, ?> delegate) {
		requireNonNull(delegate);

		delegate.setChangeBouncer(changeBouncer);

		delegates.add(delegate);
	}

	/**
	 * @see bwfdm.replaydh.ui.config.PreferencesTab#getPreferencesComponent()
	 */
	@Override
	public Component getPreferencesComponent() {
		return panel;
	}

	/**
	 * {@inheritDoc}
	 *
	 * This implementation returns over the internal list of {@link PreferencesDelegate delegates}
	 * and returns {@code true} if at least one of them reports
	 * {@link PreferencesDelegate#hasChanged() having changed}.
	 *
	 * @see bwfdm.replaydh.ui.config.PreferencesTab#hasPendingChanges()
	 */
	@Override
	public boolean hasPendingChanges() {

		for(PreferencesDelegate<?,?> delegate : delegates) {
			if(delegate.hasChanged()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 *
	 * This implementation returns over the internal list of {@link PreferencesDelegate delegates}
	 * and calls {@link PreferencesDelegate#apply()} on each of them.
	 *
	 * @see bwfdm.replaydh.ui.config.PreferencesTab#apply()
	 */
	@Override
	public TabResult apply() {
		TabResult result = null;

		for(PreferencesDelegate<?,?> delegate : delegates) {
			TabResult tmp = delegate.apply();

			if(result==null || tmp.compareTo(result)>0) {
				result = tmp;
			}
		}

		return result;
	}

	/**
	 * {@inheritDoc}
	 *
	 * This implementation returns over the internal list of {@link PreferencesDelegate delegates}
	 * and calls {@link PreferencesDelegate#update()} on each of them.
	 *
	 * @see bwfdm.replaydh.ui.config.PreferencesTab#update()
	 */
	@Override
	public void update() {
		for(PreferencesDelegate<?,?> delegate : delegates) {
			delegate.update();
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * This implementation returns over the internal list of {@link PreferencesDelegate delegates}
	 * and calls {@link PreferencesDelegate#reset()} on each of them.
	 *
	 * @see bwfdm.replaydh.ui.config.PreferencesTab#resetDefaults()
	 */
	@Override
	public void resetDefaults() {
		for(PreferencesDelegate<?,?> delegate : delegates) {
			delegate.reset();
		}
	}

	@Override
	public void addChangeListener(ChangeListener listener) {
		changeSource.addChangeListener(listener);
	}

	@Override
	public void removeChangeListener(ChangeListener listener) {
		changeSource.removeChangeListener(listener);
	}

	/**
	 * {@inheritDoc}
	 *
	 * This implementation only removes the internal change bouncer
	 * from all delegates by {@link PreferencesDelegate#setChangeBouncer(Consumer) setting}
	 * it to {@code null}.
	 *
	 * @see bwfdm.replaydh.ui.config.PreferencesTab#close()
	 */
	@Override
	public void close() {
		for(PreferencesDelegate<?, ?> delegate : delegates) {
			delegate.setChangeBouncer(null);
		}
	}
}

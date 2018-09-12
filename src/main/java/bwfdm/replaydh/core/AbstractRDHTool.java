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
package bwfdm.replaydh.core;

import static bwfdm.replaydh.utils.RDHUtils.checkState;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

import bwfdm.replaydh.stats.StatEntry;

/**
 * @author Markus Gärtner
 *
 */
public abstract class AbstractRDHTool implements RDHTool {

	private transient Reference<RDHEnvironment> environment;

	private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

	private final AtomicBoolean started = new AtomicBoolean(false);

	protected final PropertyChangeSupport getPropertyChangeSupport() {
		return changeSupport;
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(listener);
	}

	@Override
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		changeSupport.addPropertyChangeListener(propertyName, listener);
	}

	@Override
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		changeSupport.removePropertyChangeListener(propertyName, listener);
	}

	/**
	 * Subclasses should call {@code super.start()} <b>before</b>
	 * any custom setup work.
	 *
	 * @see bwfdm.replaydh.core.RDHTool#start(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public boolean start(RDHEnvironment environment) throws RDHLifecycleException {
		if(!started.compareAndSet(false, true))
			throw new RDHException("Cannot start already started tool "+getClass().getSimpleName());

		this.environment = new WeakReference<>(environment);
		return true;
	}

	/**
	 * Subclasses should call {@code super.stop()} <b>after</b>
	 * any custom cleanup work.
	 *
	 * @see bwfdm.replaydh.core.RDHTool#stop(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public void stop(RDHEnvironment environment) throws RDHLifecycleException {
		if(!started.compareAndSet(true, false))
			throw new RDHException("Cannot stop incactive tool "+getClass().getSimpleName());

		this.environment = null;
	}

	protected void logStat(StatEntry entry) {
		getEnvironment().getClient().getStatLog().log(entry);
	}

	protected boolean isStarted() {
		return started.get();
	}

	protected void checkStarted() {
		checkState("Tool "+getClass().getSimpleName()+" not started", isStarted());
	}

	private RDHEnvironment getEnvironment0() {
		Reference<RDHEnvironment> ref = this.environment;
		return ref==null ? null : ref.get();
	}

	protected final RDHEnvironment getEnvironment() {
		RDHEnvironment environment = getEnvironment0();

		if(environment==null)
			throw new RDHException("No environment available - "+getClass().getName());

		return environment;
	}

	protected final boolean hasEnvironment() {
		return getEnvironment0()!=null;
	}

	protected final boolean isVerbose() {
		RDHEnvironment environment = getEnvironment0();

		return environment==null ? false : environment.getClient().isVerbose();
	}
}

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
package bwfdm.replaydh.utils;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Markus Gärtner
 *
 */
public class Transaction {

	public static Transaction withBeginCallback(Runnable callbackBegin) {
		return new Transaction(callbackBegin, null);
	}

	public static Transaction withEndCallback(Runnable callbackEnd) {
		return new Transaction(null, callbackEnd);
	}

	public static Transaction withCallbacks(Runnable callbackBegin, Runnable callbackEnd) {
		return new Transaction(callbackBegin, callbackEnd);
	}

	public static Transaction forTracking() {
		return new Transaction();
	}

	private final AtomicInteger updateLevel = new AtomicInteger(0);
	private final AtomicBoolean endingUpdate = new AtomicBoolean(false);
	private final AtomicBoolean beginningUpdate = new AtomicBoolean(false);

	/**
	 * Action to be executed when transaction finishes.
	 * <p>
	 * We abuse {@link Runnable} here since it makes not much sense to
	 * define a new functional interface with the exact same signature
	 * for this task.
	 */
	private final Runnable callbackBegin;

	private final Runnable callbackEnd;

	private final Object callbackLock = new Object();

	private Transaction() {
		callbackBegin = callbackEnd = null;
	}

	private Transaction(Runnable callbackBegin, Runnable callbackEnd) {
		checkArgument("At least one callback must be non-null", callbackBegin!=null || callbackEnd!=null);

		this.callbackBegin = callbackBegin;
		this.callbackEnd = callbackEnd;
	}

	/**
	 * Returns whether there's an active transaction in progress.
	 * This is the case if either the current update level is greater
	 * that {@code 0} or the transaction is currently performing the
	 * maintenance work at the end of an update cycle.
	 *
	 * @return
	 */
	public boolean isTransactionInProgress() {
		return updateLevel.get()>0 || endingUpdate.get();
	}

	public void beginUpdate() {
		int level = updateLevel.getAndIncrement();
		boolean shouldBegin = level==0;
		boolean canBegin = shouldBegin && beginningUpdate.compareAndSet(false, true);

		try {
			if (canBegin && callbackBegin!=null) {
				synchronized (callbackLock) {
					callbackBegin.run();
				}
			}
		} finally {
			beginningUpdate.set(false);
		}
	}

	public void endUpdate() {
		int level = updateLevel.getAndDecrement();
		boolean shouldEnd = level==1;
		boolean canEnd = shouldEnd && endingUpdate.compareAndSet(false, true);

		try {
			if (canEnd && callbackEnd!=null) {
				synchronized (callbackLock) {
					callbackEnd.run();
				}
			}
		} finally {
			endingUpdate.set(false);
		}
	}
}

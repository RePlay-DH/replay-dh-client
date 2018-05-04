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
package bwfdm.replaydh.metadata.basic;

import java.util.concurrent.atomic.AtomicInteger;

import bwfdm.replaydh.utils.UsageAware;

/**
 * Adds {@link UsageAware usage tracking} to the {@link DefaultMetadataRecord} implementation.
 *
 * @author Markus Gärtner
 *
 */
public class UsageAwareMetadataRecord extends DefaultMetadataRecord implements UsageAware {

	private AtomicInteger useCounter = new AtomicInteger(0);
	private boolean used = false;

	public UsageAwareMetadataRecord(UID uid) {
		super(uid);
	}

	/**
	 * @see bwfdm.replaydh.utils.UsageAware#incrementUseCounter()
	 */
	@Override
	public boolean incrementUseCounter() {
		used = true;
		return useCounter.getAndIncrement()==0;
	}

	/**
	 * @see bwfdm.replaydh.utils.UsageAware#decrementUseCounter()
	 */
	@Override
	public boolean decrementUseCounter() {
		return useCounter.decrementAndGet()==0;
	}

	/**
	 * @see bwfdm.replaydh.utils.UsageAware#inUse()
	 */
	@Override
	public boolean inUse() {
		return useCounter.get()>0;
	}

	/**
	 * @see bwfdm.replaydh.utils.UsageAware#hasBeenUsed()
	 */
	@Override
	public boolean hasBeenUsed() {
		return used;
	}

}

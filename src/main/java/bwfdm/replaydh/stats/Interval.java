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
package bwfdm.replaydh.stats;

import static bwfdm.replaydh.utils.RDHUtils.checkState;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * @author Markus Gärtner
 *
 */
public class Interval {

	private LocalDateTime begin, end;

	public Interval start() {
		checkState("Already started", begin==null);
		begin = LocalDateTime.now();
		return this;
	}

	public Interval stop() {
		checkState("Not started", begin!=null);
		checkState("Already finished", end==null);
		end = LocalDateTime.now();
		return this;
	}

	public Duration getDuration() {
		checkState("Not finished", begin!=null && end!=null);
		return Duration.between(begin, end);
	}

	public String asDurationString() {
		Duration duration = getDuration();
		long sec = duration.getSeconds();
		int nano = duration.getNano();

		checkState("Negative duration", sec>=0);

		if(nano>0) {
			sec++;
		}

		return String.valueOf(sec);
	}

	public void reset() {
		begin = end = null;
	}
}

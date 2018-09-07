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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.core.AbstractRDHTool;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.io.resources.IOResource;

/**
 * @author Markus Gärtner
 *
 */
public class StatLog extends AbstractRDHTool {

	private static final Logger log = LoggerFactory.getLogger(StatLog.class);

	private final IOResource logFile;

	private final DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL);

	private Writer writer;

	/**
	 * Used to synchronize the I/O operations
	 */
	private final Object lock = new Object();

	private boolean active;

	public StatLog(IOResource logFile) {
		this.logFile = requireNonNull(logFile);
	}

	/**
	 * @see bwfdm.replaydh.core.AbstractRDHTool#start(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public boolean start(RDHEnvironment environment) throws RDHLifecycleException {
		if(!super.start(environment)) {
			return false;
		}

		synchronized (lock) {
			active = environment.getBoolean(RDHProperty.CLIENT_COLLECT_STATS);
		}

		return true;
	}

	/**
	 * @see bwfdm.replaydh.core.AbstractRDHTool#stop(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public void stop(RDHEnvironment environment) throws RDHLifecycleException {

		synchronized(lock) {
			active = false;

			if(writer!=null) {
				IOUtils.closeQuietly(writer);
				writer = null;
			}
		}

		super.stop(environment);
	}

	public void log(StatEntry entry) {
		checkStarted();

		if(active) {
			logImpl(entry);
		}
	}

	public void log(Supplier<? extends StatEntry> source) {
		checkStarted();

		if(active) {
			logImpl(source.get());
		}
	}

	/**
	 * Will only ever be called if {@link #active} is {@code true}.
	 * @param entry
	 */
	private void logImpl(StatEntry entry) {
		synchronized (lock) {
			ensureWriter();

			try {
				writeEntry(entry);
			} catch (IOException e) {
				throw new RDHException("Failed to write to stats file: "+logFile.getPath(), e);
			}
		}
	}

	private void ensureWriter() {
		if(writer==null) {
			try {
				writer = Channels.newWriter(
						logFile.getWriteChannel(false),
						StandardCharsets.UTF_8.newEncoder(),
						IOUtils.BUFFER_LENGTH);
			} catch (IOException e) {
				throw new RDHException("Unable to open writer for stats file: "+logFile.getPath(), e);
			}
		}
	}

	private static final String LB = "\r\n";
	private static final String SEP = ",";

	private void writeEntry(StatEntry entry) throws IOException {
		writer.append(LB)
			.append(formatter.format(entry.getDateTime()))
			.append(SEP)
			.append(entry.getType().name())
			.append(SEP)
			.append(entry.getLabel());

		for(String item : entry.getData()) {
			writer.append(SEP).append(item);
		}
	}
}

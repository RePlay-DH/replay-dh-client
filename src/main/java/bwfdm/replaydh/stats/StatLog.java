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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.core.AbstractRDHTool;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.io.resources.IOResource;
import bwfdm.replaydh.ui.GuiUtils;

/**
 * @author Markus Gärtner
 *
 */
public class StatLog extends AbstractRDHTool {

	private static final Logger log = LoggerFactory.getLogger(StatLog.class);

	private final IOResource logFile;

	private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu.MM.dd HH:mm:ss.SSS", Locale.GERMAN);

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
			closeWriter();
		}

		super.stop(environment);
	}

	private void closeWriter() {
		if(writer!=null) {
			IOUtils.closeQuietly(writer);
			writer = null;
		}
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

	public boolean export(IOResource destination) throws IOException {
		GuiUtils.checkNotEDT();

		synchronized (lock) {

			// Create a unique marker entry for later assembly of incremental exports
			logImpl(StatEntry.withData(StatType.INTERNAL_ACTION,
					StatConstants.EXPORT, UUID.randomUUID().toString()));

			// Kind of a double-wrapping, but simplier to write it that way
			try(InputStream in = Channels.newInputStream(logFile.getReadChannel());
					OutputStream out = Channels.newOutputStream(destination.getWriteChannel(true))) {
				IOUtils.copyStream(in, out);
			}

			return true;
		}
	}

	public void reset() throws IOException {
		synchronized (lock) {
			closeWriter();

			logFile.delete();
		}
	}

	/**
	 * Will only ever be called if {@link #active} is {@code true}.
	 * @param entry
	 */
	private void logImpl(final StatEntry entry) {
		// Pass execution over to background thread in case we're on the EDT
		if(SwingUtilities.isEventDispatchThread()) {
			//TODO this might cause issues with mixed up chronological order?
			getEnvironment().execute(() -> logImpl(entry));
			return;
		}

		synchronized (lock) {
			ensureWriter();

			boolean failed = false;

			try {
				writeEntry(entry, writer);
				writer.flush();
			} catch (IOException e) {
				failed = true;
				throw new RDHException("Failed to write to stats file: "+logFile.getPath(), e);
			} finally {
				if(log.isDebugEnabled()) {
					log.debug("Logging usage stat - write {}: [{}]",
							failed ? "failed" : "successful",
							entryToString(entry));
				}
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

	private void writeEntry(StatEntry entry, Appendable out) throws IOException {
		out.append(LB)
			.append(formatter.format(entry.getDateTime()))
			.append(SEP)
			.append(entry.getType().name())
			.append(SEP)
			.append(entry.getLabel());

		for(String item : entry.getData()) {
			out.append(SEP).append(item);
		}
	}

	private String entryToString(StatEntry entry) {
		StringWriter writer = new StringWriter();
		try {
			writeEntry(entry, writer);
		} catch (IOException e) {
			throw new InternalError("Impossible I/O issue", e);
		}
		return writer.toString();
	}
}

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
package bwfdm.replaydh.io.resources;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;

import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.io.MemoryByteStorage;
import bwfdm.replaydh.utils.AccessMode;

/**
 * @author Markus Gärtner
 *
 */
public class VirtualIOResource implements IOResource {

	private final Path path;

	private MemoryByteStorage buffer;

	private static MemoryByteStorage createMemoryByteStorage(int bufferSize) {
		return bufferSize<0 ? new MemoryByteStorage() : new MemoryByteStorage(bufferSize);
	}

	public VirtualIOResource(Path path) {
		this(path, -1);
	}

	public VirtualIOResource(Path path, int bufferSize) {
		this(path, createMemoryByteStorage(bufferSize));
	}

	public VirtualIOResource(Path path, MemoryByteStorage buffer) {
		this.path = path;
		this.buffer = requireNonNull(buffer);
	}

	/**
	 * @see bwfdm.replaydh.io.resources.IOResource#getPath()
	 */
	@Override
	public Path getPath() {
		return path;
	}

	public MemoryByteStorage getBuffer() {
		return buffer;
	}

	/**
	 * @see de.ims.icarus2.model.api.io.resources.IOResource#getAccessMode()
	 */
	@Override
	public AccessMode getAccessMode() {
		return AccessMode.READ_WRITE;
	}

	protected void checkOpen() {
		if(buffer==null)
			throw new RDHException("Buffer not prepared or already deleted");
	}

	/**
	 * @see de.ims.icarus2.model.api.io.resources.IOResource#getWriteChannel()
	 */
	@Override
	public SeekableByteChannel getWriteChannel(boolean truncate) throws IOException {
		checkOpen();

		SeekableByteChannel channel = buffer.newChannel();

		if(truncate) {
			channel.truncate(0);
		} else {
			channel.position(channel.size());
		}

		return channel;
	}

	/**
	 * @see de.ims.icarus2.model.api.io.resources.IOResource#getReadChannel()
	 */
	@Override
	public SeekableByteChannel getReadChannel() throws IOException {
		checkOpen();

		return buffer.newChannel();
	}

	/**
	 * @see de.ims.icarus2.model.api.io.resources.IOResource#delete()
	 */
	@Override
	public void delete() {
		if(buffer!=null) {
			buffer.close();
		}
		buffer = null;
	}

	/**
	 * @see de.ims.icarus2.model.api.io.resources.IOResource#prepare()
	 */
	@Override
	public void prepare() throws IOException {
		// no-op
	}

	/**
	 * @see de.ims.icarus2.model.api.io.resources.IOResource#size()
	 */
	@Override
	public long size() throws IOException {
		checkOpen();

		return buffer.size();
	}
}

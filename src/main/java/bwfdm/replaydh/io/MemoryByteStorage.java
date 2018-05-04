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
package bwfdm.replaydh.io;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements a byte storage based on a backing byte array.
 * The size of the storage is dynamically adjusted when {@link #write(int, ByteBuffer) write}
 * operations occur (in case the initial capacity is insufficient). Manipulation of data in
 * this buffer can either be done manually or by requesting a {@link SeekableByteChannel} implementation
 * that operates directly on this buffer's byte array. Note that this class as well as all
 * {@link #newChannel() byte channels} created by it are thread safe.
 *
 * @author Markus Gärtner
 *
 */
public class MemoryByteStorage implements AutoCloseable {

	private byte[] buffer;

	private static final long INT_LIMIT = (long) Integer.MAX_VALUE;

	private final Object bufferLock = new Object();
	private int size = 0;

	private volatile boolean open = true;

	public MemoryByteStorage() {
		this(8000);
	}

	public MemoryByteStorage(int size) {
		if(size<0)
			throw new IllegalArgumentException("Negative buffer size"); //$NON-NLS-1$

		buffer = new byte[size];
	}

	public SeekableByteChannel newChannel() {
		return this.new SlaveChannel();
	}

	public boolean isOpen() {
		return open;
	}

	/**
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() {
		open = false;
	}

	/**
	 * Returns the current size of the buffer, i.e. the number of bytes currently
	 * written to the underlying byte array.
	 * @return
	 */
	public int size() {
		synchronized (bufferLock) {
			return size;
		}
	}

	public int read(int position, ByteBuffer dst) throws IOException {
		return read0(position, dst, null);
	}

	private int read0(int position, ByteBuffer dst, AtomicInteger pointer) throws IOException {
		requireNonNull(dst);

		synchronized (bufferLock) {
			int size = size();

			if(pointer!=null) {
				position = pointer.get();
				if(position>=size) {
					return -1;
				}
			}

			if(position>=size || position<0)
				throw new IndexOutOfBoundsException("Position out of bounds(0 to "+(size-1)+": "+position); //$NON-NLS-1$ //$NON-NLS-2$

			int bytesToRead = Math.min(dst.remaining(), size-position);

			if(bytesToRead==0) {
				return -1;
			}

			dst.put(buffer, position, bytesToRead);

			if(pointer!=null) {
				pointer.set(position+bytesToRead);
			}

			return bytesToRead;
		}
	}

	public int write(int position, ByteBuffer src) throws IOException {
		return write0(position, src, null);
	}

	private int write0(int position, ByteBuffer src, AtomicInteger pointer) throws IOException {
		requireNonNull(src);

		synchronized (bufferLock) {

			if(pointer!=null) {
				position = pointer.get();
			}

			int bytesToWrite = Math.min(Integer.MAX_VALUE-position, src.remaining());

			ensureCapacity(position+bytesToWrite);

			src.get(buffer, position, bytesToWrite);

			size = Math.max(size, position+bytesToWrite);

			if(pointer!=null) {
				pointer.set(position+bytesToWrite);
			}

			return bytesToWrite;
		}

	}

	public void truncate(int size) {
		if(size<0)
			throw new IllegalArgumentException("Negative size: "+size); //$NON-NLS-1$

		synchronized (bufferLock) {
			int currentSize = size();

			if(size>currentSize) {
				return;
			}

			Arrays.fill(buffer, size, currentSize, (byte)0);
			this.size = size;
		}
	}

	// Assumed to be called under bufferLock synchronization
	private void ensureCapacity(int required) {

		int capacity = buffer.length;

		if(required<capacity) {
			return;
		}

		//TODO optimize growth factor
		double growthFactor = 2.0;
		if(capacity>10_000_000) {
			growthFactor = 1.5;
		}

		int newCapacity = Math.max((int)(capacity*growthFactor), required);

		buffer = Arrays.copyOf(buffer, newCapacity);
	}

	private class SlaveChannel implements SeekableByteChannel {

		private AtomicInteger position = new AtomicInteger();
		private AtomicBoolean closed = new AtomicBoolean();

		/**
		 * @see java.nio.channels.Channel#isOpen()
		 */
		@Override
		public boolean isOpen() {
			return MemoryByteStorage.this.open && !closed.get();
		}

		/**
		 * @see java.nio.channels.Channel#close()
		 */
		@Override
		public void close() throws IOException {
			closed.set(true);
		}

		private void checkOpen() throws ClosedChannelException {
			if(!isOpen())
				throw new ClosedChannelException();
		}

		/**
		 * @see java.nio.channels.SeekableByteChannel#read(java.nio.ByteBuffer)
		 */
		@Override
		public int read(ByteBuffer dst) throws IOException {
			requireNonNull(dst);

			checkOpen();

			return MemoryByteStorage.this.read0(-1, dst, position);
		}

		/**
		 * @see java.nio.channels.SeekableByteChannel#write(java.nio.ByteBuffer)
		 */
		@Override
		public int write(ByteBuffer src) throws IOException {
			requireNonNull(src);

			checkOpen();

			return MemoryByteStorage.this.write0(-1, src, position);
		}

		/**
		 * @see java.nio.channels.SeekableByteChannel#position()
		 */
		@Override
		public long position() throws IOException {
			return position.get();
		}

		/**
		 * @see java.nio.channels.SeekableByteChannel#position(long)
		 */
		@Override
		public SeekableByteChannel position(long newPosition) throws IOException {
			checkArgument("Negative position: "+newPosition, newPosition>=0);
			checkArgument("Position exceeds integer limit of buffer array: "+newPosition, newPosition<INT_LIMIT);

			checkOpen();

			position.set((int) newPosition);

			return this;
		}

		/**
		 * @see java.nio.channels.SeekableByteChannel#size()
		 */
		@Override
		public long size() throws IOException {
			return MemoryByteStorage.this.size();
		}

		/**
		 * @see java.nio.channels.SeekableByteChannel#truncate(long)
		 */
		@Override
		public SeekableByteChannel truncate(long size) throws IOException {
			checkArgument("Size exceeds integer limit of buffer array: "+size, size<INT_LIMIT);

			MemoryByteStorage.this.truncate((int) size);

			return this;
		}

	}
}

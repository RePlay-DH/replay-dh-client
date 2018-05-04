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

import java.io.IOException;
import java.net.URL;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;

import bwfdm.replaydh.utils.AccessMode;

/**
 * Models abstract access to an arbitrary byte storage that can be connected to
 * in both read and write mode.
 *
 * @author Markus Gärtner
 *
 */
public interface IOResource {

	/**
	 * Returns the {@link AccessMode} that defines whether this resource supports
	 * read or write operations or both.
	 *
	 * @return
	 */
	AccessMode getAccessMode();

	/**
	 * Opens the resource for writing. The returned channel is expected to be fully
	 * initialized and it will be only used for a single operation or batch of operations
	 * and then {@link AutoCloseable#close() closed} again.
	 * <p>
	 * The {@code truncate} parameter defines whether or not the resource should be
	 * opened so that new content gets appended or if all existing content should be
	 * truncated to a length of zero.
	 *
	 * @return
	 * @throws IOException
	 */
	SeekableByteChannel getWriteChannel(boolean truncate) throws IOException;

	/**
	 * Opens the resource for reading. The returned channel is expected to be fully
	 * initialized and it will be only used for a single operation or batch of operations
	 * and then {@link AutoCloseable#close() closed} again.
	 *
	 * @return
	 * @throws IOException
	 */
	SeekableByteChannel getReadChannel() throws IOException;

	/**
	 * Deletes this resource and all the contained data permanently.
	 *
	 * @throws IOException
	 */
	void delete() throws IOException;

	/**
	 * Initializes the resource so that subsequent calls to fetch {@link #getReadChannel() read}
	 * and {@link #getWriteChannel() write} access to data will not require expensive preparation time.
	 *
	 * @throws IOException
	 */
	void prepare() throws IOException;

	/**
	 * Returns the size in bytes of the data currently stored inside this resource.
	 *
	 * @return
	 * @throws IOException
	 */
	long size() throws IOException;

	/**
	 * Returns the path on the local file system this resource is pointing at.
	 * A return value of {@code null} indicates the resource is not referring to local data
	 * (i.e. it's either based on remote {@link URL} data or just exists virtually in
	 * memory).
	 * <p>
	 * The default implementation returns {@code null}.
	 *
	 * @return
	 */
	Path getPath();
}

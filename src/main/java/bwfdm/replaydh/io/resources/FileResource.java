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
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.utils.AccessMode;


/**
 * @author Markus Gärtner
 *
 */
public final class FileResource extends ReadWriteResource {


	private final Path file;

	public FileResource(Path file) {
		this(file, AccessMode.READ_WRITE);
	}

	public FileResource(Path file, AccessMode accessMode) {
		super(accessMode);

		requireNonNull(file);

		this.file = file;
	}

	@Override
	public String toString() {
		return "FileResource[file="+file+"]";
	}

	@Override
	public SeekableByteChannel getWriteChannel(boolean truncate) throws IOException {
		checkWriteAccess();

		Set<OpenOption> options = new HashSet<>();
		Collections.addAll(options, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
		if(truncate) {
			options.add(StandardOpenOption.TRUNCATE_EXISTING);
		} else {
			options.add(StandardOpenOption.APPEND);
		}

		return Files.newByteChannel(file, options);
	}

	@Override
	public SeekableByteChannel getReadChannel() throws IOException {
		checkReadAccess();

		return Files.newByteChannel(file, StandardOpenOption.READ);
	}

	@Override
	public void delete() throws IOException {
		checkWriteAccess();

		Files.deleteIfExists(file);
	}

	@Override
	public void prepare() throws IOException {

		if(!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
			checkWriteAccess();

			try {
				Files.createFile(file);
			} catch (IOException e) {
				throw new RDHException("Failed to open managed resource", e);
			}
		}

		if(!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
			throw new RDHException("Supplied file is not regular file: "+file);
	}

	@Override
	public long size() throws IOException {
		checkReadAccess();

		return Files.size(file);
	}

	@Override
	public final Path getPath() {
		return file;
	}
}

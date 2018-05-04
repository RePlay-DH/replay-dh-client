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

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;

import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.utils.AccessMode;


/**
 * @author Markus Gärtner
 *
 */
public class ReadOnlyStringResource extends VirtualIOResource {

	private String source;
	private final Charset encoding;

	/**
	 * @param source
	 * @param encoding
	 */
	public ReadOnlyStringResource(String source, Charset encoding) {
		super(null, source.length());

		requireNonNull(encoding);
		checkArgument("Source string must not be empty", !source.isEmpty());

		this.source = source;
		this.encoding = encoding;
	}

	/**
	 * @see de.ims.icarus2.model.api.io.resources.IOResource#getAccessMode()
	 */
	@Override
	public AccessMode getAccessMode() {
		return AccessMode.READ;
	}

	@Override
	protected void checkOpen() {
		super.checkOpen();
		if(source==null || getBuffer().size()<source.length())
			throw new RDHException("Already deleted");
	}

	/**
	 * @see de.ims.icarus2.model.api.io.resources.IOResource#getWriteChannel()
	 */
	@Override
	public SeekableByteChannel getWriteChannel(boolean truncate) throws IOException {
		throw new RDHException("Read only implementation -  backed by constant source string");
	}

	/**
	 * @see de.ims.icarus2.model.api.io.resources.IOResource#delete()
	 */
	@Override
	public void delete() {
		super.delete();
		source = null;
	}

	/**
	 * @see de.ims.icarus2.model.api.io.resources.IOResource#prepare()
	 */
	@Override
	public void prepare() throws IOException {
		if(getBuffer().size()>0) {
			return;
		}

		if(source==null)
			throw new RDHException("Resource already deleted");

		try(Writer writer = Channels.newWriter(getBuffer().newChannel(), encoding.newEncoder(), 32768)) {
			writer.write(source);
		}
	}
}

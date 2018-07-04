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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;

import bwfdm.replaydh.io.resources.IOResource;

/**
 * @author Markus Gärtner
 *
 */
public class IOUtils {

	public static final int BUFFER_LENGTH = 1<<14;

	/**
	 * Kilobyte notation in decimal form
	 */
	public static final long KB = 1000;
	/**
	 * Megabyte notation in decimal form
	 */
	public static final long MB = KB*KB;
	/**
	 * Gigabyte notation in decimal form
	 */
	public static final long GB = MB*KB;

	/**
	 * Creates a human readable textual representation of the given
	 * file size. The result will be of the following format:<br>
	 * {@code xxx <unit>}
	 * where {@code xxx} is the shortened size value as an integer
	 * according to the {@code unit} (KB,MB,GB,...). Note that we use
	 * the decimal convention for calculating the shortened size values,
	 * so {@code 1KB} is equal to {@code 1000} bytes and not {@code 1024}.
	 *
	 * @param size
	 * @return
	 */
	public static String readableSize(long size) {

		if(size<KB) {
			return size+" Bytes";
		} else if(size<MB) {
			return size/KB+" KB";
		} else if(size<GB) {
			return size/MB+" MB";
		} else {
			return size/GB+" GB";
		}
	}

	public static String readableSize(Path file) {
		try {
			long size = Files.size(file);
			return readableSize(size);
		} catch (IOException e) {
			return "-";
		}
	}

	public static long parseSizeFactor(String s) {
		switch (s) {
		case "Bytes": return 1;
		case "KB": return KB;
		case "MB": return MB;
		case "GB": return GB;

		default:
			throw new IllegalArgumentException("Unknown unit value: "+s);
		}
	}

	public static long parseSize(String s) {
		s = s.trim();

		int unitIdx = -1;

		for(int i=0; i<s.length(); i++) {
			if(!Character.isDigit(s.charAt(i))) {
				unitIdx = i;
				break;
			}
		}

		if(unitIdx==-1) {
			return Long.parseLong(s);
		} else {
			long size = Long.parseLong(s.substring(0, unitIdx));
			long factor = parseSizeFactor(s.substring(unitIdx).trim());

			return size * factor;
		}
	}

	public static URL getJarLocation() throws NoSuchFileException {
		CodeSource source = IOUtils.class.getProtectionDomain().getCodeSource();
		if(source==null)
			throw new NoSuchFileException("Unable to determine current code source");

		return source.getLocation();
	}

	public static Path getJarFile() throws NoSuchFileException {

		URL locationURL = IOUtils.getJarLocation();
		Path location;
		try {
			location = Paths.get(locationURL.toURI());
		} catch (URISyntaxException e) {
			throw new NoSuchFileException("Invalid source location URL: "+locationURL);
		}

		return location;
	}

	public static String readResource(IOResource resource, Charset charset) throws IOException {
		StringBuilder sb = new StringBuilder((int) resource.size());
		try(ReadableByteChannel channel = resource.getReadChannel()) {
			try(Reader reader = Channels.newReader(channel, charset.newDecoder(), BUFFER_LENGTH)) {
				CharBuffer cb = CharBuffer.allocate(1024);
				while(reader.read(cb)>0) {
					cb.flip();
					sb.append(cb);
					cb.clear();
				}
			}
		}
		return sb.toString();
	}

	public static String readStream(InputStream input) throws IOException {
		return readStream(input, StandardCharsets.UTF_8);
	}

	public static String readStream(InputStream input, Charset encoding) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copyStream(input, baos);
		input.close();
		return new String(baos.toByteArray(), encoding);
	}

	public static String readStreamUnchecked(InputStream input) {
		return readStreamUnchecked(input, StandardCharsets.UTF_8);
	}

	public static String readStreamUnchecked(InputStream input, Charset encoding) {
		try {
			return readStream(input, encoding);
		} catch (IOException e) {
			// ignore
		}

		return null;
	}

    public static void copyStream(final InputStream in, final OutputStream out) throws IOException {
    	copyStream(in, out, 0);
    }

    public static void copyStream(final InputStream in, final OutputStream out,
            int bufferSize) throws IOException {
    	if(bufferSize==0) {
    		bufferSize = 8000;
    	}
        byte[] buf = new byte[bufferSize];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
    }
}

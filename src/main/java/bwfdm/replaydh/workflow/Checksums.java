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
package bwfdm.replaydh.workflow;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import bwfdm.replaydh.core.RDHException;
import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.io.resources.IOResource;

/**
 * @author Markus Gärtner
 *
 */
public final class Checksums {

	public static enum ChecksumType {

		MD5("MD5"),
		SHA_1("SHA-1"),
		;

		ChecksumType(String type) {
			this.type = requireNonNull(type);
		}

		private final String type;

		public String getAlgorithm() {
			return type;
		}
	}

	/**
	 * If file size exceeds <tt>THRESHOLDS[i]</tt> then the
	 * compressed digest should be taken at <tt>2 << SPLITS[i]</tt>
	 * equidistant locations throughout the file.
	 */
	private static final long[] THRESHOLDS = {
			0,
			10,
			100*IOUtils.MB,
			500*IOUtils.MB,
			IOUtils.GB,
			10*IOUtils.GB,
	};

	public static long MAX_SIZE_BEFORE_SPLIT = THRESHOLDS[1];

	private static final int[] SPLITS = {
			-1, // indicating NO_SPLIT
			4,
			5,
			6,
			8,
			9,
	};

	private static int getSkipSize(long fileSize) {
		int  index = THRESHOLDS.length-1;
		while(fileSize<THRESHOLDS[index]) {
			index--;
		}
		int splitPower = SPLITS[index];
		return splitPower>0 ? 1<<splitPower : -1;
	}

	private static void checkInterrupted() throws InterruptedException {
		if(Thread.interrupted())
			throw new InterruptedException();
	}

	// kept in case we don't want to continue using NIO
    @SuppressWarnings("unused")
	private static byte[] digest(MessageDigest digest, InputStream data) throws IOException, InterruptedException {
        byte[] buffer = new byte[IOUtils.BUFFER_LENGTH];
        int read;

        while ((read = data.read(buffer)) > -1) {
        	checkInterrupted();
            digest.update(buffer, 0, read);
        }

        return digest.digest();
    }

    private static boolean isNativeChannel(Channel channel) {
    	return channel instanceof FileChannel;
    }

    private static byte[] digest(MessageDigest digest, ReadableByteChannel data) throws IOException, InterruptedException {
        ByteBuffer bb = isNativeChannel(data) ?
        		ByteBuffer.allocate(IOUtils.BUFFER_LENGTH)
        		: ByteBuffer.allocateDirect(IOUtils.BUFFER_LENGTH);

        while (data.read(bb) > -1) {
        	checkInterrupted();

        	bb.flip();
            digest.update(bb);
            bb.clear();
        }

        return digest.digest();
    }

    private static byte[] digestPartially(MessageDigest digest, SeekableByteChannel data, int skipSize) throws IOException, InterruptedException {
        ByteBuffer bb = isNativeChannel(data) ?
        		ByteBuffer.allocate(IOUtils.BUFFER_LENGTH)
        		: ByteBuffer.allocateDirect(IOUtils.BUFFER_LENGTH);

        long size = data.size();
        while (data.read(bb) > -1) {
        	checkInterrupted();

        	bb.flip();
            digest.update(bb);
            bb.clear();

            long nextChunk = data.position()+skipSize;
            if(nextChunk>=size) {
            	break;
            }

            data.position(nextChunk);
        }

        return digest.digest();
    }

    private static MessageDigest getDigest(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new RDHException(e.getMessage());
        }
    }

    private static byte[] combine(byte[] first, byte[] second) {
    	byte[] result = Arrays.copyOf(first, first.length+second.length);
    	System.arraycopy(second, 0, result, first.length, second.length);
    	return result;
    }

    private static byte[] split(byte[] source, boolean first) {
    	checkArgument("Payload size must be a power of 2", source.length%2==0);
    	int splitSize = source.length>>1;
		return first ?
				Arrays.copyOf(source, splitSize)
				: Arrays.copyOfRange(source, splitSize, source.length);
    }

    /**
     * Computes a complete checksum for the specified file.
     *
     *
     * @param data
     * @param type
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
	public static Checksum createChecksum(IOResource data, ChecksumType type) throws IOException, InterruptedException {
		MessageDigest digest = getDigest(type.getAlgorithm());
		long size = data.size();
		int skipSize = getSkipSize(size);

		byte[] payload = null;
		byte[] summary = null;

		// For very large files compute a "summary" digest from small chunks
		if(skipSize>0) {
			try(SeekableByteChannel channel = data.getReadChannel()) {
				summary = digestPartially(digest, channel, skipSize);
			}
		}

		// Now do a complete checksum
		try(ByteChannel channel = data.getReadChannel()) {
			payload = digest(digest, channel);
		}

		if(summary!=null) {
			payload = combine(payload, summary);
		}

		return new Checksum(type.getAlgorithm(), size, payload);
	}

	public static enum ChecksumValidationResult {
		VALID(true),
		SIZE_MISMATCH(false),
		CONTENT_CHANGED(false),
		;

		private final boolean valid;

		ChecksumValidationResult(boolean valid) {
			this.valid = valid;
		}

		public boolean isValid() {
			return valid;
		}
	}

	/**
	 * Validates that the given checksum still accurately describes the specified
	 * file.
	 *
	 * @param data
	 * @param checksum
	 * @return
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static ChecksumValidationResult validateChecksum(IOResource data, Checksum checksum) throws IOException, InterruptedException {

		// Easiest check: currentSize==expectedSize
		long size = data.size();
		if(size!=checksum.getSize()) {
			return ChecksumValidationResult.SIZE_MISMATCH;
		}

		// Here starts the computationally expensive part
		MessageDigest digest = getDigest(checksum.getType());

		// Remember: file size is already confirmed unchanged
		int skipSize = getSkipSize(size);

		byte[] expectedPayload = checksum.getPayload();

		// If possible use the summary digest for a quicker check
		if(skipSize>0) {
			byte[] expectedSummary = split(expectedPayload, false);
			expectedPayload = split(expectedPayload, true);
			try(SeekableByteChannel channel = data.getReadChannel()) {
				byte[] summary = digestPartially(digest, channel, skipSize);

				if(!Arrays.equals(expectedSummary, summary)) {
					return ChecksumValidationResult.CONTENT_CHANGED;
				}
			}
		}

		// All simple checks didn't work out, so run a complete checksum generation
		try(ByteChannel channel = data.getReadChannel()) {
			byte[] payload = digest(digest, channel);

			if(!Arrays.equals(expectedPayload, payload)) {
				return ChecksumValidationResult.CONTENT_CHANGED;
			}
		}

		return ChecksumValidationResult.VALID;
	}

	// No instantiation for utility class
	private Checksums() {
		// no -op
	}
}

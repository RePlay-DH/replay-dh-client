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
package bwfdm.replaydh.test.workflow;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.io.resources.IOResource;
import bwfdm.replaydh.io.resources.VirtualIOResource;
import bwfdm.replaydh.workflow.Checksum;
import bwfdm.replaydh.workflow.Checksums;
import bwfdm.replaydh.workflow.Checksums.ChecksumType;
import bwfdm.replaydh.workflow.Checksums.ChecksumValidationResult;

/**
 * @author Markus Gärtner
 *
 */
@RunWith(Parameterized.class)
public class ChecksumsTest {

	private static Random random = new Random();

	/**
	 * Creates a resource of specified size and fills it with completely
	 * random bytes.
	 */
	private static IOResource createTestResource(int bytes) throws IOException {
		VirtualIOResource resource = new VirtualIOResource(null, bytes);

		byte[] b = new byte[Math.min(1024, bytes)];
		ByteBuffer bb = ByteBuffer.wrap(b);

		try(ByteChannel channel = resource.getWriteChannel(true)) {

			while(bytes>0) {
				bb.clear();
				random.nextBytes(b);

				bb.limit(Math.min(bb.capacity(), bytes));

				bytes -= channel.write(bb);
			}

		}

		return resource;
	}

	private static void testChecksum(ChecksumType checksumType, int bytes) throws IOException, InterruptedException {
		IOResource resource = createTestResource(bytes);
		assertEquals(bytes, resource.size());

		Checksum checksum = Checksums.createChecksum(resource, checksumType);
		assertNotNull(checksum);
		assertEquals(checksumType.getAlgorithm(), checksum.getType());
		assertEquals(bytes, checksum.getSize());

		assertEquals(ChecksumValidationResult.VALID, Checksums.validateChecksum(resource, checksum));


	}

    @Parameters(name = "{index}: type={0}")
    public static Object[] data() {

        return ChecksumType.values();
    }

	private final ChecksumType checksumType;

	public ChecksumsTest(ChecksumType checksumType) {
		this.checksumType = requireNonNull(checksumType);
	}

	@Test
	public void testUnsplit() throws Exception {
		testChecksum(checksumType, (int) (Checksums.MAX_SIZE_BEFORE_SPLIT-1));
	}

	@Test
	public void testSmallFile() throws Exception {
		testChecksum(checksumType, (int) (512*IOUtils.KB));
	}

	@Test
	public void testMediumFile() throws Exception {
		testChecksum(checksumType, (int) (128*IOUtils.MB));
	}
}

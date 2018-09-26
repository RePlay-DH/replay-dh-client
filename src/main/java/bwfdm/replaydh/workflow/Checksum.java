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

import java.util.Arrays;
import java.util.Objects;

import bwfdm.replaydh.utils.RDHUtils;

/**
 * Models a checksum usable for consistency checking
 *
 * @author Markus
 */
public final class Checksum {

	/**
	 * Checksum algorithm used
	 */
	private final String type;

	/**
	 * Physical size of resource in bytes
	 */
	private final long size;

	/**
	 * Actual checksum. Size can vary based on algorithm used.
	 */
	private final byte[] payload;

	/**
	 * Worst minimalistic checksum scenario:
	 * Use last modification date and to bitwise XOR with size
	 * of file and store both long values as 4 bytes each.
	 */
	public static final int MIN_PAYLOAD_LENGTH = 8;

	public Checksum(String type, long size, byte[] payload) {
		requireNonNull(type);
		requireNonNull(payload);
		checkArgument("Size must not be negative", size>=0);
		checkArgument("Payload empty or too small", payload.length>=MIN_PAYLOAD_LENGTH);

		this.type = type;
		this.size = size;
		this.payload = payload;
	}

	public Checksum(String type, long size, String payload) {
		requireNonNull(type);
		checkArgument("Size must not be negative", size>=0);
		requireNonNull(payload);

		this.type = type;
		this.size = size;
		this.payload = RDHUtils.parseHexBinary(payload);
		checkArgument("Payload empty or too small", this.payload.length>=MIN_PAYLOAD_LENGTH);
	}

	public String getType() {
		return type;
	}

	public long getSize() {
		return size;
	}

	public byte[] getPayload() {
		return payload;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append(getType());
		sb.append('#');
		sb.append(Long.toHexString(size));
		sb.append('#');
		sb.append(RDHUtils.printHexBinary(getPayload()));

		return sb.toString();
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return (int) (type.hashCode() * size * Arrays.hashCode(payload));
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj==this) {
			return true;
		} else if(obj instanceof Checksum) {
			Checksum other = (Checksum) obj;
			return Objects.equals(type, other.type)
					&& size==other.size
					&& Arrays.equals(payload, other.payload);
		}
		return false;
	}

	public static Checksum parse(String s) {
		requireNonNull(s);
		checkArgument("Cannot parse empty string", !s.isEmpty());

		String[] splits = s.split("#");
		checkArgument("Invalid checksum format, missing '#' separator symbol", splits.length==3);

		String type = splits[0];
		long size = Long.parseUnsignedLong(splits[1], 16);
		byte[] payload = RDHUtils.parseHexBinary(splits[2]);

		return new Checksum(type, size, payload);
	}
}

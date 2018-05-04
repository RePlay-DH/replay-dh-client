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
package bwfdm.replaydh.workflow.export.dspace.dto.v6;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 
 * @author vk
 */
public class MetadataConstants {

	// DataCite minimum fields:
	//
	// - Identifier
	// - Creator
	// - Title
	// - Publisher
	// - Publication Year
	// - Resource Type

	public static String ITEM_METADATA_EXAMPLE = "[" + "{" + "\"key\"" + ":"
			+ "\"dc.identifier.doi\"" + ","//
			+ "\"value\"" + ":" + "\"test-doi\"" + ","//
			+ "\"language\"" + ":" + "\"null\""//
			+ "}" + ","//
			+ "{" + "\"key\"" + ":" + "\"dc.contributor.author\"" + ","//
			+ "\"value\"" + ":" + "\"test-contributor\"" + ","//
			+ "\"language\"" + ":" + "\"null\""//
			+ "}" + ","//
			+ "{" + "\"key\"" + ":" + "\"dc.title\"" + ","//
			+ "\"value\"" + ":" + "\"test-title\"" + ","//
			+ "\"language\"" + ":" + "\"null\""//
			+ "}" + ","//
			+ "{" + "\"key\"" + ":" + "\"dc.publisher\"" + ","//
			+ "\"value\"" + ":" + "\"test-publisher\"" + ","//
			+ "\"language\"" + ":" + "\"null\""//
			+ "}" + ","//
			+ "{" + "\"key\"" + ":" + "\"dc.date.issued\"" + ","//
			+ "\"value\"" + ":" + "\"test-title\"" + ","//
			+ "\"language\"" + ":" + "\"null\""//
			+ "}" + ","//
			+ "{" + "\"key\"" + ":" + "\"dc.type\"" + ","//
			+ "\"value\"" + ":" + "\"test-type\"" + ","//
			+ "\"language\"" + ":" + "\"null\""//
			+ "}" + ","//
			+ "{" + "\"key\"" + ":" + "\"uulm.typeDCMI\"" + ","//
			+ "\"value\"" + ":" + "\"test-type-UULM\"" + ","//
			+ "\"language\"" + ":" + "\"null\""//
			+ "}"//
			+ "]";

	public static String ITEM_BITSTREAM_DESCRIPTION_EXAMPLE = "{" + "\"name\":"
			+ "\"" + "test-bitstream" + "\"" + "}";

	static {
		ITEM_METADATA_EXAMPLE = read("item_metadata_example.json");
		ITEM_BITSTREAM_DESCRIPTION_EXAMPLE = read("item_bitstream_description_example.json");
	}

	private static String read(final String name) {
		final BufferedReader br = new BufferedReader(new InputStreamReader(
				MetadataConstants.class.getResourceAsStream(name)));
		final StringBuilder text = new StringBuilder();
		String line;
		try {
			while ((line = br.readLine()) != null)
				text.append(line);
			br.close();
		} catch (final IOException e) {
			// IO error to a resource really shouldn't happen...
			throw new RuntimeException(e);
		}
		return text.toString();
	}
}

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
package bwfdm.replaydh.utils;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * @author Markus Gärtner
 *
 */
@RunWith(Parameterized.class)
public class StringWrapperTest {
    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                 { "", "", 1 },
                 { "xx", "xx", 1 },
                 { "x x", "x\nx", 1 },
                 { "\n", "\n", 1 },
                 { "\nx x", "\nx\nx", 1 },
                 { "xxxx xxxx", "xxxx xxxx", 10 },
                 { "xxxx xxxx xxxx", "xxxx xxxx\nxxxx", 10 },
                 //TODO test with extra spaces around the breakpoint
           });
    }

    private String input;
    private String expected;
    private int limit;

    public StringWrapperTest(String input, String expected, int limit) {
    	this.input = requireNonNull(input);
    	this.expected = requireNonNull(expected);
    	this.limit = limit;
    }

	/**
	 * Test method for {@link bwfdm.replaydh.utils.StringWrapper#wrap(java.lang.String)}.
	 */
	@Test
	public void testWrap() {
		String wrapped = new StringWrapper()
				.limit(limit)
				.linebreak("\n")
				.wrap(input);

		assertEquals(expected, wrapped);
	}

}

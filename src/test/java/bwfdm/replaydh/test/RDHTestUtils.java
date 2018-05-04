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
package bwfdm.replaydh.test;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.util.Set;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.utils.ClassUtils;
import bwfdm.replaydh.utils.ClassUtils.Trace;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;

/**
 * @author Markus Gärtner
 *
 */
public class RDHTestUtils {

	public static RDHEnvironment createTestEnvironment() {
		RDHEnvironment environment = mock(RDHEnvironment.class);

		return environment;
	}

	public static void assertDeepEqual(String msg, Object expected, Object actual, String serializedForm) throws IllegalAccessException {

		// Collect all the differences
		Trace trace = ClassUtils.deepDiff(expected, actual);

		if(trace.hasMessages()) {
			failForTrace(msg, trace, expected, serializedForm);
		}
	}

	public static void assertDeepNotEqual(String msg, Object expected, Object actual) throws IllegalAccessException {

		// Collect all the differences
		Trace trace = ClassUtils.deepDiff(expected, actual);

		if(!trace.hasMessages()) {
			failForEqual(msg, expected, actual);
		}
	}

	private static String getId(Object obj) {
		String id = null;

		if(obj instanceof Identifiable) {
			Set<Identifier> identifiers = ((Identifiable)obj).getIdentifiers();
			if(!identifiers.isEmpty()) {
				Identifier identifier = identifiers.iterator().next();
				id = identifier.getType()+"@"+identifier.getId();
			}
		}

		if(id==null) {
			id = obj.getClass()+"@<unnamed>"; //$NON-NLS-1$
		}
		return id;
	}


	private static void failForEqual(String msg, Object original, Object created) {
		String message = "Expected result of deserialization to be different from original"; //$NON-NLS-1$
		if(msg!=null) {
			message = msg+": "+message; //$NON-NLS-1$
		}
		fail(message);
	}

	private static void failForTrace(String msg, Trace trace, Object obj, String xml) {
		String message = getId(obj);

		message += " result of deserialization is different from original: \n"; //$NON-NLS-1$
		message += trace.getMessages();
		message += " {serialized form: "+xml.replaceAll("\\s{2,}", "")+"}"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		if(msg!=null) {
			message = msg+": "+message; //$NON-NLS-1$
		}

		fail(message);
	}
}

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
package bwfdm.replaydh.test.ui.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import bwfdm.replaydh.core.RDHClient;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.ui.actions.ConditionResolver;
import bwfdm.replaydh.ui.core.RDHConditionResolver;
import bwfdm.replaydh.ui.core.RDHConditionResolver.Global;

/**
 * @author Markus Gärtner
 *
 */
public class RDHConditionResolverTest {

	private ConditionResolver resolver;
	private RDHEnvironment environment;
	private RDHClient client;

	@Before
	public void setup() {
		client = mock(RDHClient.class);
		environment = mock(RDHEnvironment.class);
		when(environment.getClient()).thenReturn(client);

		resolver = new RDHConditionResolver(environment);
	}

	@Test
	public void testGlobal() throws Exception {
		when(client.isDevMode()).thenReturn(true);
		assertTrue(resolver.resolveGlobal(Global.DEBUG.getKey()));

		when(client.isDevMode()).thenReturn(false);
		assertFalse(resolver.resolveGlobal(Global.DEBUG.getKey()));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testGlobalFail() throws Exception {
		resolver.resolveGlobal("xxxx");
	}

	@Test
	public void testSettingsBasic() throws Exception {
		String key = "test123";

		when(environment.getProperty(key)).thenReturn("xxx");
		assertFalse(resolver.resolve(ConditionResolver.SETTINGS_PREFIX, key));

		when(environment.getProperty(key)).thenReturn(null);
		assertFalse(resolver.resolve(ConditionResolver.SETTINGS_PREFIX, key));
	}

	@Test
	public void testSettingsString() throws Exception {
		String key = "test123";

		when(environment.getProperty(key)).thenReturn(Boolean.TRUE.toString());
		assertTrue(resolver.resolve(ConditionResolver.SETTINGS_PREFIX, key));

		when(environment.getProperty(key)).thenReturn(Boolean.FALSE.toString());
		assertFalse(resolver.resolve(ConditionResolver.SETTINGS_PREFIX, key));
	}

	@Test(expected=IllegalArgumentException.class)
	public void testSettingsFailNamespace() throws Exception {
		resolver.resolve("xxx", "yyy");
	}
}

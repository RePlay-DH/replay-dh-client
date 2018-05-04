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
package bwfdm.replaydh.test.workflow.resolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.io.resources.VirtualResourceProvider;
import bwfdm.replaydh.test.RDHTestUtils;
import bwfdm.replaydh.utils.LookupResult;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.impl.DefaultResource;
import bwfdm.replaydh.workflow.resolver.LocalIdentifiableResolver;
import bwfdm.replaydh.workflow.schema.IdentifierType;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public class LocalIdentifiableResolverTest {


	private Path root;
	private RDHEnvironment environment;
	private LocalIdentifiableResolver resolver;
	private VirtualResourceProvider resourceProvider;

	private static final WorkflowSchema schema = WorkflowSchema.getDefaultSchema();

	@Before
	public void prepare() throws RDHLifecycleException {
		root = Paths.get("root");

		environment = RDHTestUtils.createTestEnvironment();

		resourceProvider = new VirtualResourceProvider();
		resourceProvider.addDirectory(root);

		resolver = LocalIdentifiableResolver.newBuilder()
				.folder(root)
				.resourceProvider(resourceProvider)
				.useDefaultSerialization()
				.autoPerformCacheSerialization(false)
				.build();

		resolver.start(environment);
	}

	@After
	public void cleanup() throws RDHLifecycleException {
		if(resourceProvider!=null) {
			resourceProvider.clear();
		}

		if(resolver!=null) {
			resolver.stop(environment);
		}
	}

	/**
	 * idData format:
	 * [type, id, (context)]*
	 */
	private static Identifiable dummyResource(Object...idData) {
		Resource resource = DefaultResource.withResourceType("temp");
		for(int i=0; i<idData.length; i+=3) {
			resource.addIdentifier(new Identifier(
					(IdentifierType)idData[i], (String)idData[i+1], (String)idData[i+2]));
		}
		return resource;
	}

	private void assertLookupResult(Set<Identifiable> expectedIdentifiables, int expectedResultSize, Identifier...identifiers) {

		List<LookupResult<Identifiable, Set<Identifier>>> lookup = resolver.resolve(99, identifiers);
		assertNotNull(lookup);
		if(expectedResultSize!=-1) {
			assertEquals(expectedResultSize, lookup.size());
		}

		for(int i=0; i<expectedResultSize; i++) {
			LookupResult<Identifiable, Set<Identifier>> result = lookup.get(i);
			assertTrue(expectedIdentifiables.contains(result.getTarget()));

			assertTrue(result.getRelevance()>0);
		}
	}

	@SafeVarargs
	private static <E extends Object> Set<E> set(E...items) {
		Set<E> result = new HashSet<>();
		Collections.addAll(result, items);
		return result;
	}

	@Test
	public void testMappingOneToMany() throws Exception {
		Identifiable dummy = dummyResource(
				schema.getDefaultNameVersionIdentifierType(), "toolXXX.v1.2", null,
				schema.getDefaultChecksumIdentifierType(), "XXXXXXXXXXXXXXXXX", null,
				schema.getDefaultPathIdentifierType(), "some/sub/folder", "/mount/root/some/where",
				schema.getDefaultURLIdentifierType(), "www.my.site.de", null);

		resolver.lock();
		try {
			resolver.register(dummy);

			dummy.forEachIdentifier(identifier -> {
				assertLookupResult(set(dummy), 1, identifier);
			});
		} finally {
			resolver.unlock();
		}
	}

	@Test
	public void testMultiMapping() throws Exception {
		Identifiable dummy1 = dummyResource(
				schema.getDefaultNameVersionIdentifierType(), "tool1.v1.2", null,
				schema.getDefaultChecksumIdentifierType(), "XXXXXXXXXXXXXXXXX", null,
				schema.getDefaultPathIdentifierType(), "some/sub/folder/1", "/mount/root/some/where",
				schema.getDefaultURLIdentifierType(), "www.my.site.de/cat/1", null);
		Identifiable dummy2 = dummyResource(
				schema.getDefaultNameVersionIdentifierType(), "tool2.v1.2", null,
				schema.getDefaultChecksumIdentifierType(), "XXXXXXXXXXXXXXXXX", null,
				schema.getDefaultPathIdentifierType(), "some/sub/folder/1", "/mount/root/some/where",
				schema.getDefaultURLIdentifierType(), "www.my.site.de/cat/2", null);
		Identifiable dummy3 = dummyResource(
				schema.getDefaultNameVersionIdentifierType(), "tool3.v1.2", null,
				schema.getDefaultChecksumIdentifierType(), "XXXXXXXXXXXXXXXXX", null,
				schema.getDefaultPathIdentifierType(), "some/sub/folder/2", "/mount/root/some/where",
				schema.getDefaultURLIdentifierType(), "www.my.site.de/cat/2", null);
		Identifiable dummy4 = dummyResource(
				schema.getDefaultNameVersionIdentifierType(), "tool4.v1.2", null,
				schema.getDefaultChecksumIdentifierType(), "XXXXXXXXXXXXXXXXX", null,
				schema.getDefaultPathIdentifierType(), "some/sub/folder/3", "/mount/root/some/where",
				schema.getDefaultURLIdentifierType(), "www.my.site.de/cat/3", null);

		resolver.lock();
		try {
			resolver.register(dummy1, dummy2, dummy3, dummy4);

			assertLookupResult(set(dummy1), 1,
					new Identifier(schema.getDefaultNameVersionIdentifierType(), "tool1.v1.2", null)
					);

			assertLookupResult(set(dummy1, dummy2), 2,
					new Identifier(schema.getDefaultPathIdentifierType(), "some/sub/folder/1", "/mount/root/some/where")
					);

			assertLookupResult(set(dummy1, dummy2, dummy3, dummy4), 4,
					new Identifier(schema.getDefaultChecksumIdentifierType(), "XXXXXXXXXXXXXXXXX", null)
					);

			assertLookupResult(set(dummy2, dummy3), 2,
					new Identifier(schema.getDefaultPathIdentifierType(), "some/sub/folder/2", "/mount/root/some/where"),
					new Identifier(schema.getDefaultURLIdentifierType(), "www.my.site.de/cat/2", null)
					);
		} finally {
			resolver.unlock();
		}
	}

	@Test
	public void testWriteRead() throws Exception {

		Identifiable dummy1 = dummyResource(
				schema.getDefaultNameVersionIdentifierType(), "tool1.v1.2<3", null,
				schema.getDefaultChecksumIdentifierType(), "XXXXXXXXXXXXXXXXX", null,
				schema.getDefaultPathIdentifierType(), "some/sub/folder/1", "/mount/root/some/where",
				schema.getDefaultURLIdentifierType(), "www.my.site.de/cat/1", null);
		Identifiable dummy2 = dummyResource(
				schema.getDefaultNameVersionIdentifierType(), "tool2.v1.2", null,
				schema.getDefaultChecksumIdentifierType(), "XXXXXXXXXXXXXXXXX", null,
				schema.getDefaultPathIdentifierType(), "some/sub/folder/1", "/mount/root/some/where",
				schema.getDefaultURLIdentifierType(), "www.my.site.de/cat/2", null);
		Identifiable dummy3 = dummyResource(
				schema.getDefaultNameVersionIdentifierType(), "tool3.v1.2", null,
				schema.getDefaultChecksumIdentifierType(), "XXXXXXXXXXXXXXXXX", null,
				schema.getDefaultPathIdentifierType(), "some/sub/folder/2", "/mount/root/some/where",
				schema.getDefaultURLIdentifierType(), "www.my.site.de/cat/2", null);
		Identifiable dummy4 = dummyResource(
				schema.getDefaultNameVersionIdentifierType(), "tool4.v1.2", null,
				schema.getDefaultChecksumIdentifierType(), "XXXXXXXXXXXXXXXXX", null,
				schema.getDefaultPathIdentifierType(), "some/sub/folder/3", "/mount/root/some/where",
				schema.getDefaultURLIdentifierType(), "www.my.site.de/cat/3", null);

		Set<Identifiable> dummies = set(dummy1, dummy2, dummy3, dummy4);

		resolver.lock();
		try {
			resolver.register(dummies);
		} finally {
			resolver.unlock();
		}

		resolver.writeCache();


		//BEGIN DEBUG
//		for(Path path : resourceProvider.getPaths()) {
//			IOResource r = resourceProvider.getResource(path);
//			System.out.println("============== "+path+" ==============");
//			System.out.println(IOUtils.readResource(r, StandardCharsets.UTF_8));
//		}
		// END DEBUG

		// Erase all life cache data
		resolver.lock();
		try {
			resolver.clearCache();
		} finally {
			resolver.unlock();
		}

		resolver.readCache();

		// Make sure we find all our original identifiable instances again
		resolver.lock();
		try {
			for(Identifiable dummy : dummies) {
				dummy.forEachIdentifier(identifier -> {
					assertLookupResult(set(dummy), -1, identifier);
				});

			}
		} finally {
			resolver.unlock();
		}
	}

	@Test
	public void testUUIDLookup() throws Exception {

		Identifiable dummy1 = dummyResource(
				schema.getDefaultNameVersionIdentifierType(), "tool1.v1.2<3", null,
				schema.getDefaultChecksumIdentifierType(), "XXXXXXXXXXXXXXXXX", null,
				schema.getDefaultPathIdentifierType(), "some/sub/folder/1", "/mount/root/some/where",
				schema.getDefaultURLIdentifierType(), "www.my.site.de/cat/1", null);

		resolver.lock();
		try {
			resolver.register(dummy1);

			Identifiable result = resolver.lookup(dummy1.getSystemId());
			assertSame(dummy1, result);
		} finally {
			resolver.unlock();
		}
	}
}

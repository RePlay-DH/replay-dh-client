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
package bwfdm.replaydh.test.metadata.basic.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.io.resources.VirtualResourceProvider;
import bwfdm.replaydh.metadata.MetadataBuilder;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRecord.Entry;
import bwfdm.replaydh.metadata.MetadataRecord.UID;
import bwfdm.replaydh.metadata.MetadataRepository.RecordIterator;
import bwfdm.replaydh.metadata.basic.file.FileMetadataRepository;
import bwfdm.replaydh.test.RDHTestUtils;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.impl.DefaultResource;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public class FileMetadataRepositoryTest {

	private Path root;
	private RDHEnvironment environment;
	private FileMetadataRepository repository;
	private VirtualResourceProvider resourceProvider;
	private WorkflowSchema schema;

	@Before
	public void prepare() throws RDHLifecycleException {
		root = Paths.get("root");

		environment = RDHTestUtils.createTestEnvironment();

		resourceProvider = new VirtualResourceProvider();
		resourceProvider.addDirectory(root);

		schema = WorkflowSchema.getDefaultSchema();

		repository = FileMetadataRepository.newBuilder()
				.rootFolder(root)
				.resourceProvider(resourceProvider)
				.useDefaultCacheAndSerialization()
				.useVirtualUIDStorage()
				.build();

		repository.start(environment);
	}

	@After
	public void cleanup() throws RDHLifecycleException {
		if(resourceProvider!=null) {
			resourceProvider.clear();
		}

		if(repository!=null) {
			repository.stop(environment);
		}
	}

	private Identifiable dummyResource(String name) {
		Resource resource = DefaultResource.withResourceType("temp");
		resource.addIdentifier(new Identifier(schema.getDefaultNameVersionIdentifierType(), name));
		return resource;
	}

	/**
	 * Create a record, safe it, clear cache, load it again and check if it's still the same.
	 * @throws IOException
	 */
	@Test
	public void testWriteLoad() throws IOException {

		Identifiable resource = dummyResource("test001");

		assertNull("Repo must not know yet about resource", repository.getUID(resource));

		String key = "myKey";
		String value = "myValue";

		repository.beginUpdate();
		try {
			MetadataBuilder builder = repository.createBuilder(resource);
			builder.start();

			builder.addEntry(key, value);

			MetadataRecord record = builder.build();

			repository.addRecord(record);
		} finally {
			repository.endUpdate();
		}

		//BEGIN DEBUG
//		for(Path path : resourceProvider.getPaths()) {
//			IOResource r = resourceProvider.getResource(path);
//			System.out.println("============== "+path+" ==============");
//			System.out.println(IOUtils.readResource(r, StandardCharsets.UTF_8));
//		}
		// END DEBUG

		// Reset internal mapping
		repository.clearCache();

		UID uid = repository.getUID(resource);
		assertNotNull("Repository must know about resource now", uid);

		MetadataRecord record = repository.getRecord(uid);
		assertNotNull(record);

		assertEquals(1, record.getEntryCount());

		Entry entry = record.getEntries().iterator().next();
		assertEquals(key, entry.getName());
		assertEquals(value, entry.getValue());
	}

	@Test
	public void testRecordIterator() throws Exception {
		final int resoruceCount = 10;

		List<UUID> systemIds = new ArrayList<>();

		for(int i=0; i<resoruceCount; i++) {
			UUID systemId = UUID.randomUUID();
			systemIds.add(systemId);

			resourceProvider.create(root.resolve(systemId.toString()+FileMetadataRepository.DEFAULT_RECORD_FILE_ENDING));
		}

		try(RecordIterator recordIterator = repository.getAvailableRecords()) {
			List<UID> buffer = new ArrayList<>();

			while(recordIterator.hasNext()) {
				UID uid = recordIterator.next();
//				System.out.println(uid);
				buffer.add(uid);
			}

			assertEquals(resoruceCount, buffer.size());
		}
	}
}

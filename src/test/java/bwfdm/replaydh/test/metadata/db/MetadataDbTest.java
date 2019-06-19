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
package bwfdm.replaydh.test.metadata.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.io.resources.VirtualResourceProvider;
import bwfdm.replaydh.metadata.MetadataBuilder;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRecord.Target;
import bwfdm.replaydh.metadata.MetadataRepository.RecordIterator;
import bwfdm.replaydh.metadata.MetadataSchema;
import bwfdm.replaydh.metadata.db.MetadataDB;
import bwfdm.replaydh.test.RDHTestUtils;

/**
 * @author Markus Gärtner
 *
 */
public class MetadataDbTest {

	private Path root;
	private RDHEnvironment environment;
	private MetadataDB repository;
	private VirtualResourceProvider resourceProvider;

	@Before
	public void prepare() throws RDHLifecycleException {
		root = Paths.get("root");

		environment = RDHTestUtils.createTestEnvironment();

		resourceProvider = new VirtualResourceProvider();
		resourceProvider.addDirectory(root);

		repository = MetadataDB.newBuilder()
				.rootFolder(root)
				.resourceProvider(resourceProvider)
				.useDefaultCacheAndLocationProvider()
				.memory(true)
				.build();

		assertTrue(repository.start(environment));

		repository.addSchema(MetadataSchema.EMPTY_SCHEMA);
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

	/**
	 * Create a record, safe it, clear cache, load it again and check if it's still the same.
	 * @throws IOException
	 */
	@Test
	public void testRemoveSingle() throws IOException {

		Target target = new Target("w1", "p1");
		String schemaId = MetadataSchema.EMPTY_SCHEMA_ID;

		assertNull(repository.getRecord(target, schemaId));

		String key = "myKey";
		String value = "myValue";

		repository.beginUpdate();
		try {
			MetadataBuilder builder = repository.createBuilder(target, schemaId);
			builder.start();

			builder.addEntry(key, value);

			MetadataRecord record = builder.build();

			repository.addRecord(record);
		} finally {
			repository.endUpdate();
		}

		// Reset internal mapping
		repository.clearCache();

		MetadataRecord record = repository.getRecord(target, schemaId);
		assertNotNull(record);

		repository.removeRecord(record);

		assertNull(repository.getRecord(target, schemaId));
	}

	/**
	 * Create a record, safe it, make sure it's there, delete it and verify it's gone.
	 * @throws IOException
	 */
	@Test
	public void testAddSingle() throws IOException {

		Target target = new Target("w1", "p1");
		String schemaId = MetadataSchema.EMPTY_SCHEMA_ID;

		assertFalse(repository.hasRecords(target));
		assertNull(repository.getRecord(target, schemaId));

		int entryCount = 6;
		Map<String, String> data = new HashMap<>();

		repository.beginUpdate();
		try {
			MetadataBuilder builder = repository.createBuilder(target, schemaId);
			builder.start();

			for (int i = 0; i < entryCount; i++) {
				builder.addEntry("key"+i, "value"+i);
				data.put("key"+i, "value"+i);
			}

			MetadataRecord record = builder.build();

			repository.addRecord(record);
		} finally {
			repository.endUpdate();
		}

		// Reset internal mapping
		repository.clearCache();

		assertTrue(repository.hasRecords(target));
		MetadataRecord record = repository.getRecord(target, schemaId);
		assertNotNull(record);

		assertEquals(entryCount, record.getEntryCount());

		record.forEachEntry(entry -> {
			String key = entry.getName();
			String value = entry.getValue();
			assertEquals(value, data.get(key));
		});
	}

	@Test
	public void testAddMultiple() throws IOException {

		String workspace = "w1";
		String path = "path1";
		Target target = new Target(workspace, path);

		int recordCount = 4;
		int entryCount = 6;
		Map<String, String> data = new HashMap<>();

		repository.beginUpdate();
		try {
			for (int i = 0; i < recordCount; i++) {
				String schemaId = "schema"+i;
				MetadataBuilder builder = repository.createBuilder(target, schemaId);
				builder.start();

				for (int j = 0; j < entryCount; j++) {
					String key = schemaId+"_key"+j;
					String value = schemaId+"_value"+j;
					builder.addEntry(key, value);
					data.put(key, value);
				}

				MetadataRecord record = builder.build();

				repository.addRecord(record);
			}
		} finally {
			repository.endUpdate();
		}

		// Reset internal mapping
		repository.clearCache();

		Collection<MetadataRecord> records = repository.getRecords(target);
		assertNotNull(records);
		assertEquals(recordCount, records.size());

		for(MetadataRecord record : records) {
			record.forEachEntry(entry -> {
				String key = entry.getName();
				String value = entry.getValue();
				assertEquals(value, data.get(key));
			});
		}
	}

	@Test
	public void testAddMultipleGroups() throws IOException {

		String workspace = "w1";
		String path1 = "path1";
		String path2 = "path2";
		Target target1 = new Target(workspace, path1);
		Target target2 = new Target(workspace, path2);

		int recordCount = 4;
		int entryCount = 6;
		Map<String, String> data = new HashMap<>();

		repository.beginUpdate();
		try {
			for (int i = 0; i < recordCount; i++) {
				String schemaId = "schema"+i;

				MetadataBuilder builder1 = repository.createBuilder(target1, schemaId);
				builder1.start();
				MetadataBuilder builder2 = repository.createBuilder(target2, schemaId);
				builder2.start();

				for (int j = 0; j < entryCount; j++) {
					// For first set of records
					String key1 = path1+"_"+schemaId+"_key"+j;
					String value1 = path1+"_"+schemaId+"_value"+j;
					builder1.addEntry(key1, value1);
					data.put(key1, value1);

					// For seconds et of records
					String key2 = path2+"_"+schemaId+"_key"+j;
					String value2 = path2+"_"+schemaId+"_value"+j;
					builder2.addEntry(key2, value2);
					data.put(key2, value2);
				}

				repository.addRecord(builder1.build());
				repository.addRecord(builder2.build());
			}
		} finally {
			repository.endUpdate();
		}

		// Reset internal mapping
		repository.clearCache();

		assertTrue(repository.hasRecords(target1));
		Collection<MetadataRecord> records1 = repository.getRecords(target1);
		assertNotNull(records1);
		assertEquals(recordCount, records1.size());

		for(MetadataRecord record : records1) {
			record.forEachEntry(entry -> {
				String key = entry.getName();
				String value = entry.getValue();
				assertTrue(key.contains(path1));
				assertEquals(value, data.get(key));
			});
		}

		assertTrue(repository.hasRecords(target2));
		Collection<MetadataRecord> records2 = repository.getRecords(target2);
		assertNotNull(records2);
		assertEquals(recordCount, records2.size());

		for(MetadataRecord record : records2) {
			record.forEachEntry(entry -> {
				String key = entry.getName();
				String value = entry.getValue();
				assertTrue(key.contains(path2));
				assertEquals(value, data.get(key));
			});
		}
	}

	@Test
	public void testRecordIterator() throws Exception {
		final int resourceCount = 10;

		String workspace = "workspace1";
		String schemaId = MetadataSchema.EMPTY_SCHEMA_ID;

		repository.beginUpdate();
		try {
			for (int i = 0; i < resourceCount; i++) {
				String path = "path"+i;

				Target target = new Target(workspace, path);
				MetadataBuilder builder = repository.createBuilder(target, schemaId);
				builder.start();

				builder.addEntry("key"+i, "value"+i);

				MetadataRecord record = builder.build();

				repository.addRecord(record);
			}
		} finally {
			repository.endUpdate();
		}

		try(RecordIterator recordIterator = repository.getAvailableRecords()) {
			for (int i = 0; i < resourceCount; i++) {
				assertTrue(recordIterator.hasNext());
				Target target = recordIterator.next();
				assertNotNull(target);
				assertEquals(workspace, target.getWorkspace());
				assertEquals("path"+i, target.getPath());
			}

			assertFalse(recordIterator.hasNext());
		}
	}
}

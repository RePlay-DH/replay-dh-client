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
package bwfdm.replaydh.workflow.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import bwfdm.replaydh.utils.IdentityHashSet;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifiable.Type;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Person;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.Tool;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowStep;
import bwfdm.replaydh.workflow.catalog.MetadataCatalog.Constraint;
import bwfdm.replaydh.workflow.catalog.MetadataCatalog.QuerySettings;
import bwfdm.replaydh.workflow.catalog.MetadataCatalog.Result;
import bwfdm.replaydh.workflow.impl.DefaultPerson;
import bwfdm.replaydh.workflow.impl.DefaultResource;
import bwfdm.replaydh.workflow.impl.DefaultTool;
import bwfdm.replaydh.workflow.impl.DefaultWorkflow;
import bwfdm.replaydh.workflow.schema.IdentifierType;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;
import bwfdm.replaydh.workflow.schema.impl.IdentifierTypeImpl;

/**
 * @author Markus Gärtner
 *
 */
public class MetadataCacheTest {

	private static final QuerySettings ES = MetadataCatalog.EMPTY_SETTINGS;

	private MetadataCache cache;
	private Workflow workflow;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		workflow = new DefaultWorkflow(WorkflowSchema.getDefaultSchema());
		cache = new MetadataCache();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		cache.clear();
		cache = null;
		workflow.close();
		workflow = null;
	}

	static void assertEmpty(Collection<?> c) {
		assertNotNull("Collection null", c);
		assertTrue("Collection not empty", c.isEmpty());
	}

	@SuppressWarnings("unchecked")
	static <T> void assertCollectionEquals(Collection<T> c, T...expected) {
		assertNotNull("Collection null", c);
		assertFalse("Collection empty", c.isEmpty());

		if(expected.length!=c.size())
			throw new AssertionError(String.format(
					"Collection size mismatch, expected %d - got %d: %s vs. %s",
					expected.length, c.size(), Arrays.toString(expected), c));

		for(T item : expected) {
			assertTrue("Missing tiem: "+item, c.contains(item));
		}
	}

	@SuppressWarnings("unchecked")
	static <T> void assertListEquals(List<T> list, T...expected) {
		assertNotNull("List null", list);
		assertFalse("List empty", list.isEmpty());
		assertEquals("List size mismatch", expected.length, list.size());

		for (int i = 0; i < expected.length; i++) {
			assertEquals("Item mismatch at index "+i, expected[i], list.get(i));
		}
	}

	private void store(String key, String...values) {
		for(String value : values) {
			Identifiable proxy = mock(Resource.class);
			when(proxy.getType()).thenReturn(Type.RESOURCE);
			IdentifierType type = mock(IdentifierType.class);
			when(type.getLabel()).thenReturn(key);
			Identifier id = new Identifier(type, value);
			doAnswer(inv -> {
				@SuppressWarnings("unchecked")
				Consumer<? super Identifier> action = (Consumer<? super Identifier>) inv.getArguments()[0];
				action.accept(id);
				return null;
			}).when(proxy).forEachIdentifier(any());

			WorkflowStep step = mock(WorkflowStep.class);
			doAnswer(inv -> {
				@SuppressWarnings("unchecked")
				Consumer<? super Identifiable> action = (Consumer<? super Identifiable>) inv.getArguments()[0];
				action.accept(proxy);
				return null;
			}).when(step).forEachIdentifiable(any());

			cache.addWorkflowStep(step);
		}
	}

	private Identifier makeIdentifier(String key, String value) {
		IdentifierTypeImpl type = new IdentifierTypeImpl();
		type.setLabel(key);
		return new Identifier(type, value);
	}

	/**
	 * Creates an identifiable whose actual type is based on the
	 * {@code key} argument. If {@code key} is not a type-specific
	 * property then a {@link Resource} object will be created as default.
	 *
	 * @param key
	 * @param value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private <I extends Identifiable> I makeIdentifiable(String key, String value) {
		switch (key) {
		case MetadataCatalog.ENVIRONMENT_KEY:
			return (I) DefaultTool.withSettings("", value);
		case MetadataCatalog.PARAMETERS_KEY:
			return (I) DefaultTool.withSettings(value, "");
		case MetadataCatalog.ROLE_KEY:
			return (I) DefaultPerson.withRole(value);
		case MetadataCatalog.TYPE_KEY:
			return (I) DefaultResource.withResourceType(value);
		case MetadataCatalog.DESCRIPTION_KEY: {
			Resource resource = DefaultResource.blankResource();
			resource.setDescription(value);
			return (I) resource;
		}

		default:
			return (I) DefaultResource.withIdentifiers(Collections.singleton(
					makeIdentifier(key, value)));
		}
	}

	private void addIdentifiable(WorkflowStep step, Identifiable identifiable) {
		switch (identifiable.getType()) {
		case PERSON: step.addPerson((Person) identifiable); break;
		case RESOURCE: step.addInput((Resource) identifiable); break;
		case TOOL: step.setTool((Tool) identifiable); break;

		default:
			throw new IllegalStateException("Unknown identifiable type "+identifiable.getType());
		}
	}

	private void storeStep(String key, String value) {
		WorkflowStep step = workflow.createWorkflowStep();
		switch (key) {
		case MetadataCatalog.TITLE_KEY:
			step.setTitle(value);
			break;

		case MetadataCatalog.DESCRIPTION_KEY:
			step.setDescription(value);
			break;

		default: {
			addIdentifiable(step, makeIdentifiable(key, value));
		} break;
		}

		cache.addWorkflowStep(step);
	}

	private void storeStep(Identifiable...identifiables) {
		WorkflowStep step = workflow.createWorkflowStep();
		Arrays.asList(identifiables).forEach(identifiable -> addIdentifiable(step, identifiable));
		cache.addWorkflowStep(step);
	}

	@Test
	public void testAddWorkflowStep() throws Exception {
		storeStep(MetadataCatalog.TITLE_KEY, "testTitle");
		assertCollectionEquals(cache.suggest(ES, null, MetadataCatalog.TITLE_KEY, null), "testTitle");

		storeStep(MetadataCatalog.DESCRIPTION_KEY, "testDescription");
		assertCollectionEquals(cache.suggest(ES, null, MetadataCatalog.DESCRIPTION_KEY, null), "testDescription");

		storeStep(MetadataCatalog.ENVIRONMENT_KEY, "testEnvironment");
		assertCollectionEquals(cache.suggest(ES, null, MetadataCatalog.ENVIRONMENT_KEY, null), "testEnvironment");

		storeStep(MetadataCatalog.PARAMETERS_KEY, "testParameters");
		assertCollectionEquals(cache.suggest(ES, null, MetadataCatalog.PARAMETERS_KEY, null), "testParameters");

		storeStep(MetadataCatalog.ROLE_KEY, "testRole");
		assertCollectionEquals(cache.suggest(ES, null, MetadataCatalog.ROLE_KEY, null), "testRole");

		for(String key : Arrays.asList("bla", "xxx", "title2", "doi", "nameVersion")) {
			String value = key+"_test";
			storeStep(key, value);
			assertCollectionEquals(cache.suggest(ES, null, key, null), value);
		}
	}

	@Test
	public void suggestEmptyWhenCacheEmpty() throws Exception {
		assertEmpty(cache.suggest(ES, null, "test", null));
	}

	@Test
	public void suggestWhenPrefixEmpty() throws Exception {
		store("test", "x1", "x2");
		assertListEquals(cache.suggest(ES, null, "test", null),
				"x1", "x2");
	}

	@Test
	public void suggestWithMultipleKeysWhenPrefixEmpty() throws Exception {
		store("test", "x1", "x2");
		store("test2", "x1", "x2");
		store("test3", "x1", "x2");
		assertListEquals(cache.suggest(ES, null, "test", null),
				"x1", "x2");
	}

	@Test
	public void suggestWithMultipleEntriesWhenPrefixEmpty() throws Exception {
		store("test", "x1", "x2");
		store("test", "x2", "x4");
		store("test", "x4", "x3");
		assertListEquals(cache.suggest(ES, null, "test", null),
				"x1", "x2", "x3", "x4");
	}

	@Test
	public void suggestWithMultipleEntriesWhenPrefixEmptyAndSizeLimited() throws Exception {
		store("test", "x1", "x2");
		store("test", "x2", "x4");
		store("test", "x4", "x3");
		assertListEquals(cache.suggest(MetadataCatalog.settings().setResultLimit(3), null, "test", null),
				"x1", "x2", "x3");
	}

	@Test
	public void suggestWithMultipleEntriesWhenPrefixSet() throws Exception {
		store("test", "x10", "x11");
		store("test", "x20", "x14");
		store("test", "x1", "x21");
		assertCollectionEquals(cache.suggest(ES, null, "test", "x1"),
				"x1", "x10", "x11", "x14");
	}

	@Test
	public void suggestWithMultipleEntriesWhenPrefixSetAndSizeLimited() throws Exception {
		store("test", "x10", "x11");
		store("test", "x20", "x14");
		store("test", "x1", "x21");
		assertCollectionEquals(cache.suggest(MetadataCatalog.settings().setResultLimit(3), null, "test", "x1"),
				"x1", "x10", "x11");
	}

	private void assertResult(Result result, Identifiable...expected) {
		assertNotNull("Null result", result);

		if(expected.length==0) {
			assertTrue("Result must be empty", result.isEmpty());
		} else {
			assertFalse("Result expected to contain hits: "+Arrays.toString(expected), result.isEmpty());
			Set<Identifiable> hits = new IdentityHashSet<>();
			for(Iterator<Identifiable> it = result.iterator(); it.hasNext();)
				hits.add(it.next());

			for(Identifiable identifiable : expected) {
				assertTrue(hits.contains(identifiable));
			}
		}
	}

	@Test
	public void fullTextQueryWhenEmpty() throws Exception {
		assertResult(cache.query(ES, "x"));
	}

	@Test
	public void fullTextQueryWithNoMatch() throws Exception {
		storeStep(makeIdentifiable("key", "value"));
		assertResult(cache.query(ES, "x"));
	}

	@Test
	public void fullTextQueryWithExactMatch() throws Exception {
		Identifiable id1 = makeIdentifiable("key", "value");
		storeStep(id1);
		assertResult(cache.query(ES, "value"), id1);
	}

	@Test
	public void fullTextQueryWithExactMatch2() throws Exception {
		Identifiable id1 = makeIdentifiable("key", "value");
		Identifiable id2 = makeIdentifiable("key", "value2");
		storeStep(id1, id2);
		assertResult(cache.query(ES, "value2"), id2);
	}

	@Test
	public void fullTextQueryWithPartialMatches() throws Exception {
		Identifiable noMatch1 = makeIdentifiable("bla", "xx");
		Identifiable id1 = makeIdentifiable("key", "value");
		Identifiable id2 = makeIdentifiable("key", "value2");
		Identifiable id3 = makeIdentifiable("key", "another weird value");
		Identifiable id4 = makeIdentifiable("key", "what would this value be in a parallel universe?");
		Identifiable noMatch2 = makeIdentifiable("blub", "xxyy");
		storeStep(noMatch1, id1, id2, id3, id4, noMatch2);
		assertResult(cache.query(ES, "value"), id1, id2, id3, id4);
	}

	@Test
	public void fullTextQueryWithPartialMatchesOnMultipleSteps() throws Exception {
		Identifiable noMatch1 = makeIdentifiable("bla", "xx");
		Identifiable id1 = makeIdentifiable("key", "value");
		Identifiable id2 = makeIdentifiable("key", "value2");
		Identifiable id3 = makeIdentifiable("key", "another weird value");
		Identifiable id4 = makeIdentifiable("key", "what would this value be in a parallel universe?");
		Identifiable noMatch2 = makeIdentifiable("blub", "xxyy");

		storeStep(noMatch1);
		storeStep(id1);
		storeStep(id2);
		storeStep(id3);
		storeStep(id4);
		storeStep(noMatch2);

		assertResult(cache.query(ES, "value"), id1, id2, id3, id4);
	}

	@Test
	public void fullTextQueryWithNoMatchOnMultipleSteps() throws Exception {
		Identifiable noMatch1 = makeIdentifiable("bla", "xx");
		Identifiable id1 = makeIdentifiable("key", "value");
		Identifiable id2 = makeIdentifiable("key", "value2");
		Identifiable id3 = makeIdentifiable("key", "another weird value");
		Identifiable id4 = makeIdentifiable("key", "what would this value be in a parallel universe?");
		Identifiable noMatch2 = makeIdentifiable("blub", "xxyy");

		storeStep(noMatch1);
		storeStep(id1);
		storeStep(id2);
		storeStep(id3);
		storeStep(id4);
		storeStep(noMatch2);

		assertResult(cache.query(ES, "nothing"));
	}

	private List<Constraint> constraints(String...args) {
		assertTrue("Args must be multiple of 2", args.length>0 && args.length%2==0);
		List<Constraint> result = new ArrayList<>();
		for(int i=0; i<args.length; i += 2) {
			result.add(new Constraint(args[i], args[i+1]));
		}
		return result;
	}

	@Test
	public void constraintQueryWhenEmpty() throws Exception {
		assertResult(cache.query(ES, constraints("test", "nothing")));
	}

	@Test
	public void constraintQueryWhithNoMatch() throws Exception {
		storeStep(makeIdentifiable("key", "value"));
		assertResult(cache.query(ES, constraints("key", "anotherValue")));
	}

	@Test
	public void constraintQueryWhithNoMatch2() throws Exception {
		storeStep(makeIdentifiable("key", "value"));
		assertResult(cache.query(ES, constraints("key2", "value")));
	}

	@Test
	public void constraintQueryWhithExactMatch() throws Exception {
		Identifiable id1 = makeIdentifiable("key", "value");
		storeStep(id1);
		assertResult(cache.query(ES, constraints("key", "value")), id1);
	}

	private static final String[] SPECIAL_KEYS = {
		MetadataCatalog.DESCRIPTION_KEY,
		MetadataCatalog.ENVIRONMENT_KEY,
		MetadataCatalog.PARAMETERS_KEY,
		MetadataCatalog.ROLE_KEY,
		MetadataCatalog.TYPE_KEY,
	};

	@Test
	public void constraintQueryWhithExactMatchForSpecialKeys() throws Exception {

		// Create instances and add them first
		List<Identifiable> ids = new ArrayList<>();
		for(String key : SPECIAL_KEYS) {
			Identifiable id = makeIdentifiable(key, key+"_value");
			storeStep(id);
			ids.add(id);
		}

		// Then check them all
		for (int i = 0; i < SPECIAL_KEYS.length; i++) {
			assertResult(cache.query(ES, constraints(SPECIAL_KEYS[i], "_value")), ids.get(i));
		}
	}

	private static final String alNum =
			  "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
            + "0123456789"
            + "abcdefghijklmnopqrstuvxyz";

	private static final Random random = new Random(System.currentTimeMillis());

	private static String randString(int maxLen) {
		final int len = random.nextInt(maxLen)+1;
		assertTrue(len>0);

		char[] tmp = new char[len];
		for(int i=0; i<len; i++) {
			tmp[i] = alNum.charAt(random.nextInt(alNum.length()));
		}
		return new String(tmp);
	}

	@Test
	public void testPerformance() throws Exception {
		final int SIZE = 1_000;
		final int RUNS = 100;

		final String[] keys = new String[10];
		for (int i = 0; i < keys.length; i++) {
			keys[i] = randString(15);
		}

		final String[] vocabulary = new String[200];
		for (int i = 0; i < vocabulary.length; i++) {
			vocabulary[i] = randString(50);
		}

		Instant begin = Instant.now();
		for(int i=0; i<SIZE; i++) {
			store(keys[random.nextInt(keys.length)],
					vocabulary[random.nextInt(vocabulary.length)]);
		}
		Duration dFill = Duration.between(begin, Instant.now());
		System.out.printf("Time needed to fill cache with %d entries: %s%n",
				SIZE, dFill);

		begin = Instant.now();
		for(int i=0; i<RUNS; i++) {
			cache.query(ES, randString(25));
		}
		Duration dSearchText = Duration.between(begin, Instant.now());
		System.out.printf("Time needed to do %d full text search runs: %s%n",
				RUNS, dSearchText);

		begin = Instant.now();
		for(int i=0; i<RUNS; i++) {
			Constraint[] constraints = new Constraint[random.nextInt(5)+1];
			for (int j = 0; j < constraints.length; j++) {
				constraints[j] = new Constraint(keys[random.nextInt(keys.length)],
						random.nextDouble()<0.1 ? randString(10)
								: vocabulary[random.nextInt(vocabulary.length)]);
			}
			cache.query(ES, Arrays.asList(constraints));
		}
		Duration dSearchConstraints = Duration.between(begin, Instant.now());
		System.out.printf("Time needed to do %d constraint search runs: %s%n",
				RUNS, dSearchConstraints);

		begin = Instant.now();
		for(int i=0; i<RUNS; i++) {
			cache.suggest(ES, null, keys[random.nextInt(keys.length)], randString(15));
		}
		Duration dSuggest = Duration.between(begin, Instant.now());
		System.out.printf("Time needed to do %d auto-complete suggestions: %s%n",
				RUNS, dSuggest);
	}
}

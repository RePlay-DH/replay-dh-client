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
package bwfdm.replaydh.test.json;

import static bwfdm.replaydh.test.RDHTestUtils.assertDeepEqual;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import bwfdm.replaydh.json.JsonWorkflowStepReader;
import bwfdm.replaydh.json.JsonWorkflowStepWriter;
import bwfdm.replaydh.utils.ClassUtils;
import bwfdm.replaydh.utils.Options;
import bwfdm.replaydh.workflow.Checksum;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.WorkflowStep;
import bwfdm.replaydh.workflow.impl.DefaultPerson;
import bwfdm.replaydh.workflow.impl.DefaultResource;
import bwfdm.replaydh.workflow.impl.DefaultTool;
import bwfdm.replaydh.workflow.impl.DefaultWorkflow;
import bwfdm.replaydh.workflow.schema.IdentifierSchema;
import bwfdm.replaydh.workflow.schema.IdentifierType;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * Tests {@link JsonWorkflowStepWriter} and {@link JsonWorkflowStepReader}
 * in conjunction. {@link WorkflowStep} instances are filled with various content
 * and then serialized, deserialized and sent to a {@link ClassUtils#deepDiff(Object, Object) deep comparison}.
 * Test cases are considered successful if both the original and deserialized
 * instance hold equal object graphs.
 *
 * @author Markus Gärtner
 *
 */
public class JsonWorkflowSerializationTest {

	private DefaultWorkflow workflow;
	private WorkflowStep original, deserialized;

	@Before
	public void prepare() throws Exception {
		workflow = new DefaultWorkflow(WorkflowSchema.getDefaultSchema());
		original = workflow.createWorkflowStep();
		deserialized = null;
	}

	static final Random RANDOM = new Random(System.currentTimeMillis()|(System.nanoTime()<<32));
	// Create random checksum objects that simulate realistic serialization targets
	static Checksum randomChecksum() {
		String type = "random";
		long size = Math.abs(RANDOM.nextLong());
		byte[] payload = new byte[12];
		RANDOM.nextBytes(payload);
		return new Checksum(type, size, payload);
	}

	private void initPersons(int count) {
		IdentifierSchema schema = workflow.getSchema().getPersonIdentifierSchema();

		for(int i=0; i<count; i++) {
			DefaultPerson person = DefaultPerson.withRole("role"+i);
			person.setDescription("Some person related stuff "+i);
			person.addIdentifier(new Identifier(schema.findIdentifierType(IdentifierType.NAME), "person"+i));

			original.addPerson(person);
		}
	}

	private void initInputs(int count) {
		IdentifierSchema schema = workflow.getSchema().getResourceIdentifierSchema();

		for(int i=0; i<count; i++) {
			DefaultResource resource = DefaultResource.withResourceType("type"+i);
			resource.setDescription("Some input related stuff "+i);
			resource.addIdentifier(new Identifier(schema.findIdentifierType(IdentifierType.NAME_VERSION), "input"+i));
			resource.addIdentifier(new Identifier(schema.findIdentifierType(IdentifierType.CHECKSUM), randomChecksum().toString()));

			original.addInput(resource);
		}
	}

	private void initOutputs(int count) {
		IdentifierSchema schema = workflow.getSchema().getResourceIdentifierSchema();

		for(int i=0; i<count; i++) {
			DefaultResource resource = DefaultResource.withResourceType("type"+i);
			resource.setDescription("Some output related stuff "+i);
			resource.addIdentifier(new Identifier(schema.findIdentifierType(IdentifierType.NAME_VERSION), "output"+i));
			resource.addIdentifier(new Identifier(schema.findIdentifierType(IdentifierType.CHECKSUM), randomChecksum().toString()));

			original.addOutput(resource);
		}
	}

	private void initTool() {
		IdentifierSchema schema = workflow.getSchema().getResourceIdentifierSchema();

		DefaultTool tool = DefaultTool.uniqueTool();

//		tool.setChecksum(randomChecksum());
		tool.setResourceType("executable");
		tool.setEnvironment("architecture: win10");
		tool.setDescription("Some tool related stuff");
		tool.setParameters("java -jar xyz.jar -v -dest some/place/some/where/my_file.txt");
		tool.addIdentifier(new Identifier(schema.findIdentifierType(IdentifierType.NAME_VERSION), "random_tool"));
		tool.addIdentifier(new Identifier(schema.findIdentifierType(IdentifierType.CHECKSUM), randomChecksum().toString()));

		original.setTool(tool);
	}

	private void initId() {
		original.setId("some random step");
	}

	private void initTitle() {
		original.setTitle("some random title");
	}

	private void initDescription() {
		original.setDescription("some description\nthatcontains\nline-breaks and some other junk...");
	}

	private void initTime() {
		original.setRecordingTime(LocalDateTime.now().withNano(0)); //FIXME workaround against now() creating a date-time object with current milli seconds
	}

	private void initProperties(int count) {
		for(int i=0; i<count; i++) {
			original.setProperty("key"+i, "someValue"+i);
		}
	}

	private String writeAndRead() throws IOException, InterruptedException {

		String serializedForm = null;

		deserialized = null;
		try(JsonWorkflowStepWriter writer = new JsonWorkflowStepWriter()) {
			StringWriter sw = new StringWriter();

			writer.init(sw, Options.emptyOptions);

			writer.writeAll(Collections.singleton(original));

			serializedForm = sw.toString();
		}

//		System.out.println(serializedForm); //DEBUG

		workflow.reset();

		try(JsonWorkflowStepReader reader = new JsonWorkflowStepReader(
				workflow.getSchema(), workflow::createWorkflowStep)) {
			StringReader sr = new StringReader(serializedForm);

			reader.init(sr, null);

			deserialized = reader.read();
		}

		return serializedForm;
	}

	private void checkSerializationResult(String msg) throws Exception {
		String serializedForm = writeAndRead();

		assertDeepEqual(msg, original, deserialized, serializedForm);
	}

	// EMPTY

	@Test
	public void testEmpty() throws Exception {
		checkSerializationResult("Empty original");
	}

	// PERSONS

	@Test
	public void testWithPerson() throws Exception {
		initPersons(1);
		checkSerializationResult("Single person");
	}

	@Test
	public void testWithPersons() throws Exception {
		initPersons(2);
		checkSerializationResult("Multiple persons");
	}

	// INPUT

	@Test
	public void testWithInput() throws Exception {
		initInputs(1);
		checkSerializationResult("Single input");
	}

	@Test
	public void testWithInputs() throws Exception {
		initInputs(3);
		checkSerializationResult("Multiple inputs");
	}

	// OUTPUT

	@Test
	public void testWithOutput() throws Exception {
		initOutputs(1);
		checkSerializationResult("Single output");
	}

	@Test
	public void testWithOutputs() throws Exception {
		initOutputs(3);
		checkSerializationResult("Multiple outputs");
	}

	// TOOL

	@Test
	public void testWithTool() throws Exception {
		initTool();
		checkSerializationResult("Single tool");
	}

	// TIME

	@Test
	public void testWithTime() throws Exception {
		initTime();
		checkSerializationResult("Time only");
	}

	// ID

	@Test
	public void testWithId() throws Exception {
		initId();
		checkSerializationResult("Id only");
	}

	// TITLE

	@Test
	public void testWithTitle() throws Exception {
		initTitle();
		checkSerializationResult("Title only");
	}

	// DESCRIPTION

	@Test
	public void testWithDescription() throws Exception {
		initDescription();
		checkSerializationResult("Description only");
	}

	// PROPERTIES

	@Test
	public void testWithProperty() throws Exception {
		initProperties(1);
		checkSerializationResult("Single property");
	}

	@Test
	public void testWithProperties() throws Exception {
		initProperties(3);
		checkSerializationResult("Multiple properties");
	}

	// FULL

	@Test
	public void testFull() throws Exception {
		initId();
		initTitle();
		initDescription();
		initTime();
		initInputs(3);
		initOutputs(3);
		initPersons(2);
		initTool();
		initProperties(3);
		checkSerializationResult("Full step");
	}
}

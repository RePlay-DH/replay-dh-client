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
package bwfdm.replaydh.json;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.stream.JsonGenerator;

import bwfdm.replaydh.io.ObjectWriter;
import bwfdm.replaydh.utils.Options;
import bwfdm.replaydh.utils.RDHUtils;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Person;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.Tool;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowStep;

/**
 * @author Markus Gärtner
 *
 */
public class JsonWorkflowStepWriter implements ObjectWriter<WorkflowStep> {

	/**
	 * Property for the header to be prepended to the serialization output.
	 */
	public static final String HEADER = "header";

	/**
	 * Property for the flag indicating that the serialization should be
	 * formatted to improve readability by humans.
	 */
	public static final String PRETTY = "pretty";

	private static final String NULL = null;

	/**
	 * Supported options:
	 * <p>
	 * HEADER : appends the given property as an additional line before serializing
	 * the step.
	 *
	 * PRETTY: will activate {@link JsonGenerator#PRETTY_PRINTING} on the output stream.
	 *
	 * @param step
	 * @param options
	 * @return
	 */
	public static String writeStep(WorkflowStep step, Options options) {
		if(options==null) {
			options = Options.emptyOptions;
		}

		StringWriter sw = new StringWriter();

		String header = options.get(HEADER, NULL);
		if(header!=null) {
			header = header.trim();

			/*
			 *  Prevent header from starting with reserved '{' symbol.
			 *  This is to ensure compatibility with the reader that will use
			 *  this information to detect an artificial header text.
			 */
			if(header.charAt(0)=='{') {
				sw.write("--");
			}

			sw.write(header);
			sw.write("\n\n");
		}

		try(JsonWorkflowStepWriter writer = new JsonWorkflowStepWriter()) {
			writer.init(sw, options);
			writer.writeHeader();
			writer.write(step);
			writer.writeFooter();
		} catch (IOException | InterruptedException e) {
			return null;
		}

		return sw.toString();
	}

	private JsonGenerator generator;

	@Override
	public void init(Writer output, Options options) {
		requireNonNull(output);
		checkState("Generator already initialized", generator==null);

		if(options==null) {
			options = Options.emptyOptions;
		}

		// Activate pretty printing if requested
		if(options.getBoolean(PRETTY, false)) {
			options.put(JsonGenerator.PRETTY_PRINTING, true);
		}

		generator = Json.createGeneratorFactory(options).createGenerator(output);
	}

	@Override
	public void write(WorkflowStep step) throws IOException, InterruptedException {
		requireNonNull(step);
		checkState("Generator not initialized", generator!=null);

		generator.writeStartObject();

		// ID
		String id = step.getId();
		if(id!=null) {
			generator.write(JsonLabels.ID, id);
		}

		// TITILE
		String title = step.getTitle();
		if(title!=null) {
			generator.write(JsonLabels.TITLE, title);
		}

		// DESCRIPTION
		String description = step.getDescription();
		if(description!=null) {
			generator.write(JsonLabels.DESCRIPTION, description);
		}

		// TIMESTAMP
		LocalDateTime timestamp = step.getRecordingTime();
		if(timestamp!=null) {
			generator.write(JsonLabels.TIMESTAMP, RDHUtils.formatTimestamp(timestamp));
		}

		// INPUT
		Set<Resource> input = step.getInput();
		if(!input.isEmpty()) {
			generator.writeStartArray(JsonLabels.INPUT);
			input.forEach(this::writeResource);
			generator.writeEnd();
		}

		// TOOL
		Tool tool = step.getTool();
		if(tool!=null) {
			writeTool(tool);
		}

		// PERSONS
		Set<Person> persons = step.getPersons();
		if(!persons.isEmpty()) {
			generator.writeStartArray(JsonLabels.PERSONS);
			persons.forEach(this::writePerson);
			generator.writeEnd();
		}

		// OUTPUT
		Set<Resource> output = step.getOutput();
		if(!output.isEmpty()) {
			generator.writeStartArray(JsonLabels.OUTPUT);
			output.forEach(this::writeResource);
			generator.writeEnd();
		}

		// PROPERTIES
		Map<String, String> properties = step.getProperties();
		if(!properties.isEmpty()) {
			generator.writeStartObject(JsonLabels.PROPERTIES);
			properties.forEach(generator::write);
			generator.writeEnd();
		}

		generator.writeEnd();

		generator.flush();
	}

	private void writeIdentifiableFields(Identifiable identifiable) {
		Set<Identifier> identifiers = identifiable.getIdentifiers();

		if(identifiable.getDescription()!=null) {
			generator.write(JsonLabels.DESCRIPTION, identifiable.getDescription());
		}

		generator.writeStartArray(JsonLabels.IDENTIFIERS);

		for(Identifier identifier : identifiers) {
			generator.writeStartObject();
			generator.write(JsonLabels.TYPE, identifier.getType().getStringValue());
			if(identifier.getContext()!=null) {
				generator.write(JsonLabels.CONTEXT, identifier.getContext());
			}
			generator.write(JsonLabels.ID, identifier.getId());
			generator.writeEnd();
		}

		generator.writeEnd();
	}

	private void writeResourceFields(Resource resource) {
		writeIdentifiableFields(resource);

		// TYPE
		String type = resource.getResourceType();
		if(type!=null) {
			generator.write(JsonLabels.TYPE, type);
		}

//		// CHECKSUM
//		Checksum checksum = resource.getChecksum();
//		if(checksum!=null) {
//			generator.write(CHECKSUM, checksum.toString());
//		}
	}

	private void writeResource(Resource resource) {
		generator.writeStartObject();

		writeResourceFields(resource);

		generator.writeEnd();
	}

	private void writePerson(Person person) {
		generator.writeStartObject();

		writeIdentifiableFields(person);

		// ROLE
		String role = person.getRole();
		if(role!=null) {
			generator.write(JsonLabels.ROLE, role);
		}

		generator.writeEnd();
	}

	private void writeTool(Tool tool) {
		generator.writeStartObject(JsonLabels.TOOL);

		writeResourceFields(tool);

		// PARAMETERS
		String parameters = tool.getParameters();
		if(parameters!=null) {
			generator.write(JsonLabels.PARAMETERS, parameters);
		}

		// ENVIRONMENT
		String environment = tool.getEnvironment();
		if(environment!=null) {
			generator.write(JsonLabels.ENVIRONMENT, environment);
		}

		generator.writeEnd();
	}

	/**
	 * Writes a section (or all) of the given {@link Workflow}.
	 * Individual steps are formatted according to {@link #write(WorkflowStep)}.
	 *
	 * @param workflow
	 * @param steps
	 */
	public void writeWorkflow(Workflow workflow, Set<WorkflowStep> steps)
			throws IOException, InterruptedException {
		requireNonNull(workflow);
		requireNonNull(steps);
		checkArgument("No steps defined for output", !steps.isEmpty());
		checkState("Generator not initialized", generator!=null);

		generator.writeStartObject();

		generator.write(JsonLabels.TITLE, workflow.getTitle());
		generator.write(JsonLabels.DESCRIPTION, workflow.getDescription());

		writeSteps(steps);

		writeGraph(workflow, steps);

		generator.writeEnd();
	}

	private void writeSteps(Set<WorkflowStep> steps) throws IOException, InterruptedException {

		// For parallel sanity check against duplicate ids
		final Set<String> ids = new HashSet<>(steps.size());

		generator.writeStartArray(JsonLabels.STEPS);

		for(WorkflowStep step : steps) {
			String id = step.getId();
			if(id==null)
				throw new IllegalArgumentException("Step has no valid id");
			if(!ids.add(id))
				throw new IllegalArgumentException("Duplicate id encountered: "+id);

			write(step);
			if(Thread.interrupted())
				throw new InterruptedException();
		}

		generator.writeEnd();
	}

	private void writeGraph(Workflow workflow, Set<WorkflowStep> steps) {

		generator.writeStartObject(JsonLabels.GRAPH);

		for(WorkflowStep step : steps) {
			int nextStepCount = workflow.getNextStepCount(step);
			int previousStepCount = workflow.getPreviousStepCount(step);

			// Count steps written, -1 means JSON array hasn't been started yet
			int nextStepsWritten = -1;

			// For each next step check if we are actually allowed to write it
			for(int i=0; i<nextStepCount; i++) {
				WorkflowStep nextStep = workflow.getNextStep(step, i);
				if(steps.contains(nextStep)) {

					// Check if we have written the JSON array yet
					if(nextStepsWritten==-1) {
						generator.writeStartObject(step.getId());

						generator.writeStartArray(JsonLabels.FOLLOWED_BY);
						nextStepsWritten = 0;
					}

					generator.write(nextStep.getId());

					nextStepsWritten++;
				}
			}

			// If we actually wrote some steps, make sure to close the array context
			if(nextStepsWritten>0) {
				generator.writeEnd();
			}

			// Count steps written, -1 means JSON array hasn't been started yet
			int previousStepsWritten = -1;

			// For each previous step check if we are actually allowed to write it
			for(int i=0; i<previousStepCount; i++) {
				WorkflowStep previousStep = workflow.getPreviousStep(step, i);
				if(steps.contains(previousStep)) {

					// Check if we have written the JSON array yet
					if(previousStepsWritten==-1) {

						// Double check if we need to write the object part first
						if(nextStepsWritten==-1) {
							generator.writeStartObject(step.getId());
						}

						generator.writeStartArray(JsonLabels.PRECEEDED_BY);
						previousStepsWritten = 0;
					}

					generator.write(previousStep.getId());

					previousStepsWritten++;
				}
			}

			// If we actually wrote some steps, make sure to close the array context
			if(previousStepsWritten>0) {
				generator.writeEnd();
			}

			// Make sure to close the object part if we wrote anything
			if(previousStepsWritten>0 || nextStepsWritten>0) {
				generator.writeEnd();
			}
		}

		generator.writeEnd();
	}

	@Override
	public void close() throws IOException {
		if(generator!=null) {
			generator.close();
			generator = null;
		}
	}

}

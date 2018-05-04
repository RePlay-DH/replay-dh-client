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

import static bwfdm.replaydh.utils.RDHUtils.checkState;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.UUID;
import java.util.function.Supplier;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import javax.json.stream.JsonParsingException;

import bwfdm.replaydh.io.ObjectReader;
import bwfdm.replaydh.utils.Options;
import bwfdm.replaydh.utils.RDHUtils;
import bwfdm.replaydh.workflow.Identifier;
import bwfdm.replaydh.workflow.Person;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.Tool;
import bwfdm.replaydh.workflow.WorkflowStep;
import bwfdm.replaydh.workflow.impl.DefaultPerson;
import bwfdm.replaydh.workflow.impl.DefaultResource;
import bwfdm.replaydh.workflow.impl.DefaultTool;
import bwfdm.replaydh.workflow.schema.IdentifierSchema;
import bwfdm.replaydh.workflow.schema.IdentifierType;
import bwfdm.replaydh.workflow.schema.LabelSchema;
import bwfdm.replaydh.workflow.schema.WorkflowSchema;

/**
 * @author Markus Gärtner
 *
 */
public class JsonWorkflowStepReader implements ObjectReader<WorkflowStep> {

	public static final String SKIP_HEADER = "skipHeader";

	public static WorkflowStep parseStep(WorkflowSchema schema,
			Supplier<? extends WorkflowStep> stepFactory, String s, Options options) {
		if(options==null) {
			options = Options.emptyOptions;
		}

		// Ignore header section which is all content before the first empty line
		if(options.getBoolean(SKIP_HEADER)) {
			int emptyLineIndex = s.indexOf("\n\n");
			if(emptyLineIndex>-1) {
				s = s.substring(emptyLineIndex+2);
			}
		}

		StringReader sr = new StringReader(s);
		try(JsonWorkflowStepReader reader = new JsonWorkflowStepReader(schema, stepFactory)) {
			reader.init(sr, options);
			return reader.read();
		} catch (IOException | InterruptedException e) {
			return null;
		}
	}

	/**
	 * Supplier which is used to generate new steps
	 */
	private Supplier<? extends WorkflowStep> stepFactory;

	/**
	 * Declaration of labels and identifier types
	 */
	private WorkflowSchema schema;

	/**
	 * Declaration of allowed labels for current context
	 */
	private LabelSchema labelSchema;

	/**
	 * Declaration of allowed identifier types in current context
	 */
	private IdentifierSchema identifierSchema;

	/**
	 * ParseControl used for a sequence of parse operations
	 */
	private JsonParser parser;

	/**
	 * Last encountered event
	 */
	private Event event;

	private static String eventMismatchMessage(Event actual, Event...expected) {
		return "Unexpected event: expected "+expected+" - got "+actual;
	}

	@SuppressWarnings("unused")
	private static String valueMismatchMessage(String actual, String...expected) {
		return "Unexpected value: expected "+expected+" - got "+actual;
	}

	public JsonWorkflowStepReader(WorkflowSchema schema, Supplier<? extends WorkflowStep> stepFactory) {
		this.schema = requireNonNull(schema);
		this.stepFactory = requireNonNull(stepFactory);
	}

	/**
	 * Fetches next event, failing it there are no more events available
	 * or if the next event does not match the expected one.
	 */
	void expectEvent(Event expected) {
		event = parser.next();
		if(event!=expected)
			throw new JsonParsingException(eventMismatchMessage(event, expected), parser.getLocation());
	}

	/**
	 * Tries to fetch the next event if there are more events available
	 * and fails if the next event does not match the expected one.
	 */
	boolean tryExpectEvent(Event expected) {
		if(!parser.hasNext()) {
			return false;
		}

		expectEvent(expected);

		return true;
	}

	/**
	 * Fetches and returns the next event, failing if there are no more events available.
	 */
	Event advance() {
		event = parser.next();
		return event;
	}

	boolean tryAdvance() {
		if(!parser.hasNext()) {
			return false;
		}

		advance();

		return true;
	}

	/**
	 * @see bwfdm.replaydh.io.ObjectReader#init(java.io.Reader, bwfdm.replaydh.utils.Options)
	 */
	@Override
	public void init(Reader input, Options options) {
		requireNonNull(input);
		checkState("ParseControl already initialized", parser==null);

		parser = Json.createParserFactory(options).createParser(input);
	}

	/**
	 * @see bwfdm.replaydh.io.ObjectReader#close()
	 */
	@Override
	public void close() {
		if(parser!=null) {
			parser.close();
			parser = null;
		}

		stepFactory = null;
		schema = null;
		labelSchema = null;
		identifierSchema = null;
	}

	/**
	 * @see bwfdm.replaydh.io.ObjectReader#hasMoreData()
	 */
	@Override
	public boolean hasMoreData() throws IOException, InterruptedException {
		checkState("ParseControl not initialized", parser!=null);

		return parser.hasNext();
	}

	/**
	 * @see bwfdm.replaydh.io.ObjectReader#read()
	 */
	@Override
	public WorkflowStep read() throws IOException, InterruptedException {

		// Reset state
		event = null;

		expectEvent(Event.START_OBJECT);

		// Obtain step from external implementation
		WorkflowStep result = stepFactory.get();

		loop : while(tryAdvance()) {
			switch (event) {
			case KEY_NAME: {
				String key = parser.getString();
				switch (key) {
				case JsonLabels.INPUT: {
					expectEvent(Event.START_ARRAY);
					Resource input;
					while((input = readResource())!=null) {
						result.addInput(input);
					}
					// We're at END_ARRAY
				} break;

				case JsonLabels.PERSONS: {
					expectEvent(Event.START_ARRAY);
					Person person;
					while((person = readPerson())!=null) {
						result.addPerson(person);
					}
					// We're at END_ARRAY
				} break;

				case JsonLabels.TOOL:
					result.setTool(readTool());
					break;

				case JsonLabels.OUTPUT: {
					expectEvent(Event.START_ARRAY);
					Resource output;
					while((output = readResource())!=null) {
						result.addOutput(output);
					}
					// We're at END_ARRAY
				} break;

				case JsonLabels.ID:
					expectEvent(Event.VALUE_STRING);
					result.setId(parser.getString());
					break;

				case JsonLabels.TITLE:
					expectEvent(Event.VALUE_STRING);
					result.setTitle(parser.getString());
					break;

				case JsonLabels.DESCRIPTION:
					expectEvent(Event.VALUE_STRING);
					result.setDescription(parser.getString());
					break;

				case JsonLabels.TIMESTAMP:
					expectEvent(Event.VALUE_STRING);
					result.setRecordingTime(RDHUtils.parseTimestamp(parser.getString()));
					break;

				case JsonLabels.PROPERTIES: {
					expectEvent(Event.START_OBJECT);

					while(tryAdvance()) {
						if(event==Event.END_OBJECT) {
							break;
						} else {
							// Read single key-value pair
							String property = parser.getString();
							expectEvent(Event.VALUE_STRING);
							String value = parser.getString();

							result.setProperty(property, value);
						}
					}
				} break;

				default:
					throw new JsonParsingException("Unknown top-level key for workflow step data: "+key, parser.getLocation());
				}
			} break;

			case END_OBJECT:
				// End of our current workflow step
				break loop;

			default:
				throw new JsonParsingException(eventMismatchMessage(event, Event.KEY_NAME, Event.END_OBJECT), parser.getLocation());
			}
		}

		return result;
	}

	private Resource readResource() {
		if(!tryAdvance() || event!=Event.START_OBJECT) {
			return null;
		}

		DefaultResource resource = DefaultResource.blankResource();
		identifierSchema = schema.getResourceIdentifierSchema();
		labelSchema = schema.getResourceTypeSchema();

		loop : while(tryAdvance()) {
			switch (event) {
			case END_OBJECT:
				break loop;

			case KEY_NAME: {
				String key = parser.getString();
				switch (key) {
				case JsonLabels.TYPE:
					expectEvent(Event.VALUE_STRING);
					resource.setResourceType(parser.getString());
					break;

//				case CHECKSUM:
//					expectEvent(Event.VALUE_STRING);
//					resource.setChecksum(Checksum.parse(parser.getString()));
//					break;

				case JsonLabels.SYSTEM_ID:
					expectEvent(Event.VALUE_STRING);
					resource.setSystemId(UUID.fromString(parser.getString()));
					break;

				case JsonLabels.DESCRIPTION:
					expectEvent(Event.VALUE_STRING);
					resource.setDescription(parser.getString());
					break;

				case JsonLabels.IDENTIFIERS: {
					expectEvent(Event.START_ARRAY);
					Identifier identifier;
					while((identifier=readIdentifier())!=null) {
						resource.addIdentifier(identifier);
					}
					// We're at END_ARRAY
				} break;

				default:
					throw new JsonParsingException("Unknown key for resource data: "+key, parser.getLocation());
				}
			} break;

			default:
				throw new JsonParsingException(eventMismatchMessage(event, Event.KEY_NAME, Event.END_OBJECT), parser.getLocation());
			}
		}

		resource.ensureSystemId();

		identifierSchema = null;
		labelSchema = null;

		return resource;
	}

	private Identifier readIdentifier() {
		if(!tryAdvance() || event!=Event.START_OBJECT) {
			return null;
		}

		String id = null;
		String context = null;
		IdentifierType type = null;

		loop : while(tryAdvance()) {
			switch (event) {
			case END_OBJECT:
				break loop;

			case KEY_NAME: {
				String key = parser.getString();
				switch (key) {
				case JsonLabels.TYPE:
					expectEvent(Event.VALUE_STRING);
					type = IdentifierSchema.parseIdentifierType(identifierSchema, parser.getString());
					break;

				case JsonLabels.ID:
					expectEvent(Event.VALUE_STRING);
					id = parser.getString();
					break;

				case JsonLabels.CONTEXT:
					expectEvent(Event.VALUE_STRING);
					context = parser.getString();
					break;

				default:
					throw new JsonParsingException("Unknown key for identifier data: "+key, parser.getLocation());
				}
			} break;

			default:
				throw new JsonParsingException(eventMismatchMessage(event, Event.KEY_NAME, Event.END_OBJECT), parser.getLocation());
			}
		}

		if(type==null)
			throw new JsonParsingException("Missing type for identifier data: ", parser.getLocation());
		if(id==null)
			throw new JsonParsingException("Missing id for identifier data: ", parser.getLocation());


		return new Identifier(type, id, context);
	}

	private Person readPerson() {
		if(!tryAdvance() || event!=Event.START_OBJECT) {
			return null;
		}

		DefaultPerson person = DefaultPerson.blankPerson();
		identifierSchema = schema.getPersonIdentifierSchema();
		labelSchema = schema.getRoleSchema();

		loop : while(tryAdvance()) {
			switch (event) {
			case END_OBJECT:
				break loop;

			case KEY_NAME: {
				String key = parser.getString();
				switch (key) {

				case JsonLabels.ROLE:
					expectEvent(Event.VALUE_STRING);
					person.setRole(parser.getString());
					break;

				case JsonLabels.DESCRIPTION:
					expectEvent(Event.VALUE_STRING);
					person.setDescription(parser.getString());
					break;

				case JsonLabels.SYSTEM_ID:
					expectEvent(Event.VALUE_STRING);
					person.setSystemId(UUID.fromString(parser.getString()));
					break;

				case JsonLabels.IDENTIFIERS: {
					expectEvent(Event.START_ARRAY);
					Identifier identifier;
					while((identifier=readIdentifier())!=null) {
						person.addIdentifier(identifier);
					}
					// We're at END_ARRAY
				} break;

				default:
					throw new JsonParsingException("Unknown key for person data: "+key, parser.getLocation());
				}
			} break;

			default:
				throw new JsonParsingException(eventMismatchMessage(event, Event.KEY_NAME, Event.END_OBJECT), parser.getLocation());
			}
		}

		person.ensureSystemId();

		identifierSchema = null;
		labelSchema = null;

		return person;
	}

	private Tool readTool() {
		if(!tryAdvance() || event!=Event.START_OBJECT) {
			return null;
		}

		DefaultTool tool = DefaultTool.blankTool();
		identifierSchema = schema.getResourceIdentifierSchema();
		labelSchema = schema.getResourceTypeSchema();

		loop : while(tryAdvance()) {
			switch (event) {
			case END_OBJECT:
				break loop;

			case KEY_NAME: {
				String key = parser.getString();
				switch (key) {
				case JsonLabels.TYPE:
					expectEvent(Event.VALUE_STRING);
					tool.setResourceType(parser.getString());
					break;

//				case CHECKSUM:
//					expectEvent(Event.VALUE_STRING);
//					tool.setChecksum(Checksum.parse(parser.getString()));
//					break;

				case JsonLabels.ENVIRONMENT:
					expectEvent(Event.VALUE_STRING);
					tool.setEnvironment(parser.getString());
					break;

				case JsonLabels.PARAMETERS:
					expectEvent(Event.VALUE_STRING);
					tool.setParameters(parser.getString());
					break;

				case JsonLabels.DESCRIPTION:
					expectEvent(Event.VALUE_STRING);
					tool.setDescription(parser.getString());
					break;

				case JsonLabels.SYSTEM_ID:
					expectEvent(Event.VALUE_STRING);
					tool.setSystemId(UUID.fromString(parser.getString()));
					break;

				case JsonLabels.IDENTIFIERS: {
					expectEvent(Event.START_ARRAY);
					Identifier identifier;
					while((identifier=readIdentifier())!=null) {
						tool.addIdentifier(identifier);
					}
					// We're at END_ARRAY
				} break;

				default:
					throw new JsonParsingException("Unknown key for tool data: "+key, parser.getLocation());
				}
			} break;

			default:
				throw new JsonParsingException(eventMismatchMessage(event, Event.KEY_NAME, Event.END_OBJECT), parser.getLocation());
			}
		}

		tool.ensureSystemId();

		identifierSchema = null;
		labelSchema = null;

		return tool;
	}
}

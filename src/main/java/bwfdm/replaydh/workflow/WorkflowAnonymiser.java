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
package bwfdm.replaydh.workflow;

import static java.util.Objects.requireNonNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.workflow.impl.AbstractIdentifiable;
import bwfdm.replaydh.workflow.impl.DefaultPerson;
import bwfdm.replaydh.workflow.impl.DefaultResource;
import bwfdm.replaydh.workflow.impl.DefaultTool;
import bwfdm.replaydh.workflow.impl.DefaultWorkflow;
import bwfdm.replaydh.workflow.schema.IdentifierType;

/**
 * @author Markus Gärtner
 *
 */
public class WorkflowAnonymiser {

	private final Workflow source;

	private final DefaultWorkflow target;

	private final Map<WorkflowStep, WorkflowStep> stepMap = new IdentityHashMap<>();

	private final AtomicBoolean done = new AtomicBoolean(false);

	private final Anonymiser roles = new Anonymiser("role");
	private final Anonymiser files = new Anonymiser("file");
	private final Anonymiser resourceTypes = new Anonymiser("resourceType");
	private final Anonymiser names = new Anonymiser("name");
	private final Anonymiser urls = new Anonymiser("url");
	private final Anonymiser checksums = new Anonymiser("size") {
		/**
		 * @see bwfdm.replaydh.workflow.WorkflowAnonymiser.Anonymiser#createEntry(java.lang.String)
		 */
		@Override
		protected String createEntry(String source) {
			Checksum checksum = Checksum.parse(source);
			return base+IOUtils.readableSize(checksum.getSize());
		}
	};

	private final Map<String, Anonymiser> generics = new HashMap<>();

	public WorkflowAnonymiser(Workflow source) {
		this.source = requireNonNull(source);
		target = new DefaultWorkflow(source.getSchema());
	}

	public void anonymise() {
		if(done.compareAndSet(false, true)) {
			Set<WorkflowStep> steps = source.getAllSteps();

			// First path: anonymise the steps
			for(WorkflowStep step : steps) {
				stepMap.put(step, anonymise(step));
			}

			// Second path: re-create the graph structure with new steps
			Stack<WorkflowStep> pendingSteps = new Stack<>();
			pendingSteps.push(source.getInitialStep());
			while(!pendingSteps.isEmpty()) {
				WorkflowStep step = pendingSteps.pop();
				for(WorkflowStep next : source.getNextSteps(step)) {
					target.addWorkflowStep(lookup(step), lookup(next));
					pendingSteps.push(next);
				}
			}

			target.setTitle(anonymiseText(source.getTitle()));
			target.setDescription(anonymiseText(source.getDescription()));
		}
	}

	private WorkflowStep anonymise(WorkflowStep source) {
		if(source==this.source.getInitialStep()) {
			return target.getInitialStep();
		}

		WorkflowStep newStep = target.createWorkflowStep();

		newStep.setId(source.getId());
		newStep.setTitle(anonymiseText(source.getTitle()));
		newStep.setDescription(anonymiseText(source.getDescription()));

		newStep.setRecordingTime(source.getRecordingTime());

		Map<String, String> properties = source.getProperties();
		if(properties!=null && !properties.isEmpty()) {
			for(Entry<String, String> property : properties.entrySet()) {
				newStep.setProperty(property.getKey(), anonymiseText(property.getValue()));
			}
		}

		newStep.setTool(anonymise(source.getTool()));

		for(Resource input : source.getInput()) {
			newStep.addInput(anonymise(input));
		}

		for(Resource output : source.getOutput()) {
			newStep.addOutput(anonymise(output));
		}

		for(Person person : source.getPersons()) {
			newStep.addPerson(anonymise(person));
		}

		return newStep;
	}

	private static String anonymiseText(String text) {
		if(text==null) {
			return null;
		}
		return text.isEmpty() ? "empty" : "text:"+text.length();
	}

	@SuppressWarnings("unchecked")
	private <I extends Identifiable> I anonymise(I source) {
		if(source==null) {
			return null;
		}

		Identifiable result;

		switch (source.getType()) {
		case PERSON:
			result = DefaultPerson.blankPerson();
			processPerson((Person)source, (Person)result);
			break;

		case RESOURCE:
			result = DefaultResource.blankResource();
			processResource((Resource)source, (Resource)result);
			break;

		case TOOL:
			result = DefaultTool.blankTool();
			processTool((Tool)source, (Tool)result);
			break;

		default:
			throw new InternalError();
		}

		processIdentifiable(source, result);

		return (I) result;
	}

	private void processIdentifiable(Identifiable source, Identifiable target) {
		((AbstractIdentifiable)target).setSystemId(source.getSystemId());

		for(Identifier identifier : source.getIdentifiers()) {
			target.addIdentifier(anonymise(identifier));
		}
	}

	private Identifier anonymise(Identifier source) {
		IdentifierType type = source.getType();
		Anonymiser anonymiser;
		switch (type.getLabel()) {
		case IdentifierType.NAME:
		case IdentifierType.NAME_VERSION:
			anonymiser = names;
			break;
		case IdentifierType.CHECKSUM:
			anonymiser = checksums;
			break;
		case IdentifierType.PATH:
			anonymiser = files;
			break;
		case IdentifierType.URL:
			anonymiser = urls;
			break;

		default:
			anonymiser = generics(type.getLabel());
			break;
		}

		String id = anonymiser.anonymise(source.getId());
		String context = anonymiseText(source.getContext());

		return new Identifier(type, id, context);
	}

	private void processResource(Resource source, Resource target) {
		if(source.getResourceType()!=null) {
			target.setResourceType(resourceTypes.anonymise(source.getResourceType()));
		}
	}

	private void processPerson(Person source, Person target) {
		if(source.getRole()!=null) {
			target.setRole(roles.anonymise(source.getRole()));
		}
	}

	private void processTool(Tool source, Tool target) {
		if(source.getParameters()!=null) {
			target.setParameters(anonymiseText(source.getParameters()));
		}
		if(source.getEnvironment()!=null) {
			target.setEnvironment(anonymiseText(source.getEnvironment()));
		}
	}

	private Anonymiser generics(String type) {
		Anonymiser result = generics.get(type);
		if(result==null) {
			result = new Anonymiser(type);
			generics.put(type, result);
		}
		return result;
	}

	/**
	 * @return the source
	 */
	public Workflow getSource() {
		return source;
	}

	/**
	 * @return the target
	 */
	public Workflow getTarget() {
		return target;
	}

	private WorkflowStep lookup(WorkflowStep source) {
		WorkflowStep target = map(source);
		if(target==null)
			throw new IllegalStateException("No anonymised version of step available: "+source.getId());
		return target;
	}

	public WorkflowStep map(WorkflowStep source) {
		return stepMap.get(requireNonNull(source));
	}

	public Set<WorkflowStep> map(Set<WorkflowStep> source) {
		Set<WorkflowStep> result = new HashSet<>();
		for(WorkflowStep step : source) {
			result.add(lookup(step));
		}
		return result;
	}

	private static class Anonymiser {
		protected final Map<String, String> map = new HashMap<>();

		protected final String base;

		public Anonymiser(String base) {
			requireNonNull(base);

			if(!base.endsWith("_")) {
				base += '_';
			}

			this.base = base;
		}

		public String anonymise(String source) {
			String result = map.get(source);
			if(result==null) {
				result = createEntry(source);
				map.put(source, result);
			}
			return result;
		}

		protected String createEntry(String source) {
			return base+String.valueOf(map.size()+1);
		}
	}
}

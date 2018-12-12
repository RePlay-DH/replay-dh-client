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
package bwfdm.replaydh.workflow.impl;

import static java.util.Objects.requireNonNull;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import bwfdm.replaydh.utils.IdentityHashSet;
import bwfdm.replaydh.workflow.Person;
import bwfdm.replaydh.workflow.Resource;
import bwfdm.replaydh.workflow.Tool;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowListener;
import bwfdm.replaydh.workflow.WorkflowStep;

public class DefaultWorkflowStep implements WorkflowStep {

	private final transient DefaultWorkflow workflow;

	private LocalDateTime recordingTime;
	private String title;
	private String description;
	private volatile Set<Resource> input;
	private volatile Set<Resource> output;
	private volatile Set<Person> persons;
	private Tool tool;

	private String id;

	private volatile Map<String, String> properties;

	private volatile boolean added = false;

	public DefaultWorkflowStep(DefaultWorkflow workflow) {
		this.workflow = requireNonNull(workflow);
		id = DefaultWorkflow.UNSET_ID;
	}

	protected Set<Resource> input(boolean createIfMissing) {
		Set<Resource> input = this.input;
		if(input==null && createIfMissing) {
			synchronized (this) {
				if((input=this.input)==null) {
					input = new IdentityHashSet<>();
					this.input = input;
				}
			}
		}
		return input;
	}

	protected Set<Resource> output(boolean createIfMissing) {
		Set<Resource> output = this.output;
		if(output==null && createIfMissing) {
			synchronized (this) {
				if((output=this.output)==null) {
					output = new IdentityHashSet<>();
					this.output = output;
				}
			}
		}
		return output;
	}

	protected Set<Person> persons(boolean createIfMissing) {
		Set<Person> persons = this.persons;
		if(persons==null && createIfMissing) {
			synchronized (this) {
				if((persons=this.persons)==null) {
					persons = new IdentityHashSet<>();
					this.persons = persons;
				}
			}
		}
		return persons;
	}

	protected Map<String, String> properties(boolean createIfMissing) {
		Map<String, String> properties = this.properties;
		if(properties==null && createIfMissing) {
			synchronized (this) {
				if((properties=this.properties)==null) {
					properties = new HashMap<>();
					this.properties = properties;
				}
			}
		}
		return properties;
	}

	private static String nonNull(String s) {
		return s==null ? "" : s;
	}

	private static void clear(Collection<?> col) {
		if(col!=null) {
			col.clear();
		}
	}

	private static void clear(Map<?,?> map) {
		if(map!=null) {
			map.clear();
		}
	}

	@Override
	public void copyFrom(WorkflowStep source) {

		// Reset all buffer collections
		clear(input);
		clear(output);
		clear(persons);
		tool = null;
		clear(properties);

		// Directly assign simply fields
		id = source.getId();
		title = nonNull(source.getTitle());
		description = nonNull(source.getDescription());

		// Copy over collection content via clones
		source.getInput().forEach(r -> addInput(DefaultResource.copyResource(r)));
		source.getOutput().forEach(r -> addOutput(DefaultResource.copyResource(r)));
		source.getPersons().forEach(p -> addPerson(DefaultPerson.copyPerson(p)));
		if(source.getTool()!=null) {
			setTool(DefaultTool.copyTool(source.getTool()));
		}
		source.getProperties().forEach((k, v) -> setProperty(k, v));
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#getId()
	 */
	@Override
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	@Override
	public void setId(String id) {
		requireNonNull(id);

		if(Objects.equals(this.id, id)) {
			return;
		}

		if(added) {
//			id = workflow.acceptOrCreateNewId(id);
			//TODO maybe check if id is non-null first?
			workflow.checkUniqueId(id);
		}

		String oldId = this.id;

		this.id = id;

		if(added) {
			workflow.idChanged(this, oldId, id);
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#isAdded()
	 */
	@Override
	public boolean isAdded() {
		return added;
	}

	@Override
	public void addNotify() {
		added = true;
	}

	@Override
	public void removeNotify() {
		added = false;
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#getWorkflow()
	 */
	@Override
	public Workflow getWorkflow() {
		return workflow;
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#getRecordingTime()
	 */
	@Override
	public LocalDateTime getRecordingTime() {
		return recordingTime;
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#getTitle()
	 */
	@Override
	public String getTitle() {
		return title;
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#getInput()
	 */
	@Override
	public Set<Resource> getInput() {
		Set<Resource> input = input(false);
		return input==null ? Collections.emptySet() : Collections.unmodifiableSet(input);
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#getInputCount()
	 */
	@Override
	public int getInputCount() {
		Set<Resource> input = input(false);
		return input==null ? 0 : input.size();
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#getOutput()
	 */
	@Override
	public Set<Resource> getOutput() {
		Set<Resource> output = output(false);
		return output==null ? Collections.emptySet() : Collections.unmodifiableSet(output);
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#getOutputCount()
	 */
	@Override
	public int getOutputCount() {
		Set<Resource> output = output(false);
		return output==null ? 0 : output.size();
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#getPersons()
	 */
	@Override
	public Set<Person> getPersons() {
		Set<Person> persons = persons(false);
		return persons==null ? Collections.emptySet() : Collections.unmodifiableSet(persons);
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#getPersonsCount()
	 */
	@Override
	public int getPersonsCount() {
		Set<Person> persons = persons(false);
		return persons==null ? 0 : persons.size();
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#getTool()
	 */
	@Override
	public Tool getTool() {
		return tool;
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#setRecordingTime(java.time.LocalDateTime)
	 */
	@Override
	public void setRecordingTime(LocalDateTime time) {
		if(time!=null) {
			time = time.withNano(0); // To ensure we can properly use the recording time for equality checks
		}
		this.recordingTime = time;
		if(added) {
			workflow.fireWorkflowStepPropertyChanged(this, WorkflowListener.PROPERTY_RECORDING_TIME);
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#setTitle(java.lang.String)
	 */
	@Override
	public void setTitle(String title) {
		this.title = title;
		if(added) {
			workflow.fireWorkflowStepPropertyChanged(this, WorkflowListener.PROPERTY_TITLE);
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#setDescription(java.lang.String)
	 */
	@Override
	public void setDescription(String description) {
		this.description = description;
		if(added) {
			workflow.fireWorkflowStepPropertyChanged(this, WorkflowListener.PROPERTY_DESCRIPTION);
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#addInput(bwfdm.replaydh.workflow.Resource)
	 */
	@Override
	public void addInput(Resource resource) {
		requireNonNull(resource);

		if(!input(true).add(resource))
			throw new IllegalArgumentException("Duplicate input resource: "+resource);

		if(added) {
			workflow.fireWorkflowStepPropertyChanged(this, WorkflowListener.PROPERTY_INPUT);
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#removeInput(bwfdm.replaydh.workflow.Resource)
	 */
	@Override
	public void removeInput(Resource resource) {
		requireNonNull(resource);

		Set<Resource> input = input(false);
		if(input==null || !input.remove(resource))
			throw new IllegalArgumentException("Unknown input resource: "+resource);

		if(added) {
			workflow.fireWorkflowStepPropertyChanged(this, WorkflowListener.PROPERTY_INPUT);
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#addOutput(bwfdm.replaydh.workflow.Resource)
	 */
	@Override
	public void addOutput(Resource resource) {
		requireNonNull(resource);

		if(!output(true).add(resource))
			throw new IllegalArgumentException("Duplicate output resource: "+resource);

//		System.out.println(hashCode()+" Adding output:"+resource);

		if(added) {
			workflow.fireWorkflowStepPropertyChanged(this, WorkflowListener.PROPERTY_OUTPUT);
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#removeOutput(bwfdm.replaydh.workflow.Resource)
	 */
	@Override
	public void removeOutput(Resource resource) {
		requireNonNull(resource);

		Set<Resource> output = output(false);
		if(output==null || !output.remove(resource))
			throw new IllegalArgumentException("Unknown output resource: "+resource);

		if(added) {
			workflow.fireWorkflowStepPropertyChanged(this, WorkflowListener.PROPERTY_OUTPUT);
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#addPerson(bwfdm.replaydh.workflow.Person)
	 */
	@Override
	public void addPerson(Person person) {
		requireNonNull(person);

		if(!persons(true).add(person))
			throw new IllegalArgumentException("Duplicate person: "+person);

		if(added) {
			workflow.fireWorkflowStepPropertyChanged(this, WorkflowListener.PROPERTY_PERSONS);
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#removePerson(bwfdm.replaydh.workflow.Person)
	 */
	@Override
	public void removePerson(Person person) {
		requireNonNull(person);

		Set<Person> persons = persons(false);
		if(persons==null || !persons.remove(person))
			throw new IllegalArgumentException("Unknown person: "+person);

		if(added) {
			workflow.fireWorkflowStepPropertyChanged(this, WorkflowListener.PROPERTY_PERSONS);
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#setTool(bwfdm.replaydh.workflow.Tool)
	 */
	@Override
	public void setTool(Tool tool) {
		this.tool = tool;
		if(added) {
			workflow.fireWorkflowStepPropertyChanged(this, WorkflowListener.PROPERTY_TOOL);
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#getProperty(java.lang.String)
	 */
	@Override
	public String getProperty(String key) {
		Map<String, String> properties = properties(false);
		return properties==null ? null : properties.get(key);
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#setProperty(java.lang.String, java.lang.String)
	 */
	@Override
	public String setProperty(String key, String value) {
		String oldValue = properties(true).put(key, value);

		if(added) {
			workflow.fireWorkflowStepChanged(this);
		}

		return oldValue;
	}

	/**
	 * Removes all mappings for properties in this workflow step.
	 */
	public void clearProperties() {
		Map<String, String> properties = properties(false);
		if(properties!=null && !properties.isEmpty()) {
			properties.clear();

			if(added) {
				workflow.fireWorkflowStepChanged(this);
			}
		}
	}

	/**
	 * @see bwfdm.replaydh.workflow.WorkflowStep#getProperties()
	 */
	@Override
	public Map<String, String> getProperties() {
		Map<String, String> properties = properties(false);
		return properties==null ? Collections.emptyMap() : Collections.unmodifiableMap(properties);
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Workflowstep@"+getTitle();
	}
}

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

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Models the result of a single action in a workflow graph.
 *
 * @author Markus
 */
public interface WorkflowStep {

	public static final String PROPERTY_INTERNAL_INFO = "RDH_internalInfo";
	public String FOREIGN_COMMIT_HEADER = "???";

	/**
	 * Returns the unique id for this step that was assigned by the host
	 * workflow. Note that this id is only unique within that workflow.
	 *
	 * @return
	 */
	String getId();

	/**
	 * Signal from the hosting workflow that the step has been properly added.
	 */
	void addNotify();

	/**
	 * Signal from the hosting workflow that the step has been removed.
	 */
	void removeNotify();

	/**
	 * Tells whether or not the workflow step has been properly added
	 * to the hosting workflow.
	 * @return
	 */
	boolean isAdded();

	Workflow getWorkflow();

    /**
     * Returns the timestamp at which the workflow step has been recorded.
     *
     * @return
     */
    LocalDateTime getRecordingTime();

    /**
     * Returns the user defined title of this workflow step.
     *
     * @return
     */
    String getTitle();

    /**
     * Returns the optional textual description provided by the user for
     * this workflow step or {@code null} if the user chose not to describe
     * this step in detail.
     *
     * @return
     */
    String getDescription();

    /**
     * Returns the resources that were used as input for this worflow step.
     * The returned {@link Set} can be empty if this step describes the
     * creation of new resources and no additional input resources were used.
     *
     * @return
     */
    Set<Resource> getInput();

    default int getInputCount() {
    	return getInput().size();
    }

    /**
     * Returns the resources that were created as a result of this workflow
     * step.
     * The returned {@link Set} is never empty, since
     *
     * @return
     */
    Set<Resource> getOutput();

    default int getOutputCount() {
    	return getOutput().size();
    }

    /**
     * Returns the persons directly involved in this workflow step.
     * Note that the user himself is always implicitly participating in
     * every workflow step recorded under him.
     *
     * @return
     */
    Set<Person> getPersons();

    default int getPersonsCount() {
    	return getPersons().size();
    }

    /**
     * Returns the optional tool involved in case this is an automatic
     * workflow step.
     *
     * @return
     */
    Tool getTool();

    /**
     * Fetches the property value associated with the specified {@code key}
     * or {@code null} if there is no such property or the property's value
     * has previously been {@link #setProperty(String, String) set} to {@code null}.
     *
     * @param key
     * @return
     */
    String getProperty(String key);

    /**
     * Returns a read-only view on all the properties available for this step.
     * <p>
     * The returned map is backed by this workflow step, so subsequent modifications
     * via the {@link #setProperty(String, String)} method will be reflected in it.
     *
     * @return
     */
    Map<String, String> getProperties();

    void forEachIdentifiable(Consumer<? super Identifiable> action);

    // MODIFICATION METHODS

    void setRecordingTime(LocalDateTime time);

    void setTitle(String title);

    void setDescription(String description);

    void setId(String id);

    void addInput(Resource resource);
    void removeInput(Resource resource);

    void addOutput(Resource resource);
    void removeOutput(Resource resource);

    void addPerson(Person person);
    void removePerson(Person person);

    void setTool(Tool tool);

    String setProperty(String key, String value);

    /**
     * Overrides the content of this step with the contents of the specified
     * {@code source} step.
     *
     * @param source
     */
    void copyFrom(WorkflowStep source);

	public static final Comparator<WorkflowStep> CHRONOLOGICAL_ORDER = (s1, s2) -> {
    	LocalDateTime t1 = s1.getRecordingTime();
    	LocalDateTime t2 = s2.getRecordingTime();

    	if(t1==t2) {
    		return 0;
    	} else if(t1==null) {
    		return -1;
    	} else if(t2==null) {
    		return 1;
    	} else {
    		return t1.compareTo(t2);
    	}
    };
}

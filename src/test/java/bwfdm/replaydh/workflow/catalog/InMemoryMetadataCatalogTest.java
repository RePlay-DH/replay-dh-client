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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import bwfdm.replaydh.core.RDHClient;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.git.JGitAdapter;
import bwfdm.replaydh.io.FileTracker;
import bwfdm.replaydh.utils.Mutable.MutableObject;
import bwfdm.replaydh.workflow.Workflow;

/**
 * @author Markus Gärtner
 *
 */
public class InMemoryMetadataCatalogTest {
	RDHEnvironment environment;
	RDHClient client;
	JGitAdapter fileTracker;
	Supplier<Workflow> workflowGen;
	Workflow workflow;

	InMemoryMetadataCatalog catalog;

	/**
	 * @throws java.lang.Exception
	 */
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		environment = mock(RDHEnvironment.class);
		client = mock(RDHClient.class);
		fileTracker = mock(JGitAdapter.class);
		workflowGen = mock(Supplier.class);
		workflow = mock(Workflow.class);

		when(environment.getClient()).thenReturn(client);
		when(client.getFileTracker()).thenReturn(fileTracker);
		when(client.getWorkflowSource()).thenReturn(workflowGen);
		when(workflowGen.get()).thenReturn(workflow);

		when(workflow.isClosed()).thenReturn(Boolean.FALSE);

		catalog = new InMemoryMetadataCatalog();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		environment = null;
		client = null;
		fileTracker = null;
		workflowGen = null;
		workflow = null;
		catalog = null;
	}

	@Test
	public void testStart() throws Exception {
		assertTrue(catalog.start(environment));

		verify(workflowGen).get();
		verify(workflow).addWorkflowListener(any());
		verify(workflow).forEachStep(any());
		verify(fileTracker).addPropertyChangeListener(eq(FileTracker.NAME_WORKFLOW), any());
	}

	@Test
	public void testStartAndStop() throws Exception {
		assertTrue(catalog.start(environment));

		verify(workflowGen).get();
		verify(workflow).addWorkflowListener(any());
		verify(fileTracker).addPropertyChangeListener(eq(FileTracker.NAME_WORKFLOW), any());

		catalog.stop(environment);

		verify(workflowGen, times(2)).get();
		verify(workflow).removeWorkflowListener(any());
		verify(fileTracker).removePropertyChangeListener(eq(FileTracker.NAME_WORKFLOW), any());
	}

	@Test
	public void testWorkflowChange() throws Exception {
		MutableObject<PropertyChangeListener> listener = new MutableObject<>();

		doAnswer(inv -> {
			listener.set(inv.getArguments()[1]);
			return null;
		}).when(fileTracker).addPropertyChangeListener(eq(FileTracker.NAME_WORKFLOW), any());

		assertTrue(catalog.start(environment));

		verify(workflowGen).get();
		verify(workflow).addWorkflowListener(any());
		verify(workflow).forEachStep(any());
		verify(fileTracker).addPropertyChangeListener(eq(FileTracker.NAME_WORKFLOW), any());

		Workflow newWorkflow = mock(Workflow.class);
		when(newWorkflow.isClosed()).thenReturn(Boolean.FALSE);

		listener.get().propertyChange(new PropertyChangeEvent(
				fileTracker, FileTracker.NAME_WORKFLOW, workflow, newWorkflow));

		verify(workflow).removeWorkflowListener(any());
		verify(newWorkflow).addWorkflowListener(any());
		verify(newWorkflow).forEachStep(any());
	}

	@Test
	public void testClosedWorkflow() throws Exception {
		when(workflow.isClosed()).thenReturn(Boolean.TRUE);

		assertTrue(catalog.start(environment));

		verify(workflowGen).get();
		verify(workflow).addWorkflowListener(any());
		verify(workflow, never()).forEachStep(any());
		verify(fileTracker).addPropertyChangeListener(eq(FileTracker.NAME_WORKFLOW), any());
	}

}

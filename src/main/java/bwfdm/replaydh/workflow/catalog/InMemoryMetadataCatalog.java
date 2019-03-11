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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.event.ChangeEvent;

import bwfdm.replaydh.core.AbstractRDHTool;
import bwfdm.replaydh.core.RDHClient;
import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.core.RDHTool;
import bwfdm.replaydh.io.FileTracker;
import bwfdm.replaydh.workflow.Identifiable;
import bwfdm.replaydh.workflow.Workflow;
import bwfdm.replaydh.workflow.WorkflowAdapter;
import bwfdm.replaydh.workflow.WorkflowStep;

/**
 * Implements a simple in-memory solution for the {@link MetadataCatalog} interface.
 * <p>
 * The storage/search-space for full grown {@link Identifiable} objects is the entire
 * content of the current {@link Workflow}. This implementation is based on the assumption
 * that typical workflows will stay relatively small (sub 10k steps) so that running live
 * searches on the elements in them is still going to be fast enough.
 *
 *
 * @author Markus Gärtner
 *
 */
public class InMemoryMetadataCatalog extends AbstractRDHTool implements MetadataCatalog {

	private final Handler handler = new Handler();

	private final MetadataCache cache = new MetadataCache();

	/**
	 * @see bwfdm.replaydh.core.AbstractRDHTool#start(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public boolean start(RDHEnvironment environment) throws RDHLifecycleException {
		if(!super.start(environment)) {
			return false;
		}

		RDHClient client = environment.getClient();

		// We should start late enough to be able to access the file tracker without any issues
		RDHTool fileTracker = client.getFileTracker();
		fileTracker.addPropertyChangeListener(FileTracker.NAME_WORKFLOW, handler);

		// If a workflow is available, go and cache its data
		Workflow workflow = client.getWorkflowSource().get();
		if(workflow!=null) {
			workflow.addWorkflowListener(handler);
			cache.reload(workflow);
		}

		return true;
	}

	/**
	 * @see bwfdm.replaydh.core.AbstractRDHTool#stop(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public void stop(RDHEnvironment environment) throws RDHLifecycleException {
		cache.clear();

		RDHClient client = environment.getClient();

		RDHTool fileTracker = client.getFileTracker();
		fileTracker.removePropertyChangeListener(FileTracker.NAME_WORKFLOW, handler);

		Workflow workflow = client.getWorkflowSource().get();
		if(workflow!=null) {
			workflow.removeWorkflowListener(handler);
		}

		super.stop(environment);
	}

	@Override
	public Result query(QuerySettings settings, String fragment) throws CatalogException {
		return new LazyCloningResult(cache.query(settings, fragment));
	}

	@Override
	public Result query(QuerySettings settings, List<Constraint> constraints) throws CatalogException {
		return new LazyCloningResult(cache.query(settings, constraints));
	}

	@Override
	public List<String> suggest(QuerySettings settings, Identifiable context, String key, String valuePrefix)
			throws CatalogException {
		return cache.suggest(settings, context, key, valuePrefix);
	}

	private void registerWorkflowListener(Workflow workflow) {
		if(workflow!=null) {
			workflow.addWorkflowListener(handler);
		}
	}

	private void unregisterWorkflowListener(Workflow workflow) {
		if(workflow!=null) {
			workflow.removeWorkflowListener(handler);
		}
	}

	private class Handler extends WorkflowAdapter implements PropertyChangeListener {

		@Override
		public void stateChanged(ChangeEvent e) {
			if(e.getSource() instanceof Workflow) {
				cache.reload((Workflow) e.getSource());
			}
		}

		@Override
		public void workflowStepAdded(Workflow workflow, WorkflowStep step) {
			cache.addWorkflowStep(step);
		}

		@Override
		public void workflowStepRemoved(Workflow workflow, WorkflowStep step) {
			cache.removeWorkflowStep(step);
		}

		@Override
		public void workflowStepChanged(Workflow workflow, WorkflowStep step) {
			cache.updateWorkflowStep(step);
		}

		@Override
		public void propertyChange(PropertyChangeEvent pce) {
			if(!FileTracker.NAME_WORKFLOW.equals(pce.getPropertyName())) {
				return;
			}

			unregisterWorkflowListener((Workflow) pce.getOldValue());

			Workflow newWorkflow = (Workflow)pce.getNewValue();
			registerWorkflowListener(newWorkflow);
			cache.reload(newWorkflow);
		}

	}
}

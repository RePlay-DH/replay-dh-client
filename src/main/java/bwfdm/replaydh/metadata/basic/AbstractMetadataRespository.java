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
package bwfdm.replaydh.metadata.basic;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static java.util.Objects.requireNonNull;

import java.beans.PropertyChangeListener;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHLifecycleException;
import bwfdm.replaydh.metadata.MetadataBuilder;
import bwfdm.replaydh.metadata.MetadataEditor;
import bwfdm.replaydh.metadata.MetadataException;
import bwfdm.replaydh.metadata.MetadataListener;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRecord.Target;
import bwfdm.replaydh.metadata.MetadataRepository;
import bwfdm.replaydh.metadata.MetadataSchema;
import bwfdm.replaydh.utils.AbstractSchemaManager;
import bwfdm.replaydh.utils.Transaction;

/**
 * @author Markus Gärtner
 *
 */
public abstract class AbstractMetadataRespository extends AbstractSchemaManager<MetadataSchema>
		implements MetadataRepository {

	/**
	 * Listeners to be notified for any {@link MetadataRecord} related events.
	 */
	private final List<MetadataListener> listeners = new CopyOnWriteArrayList<>();

	/**
	 * Transaction wrapper linking to {@link #endTransaction()}
	 */
	private final Transaction transaction = Transaction.withEndCallback(this::endTransaction);

	/**
	 * Currently active builds
	 */
	private final Set<MetadataRecord> pendingBuilds = Collections.synchronizedSet(new HashSet<>());

	/**
	 * Currently active edits
	 */
	private final Set<MetadataRecord> pendingEdits = Collections.synchronizedSet(new HashSet<>());

	private static final Logger log = LoggerFactory.getLogger(AbstractMetadataRespository.class);

	private final PropertyChangeListener workspaceObserver = pce -> workspaceChanged();

	/**
	 * @see bwfdm.replaydh.core.AbstractRDHTool#start(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public boolean start(RDHEnvironment environment) throws RDHLifecycleException {
		if(!super.start(environment)) {
			return false;
		}

		environment.addPropertyChangeListener(RDHEnvironment.NAME_WORKSPACE, workspaceObserver);

		return true;
	}

	/**
	 * @see bwfdm.replaydh.core.AbstractRDHTool#stop(bwfdm.replaydh.core.RDHEnvironment)
	 */
	@Override
	public void stop(RDHEnvironment environment) throws RDHLifecycleException {
		environment.removePropertyChangeListener(RDHEnvironment.NAME_WORKSPACE, workspaceObserver);

		super.stop(environment);
	}

	protected void workspaceChanged() {
		// Hook for subclasses to react to a new workspace directory
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#toSimpleText(bwfdm.replaydh.metadata.MetadataRecord)
	 */
	@Override
	public String toSimpleText(MetadataRecord record) {
		requireNonNull(record);
		if(record.getEntryCount()==0) {
			return null;
		}

		List<String> keys = new ArrayList<>(record.getEntryNames());
		Collections.sort(keys);

		StringWriter writer = new StringWriter(50 * record.getEntryCount());
		for(String key : keys) {
			record.forEachEntry(key, e -> writer.append(e.getName())
					.append('=').append(e.getValue()).append(System.lineSeparator()));
		}


		return writer.toString().trim();
	}

	protected void fireMetadataRecordAdded(MetadataRecord record) {
		if(listeners.isEmpty()) {
			return;
		}
		for(MetadataListener listener : listeners) {
			listener.metadataRecordAdded(this, record);
		}
	}

	protected void fireMetadataRecordChanged(MetadataRecord record) {
		if(listeners.isEmpty()) {
			return;
		}
		for(MetadataListener listener : listeners) {
			listener.metadataRecordChanged(this, record);
		}
	}

	protected void fireMetadataRecordRemoved(MetadataRecord record) {
		if(listeners.isEmpty()) {
			return;
		}
		for(MetadataListener listener : listeners) {
			listener.metadataRecordRemoved(this, record);
		}
	}

	/**
	 * Callback used by this repositories {@link Transaction} wrapper.
	 * <p>
	 * The default implementation clears pending edits and builds and
	 * sends a warning to the logger for all of them.
	 * <p>
	 * If subclasses wish to override this method they should make sure
	 * to include a call to {@code super.endTransaction()} <b>before</b>
	 * any additional implementation specific cleanup work.
	 */
	protected void endTransaction() {
		cleanupPendingBuildsAndEdits();
	}

	/**
	 * {@link Set#clear() clears} all pending builds and edits and sends
	 * a warning message to the logger for them.
	 */
	protected void cleanupPendingBuildsAndEdits() {
		if(!pendingBuilds.isEmpty()) {
			StringBuilder sb = new StringBuilder("Unfinished builds pending for the following resources:");
			for(MetadataRecord record : pendingBuilds) {
				sb.append('\n').append(record.getTarget());
			}

			pendingBuilds.clear();
			log.warn(sb.toString());
		}

		if(!pendingEdits.isEmpty()) {
			StringBuilder sb = new StringBuilder("Unfinished edits pending for the following resources:");
			for(MetadataRecord record : pendingEdits) {
				sb.append('\n').append(record.getTarget());
			}

			pendingEdits.clear();
			log.warn(sb.toString());
		}
	}

	public boolean isTransactionInProgress() {
		return transaction.isTransactionInProgress();
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#beginUpdate()
	 */
	@Override
	public void beginUpdate() {
		transaction.beginUpdate();
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#endUpdate()
	 */
	@Override
	public void endUpdate() {
		transaction.endUpdate();
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#addMetadataListener(bwfdm.replaydh.metadata.MetadataListener)
	 */
	@Override
	public void addMetadataListener(MetadataListener listener) {
		listeners.add(listener);
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#removeMetadataListener(bwfdm.replaydh.metadata.MetadataListener)
	 */
	@Override
	public void removeMetadataListener(MetadataListener listener) {
		listeners.add(listener);
	}

	@Override
	public MutableMetadataRecord newRecord(Target target, String schemaId) {
		return new DefaultMetadataRecord(target, schemaId);
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataRepository#createBuilder(Target)
	 */
	@Override
	public MetadataBuilder createBuilder(Target target, String schemaId) {
		requireNonNull(target);
		requireNonNull(schemaId);

		//TODO wrap into transaction context

		MetadataSchema schema = lookupSchema(schemaId);
		if(schema==null) {
			schema = getFallbackSchema();
		}

		MutableMetadataRecord record = newRecord(target, schemaId);

		return new DefaultMetadataBuilder(schema, record) {
			/**
			 * @see bwfdm.replaydh.metadata.basic.DefaultMetadataBuilder#beforeStart()
			 */
			@Override
			protected void beforeStartBuild() {
				beginBuild(getRecord());
			}
			/**
			 * @see bwfdm.replaydh.metadata.basic.DefaultMetadataBuilder#beforeFinishBuild()
			 */
			@Override
			protected void beforeEndBuild(boolean cancel) {
				endBuild(getRecord(), cancel);
			}
		};
	}

	/**
	 * Verifies that this is the first active build for the given {@code record}.
	 * <p>
	 * Subclasses that wish to add custom logic to this callback should make sure to
	 * call this super method <b>before</b> their own handler code.
	 *
	 * @param record
	 */
	protected void beginBuild(MetadataRecord record) {
		if(!pendingBuilds.add(record))
			throw new MetadataException("Build already in progress for resource: "+record.getTarget());
	}

	/**
	 * Verifies that we indeed have an active build for the given {@code record}.
	 * <p>
	 * Subclasses that wish to add custom logic to this callback should make sure to
	 * call this super method <b>before</b> their own handler code.
	 *
	 * @param record
	 * @param discard
	 */
	protected void endBuild(MetadataRecord record, boolean cancel) {
		if(!pendingBuilds.remove(record))
			throw new MetadataException("No build in progress for resource: "+record.getTarget()
				+" (make sue to finish building within a transaction context)");
	}

	/**
	 * Instantiates a new {@link DefaultMetadataEditor} that links its internal
	 * pre- and post-edit callbacks to the matching methods of this repository.
	 *
	 * @see bwfdm.replaydh.metadata.MetadataRepository#createEditor(bwfdm.replaydh.metadata.MetadataRecord)
	 */
	@Override
	public MetadataEditor createEditor(MetadataRecord record) {
		requireNonNull(record);
		checkArgument("Provided metadata record is not mutable", record instanceof MutableMetadataRecord);

		MetadataSchema schema = lookupSchema(record.getSchemaId());

		// Produce a new editor whose pre-final callbacks are linked to endEdit() calls
		return new DefaultMetadataEditor(schema, (MutableMetadataRecord) record) {
			/**
			 * @see bwfdm.replaydh.metadata.basic.DefaultMetadataEditor#beforeStart()
			 */
			@Override
			protected void beforeStart() {
				beginEdit(getOriginalMetadataRecord());
			}
			/**
			 * @see bwfdm.replaydh.metadata.basic.DefaultMetadataEditor#beforeCommitEdit()
			 */
			@Override
			protected void beforeEndEdit(boolean discard) {
				AbstractMetadataRespository.this.beforeEndEdit(getOriginalMetadataRecord(), discard);
			}

			/**
			 * @see bwfdm.replaydh.metadata.basic.DefaultMetadataEditor#afterEndEdit(boolean)
			 */
			@Override
			protected void afterEndEdit(boolean discard) {
				AbstractMetadataRespository.this.afterEndEdit(getOriginalMetadataRecord(), discard);
			}
		};
	}

	/**
	 * Verifies that this is the first active edit for the given {@code record}.
	 * <p>
	 * Subclasses that wish to add custom logic to this callback should make sure to
	 * call this super method <b>before</b> their own handler code.
	 *
	 * @param record
	 */
	protected void beginEdit(MetadataRecord record) {
		if(!pendingEdits.add(record))
			throw new MetadataException("Edit already in progress for resource: "+record.getTarget());
	}

	/**
	 * Verifies that we indeed have an active edit for the given {@code record}.
	 * <p>
	 * Subclasses that wish to add custom logic to this callback should make sure to
	 * call this super method <b>before</b> their own handler code.
	 *
	 * @param record
	 * @param discard
	 */
	protected void beforeEndEdit(MetadataRecord record, boolean discard) {
		if(!pendingEdits.remove(record))
			throw new MetadataException("No edit in progress for resource: "+record.getTarget()
				+" (make sue to finish editing within a transaction context)");
	}

	/**
	 * If the edit ended in a {@link MetadataEditor#commit()} then this implementation
	 * publishes the change via {@link MetadataListener#metadataRecordChanged(MetadataRepository, MetadataRecord)}
	 * of all registered listeners.
	 * <p>
	 * Subclasses that wish to add custom logic to this callback should make sure to
	 * call this super method <b>after</b> their own handler code.
	 *
	 * @param record
	 * @param discard
	 */
	protected void afterEndEdit(MetadataRecord record, boolean discard) {
		if(!discard) {
			fireMetadataRecordChanged(record);
		}
	}
}

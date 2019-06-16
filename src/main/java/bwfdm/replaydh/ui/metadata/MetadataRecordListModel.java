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
package bwfdm.replaydh.ui.metadata;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.metadata.MetadataListener;
import bwfdm.replaydh.metadata.MetadataRecord;
import bwfdm.replaydh.metadata.MetadataRecord.Target;
import bwfdm.replaydh.metadata.MetadataRepository;
import bwfdm.replaydh.metadata.MetadataRepository.RecordIterator;
import bwfdm.replaydh.ui.GuiUtils;

/**
 * @author Markus Gärtner
 *
 */
public class MetadataRecordListModel extends AbstractListModel<Target> implements MetadataListener {

	private static final long serialVersionUID = -6450371057096506540L;

	private static final Logger log = LoggerFactory.getLogger(MetadataRecordListModel.class);

	private final List<Target> elements = new ArrayList<>();

	private transient MetadataRepository repository;

	/**
	 * @see javax.swing.ListModel#getSize()
	 */
	@Override
	public int getSize() {
		return elements.size();
	}

	/**
	 * @see javax.swing.ListModel#getElementAt(int)
	 */
	@Override
	public Target getElementAt(int index) {
		return elements.get(index);
	}

	/**
	 * Does a complete update, removing all entries first and then
	 * reloading the internal list from scratch based on the current
	 * content of the repository.
	 */
	public void update() {
		GuiUtils.checkEDT();

		int size = elements.size();
		elements.clear();

		if(size>0) {
			fireIntervalRemoved(this, 0, size-1);
		}

		if(repository!=null) {
			try(RecordIterator iterator = repository.getAvailableRecords()) {
				while(iterator.hasNext()) {
					elements.add(iterator.next());
				}
			} catch (IOException e) {
				log.error("Failed to close record iterator", e);
			}

			if(!elements.isEmpty()) {
				fireIntervalAdded(this, 0, elements.size()-1);
			}
		}
	}

	public MetadataRepository getRepository() {
		return repository;
	}

	public void setRepository(MetadataRepository repository) {
		if(this.repository==repository) {
			return;
		}

		if(this.repository!=null) {
			this.repository.removeMetadataListener(this);
		}

		this.repository = repository;

		if(this.repository!=null) {
			this.repository.addMetadataListener(this);
		}

		GuiUtils.invokeEDT(this::update);
	}

	private void checkRepository(MetadataRepository repository) {
		checkArgument("Foreign repository", this.repository==repository);
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataListener#metadataRecordAdded(bwfdm.replaydh.metadata.MetadataRepository, bwfdm.replaydh.metadata.MetadataRecord)
	 */
	@Override
	public void metadataRecordAdded(MetadataRepository repository, MetadataRecord record) {
		checkRepository(repository);

		Target target = record.getTarget();
		int index = elements.size();

		elements.add(target);

		GuiUtils.invokeEDT(() -> fireIntervalAdded(MetadataRecordListModel.this, index, index));
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataListener#metadataRecordRemoved(bwfdm.replaydh.metadata.MetadataRepository, bwfdm.replaydh.metadata.MetadataRecord)
	 */
	@Override
	public void metadataRecordRemoved(MetadataRepository repository, MetadataRecord record) {
		checkRepository(repository);

		Target target = record.getTarget();
		int index = elements.indexOf(target);

		// Assuming we want to at some point add filters for the visualization, this prevents nasty errors
		if(index==-1) {
			return;
		}

		elements.remove(index);

		GuiUtils.invokeEDT(() -> fireIntervalRemoved(MetadataRecordListModel.this, index, index));
	}

	/**
	 * @see bwfdm.replaydh.metadata.MetadataListener#metadataRecordChanged(bwfdm.replaydh.metadata.MetadataRepository, bwfdm.replaydh.metadata.MetadataRecord)
	 */
	@Override
	public void metadataRecordChanged(MetadataRepository repository, MetadataRecord record) {
		checkRepository(repository);

		Target target = record.getTarget();
		int index = elements.indexOf(target);

		// Assuming we want to at some point add filters for the visualization, this prevents nasts errors
		if(index==-1) {
			return;
		}

		GuiUtils.invokeEDT(() -> fireContentsChanged(MetadataRecordListModel.this, index, index));

	}
}

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
package bwfdm.replaydh.ui.helper;

import static java.util.Objects.requireNonNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.helper.Wizard.WizardControl;

/**
 * @author Markus Gärtner
 *
 */
public abstract class AbstractWizardStep<E extends Object> implements Wizard.Page<E> {


	protected final JPanel panel;
	protected final String id;
	protected final String title;
	protected final String description;

	protected WizardControl<E> control;

	public AbstractWizardStep(String id, String titleKey, String descriptionKey) {
		this.id = requireNonNull(id);
		title = ResourceManager.getInstance().get(titleKey);
		description = descriptionKey==null ? null : ResourceManager.getInstance().get(descriptionKey);
		panel = createPanel();
	}

	protected abstract JPanel createPanel();

	/**
	 * @see bwfdm.replaydh.ui.helper.Wizard.Page#getId()
	 */
	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public String getDescription() {
		return (description==null || description.isEmpty()) ? null : description;
	}

	@Override
	public JComponent getPageComponent() {
		return panel;
	}

	@Override
	public void refresh(RDHEnvironment environment, E context) {
		// no-op
	}

	@Override
	public void persist(RDHEnvironment environment, E context) {
		// no-op
	}

	@Override
	public void cancel(RDHEnvironment environment, E context) {
		// no-op
	}

	@Override
	public void setControl(WizardControl<E> control) {
		this.control = control;
	}

	protected void setNextEnabled(boolean enabled) {
		if(control!=null) {
			control.setNextEnabled(enabled);
		}
	}
}

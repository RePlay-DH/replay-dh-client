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
package bwfdm.replaydh.ui.config;

import static java.util.Objects.requireNonNull;

import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.ui.config.PreferencesTab.TabResult;
import bwfdm.replaydh.ui.helper.DocumentAdapter;
import bwfdm.replaydh.utils.StringConverter;

/**
 * @author Markus Gärtner
 *
 * @param <O> the type of the underlying property
 * @param <C> the type of the associated component
 */
public abstract class PreferencesDelegate<O extends Object, C extends JComponent> {

	protected final RDHEnvironment environment;
	protected final RDHProperty property;
	protected final C component;
	protected final O defaultValue;

	private Consumer<PreferencesDelegate<?, ?>> changeBouncer;
	private Policy policy;

	protected PreferencesDelegate(RDHEnvironment environment, RDHProperty property, C component, O defaultValue) {
		this.environment = requireNonNull(environment);
		this.property = requireNonNull(property);
		this.component = requireNonNull(component);
		if(defaultValue==null) {
			defaultValue = property.getDefaultValue();
		}
		this.defaultValue = requireNonNull(defaultValue);
	}

	protected abstract void installListeners();
	protected abstract void uninstallListeners();

	public PreferencesDelegate<O, C> setPolicy(Policy policy) {
		this.policy = requireNonNull(policy);
		return this;
	}

	/**
	 * Sets the external consumer that acts as bouncer for change notifications.
	 * Internally this method also ensures that old listeners to underlying
	 * components get un-installed or (re-)installed depending on whether or
	 * not a bouncer previously existed or whether or not the new one is {@code null}.
	 *
	 * @param changeBouncer the changeBouncer to set
	 */
	public void setChangeBouncer(Consumer<PreferencesDelegate<?, ?>> changeBouncer) {

		if(this.changeBouncer!=null) {
			uninstallListeners();
		}

		this.changeBouncer = changeBouncer;

		if(this.changeBouncer!=null) {
			installListeners();
		}
	}

	protected void fireChange() {
		if(changeBouncer!=null) {
			changeBouncer.accept(this);
		}
	}

	public abstract O getPropertyValue();

	public abstract O getComponentValue();

	public void setPropertyValue(O value) {
		environment.setProperty(property, String.valueOf(value));
	}

	public abstract void setComponentValue(O value);

	public O getDefaultValue() {
		return defaultValue;
	}

	public boolean hasChanged() {
		return !Objects.equals(getPropertyValue(), getComponentValue());
	}

	public void update() {
		setComponentValue(getPropertyValue());
	}

	public TabResult apply() {
		TabResult result = TabResult.DONE;

		if(policy==Policy.REQUIRES_RESTART && hasChanged()) {
			result = TabResult.REQUIRES_RESTART;
		}

		setPropertyValue(getComponentValue());

		return result;
	}

	public void reset() {
		setPropertyValue(getDefaultValue());
		update();
	}

	public enum Policy {
		NONE,

		REQUIRES_RESTART,
		;
	}

	public static class TextComponentDelegate extends PreferencesDelegate<String, JTextComponent> {

		private final DocumentListener documentListener = new DocumentAdapter() {
			@Override
			public void anyUpdate(DocumentEvent e) {
				fireChange();
			}
		};

		public TextComponentDelegate(RDHEnvironment environment, RDHProperty property, JTextComponent component, String defaultValue) {
			super(environment, property, component, defaultValue);
		}

		/**
		 * @see bwfdm.replaydh.ui.config.PreferencesDelegate#getPropertyValue()
		 */
		@Override
		public String getPropertyValue() {
			return environment.getProperty(property);
		}

		/**
		 * @see bwfdm.replaydh.ui.config.PreferencesDelegate#getComponentValue()
		 */
		@Override
		public String getComponentValue() {
			return component.getText();
		}

		/**
		 * @see bwfdm.replaydh.ui.config.PreferencesDelegate#setComponentValue(java.lang.Object)
		 */
		@Override
		public void setComponentValue(String value) {
			component.setText(value);
		}

		/**
		 * @see bwfdm.replaydh.ui.config.PreferencesDelegate#installListeners()
		 */
		@Override
		protected void installListeners() {
			component.getDocument().addDocumentListener(documentListener);
		}

		/**
		 * @see bwfdm.replaydh.ui.config.PreferencesDelegate#uninstallListeners()
		 */
		@Override
		protected void uninstallListeners() {
			component.getDocument().removeDocumentListener(documentListener);
		}
	}

	public static class CheckboxDelegate extends PreferencesDelegate<Boolean, AbstractButton> {

		private final ItemListener itemListener = e -> fireChange();

		public CheckboxDelegate(RDHEnvironment environment, RDHProperty property, AbstractButton component, Boolean defaultValue) {
			super(environment, property, component, defaultValue);
		}

		/**
		 * @see bwfdm.replaydh.ui.config.PreferencesDelegate#getPropertyValue()
		 */
		@Override
		public Boolean getPropertyValue() {
			return environment.getBoolean(property, defaultValue);
		}

		/**
		 * @see bwfdm.replaydh.ui.config.PreferencesDelegate#getComponentValue()
		 */
		@Override
		public Boolean getComponentValue() {
			return component.isSelected();
		}

		/**
		 * @see bwfdm.replaydh.ui.config.PreferencesDelegate#setComponentValue(java.lang.Object)
		 */
		@Override
		public void setComponentValue(Boolean value) {
			component.setSelected(value.booleanValue());
		}

		/**
		 * @see bwfdm.replaydh.ui.config.PreferencesDelegate#installListeners()
		 */
		@Override
		protected void installListeners() {
			component.addItemListener(itemListener);
		}

		/**
		 * @see bwfdm.replaydh.ui.config.PreferencesDelegate#uninstallListeners()
		 */
		@Override
		protected void uninstallListeners() {
			component.removeItemListener(itemListener);
		}

	}

	public static class ComboBoxDelegate<E extends Object> extends PreferencesDelegate<E, JComboBox<E>> {

		private final StringConverter<E> converter;

		private final ActionListener actionListener = e -> fireChange();

		public ComboBoxDelegate(RDHEnvironment environment, RDHProperty property, JComboBox<E> component, E defaultValue,
				StringConverter<E> converter) {
			super(environment, property, component, defaultValue);

			this.converter = requireNonNull(converter);
		}

		/**
		 * @see bwfdm.replaydh.ui.config.PreferencesDelegate#getPropertyValue()
		 */
		@Override
		public E getPropertyValue() {
			return converter.deserialize(environment.getProperty(property));
		}

		/**
		 * @see bwfdm.replaydh.ui.config.PreferencesDelegate#getComponentValue()
		 */
		@SuppressWarnings("unchecked")
		@Override
		public E getComponentValue() {
			return (E) component.getSelectedItem();
		}

		/**
		 * @see bwfdm.replaydh.ui.config.PreferencesDelegate#setComponentValue(java.lang.Object)
		 */
		@Override
		public void setComponentValue(E value) {
			component.setSelectedItem(value);
		}

		/**
		 * @see bwfdm.replaydh.ui.config.PreferencesDelegate#installListeners()
		 */
		@Override
		protected void installListeners() {
			component.addActionListener(actionListener);
		}

		/**
		 * @see bwfdm.replaydh.ui.config.PreferencesDelegate#uninstallListeners()
		 */
		@Override
		protected void uninstallListeners() {
			component.addActionListener(actionListener);
		}

	}
}

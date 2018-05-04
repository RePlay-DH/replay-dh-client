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

import java.awt.Component;
import java.awt.ScrollPane;

import javax.swing.Scrollable;

/**
 * Models a helper object that can allow the user to configure it
 * through an interactive interface. Note that except for the basic
 * {@link #isConfigurable()} check all methods in this interface
 * are guaranteed to be called on the event dispatch thread.
 *
 * @author Markus Gärtner
 *
 */
public interface ConfigurableHelper {

	/**
	 * Returns the component that hosts the UI elements this
	 * {@link ConfigurableHelper} uses for configuration by the user.
	 * <p>
	 * This component should abide by a few general rules:
	 * <ul>
	 * <li>The component should not host its own {@link ScrollPane},
	 * since it cannot know beforehand whether or not it will already
	 * be placed into a surrounding ScrollPane.</li>
	 * <li>If scrolling is required, the component itself should implement
	 * the {@link Scrollable} interface (e.g. {@link ScrollablePanel}.</li>
	 * <li>Take into account that there is only one configuration step
	 * during the lifecycle of a {@link ConfigurableHelper} instance.</li>
	 * <li>The decision whether configuration options selected in one
	 * helper's lifecycle should be stored as "defaults" for the next pass
	 * of another instance, is implementation specific.</li>
	 * <li></li>
	 * </ul>
	 * <p>
	 * <b>Note:</b><br>
	 * This method must never return {@code null}!. If the {@link #isConfigurable()}
	 * method returns {@code true} then this method must be guaranteed to return
	 * a valid component. If this helper is <b>not</b> configurable (i.e. aforementioned
	 * method returns {@code false}) then this method should throw an exception
	 * of type {@link UnsupportedOperationException}.
	 * <p>
	 * This method is guaranteed to be called on the event dispatch thread!
	 *
	 * @return the component used for configuring this helper
	 *
	 * @throws UnsupportedOperationException if this helper is not configurable
	 * and the {@link #isConfigurable()} method returns {@code false}.
	 */
	default Component getDialogComponent() {
		throw new UnsupportedOperationException("Not configurable");
	}

	/**
	 * Reads the content of the {@link #getDialogComponent()} component
	 * used for configuration.
	 * <p>
	 * This method is guaranteed to be called on the event dispatch thread!
	 *
	 * @throws UnsupportedOperationException if this helper is not configurable
	 * and the {@link #isConfigurable()} method returns {@code false}.
	 */
	default void configure() {
		throw new UnsupportedOperationException("Not configurable");
	}

	/**
	 * Tells the client that this {@link ConfigurableHelper} is indeed
	 * configurable by the user. If this method returns {@code true} then
	 * the {@link #getDialogComponent()} method <b>must</b> return a valid
	 * component that hosts the actual UI elements used for configuration.
	 *
	 * @return
	 */
	default boolean isConfigurable() {
		return false;
	}
}

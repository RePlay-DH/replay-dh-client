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
package bwfdm.replaydh.ui.config.tabs;

import javax.swing.JCheckBox;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.config.DelegatingPreferencesTab;
import bwfdm.replaydh.ui.config.PreferencesDelegate;

/**
 * @author Markus Gärtner
 *
 */
public class TrackingPreferencesTab extends DelegatingPreferencesTab {

	public TrackingPreferencesTab(RDHEnvironment environment) {

		ResourceManager rm = ResourceManager.getInstance();

		JTextField tfMaxSize = new JTextField(20);
		tfMaxSize.setToolTipText(rm.get("replaydh.plugins.trackingPreferencesTab.ignoreLargerThan.description"));
		GuiUtils.addErrorFeedback(tfMaxSize, s -> {
			try {
				IOUtils.parseSize(s);
				return true;
			} catch(Exception e) {
				return false;
			}
		});

		JCheckBox cbIgnoreEmpty = new JCheckBox();
		JCheckBox cbIgnoreHidden = new JCheckBox();


		FormBuilder.create()
				.columns("left:pref, 15dlu, left:pref, fill:pref:grow")
				.rows("pref, $nlg, pref, $lg, pref, 10dlu, pref, $nlg, pref, $lg, pref, $lg, pref, $lg, pref, $lg, pref")
				.panel(getPanel())

				.addSeparator(rm.get("replaydh.plugins.trackingPreferencesTab.ignoreRules")).xyw(1, 1, 4)
				.addLabel(rm.get("replaydh.plugins.trackingPreferencesTab.ignoreEmpty")).xy(1, 3).add(cbIgnoreEmpty).xyw(3, 3, 2)
				.addLabel(rm.get("replaydh.plugins.trackingPreferencesTab.ignoreHidden")).xy(1, 5).add(cbIgnoreHidden).xyw(3, 5, 2)
				.addLabel(rm.get("replaydh.plugins.trackingPreferencesTab.ignoreLargerThan")).xy(1, 7).add(tfMaxSize).xyw(3, 7, 2)

				.build();

		addDelegate(new PreferencesDelegate.CheckboxDelegate(environment, RDHProperty.GIT_IGNORE_EMPTY, cbIgnoreEmpty, null));
		addDelegate(new PreferencesDelegate.CheckboxDelegate(environment, RDHProperty.GIT_IGNORE_HIDDEN, cbIgnoreHidden, null));
		addDelegate(new PreferencesDelegate.TextComponentDelegate(environment, RDHProperty.GIT_MAX_FILESIZE, tfMaxSize, null));
	}
}

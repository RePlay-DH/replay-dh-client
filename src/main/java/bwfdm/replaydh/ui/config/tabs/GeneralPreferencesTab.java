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

import java.util.Locale;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.config.DelegatingPreferencesTab;
import bwfdm.replaydh.ui.config.PreferencesDelegate;
import bwfdm.replaydh.ui.config.PreferencesDelegate.Policy;
import bwfdm.replaydh.utils.StringConverter;

/**
 * @author Markus Gärtner
 *
 */
public class GeneralPreferencesTab extends DelegatingPreferencesTab {

	public GeneralPreferencesTab(RDHEnvironment environment) {

		ResourceManager rm = ResourceManager.getInstance();

		JTextField tfUsername = new JTextField(20);
		GuiUtils.addErrorFeedback(tfUsername, (String)null);

		JTextField tfOrganization = new JTextField(20);
		GuiUtils.addErrorFeedback(tfOrganization, (String)null);

		JCheckBox cbExpertMode = new JCheckBox();
		JCheckBox cbDisableTray = new JCheckBox();
		JCheckBox cbAlwaysOnTop = new JCheckBox();
		JCheckBox cbCollectStats = new JCheckBox();

		JComboBox<Locale> cbLocale = new JComboBox<>(new Locale[] {Locale.GERMAN, Locale.ENGLISH});
		cbLocale.setEditable(false);


		FormBuilder.create()
				.columns("left:pref, 15dlu, left:pref, fill:pref:grow")
				.rows("pref, $nlg, pref, $lg, pref, 10dlu, pref, $nlg, pref, $lg, pref, $lg, pref, $lg, pref, $lg, pref")
				.panel(getPanel())

				.addSeparator(rm.get("replaydh.plugins.generalPreferencesTab.user")).xyw(1, 1, 4)
				.addLabel(rm.get("replaydh.plugins.generalPreferencesTab.username")).xy(1, 3).add(tfUsername).xyw(3, 3, 2)
				.addLabel(rm.get("replaydh.plugins.generalPreferencesTab.organization")).xy(1, 5).add(tfOrganization).xyw(3, 5, 2)

				.addSeparator(rm.get("replaydh.plugins.generalPreferencesTab.client")).xyw(1, 7, 4)
				.addLabel(rm.get("replaydh.plugins.generalPreferencesTab.locale")).xy(1, 9).add(cbLocale).xy(3, 9)
				.addLabel(rm.get("replaydh.plugins.generalPreferencesTab.collectStats")).xy(1, 11).add(cbCollectStats).xy(3, 11)
				.addLabel(rm.get("replaydh.plugins.generalPreferencesTab.trayDisabled")).xy(1, 13).add(cbDisableTray).xy(3, 13)
				.addLabel(rm.get("replaydh.plugins.generalPreferencesTab.alwaysOnTop")).xy(1, 15).add(cbAlwaysOnTop).xy(3, 15)
				.addLabel(rm.get("replaydh.plugins.generalPreferencesTab.expertMode")).xy(1, 17).add(cbExpertMode).xy(3, 17)

				.build();

		addDelegate(new PreferencesDelegate.TextComponentDelegate(environment, RDHProperty.CLIENT_USERNAME, tfUsername, ""));
		addDelegate(new PreferencesDelegate.TextComponentDelegate(environment, RDHProperty.CLIENT_ORGANIZATION, tfOrganization, ""));
		addDelegate(new PreferencesDelegate.CheckboxDelegate(environment, RDHProperty.CLIENT_EXPERT_MODE, cbExpertMode, false));
		addDelegate(new PreferencesDelegate.CheckboxDelegate(environment, RDHProperty.CLIENT_COLLECT_STATS, cbCollectStats, false));
		addDelegate(new PreferencesDelegate.CheckboxDelegate(environment, RDHProperty.CLIENT_UI_TRAY_DISABLED, cbDisableTray, false));
		addDelegate(new PreferencesDelegate.CheckboxDelegate(environment, RDHProperty.CLIENT_UI_ALWAYS_ON_TOP, cbAlwaysOnTop, false));

		StringConverter<Locale> localeConverter = StringConverter.fromFunctions(
				l -> l.toString(), s -> Locale.forLanguageTag(s));

		addDelegate(new PreferencesDelegate.ComboBoxDelegate<Locale>(
				environment, RDHProperty.CLIENT_LOCALE, cbLocale, Locale.ENGLISH, localeConverter).setPolicy(Policy.REQUIRES_RESTART));
	}
}

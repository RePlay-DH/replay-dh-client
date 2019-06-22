/**
 *
 */
package bwfdm.replaydh.ui.config.tabs;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JCheckBox;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.metadata.basic.DublinCoreField;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.ui.config.DelegatingPreferencesTab;
import bwfdm.replaydh.ui.config.PreferencesDelegate;
import bwfdm.replaydh.ui.helper.PassiveTextArea;

/**
 * @author Florian Fritze
 *
 */
public class DublinCorePreferencesTab extends DelegatingPreferencesTab {

	public DublinCorePreferencesTab(RDHEnvironment environment) {
		ResourceManager rm = ResourceManager.getInstance();

		final DublinCoreField[] fields = DublinCoreField.values();

		Matcher matcher = Pattern.compile("[a-zA-Z0-9.-:]*").matcher("");
		Predicate<String> verifier = s -> matcher.reset(s).matches();

		JTextArea taInfo = new PassiveTextArea(rm.get("replaydh.plugins.dublinCorePreferencesTab.info"));
		JCheckBox cbSpecialTreatment = new JCheckBox();
		JTextArea taSpecialTreatment = new PassiveTextArea(rm.get(
				"replaydh.plugins.dublinCorePreferencesTab.specialTreatment"));

		FormBuilder builder = FormBuilder.create()
				.columns("left:pref, 15dlu, fill:pref:grow")
				.rows("pref, 4dlu, pref")
				.panel(getPanel())

				.add(taInfo).xyw(1, 1, 3)
				.add(cbSpecialTreatment).xy(1, 3, "right, top")
				.add(taSpecialTreatment).xy(3, 3);

		int startRow = 5;
		for (int i = 0; i < fields.length; i++) {
			DublinCoreField field = fields[i];

			JTextField tf = new JTextField(18);
			GuiUtils.addErrorFeedback(tf, verifier);

			int row = startRow+(i*2);
			builder.appendRows("$nlg, pref");
			builder.addLabel(field.getName()).xy(1, row);
			builder.add(tf).xy(3, row);

			addDelegate(new PreferencesDelegate.TextComponentDelegate(
					environment, field, tf, field.getDefaultValue()));
		}

		builder.build();

		addDelegate(new PreferencesDelegate.CheckboxDelegate(environment,
				RDHProperty.METADATA_AUTOFILL_DC_SPECIAL_TREATMENT, cbSpecialTreatment,
				RDHProperty.METADATA_AUTOFILL_DC_SPECIAL_TREATMENT.getDefaultValue()));
	}
}

/**
 *
 */
package bwfdm.replaydh.ui.config.tabs;

import javax.swing.JCheckBox;

import com.jgoodies.forms.builder.FormBuilder;

import bwfdm.replaydh.core.RDHEnvironment;
import bwfdm.replaydh.core.RDHProperty;
import bwfdm.replaydh.resources.ResourceManager;
import bwfdm.replaydh.ui.config.DelegatingPreferencesTab;
import bwfdm.replaydh.ui.config.PreferencesDelegate;

/**
 * @author Florian Fritze
 *
 */
public class MetadataPreferencesTab extends DelegatingPreferencesTab {

	public MetadataPreferencesTab(RDHEnvironment environment) {
		ResourceManager rm = ResourceManager.getInstance();

		JCheckBox cbMetadataScope = new JCheckBox();
		JCheckBox cbEnforceDc = new JCheckBox();

		FormBuilder.create()
				.columns("left:pref, 15dlu, left:pref, fill:pref:grow")
				.rows("pref, $nlg, pref, 10dlu, pref, $nlg, pref")
				.panel(getPanel())

				// Object metadata section
				.addSeparator(rm.get("replaydh.plugins.metadataPreferencesTab.objectMetadata")).xyw(1, 1, 4)
				.addLabel(rm.get("replaydh.plugins.metadataPreferencesTab.enforceDc")).xy(1, 3).add(cbEnforceDc).xy(3, 3)

				// Process metadata section
				.addSeparator(rm.get("replaydh.plugins.metadataPreferencesTab.processMetadataExport")).xyw(1, 5, 4)
				.addLabel(rm.get("replaydh.plugins.metadataPreferencesTab.scope")).xy(1, 7).add(cbMetadataScope).xy(3, 7)

		.build();

		addDelegate(new PreferencesDelegate.CheckboxDelegate(environment,
				RDHProperty.OWL_METADATA_EXPORT_FULL_ONTOLOGY, cbMetadataScope, true));
		addDelegate(new PreferencesDelegate.CheckboxDelegate(environment,
				RDHProperty.METADATA_ENFORCE_DC, cbEnforceDc, RDHProperty.METADATA_ENFORCE_DC.getDefaultValue()));

	}
}

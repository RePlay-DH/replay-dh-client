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
		JCheckBox cbFillRecords = new JCheckBox();
		JCheckBox cbFillResources = new JCheckBox();
		JCheckBox cbEmptySchemaAsFallback = new JCheckBox();

		FormBuilder.create()
				.columns("left:pref, 15dlu, left:pref, fill:pref:grow")
				.rows("pref, $nlg, pref, $nlg, pref, $nlg, pref, $nlg, pref, 10dlu, pref, $nlg, pref")
				.panel(getPanel())

				// Object metadata section
				.addSeparator(rm.get("replaydh.plugins.metadataPreferencesTab.objectMetadata")).xyw(1, 1, 4)
				.addLabel(rm.get("replaydh.plugins.metadataPreferencesTab.enforceDc")).xy(1, 3).add(cbEnforceDc).xy(3, 3)
				.addLabel(rm.get("replaydh.plugins.metadataPreferencesTab.emptySchemaAsFallback")).xy(1, 5).add(cbEmptySchemaAsFallback).xy(3, 5)
				.addLabel(rm.get("replaydh.plugins.metadataPreferencesTab.autofillRecords")).xy(1, 7).add(cbFillRecords).xy(3, 7)
				.addLabel(rm.get("replaydh.plugins.metadataPreferencesTab.autofillResources")).xy(1, 9).add(cbFillResources).xy(3, 9)

				// Process metadata section
				.addSeparator(rm.get("replaydh.plugins.metadataPreferencesTab.processMetadataExport")).xyw(1, 11, 4)
				.addLabel(rm.get("replaydh.plugins.metadataPreferencesTab.scope")).xy(1, 13).add(cbMetadataScope).xy(3, 13)

		.build();

		addDelegate(new PreferencesDelegate.CheckboxDelegate(environment,
				RDHProperty.OWL_METADATA_EXPORT_FULL_ONTOLOGY, cbMetadataScope, true));
		addDelegate(new PreferencesDelegate.CheckboxDelegate(environment,
				RDHProperty.METADATA_ENFORCE_DC, cbEnforceDc,
				RDHProperty.METADATA_ENFORCE_DC.getDefaultValue()));
		addDelegate(new PreferencesDelegate.CheckboxDelegate(environment,
				RDHProperty.METADATA_AUTOFILL_RECORDS, cbFillRecords,
				RDHProperty.METADATA_AUTOFILL_RECORDS.getDefaultValue()));
		addDelegate(new PreferencesDelegate.CheckboxDelegate(environment,
				RDHProperty.METADATA_AUTOFILL_RESOURCES, cbFillResources,
				RDHProperty.METADATA_AUTOFILL_RESOURCES.getDefaultValue()));
		addDelegate(new PreferencesDelegate.CheckboxDelegate(environment,
				RDHProperty.METADATA_EMPTY_SCHEMA_AS_FALLBACK, cbEmptySchemaAsFallback,
				RDHProperty.METADATA_EMPTY_SCHEMA_AS_FALLBACK.getDefaultValue()));

	}
}

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
		
		FormBuilder.create()
		.columns("left:pref, 15dlu, left:pref, fill:pref:grow")
		.rows("pref, $nlg, pref, $lg, pref, 10dlu, pref, $nlg, pref, $lg, pref, $lg, pref")
		.panel(getPanel())
		.addLabel(rm.get("replaydh.plugins.metadataPreferencesTab.scope")).xy(1, 9).add(cbMetadataScope).xy(3, 9)
		
		.build();

		addDelegate(new PreferencesDelegate.CheckboxDelegate(environment, RDHProperty.METADATA_EXPORT_ONTOLOGY,cbMetadataScope,true));
		
	}
}

package bwfdm.replaydh.workflow.export.dataverse;

import java.util.Set;
import java.util.TreeSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Representation of the collection of <String, String> in the ComboBox
 * @author Florian Fritze
 */
public class CollectionEntry {
	
	private Set<Entry<String, String>> entries;
	private Set<String> valuesForDatasets = new TreeSet<>();
	private Map<String,String> keysForDatasets = new HashMap<>();
	
	CollectionEntry(Set<Entry<String, String>> entries) {
		this.entries = entries;
	}

	public Set<String> getKeys() {
		Set<String> keys = new TreeSet<>();
		for(Entry<String, String> entry : entries) {
			keys.add(entry.getKey());
		}
		return keys;
	}
		
	public Set<String> getValues() {
		Set<String> values = new TreeSet<>();
		for(Entry<String, String> entry : entries) {
			values.add(entry.getValue());
		}
		return values;
	}
	
	public Set<String> getValuesForDatasets() {
		String doiEnding;
		valuesForDatasets = new TreeSet<>();
		for(Entry<String, String> entry : entries) {
			doiEnding=entry.getKey().substring(entry.getKey().indexOf("doi:"), entry.getKey().length()-1);
			valuesForDatasets.add(entry.getValue()+" - "+doiEnding);
			keysForDatasets.put(entry.getValue()+" - "+doiEnding, entry.getKey());
		}
		return valuesForDatasets;
	}
	
	public String getKey(String value) {
		String key = null;
		for(Entry<String, String> entry : entries) {
			if(entry.getValue().equals(value)) {
				key=entry.getKey();
				break;
			}
		}
		return key;
	}
	
	public String getKeyForDatasets(String value) {
		String key = null;
		for(String valueForDataset : valuesForDatasets) {
			if (valueForDataset.equals(value)) {
				key=keysForDatasets.get(value);
				break;
			} 
		}
		return key;
	}
	
	public String getValue(String key) {
		String value = null;
		for(Entry<String, String> entry : entries) {
			if(entry.getKey().equals(key)) {
				value=entry.getValue();
				break;
			}
		}
		return value;
	}
}

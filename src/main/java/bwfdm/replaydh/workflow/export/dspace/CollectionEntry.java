package bwfdm.replaydh.workflow.export.dspace;

import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

/**
 * Useful to fill a ComboBox
 * @author Florian Fritze
 */
public class CollectionEntry {
	
	private Set<Entry<String, String>> entries;
	
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
			if(entry.getValue() != null) {
				values.add(entry.getValue());
			} 
		}
		return values;
	}
	
	public String getKey(String value) {
		String key = null;
		for(Entry<String, String> entry : entries) {
			if(entry.getValue() != null) {
				if(entry.getValue().equals(value)) {
					key=entry.getKey();
					break;
				}
			}
		}
		return key;
	}
	
	public String getKeyForDataset(String value) {
		String key = null;
		for(String valueForDataset : this.getValues()) {
			if (valueForDataset.equals(value)) {
				key=this.getKey(value);
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

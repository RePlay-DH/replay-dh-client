package bwfdm.replaydh.workflow.export.dataverse;

import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

/**
 * Representation of the collection of <String, String> in the ComboBox
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
			values.add(entry.getValue());
		}
		return values;
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
}

package bwfdm.replaydh.workflow.export.generic;

public enum SwordRequestType {
	
	DEPOSIT("DEPOSIT"), //"POST" request
	REPLACE("REPLACE"), //"PUT" request
	DELETE("DELETE")	//reserved for the future
	;
	
	private final String label;
	
	private SwordRequestType(String label) {
		this.label = label;
	}
	
	public String getLabel() {
		return label;
	}
	
	@Override
	public String toString() {
		return label;
	}
}

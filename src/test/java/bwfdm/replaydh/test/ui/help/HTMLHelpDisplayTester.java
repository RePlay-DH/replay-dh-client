package bwfdm.replaydh.test.ui.help;

import bwfdm.replaydh.ui.help.HTMLHelpDisplay;

public class HTMLHelpDisplayTester {

	public static void main(String[] args) {
		
		HTMLHelpDisplay display = new HTMLHelpDisplay();
		
		display.readHelpFile();
		display.showHelpSection("replaydh.ui.editor.workflowStep");
		
	}

}

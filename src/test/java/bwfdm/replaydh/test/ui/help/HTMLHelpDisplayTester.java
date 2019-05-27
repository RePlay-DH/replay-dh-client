package bwfdm.replaydh.test.ui.help;

import bwfdm.replaydh.ui.help.HTMLHelpDisplay;

public class HTMLHelpDisplayTester {

	public static void main(String[] args) {
		HTMLHelpDisplay display = new HTMLHelpDisplay("/Users/ffritzew/Documents/GitHub/replay-dh-client/src/main/resources/bwfdm/replaydh/help/client.html");
		
		display.readHelpFile();
		display.showHelpSection("the-workflow-step-editor",null);
		
	}

}

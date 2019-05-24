package bwfdm.replaydh.test.ui.help;

import bwfdm.replaydh.ui.help.HTMLHelpDisplay;

public class HTMLHelpDisplayTester {

	public static void main(String[] args) {
		HTMLHelpDisplay display = new HTMLHelpDisplay("/Users/ffritzew/Documents/GitLab/replay-dh/Dokumentation/Client-Doku/docu.html");
		
		display.readHelpFile();
		display.showHelpSection("the-workflow-step-editor",null);
		
	}

}

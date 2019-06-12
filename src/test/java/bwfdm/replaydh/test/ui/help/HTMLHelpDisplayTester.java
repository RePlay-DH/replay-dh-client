package bwfdm.replaydh.test.ui.help;

import java.util.Locale;

import bwfdm.replaydh.ui.help.HTMLHelpDisplay;

public class HTMLHelpDisplayTester {

	public static void main(String[] args) {

		HTMLHelpDisplay display = new HTMLHelpDisplay(Locale.GERMAN);

		display.showHelpSection("replaydh.ui.editor.workflowStep");

	}

}

package bwfdm.replaydh.test.ui.help;

import bwfdm.replaydh.ui.help.MarkdownDisplay;

public class MarkdownTester {

	public static void main(String[] args) {
		MarkdownDisplay display = new MarkdownDisplay();
		
		display.readMarkdownFile("/Users/ffritzew/Documents/GitLab/replay-dh/Dokumentation/Client-Doku/RePlay-DH-Client-Documentation.md");
	}

}

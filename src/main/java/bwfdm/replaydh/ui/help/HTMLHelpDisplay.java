package bwfdm.replaydh.ui.help;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.io.IOUtils;

public class HTMLHelpDisplay {
	
	private static final Logger log = LoggerFactory.getLogger(HTMLHelpDisplay.class);
	
	private String document = null;
	
	public void readHelpFile(String markdownFilePath) {
		File markdownFile = new File(markdownFilePath);
		InputStream input = null;
		try {
			input = new FileInputStream(markdownFile);
			document=IOUtils.readStreamUnchecked(input);
		} catch (FileNotFoundException e) {
			log.error("Error reading markdown file as stream",e);
		}
		System.out.println(document); 
	}
	
	public void showHelpSection(String anchor, JComponent comp) {
		
	}
}

package bwfdm.replaydh.ui.help;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JComponent;

import org.commonmark.Extension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension.Builder;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.io.IOUtils;

public class MarkdownDisplay {
	
	private static final Logger log = LoggerFactory.getLogger(MarkdownDisplay.class);
	
	private Node document = null;
	
	public void readMarkdownFile(String markdownFilePath) {
		File markdownFile = new File(markdownFilePath);
		InputStream input = null;
		try {
			input = new FileInputStream(markdownFile);
		} catch (FileNotFoundException e) {
			log.error("Error reading markdown file as stream",e);
		}
		Parser parser = Parser.builder().build();
		
		try {
			document = parser.parse(IOUtils.readStream(input));
		} catch (IOException e) {
			log.error("Error parsing markdown file",e);
		}
		HtmlRenderer renderer = HtmlRenderer.builder().build();
		Builder extension = HeadingAnchorExtension.builder();
		Extension ext = extension.build();
		System.out.println(renderer.render(document).toString()); 
	}
	
	public void showHelpSection(String anchor, JComponent comp) {
		
	}
}

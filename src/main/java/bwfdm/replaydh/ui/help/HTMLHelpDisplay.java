package bwfdm.replaydh.ui.help;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.swing.JComponent;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.io.IOUtils;

public class HTMLHelpDisplay {
	
	public HTMLHelpDisplay(String HTMLFilePath) {
		this.HTMLFilePath=HTMLFilePath;
	}
	
	private static final Logger log = LoggerFactory.getLogger(HTMLHelpDisplay.class);
	
	private String document = null;
	private Document doc;
	private String HTMLFilePath;
	
	public void readHelpFile() {
		File markdownFile = new File(HTMLFilePath);
		InputStream input = null;
		try {
			input = new FileInputStream(markdownFile);
			document=IOUtils.readStream(input);
		} catch (IOException e) {
			log.error("Error reading markdown file as stream",e);
		}
		doc = Jsoup.parse(document);
		System.out.println(document); 
	}
	
	public String findAndPrintPosition(String id) {
		Elements sections =	doc.select("h3[id$="+id+"]");
		String section="";
		for (Element element : sections) {
			List<Element> list = element.nextElementSiblings();
			for (Element next : list) {
				if((next.normalName()).equals(element.normalName())) {
					break;
				}
				section=section+next;
			}
		}
		return section;
	}
	
	public void showHelpSection(String anchor, JComponent comp) {
		String section=findAndPrintPosition(anchor);
		System.out.println(section);
	}
}

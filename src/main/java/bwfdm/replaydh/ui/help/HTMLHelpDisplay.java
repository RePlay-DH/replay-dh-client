package bwfdm.replaydh.ui.help;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.resources.ResourceManager;

public class HTMLHelpDisplay {
	
	public HTMLHelpDisplay() {
	}
	
	private static final Logger log = LoggerFactory.getLogger(HTMLHelpDisplay.class);
	
	private String document = null;
	private Document doc;
	private ResourceManager rm = ResourceManager.getInstance();
	
	/**
	 * Reads the HTML Help file
	 */
	public void readHelpFile() {
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		File markdownFile = new File(classloader.getResource("bwfdm/replaydh/help/client.html").getFile());
		InputStream input = null;
		try {
			input = new FileInputStream(markdownFile);
			document=IOUtils.readStream(input);
		} catch (IOException e) {
			log.error("Error reading markdown file as stream",e);
		}
		doc = Jsoup.parse(document);
	}
	
	/**
	 * finds the position of a h3 tag
	 * @param id of the h3 tag
	 * @return html section
	 */
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
	
	/**
	 * 
	 * @param anchor
	 * @param comp
	 */
	public void showHelpSection(String anchor, JComponent comp) {
		String section=findAndPrintPosition(anchor);
		JFrame frame = new JFrame();
		frame.setTitle(rm.get("replaydh.documentation.helpWindow.title"));
	    JEditorPane editorPane=new JEditorPane();
	    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
	    File htmlPart = new File(classloader.getResource("bwfdm/replaydh/help/htmlPart.html").getFile());
	    try {
			FileWriter writer = new FileWriter(htmlPart);
			writer.write(section);
			writer.close();
			JScrollPane scrollPane = null;
			scrollPane = new JScrollPane(editorPane);
			editorPane.setPage(htmlPart.toURI().toURL());

			frame.add(scrollPane);
			frame.setVisible(true);
			frame.setSize(800, 600);
			frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		} catch (IOException e) {
			log.error("Error during file operations",e);
		}
	}
}

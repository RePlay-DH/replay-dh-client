package bwfdm.replaydh.ui.help;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle.Control;

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
import bwfdm.replaydh.resources.UTF8Control;
import bwfdm.replaydh.ui.GuiUtils;

/**
 *
 * @author Florian Fritze
 *
 */
public class HTMLHelpDisplay {

	private static final Logger log = LoggerFactory.getLogger(HTMLHelpDisplay.class);

	private String document = null; //TODO do we even need the raw string content after initial parsing?
	private Document doc;

	private JFrame helpFrame;
	private JEditorPane editorPane;

	private static final String DOCU_BASENAME = "bwfdm.replaydh.help.client-docu";
	private static final String DOCU_SUFFIX = "html";

	public HTMLHelpDisplay(Locale targetLocale) {
		Control control = new UTF8Control();
		List<Locale> candidateLocales = control.getCandidateLocales(DOCU_BASENAME, targetLocale);

		URL docuUrl = null;

		ClassLoader classLoader = getClass().getClassLoader();
		for(Locale locale : candidateLocales) {
			String path = control.toBundleName(DOCU_BASENAME, locale);
			path = control.toResourceName(path, DOCU_SUFFIX);

			docuUrl = classLoader.getResource(path);
			if(docuUrl!=null) {
				break;
			}
		}

		if(docuUrl==null)
			throw new InternalError("Failed to locate docu file");

		try(InputStream input = docuUrl.openStream()) {
			document = IOUtils.readStream(input);
		} catch (IOException e) {
			log.error("Error reading html file as stream",e);
		}

		doc = Jsoup.parse(document);
	}

	/**
	 * finds the position of a h3 tag
	 * @param id of the h3 tag
	 * @return html section
	 */
	private String findAndPrintPosition(String id) {
		Elements sections =	doc.select("h3[id$="+id+"]");
		String section="";
		for (Element element : sections) {
			List<Element> list = element.nextElementSiblings();
			for (Element next : list) {
				if((next.normalName()).equals(element.normalName()) || (next.normalName()).equals("h2")) {
					break;
				}
				section=section+next;
			}
		}
		return section;
	}

	private String processHelpString(String anchor, String s) {
		if(s==null || s.isEmpty()) {
			return ResourceManager.getInstance().get("replaydh.documentation.helpWindow.missingContent", anchor);
		}

		URL jarLocation = HTMLHelpDisplay.class.getProtectionDomain().getCodeSource().getLocation();
		URL baseURL;
		try {
			baseURL = new URL(jarLocation, "bwfdm/replaydh/help/images/");
		} catch (MalformedURLException e) {
			log.error("Error creating base URL for help display", e);
			return ResourceManager.getInstance().get("replaydh.documentation.helpWindow.error");
		}
		String htmlHeader = "<head><base href=\"" + baseURL + "\"/></head>";
		String htmlPart = "<!DOCTYPE html><html>" + htmlHeader + "<body><div style='font-family:monospace'>" + s + "</div></body></html>";

		return htmlPart;
	}

	/**
	 *
	 * @param anchor
	 * @param comp
	 */
	public JFrame showHelpSection(String anchor) {
		String content = findAndPrintPosition(anchor);
		content = processHelpString(anchor, content);

		JFrame frame = helpFrame;
		if(frame==null) {
			synchronized (this) {
				if(frame==null) {
					editorPane = new JEditorPane("text/html", "");
					editorPane.setEditable(false);

					frame = new JFrame();
					frame.setTitle(ResourceManager.getInstance().get("replaydh.documentation.helpWindow.title"));
					frame.add(new JScrollPane(editorPane));
					frame.setSize(800, 600);
					frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
					frame.setLocationRelativeTo(null);
					GuiUtils.decorateFrame(frame);
					helpFrame = frame;
				}
			}
		}

		editorPane.setText(content);
		editorPane.setCaretPosition(0);
		frame.setVisible(true);

		return frame;
	}
}

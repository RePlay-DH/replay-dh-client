package bwfdm.replaydh.workflow.export.dataverse;

import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.Response;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.eclipse.jgit.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.client.AuthCredentials;
import org.swordapp.client.DepositReceipt;
import org.swordapp.client.ProtocolViolationException;
import org.swordapp.client.SWORDClientException;
import org.swordapp.client.SWORDError;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;

import bwfdm.replaydh.ui.GuiUtils;
import bwfdm.replaydh.workflow.export.generic.SwordExporter;

/**
 * 
 * @author Florian Fritze
 *
 */
public class DataverseRepository_v4 extends SwordExporter {
	
	public DataverseRepository_v4(AuthCredentials authCredentials, String serviceDocumentURL) {
		super(authCredentials);
		this.serviceDocumentURL=serviceDocumentURL;
	}
	
	private static final Logger log = LoggerFactory.getLogger(DataverseRepository_v4.class);
	
	// For SWORD
	private String serviceDocumentURL;
			
	private Abdera abdera = new Abdera();
	
	/**
	 * Parts copied from: public Content getContent(String contentURL, String mimeType, String packaging, AuthCredentials auth)
	 * class SWORDClient
	 */
	public Feed getAtomFeed(String dataverseURL)
			throws SWORDClientException, MalformedURLException {
		// do some error checking and validations
		if (dataverseURL == null) {
			log.error("Null string passed in to getAtomFeed; returning null");
			return null;
		}
		if (log.isDebugEnabled()) {
			log.debug("getting Atom Feed from " + dataverseURL);
		}

		AbderaClient client = new AbderaClient(this.abdera);
		
		AuthCredentials auth = super.getAuthCredentials();
		
		if (auth != null) {
			if (auth.getUsername() != null) {
				if (log.isDebugEnabled()) {
					log.debug("Setting username/password: " + auth.getUsername() + "/****password omitted *****");
				}
				UsernamePasswordCredentials unpw = new UsernamePasswordCredentials(auth.getUsername(),
						auth.getPassword());

				// create the credentials - target and realm can be null (and are so by default)
				try {
					client.addCredentials(auth.getTarget(), auth.getRealm(), "basic", unpw);
				} catch (URISyntaxException e) {
					log.error("Unable to parse authentication target in AuthCredential", e);
					throw new SWORDClientException("Unable to parse authentication target in AuthCredentials", e);
				}
			}
		}
		// ensure that the URL is valid
		URL url = new URL(dataverseURL);
		if (log.isDebugEnabled()) {
			log.debug("Formalised Atom Document URL to " + url.toString());
		}

		// make the request for atom feed
		if (log.isDebugEnabled()) {
			log.debug("Connecting to Server to Atom Feed Document from " + url.toString() + " ...");
		}
		ClientResponse resp = client.get(url.toString());
		if (log.isDebugEnabled()) {
			log.debug("Successfully retrieved Atom Feed from " + url.toString());
		}

		// if the response is successful, get the Atom Feed out of the response
		if (resp.getType() == Response.ResponseType.SUCCESS) {
			log.info("Retrieved Atom Feed from " + url.toString() + " with HTTP success code");
			Document<Feed> doc = resp.getDocument();
			Feed sd = doc.getRoot();
			return sd;
		}

		// if we don't get anything respond with null
		log.warn("Unable to retrieve Atom Feed from " + url.toString() + "; responded with " + resp.getStatus()
				+ ". Possible problem with SWORD server, or URL");
		return null;
	}
	

	public Map<String, String> getUserAvailableCollectionsWithTitle(String repoUrl) {
		Map<String, String> collections = new HashMap<>();
		collections=super.getCollections(this.getServiceDocument(serviceDocumentURL));
		for(String key : collections.keySet()) {
			int index=key.indexOf("/dataverse/");
			String urlEnding=key.substring(index, key.length());
			urlEnding=urlEnding.replace("dataverse", "dataverses");
			String fullAPIUrl=repoUrl+urlEnding;
			try {
				String jsonMetadata=getJSONMetadata(fullAPIUrl);
				Configuration conf = Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS);
				String alias = JsonPath.using(conf).parse(jsonMetadata).read("$.data.alias");
				String newValue=collections.get(key).replace(collections.get(key), collections.get(key)+" -- "+alias);
				collections.put(key, newValue);
			} catch (SWORDClientException | IOException e) {
				log.error("Failed to retrieve dataverses available by the user: {}",repoUrl, e);
				GuiUtils.showErrorDialog(null, e);
			}
		}
		return collections;
	}



	public Entry getUserAvailableMetadataset(Feed feed, String doiId) {
		if(feed != null) {
			List<Entry> entries = feed.getEntries();
			Entry chosenEntry = null;
			for (Entry entry : entries) {
				int beginDOI=entry.getEditMediaLinkResolvedHref().toString().indexOf("doi:");
				int end=entry.getEditMediaLinkResolvedHref().toString().length();
				if (doiId.equals(entry.getEditMediaLinkResolvedHref().toString().substring(beginDOI, end))) {
					chosenEntry=entry;
				}
			}
			return chosenEntry;
		} else {
			log.error("Entry ID "+doiId+" was not found!");
			return null;
		}
	}
	
	public Map<String, String> getMetadataSetsWithId(Feed feed) {
		Map<String, String> entries = new HashMap<>();
		if(feed != null) {
			List<Entry> entriesInFeed = feed.getEntries();
			for (Entry entry : entriesInFeed) {
				entries.put(entry.getId().toString(), entry.getTitle());
			}
		}
		return entries;
	}

	@Override
	public Map<String, String> getCollectionEntries(String collectionUrl) {
		Map<String, String> entries = new HashMap<>();
		try {
			entries=getMetadataSetsWithId(getAtomFeed(collectionUrl));
		} catch (MalformedURLException| SWORDClientException e) {
			log.error("Failed to retrieve entries from a dataverse {}", collectionUrl, e);
			GuiUtils.showErrorDialog(null, e);
		}
		return entries;
	}

	/**
	 * Parts copied from: public Content getContent(String contentURL, String mimeType, String packaging, AuthCredentials auth)
	 * class SWORDClient
	 */
	public String getJSONMetadata(String doiUrl) throws SWORDClientException, IOException {
		if (doiUrl == null) {
			log.error("Null doiUrl passed in to getJSONMetadata; returning null");
			return null;
		}
		if (log.isDebugEnabled()) {
			log.debug("Showing JSON object contents from " + doiUrl);
		}

		AbderaClient client = new AbderaClient(this.abdera);
		RequestOptions options = new RequestOptions();
		options.setHeader("X-Dataverse-key", super.getAuthCredentials().getUsername());

		// ensure that the URL is valid
		URL url = this.formaliseURL(doiUrl);
		if (log.isDebugEnabled()) {
			log.debug("Formalised Collection URL to " + url.toString());
		}

		// make the request for the service document
		if (log.isDebugEnabled()) {
			log.debug("Connecting to Server to get JSON Object " + url.toString() + " ...");
		}
		ClientResponse resp = client.get(url.toString(), options);
		if (log.isDebugEnabled()) {
			log.debug("Successfully retrieved JSON Object from " + url.toString());
		}

		// if the response is successful, get the JSON object out of the response,
		// and return it as String
		if (resp.getType() == Response.ResponseType.SUCCESS) {
			log.info("Successfully retrieved JSON Object from " + url.toString());
			return this.getStringFromInputStream(resp.getInputStream());
		}

		// if we don't get anything respond with null
		log.warn("Unable to retrieve JSON Object from " + url.toString() + "; responded with " + resp.getStatus()
				+ ". Possible problem with Dataverse API, or URL");
		return null;
	}

	/**
	 * Copied from class SWORDClient
	 * 
	 * @param url
	 * @return
	 * @throws SWORDClientException
	 */
	private URL formaliseURL(String url) throws SWORDClientException {
		try {
			URL nurl = new URL(url);
			return nurl;
		} catch (MalformedURLException e) {
			// No dice, can't even form base URL...
			throw new SWORDClientException(url + " is not a valid URL (" + e.getMessage() + ")");
		}
	}
	
	
	/**
	 * Copied from https://www.mkyong.com/java/how-to-convert-inputstream-to-string-in-java/
	 * @param is
	 * @return
	 */
	private String getStringFromInputStream(InputStream is) {

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try {

			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

		} catch (IOException e) {
			log.error("Failed to convert file content to string", e);
			GuiUtils.showErrorDialog(null, e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.error("Failed to close a buffered reader", e);
					GuiUtils.showErrorDialog(null, e);
				}
			}
		}

		return sb.toString();

	}

	/**
	 * @param collectionURL holds the collection URL where the metadata will be exported to
	 * @param metadataMap holds the metadata itself
	 * @return
	 */
	public String createEntryWithMetadata(String collectionURL, Map<String, List<String>> metadataMap) {
		requireNonNull(metadataMap);
		requireNonNull(collectionURL);
		DepositReceipt receipt = null;
		try {
			receipt = (DepositReceipt) exportElement(collectionURL, SwordRequestType.DEPOSIT, MIME_FORMAT_ATOM_XML, null, null, metadataMap);
		} catch (FileNotFoundException | SWORDClientException | SWORDError | ProtocolViolationException e) {
			log.error("Failed to create entry with metadata: {}",collectionURL, e);
			GuiUtils.showErrorDialog(null, e);
		}
		return receipt.getEntry().getEditMediaLinkResolvedHref().toString();
	}

	/**
	 * @param collectionURL holds the collection URL where items will be exported to
	 * @param packedFiles holds a zip file which can contain one or multiple files
	 * @param unpackZip decides whether to unpack the zipfile or places the packed zip file as uploaded data
	 * @param metadataMap holds the metadata which is necessary for the ingest
	 * @return
	 * @throws SWORDClientException 
	 * @throws IOException 
	 */
	public String createEntryWithMetadataAndFile(String collectionURL, File zipFile, boolean unpackZip, Map<String, List<String>> metadataMap){
		requireNonNull(metadataMap);
		requireNonNull(collectionURL);
		String metadataSetUrl = createEntryWithMetadata(collectionURL, metadataMap);
		this.exportFile(metadataSetUrl, zipFile);
		return metadataSetUrl;
	}
	
	/**
	 * @param metadataSetURL The URL where to export the zipFile to.
	 * @param zipFile A zip file that should be exported.
	 * @return
	 * @throws IOException
	 */
	public void exportFile(String metadataSetHrefURL, File zipFile) {
		String packageFormat = getPackageFormat(zipFile.getName()); //zip-archive or separate file
		try {
			exportElement(metadataSetHrefURL, SwordRequestType.DEPOSIT, MIME_FORMAT_ZIP, packageFormat, zipFile, null);
		} catch (FileNotFoundException | SWORDClientException | SWORDError | ProtocolViolationException e) {
			log.error("Failed to export file to repository: {}",metadataSetHrefURL, e);
			GuiUtils.showErrorDialog(null, e);
		}
		
		try {
			FileUtils.delete(zipFile);
		} catch (IOException e) {
			log.error("Failed to delete zip file:", e);
			GuiUtils.showErrorDialog(null, e);
		}
	}
	
	public void replaceMetadataAndAddFile(String collectionURL, String doiUrl, File zipFile, Map<String, List<String>> metadataMap) {
		try {
			replaceMetadataEntry(doiUrl, metadataMap);
		} catch (SWORDClientException e1) {
			log.error("Failed to replace metadata entry in {}",doiUrl, e1);
			GuiUtils.showErrorDialog(null, e1);
		}
		
		Entry entry = null;
		int beginDOI=doiUrl.indexOf("doi:");
		int end=doiUrl.length();
		try {
			entry=getUserAvailableMetadataset(getAtomFeed(collectionURL),doiUrl.substring(beginDOI, end));
		} catch (MalformedURLException | SWORDClientException e) {
			log.error("Failed to get user available metadata sets from dataverse: {}",collectionURL, e);
			GuiUtils.showErrorDialog(null, e);
		}
		exportFile(entry.getEditMediaLinkResolvedHref().toString(), zipFile);
	}
}

/*
 * Unless expressly otherwise stated, code from this project is licensed under the MIT license [https://opensource.org/licenses/MIT].
 * 
 * Copyright (c) <2018> <Markus Gärtner, Volodymyr Kushnarenko, Florian Fritze, Sibylle Hermann and Uli Hahn>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A 
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package bwfdm.replaydh.workflow.export.dspace;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.client.Content;
import org.swordapp.client.DepositReceipt;
import org.swordapp.client.ProtocolViolationException;
import org.swordapp.client.SWORDClientException;
import org.swordapp.client.SWORDCollection;
import org.swordapp.client.SWORDError;
import org.swordapp.client.SWORDWorkspace;
import org.swordapp.client.ServiceDocument;
import org.swordapp.client.SwordResponse;
import org.swordapp.client.UriRegistry;
import org.xml.sax.SAXException;

import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.workflow.export.dspace.WebUtils.RequestType;
import bwfdm.replaydh.workflow.export.dspace.dto.v6.CollectionObject;
import bwfdm.replaydh.workflow.export.dspace.dto.v6.HierarchyObject;
import bwfdm.replaydh.workflow.export.generic.SwordExporter;


/**
 * @author Volodymyr Kushnarenko
 * @author Florian Fritze
 *
 */
public class DSpace_v6 extends SwordExporter implements DSpaceRepository {

	protected static final Logger log = LoggerFactory.getLogger(DSpace_v6.class);

	// For SWORD
	protected String serviceDocumentURL;

	// For REST
	//
	// URLs
	protected String restURL;
	protected String communitiesURL;
	protected String collectionsURL;
	protected String hierarchyURL;
	protected String restTestURL;
	protected String restItemMetadataURL;
	protected String restLogin;
	
	private String standardUser;
	private String password;

	CloseableHttpClient httpClient;
	
	HttpClient httpRestClient;
	
	public DSpace_v6(String serviceDocumentURL, String restURL, String adminUser, String standardUser, char[] adminPassword) throws ClientProtocolException, IOException, URISyntaxException {

		super(SwordExporter.createAuthCredentials(adminUser, adminPassword, standardUser));
		
		requireNonNull(serviceDocumentURL);
		requireNonNull(restURL);
		requireNonNull(adminUser);
		requireNonNull(standardUser);
		requireNonNull(adminPassword);
		
		this.setServiceDocumentURL(serviceDocumentURL);
		this.setAllRestURLs(restURL);
		
		this.standardUser=standardUser;
		this.password=String.valueOf(adminPassword);

		// HttpClient which ignores the ssl certificate
		this.httpClient = WebUtils.createHttpClientWithSSLSupport();
		
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(this.standardUser, this.password);
		provider.setCredentials(AuthScope.ANY, credentials);

		httpRestClient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
		
		URIBuilder builder = new URIBuilder(this.restLogin);
		builder.setParameter("email", this.standardUser);
		builder.setParameter("password", this.password);
		
		httpRestClient.execute(new HttpPost(builder.build()));
		
		// TODO: original version, without ignoring of ssl certificate
		// this.client = HttpClientBuilder.create().build();	
	}
	
	public DSpace_v6(String serviceDocumentURL, String restURL, String userName, char[] userPassword) throws ClientProtocolException, IOException, URISyntaxException {
	
		super(SwordExporter.createAuthCredentials(userName, userPassword));
		
		requireNonNull(serviceDocumentURL);
		requireNonNull(restURL);
		requireNonNull(userName);
		requireNonNull(userPassword);
		
		this.setServiceDocumentURL(serviceDocumentURL);
		this.setAllRestURLs(restURL);
		
		this.standardUser=userName;
		this.password=String.valueOf(userPassword);

		// HttpClient which ignores the ssl certificate
		this.httpClient = WebUtils.createHttpClientWithSSLSupport();
		
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(this.standardUser, this.password);
		provider.setCredentials(AuthScope.ANY, credentials);

		httpRestClient = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
		
		URIBuilder builder = new URIBuilder(this.restLogin);
		builder.setParameter("email", this.standardUser);
		builder.setParameter("password", this.password);
		
		httpRestClient.execute(new HttpPost(builder.build()));
		// TODO: original version, without ignoring of ssl certificate
		// this.client = HttpClientBuilder.create().build();
	}
	

	/*
	 * ----------------------- 
	 * 
	 * DSpace specific methods 
	 * 
	 * -----------------------
	 */

	public void setServiceDocumentURL(String serviceDocumentURL) {
		this.serviceDocumentURL = serviceDocumentURL;
	}

	
	public void setAllRestURLs(String restURL) {
		
		requireNonNull(restURL);
		
		this.restURL = restURL;
		this.communitiesURL = this.restURL + "/communities";
		this.collectionsURL = this.restURL + "/collections";
		this.hierarchyURL = this.restURL + "/hierarchy";
		this.restTestURL = this.restURL + "/test";
		this.restItemMetadataURL = this.restURL + "/items/";
		this.restLogin = this.restURL + "/login";
	}

	
	/**
	 * Check if REST-interface is accessible.
	 * 
	 * @return {@code true} if REST-API is accessible and {@code false} otherwise
	 */
	public boolean isRestAccessible() {

		final CloseableHttpResponse response = WebUtils.getResponse(this.httpClient, this.restTestURL, RequestType.GET,
				APPLICATION_JSON, APPLICATION_JSON);
		if ((response != null) && (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)) {
			WebUtils.closeResponse(response);
			return true;
		} else {
			if (response != null) {
				WebUtils.closeResponse(response);
			}
			return false;
		}
	}

	public String getItemMetadata(String swordEditLink) throws SWORDClientException, IOException, SAXException, ParserConfigurationException {
		int startindex=swordEditLink.indexOf("swordv2/edit/");
		String itemId=swordEditLink.substring(startindex, swordEditLink.length());
		itemId=itemId.replace("swordv2/edit/", "");
		String jsonOutput = getEntryMetadata(this.restItemMetadataURL+itemId+"/metadata");
		return jsonOutput;
	}
	
	/**
	 * Get a list of communities for the current collection. Specific only for DSpace-6.
	 * <p>
	 * REST and SWORD requests are used. ServiceDocument must be received already.
	 * 
	 * @param collectionURL - URL of the collection as {@link String}
	 * @param serviceDocument - object of {@link ServiceDocument}
	 * @param hierarchy - object of {@link HierarchyObject}
	 * @param existedCollectionObjects - array of {@link CollectionObject}
	 * 
	 * @return a {@code List<String>} of communities (0 or more communities are
	 *         possible) or {@code null} if a collection was not found
	 */
	protected List<String> getCommunitiesForCollection(String collectionURL, ServiceDocument serviceDocument,
			HierarchyObject hierarchy, CollectionObject[] existedCollectionObjects) {

		requireNonNull(collectionURL);
		requireNonNull(serviceDocument);
		requireNonNull(hierarchy);
		requireNonNull(existedCollectionObjects);
		
		String collectionHandle = getCollectionHandle(collectionURL, serviceDocument, existedCollectionObjects);
		if (collectionHandle == null) {
			return null;
		}

		List<String> communityList = new ArrayList<String>(0);

		// Get List of communities or "null", if collection is not found
		communityList = hierarchy.getCommunityListForCollection(hierarchy, collectionHandle, communityList);
		
		// remove "Workspace" - it is not a community, but it is always on the first level of the hierarchy
		if (communityList != null) {
			communityList.remove(0); 
		}
		return communityList; // List of communities ( >= 0) or "null"
	}
	
	
	/**
	 * Get a complete hierarchy of collections as HierarchyObject. REST is used.
	 * Works up DSpace-6.
	 * 
	 * @return {@link HierarchyObject}
	 */
	protected HierarchyObject getHierarchyObject() {

		final CloseableHttpResponse response = WebUtils.getResponse(this.httpClient, this.hierarchyURL, RequestType.GET, APPLICATION_JSON, APPLICATION_JSON);
		final HierarchyObject hierarchy = JsonUtils.jsonStringToObject(WebUtils.getResponseEntityAsString(response), HierarchyObject.class);
		WebUtils.closeResponse(response);
		return hierarchy;
	}

	
	/**
	 * Get a collection handle based on the collection URL.
	 * <p>
	 * REST and SWORDv2 requests are used.
	 * 
	 * @param collectionURL a {@link String} with the URL of the collection 
	 * 
	 * @return String with a handle or {@code null} if collectionURL was not found
	 */
	public String getCollectionHandle(String collectionURL) {

		requireNonNull(collectionURL);
		
		ServiceDocument serviceDocument = super.getServiceDocument(this.serviceDocumentURL);

		// Get all collections via REST to check, if swordCollectionPath contains a REST-handle
		CollectionObject[] existedCollectionObjects = getAllCollectionObjects();
		return getCollectionHandle(collectionURL, serviceDocument, existedCollectionObjects);
	}

	
	/**
	 * Get a collection handle based on the collection URL. Private method with logic.
	 * <p>
	 * REST and SWORDv2 requests are used. ServiceDocument must be already retrieved.
	 * 
	 * @param collectionURL - URL of the collection as {@link String}
	 * @param serviceDocument - object of {@link ServiceDocument}
	 * @param existedCollections - array of {@link CollectionObject}
	 * 
	 * @return String with a handle or {@code null} if collectionURL was not found
	 */
	protected String getCollectionHandle(String collectionURL, ServiceDocument serviceDocument,
			CollectionObject[] existedCollections) {

		requireNonNull(collectionURL);
		requireNonNull(serviceDocument);
		requireNonNull(existedCollections);
		
		String swordCollectionPath = ""; // collectionURL without a hostname and port

		for (SWORDWorkspace workspace : serviceDocument.getWorkspaces()) {
			for (SWORDCollection collection : workspace.getCollections()) {
				if (collection.getHref().toString().equals(collectionURL)) {
					swordCollectionPath = collection.getHref().getPath();
				}
			}
		}

		// Compare REST-handle and swordCollectionPath
		for (CollectionObject collection : existedCollections) {
			if (swordCollectionPath.contains(collection.handle)) {
				return collection.handle; //return collection handle
			}
		}
		return null; // collectionURL was not found
	}

	
	/**
	 * Get all existed collections as an array of CollectionObject. REST is used.
	 * 
	 * @return {@link CollectionObject}[]
	 */
	protected CollectionObject[] getAllCollectionObjects() {

		final CloseableHttpResponse response = WebUtils.getResponse(this.httpClient, this.collectionsURL, RequestType.GET,
				APPLICATION_JSON, APPLICATION_JSON);
		final CollectionObject[] collections = JsonUtils
				.jsonStringToObject(WebUtils.getResponseEntityAsString(response), CollectionObject[].class);
		WebUtils.closeResponse(response);
		return collections;
	}
	
	
	/**
	 * Export metadata only to some already existed entry (via edit-URL, @link {@link SwordRequestType}) or create a Private method which can use different request types. See
	 * {@link SwordRequestType}.
	 * 
	 * @param url - collection URL (with "collection" substring inside) or item URL (with "edit" substring inside)
	 * 				where to to export (or edit) metadata 
	 * @param metadataMap - metadata as a Map
	 * @param swordRequestType - object of {@link SwordRequestType}
	 *
	 * @return {@link String} with the URL to edit the entry (with "edit" substring inside)
	 * 
	 * @throws {@link IOException}
	 * @throws {@link SWORDClientException}
	 * @throws {@link SWORDError}
	 * @throws {@link ProtocolViolationException}
	 */
	protected String exportMetadataAsMap(String url, Map<String, List<String>> metadataMap,
			SwordRequestType swordRequestType)throws IOException, SWORDClientException, SWORDError, ProtocolViolationException {

		SwordResponse response = super.exportElement(url, swordRequestType, SwordExporter.MIME_FORMAT_ATOM_XML, 
				UriRegistry.PACKAGE_BINARY, null, metadataMap);
		
		if(response instanceof DepositReceipt) {
			return ((DepositReceipt)response).getEditLink().getHref(); //response from DEPOSIT request
		} else {
			//FIXME: 	
			//current implementation of SWORD-Client library returns in case of "REPLACE" request
			//the SwordResponse object only with the actual status code, 
			//"Location" link and other fields are "null". That's why to avoid misunderstanding, 
			//when "Location" link is null we return just an input-url (which also should be the "edit" url 
			//in case of REPLACE request) 
			//			
			String editURL = response.getLocation(); //should be a string with the edit URL ("/swordv2/edit/" substring inside)
			return ((editURL != null) ? editURL : url); // return input-url if "Location" link is null
		}
	}

	
	/**
	 * Private method which can use different request types. See
	 * {@link SwordRequestType}.
	 * 
	 * @param url - collection URL (with "collection" substring inside) or item URL (with "edit" substring inside)
	 * 				where to to export (or edit) metadata
	 * @param metadataFileXML 
	 * 			  - file in XML-format (ATOM format of the metadata description) and
	 *            	with an XML-extension  
	 * @param swordRequestType - object of {@link SwordRequestType}
	 * 
	 * @return {@link String} with the URL to edit the entry (with "edit" substring inside)
	 * 
	 * @throws {@link IOException}
	 * @throws {@link SWORDClientException}
	 * @throws {@link SWORDError}
	 * @throws {@link ProtocolViolationException}
	 */
	protected String exportMetadataAsFile(String url, File metadataFileXML, SwordRequestType swordRequestType) 
				throws IOException, SWORDClientException, SWORDError, ProtocolViolationException{

		// Check if file has an XML-extension
		String ext = super.getFileExtension(metadataFileXML.getName()).toLowerCase();
		if (!ext.equals("xml")) {
			log.error("Wrong metadata file extension: {} : Supported extension is: {}",	ext, "xml");
			throw new ProtocolViolationException("Wrong metadta file extension: " + ext + " : Supported extension is: xml");
		}

		String mimeFormat = SwordExporter.MIME_FORMAT_ATOM_XML;
		String packageFormat = super.getPackageFormat(metadataFileXML.getName());
		SwordResponse response = super.exportElement(url, swordRequestType, mimeFormat, packageFormat, metadataFileXML, null);
		
		if(response instanceof DepositReceipt) {
			return ((DepositReceipt)response).getEditLink().getHref(); //response from DEPOSIT request
		} else {
			//FIXME: 	
			//current implementation of SWORD-Client library returns in case of "REPLACE" request
			//the SwordResponse object only with the actual status code, 
			//"Location" link and other fields are "null". That's why to avoid misunderstanding, 
			//when "Location" link is null we return just an input-url (which also should be the "edit" url 
			//in case of REPLACE request) 
			//			
			String editURL = response.getLocation(); //should be a string with the edit URL ("/swordv2/edit/" substring inside)
			return ((editURL != null) ? editURL : url); // return input-url if "Location" link is null
		}
	}
	
	
	/**
	 * Export (create) a new entry with a file in some collection, which is available.
	 * <p>
	 * IMPORTANT: authentication credentials are used implicitly. 
	 * Definition of the credentials is realized via the class constructor.
	 *
	 * @param collectionURL the full URL of the collection, where the export (ingest) will be done 
	 * @param file an archive file with one or more files inside (e.g. ZIP-file as a standard) or a binary file 
	 * 			which will be exported.
	 * @param unpackFileIfArchive should be used for archive files (e.g. ZIP). A flag which decides, 
	 * 			if the exported archive will be unpacked in the repository ({@code true} value,
	 * 			new entry will include in this case all files of the archive file) or archive will be exported 
	 * 			as a binary file ({@code false} value, new entry will include only 1 file - the exported archive
	 * 			as a binary file). <b>NOTE:</b> if unpacking is not supported by the repository, 
	 * 			please use {@code false} value.
	 *
	 * @return {@link String} with the URL of the new created entry or {@code null} in case of error.	
	 *
	 * @throws IOException
	 */
	public String exportNewEntryWithFile(String collectionUrl, File file, boolean unpackFileIfArchive) throws IOException {

		requireNonNull(collectionUrl);
		requireNonNull(file);
		requireNonNull(unpackFileIfArchive);
		
		String mimeFormat = SwordExporter.MIME_FORMAT_ZIP; // for every file type, to publish even "XML" files as a normal file
		String packageFormat = SwordExporter.getPackageFormat(file.getName(), unpackFileIfArchive); // unpack zip-archive or export as a binary 
		
		try {
			SwordResponse response = super.exportElement(collectionUrl, SwordRequestType.DEPOSIT, mimeFormat, packageFormat, file, null);
			if(response instanceof DepositReceipt) {
				return ((DepositReceipt)response).getEditLink().getHref(); // "edit" URL from the DEPOSIT receipt
			} else {
				return null; // for current moment we should receipt a DepositReceipt object. If not, that something went wrong. 
			}
		} catch (SWORDClientException | SWORDError | ProtocolViolationException e) {
			log.error("Exception by exporting new entry with file only: {}: {}", e.getClass().getSimpleName(), e.getMessage());
			return null;
		}
	}

	
	/**
	 * TODO: move method declaration and javadoc to the SwordExporter abstract class.
	 * 
	 * Create new entry with metadata only (without any file) in some collection.
	 * Metadata are described as a XML-file.
	 * <p>
	 * IMPORTANT: authentication credentials are used implicitly. 
	 * Definition of the credentials is realized via the class constructor.
	 *
	 * @param collectionURL holds the collection URL where the metadata will be exported to.
	 * @param metadataFileXml XML-file with metadata in ATOM format.
	 * 
	 * @return {@link String} with the entry URL which includes "/swordv2/edit/" substring inside. 
	 * 		This URL could be used without changes for further update of the metadata 
	 * 		(see {@link #replaceMetadataEntry(String, Map) replaceMetadataEntry(entryURL, metadataMap)}) 
	 * 		<p>
	 * 		<b>IMPORTANT for Dataverse repository:</b> for further update/extension of the media part 
	 * 		(e.g. uploaded files inside the dataset) please replace "/swordv2/edit/" substring inside the entry URL to 
	 * 		"/swordv2/edit-media/". 
	 * 		For more details please visit <a href="http://guides.dataverse.org/en/latest/api/sword.html">http://guides.dataverse.org/en/latest/api/sword.html</a>
	 * 		<p>
	 * 		<b>IMPORTANT for DSpace repository:</b> further update/extension of the media part (e.g. uploaded files)
	 * 		via SWORD is not supported, only update of the metadata is allowed.
	 * 
	 * @throws IOException
	 * @throws SWORDClientException
	 */
	public String createEntryWithMetadata(String collectionURL, File metadataFileXml) throws IOException, SWORDClientException{
		
		requireNonNull(collectionURL);
		requireNonNull(metadataFileXml);
		
		try {
			return exportMetadataAsFile(collectionURL, metadataFileXml, SwordRequestType.DEPOSIT);
		} catch (ProtocolViolationException | SWORDError e) {
			throw new SWORDClientException("Exception by creation of item with only metadta as XML-file: " 
					+ e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
	
	
	/**
	 * TODO: move method declaration and javadoc to the SwordExporter abstract class.
	 * 
	 * Create a new entry with some file and metadata in the provided collection.
	 * Metadata are described as a XML-file.
	 * <p>
	 * IMPORTANT: authentication credentials are used implicitly. 
	 * Definition of the credentials is realized via the class constructor.
	 * <p>
	 * For DSpace: export will be realized in 2 steps: 1 - export a file (create a new entry), 
	 * 2 - add metadata via PUT request.
	 * 
	 * @param collectionURL holds the collection URL where items will be exported to, usually has "collection" substring inside
	 * @param file holds a file which can contain one or multiple files
	 * @param unpackZip decides whether to unpack the zipfile or places the packed zip file as uploaded data
	 * @param metadataFileXml holds the metadata which is necessary for the ingest
	 *
	 * @throws IOException
	 * @throws {@link SWORDClientException}
	 */
	public String createEntryWithMetadataAndFile(String collectionURL, File file, boolean unpackZip, File metadataFileXml)
			throws IOException, SWORDClientException {
		
		requireNonNull(collectionURL);
		requireNonNull(file);
		requireNonNull(unpackZip);
		requireNonNull(metadataFileXml);
		
		String mimeFormat = SwordExporter.MIME_FORMAT_ZIP; // as a common file (even for XML-file)
		String packageFormat = SwordExporter.getPackageFormat(file.getName(), unpackZip);

		try {
			// Step 1: export file (as file or archive), without metadata
			SwordResponse response = super.exportElement(collectionURL, SwordRequestType.DEPOSIT, mimeFormat, 
					packageFormat, file, null); // "POST" request (DEPOSIT)
			String editLink = response.getLocation();
			if (editLink == null) {
				throw new SWORDClientException("Error by exporting file and metadta as xml-file: "
						+ "after the file export the item URL for editing (as a response) is null. "
						+ "Not possible to add metadata as the next step.");
			}
			
			// Step 2: add metadata (as a XML-file)
			//
			// "PUT" request (REPLACE) is used to overwrite some previous automatically generated metadata
			return exportMetadataAsFile(editLink, metadataFileXml, SwordRequestType.REPLACE);
	
			// NOTE: if replace order (step 1: export metadata, step 2: export file) --> Bad request, ERROR 400
			
		} catch (ProtocolViolationException | SWORDError e) {
			throw new SWORDClientException("Exception by exporting file and metadta as xml-file: " 
						+ e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
		
	
	/*
	 * ---------------------------------
	 * 
	 * SwordExporter methods realization
	 * 
	 * ---------------------------------
	 */

	
	/**
	 * {@inheritDoc}
	 * 
	 * Implementation via parsing of response String using regular expressions.
	 */
	//TODO: return "null" in case of error
	@Override
	public Map<String, String> getCollectionEntries(String collectionUrl) {
		
		requireNonNull(collectionUrl);
		
		Map<String, String> entriesMap = new HashMap<String, String>();
		
		try {
			// Get request on collectionUrl, same as via "curl" 
			// -> curl -i $collectionUrl --user "$USER_MAIL:$USER_PASSWORD"
			Content content = super.getSwordClient().getContent(collectionUrl, SwordExporter.MIME_FORMAT_ATOM_XML, 
					UriRegistry.PACKAGE_SIMPLE_ZIP, super.getAuthCredentials());
			try {
				String response = IOUtils.readStream(content.getInputStream());
				
				Pattern entryPattern = Pattern.compile("<entry>(.+?)</entry>", Pattern.DOTALL | Pattern.MULTILINE); //e.g. "<entry>some_entry_with_other_tags_inside</entry>
				Matcher entryMatcher = entryPattern.matcher(response);
				
				// Find all entries
				while(entryMatcher.find()) {
					String entryString = entryMatcher.group(1);
					
					Pattern idPattern = Pattern.compile("<id>(.+?)</id>", Pattern.DOTALL | Pattern.MULTILINE); //e.g. "<id>https://some_link</id>"
					Matcher idMatcher = idPattern.matcher(entryString);
					
					//Pattern titlePattern = Pattern.compile("<title.+?>(.+?)</title>", Pattern.DOTALL | Pattern.MULTILINE); //e.g. "<title type="text">some_title</title>" 
					//Matcher titleMatcher = titlePattern.matcher(entryString);
					
					// Find id and title
					if(idMatcher.find()) { 
						entriesMap.put(idMatcher.group(1), this.createUniqueEntryID(this.getEntryMetadata(idMatcher.group(1))));
					}
				}
			} catch (IOException e) {
				log.error("Exception by converting Bitstream to String: {}: {}", e.getClass().getSimpleName(), e.getMessage());
			}	
		} catch (SWORDClientException | ProtocolViolationException | SWORDError e) {
			log.error("Exception by getting content (request) via SWORD: {}: {}", e.getClass().getSimpleName(), e.getMessage());
		}
		return entriesMap;
	}
	
		
	/**
	 * Parts copied from: https://www.baeldung.com/httpclient-4-basic-authentication
	 * @throws IOException 
	 * @throws ClientProtocolException 
	 */
	public String getEntryMetadata(String datasetSwordLink) throws ClientProtocolException, IOException {
		if (datasetSwordLink == null) {
			log.error("Null datasetUrl passed in to getEntryMetadata; returning null");
			return null;
		}

		HttpResponse response = httpRestClient.execute(new HttpGet(datasetSwordLink));
		int statusCode = response.getStatusLine().getStatusCode();
		
		String responseString=null;
		
		if (statusCode == HttpStatus.SC_OK) {
			HttpEntity entity = response.getEntity();
			responseString = EntityUtils.toString(entity, "UTF-8");
		}
		if ((log.isDebugEnabled()) && (responseString != null)) {
			log.debug("Connecting with" + datasetSwordLink + "was successfully");
		}

		return responseString;
	}
	
	
	public String createUniqueEntryID(String entryXML) {
		String response=entryXML;
		
		String identifier=null;
		Pattern entryPattern = Pattern.compile("<entry.+?>(.*?)</entry>", Pattern.DOTALL | Pattern.MULTILINE); //e.g. "<entry>some_entry_with_other_tags_inside</entry>
		Matcher entryMatcher = entryPattern.matcher(response);
		
		entryMatcher.find();
		
		String entryString = entryMatcher.group(1);
		
		Pattern availablePattern = Pattern.compile("<available.+?>(.+?)</available>", Pattern.DOTALL | Pattern.MULTILINE); //e.g. "<id>https://some_link</id>"
		Matcher availableMatcher = availablePattern.matcher(entryString);
		
		availableMatcher.find();
		
		Pattern titlePattern = Pattern.compile("<title.+?>(.+?)</title>", Pattern.DOTALL | Pattern.MULTILINE); //e.g. "<title type="text">some_title</title>" 
		Matcher titleMatcher = titlePattern.matcher(entryString);
		
		if(availableMatcher.group(1).toString().equals("Date Available")) {
			Pattern updatedPattern = Pattern.compile("<updated>(.+?)</updated>", Pattern.DOTALL | Pattern.MULTILINE); //e.g. "<id>https://some_link</id>"
			Matcher updatedMatcher = updatedPattern.matcher(entryString);
			if(updatedMatcher.find() && titleMatcher.find()) {
				identifier=updatedMatcher.group(1)+ " - " + titleMatcher.group(1);
			}
		} else {
			if(titleMatcher.find()) {
				identifier=availableMatcher.group(1)+ " - " + titleMatcher.group(1);
			}
		}
		
		
		
		
		return identifier;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String createEntryWithMetadata(String collectionURL, Map<String, List<String>> metadataMap) 
			throws SWORDClientException {
		
		requireNonNull(collectionURL);
		requireNonNull(metadataMap);
		
		try {			
			return exportMetadataAsMap(collectionURL, metadataMap, SwordRequestType.DEPOSIT);			
		} catch (IOException | ProtocolViolationException | SWORDError e) {
			throw new SWORDClientException("Exception by export metadta as Map: " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
	
		
	/**
	 * {@inheritDoc}
	 * <p>
	 * DSpace: export will be realized in 2 steps: 1 - export a file (create a new entry), 
	 * 2 - add metadata via PUT request.
	 */
	@Override
	public String createEntryWithMetadataAndFile(String collectionURL, File file, boolean unpackZip, Map<String, List<String>> metadataMap)
			throws IOException, SWORDClientException {
		
		requireNonNull(collectionURL);
		requireNonNull(file);
		requireNonNull(unpackZip);
		requireNonNull(metadataMap);
		
		String mimeFormat = SwordExporter.MIME_FORMAT_ZIP; // as a common file (even for XML-file)
		String packageFormat = SwordExporter.getPackageFormat(file.getName(), unpackZip);
		
		try {
			// Step 1: export file (as file or archive), without metadata
			SwordResponse response = super.exportElement(collectionURL, SwordRequestType.DEPOSIT, mimeFormat, 
					packageFormat, file, null); // "POST" request (DEPOSIT)
			String editLink = response.getLocation();
			if (editLink == null) {
				throw new SWORDClientException("Error by export file and metadta as Map: "
						+ "after the file export the item URL for editing (as a response) is null. "
						+ "Not possible to add metadata as the next step.");
			}
			
			// Step 2: add metadata (as a Map structure)
			//
			//"PUT" request (REPLACE) is used to overwrite some previous automatically generated metadata
			return exportMetadataAsMap(editLink, metadataMap, SwordRequestType.REPLACE);
						
			// NOTE: if replace order (step 1: export metadata, step 2: export file) --> Bad request, ERROR 400
			
		} catch (ProtocolViolationException | SWORDError e) {
			throw new SWORDClientException("Exception by export file and metadta as Map: " 
						+ e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		
	}
		
	
	/*
	 * -----------------------------------------
	 * 
	 * DSpaceRepository interface implementation
	 * 
	 * -----------------------------------------
	 */
	
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * DSpace-v6: REST and SWORD requests will be used.
	 */
	@Override
	public List<String> getCommunitiesForCollection(String collectionURL) {

		ServiceDocument serviceDocument = super.getServiceDocument(this.serviceDocumentURL);
		HierarchyObject hierarchy = getHierarchyObject();
		CollectionObject[] existedCollectionObjects = getAllCollectionObjects();

		return getCommunitiesForCollection(collectionURL, serviceDocument, hierarchy, existedCollectionObjects);
	}
	
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * DSpace-v6: REST and SWORD requests will be used. 
	 */
	@Override
	public Map<String, String> getAvailableCollectionsWithFullName(String fullNameSeparator) {

		// Get available collections from the ServiceDocument (SWORD)
		ServiceDocument serviceDocument = super.getServiceDocument(this.serviceDocumentURL);
		Map<String, String> collectionsMap = super.getCollections(serviceDocument);
		
		// Get complete hierarchy of collections and array of CollectionOnject-s (REST) 
		final HierarchyObject hierarchy = getHierarchyObject();
		final CollectionObject[] existedCollectionObjects = getAllCollectionObjects();

		// Extend collection name with communities and separators
		for (String collectionUrl : collectionsMap.keySet()) {
			List<String> communities = this.getCommunitiesForCollection(collectionUrl, serviceDocument, hierarchy, existedCollectionObjects);
			String fullName = "";
			if(communities != null) {
				for (String community : communities) {
					fullName += community + fullNameSeparator; // add community + separator
				}
				fullName += collectionsMap.get(collectionUrl); // add collection name (title)
				collectionsMap.put(collectionUrl, fullName);
			}
		}
		return collectionsMap;
	}
		
	
	/*
	 * -----------------------------------------
	 * 
	 * ExportRepository interface implementation
	 * 
	 * -----------------------------------------
	 */
	
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * For DSpace it is done by access to the Service Document via SWORD-protocol
	 * and checking an access to the REST-API.
	 * 
	 * @return {@code true} if service document and REST-API are accessible, and
	 *         {@code false} otherwise (e.g. by Error 403).
	 */
	@Override
	public boolean isRepositoryAccessible() {
		return (isRestAccessible() && super.isSwordAccessible(this.serviceDocumentURL));
	}

	
	/**
	 * {@inheritDoc}
	 * <p>
	 * In DSpace it will be checked via access to the service document
	 * (SWORD-protocol).
	 */
	@Override
	public boolean hasRegisteredCredentials() {
		return super.getServiceDocument(this.serviceDocumentURL) != null;
	}

	
	/**
	 * {@inheritDoc}
	 * <p>
	 * In DSpace it will be checked via access to the service document and 
	 * count of available for the current authentication credentials collections.
	 * 
	 * @return {@code true} if there is at least 1 collection for export, 
	 * 			and {@code false} if there are not available collections.
	 */
	@Override
	public boolean hasAssignedCredentials() {
		
		ServiceDocument serviceDocument = super.getServiceDocument(this.serviceDocumentURL);
		if (serviceDocument != null) {
			for (SWORDWorkspace workspace : serviceDocument.getWorkspaces()) {
				if(workspace.getCollections().size() > 0) {
					return true; // there is at least 1 available collection, credentials are assigned to export
				}
			}
		}
		return false;
	}

	
	/**
	 * {@inheritDoc}
	 * <p>
	 * DSpace-v6: SWORD requests will be done.
	 */
	@Override
	public Map<String, String> getAvailableCollections() {
		ServiceDocument serviceDocument = super.getServiceDocument(this.serviceDocumentURL);
		return super.getCollections(serviceDocument);
	}

	
	/**
	 * {@inheritDoc}
	 * <p>
	 * DSpace-v6: SWORD requests will be done.
	 */
	@Override
	public String exportNewEntryWithMetadata(String collectionURL, Map<String, List<String>> metadataMap) {
		try {
			return this.createEntryWithMetadata(collectionURL, metadataMap);
		} catch (SWORDClientException e) {
			log.error("Exception by creation of new entry with metadata as Map.", e);
			return null;
		}
	}

	
	/**
	 * {@inheritDoc}
	 * <p>
	 * DSpace-v6: SWORD requests will be done.
	 */
	@Override
	public String exportNewEntryWithFileAndMetadata(String collectionURL, File file, boolean unpackFileIfArchive,
			Map<String, List<String>> metadataMap) throws IOException {
		
		try {
			return this.createEntryWithMetadataAndFile(collectionURL, file, unpackFileIfArchive, metadataMap);
		} catch (SWORDClientException e) {
			log.error("Exception by creation of new entry with file and metadata as Map.", e);
			return null;
		}
	}

}

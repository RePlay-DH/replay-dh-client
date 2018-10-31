/*
 * Unless expressly otherwise stated, code from this project is licensed under the MIT license [https://opensource.org/licenses/MIT].
 * 
 * Copyright (c) <2018> <Volodymyr Kushnarenko, Stefan Kombrink, Markus Gärtner, Florian Fritze, Matthias Fratz, Daniel Scharon, Sibylle Hermann, Franziska Rapp and Uli Hahn>
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
package bwfdm.replaydh.workflow.export.dspace.refactor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.client.Content;
import org.swordapp.client.ProtocolViolationException;
import org.swordapp.client.SWORDClientException;
import org.swordapp.client.SWORDCollection;
import org.swordapp.client.SWORDError;
import org.swordapp.client.SWORDWorkspace;
import org.swordapp.client.ServiceDocument;
import org.swordapp.client.SwordResponse;
import org.swordapp.client.UriRegistry;

import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.workflow.export.dspace.dto.v6.CollectionObject;
import bwfdm.replaydh.workflow.export.dspace.dto.v6.HierarchyObject;
import bwfdm.replaydh.workflow.export.dspace.refactor.WebUtils.RequestType;
import bwfdm.replaydh.workflow.export.generic.SwordExporter;



public class DSpace_v6 extends DSpaceRepository {

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

	CloseableHttpClient client;

	public DSpace_v6(String serviceDocumentURL, String restURL, String adminUser, String standardUser, char[] adminPassword) {

		super(SwordExporter.createAuthCredentials(adminUser, adminPassword, standardUser));
		
		this.setServiceDocumentURL(serviceDocumentURL);
		this.setAllRestURLs(restURL);

		// HttpClient which ignores the ssl certificate
		this.client = WebUtils.createHttpClientWithSSLSupport();

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
		this.restURL = restURL;
		this.communitiesURL = this.restURL + "/communities";
		this.collectionsURL = this.restURL + "/collections";
		this.hierarchyURL = this.restURL + "/hierarchy";
		this.restTestURL = this.restURL + "/test";
	}

	
	/**
	 * Check if REST-interface is accessible.
	 * 
	 * @return {@code true} if REST-API is accessible and {@code false} otherwise
	 */
	public boolean isRestAccessible() {

		final CloseableHttpResponse response = WebUtils.getResponse(this.client, this.restTestURL, RequestType.GET,
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

	
	/**
	 * {@inheritDoc}
	 * 
	 * For DSpace-v6 REST and SWORD requests are used.
	 */
	@Override
	public List<String> getCommunitiesForCollection(String collectionURL) {

		ServiceDocument serviceDocument = super.getServiceDocument(this.serviceDocumentURL);

		HierarchyObject hierarchy = getHierarchyObject();
		CollectionObject[] existedCollectionObjects = getAllCollectionObjects();

		return getCommunitiesForCollection(collectionURL, serviceDocument, hierarchy, existedCollectionObjects);
	}
	
	
	/**
	 * Private method with logic. Get a list of communities for the current collection.
	 * Specific only for DSpace-6.
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

		String collectionHandle = getCollectionHandle(collectionURL, serviceDocument, existedCollectionObjects);
		if (collectionHandle == null) {
			return null;
		}

		List<String> communityList = new ArrayList<String>(0);

		// Get List of communities or "null", if collection is not found
		communityList = hierarchy.getCommunityListForCollection(hierarchy, collectionHandle, communityList);

		if (communityList != null) {
			communityList.remove(0); 	// remove "Workspace" - it is not a community,
										// but it is always on the first level of the hierarchy
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

		final CloseableHttpResponse response = WebUtils.getResponse(this.client, this.hierarchyURL, RequestType.GET, APPLICATION_JSON, APPLICATION_JSON);
		final HierarchyObject hierarchy = JsonUtils.jsonStringToObject(WebUtils.getResponseEntityAsString(response), HierarchyObject.class);
		WebUtils.closeResponse(response);
		return hierarchy;
	}

	
	/**
	 * Get a collection handle based on the collection URL.
	 * <p>
	 * REST and SWORDv2 requests are used.
	 * 
	 * @param collectionURL - URL of the collection as {@link String}
	 * 
	 * @return String with a handle or {@code null} if collectionURL was not found
	 */
	public String getCollectionHandle(String collectionURL) {

		ServiceDocument serviceDocument = super.getServiceDocument(this.serviceDocumentURL);

		// Get all collections via REST to check, if swordCollectionPath contains a REST-handle
		CollectionObject[] existedCollectionObjects = getAllCollectionObjects();

		return getCollectionHandle(collectionURL, serviceDocument, existedCollectionObjects);
	}

	
	/**
	 * Private method with logic. Get a collection handle based on the collection URL.
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
				return collection.handle;
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

		final CloseableHttpResponse response = WebUtils.getResponse(this.client, this.collectionsURL, RequestType.GET,
				APPLICATION_JSON, APPLICATION_JSON);
		final CollectionObject[] collections = JsonUtils
				.jsonStringToObject(WebUtils.getResponseEntityAsString(response), CollectionObject[].class);
		WebUtils.closeResponse(response);
		return collections;
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
	@Override
	public Map<String, String> getCollectionEntries(String collectionUrl) {
			
		Map<String, String> entriesMap = new HashMap<String, String>();
		
		try {
			// Get request on collectionUrl, same as via "curl" 
			// -> curl -i $collectionUrl --user "$USER_MAIL:$USER_PASSWORD"
			Content content = super.getSwordClient().getContent(collectionUrl, SwordExporter.MIME_FORMAT_ATOM_XML, 
					UriRegistry.PACKAGE_SIMPLE_ZIP, super.getAuthCredentials());
			try {
				String response = IOUtils.readStream(content.getInputStream());
				System.out.println("Response: " + response);
				
				Pattern entryPattern = Pattern.compile("<entry>(.+?)</entry>"); //e.g. "<entry>some_entry_with_other_tags_inside</entry>
				Matcher entryMatcher = entryPattern.matcher(response);
				
				// Find all entries
				while(entryMatcher.find()) {
					String entryString = entryMatcher.group(1);
					
					Pattern idPattern = Pattern.compile("<id>(.+?)</id>"); //e.g. "<id>https://some_link</id>"
					Matcher idMatcher = idPattern.matcher(entryString);
					
					Pattern titlePattern = Pattern.compile("<title.+?>(.+?)</title>"); //e.g. "<title type="text">some_title</title>" 
					Matcher titleMatcher = titlePattern.matcher(entryString);
					
					// Find id and title
					if(idMatcher.find() && titleMatcher.find()) { 
						entriesMap.put(idMatcher.group(1), titleMatcher.group(1));
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
	 * {@inheritDoc}
	 * <p>
	 * In DSpace it means an export to some collection. SWORD-v2 protocol is used.
	 * 
	 * @param url - URL of the collection where to export
	 * @param file - full path to the file for export 
	 */
	@Override
	public void exportFile(String url, File file) throws IOException, SWORDClientException {

		String mimeFormat = SwordExporter.MIME_FORMAT_ZIP; // for every file type, to publish even "XML" files as a normal file
		String packageFormat = SwordExporter.getPackageFormat(file.getName()); // zip-archive or separate file
		try {
			super.exportElement(url, SwordRequestType.DEPOSIT, mimeFormat, packageFormat, file, null);
		} catch (SWORDError | ProtocolViolationException e) {
			throw new SWORDClientException("Exception by export file: " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void exportMetadata(String collectionURL, Map<String, List<String>> metadataMap) 
			throws SWORDClientException {
		try {
			exportMetadataViaSword(collectionURL, metadataMap, SwordRequestType.DEPOSIT);
		} catch (IOException | ProtocolViolationException | SWORDError e) {
			throw new SWORDClientException("Exception by export metadta as Map: " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * Publish metadata as a XML-file in ATOM-format.
	 * 
	 * @param collectionURL - URL of the collection where to publish
	 * @param metadataFileXML - file in XML-format (ATOM format of the metadata description) and
	 *            			    with an XML-extension
	 * @throws IOException
	 * @throws {@link SWORDClientException}
	 */
	public void exportMetadata(String collectionURL, File metadataFileXML) throws IOException, SWORDClientException{
		try {
			exportMetadataViaSword(collectionURL, metadataFileXML, SwordRequestType.DEPOSIT);
		} catch (ProtocolViolationException | SWORDError e) {
			throw new SWORDClientException("Exception by export file and metadta as XML-file: " 
					+ e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * Export will be realized via 2 steps: 1 - export a file (create a new item), 2 - add metadata via PUT request.
	 */
	@Override
	public void exportMetadataAndFile(String collectionURL, File file, Map<String, List<String>> metadataMap)
			throws IOException, SWORDClientException {
		
		String mimeFormat = SwordExporter.MIME_FORMAT_ZIP; // as a common file (even for XML-file)
		String packageFormat = SwordExporter.getPackageFormat(file.getName());

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
			exportMetadataViaSword(editLink, metadataMap, SwordRequestType.REPLACE); 	// "PUT" request (REPLACE) 
																						// to overwrite some previous
																						// automatically generated 
																						// metadata 
	
			// NOTE: if replace order (step 1: export metadata, step 2: export file) --> Bad request, ERROR 400
			
		} catch (ProtocolViolationException | SWORDError e) {
			throw new SWORDClientException("Exception by export file and metadta as Map: " 
						+ e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		
	}
	
	
	/**
	 * Export a file together with the metadata to some collection.
	 * Metadata are described as a xml-file.
	 * <p>
	 * IMPORTANT: authentication credentials are used implicitly. Definition of the credentials is realized via the class constructor.
	 * <p>
	 * Export will be realized via 2 steps: 1 - export a file (create a new item), 2 - add metadata via PUT request.
	 * 
	 * @param collectionURL holds the collection URL where items will be exported to, usually has "collection" substring inside
	 * @param file holds a file which can contain one or multiple files
	 * @param metadataFileXML holds the metadata which is necessary for the ingest
	 *
	 * @throws @linkIOException
	 * @throws SWORDClientException
	 */
	public void exportMetadataAndFile(String collectionURL, File file, File metadataFileXML)
			throws IOException, SWORDClientException {
		
		String mimeFormat = SwordExporter.MIME_FORMAT_ZIP; // as a common file (even for XML-file)
		String packageFormat = SwordExporter.getPackageFormat(file.getName());

		try {
			// Step 1: export file (as file or archive), without metadata
			SwordResponse response = super.exportElement(collectionURL, SwordRequestType.DEPOSIT, mimeFormat, 
					packageFormat, file, null); // "POST" request (DEPOSIT)
			String editLink = response.getLocation();
			if (editLink == null) {
				throw new SWORDClientException("Error by export file and metadta as xml-file: "
						+ "after the file export the item URL for editing (as a response) is null. "
						+ "Not possible to add metadata as the next step.");
			}
			
			// Step 2: add metadata (as a Map structure)
			exportMetadataViaSword(editLink, metadataFileXML, SwordRequestType.REPLACE);// "PUT" request (REPLACE) 
																						// to overwrite some previous
																						// automatically generated 
																						// metadata 
	
			// NOTE: if replace order (step 1: export metadata, step 2: export file) --> Bad request, ERROR 400
			
		} catch (ProtocolViolationException | SWORDError e) {
			throw new SWORDClientException("Exception by export file and metadta as xml-file: " 
						+ e.getClass().getSimpleName() + ": " + e.getMessage());
		}
		
	}
	
	
	/**
	 * Private method which can use different request types. See
	 * {@link SwordRequestType}.
	 * 
	 * @param url - collection URL (with "collection" substring inside) or item URL (with "edit" substring inside)
	 * 				where to to export (or edit) metadata 
	 * @param metadataMap - metadata as a Map
	 * @param swordRequestType - object of {@link SwordRequestType}
	 * 
	 * @throws {@link IOException}
	 * @throws {@link SWORDClientException}
	 * @throws {@link SWORDError}
	 * @throws {@link ProtocolViolationException}
	 */
	protected void exportMetadataViaSword(String url, Map<String, List<String>> metadataMap,
			SwordRequestType swordRequestType)throws IOException, SWORDClientException, SWORDError, ProtocolViolationException {

		String mimeFormat = SwordExporter.MIME_FORMAT_ATOM_XML;
		String packageFormat = UriRegistry.PACKAGE_BINARY;

		try {
			super.exportElement(url, swordRequestType, mimeFormat, packageFormat, null,	metadataMap);
		} catch (IOException e) {
			log.error("Exception by export metadata as Map: {}: {}", e.getClass().getSimpleName(), e.getMessage());
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
	 * @return {@code true} if case of success and {@code false} otherwise
	 */
	protected void exportMetadataViaSword(String url, File metadataFileXML, SwordRequestType swordRequestType) 
				throws IOException, SWORDClientException, SWORDError, ProtocolViolationException{

		// Check if file has an XML-extension
		String ext = super.getFileExtension(metadataFileXML.getName()).toLowerCase();
		if (!ext.equals("xml")) {
			log.error("Wrong metadata file extension: {} : Supported extension is: {}",	ext, "xml");
			throw new ProtocolViolationException("Wrong metadta file extension: " + ext + " : Supported extension is: xml");
		}

		String mimeFormat = SwordExporter.MIME_FORMAT_ATOM_XML;
		String packageFormat = super.getPackageFormat(metadataFileXML.getName());
		super.exportElement(url, swordRequestType, mimeFormat, packageFormat, metadataFileXML, null);
	}
	
	
	/*
	 * ---------------------------------------
	 * 
	 * DSpaceRepository methods implementation
	 * 
	 * ---------------------------------------
	 */
	
	
	/**
	 * {@inheritDoc}
	 * 
	 * In DSpace-v6 SWORD and REST are used. 
	 */
	@Override
	public Map<String, String> getAvailableCollectionsWithFullName(String fullNameSeparator) {

		// Get all available for the user collections from the ServiceDocument 
		// (via SWORD)
		ServiceDocument serviceDocument = super.getServiceDocument(this.serviceDocumentURL);
		Map<String, String> collectionsMap = super.getCollections(serviceDocument); // all available collections
		
		// Get complete hierarchy of collections. Is needed later to get communities of
		// the collection.
		final HierarchyObject hierarchy = getHierarchyObject();

		// Get all existed collections via REST as an array of CollectionObject. Is
		// needed later to get communities of the collection.
		final CollectionObject[] existedCollectionObjects = getAllCollectionObjects();

		// Extend the collection name with communities and separators
		for (String url : collectionsMap.keySet()) {
			List<String> communities = this.getCommunitiesForCollection(url, serviceDocument, hierarchy, existedCollectionObjects);
			String fullName = "";
			for (String community : communities) {
				fullName += community + fullNameSeparator; // add community + separator
			}
			fullName += collectionsMap.get(url); // add title
			collectionsMap.put(url, fullName);
		}
		return collectionsMap;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean createNewEntryWithFile(String collectionURL, File file) throws IOException {

		try {
			this.exportFile(collectionURL, file);
		} catch (SWORDClientException e) {
			log.error("Exception by creation of new item with file without metadata.", e);
			return false;
		}
		return true;
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
	 * and checking an access to the REST-interface.
	 * 
	 * @return {@code true} if service document and REST are accessible, and
	 *         {@code false} if not (e.g. by Error 403).
	 */
	@Override
	public boolean isRepositoryAccessible() {
		return (isRestAccessible() && super.isSwordAccessible(this.serviceDocumentURL));
	}

	
	/**
	 * {@inheritDoc}
	 * <p>
	 * In DSpace it will be checked via access to the service document
	 * (SWORD-protocol)
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
	 */
	@Override
	public Map<String, String> getAvailableCollections() {
		ServiceDocument serviceDocument = super.getServiceDocument(this.serviceDocumentURL);
		return super.getCollections(serviceDocument);
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean createNewEntryWithMetadata(String collectionURL, Map<String, List<String>> metadataMap) {
		
		try {
			this.exportMetadata(collectionURL, metadataMap);
		} catch (SWORDClientException e) {
			log.error("Exception by creation of new entry with metadata as Map.", e);
			return false;
		}
		return true;
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean createNewEntryWithMetadata(String collectionURL, File metadataFileXml) throws IOException {
		
		try {
			this.exportMetadata(collectionURL, metadataFileXml);
		} catch (SWORDClientException e) {
			log.error("Exception by creation of new entry with metadata as xml-file.", e);
			return false;
		}
		return true;
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean createNewEntryWithFileAndMetadata(String collectionURL, File file,
			Map<String, List<String>> metadataMap) throws IOException {

		try {
			this.exportMetadataAndFile(collectionURL, file, metadataMap);
		} catch (SWORDClientException e) {
			log.error("Exception by creation of new entry with file and metadata as Map.", e);
			return false;
		}
		return true;
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean createNewEntryWithFileAndMetadata(String collectionURL, File file, File metadataFileXml)
			throws IOException {

		try {
			this.exportMetadataAndFile(collectionURL, file, metadataFileXml);
		} catch (SWORDClientException e) {
			log.error("Exception by creation of new entry with file and metadata as xml-file.", e);
			return false;
		}
		return true;
	}

}

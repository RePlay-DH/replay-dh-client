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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.swordapp.client.AuthCredentials;
import org.swordapp.client.Deposit;
import org.swordapp.client.DepositReceipt;
import org.swordapp.client.EntryPart;
import org.swordapp.client.ProtocolViolationException;
import org.swordapp.client.SWORDClient;
import org.swordapp.client.SWORDClientException;
import org.swordapp.client.SWORDCollection;
import org.swordapp.client.SWORDError;
import org.swordapp.client.SWORDWorkspace;
import org.swordapp.client.ServiceDocument;
import org.swordapp.client.SwordResponse;
import org.swordapp.client.UriRegistry;

import bwfdm.replaydh.workflow.export.dspace.dto.v6.HierarchyObject;
import bwfdm.replaydh.workflow.export.dspace.dto.v6.CollectionObject;
import bwfdm.replaydh.workflow.export.dspace.WebUtils;
import bwfdm.replaydh.workflow.export.dspace.WebUtils.RequestType;


public class DSpace_v6 extends DSpaceRepository {

	protected static final Logger log = LoggerFactory.getLogger(DSpace_v6.class);
	
	// For SWORD
	protected String adminUser;
	protected char[] adminPassword;
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
		
	
	public DSpace_v6(String serviceDocumentURL, String restURL, String adminUser, char[] adminPassword) {
		
		this.adminUser = adminUser;
		this.adminPassword = adminPassword;
		setServiceDocumentURL(serviceDocumentURL);
		
		// HttpClient which ignores the ssl certificate
		this.client = WebUtils.createHttpClientWithSSLSupport();
		
		//TODO: original version, without ignoring of ssl certificate
		//this.client = HttpClientBuilder.create().build();
		
		setAllRestURLs(restURL);
	}
	
	
	/*
	 * -----------------------
	 * DSpace specific methods
	 * ----------------------- 
	 */
	
	
	public void setServiceDocumentURL(String serviceDocumentURL) {
		this.serviceDocumentURL = serviceDocumentURL;
	}
	
	
	public void setAllRestURLs(String restURL) {
		this.restURL = restURL;
		this.communitiesURL = this.restURL + "/communities";
		this.collectionsURL  =this.restURL + "/collections";
		this.hierarchyURL = this.restURL + "/hierarchy";
		this.restTestURL = this.restURL + "/test";
	}
	
	
	/**
	 * Check if REST-interface is accessible.
	 * @return
	 */
	public boolean isRestAccessible() {
		
		final CloseableHttpResponse response = WebUtils.getResponse(this.client, this.restTestURL, RequestType.GET, APPLICATION_JSON, APPLICATION_JSON);
		if((response != null) && (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)) {
			WebUtils.closeResponse(response);
			return true;
		} else {
			if(response != null) {
				WebUtils.closeResponse(response);
			}
			return false;
		}
	}
	
	
	/**
	 * Check if SWORDv2-protocol is accessible
	 * @return
	 */
	public boolean isSwordAccessible() {
		
		SWORDClient swordClient = new SWORDClient();
		AuthCredentials authCredentials = new AuthCredentials(this.adminUser, String.valueOf(this.adminPassword));
		if(DSpaceRepository.getServiceDocument(swordClient, this.serviceDocumentURL, authCredentials) != null) {
			return true;
		} else {
			return false;
		}
	}
	
	
	/**
	 * Get a list of communities for the collection
	 * Specific only for DSpace-6.
	 * <p>
	 * REST and SWORD requests are used.
	 * 
	 * @return a {@code List<String>} of communities (0 or more communities are possible) 
	 * 		   or {@code null} if a collection was not found
	 */
	public List<String> getCommunitiesForCollection(String collectionURL){
		
		SWORDClient swordClient = new SWORDClient();
		AuthCredentials authCredentials = new AuthCredentials(this.adminUser, String.valueOf(this.adminPassword));
		ServiceDocument serviceDocument = DSpaceRepository.getServiceDocument(swordClient, this.serviceDocumentURL, authCredentials);
		
		HierarchyObject hierarchy = getHierarchyObject();
		CollectionObject[] existedCollectionObjects = getAllCollectionObjects();
		
		return getCommunitiesForCollection(collectionURL, serviceDocument, hierarchy, existedCollectionObjects);
	}
	
	
	/**
	 * Private method with logic. Get a list of communities for the collection
	 * Specific only for DSpace-6.
	 * <p>
	 * REST and SWORD requests are used. ServiceDocument must be received already.
	 * 
	 * @return a {@code List<String>} of communities (0 or more communities are possible) 
	 * 		   or {@code null} if a collection was not found
	 */
	protected List<String> getCommunitiesForCollection(String collectionURL, ServiceDocument serviceDocument, HierarchyObject hierarchy, CollectionObject[] existedCollectionObjects){
		
		String collectionHandle = getCollectionHandle(collectionURL, serviceDocument, existedCollectionObjects);
		if(collectionHandle == null) {
			return null;
		}
		
		List<String> communityList = new ArrayList<String>(0);
		
		// Get List of communities or "null", if collection is not found
		communityList = hierarchy.getCommunityListForCollection(hierarchy, collectionHandle, communityList);
		
		if(communityList != null) {
			communityList.remove(0); 	//remove "Workspace" - it is not a community, 
							    	   	//but it is always on the first level of the hierarchy
		}
		return communityList; // List of communities ( >= 0) or "null"
	}
	
	
	/**
	 * Get a complete hierarchy of collections as HierarchyObject. REST is used. Works up DSpace-6.
	 * @return {@link HierarchyObject}
	 */
	protected HierarchyObject getHierarchyObject() {
		
		final CloseableHttpResponse response = WebUtils.getResponse(this.client, this.hierarchyURL, RequestType.GET, APPLICATION_JSON, APPLICATION_JSON);
		final HierarchyObject hierarchy = JsonUtils.jsonStringToObject(
					WebUtils.getResponseEntityAsString(response), HierarchyObject.class);
		WebUtils.closeResponse(response);
		return hierarchy;
	}
	
	
	/**
	 * Get a collection handle based on the collection URL.
	 * <p> 
	 * REST and SWORDv2 requests are used.
	 * 
	 * @param collectionURL
	 * @return String with a handle or {@code null} if collectionURL was not found 
	 */
	public String getCollectionHandle(String collectionURL) {
				
		// Get ServiceDocument
		SWORDClient swordClient = new SWORDClient();
		AuthCredentials authCredentials = new AuthCredentials(this.adminUser, String.valueOf(this.adminPassword));
		ServiceDocument serviceDocument = DSpaceRepository.getServiceDocument(swordClient, this.serviceDocumentURL, authCredentials);
		
		// Get all collections via REST to check, if swordCollectionPath contains a REST-handle
		CollectionObject[] existedCollectionObjects = getAllCollectionObjects();
		
		return getCollectionHandle(collectionURL, serviceDocument, existedCollectionObjects);	
	}
	
	
	/**
	 * Private method with logic. Get a collection handle based on the collection URL. 
	 * <p> 
	 * REST and SWORDv2 requests are used. ServiceDocument must be already retrieved.
	 * 
	 * @param collectionURL
	 * @param serviceDocument
	 * @return String with a handle or {@code null} if collectionURL was not found 
	 */
	protected String getCollectionHandle(String collectionURL, ServiceDocument serviceDocument, CollectionObject[] existedCollections) {
		
		String swordCollectionPath = ""; //collectionURL without a hostname and port
		
		for(SWORDWorkspace workspace : serviceDocument.getWorkspaces()) {
			for (SWORDCollection collection : workspace.getCollections()) {
				if(collection.getHref().toString().equals(collectionURL)) {
					swordCollectionPath = collection.getHref().getPath();
				}				
			}
		}	

		// Compare REST-handle and swordCollectionPath
		for(CollectionObject collection: existedCollections) {
			if(swordCollectionPath.contains(collection.handle)) {
				return collection.handle;
			}
		}		
		return null; //collectionURL was not found	
	}
	
	
	/**
	 * Get all existed collections as an array of CollectionObject. REST is used.
	 * @return
	 */
	protected CollectionObject[] getAllCollectionObjects() {
		
		final CloseableHttpResponse response = WebUtils.getResponse(this.client, this.collectionsURL, RequestType.GET, APPLICATION_JSON, APPLICATION_JSON);
		final CollectionObject[] collections = JsonUtils.jsonStringToObject(
					WebUtils.getResponseEntityAsString(response), CollectionObject[].class);
		WebUtils.closeResponse(response);
		return collections;
	}
	
	
	/**
	 * Publish a file or metadata. Private method.
	 * <p>
	 * IMPORTANT - you can use ONLY 1 possibility in the same time (only file, or only metadata). 
	 * "Multipart" is not supported!
	 * 
	 * @param userLogin
	 * @param collectionURL - could be link to the collection (from the service document) 
	 * 		  or a link to edit the collection ("Location" field in the response)
	 * @param mimeFormat - use e.g. {@code "application/atom+xml"} or {@code "application/zip"}
	 * @param packageFormat - see {@link UriRegistry.PACKAGE_SIMPLE_ZIP} or {@linkplain UriRegistry.PACKAGE_BINARY}
	 * @param file
	 * @param metadataMap
	 * @return "Location" parameter from the response in case of {@code SwordRequestType.DEPOSIT} request, 
	 *  	   "StatusCode" parameter from the response in case of {@code SwordRequestType.REPLACE} request,
	 *  	   or {@code null} in case of error
	 */
	protected String publishElement(String userLogin, String collectionURL, SwordRequestType swordRequestType, String mimeFormat, String packageFormat, File file, Map<String, String> metadataMap) {
		
		// Check if only 1 parameter is used (metadata OR file). 
		// Multipart is not supported.
		if( ((file != null)&&(metadataMap != null)) || ((file == null)&&(metadataMap == null)) ) {
			return null; 
		}
		
		SWORDClient swordClient = new SWORDClient();
		AuthCredentials authCredentials = getNewAuthCredentials(this.adminUser, this.adminPassword, userLogin);
		
		FileInputStream fis = null;
		
		Deposit deposit = new Deposit();
		
		try {
			// Check if "metadata as a Map"
			if(metadataMap != null) {
				EntryPart ep = new EntryPart();
				for(Map.Entry<String, String> metadataEntry : metadataMap.entrySet()) {
					ep.addDublinCore(metadataEntry.getKey(), metadataEntry.getValue());
				}
				deposit.setEntryPart(ep);
			}
			
			// Check if "file"
			if(file != null) {
				fis = new FileInputStream(file); // open FileInputStream
				deposit.setFile(fis);				
				deposit.setFilename(file.getName()); 	// deposit works properly ONLY with a "filename" parameter 
														// --> in curl: -H "Content-Disposition: filename=file.zip"
			}
			
			deposit.setMimeType(mimeFormat);
			deposit.setPackaging(packageFormat);
			deposit.setInProgress(true);
			//deposit.setMd5("fileMD5");					//put here only as example
			//deposit.setSuggestedIdentifier("abcdefg");	//put here only as example
			
			
			switch (swordRequestType) {
			case DEPOSIT:
				DepositReceipt receipt = swordClient.deposit(collectionURL, deposit, authCredentials);
				return receipt.getLocation(); //"Location" parameter from the response
			case REPLACE:
				SwordResponse response = swordClient.replace(collectionURL, deposit, authCredentials);
				return Integer.toString(response.getStatusCode()); //"StatusCode" parameter from the response
			default:
				log.error("Wrong SWORD-request type: " + swordRequestType + " : Supported here types are: " + SwordRequestType.DEPOSIT + ", " + SwordRequestType.REPLACE);
				return null;					
			}
			
		} catch (FileNotFoundException e) {
			log.error("Exception by accessing a file: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			return null;	
		
		} catch (SWORDClientException | SWORDError | ProtocolViolationException e) {
			log.error("Exception by making deposit: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			return null;
		} finally {
			// Close FileInputStream
			if(fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					log.error("Exception by closing the FileInputStream: " + e.getClass().getSimpleName() + ": " + e.getMessage());
				}
			}
		}
	}
	
	
	/* 
	 * ---------------------------------------
	 * PublicationRepository interface methods
	 * ---------------------------------------
	 */ 
		
	
	/**
	 * {@inheritDoc}
	 * <p> 
	 * In DSpace SWORD-v2 protocol will be used.
	 *  	
	 * @param userLogin
	 * @param collectionURL
	 * @param fileFullPath
	 * @return
	 */
	@Override
	public boolean publishFile(String userLogin, String collectionURL, File fileFullPath) {
			
		String mimeFormat = "application/zip"; // for every file type, to publish even "XML" files as a normal file
		String packageFormat = getPackageFormat(fileFullPath.getName()); //zip-archive or separate file
		
		if(publishElement(userLogin, collectionURL, SwordRequestType.DEPOSIT, mimeFormat, packageFormat, fileFullPath, null) != null) {
			return true;
		} else {
			return false;
		}
	}
		
	
	/**
	 * {@inheritDoc}
	 * Publish metadata as a Map.
	 * 
	 * @param userLogin
	 * @param collectionURL
	 * @param metadataMap
	 * @return
	 */
	@Override
	public boolean publishMetadata(String userLogin, String collectionURL, Map<String, String> metadataMap) {
		return publishMetadata(userLogin, collectionURL, metadataMap, SwordRequestType.DEPOSIT);	
	}
	
	
	/**
	 * Private method which can use different request types. See {@link SwordRequestType}.
	 * @param userLogin
	 * @param collectionURL
	 * @param metadataMap
	 * @param swordRequestType
	 * @return
	 */
	protected boolean publishMetadata(String userLogin, String collectionURL, Map<String, String> metadataMap, SwordRequestType swordRequestType) {
		
		String mimeFormat = "application/atom+xml";
		String packageFormat = UriRegistry.PACKAGE_BINARY;
		
		if(publishElement(userLogin, collectionURL, swordRequestType, mimeFormat, packageFormat, null, metadataMap) != null) {
			return true;
		} else {
			return false;
		}		
	}
	
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * Publish metadata as a XML-file in ATOM-format.
	 * 
	 * @param userLogin
	 * @param collectionURL
	 * @param metadataFileXML - file in XML-format (ATOM format of the metadata description) and with an XML-extension
	 * @return 
	 */
	@Override
	public boolean publishMetadata(String userLogin, String collectionURL, File metadataFileXML) {
		return publishMetadata(userLogin, collectionURL, metadataFileXML, SwordRequestType.DEPOSIT);	
	}
	
	
	/**
	 * Private method which can use different request types. See {@link SwordRequestType}.
	 * @param userLogin
	 * @param collectionURL
	 * @param metadataFileXML
	 * @param swordRequestType
	 * @return
	 */
	protected boolean publishMetadata(String userLogin, String collectionURL, File metadataFileXML, SwordRequestType swordRequestType) {
		
		// Check if file has an XML-extension
		if(!getFileExtension(metadataFileXML.getName()).toLowerCase().equals("xml")) {
			return false;
		}
		
		String mimeFormat = "application/atom+xml";
		String packageFormat = getPackageFormat(metadataFileXML.getName());
		
		if(publishElement(userLogin, collectionURL, swordRequestType, mimeFormat, packageFormat, metadataFileXML, null) != null) {
			return true;
		} else {
			return false;
		}		
	}
		
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * In DSpace SWORD-v2 protocol will be used.
	 */
	@Override
	public boolean publishFileAndMetadata(String userLogin, String collectionURL, File fileFullPath, Map<String, String> metadataMap) {
		
		String mimeFormat = "application/zip"; //as a common file (even for XML)
		String packageFormat = getPackageFormat(fileFullPath.getName());
		
		// Step 1: publish file (as file or archive), without metadata
		String editLink = publishElement(userLogin, collectionURL, SwordRequestType.DEPOSIT, mimeFormat, packageFormat, fileFullPath, null); //"POST" request (DEPOSIT)
		
		// Step 2: add metadata (as a Map structure)
		if (editLink != null) {
			return publishMetadata(userLogin, editLink, metadataMap, SwordRequestType.REPLACE); //"PUT" request (REPLACE) to overwrite some previous automatically generated metadata
		} else {
			return false;
		}
		
		//If replace order (step 1: metadata, step 2: file) --> Bad request, ERROR 400		
	}
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * In DSpace SWORD-v2 protocol will be used.
	 */
	@Override
	public boolean publishFileAndMetadata(String userLogin, String collectionURL, File fileFullPath, File metadataFileXML) {
		
		// Check if metadata file has an XML-extension
		if (!getFileExtension(metadataFileXML.getName()).toLowerCase().equals("xml")) {
			return false;
		}
		
		String mimeFormat = "application/zip"; //as a common file (even for XML)
		String packageFormat = getPackageFormat(fileFullPath.getName());
		
		// Step 1: publish file (as file or archive), without metadata
		String editLink = publishElement(userLogin, collectionURL, SwordRequestType.DEPOSIT, mimeFormat, packageFormat, fileFullPath, null); //"POST" request (DEPOSIT)
		
		// Step 2: add metadata (as XML-file)
		if (editLink != null) {
			return publishMetadata(userLogin, editLink, metadataFileXML, SwordRequestType.REPLACE); //"PUT" request (REPLACE) to overwrite some previous automatically generated metadata
		} else {
			return false;
		}
		
		//If replace order (step 1: metadata, step 2: file) --> Bad request, ERROR 400
	}
	
		
	/**
	 * {@inheritDoc}
	 * <p>
	 * For DSpace it is done by access to the Service Document via SWORD-protocol 
	 * and checking an access to the REST-interface.
	 * 
	 * @return {@code true} if service document and REST are accessible, and {@code false} if not (e.g. by Error 403).  
	 */
	@Override
	public boolean isAccessible() {
		
		if(isRestAccessible() && isSwordAccessible()) {
			return true;
		} else {
			return false;
		}		
	}
		
	
	/**
	 * {@inheritDoc}
	 * <p>
	 * In DSpace it will be checked via access to the service document (SWORD-protocol)
	 */
	@Override
	public boolean isUserRegistered(String userLogin) {	
		SWORDClient swordClient = new SWORDClient();
		AuthCredentials authCredentials = getNewAuthCredentials(this.adminUser, this.adminPassword, userLogin);
		
		if(DSpaceRepository.getServiceDocument(swordClient, this.serviceDocumentURL, authCredentials) != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * In DSpace it will be checked via access to the service document (SWORD-protocol)
	 */
	@Override
	public boolean isUserAssigned(String userLogin) {
		SWORDClient swordClient = new SWORDClient();
		AuthCredentials authCredentials = getNewAuthCredentials(this.adminUser, this.adminPassword, userLogin);
		ServiceDocument serviceDocument = DSpaceRepository.getServiceDocument(swordClient, this.serviceDocumentURL, authCredentials);

		int collectionCount = 0;
		if(serviceDocument != null) {
			for(SWORDWorkspace workspace : serviceDocument.getWorkspaces()) {
				collectionCount += workspace.getCollections().size(); //increment collection count
			}
		}		
		return ((collectionCount > 0) ? true : false);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, String> getUserAvailableCollectionsWithTitle(String userLogin) {		
		SWORDClient swordClient = new SWORDClient();
		AuthCredentials authCredentials = getNewAuthCredentials(this.adminUser, this.adminPassword, userLogin);	
		ServiceDocument serviceDocument = DSpaceRepository.getServiceDocument(swordClient, this.serviceDocumentURL, authCredentials);
		return DSpaceRepository.getAvailableCollectionsViaSWORD(serviceDocument);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, String> getAdminAvailableCollectionsWithTitle() {
		SWORDClient swordClient = new SWORDClient();
		AuthCredentials authCredentials = new AuthCredentials(this.adminUser, String.valueOf(this.adminPassword)); // login as "adminUser"		
		ServiceDocument serviceDocument = DSpaceRepository.getServiceDocument(swordClient, this.serviceDocumentURL, authCredentials);
		return DSpaceRepository.getAvailableCollectionsViaSWORD(serviceDocument);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setCredentials(String user, char[] password) {
		this.adminUser = user;
		this.adminPassword = password;
	}


	/**
	 * {@inheritDoc}	  
	 */
	@Override
	public Map<String, String> getUserAvailableCollectionsWithFullName(String userLogin, String fullNameSeparator) {
		
		// Get all available for the user collections from the ServiceDocument (via SWORD)
		SWORDClient swordClient = new SWORDClient();
		AuthCredentials authCredentials = getNewAuthCredentials(this.adminUser, this.adminPassword, userLogin);		
		ServiceDocument serviceDocument = DSpaceRepository.getServiceDocument(swordClient, this.serviceDocumentURL, authCredentials);
		Map<String, String> collectionsMap = DSpaceRepository.getAvailableCollectionsViaSWORD(serviceDocument); //all available collections
		
		// Get complete hierarchy of collections. Is needed later to get communities of the collection.
		final HierarchyObject hierarchy = getHierarchyObject();
		
		// Get all existed collections via REST as an array of CollectionObject. Is needed later to get communities of the collection. 
		final CollectionObject[] existedCollectionObjects = getAllCollectionObjects();
		
		// Extend the collection name with communities and separators
		for(String url: collectionsMap.keySet()) {
			List<String> communities = this.getCommunitiesForCollection(url, serviceDocument, hierarchy, existedCollectionObjects);
			String fullName = "";
			for(String community: communities) {
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
	public Map<String, String> getAdminAvailableCollectionsWithFullName(String fullNameSeparator) {
		return this.getUserAvailableCollectionsWithFullName(this.adminUser, fullNameSeparator);
	}
	
}

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
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
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
import org.swordapp.client.UriRegistry;

import bwfdm.replaydh.workflow.export.dspace.dto.v6.HierarchyObject;
import bwfdm.replaydh.workflow.export.dspace.dto.v6.CollectionObject;


public class DSpace_v6 implements PublicationRepository{

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
	// Header constants
	protected final String APPLICATION_JSON = "application/json";
	protected final String CONTENT_TYPE_HEADER = "Content-Type";
	protected final String ACCEPT_HEADER = "Accept";
	
	CloseableHttpClient client;
		
	
	public DSpace_v6(String serviceDocumentURL, String restURL, String adminUser, char[] adminPassword) {
		
		this.adminUser = adminUser;
		this.adminPassword = adminPassword;
		setServiceDocumentURL(serviceDocumentURL);
		
		// HttpClient which ignores the ssl certificate
		this.client = createHttpClientWithSSLSupport();
		
		//TODO: original version, without ignoring of ssl certificate
		//this.client = HttpClientBuilder.create().build();
		
		setAllRestURLs(restURL);
	}
	
	
	//=== Private methods ===

	
	/** 
	 * Create a ClosableHttpClient with ignoring of SSL certificates
	 * @return
	 */
	private CloseableHttpClient createHttpClientWithSSLSupport() {
		
		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
			
			SSLContextBuilder builder = new SSLContextBuilder();
			builder.loadTrustMaterial(trustStore, new TrustSelfSignedStrategy() {
				@Override
			    public boolean isTrusted(X509Certificate[] chain, String authType)
			            throws CertificateException {
			        return true;
			    }
			});
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());	
			return HttpClients.custom().setSSLSocketFactory(
		            sslsf).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).build();
			
			//return HttpClients.custom().setSSLSocketFactory(sslsf).build();
			
		} catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
			log.error("Exception by creation of http-clinet with ssl support: " + e.getClass() + ": " + e.getMessage());
			e.printStackTrace();
			return null;
		}		
	}
	
	
	/**
	 * Get new authentication credentials. 
	 * <p> To disactivate "on-behalf-of" option please use the same string for "adminUser" and "userLogin".
	 * <p> If "adminUser" and "userLogin" are different, "on-behalf-of" option will be used.
	 * 
	 * @param adminUser
	 * @param adminPassword
	 * @param userLogin
	 * @return
	 */
	private AuthCredentials getNewAuthCredentials(String adminUser, char[] adminPassword, String userLogin) {
		
		if(adminUser.equals(userLogin)) {
			return new AuthCredentials(userLogin, String.valueOf(adminPassword)); // without "on-behalf-of"
		} else {
			return new AuthCredentials(adminUser, String.valueOf(adminPassword), userLogin); // with "on-behalf-of"
		}
	}
	
	
	/**
	 * Get service document via SWORD v2
	 * 
	 * @param swordClient
	 * @param serviceDocumentURL
	 * @param authCredentials
	 * @return ServiceDocument or null in case of error/exception
	 */
	private ServiceDocument getServiceDocument(SWORDClient swordClient, String serviceDocumentURL, AuthCredentials authCredentials) {
		ServiceDocument serviceDocument = null;
		try {
			serviceDocument = swordClient.getServiceDocument(this.serviceDocumentURL, authCredentials);
		} catch (SWORDClientException | ProtocolViolationException e) {
			log.error("Exception by accessing service document: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			return null;
		}
		return serviceDocument;
	}
	
	
	/**
	 * Get available collections via SWORD v2
	 * 
	 * @return Map<String, String> where key=URL, value=Title
	 */
	private Map<String, String> getAvailableCollectionsViaSWORD(AuthCredentials authCredentials){
		Map<String, String> collections = new HashMap<String, String>();
		SWORDClient swordClient = new SWORDClient();
		ServiceDocument serviceDocument = this.getServiceDocument(swordClient, serviceDocumentURL, authCredentials);
		
		if(serviceDocument != null) {
			for(SWORDWorkspace workspace : serviceDocument.getWorkspaces()) {
				for (SWORDCollection collection : workspace.getCollections()) {
					// key = full URL, value = Title
					collections.put(collection.getHref().toString(), collection.getTitle());
				}
			}
		}
		return collections;
	}
	
	
	private String getFileExtension(String fileName) {
		
		String extension = "";
		int i = fileName.lastIndexOf('.');
		if(i>0) {
			extension = fileName.substring(i+1);
		}
		return extension;		
	}
	
	
	private String getPackageFormat(String fileName) {
		String extension = this.getFileExtension(fileName);
		
		if(extension.toLowerCase().equals("zip")) {
			return UriRegistry.PACKAGE_SIMPLE_ZIP;
		}
		return UriRegistry.PACKAGE_BINARY;
	}
	
	
	/**
	 * Get a response to the REST-request
	 * @param url
	 * @param contentType
	 * @param acceptType
	 * @return
	 */
	private CloseableHttpResponse getResponse(String url, String contentType, String acceptType) {
		try {
			HttpGet request = new HttpGet(url);
			request.addHeader("Content-Type", contentType);
			request.addHeader("Accept", acceptType);
			CloseableHttpResponse response = client.execute(request);
			return response;
			
		} catch (IOException e) {
			log.error("Exception by http request: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Get a response entity as a String
	 * 
	 * @param response
	 * @return
	 */
	private String getResponseEntityAsString(CloseableHttpResponse response) {
		try {
			return EntityUtils.toString(response.getEntity(),"UTF-8");
		} catch (ParseException | IOException e) {
			log.error("Exception by converting response entity to String: " 
						+ e.getClass().getSimpleName() + ": " + e.getMessage());
			return null;			
		} 
	}
	
	private void closeResponse(CloseableHttpResponse response) {
		try {
			response.close();
		} catch (IOException e) {
			log.error("Exception by response closing: "	+ e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
	
		
	//=== DSpace specific public methods ===
	
	
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
		
		final CloseableHttpResponse response = getResponse(this.restTestURL, APPLICATION_JSON, APPLICATION_JSON);
		if((response != null) && (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)) {
			closeResponse(response);
			return true;
		} else {
			if(response != null) {
				closeResponse(response);
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
		AuthCredentials authCredentials = new AuthCredentials(adminUser, String.valueOf(adminPassword));
		
		if(this.getServiceDocument(swordClient, this.serviceDocumentURL, authCredentials) != null) {
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
		
		String collectionHandle = getCollectionHandle(collectionURL);
		if(collectionHandle == null) {
			return null;
		}
		
		List<String> communityList = new ArrayList<String>(0);
		
		final CloseableHttpResponse response = getResponse(this.hierarchyURL, APPLICATION_JSON, APPLICATION_JSON);
		final HierarchyObject hierarchy = JsonUtils.jsonStringToObject(
					getResponseEntityAsString(response), HierarchyObject.class);
		closeResponse(response);
		
		// Get List of communities or "null", if collection is not found
		communityList = hierarchy.getCommunityListForCollection(hierarchy, collectionHandle, communityList);
		
		if(communityList != null) {
			communityList.remove(0); 	//remove "Workspace" - it is not a community, 
							    	   	//but it is always on the first level of the hierarchy
		}
		return communityList; // List of communities ( >= 0) or "null"
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
		
		String swordCollectionPath = ""; //collectionURL without a hostname and port
		
		// Find a collectionURL inside of all avaialble collections. SWORD is used.
		SWORDClient swordClient = new SWORDClient();
		AuthCredentials authCredentials = new AuthCredentials(adminUser, String.valueOf(adminPassword));
		ServiceDocument serviceDocument = this.getServiceDocument(swordClient, serviceDocumentURL, authCredentials);
		
		for(SWORDWorkspace workspace : serviceDocument.getWorkspaces()) {
			for (SWORDCollection collection : workspace.getCollections()) {
				if(collection.getHref().toString().equals(collectionURL)) {
					swordCollectionPath = collection.getHref().getPath();
				}				
			}
		}	
		
		// Get all collections via REST to check, if swordCollectionPath contains a REST-handle
		final CloseableHttpResponse response = getResponse(this.collectionsURL, APPLICATION_JSON, APPLICATION_JSON);
		final CollectionObject[] collections = JsonUtils.jsonStringToObject(
					getResponseEntityAsString(response), CollectionObject[].class);
		closeResponse(response);
		
		// Compare REST-handle and swordCollectionPath
		for(CollectionObject collection: collections) {
			if(swordCollectionPath.contains(collection.handle)) {
				return collection.handle;
			}
		}		
		return null; //collectionURL was not found	
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
	 * @return "Location" parameter from the response, or {@code null} in case of error
	 */
	private String publishElement(String userLogin, String collectionURL, String mimeFormat, String packageFormat, File file, Map<String, String> metadataMap) {
		
		// Check if only 1 parameter is used (metadata OR file). 
		// Multipart is not supported.
		if( ((file != null)&&(metadataMap != null)) || ((file == null)&&(metadataMap == null)) ) {
			return null; 
		}
		
		//Check if userLogin is registered
		if(!isUserRegistered(userLogin)) {
			return null;
		}
		
		SWORDClient swordClient = new SWORDClient();
		AuthCredentials authCredentials = getNewAuthCredentials(adminUser, adminPassword, userLogin);
		
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
			//deposit.setMd5("fileMD5");
			//deposit.setSuggestedIdentifier("abcdefg");
			
			DepositReceipt receipt = swordClient.deposit(collectionURL, deposit, authCredentials);
			
			return receipt.getLocation(); // "Location" parameter from the response
			
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
	
	
	//=== PublicationRepository interface methods ===
	
	
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
		
		if(publishElement(userLogin, collectionURL, mimeFormat, packageFormat, fileFullPath, null) != null) {
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
		
		String mimeFormat = "application/atom+xml";
		String packageFormat = UriRegistry.PACKAGE_BINARY;
		
		if(publishElement(userLogin, collectionURL, mimeFormat, packageFormat, null, metadataMap) != null) {
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
		
		// Check if file has an XML-extension
		if(!getFileExtension(metadataFileXML.getName()).toLowerCase().equals("xml")) {
			return false;
		}
		
		String mimeFormat = "application/atom+xml";
		String packageFormat = getPackageFormat(metadataFileXML.getName());
		
		if(publishElement(userLogin, collectionURL, mimeFormat, packageFormat, metadataFileXML, null) != null) {
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
		String editLink = publishElement(userLogin, collectionURL, mimeFormat, packageFormat, fileFullPath, null);
		
		// Step 2: add metadata (as a Map structure)
		if (editLink != null) {
			return publishMetadata(userLogin, editLink, metadataMap);
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
		String editLink = publishElement(userLogin, collectionURL, mimeFormat, packageFormat, fileFullPath, null); 
		
		// Step 2: add metadata (as XML-file)
		if (editLink != null) {
			return publishMetadata(userLogin, editLink, metadataFileXML); 
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
		AuthCredentials authCredentials = getNewAuthCredentials(adminUser, adminPassword, userLogin);
		
		if(this.getServiceDocument(swordClient, serviceDocumentURL, authCredentials) != null) {
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
		AuthCredentials authCredentials = getNewAuthCredentials(adminUser, adminPassword, userLogin);
		ServiceDocument serviceDocument = this.getServiceDocument(swordClient, serviceDocumentURL, authCredentials);

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
		
		AuthCredentials authCredentials = getNewAuthCredentials(adminUser, adminPassword, userLogin);	
		return this.getAvailableCollectionsViaSWORD(authCredentials);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, String> getAdminAvailableCollectionsWithTitle() {
		AuthCredentials authCredentials = new AuthCredentials(adminUser, String.valueOf(adminPassword)); // login as "adminUser"		
		return this.getAvailableCollectionsViaSWORD(authCredentials);
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
		AuthCredentials authCredentials = getNewAuthCredentials(adminUser, adminPassword, userLogin);		
		Map<String, String> collectionsMap = this.getAvailableCollectionsViaSWORD(authCredentials);
		
		for(String url: collectionsMap.keySet()) {
			List<String> communities = this.getCommunitiesForCollection(url);
			String fullName = "";
			for(String community: communities) {
				fullName += community + fullNameSeparator; // add community + separator
			}
			fullName += collectionsMap.get(url); // add title
			collectionsMap.put(url, fullName);
		}		
		return collectionsMap;
	}


	@Override
	public Map<String, String> getAdminAvailableCollectionsWithFullName(String fullNameSeparator) {
		
		return this.getUserAvailableCollectionsWithFullName(this.adminUser, fullNameSeparator);
	}
	
}

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
package bwfdm.replaydh.workflow.export.generic;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import bwfdm.replaydh.io.IOUtils;

/**
 * General exporting methods for SWORD-based repositories (e.g. DSpace, Dataverse).
 *
 * @author Markus Gärtner
 * @author Volodymyr Kushnarenko
 * @author Florian Fritze
 *
 */
public abstract class SwordExporter {

	protected static final Logger log = LoggerFactory.getLogger(SwordExporter.class);

	public static final String APPLICATION_JSON = "application/json";
	public static final String CONTENT_TYPE_HEADER = "Content-Type";
	public static final String ACCEPT_HEADER = "Accept";
	public static final String MIME_FORMAT_ZIP = "application/zip";
	public static final String MIME_FORMAT_ATOM_XML = "application/atom+xml";

	public static enum SwordRequestType {
		DEPOSIT("DEPOSIT"), //"POST" request
		REPLACE("REPLACE"), //"PUT" request
		DELETE("DELETE")	//reserved for the future
		;

		private final String label;

		private SwordRequestType(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}

		@Override
		public String toString() {
			return label;
		}
	}


	/**
	 * Get a file extension (without a dot) from the file name
	 * (e.g. "txt", "zip", * ...)
	 *
	 * @param fileName {@link String} with the file name
	 * @return {@link String}
	 */
	public static String getFileExtension(String fileName) {
		String extension = "";
		int i = fileName.lastIndexOf('.');
		if(i>0) {
			extension = fileName.substring(i+1);
		}
		return extension;
	}


	/**
	 * Get package format basing on the file name.
	 * E.g. {@code UriRegistry.PACKAGE_SIMPLE_ZIP} or {@code UriRegistry.PACKAGE_BINARY}
	 *
	 * @param fileName {@link String} with the file name (not a full path)
	 * @return {@link String} with the package format
	 */
	public static String getPackageFormat(String fileName) {
		String extension = getFileExtension(fileName);

		if(extension.toLowerCase().equals("zip")) {
			return UriRegistry.PACKAGE_SIMPLE_ZIP;
		}
		return UriRegistry.PACKAGE_BINARY;
	}
	
	
	/**
	 * Get package format basing on the file name and "unpackZip" flag.
	 * E.g. {@code UriRegistry.PACKAGE_SIMPLE_ZIP} or {@code UriRegistry.PACKAGE_BINARY}
	 *
	 * @param fileName {@link String} with the file name (not a full path)
	 * @param unpackZip a flag, if the package should be unpacked ({@code true}) 
	 * 			in the repository or not ({@code false}) 
	 * @return {@link String} with the package format
	 */
	public static String getPackageFormat(String fileName, boolean unpackZip) {
		
		String packageFormat = getPackageFormat(fileName);
		if (packageFormat.equals(UriRegistry.PACKAGE_SIMPLE_ZIP) && !unpackZip) {
			return UriRegistry.PACKAGE_BINARY;
		}
		return packageFormat; 
	}
	
	
	/**
	 * Create new authentication credentials based on the user login and password.
	 * 
	 * @param userLogin login name of the user, usually is an E-mail address
	 * @param userPassword password for the user login
	 * 
	 * @return {@link AuthCredentials} object
	 */
	public static AuthCredentials createAuthCredentials(String userLogin, char[] userPassword) {

		requireNonNull(userLogin);
		requireNonNull(userPassword);
		return new AuthCredentials(userLogin, String.valueOf(userPassword)); // without "on-behalf-of" option
	}
	
	
	/**
	 * Create new authentication credentials with activated "on-behalf-of" option, what allows to make an export
	 * for some user (onBehalfOfUser) based only on its login name. For that case credentials of some privileged user
	 * are needed (login name, password), who could play e.g. an administrator role or just could be only allowed 
	 * to make an export into the repository and whose credentials are known. The privileged user in this case 
	 * will make an export on behalf of other user.
	 * <p>
	 * This type of authentication credentials could be used, if only administrator credentials are available 
	 * (adminUser, adminPassword) and from credentials of the current export's owner only login name is known 
	 * (onBehalfOfUser) and the password must not be used.      
	 * <p>
	 * <b>IMPORTNANT:</b> if "adminUser" and "onBehalfOfUser" are <b>identical</b>, authentication credentials 
	 * will be created <b>without the "on-behalf-of" option</b>.
	 * 
	 * @param adminUser login name of the privileged user, usually is an E-mail address
	 * @param adminPassword password of the privileged user
	 * @param onBehalfOfUser login name of the current owner of the export, usually is an E-mail address   
	 * 
	 * @return {@link AuthCredentials} object
	 */
	public static AuthCredentials createAuthCredentials(String adminUser, char[] adminPassword, String onBehalfOfUser) {

		requireNonNull(adminUser);
		requireNonNull(adminPassword);
		requireNonNull(onBehalfOfUser);
		
		if (adminUser.equals(onBehalfOfUser)) {
			return createAuthCredentials(onBehalfOfUser, adminPassword); // without "on-behalf-of" 
		} else {
			return new AuthCredentials(adminUser, String.valueOf(adminPassword), onBehalfOfUser); // with "on-behalf-of"
		}
	}
	
	
	/**
	 * Create new authentication credentials based on the API token. Could be used for the Dataverse repositories.
	 * 
	 * @param apiToken - API token, which could be usually found in the export repository GUI in the account settings. 
	 * 		Password in this case is not needed.
	 * 
	 * @return {@link AuthCredentials} object
	 */
	public static AuthCredentials createAuthCredentials(char[] apiToken) {

		requireNonNull(apiToken);
		return new AuthCredentials(String.valueOf(apiToken), ""); // use an empty string instead of password
	}


	private final AuthCredentials authCredentials;
	private final SWORDClient swordClient;


	/**
	 * Constructor, creates private final {@link SWORDClient} object and sets the authentication credentials (as private final object).
	 * To change the authentication credentials, please always create a new object.
	 *
	 * @param authCredentials {@link AuthCredentials} object. To create it please use the following methods: ...
	 *
	 * TODO: add 2 methods to create an AuthCredentials object (for user/password and for API-token) and make link above
	 *
	 */
	protected SwordExporter(AuthCredentials authCredentials) {
		swordClient = new SWORDClient();
		this.authCredentials = requireNonNull(authCredentials);
	}


	/**
	 * Get available collections via SWORD v2 protocol based on the {@link ServiceDocument}.
	 *
	 * @param serviceDocument can be created via {@link #getServiceDocument(String) getServiceDocument(serviceDocumentURL)}
	 * @return Map<String, String> where key = collection URL, value = collection title
	 */
	public Map<String, String> getCollections(ServiceDocument serviceDocument){
		requireNonNull(serviceDocument);
		Map<String, String> collections = new HashMap<String, String>();

		for (SWORDWorkspace workspace : serviceDocument.getWorkspaces()) {
			for (SWORDCollection collection : workspace.getCollections()) {
				// key = full URL, value = Title
				collections.put(collection.getHref().toString(), collection.getTitle());
			}
		}
		return collections;
	}

	
	/**
	 * Get available entries of the provided collection based on the the current authentication credentials.
	 * E.g. for DSpace repository it means - items inside the collection. 
	 * For Dataverse repository - datasets inside the dataverse.
	 * <p>
	 * IMPORTANT: authentication credentials are used implicitly. Definition of the credentials is realized via the class constructor.
	 * 
	 * @param collectionUrl a collection URL, must have a "/swordv2/collection/" substring inside
	 * @return {@link Map} of entries, where key = entry URL (with "/swordv2/edit/" substring inside), 
	 * 					value = entry title. If there are not available entries, the map will be also empty.
	 */
	public abstract Map<String, String> getCollectionEntries(String collectionUrl);


	public AuthCredentials getAuthCredentials() {
		return authCredentials;
	}


	protected SWORDClient getSwordClient() {
		return swordClient;
	}


	/**
	 * Request a service document based on the URL.
	 * <p>
	 * IMPORTANT: credentials are used implicitly. Definition of the credentials is realized via the class constructor.
	 *
	 * @param serviceDocumentURL string with the service document URL
	 * @return {@link ServiceDocument} object or {@code null} if service document is not accessible via provided URL 
	 * 			and implicitly used authentication credentials or in case of error.
	 */
	public ServiceDocument getServiceDocument(String serviceDocumentURL) {
		ServiceDocument serviceDocument = null;
		try {
			serviceDocument = swordClient.getServiceDocument(serviceDocumentURL, authCredentials);
		} catch (SWORDClientException | ProtocolViolationException e) {
			log.error("Exception by accessing service document", e);
			return null;
		}
		return serviceDocument;
	}

	/**
	 * Check if SWORDv2-protocol is accessible
	 * @param serviceDocumentURL string with the service document URL
	 * @return {@code true} if SWORD API is accessible and {@code false} otherwise
	 */
	public boolean isSwordAccessible(String serviceDocumentURL) {
		return getServiceDocument(serviceDocumentURL) != null;
	}

	/**
	 * Export an element via SWORD - any file (also including metadata as a xml-file) or metadata as a {@link Map}.
	 * Private internal method, should be used ONLY for the internal implementation.
	 * <p>
	 * IMPORTANT: is possible to export ONLY 1 option in the same time (only file, or only a Map of metadata).
	 * "Multipart" is not supported!
	 * <p>
	 * IMPORTANT: authentication credentials are used implicitly. Definition of the credentials is realized via the class constructor.
	 *
	 * @param exportURL could be link to the collection (from the service document)
	 * 		  or a link to edit the collection ("Location" field in the response)
	 * @param swordRequestType see {@link SwordRequestType}
	 * @param mimeFormat String with e.g. {@code "application/atom+xml"} or {@code "application/zip"}, see {@link SwordRequestType}}
	 * @param packageFormat {@code String} with the package format, see {@link UriRegistry.PACKAGE_SIMPLE_ZIP} or {@linkplain UriRegistry.PACKAGE_BINARY}
	 * @param file {@link File} for export
	 * @param metadataMap {@link Map} of metadata for export
	 *
	 * @return {@link SwordResponse} object or {@code null} in case of error.
	 * 		   <pre>
	 * 		   If request type is {@code SwordRequestType.DEPOSIT}, please cast the returned object to {@code DepositReceipt},
	 * 		   you can check it via e.g. {@code instanceof} operator.
	 *  	   If request type is {@code SwordRequestType.REPLACE}, the casting is not needed.
	 *  	
	 *  	   <b>IMPORTANT:</b> by request type {@code SwordRequestType.REPLACE} there is no warranty,
	 *  	   that all fields of the {@link SwordResponse} object are initialized, so for the current moment
	 *  	   only status code field is available, all other fields are {@code null} (e.g. "Location" field).
	 *  	   In case of {@code SwordRequestType.DEPOSIT} request type such problems were not found.
	 *  	   </pre>
	 * @throws ProtocolViolationException
	 * @throws SWORDError
	 * @throws SWORDClientException
	 * @throws FileNotFoundException
	 *
	 */
	protected SwordResponse exportElement(String exportURL, SwordRequestType swordRequestType,
			String mimeFormat, String packageFormat, File file, Map<String, List<String>> metadataMap)
					throws SWORDClientException, SWORDError, ProtocolViolationException, FileNotFoundException {

		requireNonNull(exportURL);
		requireNonNull(swordRequestType);

		// Check if only 1 parameter is used (metadata OR file).
		// Multipart is not supported.
		if( ((file != null)&&(metadataMap != null)) || ((file == null)&&(metadataMap == null)) ) {
			return null;
		}

		FileInputStream fis = null;

		Deposit deposit = new Deposit();

		// Check if "metadata as a Map"
		if(metadataMap != null) {
			EntryPart ep = new EntryPart();
			for(Map.Entry<String, List<String>> metadataEntry : metadataMap.entrySet()) {
				for (String property: metadataEntry.getValue()) {
					ep.addDublinCore(metadataEntry.getKey(), property);
				}
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

		try {
			switch (swordRequestType) {
			case DEPOSIT:
				DepositReceipt receipt = swordClient.deposit(exportURL, deposit, authCredentials);
				return receipt; // returns Deposit Receipt instance;
			case REPLACE:
				SwordResponse response = swordClient.replace(exportURL, deposit, authCredentials);
				return response; //returns the Sword response
			default:
				log.error("Wrong SWORD-request type: {} : Supported here types are: {}, {}",
						swordRequestType, SwordRequestType.DEPOSIT, SwordRequestType.REPLACE);
				throw new IllegalArgumentException("Wrong SWORD-request type: "+swordRequestType);
			}
		} finally {
			if(fis!=null) {
				IOUtils.closeQuietly(fis);
			}
		}

	}

	
	/**
	 * Export the metadata only (without any file) to some collection.
	 * Metadata are described as a {@link java.util.Map}.
	 * <p>
	 * IMPORTANT: authentication credentials are used implicitly. Definition of the credentials is realized via the class constructor.
	 *
	 * @param collectionURL holds the collection URL where the metadata will be exported to
	 * @param metadataMap holds the metadata itself
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
	 * @throws SWORDClientException
	 */
	public abstract String createEntryWithMetadata(String collectionURL, Map<String, List<String>> metadataMap) throws SWORDClientException;

	
	/**
	 * Export a file together with the metadata to some collection.
	 * Metadata are described as a {@link java.util.Map}.
	 * <p>
	 * IMPORTANT: authentication credentials are used implicitly. Definition of the credentials is realized via the class constructor.
	 *
	 * @param collectionURL holds the collection URL where items will be exported to
	 * @param unpackZip decides whether to unpack the zipfile or places the packed zip file as uploaded data
	 * @param file holds a file which can contain one or multiple files
	 * @param metadataMap holds the metadata which is necessary for the ingest
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
	 * @throws SWORDClientException
	 * @throws IOException
	 */
	public abstract String createEntryWithMetadataAndFile(String collectionURL, File file, boolean unpackZip, Map<String, List<String>> metadataMap) throws IOException, SWORDClientException;


	/**
	 * Export a file to some URL (e.g. URL of some collection or metadata set).
	 * <p>
	 * IMPORTANT: authentication credentials are used implicitly. Definition of the credentials is realized via the class constructor.
	 *
	 * @param url The URL where to export the zipFile to.
	 * @param file A file that should be exported.
	 *
	 * TODO: uncomment later. Think about - return location link as String (with "edit" substring inside)
	 *
	 * @throws IOException
	 * @throws SWORDClientException
	 */
	//public abstract void exportFile(String url, File file) throws IOException, SWORDClientException;
	
	
	/**
	 * Replaces an existing metadata entry with new metadata in the repository
	 * 
	 * @param entryUrl The URL which points to the metadata entry, includes "/swordv2/edit/" substring inside.
	 * @param metadataMap The metadata that will replace the old metadata.
	 * 
	 * @throws SWORDClientException 
	 */
	public void replaceMetadataEntry(String entryUrl, Map<String, List<String>> metadataMap) throws SWORDClientException {
		requireNonNull(entryUrl);
		requireNonNull(metadataMap);
		
		try {
			exportElement(entryUrl, SwordRequestType.REPLACE, MIME_FORMAT_ATOM_XML, null, null, metadataMap);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SWORDError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ProtocolViolationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
	}
}

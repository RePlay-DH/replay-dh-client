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
	 * @param fileName {@link String} with the file name
	 * @return {@link String}
	 */
	public static String getPackageFormat(String fileName) {
		String extension = getFileExtension(fileName);

		if(extension.toLowerCase().equals("zip")) {
			return UriRegistry.PACKAGE_SIMPLE_ZIP;
		}
		return UriRegistry.PACKAGE_BINARY;
	}


	/**
	 * Get available collections via SWORD v2 protocol based on the {@link ServiceDocument}.
	 * 
	 * @param serviceDocument can be created via {@link #getServiceDocument(String) getServiceDocument(serviceDocumentURL)}
	 * @return Map<String, String> where key=URL, value=Title
	 */
	public Map<String, String> getAvailableCollectionsViaSWORD(ServiceDocument serviceDocument){
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
	 * @return {@link ServiceDocument} object
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
	 * @param collectionURL could be link to the collection (from the service document)
	 * 		  or a link to edit the collection ("Location" field in the response)
	 * @param swordRequestType see {@link SwordRequestType}
	 * @param mimeFormat String with e.g. {@code "application/atom+xml"} or {@code "application/zip"}, see {@link SwordRequestType}}
	 * @param packageFormat {@code String} with the package format, see {@link UriRegistry.PACKAGE_SIMPLE_ZIP} or {@linkplain UriRegistry.PACKAGE_BINARY}
	 * @param file {@link File} for export
	 * @param metadataMap {@link Map} of metadata for export
	 *  
	 * @return <pre>{@link SwordResponse} object or {@code null} in case of error. 
	 * 		   If request type is {@code SwordRequestType.DEPOSIT}, please cast the returned object to {@code DepositReceipt},
	 * 		   you can check it via e.g. {@code instanceof} operator.
	 *  	   If request type is {@code SwordRequestType.REPLACE}, the casting is not needed.
	 *  	   </pre>  
	 * @throws ProtocolViolationException 
	 * @throws SWORDError 
	 * @throws SWORDClientException 
	 * @throws FileNotFoundException 
	 *  	   
	 */
	protected SwordResponse exportElement(String collectionURL, SwordRequestType swordRequestType, String mimeFormat, String packageFormat, File file, Map<String, List<String>> metadataMap) throws SWORDClientException, SWORDError, ProtocolViolationException, FileNotFoundException {

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

			switch (swordRequestType) {
			case DEPOSIT:
				DepositReceipt receipt = swordClient.deposit(collectionURL, deposit, authCredentials);
				return receipt; // returns Deposit Receipt instance;
			case REPLACE:
				SwordResponse response = swordClient.replace(collectionURL, deposit, authCredentials);
				return response; //returns the Sword response
			default:
				log.error("Wrong SWORD-request type: {} : Supported here types are: {}, {}",
						swordRequestType, SwordRequestType.DEPOSIT, SwordRequestType.REPLACE);
				throw new IllegalArgumentException();
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
	 * @return String 
	 */
	public abstract void exportMetadata(String collectionURL, Map<String, List<String>> metadataMap) throws SWORDClientException;
	
	//TODO: add exportMetadata based on the XML-file in future releases
	//public abstract String exportMetadata(String collectionURL, File metadataFileXML);
	
	
	/**
	 * Export a file together with the metadata to some collection.
	 * Metadata are described as a {@link java.util.Map}. 
	 * <p>
	 * IMPORTANT: authentication credentials are used implicitly. Definition of the credentials is realized via the class constructor.
	 * 
	 * @param collectionURL holds the collection URL where items will be exported to
	 * @param file holds a file which can contain one or multiple files
	 * @param metadataMap holds the metadata which is necessary for the ingest
	 * @return {@code true} if publication was successful and {@code false} otherwise (e.g. some error has occurred)
	 * 
	 * TODO: remove "throws", catch exceptions inside the method
	 * 
	 * @throws SWORDClientException 
	 * @throws IOException 
	 */ 
	public abstract void exportMetadataAndFile(String collectionURL, File file, Map<String, List<String>> metadataMap) throws IOException, SWORDClientException;
	
	//TODO: add exportMetadataAndFile with the metadata as a XML-file in future releases 
	//public abstract boolean exportFileAndMetadata(String collectionURL, File file, File metadataFileXML);

	
	/**
	 * Export a file to some URL (e.g. URL of some collection or metadata set).
	 * <p>
	 * IMPORTANT: authentication credentials are used implicitly. Definition of the credentials is realized via the class constructor.
	 * 
	 * @param url The URL where to export the zipFile to.
	 * @param file A file that should be exported.
	 * @return {@code true} if publication was successful and {@code false} otherwise (e.g. some error has occurred)
	 * 
	 * TODO: remove "throws", catch exceptions inside the method
	 * 
	 * @throws IOException
	 */
	public abstract void exportFile(String url, File file) throws IOException, SWORDClientException;
}

package bwfdm.replaydh.workflow.export.generic.refactor;

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
 * 
 * @author Markus Gärtner, Volodymyr Kushnarenko, Florian Fritze
 * 
 * TODO: rename to "SwordExporter"
 *
 */
public abstract class SwordRepositoryExporter {

	private static final Logger log = LoggerFactory.getLogger(SwordRepositoryExporter.class);

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
	 * Get available collections via SWORD v2
	 *
	 * @return Map<String, String> where key=URL, value=Title
	 */
	public static Map<String, String> getAvailableCollectionsViaSWORD(ServiceDocument serviceDocument){
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
	 * Set the authentication credentials only once via constructor. 
	 * To change it, please create a new object.
	 * 
	 * @param authCredentials
	 */
	protected SwordRepositoryExporter(AuthCredentials authCredentials) {
		swordClient = new SWORDClient();
		this.authCredentials = requireNonNull(authCredentials);
	}

	
	public AuthCredentials getAuthCredentials() {
		return authCredentials;
	}

	
	protected SWORDClient getSwordClient() {
		return swordClient;
	}

	
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
	 * 
	 * @return boolean
	 */
	public boolean isSwordAccessible(String serviceDocumentURL) {
		return getServiceDocument(serviceDocumentURL) != null;
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
	 *  
	 * @return <pre>{@link SwordResponse} object or {@code null} in case of error. 
	 * 		   If request type is {@code SwordRequestType.DEPOSIT}, please cast the returned object to {@code DepositReceipt},
	 * 		   you can check it via e.g. {@code instanceof} operator.
	 *  	   If request type is {@code SwordRequestType.REPLACE}, the casting is not needed.
	 *  	   </pre>  
	 *  	   
	 */
	protected SwordResponse exportElement(String collectionURL, SwordRequestType swordRequestType, String mimeFormat, 
											String packageFormat, File file, Map<String, List<String>> metadataMap) {

		
		//TODO: replace with the code from DSpace-Connector!!! But with SwordResponse as output.
		
		
		// Check if only 1 parameter is used (metadata OR file).
		// Multipart is not supported.
		if( ((file != null)&&(metadataMap != null)) || ((file == null)&&(metadataMap == null)) ) {
			return null;
		}

		FileInputStream fis = null;

		Deposit deposit = new Deposit();

		try {
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
				return null;
			}

		} catch (FileNotFoundException e) {
			log.error("Exception by accessing a file", e);
			return null;

		} catch (SWORDClientException | SWORDError | ProtocolViolationException e) {
			log.error("Exception by making deposit", e);
			return null;
		} finally {
			// Close FileInputStream
			if(fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					log.error("Exception by closing the FileInputStream", e);
				}
			}
		}
	}
	
}

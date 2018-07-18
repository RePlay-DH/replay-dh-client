package bwfdm.replaydh.workflow.export.dataverse;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.client.AuthCredentials;
import org.swordapp.client.SWORDClient;
import org.swordapp.client.SWORDClientException;
import org.swordapp.client.SWORDCollection;
import org.swordapp.client.SWORDWorkspace;
import org.swordapp.client.ServiceDocument;
import org.swordapp.client.UriRegistry;

/**
 * 
 * @author Florian Fritze, Volodymyr Kushnarenko
 *
 */
public abstract class DataVerseRepository {
	
	protected static final Logger log = LoggerFactory.getLogger(DataVerseRepository.class);
	
	// Header constants
	public static final String APPLICATION_JSON = "application/json";
	public static final String CONTENT_TYPE_HEADER = "Content-Type";
	public static final String ACCEPT_HEADER = "Accept";
	public static final String MIME_FORMAT_ZIP = "application/zip";
	public static final String MIME_FORMAT_ATOM_XML = "application/atom+xml";
	
	protected Abdera abdera =  new Abdera();
	
	
	/*
	 * -------------------------------
	 * General purpose methods
	 * -------------------------------
	 */
	
	
	/**
	 * Get available collections via SWORD v2
	 * 
	 * @return Map<String, String> where key=URL, value=Title
	 */
	public Map<String, String> getAvailableCollectionsViaSWORD(ServiceDocument serviceDocument){
		Map<String, String> collections = new HashMap<String, String>();
		
		if(serviceDocument != null) {
			for(SWORDWorkspace workspace : serviceDocument.getWorkspaces()) {
				for (SWORDCollection collection : workspace.getCollections()) {
					// key = Title, value = full URL
					collections.put(collection.getTitle(), collection.getHref().toString());
				}
			}
		}
		return collections;
	}
	
	
	/**
	 * Get a file extension (without a dot) from the file name (e.g. "txt", "zip", ...)
	 * @param fileName
	 * @return
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
	 * E.g. {@link UriRegistry.PACKAGE_SIMPLE_ZIP} {@link UriRegistry.PACKAGE_BINARY}
	 * @param fileName
	 * @return 
	 */
	public String getPackageFormat(String fileName) {
		String extension = getFileExtension(fileName);
		
		if(extension.toLowerCase().equals("zip")) {
			return UriRegistry.PACKAGE_SIMPLE_ZIP;
		}
		return UriRegistry.PACKAGE_BINARY;
	}
		
	
	/*
	 * -------------
	 * Extra classes
	 * -------------
	 */
	
	
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
	 * Get the Atom Feed of a Dataverse collection URL
	 * @param dataverseURL
	 * @param auth
	 * @return
	 * @throws SWORDClientException
	 * @throws MalformedURLException
	 */
	public abstract Feed getAtomFeed(String dataverseURL, AuthCredentials auth) throws SWORDClientException, MalformedURLException;
	
	
	/**
	 * Get service document via SWORD v2
	 * 
	 * @param swordClient
	 * @param serviceDocumentURL
	 * @param authCredentials
	 * @return ServiceDocument or null in case of error/exception
	 */
	public abstract ServiceDocument getServiceDocument(SWORDClient swordClient, String serviceDocumentURL, AuthCredentials authCredentials);

	/**
	 * Publish a file to some collections, which is available for the user.
	 * 
	 * @param userLogin
	 * @param collectionURL
	 * @param fileFullPath
	 * @return
	 */
	public abstract boolean publishFile(String collectionURL, File fileFullPath);

	/**
	 * Publish metada only (without any file) to some collection, which is available for the user.
	 * Metadata are described as a {@link java.util.Map}. 
	 *  
	 * @param userLogin
	 * @param collectionURL
	 * @param metadataMap
	 * @return
	 */
	public abstract boolean publishMetadata(String collectionURL, File fileFullPath);

	/**
	 * Publish a file together with the metadata.
	 * Metadata are described in the xml-file.
	 * 
	 * @param userLogin
	 * @param collectionURL
	 * @param dataFile
	 * @param metadataFileXML
	 * @return
	 * @throws IOException 
	 * @throws SWORDClientException 
	 */
	public abstract boolean publishFileAndMetadata(String collectionURL, File dataFile, File metadataFileXML) throws IOException, SWORDClientException;

	/**
	 * Get the entry in the Atom Feed which refers to the URL of the metadata entry in Dataverse. This entry is necessary to add files to the metadata entry in Dataverse.
	 * @param feed
	 * @return
	 */
	public abstract Entry getUserAvailableMetadataset(Feed feed, String doiId);

}

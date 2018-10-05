package bwfdm.replaydh.workflow.export.generic;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.swordapp.client.SWORDCollection;
import org.swordapp.client.SWORDWorkspace;
import org.swordapp.client.ServiceDocument;
import org.swordapp.client.UriRegistry;

public abstract class RepositoryExporter {
	
	public static final String APPLICATION_JSON = "application/json";
	public static final String CONTENT_TYPE_HEADER = "Content-Type";
	public static final String ACCEPT_HEADER = "Accept";
	public static final String MIME_FORMAT_ZIP = "application/zip";
	public static final String MIME_FORMAT_ATOM_XML = "application/atom+xml";
	
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
					collections.put(collection.getHref().toString(), collection.getTitle());
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
	 * Check if publication repository is accessible via API
	 * 
	 * @return
	 */
	public abstract boolean isAccessible();
	
	/**
	 * Set login and password of the user.
	 * Password is needed for the communication with the publication repository via API (e.g. SWORD or REST)
	 * <p>
	 * If the publication repository is DSpace you should put login/password ONLY of the admin-user.
	 * Credentials of the admin-user will be used for the REST/SWORD mechanism. 
	 * This mechanism is needed because of limitations of DSpace-API, where password is always needed.   
	 * <p>
	 *   
	 * @param user
	 * @param password
	 */
	public abstract void setCredentials(String user, char[] password);
	
	/**
	 * Check if user is registered in the publication repository
	 * 
	 * @param loginName
	 * @return
	 */
	public abstract boolean isUserRegistered(String loginName);
	
	/**
	 * Check if user is assigned to publish something in the repository
	 *
	 * @param loginName
	 * @return {@code true} if count of user available collections is great than zero, 
	 * 		   otherwise {@code false} 
 	 */
	public abstract boolean isUserAssigned(String loginName);
		
	/**
	 * Get collections, which are available for the user
	 * Could be, that user has an access only for some specific collections.
	 *  
	 * @param loginName
	 * @return Map of Strings, where key="Collection full URL", value="Collection title", or empty Map if there are not available collections.
	 */
	public abstract Map<String, String> getUserAvailableCollectionsWithTitle(String loginName);
	
	/**
	 * Get collections, which are available for the user, and show their full name
	 * (e.g. for DSpace-repository it means "community/subcommunity/collection")
	 * <p>
	 * Could be, that user has an access only for some specific collections.
	 *  
	 * @param loginName, nameSeparator
	 * @return Map of Strings, where key="Collection full URL", value="Collection full name", or empty Map if there are not available collections.
	 */
	public abstract Map<String, String> getUserAvailableCollectionsWithFullName(String loginName, String fullNameSeparator);
	
	/**
	 * Get available for the admin-user collections for publication.
	 * As credentials for the request are used login/password of the admin-user
	 * 
	 * @return Map of Strings, where key="Collection full URL", value="Collection title", or empty Map if there are not available collections.
	 */
	public abstract Map<String, String> getAdminAvailableCollectionsWithTitle();
	
	/**
	 * Get available for the admin-user collections with full name
	 * (e.g. for DSpace-repository it means "community/subcommunity/collection")
	 * <p>
	 * As credentials for the request are used login/password of the admin-user
	 *  
	 * @param fullNameSeparator 
	 * @return Map of Strings, where key="Collection full URL", value="Collection full name", or empty Map if there are not available collections. 
	 */
	public abstract Map<String, String> getAdminAvailableCollectionsWithFullName(String fullNameSeparator);
	
	/**
	 * Publish a file to some collections, which is available for the user.
	 * 
	 * @param userLogin
	 * @param collectionURL
	 * @param fileFullPath
	 * @return
	 */
	public abstract boolean exportFile(String userLogin, String collectionURL, File fileFullPath);
	
	/**
	 * Publish metada only (without any file) to some collection, which is available for the user.
	 * Metadata are described as a {@link java.util.Map}. 
	 *  
	 * @param userLogin
	 * @param collectionURL
	 * @param metadataMap
	 * @return
	 */
	public abstract boolean exportMetadata(String userLogin, String collectionURL, Map<String, String> metadataMap);
		
	/**
	 * Publish metada only (without any file) to some collection, which is available for the user.
	 * Metadata are described in the xml-file.
	 * 
	 * @param userLogin
	 * @param collectionURL
	 * @param metadataFileXML
	 * @return
	 */
	public abstract boolean exportMetadata(String userLogin, String collectionURL, File metadataFileXML);
	
	/**
	 * Publish a file together with the metadata.
	 * Metadata are described as a {@link java.util.Map}. 
	 * 
	 * @param userLogin
	 * @param collectionURL
	 * @param fileFullPath
	 * @param metadataMap
	 * @return
	 */
	public abstract boolean exportFileAndMetadata(String userLogin, String collectionURL, File fileFullPath, Map<String, String> metadataMap);
	
	/**
	 * Publish a file together with the metadata.
	 * Metadata are described in the xml-file.
	 * 
	 * @param userLogin
	 * @param collectionURL
	 * @param fileFullPath
	 * @param metadataFileXML
	 * @return
	 */
	public abstract boolean exportFileAndMetadata(String userLogin, String collectionURL, File fileFullPath, File metadataFileXML);
}

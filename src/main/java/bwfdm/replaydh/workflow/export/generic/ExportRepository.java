package bwfdm.replaydh.workflow.export.generic;

import java.io.File;
import java.util.Map;

public interface ExportRepository {

	/**
	 * Check if export repository is accessible via API
	 * 
	 * @return
	 */
	public boolean isAccessible();
	
	/**
	 * Set login and password of the user.
	 * Password is needed for the communication with the export repository via API (e.g. SWORD or REST)
	 * <p>
	 * 
	 * TODO: move this part to DSpace package (DSpace connector) 
	 * 
	 * If the export repository is DSpace you should put login/password ONLY of the admin-user.
	 * Credentials of the admin-user will be used for the REST/SWORD mechanism. 
	 * This mechanism is needed because of limitations of DSpace-API, where password is always needed.   
	 * <p>
	 *   
	 * @param user
	 * @param password
	 */
	public void setCredentials(String user, char[] password);
	
	/**
	 * Check if user is registered in the export repository
	 * 
	 * @param loginName
	 * @return
	 */
	public boolean isUserRegistered(String loginName);
	
	/**
	 * Check if user is assigned to publish something in the repository
	 *
	 * @param loginName
	 * @return {@code true} if count of user available collections is great than zero, 
	 * 		   otherwise {@code false} 
 	 */
	public boolean isUserAssigned(String loginName);
		
	/**
	 * Get collections, which are available for the user
	 * Could be, that user has an access only for some specific collections.
	 *  
	 * @param loginName
	 * @return Map of Strings, where key="Collection full URL", value="Collection title", or empty Map if there are not available collections.
	 */
	public Map<String, String> getUserAvailableCollectionsWithTitle(String loginName);
	
	/**
	 * Get collections, which are available for the user, and show their full name
	 * (e.g. for DSpace-repository it means "community/subcommunity/collection")
	 * <p>
	 * Could be, that user has an access only for some specific collections.
	 *  
	 * @param loginName, nameSeparator
	 * @return Map of Strings, where key="Collection full URL", value="Collection full name", or empty Map if there are not available collections.
	 */
	public Map<String, String> getUserAvailableCollectionsWithFullName(String loginName, String fullNameSeparator);
	
	/**
	 * Get available for the admin-user collections for export.
	 * As credentials for the request are used login/password of the admin-user
	 * 
	 * @return Map of Strings, where key="Collection full URL", value="Collection title", or empty Map if there are not available collections.
	 */
	public Map<String, String> getAdminAvailableCollectionsWithTitle();
	
	/**
	 * Get available for the admin-user collections with full name
	 * (e.g. for DSpace-repository it means "community/subcommunity/collection")
	 * <p>
	 * As credentials for the request are used login/password of the admin-user
	 *  
	 * @param fullNameSeparator 
	 * @return Map of Strings, where key="Collection full URL", value="Collection full name", or empty Map if there are not available collections. 
	 */
	public Map<String, String> getAdminAvailableCollectionsWithFullName(String fullNameSeparator);
	
	/**
	 * Publish a file to some collections, which is available for the user.
	 * 
	 * @param userLogin
	 * @param collectionURL
	 * @param fileFullPath
	 * @return
	 */
	public boolean publishFile(String userLogin, String collectionURL, File fileFullPath);
	
	/**
	 * Publish metada only (without any file) to some collection, which is available for the user.
	 * Metadata are described as a {@link java.util.Map}. 
	 *  
	 * @param userLogin
	 * @param collectionURL
	 * @param metadataMap
	 * @return
	 */
	public boolean publishMetadata(String userLogin, String collectionURL, Map<String, String> metadataMap);
		
	/**
	 * Publish metada only (without any file) to some collection, which is available for the user.
	 * Metadata are described in the xml-file.
	 * 
	 * @param userLogin
	 * @param collectionURL
	 * @param metadataFileXML
	 * @return
	 */
	public boolean publishMetadata(String userLogin, String collectionURL, File metadataFileXML);
	
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
	public boolean publishFileAndMetadata(String userLogin, String collectionURL, File fileFullPath, Map<String, String> metadataMap);
	
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
	public boolean publishFileAndMetadata(String userLogin, String collectionURL, File fileFullPath, File metadataFileXML);
	
}

package bwfdm.replaydh.workflow.export.generic.refactor;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface ExportRepository {

	
	/**
	 * Check if export repository is accessible via API.
	 * 
	 * @return {@code true} if repository is accessible and {@code false} otherwise
	 */
	public boolean isAccessible();
	
	
	/**
	 * Check if current authentication credentials (user login and password) are registered in the export repository.
	 * <p>
	 * IMPORTANT: credentials must be defined in other place, e.g. via class constructor.
	 * 
	 * @return {@code true} if credentials are registered and {@code false} otherwise 
	 */
	public boolean isUserRegistered();
	
	
	/**
	 * Check if current authentication credentials (user login and password) are assigned to export in the repository.
	 * <p>
	 * IMPORTANT: credentials must be defined in other place, e.g. via class constructor.
	 *
	 * @return {@code true} if count of user available collections is great than zero, 
	 * 		   otherwise {@code false} 
 	 */
	public boolean isUserAssigned();
	
	
	/**
	 * Get collections, which are available for the current authentication credentials.
	 * Could be, that different credentials can have an access to different collections.
	 * <p>
	 * IMPORTANT: credentials must be defined in other place, e.g. via class constructor.
	 *  
	 * @return Map of Strings, where key="Collection full URL", value="Collection title", or empty Map if there are not available collections.
	 */
	public Map<String, String> getUserAvailableCollectionsWithTitle();
	
	
	/**
	 * Get collections, which are available for the current authentication credentials, and show their full name
	 * (e.g. for DSpace-repository it means "community/subcommunity/collection". where '/' is the separator)
	 * <p>
	 * Could be, that the current credentials have an access only for some specific collections.
	 * <p>
	 * IMPORTANT: credentials must be defined in other place, e.g. via class constructor.
	 *  
	 * @param nameSeparator
	 * @return Map of Strings, where key="Collection full URL", value="Collection full name", or empty Map if there are not available collections.
	 */
	public Map<String, String> getUserAvailableCollectionsWithFullName(String fullNameSeparator);
	
		
	/**
	 * Publish a file to some collection, which is available for the current the authentication credentials.
	 * <p>
	 * IMPORTANT: credentials must be defined in other place, e.g. via class constructor.
	 * 
	 * @param collectionURL
	 * @param fileFullPath
	 * @return
	 */
	public boolean publishFile(String collectionURL, File fileFullPath);
	
	
	/**
	 * Publish metadata only (without any file) to some collection, which is available for the current authentication credentials.
	 * Metadata are described as a {@link java.util.Map}. 
	 * <p>
	 * IMPORTANT: credentials must be defined in other place, e.g. via class constructor.
	 *  
	 * @param collectionURL
	 * @param metadataMap
	 * @return
	 */
	public boolean publishMetadata(String collectionURL, Map<String, List<String>> metadataMap);
	
	
	/**
	 * Publish metadata only (without any file) to some collection, which is available for the current authentication credentials.
	 * Metadata are described in the xml-file.
	 * <p>
	 * IMPORTANT: credentials must be defined in other place, e.g. via class constructor.
	 * 
	 * @param collectionURL
	 * @param metadataFileXML
	 * @return
	 */
	public boolean publishMetadata(String collectionURL, File metadataFileXML);
	
	
	/**
	 * Publish a file together with the metadata to some collection, which is available for the current authentication credentials.
	 * Metadata are described as a {@link java.util.Map}. 
	 * <p>
	 * IMPORTANT: credentials must be defined in other place, e.g. via class constructor.
	 * 
	 * @param collectionURL
	 * @param fileFullPath
	 * @param metadataMap
	 * @return
	 */
	public boolean publishFileAndMetadata(String collectionURL, File fileFullPath, Map<String, List<String>> metadataMap);
	
	
	/**
	 * Publish a file together with the metadata.
	 * Metadata are described in the xml-file.
	 * <p>
	 * IMPORTANT: credentials must be defined in other place, e.g. via class constructor.
	 * 
	 * @param collectionURL
	 * @param fileFullPath
	 * @param metadataFileXML
	 * @return
	 */
	public boolean publishFileAndMetadata(String collectionURL, File fileFullPath, File metadataFileXML);
	
}

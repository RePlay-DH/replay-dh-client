package bwfdm.replaydh.workflow.export.dspace.refactor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ExportRepository {
	
	//TODO: use this interface as a common interface for each export repository. Move it to package with SwordExporter.

	
	/**
	 * Check if export repository is accessible via API.
	 * 
	 * @return {@code true} if repository is accessible and {@code false} otherwise
	 */
	public boolean isRepositoryAccessible();
		
	
	/**
	 * Check if current authentication credentials (e.g. API token, user login and password) are registered in the export repository.
	 * <p>
	 * IMPORTANT: credentials are used implicitly. Definition of the credentials must be done in other place, e.g. via class constructor.
	 * 
	 * @return {@code true} if credentials are registered and {@code false} otherwise 
	 */
	public boolean hasRegisteredCredentials();
	
	
	/**
	 * Check if current authentication credentials (e.g. API token, user login and password) are assigned to export in the repository.
	 * <p>
	 * IMPORTANT: credentials are used implicitly. Definition of the credentials must be done in other place, e.g. via class constructor.
	 *
	 * @return {@code true} if count of user available collections is great than zero, 
	 * 		   otherwise {@code false} 
 	 */
	public boolean hasAssignedCredentials();
	
	
	/**
	 * Get collections, which are available for the current authentication credentials.
	 * Could be, that different credentials can have an access to different collections.
	 * <p>
	 * IMPORTANT: credentials are used implicitly. Definition of the credentials must be done in other place, e.g. via class constructor.
	 *  
	 * @return Map of Strings, where key="Collection full URL", value="Collection title", or empty Map if there are not available collections.
	 */
	public Map<String, String> getAvailableCollections();
	
	
	/**
	 * Create a new item with metadata only (without any file) in some collection, which is available for the current authentication credentials.
	 * Metadata are described as a {@link java.util.Map}. 
	 * <p>
	 * IMPORTANT: credentials are used implicitly. Definition of the credentials must be done in other place, e.g. via class constructor.
	 *  
	 * @param collectionURL
	 * @param metadataMap
	 * @return {@code true} in case of success and {@code false} otherwise  
	 * 
	 * TODO: String with the new item URL in case of success or "" otherwise
	 */
	public boolean createNewItemWithMetadata(String collectionURL, Map<String, List<String>> metadataMap);
	
	
	/**
	 * Create a new item with metadata only (without any file) in some collection, which is available for the current authentication credentials.
	 * Metadata are described in the xml-file.
	 * <p>
	 * IMPORTANT: credentials are used implicitly. Definition of the credentials must be done in other place, e.g. via class constructor.
	 * 
	 * @param collectionURL
	 * @param metadataFileXml
	 * @return {@code true} in case of success and {@code false} otherwise
	 * 
	 * TODO: String with the new item URL in case of success or "" otherwise
	 */
	public boolean createNewItemWithMetadata(String collectionURL, File metadataFileXml) throws IOException;
	
	
	/**
	 * Create a new item with a file and metadata in some collection, which is available for the current authentication credentials.
	 * Metadata are described as a {@link java.util.Map}. 
	 * <p>
	 * IMPORTANT: credentials are used implicitly. Definition of the credentials must be done in other place, e.g. via class constructor.
	 * 
	 * @param collectionURL
	 * @param file
	 * @param metadataMap
	 * @return {@code true} in case of success and {@code false} otherwise
	 * 
	 * TODO: String with the new item URL in case of success or "" otherwise
	 */
	public boolean createNewItemWithFileAndMetadata(String collectionURL, File file, Map<String, List<String>> metadataMap) throws IOException;
	
	
	/**
	 * Create a new item with a file and metadata in some collection, which is available for the current authentication credentials.
	 * Metadata are described in the xml-file.
	 * <p>
	 * IMPORTANT: credentials are used implicitly. Definition of the credentials must be done in other place, e.g. via class constructor.
	 * 
	 * @param collectionURL
	 * @param file
	 * @param metadataFileXml
	 * @return {@code true} in case of success and {@code false} otherwise
	 * 
	 * TODO: return String with the new item URL in case of success or "" otherwise 
	 */
	public boolean createNewItemWithFileAndMetadata(String collectionURL, File file, File metadataFileXml) throws IOException;
	
}

package bwfdm.replaydh.workflow.export.generic;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ExportRepository {
	
	//TODO: use this interface as a common interface for each export repository. Move it to package with SwordExporter.
	
	/**
	 * Check if export repository is accessible via API.
	 * 
	 * @return {@code true} if repository is accessible and {@code false} otherwise.
	 */
	public boolean isRepositoryAccessible();
		
	
	/**
	 * Check if current authentication credentials (e.g. API token, user login and password) are registered 
	 * in the export repository.
	 * <p>
	 * <b>IMPORTANT:</b> credentials are used implicitly. Definition of the credentials must be done in other place, 
	 * e.g. via class constructor.
	 * 
	 * @return {@code true} if credentials are registered and {@code false} otherwise.
	 */
	public boolean hasRegisteredCredentials();
	
	
	/**
	 * Check if current authentication credentials (e.g. API token, user login and password) 
	 * are assigned to export in the repository.
	 * <p>
	 * <b>IMPORTANT:</b> credentials are used implicitly. Definition of the credentials must be done in other place, 
	 * e.g. via class constructor.
	 *
	 * @return {@code true} if count of user available collections is great than zero, 
	 * 		   otherwise {@code false}. 
 	 */
	public boolean hasAssignedCredentials();
	
	
	/**
	 * Get collections, which are available for the current authentication credentials.
	 * Could be, that different credentials can have an access to different collections.
	 * <p>
	 * <b>IMPORTANT:</b> credentials are used implicitly. Definition of the credentials must be done in other place, 
	 * e.g. via class constructor.
	 *  
	 * @return {@link Map} of Strings, where key = "collection full URL", value = "collection title". 
	 * 		The map can be also empty if there are not available collections. 
	 * 		In case of some error should be returned a {@code null} value.  
	 */
	public Map<String, String> getAvailableCollections();
	
	
	/**
	 * Export (create) a new entry with metadata only (without any file) in some collection, which is available 
	 * for the current authentication credentials. Metadata are described as a {@link java.util.Map}. 
	 * <p>
	 * <b>IMPORTANT:</b> credentials are used implicitly. Definition of the credentials must be done in other place, 
	 * e.g. via class constructor.
	 *  
	 * @param collectionURL the full URL of the collection, where the export (ingest) will be done.
	 * @param metadataMap metadata as {@link Map}, where key = metadata field (e.g. "creator", "title", "year", ... ), 
	 * 		value = {@link List} with the metadata field values (e.g. {"Author-1", "Author-2", ... }).
	 * 
	 * @return {@link String} with the URL of the new created entry or {@code null} in case of error.  
	 */
	public String exportNewEntryWithMetadata(String collectionURL, Map<String, List<String>> metadataMap);
	
	
	/**
	 * Export (create) a new entry with metadata only (without any file) in some collection, which is available 
	 * for the current authentication credentials. Metadata are described as a xml-file.
	 * <p>
	 * <b>IMPORTANT:</b> credentials are used implicitly. Definition of the credentials must be done in other place, 
	 * e.g. via class constructor.
	 * 
	 * @param collectionURL the full URL of the collection, where the export (ingest) will be done.
	 * @param metadataFileXml metadata as a xml-file in dublin core (DC) format.
	 * 
	 * @return {@link String} with the URL of the new created entry or {@code null} in case of error.
	 * 
	 * @throws IOException
	 */
	//TODO: activate in future releases
	//TODO: differentiate between dublin core (DC) and METS formats
	//public String exportNewEntryWithMetadata(String collectionURL, File metadataFileXml) throws IOException;
	
	
	/**
	 * Export (create) a new entry with a file and metadata in some collection, which is available 
	 * for the current authentication credentials. Metadata are described as a {@link java.util.Map}. 
	 * <p>
	 * <b>IMPORTANT:</b> credentials are used implicitly. Definition of the credentials must be done in other place, 
	 * e.g. via class constructor.
	 * 
	 * @param collectionURL the full URL of the collection, where the export (ingest) will be done 
	 * @param file an archive file with one or more files inside (e.g. ZIP-file as a standard) or a binary file 
	 * 			which will be exported.
	 * @param unpackFileIfArchive should be used for archive files (e.g. ZIP). A flag which decides, 
	 * 			if the exported archive will be unpacked in the repository ({@code true} value,
	 * 			new entry will include in this case all files of the archive file) or archive will be exported 
	 * 			as a binary file ({@code false} value, new entry will include only 1 file - the exported archive
	 * 			as a binary file). <b>NOTE:</b> if unpacking is not supported by the repository, 
	 * 			please use {@code false} value. 
	 * @param metadataMap metadata as {@link Map}, where key = metadata field (e.g. "creator", "title", "year", ... ), 
	 * 		value = {@link List} with the metadata field values (e.g. {"Author-1", "Author-2", ... }). 
	 * 
	 * @return {@link String} with the URL of the new created entry or {@code null} in case of error.
	 * 
	 * @throws IOException
	 */
	public String exportNewEntryWithFileAndMetadata(String collectionURL, File file, 
				boolean unpackFileIfArchive, Map<String, List<String>> metadataMap) throws IOException;
	
	
	/**
	 * Export (create) a new entry with a file and metadata in some collection, which is available 
	 * for the current authentication credentials. Metadata are described as a xml-file.
	 * <p>
	 * <b>IMPORTANT:</b> credentials are used implicitly. Definition of the credentials must be done in other place, 
	 * e.g. via class constructor.
	 * 
	 * @param collectionURL the full URL of the collection, where the export (ingest) will be done.
	 * @param file an archive file with one or more files inside (e.g. ZIP-file as a standard) or a binary file 
	 * 			which will be exported.
	 * @param unpackFileIfArchive should be used for archive files (e.g. ZIP). A flag which decides, 
	 * 			if the exported archive will be unpacked in the repository ({@code true} value,
	 * 			new entry will include in this case all files of the archive file) or archive will be exported 
	 * 			as a binary file ({@code false} value, new entry will include only 1 file - the exported archive
	 * 			as a binary file). <b>NOTE:</b> if unpacking is not supported by the repository, 
	 * 			please use {@code false} value.
	 * @param metadataFileXml metadata as a xml-file in dublin core (DC) format.
	 * 
	 * @return {@link String} with the URL of the new created entry or {@code null} in case of error.
	 * 
	 * @throws IOException
	 */
	//TODO: activate in future releases
	//TODO: differentiate between dublin core (DC) and METS formats
	//public String exportNewEntryWithFileAndMetadata(String collectionURL, File file, 
	//			boolean unpackFileIfArchive, File metadataFileXml) throws IOException;
	
}

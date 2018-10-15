package bwfdm.replaydh.workflow.export.dataverse;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.client.AuthCredentials;
import org.swordapp.client.SWORDClientException;
import bwfdm.replaydh.workflow.export.generic.SwordRepositoryExporter;

/**
 * 
 * @author Florian Fritze, Volodymyr Kushnarenko
 *
 */
public abstract class DataverseRepository extends SwordRepositoryExporter {
	
	public DataverseRepository(AuthCredentials authCredentials) {
		super(authCredentials);
	}

	protected static final Logger log = LoggerFactory.getLogger(DataverseRepository.class);
	
	
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
	 * Publish a file to some collections, which is available for the user.
	 * 
	 * @param userLogin
	 * @param metadataSetHrefURL
	 * @param zipFile
	 * @return
	 * @throws IOException 
	 */
	public abstract boolean uploadZipFile(String metadataSetHrefURL, File zipFile) throws IOException;

	/**
	 * Publish metada only (without any file) to some collection, which is available for the user.
	 * Metadata are described as a {@link java.util.Map}. 
	 *  
	 * @param userLogin
	 * @param collectionURL
	 * @param metadataMap
	 * @return
	 */
	public abstract String uploadMetadata(String collectionURL, File fileFullPath, Map<String, List<String>> metadataMap);

	/**
	 * Publish a file together with the metadata.
	 * Metadata are described in the xml-file.
	 * 
	 * @param userLogin
	 * @param collectionURL
	 * @param filesToZip
	 * @param metadataFileXML
	 * @return
	 * @throws IOException 
	 * @throws SWORDClientException 
	 */
	public abstract boolean uploadNewMetadataAndFile(String collectionURL, File zipFile, File metadataFileXML, Map<String, List<String>> metadataMap) throws IOException, SWORDClientException;
	
	/**
	 * Replaces a metadata entry
	 * @param doiUrl
	 * @param zipFile
	 * @param metadataFileXML
	 * @param metadataMap
	 * @return
	 * @throws IOException
	 * @throws SWORDClientException
	 */
	public abstract boolean replaceMetadata(String doiUrl, File zipFile, File metadataFileXML, Map<String, List<String>> metadataMap) throws IOException, SWORDClientException;
	
	/**
	 * Replace metadata entry and add file
	 * @param collectionURL TODO
	 * @param doiUrl
	 * @param zipFile
	 * @param metadataFileXML
	 * @param metadataMap
	 * @return
	 * @throws IOException
	 * @throws SWORDClientException
	 */
	public abstract boolean replaceMetadataAndAddFile(String collectionURL, String doiUrl, File zipFile, File metadataFileXML, Map<String, List<String>> metadataMap) throws IOException, SWORDClientException;

	/**
	 * Get the entry in the Atom Feed which refers to the URL of the metadata entry in Dataverse. This entry is necessary to add files to the metadata entry in Dataverse.
	 * @param feed
	 * @return
	 */
	public abstract Entry getUserAvailableMetadataset(Feed feed, String doiId);
	
	/**
	 * Returns all the entries of a DataVerse URL saved in ID --> Title map
	 * @param feed
	 * @return
	 */
	public abstract Map<String, String> getMetadataSetsWithId(Feed feed);
	
	/**
	 * Returns a Map of all files in a dataverse collection
	 * @param chosenCollection
	 * @return
	 * @throws SWORDClientException 
	 * @throws MalformedURLException 
	 */
	public abstract Map<String, String> getDatasetsInDataverseCollection(String chosenCollection) throws MalformedURLException, SWORDClientException;

	/**
	 * Copied from class SWORDClient but modified for the Dataverse REST API
	 * @param doiUrl
	 * @param apiKey
	 * @return TODO
	 * @throws SWORDClientException 
	 * @throws IOException 
	 */
	public abstract String getJSONMetadata(String doiUrl) throws SWORDClientException, IOException;
	
}

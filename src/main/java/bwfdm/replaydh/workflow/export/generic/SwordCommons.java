package bwfdm.replaydh.workflow.export.generic;

import java.io.File;
import java.util.Map;

import org.swordapp.client.AuthCredentials;
import org.swordapp.client.SWORDClient;
import org.swordapp.client.ServiceDocument;
import org.swordapp.client.SwordResponse;
import org.swordapp.client.UriRegistry;

import bwfdm.replaydh.workflow.export.dspace.DSpaceRepository.SwordRequestType;

/**
 * SWORD methods, which could be common for different types of publication repositories
 * (e.g. DSpace, Dataverse)
 * 
 * @author Volodymyr Kushnarenko
 * @author Florian Fritze
 *
 */
public interface SwordCommons {

	/**
	 * Get new authentication credentials. 
	 * <p> To disactivate "on-behalf-of" option please use the same string for "adminUser" and "userLogin".
	 * <p> If "adminUser" and "userLogin" are different, "on-behalf-of" option will be used.
	 * 
	 * @param adminUser
	 * @param adminPassword
	 * @param userLogin
	 * @return
	 */
	public AuthCredentials getNewAuthCredentials(String adminUser, char[] adminPassword, String userLogin);
	
	
	/**
	 * Get service document via SWORD v2
	 * 
	 * @param swordClient
	 * @param serviceDocumentURL
	 * @param authCredentials
	 * @return ServiceDocument or null in case of error/exception
	 */
	public ServiceDocument getServiceDocument(SWORDClient swordClient, String serviceDocumentURL, AuthCredentials authCredentials);
	
	
	/**
	 * Get available collections via SWORD v2
	 * 
	 * @return Map<String, String> where key=URL, value=Title
	 */
	public Map<String, String> getAvailableCollectionsViaSWORD(ServiceDocument serviceDocument);
	
	
	/**
	 * Export a file or metadata.
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
	 * TODO: what to return??
	 * 
	 * @return "Location" parameter from the response in case of {@code SwordRequestType.DEPOSIT} request, 
	 *  	   "StatusCode" parameter from the response in case of {@code SwordRequestType.REPLACE} request,
	 *  	   or {@code null} in case of error
	 */
	public <T extends SwordResponse> T exportElement(String userLogin, String collectionURL, SwordRequestType swordRequestType, String mimeFormat, String packageFormat, File file, Map<String, String> metadataMap);
	//TODO: return DepositReceipt ???
	
	
	/**
	 * Check if SWORDv2-protocol is accessible
	 * @return boolean
	 */
	public boolean isSwordAccessible();
		
	
}

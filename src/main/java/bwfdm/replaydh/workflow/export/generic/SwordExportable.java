package bwfdm.replaydh.workflow.export.generic;

import java.io.File;
import java.util.Map;

import org.swordapp.client.AuthCredentials;
import org.swordapp.client.ServiceDocument;
import org.swordapp.client.SwordResponse;
import org.swordapp.client.UriRegistry;

import bwfdm.replaydh.workflow.export.dspace.DSpaceRepository.SwordRequestType;

/**
 * Interface with SWORD methods, which could be common for different publication repositories types
 * (for the current moment - DSpace and Dataverse)
 * 
 * @author Volodymyr Kushnarenko
 * @author Florian Fritze
 *
 */
public interface SwordExportable {

	/**
	 * Create new authentication credentials. 
	 * 
	 * <p> To use "on-behalf-of" option the "adminUser" and "userLogin" strings must be different. 
	 * <p> To disable the "on-behalf-of" option please use the same value for "adminUser" and "userLogin" fields.
	 * 
	 * @param adminUser
	 * @param adminPassword
	 * @param userLogin
	 * @return
	 */
	public AuthCredentials createAuthCredentials(String adminUser, char[] adminPassword, String userLogin);
	
	
	/**
	 * Get service document via SWORD v2
	 * 
	 * @param serviceDocumentURL
	 * @param authCredentials
	 * @return ServiceDocument or null in case of error/exception
	 */
	public ServiceDocument getServiceDocument(String serviceDocumentURL, AuthCredentials authCredentials);
	
	
	/**
	 * Get available collections via SWORD v2
	 * 
	 * TODO: rename to "getSwordAvailableCollections"
	 * 
	 * @return Map<String, String> where key=URL, value=Title
	 */
	public Map<String, String> getAvailableCollectionsViaSWORD(ServiceDocument serviceDocument);
	
	
	/**
	 * Export file or metadata.
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
	 * 
	 * TODO: what to return??
	 * 
	 * 
	 * TODO: rename to "exportElement"
	 * 
	 * 
	 * @return "Location" parameter from the response in case of {@code SwordRequestType.DEPOSIT} request, 
	 *  	   "StatusCode" parameter from the response in case of {@code SwordRequestType.REPLACE} request,
	 *  	   or {@code null} in case of error
	 */
	public SwordResponse publishElement(AuthCredentials authCredentials, String collectionURL, SwordRequestType swordRequestType, String mimeFormat, String packageFormat, File file, Map<String, String> metadataMap);
	//TODO: return DepositReceipt ???
	
	
	/**
	 * Check if SWORDv2-protocol is accessible
	 * 
	 * TODO: rename to "isSwordAvailable"
	 * 
	 * @return boolean
	 */
	public boolean isSwordAccessible(String serviceDocumentURL, AuthCredentials authCredentials);
		
	
}

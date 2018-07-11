package bwfdm.replaydh.workflow.export.dataverse;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.client.AuthCredentials;
import org.swordapp.client.SWORDClient;

import bwfdm.replaydh.workflow.export.dspace.DSpaceRepository;

/**
 * 
 * @author Florian Fritze
 *
 */
public class DataVerseRepository_v4 extends DataVerseRepository {
	
	public DataVerseRepository_v4(String serviceDocumentURL, String adminUser, String adminPassword) {
		this.adminUser=adminUser;
		this.adminPassword=adminPassword;
		this.serviceDocumentURL=serviceDocumentURL;
	}
	
	// For SWORD
	protected String adminUser;
	protected String adminPassword;
	protected String serviceDocumentURL;
			
	// For REST	
	//
	// URLs
	protected String restURL;	
	
	protected static final Logger log = LoggerFactory.getLogger(DataVerseRepository_v4.class);

	
	/**
	 * Check if SWORDv2-protocol is accessible
	 * @return
	 */
	public boolean isSwordAccessible() {
		
		SWORDClient swordClient = new SWORDClient();
		AuthCredentials authCredentials = new AuthCredentials(this.adminUser, String.valueOf(this.adminPassword));
		if(DSpaceRepository.getServiceDocument(swordClient, this.serviceDocumentURL, authCredentials) != null) {
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean isAccessible() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setCredentials(String user, char[] password) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isUserRegistered(String loginName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isUserAssigned(String loginName) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<String, String> getUserAvailableCollectionsWithTitle(String loginName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getUserAvailableCollectionsWithFullName(String loginName, String fullNameSeparator) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getAdminAvailableCollectionsWithTitle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, String> getAdminAvailableCollectionsWithFullName(String fullNameSeparator) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean publishFile(String userLogin, String collectionURL, File fileFullPath) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean publishMetadata(String userLogin, String collectionURL, Map<String, String> metadataMap) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean publishMetadata(String userLogin, String collectionURL, File metadataFileXML) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean publishFileAndMetadata(String userLogin, String collectionURL, File fileFullPath,
			Map<String, String> metadataMap) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean publishFileAndMetadata(String userLogin, String collectionURL, File fileFullPath,
			File metadataFileXML) {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	
}

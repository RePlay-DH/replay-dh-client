package bwfdm.replaydh.workflow.export.dataverse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.client.AuthCredentials;
import org.swordapp.client.Deposit;
import org.swordapp.client.DepositReceipt;
import org.swordapp.client.EntryPart;
import org.swordapp.client.ProtocolViolationException;
import org.swordapp.client.SWORDClient;
import org.swordapp.client.SWORDClientException;
import org.swordapp.client.SWORDError;
import org.swordapp.client.ServiceDocument;
import org.swordapp.client.SwordResponse;
import org.swordapp.client.UriRegistry;

import bwfdm.replaydh.workflow.export.dspace.DSpaceRepository;
import bwfdm.replaydh.workflow.export.dspace.DSpaceRepository.SwordRequestType;

/**
 * 
 * @author Florian Fritze, Volodymyr Kushnarenko
 *
 */
public class DataVerseRepository_v4 extends DataVerseRepository {
	
	public DataVerseRepository_v4(String serviceDocumentURL, String adminUser, String adminPassword) {
		this.serviceDocumentURL=serviceDocumentURL;
		this.adminPassword=adminPassword;
		swordClient = new SWORDClient();
		authCredentials = new AuthCredentials(adminUser, String.valueOf(adminPassword));
	}
	
	// For SWORD
	private String adminPassword;
	protected String serviceDocumentURL;
			
	private AuthCredentials authCredentials = null;
	
	private SWORDClient swordClient = null;
	
	// For REST	
	//
	// URLs
	protected String restURL;	
	
	protected static final Logger log = LoggerFactory.getLogger(DataVerseRepository_v4.class);

	static {
		
	}
	/**
	 * Check if SWORDv2-protocol is accessible
	 * @return
	 */
	public boolean isSwordAccessible() {
		SWORDClient swordClient = new SWORDClient();
		if(DSpaceRepository.getServiceDocument(swordClient, this.serviceDocumentURL, authCredentials) != null) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Publish a file or metadata. Private method.
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
	 * @return "Location" parameter from the response in case of {@code SwordRequestType.DEPOSIT} request, 
	 *  	   "StatusCode" parameter from the response in case of {@code SwordRequestType.REPLACE} request,
	 *  	   or {@code null} in case of error
	 */
	protected String publishElement(String collectionURL, SwordRequestType swordRequestType, String mimeFormat, String packageFormat, File file, Map<String, String> metadataMap) {
		
		// Check if only 1 parameter is used (metadata OR file). 
		// Multipart is not supported.
		if( ((file != null)&&(metadataMap != null)) || ((file == null)&&(metadataMap == null)) ) {
			return null; 
		}
		
		SWORDClient swordClient = new SWORDClient();
		//AuthCredentials authCredentials = getNewAuthCredentials(null, this.adminPassword, userLogin);
		
		FileInputStream fis = null;
		
		Deposit deposit = new Deposit();
		
		try {
			// Check if "metadata as a Map"
			if(metadataMap != null) {
				EntryPart ep = new EntryPart();
				for(Map.Entry<String, String> metadataEntry : metadataMap.entrySet()) {
					ep.addDublinCore(metadataEntry.getKey(), metadataEntry.getValue());
				}
				deposit.setEntryPart(ep);
			}
			
			// Check if "file"
			if(file != null) {
				fis = new FileInputStream(file); // open FileInputStream
				deposit.setFile(fis);				
				deposit.setFilename(file.getName()); 	// deposit works properly ONLY with a "filename" parameter 
														// --> in curl: -H "Content-Disposition: filename=file.zip"
			}
			
			deposit.setMimeType(mimeFormat);
			deposit.setPackaging(packageFormat);
			deposit.setInProgress(true);
			//deposit.setMd5("fileMD5");					//put here only as example
			//deposit.setSuggestedIdentifier("abcdefg");	//put here only as example
			
			
			switch (swordRequestType) {
			case DEPOSIT:
				DepositReceipt receipt = swordClient.deposit(collectionURL, deposit, authCredentials);
				return receipt.getLocation(); //"Location" parameter from the response
			case REPLACE:
				SwordResponse response = swordClient.replace(collectionURL, deposit, authCredentials);
				return Integer.toString(response.getStatusCode()); //"StatusCode" parameter from the response
			default:
				log.error("Wrong SWORD-request type: " + swordRequestType + " : Supported here types are: " + SwordRequestType.DEPOSIT + ", " + SwordRequestType.REPLACE);
				return null;					
			}
			
		} catch (FileNotFoundException e) {
			log.error("Exception by accessing a file: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			return null;	
		
		} catch (SWORDClientException | SWORDError | ProtocolViolationException e) {
			log.error("Exception by making deposit: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			return null;
		} finally {
			// Close FileInputStream
			if(fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					log.error("Exception by closing the FileInputStream: " + e.getClass().getSimpleName() + ": " + e.getMessage());
				}
			}
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
	public Map<String, String> getUserAvailableCollectionsWithTitle() {
		// TODO Auto-generated method stub
		if(this.getServiceDocument(swordClient, serviceDocumentURL, authCredentials) != null) {
			return super.getAvailableCollectionsViaSWORD(this.getServiceDocument(swordClient, serviceDocumentURL, authCredentials));
		} else {
			return null;
		}
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
	public boolean publishFile(String collectionURL, File fileFullPath) {
		String mimeFormat = "application/zip"; // for every file type, to publish even "XML" files as a normal file
		String packageFormat = getPackageFormat(fileFullPath.getName()); //zip-archive or separate file
		
		if(publishElement(collectionURL, SwordRequestType.DEPOSIT, mimeFormat, packageFormat, fileFullPath, null) != null) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean publishMetadata(String collectionURL, File fileFullPath) {
		// TODO Auto-generated method stub
		String mimeFormat = "application/atom+xml";
		
		if(publishElement(collectionURL, SwordRequestType.DEPOSIT, mimeFormat, null, fileFullPath, null) != null) {
			return true;
		} else {
			return false;
		}
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

	@Override
	public ServiceDocument getServiceDocument(SWORDClient swordClient, String serviceDocumentURL,
			AuthCredentials authCredentials) {
		ServiceDocument serviceDocument = null;
		try {
			serviceDocument = swordClient.getServiceDocument(serviceDocumentURL, authCredentials);
		} catch (SWORDClientException | ProtocolViolationException e) {
			log.error("Exception by accessing service document: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			return null;
		}
		return serviceDocument;
	}

	@Override
	public boolean publishMetadata(String userLogin, String collectionURL, File metadataFileXML) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<String, String> getUserAvailableDatasetsWithTitle(String dataverse) {
		// TODO Auto-generated method stub
		if(this.getServiceDocument(swordClient, serviceDocumentURL, authCredentials) != null) {
			return super.getAvailableCollectionsViaSWORD(this.getServiceDocument(swordClient, serviceDocumentURL, authCredentials));
		} else {
			return null;
		}
	}
	
	
	
}

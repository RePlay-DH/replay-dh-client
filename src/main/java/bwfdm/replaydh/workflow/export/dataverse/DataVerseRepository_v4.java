package bwfdm.replaydh.workflow.export.dataverse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.Response;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
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

import bwfdm.replaydh.io.IOUtils;
import bwfdm.replaydh.workflow.export.dspace.DSpaceRepository;

/**
 * 
 * @author Florian Fritze, Volodymyr Kushnarenko
 *
 */
public class DataVerseRepository_v4 extends DataVerseRepository {
	
	public DataVerseRepository_v4(String serviceDocumentURL, String apiKey) {
		this.serviceDocumentURL=serviceDocumentURL;
		swordClient = new SWORDClient();
		authCredentials = new AuthCredentials(apiKey, "null");
	}
	
	// For SWORD
	private String serviceDocumentURL;
			
	private AuthCredentials authCredentials = null;
	
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
	
	public Feed getAtomFeed(String dataverseURL, AuthCredentials auth) throws SWORDClientException, MalformedURLException
    {
        // do some error checking and validations
        if (dataverseURL == null)
        {
            log.error("Null string passed in to getAtomFeed; returning null");
            return null;
        }
        if (log.isDebugEnabled())
        {
            log.debug("getting Atom Feed from " + dataverseURL);
        }

        AbderaClient client = new AbderaClient(this.abdera);

        if (auth != null) {
        	if (auth.getUsername() != null) {
        		if (log.isDebugEnabled())
                {
                    log.debug("Setting username/password: " + auth.getUsername() + "/****password omitted *****");
                }
                UsernamePasswordCredentials unpw = new UsernamePasswordCredentials(auth.getUsername(), auth.getPassword());

                // create the credentials - target and realm can be null (and are so by default)
                try
                {
                    client.addCredentials(auth.getTarget(), auth.getRealm(), "basic", unpw);
                }
                catch (URISyntaxException e)
                {
                    log.error("Unable to parse authentication target in AuthCredential", e);
                    throw new SWORDClientException("Unable to parse authentication target in AuthCredentials", e);
                }
        	}
        }
        // ensure that the URL is valid
        URL url = new URL(dataverseURL);
        if (log.isDebugEnabled())
        {
            log.debug("Formalised Atom Document URL to " + url.toString());
        }

        // make the request for atom feed
        if (log.isDebugEnabled())
        {
           log.debug("Connecting to Server to Atom Feed Document from " + url.toString() + " ...");
        }
        ClientResponse resp = client.get(url.toString());
        System.out.println("Output: "+resp);
        if (log.isDebugEnabled())
        {
            log.debug("Successfully retrieved Atom Feed from " + url.toString());
        }

        // if the response is successful, get the Atom Feed out of the response
        if (resp.getType() == Response.ResponseType.SUCCESS)
        {
            log.info("Retrieved Atom Feed from " + url.toString() + " with HTTP success code");
            Document<Feed> doc = resp.getDocument();
            Feed sd = doc.getRoot();
            return sd;
        }

        // if we don't get anything respond with null
        log.warn("Unable to retrieve Atom Feed from " + url.toString() + "; responded with " + resp.getStatus()
                + ". Possible problem with SWORD server, or URL");
        return null;
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
				return receipt.getEntry().getEditMediaLinkResolvedHref().toString(); //"Location" parameter from the response
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
	

	public Map<String, String> getUserAvailableCollectionsWithTitle() {
		if(this.getServiceDocument(serviceDocumentURL) != null) {
			return super.getAvailableCollectionsViaSWORD(this.getServiceDocument(serviceDocumentURL));
		} else {
			return null;
		}
	}


	public void publishZipFile(String metadataSetHrefURL, File fileFullPath) {
		String packageFormat = getPackageFormat(fileFullPath.getName()); //zip-archive or separate file
		publishElement(metadataSetHrefURL, SwordRequestType.DEPOSIT, MIME_FORMAT_ZIP, packageFormat, fileFullPath, null);
	}

	public String publishMetadata(String collectionURL, File fileFullPath) {
		String returnValue = publishElement(collectionURL, SwordRequestType.DEPOSIT, MIME_FORMAT_ATOM_XML, null, fileFullPath, null);
		if(returnValue != null) {
			return returnValue;
		} else {
			log.error("No return value from publishElement method");
			return null;
		}
	}

	public void publisNewMetadataAndFile(String collectionURL, List<File> fileslist, File metadataFileXML) throws IOException, SWORDClientException {
		Entry entry = null;
		List<File> ziplist = new ArrayList<>();
		for (File dataFile : fileslist) {
			ziplist.add(dataFile);
		}
		File zipfile = new File("ingest.zip");
		String doiId = this.publishMetadata(collectionURL, metadataFileXML);
		entry=getUserAvailableMetadataset(getAtomFeed(collectionURL, authCredentials),doiId);
		IOUtils.packFilesToZip(ziplist, zipfile, ".");
		this.publishZipFile(entry.getEditMediaLinkResolvedHref().toString(), zipfile);
	}

	public ServiceDocument getServiceDocument(String serviceDocumentURL) {
		ServiceDocument serviceDocument = null;
		try {
			serviceDocument = swordClient.getServiceDocument(serviceDocumentURL, authCredentials);
		} catch (SWORDClientException | ProtocolViolationException e) {
			log.error("Exception by accessing service document: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			return null;
		}
		return serviceDocument;
	}

	public Entry getUserAvailableMetadataset(Feed feed, String doiId) {
		if(feed != null) {
			List<Entry> entries = feed.getEntries();
			Entry chosenEntry = null;
			for (Entry entry : entries) {
				if (doiId.equals(entry.getEditMediaLinkResolvedHref().toString())) {
					chosenEntry=entry;
				}
			}
			return chosenEntry;
		} else {
			log.error("Entry ID "+doiId+" was not found!");
			return null;
		}
	}
	
	public Map<String, String> getMetadataSetsWithId(Feed feed) {
		Map<String, String> entries = new HashMap<>();
		if(feed != null) {
			List<Entry> entriesInFeed = feed.getEntries();
			for (Entry entry : entriesInFeed) {
				entries.put(entry.getId().toString(), entry.getTitle());
			}
		}
		return entries;
	}
}

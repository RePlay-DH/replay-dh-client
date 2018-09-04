package bwfdm.replaydh.workflow.export.dataverse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.Response;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.abdera.protocol.client.RequestOptions;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.eclipse.jgit.util.FileUtils;
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

/**
 * 
 * @author Florian Fritze, Volodymyr Kushnarenko
 *
 */
public class DataverseRepository_v4 extends DataverseRepository {
	
	public DataverseRepository_v4(String serviceDocumentURL, char[] apiKey) {
		this.serviceDocumentURL=serviceDocumentURL;
		swordClient = new SWORDClient();
		authCredentials = new AuthCredentials(String.valueOf(apiKey), "null");
		this.apiKey=String.valueOf(apiKey);
	}
	
	// For SWORD
	private String serviceDocumentURL;
			
	private AuthCredentials authCredentials = null;
	
	private String apiKey;
	
	/**
	 * Check if SWORDv2-protocol is accessible
	 * @return
	 */
	public boolean isSwordAccessible() {
		if(this.getServiceDocument(this.serviceDocumentURL) != null) {
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
	protected String publishElement(String collectionURL, SwordRequestType swordRequestType, String mimeFormat, String packageFormat, File file, Map<String, List<String>> metadataMap) {
		
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
				for(Map.Entry<String, List<String>> metadataEntry : metadataMap.entrySet()) {
					for (String property: metadataEntry.getValue()) {
						ep.addDublinCore(metadataEntry.getKey(), property);
					}
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


	public boolean uploadZipFile(String metadataSetHrefURL, File zipFile) throws IOException {
		String packageFormat = getPackageFormat(zipFile.getName()); //zip-archive or separate file
		String returnValue = publishElement(metadataSetHrefURL, SwordRequestType.DEPOSIT, MIME_FORMAT_ZIP, packageFormat, zipFile, null);
		FileUtils.delete(zipFile);
		if (returnValue != null) {
			return true;
		} else {
			return false;
		}
	}

	public String uploadMetadata(String collectionURL, File fileFullPath, Map<String, List<String>> metadataMap) {
		String returnValue = null;
		if ((metadataMap == null) && (fileFullPath != null)) {
			returnValue = publishElement(collectionURL, SwordRequestType.DEPOSIT, MIME_FORMAT_ATOM_XML, null, fileFullPath, null);
		} else if ((metadataMap != null) && (fileFullPath == null)) {
			returnValue = publishElement(collectionURL, SwordRequestType.DEPOSIT, MIME_FORMAT_ATOM_XML, null, null, metadataMap);
		}
		if(returnValue != null) {
			return returnValue;
		} else {
			log.error("No return value from publishElement method");
			return null;
		}
	}

	public boolean uploadNewMetadataAndFile(String collectionURL, File zipFile, File metadataFileXML, Map<String, List<String>> metadataMap) throws IOException, SWORDClientException {
		Entry entry = null;
		String doiId = this.uploadMetadata(collectionURL, metadataFileXML, metadataMap);
		int beginDOI=doiId.indexOf("doi:");
		int end=doiId.length();
		entry=getUserAvailableMetadataset(getAtomFeed(collectionURL, authCredentials),doiId.substring(beginDOI, end));
		return this.uploadZipFile(entry.getEditMediaLinkResolvedHref().toString(), zipFile);
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
				int beginDOI=entry.getEditMediaLinkResolvedHref().toString().indexOf("doi:");
				int end=entry.getEditMediaLinkResolvedHref().toString().length();
				if (doiId.equals(entry.getEditMediaLinkResolvedHref().toString().substring(beginDOI, end))) {
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

	@Override
	public Map<String, String> getDatasetsInDataverseCollection(String chosenCollection) throws MalformedURLException, SWORDClientException {
		return getMetadataSetsWithId(getAtomFeed(chosenCollection, authCredentials));
	}

	@Override
	public String getJSONMetadata(String doiUrl) throws SWORDClientException, IOException {
		if (doiUrl == null)
        {
            log.error("Null doiUrl passed in to getJSONMetadata; returning null");
            return null;
        }
        if (log.isDebugEnabled())
        {
            log.debug("Showing JSON object contents from " + doiUrl);
        }

        AbderaClient client = new AbderaClient(this.abdera);
        RequestOptions options = new RequestOptions();
        options.setHeader("X-Dataverse-key", apiKey);

        // ensure that the URL is valid
        URL url = this.formaliseURL(doiUrl);
        if (log.isDebugEnabled())
        {
            log.debug("Formalised Collection URL to " + url.toString());
        }

        // make the request for the service document
        if (log.isDebugEnabled())
        {
           log.debug("Connecting to Server to get JSON Object " + url.toString() + " ...");
        }
        ClientResponse resp = client.get(url.toString(), options);
        if (log.isDebugEnabled())
        {
            log.debug("Successfully retrieved JSON Object from " + url.toString());
        }

        // if the response is successful, get the JSON object out of the response,
        // and return it as String
        if (resp.getType() == Response.ResponseType.SUCCESS)
        {
            log.info("Successfully retrieved JSON Object from " + url.toString());
            return this.getStringFromInputStream(resp.getInputStream());
        }

        // if we don't get anything respond with null
        log.warn("Unable to retrieve JSON Object from " + url.toString() + "; responded with " + resp.getStatus()
                + ". Possible problem with Dataverse API, or URL");
        return null;		
	}
	
	/**
	 * Copied from class SWORDClient
	 * @param url
	 * @return
	 * @throws SWORDClientException
	 */
	private URL formaliseURL(String url)
            throws SWORDClientException
    {
        try
        {
            URL nurl = new URL(url);
            return nurl;
        }
        catch (MalformedURLException e)
        {
            // No dice, can't even form base URL...
            throw new SWORDClientException(url + " is not a valid URL ("
                    + e.getMessage()
                    + ")");
        }
    }
	
	
	/**
	 * Copied from https://www.mkyong.com/java/how-to-convert-inputstream-to-string-in-java/
	 * @param is
	 * @return
	 */
	private String getStringFromInputStream(InputStream is) {

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try {

			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return sb.toString();

	}

	@Override
	public boolean replaceMetadata(String doiUrl, File zipFile, File metadataFileXML, Map<String, List<String>> metadataMap) throws IOException, SWORDClientException {
		String returnValue = null;
		if (metadataMap != null)  {
			returnValue = publishElement(doiUrl, SwordRequestType.REPLACE, MIME_FORMAT_ATOM_XML, null, null, metadataMap);
		}
		if(returnValue != null) {
			return true;
		} else {
			log.error("No return value from publishElement method");
			return false;
		}
	}

	@Override
	public boolean replaceMetadataAndAddFile(String collectionURL, String doiUrl, File zipFile, File metadataFileXML, Map<String, List<String>> metadataMap) throws IOException, SWORDClientException {
		Entry entry = null;
		int beginDOI=doiUrl.indexOf("doi:");
		int end=doiUrl.length();
		entry=getUserAvailableMetadataset(getAtomFeed(collectionURL, authCredentials),doiUrl.substring(beginDOI, end));
		replaceMetadata(doiUrl, null, null, metadataMap);
		return uploadZipFile(entry.getEditMediaLinkResolvedHref().toString(), zipFile);
	}
}

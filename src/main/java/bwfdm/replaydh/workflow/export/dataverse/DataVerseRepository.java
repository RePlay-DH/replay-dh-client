package bwfdm.replaydh.workflow.export.dataverse;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Feed;
import org.apache.abdera.protocol.Response;
import org.apache.abdera.protocol.client.AbderaClient;
import org.apache.abdera.protocol.client.ClientResponse;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.client.AuthCredentials;
import org.swordapp.client.SWORDClient;
import org.swordapp.client.SWORDClientException;
import org.swordapp.client.SWORDCollection;
import org.swordapp.client.SWORDWorkspace;
import org.swordapp.client.ServiceDocument;
import org.swordapp.client.UriRegistry;

import bwfdm.replaydh.workflow.export.dataverse.PublicationRepository;

public abstract class DataVerseRepository implements PublicationRepository {
	
	private static final Logger log = LoggerFactory.getLogger(DataVerseRepository.class);
	
	// Header constants
	public static final String APPLICATION_JSON = "application/json";
	public static final String CONTENT_TYPE_HEADER = "Content-Type";
	public static final String ACCEPT_HEADER = "Accept";
	
	private Abdera abdera =  new Abdera();
	
	
	/*
	 * -------------------------------
	 * General purpose methods
	 * -------------------------------
	 */
	
	
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
	public static AuthCredentials getNewAuthCredentials(String adminUser, String adminPassword, String userLogin) {
		
		if(adminUser.equals(userLogin)) {
			return new AuthCredentials(userLogin, String.valueOf(adminPassword)); // without "on-behalf-of"
		} else {
			return new AuthCredentials(adminUser, String.valueOf(adminPassword), userLogin); // with "on-behalf-of"
		}
	}

	/**
	 * Get service document via SWORD v2
	 * 
	 * @param swordClient
	 * @param serviceDocumentURL
	 * @param authCredentials
	 * @return ServiceDocument or null in case of error/exception
	 */
	public abstract ServiceDocument getServiceDocument(SWORDClient swordClient, String serviceDocumentURL, AuthCredentials authCredentials);
	
	
	/**
	 * Get available collections via SWORD v2
	 * 
	 * @return Map<String, String> where key=URL, value=Title
	 */
	public static Map<String, String> getAvailableCollectionsViaSWORD(ServiceDocument serviceDocument){
		Map<String, String> collections = new HashMap<String, String>();
		
		if(serviceDocument != null) {
			for(SWORDWorkspace workspace : serviceDocument.getWorkspaces()) {
				for (SWORDCollection collection : workspace.getCollections()) {
					// key = full URL, value = Title
					collections.put(collection.getHref().toString(), collection.getTitle());
				}
			}
		}
		return collections;
	}
	
	
	/**
	 * Get a file extension (without a dot) from the file name (e.g. "txt", "zip", ...)
	 * @param fileName
	 * @return
	 */
	public static String getFileExtension(String fileName) {	
		String extension = "";
		int i = fileName.lastIndexOf('.');
		if(i>0) {
			extension = fileName.substring(i+1);
		}
		return extension;		
	}
	
	
	/**
	 * Get package format basing on the file name.
	 * E.g. {@link UriRegistry.PACKAGE_SIMPLE_ZIP} {@link UriRegistry.PACKAGE_BINARY}
	 * @param fileName
	 * @return 
	 */
	public static String getPackageFormat(String fileName) {
		String extension = getFileExtension(fileName);
		
		if(extension.toLowerCase().equals("zip")) {
			return UriRegistry.PACKAGE_SIMPLE_ZIP;
		}
		return UriRegistry.PACKAGE_BINARY;
	}
		
	
	/*
	 * -------------
	 * Extra classes
	 * -------------
	 */
	
	
	public static enum SwordRequestType {	
		DEPOSIT("DEPOSIT"), //"POST" request
		REPLACE("REPLACE"), //"PUT" request
		DELETE("DELETE")	//reserved for the future
		;
		
		private final String label;
		
		private SwordRequestType(String label) {
			this.label = label;
		}
		
		public String getLabel() {
			return label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}	
	
	public Feed getAtomFeed(String sdURL, AuthCredentials auth) throws SWORDClientException, MalformedURLException
    {
        // do some error checking and validations
        if (sdURL == null)
        {
            log.error("Null string passed in to getAtomFeed; returning null");
            return null;
        }
        if (log.isDebugEnabled())
        {
            log.debug("getting Atom Feed from " + sdURL);
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
        URL url = new URL(sdURL);
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
            log.debug("Successfully retrieved Service Document from " + url.toString());
        }

        // if the response is successful, get the Atom Feed out of the response
        if (resp.getType() == Response.ResponseType.SUCCESS)
        {
            log.info("Retrieved Service Document from " + url.toString() + " with HTTP success code");
            Document<Feed> doc = resp.getDocument();
            Feed sd = doc.getRoot();
            return sd;
        }

        // if we don't get anything respond with null
        log.warn("Unable to retrieve service document from " + url.toString() + "; responded with " + resp.getStatus()
                + ". Possible problem with SWORD server, or URL");
        return null;
    }
}

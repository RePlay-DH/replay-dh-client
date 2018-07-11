package bwfdm.replaydh.workflow.export.dataverse;

/*
 * Unless expressly otherwise stated, code from this project is licensed under the MIT license [https://opensource.org/licenses/MIT].
 * 
 * Copyright (c) <2018> <Markus Gärtner, Volodymyr Kushnarenko, Florian Fritze, Sibylle Hermann and Uli Hahn>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A 
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

import java.io.File;

/**
 * General Interface for the publication repository.
 * 
 * @author sk, vk
 */

import java.util.Map;

public interface PublicationRepository {

	/**
	 * Check if publication repository is accessible via API
	 * 
	 * @return
	 */
	public boolean isAccessible();
	
	/**
	 * Set login and password of the user.
	 * Password is needed for the communication with the publication repository via API (e.g. SWORD or REST)
	 * <p>
	 * If the publication repository is DSpace you should put login/password ONLY of the admin-user.
	 * Credentials of the admin-user will be used for the REST/SWORD mechanism. 
	 * This mechanism is needed because of limitations of DSpace-API, where password is always needed.   
	 * <p>
	 *   
	 * @param user
	 * @param password
	 */
	public void setCredentials(String user, char[] password);
	
	/**
	 * Check if user is registered in the publication repository
	 * 
	 * @param loginName
	 * @return
	 */
	public boolean isUserRegistered(String loginName);
	
	/**
	 * Check if user is assigned to publish something in the repository
	 *
	 * @param loginName
	 * @return {@code true} if count of user available collections is great than zero, 
	 * 		   otherwise {@code false} 
 	 */
	public boolean isUserAssigned(String loginName);
		
	/**
	 * Get collections, which are available for the user
	 * Could be, that user has an access only for some specific collections.
	 *  
	 * @param loginName
	 * @return Map of Strings, where key="Collection full URL", value="Collection title", or empty Map if there are not available collections.
	 */
	public Map<String, String> getUserAvailableCollectionsWithTitle();
	
	/**
	 * Get collections, which are available for the user, and show their full name
	 * (e.g. for DSpace-repository it means "community/subcommunity/collection")
	 * <p>
	 * Could be, that user has an access only for some specific collections.
	 *  
	 * @param loginName, nameSeparator
	 * @return Map of Strings, where key="Collection full URL", value="Collection full name", or empty Map if there are not available collections.
	 */
	public Map<String, String> getUserAvailableCollectionsWithFullName(String loginName, String fullNameSeparator);
	
	/**
	 * Get available for the admin-user collections for publication.
	 * As credentials for the request are used login/password of the admin-user
	 * 
	 * @return Map of Strings, where key="Collection full URL", value="Collection title", or empty Map if there are not available collections.
	 */
	public Map<String, String> getAdminAvailableCollectionsWithTitle();
	
	/**
	 * Get available for the admin-user collections with full name
	 * (e.g. for DSpace-repository it means "community/subcommunity/collection")
	 * <p>
	 * As credentials for the request are used login/password of the admin-user
	 *  
	 * @param fullNameSeparator 
	 * @return Map of Strings, where key="Collection full URL", value="Collection full name", or empty Map if there are not available collections. 
	 */
	public Map<String, String> getAdminAvailableCollectionsWithFullName(String fullNameSeparator);
	
	/**
	 * Publish a file to some collections, which is available for the user.
	 * 
	 * @param userLogin
	 * @param collectionURL
	 * @param fileFullPath
	 * @return
	 */
	public boolean publishFile(String userLogin, String collectionURL, File fileFullPath);
	
	/**
	 * Publish metada only (without any file) to some collection, which is available for the user.
	 * Metadata are described as a {@link java.util.Map}. 
	 *  
	 * @param userLogin
	 * @param collectionURL
	 * @param metadataMap
	 * @return
	 */
	public boolean publishMetadata(String userLogin, String collectionURL, Map<String, String> metadataMap);
		
	/**
	 * Publish metada only (without any file) to some collection, which is available for the user.
	 * Metadata are described in the xml-file.
	 * 
	 * @param userLogin
	 * @param collectionURL
	 * @param metadataFileXML
	 * @return
	 */
	public boolean publishMetadata(String userLogin, String collectionURL, File metadataFileXML);
	
	/**
	 * Publish a file together with the metadata.
	 * Metadata are described as a {@link java.util.Map}. 
	 * 
	 * @param userLogin
	 * @param collectionURL
	 * @param fileFullPath
	 * @param metadataMap
	 * @return
	 */
	public boolean publishFileAndMetadata(String userLogin, String collectionURL, File fileFullPath, Map<String, String> metadataMap);
	
	/**
	 * Publish a file together with the metadata.
	 * Metadata are described in the xml-file.
	 * 
	 * @param userLogin
	 * @param collectionURL
	 * @param fileFullPath
	 * @param metadataFileXML
	 * @return
	 */
	public boolean publishFileAndMetadata(String userLogin, String collectionURL, File fileFullPath, File metadataFileXML);
	
}


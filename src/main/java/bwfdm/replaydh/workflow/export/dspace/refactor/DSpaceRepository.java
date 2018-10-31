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
package bwfdm.replaydh.workflow.export.dspace.refactor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.client.AuthCredentials;

import bwfdm.replaydh.workflow.export.generic.SwordExporter;

/**
 * The class consists implementation of some general purpose DSpace-specific methods.
 * It should be used as a parent for further child-classes as e.g. DSpace_v6, Dspace_v5.
 * 
 * @author Volodymyr Kushnarenko
 *
 */
public abstract class DSpaceRepository extends SwordExporter implements ExportRepository{

	protected DSpaceRepository(AuthCredentials authCredentials) {
		super(authCredentials);
	}


	private static final Logger log = LoggerFactory.getLogger(DSpaceRepository.class);
		
	
	/*
	 * -------------------------------
	 * General DSpace specific methods
	 * -------------------------------
	 */
	
	
	/**
	 * Get a list of communities for the collection Specific only for DSpace-6.
	 *  
	 * @param collectionURL - URL of the collection as {@link String}
	 * 
	 * @return a {@code List<String>} of communities (0 or more communities are
	 *         possible) or {@code null} if the collection was not found
	 */
	public abstract List<String> getCommunitiesForCollection(String collectionURL);
	
	
	/**
	 * Get collections, which are available for the current authentication credentials, and show their full name
	 * (e.g. for DSpace-repository it means "community/subcommunity/collection", where '/' is the full name separator)
	 * <p>
	 * Could be, that the current credentials have an access only for some specific collections.
	 * <p>
	 * IMPORTANT: credentials are used implicitly. Definition of the credentials must be done in other place, e.g. via class constructor.
	 *  
	 * @param nameSeparator
	 * @return Map of Strings, where key="Collection full URL", value="Collection full name", or empty Map if there are not available collections.
	 */
	public abstract Map<String, String> getAvailableCollectionsWithFullName(String fullNameSeparator);

	
	/**
	 * Create a new item with a file only (without metadata) in some collection, which is available for the current authentication credentials.
	 * <p>
	 * IMPORTANT: credentials are used implicitly. Definition of the credentials must be done in other place, e.g. via class constructor.
	 * 
	 * @param collectionURL
	 * @param file
	 * @return {@code true} in case of success and {@code false} otherwise 
	 */
	// TODO: return String with the new item URL in case of success or "" otherwise
	public abstract boolean createNewEntryWithFile(String collectionURL, File file) throws IOException;
			
}

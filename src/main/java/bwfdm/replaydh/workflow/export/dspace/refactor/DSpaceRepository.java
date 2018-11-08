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

import java.util.List;
import java.util.Map;

import bwfdm.replaydh.workflow.export.generic.ExportRepository;

/**
 * DSpace-specific common methods.
 * Implementation of the interface should be done in further classes such as e.g. DSpace_v6, DSpace_v5.
 * 
 * @author Volodymyr Kushnarenko
 */
public interface DSpaceRepository extends ExportRepository {
	
	/**
	 * Get a list of communities for the provided collection.
	 *  
	 * @param collectionURL - a {@link String} with the URL of the collection. 
	 * 			<b>IMPORTANT:</b> implementation of the method should take into consideration 
	 * 			if URL is for SWORD (e.g. substring "/swordv2/collection/" inside) 
	 * 			or for REST API (e.g. "/rest/collections/" substring inside).   
	 * 
	 * @return a {@code List<String>} of communities (0 or more communities are
	 *         	possible) or {@code null} in case of error.
	 */
	public List<String> getCommunitiesForCollection(String collectionURL);
	
	
	/**
	 * Get collections, which are available for the current authentication credentials, and show their full name
	 * (e.g. for DSpace-repository it means "community/subcommunity/collection", where "/" is the fullNameSeparator)
	 * <p>
	 * Could be, that the current credentials have an access only for some specific collections.
	 * <p>
	 * <b>IMPORTANT:</b> credentials are used implicitly. Definition of the credentials must be done in other place, e.g. via class constructor.
	 *  
	 * @param fullNameSeparator a {@link String} separator between collections and communities (e.g. "/"). 
	 * 			It could be also used as a separator for further parsing of the the collection's full name. 
	 * @return Map of Strings, where key = "collection full URL", value = "collection full name" 
	 * 			(it could be also empty if there are not available collections) or {@code null} in case of error.
	 */
	public Map<String, String> getAvailableCollectionsWithFullName(String fullNameSeparator);

}

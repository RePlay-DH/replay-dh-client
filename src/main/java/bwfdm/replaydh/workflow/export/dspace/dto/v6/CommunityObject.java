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
package bwfdm.replaydh.workflow.export.dspace.dto.v6;

import java.util.List;

/**
 * 
 * @author vk
 */
public class CommunityObject {

	public String uuid;
	public String name;
	public String handle;
	public String type;
	public String link;
	public List<String> expand;
	public String logo;
	public String parentCommunity;
	public String copyrightText;
	public String introductoryText;
	public String shortDescription;
	public String sidebarText;
	public String countItems;
	public List<String> subcommunities;
	public List<String> collections;

	// "uuid":"122b7dfd-7814-40fb-b98f-fe3f968c9680",
	// "name":"Fakultät für Ingenieurwissenschaften, Informatik und Psychologie",
	// "handle":"123456789/82",
	// "type":"community",
	// "expand":["parentCommunity","collections","subCommunities","logo","all"],
	// "logo":null,
	// "parentCommunity":null,
	// "copyrightText":"",
	// "introductoryText":"",
	// "shortDescription":"",
	// "sidebarText":"",
	// "countItems":59,
	// "collections":[],
	// "link":"/rest/communities/122b7dfd-7814-40fb-b98f-fe3f968c9680",
	// "subcommunities":[]}
	//
	//
	// {
	// "uuid":456,
	// "name":"Reports Community",
	// "handle":"10766/10213",
	// "type":"community",
	// "link":"/rest/communities/456",
	// "expand":["parentCommunity","collections","subCommunities","logo","all"],
	// "logo":null,
	// "parentCommunity":null,
	// "copyrightText":"",
	// "introductoryText":"",
	// "shortDescription":"Collection contains materials pertaining to the Able Family",
	// "sidebarText":"",
	// "countItems":3,
	// "subcommunities":[],
	// "collections":[]
	// }

}

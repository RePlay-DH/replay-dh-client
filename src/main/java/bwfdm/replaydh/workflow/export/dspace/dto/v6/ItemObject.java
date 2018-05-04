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
public class ItemObject {

	public String uuid;
	public String name;
	public String handle;
	public String type;
	public String link;
	public List<String> expand;
	public String lastModified;
	public String parentCollection;
	public List<String> parentCollectionList;
	public List<String> parentCommunityList;
	public String bitstreams;
	public String archived;
	public String withdrawn;
	public String metadata;

	// {
	// "uuid" : "6d408a16-6ab9-41d1-974b-373c06d5d63d",
	// "name" :
	// "Gestational Weight Gain and Fetal-Maternal Adiponectin, Leptin, and CRP: results of two birth cohorts studies",
	// "handle" : "123456789/1206",
	// "type" : "item",
	// "expand" : [ "metadata", "parentCollection", "parentCollectionList",
	// "parentCommunityList", "bitstreams", "all" ],
	// "lastModified" : "2017-03-23 11:15:28.61",
	// "parentCollection" : null,
	// "parentCollectionList" : null,
	// "parentCommunityList" : null,
	// "bitstreams" : null,
	// "archived" : "true",
	// "withdrawn" : "false",
	// "link" : "/rest/items/6d408a16-6ab9-41d1-974b-373c06d5d63d",
	// "metadata" : null
	// }
	//
	// {
	// "id":14301,
	// "name":"2015 Annual Report",
	// "handle":"123456789/13470",
	// "type":"item",
	// "link":"/rest/items/14301",
	// "expand":["metadata","parentCollection","parentCollectionList","parentCommunityList","bitstreams","all"],
	// "lastModified":"2015-01-12 15:44:12.978",
	// "parentCollection":null,
	// "parentCollectionList":null,
	// "parentCommunityList":null,
	// "bitstreams":null,
	// "archived":"true",
	// "withdrawn":"false"
	// }

}

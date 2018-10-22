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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.client.AuthCredentials;

import bwfdm.replaydh.workflow.export.generic.refactor.ExportRepository;
import bwfdm.replaydh.workflow.export.generic.refactor.SwordRepositoryExporter;

/**
 * The class consists implementation of some general purpose DSpace-specific methods.
 * It should be used as a parent for further child-classes as e.g. DSpace_v6, Dspace_v5.
 * 
 * @author Volodymyr Kushnarenko
 *
 */
public abstract class DSpaceRepository extends SwordRepositoryExporter implements ExportRepository{

	protected DSpaceRepository(AuthCredentials authCredentials) {
		super(authCredentials);
	}


	private static final Logger log = LoggerFactory.getLogger(DSpaceRepository.class);
		
	
	/*
	 * -------------------------------
	 * General DSpace specific methods
	 * -------------------------------
	 */
	

	// TODO: put here some common DSpace specific methods... 
	
	
	
		
}

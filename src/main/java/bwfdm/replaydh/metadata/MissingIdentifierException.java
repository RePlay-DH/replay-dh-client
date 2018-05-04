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
package bwfdm.replaydh.metadata;

import static bwfdm.replaydh.utils.RDHUtils.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Markus Gärtner
 *
 */
public class MissingIdentifierException extends MetadataException {

	private static final long serialVersionUID = 3216645732829156394L;

	private final Set<String> supportedIdentifierTypes;

	/**
	 * @param message
	 */
	public MissingIdentifierException(String message, Set<String> supportedIdentifierTypes) {
		super("Insufficient identifiers defined for resource");

		this.supportedIdentifierTypes = Collections.unmodifiableSet(requireNonNull(supportedIdentifierTypes));
	}


	public MissingIdentifierException(String message, String...supportedIdentifierTypes) {
		super("Insufficient identifiers defined for resource");

		checkArgument("No supported identifier type provided", supportedIdentifierTypes.length>0);

		Set<String> tmp = new HashSet<>();
		for(String supportedIdentifierType : supportedIdentifierTypes) {
			tmp.add(supportedIdentifierType);
		}

		this.supportedIdentifierTypes = Collections.unmodifiableSet(tmp);
	}

	/**
	 * Returns the collection of identifier types supported by the entity
	 * that threw this exception.
	 * <p>
	 * Client code can use this information to either compute the missing
	 * identifiers or ask the user to provide them.
	 *
	 * @return
	 */
	public Set<String> getSupportedIdentifierTypes() {
		return supportedIdentifierTypes;
	}
}

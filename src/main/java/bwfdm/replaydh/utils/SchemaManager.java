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
package bwfdm.replaydh.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Markus Gärtner
 *
 */
public interface SchemaManager<S extends SchemaManager.Schema> {

	default Set<S> getAvailableSchemas() {
		Set<String> schemaIds = getAvailableSchemaIds();
		if(schemaIds==null) {
			return Collections.emptySet();
		}

		Set<S> schemas = new HashSet<>(schemaIds.size());
		for(String schemaId : schemaIds) {
			schemas.add(lookupSchema(schemaId));
		}

		return schemas;
	}

	int getAvailableSchemaCount();

	Set<String> getAvailableSchemaIds();

	S lookupSchema(String schemaId);

	void addSchema(S schema);

	void removeSchema(S schema);

	void setDefaultSchema(S schema);

	S getDefaultSchema();

	public interface Schema {
		String getId();
	}
}

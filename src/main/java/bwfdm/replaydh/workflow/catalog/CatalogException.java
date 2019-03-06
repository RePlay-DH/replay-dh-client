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
package bwfdm.replaydh.workflow.catalog;

import static java.util.Objects.requireNonNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * @author Markus Gärtner
 *
 */
public class CatalogException extends Exception {

	private static final long serialVersionUID = 2133314701022590875L;

	private final Reference<MetadataCatalog> catalog;

	public CatalogException(MetadataCatalog catalog, String message, Throwable cause) {
		super(message, cause);
		this.catalog = new WeakReference<>(requireNonNull(catalog));
	}

	public CatalogException(MetadataCatalog catalog, String message) {
		super(message);
		this.catalog = new WeakReference<>(requireNonNull(catalog));
	}

	/**
	 * @return the catalog
	 */
	public MetadataCatalog getCatalog() {
		return catalog.get();
	}
}

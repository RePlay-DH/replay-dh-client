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
package bwfdm.replaydh.workflow;

/**
 *
 * @author Markus
 */
public interface Resource extends Identifiable {

    /**
     *
     * @return
     */
    String getResourceType();

    void setResourceType(String resourceType);

    /**
     * {@inheritDoc}
     *
     * In addition to the fields copied by the super method, this implementation
     * also copies over the {@link #getResourceType() resource type} if available.
     *
     * @see bwfdm.replaydh.workflow.Identifiable#copyFrom(bwfdm.replaydh.workflow.Identifiable)
     */
    @Override
	default void copyFrom(Identifiable source) {
    	Identifiable.super.copyFrom(source);

    	if(source instanceof Resource) {
    		String resourceType = ((Resource)source).getResourceType();
    		if(resourceType!=null) {
    			setResourceType(resourceType);
    		}
    	}
    }
}

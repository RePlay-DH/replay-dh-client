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
 * Models a single tool invocation during a workflow step.
 * For simplification a tool is merely an executable resource that
 * can provide an optional {@link #getParameters() parameters string}
 * and a recording of the {@link #getEnvironment() execution environment}.
 *
 * @author Markus
 */
public interface Tool extends Resource {

    /**
     * Returns the optional parameters string as it was used during the
     * command line invocation.
     * <p>
     * Note that more complex configuration mechanisms like setting files
     * should be modeled as additional input resources.
     *
     * @return
     */
    String getParameters();

    void setParameters(String parameters);

    /**
     * Returns a free-text description of the execution environment.
     * This is purely optional and may contain anything from the operating
     * system (OS) to exact processor architecture or other relevant hardware
     * information.
     *
     * It is currently not required to have this information in a standardized
     * format, so any human-readable form is fine.
     *
     * @return
     */
    String getEnvironment();

    void setEnvironment(String environment);

    /**
     * {@inheritDoc}
     *
     * In addition to the fields copied by the super method, this implementation
     * also copies over the {@link #getEnvironment() environment} and
     * {@link #getParameters() parameters} if available.
     *
     * @see bwfdm.replaydh.workflow.Resource#copyFrom(bwfdm.replaydh.workflow.Identifiable)
     */
    @Override
	default void copyFrom(Identifiable source) {
    	Resource.super.copyFrom(source);

    	if(source instanceof Tool) {
    		Tool other = (Tool) source;

    		String environment = other.getEnvironment();
    		if(environment!=null) {
    			setEnvironment(environment);
    		}

    		String parameters = other.getParameters();
    		if(parameters!=null) {
    			setParameters(parameters);
    		}
    	}
    }
}

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
package bwfdm.replaydh.workflow.export.dspace;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Volodymyr Kushnarenko
 */
public class JsonUtils {
	
	protected static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
      
    /**
     * Make pretty print of JSON.
     * 
     * @param jsonString
     * @return String or ""
     */
    public static String jsonStringPrettyPrint(String jsonString){
        
        if (jsonString.equals("")){
            return "bwfdm.sara.utils.JsonUtils.jsonStringPrettyPrint: empty string.";
        }
        ObjectMapper mapper = new ObjectMapper();
        String prettyJsonString = "";
        try {
            Object jsonObject = mapper.readValue(jsonString, Object.class);      
            prettyJsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } catch (IOException e) {
        	log.error("Exception in JSON Pretty Print: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return prettyJsonString;
    }
    
    /**
     * Convert JSON-String to the object (DTO)
     * 
     * @param <T> - any class
     * @param jsonString - input string
     * @param type - type of the class
     * @return any object, DTO
     */
    public static <T> T jsonStringToObject(String jsonString, Class<T> type){    	
    	
    	ObjectMapper mapper = new ObjectMapper();
        T obj = null;
        try{
            obj = type.cast(mapper.readValue(jsonString, type));
        }catch (IOException e) {
        	log.error("Exception in JSON-to-Object conversion: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return obj;
    }
    
    /**
     * Convert some object to JSON-String
     * 
     * @param <T> - any type of object
     * @param obj - any object
     * @return String or ""
     */
    public static <T> String objectToJsonString(T obj){
        
    	ObjectMapper mapper = new ObjectMapper();
        String jsonString = null;
        try {
            jsonString  = mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
        	log.error("Exception in Object-to-JSON conversion: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return jsonString;
    }
    
}


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

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bwfdm.replaydh.ui.GuiUtils;

/**
 * 
 * @author Volodymyr Kushnarenko
 */
public class WebUtils {

	protected static final Logger log = LoggerFactory.getLogger(WebUtils.class);

	/**
	 * Create a ClosableHttpClient ignoring SSL certificates
	 * 
	 * @return {@link CloseableHttpClient} or {@code null} in case of error
	 */
	public static CloseableHttpClient createHttpClientWithSSLSupport() {

		try {
			KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());

			SSLContextBuilder builder = new SSLContextBuilder();
			builder.loadTrustMaterial(trustStore, new TrustSelfSignedStrategy() {
				@Override
				public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					return true;
				}
			});
			SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());
			return HttpClients.custom().setSSLSocketFactory(sslsf).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
					.build();

			// return HttpClients.custom().setSSLSocketFactory(sslsf).build();

		} catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException ex) {
			log.error("Exception by creation of http-clinet with ssl support: {}: {}", ex.getClass().getSimpleName(),
					ex.getMessage());
			return null;
		}
	}

	/**
	 * Get a response to the REST-request
	 * 
	 * @param client      - object of {@link CloseableHttpClient}
	 * @param url         - URL as {@link String}
	 * @param requestType - object of {@link RequestType}
	 * @param contentType - content type as {@link String}
	 * @param acceptType  - accept type as {@link String}
	 * 
	 * @return {@link CloseableHttpResponse} or {@code null} in case of error
	 */
	public static CloseableHttpResponse getResponse(HttpClient client, String url, RequestType requestType, String contentType,
			String acceptType) {

		HttpUriRequest request;
		switch (requestType) {
		case GET:
			request = new HttpGet(url);
			break;
		case PUT:
			request = new HttpPut(url);
			break;
		case POST:
			request = new HttpPost(url);
			break;
		default:
			log.error("Not supported request type: {}", requestType.toString());
			return null;
		}

		request.addHeader("Content-Type", contentType);
		request.addHeader("Accept", acceptType);
		CloseableHttpResponse response = null;
		try {
			response = (CloseableHttpResponse) client.execute(request);
		} catch (IOException e) {
			log.error("Failed to process a http request and getting therefore no response {}", url, e);
			GuiUtils.showErrorDialog(null, e);
		}
		return response;
	}

	/**
	 * Get a response entity as a String
	 * 
	 * @param response - object of {@link CloseableHttpResponse}
	 * 
	 * @return {@link String} with response or {@code null} in case of error
	 */
	public static String getResponseEntityAsString(HttpResponse response) {
		try {
			return EntityUtils.toString(response.getEntity(), "UTF-8");
		} catch (ParseException | IOException ex) {
			log.error("Exception by converting response entity to String: {}: {}", ex.getClass().getSimpleName(),
					ex.getMessage());
			return null;
		}
	}

	/**
	 * Close the response
	 * 
	 * @param response - object of {@link CloseableHttpResponse}
	 */
	public static void closeResponse(CloseableHttpResponse response) {
		try {
			response.close();
		} catch (IOException ex) {
			log.error("Exception by response closing: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
		}
	}

	/**
	 * Possible types of request
	 * 
	 * @author Volodymyr Kushnarenko
	 */
	public static enum RequestType {
		GET("GET"), PUT("PUT"), POST("POST");

		private final String label;

		private RequestType(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}

		@Override
		public String toString() {
			return label;
		}
	}

}

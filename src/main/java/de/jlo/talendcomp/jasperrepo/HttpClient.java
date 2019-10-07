package de.jlo.talendcomp.jasperrepo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class HttpClient {
	
	private static Logger LOG = LoggerFactory.getLogger(HttpClient.class);
	private int statusCode = 0;
	private String statusMessage = null;
	private int maxRetriesInCaseOfErrors = 0;
	private int currentAttempt = 0;
	private long waitMillisAfterError = 1000l;
	private CloseableHttpClient closableHttpClient = null;
	private HttpClientContext context = null;
	private String serverUrl = null;
	
	public HttpClient(String urlStr, String user, String password, int timeout) throws Exception {
		closableHttpClient = createCloseableClient(urlStr, user, password, timeout);
	}
	
	private HttpEntity buildEntity(JsonNode node) throws UnsupportedEncodingException {
		if (node != null && node.isNull() == false && node.isMissingNode() == false) {
			HttpEntity entity = new StringEntity(node.toString(), "UTF-8");
			return entity;
		} else {
			return null;
		}
	}
	
	public String buildBase64String(String filePath) throws Exception {
		if (filePath == null || filePath.trim().isEmpty()) {
			return null;
		}
		File test = new File(filePath);
		if (test.canRead() == false) {
			throw new Exception("Read file: " + filePath + " failed. File does not exist or cannot be read.");
		}
		byte[] bytes = null;
		try {
			bytes = Files.readAllBytes(FileSystems.getDefault().getPath(filePath, ""));
		} catch (IOException e) {
			String message = "Read file: " + filePath + " failed: " + e.getMessage();
			throw new Exception(message, e);
		}
		return Base64.encodeBase64String(bytes);
	}
	
	private String execute(HttpUriRequest request, boolean expectResponse) throws Exception {
		String responseContent = "";
		currentAttempt = 0;
		for (currentAttempt = 0; currentAttempt <= maxRetriesInCaseOfErrors; currentAttempt++) {
			if (Thread.currentThread().isInterrupted()) {
				break;
			}
            CloseableHttpResponse httpResponse = null;
            try {
            	if (context != null) {
                	httpResponse = closableHttpClient.execute(request, context);
            	} else {
                	httpResponse = closableHttpClient.execute(request);
            	}
            	statusCode = httpResponse.getStatusLine().getStatusCode();
            	statusMessage = httpResponse.getStatusLine().getReasonPhrase();
            	if (expectResponse || (statusCode >= 200 && statusCode <= 209)) {
                	responseContent = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
                	if (Util.isEmpty(responseContent)) {
                		throw new Exception("Empty response received.");
                	}
            	}
            	if (statusCode > 300) {
            		responseContent = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            		throw new Exception("Got status-code: " + statusCode + ", reason-phrase: " + statusMessage + ", response: " + responseContent);
            	}
            	break;
            } catch (Throwable e) {
            	if (currentAttempt < maxRetriesInCaseOfErrors) {
                	// this can happen, we try it again
            		if (request instanceof HttpPost) {
                    	LOG.warn("POST request: " + request.getURI() + " failed (" + (currentAttempt + 1) + ". attempt, " + (maxRetriesInCaseOfErrors - currentAttempt) + " retries left). StatusCode=" + statusCode + ". \n   Payload: " + EntityUtils.toString(((HttpPost) request).getEntity()) + "\n   Waiting " + waitMillisAfterError + "ms and retry request.", e);
            		} else if (request instanceof HttpPut) {
                    	LOG.warn("PUT request: " + request.getURI() + " failed (" + (currentAttempt + 1) + ". attempt, " + (maxRetriesInCaseOfErrors - currentAttempt) + " retries left). StatusCode=" + statusCode + ". \n   Payload: " + EntityUtils.toString(((HttpPut) request).getEntity()) + "\n   Waiting " + waitMillisAfterError + "ms and retry request.", e);
            		} else if (request instanceof HttpGet) {
                    	LOG.warn("GET request: " + request.getURI() + " failed (" + (currentAttempt + 1) + ". attempt, " + (maxRetriesInCaseOfErrors - currentAttempt) + " retries left). StatusCode=" + statusCode + ". \n Waiting " + waitMillisAfterError + "ms and retry request.", e);
            		} else if (request instanceof HttpDelete) {
                    	LOG.warn("DEL request: " + request.getURI() + " failed (" + (currentAttempt + 1) + ". attempt, " + (maxRetriesInCaseOfErrors - currentAttempt) + " retries left). StatusCode=" + statusCode + ". \n Waiting " + waitMillisAfterError + "ms and retry request.", e);
            		}
                	Thread.sleep(waitMillisAfterError);
            	} else {
            		if (request instanceof HttpPost) {
            			String message = "POST request: " + request.getURI() + " failed. StatusCode=" + statusCode + ". No retry left, max: " + maxRetriesInCaseOfErrors + ".\n   Payload: " + EntityUtils.toString(((HttpPost) request).getEntity()); 
                    	throw new Exception(message, e);
            		} else if (request instanceof HttpPut) {
            			String message = "PUT request: " + request.getURI() + " failed. StatusCode=" + statusCode + ". No retry left, max: " + maxRetriesInCaseOfErrors + ".\n   Payload: " + EntityUtils.toString(((HttpPut) request).getEntity()); 
                    	throw new Exception(message, e);
            		} else if (request instanceof HttpGet) {
            			String message = "GET request: " + request.getURI() + " failed. StatusCode=" + statusCode + ". No retry left, max: " + maxRetriesInCaseOfErrors + "."; 
                    	throw new Exception(message, e);
            		} else if (request instanceof HttpDelete) {
            			String message = "DEL request: " + request.getURI() + " failed. StatusCode=" + statusCode + ". No retry left, max: " + maxRetriesInCaseOfErrors + "."; 
                    	throw new Exception(message, e);
            		}
            	}
            } finally {
            	if (httpResponse != null) {
                	try {
                    	httpResponse.close();
                	} catch (Exception ce) {
                		// ignore
                	}
            	}
            }
		} // for
        return responseContent;
	}

	public String post(String urlStr, JsonNode node, boolean expectResponse) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("POST " + urlStr + " body: " + node.toString());
		}
        HttpPost request = new HttpPost(urlStr);
        request.getConfig();
        if (node != null) {
            request.setEntity(buildEntity(node));
            request.addHeader("Connection", "Keep-Alive");
            request.addHeader("Accept", "application/json");
            request.addHeader("Content-Type", "application/json;charset=UTF-8");
            request.addHeader("Keep-Alive", "timeout=5, max=0");
        }
        return execute(request, expectResponse);
	}

	public String put(String urlStr, JsonNode node, boolean expectResponse) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("PUT " + urlStr + " body: " + node.toString());
		}
        HttpPost request = new HttpPost(urlStr);
        request.getConfig();
        if (node != null) {
            request.setEntity(buildEntity(node));
            request.addHeader("Connection", "Keep-Alive");
            request.addHeader("Accept", "application/json");
            request.addHeader("Content-Type", "application/json;charset=UTF-8");
            request.addHeader("Keep-Alive", "timeout=5, max=0");
        }
        return execute(request, expectResponse);
	}

	public String get(String urlStr) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("GET " + urlStr);
		}
        HttpGet request = new HttpGet(urlStr);
        request.addHeader("Connection", "Keep-Alive");
        request.addHeader("Accept", "application/json");
        request.addHeader("Content-Type", "application/json;charset=UTF-8");
        request.addHeader("Keep-Alive", "timeout=5, max=0");
        return execute(request, true);
	}

	public String delete(String urlStr) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("DEL " + urlStr);
		}
        HttpDelete request = new HttpDelete(urlStr);
        request.addHeader("Connection", "Keep-Alive");
        request.addHeader("Accept", "application/json");
        request.addHeader("Content-Type", "application/json;charset=UTF-8");
        request.addHeader("Keep-Alive", "timeout=5, max=0");
        return execute(request, true);
	}

	public boolean exist(String urlStr) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("GET " + urlStr);
		}
        HttpGet request = new HttpGet(urlStr);
        request.addHeader("Connection", "Keep-Alive");
        request.addHeader("Accept", "application/json");
        request.addHeader("Content-Type", "application/json;charset=UTF-8");
        request.addHeader("Keep-Alive", "timeout=5, max=0");
        try {
        	execute(request, false);
        	return true;
        } catch (Exception e) {
        	if (e.getMessage().contains("404")) {
        		return false;
        	} else {
        		throw e;
        	}
        }
	}

	public void download(String urlStr, String targetFilePath) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("GET " + urlStr + " -> download to: " + targetFilePath);
		}
        HttpGet request = new HttpGet(urlStr);
        request.getConfig();
        request.addHeader("Connection", "Keep-Alive");
        request.addHeader("Keep-Alive", "timeout=5, max=0");
        executeDownload(request, targetFilePath);
	}

	private void executeDownload(HttpUriRequest request, String targetFilePath) throws Exception {	
		currentAttempt = 0;
		for (currentAttempt = 0; currentAttempt <= maxRetriesInCaseOfErrors; currentAttempt++) {
			if (Thread.currentThread().isInterrupted()) {
				break;
			}
            CloseableHttpResponse httpResponse = null;
            try {
            	if (context != null) {
                	httpResponse = closableHttpClient.execute(request, context);
            	} else {
                	httpResponse = closableHttpClient.execute(request);
            	}
            	statusCode = httpResponse.getStatusLine().getStatusCode();
            	statusMessage = httpResponse.getStatusLine().getReasonPhrase();
            	if (statusCode >= 200 && statusCode <= 209) {
            		HttpEntity entity = httpResponse.getEntity();
            		if (entity != null) {
            			try (FileOutputStream outstream = new FileOutputStream(targetFilePath)) {
            	            entity.writeTo(outstream);
            	        }
            		}
            	}
            	try {
                	httpResponse.close();
                	httpResponse = null;
            	} catch (Exception ce) {
            		// ignore
            	}
            	if (statusCode > 300) {
            		throw new Exception("Got status-code: " + statusCode + ", reason-phrase: " + statusMessage);
            	}
            	break;
            } catch (Throwable e) {
            	if (currentAttempt < maxRetriesInCaseOfErrors) {
                	// this can happen, we try it again
            		if (request instanceof HttpPost) {
                    	LOG.warn("POST request: " + request.getURI() + " failed (" + (currentAttempt + 1) + ". attempt, " + (maxRetriesInCaseOfErrors - currentAttempt) + " retries left). \n   Payload: " + EntityUtils.toString(((HttpPost) request).getEntity()) + "\n   Waiting " + waitMillisAfterError + "ms and retry request.", e);
            		} else if (request instanceof HttpPut) {
                    	LOG.warn("PUT request: " + request.getURI() + " failed (" + (currentAttempt + 1) + ". attempt, " + (maxRetriesInCaseOfErrors - currentAttempt) + " retries left). \n   Payload: " + EntityUtils.toString(((HttpPut) request).getEntity()) + "\n   Waiting " + waitMillisAfterError + "ms and retry request.", e);
            		} else if (request instanceof HttpGet) {
                    	LOG.warn("GET request: " + request.getURI() + " failed (" + (currentAttempt + 1) + ". attempt, " + (maxRetriesInCaseOfErrors - currentAttempt) + " retries left). \n Waiting " + waitMillisAfterError + "ms and retry request.", e);
            		} else if (request instanceof HttpDelete) {
                    	LOG.warn("DEL request: " + request.getURI() + " failed (" + (currentAttempt + 1) + ". attempt, " + (maxRetriesInCaseOfErrors - currentAttempt) + " retries left). \n Waiting " + waitMillisAfterError + "ms and retry request.", e);
            		}
                	Thread.sleep(waitMillisAfterError);
            	} else {
            		if (request instanceof HttpPost) {
            			String message = "POST request: " + request.getURI() + " failed. No retry left, max: " + maxRetriesInCaseOfErrors + ".\n   Payload: " + EntityUtils.toString(((HttpPost) request).getEntity()); 
                    	LOG.error(message, e);
                    	throw new Exception(message, e);
            		} else if (request instanceof HttpPut) {
            			String message = "PUT request: " + request.getURI() + " failed. No retry left, max: " + maxRetriesInCaseOfErrors + ".\n   Payload: " + EntityUtils.toString(((HttpPut) request).getEntity()); 
                    	LOG.error(message, e);
                    	throw new Exception(message, e);
            		} else if (request instanceof HttpGet) {
            			String message = "GET request: " + request.getURI() + " failed. No retry left, max: " + maxRetriesInCaseOfErrors + "."; 
                    	LOG.error(message, e);
                    	throw new Exception(message, e);
            		} else if (request instanceof HttpDelete) {
            			String message = "DEL request: " + request.getURI() + " failed. No retry left, max: " + maxRetriesInCaseOfErrors + "."; 
                    	LOG.error(message, e);
                    	throw new Exception(message, e);
            		}
            	}
            } finally {
            	if (httpResponse != null) {
                	try {
                    	httpResponse.close();
                	} catch (Exception ce) {
                		// ignore
                	}
            	}
            }
		} // for
	}

	private CloseableHttpClient createCloseableClient(String serverUrl, String user, String password, int timeout) throws Exception {
		if (serverUrl == null || serverUrl.trim().isEmpty()) {
			throw new IllegalArgumentException("serverUrl cannot be null or empty.");
		}
		serverUrl = serverUrl.trim();
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        if (closableHttpClient == null) {
            if (user != null && user.trim().isEmpty() == false) {
        		URL url = new URL(serverUrl);
                credsProvider.setCredentials(
                        AuthScope.ANY,
                        new UsernamePasswordCredentials(user, password));
                RequestConfig requestConfig = RequestConfig.custom()
                        .setSocketTimeout(timeout)
                        .setConnectTimeout(timeout)
                        .setConnectionRequestTimeout(timeout)
                        .setRedirectsEnabled(true)
                        .setRelativeRedirectsAllowed(false)
                        .setAuthenticationEnabled(true)
                        .build();
                AuthCache authCache = new BasicAuthCache();
                HttpHost httpHost = new HttpHost(url.getHost(), url.getPort());
                authCache.put(httpHost, new BasicScheme());
                context = HttpClientContext.create();
                context.setCredentialsProvider(credsProvider);
                context.setAuthCache(authCache);
                CloseableHttpClient client = HttpClients.custom()
                        .setDefaultCredentialsProvider(credsProvider)
                        .setDefaultRequestConfig(requestConfig)
                        .build();
            	closableHttpClient = client;
            	if (serverUrl.endsWith("/")) {
                	this.serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
            	} else {
                	this.serverUrl = serverUrl;
            	}
                return client;
            } else {
                RequestConfig requestConfig = RequestConfig.custom()
                        .setSocketTimeout(timeout)
                        .setConnectTimeout(timeout)
                        .setConnectionRequestTimeout(timeout)
                        .setRedirectsEnabled(true)
                        .setRelativeRedirectsAllowed(true)
                        .build();
                CloseableHttpClient client = HttpClients.custom()
                        .setDefaultRequestConfig(requestConfig)
                        .build();
            	closableHttpClient = client;
            	this.serverUrl = serverUrl;
                return client;
            }
        } else {
        	return closableHttpClient;
        }
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public int getMaxRetriesInCaseOfErrors() {
		return maxRetriesInCaseOfErrors;
	}

	public void setMaxRetriesInCaseOfErrors(Integer maxRetriesInCaseOfErrors) {
		if (maxRetriesInCaseOfErrors != null) {
			this.maxRetriesInCaseOfErrors = maxRetriesInCaseOfErrors;
		}
	}

	public int getCurrentAttempt() {
		return currentAttempt;
	}

	public long getWaitMillisAfterError() {
		return waitMillisAfterError;
	}

	public void setWaitMillisAfterError(Long waitMillisAfterError) {
		if (waitMillisAfterError != null) {
			this.waitMillisAfterError = waitMillisAfterError;
		}
	}

	public void close() {
		if (closableHttpClient != null) {
			try {
				closableHttpClient.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	public CloseableHttpClient getClosableHttpClient() {
		return closableHttpClient;
	}

	public void setClosableHttpClient(CloseableHttpClient closableHttpClient) {
		this.closableHttpClient = closableHttpClient;
	}

	public String getServerUrl() {
		return serverUrl;
	}
	
	public String getAbsoluteUrl(String fulServicePath) {
		return this.serverUrl + fulServicePath;
	}

}

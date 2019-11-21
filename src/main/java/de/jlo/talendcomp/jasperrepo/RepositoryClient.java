/**
 * Copyright 2019 Jan Lolling jan.lolling@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.jlo.talendcomp.jasperrepo;

import java.io.File;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class RepositoryClient {

	private boolean overwrite = false;
	private static String fileViewUri = "/fileview/fileview";
	private int currentIndex = 0;
	private File currentDownloadFile;
	private static final String repositoryUrlPath = "/rest_v2/resources";
	private static final String serverInfoUrlPath = "/rest_v2/serverInfo";
	private Integer timeout = 20000;
	private HttpClient httpClient = null;
	private JsonNode currentResourceNode = null;
	private String currentListFolderUri = null;
	private final static ObjectMapper objectMapper = new ObjectMapper();
	private List<JsonNode> currentListResources = new ArrayList<>();
	
	public void init(RepositoryClient client) {
		this.httpClient = client.getHttpClient();
	}
	
	public void init(String serverUrl, String user, String password) throws Exception {
		httpClient = new HttpClient(serverUrl, user, password, timeout);
	}
	
	public String getAbsoluteRepoUrl(String uri) throws Exception {
		checkHttpClient();
		return httpClient.getAbsoluteUrl(repositoryUrlPath + uri);
	}
	
	private void checkHttpClient() throws Exception {
		if (httpClient == null) {
			throw new Exception("HttpClient is not initialized. Call RepositoryClient#init before!");
		}
	}
	
	public JsonNode getServerInfo() throws Exception {
		checkHttpClient();
		String response = httpClient.get(httpClient.getAbsoluteUrl(serverInfoUrlPath));
		if (httpClient.isSuccessFul()) {
			ObjectNode responseNode = (ObjectNode) objectMapper.readTree(response);
			responseNode.put("serverUrl", httpClient.getServerUrl());
			responseNode.put("serverLogin", httpClient.getLogin());
			currentResourceNode = responseNode;
			return currentResourceNode;
		} else {
			return null;
		}
	}
	
	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}

	public JsonNode upload(String filePath, String folderUri, String description) throws Exception {
		currentListResources.clear();
		if (filePath == null || filePath.trim().isEmpty()) {
			throw new IllegalArgumentException("filePath cannot be null or empty");
		}
		File f = new File(filePath);
		if (f.canRead() == false) {
			throw new Exception("Upload file failed: File: " + f.getAbsolutePath() + " does not exists or cannot be read!");
		}
		if (description == null) {
			description = "Uploaded by Talend-job";
		}
		final String fileUri =folderUri + "/" + Util.buildResourceId(f.getName());
		boolean alreadyExist = exist(fileUri);
		if (alreadyExist) {
			if (overwrite) {
				delete(fileUri);
			} else {
				throw new Exception("upload file: " + filePath + " to folder: " + folderUri + " failed: resource already exists.");
			}
		}
		try {
			String url = getAbsoluteRepoUrl(folderUri);
			String response = httpClient.upload(url, filePath, description);
			if (httpClient.isSuccessFul()) {
				JsonNode responseNode = objectMapper.readTree(response);
				currentResourceNode = responseNode;
				setupResourceTypeAttribute();
				return responseNode;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new Exception("Upload file: " + filePath + " folderUri: " + folderUri + " failed: " + e.getMessage(), e);
		}
	}

	public File download(String uri, File dir, String targetFileName, boolean createDir, boolean overwrite) throws Exception {
		currentResourceNode = null;
		if (uri == null || uri.trim().isEmpty()) {
			throw new IllegalArgumentException("uri cannot be null or empty");
		}
		if (dir == null) {
			throw new IllegalArgumentException("dir cannot be null");
		}
		if (exist(uri)) {
			info(uri, false);
		} else {
			throw new Exception("download resource: " + uri + " failed: resource does not exist.");
		}
		String resourceId = Util.getResourceId(uri);
		if (targetFileName == null || targetFileName.isEmpty()) {
			targetFileName = resourceId;
		}
		File df = new File(dir, targetFileName);
		File parent = df.getParentFile();
		if (parent.exists() == false) {
			if (createDir) {
				parent.mkdirs();
			} else {
				throw new Exception("Target directory: " + parent.getAbsolutePath() + " does not exists!");
			}
		} else {
			if (parent.isFile()) {
				throw new Exception("Target directory: " + parent.getAbsolutePath() + " already exists as file!");
			}
		}
		currentDownloadFile = df;
		if (overwrite == false && currentDownloadFile.exists()) {
			throw new Exception("File " + currentDownloadFile.getAbsolutePath() + " already exists!");
		}
		try {
			String url = getAbsoluteRepoUrl(uri);
			httpClient.download(url, currentDownloadFile.getAbsolutePath());
		} catch (Exception e) {
			throw new Exception("download file: uri: " + uri + " targetFile: " + targetFileName + " failed: " + e.getMessage(), e);
		}
		return currentDownloadFile;
	}

	public File download(String uri, String dir, String name, boolean createDir, boolean overwrite) throws Exception {
		if (dir == null || dir.trim().isEmpty()) {
			throw new IllegalArgumentException("dir cannot be null or empty");
		}
		final File file = new File(dir);
		return download(uri, file, name, createDir, overwrite);
	}

	public JsonNode copy(String sourceUri, String targetFolderUri) throws Exception {
		currentResourceNode = null;
		String sourceResourceId = Util.getResourceId(sourceUri);
		String targetUri = targetFolderUri + "/" + sourceResourceId;
		if (exist(sourceUri) == false) {
			throw new Exception("copy failed: source resource: " + sourceUri + " does not exist.");
		}
		if (exist(targetUri)) {
			if (overwrite) {
				delete(targetUri);
			}
		}
		try {
			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Location", sourceUri);
			String url = getAbsoluteRepoUrl(targetFolderUri) + "?createFolders=true&overwrite=" + overwrite;
			String response = httpClient.post(url, null, true, headers);
			if (httpClient.isSuccessFul()) {
				JsonNode responseNode = objectMapper.readTree(response);
				currentResourceNode = responseNode;
				setupResourceTypeAttribute();
				return currentResourceNode;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new Exception("copy sourceUri: " + sourceUri + " targetUri: " + targetUri + " failed: " + e.getMessage(), e);
		}
	}
	
	public JsonNode move(String sourceUri, String targetFolderUri) throws Exception {
		currentResourceNode = null;
		if (sourceUri == null || sourceUri.trim().isEmpty()) {
			throw new IllegalArgumentException("sourceUri cannot be null or empty");
		}
		if (targetFolderUri == null || targetFolderUri.trim().isEmpty()) {
			throw new IllegalArgumentException("targetFolderUri cannot be null or empty");
		}
		String sourceResourceId = Util.getResourceId(sourceUri);
		String targetUri = targetFolderUri + "/" + sourceResourceId;
		if (exist(sourceUri) == false) {
			throw new Exception("move failed: source resource: " + sourceUri + " does not exist.");
		}
		if (overwrite && exist(targetUri)) {
			delete(targetUri);
		}
		try {
			Map<String, String> headers = new HashMap<>();
			headers.put("Content-Location", sourceUri);
			String url = getAbsoluteRepoUrl(targetFolderUri) + "?createFolders=true&overwrite=" + overwrite;
			String response = httpClient.put(url, null, true, headers);
			if (httpClient.isSuccessFul()) {
				JsonNode responseNode = objectMapper.readTree(response);
				currentResourceNode = responseNode;
				setupResourceTypeAttribute();
				return currentResourceNode;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new Exception("copy sourceUri: " + sourceUri + " targetUri: " + targetUri + " failed: " + e.getMessage(), e);
		}
	}

	public void delete(String uri) throws Exception {
		checkHttpClient();
		currentResourceNode = null;
		try {
			httpClient.delete(getAbsoluteRepoUrl(uri));
		} catch (Exception e) {
			throw new Exception("delete uri: " + uri + " failed: " + e.getMessage(), e);
		}
	}
		
	public boolean exist(String uri) throws Exception {
		checkHttpClient();
		try {
			return httpClient.exist(getAbsoluteRepoUrl(uri));
		} catch (Exception e) {
			throw new Exception("exist uri: " + uri + " failed: " + e.getMessage(), e);
		}
	}
	
	private void setupResourceTypeAttribute() {
		String hv = httpClient.getResponseHeaderValue("Content-Type");
		if (hv != null && hv.startsWith("application/repository.")) {
			int pos = hv.lastIndexOf('+');
			String resourceType = null;
			if (pos != -1) {
				resourceType = hv.substring("application/repository.".length(), pos);
				((ObjectNode) currentResourceNode).put("resourceType", resourceType);
			} else {
				resourceType = hv.substring("application/repository.".length());
				((ObjectNode) currentResourceNode).put("resourceType", resourceType);
			}
		}
	}
	
	public JsonNode info(String uri, boolean expanded) throws Exception {
		currentResourceNode = null;
		checkHttpClient();
		try {
			Map<String, String> additionalHeaders = new HashMap<>();
			additionalHeaders.put("Accept", "application/repository.file+json");
			String response = httpClient.get(getAbsoluteRepoUrl(uri) + "?expanded=" + expanded, additionalHeaders);
			if (httpClient.isSuccessFul()) {
				JsonNode responseNode = objectMapper.readTree(response);
				currentResourceNode = responseNode;
				setupResourceTypeAttribute();
				return currentResourceNode;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new Exception("info uri: " + uri + " failed: " + e.getMessage(), e);
		}
	}

	public JsonNode list(String uri, String filter, boolean recursive) throws Exception {
		currentListResources.clear();
		currentIndex = 0;
		currentResourceNode = null;
		if (uri == null || uri.trim().isEmpty()) {
			throw new IllegalArgumentException("uri cannot be null or empty");
		}
		currentListFolderUri = uri;
		checkHttpClient();
		try {
			String response = httpClient.get(getAbsoluteRepoUrl("") + "?folderUri=" + uri + "&limit=0&type=file&recursive=" + recursive + (filter != null ? "&q=" + URLEncoder.encode(filter,"UTF-8") : ""));
			if (httpClient.isSuccessFul()) {
				JsonNode responseNode = objectMapper.readTree(response);
				ArrayNode resourceArray = (ArrayNode) responseNode.get("resourceLookup");
				for (JsonNode n : resourceArray) {
					if (n.get("resourceType").asText().equals("file")) {
						currentListResources.add(n);
					}
				}
				return responseNode;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new Exception("list uri: " + uri + " failed: " + e.getMessage(), e);
		}
	}

	public boolean next() {
		if (currentIndex < currentListResources.size()) {
			currentResourceNode = currentListResources.get(currentIndex++);
			return true;
		}
		return false;
	}
	
	public String getCurrentUri() {
		if (currentResourceNode != null) {
			return currentResourceNode.get("uri").asText();
		} else {
			return null;
		}
	}
	
	public String getCurrentResourceLabel() {
		if (currentResourceNode != null) {
			return currentResourceNode.get("label").asText();
		} else {
			return null;
		}
	}

	public String getCurrentListFolderUri() {
		return currentListFolderUri;
	}
	
	public String getCurrentResourceId() {
		return Util.getResourceId(getCurrentUri());
	}
	
	public String getCurrentRelativeUri() {
		return Util.getRelativePath(getCurrentUri(), getCurrentListFolderUri());
	}

	public String getCurrentDownloadLink() throws Exception {
		if (currentResourceNode != null) {
			URL url = new URL(httpClient.getServerUrl());
			String path = url.getPath();
			StringBuilder sb = new StringBuilder();
			sb.append(url.getProtocol());
			sb.append("://");
			sb.append(url.getHost());
			if (url.getPort() != 80) {
				sb.append(":");
				sb.append(url.getPort());
			}
			int pos = path.indexOf('/', 1);
			if (pos != -1) {
				sb.append(path.substring(0, pos));
			} else {
				sb.append(path);
			}
			sb.append(fileViewUri);
			sb.append(getCurrentUri());
			return sb.toString();
		} else {
			return null;
		}
	}

	public File getCurrentDownloadFile() {
		return currentDownloadFile;
	}
	
	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}
	
	public void close() {
		if (httpClient != null) {
			httpClient.close();
		}
	}

	public HttpClient getHttpClient() {
		return httpClient;
	}

}

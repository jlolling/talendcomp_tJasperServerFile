/**
 * Copyright 2015 Jan Lolling jan.lolling@gmail.com
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
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class RepositoryClient implements IRepositoryClient {

	private String currentUri;
	private boolean overwrite = false;
	private static String fileViewUri = "/fileview/fileview";
	private String currentListFolderUri;
	private int currentIndex = 0;
	private File currentDownloadFile;
	private static final String repositoryUrlPath = "/rest_v2/resources";
	private static final String serverInfoUrlPath = "/rest_v2/serverInfo";
	private Integer timeout = 20000;
	private HttpClient cachedHttpClient = null;
	
	public String init(String serverUrl, String user, String password) throws Exception {
		cachedHttpClient = new HttpClient(serverUrl, user, password, timeout);
		return cachedHttpClient.getServerUrl() + " " + getServerInfo();
	}
	
	public String getAbsoluteRepoUrl(String uri) throws Exception {
		checkHttpClient();
		return cachedHttpClient.getAbsoluteUrl(repositoryUrlPath + uri);
	}
	
	private void checkHttpClient() throws Exception {
		if (cachedHttpClient == null) {
			throw new Exception("HttpClient is not initialized. Call RepositoryClient#init before!");
		}
	}
	
	public String getServerInfo() throws Exception {
		checkHttpClient();
		return cachedHttpClient.get(cachedHttpClient.getAbsoluteUrl(serverInfoUrlPath));
	}
	
	public static String checkRepositoryUrl(String urlStr) {
		if (urlStr == null || urlStr.isEmpty()) {
			throw new IllegalArgumentException("url cannot be null or empty");
		}
		if (urlStr.endsWith(repositoryUrlPath)) {
			// everything is fine
			return urlStr;
		} else {
			// extract url parts
			try {
				URL url = new URL(urlStr);
				String host = url.getHost();
				String prot = url.getProtocol();
				int port = url.getPort();
				String path = url.getPath();
				if (path.length() > 1) {
					int pos = path.indexOf('/', 1);
					if (pos > 0) {
						path = path.substring(0, pos);
					}
					path = path + repositoryUrlPath;
				} else {
					path = repositoryUrlPath;
				}
				StringBuilder newUrl = new StringBuilder();
				newUrl.append(prot);
				newUrl.append("://");
				newUrl.append(host);
				if (port > 0) {
					newUrl.append(":");
					newUrl.append(port);
				}
				newUrl.append(path);
				System.out.println("Given URL:" + urlStr + " changed to a repository URL:" + newUrl.toString());
				return newUrl.toString();
			} catch (MalformedURLException e) {
				throw new IllegalArgumentException("URL: " + urlStr + " is not valied:" + e.getMessage(), e);
			}
		}
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	/* (non-Javadoc)
	 * @see de.jlo.talendcomp.jasperrepo.IRepositoryClient#setOverwrite(boolean)
	 */
	@Override
	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}
	
	@Override
	public void uploadFile(String fileName, String folderUri, String description) throws Exception {
		File f = new File(fileName);
		if (f.canRead() == false) {
			throw new IOException("File " + f.getAbsolutePath() + " does not exists or cannot be read!");
		}
		if (description == null) {
			description = "Uploaded by Talend-job.";
		}
		final String fileUri = currentUri;
		if (overwrite && existsResource(fileUri)) {
			deleteResource(fileUri);
		}
		try {

		} catch (Exception e) {
			throw new Exception("Upload file: fileName: " + fileName + " folderUri: " + folderUri + " failed: " + e.getMessage(), e);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.jlo.talendcomp.jasperrepo.IRepositoryClient#downloadFile(java.lang.String, java.io.File, java.lang.String, boolean, boolean)
	 */
	@Override
	public File downloadFile(String uri, File dir, String targetFileName, boolean createDir, boolean overwrite) throws Exception {
		String resourceId = getResourceId(uri);
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
			String downloadUrl = getAbsoluteRepoUrl(uri);
			cachedHttpClient.download(downloadUrl, currentDownloadFile.getAbsolutePath());
		} catch (Exception e) {
			throw new Exception("download file: uri: " + uri + " targetFile: " + targetFileName + " failed: " + e.getMessage(), e);
		}
		return currentDownloadFile;
	}

	/* (non-Javadoc) 
	 * @see de.jlo.talendcomp.jasperrepo.IRepositoryClient#downloadFile(java.lang.String, java.lang.String, java.lang.String, boolean, boolean)
	 */
	@Override
	public File downloadFile(String uri, String dir, String name, boolean createDir, boolean overwrite) throws Exception {
		final File file = new File(dir);
		return downloadFile(uri, file, name, createDir, overwrite);
	}

	/* (non-Javadoc)
	 * @see de.jlo.talendcomp.jasperrepo.IRepositoryClient#copy(java.lang.String, java.lang.String)
	 */
	@Override
	public void copy(String sourceUri, String targetFolderUri) throws Exception {
		String sourceResourceId = getResourceId(sourceUri);
		String sourceFolderURI = getParentUri(sourceUri);

		currentUri = targetFolderUri + "/" + sourceResourceId;
		String targetUri = targetFolderUri + "/" + sourceResourceId;
		if (overwrite && existsResource(targetUri)) {
			deleteResource(targetUri);
		}
		currentUri = targetUri;
		try {

		} catch (Exception e) {
			throw new Exception("copy sourceUri: " + sourceUri + " targetUri: " + targetUri + " failed: " + e.getMessage(), e);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.jlo.talendcomp.jasperrepo.IRepositoryClient#move(java.lang.String, java.lang.String)
	 */
	@Override
	public void move(String sourceUri, String targetFolderUri) throws Exception {
		String sourceResourceId = getResourceId(sourceUri);
		String sourceFolderURI = getParentUri(sourceUri);

		currentUri = targetFolderUri + "/" + sourceResourceId;
		String targetUri = targetFolderUri + "/" + sourceResourceId;
		if (overwrite && existsResource(targetUri)) {
			deleteResource(targetUri);
		}
		currentUri = targetUri;
		try {

		} catch (Exception e) {
			throw new Exception("move sourceUri: " + sourceUri + " targetFolderUri: " + targetFolderUri + " failed: " + e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see de.jlo.talendcomp.jasperrepo.IRepositoryClient#deleteResource(java.lang.String)
	 */
	@Override
	public void deleteResource(String uri) throws Exception {
		checkHttpClient();
		try {
			cachedHttpClient.delete(getAbsoluteRepoUrl(uri));
		} catch (Exception e) {
			throw new Exception("delete uri: " + uri + " failed: " + e.getMessage(), e);
		}
	}
		
	private String buildResourceId(String name) {
		if (name == null) {
			throw new IllegalArgumentException("buildResourceId failed: name cannot be null");
		}
		name = name.replace(' ', '_');
		name = name.replace('#', '_');
		name = name.replace(':', '_');
		name = name.replace('[', '_');
		name = name.replace(']', '_');
		return name;
	}
	
	@Override
	public boolean existsResource(String uri) throws Exception {
		checkHttpClient();
		try {
			return cachedHttpClient.exist(getAbsoluteRepoUrl(uri));
		} catch (Exception e) {
			throw new Exception("existsResource uri: " + uri + " failed: " + e.getMessage(), e);
		}
	}
	
	@Override
	public String infoResource(String uri, boolean expanded) throws Exception {
		checkHttpClient();
		try {
			String content = cachedHttpClient.get(getAbsoluteRepoUrl(uri) + (expanded ? "?expanded=true" : ""));
			return content;
		} catch (Exception e) {
			throw new Exception("infoResource uri: " + uri + " failed: " + e.getMessage(), e);
		}
	}

	/* (non-Javadoc)
	 * @see de.jlo.talendcomp.jasperrepo.IRepositoryClient#createFolder(java.lang.String)
	 */
	@Override
	public boolean createFolder(String uri) throws Exception {
		List<String> uriPath = buildPathList(uri);
		boolean created = false;
		for (String u : uriPath) {

		}
		return created;
	}
	
	private List<String> buildPathList(String uri) {
		List<String> list = new ArrayList<String>();
		while (getDeepth(uri) > 0) {
			list.add(0, uri);
			uri = getParentUri(uri);
		}
		list.add(0, uri);
		return list;
	}
	
	private int getDeepth(String uri) {
		int level = -1; // 0 means root
		for (int i = 0; i < uri.length(); i++) {
			char c = uri.charAt(i);
			if (c == '/') {
				level++;
			}
		}
		return level;
	}
	
	/* (non-Javadoc)
	 * @see de.jlo.talendcomp.jasperrepo.IRepositoryClient#list(java.lang.String, java.lang.String, boolean)
	 */
	@Override
	public void list(String folderUri, String filterExpr, boolean recursive) throws Exception {
		currentListFolderUri = folderUri;

	}
	
	@Override
	public boolean nextListedResource() {
		return false;
	}
	
	private String getParentUri(String uri) {
		String folderUri = "";
		int pos = uri.lastIndexOf('/');
		if (pos == -1) {
			throw new IllegalArgumentException("uri must contain an / (minimum at start)!");
		} else if (pos > 0 && pos == (uri.length() - 1)) {
			throw new IllegalArgumentException("uri cannot have an / at the end!");
		}
		if (pos == 0) {
			folderUri = "/";
		} else {
			folderUri = uri.substring(0, pos);
		}
		return folderUri;
	}

	public static String getResourceId(String uri) {
		int pos = uri.lastIndexOf('/');
		if (pos == -1) {
			throw new IllegalArgumentException("uri must contain an / (minimum at start)!");
		} else if (pos > 0 && pos == (uri.length() - 1)) {
			throw new IllegalArgumentException("uri cannot have an / at the end!");
		}
		return uri.substring(pos + 1);
	}

	private String getResourceMimeType(String filename) {
		int p = filename.lastIndexOf('.');
		if (p > 0 && p < filename.length() - 1) {
			String ext = filename.substring(p + 1).toLowerCase();
			if ("pdf".equals(ext)) {
				return "application/pdf";
			} else if ("xls".equals(ext) || "xlsx".equals(ext) || "ods".equals(ext)) {
				return "application/excel";
			} else if ("png".equals(ext) || "gif".equals(ext) || "bmp".equals(ext) || "psd".equals(ext) || "jpg".equals(ext) || "dia".equals(ext)) {

			} else if ("html".equals(ext)) {

			} else if ("rtf".equals(ext) || "doc".equals(ext) || "docx".equals(ext)) {

			} else {

			}
		} else {

		}
		return null;
	}

	public String getCurrentUri() {
		return currentUri;
	}
	
	public String getCurrentListFolderUri() {
		return currentListFolderUri;
	}
	
	public String getCurrentRelativeUri() {
		return getRelativePath(currentUri, currentListFolderUri);
	}
	
	public String getCurrentResourceId() {
		return getResourceId(currentUri);
	}
	
	public String getCurrentDownloadLink() throws Exception {
		URL url = new URL(cachedHttpClient.getServerUrl());
		String path = url.getPath();
		int pos = path.indexOf('/', 1);
		StringBuilder sb = new StringBuilder();
		sb.append(url.getProtocol());
		sb.append("://");
		sb.append(url.getHost());
		if (url.getPort() != 80) {
			sb.append(":");
			sb.append(url.getPort());
		}
		sb.append(path.substring(0, pos));
		sb.append(fileViewUri);
		sb.append(currentUri);
		return sb.toString();
	}

	public File getCurrentDownloadFile() {
		return currentDownloadFile;
	}
	
	public static String getRelativePath(String fullPath, String basePath) {
		if (fullPath == null || fullPath.trim().isEmpty()) {
			return null;
		}
		if (basePath == null || basePath.trim().isEmpty()) {
			return fullPath;
		}
		// normalize path
		fullPath = fullPath.replaceAll("\\\\", "/").trim();
		fullPath = fullPath.replaceAll("[/]{2,}", "/").trim();
		fullPath = fullPath.replaceAll("/./", "/").trim();
		basePath = basePath.replaceAll("\\\\", "/").trim();
		basePath = basePath.replaceAll("[/]{2,}", "/").trim();
		basePath = basePath.replaceAll("/./", "/").trim();
		if (basePath.endsWith("/")) {
			basePath = basePath.substring(0, basePath.length() - 1);
		}
		int pos = fullPath.indexOf(basePath);
		if (pos == -1) {
			throw new IllegalArgumentException("fullPath does not contains basePath!");
		}
		return fullPath.substring(pos + basePath.length() + 1);
	}

	/* (non-Javadoc)
	 * @see de.jlo.talendcomp.jasperrepo.IRepositoryClient#setTimeout(java.lang.Integer)
	 */
	@Override
	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}
	
	@Override
	public void close() {
		if (cachedHttpClient != null) {
			cachedHttpClient.close();
		}
	}

}

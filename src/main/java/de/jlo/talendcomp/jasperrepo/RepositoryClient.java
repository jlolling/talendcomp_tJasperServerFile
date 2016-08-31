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

import org.apache.commons.io.FilenameUtils;

import com.jaspersoft.ireport.jasperserver.JServer;
import com.jaspersoft.jasperserver.api.metadata.xml.domain.impl.ResourceDescriptor;

public class RepositoryClient {

	private JServer server = null;
	private WSClient client;
	private String currentUri;
	private ResourceDescriptor currentResourceDescriptor;
	private boolean overwrite = false;
	private static String fileViewUri = "/fileview/fileview";
	private List<ResourceDescriptor> currentListResult;
	private String currentListFolderUri;
	private int currentIndex = 0;
	private File currentDownloadFile;
	private static final String repositoryUrlPath = "/services/repository";
	private Integer timeout = 20000;
	
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

	public void setOverwrite(boolean overwrite) {
		this.overwrite = overwrite;
	}
	
	public WSClient getClient() {
		return client;
	}
	
	public void init(RepositoryClient repoClient) {
		if (repoClient == null) {
			throw new IllegalArgumentException("RepositoryClient cannot be null");
		}
		this.client = repoClient.getClient();
		this.server = client.getServer();
	}

	public void init(WSClient client) {
		if (client == null) {
			throw new IllegalArgumentException("WSClient cannot be null");
		}
		this.client = client;
		this.server = client.getServer();
	}

	public void init(String urlStr, String user, String password) throws Exception {
		server = new JServer();
		server.setUrl(checkRepositoryUrl(urlStr));
		server.setUsername(user);
		server.setPassword(password);
		client = new WSClient(server);
		client.setTimeout(timeout);
	}
	
	public String getUrl() {
		return server.getUrl();
	}
	
	public ResourceDescriptor uploadFile(String fileName, String folderUri, String description) throws Exception {
		File f = new File(fileName);
		if (f.canRead() == false) {
			throw new IOException("File " + f.getAbsolutePath() + " does not exists or cannot be read!");
		}
		if (description == null) {
			description = "Uploaded by " + server.getUsername();
		}
		ResourceDescriptor rd = createResourceDescriptor(f, folderUri, description);
		currentUri = rd.getUriString();
		final String fileUri = currentUri;
		if (overwrite && existsFile(fileUri)) {
			deleteResource(fileUri);
		}
		ResourceDescriptor uploadedRes = client.addOrModifyResource(rd, f);
		currentUri = uploadedRes.getUriString();
		return uploadedRes;
	}
	
	public File downloadFile(String uri, File dir, String targetFileName, boolean createDir, boolean overwrite) throws Exception {
		String resourceId = getResourceId(uri);
		String folderURI = getParentUri(uri);
		ResourceDescriptor rd = createResourceDescriptor(resourceId, folderURI, null);
		currentResourceDescriptor = rd;
		currentUri = rd.getUriString();
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
		client.get(rd, currentDownloadFile);
		return currentDownloadFile;
	}

	public ResourceDescriptor copy(String sourceUri, String targetFolderUri) throws Exception {
		String sourceResourceId = getResourceId(sourceUri);
		String sourceFolderURI = getParentUri(sourceUri);
		ResourceDescriptor sourceRd = createResourceDescriptor(sourceResourceId, sourceFolderURI, null);
		sourceRd.setIsNew(false);
		sourceRd.setHasData(false);
		currentResourceDescriptor = sourceRd;
		currentUri = targetFolderUri + "/" + sourceResourceId;
		String targetUri = targetFolderUri + "/" + sourceResourceId;
		if (overwrite && existsFile(targetUri)) {
			deleteResource(targetUri);
		}
		currentUri = targetUri;
		return client.copy(sourceRd, targetUri);
	}
	
	public void move(String sourceUri, String targetFolderUri) throws Exception {
		String sourceResourceId = getResourceId(sourceUri);
		String sourceFolderURI = getParentUri(sourceUri);
		ResourceDescriptor sourceRd = createResourceDescriptor(sourceResourceId, sourceFolderURI, null);
		sourceRd.setIsNew(false);
		sourceRd.setHasData(false);
		currentResourceDescriptor = sourceRd;
		currentUri = targetFolderUri + "/" + sourceResourceId;
		String targetUri = targetFolderUri + "/" + sourceResourceId;
		if (overwrite && existsFile(targetUri)) {
			deleteResource(targetUri);
		}
		currentUri = targetUri;
		client.move(sourceRd, targetFolderUri);
	}

	public void downloadFile(String uri, String dir, String name, boolean createDir, boolean overwrite) throws Exception {
		final File file = new File(dir);
		downloadFile(uri, file, name, createDir, overwrite);
	}

	public void deleteResource(String uri) throws Exception {
		String resourceId = getResourceId(uri);
		String folderURI = getParentUri(uri);
		ResourceDescriptor rd = createResourceDescriptor(resourceId, folderURI, null);
		currentUri = rd.getUriString();
		currentResourceDescriptor = rd;
		client.delete(rd);
	}
	
	private ResourceDescriptor createResourceDescriptor(File uploadFile, String folderURI, String description) {
		ResourceDescriptor rd = new ResourceDescriptor();
		rd.setName(buildResourceId(uploadFile.getName()));
		rd.setLabel(uploadFile.getName());
		rd.setParentFolder(folderURI);
		rd.setDescription(description);
		rd.setUriString(rd.getParentFolder() + "/" + rd.getName());
		rd.setWsType(ResourceDescriptor.TYPE_CONTENT_RESOURCE);
		rd.setResourceType(getResourceType(uploadFile.getName()));
		rd.setResourceProperty(ResourceDescriptor.PROP_CONTENT_RESOURCE_TYPE, getResourceType(uploadFile.getName()));
		rd.setHasData(true);
		rd.setIsNew(true);
		return rd;
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
	
	private ResourceDescriptor createResourceDescriptor(String resourceId, String folderURI, String description) {
		ResourceDescriptor rd = new ResourceDescriptor();
		rd.setWsType(ResourceDescriptor.TYPE_CONTENT_RESOURCE);
		rd.setResourceType(getResourceType(resourceId));
		rd.setName(resourceId);
		rd.setDescription(description);
		rd.setParentFolder(folderURI);
		rd.setUriString(rd.getParentFolder() + "/" + resourceId);
		rd.setHasData(true);
		rd.setIsNew(true);
		return rd;
	}
	
	private ResourceDescriptor createFolderResourceDescriptor(String uri, String description) {
		ResourceDescriptor rd = new ResourceDescriptor();
		rd.setHasData(false);
		rd.setIsNew(true);
		rd.setWsType(ResourceDescriptor.TYPE_FOLDER);
		if (uri.equals("/")) {
			// create root
			rd.setName("root");
			rd.setUriString("/");
		} else {
			String folderUri = getParentUri(uri);
			String resourceId = getResourceId(uri);
			rd.setName(resourceId);
			rd.setLabel(resourceId);
			rd.setDescription(description);
			rd.setParentFolder(folderUri);
			if (folderUri.equals("/")) {
				rd.setUriString("/" + resourceId);
			} else {
				rd.setUriString(rd.getParentFolder() + "/" + resourceId);
			}
		}
		return rd;
	}
	
	public boolean existsFile(String uri) throws Exception {
		return getFileResourceDescriptor(uri) != null;
	}
	
	private ResourceDescriptor getFileResourceDescriptor(String uri) throws Exception {
		String folderUri = getParentUri(uri);
		String resourceId = getResourceId(uri);
		List<ResourceDescriptor> list = list(folderUri, null, false);
		for (ResourceDescriptor rd : list) {
			if (rd.getName().equals(resourceId)) {
				return rd;
			}
		}
		return null;
	}
	
	private ResourceDescriptor getFolderResourceDescriptor(String uri) throws Exception {
		String folderUri = getParentUri(uri);
		String resourceId = getResourceId(uri);
		List<ResourceDescriptor> list = listFolders(folderUri);
		for (ResourceDescriptor rd : list) {
			if (rd.getName().equals(resourceId)) {
				return rd;
			}
		}
		return null;
	}

	public boolean createFolder(String uri) throws Exception {
		List<String> uriPath = buildPathList(uri);
		boolean created = false;
		for (String u : uriPath) {
			ResourceDescriptor rd = getFolderResourceDescriptor(u);
			if (rd == null) {
				rd = createFolderResourceDescriptor(u, null);
				client.addOrModifyResource(rd, null);
				currentUri = rd.getUriString();
				created = true;
			}
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
	
	public List<ResourceDescriptor> list(String folderUri, String filterExpr, boolean recursive) throws Exception {
		currentListFolderUri = folderUri;
		currentListResult = new ArrayList<ResourceDescriptor>();
		list(folderUri, filterExpr, recursive, currentListResult);
		return currentListResult;
	}
	
	private void list(String folderUri, String filterExpr, boolean recursive, List<ResourceDescriptor> list) throws Exception {
		ResourceDescriptor rd = createFolderResourceDescriptor(folderUri, null);
		currentUri = rd.getUriString();
		boolean dofilter = filterExpr != null && filterExpr.isEmpty() == false;
		final List<ResourceDescriptor> allResults = client.list(rd);
		for (ResourceDescriptor r : allResults) {
			if (isContentResource(r)) {
				if (dofilter == false || FilenameUtils.wildcardMatch(getResourceId(r.getUriString()), filterExpr)) {
					list.add(r);
				}
			}
		}
		for (ResourceDescriptor r : allResults) {
			if (recursive && isFolder(r)) {
				list(folderUri + "/" + r.getName(), filterExpr, recursive, list);
			}
		}
	}
	
	private List<ResourceDescriptor> listFolders(String folderUri) throws Exception {
		List<ResourceDescriptor> list = new ArrayList<ResourceDescriptor>();
		ResourceDescriptor rdParentFolder = createFolderResourceDescriptor(folderUri, null);
		final List<ResourceDescriptor> allResults = client.list(rdParentFolder);
		for (ResourceDescriptor rd : allResults) {
			if (isFolder(rd)) {
				list.add(rd);
			}
		}
		return list;
	}

	public boolean nextListedResource() {
		if (currentIndex < currentListResult.size()) {
			currentResourceDescriptor = currentListResult.get(currentIndex++);
			currentUri = currentResourceDescriptor.getUriString();
			return true;
		} else {
			return false;
		}
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

	private String getResourceType(String filename) {
		int p = filename.lastIndexOf('.');
		if (p > 0 && p < filename.length() - 1) {
			String ext = filename.substring(p + 1).toLowerCase();
			if ("pdf".equals(ext)) {
				return ResourceDescriptor.CONTENT_TYPE_PDF;
			} else if ("xls".equals(ext) || "xlsx".equals(ext) || "ods".equals(ext)) {
				return ResourceDescriptor.CONTENT_TYPE_XLS;
			} else if ("png".equals(ext) || "gif".equals(ext) || "bmp".equals(ext) || "psd".equals(ext) || "jpg".equals(ext) || "dia".equals(ext)) {
				return ResourceDescriptor.CONTENT_TYPE_IMAGE;
			} else if ("html".equals(ext)) {
				return ResourceDescriptor.CONTENT_TYPE_HTML;
			} else if ("rtf".equals(ext) || "doc".equals(ext) || "docx".equals(ext)) {
				return ResourceDescriptor.CONTENT_TYPE_RTF;
			} else {
				return ResourceDescriptor.CONTENT_TYPE_CSV;
			}
		} else {
			return ResourceDescriptor.CONTENT_TYPE_CSV;
		}
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
	
	public boolean isCurrentResourceFolder() {
		return isFolder(currentResourceDescriptor);
	}
	
	public String getCurrentResourceLabel() {
		return currentResourceDescriptor.getLabel();
	}
	
	private static boolean isFolder(ResourceDescriptor rd) {
		return ResourceDescriptor.TYPE_FOLDER.equals(rd.getWsType());
	}
	
	private static boolean isContentResource(ResourceDescriptor rd) {
		if (ResourceDescriptor.TYPE_CONTENT_RESOURCE.equals(rd.getWsType())) {
			return true;
		} else {
			Object test = rd.getProperty("PROP_RESOURCE_TYPE");
			if (test instanceof com.jaspersoft.jasperserver.api.metadata.xml.domain.impl.ResourceProperty) {
				com.jaspersoft.jasperserver.api.metadata.xml.domain.impl.ResourceProperty p = (com.jaspersoft.jasperserver.api.metadata.xml.domain.impl.ResourceProperty) test;
				return "com.jaspersoft.jasperserver.api.metadata.common.domain.FileResource".equals(p.getValue());
			} else {
				return false;
			}
		}
	}
	
	public String getCurrentDownloadLink() throws MalformedURLException {
		URL url = new URL(server.getUrl());
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

	public void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

}

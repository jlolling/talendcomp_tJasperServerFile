package de.jlo.talendcomp.jasperrepo;

import java.io.File;

public interface IRepositoryClient {

	void setOverwrite(boolean overwrite);

	void uploadFile(String fileName, String folderUri, String description) throws Exception;

	File downloadFile(String uri, File dir, String targetFileName, boolean createDir, boolean overwrite)
			throws Exception;

	void copy(String sourceUri, String targetFolderUri) throws Exception;

	void move(String sourceUri, String targetFolderUri) throws Exception;

	void downloadFile(String uri, String dir, String name, boolean createDir, boolean overwrite) throws Exception;

	void deleteResource(String uri) throws Exception;

	boolean existsFile(String uri) throws Exception;

	boolean createFolder(String uri) throws Exception;

	void list(String folderUri, String filterExpr, boolean recursive) throws Exception;

	boolean nextListedResource();

	void setTimeout(Integer timeout);

}
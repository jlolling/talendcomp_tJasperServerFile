package de.jlo.talendcomp.jasperrepo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

public class TestRepositoryClient {
	
	private RepositoryClient rc = null;
	
	@Before
	@Test
	public void testConnect() throws Exception {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		System.out.println("Connect to server...");
		rc = new RepositoryClient();
		rc.setTimeout(10000);
		rc.setOverwrite(true);
		String serverUrl = "http://localhost:8080/jasperserver";
		String user = "jasperadmin";
		String passwd = "jasperadmin";
		rc.init(serverUrl, user, passwd);
		String info = rc.getServerInfo();
		System.out.println("Repository client connected. server: " + info);
		assertTrue(info != null);
	}
	
	@After
	public void close() {
		System.out.println("Close client...");
		rc.close();
	}
	
	@Test
	public void testDownloadFile1() throws Exception {
		String localDir = "/var/testdata/jasper/download_test/";
		String jasperFileName = "Exasol_SQLServer.pdf";
		File f = rc.download("/ContentFiles/pdf/" + jasperFileName, localDir, null, true, true);
		System.out.println("Received file: " + f.getAbsolutePath());
		assertEquals("Wrong path", localDir + jasperFileName, f.getAbsolutePath());
		assertTrue(f.canRead());
	}

	@Test
	public void testDownloadFile2() throws Exception {
		String localDir = "/var/testdata/jasper/download_test/";
		String jasperFileName = "overrides_custom.css";
		File f = rc.download("/themes/default/" + jasperFileName, localDir, null, true, true);
		System.out.println("Received file: " + f.getAbsolutePath());
		assertEquals("Wrong path", localDir + jasperFileName, f.getAbsolutePath());
		assertTrue(f.canRead());
	}
	
	@Test
	public void testExist1() throws Exception {
		String resourceId = "/reports/interactive/TableReport";
		boolean exists = rc.exist(resourceId);
		assertTrue(exists);
		resourceId = "/reports/interactive/TableReportX";
		exists = rc.exist(resourceId);
		assertTrue(exists == false);
	}

	@Test
	public void testExist2() throws Exception {
		String resourceId = "/ContentFiles/pdf/barcode_test.pdf";
		boolean exists = rc.exist(resourceId);
		assertTrue(exists);
	}

	@Test
	public void testInfo() throws Exception {
		String resourceId = "/reports/interactive/TableReport";
		JsonNode json = rc.info(resourceId, false);
		System.out.println(json);
		assertTrue(json != null);
	}
	
	@Test
	public void testInfoBinary() throws Exception {
		String resourceId = "/themes/default/overrides_custom.css";
		JsonNode json = rc.info(resourceId, false);
		System.out.println(json);
		assertTrue(json != null);
	}

	@Test
	public void testCopy() throws Exception {
		String resourceId = "/ContentFiles/pdf/barcode_test.pdf";
		String targetFolderUri = "/ContentFiles/pdf/copied";
		JsonNode json = rc.copy(resourceId, targetFolderUri);
		System.out.println(json);
		assertTrue(json != null);
	}

	@Test
	public void testMove() throws Exception {
		String resourceId = "/ContentFiles/pdf/barcode_test.pdf";
		String targetFolderUri = "/ContentFiles/pdf/moved";
		JsonNode json = rc.move(resourceId, targetFolderUri);
		System.out.println(json);
		assertTrue(json != null);
	}

	@Test
	public void testDelete() throws Exception {
		String targetFolderUri = "/reports/synctest/fields.xml";
		rc.delete(targetFolderUri);
		assertTrue(true);
	}

	@Test
	public void testUpload() throws Exception {
		String filePath = "/Data/Talend/testdata/jasper/barcode_test.pdf";
		String description = "Upload Test time: " + System.currentTimeMillis();
		String targetFolderId = "/ContentFiles/pdf";
		JsonNode json = rc.upload(filePath, targetFolderId, description);
		System.out.println(json);
		assertTrue(true);
	}

	@Test
	public void testList() throws Exception {
		String resourceId = "/ContentFiles/pdf";
		JsonNode json = rc.list(resourceId, null, false);
		System.out.println(json);
		while (rc.next()) {
			System.out.println(rc.getCurrentUri());
		}
		assertTrue(json != null);
	}

}
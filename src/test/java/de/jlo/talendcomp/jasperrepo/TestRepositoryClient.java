package de.jlo.talendcomp.jasperrepo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.fasterxml.jackson.databind.JsonNode;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRepositoryClient {
	
	private static RepositoryClient rc = null;
	
	@BeforeClass
	public static void test01Connect() throws Exception {
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
		JsonNode info = rc.getServerInfo();
		System.out.println("Repository client connected. server: " + info);
	}
	
	@AfterClass
	public static void close() throws Exception {
		System.out.println("Close client...");
		rc.close();
	}
	
	@Test
	public void test02DownloadFile() throws Exception {
		String localDir = "/var/testdata/jasper/download_test/";
		String jasperFileName = "overrides_custom.css";
		File f = rc.download("/themes/default/" + jasperFileName, localDir, null, true, true);
		System.out.println("Received file: " + f.getAbsolutePath());
		assertEquals("Wrong path", localDir + jasperFileName, f.getAbsolutePath());
		assertTrue(f.canRead());
	}
	
	@Test
	public void test03Exist() throws Exception {
		String resourceId = "/reports/interactive/TableReport";
		boolean exists = rc.exist(resourceId);
		assertTrue(exists);
		resourceId = "/reports/interactive/TableReportX";
		exists = rc.exist(resourceId);
		assertTrue(exists == false);
	}
	
	@Test
	public void test04Info() throws Exception {
		String resourceId = "/reports/interactive/TableReport";
		JsonNode json = rc.info(resourceId, false);
		System.out.println(json);
		assertTrue(json != null);
	}
	
	@Test
	public void test05InfoBinary() throws Exception {
		String resourceId = "/themes/default/overrides_custom.css";
		JsonNode json = rc.info(resourceId, false);
		System.out.println(json);
		assertTrue(json != null);
	}

	@Test
	public void test061UploadContentResource() throws Exception {
		String filePath = "/Data/Talend/testdata/jasper/barcode_test.pdf";
		String description = "Upload Test time: " + System.currentTimeMillis();
		String targetFolderId = "/ContentFiles/pdf";
		JsonNode json = rc.upload(filePath, targetFolderId, description);
		System.out.println(json);
		assertTrue(true);
	}

	@Test
	public void test062UploadResourceFile() throws Exception {
		String filePath = "/Data/Talend/testdata/jasper/barcode_test.jrxml";
		String description = "Upload Test time: " + System.currentTimeMillis();
		String targetFolderId = "/ContentFiles/pdf";
		JsonNode json = rc.upload(filePath, targetFolderId, description);
		System.out.println(json);
		assertTrue(true);
	}

	@Test
	public void test07Exist() throws Exception {
		String resourceId = "/ContentFiles/pdf/barcode_test.pdf";
		boolean exists = rc.exist(resourceId);
		assertTrue(exists);
	}

	@Test
	public void test08Copy() throws Exception {
		String resourceId = "/ContentFiles/pdf/barcode_test.pdf";
		String targetFolderUri = "/ContentFiles/pdf/copied";
		JsonNode json = rc.copy(resourceId, targetFolderUri);
		System.out.println(json);
		assertTrue(json != null);
	}

	@Test
	public void test09List() throws Exception {
		String resourceId = "/ContentFiles/pdf";
		JsonNode json = rc.list(resourceId, null, false);
		System.out.println(json);
		while (rc.next()) {
			System.out.println(rc.getCurrentUri());
		}
		assertTrue(json != null);
	}
	
	@Test
	public void test111Move() throws Exception {
		String resourceId = "/ContentFiles/pdf/barcode_test.pdf";
		String targetFolderUri = "/ContentFiles/pdf/moved";
		JsonNode json = rc.move(resourceId, targetFolderUri);
		System.out.println(json);
		assertTrue(json != null);
	}

	@Test
	public void test112Move() throws Exception {
		String resourceId = "/ContentFiles/pdf/barcode_test.jrxml";
		String targetFolderUri = "/ContentFiles/pdf/moved";
		JsonNode json = rc.move(resourceId, targetFolderUri);
		System.out.println(json);
		assertTrue(json != null);
	}

	@Test
	public void test121Delete() throws Exception {
		String targetFolderUri = "/ContentFiles/pdf/moved/barcode_test.pdf";
		rc.delete(targetFolderUri);
		assertTrue(true);
	}

	@Test
	public void test122Delete() throws Exception {
		String targetFolderUri = "/ContentFiles/pdf/moved/barcode_test.jrxml";
		rc.delete(targetFolderUri);
		assertTrue(true);
	}

}
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

public class TestHttpClient {
	
	private RepositoryClient rc = null;
	
	@Before
	@Test
	public void testConnect() throws Exception {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		System.out.println("Connect to server...");
		rc = new RepositoryClient();
		rc.setTimeout(10000);
		String serverUrl = "http://localhost:8080/jasperserver";
		String user = "jasperadmin";
		String passwd = "jasperadmin";
		String info = rc.init(serverUrl, user, passwd);
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
		File f = rc.downloadFile("/ContentFiles/pdf/" + jasperFileName, localDir, null, true, true);
		System.out.println("Received file: " + f.getAbsolutePath());
		assertEquals("Wrong path", localDir + jasperFileName, f.getAbsolutePath());
		assertTrue(f.canRead());
	}

	@Test
	public void testDownloadFile2() throws Exception {
		String localDir = "/var/testdata/jasper/download_test/";
		String jasperFileName = "overrides_custom.css";
		File f = rc.downloadFile("/themes/default/" + jasperFileName, localDir, null, true, true);
		System.out.println("Received file: " + f.getAbsolutePath());
		assertEquals("Wrong path", localDir + jasperFileName, f.getAbsolutePath());
		assertTrue(f.canRead());
	}
	
	@Test
	public void testExist() throws Exception {
		String resourceId = "/reports/interactive/TableReport";
		boolean exists = rc.existsResource(resourceId);
		assertTrue(exists);
		resourceId = "/reports/interactive/TableReportX";
		exists = rc.existsResource(resourceId);
		assertTrue(exists == false);
	}

	@Test
	public void testInfo() throws Exception {
		String resourceId = "/reports/interactive/TableReport";
		String json = rc.infoResource(resourceId, false);
		System.out.println(json);
		assertTrue(json != null);
	}

}
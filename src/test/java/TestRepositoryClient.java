import java.io.File;

import de.jlo.talendcomp.jasperrepo.RepositoryClient;


public class TestRepositoryClient {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		testCheckUrl();
//		testList();
//		testDownload();
//		testMove();
//		testDelete();
	}
	
	private static void testDownload() {
		System.out.println("###### testDownload");
		de.jlo.talendcomp.jasperrepo.RepositoryClient client = new de.jlo.talendcomp.jasperrepo.RepositoryClient();
		try {
			client.init("http://localhost:8080/jasperserver/services/repository", "jasperadmin", "jasperadmin");
			File dir = new File("/home/jlolling/test_get/");
			String name = "f2/update_meta_schema.sql";
			client.downloadFile("/reports/output/f2/update_meta_schema.sql", dir, name, true, true);
			if (client.getCurrentDownloadFile().exists()) {
				System.out.println("OK: " + client.getCurrentDownloadFile());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void testDelete() {
		System.out.println("###### testDelete");
		de.jlo.talendcomp.jasperrepo.RepositoryClient client = new de.jlo.talendcomp.jasperrepo.RepositoryClient();
		try {
			client.init("http://localhost:8080/jasperserver/services/repository", "jasperadmin", "jasperadmin");
			client.deleteResource("/reports/output/f6/vouchers.txt");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void testCopy() {
		System.out.println("###### testCopy");
		de.jlo.talendcomp.jasperrepo.RepositoryClient client = new de.jlo.talendcomp.jasperrepo.RepositoryClient();
		try {
			client.init("http://localhost:8080/jasperserver/services/repository", "jasperadmin", "jasperadmin");
			client.createFolder("/reports/output/f6");
			System.out.println(client.copy("/reports/output/f3/vouchers.txt", "/reports/output/f6").getUriString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void testMove() {
		System.out.println("###### testMove");
		de.jlo.talendcomp.jasperrepo.RepositoryClient client = new de.jlo.talendcomp.jasperrepo.RepositoryClient();
		try {
			client.init("http://localhost:8080/jasperserver/services/repository", "jasperadmin", "jasperadmin");
			client.createFolder("/reports/output/f6");
			client.move("/reports/output/f3/vouchers.txt", "/reports/output/f6");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void testList() {
		System.out.println("###### testList");
		de.jlo.talendcomp.jasperrepo.RepositoryClient client = new de.jlo.talendcomp.jasperrepo.RepositoryClient();
		try {
			client.init("http://localhost:8080/jasperserver/services/repository", "jasperadmin", "jasperadmin");
			client.list("/reports/Uebungen", null, true);
			while (client.nextListedResource()) {
				System.out.print(client.getCurrentUri());
				System.out.print("#");
				System.out.println(client.getCurrentRelativeUri());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void testUpload() {
		System.out.println("###### testUpload");
		de.jlo.talendcomp.jasperrepo.RepositoryClient client = new de.jlo.talendcomp.jasperrepo.RepositoryClient();
		try {
			client.init("http://on-0337-jll.local:8380/jasperserver-pro/services/repository", "jasperadmin", "jasperadmin");
			client.setOverwrite(true);
			client.createFolder("/reports/output/f5");
			client.uploadFile("/private/var/testdata/vouchers copy.txt", "/reports/output/f5", null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void testCheckUrl() {
		String urlStr = "https://www.on-0337-jll.local/jasperserver/nix";
		String newUrl = RepositoryClient.checkRepositoryUrl(urlStr);
		System.out.println(newUrl);
	}

}

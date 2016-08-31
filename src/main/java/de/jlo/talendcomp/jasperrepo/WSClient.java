/*
 * iReport - Visual Designer for JasperReports.
 * Copyright (C) 2002 - 2009 Jaspersoft Corporation. All rights reserved.
 * http://www.jaspersoft.com
 *
 * Unless you have purchased a commercial license agreement from Jaspersoft,
 * the following license terms apply:
 *
 * This program is part of iReport.
 *
 * iReport is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * iReport is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with iReport. If not, see <http://www.gnu.org/licenses/>.
 */
package de.jlo.talendcomp.jasperrepo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;

import org.apache.axis.EngineConfiguration;
import org.apache.axis.attachments.AttachmentPart;
import org.apache.axis.client.Call;

import com.jaspersoft.ireport.jasperserver.JServer;
import com.jaspersoft.ireport.jasperserver.ws.ManagementService;
import com.jaspersoft.ireport.jasperserver.ws.ManagementServiceServiceLocator;
import com.jaspersoft.ireport.jasperserver.ws.RequestAttachment;
import com.jaspersoft.ireport.jasperserver.ws.util.ResourceConfigurationProvider;
import com.jaspersoft.jasperserver.api.metadata.xml.domain.impl.Argument;
import com.jaspersoft.jasperserver.api.metadata.xml.domain.impl.OperationResult;
import com.jaspersoft.jasperserver.api.metadata.xml.domain.impl.Request;
import com.jaspersoft.jasperserver.api.metadata.xml.domain.impl.ResourceDescriptor;
import com.jaspersoft.jasperserver.ws.xml.Marshaller;
import com.jaspersoft.jasperserver.ws.xml.Unmarshaller;

/**
 * 
 * @author gtoffoli, Jan Lolling
 */
public class WSClient {

	public static final String AXIS_CONFIGURATION_RESOURCE = "/com/jaspersoft/ireport/jasperserver/ws/client-config.wsdd";
	private JServer server = null;
	private String webservicesUri = null; // "http://127.0.0.1:8080/axis2/services/repository-ws-1.0";
	private ManagementService managementService = null;
	private Unmarshaller unmarshaller = new Unmarshaller();
	private Marshaller marshaller = new Marshaller();
	private String cachedServerVersion;
	private int timeout = 20000;

	public WSClient(JServer server) throws Exception {
		this.server = server;
		URL url;
		try {
			url = new URL(server.getUrl());
		} catch (MalformedURLException e1) {
			throw new Exception(e1);
		}
		setWebservicesUri(url.toString());
	}

	/**
	 * It returns a list of resourceDescriptors.
	 */
	public java.util.List<ResourceDescriptor> list(ResourceDescriptor descriptor) throws Exception {
		Request req = new Request();
		req.setOperationName(Request.OPERATION_LIST);
		req.setResourceDescriptor(descriptor);
		req.setLocale(getServer().getLocale());
		StringWriter xmlStringWriter = new StringWriter();
		Marshaller.marshal(req, xmlStringWriter);
		return list(xmlStringWriter.toString());
	}

	public String getVersion() throws Exception {
		if (cachedServerVersion != null) {
			return cachedServerVersion;
		}
		Request req = new Request();
		req.setOperationName(Request.OPERATION_LIST);
		req.setResourceDescriptor(null);
		req.setLocale(getServer().getLocale());
		try {
			ManagementService ms = getManagementService();
			String reqXml = marshaller.marshal(req);
			// System.out.println("Executing list for version.." + new
			// java.util.Date());
			// System.out.flush();
			String result = ms.list(reqXml);
			// System.out.println("Finished list for version.." + new
			// java.util.Date());
			// System.out.flush();

			// In order to avoid problem with the classloading, forse a
			// classloader for
			// the parting...
			OperationResult or = (OperationResult) unmarshal(result);
			if (or.getReturnCode() != 0)
				throw new Exception(or.getReturnCode() + " - "
						+ or.getMessage());
			cachedServerVersion = or.getVersion();
			return cachedServerVersion;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	/**
	 * It returns a list of resourceDescriptors.
	 */
	@SuppressWarnings("unchecked")
	public java.util.List<ResourceDescriptor> list(String xmlRequest) throws Exception {
		try {
			String result = getManagementService().list(xmlRequest);
			OperationResult or = (OperationResult) unmarshal(result);
			if (or.getReturnCode() != 0)
				throw new Exception(or.getReturnCode() + " - "
						+ or.getMessage());
			return or.getResourceDescriptors();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	public void delete(ResourceDescriptor descriptor) throws Exception {
		delete(descriptor, null);
	}

	/**
	 * Delete a resource and its contents Specify the reportUnitUri if you are
	 * deleting something inside this report unit.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void delete(ResourceDescriptor descriptor, String reportUnitUri)	throws Exception {
		try {
			Request req = new Request();
			req.setOperationName("delete");
			req.setResourceDescriptor(descriptor);
			req.setLocale(getServer().getLocale());
			if (reportUnitUri != null && reportUnitUri.length() > 0) {
				req.getArguments()
						.add(new Argument(Argument.MODIFY_REPORTUNIT,
								reportUnitUri));
			}
			String result = getManagementService().delete(marshaller.marshal(req));
			OperationResult or = (OperationResult) unmarshal(result);
			if (or.getReturnCode() != 0)
				throw new Exception(or.getReturnCode() + " - "
						+ or.getMessage());
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	/**
	 * Export a resource using the "get" ws and save the resource in the file
	 * specified by the user... If the outputFile is null, the argument
	 * "NO_ATTACHMENT" is added to the request in order of avoid the attachment
	 * trasmission.
	 * 
	 */
	public ResourceDescriptor get(ResourceDescriptor descriptor, File outputFile) throws Exception {
		return get(descriptor, outputFile, null);
	}

	/**
	 * Export a resource using the "get" ws and save the resource in the file
	 * specified by the user... If the outputFile is null, the argument
	 * "NO_ATTACHMENT" is added to the request in order of avoid the attachment
	 * transmission.
	 * 
	 */
	@SuppressWarnings("unchecked")
	public ResourceDescriptor get(
			ResourceDescriptor descriptor,
			File outputFile, 
			java.util.List<Argument> args) throws Exception {
		Request req = new Request();
		req.setOperationName("get");
		req.setResourceDescriptor(descriptor);
		req.setLocale(getServer().getLocale());
		if (args != null) {
			for (int i = 0; i < args.size(); ++i) {
				Argument arg = args.get(i);
				req.getArguments().add(arg);
			}
		}
		if (outputFile == null) {
			req.getArguments()
					.add(new Argument(Argument.NO_RESOURCE_DATA_ATTACHMENT,
							null));
		}
		String result = getManagementService().get(marshaller.marshal(req));
		OperationResult or = (OperationResult) unmarshal(result);
		if (or.getReturnCode() != 0)
			throw new Exception(or.getReturnCode() + " - "
					+ or.getMessage());
		Object[] resAtts = ((org.apache.axis.client.Stub) getManagementService()).getAttachments();
		if (resAtts != null && resAtts.length > 0 && outputFile != null) {
			java.io.InputStream is = null;
			OutputStream os = null;
			try {
				is = ((org.apache.axis.attachments.AttachmentPart) resAtts[0]).getDataHandler().getInputStream();
				byte[] buffer = new byte[1024];
				os = new FileOutputStream(outputFile);
				int bCount = 0;
				while ((bCount = is.read(buffer)) > 0) {
					os.write(buffer, 0, bCount);
				}
				os.close();
			} finally {
				if (is != null) {
					is.close();
				}
				if (os != null) {
					os.flush();
					os.close();
				}
			}
		} else if (outputFile != null) {
			throw new Exception("Attachment not present!");
		}
		return (ResourceDescriptor) or.getResourceDescriptors().get(0);
	}

	/**
	 * Add or Modify a resource. Return the updated ResourceDescriptor
	 * 
	 */
	public ResourceDescriptor addOrModifyResource(
			ResourceDescriptor descriptor, 
			File inputFile) throws Exception {
		return modifyReportUnitResource(null, descriptor, inputFile);
	}

	public ResourceDescriptor putResource(
			ResourceDescriptor descriptor,
			RequestAttachment[] attachments) throws Exception {
		return putReportUnitResource(null, descriptor, attachments);
	}

	public JServer getServer() {
		return server;
	}

	public void setServer(JServer server) {
		this.server = server;
	}

	public String getUsername() {
		return getServer().getUsername();
	}

	public String getPassword() {
		return getServer().getPassword();
	}

	public ResourceDescriptor modifyReportUnitResource(
			String reportUnitUri,
			ResourceDescriptor descriptor, 
			File inputFile) throws Exception {
		RequestAttachment[] attachments;
		if (inputFile == null) {
			attachments = new RequestAttachment[0];
		} else {
			FileDataSource fileDataSource = new FileDataSource(inputFile);
			RequestAttachment attachment = new RequestAttachment(fileDataSource);
			attachment.setContentID(descriptor.getName()); // patched by Jan Lolling
			attachments = new RequestAttachment[] { attachment };
		}
		return putReportUnitResource(reportUnitUri, descriptor, attachments);
	}

	@SuppressWarnings("unchecked")
	public ResourceDescriptor putReportUnitResource(
			String reportUnitUri,
			ResourceDescriptor descriptor, 
			RequestAttachment[] attachments) throws Exception {
		try {
			Request req = new Request();
			req.setOperationName("put");
			req.setLocale(getServer().getLocale());
			if (reportUnitUri != null && reportUnitUri.length() > 0) {
				req.getArguments()
						.add(new Argument(Argument.MODIFY_REPORTUNIT,
								reportUnitUri));
			}
			ManagementService ms = getManagementService();
			// attach the file...
			if (attachments != null && attachments.length > 0) {
				descriptor.setHasData(true);
				// Tell the stub that the message being formed also contains an
				// attachment, and it is of type MIME encoding.

				((org.apache.axis.client.Stub) ms)._setProperty(
						Call.ATTACHMENT_ENCAPSULATION_FORMAT,
						Call.ATTACHMENT_ENCAPSULATION_FORMAT_MIME);
				for (int i = 0; i < attachments.length; i++) {
					RequestAttachment attachment = attachments[i];
					DataHandler attachmentHandler = new DataHandler(attachment.getDataSource());
					AttachmentPart attachmentPart = new AttachmentPart(attachmentHandler);
					if (attachment.getContentID() != null) {
						attachmentPart.setContentId(attachment.getContentID());
					}
					// Add the attachment to the message
					((org.apache.axis.client.Stub) ms).addAttachment(attachmentPart);
				}
			}
			req.setResourceDescriptor(descriptor);
			String result = ms.put(marshaller.marshal(req));
			OperationResult or = (OperationResult) unmarshal(result);
			if (or.getReturnCode() != 0) {
				throw new Exception("resourceId=" + descriptor.getName() + " failed: " + or.getReturnCode() + " - " + or.getMessage());
			}
			return (ResourceDescriptor) or.getResourceDescriptors().get(0);
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	@SuppressWarnings("unchecked")
	public void move(ResourceDescriptor resource, String destinationURI) throws Exception {
		try {
			Request req = new Request();
			req.setOperationName("move");
			req.setResourceDescriptor(resource);
			req.setLocale(getServer().getLocale());
			req.getArguments()
					.add(new Argument(
							Argument.DESTINATION_URI,
							destinationURI));
			String result = getManagementService().move(marshaller.marshal(req));
			OperationResult or = (OperationResult) unmarshal(result);
			if (or.getReturnCode() != OperationResult.SUCCESS) {
				throw new Exception(or.getReturnCode() + " - " + or.getMessage());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	@SuppressWarnings("unchecked")
	public ResourceDescriptor copy(
			ResourceDescriptor resource,
			String destinationURI) throws Exception {
		try {
			Request req = new Request();
			req.setOperationName("copy");
			req.setResourceDescriptor(resource);
			req.setLocale(getServer().getLocale());
			req.getArguments()
					.add(new Argument(
							Argument.DESTINATION_URI,
							destinationURI));
			String result = getManagementService().copy(marshaller.marshal(req));
			OperationResult or = (OperationResult) unmarshal(result);
			if (or.getReturnCode() != OperationResult.SUCCESS) {
				throw new Exception(or.getReturnCode() + " - "
						+ or.getMessage());
			}
			ResourceDescriptor copyDescriptor;
			List<ResourceDescriptor> resultDescriptors = or.getResourceDescriptors();
			if (resultDescriptors == null || resultDescriptors.isEmpty()) {
				copyDescriptor = null;
			} else {
				copyDescriptor = (ResourceDescriptor) resultDescriptors.get(0);
			}
			return copyDescriptor;
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	public String getWebservicesUri() {
		return webservicesUri;
	}

	public void setWebservicesUri(String webservicesUri) {
		this.webservicesUri = webservicesUri;
	}

	public ManagementService getManagementService() throws Exception {
		if (managementService == null) {
			ManagementServiceServiceLocator rsl = new ManagementServiceServiceLocator(getEngineConfiguration());
			managementService = rsl.getrepository(new java.net.URL(getWebservicesUri()));
			((org.apache.axis.client.Stub) managementService).setUsername(getUsername());
			((org.apache.axis.client.Stub) managementService).setPassword(getPassword());
			((org.apache.axis.client.Stub) managementService).setMaintainSession(true);
		}
		if (timeout != ((org.apache.axis.client.Stub) managementService).getTimeout()) {
			((org.apache.axis.client.Stub) managementService).setTimeout(timeout);
		}
		return managementService;
	}

	protected EngineConfiguration getEngineConfiguration() {
		try {
			return new ResourceConfigurationProvider(AXIS_CONFIGURATION_RESOURCE);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public void setManagementService(ManagementService managementService) {
		this.managementService = managementService;
	}

	protected Object unmarshal(String xml) throws Exception {
		Object obj = null;
		ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
		try {
//			Thread.currentThread().setContextClassLoader(DOMParser.class.getClassLoader());
			obj = unmarshaller.unmarshal(xml);
		} finally {
			Thread.currentThread().setContextClassLoader(oldClassLoader);
		}
		return obj;
	}

	public void setTimeout(Integer timeout) {
		if (timeout != null && timeout > 0) {
			this.timeout = timeout;
		}
	}

}

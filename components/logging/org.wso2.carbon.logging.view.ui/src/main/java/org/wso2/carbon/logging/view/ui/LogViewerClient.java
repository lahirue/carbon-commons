/*
 * Copyright 2005-2007 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.logging.view.ui;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.logging.view.stub.LogViewerException;
import org.wso2.carbon.logging.view.stub.LogViewerLogViewerException;
import org.wso2.carbon.logging.view.stub.LogViewerStub;
import org.wso2.carbon.logging.view.stub.types.carbon.LogEvent;
import org.wso2.carbon.logging.view.stub.types.carbon.PaginatedLogEvent;
import org.wso2.carbon.logging.view.stub.types.carbon.PaginatedLogFileInfo;

import javax.activation.DataHandler;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;

public class LogViewerClient {
	private static final Log log = LogFactory.getLog(LogViewerClient.class);
	public LogViewerStub stub;

	public LogViewerClient(String cookie, String backendServerURL, ConfigurationContext configCtx)
			throws AxisFault {
		String serviceURL = backendServerURL + "LogViewer";
		stub = new LogViewerStub(configCtx, serviceURL);
		ServiceClient client = stub._getServiceClient();
		Options option = client.getOptions();
		option.setManageSession(true);
		option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
	}

	/**
	 * Provide all remote system logs
	 *
	 * @return
	 * @throws RemoteException
	 */
	public LogEvent[] getAllRemoteSystemLogs() throws RemoteException {

		try {
			return stub.getAllSystemLogs();
		} catch (RemoteException e) {
			log.error("Fail to get all logs ", e);
			throw new RemoteException("Fail to get all system logs ", e);
		}
	}

	/**
	 * Deprecated
	 *
	 * @return
	 * @throws RemoteException
	 */
	@Deprecated
	public LogEvent[] getAllSystemLogs() throws RemoteException {

		LogEvent[] logEvents;
		try {
			logEvents = stub.getAllSystemLogs();
		} catch (RemoteException e) {
			log.error("Fail to get all logs ", e);
			throw new RemoteException("Fail to get all system logs ", e);
		}
		return logEvents;
	}

	public void clearLogs() throws RemoteException {
		try {
			stub.clearLogs();
		} catch (RemoteException e) {
			String msg = "Error occurred while getting logger data. Backend service may be " +
			             "unavailable";
			log.error(msg, e);
			throw e;
		}
	}


	public void downloadArchivedLogFiles(String logFile, HttpServletResponse response,
	                                     String tenantDomain, String serverKey)
			throws RemoteException, LogViewerException {
		String msg = "Error occurred while getting logger data. Backend service may be " +
		             "unavailable";

		InputStream fileToDownload = null;
		try {
			logFile = logFile.replace(".gz", "");
			ServletOutputStream outputStream = response.getOutputStream();
			response.setContentType("application/txt");
			response.setHeader("Content-Disposition",
			                   "attachment;filename=" + logFile.replaceAll("\\s", "_"));
			DataHandler data = null;

				data = stub.downloadArchivedLogFiles(logFile, tenantDomain, serverKey);
			fileToDownload = data.getInputStream();
			int c;
			while ((c = fileToDownload.read()) != -1) {
				outputStream.write(c);
			}
			outputStream.flush();
			outputStream.flush();
		} catch (RemoteException e) {
			log.error(msg, e);
			throw e;
		}  catch (IOException e) {
			String errorWhileDownloadingMsg = "Error while downloading file.";
			log.error(errorWhileDownloadingMsg, e);
			throw new LogViewerException(errorWhileDownloadingMsg, e);
		} catch (LogViewerLogViewerException e) {
			log.error(msg, e);
			e.printStackTrace();
		} finally {
			try {
				if(fileToDownload != null) {
					fileToDownload.close();
				}
			} catch (IOException e) {
				log.error("Couldn't close the InputStream " + e.getMessage(), e);
			}
		}
	}

	public int getLineNumbers(String logFile)
			throws RemoteException, LogViewerException {
		String msg = "Error occurred while getting logger data. Backend service may be " +
		             "unavailable";
		try {
			return stub.getLineNumbers(logFile);
		} catch (RemoteException e) {
			log.error(msg, e);
			throw e;
		} catch (LogViewerException e) {
			log.error(msg, e);
			throw e;
		}
	}

	public PaginatedLogFileInfo getPaginatedLogFileInfo(int pageNumber, String tenantDomain,
	                                                    String serviceName)
			throws RemoteException, LogViewerLogViewerException {
		String msg = "Error occurred while getting logger data. Backend service may be " +
		             "unavailable";
		try {
			return stub.getPaginatedLogFileInfo(pageNumber, tenantDomain, serviceName);
		} catch (RemoteException e) {
			log.error(msg, e);
			throw e;
		} catch (LogViewerLogViewerException e) {
			log.error(msg, e);
			throw e;
		}
	}

	public LogEvent[] getLogs(String type, String keyword, String tenantDomain, String serverkey)
			throws RemoteException, LogViewerLogViewerException {
		String msg = "Error occurred while getting logger data. Backend service may be " +
		             "unavailable";
		if (type == null || type.equals("")) {
			type = "ALL";
		}
		try {
			return stub.getLogs(type, keyword, tenantDomain, serverkey);
		} catch (RemoteException e) {
			log.error(msg, e);
			throw e;
		} catch (LogViewerLogViewerException e) {
			log.error(msg, e);
			throw e;
		}
	}

	public String[] getLogLinesFromFile(String logFile, int maxLogs, int start, int end)
			throws RemoteException, LogViewerLogViewerException {
		try {
			return stub.getLogLinesFromFile(logFile, maxLogs, start, end);
		} catch (RemoteException e) {
			String msg = "Error occurred while getting logger data. Backend service may be " +
			             "unavailable";
			log.error(msg, e);
			throw e;
		} catch (LogViewerLogViewerException e) {
			String msg = "Error occurred while getting logger data. Backend service may be " +
			             "unavailable";
			log.error(msg, e);
			throw e;
		}
	}

	public int getNoOfLogEvents(String tenantDomain, String serverKey)
			throws RemoteException, LogViewerLogViewerException {
		String msg = "Error occurred while getting logger data. Backend service may be " +
		             "unavailable";

			return 2000;

	}

	public PaginatedLogEvent getPaginatedLogEvents(int pageNumber, String type, String keyword,
	                                               String tenantDomain, String serverKey)
			throws RemoteException, LogViewerLogViewerException {
		String msg = "Error occurred while getting logger data. Backend service may be " +
		             "unavailable";
		try {
			return stub.getPaginatedLogEvents(pageNumber, type, keyword, tenantDomain, serverKey);
		} catch (RemoteException e) {
			log.error(msg, e);
			throw e;
		} catch (LogViewerLogViewerException e) {
			log.error(msg, e);
			throw e;
		}
	}

	public PaginatedLogFileInfo getLocalLogFiles(int pageNo, String tenantDomain, String serverKey)
			throws RemoteException, LogViewerLogViewerException {

		String msg = "Error occurred while getting logger data. Backend service may be unavailable";
		try {
			return stub.getLocalLogFiles(pageNo, tenantDomain, serverKey);
		} catch (RemoteException e) {
			log.error(msg, e);
			throw e;
		} catch (LogViewerLogViewerException e) {
			log.error(msg, e);
			throw e;
		}

	}

	public String getImageName(String type) {
		if (type.equals("INFO")) {
			return "images/information.gif";
		} else if (type.equals("ERROR")) {
			return "images/error.png";
		} else if (type.equals("WARN")) {
			return "images/warn.png";
		} else if (type.equals("DEBUG")) {
			return "images/debug.png";
		} else if (type.equals("TRACE")) {
			return "images/trace.png";
		} else if (type.equals("FATAL")) {
			return "images/fatal.png";
		}
		return "";
	}

	public String[] getLogLevels() {
		return new String[]{"ALL", "FATAL", "ERROR", "WARN", "INFO", "DEBUG", "TRACE"};
	}

	public boolean isManager() throws RemoteException {
		return stub.isManager();
	}

	public boolean isValidTenant(String tenantDomain) throws RemoteException {
		return stub.isValidTenant(tenantDomain);

	}

}
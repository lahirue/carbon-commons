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

import java.rmi.RemoteException;

public class LogViewerClient {
	private static final Log log = LogFactory.getLog(LogViewerClient.class);
	public org.wso2.carbon.logging.view.stub.LogViewerStub stub;

	public LogViewerClient(String cookie, String backendServerURL, ConfigurationContext configCtx)
			throws AxisFault {
		String serviceURL = backendServerURL + "LogViewer";
		stub = new org.wso2.carbon.logging.view.stub.LogViewerStub(configCtx, serviceURL);
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
	public org.wso2.carbon.logging.view.data.xsd.LogEvent[] getAllRemoteSystemLogs() throws RemoteException {

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
	public org.wso2.carbon.logging.view.data.xsd.LogEvent[] getAllSystemLogs() throws RemoteException {

		org.wso2.carbon.logging.view.data.xsd.LogEvent[] logEvents;
		try {
			logEvents = stub.getAllSystemLogs();
		} catch (RemoteException e) {
			log.error("Fail to get all logs ", e);
			throw new RemoteException("Fail to get all system logs ", e);
		}
		return logEvents;
	}

	public void clearLogs() throws RemoteException {

		stub.clearLogs();
	}
}
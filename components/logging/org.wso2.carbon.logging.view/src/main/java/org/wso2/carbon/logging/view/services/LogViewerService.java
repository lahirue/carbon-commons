/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.carbon.logging.view.services;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.logging.view.*;
import org.wso2.carbon.logging.view.internal.DataHolder;
import org.wso2.carbon.logging.view.provider.api.LogFileProvider;
import org.wso2.carbon.logging.view.util.LoggingUtil;
import org.wso2.carbon.utils.DataPaginator;

import javax.activation.DataHandler;
import java.util.List;

/**
 * Log Viewer Service
 */
public class LogViewerService {

    private static LogFileProvider logFileProvider;
    private PaginatedLogMessageCarbon paginatedLogMessageCarbon = new PaginatedLogMessageCarbon();

    public PaginatedLogMessageCarbon getPaginatedLogMessageCarbon() {
        return paginatedLogMessageCarbon;
    }

    /**
     *  Return all logs in system
     * @return array of {@link LogEvent}
     */
    public LogEvent[] getAllSystemLogs() {

        List<LogEvent> logEventList = DataHolder.getInstance().getLogBuffer().get(2000);
        return logEventList.toArray(new LogEvent[logEventList.size()]);
    }

    public void clearLogs() {

        DataHolder.getInstance().getLogBuffer().clear();
    }


    public DataHandler downloadArchivedLogFiles(String logFile, String tenantDomain,
                                                String serverKey)
            throws LogViewerException {
        return logFileProvider.downloadLogFile(logFile, tenantDomain, serverKey);
    }

    public boolean isValidTenantDomain(String tenantDomain) {
        return true;
    }

    public boolean isManager() {
        return true;
    }

    public boolean isValidTenant(String tenantDomain) {
        return true;
    }

    public int getLineNumbers(String logFile) throws Exception {
        return LoggingUtil.getLineNumbers(logFile);
    }

    public String[] getLogLinesFromFile(String logFile, int maxLogs, int start, int end)
            throws LogViewerException {
        return LoggingUtil.getLogLinesFromFile(logFile, maxLogs, start, end);
    }

    public boolean isFileAppenderConfiguredForST() {
        Logger rootLogger = Logger.getRootLogger();
        FileAppender logger = (FileAppender) rootLogger.getAppender("CARBON_LOGFILE");
        return logger != null
               && CarbonContext.getThreadLocalCarbonContext()
                               .getTenantId() == MultitenantConstants.SUPER_TENANT_ID;
    }

    public PaginatedLogEvent getPaginatedLogEvents(int pageNumber, String type, String keyword,
                                                   String tenantDomain, String serverKey)
            throws LogViewerException {

        List<LogEvent> logMsgList = DataHolder.getInstance().getLogBuffer().get(2000);
        return getPaginatedLogEvent(pageNumber, logMsgList);
    }

    public PaginatedLogFileInfo getPaginatedLogFileInfo(int pageNumber, String tenantDomain,
                                                        String serviceName)
            throws LogViewerException {
        List<LogFileInfo> logFileInfoList = logFileProvider.getLogFileInfoList(tenantDomain,
                                                                               serviceName);
            return getPaginatedLogFileInfo(pageNumber, logFileInfoList);
    }

    public PaginatedLogFileInfo getLocalLogFiles(int pageNumber, String tenantDomain,
                                                 String serverKey) throws LogViewerException {
        List<LogFileInfo> logFileInfoList = logFileProvider
                .getLogFileInfoList(tenantDomain, serverKey);
        return getPaginatedLogFileInfo(pageNumber, logFileInfoList);
    }

    public LogEvent[] getLogs(String type, String keyword, String tenantDomain,
                              String serverKey) throws LogViewerException {

        List<LogEvent> logEventList = DataHolder.getInstance().getLogBuffer().get(2000);
        return logEventList.toArray(new LogEvent[logEventList.size()]);
    }

    private PaginatedLogFileInfo getPaginatedLogFileInfo(int pageNumber,
                                                         List<LogFileInfo> logFileInfoList) {
        if (logFileInfoList != null && !logFileInfoList.isEmpty()) {
            PaginatedLogFileInfo paginatedLogFileInfo = new PaginatedLogFileInfo();
            DataPaginator.doPaging(pageNumber, logFileInfoList, paginatedLogFileInfo);
            return paginatedLogFileInfo;
        } else {
            return null;
        }
    }

    private PaginatedLogEvent getPaginatedLogEvent(int pageNumber, List<LogEvent> logMsgList) {
        if (logMsgList != null && !logMsgList.isEmpty()) {
            PaginatedLogEvent paginatedLogEvent = new PaginatedLogEvent();
            DataPaginator.doPaging(pageNumber, logMsgList, paginatedLogEvent);
            return paginatedLogEvent;
        } else {
            return null;
        }
    }


}

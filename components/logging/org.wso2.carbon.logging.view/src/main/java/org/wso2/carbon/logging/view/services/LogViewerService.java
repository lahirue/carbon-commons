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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.wso2.carbon.base.MultitenantConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.logging.view.*;
import org.wso2.carbon.logging.view.internal.DataHolder;
import org.wso2.carbon.logging.view.provider.api.LogFileProvider;
import org.wso2.carbon.logging.view.provider.api.LogProvider;
import org.wso2.carbon.logging.view.util.LoggingConstants;
import org.wso2.carbon.logging.view.util.LoggingUtil;
import org.wso2.carbon.registry.core.RegistryConstants;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.DataPaginator;

import javax.activation.DataHandler;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Log Viewer Service
 */
public class LogViewerService {


    private PaginatedLogMessageCarbon paginatedLogMessageCarbon = new PaginatedLogMessageCarbon();

    public PaginatedLogMessageCarbon getPaginatedLogMessageCarbon() {
        return paginatedLogMessageCarbon;
    }

    private static final Log log = LogFactory.getLog(LogViewerService.class);
    private static final String LOGGING_CONFIG_FILE_WITH_PATH = CarbonUtils.getCarbonConfigDirPath()
                                                                + RegistryConstants.PATH_SEPARATOR
                                                                + LoggingConstants.ETC_DIR
                                                                + RegistryConstants.PATH_SEPARATOR
                                                                + LoggingConstants.LOGGING_CONF_FILE;
    private static LoggingConfig loggingConfig;
    private static LogFileProvider logFileProvider;
    private static LogProvider logProvider;

    // configured classes are loaded during LogViewer class load time
    // inside this static block.
    static {
        // load the configuration from the config file.
        loggingConfig = loadLoggingConfiguration();

        String lpClass = loggingConfig.getLogProviderImplClassName();
        loadLogProviderClass(lpClass);

        String lfpClass = loggingConfig.getLogFileProviderImplClassName();
        loadLogFileProviderClass(lfpClass);
    }

    /**
     * Load the LogProvider implementation as mentioned in the config file. This method is called
     * when this class is loaded. (Called within the static block)
     *
     * @param lpClass
     *         - Log Provider implementation class name
     */
    private static void loadLogProviderClass(String lpClass) {
        try {
            // initiate Log provider instance
            if (lpClass != null && !"".equals(lpClass)) {
                Class<?> logProviderClass = Class.forName(lpClass);
                Constructor<?> constructor = logProviderClass.getConstructor();
                logProvider = (LogProvider) constructor.newInstance();
                logProvider.init(loggingConfig);
            } else {
                String msg = "Log provider is not defined in logging configuration file : " +
                             LOGGING_CONFIG_FILE_WITH_PATH;
                throw new LoggingConfigReaderException(msg);
            }
        } catch (Exception e) {
            String msg = "Error while loading log provider implementation class: " + lpClass;
            log.error(msg, e);
            // A RuntimeException is thrown here since an Exception cannot be thrown from the static
            // block. An Exception occurs when the class could not be loaded. We cannot proceed
            // further in that case, therefore we throw a RuntimeException.
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Load the LogFileProvider implementation as mentioned in the config file. This method is
     * called when this class is loaded. (Called within the static block)
     *
     * @param lfpClass
     *         - Log File Provider implementation class name
     */
    private static void loadLogFileProviderClass(String lfpClass) {
        try {
            // initiate log file provider instance
            if (lfpClass != null && !"".equals(lfpClass)) {
                Class<?> logFileProviderClass = Class.forName(lfpClass);
                Constructor<?> constructor = logFileProviderClass.getConstructor();
                logFileProvider = (LogFileProvider) constructor.newInstance();
                logFileProvider.init(loggingConfig);
            } else {
                String msg = "Log file provider is not defined in logging configuration file : " +
                             LOGGING_CONFIG_FILE_WITH_PATH;
                throw new LoggingConfigReaderException(msg);
            }
        } catch (Exception e) {
            String msg = "Error while loading log file provider implementation class: " + lfpClass;
            log.error(msg, e);
            // A RuntimeException is thrown here since an Exception cannot be thrown from the static
            // block. An Exception occurs when the class could not be loaded. We cannot proceed
            // further in that case, therefore we throw a RuntimeException.
            throw new RuntimeException(msg, e);
        }
    }

    /**
     * Load logging configuration from the logging-config file. This method is called when this
     * class is loaded. (Called within the static block)
     *
     * @return - a LoggingConfig
     */
    private static LoggingConfig loadLoggingConfiguration() {
        try {
            return LoggingConfigManager.loadLoggingConfiguration(
                    LOGGING_CONFIG_FILE_WITH_PATH);
        } catch (IOException e) {
            String msg = "Error while reading the configuration file";
            log.error(msg, e);
            // We cannot proceed further without reading the logging config properly.
            // Therefore throw a runtime exception
            throw new RuntimeException(msg, e);
        } catch (XMLStreamException e) {
            String msg = "Error while parsing the configuration file";
            log.error(msg, e);
            // We cannot proceed further without reading the logging config properly.
            // Therefore throw a runtime exception
            throw new RuntimeException(msg, e);
        } catch (LoggingConfigReaderException e) {
            String msg = "Error while reading the configuration file";
            log.error(msg, e);
            // We cannot proceed further without reading the logging config properly.
            // Therefore throw a runtime exception
            throw new RuntimeException(msg, e);
        }
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

//        List<LogEvent> logMsgList = logProvider
//                .getLogs(type, keyword, null, tenantDomain, serverKey);
        List<LogEvent> logEventList = DataHolder.getInstance().getLogBuffer().get(2000);
        List<LogEvent> filteredLogEventList = logEventList.stream().filter(event-> event.getMessage().contains(keyword)).collect(
                Collectors.toList());


        return getPaginatedLogEvent(pageNumber, filteredLogEventList);
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

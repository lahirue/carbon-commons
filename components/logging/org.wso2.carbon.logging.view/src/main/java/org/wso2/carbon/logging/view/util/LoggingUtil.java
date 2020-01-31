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

package org.wso2.carbon.logging.view.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Appender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.logging.view.LogViewerException;
import org.wso2.carbon.utils.CarbonUtils;
import org.wso2.carbon.utils.Pageable;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

public class LoggingUtil {

	public static final String SYSTEM_LOG_PATTERN = "[%d] %5p - %x %m {%c}%n";
	private static final int MAX_LOG_MESSAGES = 200;
	private static final String CARBON_LOGFILE_APPENDER = "CARBON_LOGFILE";
	private static final Log log = LogFactory.getLog(LoggingUtil.class);

	public static boolean isFileAppenderConfiguredForST() {
		Logger rootLogger = Logger.getRootLogger();
		DailyRollingFileAppender logger =
				(DailyRollingFileAppender) rootLogger.getAppender(CARBON_LOGFILE_APPENDER);
		if (logger != null && CarbonContext.getThreadLocalCarbonContext().getTenantId() ==
		                      org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_ID) {
			return true;
		} else {
			return false;
		}
	}

	private static void addAppendersToSet(Enumeration appenders, Set<Appender> appenderSet) {
		while (appenders.hasMoreElements()) {
			Appender appender = (Appender) appenders.nextElement();
			appenderSet.add(appender);
		}
	}

	public static Appender getAppenderFromSet(Set<Appender> appenderSet, String name) {
		for (Appender appender : appenderSet) {
			if ((appender.getName() != null) && (appender.getName().equals(name))) {
				return appender;
			}
		}
		return null;
	}

	public static int getLineNumbers(String logFile) throws Exception {
		InputStream logStream;

		try {
			logStream = getLocalInputStream(logFile);
		} catch (IOException e) {
			throw new LogViewerException("Cannot find the specified file location to the log file",
			                             e);
		} catch (Exception e) {
			throw new LogViewerException("Cannot find the specified file location to the log file",
			                             e);
		}
		try {
			byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
			while ((readChars = logStream.read(c)) != -1) {
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						++count;
					}
				}
			}
			return count;
		} catch (IOException e) {
			throw new LogViewerException("Cannot read file size from the " + logFile, e);
		} finally {
			try {
				logStream.close();
			} catch (IOException e) {
				throw new LogViewerException("Cannot close the input stream " + logFile, e);
			}
		}
	}

	public static String[] getLogLinesFromFile(String logFile, int maxLogs, int start, int end)
			throws LogViewerException {
		ArrayList<String> logsList = new ArrayList<String>();
		InputStream logStream;
		if (end > maxLogs) {
			end = maxLogs;
		}
		try {
			logStream = getLocalInputStream(logFile);
		} catch (Exception e) {
			throw new LogViewerException("Cannot find the specified file location to the log file",
			                             e);
		}
		BufferedReader dataInput = new BufferedReader(new InputStreamReader(logStream));
		int index = 1;
		String line;
		try {
			while ((line = dataInput.readLine()) != null) {
				if (index <= end && index > start) {
					logsList.add(line);
				}
				index++;
			}
			dataInput.close();
		} catch (IOException e) {
			log.error("Cannot read the log file", e);
			throw new LogViewerException("Cannot read the log file", e);
		}
		return logsList.toArray(new String[logsList.size()]);
	}

	private static InputStream getLocalInputStream(String logFile)
			throws FileNotFoundException, LogViewerException {
		Path logFilePath = Paths.get(CarbonUtils.getCarbonLogsPath(), logFile);

		if (!isPathInsideBaseDirectory(Paths.get(CarbonUtils.getCarbonLogsPath()), logFilePath)) {
			throw new LogViewerException(
					"Specified log file path is outside carbon logs directory.");
		}

		FileAppender carbonLogFileAppender =
				(FileAppender) Logger.getRootLogger().getAppender(CARBON_LOGFILE_APPENDER);
		String carbonLogFileName = new File(carbonLogFileAppender.getFile()).getName();

		if (!logFilePath.getFileName().toString().startsWith(carbonLogFileName)) {
			throw new LogViewerException(
					"Trying to access logs other than CARBON_LOGFILE appender log file.");
		}

		InputStream is = new BufferedInputStream(new FileInputStream(logFilePath.toString()));
		return is;
	}

	/**
	 * This method stream log messages and retrieve 100 log messages per page
	 *
	 * @param pageNumber The page required. Page number starts with 0.
	 * @param sourceList The original list of items
	 * @param pageable   The type of Pageable item
	 * @return Returned page
	 */
	public static <C> List<C> doPaging(int pageNumber, List<C> sourceList, int maxLines,
	                                   Pageable pageable) {
		if (pageNumber < 0 || pageNumber == Integer.MAX_VALUE) {
			pageNumber = 0;
		}
		if (sourceList.size() == 0) {
			return sourceList;
		}
		if (pageNumber < 0) {
			throw new RuntimeException(
					"Page number should be a positive integer. " + "Page numbers begin at 0.");
		}
		int itemsPerPageInt = MAX_LOG_MESSAGES; // the default number of item
		// per page
		int numberOfPages = (int) Math.ceil((double) maxLines / itemsPerPageInt);
		if (pageNumber > numberOfPages - 1) {
			pageNumber = numberOfPages - 1;
		}
		List<C> returnList = new ArrayList<C>();
		for (int i = 0; i < sourceList.size(); i++) {
			returnList.add(sourceList.get(i));
		}
		int pages = calculatePageLevel(pageNumber + 1);
		if (pages > numberOfPages) {
			pages = numberOfPages;
		}
		pageable.setNumberOfPages(pages);
		pageable.set(returnList);
		return returnList;
	}

	/*
	 * This is an equation to retrieve the visible number of pages ie p1-p5 -> 5
	 * p6-p10 -> 10 p11-p15 -> 15
	 */
	private static int calculatePageLevel(int x) {
		int p = x / 5;
		int q = x % 5;
		int t = (p + 1) * 5;
		int s = (p * 5) + 1;
		int y = q > 0 ? t : s;
		return y;
	}

	/**
	 * Tests if the provided path is inside the base directory path.
	 *
	 * @param baseDirPath absolute {@link Path} of the base directory in which we want to check whether the given path
	 *                    is inside
	 * @param path        {@link Path} to be tested
	 * @return {@code true} if the given path is inside the base directory path, otherwise {@code false}
	 */
	private static boolean isPathInsideBaseDirectory(Path baseDirPath, Path path) {
		Path resolvedPath = baseDirPath.resolve(path).normalize();
		return resolvedPath.startsWith(baseDirPath.normalize());
	}
}


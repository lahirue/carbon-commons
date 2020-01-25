<!--
~ Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
~
~ WSO2 Inc. licenses this file to you under the Apache License,
~ Version 2.0 (the "License"); you may not use this file except
~ in compliance with the License.
~ You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing,
~ software distributed under the License is distributed on an
~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar"
           prefix="carbon" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.logging.view.stub.types.carbon.LogEvent" %>
<%-- <%@ page --%>
<!-- import="org.wso2.carbon.logging.view.stub.types.carbon.LogEvent"%> -->

<%@ page import="org.wso2.carbon.logging.view.ui.LogViewerClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>

<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>

<%@ page import="org.wso2.carbon.utils.ServerConstants" %>

<script type="text/javascript" src="js/logviewer.js"></script>
<script type="text/javascript" src="../admin/dialog/js/dialog.js"></script>


<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <title>View System Logs</title>
    <script type="text/JavaScript">
        function refresh(time) {
            setTimeout("location.reload(true);", time);
        }
    </script>
</head>
<body onload="JavaScript:refresh(120000);">

<%
    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext = (ConfigurationContext) config.getServletContext()
                                                                      .getAttribute(
                                                                              CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    LogViewerClient logViewerClient;
    LogEvent[] events = null;

    try {

        logViewerClient = new LogViewerClient(cookie, backendServerURL, configContext);
        events = logViewerClient.getAllSystemLogs();
%>
<h1> Logs List</h1>
<table class="styledLeft" id="appTable" width="100%">
    <thead>
    <tr>
        <th>Log Message</th>
        <th>Log Time</th>
    </tr>
    </thead>
    <tbody>
            <%
                for (LogEvent oneEvent : events) {
                    String message = oneEvent.getMessage();
                    String logTime = oneEvent.getLogTime();
            %>
            <tr>
                <td> <%=message%> </td>

                <td><%=logTime%></td>
             </tr>
            <%
                }
            %>
    </tbody>
</table>

<%



} catch (Exception e) {
    CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
%>
<script type="text/javascript">
    location.href = "../admin/error.jsp";
</script>
<%
        return;
    }
%>
<fmt:bundle basename="org.wso2.carbon.logging.view.ui.i18n.Resources">

    <carbon:breadcrumb label="system.logs"
                       resourceBundle="org.wso2.carbon.logging.ui.i18n.Resources"
                       topPage="true" request="<%=request%>"/>
    <div id="middle">
        <h1>Hello Middle</h1>
    </div>
</fmt:bundle>
</body>
</html>


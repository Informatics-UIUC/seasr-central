/*
 * University of Illinois/NCSA
 * Open Source License
 *
 * Copyright (c) 2008, NCSA.  All rights reserved.
 *
 * Developed by:
 * The Automated Learning Group
 * University of Illinois at Urbana-Champaign
 * http://www.seasr.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal with the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject
 * to the following conditions:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimers.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimers in
 * the documentation and/or other materials provided with the distribution.
 *
 * Neither the names of The Automated Learning Group, University of
 * Illinois at Urbana-Champaign, nor the names of its contributors may
 * be used to endorse or promote products derived from this Software
 * without specific prior written permission.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS WITH THE SOFTWARE.
 */

package org.seasr.central.ws.restlets.flow;

import com.google.gdata.util.ContentType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.seasr.central.storage.SCError;
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.storage.exceptions.FlowNotFoundException;
import org.seasr.central.storage.exceptions.GroupNotFoundException;
import org.seasr.central.storage.exceptions.UserNotFoundException;
import org.seasr.central.util.IdVersionPair;
import org.seasr.central.util.SCSecurity;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ContentTypes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.logging.Level;

import static org.seasr.central.util.Tools.sendErrorInternalServerError;
import static org.seasr.central.util.Tools.sendErrorNotAcceptable;

/**
 * Restlet for sharing a flow with a group
 *
 * @author Boris Capitanu
 */
public class ShareFlowRestlet extends AbstractBaseRestlet {

    private static final Map<String, ContentType> supportedResponseTypes = new HashMap<String, ContentType>();

    static {
        supportedResponseTypes.put("json", ContentType.JSON);
        supportedResponseTypes.put("xml", ContentType.APPLICATION_XML);
        supportedResponseTypes.put("html", ContentType.TEXT_HTML);
        supportedResponseTypes.put("txt", ContentType.TEXT_PLAIN);
        supportedResponseTypes.put("sgwt", ContentTypes.SmartGWT);
    }

    @Override
    public Map<String, ContentType> getSupportedResponseTypes() {
        return supportedResponseTypes;
    }

    @Override
    public String getRestContextPathRegexp() {
        return "/services/groups/([^/\\s]+)/flows(?:/|" + regexExtensionMatcher() + ")?$";
    }

    @Override
    public boolean process(HttpServletRequest request, HttpServletResponse response, String method, String... values) {
        // Check for POST
        if (!method.equalsIgnoreCase("POST")) return false;

        ContentType ct = getDesiredResponseContentType(request);
        if (ct == null) {
            sendErrorNotAcceptable(response);
            return true;
        }

        JSONArray jaSuccess = new JSONArray();
        JSONArray jaErrors = new JSONArray();

        UUID remoteUserId;
        String remoteUser = request.getRemoteUser();

        //TODO: for test purposes
        if (request.getParameterMap().containsKey("remoteUser") && request.getParameter("remoteUser").trim().length() > 0)
            remoteUser = request.getParameter("remoteUser");

        String[] flowIds = request.getParameterValues("flow");
        String[] flowVersions = request.getParameterValues("version");

        // Check for proper request
        if (!(flowIds != null && flowVersions != null && flowIds.length == flowVersions.length)) {
            jaErrors.put(SCError.createErrorObj(SCError.INCOMPLETE_REQUEST, bsl));
            sendResponse(jaSuccess, jaErrors, ct, response);
            return true;
        }

        try {
            UUID groupId;
            String groupName;

            try {
                Properties groupProps = getGroupNameAndId(values[0]);
                groupId = UUID.fromString(groupProps.getProperty("uuid"));
                groupName = groupProps.getProperty("name");

                remoteUserId = bsl.getUserId(remoteUser);
            }
            catch (BackendStoreException e) {
                logger.log(Level.SEVERE, null, e);
                jaErrors.put(SCError.createErrorObj(SCError.BACKEND_ERROR, e, bsl));
                sendResponse(jaSuccess, jaErrors, ct, response);
                return true;
            }

            Set<IdVersionPair> flows = new HashSet<IdVersionPair>(flowIds.length);

            for (int i = 0, iMax = flowIds.length; i < iMax; i++) {
                String sFlowId = flowIds[i];
                String sVersion = flowVersions[i];

                try {
                    UUID flowId = UUID.fromString(sFlowId);
                    int version = Integer.parseInt(sVersion);

                    flows.add(new IdVersionPair(flowId, version));
                }
                catch (IllegalArgumentException e) {
                    JSONObject joError = SCError.createErrorObj(SCError.INVALID_PARAM_VALUE, e, bsl);
                    joError.put("uuid", sFlowId);
                    joError.put("version", sVersion);
                    jaErrors.put(joError);
                    continue;
                }
            }

            for (IdVersionPair flow : flows) {
                UUID flowId = flow.getId();
                int version = flow.getVersion();

                try {
                    // Check permissions
                    if (!SCSecurity.canShareFlow(flowId, version, remoteUserId, bsl, request)) {
                        JSONObject joError = SCError.createErrorObj(SCError.UNAUTHORIZED, bsl);
                        joError.put("uuid", flowId.toString());
                        joError.put("version", version);
                        jaErrors.put(joError);
                        continue;
                    }

                    // Share the flow
                    bsl.shareFlow(flowId, version, groupId, remoteUserId);

                    JSONObject joFlow = new JSONObject();
                    joFlow.put("uuid", flowId.toString());
                    joFlow.put("version", version);
                    jaSuccess.put(joFlow);
                }
                catch (FlowNotFoundException e) {
                    JSONObject joError = SCError.createErrorObj(SCError.FLOW_NOT_FOUND, bsl,
                            flowId.toString(), Integer.toString(version));
                    joError.put("uuid", flowId.toString());
                    joError.put("version", version);
                    jaErrors.put(joError);
                    continue;
                }
                catch (BackendStoreException e) {
                    JSONObject joError = SCError.createErrorObj(SCError.BACKEND_ERROR, e, bsl);
                    joError.put("uuid", flowId.toString());
                    joError.put("version", version);
                    jaErrors.put(joError);
                    continue;
                }
            }
        }
        catch (GroupNotFoundException e) {
            jaErrors.put(SCError.createErrorObj(SCError.GROUP_NOT_FOUND, bsl, values[0]));
            sendResponse(jaSuccess, jaErrors, ct, response);
            return true;
        }
        catch (UserNotFoundException e) {
            logger.log(Level.WARNING, String.format("Cannot obtain user id for authenticated user '%s'!", remoteUser));
            jaErrors.put(SCError.createErrorObj(SCError.UNAUTHORIZED, e, bsl));
            sendResponse(jaSuccess, jaErrors, ct, response);
            return true;
        }
        catch (JSONException e) {
            // Should not happen
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
            return true;
        }

        // Send the response
        sendResponse(jaSuccess, jaErrors, ct, response);

        return true;
    }
}

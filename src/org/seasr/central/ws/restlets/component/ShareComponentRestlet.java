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

package org.seasr.central.ws.restlets.component;

import com.google.gdata.util.ContentType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.seasr.central.storage.SCError;
import org.seasr.central.storage.SCRole;
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.util.IdVersionPair;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ContentTypes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.logging.Level;

import static org.seasr.central.util.Tools.sendErrorInternalServerError;
import static org.seasr.central.util.Tools.sendErrorNotAcceptable;

/**
 * Restlet for sharing a component with a group
 *
 * @author Boris Capitanu
 */
public class ShareComponentRestlet extends AbstractBaseRestlet {

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
        return "/services/groups/([^/\\s]+)/components(?:/|" + regexExtensionMatcher() + ")?$";
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

        UUID groupId;
        @SuppressWarnings("unused")
        String groupName = null;

        try {
            Properties groupProps = getGroupNameAndId(values[0]);
            if (groupProps != null) {
                groupId = UUID.fromString(groupProps.getProperty("uuid"));
                groupName = groupProps.getProperty("name");
            } else {
                // Specified group does not exist
                jaErrors.put(SCError.createErrorObj(SCError.GROUP_NOT_FOUND, bsl, values[0]));
                sendResponse(jaSuccess, jaErrors, ct, response);
                return true;
            }

            remoteUserId = (remoteUser != null) ? bsl.getUserId(remoteUser) : null;
        }
        catch (BackendStoreException e) {
            logger.log(Level.SEVERE, null, e);
            jaErrors.put(SCError.createErrorObj(SCError.BACKEND_ERROR, e, bsl));
            sendResponse(jaSuccess, jaErrors, ct, response);
            return true;
        }

        String[] compIds = request.getParameterValues("component");
        String[] compVersions = request.getParameterValues("version");

        // Check for proper request
        if (!(compIds != null && compVersions != null && compIds.length == compVersions.length)) {
            jaErrors.put(SCError.createErrorObj(SCError.INCOMPLETE_REQUEST, bsl));
            sendResponse(jaSuccess, jaErrors, ct, response);
            return true;
        }

        try {
            Set<IdVersionPair> components = new HashSet<IdVersionPair>(compIds.length);

            for (int i = 0, iMax = compIds.length; i < iMax; i++) {
                try {
                    UUID compId = UUID.fromString(compIds[i]);
                    int version = Integer.parseInt(compVersions[i]);

                    components.add(new IdVersionPair(compId, version));
                }
                catch (IllegalArgumentException e) {
                    JSONObject joError = SCError.createErrorObj(SCError.INVALID_PARAM_VALUE, e, bsl);
                    joError.put("uuid", compIds[i]);
                    joError.put("version", compVersions[i]);
                    jaErrors.put(joError);
                    return true;
                }
            }

            for (IdVersionPair component : components) {
                UUID compId = component.getId();
                int version = component.getVersion();

                try {
                    // Check whether the user making the request is the owner of the component(s)
                    UUID ownerId = bsl.getComponentOwner(compId, version);
                    if (ownerId != null) {
                        // Check permissions
                        if ((ownerId.equals(remoteUserId) && bsl.isGroupMember(ownerId, groupId))
                                || request.isUserInRole(SCRole.ADMIN.name())) {

                            bsl.shareComponent(compId, version, groupId, remoteUserId);

                            JSONObject joComponent = new JSONObject();
                            joComponent.put("uuid", compId.toString());
                            joComponent.put("version", version);
                            jaSuccess.put(joComponent);
                        } else {
                            // Not the owner of this component version
                            JSONObject joError = SCError.createErrorObj(SCError.UNAUTHORIZED, bsl);
                            joError.put("uuid", compId.toString());
                            joError.put("version", version);
                            jaErrors.put(joError);
                        }
                    } else {
                        // Component version not found
                        JSONObject joError = SCError.createErrorObj(SCError.COMPONENT_NOT_FOUND, bsl,
                            compId.toString(), Integer.toString(version));
                        joError.put("uuid", compId.toString());
                        joError.put("version", version);
                        jaErrors.put(joError);
                    }
                }
                catch (BackendStoreException e) {
                    JSONObject joError = SCError.createErrorObj(SCError.BACKEND_ERROR, e, bsl);
                    joError.put("uuid", compId.toString());
                    joError.put("version", version);
                    jaErrors.put(joError);
                }
            }
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

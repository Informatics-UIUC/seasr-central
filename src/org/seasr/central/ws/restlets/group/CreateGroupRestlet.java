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

package org.seasr.central.ws.restlets.group;

import com.google.gdata.util.ContentType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.seasr.central.storage.SCError;
import org.seasr.central.storage.SCRole;
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.util.Validator;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ContentTypes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;

import static org.seasr.central.util.Tools.sendErrorInternalServerError;
import static org.seasr.central.util.Tools.sendErrorNotAcceptable;

/**
 * Restlet for creating groups
 *
 * @author Boris Capitanu
 */
public class CreateGroupRestlet extends AbstractBaseRestlet {

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
        return "/services/users/([^/\\s]+)/groups/?(?:" + regexExtensionMatcher() + ")?$";
    }

    @Override
    public boolean process(HttpServletRequest request, HttpServletResponse response, String method, String... values) {
        // check for POST
        if (!method.equalsIgnoreCase("POST")) return false;

        ContentType ct = getDesiredResponseContentType(request);
        if (ct == null) {
            sendErrorNotAcceptable(response);
            return true;
        }

        JSONArray jaSuccess = new JSONArray();
        JSONArray jaErrors = new JSONArray();

        String[] groupNames = request.getParameterValues("name");
        String[] profiles = request.getParameterValues("profile");

        // Check for proper request format
        if (!(groupNames != null && profiles != null && groupNames.length == profiles.length)) {
            jaErrors.put(SCError.createErrorObj(SCError.INCOMPLETE_REQUEST, bsl));
            sendResponse(jaSuccess, jaErrors, ct, response);
            return true;
        }

        UUID remoteUserId;
        String remoteUser = request.getRemoteUser();

        //TODO: for test purposes
        if (request.getParameterMap().containsKey("remoteUser") && request.getParameter("remoteUser").trim().length() > 0)
            remoteUser = request.getParameter("remoteUser");

        UUID userId;
        @SuppressWarnings("unused")
        String screenName = null;

        try {
            Properties userProps = getUserScreenNameAndId(values[0]);
            if (userProps != null) {
                userId = UUID.fromString(userProps.getProperty("uuid"));
                screenName = userProps.getProperty("screen_name");
            } else {
                // Specified user does not exist
                jaErrors.put(SCError.createErrorObj(SCError.USER_NOT_FOUND, bsl, values[0]));
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

        // Check permissions
        if (!(request.isUserInRole(SCRole.ADMIN.name()) || userId.equals(remoteUserId))) {
            jaErrors.put(SCError.createErrorObj(SCError.UNAUTHORIZED, bsl));
            sendResponse(jaSuccess, jaErrors, ct, response);
            return true;
        }

        UUID groupId;

        try {
            for (int i = 0, iMax = groupNames.length; i < iMax; i++) {
                String groupName = groupNames[i];

                // Check for valid group name
                if (!Validator.isValidGroupName(groupName)) {
                    JSONObject joError = SCError.createErrorObj(SCError.INVALID_GROUP_NAME, bsl, groupName);
                    joError.put("name", groupName);
                    jaErrors.put(joError);
                    continue;
                }

                try {
                    // Check if another group with the same name exists
                    groupId = bsl.getGroupId(groupName);
                    if (groupId != null) {
                        JSONObject joError = SCError.createErrorObj(SCError.GROUP_NAME_EXISTS, bsl, groupName);
                        joError.put("uuid", groupId);
                        joError.put("created_at", bsl.getGroupCreationTime(groupId));
                        joError.put("profile", bsl.getGroupProfile(groupId));

                        jaErrors.put(joError);
                        continue;
                    }

                    JSONObject joProfile;
                    try {
                        joProfile = new JSONObject(profiles[i]);
                    }
                    catch (JSONException e) {
                        JSONObject joError = SCError.createErrorObj(SCError.GROUP_PROFILE_ERROR, e, bsl);
                        joError.put("name", groupName);
                        jaErrors.put(joError);
                        continue;
                    }

                    // Add the group to the backend store
                    groupId = bsl.createGroup(userId, groupName, joProfile);

                    // Group added successfully
                    JSONObject joGroup = new JSONObject();
                    joGroup.put("uuid", groupId.toString());
                    joGroup.put("name", groupName);
                    joGroup.put("created_at", bsl.getGroupCreationTime(groupId));
                    joGroup.put("profile", joProfile);

                    jaSuccess.put(joGroup);
                }
                catch (BackendStoreException e) {
                    logger.log(Level.SEVERE, null, e);

                    JSONObject joError = SCError.createErrorObj(SCError.BACKEND_ERROR, e, bsl);
                    joError.put("name", groupName);
                    jaErrors.put(joError);
                    continue;
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

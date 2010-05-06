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
import org.seasr.central.storage.exceptions.GroupNotFoundException;
import org.seasr.central.storage.exceptions.UserNotFoundException;
import org.seasr.central.util.SCSecurity;
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
 * Restlet for joining a user to a group
 *
 * @author Boris Capitanu
 */
public class AddGroupMemberRestlet extends AbstractBaseRestlet {

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
        return "/services/groups/([^/\\s]+)/members(?:/|" + regexExtensionMatcher() + ")?$";
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

        String[] users = request.getParameterValues("user");
        String[] roles = request.getParameterValues("role");

        // Check for proper request format
        if (!(users != null && roles != null && users.length == roles.length)) {
            jaErrors.put(SCError.createErrorObj(SCError.INCOMPLETE_REQUEST, bsl));
            sendResponse(jaSuccess, jaErrors, ct, response);
            return true;
        }

        try {
            Properties groupProps = getGroupNameAndId(values[0]);
            UUID groupId = UUID.fromString(groupProps.getProperty("uuid"));
            String groupName = groupProps.getProperty("name");

            try {
                remoteUserId = bsl.getUserId(remoteUser);

                // Check permissions
                if (!SCSecurity.canAddGroupMember(groupId, remoteUserId, bsl, request)) {
                    jaErrors.put(SCError.createErrorObj(SCError.UNAUTHORIZED, bsl));
                    sendResponse(jaSuccess, jaErrors, ct, response);
                    return true;
                }
            }
            catch (UserNotFoundException e) {
                logger.log(Level.WARNING, String.format("Cannot obtain user id for authenticated user '%s'!", remoteUser));
                jaErrors.put(SCError.createErrorObj(SCError.UNAUTHORIZED, e, bsl));
                sendResponse(jaSuccess, jaErrors, ct, response);
                return true;
            }

            for (int i = 0, iMax = users.length; i < iMax; i++) {
                String sUser = users[i];
                String sRole = roles[i].toUpperCase();

                try {
                    // Try to obtain the user id and role
                    Properties userProps = getUserScreenNameAndId(sUser);
                    UUID userId = UUID.fromString(userProps.getProperty("uuid"));
                    SCRole role = SCRole.valueOf(sRole);

                    bsl.addGroupMember(userId, groupId, role);

                    JSONObject jo = new JSONObject();
                    jo.put("uuid", userProps.getProperty("uuid"));
                    jo.put("role", sRole);
                    jaSuccess.put(jo);
                }
                catch (UserNotFoundException e) {
                    JSONObject joError = SCError.createErrorObj(SCError.USER_NOT_FOUND, bsl, sUser);
                    joError.put("uuid", groupId.toString());
                    joError.put("user", sUser);
                    joError.put("role", sRole);
                    jaErrors.put(joError);
                    continue;
                }
                catch (IllegalArgumentException e) {
                    // Role unknown
                    JSONObject joError = SCError.createErrorObj(SCError.UNKNOWN_ROLE, bsl, sRole);
                    joError.put("uuid", groupId.toString());
                    joError.put("user", sUser);
                    joError.put("role", sRole);
                    jaErrors.put(joError);
                    continue;
                }
                catch (BackendStoreException e) {
                    logger.log(Level.SEVERE, null, e);

                    JSONObject joError = SCError.createErrorObj(SCError.BACKEND_ERROR, e, bsl);
                    joError.put("uuid", groupId.toString());
                    joError.put("user", sUser);
                    joError.put("role", sRole);
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
        catch (BackendStoreException e) {
            logger.log(Level.SEVERE, null, e);
            jaErrors.put(SCError.createErrorObj(SCError.BACKEND_ERROR, e, bsl));
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

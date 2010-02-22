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

package org.seasr.central.ws.restlets.user;

import com.google.gdata.util.ContentType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ContentTypes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;

import static org.seasr.central.util.Tools.*;

/**
 * @author Boris Capitanu
 */
public class JoinGroupRestlet extends AbstractBaseRestlet {

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
        return "/services/users/(.+)/groups/([a-f\\d]{8}(?:-[a-f\\d]{4}){3}-[a-f\\d]{12})/?" +
                "(?:" + regexExtensionMatcher() + ")?$";
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

        UUID userId;
        @SuppressWarnings("unused")
        String screenName = null;

        try {
            Properties userProps = getUserScreenNameAndId(values[0]);
            if (userProps != null) {
                userId = UUID.fromString(userProps.getProperty("uuid"));
                screenName = userProps.getProperty("screen_name");
            } else {
                sendErrorNotFound(response);
                return true;
            }
        }
        catch (BackendStoreException e) {
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
            return true;
        }

        UUID groupId;
        try {
            groupId = UUID.fromString(values[1]);
        }
        catch (IllegalArgumentException e) {
            sendErrorBadRequest(response);
            return true;
        }

        JSONArray jaSuccess = new JSONArray();
        JSONArray jaErrors = new JSONArray();

        try {
            try {
                bsl.requestJoinGroup(userId, groupId);
            }
            catch (BackendStoreException e) {

            }

            JSONObject joContent = new JSONObject();
            joContent.put(OperationResult.SUCCESS.name(), jaSuccess);
            joContent.put(OperationResult.FAILURE.name(), jaErrors);

            if (jaErrors.length() == 0)
                response.setStatus(HttpServletResponse.SC_CREATED);
            else
                response.setStatus(HttpServletResponse.SC_OK);

            try {
                sendContent(response, joContent, ct);
            }
            catch (IOException e) {
                logger.log(Level.WARNING, null, e);
            }
        }
        catch (JSONException e) {
            // Should not happen
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
            return true;
        }

        return true;
    }
}

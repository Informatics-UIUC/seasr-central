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

package org.seasr.central.ws.restlets;

import com.google.gdata.util.ContentType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.seasr.central.storage.BackendStoreLink;
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.util.Tools;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.seasr.central.util.Tools.sendContent;
import static org.seasr.central.util.Tools.sendErrorInternalServerError;

/**
 * Base restlet that provides common functionality
 *
 * @author Xavier Llora
 * @author Boris Capitanu
 */
public abstract class AbstractBaseRestlet implements RestServlet {

    /** The restlet logger */
    protected Logger logger;

    /** The back end storage link */
    protected BackendStoreLink bsl;

    private static final Pattern patternExtension;


    static {
        patternExtension = Pattern.compile("\\.([a-z]+)$");
    }

    @Override
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void setBackendStoreLink(BackendStoreLink bsl) {
        this.bsl = bsl;
    }

    /**
     * Returns the supported 'Content-Type' values for this restlet
     * format is: <extension, ContentType>
     *
     * @return The supported content types for this restlet
     */
    public abstract Map<String, ContentType> getSupportedResponseTypes();

    /**
     * Returns a partial regular expression that matches the supported resource extensions
     * Example: \\.txt|\\.json|\\.xml   ---- matches .txt or .json or .xml
     *
     * @return A partial regular expression that matches the supported resource extensions
     */
    public String regexExtensionMatcher() {
        StringBuffer sb = new StringBuffer();
        for (String ext : getSupportedResponseTypes().keySet())
            sb.append("|\\.").append(ext);

        return sb.toString().substring(1);
    }

    /**
     * Returns the desired content type for this request
     *
     * @param request The request
     * @return The content type desired for the response, or null if couldn't determine
     */
    public ContentType getDesiredResponseContentType(HttpServletRequest request) {
        ContentType ct = null;

        Map<String, ContentType> allowedTypes = getSupportedResponseTypes();
        String reqUri = request.getRequestURI().toLowerCase();
        Matcher m = patternExtension.matcher(reqUri);
        if (m.find())
            ct = allowedTypes.get(m.group(1));
        else {
            String accept = request.getHeader("Accept");
            if (accept != null)
                ct = ContentType.getBestContentType(accept,
                        new ArrayList<ContentType>(allowedTypes.values()));
        }

        return ct;
    }

    /**
     * Interprets a string representing either a screen name or a user id
     *
     * @param screenNameOrUserId The string representing a screen name or a user id
     * @return A property map keyed on "uuid" and "screen_name", or null if either could not be determined
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public Properties getUserScreenNameAndId(String screenNameOrUserId) throws BackendStoreException {
        String screenName;
        UUID userId;

        try {
            userId = UUID.fromString(screenNameOrUserId);
            screenName = bsl.getUserScreenName(userId);
        }
        catch (IllegalArgumentException e) {
            screenName = screenNameOrUserId;
            userId = bsl.getUserId(screenName);
        }

        if (userId == null || screenName == null)
            return null;

        Properties result = new Properties();
        result.put("uuid", userId.toString());
        result.put("screen_name", screenName);

        return result;
    }

    /**
     * Interprets a string representing either a group name or a group id
     *
     * @param groupNameOrId The string representing a group name or a group id
     * @return A property map keyed on "uuid" and "name", or null if either could not be determined
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public Properties getGroupNameAndId(String groupNameOrId) throws BackendStoreException {
        String groupName;
        UUID groupId;

        try {
            groupId = UUID.fromString(groupNameOrId);
            groupName = bsl.getGroupName(groupId);
        }
        catch (IllegalArgumentException e) {
            groupName = groupNameOrId;
            groupId = bsl.getGroupId(groupName);
        }

        if (groupId == null || groupName == null)
            return null;

        Properties result = new Properties();
        result.put("uuid", groupId.toString());
        result.put("name", groupName);

        return result;
    }

    /**
     * Packages and sends a response to a API request
     *
     * @param jaSuccess The responses successfuly generated
     * @param jaErrors The responses that failed
     * @param ct The desired response content type
     * @param response The response object
     */
    protected void sendResponse(JSONArray jaSuccess, JSONArray jaErrors, ContentType ct, HttpServletResponse response) {
        try {
            JSONObject joContent = new JSONObject();
            joContent.put(Tools.OperationResult.SUCCESS.name(), jaSuccess);
            joContent.put(Tools.OperationResult.FAILURE.name(), jaErrors);

            sendContent(response, joContent, ct);
        }
        catch (IOException e) {
            logger.log(Level.WARNING, null, e);
        }
        catch (JSONException e) {
            // Should not happen
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
        }
    }
}

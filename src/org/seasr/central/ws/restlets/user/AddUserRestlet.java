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
import org.seasr.central.main.SC;
import org.seasr.central.storage.Event;
import org.seasr.central.storage.SourceType;
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ContentTypes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.seasr.central.util.Tools.*;

/**
 * Restlet for adding users
 *
 * @author Xavier Llora
 * @author Boris Capitanu
 */
public class AddUserRestlet extends AbstractBaseRestlet {

    private static final Logger logger = Logger.getLogger(SC.class.getName());
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
        return "/services/users/(?:" + regexExtensionMatcher() + ")?$";
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

        Map<String, String[]> map = extractTextPayloads(request);

        // check for proper request
        if (!(map.containsKey("screen_name") && map.containsKey("password") && map.containsKey("profile")
                && map.get("screen_name").length == map.get("password").length
                && map.get("password").length == map.get("profile").length)) {

            sendErrorExpectationFail(response);
            return true;
        }

        String[] screenNames = map.get("screen_name");
        String[] passwords = map.get("password");
        String[] profiles = map.get("profile");

        UUID userId;

        JSONArray jaSuccess = new JSONArray();
        JSONArray jaErrors = new JSONArray();

        try {
            for (int i = 0, iMax = screenNames.length; i < iMax; i++) {
                try {
                    // Check if another user with the same screen name exists
                    userId = bsl.getUserId(screenNames[i]);
                    if (userId != null) {
                        JSONObject joError = createJSONErrorObj(
                                String.format("Unable to add user '%s'", screenNames[i]),
                                String.format("Screen name '%s' already exists", screenNames[i]));
                        joError.put("uuid", userId);
                        joError.put("created_at", bsl.getUserCreationTime(userId));
                        joError.put("profile", bsl.getUserProfile(userId));

                        jaErrors.put(joError);
                        continue;
                    }

                    JSONObject joProfile;
                    try {
                        joProfile = new JSONObject(profiles[i]);
                    }
                    catch (JSONException e) {
                        // Could not decode the user profile
                        logger.log(Level.WARNING, "Could not decode the user profile", e);
                        sendErrorBadRequest(response);
                        return true;
                    }

                    // Add the user to the backend store
                    userId = bsl.addUser(screenNames[i], passwords[i], joProfile);

                    // User added successfully
                    JSONObject joUser = new JSONObject();
                    joUser.put("uuid", userId.toString());
                    joUser.put("screen_name", screenNames[i]);
                    joUser.put("created_at", bsl.getUserCreationTime(userId));
                    joUser.put("profile", joProfile);

                    jaSuccess.put(joUser);

                    // Record this event
                    bsl.addEvent(SourceType.USER, userId, Event.USER_CREATED, joUser);
                }
                catch (BackendStoreException e) {
                    logger.log(Level.SEVERE, null, e);

                    jaErrors.put(createJSONErrorObj(String.format("Unable to add user '%s'", screenNames[i]), e));
                }
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

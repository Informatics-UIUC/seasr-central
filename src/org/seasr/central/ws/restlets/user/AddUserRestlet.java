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
import org.seasr.central.storage.SCError;
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.storage.exceptions.UserNotFoundException;
import org.seasr.central.util.SCValidator;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ContentTypes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import static org.seasr.central.util.Tools.sendErrorInternalServerError;
import static org.seasr.central.util.Tools.sendErrorNotAcceptable;

/**
 * Restlet for adding users
 *
 * @author Xavier Llora
 * @author Boris Capitanu
 */
public class AddUserRestlet extends AbstractBaseRestlet {

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
        return "/services/users(?:/|" + regexExtensionMatcher() + ")?$";
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

        String[] screenNames = request.getParameterValues("screen_name");
        String[] passwords = request.getParameterValues("password");
        String[] profiles = request.getParameterValues("profile");

        // Check for proper request format
        if (!(screenNames != null && passwords != null && profiles != null &&
                screenNames.length == passwords.length && screenNames.length == profiles.length)) {
            jaErrors.put(SCError.createErrorObj(SCError.INCOMPLETE_REQUEST, bsl));
            sendResponse(jaSuccess, jaErrors, ct, response);
            return true;
        }

        UUID userId;

        try {
            for (int i = 0, iMax = screenNames.length; i < iMax; i++) {
                String screenName = screenNames[i];

                // Check for valid screen name
                if (!SCValidator.isValidScreenName(screenName)) {
                    JSONObject joError = SCError.createErrorObj(SCError.INVALID_SCREEN_NAME, bsl, screenName);
                    joError.put("screen_name", screenName);
                    jaErrors.put(joError);
                    continue;
                }

                try {
                    // Check if another user with the same screen name exists
                    try {
                        userId = bsl.getUserId(screenName);

                        JSONObject joError = SCError.createErrorObj(SCError.SCREEN_NAME_EXISTS, bsl, screenName);
                        joError.put("screen_name", screenName);
                        joError.put("uuid", userId);
                        joError.put("created_at", bsl.getUserCreationTime(userId));
                        joError.put("profile", bsl.getUserProfile(userId));
                        jaErrors.put(joError);
                        continue;
                    }
                    catch (UserNotFoundException e) {
                        // This is ok - it means there's no collision with other users
                    }

                    JSONObject joProfile;
                    try {
                        joProfile = new JSONObject(profiles[i]);
                    }
                    catch (JSONException e) {
                        // Could not decode the user profile
                        JSONObject joError = SCError.createErrorObj(SCError.USER_PROFILE_ERROR, e, bsl);
                        joError.put("screen_name", screenName);
                        jaErrors.put(joError);
                        continue;
                    }

                    // Add the user to the backend store
                    userId = bsl.addUser(screenName, passwords[i], joProfile);

                    // User added successfully
                    JSONObject joUser = new JSONObject();
                    joUser.put("uuid", userId.toString());
                    joUser.put("screen_name", screenName);
                    joUser.put("created_at", bsl.getUserCreationTime(userId));
                    joUser.put("profile", joProfile);

                    jaSuccess.put(joUser);
                }
                catch (UserNotFoundException e) {
                    // Should never happen
                    JSONObject joError = SCError.createErrorObj(SCError.USER_NOT_FOUND, e, bsl);
                    joError.put("screen_name", screenName);
                    jaErrors.put(joError);
                    continue;
                }
                catch (BackendStoreException e) {
                    logger.log(Level.SEVERE, null, e);

                    JSONObject joError = SCError.createErrorObj(SCError.BACKEND_ERROR, e, bsl);
                    joError.put("screen_name", screenName);
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

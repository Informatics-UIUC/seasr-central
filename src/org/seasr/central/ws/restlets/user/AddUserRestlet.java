/**
 *
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
 *
 */

package org.seasr.central.ws.restlets.user;

import static org.seasr.central.ws.restlets.Tools.extractTextPayloads;
import static org.seasr.central.ws.restlets.Tools.logger;
import static org.seasr.central.ws.restlets.Tools.sendContent;
import static org.seasr.central.ws.restlets.Tools.sendErrorBadRequest;
import static org.seasr.central.ws.restlets.Tools.sendErrorExpectationFail;
import static org.seasr.central.ws.restlets.Tools.sendErrorNotAcceptable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.seasr.central.storage.Event;
import org.seasr.central.storage.SourceType;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ContentTypes;
import org.seasr.central.ws.restlets.Tools.OperationResult;

import com.google.gdata.util.ContentType;

/**
 * This servlet implements add user functionality.
 *
 * @author xavier
 * @author Boris Capitanu
 *
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

		UUID uuid;

		JSONArray jaSuccess = new JSONArray();
		JSONArray jaErrors = new JSONArray();

		for (int i = 0, iMax = screenNames.length; i < iMax; i++) {
		    try {
		        // attempt to add the user
		        JSONObject joProfile = new JSONObject(profiles[i]);
		        uuid = bsl.addUser(screenNames[i], passwords[i], joProfile);

		        if (uuid != null) {
                    // User added successfully
                    JSONObject joUser = new JSONObject();
                    joUser.put("uuid", uuid.toString());
                    joUser.put("screen_name", screenNames[i]);
                    joUser.put("created_at", bsl.getUserCreationTime(uuid));
                    joUser.put("profile", joProfile);

                    if (!bsl.addEvent(SourceType.USER, uuid, Event.USER_CREATED, joUser))
                        logger.warning(String.format("Could not record the %s event for user: %s (%s)",
                                Event.USER_CREATED, screenNames[i], uuid));

                    jaSuccess.put(joUser);
		        } else {
                    // Could not add the user
                    JSONObject joError = new JSONObject();
                    UUID oldUUID = bsl.getUserUUID(screenNames[i]);

                    if (oldUUID != null) {
                        joError.put("text", "User screen name "+screenNames[i]+" already exists");
                        joError.put("uuid", oldUUID);
                        joError.put("created_at", bsl.getUserCreationTime(oldUUID));
                        joError.put("profile", bsl.getUserProfile(screenNames[i]));
                    } else
                        joError.put("text", "Unable to add the user");

                    jaErrors.put(joError);
		        }
		    }
		    catch (JSONException e) {
		        logger.log(Level.WARNING, e.getMessage(), e);
		        sendErrorBadRequest(response);
		        return true;
		    }
		}

		try {
		    JSONObject joContent = new JSONObject();
		    joContent.put(OperationResult.SUCCESS.name(), jaSuccess);
		    joContent.put(OperationResult.FAILURE.name(), jaErrors);

		    if (jaErrors.length() == 0)
		        response.setStatus(HttpServletResponse.SC_CREATED);
		    else
		        response.setStatus(HttpServletResponse.SC_OK);

		    sendContent(response, joContent, ct);
		}
		catch (JSONException e) {
		    // should not happen
		    logger.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (IOException e) {
		    logger.log(Level.WARNING, e.getMessage(), e);
		}

		return true;
	}
}

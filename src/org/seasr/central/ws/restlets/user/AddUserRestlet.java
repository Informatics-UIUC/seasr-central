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
import java.util.ArrayList;
import java.util.List;
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
import org.seasr.central.ws.restlets.BaseAbstractRestlet;

import com.google.gdata.util.ContentType;

/**
 * This servlet implements add user functionality.
 *
 * @author xavier
 * @author Boris Capitanu
 *
 */
public class AddUserRestlet extends BaseAbstractRestlet {

	@Override
	public String getRestContextPathRegexp() {
		return "/services/users/?";
		//|/services/users/\\.(txt|json|xml|html|sgwt)
	}

    @Override
	public boolean process(HttpServletRequest request, HttpServletResponse response, String method, String... values) {
	    // check for POST
	    if (!method.equalsIgnoreCase("POST")) return false;

	    String format;

	    if (values.length == 0) {
	        // format not specified, look at headers
	        //TODO figure out how to deal with accept headers
	        List<ContentType> allowedTypes = new ArrayList<ContentType>();
	        allowedTypes.add(ContentType.JSON);
	        allowedTypes.add(ContentType.APPLICATION_XML);
	        allowedTypes.add(ContentType.TEXT_HTML);
	        allowedTypes.add(ContentType.TEXT_PLAIN);
	        allowedTypes.add(new ContentType("application/gwt"));

	        String accept = request.getHeader("Accept");
	        ContentType ct = null;

	        if (accept != null)
	            ct = ContentType.getBestContentType(accept, allowedTypes);

            if (ct == null) {
                sendErrorNotAcceptable(response);
                return true;
            }

	        format = ct.getSubType();

	        System.out.println("      accept: " + accept);
	        System.out.println("content_type: " + ct.toString());
	        System.out.println("      format: " + format);

	    } else
	        format = values[0];

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
		JSONArray ja = new JSONArray();

		for (int i = 0, iMax = screenNames.length; i < iMax; i++) {
		    try {
		        JSONObject jo = new JSONObject();
		        JSONObject joProfile = new JSONObject(profiles[i]);

		        // attempt to add the user
		        uuid = bsl.addUser(screenNames[i], passwords[i], joProfile);

		        if (uuid == null) {
		            // Could not add the user
		            JSONObject error = new JSONObject();
		            UUID oldUUID = bsl.getUserUUID(screenNames[i]);

		            if (oldUUID != null) {
		                error.put("text", "User screen name "+screenNames[i]+" already exists");
		                error.put("uuid", oldUUID);
		                error.put("created_at", bsl.getUserCreationTime(oldUUID));
		                error.put("profile", bsl.getUserProfile(screenNames[i]));
		            } else
		                error.put("text", "Unable to add the user");

		            jo.put("error", error);
		        } else {
		            // User added successfully
		            jo.put("uuid", uuid.toString());
		            jo.put("screen_name", screenNames[i]);
		            jo.put("created_at", bsl.getUserCreationTime(uuid));
		            jo.put("profile", joProfile);

		            if (!bsl.addEvent(SourceType.USER, uuid, Event.USER_CREATED, jo))
		                logger.warning(String.format("Could not record the %s event for user: %s (%s)",
		                        Event.USER_CREATED, screenNames[i], uuid));
		        }

		        ja.put(jo);
		    }
		    catch (JSONException e) {
		        sendErrorBadRequest(response);
		        return true;
		    }
		}

		try {
		    sendContent(response, ja, format);
		}
		catch (IOException e) {
		    logger.log(Level.WARNING, e.getMessage(), e);
		}

		return true;
	}
}

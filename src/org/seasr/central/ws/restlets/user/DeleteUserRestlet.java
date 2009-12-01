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

import static org.seasr.central.ws.restlets.Tools.exceptionToText;
import static org.seasr.central.ws.restlets.Tools.logger;
import static org.seasr.central.ws.restlets.Tools.sendContent;
import static org.seasr.central.ws.restlets.Tools.sendErrorNotFound;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.seasr.central.ws.restlets.BaseAbstractRestlet;

import com.google.gdata.util.ContentType;

/**
 * This servlet implements add user functionality.
 *
 * @author xavier
 * @author Boris Capitanu
 */
public class DeleteUserRestlet extends BaseAbstractRestlet {

    private static final Map<String, ContentType> supportedResponseTypes = new HashMap<String, ContentType>();

    static {
        supportedResponseTypes.put("json", ContentType.JSON);
        supportedResponseTypes.put("xml", ContentType.APPLICATION_XML);
        supportedResponseTypes.put("html", ContentType.TEXT_HTML);
        supportedResponseTypes.put("txt", ContentType.TEXT_PLAIN);
        supportedResponseTypes.put("sgwt", new ContentType("application/smartgwt"));
    }

    @Override
    public Map<String, ContentType> getSupportedResponseTypes() {
        return supportedResponseTypes;
    }

	@Override
	public String getRestContextPathRegexp() {
		return "/services/users/(.*)\\.(txt|json|xml|html|sgwt)";
	}

	@Override
	public boolean process(HttpServletRequest request, HttpServletResponse response, String method, String... values) {
	    // check for DELETE
	    if (!method.equalsIgnoreCase("DELETE")) return false;

	    String screenName = values[0];

	    UUID uuid = bsl.getUserUUID(screenName);
        if (uuid == null) {
            sendErrorNotFound(response);
            return true;
        }

		JSONArray ja = new JSONArray();

		try {
		    JSONObject jo = new JSONObject();

			if ( bsl.removeUser(screenName)) {
			    // User deleted successfully
				jo.put("uuid", uuid.toString());
				jo.put("screen_name", screenName);
			} else {
				// Could not add the user
				JSONObject error = new JSONObject();
				error.put("text", "User with UUID "+uuid+" could not be deleted");
				error.put("uuid", uuid.toString());
				error.put("screen_name", screenName);
				jo.put("error_msg", error);
			}

			ja.put(jo);
		}
		catch (JSONException e) {
		    logger.warning(exceptionToText(e));
		    return false;
		}

		try {
		    sendContent(response, ja, values[1]);
		}
		catch (IOException e) {
		    logger.warning(exceptionToText(e));
			return false;
		}

		return true;
	}
}

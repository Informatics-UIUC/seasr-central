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

import static org.seasr.central.ws.restlets.Tools.ContentType_SmartGWT;
import static org.seasr.central.ws.restlets.Tools.logger;
import static org.seasr.central.ws.restlets.Tools.sendContent;
import static org.seasr.central.ws.restlets.Tools.sendErrorBadRequest;
import static org.seasr.central.ws.restlets.Tools.sendErrorInternalServerError;
import static org.seasr.central.ws.restlets.Tools.sendErrorNotAcceptable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.seasr.central.ws.restlets.BaseAbstractRestlet;
import org.seasr.central.ws.restlets.Tools.OperationResult;

import com.google.gdata.util.ContentType;

/**
 * This servlet implements list user functionality.
 *
 * @author xavier
 * @author Boris Capitanu
 */
public class ListUsersRestlet extends BaseAbstractRestlet {

    private static final Map<String, ContentType> supportedResponseTypes = new HashMap<String, ContentType>();

    static {
        supportedResponseTypes.put("json", ContentType.JSON);
        supportedResponseTypes.put("xml", ContentType.APPLICATION_XML);
        supportedResponseTypes.put("html", ContentType.TEXT_HTML);
        supportedResponseTypes.put("txt", ContentType.TEXT_PLAIN);
        supportedResponseTypes.put("sgwt", ContentType_SmartGWT);
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
	    // check for GET
	    if (!method.equalsIgnoreCase("GET")) return false;

        ContentType ct = getDesiredResponseContentType(request);
        if (ct == null) {
            sendErrorNotAcceptable(response);
            return true;
        }

		long offset = 0;
		long count  = Long.MAX_VALUE;

		String sOffset = request.getParameter("offset");
		String sCount = request.getParameter("count");

		try {
		    if (sOffset != null ) offset = Long.parseLong(sOffset);
		    if (sCount != null ) count = Long.parseLong(sCount);
		}
		catch (NumberFormatException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            sendErrorBadRequest(response);
            return true;
		}

		JSONArray ja = bsl.listUsers(offset, count);
		if (ja == null) {
		    logger.log(Level.SEVERE, "listUsers() returned null - possible SQLException");
		    sendErrorInternalServerError(response);
		    return true;
		}

		try {
		    JSONObject joContent = new JSONObject();
		    joContent.put(OperationResult.SUCCESS.name(), ja);
		    joContent.put(OperationResult.FAILURE.name(), new JSONArray());

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

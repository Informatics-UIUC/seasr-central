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

package org.seasr.central.ws.restlets.component;

import com.google.gdata.util.ContentType;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.seasr.central.storage.exceptions.ComponentNotFoundException;
import org.seasr.central.storage.exceptions.GroupNotFoundException;
import org.seasr.central.storage.exceptions.UserNotFoundException;
import org.seasr.central.util.SCSecurity;
import org.seasr.central.ws.restlets.ContentTypes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;

import static org.seasr.central.util.Tools.*;

/**
 * Restlet for retrieving component descriptors of components shared with a group
 *
 * @author Boris Capitanu
 */
public class RetrieveGroupComponentsRestlet extends ListGroupComponentsRestlet {

    private static final Map<String, ContentType> supportedResponseTypes = new HashMap<String, ContentType>();

    static {
        supportedResponseTypes.put("rdf", ContentTypes.RDFXML);
        supportedResponseTypes.put("ttl", ContentTypes.RDFTTL);
        supportedResponseTypes.put("nt", ContentTypes.RDFNT);
    }

    @Override
    public Map<String, ContentType> getSupportedResponseTypes() {
        return supportedResponseTypes;
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

        UUID remoteUserId;
        String remoteUser = request.getRemoteUser();

        //TODO: for test purposes
        if (request.getParameterMap().containsKey("remoteUser") && request.getParameter("remoteUser").trim().length() > 0)
            remoteUser = request.getParameter("remoteUser");

        try {
            Properties groupProps = getGroupNameAndId(values[0]);
            UUID groupId = UUID.fromString(groupProps.getProperty("uuid"));
            String groupName = groupProps.getProperty("name");

            try {
                remoteUserId = bsl.getUserId(remoteUser);

                // Check permissions
                if (!SCSecurity.canAccessGroupComponents(groupId, remoteUserId, bsl, request)) {
                    sendErrorUnauthorized(response);
                    return true;
                }
            }
            catch (UserNotFoundException e) {
                logger.log(Level.WARNING, String.format("Cannot obtain user id for authenticated user '%s'!", remoteUser));
                sendErrorUnauthorized(response);
                return true;
            }

            boolean includeOldVersions = false;
            if (request.getParameterMap().containsKey("includeOldVersions"))
                includeOldVersions = Boolean.parseBoolean(request.getParameter("includeOldVersions"));

            JSONArray jaResult = bsl.listGroupComponents(groupId, 0, Long.MAX_VALUE, includeOldVersions);

            // Create the accumulator model
            Model model = ModelFactory.createDefaultModel();

            // ...and add all the accessible components to it
            for (int i = 0, iMax = jaResult.length(); i < iMax; i++) {
                JSONObject joCompVer = jaResult.getJSONObject(i);
                UUID compId = UUID.fromString(joCompVer.getString("uuid"));
                int compVersion = joCompVer.getInt("version");

                // Retrieve the component from the backend
                Model compModel = bsl.getComponent(compId, compVersion);

                rewriteComponentModel(compModel, compId, compVersion, request);
                model.add(compModel);
            }

            // Send the response
            response.setContentType(ct.toString());
            response.setStatus(HttpServletResponse.SC_OK);

            if (ct.equals(ContentTypes.RDFXML))
                model.write(response.getOutputStream(), "RDF/XML");

            else

            if (ct.equals(ContentTypes.RDFNT))
                model.write(response.getOutputStream(), "N-TRIPLE");

            else

            if (ct.equals(ContentTypes.RDFTTL))
                model.write(response.getOutputStream(), "TURTLE");
        }
        catch (GroupNotFoundException e) {
            sendErrorNotFound(response);
            return true;
        }
        catch (ComponentNotFoundException e) {
            sendErrorNotFound(response);
            return true;
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
            return true;
        }

        return true;
    }
}

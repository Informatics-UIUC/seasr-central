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

package org.seasr.central.ws.restlets.flow;

import com.google.gdata.util.ContentType;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.json.JSONArray;
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.util.IdVersionPair;
import org.seasr.central.ws.restlets.ContentTypes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.logging.Level;

import static org.seasr.central.util.Tools.*;

/**
 * Restlet for retrieving flow descriptors of accessible flows owned by a user
 *
 * @author Boris Capitanu
 */
public class RetrieveUserFlowsRestlet extends ListUserFlowsRestlet {

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

        UUID userId;
        String screenName;

        UUID remoteUserId;
        String remoteUser = request.getRemoteUser();

        //TODO: for test purposes
        if (request.getParameterMap().containsKey("remoteUser") && request.getParameter("remoteUser").trim().length() > 0)
            remoteUser = request.getParameter("remoteUser");

        try {
            Properties userProps = getUserScreenNameAndId(values[0]);
            if (userProps != null) {
                userId = UUID.fromString(userProps.getProperty("uuid"));
                screenName = userProps.getProperty("screen_name");
            } else {
                // Specified user does not exist
                sendErrorNotFound(response);
                return true;
            }

            remoteUserId = (remoteUser != null) ? bsl.getUserId(remoteUser) : null;

            boolean includeOldVersions = false;
            if (request.getParameterMap().containsKey("includeOldVersions"))
                includeOldVersions = Boolean.parseBoolean(request.getParameter("includeOldVersions"));

            // Get the list of all the versions of all flows owned by a user and
            // the groups each version is shared with
            JSONArray jaResult = bsl.listUserFlows(userId, 0, Long.MAX_VALUE);

            // Build a data structure that sorts the versions of each flow in decreasing order (highest->lowest)
            Map<UUID, SortedMap<Integer, List<UUID>>> flowMap = buildVersionSharingMap(jaResult);

            // Get the set of groups that the remote user belongs to (or null if the remote user is the same as the user queried)
            Set<UUID> remoteUserGroups = (userId.equals(remoteUserId)) ? null : getAdjustedGroupsForUser(remoteUserId);

            // Compute the list of accessible flows based on the group participation status
            List<IdVersionPair> accessibleFlows = getAccessibleCompsOrFlows(flowMap, remoteUserGroups, includeOldVersions);

            // Create the accumulator model
            Model model = ModelFactory.createDefaultModel();

            // ...and add all the accessible flows to it
            for (IdVersionPair flow : accessibleFlows) {
                Model flowModel = bsl.getFlow(flow.getId(), flow.getVersion());
                if (flowModel == null)
                    throw new BackendStoreException(
                            String.format("Could not retrieve flow %s version %d", flow.getId(), flow.getVersion()));

                rewriteFlowModel(flowModel, flow.getId(), flow.getVersion(), request);

                model.add(flowModel);
            }

            // Send the response
            response.setContentType(ct.toString());
            response.setStatus(HttpServletResponse.SC_OK);

            if (ct.equals(ContentTypes.RDFXML))
                model.write(response.getOutputStream(), "RDF/XML");

            else if (ct.equals(ContentTypes.RDFNT))
                model.write(response.getOutputStream(), "N-TRIPLE");

            else if (ct.equals(ContentTypes.RDFTTL))
                model.write(response.getOutputStream(), "TURTLE");
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
            return true;
        }

        return true;
    }
}
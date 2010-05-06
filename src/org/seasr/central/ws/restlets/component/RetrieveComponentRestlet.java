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
import org.seasr.central.storage.exceptions.ComponentNotFoundException;
import org.seasr.central.storage.exceptions.UserNotFoundException;
import org.seasr.central.util.SCSecurity;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ContentTypes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import static org.seasr.central.util.Tools.*;

/**
 * Restlet for retrieving component descriptors
 *
 * @author Boris Capitanu
 */
public class RetrieveComponentRestlet extends AbstractBaseRestlet {

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
    public String getRestContextPathRegexp() {
        return "/services/components/([a-f\\d]{8}(?:-[a-f\\d]{4}){3}-[a-f\\d]{12})/versions/(\\d+)" +
                "(?:/|" + regexExtensionMatcher() + ")?$";
    }

    @Override
    public boolean process(HttpServletRequest request, HttpServletResponse response, String method, String... values) {
        // Check for GET
        if (!method.equalsIgnoreCase("GET")) return false;

        ContentType ct = getDesiredResponseContentType(request);
        if (ct == null) {
            sendErrorNotAcceptable(response);
            return true;
        }

        UUID componentId;
        int version;

        try {
            componentId = UUID.fromString(values[0]);
            version = Integer.parseInt(values[1]);
            if (version < 1)
                throw new IllegalArgumentException("The version number cannot be less than 1");
        }
        catch (IllegalArgumentException e) {
            sendErrorBadRequest(response);
            return true;
        }

        UUID remoteUserId;
        String remoteUser = request.getRemoteUser();

        //TODO: for test purposes
        if (request.getParameterMap().containsKey("remoteUser") && request.getParameter("remoteUser").trim().length() > 0)
            remoteUser = request.getParameter("remoteUser");

        try {
            try {
                remoteUserId = bsl.getUserId(remoteUser);

                // Check permissions
                if (!SCSecurity.canAccessComponent(componentId, version, remoteUserId, bsl, request)) {
                    sendErrorUnauthorized(response);
                    return true;
                }
            }
            catch (UserNotFoundException e) {
                logger.log(Level.WARNING, String.format("Cannot obtain user id for authenticated user '%s'!", remoteUser));
                sendErrorUnauthorized(response);
                return true;
            }

            // Attempt to retrieve the component from the backend store
            Model compModel = bsl.getComponent(componentId, version);

            rewriteComponentModel(compModel, componentId, version, request);

            // Send the response
            response.setContentType(ct.toString());
            response.setStatus(HttpServletResponse.SC_OK);

            if (ct.equals(ContentTypes.RDFXML))
                compModel.write(response.getOutputStream(), "RDF/XML");

            else

            if (ct.equals(ContentTypes.RDFNT))
                compModel.write(response.getOutputStream(), "N-TRIPLE");

            else

            if (ct.equals(ContentTypes.RDFTTL))
                compModel.write(response.getOutputStream(), "TURTLE");
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

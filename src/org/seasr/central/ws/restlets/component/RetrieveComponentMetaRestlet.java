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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.meandre.core.repository.*;
import org.seasr.central.storage.SCError;
import org.seasr.central.storage.exceptions.BackendStoreException;
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

import static org.seasr.central.util.Tools.sendErrorInternalServerError;
import static org.seasr.central.util.Tools.sendErrorNotAcceptable;

/**
 * Restlet for retrieving component information metadata
 *
 * @author Boris Capitanu
 */
public class RetrieveComponentMetaRestlet extends AbstractBaseRestlet {

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

        JSONArray jaSuccess = new JSONArray();
        JSONArray jaErrors = new JSONArray();

        UUID remoteUserId;
        String remoteUser = request.getRemoteUser();

        //TODO: for test purposes
        if (request.getParameterMap().containsKey("remoteUser") && request.getParameter("remoteUser").trim().length() > 0)
            remoteUser = request.getParameter("remoteUser");

        try {
            UUID componentId;
            int version;

            try {
                componentId = UUID.fromString(values[0]);
                version = Integer.parseInt(values[1]);
                if (version < 1)
                    throw new IllegalArgumentException("The version number cannot be less than 1");
            }
            catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, null, e);
                jaErrors.put(SCError.createErrorObj(SCError.INVALID_PARAM_VALUE, e, bsl));
                sendResponse(jaSuccess, jaErrors, ct, response);
                return true;
            }

            try {
                try {
                    remoteUserId = bsl.getUserId(remoteUser);

                    // Check permissions
                    if (!SCSecurity.canAccessComponent(componentId, version, remoteUserId, bsl, request)) {
                        JSONObject joError = SCError.createErrorObj(SCError.UNAUTHORIZED, bsl);
                        joError.put("uuid", componentId.toString());
                        joError.put("version", version);
                        jaErrors.put(joError);
                        sendResponse(jaSuccess, jaErrors, ct, response);
                        return true;
                    }
                }
                catch (UserNotFoundException e) {
                    logger.log(Level.WARNING, String.format("Cannot obtain user id for authenticated user '%s'!", remoteUser));
                    jaErrors.put(SCError.createErrorObj(SCError.UNAUTHORIZED, e, bsl));
                    sendResponse(jaSuccess, jaErrors, ct, response);
                    return true;
                }

                // Attempt to retrieve the component from the backend store
                Model compModel = bsl.getComponent(componentId, version);

                QueryableRepository qr = new RepositoryImpl(compModel);
                ExecutableComponentDescription ecd = qr.getAvailableExecutableComponentDescriptions().iterator().next();

                JSONObject joComponentMeta = new JSONObject();
                joComponentMeta.put("uuid", componentId.toString());
                joComponentMeta.put("version", version);
                joComponentMeta.put("name", ecd.getName());
                joComponentMeta.put("creator", ecd.getCreator());
                joComponentMeta.put("creationDate", ecd.getCreationDate().getTime());
                joComponentMeta.put("description", ecd.getDescription());
                joComponentMeta.put("rights", ecd.getRights());
                joComponentMeta.put("firingPolicy", ecd.getFiringPolicy());
                joComponentMeta.put("runnable", ecd.getRunnable());
                joComponentMeta.put("uri", ecd.getExecutableComponent().toString());
                joComponentMeta.put("format", ecd.getFormat());

                JSONArray jaTags = new JSONArray();
                for (String tag : ecd.getTags().getTags())
                    jaTags.put(tag);
                joComponentMeta.put("tags", jaTags);

                JSONArray jaInputs = new JSONArray();
                for (DataPortDescription port : ecd.getInputs()) {
                    JSONObject joInput = new JSONObject();
                    joInput.put("name", port.getName());
                    joInput.put("description", port.getDescription());

                    jaInputs.put(joInput);
                }
                joComponentMeta.put("inputs", jaInputs);

                JSONArray jaOutputs = new JSONArray();
                for (DataPortDescription port : ecd.getOutputs()) {
                    JSONObject joOutput = new JSONObject();
                    joOutput.put("name", port.getName());
                    joOutput.put("description", port.getDescription());

                    jaOutputs.put(joOutput);
                }
                joComponentMeta.put("outputs", jaOutputs);

                JSONArray jaProperties = new JSONArray();
                PropertiesDescriptionDefinition propDescriptionDef = ecd.getProperties();
                for (String prop : propDescriptionDef.getKeys()) {
                    JSONObject joProp = new JSONObject();
                    joProp.put("key", prop);
                    joProp.put("value", propDescriptionDef.getValue(prop));
                    joProp.put("description", propDescriptionDef.getDescription(prop));

                    jaProperties.put(joProp);
                }
                joComponentMeta.put("properties", jaProperties);

                jaSuccess.put(joComponentMeta);
            }
            catch (ComponentNotFoundException e) {
                JSONObject joError = SCError.createErrorObj(SCError.COMPONENT_NOT_FOUND, bsl,
                        e.getComponentId().toString(), Integer.toString(e.getVersion()));
                joError.put("uuid", e.getComponentId().toString());
                joError.put("version", e.getVersion());
                jaErrors.put(joError);
                sendResponse(jaSuccess, jaErrors, ct, response);
            }
            catch (BackendStoreException e) {
                logger.log(Level.SEVERE, null, e);

                JSONObject joError = SCError.createErrorObj(SCError.BACKEND_ERROR, e, bsl);
                joError.put("uuid", componentId.toString());
                joError.put("version", version);
                jaErrors.put(joError);
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

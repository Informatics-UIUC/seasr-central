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
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.meandre.core.repository.FlowDescription;
import org.meandre.core.repository.QueryableRepository;
import org.meandre.core.repository.RepositoryImpl;
import org.meandre.core.utils.vocabulary.RepositoryVocabulary;
import org.seasr.central.storage.SCError;
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.storage.exceptions.UnknownComponentsException;
import org.seasr.central.storage.exceptions.UserNotFoundException;
import org.seasr.central.util.SCSecurity;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ContentTypes;
import org.seasr.meandre.support.generic.io.ModelUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.logging.Level;

import static org.seasr.central.util.Tools.*;

/**
 * Restlet for uploading flows
 *
 * @author Boris Capitanu
 */
public class UploadFlowRestlet extends AbstractBaseRestlet {

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
        return "/services/users/([^/\\s]+)/flows/?(?:" + regexExtensionMatcher() + ")?$";
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean process(HttpServletRequest request, HttpServletResponse response, String method, String... values) {
        // check for POST
        if (!method.equalsIgnoreCase("POST")) return false;

        ContentType ct = getDesiredResponseContentType(request);
        if (ct == null) {
            sendErrorNotAcceptable(response);
            return true;
        }

        JSONArray jaSuccess = new JSONArray();
        JSONArray jaErrors = new JSONArray();

        UUID remoteUserId = null;
        String remoteUser = request.getRemoteUser();

        //TODO: for test purposes
        if (request.getParameterMap().containsKey("remoteUser") && request.getParameter("remoteUser").trim().length() > 0)
            remoteUser = request.getParameter("remoteUser");

        if (!ServletFileUpload.isMultipartContent(request)) {
            jaErrors.put(SCError.createErrorObj(SCError.UPLOAD_ERROR, bsl));
            sendResponse(jaSuccess, jaErrors, ct, response);
            return true;
        }

        try {
            Properties userProps = getUserScreenNameAndId(values[0]);
            UUID userId = UUID.fromString(userProps.getProperty("uuid"));
            String screenName = userProps.getProperty("screen_name");

            remoteUserId = bsl.getUserId(remoteUser);

            // Check permissions
            if (!SCSecurity.canUploadFlow(userId, remoteUserId, bsl, request)) {
                jaErrors.put(SCError.createErrorObj(SCError.UNAUTHORIZED, bsl));
                sendResponse(jaSuccess, jaErrors, ct, response);
                return true;
            }

            ServletFileUpload fileUpload = new ServletFileUpload(new DiskFileItemFactory());
            List<FileItem> files;
            try {
                files = fileUpload.parseRequest(request);
            }
            catch (FileUploadException e) {
                jaErrors.put(SCError.createErrorObj(SCError.UPLOAD_ERROR, e, bsl));
                sendResponse(jaSuccess, jaErrors, ct, response);
                return true;
            }

            // Accumulator for the flow models
            Model model = ModelFactory.createDefaultModel();

            for (FileItem file : files) {
                // Check for proper request parameters
                if (file == null || !file.getFieldName().equals("flow_rdf"))
                    continue;

                // Make sure we have non-empty fields
                if ((!file.isFormField() && file.getName().trim().length() == 0) ||
                        (file.isFormField() && file.getString().trim().length() == 0))
                    continue;

                if (!file.isFormField()) {
                    logger.fine(String.format("Uploaded file '%s' (%,d bytes) [%s]",
                            file.getName(), file.getSize(), file.getFieldName()));
                    if (file.getSize() == 0)
                        logger.warning(String.format("Uploaded file '%s' has size 0", file.getName()));
                }

                if (file.getFieldName().equalsIgnoreCase("flow_rdf")) {
                    try {
                        // Read the flow model and check that it contains a single flow
                        Model flowModel = file.isFormField() ?
                                // TODO: Add mechanism for request timeouts when retrieving remote descriptors
                                ModelUtils.getModel(new URI(file.getString()), null) :
                                ModelUtils.getModel(file.getInputStream(), null);

                        List<Resource> flowResList = flowModel.listSubjectsWithProperty(
                                RDF.type, RepositoryVocabulary.flow_component).toList();
                        if (flowResList.size() != 1)
                            throw new Exception("RDF descriptor does not contain a flow, " +
                                    "or contains more than one flow.");

                        // Accumulate the flow model
                        model.add(flowModel);
                    }
                    catch (Exception e) {
                        String descriptorName = file.isFormField() ? file.getString().trim() : file.getName();
                        logger.log(Level.WARNING, String.format("Error parsing RDF from '%s'", descriptorName), e);

                        JSONObject joError = (e instanceof IOException) ?
                                SCError.createErrorObj(SCError.NETWORK_ERROR, e, bsl) :
                                SCError.createErrorObj(SCError.RDF_PARSE_ERROR, e, bsl, descriptorName);
                        joError.put("descriptor", descriptorName);
                        jaErrors.put(joError);
                        continue;
                    }
                } else {
                    JSONObject joError = SCError.createErrorObj(SCError.INVALID_PARAM_VALUE, bsl);
                    joError.put("param", file.getFieldName());
                    jaErrors.put(joError);
                    continue;
                }
            }

            QueryableRepository qr = new RepositoryImpl(model);

            for (FlowDescription fd : qr.getAvailableFlowDescriptions()) {
                String origUri = fd.getFlowComponent().getURI();

                try {
                    // Attempt to add the flow to the backend storage
                    JSONObject joResult = bsl.addFlow(userId, fd);

                    String flowId = joResult.getString("uuid");
                    int flowVersion = joResult.getInt("version");

                    String flowUrl = getFlowBaseAccessUrl(request, flowId, flowVersion) + ".ttl";

                    JSONObject joFlow = new JSONObject();
                    joFlow.put("orig_uri", origUri);
                    joFlow.put("uuid", flowId);
                    joFlow.put("version", flowVersion);
                    joFlow.put("url", flowUrl);

                    jaSuccess.put(joFlow);
                }
                catch (BackendStoreException e) {
                    JSONObject joError;

                    if (e.getCause() != null && e.getCause() instanceof UnknownComponentsException) {
                        UnknownComponentsException ex = (UnknownComponentsException) e.getCause();
                        joError = SCError.createErrorObj(SCError.UNKNOWN_COMP_IN_FLOW, bsl);
                        joError.put("unknown_components", new JSONArray(ex.getUnknownComponents()));
                    } else {
                        logger.log(Level.SEVERE, null, e);
                        joError = SCError.createErrorObj(SCError.BACKEND_ERROR, e, bsl);
                    }

                    joError.put("name", fd.getName());
                    joError.put("orig_uri", origUri);
                    jaErrors.put(joError);
                    continue;
                }
            }
        }
        catch (UserNotFoundException e) {
            if ((remoteUser != null && remoteUser.equals(e.getUserName())) ||
                    (remoteUserId != null && remoteUserId.equals(e.getUserId()))) {
                logger.log(Level.WARNING, String.format("Cannot obtain user id for authenticated user '%s'!", remoteUser));
                jaErrors.put(SCError.createErrorObj(SCError.UNAUTHORIZED, e, bsl));
            } else
                jaErrors.put(SCError.createErrorObj(SCError.USER_NOT_FOUND, bsl, values[0]));
            sendResponse(jaSuccess, jaErrors, ct, response);
            return true;
        }
        catch (BackendStoreException e) {
            logger.log(Level.SEVERE, null, e);

            jaErrors.put(SCError.createErrorObj(SCError.BACKEND_ERROR, e, bsl));
            sendResponse(jaSuccess, jaErrors, ct, response);
            return true;
        }
        catch (JSONException e) {
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
            return true;
        }

        // Send the response
        sendResponse(jaSuccess, jaErrors, ct, response);

        return true;
    }
}

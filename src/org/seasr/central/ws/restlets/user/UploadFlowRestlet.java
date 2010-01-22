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
import org.seasr.central.main.SC;
import org.seasr.central.storage.Event;
import org.seasr.central.storage.SourceType;
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ContentTypes;
import org.seasr.meandre.support.generic.io.ModelUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        return "/services/users/(.+)/flows/?(?:" + regexExtensionMatcher() + ")?$";
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

        if (!ServletFileUpload.isMultipartContent(request)) {
            sendErrorBadRequest(response);
            return true;
        }

        UUID uuid;
        @SuppressWarnings("unused")
        String screenName = null;

        try {
            Properties userProps = getUserScreenNameAndId(values[0]);
            if (userProps != null) {
                uuid = UUID.fromString(userProps.getProperty("uuid"));
                screenName = userProps.getProperty("screen_name");
            } else {
                sendErrorNotFound(response);
                return true;
            }
        }
        catch (BackendStoreException e) {
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
            return true;
        }

        ServletFileUpload fileUpload = new ServletFileUpload(new DiskFileItemFactory());
        List<FileItem> files;
        try {
            files = fileUpload.parseRequest(request);
        }
        catch (FileUploadException e) {
            sendErrorBadRequest(response);
            return true;
        }

        JSONArray jaSuccess = new JSONArray();
        JSONArray jaErrors = new JSONArray();

        try {
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
                    logger.fine(String.format("Uploaded file '%s' (%,d bytes) [%s]", file.getName(), file.getSize(), file.getFieldName()));
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
                            throw new Exception("RDF descriptor does not contain a flow, or contains more than one flow.");

                        // Accumulate the flow model
                        model.add(flowModel);
                    }
                    catch (Exception e) {
                        String descriptorName = file.isFormField() ? file.getString().trim() : file.getName();
                        logger.log(Level.WARNING, String.format("Error parsing RDF from '%s'", descriptorName), e);

                        JSONObject joError = createJSONErrorObj("Invalid flow RDF descriptor received", e);
                        joError.put("descriptor", descriptorName);

                        jaErrors.put(joError);
                    }
                } else {
                    sendErrorBadRequest(response);
                    return true;
                }
            }

            QueryableRepository qr = new RepositoryImpl(model);

            for (FlowDescription fd : qr.getAvailableFlowDescriptions()) {
                String origUri = fd.getFlowComponent().getURI();

                try {
                    // Attempt to add the flow to the backend storage
                    JSONObject joResult = bsl.addFlow(uuid, fd);

                    String flowId = joResult.getString("uuid");
                    int flowVersion = joResult.getInt("version");

                    String flowUrl = String.format("%s://%s:%d/repository/flow/%s/%d.ttl",
                            request.getScheme(), request.getServerName(), request.getServerPort(), flowId, flowVersion);

                    JSONObject joFlow = new JSONObject();
                    joFlow.put("orig_uri", origUri);
                    joFlow.put("uuid", flowId);
                    joFlow.put("version", flowVersion);
                    joFlow.put("url", flowUrl);

                    jaSuccess.put(joFlow);

                    // Record the event
                    Event event = (flowVersion == 1) ? Event.FLOW_UPLOADED : Event.FLOW_UPDATED;
                    bsl.addEvent(SourceType.USER, uuid, event, joFlow);
                }
                catch (BackendStoreException e) {
                    logger.log(Level.SEVERE, null, e);

                    jaErrors.put(createJSONErrorObj(String.format("Failed to add flow '%s' (%s)",
                            fd.getName(), origUri), e));
                }
            }

            JSONObject joContent = new JSONObject();
            joContent.put(OperationResult.SUCCESS.name(), jaSuccess);
            joContent.put(OperationResult.FAILURE.name(), jaErrors);

            if (jaErrors.length() == 0)
                response.setStatus(HttpServletResponse.SC_CREATED);
            else
                response.setStatus(HttpServletResponse.SC_OK);

            try {
                sendContent(response, joContent, ct);
            }
            catch (IOException e) {
                logger.log(Level.WARNING, null, e);
            }
        }
        catch (JSONException e) {
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
            return true;
        }

        return true;
    }
//
//    protected void updateComponents(FlowDescription flow) {
//        // Check whether the backend contains the components in this flow
//        Set<String> components = new HashSet<String>();
//        for (ExecutableComponentInstanceDescription ecid : flow.getExecutableComponentInstances())
//            components.add(ecid.getExecutableComponent().getURI());
//
//
//    }

}

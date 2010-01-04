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

import static org.seasr.central.ws.restlets.Tools.createJSONErrorObj;
import static org.seasr.central.ws.restlets.Tools.logger;
import static org.seasr.central.ws.restlets.Tools.sendContent;
import static org.seasr.central.ws.restlets.Tools.sendErrorBadRequest;
import static org.seasr.central.ws.restlets.Tools.sendErrorInternalServerError;
import static org.seasr.central.ws.restlets.Tools.sendErrorNotAcceptable;
import static org.seasr.central.ws.restlets.Tools.sendErrorNotFound;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import org.seasr.central.storage.BackendStorageException;
import org.seasr.central.storage.Event;
import org.seasr.central.storage.SourceType;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ContentTypes;
import org.seasr.central.ws.restlets.Tools.OperationResult;
import org.seasr.meandre.support.generic.io.ModelUtils;

import com.google.gdata.util.ContentType;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;

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

        UUID uuid = null;
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
        catch (BackendStorageException e) {
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
            return true;
        }

        ServletFileUpload fileUpload = new ServletFileUpload(new DiskFileItemFactory());
        List<FileItem> files = null;
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
                if (file == null || file.isFormField() || file.getName().length() == 0)
                    continue;

                logger.fine(String.format("Uploaded file '%s' (%,d bytes) [%s]", file.getName(), file.getSize(), file.getFieldName()));

                if (file.getSize() == 0)
                    logger.warning(String.format("Uploaded file '%s' has size 0", file.getName()));

                if (file.getFieldName().equalsIgnoreCase("flow_rdf")) {
                    try {
                        // Read the flow model and check that it contains a single flow
                        Model flowModel = ModelUtils.getModel(file.getInputStream(), null);
                        List<Resource> flowResList = flowModel.listSubjectsWithProperty(
                                RDF.type, RepositoryVocabulary.flow_component).toList();
                        if (flowResList.size() != 1)
                            throw new Exception("RDF descriptor does not contain a flow, or contains more than one flow.");

                        // Accumulate the flow model
                        model.add(flowModel);
                    }
                    catch (Exception e) {
                        logger.log(Level.WARNING,
                                String.format("Error parsing RDF file '%s'", file.getName()), e);

                        JSONObject joError = createJSONErrorObj("Invalid flow RDF descriptor received", e);
                        joError.put("file", file.getName());

                        jaErrors.put(joError);
                    }
                } else {
                    sendErrorBadRequest(response);
                    return true;
                }
            }

            QueryableRepository qr = new RepositoryImpl(model);

            for (FlowDescription fd : qr.getAvailableFlowDescriptions()) {
                try {
                    // Attempt to add the flow to the backend storage
                    JSONObject joResult = bsl.addFlow(uuid, fd);

                    String flowId = joResult.getString("uuid");
                    int flowVersion = joResult.getInt("version");

                    String flowUrl = String.format("%s://%s:%d/repository/flow/%s/%d.ttl",
                            request.getScheme(), request.getServerName(), request.getServerPort(), flowId, flowVersion);

                    JSONObject joFlow = new JSONObject();
                    joFlow.put("uuid", flowId);
                    joFlow.put("version", flowVersion);
                    joFlow.put("url", flowUrl);

                    jaSuccess.put(joFlow);

                    // Record the event
                    Event event = (flowVersion == 1) ? Event.FLOW_UPLOADED : Event.FLOW_UPDATED;
                    bsl.addEvent(SourceType.USER, uuid, event, joFlow);
                }
                catch (BackendStorageException e) {
                    logger.log(Level.SEVERE, null, e);

                    jaErrors.put(createJSONErrorObj(String.format("Failed to add flow '%s' (%s)",
                            fd.getName(), fd.getFlowComponent().getURI()), e));
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

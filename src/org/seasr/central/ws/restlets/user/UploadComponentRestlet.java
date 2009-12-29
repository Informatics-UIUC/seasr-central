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

import static org.seasr.central.ws.restlets.Tools.logger;
import static org.seasr.central.ws.restlets.Tools.sendContent;
import static org.seasr.central.ws.restlets.Tools.sendErrorBadRequest;
import static org.seasr.central.ws.restlets.Tools.sendErrorInternalServerError;
import static org.seasr.central.ws.restlets.Tools.sendErrorNotAcceptable;
import static org.seasr.central.ws.restlets.Tools.sendErrorNotFound;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.QueryableRepository;
import org.meandre.core.repository.RepositoryImpl;
import org.meandre.core.utils.vocabulary.RepositoryVocabulary;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ContentTypes;
import org.seasr.central.ws.restlets.Tools.OperationResult;
import org.seasr.meandre.support.generic.io.ModelUtils;

import com.google.gdata.util.ContentType;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;


public class UploadComponentRestlet extends AbstractBaseRestlet {

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
        return "/services/users/(.+)/components/?(?:" + regexExtensionMatcher() + ")?$";
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

        Properties userProps = getUserScreenNameAndUUID(values[0]);
        if (userProps != null)
            uuid = UUID.fromString(userProps.getProperty("uuid"));
        else {
            sendErrorNotFound(response);
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

        // Mapping between component uri and set of contexts
        Map<String, Set<FileItem>> componentsMap = new HashMap<String, Set<FileItem>>();

        // Accumulator for the component models
        Model model = ModelFactory.createDefaultModel();

        String currentComponentResUri = null;
        boolean skipProcessingContexts = false;

        for (FileItem file : files) {
            if (file == null || file.isFormField() || file.getName().length() == 0)
                continue;

            logger.fine(String.format("Uploaded file '%s' (%,d bytes) [%s]", file.getName(), file.getSize(), file.getFieldName()));

            if (file.getSize() == 0)
                logger.warning(String.format("Uploaded file '%s' has size 0", file.getName()));

            if (file.getFieldName().equalsIgnoreCase("component_rdf")) {
                try {
                    skipProcessingContexts = false;

                    // Read the component model and check that it contains a single executable component
                    Model compModel = ModelUtils.getModel(file.getInputStream(), null);
                    List<Resource> compResList = compModel.listSubjectsWithProperty(
                            RDF.type, RepositoryVocabulary.executable_component).toList();
                    if (compResList.size() != 1) {
                        skipProcessingContexts = true;
                        throw new Exception("RDF descriptor does not contain an executable component, or contains more than one component.");
                    }

                    // Accumulate and create a new entry in the component context hashmap
                    model.add(compModel);
                    currentComponentResUri = compResList.get(0).getURI();
                    componentsMap.put(currentComponentResUri, new HashSet<FileItem>());
                }
                catch (Exception e) {
                    logger.log(Level.WARNING,
                            String.format("Error parsing RDF file '%s'", file.getName()), e);

                    JSONObject joError = new JSONObject();
                    try {
                        joError.put("text", "Invalid component RDF descriptor received");
                        joError.put("reason", e.getMessage());
                        joError.put("file", file.getName());
                    }
                    catch (JSONException e1) {
                        logger.log(Level.SEVERE, e1.getMessage(), e1);
                        sendErrorInternalServerError(response);
                        return true;
                    }
                    jaErrors.put(joError);
                }
            }

            else

            if (file.getFieldName().equalsIgnoreCase("context")) {
                if (skipProcessingContexts) continue;

                // Sanity check
                if (currentComponentResUri == null) {
                    sendErrorBadRequest(response);
                    return true;
                }

                // Add the context file to the current component being uploaded
                componentsMap.get(currentComponentResUri).add(file);
            }
        }

        QueryableRepository qr = new RepositoryImpl(model);
        for (ExecutableComponentDescription ecd : qr.getAvailableExecutableComponentDescriptions()) {
            JSONObject joResult = bsl.addComponent(uuid, ecd, componentsMap.get(ecd.getExecutableComponent().getURI()));
            try {
                if (joResult.has("error")) {
                    JSONObject joError = new JSONObject();
                    joError.put("text", String.format("Failed to add component %s (%s)", ecd.getName(), ecd.getExecutableComponent().getURI()));
                    joError.put("reason", joResult.getString("error"));
                    jaErrors.put(joError);
                } else {
                    String compUUID = joResult.getString("uuid");
                    int compVersion = joResult.getInt("version");

                    String compUrl = String.format("%s://%s:%d/repository/component/%s/%d.ttl",
                            request.getScheme(), request.getServerName(), request.getServerPort(), compUUID, compVersion);

                    JSONObject joComponent = new JSONObject();
                    joComponent.put("uuid", compUUID);
                    joComponent.put("version", compVersion);
                    joComponent.put("url", compUrl);
                    jaSuccess.put(joComponent);
                }
            }
            catch (JSONException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                sendErrorInternalServerError(response);
                return true;
            }
        }

        try {
            JSONObject joContent = new JSONObject();
            joContent.put(OperationResult.SUCCESS.name(), jaSuccess);
            joContent.put(OperationResult.FAILURE.name(), jaErrors);

            if (jaErrors.length() == 0)
                response.setStatus(HttpServletResponse.SC_CREATED);
            else
                response.setStatus(HttpServletResponse.SC_OK);

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

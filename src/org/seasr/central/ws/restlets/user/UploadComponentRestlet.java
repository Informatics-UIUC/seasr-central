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
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.QueryableRepository;
import org.meandre.core.repository.RepositoryImpl;
import org.meandre.core.utils.vocabulary.RepositoryVocabulary;
import org.seasr.central.storage.Event;
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ContentTypes;
import org.seasr.meandre.support.generic.io.ModelUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

import static org.seasr.central.util.Tools.*;

/**
 * Restlet for uploading components
 *
 * @author Boris Capitanu
 */
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

        UUID userId;
        @SuppressWarnings("unused")
        String screenName = null;

        try {
            Properties userProps = getUserScreenNameAndId(values[0]);
            if (userProps != null) {
                userId = UUID.fromString(userProps.getProperty("uuid"));
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
        List<FileItem> uploadedFiles;
        try {
            uploadedFiles = fileUpload.parseRequest(request);
        }
        catch (FileUploadException e) {
            sendErrorBadRequest(response);
            return true;
        }

        JSONArray jaSuccess = new JSONArray();
        JSONArray jaErrors = new JSONArray();

        try {
            // Mapping between component uri and set of contexts, and temp context folders
            Map<String, Set<URL>> componentsMap = new HashMap<String, Set<URL>>();
            Map<String, File> contextTempFolderMap = new HashMap<String, File>();

            // Accumulator for the component models
            Model model = ModelFactory.createDefaultModel();

            String currentComponentResUri = null;
            boolean skipProcessingContexts = false;

            for (FileItem file : uploadedFiles) {
                // Check for proper request parameters
                if (file == null || !file.getFieldName().equals("context")
                        && !file.getFieldName().equals("component_rdf"))
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

                if (file.getFieldName().equalsIgnoreCase("component_rdf")) {
                    try {
                        skipProcessingContexts = false;

                        // Read the component model and check that it contains a single executable component
                        Model compModel = file.isFormField() ?
                                // TODO: Add mechanism for request timeouts when retrieving remote descriptors
                                ModelUtils.getModel(new URI(file.getString()), null) :
                                ModelUtils.getModel(file.getInputStream(), null);
                        List<Resource> compResList = compModel.listSubjectsWithProperty(
                                RDF.type, RepositoryVocabulary.executable_component).toList();
                        if (compResList.size() != 1) {
                            skipProcessingContexts = true;
                            throw new Exception("RDF descriptor does not contain an executable component, " +
                                    "or contains more than one component.");
                        }

                        // Accumulate and create a new entry in the component context hashmap
                        model.add(compModel);
                        currentComponentResUri = compResList.get(0).getURI();
                        componentsMap.put(currentComponentResUri, new HashSet<URL>());

                        // Create a temp folder to store any context files for this component
                        File tempFolder = createTempFolder(String.format("%s_%d_",
                                request.getRemoteAddr(), request.getRemotePort()));
                        if (tempFolder == null) {
                            logger.severe("Cannot create a temp folder to store the context files in this request!");
                            sendErrorInternalServerError(response);
                            return true;
                        }
                        contextTempFolderMap.put(currentComponentResUri, tempFolder);
                    }
                    catch (Exception e) {
                        String descriptorName = file.isFormField() ? file.getString().trim() : file.getName();
                        logger.log(Level.WARNING, String.format("Error parsing RDF from '%s'", descriptorName), e);

                        JSONObject joError = createJSONErrorObj("Invalid component RDF descriptor received", e);
                        joError.put("descriptor", descriptorName);

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

                    // If we're uploading a context file as a form field (non-file)
                    // then assume it's specifying a full URL, otherwise the field
                    // is assumed to be the context file uploaded
                    if (file.isFormField()) {
                        try {
                            componentsMap.get(currentComponentResUri).add(new URL(file.getString()));
                        }
                        catch (MalformedURLException e) {
                            sendErrorBadRequest(response);
                            return true;
                        }
                    } else {
                        // Add the context file to the current component being uploaded
                        File contextFile = new File(contextTempFolderMap.get(currentComponentResUri), file.getName());
                        try {
                            file.write(contextFile);
                        }
                        catch (Exception e) {
                            logger.log(Level.SEVERE, "Cannot save uploaded context file: " + contextFile, e);
                            sendErrorInternalServerError(response);
                            return true;
                        }

                        try {
                            componentsMap.get(currentComponentResUri).add(contextFile.toURI().toURL());
                        }
                        catch (MalformedURLException e) {
                            // Should never happen
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            QueryableRepository qr = new RepositoryImpl(model);

            for (ExecutableComponentDescription ecd : qr.getAvailableExecutableComponentDescriptions()) {
                String origUri = ecd.getExecutableComponent().getURI();

                try {
                    // Attempt to add the component to the backend storage
                    Set<URL> contexts = componentsMap.get(origUri);
                    JSONObject joResult = bsl.addComponent(userId, ecd, contexts);

                    String compId = joResult.getString("uuid");
                    int compVersion = joResult.getInt("version");

                    String compUrl = String.format("%s://%s:%d/repository/component/%s/%d.ttl",
                            request.getScheme(), request.getServerName(), request.getServerPort(), compId, compVersion);

                    JSONObject joComponent = new JSONObject();
                    joComponent.put("orig_uri", origUri);
                    joComponent.put("uuid", compId);
                    joComponent.put("version", compVersion);
                    joComponent.put("url", compUrl);

                    jaSuccess.put(joComponent);

                    // Record the event
                    Event event = (compVersion == 1) ? Event.COMPONENT_UPLOADED : Event.COMPONENT_UPDATED;
                    bsl.addEvent(event, userId, null, null, null, joComponent);
                }
                catch (BackendStoreException e) {
                    logger.log(Level.SEVERE, null, e);

                    jaErrors.put(createJSONErrorObj(String.format("Failed to add component '%s' (%s)",
                            ecd.getName(), origUri), e));
                }
            }

            // Clean up the temp folders
            for (File tempFolder : contextTempFolderMap.values())
                try {
                    FileUtils.deleteDirectory(tempFolder);
                }
                catch (IOException e) {
                    logger.log(Level.WARNING, "Cannot delete temp context folder: " + tempFolder, e);
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
}
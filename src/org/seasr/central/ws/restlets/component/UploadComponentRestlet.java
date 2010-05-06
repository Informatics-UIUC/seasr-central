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
import org.seasr.central.storage.SCError;
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.storage.exceptions.UserNotFoundException;
import org.seasr.central.util.SCSecurity;
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
import java.net.URLConnection;
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
        return "/services/users/([^/\\s]+)/components/?(?:" + regexExtensionMatcher() + ")?$";
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

        UUID remoteUserId;
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

            try {
                remoteUserId = bsl.getUserId(remoteUser);
            }
            catch (UserNotFoundException e) {
                logger.log(Level.WARNING, String.format("Cannot obtain user id for authenticated user '%s'!", remoteUser));
                jaErrors.put(SCError.createErrorObj(SCError.UNAUTHORIZED, e, bsl));
                sendResponse(jaSuccess, jaErrors, ct, response);
                return true;
            }

            // Check permissions
            if (!SCSecurity.canUploadComponent(userId, remoteUserId, bsl, request)) {
                jaErrors.put(SCError.createErrorObj(SCError.UNAUTHORIZED, bsl));
                sendResponse(jaSuccess, jaErrors, ct, response);
                return true;
            }

            ServletFileUpload fileUpload = new ServletFileUpload(new DiskFileItemFactory());
            List<FileItem> uploadedFiles;
            try {
                uploadedFiles = fileUpload.parseRequest(request);
            }
            catch (FileUploadException e) {
                jaErrors.put(SCError.createErrorObj(SCError.UPLOAD_ERROR, e, bsl));
                sendResponse(jaSuccess, jaErrors, ct, response);
                return true;
            }

            // Mapping between component uri and set of contexts, and temp context folders
            Map<String, Component> componentsMap = new HashMap<String, Component>();

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
                        if (compResList.size() != 1)
                            throw new Exception("RDF model does not contain an executable component, " +
                                    "or contains more than one component.");

                        // Create a temp folder to store any context files for this component
                        File tempFolder = createTempFolder(String.format("%s_%d_",
                                request.getRemoteAddr(), request.getRemotePort()));
                        if (tempFolder == null) {
                            logger.severe("Cannot create a temp folder to store the context files in this request!");
                            sendErrorInternalServerError(response);
                            return true;
                        }

                        currentComponentResUri = compResList.get(0).getURI();
                        componentsMap.put(currentComponentResUri, new Component(compModel, tempFolder));
                    }
                    catch (Exception e) {
                        String descriptorName = file.isFormField() ? file.getString().trim() : file.getName();
                        logger.log(Level.WARNING, String.format("Error parsing RDF from '%s'", descriptorName), e);

                        JSONObject joError = (e instanceof IOException) ?
                                SCError.createErrorObj(SCError.NETWORK_ERROR, e, bsl) :
                                SCError.createErrorObj(SCError.RDF_PARSE_ERROR, e, bsl, descriptorName);
                        joError.put("descriptor", descriptorName);
                        jaErrors.put(joError);

                        skipProcessingContexts = true;
                        continue;
                    }
                }

                else

                if (file.getFieldName().equalsIgnoreCase("context")) {
                    if (skipProcessingContexts) continue;

                    // Sanity check
                    if (currentComponentResUri == null) {
                        JSONObject joError = SCError.createErrorObj(SCError.INCOMPLETE_REQUEST, bsl);
                        joError.put("param", "component_rdf");
                        jaErrors.put(joError);
                        skipProcessingContexts = true;
                        continue;
                    }

                    Component component = componentsMap.get(currentComponentResUri);

                    // If we're uploading a context file as a form field (non-file)
                    // then assume it's specifying a full URL, otherwise the field
                    // is assumed to be the context file uploaded
                    if (file.isFormField()) {
                        try {
                            URL url = new URL(file.getString());
                            URLConnection connection = url.openConnection();
                            connection.setConnectTimeout(CONNECTION_TIMEOUT);
                            connection.setReadTimeout(READ_TIMEOUT);
                            String contentType = connection.getContentType();
                            component.getContexts().put(url, contentType);
                        }
                        catch (MalformedURLException e) {
                            JSONObject joError = SCError.createErrorObj(SCError.INVALID_PARAM_VALUE, e, bsl);
                            joError.put("url", file.getString());
                            joError.put("compUri", currentComponentResUri);
                            skipProcessingContexts = true;
                            continue;
                        }
                        catch (IOException e) {
                            // Error reading from URL
                            logger.log(Level.WARNING, "Error reading from context url: " + file.getString(), e);

                            JSONObject joError = SCError.createErrorObj(SCError.NETWORK_ERROR, e, bsl);
                            joError.put("compUri", currentComponentResUri);
                            joError.put("url", file.getString());
                            jaErrors.put(joError);

                            try {
                                FileUtils.deleteDirectory(component.getTempContextFolder());
                            }
                            catch (IOException ex) {
                                logger.log(Level.WARNING, "Cannot delete temp context folder: " +
                                        component.getTempContextFolder(), ex);
                            }

                            componentsMap.remove(currentComponentResUri);
                            skipProcessingContexts = true;
                            continue;
                        }
                    } else {
                        // Add the context file to the current component being uploaded
                        File contextFile = new File(component.getTempContextFolder(), file.getName());
                        try {
                            file.write(contextFile);
                        }
                        catch (Exception e) {
                            logger.log(Level.SEVERE, "Cannot save uploaded context file: " + contextFile, e);
                            JSONObject joError = SCError.createErrorObj(SCError.IO_ERROR, e, bsl);
                            joError.put("compUri", currentComponentResUri);
                            joError.put("context_file", file.getName());
                            jaErrors.put(joError);
                            skipProcessingContexts = true;
                            continue;
                        }

                        try {
                            component.getContexts().put(contextFile.toURI().toURL(), file.getContentType());
                        }
                        catch (MalformedURLException e) {
                            // Should never happen
                            throw new RuntimeException(e);
                        }
                    }
                }
            }

            if (componentsMap.size() > 0) {
                // Accumulate the component models
                Model model = ModelFactory.createDefaultModel();

                for (Component component : componentsMap.values())
                    model.add(component.getModel());

                QueryableRepository qr = new RepositoryImpl(model);

                for (ExecutableComponentDescription ecd : qr.getAvailableExecutableComponentDescriptions()) {
                    String origUri = ecd.getExecutableComponent().getURI();

                    // Attempt to add the component to the backend storage
                    Map<URL, String> contexts = componentsMap.get(origUri).getContexts();
                    JSONObject joResult = bsl.addComponent(userId, ecd, contexts);

                    String compId = joResult.getString("uuid");
                    int compVersion = joResult.getInt("version");

                    String compUrl = getComponentBaseAccessUrl(request, compId, compVersion) + ".ttl";

                    JSONObject joComponent = new JSONObject();
                    joComponent.put("orig_uri", origUri);
                    joComponent.put("uuid", compId);
                    joComponent.put("version", compVersion);
                    joComponent.put("url", compUrl);

                    jaSuccess.put(joComponent);
                }

                // Clean up the temp folders
                for (Component component : componentsMap.values())
                    try {
                        FileUtils.deleteDirectory(component.getTempContextFolder());
                    }
                    catch (IOException e) {
                        logger.log(Level.WARNING, "Cannot delete temp context folder: "
                                + component.getTempContextFolder(), e);
                    }
            }
        }
        catch (UserNotFoundException e) {
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

    private class Component {
        private final Model model;
        private final File tempContextFolder;
        private final Map<URL, String> contexts = new HashMap<URL, String>();

        public Component(Model model, File tempContextFolder) {
            this.model = model;
            this.tempContextFolder = tempContextFolder;
        }

        public Model getModel() {
            return model;
        }

        public Map<URL, String> getContexts() {
            return contexts;
        }

        public File getTempContextFolder() {
            return tempContextFolder;
        }
    }
}

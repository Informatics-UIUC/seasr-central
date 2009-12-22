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
import static org.seasr.central.ws.restlets.Tools.sendErrorBadRequest;
import static org.seasr.central.ws.restlets.Tools.sendErrorInternalServerError;
import static org.seasr.central.ws.restlets.Tools.sendErrorNotAcceptable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.meandre.core.repository.QueryableRepository;
import org.meandre.core.repository.RepositoryImpl;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ContentTypes;
import org.seasr.meandre.support.generic.io.ModelUtils;

import com.google.gdata.util.ContentType;
import com.hp.hpl.jena.rdf.model.Model;


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

        String screen_name = values[0];

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

        Map<FileItem, Set<FileItem>> componentsMap = new HashMap<FileItem, Set<FileItem>>();

        for (FileItem file : files) {
            if (file == null || file.isFormField() || file.getName().length() == 0)
                continue;

            if (file.getSize() == 0)
                logger.warning(String.format("Uploaded file '%s' has size 0", file.getName()));

            logger.fine(String.format("Uploaded file '%s' (%,d bytes) [%s]", file.getName(), file.getSize(), file.getFieldName()));

            if (file.getFieldName().equalsIgnoreCase("component_rdf")) {
                try {
                    Model model = ModelUtils.getModel(file.getInputStream(), null);
                    QueryableRepository qr = new RepositoryImpl(model);

                }
                catch (IOException e) {
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
        }

        response.setStatus(HttpServletResponse.SC_OK);

        return true;
    }

}

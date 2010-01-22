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

package org.seasr.central.ws.restlets.repository;

import com.google.gdata.util.ContentType;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.RDF;
import org.meandre.core.utils.vocabulary.RepositoryVocabulary;
import org.seasr.central.main.SC;
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ContentTypes;
import org.seasr.meandre.support.generic.io.ModelUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

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
        return "/repository/component/(.+)/(.+?)(?:/|" + regexExtensionMatcher() + ")?$";
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

        Model compModel;

        try {
            // Attempt to retrieve the component from the backend store
            compModel = bsl.getComponent(componentId, version);

            if (compModel == null) {
                sendErrorNotFound(response);
                return true;
            }
        }
        catch (BackendStoreException e) {
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
            return true;
        }

        String oldCompUri = compModel.listSubjectsWithProperty(
                RDF.type, RepositoryVocabulary.executable_component).nextResource().getURI();

        String serverBase = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());
        String compUri = String.format("%s/repository/component/%s/%d", serverBase, componentId, version);
        String contextBase = String.format("%s/repository/context/", serverBase);

        if (oldCompUri.endsWith("/")) compUri += "/";

        // Update the component URI and component context(s) URIs
        // TODO: Find a better method to do this (one that relies on direct Model manipulation)
        String sModel;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        compModel.write(baos);
        try {
            sModel = baos.toString("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
            return true;
        }
        sModel = sModel.replaceAll(Pattern.quote(oldCompUri), compUri);  // Update the component URI
        sModel = sModel.replaceAll("context://localhost/", contextBase); // Update the component context(s) URIs

        compModel = ModelFactory.createDefaultModel();
        try {
            ModelUtils.readModelFromString(compModel, sModel);

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
        catch (IOException e) {
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
            return true;
        }

        return true;
    }
}

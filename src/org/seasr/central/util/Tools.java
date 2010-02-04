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

package org.seasr.central.util;

import com.google.gdata.util.ContentType;
import com.hp.hpl.jena.rdf.model.Model;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.meandre.core.ExecutableComponent;
import org.meandre.core.repository.DataPortDescription;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.seasr.central.main.SC;
import org.seasr.central.main.SCServer;
import org.seasr.central.ws.restlets.ContentTypes;
import org.seasr.meandre.support.generic.crypto.Crypto;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Boris Capitanu
 */
public class Tools {
    private static final Logger logger = Logger.getLogger(SCServer.class.getName());

    public static final int CONNECTION_TIMEOUT = 0;   // Used for connecting and reading from URLs
    public static final int READ_TIMEOUT = 0;         // Setting this value to 0 signifies "wait-forever"

    /** The XSL transformation used to convert XML to HTML */
    private static final Transformer xslTrans;

    /** Define the RDF format types RDF, TTL, NT */
    public enum RDFFormat { RDF, TTL, NT }

    /** Defines the possible REST operation results */
    public enum OperationResult { SUCCESS, FAILURE }


    static {
        // Initialize the XSL transformation engine
        String xsltFile = Tools.class.getSimpleName() + ".xslt";
        StreamSource xsltSource = new StreamSource(Tools.class.getResourceAsStream(xsltFile));
        TransformerFactory xslTransFact = TransformerFactory.newInstance();

        try {
            xslTrans = xslTransFact.newTransformer(xsltSource);
        }
        catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Sets the servlet response content type to html.
     *
     * @param response The response object
     * @throws IOException A problem thrown while writing the content
     */
    public static void sendRawContent(HttpServletResponse response, Object content) {
        try {
            response.getWriter().print(content.toString());
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * Given a model, it serializes it to the response with the proper requested
     * serialization format. The current supported formats are RDF-XML, TTL, and
     * N-TRIPLE.
     *
     * @param response The response object
     * @param model The model to dump
     * @param format The format
     * @throws IOException Problem thrown while writing to the response
     */
    public static void sendRDFModel(HttpServletResponse response, Model model, RDFFormat format) throws IOException {
        switch (format) {
            case RDF:
                response.setContentType(ContentType.APPLICATION_XML.toString());
                model.write(response.getOutputStream(), "RDF/XML-ABBREV");
                break;

            case TTL:
                response.setContentType(ContentType.TEXT_PLAIN.toString());
                model.write(response.getOutputStream(), "TTL");
                break;

            case NT:
                response.setContentType(ContentType.TEXT_PLAIN.toString());
                model.write(response.getOutputStream(), "N-TRIPLE");
                break;

            default:
                throw new RuntimeException("Invalid RDF format specified");
        }
    }

    public static void sendContent(HttpServletResponse response, JSONObject content, ContentType contentType)
            throws IOException {

        response.setContentType(contentType.toString());

        // JSON
        if (contentType.equals(ContentType.JSON)) {
            sendRawContent(response, content);
        }

        else

        // SmartGWT
        if (contentType.equals(ContentTypes.SmartGWT)) {
            System.out.println(contentType);
            throw new RuntimeException("Not implemented");
        }

        else

        // XML
        if (contentType.equals(ContentType.APPLICATION_XML)) {
            try {
                String xmlc = XML.toString(content, "meandre_item");
                sendRawContent(response, "<?xml version='1.0' encoding='UTF-8'?><meandre_response>");
                sendRawContent(response, xmlc);
                sendRawContent(response, "</meandre_response>");
            }
            catch (JSONException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        else

        // HTML
        if (contentType.equals(ContentType.TEXT_HTML)) {
            try {
                String xmlc = "<?xml version='1.0' encoding='UTF-8'?><meandre_response>";
                xmlc += XML.toString(content, "meandre_item");
                xmlc += "</meandre_response>";
                StreamSource xmlSource = new StreamSource(new StringReader(xmlc));
                StreamResult result = new StreamResult(response.getOutputStream());
                xslTrans.transform(xmlSource, result);
            }
            catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        else

        // TXT
        if (contentType.equals(ContentType.TEXT_PLAIN)) {
            try {
                sendRawContent(response, content.toString(4));
            }
            catch (JSONException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        else
            sendErrorNotAcceptable(response);
    }


    /**
     * Sets the servlet response code to unauthorized (401)
     *
     * @param response The response object
     */
    public static void sendErrorUnauthorized(HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
        catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * Sets the servlet response code to not found (404)
     *
     * @param response The response object
     */
    public static void sendErrorNotFound(HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * Sets the servlet response code to forbidden (403)
     *
     * @param response The response object
     */
    public static void sendErrorForbidden(HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
        catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * Sets the servlet response code to bad request (400)
     *
     * @param response The response object
     */
    public static void sendErrorBadRequest(HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
        catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * Sets the servlet response code to expectation failed (417)
     *
     * @param response The response object
     */
    public static void sendErrorExpectationFail(HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_EXPECTATION_FAILED);
        }
        catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * Sets the servlet response code to unsupported media type (415)
     *
     * @param response The response object
     */
    public static void sendErrorUnsupportedMediaType(HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        }
        catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * Sets the servlet response code to internal server error (500)
     *
     * @param response The response object
     */
    public static void sendErrorInternalServerError(HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * Sets the servlet response code to not acceptable (406)
     *
     * @param response The response object
     */
    public static void sendErrorNotAcceptable(HttpServletResponse response) {
        try {
            response.sendError(HttpServletResponse.SC_NOT_ACCEPTABLE);
        }
        catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * Returns the map containing all the parameters and values passed to the request
     *
     * @param request The request
     * @return The request parameter map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String[]> extractTextPayloads(HttpServletRequest request) {
        Map<String, String[]> map = new HashMap<String, String[]>();

        Enumeration it = request.getParameterNames();
        while (it.hasMoreElements()) {
            String name = it.nextElement().toString();
            String[] values = request.getParameterValues(name);
            map.put(name, values);
        }

        return map;
    }

    /**
     * Returns a formatted string containing the exception details
     *
     * @param e The exception
     * @return A formatted string containing the details of the exception
     */
    public static String getExceptionDetails(Throwable e) {
        StringBuffer sb = new StringBuffer();
        String errMsg = e.getMessage();
        String errCauseMsg = (e.getCause() != null) ? e.getCause().getMessage() : null;

        if (errMsg != null)
            sb.append(errMsg);
        else if (errCauseMsg != null)
            sb.append(errCauseMsg);

        return sb.toString();
    }

    /**
     * Creates a JSON error object
     *
     * @param message The message
     * @param reason  The reason, or null
     * @return The JSON error object
     * @throws JSONException
     */
    public static JSONObject createJSONErrorObj(String message, String reason) throws JSONException {
        JSONObject joError = new JSONObject();
        joError.put("message", message);
        if (reason != null)
            joError.put("reason", reason);

        return joError;
    }

    /**
     * Creates a JSON error object
     *
     * @param message The message
     * @param e       The exception, or null
     * @return The JSON error object
     * @throws JSONException
     */
    public static JSONObject createJSONErrorObj(String message, Throwable e) throws JSONException {
        return (e != null) ? createJSONErrorObj(message, getExceptionDetails(e)) : createJSONErrorObj(message, (String) null);
    }

    /**
     * Creates a temporary folder with a specified name prefix
     *
     * @param prefix The name prefix
     * @return The File
     */
    public static File createTempFolder(String prefix) {
        File tempFolder;

        try {
            tempFolder = File.createTempFile(prefix, "");
        }
        catch (IOException e) {
           return null;
        }

        // Delete the temp file so we can make a folder with the same name
        tempFolder.delete();

        return (tempFolder.mkdirs() ? tempFolder : null);
    }

    /**
     * Computes a SHA1 digest for a given string
     *
     * @param string The string
     * @return The SHA1 digest
     */
    public static String computePasswordDigest(String string) {
        try {
            return Crypto.getHexString(Crypto.createSHA1Hash(string.getBytes("UTF-8")));
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Calculates a hash signature for the core parts of a component
     *
     * @param component The component
     * @param contextHashes The hashes of the component's context files
     * @return The core hash
     */
    public static byte[] getComponentCoreHash(ExecutableComponentDescription component,
                                              SortedSet<String> contextHashes) {
        try {
            return Crypto.createMD5Hash(getComponentCoreAsString(component, contextHashes).getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e) {
            // This should not happen
            logger.log(Level.SEVERE, null, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper method that isolates the "core" features of a component
     *
     * @param component The component
     * @param contextHashes The component's context hashes
     * @return A string containing the core features
     */
    private static String getComponentCoreAsString(ExecutableComponentDescription component,
                                                   SortedSet<String> contextHashes) {
        StringBuilder sb = new StringBuilder();

        sb.append("runnable: ").append(component.getRunnable()).append("\n");
        sb.append("format: ").append(component.getFormat()).append("\n");
        String resLocation = component.getLocation().toString();
        sb.append("resource_location: ").append(resLocation.substring(resLocation.lastIndexOf("/") + 1)).append("\n");
        sb.append("mode: ").append(component.getMode()).append("\n");
        sb.append("firing_policy: ").append(component.getFiringPolicy()).append("\n");

        SortedSet<String> sortedStrings = new TreeSet<String>();
        for (DataPortDescription inputPort : component.getInputs())
            sortedStrings.add(inputPort.getName());

        for (String inputPort : sortedStrings)
            sb.append("input: ").append(inputPort).append("\n");

        sortedStrings.clear();

        for (DataPortDescription outputPort : component.getOutputs())
            sortedStrings.add(outputPort.getName());

        for (String outputPort : sortedStrings)
            sb.append("output: ").append(outputPort).append("\n");

        SortedMap<String, String> propMap = new TreeMap<String, String>();
        propMap.putAll(component.getProperties().getValueMap());

        for (Map.Entry<String, String> entry : propMap.entrySet())
            sb.append("property: ").append("key=").append(entry.getKey())
                    .append(" value=").append(entry.getValue()).append("\n");

        for (String contextHash : contextHashes)
            sb.append("context: ").append(contextHash).append("\n");

        return sb.toString();
    }

    /**
     * Returns a hash code for a particular rights text
     *
     * @param rights The rights text
     * @return The hash code
     */
    public static byte[] getRightsHash(String rights) {
        try {
            return Crypto.createMD5Hash(rights.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e) {
            logger.log(Level.SEVERE, null, e);
            throw new RuntimeException(e);
        }
    }

}

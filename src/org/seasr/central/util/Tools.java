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
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.meandre.core.repository.*;
import org.meandre.core.utils.vocabulary.RepositoryVocabulary;
import org.seasr.central.main.SCServer;
import org.seasr.central.ws.restlets.ContentTypes;
import org.seasr.meandre.support.generic.crypto.Crypto;
import org.seasr.meandre.support.generic.io.ModelUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author Boris Capitanu
 */
public class Tools {
    private static final Logger logger = Logger.getLogger(SCServer.class.getName());

    public static final int CONNECTION_TIMEOUT = 0;   // Used for connecting and reading from URLs
    public static final int READ_TIMEOUT = 0;         // Setting this value to 0 signifies "wait-forever"

    /** The XSL transformation used to convert XML to HTML */
    //private static final Transformer xslTrans;

    /** Define the RDF format types RDF, TTL, NT */
    public enum RDFFormat { RDF, TTL, NT }

    /** Defines the possible REST operation results */
    public enum OperationResult { SUCCESS, FAILURE }


    static {
//        // Initialize the XSL transformation engine
//        String xsltFile = Tools.class.getSimpleName() + ".xslt";
//        StreamSource xsltSource = new StreamSource(Tools.class.getResourceAsStream(xsltFile));
//        TransformerFactory xslTransFact = TransformerFactory.newInstance();
//
//        try {
//            xslTrans = xslTransFact.newTransformer(xsltSource);
//        }
//        catch (TransformerConfigurationException e) {
//            throw new RuntimeException(e);
//        }
    }

    /**
     * Sends a response
     *
     * @param response The response object
     * @param content The content being sent
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

    /**
     * Sends an appropriately formatted response to a API request
     *
     * @param response The response object
     * @param content The content to send
     * @param contentType The content type requested for the response
     * @throws IOException
     * @throws JSONException
     */
    public static void sendContent(HttpServletResponse response, JSONObject content, ContentType contentType)
            throws IOException, JSONException {

        JSONArray jaSuccess = content.getJSONArray(OperationResult.SUCCESS.name());
        JSONArray jaFailure = content.getJSONArray(OperationResult.FAILURE.name());

        response.setContentType(contentType.toString());
        response.setStatus(determineHttpStatusCode(jaSuccess, jaFailure));

        // JSON
        if (contentType.equals(ContentType.JSON)) {
            sendRawContent(response, content);
        }

        else

        // SmartGWT
        if (contentType.equals(ContentTypes.SmartGWT)) {
            response.setStatus(HttpServletResponse.SC_OK);
            sendRawContent(response, content);
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

//        // HTML
//        if (contentType.equals(ContentType.TEXT_HTML)) {
//            try {
//                String xmlc = "<?xml version='1.0' encoding='UTF-8'?><meandre_response>";
//                xmlc += XML.toString(content, "meandre_item");
//                xmlc += "</meandre_response>";
//                StreamSource xmlSource = new StreamSource(new StringReader(xmlc));
//                StreamResult result = new StreamResult(response.getOutputStream());
//                xslTrans.transform(xmlSource, result);
//            }
//            catch (Exception e) {
//                logger.log(Level.SEVERE, e.getMessage(), e);
//            }
//        }
//
//        else

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
     * Determines the HTTP status code based on the success / failure of a request
     *
     * @param jaSuccess The success response
     * @param jaFailure The failure response
     * @return The HTTP status code
     * @throws JSONException
     */
    private static int determineHttpStatusCode(JSONArray jaSuccess, JSONArray jaFailure) throws JSONException {
        if (jaFailure.length() == 0)
            return HttpServletResponse.SC_OK;

        Set<Integer> errorCodes = new HashSet<Integer>();

        for (int i = 0, iMax = jaFailure.length(); i < iMax; i++) {
            JSONObject joError = jaFailure.getJSONObject(i);
            Object obj = joError.remove("http_status");
            if (obj != null)
                errorCodes.add((Integer) obj);
        }

        if (jaSuccess.length() > 0)
            return 207;

        if (errorCodes.size() > 1) {
            if (errorCodes.contains(HttpServletResponse.SC_INTERNAL_SERVER_ERROR))
                return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

            return HttpServletResponse.SC_BAD_REQUEST;
        }

        return errorCodes.iterator().next();
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
//    @SuppressWarnings("unchecked")
//    public static Map<String, String[]> extractRequestParameters(HttpServletRequest request) {
//        Map<String, String[]> map = new HashMap<String, String[]>();
//
//        Enumeration it = request.getParameterNames();
//        while (it.hasMoreElements()) {
//            String name = it.nextElement().toString();
//            String[] values = request.getParameterValues(name);
//            map.put(name, values);
//        }
//
//        return map;
//    }

    /**
     * Returns a list of non-empty parameter values for the specified request parameter
     *
     * @param parameter The HTTP request parameter
     * @param request The HTTP request object
     * @return The list of non-empty parameter values for this parameter, or null if none found
     */
//    public static List<String> getSanitizedRequestParameterValues(String parameter, HttpServletRequest request) {
//        List<String> result = new ArrayList<String>();
//
//        String[] values = request.getParameterValues(parameter);
//        if (values != null) {
//            for (String value : values) {
//                value = value.trim();
//                if (value.length() > 0)
//                    result.add(value);
//            }
//        }
//
//        return result.size() > 0 ? result : null;
//    }

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
//    public static JSONObject createJSONErrorObj(String message, String reason) throws JSONException {
//        JSONObject joError = new JSONObject();
//        joError.put("message", message);
//        if (reason != null)
//            joError.put("reason", reason);
//
//        return joError;
//    }

    /**
     * Creates a JSON error object
     *
     * @param message The message
     * @param e       The exception, or null
     * @return The JSON error object
     * @throws JSONException
     */
//    public static JSONObject createJSONErrorObj(String message, Throwable e) throws JSONException {
//        return (e != null) ? createJSONErrorObj(message, getExceptionDetails(e)) : createJSONErrorObj(message, (String) null);
//    }

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
            return Crypto.toHexString(Crypto.createSHA1Hash(string.getBytes("UTF-8")));
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Computes a hash signature for the core parts of a component
     *
     * @param component The component
     * @param contextHashes The hashes of the component's context files
     * @return The core hash
     */
    public static byte[] getComponentCoreHash(ExecutableComponentDescription component,
                                              SortedSet<BigInteger> contextHashes) {
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
     * Computes a hash signature for the core parts of a flow
     *
     * @param flow The flow
     * @return The core hash
     */
    public static byte[] getFlowCoreHash(FlowDescription flow) {
        try {
            return Crypto.createMD5Hash(getFlowCoreAsString(flow).getBytes("UTF-8"));
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
                                                   SortedSet<BigInteger> contextHashes) {
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

        SortedMap<String, String> propMap = new TreeMap<String, String>(component.getProperties().getValueMap());

        for (Map.Entry<String, String> entry : propMap.entrySet())
            sb.append("property: ").append("key=").append(entry.getKey())
                    .append(" value=").append(entry.getValue()).append("\n");

        for (BigInteger contextHash : contextHashes)
            sb.append("context: ").append(contextHash).append("\n");

        return sb.toString();
    }

    /**
     * Helper method that isolates the "core" features of a flow
     *
     * @param flow The flow
     * @return A string containing the core features
     */
    private static String getFlowCoreAsString(FlowDescription flow) {
        StringBuilder sb = new StringBuilder();

        for (ExecutableComponentInstanceDescription ecid : flow.getExecutableComponentInstancesOrderedByName()) {
            sb.append("instance: ").append(ecid.getExecutableComponentInstance().getURI()).append("\n");
            sb.append("instance_of: ").append(ecid.getExecutableComponent().getURI()).append("\n");
            SortedMap<String, String> propMap = new TreeMap<String, String>(ecid.getProperties().getValueMap());
            for (Map.Entry<String, String> entry : propMap.entrySet())
                sb.append("property: ").append("key=").append(entry.getKey())
                        .append(" value=").append(entry.getValue()).append("\n");
        }

        SortedMap<String, String> connectorMap = new TreeMap<String, String>();
        for (ConnectorDescription cd : flow.getConnectorDescriptions()) {
            StringBuilder csb = new StringBuilder();
            csb.append("connector: ").append(cd.getConnector().getURI()).append("\n");
            csb.append("source_instance: ").append(cd.getSourceInstance().getURI()).append("\n");
            csb.append("source_port: ").append(cd.getSourceInstanceDataPort().getURI()).append("\n");
            csb.append("target_instance: ").append(cd.getTargetInstance().getURI()).append("\n");
            csb.append("target_port: ").append(cd.getTargetInstanceDataPort().getURI()).append("\n");
            connectorMap.put(cd.getConnector().getURI(), csb.toString());
        }

        for (String connector : connectorMap.values())
            sb.append("\n").append(connector);

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

    public static String getComponentBaseAccessUrl(HttpServletRequest request, String compId, int compVersion) {
        return String.format("%s://%s:%d/services/components/%s/versions/%d",
                request.getScheme(), request.getServerName(), request.getServerPort(), compId, compVersion);
    }

    public static String getFlowBaseAccessUrl(HttpServletRequest request, String flowId, int flowVersion) {
        return String.format("%s://%s:%d/services/flows/%s/versions/%d",
                request.getScheme(), request.getServerName(), request.getServerPort(), flowId, flowVersion);
    }

    /**
     * Rewrites the component model to align the URIs with SC
     *
     * @param compModel The component model to rewrite
     * @param componentId The SC component id
     * @param version The SC component version
     * @param request The HTTP request
     * @throws IOException
     */
    public static void rewriteComponentModel(Model compModel, UUID componentId, int version, HttpServletRequest request)
            throws IOException {

        Resource resExecComp = compModel.listSubjectsWithProperty(
                RDF.type, RepositoryVocabulary.executable_component).nextResource();
        String oldCompUri = resExecComp.getURI();
        String compUri = getComponentBaseAccessUrl(request, componentId.toString(), version);
        String contextBase = String.format("%s/contexts/", compUri);

        if (oldCompUri.endsWith("/")) compUri += "/";

        // TODO: Find a better method to do this (one that relies on direct Model manipulation)
        String sModel = ModelUtils.modelToDialect(compModel, "TURTLE");
        sModel = sModel.replaceAll(Pattern.quote(oldCompUri), compUri);  // Update the component URI
        sModel = sModel.replaceAll("context://localhost/", contextBase); // Update the component context(s) URIs

        compModel.removeAll();
        ModelUtils.readModelFromString(compModel, sModel);
    }

    /**
     * Rewrites the flow model to align the URIs with SC
     *
     * @param flowModel The flow model to rewrite
     * @param flowId The SC flow id
     * @param version The SC flow version
     * @param request The HTTP request
     * @throws IOException
     */
    public static void rewriteFlowModel(Model flowModel, UUID flowId, int version, HttpServletRequest request)
            throws IOException {

        Resource resFlow = flowModel.listSubjectsWithProperty(
                RDF.type, RepositoryVocabulary.flow_component).nextResource();
        String oldFlowUri = resFlow.getURI();
        String flowUri = getFlowBaseAccessUrl(request, flowId.toString(), version);

        if (oldFlowUri.endsWith("/")) flowUri += "/";

        // Update the flow URI
        // TODO: Find a better method to do this (one that relies on direct Model manipulation)
        String sModel = ModelUtils.modelToDialect(flowModel, "TURTLE");
        sModel = sModel.replaceAll(Pattern.quote(oldFlowUri), flowUri);  // Update the flow URI

        flowModel.removeAll();
        ModelUtils.readModelFromString(flowModel, sModel);
    }
}

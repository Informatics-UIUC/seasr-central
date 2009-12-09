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

package org.seasr.central.ws.restlets;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

import com.google.gdata.util.ContentType;
import com.hp.hpl.jena.rdf.model.Model;

/**
 * This class provides basic auxiliary tools for restlets. For instance,
 * methods for dumping, JSON, XML, TXT, RDF, TTL, NT etc.
 *
 * @author xavier
 * @author Boris Capitanu
 *
 */
public class Tools {

    /** Define the SmartGWT ContentType */
    public static final ContentType ContentType_SmartGWT =
        new ContentType("smartgwt/json;" + ContentType.ATTR_CHARSET + "=UTF-8").lock();

    /** Define the RDF format types RDF, TTL, NT */
    public enum RDFFormat { RDF, TTL, NT }

    /** Defines the possible REST operation results */
    public enum OperationResult { SUCCESS, FAILURE };

	/** The formatter class to use for the central SC logger.
	 *
	 * @author xavier
	 */
    private static class SCAPIFormatter extends Formatter {

        /** Creates the default formatter */
        public SCAPIFormatter () {
        }

        /**
         * Formats the record.
         *
         * @param record The log record to format
         * @return The formated record
         */
        @Override
        public String format(LogRecord record) {
            String msg = record.getMessage();
            if (msg == null || msg.length() == 0)
                msg = null;

            Throwable thrown = record.getThrown();
            if (thrown != null) {
                if (msg == null)
                    msg = thrown.toString();
                else
                    msg += "  (" + thrown.toString() + ")";
            }

            String srcClassName = record.getSourceClassName();
            String srcMethodName = record.getSourceMethodName();

            srcClassName = srcClassName.substring(srcClassName.lastIndexOf(".") + 1);

            return String.format("%5$tY-%5$tm-%5$td %5$tH:%5$tM:%5$tS.%5$tL [%s]: %s\t[%s.%s]%n",
                    record.getLevel(), msg, srcClassName, srcMethodName, new Date(record.getMillis()));
        }
    }


	/** The xsl transformation to use to xml to html. */
	private static Transformer xslTrans;

	/** The central logger for restlets */
	public static Logger logger;

	static {
		// Initialize the logger
		logger = Logger.getLogger(Tools.class.getName());
		FileHandler handler;
		try {
			handler = new FileHandler("logs"+File.separator+"scapi.log",true);
			handler.setFormatter(new SCAPIFormatter());
	        logger.addHandler(handler);
		} catch (SecurityException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		// Initialize the transformation engine
		String xsltFile = Tools.class.getSimpleName()+".xslt";
		StreamSource xsltSource = new StreamSource(Tools.class.getResourceAsStream(xsltFile));
		TransformerFactory xslTransFact = TransformerFactory.newInstance();
		try {
			xslTrans = xslTransFact.newTransformer(xsltSource);
		} catch (TransformerConfigurationException e) {
			xslTrans = null;
		}
	}

	/**
	 * Sets the servlet response code to unauthorized.
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
	 * Sets the servlet response code to not found.
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
	 * Sets the servlet response code to forbidden.
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
	 * Sets the servlet response code to bad request.
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
	 * Sets the servlet response code to expectation failed.
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
	 * Sets the servlet response code to unsupported media type
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
     * Sets the servlet response code to internal server error
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
     * Sets the servlet response code to not acceptable
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
	public static void sendRDFModel(HttpServletResponse response, Model model, RDFFormat format)
	throws IOException {
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
		if (contentType.equals(ContentType_SmartGWT)) {
		    System.out.println(contentType);
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
		    sendErrorNotFound(response);
	}

	/**
	 * Returns the map containing all the parameters and values pased
	 * to the request.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String[]> extractTextPayloads(HttpServletRequest request) {
		Map<String, String[]> map = new HashMap<String, String[]>();

		Enumeration it = request.getParameterNames();
		while (it.hasMoreElements()) {
			String name = it.nextElement().toString();
			String[] values = request.getParameterValues(name);
			map.put(name,values);
		}

		return map;
	}
}

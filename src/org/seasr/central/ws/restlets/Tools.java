/**
 * 
 */
package org.seasr.central.ws.restlets;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.Formatter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.XML;

import com.hp.hpl.jena.rdf.model.Model;

/** This class provides basic auxiliar tools for reslets. For instance,
 * methods for dumping, JSON, XML, txt, RDF, TTL, NT etc.
 * 
 * @author xavier
 *
 */
public class Tools {
	
	/** The date formater */
    private final static SimpleDateFormat FORMATER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    
    /** The new line separator */
    private final static String NEW_LINE = System.getProperty("line.separator");

	/** The formatter class to use for the centra SC logger.
	 * 
	 * @author xavier
	 */
	private static class SCAPIFormatter extends Formatter {
	       
	       /** Creates the default formater */
	       public SCAPIFormatter () {
	       }
	       
	       
	       /** Formats the record.
	        * 
	        * @param record The log record to format
	        * @return The formated record
	        */
	         public String format(LogRecord record) {
	               
	               String sTimeStamp = FORMATER.format(new Date(record.getMillis()));
	               
	               return sTimeStamp+"::"+
	                   record.getLevel()+":  "+
	                   record.getMessage()+ "  " + 
	                   NEW_LINE;
	         }
	}


	/** The xsl transformation to use to xml to html. */
	private static Transformer xslTrans;
	
	/** The central logger for restlets */
	public static Logger log;
	
	static {
		// Initialize the logger
		log = Logger.getLogger(Tools.class.getName());
		FileHandler handler;
		try {
			handler = new FileHandler("logs"+File.separator+"scapi.log",true);
			handler.setFormatter(new SCAPIFormatter());
	        log.addHandler(handler);
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

	/** Sets the servlet response status to OK.
	 * 
	 * @param response The response object
	 */
	public static void statusOK ( HttpServletResponse response ) {
		response.setStatus(HttpServletResponse.SC_OK);
	}

	/** Sets the servlet response code to unauthorized.
	 * 
	 * @param response The response object
	 * @throws IOException Problem a rised sending error
	 */
	public static void errorUnauthorized ( HttpServletResponse response ) throws IOException { 
		response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
	}

	/** Sets the servlet response code to not found.
	 * 
	 * @param response The response object
	 * @throws IOException 
	 * @throws IOException Problem a rised sending error
	 */
	public static void errorNotFound ( HttpServletResponse response ) throws IOException {
		response.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	/** Sets the servlet response code to forbidden.
	 * 
	 * @param response The response object
	 * @throws IOException 
	 * @throws IOException Problem a rised sending error
	 */
	public static void errorForbidden ( HttpServletResponse response ) throws IOException {
		response.sendError(HttpServletResponse.SC_FORBIDDEN);
	}

	/** Sets the servlet response code to bad request.
	 * 
	 * @param response The response object
	 * @throws IOException 
	 * @throws IOException Problem a rised sending error
	 */
	public static void  errorBadRequest ( HttpServletResponse response ) throws IOException {
		response.sendError(HttpServletResponse.SC_BAD_REQUEST);
	}

	/** Sets the servlet response code to expectation failed.
	 * 
	 * @param response The response object
	 * @throws IOException 
	 * @throws IOException Problem a rised sending error
	 */
	public static void  errorExpectationFail ( HttpServletResponse response ) throws IOException {
		response.sendError(HttpServletResponse.SC_EXPECTATION_FAILED);
	}

	/** Sets the servlet content type to text/plain
	 * 
	 * @param response The response object
	 */
	public static void  contentTextPlain ( HttpServletResponse response ) {
		response.setContentType("text/plain");
	}

	/** Sets the servlet response cotentnt type to json
	 * 
	 * @param response The response object
	 */
	public static void  contentAppJSON ( HttpServletResponse response ) {
		response.setContentType("application/json");
	}

	/** Sets the servlet response content type to xml
	 * 
	 * @param response The response object
	 */
	public static void  contentAppXML ( HttpServletResponse response ) {
		response.setContentType("application/xml");
	}

	/** Sets the servlet response content type to html.
	 * 
	 * @param response The response object
	 */
	public static void contentAppHTML ( HttpServletResponse response ) {
		response.setContentType("text/html");
	}

	/** Sets the servlet response content type to html.
	 * 
	 * @param response The response object
	 * @throws IOException A problem thrown while writing the content
	 */
	public static void sendRawContent ( HttpServletResponse response, Object content ) {
		try {
			response.getWriter().print(content.toString());
		} catch (IOException e) {
			log.warning(exceptionToText(e));
		}
	}

	/** Given a model, it serializes it to the response with the proper requested 
	 *  serialization format. The current supported formats are RDF-XML, TTL, and 
	 *  N-TRIPLE.
	 *
	 *  @param response The response object
	 *  @param model The model to dump
	 *  @param format The format 
	 * @throws IOException Problem thrown while writing to the response
	 */
	public static void sendRDFModel ( HttpServletResponse response, Model model, String format ) 
	throws IOException {
		if (format.equals("rdf") )  {
			contentAppXML(response);
			model.write(response.getOutputStream(),"RDF/XML-ABBREV");
		}
		else if ( format=="ttl" ) {
			contentTextPlain(response);
			model.write(response.getOutputStream(),"TTL");
		}
		else if ( format=="nt" ) {
			contentTextPlain(response);
			model.write(response.getOutputStream(),"N-TRIPLE");
		}
		else
			errorNotFound(response);
	}

	public static void sendContent ( HttpServletResponse response, JSONArray content, String format ) 
	throws IOException {
		if (format.equals("json") )  {
			contentAppJSON(response);
			sendRawContent(response,content);
		}
		else if (format.equals("xml") )  {
			try {
				contentAppXML(response);
				String xmlc = XML.toString(content,"meandre_item");
				sendRawContent(response, "<?xml version=\"1.0\" encoding=\"UTF-8\"?><meandre_response>");
				sendRawContent(response, xmlc);
				sendRawContent(response, "</meandre_response>");
			} catch (JSONException e) {
				log.warning(exceptionToText(e));
			}
		}
		else if (format.equals("html") )  {
			try {
				contentAppHTML(response);
				String xmlc = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><meandre_response>";
				xmlc += XML.toString(content,"meandre_item");
				xmlc += "</meandre_response>";
				StreamSource xmlSource = new StreamSource(new StringReader(xmlc));
		        StreamResult result = new StreamResult(response.getOutputStream());
		        xslTrans.transform(xmlSource, result);
			} catch (Exception e) {
				log.warning(exceptionToText(e));
			} 
		}
		else if (format.equals("txt") )  {
			contentTextPlain(response);
			try {
				sendRawContent(response, content.toString(4));
			} catch (JSONException e) {
				log.warning(exceptionToText(e));
			}
		}
		else
			errorNotFound(response);
	}

	/** Returns the map containing all the parameters and values pased 
	 * to the request.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String[]> extractTextPayloads ( HttpServletRequest request ) {
		Map<String,String[]> map = new HashMap<String,String[]>();
	
		Enumeration it = request.getParameterNames();
		while  ( it.hasMoreElements() ) {
			String name = it.nextElement().toString();
			String[] values = request.getParameterValues(name);
			map.put(name,values);
		}	
		return map;
	}
	
	
	/** Given an exception returns the text including its stack trace.
	 * 
	 * @param e The exception to convert
	 * @return The exception text including the stack trace
	 */
	public static String exceptionToText ( Exception e ) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos));
		e.printStackTrace(pw);
		return baos.toString();
	}
}

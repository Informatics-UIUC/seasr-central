/**
 * 
 */
package org.seasr.central.ws.restlets;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** This class is the base for a restfull usage of servlets. Servlets should not be
 * added or removed once the servlet is in use, since may introduce unexpecte behavior
 * on heavy load servlets.
 * 
 * @author xavier
 *
 */
public class RestfullExtensibleDispatcher extends HttpServlet {

	/** A default serial ID */
	private static final long serialVersionUID = 1L;
	
	/** The basic collection of rest servlets */
	private ArrayList<RestServlet> lstServlets = new ArrayList<RestServlet>(30);
	
	/** The list of compiled expressions */
	private ArrayList<Pattern> lstPatterns = new ArrayList<Pattern>(30);	
	
	/** The number of contained servlets */
	private int iNumRestlets = 0;

	/** Add the rest servlet to the list of servlets to process.
	 * 
	 * @param restlet The rest servlet to add.
	 */
	public void add ( RestServlet restlet ) {
		lstPatterns.add(Pattern.compile(restlet.getRestRegularExpression()));
		lstServlets.add(restlet);
		iNumRestlets++;
	}
	
	/** Remove the rest servlet from the list of servlets to process.
	 * 
	 * @param restlet The rest servlet to remove.
	 */
	public void remove ( RestServlet restlet ) {
		int idx = lstServlets.indexOf(restlet);
		if ( idx>=0 ) {
			lstServlets.remove(idx);
			lstPatterns.remove(idx);
			iNumRestlets--;
		}
	}
	
	/** Remove all the contained rest servlets.
	 * 
	 */
	public void clear () {
		lstServlets.clear();
		lstPatterns.clear();
		iNumRestlets = 0;
	}
	
	/** The number of restfull servlets contained in this dispatcher.
	 * 
	 * @return The number of restfull servlets contained in this dispatcher
	 */
	public int size () {
		return iNumRestlets;
	}
	
	/** Response to a get request.
	 * 
	 * @param req The request object
	 * @param resp The response object
	 */
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)  {
		dispatch("GET",req,resp);  
	}

	/** Response to a post request.
	 * 
	 * @param req The request object
	 * @param resp The response object
	 */
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)  {
		dispatch("POST",req,resp);     
	}

	/** Response to a put request.
	 * 
	 * @param req The request object
	 * @param resp The response object
	 */    
	protected void doPut(HttpServletRequest req, HttpServletResponse resp)  {
		dispatch("PUT",req,resp);
	}

	/** Response to a delete request.
	 * 
	 * @param req The request object
	 * @param resp The response object
	 */
	protected void doDelete(HttpServletRequest req, HttpServletResponse resp)  {
		dispatch("DELET",req,resp);
	}

	/** Response to a head request.
	 * 
	 * @param req The request object
	 * @param resp The response object
	 */
	protected void doHead(HttpServletRequest req, HttpServletResponse resp)  {
		dispatch("HEAD",req,resp);
	}

	/** Response to a options request.
	 * 
	 * @param req The request object
	 * @param resp The response object
	 */
	protected void doOptions(HttpServletRequest req, HttpServletResponse resp)  {
		dispatch("OPTIONS",req,resp);
	}

	/** Response to a trace request.
	 * 
	 * @param req The request object
	 * @param resp The response object
	 */
	protected void doTrace(HttpServletRequest req, HttpServletResponse resp)  {
		dispatch("TRACE",req,resp);
	}

	/** Dispatch the request.
	 * 
	 * @param method The method of the request
	 * @param req The request object
	 * @param resp The response
	 */
	private void dispatch(String method, HttpServletRequest req, HttpServletResponse resp) {
		Matcher m = null;
		String sUrl = req.getRequestURL().toString();
		
		for ( int idx = 0 ; idx<iNumRestlets ; idx++ ) {
			m = lstPatterns.get(idx).matcher(sUrl);
			if ( m.find() ) {
				// The specified pattern was matched
				// Extract the values and invoke the restlet
				int iGroups = m.groupCount();
				String [] values = new String[iGroups];
				for ( int i = 1 ; i<=iGroups ; i++)
					values[i-1] = m.group(i);
				if ( lstServlets.get(idx).process(req, resp, method, values) )
					break;				
			}
		}
	}
}

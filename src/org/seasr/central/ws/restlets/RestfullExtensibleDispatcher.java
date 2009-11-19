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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class is the base for a restfull usage of servlets. Servlets should not be
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
	private final ArrayList<RestServlet> lstServlets = new ArrayList<RestServlet>(30);

	/** The list of compiled expressions */
	private final ArrayList<Pattern> lstPatterns = new ArrayList<Pattern>(30);

	/** The number of contained servlets */
	private int iNumRestlets = 0;


	/**
	 * Add the rest servlet to the list of servlets to process.
	 *
	 * @param restlet The rest servlet to add.
	 */
	public void add ( RestServlet restlet ) {
		lstPatterns.add(Pattern.compile(restlet.getRestContextPathRegexp()));
		lstServlets.add(restlet);
		iNumRestlets++;
	}

	/**
	 * Remove the rest servlet from the list of servlets to process.
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

	/**
	 * Remove all the contained rest servlets.
	 *
	 */
	public void clear () {
		lstServlets.clear();
		lstPatterns.clear();
		iNumRestlets = 0;
	}

	/**
	 * The number of restful servlets contained in this dispatcher.
	 *
	 * @return The number of restful servlets contained in this dispatcher
	 */
	public int size () {
		return iNumRestlets;
	}

	/**
	 * Response to a get request.
	 *
	 * @param req The request object
	 * @param resp The response object
	 */
	@Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)  {
		dispatch("GET",req,resp);
	}

	/**
	 * Response to a post request.
	 *
	 * @param req The request object
	 * @param resp The response object
	 */
	@Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)  {
		dispatch("POST",req,resp);
	}

	/**
	 * Response to a put request.
	 *
	 * @param req The request object
	 * @param resp The response object
	 */
	@Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)  {
		dispatch("PUT",req,resp);
	}

	/**
	 * Response to a delete request.
	 *
	 * @param req The request object
	 * @param resp The response object
	 */
	@Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)  {
		dispatch("DELETE",req,resp);
	}

	/**
	 * Response to a head request.
	 *
	 * @param req The request object
	 * @param resp The response object
	 */
	@Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp)  {
		dispatch("HEAD",req,resp);
	}

	/**
	 * Response to a options request.
	 *
	 * @param req The request object
	 * @param resp The response object
	 */
	@Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)  {
		dispatch("OPTIONS",req,resp);
	}

	/**
	 * Response to a trace request.
	 *
	 * @param req The request object
	 * @param resp The response object
	 */
	@Override
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp)  {
		dispatch("TRACE",req,resp);
	}

	/**
	 * Dispatch the request.
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

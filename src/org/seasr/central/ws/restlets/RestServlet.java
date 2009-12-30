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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.seasr.central.storage.BackendStorageLink;
import org.seasr.central.ws.SC;


/**
 * This interface provides the basic interface to implement a REST based service.
 *
 * @author Xavier Llora
 */

public interface RestServlet {

	/**
	 * Set the SC central.
	 *
	 * @param sc SEASR Central reference
	 */
	public void setSCParent(SC sc);

	/**
	 * Set the back end store link.
	 *
	 * @param bsl The back end store link reference
	 */
	public void setBackendStoreLink(BackendStorageLink bsl);

	/**
	 * Return the regular expression used for the restful service.
	 *
	 * @return The string containing the regular expression
	 */
	public String getRestContextPathRegexp();

	/**
	 * Process a matching restful request.
	 *
	 * @param request The request object
	 * @param response The response object
	 * @param method The HTTP method used
	 * @param values The extracted values from the rest request
	 * @return True is the request is processed and no further attempts should be ma
	 */
	public boolean process(HttpServletRequest request, HttpServletResponse response, String method, String...values);

}

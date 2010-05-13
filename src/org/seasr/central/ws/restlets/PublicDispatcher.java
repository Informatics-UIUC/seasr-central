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

package org.seasr.central.ws.restlets;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.seasr.central.util.Tools.sendErrorInternalServerError;

/**
 * @author Boris Capitanu
 */
public class PublicDispatcher extends HttpServlet {
    protected Logger logger = null;

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
      * Response to a get request.
      *
      * @param req  The request object
      * @param resp The response object
      */
     @Override
     protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
         dispatch(req, resp);
     }

     /**
      * Response to a post request.
      *
      * @param req  The request object
      * @param resp The response object
      */
     @Override
     protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
         dispatch(req, resp);
     }

     /**
      * Response to a put request.
      *
      * @param req  The request object
      * @param resp The response object
      */
     @Override
     protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
         dispatch(req, resp);
     }

     /**
      * Response to a delete request.
      *
      * @param req  The request object
      * @param resp The response object
      */
     @Override
     protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
         dispatch(req, resp);
     }

     /**
      * Response to a head request.
      *
      * @param req  The request object
      * @param resp The response object
      */
     @Override
     protected void doHead(HttpServletRequest req, HttpServletResponse resp) {
         dispatch(req, resp);
     }

     /**
      * Response to a options request.
      *
      * @param req  The request object
      * @param resp The response object
      */
     @Override
     protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
         dispatch(req, resp);
     }

     /**
      * Response to a trace request.
      *
      * @param req  The request object
      * @param resp The response object
      */
     @Override
     protected void doTrace(HttpServletRequest req, HttpServletResponse resp) {
         dispatch(req, resp);
     }

    protected void dispatch(HttpServletRequest request, HttpServletResponse response) {
        try {
            request.getRequestDispatcher("/services" + request.getPathInfo()).forward(request, response);
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
        }
    }
}

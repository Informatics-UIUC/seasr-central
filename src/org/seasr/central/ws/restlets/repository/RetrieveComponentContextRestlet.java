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
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ComponentContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import static org.seasr.central.util.Tools.*;

/**
 * Restlet for retrieving component contexts
 *
 * @author Boris Capitanu
 */
public class RetrieveComponentContextRestlet extends AbstractBaseRestlet {

    @Override
    public Map<String, ContentType> getSupportedResponseTypes() {
        return null;
    }

    @Override
    public String getRestContextPathRegexp() {
        return "/repository/component/([a-f\\d]{8}(?:-[a-f\\d]{4}){3}-[a-f\\d]{12})/(\\d+)" +
                "/context/([a-f\\d]{32})(?:/.*)?$";
    }

    @Override
    public boolean process(HttpServletRequest request, HttpServletResponse response, String method, String... values) {
        // Check for GET
        if (!method.equalsIgnoreCase("GET")) return false;

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

        String contextId = values[2];

        try {
            ComponentContext context = bsl.getComponentContext(componentId, version, contextId);
            if (context == null) {
                sendErrorNotFound(response);
                return true;
            }
            
            InputStream contextStream = context.getDataStream();
            OutputStream responseStream = response.getOutputStream();

            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(context.getContentType());

            byte[] buffer = new byte[8192];
            int nRead;

            while ((nRead = contextStream.read(buffer)) > 0)
                responseStream.write(buffer, 0, nRead);
        }
        catch (BackendStoreException e) {
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
            return true;
        }
        catch (IOException e) {
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
            return true;
        }

        return true;
    }
}

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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.seasr.central.storage.BackendStorageLink;
import org.seasr.central.ws.SC;

import com.google.gdata.util.ContentType;

/**
 * This servlet implements a base abstract restlet.
 *
 * @author xavier
 *
 */
public abstract class AbstractBaseRestlet implements RestServlet {

	/** The parent SC object */
	protected SC sc;

	/** The back end storage link */
	protected BackendStorageLink bsl;

	private static final Pattern patternExtension;

	static {
	    patternExtension = Pattern.compile("\\.([a-z]+)$");
	}

	@Override
	public void setSCParent(SC sc) {
		this.sc = sc;
		this.bsl = sc.getBackendStorageLink();
	}

	@Override
	public void setBackendStoreLink(BackendStorageLink bsl) {
		this.bsl = bsl;
	}

	/**
     * Returns the supported 'Content-Type' values for this restlet
     * format is: <extension, ContentType>
     *
     * @return The supported content types for this restlet
     */
    public abstract Map<String, ContentType> getSupportedResponseTypes();

    /**
     * Returns a partial regular expression that matches the supported resource extensions
     * Example: \\.txt|\\.json|\\.xml   ---- matches .txt or .json or .xml
     *
     * @return A partial regular expression that matches the supported resource extensions
     */
    public String regexExtensionMatcher() {
        StringBuffer sb = new StringBuffer();
        for (String ext : getSupportedResponseTypes().keySet())
            sb.append("|\\." + ext);

        return sb.toString().substring(1);
    }

    /**
     * Returns the desired content type for this request
     *
     * @param request The request
     * @return The content type desired for the response, or null if couldn't determine
     */
	public ContentType getDesiredResponseContentType(HttpServletRequest request) {
	    ContentType ct = null;

	    Map<String, ContentType> allowedTypes = getSupportedResponseTypes();
        String reqUri = request.getRequestURI().toLowerCase();
        Matcher m = patternExtension.matcher(reqUri);
        if (m.find())
            ct = allowedTypes.get(m.group(1));
        else {
            String accept = request.getHeader("Accept");
            if (accept != null)
                ct = ContentType.getBestContentType(accept,
                        new ArrayList<ContentType>(allowedTypes.values()));
        }

        return ct;
	}
}

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

package org.seasr.central.storage;

import org.json.JSONException;
import org.json.JSONObject;
import org.seasr.central.storage.exceptions.BackendStoreException;

/**
 * Defines the SC application-specific error codes
 *
 * @author Boris Capitanu
 */
public enum SCError {

    INVALID_SCREEN_NAME     (100, "Invalid screen name: '%s'"),
    SCREEN_NAME_EXISTS      (101, "Screen name '%s' already exists"),
    USER_PROFILE_ERROR      (102, "Could not decode the user profile"),

    INCOMPLETE_REQUEST      (500, "Incomplete request / Expected parameter missing"),

    BACKEND_ERROR           (900, "Backend error");

    //--------------------------------------------------------------------------------------------

    private final int _code;
    private final String _message;

    SCError(int code, String message) {
        _code = code;
        _message = message;
    }

    /**
     * Returns the event code for this event
     *
     * @return The event code for this event
     */
    public int getErrorCode() {
        return _code;
    }

    public String getErrorMessage() {
        return _message;
    }

    public static String getErrorCodeKey() {
        return "sc_error_code";
    }

    public static String getErrorReasonKey() {
        return "sc_error_reason";
    }

    public static String getExceptionMsgKey() {
        return "sc_exception_msg";
    }

    public static JSONObject createErrorObj(SCError error, BackendStoreLink bsl, String... params) {
        return createErrorObj(error, null, bsl, params);
    }

    public static JSONObject createErrorObj(SCError error, Exception e, BackendStoreLink bsl, String... params) {
        try {
            String errMsg = bsl.getErrorMessage(error);
            if (errMsg == null) errMsg = "No error message found for error code: " + error.getErrorCode();

            if (params != null && params.length > 0)
                errMsg = String.format(errMsg, params);

            JSONObject joError = new JSONObject();
            joError.put(getErrorCodeKey(), error.getErrorCode());
            joError.put(getErrorReasonKey(), errMsg);
            if (e != null)
                joError.put(getExceptionMsgKey(), e.getMessage());

            return joError;
        }
        catch (BackendStoreException ex) {
            return null;
        }
        catch (JSONException ex) {
            return null;
        }
    }
}

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

package org.seasr.central.main;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPResult;
import org.seasr.central.util.Version;

/**
 * @author Boris Capitanu
 */
public class SC {
    public static final String DEFAULT_SERVER_CONFIG_FILE = "sc-server-conf.xml";
    public static final String DEFAULT_STORE_CONFIG_FILE = "sc-store-conf.xml";

    public static void main(String[] args) {
        JSAPResult jsapResult = parseCmdLineArgs(args);

        System.out.println("SC Version " + Version.getFullVersion());
    }

    public static JSAPResult parseCmdLineArgs(String[] args) {
        JSAP jsap = new JSAP();

        FlaggedOption serverConfOption = new FlaggedOption("serverconf")
                .setDefault(DEFAULT_SERVER_CONFIG_FILE)
                .setShortFlag('c');
        serverConfOption.setHelp("Specifies the server configuration file to use");

        FlaggedOption storeConfOption = new FlaggedOption("storeconf")
                .setDefault(DEFAULT_STORE_CONFIG_FILE)
                .setShortFlag('s');
        storeConfOption.setHelp("Specifies the backend store configuration file to use");
    }
}

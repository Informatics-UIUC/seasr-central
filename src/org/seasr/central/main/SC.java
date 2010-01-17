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

import com.martiansoftware.jsap.*;

/**
 * @author Boris Capitanu
 */
public class SC {
    public static final String DEFAULT_SERVER_CONFIG_FILE = "sc-server-config.xml";
    public static final String DEFAULT_STORE_CONFIG_FILE = "sc-store-config.xml";

    public SC() {

    }

    public SC(JSAPResult config) {

    }

    public void start() {

    }

    public void join() {

    }

    /**
     * SC entry point
     *
     * @param args The command line arguments
     * @throws Exception Thrown if an error occurs
     */
    public static void main(String[] args) throws Exception {
        SimpleJSAP jsap = getArgumentParser();
        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted())
            System.exit(1);

        SC sc = new SC(config);
        sc.start();
        sc.join();
    }

    /**
     * Creates a command line argument parser
     *
     * @return The parser
     * @throws JSAPException Thrown if a problem occurs
     */
    public static SimpleJSAP getArgumentParser() throws JSAPException {
        String generalHelp = "Starts the SEASR Central repository manager";

        Parameter serverConfOption = new FlaggedOption("server_configuration_file")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(JSAP.NOT_REQUIRED)
                .setDefault(DEFAULT_SERVER_CONFIG_FILE)
                .setShortFlag('c')
                .setLongFlag("serverconfig")
                .setHelp("Specifies the server configuration file to use");

        Parameter storeConfOption = new FlaggedOption("store_configuration_file")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(JSAP.NOT_REQUIRED)
                .setDefault(DEFAULT_STORE_CONFIG_FILE)
                .setShortFlag('s')
                .setLongFlag("storeconfig")
                .setHelp("Specifies the backend store configuration file to use");

        return new SimpleJSAP(SC.class.getSimpleName(), generalHelp,
                new Parameter[] { serverConfOption, storeConfOption });
    }
}

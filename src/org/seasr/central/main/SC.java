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
import org.mortbay.jetty.Connector;
import org.mortbay.xml.XmlConfiguration;
import org.seasr.central.exceptions.ServerConfigurationException;
import org.seasr.central.storage.BackendStoreLink;
import org.seasr.central.storage.db.properties.DBProperties;
import org.seasr.central.util.Version;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * SEASR Central main class
 *
 * @author Boris Capitanu
 */
public class SC {
    public static final String DEFAULT_SERVER_CONFIG_FILE = "sc-server-config.xml";
    public static final String DEFAULT_STORE_CONFIG_FILE = "sc-store-config.xml";

    private final Logger logger;
    private final String serverConfigFile;
    private final String storeConfigFile;

    private final Properties bslProps;
    private final BackendStoreLink bsl;
    private final SCServer server;


    public SC() throws ServerConfigurationException {
        this(DEFAULT_SERVER_CONFIG_FILE, DEFAULT_STORE_CONFIG_FILE);
    }

    public SC(JSAPResult config) throws ServerConfigurationException {
        this(config.getString("server_configuration_file"),
                config.getString("store_configuration_file"));
    }

    public SC(String serverConfigFile, String storeConfigFile) throws ServerConfigurationException {
        this.serverConfigFile = serverConfigFile;
        this.storeConfigFile = storeConfigFile;

        bslProps = new Properties();
        try {
            bslProps.loadFromXML(new FileInputStream(storeConfigFile));
        }
        catch (Exception e) {
            throw new ServerConfigurationException("Error loading configuration file: " + storeConfigFile, e);
        }

        // Retrieve the backend store link driver
        String bslClass = bslProps.getProperty(DBProperties.STORAGE_LINK, "").trim();
        if (bslClass.length() == 0)
            throw new ServerConfigurationException("Missing configuration entry for: " + DBProperties.STORAGE_LINK);

        try {
            // Attempt to instantiate the backend store link driver
            bsl = (BackendStoreLink) Class.forName(bslClass).newInstance();
        }
        catch (Exception e) {
            throw new ServerConfigurationException(
                    String.format("Cannot instantiate backend storage link class: %s", bslClass), e);
        }

        // Instantiate the main SC Jetty server...
        server = new SCServer(bsl);

        try {
            // ... and configure it using the specified configuration file
            new XmlConfiguration(new FileInputStream(serverConfigFile)).configure(server);
        }
        catch (IOException e) {
            throw new ServerConfigurationException("Error loading configuration file: " + serverConfigFile, e);
        }
        catch (Exception e) {
            throw new ServerConfigurationException("Error configuring Jetty server", e);
        }

        logger = server.getLogger();
    }

    /**
     * Starts the SC server
     *
     * @throws Exception Thrown if a problem occurs
     */
    public void start() throws Exception {
        logger.info(String.format("Starting SEASR Central API Server (version %s)", Version.getFullVersion()));
        if (Version.getBuildDate() != null)
            logger.fine("Server built on " + Version.getBuildDate());
        logger.fine("Using server configuration file: " + serverConfigFile);
        logger.fine("Using store configuration file: " + storeConfigFile);

        // Initialize the backend store link
        bsl.init(bslProps);

        // Start the SC Jetty server
        server.start();

        for (Connector connector : server.getConnectors())
            logger.info(String.format("Listening on %s", connector.getName()));
    }

    /**
     * Attempts to join the server thread to the main thread
     *
     * @throws InterruptedException Thrown if a problem occurs
     */
    public void join() throws InterruptedException {
        server.join();
    }

    /**
     * Stops the SC server
     *
     * @throws Exception Thrown if a problem occurs
     */
    public void stop() throws Exception {
        logger.info("Stopping SEASR Central API...");
        server.stop();
    }

    public boolean isStarted() {
        return server.isStarted();
    }

    public boolean isStopped() {
        return server.isStopped();
    }

    /**
     * Returns the backend store link
     *
     * @return The backend store link
     */
    public BackendStoreLink getBackendStoreLink() {
        return bsl;
    }

    /**
     * SC entry point
     *
     * @param args The command line arguments
     * @throws Exception Thrown if an error occurs
     */
    public static void main(String[] args) throws Exception {
        // Parse the command line
        SimpleJSAP jsap = getArgumentParser();
        JSAPResult config = jsap.parse(args);
        if (jsap.messagePrinted()) {
            if (!config.success())
                System.err.println(String.format("%nUsage: %s %s",
                        SC.class.getSimpleName(), jsap.getUsage()));
            System.exit(1);
        }

        if (!config.getBoolean("debug")) {
            // Turn off Jetty logging
            System.setProperty("org.mortbay.log.class", "org.seasr.central.util.EmptyLogger");

            // Turn off c3p0 logging
            System.setProperty("com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog");
            System.setProperty("com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "OFF");
        }

        // Start the server and join the main thread
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

        Parameter debugOption = new Switch("debug")
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("debug");

        return new SimpleJSAP(SC.class.getSimpleName(), generalHelp,
                new Parameter[] { serverConfOption, storeConfOption, debugOption });
    }
}

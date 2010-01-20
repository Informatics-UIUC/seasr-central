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
import org.mortbay.jetty.Server;
import org.mortbay.xml.XmlConfiguration;
import org.seasr.central.exceptions.ServerConfigurationException;
import org.seasr.central.storage.BackendStoreLink;
import org.seasr.central.storage.db.properties.DBProperties;
import org.seasr.central.util.SCLogFormatter;
import org.seasr.central.util.Version;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
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
    private final Server server;


    public SC() throws ServerConfigurationException {
        this(DEFAULT_SERVER_CONFIG_FILE, DEFAULT_STORE_CONFIG_FILE);
    }

    public SC(JSAPResult config) throws ServerConfigurationException {
        this(config.getString("server_configuration_file"),
                config.getString("store_configuration_file"));
    }

    public SC(String serverConfigFile, String storeConfigFile) throws ServerConfigurationException {
        logger = Logger.getLogger(SC.class.getName());
        logger.setUseParentHandlers(false);

        this.serverConfigFile = serverConfigFile;
        this.storeConfigFile = storeConfigFile;

        try {
            bslProps = new Properties();
            bslProps.loadFromXML(new FileInputStream(storeConfigFile));
        }
        catch (Exception e) {
            throw new ServerConfigurationException("Error loading configuration file: " + storeConfigFile, e);
        }

        String bslClass = bslProps.getProperty(DBProperties.STORAGE_LINK);
        if (bslClass == null)
            throw new ServerConfigurationException("Missing configuration entry for: " + DBProperties.STORAGE_LINK);

        try {
            bsl = (BackendStoreLink) Class.forName(bslClass).newInstance();
        }
        catch (Exception e) {
            throw new ServerConfigurationException(
                    String.format("Cannot instantiate backend storage link class: %s", bslClass), e);
        }

        server = new Server();

        try {
            new XmlConfiguration(new FileInputStream(serverConfigFile)).configure(server);
        }
        catch (IOException e) {
            throw new ServerConfigurationException("Error loading configuration file: " + serverConfigFile, e);
        }
        catch (Exception e) {
            throw new ServerConfigurationException("Error configuring Jetty server", e);
        }
    }

    /**
     * Starts the SC server
     *
     * @throws Exception Thrown if a problem occurs
     */
    public void start() throws Exception {
        logger.info(String.format("Starting SEASR Central API (version %s)", Version.getFullVersion()));
        if (Version.getBuildDate() != null)
            logger.fine("Server built on " + Version.getBuildDate());
        logger.fine("Using server configuration file: " + serverConfigFile);
        logger.fine("Using store configuration file: " + storeConfigFile);

        // Initialize the backend storage link
        bsl.init(bslProps);

        // Start the Jetty server
        server.start();
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
     * Returns the SC logger
     *
     * @return The SC logger
     */
    public Logger getLogger() {
        return logger;
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

        SC sc = new SC(config);

        // Set up logging
        String[] logDestinations;
        if ((logDestinations = config.getStringArray("log")) != null) {
            final Logger logger = sc.getLogger();
            final Level logLevel = Level.parse(config.getString("log_level"));

            HashSet<String> hsLogDest = new HashSet<String>(Arrays.asList(logDestinations));
            if (hsLogDest.contains("console")) {
                ConsoleHandler consoleHandler = new ConsoleHandler();
                consoleHandler.setFormatter(new SCLogFormatter());
                consoleHandler.setLevel(logLevel);
                logger.addHandler(consoleHandler);
                hsLogDest.remove("console");
            }

            if (hsLogDest.contains("file")) {
                FileHandler fileHandler = new FileHandler(config.getString("log_file"), true);
                fileHandler.setFormatter(new SCLogFormatter());
                fileHandler.setLevel(logLevel);
                logger.addHandler(fileHandler);
                hsLogDest.remove("file");
            }

            // If the user specified any more log destinations that we don't know about, warn him/her
            if (hsLogDest.size() > 0) {
                System.err.println("Warning: Ignoring unsupported log destination(s): " + hsLogDest);
            }

            logger.setLevel(logLevel);
        }

        // Start the server and join the main thread
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

        Parameter logConfOption = new FlaggedOption("log")
                .setStringParser(JSAP.STRING_PARSER)
                .setList(true)
                .setListSeparator(',')
                .setDefault("console")
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("log")
                .setHelp("Specifies the logging destination (can be: console and/or file)." +
                        "For file logging, set the log file with the --logfile <log_file> option");

        Parameter logFileConfOption = new FlaggedOption("log_file")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(JSAP.NOT_REQUIRED)
                .setDefault("scapi.log")
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("logfile")
                .setHelp("Specifies the log file to write logging information to");

        Parameter logLevelConfOption = new FlaggedOption("log_level")
                .setStringParser(JSAP.STRING_PARSER)
                .setRequired(JSAP.NOT_REQUIRED)
                .setDefault("INFO")
                .setShortFlag(JSAP.NO_SHORTFLAG)
                .setLongFlag("loglevel")
                .setHelp("Specifies the logging level (can be: SEVERE, WARNING, INFO, FINE, FINER, FINEST)");

        return new SimpleJSAP(SC.class.getSimpleName(), generalHelp,
                new Parameter[] { serverConfOption, storeConfOption, logConfOption, logFileConfOption, logLevelConfOption });
    }
}

package org.seasr.central.ws;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.Date;
import java.util.Properties;

import org.mortbay.jetty.Server;
import org.mortbay.xml.XmlConfiguration;
import org.seasr.central.storage.BackendStorageLink;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;

import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_LINK;
import static org.seasr.central.ws.restlets.Tools.log;

/** The main entry class that starts SEASR Central.
 * 
 * @author xavier
 *
 */
public class SC {

	/** The server versrion */
	private static final String VERSION = "0.1";
	
	/** The default store configuration file to assume */
	public static final String DEFAULT_STORE_CONFIG_FILE = "scs-store-sqlite.xml";

	/** The default jetty configuration file to assume */
	public static final String DEFAULT_JETTY_CONFIG_FILE = "jetty-server.xml";

	/** The default configuration directory  to assume */
	public static final String DEFAULT_CONFIG_FOLDER = "conf";

	/** The command line store configuration parameter name */
	public static final String CMDLINE_CONFIG_FOLDER = "conf";

	/** The command line store configuration parameter name */
	public static final String CMDLINE_STORE_CONFIG = "storeconfig";

	/** The command line jetty configuration parameter name */
	public static final String CMDLINE_JETTY_CONFIG = "jettyconfig";

	/** The instance configuration folder */
	private String sConfFolder;

	/** The jetty configuration file */
	private String sConfJetty;

	/** The store configuration file */
	private String sConfStr;

	/** The embedded Jetty server */
	private Server server;

	/** The intance to the backend storage link */
	private BackendStorageLink basd;

	/** Creates an instance of SEASR central with the default configuration.
	 * 
	 */
	public SC () {
		this.sConfFolder = DEFAULT_CONFIG_FOLDER;
		this.sConfStr = DEFAULT_STORE_CONFIG_FILE;
		this.sConfJetty = DEFAULT_JETTY_CONFIG_FILE;
		this.server = null;
	}

	/** Creates an instance of SEASR central with the provided paramaters.
	 * 
	 * @param sFolder The folder containing the configuration files
	 * @param sJetty The Jetty configuration file
	 * @param sWS The web service configuration file
	 * @param sStr The store configuration file
	 */
	public SC (String sFolder, String sJetty, String sWS, String sStr ) {
		this.sConfFolder = sFolder;
		this.sConfJetty = sJetty;
		this.sConfStr = sStr;
		this.server = null;
	}

	/** Sets the Jetty configuration file
	 * 
	 * @param sConfJetty the sConfJetty to set
	 */
	public void setConfigJetty(String sConfJetty) {
		this.sConfJetty = sConfJetty;
	}

	/** Gets the Jetty configuration file
	 * @return the sConfJetty
	 */
	public String getConfigJetty() {
		return sConfJetty;
	}

	/** Sets the store configuration file
	 * @param sConfStr the sConfStr to set
	 */
	public void setConfigStore(String sConfStr) {
		this.sConfStr = sConfStr;
	}

	/** Gets the store configuration file
	 * 
	 * @return the sConfStr
	 */
	public String getConfigStore() {
		return sConfStr;
	}

	/** Sets the configuration folder
	 * 
	 * @param sConfFolder the sConfFolder to set
	 */
	public void setConfigFolder(String sConfFolder) {
		this.sConfFolder = sConfFolder;
	}

	/** Gets the configuration folder
	 * 
	 * @return the sConfFolder
	 */
	public String getConfigFolder() {
		return sConfFolder;
	}

	/** Starts a server with the given information.
	 * 
	 * @throws Exception The server could not be started. The exception wraps
	 *                   the original exception.
	 */
	public void start () throws Exception {
		try {
			// Start the backend storage link
			Properties propStore = new Properties();
			propStore.loadFromXML(new FileInputStream(sConfFolder+File.separator+sConfStr));
			basd = (BackendStorageLink) Class.forName(propStore.getProperty(ORG_SEASR_CENTRAL_STORAGE_LINK)).newInstance();
			if ( !basd.init(propStore) )
				throw new Exception("Failed to instantiate the backend link");
				
			// Start the web server
			server = new Server();
			
			// Configuring the context
			XmlConfiguration configuration = new XmlConfiguration(new FileInputStream(sConfFolder+File.separator+sConfJetty));
			configuration.configure(server);
			
			// Configuring the contest
			//configuration = new XmlConfiguration(new FileInputStream(sConfFolder+File.separator+sConfWS)); 
			//ContextHandler context = (ContextHandler)configuration.configure();
			
			// Getting the server ready
			//server.setHandler(context);
			server.start();
		}
		catch (Exception e) {
			throw e;
		}

	}

	/** Attempts to join the server, so the main thread gets locked.
	 * 
	 * @throws InterruptedException The calling thread got interrupted while joining the server.
	 */
	public void join() throws InterruptedException {
		server.join();
	}

	/** Tries to stop the server.
	 * 
	 * @throws Exception Will be thrown if it failed to do so.
	 */
	public void stop () throws Exception {
		try {
			server.stop();
		}
		catch ( Exception e ) {
			throw e;
		}
		finally {
			basd.close();
		}
	}

	/** Returns the status of a server the server.
	 * 
	 * @return True if the server finished the starting process; false otherwise.
	 * @throws Exception Will be thrown if it could not check the status.
	 */
	public boolean isStarted () throws Exception {
		return server.isStarted();
	}

	/** Returns the status of a server the server.
	 * 
	 * @return True if the server finished the stopping process; false otherwise.
	 * @throws Exception Will be thrown if it could not check the status.
	 */
	public boolean isStopped () throws Exception {
		return server.isStopped();
	}
	
	/** Returns the back end storage link use by this SEASR central instance.
	 * 
	 * @return The back end storage link
	 */
	public BackendStorageLink getBackendStorageLink () {
		return this.basd;
	}

	/** Process a command line to extract parameters out if it. 
	 * 
	 * @param args The command line arguments
	 * @return The parser results 
	 * @throws JSAPException The command line could not be properly parsed
	 */
	public static JSAPResult processCommandLine ( String...args ) 
	throws JSAPException {
		JSAP jsap = new JSAP();

		FlaggedOption confOption = new FlaggedOption(CMDLINE_CONFIG_FOLDER)
		.setStringParser(JSAP.STRING_PARSER)
		.setDefault(DEFAULT_CONFIG_FOLDER) 
		.setRequired(true) 
		.setShortFlag('c') 
		.setLongFlag(CMDLINE_CONFIG_FOLDER);

		FlaggedOption jettyOption = new FlaggedOption(CMDLINE_JETTY_CONFIG)
		.setStringParser(JSAP.STRING_PARSER)
		.setDefault(DEFAULT_JETTY_CONFIG_FILE) 
		.setRequired(true) 
		.setShortFlag('j') 
		.setLongFlag(CMDLINE_JETTY_CONFIG);

		FlaggedOption storeOption = new FlaggedOption(CMDLINE_STORE_CONFIG)
		.setStringParser(JSAP.STRING_PARSER)
		.setDefault(DEFAULT_STORE_CONFIG_FILE) 
		.setRequired(true) 
		.setShortFlag('s') 
		.setLongFlag(CMDLINE_STORE_CONFIG);

		jsap.registerParameter(confOption);
		jsap.registerParameter(jettyOption);
		jsap.registerParameter(storeOption);

		JSAPResult config = jsap.parse(args);    

		if (!config.success()) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			ps.println("Usage: java "+ SC.class.getName());
			ps.println("                "+ jsap.getUsage());
			ps.println();
			throw new JSAPException(baos.toString());
		}

		return config;
	}

	public static void main ( String...args ) {
		try {
			JSAPResult config = processCommandLine(args);

			log.info("Starting SEASR Central API ("+VERSION+"-vcli) on "+new Date());
			log.info("Is server successfully configured: "+config.success());
			log.info("Configuration folder: "+config.getString(CMDLINE_CONFIG_FOLDER));
			log.info("Jetty configuration file: "+config.getString(CMDLINE_JETTY_CONFIG));
			log.info("Storage link configuration file: "+config.getString(CMDLINE_STORE_CONFIG));

			SC sc = new SC();
			sc.start();
			sc.join();
		}
		catch (JSAPException e) {
			System.err.println(e.getMessage());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}



}

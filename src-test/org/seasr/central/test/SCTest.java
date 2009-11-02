package org.seasr.central.test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.seasr.central.ws.SC;

import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;

/** This class test the basic SC loader class.
 * 
 * @author xavier
 *
 */
public class SCTest {

	/** Test the command line parser.
	 * 
	 */
	@Test
	public void testCommandLineParser () {
		try {
			// Check defaults 
			JSAPResult config = SC.processCommandLine(new String[]{});
			checkConfigParameters(config);
			assertEquals(SC.DEFAULT_CONFIG_FOLDER,config.getString(SC.CMDLINE_CONFIG_FOLDER));
			assertEquals(SC.DEFAULT_STORE_CONFIG_FILE,config.getString(SC.CMDLINE_STORE_CONFIG));

			// Check correct changes short notation
			config = SC.processCommandLine(new String[]{"-c","c/c","-s","s/s"});
			checkConfigParameters(config);
			assertEquals("c/c",config.getString(SC.CMDLINE_CONFIG_FOLDER));
			assertEquals("s/s",config.getString(SC.CMDLINE_STORE_CONFIG));


			// Check correct changes long notation
			config = SC.processCommandLine(new String[]{"--"+SC.CMDLINE_CONFIG_FOLDER,"c/c","--"+SC.CMDLINE_STORE_CONFIG,"s/s"});
			checkConfigParameters(config);
			assertEquals("c/c",config.getString(SC.CMDLINE_CONFIG_FOLDER));
			assertEquals("s/s",config.getString(SC.CMDLINE_STORE_CONFIG));

			// Check failures
			try {
				config = SC.processCommandLine(new String[]{"-"+SC.CMDLINE_STORE_CONFIG,"s/s"});
				fail("The provided command line should have never been parsed");
			} catch (JSAPException e) {
				// OK
			}
			
			// Check failures
			try {
				config = SC.processCommandLine(new String[]{"-x","s/s","-w","w/w"});
				fail("The provided command line should have never been parsed");
			} catch (JSAPException e) {
				// OK
			}

		} catch (JSAPException e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		}

	}

	/** Checks that a returned configuration object contains all the required parameters.
	 * 
	 * @param config The configuration object to check
	 */
	private void checkConfigParameters(JSAPResult config) {
		if ( !config.contains(SC.CMDLINE_CONFIG_FOLDER) )    fail(SC.CMDLINE_CONFIG_FOLDER+" missing");
		if ( !config.contains(SC.CMDLINE_JETTY_CONFIG) )    fail(SC.CMDLINE_JETTY_CONFIG+" missing");
		if ( !config.contains(SC.CMDLINE_STORE_CONFIG) ) fail(SC.CMDLINE_STORE_CONFIG+" missing");
	}
	
	/** Checks that the instantiation, setters, and getters are all fine.
	 * 
	 */
	@Test
	public void testInstantiation () {
		SC sc = new SC();
		assertEquals(SC.DEFAULT_CONFIG_FOLDER, sc.getConfigFolder());
		assertEquals(SC.DEFAULT_JETTY_CONFIG_FILE, sc.getConfigJetty());
		assertEquals(SC.DEFAULT_STORE_CONFIG_FILE, sc.getConfigStore());
		
		sc = new SC("a","b","c","d");
		assertEquals("a", sc.getConfigFolder());
		assertEquals("b", sc.getConfigJetty());
		assertEquals("d", sc.getConfigStore());
		
		sc = new SC();
		sc.setConfigFolder("a");
		sc.setConfigStore("c");
		sc.setConfigJetty("d");
		assertEquals("a", sc.getConfigFolder());
		assertEquals("c", sc.getConfigStore());
		assertEquals("d", sc.getConfigJetty());
	}
	
	/** Test the instantiation of a server.
	 * 
	 */
	@Test
	public void testServerInstantiation() {
		SC sc = new SC();
		
		try {
			sc.start();
			while ( !sc.isStarted() ) {
				Thread.sleep(100);
			}
			sc.stop();
			while ( !sc.isStopped() ) {
				Thread.sleep(100);
			}
		} catch (Exception e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		}
		
	}
}

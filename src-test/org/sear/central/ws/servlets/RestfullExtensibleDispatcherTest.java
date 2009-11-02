/**
 * 
 */
package org.sear.central.ws.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.seasr.central.storage.BackendStorageLink;
import org.seasr.central.ws.SC;
import org.seasr.central.ws.restlets.RestServlet;
import org.seasr.central.ws.restlets.RestfullExtensibleDispatcher;


/** Test class for the basic resfull dispatcher
 * 
 * @author xavier
 *
 */
public class RestfullExtensibleDispatcherTest {
	
	/** The test server port to bind to */
	private static final int TEST_SERVER_PORT = 9090;
	
	/** The fixture server */
	private Server server;
	
	/** A basic dummy test rest servlet */
	private class TestRestlet implements RestServlet {

		/** The pattern to match */
		private final static String PATTERN = "/hello/([A-Za-z /]+)/";
		
		/** Returns the pattern to match.
		 * 
		 * @return The regular expression to use
		 */
		@Override
		public String getRestRegularExpression() {
			return PATTERN;
		}

		/** Process the provided request.
		 * 
		 * @param request The original request object
		 * @param response The response object
		 * @param method The method used to issue the request
		 * @param values The matched values
		 */
		@Override
		public boolean process(HttpServletRequest request,
				HttpServletResponse response, String method, String... values)  {
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType("text/plain");
			
			try {
				PrintWriter pw = response.getWriter();
				pw.print("Hello to|");
				for ( String s:(values[0].split("/")) )
					pw.print(s+"|");
				pw.println();
				return true;
			} catch (IOException e) {
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				e.printStackTrace(new PrintStream(baos));
				fail(baos.toString());
				return false;
			}
			
		}

		@Override
		public void setSCParent(SC sc) {
			
		}

		@Override
		public void setBackendStoreLink(BackendStorageLink bsl) {
			// TODO Auto-generated method stub
			
		}
		
	}

	/** Sets up the fixture starting a test server
	 *  
	 */
	@Before
	public void setUpFixture () {
		server = new Server(TEST_SERVER_PORT);
		Context context = new Context(server,"/",Context.NO_SESSIONS);
		RestfullExtensibleDispatcher red = new RestfullExtensibleDispatcher();
		red.add(new TestRestlet());		
		context.addServlet(new ServletHolder((Servlet)red), "/*");
		try {
			server.start();
		} catch (Exception e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		}
	}
	
	/** Tears down the fixture shutting down the test server.
	 *
	 */
	@After
	public void tearDownFixture () {
		try {
			server.stop();
			server.destroy();
			server = null;
		} catch (Exception e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
			server = null;
		}
	}
	
	/** Runs a simple test against the basic servlet.
	 * 
	 */
	@Test
	public void basicRestTest () {
		try {
			String sUrl = "http://localhost:"+TEST_SERVER_PORT+"/hello/john/peter/any/mary/";
			String response = getRequest(sUrl).trim();
			String [] values = response.split("\\|");
			assertEquals(5,values.length);
			assertEquals("Hello to",values[0]);
			assertEquals("john",values[1]);
			assertEquals("peter",values[2]);
			assertEquals("any",values[3]);
			assertEquals("mary",values[4]);
		} catch (MalformedURLException e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		} catch (IOException e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		}
		
	}

	/** Makes a simple get request to the provided url and append all the content together.
	 * 
	 * @param sUrl The url
	 * @return The retrieved content
	 * @throws MalformedURLException Wrong url format
	 * @throws IOException The connection failed
	 */
	private String getRequest(String sUrl) throws MalformedURLException,
			IOException {
		URL url = new URL(sUrl);
		LineNumberReader lnr = new LineNumberReader(new InputStreamReader(url.openStream()));
		String line;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		while ( (line=lnr.readLine())!=null )
			pw.println(line);
		lnr.close();
		pw.close();
		return baos.toString();
	}
	
//	public static void main ( String...args ) {
//		String PATTERN = "/hello/([A-Za-z /]+)/";
//		Pattern p = Pattern.compile(PATTERN);
//		Matcher m = p.matcher("/hello/john/peter/any/mary/");
//		System.out.println(m.find());
//		System.out.println(m.groupCount());
//		for ( int i=0 ; i<=m.groupCount() ; i++ ) 
//			System.out.println(m.group(i));
//	}
}

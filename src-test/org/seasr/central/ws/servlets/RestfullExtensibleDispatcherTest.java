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

package org.seasr.central.ws.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;

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
import org.seasr.central.ws.restlets.RestfulExtensibleDispatcher;
import org.seasr.meandre.support.generic.io.HttpUtils;

import com.google.gdata.util.ContentType;

/**
 * Test class for the basic restful dispatcher
 *
 * @author Xavier Llora
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

		/**
		 * Returns the pattern to match.
		 *
		 * @return The regular expression to use
		 */
		@Override
		public String getRestContextPathRegexp() {
			return PATTERN;
		}

		/**
		 * Process the provided request.
		 *
		 * @param request The original request object
		 * @param response The response object
		 * @param method The method used to issue the request
		 * @param values The matched values
		 */
		@Override
		public boolean process(HttpServletRequest request, HttpServletResponse response, String method, String... values)  {
			response.setContentType(ContentType.TEXT_PLAIN.toString());
			response.setStatus(HttpServletResponse.SC_OK);

			try {
				PrintWriter pw = response.getWriter();
				pw.print("Hello to|");
				for (String s : (values[0].split("/")))
					pw.print(s + "|");
				pw.println();

				return true;
			}
			catch (IOException e) {
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
		}
	}

	/**
	 * Sets up the fixture starting a test server
	 */
	@Before
	public void setUpFixture() {
		server = new Server(TEST_SERVER_PORT);
		Context context = new Context(server, "/", Context.NO_SESSIONS);
		RestfulExtensibleDispatcher red = new RestfulExtensibleDispatcher();
		red.add(new TestRestlet());
		context.addServlet(new ServletHolder(red), "/*");

		try {
			server.start();
		}
		catch (Exception e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		}
	}

	/**
	 * Tears down the fixture shutting down the test server.
	 */
	@After
	public void tearDownFixture() {
		try {
			server.stop();
			server.destroy();
			server = null;
		}
		catch (Exception e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
			server = null;
		}
	}

	/**
	 * Runs a simple test against the basic servlet.
	 */
	@Test
	public void basicRestTest() {
		try {
			String sUrl = "http://localhost:" + TEST_SERVER_PORT + "/hello/john/peter/any/mary/";
			String response = HttpUtils.doGET(sUrl, null).trim();
			String [] values = response.split("\\|");
			assertEquals(5, values.length);
			assertEquals("Hello to", values[0]);
			assertEquals("john", values[1]);
			assertEquals("peter", values[2]);
			assertEquals("any", values[3]);
			assertEquals("mary", values[4]);
		}
		catch (MalformedURLException e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		}
		catch (IOException e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		}
	}
}

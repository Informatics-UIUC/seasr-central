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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_URL;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_LINK;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.seasr.central.storage.BackendStorageLink;
import org.seasr.central.test.storage.BackendStorageLinkTest;
import org.seasr.central.ws.restlets.RestServlet;
import org.seasr.central.ws.restlets.RestfulExtensibleDispatcher;
import org.seasr.central.ws.restlets.Tools.OperationResult;
import org.seasr.central.ws.restlets.user.AddUserRestlet;
import org.seasr.central.ws.restlets.user.DeleteUserRestlet;
import org.seasr.central.ws.restlets.user.ListUsersRestlet;
import org.seasr.central.ws.restlets.user.UserInfoRestlet;
import org.seasr.meandre.support.generic.io.HttpUtils;

/**
 * Test class for the basic restful dispatcher
 *
 * @author Xavier Llora
 * @author Boris Capitanu
 */

public class UserRestletTest {

	/** The test server port to bind to */
	private static final int TEST_SERVER_PORT = 9090;

	/** The fixture server */
	private Server server;

	/** The back end storage link */
	private BackendStorageLink bsl;

	/** The property file containing the connection information */
	private static final String DB_PROPERTY_FILE = "conf" + File.separator + "scs-store-sqlite.xml";

	private static File dbFile;

	@BeforeClass
	public static void setUp() {
	    try {
            dbFile = File.createTempFile("SCStore-test", ".sqlite");
            dbFile.deleteOnExit();
        }
        catch (IOException e) {
            fail("Failed to create test database file. Reason: " + e.getMessage());
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
		RestServlet[] rsa = {
				new AddUserRestlet(),
				new DeleteUserRestlet(),
				new ListUsersRestlet(),
				new UserInfoRestlet()
		};

		Properties props = new Properties();
		try {
			props.loadFromXML(new FileInputStream(DB_PROPERTY_FILE));
			props.setProperty(ORG_SEASR_CENTRAL_STORAGE_DB_URL,
			        "jdbc:sqlite:" + dbFile.getAbsolutePath());

			bsl = (BackendStorageLink) Class.forName(props.getProperty(ORG_SEASR_CENTRAL_STORAGE_LINK)).newInstance();
			bsl.init(props);

			for (RestServlet rs : rsa) {
				rs.setBackendStoreLink(bsl);
				red.add(rs);
			}

			context.addServlet(new ServletHolder(red), "/*");
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
	public void basicUserListTest() {
		try {
			String sUrl = "http://localhost:" + TEST_SERVER_PORT + "/services/users/";
			String response = HttpUtils.doGET(sUrl, "application/json").trim();

			JSONObject joResp = new JSONObject(response);
			JSONArray jaUsers = joResp.getJSONArray(OperationResult.SUCCESS.name());  // get the "success" entries
			boolean found = false;

			for (int i = 0, iMax = jaUsers.length(); !found && i < iMax; i++)
				if (jaUsers.getJSONObject(i).get("screen_name").equals("admin"))
					found = true;

			assertTrue(found);
		}
		catch (Exception e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		}
	}

	/**
	 * Runs a simple add test against the basic servlet.
	 */
	@Test
	public void basicUserAddTest() {
		try {
			String screenName = BackendStorageLinkTest.generateTestUserScreenName();
			String password = "sekret";
			String profile = "{\"test\": \"true\"}";
			String sUrl = "http://localhost:" + TEST_SERVER_PORT + "/services/users/";

			Properties props = new Properties();
			props.put("screen_name", screenName);
			props.put("password", password);
			props.put("profile", profile);

			String response = HttpUtils.doPOST(sUrl, "application/json", props).trim();
			JSONObject joResp = new JSONObject(response);
			JSONObject jo = joResp.getJSONArray(OperationResult.SUCCESS.name()).getJSONObject(0);

			assertTrue(jo.has("uuid"));
			assertTrue(jo.has("screen_name"));
			assertEquals(screenName, jo.getString("screen_name"));
			assertTrue(jo.has("created_at"));
			assertTrue(jo.has("profile"));
		}
		catch (Exception e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		}
	}

	@Test
	public void basicUserInfoTest() {
	    try {
	        String screenName = BackendStorageLinkTest.generateTestUserScreenName();
	        String password = "sekret";
	        String profile = "{\"test\": \"true\"}";
	        String sUrlAddUser = "http://localhost:" + TEST_SERVER_PORT + "/services/users/";

	        Properties props = new Properties();
	        props.put("screen_name", screenName);
	        props.put("password", password);
	        props.put("profile", profile);

	        String responseAdd = HttpUtils.doPOST(sUrlAddUser, "application/json", props).trim();
	        JSONObject joResp = new JSONObject(responseAdd);
	        JSONObject joAdd = joResp.getJSONArray(OperationResult.SUCCESS.name()).getJSONObject(0);
	        assertTrue(joAdd.has("uuid"));
	        assertTrue(joAdd.has("screen_name"));
	        assertEquals(screenName, joAdd.getString("screen_name"));
	        assertTrue(joAdd.has("created_at"));
	        assertTrue(joAdd.has("profile"));

	        String sUrlInfoUser =
	            String.format("http://localhost:%d/services/users/%s.json", TEST_SERVER_PORT, joAdd.get("screen_name"));
	        String responseInfo = HttpUtils.doGET(sUrlInfoUser, null).trim();
	        JSONObject joInfoResp = new JSONObject(responseInfo);
	        JSONObject jo = joInfoResp.getJSONArray(OperationResult.SUCCESS.name()).getJSONObject(0);
	        assertTrue(jo.has("uuid"));
	        assertEquals(joAdd.getString("uuid"), jo.getString("uuid"));
	        assertTrue(jo.has("screen_name"));
	        assertEquals(screenName, joAdd.getString("screen_name"));
	        assertTrue(jo.has("created_at"));
	        assertTrue(jo.has("profile"));

	    }
	    catch (Exception e) {
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        e.printStackTrace(new PrintStream(baos));
	        fail(baos.toString());
	    }
	}

	/**
	 * Runs a simple delete test against the basic servlet using the screen name.
	 */
	@Test
	public void basicUserDeleteUsingScreenNameTest() {
		try {
			String screenName = BackendStorageLinkTest.generateTestUserScreenName();
			String password = "sekret";
			String profile = "{\"test\": \"true\"}";
			String sUrlAddUser = "http://localhost:" + TEST_SERVER_PORT + "/services/users/";

			Properties props = new Properties();
			props.put("screen_name", screenName);
			props.put("password", password);
			props.put("profile", profile);

			String responseAdd = HttpUtils.doPOST(sUrlAddUser, "application/json", props).trim();
			JSONObject joResp = new JSONObject(responseAdd);
            JSONObject joAdd = joResp.getJSONArray(OperationResult.SUCCESS.name()).getJSONObject(0);
			assertTrue(joAdd.has("uuid"));
			assertTrue(joAdd.has("screen_name"));
			assertEquals(screenName, joAdd.getString("screen_name"));
			assertTrue(joAdd.has("created_at"));
			assertTrue(joAdd.has("profile"));

			String sUrlDeleteUser =
			    String.format("http://localhost:%d/services/users/%s.json", TEST_SERVER_PORT, joAdd.get("screen_name"));
			String responseDelete = HttpUtils.doDELETE(sUrlDeleteUser, null).trim();
			JSONObject joDelResp = new JSONObject(responseDelete);
			JSONObject jo = joDelResp.getJSONArray(OperationResult.SUCCESS.name()).getJSONObject(0);
			assertTrue(jo.has("uuid"));
			assertEquals(joAdd.getString("uuid"), jo.getString("uuid"));
			assertTrue(jo.has("screen_name"));
			assertEquals(screenName, jo.getString("screen_name"));

		}
		catch (Exception e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		}
	}

	/**
	 * Runs a simple delete test against the basic servlet using the uuid.
	 */
	@Test
	public void basicUserDeleteUsingUUIDTest() {
		try {
		    String screenName = BackendStorageLinkTest.generateTestUserScreenName();
		    String password = "sekret";
		    String profile = "{\"test\": \"true\"}";
		    String sUrlAddUser = "http://localhost:" + TEST_SERVER_PORT + "/services/users/";

		    Properties props = new Properties();
		    props.put("screen_name", screenName);
		    props.put("password", password);
		    props.put("profile", profile);

		    String responseAdd = HttpUtils.doPOST(sUrlAddUser, "application/json", props).trim();
		    JSONObject joResp = new JSONObject(responseAdd);
		    JSONObject joAdd = joResp.getJSONArray(OperationResult.SUCCESS.name()).getJSONObject(0);
		    assertTrue(joAdd.has("uuid"));
		    assertTrue(joAdd.has("screen_name"));
		    assertEquals(screenName, joAdd.getString("screen_name"));
		    assertTrue(joAdd.has("created_at"));
		    assertTrue(joAdd.has("profile"));

		    String sUrlDeleteUser =
		        String.format("http://localhost:%d/services/users/%s.json", TEST_SERVER_PORT, joAdd.get("uuid"));
		    String responseDelete = HttpUtils.doDELETE(sUrlDeleteUser, null).trim();
		    JSONObject joDelResp = new JSONObject(responseDelete);
		    JSONObject jo = joDelResp.getJSONArray(OperationResult.SUCCESS.name()).getJSONObject(0);
		    assertTrue(jo.has("uuid"));
		    assertEquals(joAdd.getString("uuid"), jo.getString("uuid"));
		    assertTrue(jo.has("screen_name"));
            assertEquals(screenName, jo.getString("screen_name"));

		}
		catch (Exception e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		}
	}
}

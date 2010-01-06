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
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_REPOSITORY_LOCATION;
import static org.seasr.central.ws.restlets.Tools.getExceptionTrace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.Map.Entry;

import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mortbay.jetty.NCSARequestLog;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.RequestLogHandler;
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
import org.seasr.central.ws.restlets.user.UploadComponentRestlet;
import org.seasr.central.ws.restlets.user.UploadFlowRestlet;
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

	/** The database file to use for this test suite */
	private static File dbFile;

	/** The location of the SC repository */
	private File repositoryFolder;


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
				new UserInfoRestlet(),
				new UploadComponentRestlet(),
				new UploadFlowRestlet()
		};

		Properties props = new Properties();
		try {
	        // Get a temporary folder location to store the SC repository
	        repositoryFolder = File.createTempFile("repository-test", "");
	        repositoryFolder.delete();
	        repositoryFolder.deleteOnExit();

	        // Load the DB configuration file and override properties to accommodate unit tests
		    props.loadFromXML(new FileInputStream(DB_PROPERTY_FILE));
			props.setProperty(ORG_SEASR_CENTRAL_STORAGE_DB_URL, "jdbc:sqlite:" + dbFile.getAbsolutePath());
			props.setProperty(ORG_SEASR_CENTRAL_STORAGE_REPOSITORY_LOCATION, repositoryFolder.getAbsolutePath());

			bsl = (BackendStorageLink) Class.forName(props.getProperty(ORG_SEASR_CENTRAL_STORAGE_LINK)).newInstance();
			bsl.init(props);

			for (RestServlet rs : rsa) {
				rs.setBackendStoreLink(bsl);
				red.add(rs);
			}

			context.addServlet(new ServletHolder(red), "/*");

			// Set up Jetty request logging (to aid with debugging)
			NCSARequestLog requestLog = new NCSARequestLog();
			requestLog.setFilename("logs/access.log");
			requestLog.setAppend(true);
			requestLog.setExtended(false);

			RequestLogHandler logHandler = new RequestLogHandler();
			logHandler.setRequestLog(requestLog);
			logHandler.setServer(server);

			context.addHandler(logHandler);

			server.start();
		}
		catch (Exception e) {
			fail(getExceptionTrace(e));
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
		    server = null;
			fail(getExceptionTrace(e));
		}
		finally {
		    try {
                FileUtils.deleteDirectory(repositoryFolder);
            }
            catch (IOException e) {
            }
		}
	}

	/**
	 * Runs a simple test against the basic servlet.
	 */
	@Test
	public void basicUserListTest() {
		try {
			JSONObject joResp = listUsers();
			JSONArray jaUsers = joResp.getJSONArray(OperationResult.SUCCESS.name());  // get the "success" entries
			boolean found = false;

			// Look for the 'admin' user
			for (int i = 0, iMax = jaUsers.length(); !found && i < iMax; i++)
				if (jaUsers.getJSONObject(i).get("screen_name").equals("admin"))
					found = true;

			assertTrue(found);

			// Get the current user count
			int nUsers = jaUsers.length();

			// Create a new user and make sure it succeeds
			assertEquals(0, createUser(BackendStorageLinkTest.generateTestUserScreenName(), "sekret", new JSONObject())
			        .getJSONArray(OperationResult.FAILURE.name()).length());

			// Make sure listUsers returns an updated user count
			assertEquals(nUsers + 1, listUsers().getJSONArray(OperationResult.SUCCESS.name()).length());
		}
		catch (Exception e) {
            fail(getExceptionTrace(e));
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
			JSONObject joProfile = new JSONObject().put("test", true);

			// Create a test user and make sure there were no errors
			JSONObject joResult = createUser(screenName, password, joProfile);
			assertEquals(0, joResult.getJSONArray(OperationResult.FAILURE.name()).length());

            JSONArray jaSuccess = joResult.getJSONArray(OperationResult.SUCCESS.name());
            assertEquals(1, jaSuccess.length());

            JSONObject joAddedUser = jaSuccess.getJSONObject(0);

			assertTrue(joAddedUser.has("uuid"));
			assertTrue(joAddedUser.has("screen_name"));
			assertEquals(screenName, joAddedUser.getString("screen_name"));
			assertTrue(joAddedUser.has("created_at"));
			assertTrue(joAddedUser.has("profile"));
		}
		catch (Exception e) {
            fail(getExceptionTrace(e));
		}
	}

	@Test
	public void basicUserInfoTest() {
	    try {
	        String screenName = BackendStorageLinkTest.generateTestUserScreenName();
	        String password = "sekret";
	        JSONObject joProfile = new JSONObject().put("test", true);

            // Create a test user and make sure there were no errors
            JSONObject joResult = createUser(screenName, password, joProfile);
            assertEquals(0, joResult.getJSONArray(OperationResult.FAILURE.name()).length());
	        JSONObject joTestUser = joResult.getJSONArray(OperationResult.SUCCESS.name()).getJSONObject(0);

	        // Get the user info for the test user and make sure there were no errors
	        joResult = getUserInfo(screenName);
            assertEquals(0, joResult.getJSONArray(OperationResult.FAILURE.name()).length());
	        JSONArray jaSuccess = joResult.getJSONArray(OperationResult.SUCCESS.name());
	        assertEquals(1, jaSuccess.length());
            JSONObject joUserInfo = jaSuccess.getJSONObject(0);

	        assertTrue(joUserInfo.has("uuid"));
	        assertEquals(joTestUser.getString("uuid"), joUserInfo.getString("uuid"));
	        assertTrue(joUserInfo.has("screen_name"));
	        assertEquals(screenName, joUserInfo.getString("screen_name"));
	        assertTrue(joUserInfo.has("created_at"));
	        assertTrue(joUserInfo.has("profile"));
	        assertTrue(joUserInfo.getJSONObject("profile").has("test"));
	    }
	    catch (Exception e) {
	        fail(getExceptionTrace(e));
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
	        JSONObject joProfile = new JSONObject().put("test", true);

            // Create a test user and make sure there were no errors
            JSONObject joResult = createUser(screenName, password, joProfile);
            assertEquals(0, joResult.getJSONArray(OperationResult.FAILURE.name()).length());
            JSONObject joTestUser = joResult.getJSONArray(OperationResult.SUCCESS.name()).getJSONObject(0);

            // Delete the test user and make sure there were no errors
            joResult = deleteUser(screenName);
            assertEquals(0, joResult.getJSONArray(OperationResult.FAILURE.name()).length());
			JSONArray jaSuccess = joResult.getJSONArray(OperationResult.SUCCESS.name());
			assertEquals(1, jaSuccess.length());
            JSONObject joDeletedUser = jaSuccess.getJSONObject(0);

			assertTrue(joDeletedUser.has("uuid"));
			assertEquals(joTestUser.getString("uuid"), joDeletedUser.getString("uuid"));
			assertTrue(joDeletedUser.has("screen_name"));
			assertEquals(screenName, joDeletedUser.getString("screen_name"));

			// Try to delete a non-existent user and make sure this fails
			try {
                deleteUser("NoSuchUser");
            }
            catch (FileNotFoundException e) {
                // Do nothing, this is ok and expected
            }
            catch (Exception e) {
                fail(getExceptionTrace(e));
            }
		}
		catch (Exception e) {
	         fail(getExceptionTrace(e));
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
            JSONObject joProfile = new JSONObject().put("test", true);

            // Create a test user and make sure there were no errors
            JSONObject joResult = createUser(screenName, password, joProfile);
            assertEquals(0, joResult.getJSONArray(OperationResult.FAILURE.name()).length());
            JSONObject joTestUser = joResult.getJSONArray(OperationResult.SUCCESS.name()).getJSONObject(0);

            UUID testUserUUID = UUID.fromString(joTestUser.getString("uuid"));

            // Delete the test user and make sure there were no errors
            joResult = deleteUser(testUserUUID);
            assertEquals(0, joResult.getJSONArray(OperationResult.FAILURE.name()).length());
            JSONArray jaSuccess = joResult.getJSONArray(OperationResult.SUCCESS.name());
            assertEquals(1, jaSuccess.length());
            JSONObject joDeletedUser = jaSuccess.getJSONObject(0);

            assertTrue(joDeletedUser.has("uuid"));
            assertEquals(testUserUUID, UUID.fromString(joDeletedUser.getString("uuid")));
            assertTrue(joDeletedUser.has("screen_name"));
            assertEquals(screenName, joDeletedUser.getString("screen_name"));

            // Try to delete a non-existent user and make sure this fails
            try {
                deleteUser(UUID.randomUUID());
            }
            catch (FileNotFoundException e) {
                // Do nothing, this is ok and expected
            }
            catch (Exception e) {
                fail(getExceptionTrace(e));
            }
        }
        catch (Exception e) {
             fail(getExceptionTrace(e));
        }
	}

	@Test
	public void basicUserComponentUploadTest() {
        try {
            String screenName = BackendStorageLinkTest.generateTestUserScreenName();
            String password = "sekret";
            JSONObject joProfile = new JSONObject().put("test", true);

            // Create a test user and make sure there were no errors
            JSONObject joResult = createUser(screenName, password, joProfile);
            assertEquals(0, joResult.getJSONArray(OperationResult.FAILURE.name()).length());

            // Make sure the test components exist
            File comp1Rdf = new File("test/components/opennlp-tokenizer.rdf");
            File comp1Context = new File("test/components/test.jar");
            assertTrue(comp1Rdf.exists() && comp1Context.exists());

            File comp2Rdf = new File("test/components/tolowercase.ttl");
            assertTrue(comp2Rdf.exists());

            Map<File, File[]> components = new HashMap<File, File[]>();
            components.put(comp1Rdf, new File[] { comp1Context });
            components.put(comp2Rdf, new File[] { });

            // Upload the components and check for errors
            joResult = uploadComponents(screenName, components);
            assertEquals(0, joResult.getJSONArray(OperationResult.FAILURE.name()).length());
            JSONArray jaSuccess = joResult.getJSONArray(OperationResult.SUCCESS.name());
            assertEquals(2, jaSuccess.length());

            JSONObject joComponent1 = jaSuccess.getJSONObject(0);
            assertTrue(joComponent1.has("uuid"));
            assertTrue(joComponent1.has("version"));
            assertEquals(1, joComponent1.getInt("version"));
            assertTrue(joComponent1.has("url"));

            JSONObject joComponent2 = jaSuccess.getJSONObject(1);
            assertTrue(joComponent2.has("uuid"));
            assertTrue(joComponent2.has("version"));
            assertEquals(1, joComponent1.getInt("version"));
            assertTrue(joComponent2.has("url"));

            // Now re-upload one of the components and check the version number
            components.remove(comp2Rdf);
            joResult = uploadComponents(screenName, components);
            assertEquals(0, joResult.getJSONArray(OperationResult.FAILURE.name()).length());
            jaSuccess = joResult.getJSONArray(OperationResult.SUCCESS.name());
            assertEquals(1, jaSuccess.length());

            joComponent1 = jaSuccess.getJSONObject(0);
            assertTrue(joComponent1.has("uuid"));
            assertTrue(joComponent1.has("version"));
            assertEquals(2, joComponent1.getInt("version"));
            assertTrue(joComponent1.has("url"));
        }
        catch (Exception e) {
            fail(getExceptionTrace(e));
        }
	}

	@Test
	public void basicUserFlowUploadTest() {
	    try {
	        String screenName = BackendStorageLinkTest.generateTestUserScreenName();
	        String password = "sekret";
	        JSONObject joProfile = new JSONObject().put("test", true);

	        // Create a test user and make sure there were no errors
	        JSONObject joResult = createUser(screenName, password, joProfile);
	        assertEquals(0, joResult.getJSONArray(OperationResult.FAILURE.name()).length());

	        // Make sure the test components exist
	        File flow1Rdf = new File("test/flows/fpgrowth.rdf");
	        File flow2Rdf = new File("test/flows/readability.nt");
	        assertTrue(flow1Rdf.exists() && flow2Rdf.exists());

	        // Upload the flows and check for errors
	        joResult = uploadFlows(screenName, new File[] { flow1Rdf, flow2Rdf });
	        assertEquals(0, joResult.getJSONArray(OperationResult.FAILURE.name()).length());
	        JSONArray jaSuccess = joResult.getJSONArray(OperationResult.SUCCESS.name());
	        assertEquals(2, jaSuccess.length());

	        JSONObject joFlow1 = jaSuccess.getJSONObject(0);
	        assertTrue(joFlow1.has("uuid"));
	        assertTrue(joFlow1.has("version"));
	        assertEquals(1, joFlow1.getInt("version"));
	        assertTrue(joFlow1.has("url"));

	        JSONObject joFlow2 = jaSuccess.getJSONObject(1);
	        assertTrue(joFlow2.has("uuid"));
	        assertTrue(joFlow2.has("version"));
	        assertEquals(1, joFlow1.getInt("version"));
	        assertTrue(joFlow2.has("url"));

	        // Now re-upload one of the flows and check the version number
	        joResult = uploadFlows(screenName, new File[] { flow1Rdf });
	        assertEquals(0, joResult.getJSONArray(OperationResult.FAILURE.name()).length());
	        jaSuccess = joResult.getJSONArray(OperationResult.SUCCESS.name());
	        assertEquals(1, jaSuccess.length());

	        joFlow1 = jaSuccess.getJSONObject(0);
	        assertTrue(joFlow1.has("uuid"));
	        assertTrue(joFlow1.has("version"));
	        assertEquals(2, joFlow1.getInt("version"));
	        assertTrue(joFlow1.has("url"));
	    }
	    catch (Exception e) {
	        fail(getExceptionTrace(e));
	    }
	}

    //-------------------------------------------------------------------------------------

	/**
	 * Creates a new SC user
	 *
	 * @param screenName The user name
	 * @param password The password
	 * @param profile The profile
	 * @return The JSON response
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws JSONException
	 */
    protected JSONObject createUser(String screenName, String password, JSONObject profile) throws MalformedURLException, IOException, JSONException {
        String reqUrl = "http://localhost:" + TEST_SERVER_PORT + "/services/users/";

        Properties props = new Properties();
        props.put("screen_name", screenName);
        props.put("password", password);
        props.put("profile", profile);

        return new JSONObject(HttpUtils.doPOST(reqUrl, "application/json", props).trim());
    }

    /**
     * Delete a SC user
     *
     * @param screenName The user name
     * @return The JSON response
     * @throws MalformedURLException
     * @throws IOException
     * @throws JSONException
     */
    protected JSONObject deleteUser(String screenName) throws MalformedURLException, IOException, JSONException {
        String reqUrl = String.format("http://localhost:%d/services/users/%s.json", TEST_SERVER_PORT, screenName);
        String responseDelete = HttpUtils.doDELETE(reqUrl, null).trim();

        return new JSONObject(responseDelete);
    }

    /**
     * Delete a SC user
     *
     * @param uuid The user id
     * @return The JSON response
     * @throws MalformedURLException
     * @throws IOException
     * @throws JSONException
     */
    protected JSONObject deleteUser(UUID uuid) throws MalformedURLException, IOException, JSONException {
        String reqUrl = String.format("http://localhost:%d/services/users/%s.json", TEST_SERVER_PORT, uuid);
        String responseDelete = HttpUtils.doDELETE(reqUrl, null).trim();

        return new JSONObject(responseDelete);
    }

    /**
     * Retrieves user information
     *
     * @param screenName The user name
     * @return The JSON response
     * @throws MalformedURLException
     * @throws IOException
     * @throws JSONException
     */
    protected JSONObject getUserInfo(String screenName) throws MalformedURLException, IOException, JSONException {
        String reqUrl = String.format("http://localhost:%d/services/users/%s.json", TEST_SERVER_PORT, screenName);
        String responseInfo = HttpUtils.doGET(reqUrl, null).trim();

        return new JSONObject(responseInfo);
    }

    /**
     * Lists users in SC
     *
     * @return The JSON response
     * @throws MalformedURLException
     * @throws IOException
     * @throws JSONException
     */
    protected JSONObject listUsers() throws MalformedURLException, IOException, JSONException {
        String reqUrl = "http://localhost:" + TEST_SERVER_PORT + "/services/users/";
        String response = HttpUtils.doGET(reqUrl, "application/json").trim();

        return new JSONObject(response);
    }

    /**
     * Uploads components to SC
     *
     * @param screenName The user to be credited with the upload
     * @param components The components (key: rdf descriptor, value: array of context files)
     * @return The JSON response
     * @throws IOException
     * @throws JSONException
     */
    protected JSONObject uploadComponents(String screenName, Map<File, File[]> components) throws IOException, JSONException {
        String reqUrl = String.format("http://localhost:%d/services/users/%s/components/", TEST_SERVER_PORT, screenName);
        List<Part> parts = new ArrayList<Part>();

        for (Entry<File, File[]> component : components.entrySet()) {
            parts.add(new FilePart("component_rdf", component.getKey()));
            for (File contextFile : component.getValue())
                parts.add(new FilePart("context", contextFile));
        }

        Part[] partsArr = new Part[parts.size()];
        return new JSONObject(HttpUtils.doPOST(reqUrl, "application/json", parts.toArray(partsArr)));
    }

    /**
     * Uploads flows to SC
     *
     * @param screenName The user to be credited with the upload
     * @param flows The flow descriptors
     * @return The JSON response
     * @throws IOException
     * @throws JSONException
     */
    protected JSONObject uploadFlows(String screenName, File[] flows) throws IOException, JSONException {
        String reqUrl = String.format("http://localhost:%d/services/users/%s/flows/", TEST_SERVER_PORT, screenName);
        List<Part> parts = new ArrayList<Part>();

        for (File flow : flows)
            parts.add(new FilePart("flow_rdf", flow));

        Part[] partsArr = new Part[parts.size()];
        return new JSONObject(HttpUtils.doPOST(reqUrl, "application/json", parts.toArray(partsArr)));
    }
}

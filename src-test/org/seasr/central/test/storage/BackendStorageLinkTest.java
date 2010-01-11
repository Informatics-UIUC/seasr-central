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

package org.seasr.central.test.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_DB;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_DRIVER;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_PASSWORD;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_URL;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_USER;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_LINK;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_REPOSITORY_LOCATION;
import static org.seasr.central.ws.restlets.Tools.createTempFolder;
import static org.seasr.central.ws.restlets.Tools.getExceptionDetails;
import static org.seasr.central.ws.restlets.Tools.getExceptionTrace;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.FlowDescription;
import org.meandre.core.repository.RepositoryImpl;
import org.seasr.central.storage.BackendStorageLink;
import org.seasr.central.storage.exceptions.BackendStorageException;
import org.seasr.central.storage.exceptions.InactiveUserException;
import org.seasr.meandre.support.generic.io.ModelUtils;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Test basic functionalities of SQLite driver
 *
 * @author Xavier Llora
 * @author Boris Capitanu
 */

public class BackendStorageLinkTest {

	/** The random number generator */
	private static final Random rnd = new Random();

	/** The test user to use */
	private static final String TEST_USER = "test";

	/** The property file containing the connection information */
	private static final String DB_PROPERTY_FILE = "conf" + File.separator + "scs-store-sqlite.xml";

	/** The loaded property file */
	protected static Properties props;

	/** The back end storage link */
	private static BackendStorageLink bsl;

	/** The location of the SC repository */
    private static File repositoryFolder;


	@BeforeClass
	public static void setUp() {
		try {
		    File dbFile = File.createTempFile("SCStore-test", ".sqlite");
		    dbFile.deleteOnExit();

	        // Get a temporary folder location to store the SC repository
            repositoryFolder = createTempFolder("repository-test");
            repositoryFolder.deleteOnExit();

			props = new Properties();
			props.loadFromXML(new FileInputStream(new File(DB_PROPERTY_FILE)));
	        props.setProperty(ORG_SEASR_CENTRAL_STORAGE_DB_URL, "jdbc:sqlite:" + dbFile.getAbsolutePath());
            props.setProperty(ORG_SEASR_CENTRAL_STORAGE_REPOSITORY_LOCATION, repositoryFolder.getAbsolutePath());

			if (!props.containsKey(ORG_SEASR_CENTRAL_STORAGE_DB_DRIVER))   fail("Missing property "+ORG_SEASR_CENTRAL_STORAGE_DB_DRIVER);
			if (!props.containsKey(ORG_SEASR_CENTRAL_STORAGE_DB_URL))      fail("Missing property "+ORG_SEASR_CENTRAL_STORAGE_DB_URL);
			if (!props.containsKey(ORG_SEASR_CENTRAL_STORAGE_DB_USER))     fail("Missing property "+ORG_SEASR_CENTRAL_STORAGE_DB_USER);
			if (!props.containsKey(ORG_SEASR_CENTRAL_STORAGE_DB_PASSWORD)) fail("Missing property "+ORG_SEASR_CENTRAL_STORAGE_DB_PASSWORD);
			if (!props.containsKey(ORG_SEASR_CENTRAL_STORAGE_DB_DB))       fail("Missing property "+ORG_SEASR_CENTRAL_STORAGE_DB_DB);
		}
		catch (IOException e) {
			fail("Failed to load property file. Reason: " + getExceptionDetails(e));
		}

		try {
			String storageLink = props.getProperty(ORG_SEASR_CENTRAL_STORAGE_LINK);
            bsl = (BackendStorageLink) Class.forName(storageLink).newInstance();
		}
		catch (Exception e) {
		    fail("Failed to load the storage link class. Reason: " + getExceptionDetails(e));
		}

		try {
            bsl.init(props);
        }
        catch (BackendStorageException e) {
            fail("Failed to initialize the backend link. Reason: " + getExceptionDetails(e));
        }
	}

	@AfterClass
	public static void tearDown() {
		props = null;
        try {
            FileUtils.deleteDirectory(repositoryFolder);
        }
        catch (IOException e) {
        }
	}

	@Test
	public void testUserCRUDCycle() {
		try {
		    long lUsers = bsl.userCount();
		    String sUser = generateTestUserScreenName();
		    JSONObject profile = createProfile(sUser);

			UUID uuid = bsl.addUser(sUser, "password", profile );

			assertNotNull(uuid);
			assertEquals(lUsers+1, bsl.userCount());
			assertEquals(uuid, bsl.getUserId(sUser));
			assertEquals(sUser, bsl.getUserScreenName(uuid));
			assertNotNull(bsl.getUserCreationTime(sUser));
			assertNotNull(bsl.getUserCreationTime(uuid));
			assertEquals(profile.toString(), bsl.getUserProfile(sUser).toString());
			assertEquals(profile.toString(), bsl.getUserProfile(uuid).toString());

			profile.put("tested_at", new Date());
			bsl.updateProfile(uuid,profile);
			assertEquals(profile.toString(), bsl.getUserProfile(sUser).toString());
			assertEquals(profile.toString(), bsl.getUserProfile(uuid).toString());

			profile.put("tested_at", new Date());
			bsl.updateProfile(sUser,profile);
			assertEquals(profile.toString(), bsl.getUserProfile(sUser).toString());
			assertEquals(profile.toString(), bsl.getUserProfile(uuid).toString());

			assertEquals(true, bsl.isUserPasswordValid(sUser, "password"));
			assertEquals(true, bsl.isUserPasswordValid(uuid, "password"));
			bsl.updateUserPassword(uuid,"new_password");
			assertEquals(true, bsl.isUserPasswordValid(sUser, "new_password"));
			assertEquals(true, bsl.isUserPasswordValid(uuid, "new_password"));
			bsl.updateUserPassword(sUser, "new_password2");
			assertEquals(true, bsl.isUserPasswordValid(sUser, "new_password2"));
			assertEquals(true, bsl.isUserPasswordValid(uuid, "new_password2"));

			bsl.removeUser(uuid);
			assertEquals(lUsers, bsl.userCount());

			sUser = generateTestUserScreenName();
			uuid = bsl.addUser(sUser, "admin", profile );
			assertEquals(lUsers+1, bsl.userCount());
			bsl.removeUser(sUser);
			assertEquals(lUsers, bsl.userCount());

			assertEquals(bsl.userCount(), bsl.listUsers(0, 1000).length());
		}
		catch (Exception e) {
			fail(getExceptionTrace(e));
		}
	}

	@Test
	public void testComponentUpload() {
	    try {
	        // Create a test user
	        String screenName = generateTestUserScreenName();
	        UUID testUserId = bsl.addUser(screenName, "sekret", createProfile(screenName));
	        assertNotNull(testUserId);

	        Model model = ModelUtils.getModel(new FileInputStream("test/components/tolowercase.ttl"), null);
	        ExecutableComponentDescription component = new RepositoryImpl(model).getAvailableExecutableComponentDescriptions().iterator().next();

	        Set<URL> contexts = new HashSet<URL>();
	        contexts.add(new File("test/components/test.jar").toURI().toURL());

	        // Add a component
	        JSONObject joResult = bsl.addComponent(testUserId, component, contexts, true);
	        assertTrue(joResult.has("uuid"));
	        assertTrue(joResult.has("version"));
	        assertEquals(1, joResult.getInt("version"));

	        // Add the component again
	        joResult = bsl.addComponent(testUserId, component, contexts, true);
	        assertTrue(joResult.has("uuid"));
	        assertTrue(joResult.has("version"));
	        assertEquals(2, joResult.getInt("version"));

	        // Now delete the user and try to upload the component again (should fail)
	        bsl.removeUser(testUserId);
	        try {
	            bsl.addComponent(testUserId, component, contexts, true);
	            fail("Should not be able to add a component for a deleted user");
	        }
	        catch (BackendStorageException e) {
	            if (e.getCause() instanceof InactiveUserException) {
	                InactiveUserException iue = (InactiveUserException)e.getCause();
	                assertEquals(testUserId, iue.getUserId());
	            } else
	                throw e;
	        }
	    }
	    catch (Exception e) {
	        fail(getExceptionTrace(e));
	    }
	}

	@Test
	public void testFlowUpload() {
	    try {
	        // Create a test user
	        String screenName = generateTestUserScreenName();
	        UUID testUserId = bsl.addUser(screenName, "sekret", createProfile(screenName));
	        assertNotNull(testUserId);

	        Model model = ModelUtils.getModel(new FileInputStream("test/flows/fpgrowth.rdf"), null);
	        FlowDescription flow = new RepositoryImpl(model).getAvailableFlowDescriptions().iterator().next();

	        // Add a flow
	        JSONObject joResult = bsl.addFlow(testUserId, flow);
	        assertTrue(joResult.has("uuid"));
	        assertTrue(joResult.has("version"));
	        assertEquals(1, joResult.getInt("version"));

	        // Add the flow again
	        joResult = bsl.addFlow(testUserId, flow);
	        assertTrue(joResult.has("uuid"));
	        assertTrue(joResult.has("version"));
	        assertEquals(2, joResult.getInt("version"));

	        // Now delete the user and try to upload the flow again (should fail)
	        bsl.removeUser(testUserId);
	        try {
	            bsl.addFlow(testUserId, flow);
	            fail("Should not be able to add a flow for a deleted user");
	        }
	        catch (BackendStorageException e) {
	            if (e.getCause() instanceof InactiveUserException) {
	                InactiveUserException iue = (InactiveUserException)e.getCause();
	                assertEquals(testUserId, iue.getUserId());
	            } else
	                throw e;
	        }
	    }
	    catch (Exception e) {
	        fail(getExceptionTrace(e));
	    }
	}

    //-------------------------------------------------------------------------------------

	/**
	 * Generates a new test user screen name
	 *
	 * @return The new test user screen name
	 */
	public static String generateTestUserScreenName() {
		return TEST_USER + "_" + System.currentTimeMillis() + "_" + Math.abs(rnd.nextInt());
	}

    private JSONObject createProfile(String user)  {
        JSONObject profile = new JSONObject();
        try {
            profile.put("screen_name", user);
            profile.put("tested_at", new Date());
        }
        catch (JSONException e) {
            fail(getExceptionTrace(e));
        }

        return profile;
    }
}

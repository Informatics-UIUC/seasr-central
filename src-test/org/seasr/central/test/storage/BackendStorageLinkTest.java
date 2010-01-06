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
import static org.junit.Assert.fail;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_DB;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_DRIVER;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_PASSWORD;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_URL;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_USER;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_LINK;
import static org.seasr.central.ws.restlets.Tools.getExceptionDetails;
import static org.seasr.central.ws.restlets.Tools.getExceptionTrace;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.seasr.central.storage.BackendStorageException;
import org.seasr.central.storage.BackendStorageLink;

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


	@BeforeClass
	public static void setUp() {
		try {
		    File dbFile = File.createTempFile("SCStore-test", ".sqlite");
		    dbFile.deleteOnExit();

			props = new Properties();
			props.loadFromXML(new FileInputStream(new File(DB_PROPERTY_FILE)));
	        props.setProperty(ORG_SEASR_CENTRAL_STORAGE_DB_URL, "jdbc:sqlite:" + dbFile.getAbsolutePath());

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

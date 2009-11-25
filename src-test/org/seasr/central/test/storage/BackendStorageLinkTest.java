/**
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.seasr.central.storage.BackendStorageLink;


/**
 * Test SQLite driver basic functionalities
 *
 * @author xavier
 *
 */
public class BackendStorageLinkTest {

	/** The random number generator */
	private static final Random rnd = new Random();

	/** The test user to use */
	private static final String TEST_USER = "test";

	/** The property file containing the connection information */
	private static final String DB_PROPERTY_FILE = "conf"+File.separator+"scs-store-sqlite.xml";

	/** The loaded property file */
	protected static Properties props;

	/** The back end storage link */
	private static BackendStorageLink bsl;

	@BeforeClass
	public static void setUp () {
		try {
			props = new Properties();
			props.loadFromXML(new FileInputStream(new File(DB_PROPERTY_FILE)));
			if ( !props.containsKey(ORG_SEASR_CENTRAL_STORAGE_DB_DRIVER) ) fail("Missing property "+ORG_SEASR_CENTRAL_STORAGE_DB_DRIVER);
			if ( !props.containsKey(ORG_SEASR_CENTRAL_STORAGE_DB_URL) ) fail("Missing property "+ORG_SEASR_CENTRAL_STORAGE_DB_URL);
			if ( !props.containsKey(ORG_SEASR_CENTRAL_STORAGE_DB_USER) ) fail("Missing property "+ORG_SEASR_CENTRAL_STORAGE_DB_USER);
			if ( !props.containsKey(ORG_SEASR_CENTRAL_STORAGE_DB_PASSWORD) ) fail("Missing property "+ORG_SEASR_CENTRAL_STORAGE_DB_PASSWORD);
			if ( !props.containsKey(ORG_SEASR_CENTRAL_STORAGE_DB_DB) ) fail("Missing property "+ORG_SEASR_CENTRAL_STORAGE_DB_DB);
		} catch (IOException e) {
			fail("Failed to load property file. "+e.toString());
		}

		try {
			bsl = (BackendStorageLink) Class.forName(props.getProperty(ORG_SEASR_CENTRAL_STORAGE_LINK)).newInstance();
			if ( !bsl.init(props) )
				fail("Failed to initialize the back end link");
		}
		catch (Exception e) {
			fail("Failed to load property file. "+e.toString());
		}

	}

	@AfterClass
	public static void tearDown() {
		props = null;
		bsl.close();
	}

	private JSONObject createProfile(String user)  {
		JSONObject profile = new JSONObject();
		try {
			profile.put("screen_name", user);
			profile.put("tested_at", new Date());
		}
		catch ( JSONException e ) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		}
		return profile;
	}

	@Test
	public void testCRUDCycle () {

		long lUsers = bsl.userCount();
		String sUser = generateTestUserScreenName();
		JSONObject profile = createProfile(sUser);
		try {
			UUID uuid = bsl.addUser(sUser, "password", profile );

			assertNotNull(uuid);
			assertEquals(lUsers+1,bsl.userCount());
			assertEquals(uuid,bsl.getUserUUID(sUser));
			assertEquals(sUser,bsl.getUserScreenName(uuid));
			assertNotNull(bsl.getUserCreationTime(sUser));
			assertNotNull(bsl.getUserCreationTime(uuid));
			assertEquals(profile.toString(),bsl.getUserProfile(sUser).toString());
			assertEquals(profile.toString(),bsl.getUserProfile(uuid).toString());

			profile.put("tested_at", new Date());
			assertEquals(true,bsl.updateProfile(uuid,profile));
			assertEquals(profile.toString(),bsl.getUserProfile(sUser).toString());
			assertEquals(profile.toString(),bsl.getUserProfile(uuid).toString());

			profile.put("tested_at", new Date());
			assertEquals(true,bsl.updateProfile(sUser,profile));
			assertEquals(profile.toString(),bsl.getUserProfile(sUser).toString());
			assertEquals(profile.toString(),bsl.getUserProfile(uuid).toString());

			assertEquals(true,bsl.isUserPasswordValid(sUser, "password"));
			assertEquals(true,bsl.isUserPasswordValid(uuid, "password"));
			assertEquals(true,bsl.updateUserPassword(uuid,"new_password"));
			assertEquals(true,bsl.isUserPasswordValid(sUser, "new_password"));
			assertEquals(true,bsl.isUserPasswordValid(uuid, "new_password"));
			assertEquals(true,bsl.updateUserPassword(sUser, "new_password2"));
			assertEquals(true,bsl.isUserPasswordValid(sUser, "new_password2"));
			assertEquals(true,bsl.isUserPasswordValid(uuid, "new_password2"));

			assertEquals(true, bsl.removeUser(uuid));
			assertEquals(lUsers,bsl.userCount());

			sUser = generateTestUserScreenName();
			uuid = bsl.addUser(sUser, "admin", profile );
			assertEquals(lUsers+1,bsl.userCount());
			assertEquals(true, bsl.removeUser(sUser));
			assertEquals(lUsers,bsl.userCount());

			assertEquals(bsl.userCount(), bsl.listUsers(0, 1000).length());

		}
		catch ( Exception e ) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		}

	}

	/** Generates a new test user screen name
	 *
	 * @return The new test user screen name
	 */
	public static String generateTestUserScreenName() {
		return TEST_USER+"_"+System.currentTimeMillis()+"_"+Math.abs(rnd.nextInt());
	}



}

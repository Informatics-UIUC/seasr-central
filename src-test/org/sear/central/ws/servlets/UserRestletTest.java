/**
 * 
 */
package org.sear.central.ws.servlets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_LINK;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;

import javax.servlet.Servlet;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.seasr.central.storage.BackendStorageLink;
import org.seasr.central.test.storage.BackendStorageLinkTest;
import org.seasr.central.ws.restlets.RestServlet;
import org.seasr.central.ws.restlets.RestfullExtensibleDispatcher;
import org.seasr.central.ws.restlets.user.AddUserRestlet;
import org.seasr.central.ws.restlets.user.DeleteUserRestlet;
import org.seasr.central.ws.restlets.user.ListUserRestlet;


/** Test class for the basic resfull dispatcher
 * 
 * @author xavier
 *
 */
public class UserRestletTest {

	/** The test server port to bind to */
	private static final int TEST_SERVER_PORT = 9090;

	/** The fixture server */
	private Server server;

	/** The back end storage link */
	private BackendStorageLink bsl;

	/** The property file containing the connection information */
	private static final String DB_PROPERTY_FILE = "conf"+File.separator+"scs-store-sqlite.xml";

	/** Sets up the fixture starting a test server
	 *  
	 */
	@Before
	public void setUpFixture () {
		server = new Server(TEST_SERVER_PORT);
		Context context = new Context(server,"/",Context.NO_SESSIONS);
		RestfullExtensibleDispatcher red = new RestfullExtensibleDispatcher();
		RestServlet [] rsa = {
				new ListUserRestlet(),
				new AddUserRestlet(),
				new DeleteUserRestlet()
		};
		Properties props = new Properties();
		try {
			props.loadFromXML(new FileInputStream(DB_PROPERTY_FILE));
			bsl = (BackendStorageLink) Class.forName(props.getProperty(ORG_SEASR_CENTRAL_STORAGE_LINK)).newInstance();
			if ( !bsl.init(props) )
				fail("Failed to initialize the back end link");
			for ( RestServlet rs: rsa ) {
				rs.setBackendStoreLink(bsl);
				red.add(rs);
			}
			context.addServlet(new ServletHolder((Servlet)red), "/*");
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
	public void  basicUserListTest () {
		try {
			String sUrl = "http://localhost:"+TEST_SERVER_PORT+"/services/user/list/format.json";
			String response = getRequest(sUrl).trim();
			JSONArray ja = new JSONArray(response);
			boolean found = false;
			for ( int i=0, iMax=ja.length() ; !found && i<iMax ; i++ )
				if ( ja.getJSONObject(i).get("screen_name").equals("admin") )
					found = true;
			assertTrue(found);
		} catch (Exception e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		}

	}
	

	/** Runs a simple add test against the basic servlet.
	 * 
	 */
	@Test
	public void basicUserAddTest () {
		try {
			String screenName = BackendStorageLinkTest.generateTestUserScreenName();
			String password = "sekret";
			String profile = "{\"test\": \"true\"}";
			String sUrl = "http://localhost:"+TEST_SERVER_PORT+"/services/user/add/format.json?"+
							"screen_name="+URLEncoder.encode(screenName,"UTF-8")+"" +
							"&password="+URLEncoder.encode(password,"UTF-8")+
							"&profile="+URLEncoder.encode(profile,"UTF-8");
			String response = getRequest(sUrl).trim();
			JSONObject jo = (new JSONArray(response)).getJSONObject(0);
			assertTrue(jo.has("uuid"));
			assertTrue(jo.has("screen_name"));
			assertEquals(screenName,jo.getString("screen_name"));
			assertTrue(jo.has("created_at"));
			assertTrue(jo.has("profile"));
		} catch (Exception e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		}

	}
	

	/** Runs a simple delete test against the basic servlet using the screen name.
	 * 
	 */
	@Test
	public void basicUserDeleteUsingScreenNameTest () {
		try {
			String screenName = BackendStorageLinkTest.generateTestUserScreenName();
			String password = "sekret";
			String profile = "{\"test\": \"true\"}";
			String sUrlAddUser = "http://localhost:"+TEST_SERVER_PORT+"/services/user/add/format.json?"+
					   			 "screen_name="+URLEncoder.encode(screenName,"UTF-8")+"" +
					   			 "&password="+URLEncoder.encode(password,"UTF-8")+
					   			 "&profile="+URLEncoder.encode(profile,"UTF-8");
			String responseAdd = getRequest(sUrlAddUser).trim();
			JSONObject joAdd = (new JSONArray(responseAdd)).getJSONObject(0);
			assertTrue(joAdd.has("uuid"));
			assertTrue(joAdd.has("screen_name"));
			assertTrue(joAdd.has("created_at"));
			assertTrue(joAdd.has("profile"));
			
			 String sUrlDeleteUser = "http://localhost:"+TEST_SERVER_PORT+"/services/user/delete/format.json?"+
			 						 "screen_name="+screenName;
			String responseDelete = getRequest(sUrlDeleteUser).trim();
			JSONObject jo = (new JSONArray(responseDelete)).getJSONObject(0);
			assertTrue(jo.has("uuid"));
			assertTrue(jo.has("screen_name"));
			
		} catch (Exception e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(baos));
			fail(baos.toString());
		}

	}
	
	/** Runs a simple delete test against the basic servlet using the uuid.
	 * 
	 */
	@Test
	public void basicUserDeleteUsingUUIDTest () {
		try {
			String screenName = BackendStorageLinkTest.generateTestUserScreenName();
			String password = "sekret";
			String profile = "{\"test\": \"true\"}";
			String sUrlAddUser = "http://localhost:"+TEST_SERVER_PORT+"/services/user/add/format.json?"+
					   			 "screen_name="+URLEncoder.encode(screenName,"UTF-8")+"" +
					   			 "&password="+URLEncoder.encode(password,"UTF-8")+
					   			 "&profile="+URLEncoder.encode(profile,"UTF-8");
			String responseAdd = getRequest(sUrlAddUser).trim();
			JSONObject joAdd = (new JSONArray(responseAdd)).getJSONObject(0);
			assertTrue(joAdd.has("uuid"));
			assertTrue(joAdd.has("screen_name"));
			assertTrue(joAdd.has("created_at"));
			assertTrue(joAdd.has("profile"));
			
			 String sUrlDeleteUser = "http://localhost:"+TEST_SERVER_PORT+"/services/user/delete/format.json?"+
			 						 "uuid="+URLEncoder.encode(joAdd.getString("uuid"),"UTF-8");
			String responseDelete = getRequest(sUrlDeleteUser).trim();
			JSONObject jo = (new JSONArray(responseDelete)).getJSONObject(0);
			assertTrue(jo.has("uuid"));
			assertTrue(jo.has("screen_name"));
			
		} catch (Exception e) {
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

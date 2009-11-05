/**
 * 
 */
package org.seasr.central.storage.db;

import static org.seasr.central.properties.SCDBProperties.*;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_DRIVER;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_PASSWORD;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_URL;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_USER;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.seasr.central.storage.BackendStorageLink;

/** The SQLite driver to create a backend storage link
 * 
 * @author xavier
 *
 */
public class SQLiteLink implements BackendStorageLink {

	//-------------------------------------------------------------------------------------

	/** The properties available */
	Properties properties = null;

	/** The connection object to que SQLite database */
	private Connection conn = null;

	/** The digest calculator */
	private MessageDigest md;

	/** The connection properties. */
	private Properties conProps;

	//-------------------------------------------------------------------------------------

	/** Initialize the back end storage driver with the given properties.
	 * 
	 * @param properties The properties required to initialize the backend link
	 * @return true if it could be properly initialized, false otherwise
	 */
	@Override
	public boolean init(Properties properties) {
		try {
			// Retain the properties
			this.properties = properties;
			this.md = MessageDigest.getInstance("SHA-1");

			// Initialize the connection
			Class.forName(properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_DRIVER));
			conProps = new Properties();
			conProps.put("user", properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_USER));
			conProps.put("password", properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_PASSWORD));
			conn = DriverManager.getConnection(properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_URL),conProps);

			// Create the authentication schema if needed
			String [] sql = properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_AUTH_SCHEMA).toString().replace('\t', ' ').replace('\r', ' ').split("\n");
			for ( String update:sql ) {
				update = update.trim();
				if ( update.length()>0 )
					conn.createStatement().executeUpdate(update);
			}

			// Create the schema if needed
			sql = properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_SCHEMA).toString().replace('\t', ' ').replace('\r', ' ').split("\n");
			for ( String update:sql ) {
				update = update.trim();
				if ( update.length()>0 )
					conn.createStatement().executeUpdate(update);
			}

			// Signal that everything went fine
			return true;
		}
		catch ( Exception e ) {
			// Could not initialize nicely
			return false;
		}
	}
	
	/** Closes the backend storage link.
	 * 
	 * @return true if it could be properly closed, false otherwise
	 */
	public boolean close() {
		try {
			conn.close();
			return true;
		}
		catch ( Exception e ) {
			// Could not close nicely
			return false;
		}
	}


	//-------------------------------------------------------------------------------------

	/** Adds a user to the back end storage facility.
	 * 
	 * @param user The user name
	 * @param password The password for this users
	 * @param profile The profile information for this user
	 * @return The UUID of the created users. If the user could not be created null is returned
	 */
	public UUID addUser ( String user, String password, JSONObject profile ) {
		try {
			UUID uuid = UUID.randomUUID();
			PreparedStatement ps = conn.prepareStatement(properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_ADD).toString());
			ps.setString(1, uuid.toString());
			ps.setString(2, user);
			ps.setString(3, computeDigest(password));
			ps.setString(4, profile.toString());
			ps.executeUpdate();
			return uuid;
		} catch (SQLException e) {
			return null;
		}
	}

	/** Computes the digest of a string and returns its hex encoded string
	 * 
	 * @param string The string to process
	 * @return The resulting digest
	 */
	private String computeDigest(String string) {
		StringBuffer sb = new StringBuffer();
		md.reset();
		for ( byte b:md.digest(string.getBytes()) )
			sb.append(Integer.toString(b, 32));
		return sb.toString();
	}

	/** Remove a user from the back end storage facility.
	 * 
	 * @param uuid The UUID of the user to remove
	 * @return True if the user could be successfully removed. False otherwise
	 */
	public boolean removeUser ( UUID uuid ) {
		try {
			PreparedStatement ps = conn.prepareStatement(properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_REMOVE_UUID).toString());
			ps.setString(1, uuid.toString());
			ps.executeUpdate();
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	/** Remove a user from the back end storage facility.
	 * 
	 * @param user The screen name of the user to remove
	 * @return True if the user could be successfully removed. False otherwise
	 */
	public boolean removeUser ( String user ) {
		try {
			PreparedStatement ps = conn.prepareStatement(properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_REMOVE_SCREEN_NAME).toString());
			ps.setString(1, user);
			ps.executeUpdate();
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	/** Updates the user's password.
	 * 
	 * @param uuid The user UUID 
	 * @param password The new password to use
	 * @return True if the password could be successfully updated. False otherwise
	 */
	public boolean updateUserPassword ( UUID uuid, String password ) {
		try {
			PreparedStatement ps = conn.prepareStatement(properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PASSWORD_UUID).toString());
			ps.setString(1, computeDigest(password));
			ps.setString(2, uuid.toString());
			ps.executeUpdate();
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	/** Updates the user's password.
	 * 
	 * @param user The user screen_name 
	 * @param password The new password to use
	 * @return True if the password could be successfully updated. False otherwise
	 */
	public boolean updateUserPassword ( String user, String password ) {
		try {
			PreparedStatement ps = conn.prepareStatement(properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PASSWORD_SCREEN_NAME).toString());
			ps.setString(1, computeDigest(password));
			ps.setString(2, user);
			ps.executeUpdate();
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	/** Updates the user's profile.
	 * 
	 * @param uuid The user UUID
	 * @param profile The new profile to use
	 * @return True if the profile could be successfully updated. False otherwise
	 */
	public boolean updateProfile ( UUID uuid, JSONObject profile ) {
		try {
			PreparedStatement ps = conn.prepareStatement(properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PROFILE_UUID).toString());
			ps.setString(1, profile.toString());
			ps.setString(2, uuid.toString());
			ps.executeUpdate();
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	/** Updates the user's profile.
	 * 
	 * @param user The user screen_name 
	 * @param profile The new profile to use
	 * @return True if the profile could be successfully updated. False otherwise
	 */
	public boolean updateProfile ( String user, JSONObject profile ) {
		try {
			PreparedStatement ps = conn.prepareStatement(properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PROFILE_SCREEN_NAME).toString());
			ps.setString(1, profile.toString());
			ps.setString(2, user);
			ps.executeUpdate();
			return true;
		} catch (SQLException e) {
			return false;
		}
	}

	/** Returns the UUID of a user given his screen name.
	 * 
	 * @param user The user's screen name
	 * @return The UUID of the user or null if the user does not exist
	 */
	public UUID getUserUUID ( String user ) {
		ResultSet rs = null;
		try {
			PreparedStatement ps = conn.prepareStatement(properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_UUID).toString());
			ps.setString(1, user);
			rs = ps.executeQuery();
			rs.next();	
			UUID uuid = UUID.fromString(rs.getString(1));
			rs.close();
			return uuid;
		} catch (SQLException e) {
			if ( rs!=null )
				try {
					rs.close();
				} catch (SQLException e1) {
					// Failed to clean
				}
				return null;
		}
	}
	
	/** Returns the screen name of a user given his screen name.
	 * 
	 * @param uuid The user's UUID
	 * @return The screen name of the user or null if the user does not exist
	 */
	public String getUserScreenName ( UUID uuid ) {
		ResultSet rs = null;
		try {
			PreparedStatement ps = conn.prepareStatement(properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_SCREEN_NAME).toString());
			ps.setString(1, uuid.toString());
			rs = ps.executeQuery();
			rs.next();	
			String screenName = rs.getString(1);
			rs.close();
			return screenName;
		} catch (SQLException e) {
			if ( rs!=null )
				try {
					rs.close();
				} catch (SQLException e1) {
					// Failed to clean
				}
				return null;
		}
	}

	/** Returns the profile of a user given his screen name.
	 * 
	 * @param user The user's screen name
	 * @return The profile of the user or null if the user does not exist
	 */
	public JSONObject getUserProfile ( String user ) {
		ResultSet rs = null;
		try {
			PreparedStatement ps = conn.prepareStatement(properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_PROFILE_SCREEN_NAME).toString());
			ps.setString(1, user);
			rs = ps.executeQuery();
			rs.next();	
			JSONObject jo =  new JSONObject(rs.getString(1));
			rs.close();
			return jo;
		} catch (Exception e) {
			if ( rs!=null )
				try {
					rs.close();
				} catch (SQLException e1) {
					// Failed to clean
				}
				return null;
		} 
	}


	/** Returns the profile of a user given his UUID.
	 * 
	 * @param uuid The user's UUID
	 * @return The profile of the user or null if the user does not exist
	 */
	public JSONObject getUserProfile ( UUID uuid ) {
		ResultSet rs = null;
		try {
			PreparedStatement ps = conn.prepareStatement(properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_PROFILE_UUID).toString());
			ps.setString(1, uuid.toString());
			rs = ps.executeQuery();
			rs.next();	
			JSONObject jo =  new JSONObject(rs.getString(1));
			rs.close();
			return jo;
		} catch (Exception e) {
			if ( rs!=null )
				try {
					rs.close();
				} catch (SQLException e1) {
					// Failed to clean
				}
				return null;
		} 
	}
	
	/** Returns the creation time of a user given his screen name.
	 * 
	 * @param user The user's screen name
	 * @return The creation time of the user or null if the user does not exist
	 */
	public Date getUserCreationTime ( String user ) {
		ResultSet rs = null;
		try {
			PreparedStatement ps = conn.prepareStatement(properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_CREATEDAT_SCREEN_NAME).toString());
			ps.setString(1, user);
			rs = ps.executeQuery();
			rs.next();	
			Date date = rs.getTimestamp(1);
			rs.close();
			return date;
		} catch (Exception e) {
			if ( rs!=null )
				try {
					rs.close();
				} catch (SQLException e1) {
					// Failed to clean
				}
				return null;
		} 
	}



	/** Returns the creation time of a user given his UUID.
	 * 
	 * @param uuid The user's UUID
	 * @return The creation time of the user or null if the user does not exist
	 */
	public Date getUserCreationTime ( UUID uuid ) {
		ResultSet rs = null;
		try {
			PreparedStatement ps = conn.prepareStatement(properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_CREATEDAT_UUID).toString());
			ps.setString(1, uuid.toString());
			rs = ps.executeQuery();
			rs.next();	
			Date date = rs.getTimestamp(1);
			rs.close();
			return date;
		} catch (Exception e) {
			if ( rs!=null )
				try {
					rs.close();
				} catch (SQLException e1) {
					// Failed to clean
				}
				return null;
		} 
	}


	/** Check if the user password is valid based on user's screen name.
	 * 
	 * @param user The user's screen name
	 * @param password The password to check
	 * @return True if the password matches, false otherwise
	 */
	public boolean isUserPasswordValid ( String user, String password ) {
		ResultSet rs = null;
		try {
			PreparedStatement ps = conn.prepareStatement(properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_VALID_PASSWORD_SCREEN_NAME).toString());
			ps.setString(1, user);
			ps.setString(2, computeDigest(password));
			rs = ps.executeQuery();
			if ( rs.next() )
				if ( rs.getBoolean(1) ) {
					rs.close();
					return true;
				}
			rs.close();
			return false;
		} catch (Exception e) {
			if ( rs!=null )
				try {
					rs.close();
				} catch (SQLException e1) {
					// Failed to clean
				}
				return false;
		} 
	}

	/** Check if the user password is valid based on the UUID of the user.
	 * 
	 * @param uuid The user's UUID
	 * @param password The password to check
	 * @return True if the password matches, false otherwise
	 */
	public boolean isUserPasswordValid ( UUID uuid, String password ) {
		ResultSet rs = null;
		try {
			PreparedStatement ps = conn.prepareStatement(properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_VALID_PASSWORD_UUID).toString());
			ps.setString(1, uuid.toString());
			ps.setString(2, computeDigest(password));
			rs = ps.executeQuery();
			if ( rs.next() )
				if ( rs.getBoolean(1) ) {
					rs.close();
					return true;
				}
			rs.close();
			return false;
		} catch (Exception e) {
			if ( rs!=null )
				try {
					rs.close();
				} catch (SQLException e1) {
					// Failed to clean
				}
				return false;
		} 
	}


	/** Return the number of users on the back end storage.
	 * 
	 * @return The number of users in SC back end storage. -1 indicates failure
	 */
	public long userSize() {
		String query = properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_COUNT).toString().trim();
		try {
			ResultSet rs = conn.createStatement().executeQuery(query);
			rs.next();
			long res = rs.getLong(1);
			rs.close();
			return res;
		} catch (SQLException e) {
			return -1;
		}
	}


	/** List the users contained on the database. Must provide number of users desired
	 * and offset into the listing.
	 * 
	 * @param count The number of users to be returned
	 * @param offset The offset where to start computing
	 * @return The list of retrieved users
	 */
	public JSONArray listUsers ( long count, long offset ) {
		String query = properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_LIST).toString().trim();
		JSONArray ja = new JSONArray();
		ResultSet rs = null;
		try {
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setLong(1, offset);
			ps.setLong(2, count);
			rs = ps.executeQuery();
			while ( rs.next() ){
				JSONObject jo = new JSONObject();
				try {
					jo.put("uuid",rs.getString("uuid"));
					jo.put("screen_name",rs.getString("screen_name"));
					jo.put("profile", new JSONObject(rs.getString("profile")));
					ja.put(jo);
				} catch (JSONException e) {
					// Discarding user
				}
			}
			rs.close();
			return ja;
		} catch (SQLException e) {
			if ( rs!=null )
				try {
					rs.close();
				} catch (SQLException e1) {
					// Tried to properly clean out
				}
			return ja;
		}
	}

	//-------------------------------------------------------------------------------------

}
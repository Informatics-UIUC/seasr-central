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

package org.seasr.central.storage.db;

import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_AUTH_SCHEMA;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_ADD;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_ADD_ID;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_GET_COMPONENT_ID_BASEURI_USER;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_GET_LAST_VERSION_NUMBER;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_EVENT_ADD;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_ADD;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_COUNT;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_CREATEDAT_SCREEN_NAME;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_CREATEDAT_UUID;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_PROFILE_SCREEN_NAME;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_PROFILE_UUID;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_SCREEN_NAME;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_UUID;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_LIST;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_REMOVE_SCREEN_NAME;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_REMOVE_UUID;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PASSWORD_SCREEN_NAME;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PASSWORD_UUID;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PROFILE_SCREEN_NAME;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PROFILE_UUID;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_VALID_PASSWORD_SCREEN_NAME;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_VALID_PASSWORD_UUID;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_SCHEMA;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_DRIVER;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_PASSWORD;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_URL;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_USER;
import static org.seasr.central.ws.restlets.Tools.logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

import org.apache.commons.fileupload.FileItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.seasr.central.storage.BackendStorageLink;
import org.seasr.central.storage.Event;
import org.seasr.central.storage.SourceType;
import org.seasr.meandre.support.generic.crypto.Crypto;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * The SQLite driver to create a backend storage link
 *
 * @author xavier
 */
public class SQLiteLink implements BackendStorageLink {

    private static final String REPOSITORY_FOLDER = "repository";

	//-------------------------------------------------------------------------------------

	/** The properties available */
	Properties properties = null;

	/** The connection object to queue SQLite database */
	private Connection conn = null;

	/** The connection properties. */
	private Properties conProps;

	//-------------------------------------------------------------------------------------

	/**
	 * Initialize the back end storage driver with the given properties.
	 *
	 * @param properties The properties required to initialize the backend link
	 * @return true if it could be properly initialized, false otherwise
	 */
	@Override
	public boolean init(Properties properties) {
		try {
			// Retain the properties
			this.properties = properties;

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

	/**
	 * Closes the backend storage link.
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

	/**
	 * Adds a user to the back end storage facility.
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

	/**
	 * Computes the digest of a string and returns its hex encoded string
	 *
	 * @param string The string to process
	 * @return The resulting digest
	 */
	private String computeDigest(String string) {
		try {
            return Crypto.getSHA1Hash(string);
        }
        catch (NoSuchAlgorithmException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
        catch (UnsupportedEncodingException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }

        return null;
	}

	/**
	 * Remove a user from the back end storage facility.
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

	/**
	 * Remove a user from the back end storage facility.
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

	/**
	 * Updates the user's password.
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

	/**
	 * Updates the user's password.
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

	/**
	 * Updates the user's profile.
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

	/**
	 * Updates the user's profile.
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

	/**
	 * Returns the UUID of a user given his screen name.
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
		    if ( rs!=null ) {
		        try {
		            rs.close();
		        } catch (SQLException e1) {
		            // Failed to clean
		        }
		    }

		    return null;
		}
	}

	/**
	 * Returns the screen name of a user given his screen name.
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

	/**
	 * Returns the profile of a user given his screen name.
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


	/**
	 * Returns the profile of a user given his UUID.
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

	/**
	 * Returns the creation time of a user given his screen name.
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

	/**
	 * Returns the creation time of a user given his UUID.
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


	/**
	 * Check if the user password is valid based on user's screen name.
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

	/**
	 * Check if the user password is valid based on the UUID of the user.
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

	/**
	 * Return the number of users on the back end storage.
	 *
	 * @return The number of users in SC back end storage. -1 indicates failure
	 */
	public long userCount() {
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
	 * @param offset The offset where to start computing
	 * @param count The number of users to be returned
	 * @return The list of retrieved users
	 */
	public JSONArray listUsers(long offset, long count) {
		String query = properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_LIST).toString().trim();
		JSONArray ja = new JSONArray();
		ResultSet rs = null;

		try {
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setLong(1, offset);
			ps.setLong(2, count);
			rs = ps.executeQuery();
			while (rs.next()) {
				JSONObject jo = new JSONObject();
				try {
					jo.put("uuid", rs.getString("uuid"));
					jo.put("screen_name", rs.getString("screen_name"));
					jo.put("profile", new JSONObject(rs.getString("profile")));
					ja.put(jo);
				}
				catch (JSONException e) {
					// Discarding user
				    logger.log(Level.WARNING, e.getMessage(), e);
				}
			}

			return ja;
		}
		catch (SQLException e) {
			return null;
		}
		finally {
		    if (rs != null)
                try {
                    rs.close();
                }
		        catch (SQLException e1) {
                    // Tried to properly clean out
                }
		}
	}

    @Override
    public boolean addEvent(SourceType sourceType, UUID uuid, Event eventCode, JSONObject description) {
        String query = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_EVENT_ADD);

        try {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, "" + sourceType.getSourceTypeCode());
            ps.setString(2, uuid.toString());
            ps.setInt(3, eventCode.getEventCode());
            ps.setString(4, description.toString());
            ps.executeUpdate();
            return true;
        }
        catch (SQLException e) {
            return false;
        }
    }

    @Override
    public JSONObject addComponent(UUID userId, ExecutableComponentDescription comp, Set<FileItem> contexts) {
        String queryAddId = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_ADD_ID);
        String queryAdd = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_ADD);

        JSONObject joResult = new JSONObject();

        String origURI = comp.getExecutableComponent().getURI();

        UUID uuid = getComponentUUID(origURI, userId);
        int version = -1;

        if (uuid != null) {
            version = getLastVersionNumberForComponent(uuid);
            // Sanity check
            if (version == -1)
                logger.warning(String.format("No version found for existing component %s (uri: %s)", uuid, origURI));
            else
                version++;
        } else {
            uuid = UUID.randomUUID();

            try {
                // Add the mapping from origURI, userID to component UUID
                PreparedStatement psAddId = conn.prepareStatement(queryAddId);
                psAddId.setString(1, origURI);
                psAddId.setString(2, userId.toString());
                psAddId.setString(3, uuid.toString());
                psAddId.executeUpdate();
            }
            catch (SQLException e) {
                try {
                    joResult.put("error", e.getMessage());
                    return joResult;
                }
                catch (JSONException e1) {
                    throw new RuntimeException(e1);
                }
            }
        }

        if (version == -1) version = 1;

        logger.fine(String.format("Adding component %s (uuid: %s, version %d, user: %s", origURI, uuid, version, userId));

        // Make sure we have a place to put the components and contexts
        File fREPOSITORY_COMPONENTS = new File(REPOSITORY_FOLDER, "components");
        File fREPOSITORY_CONTEXTS = new File(REPOSITORY_FOLDER, "contexts");
        fREPOSITORY_COMPONENTS.mkdirs();
        fREPOSITORY_CONTEXTS.mkdirs();

        // Clear any existing contexts
        Set<RDFNode> compContexts = comp.getContext();
        compContexts.clear();

        Model tmpModel = comp.getExecutableComponent().getModel();

        for (FileItem context : contexts) {
            logger.finer("Processing context file: " + context.getName());
            try {
                File tmpFile = File.createTempFile("context", ".tmp", fREPOSITORY_CONTEXTS);
                context.write(tmpFile);

                final String md5 = Crypto.getHexString(Crypto.createMD5Checksum(tmpFile));

                // Look for context files that have the same MD5 hash value
                File[] files = fREPOSITORY_CONTEXTS.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith(md5 + "_");
                    }
                });

                if (files.length > 0) {
                    logger.fine(String.format("Context file %s already exists (hashed to: %s), skipping it...",
                            context.getName(), files[0].getName()));
                    tmpFile.delete();
                    compContexts.add(tmpModel.createResource("context://localhost/" + files[0].getName()));
                    continue;
                }

                File ctxFile = new File(fREPOSITORY_CONTEXTS, md5 + "_" + context.getName());
                if (!tmpFile.renameTo(ctxFile))
                    throw new RuntimeException("Could not rename " + tmpFile + " to " + ctxFile);

                compContexts.add(tmpModel.createResource("context://localhost/" + ctxFile.getName()));
                logger.finer("Context file saved as " + ctxFile);
            }
            catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            catch (IOException e) {
               throw new RuntimeException(e);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        File fCompFolder = new File(fREPOSITORY_COMPONENTS, uuid.toString());
        fCompFolder.mkdir();

        try {
            // Write the component descriptor file for this version
            Model compModel = comp.getModel();
            File fCompDescriptor = new File(fCompFolder, "" + version + ".ttl");
            logger.finer("Saving component descriptor as " + fCompDescriptor);
            FileOutputStream fos = new FileOutputStream(fCompDescriptor);
            compModel.write(fos, "TURTLE");
            fos.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            // Add the new component version
            PreparedStatement psAdd = conn.prepareStatement(queryAdd);
            psAdd.setString(1, uuid.toString());
            psAdd.setInt(2, version);
            psAdd.executeUpdate();
        }
        catch (SQLException e) {
            try {
                joResult.put("error", e.getMessage());
                return joResult;
            }
            catch (JSONException e1) {
                throw new RuntimeException(e1);
            }
        }

        try {
            joResult.put("uuid", uuid.toString());
            joResult.put("version", version);
        }
        catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return joResult;
    }

	//-------------------------------------------------------------------------------------

    private UUID getComponentUUID(String baseUri, UUID userId) {
        String query = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_GET_COMPONENT_ID_BASEURI_USER);
        ResultSet rs = null;

        try {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, baseUri);
            ps.setString(2, userId.toString());
            rs = ps.executeQuery();
            rs.next();
            return UUID.fromString(rs.getString(1));
        }
        catch (SQLException e) {
            return null;
        }
        finally {
            if (rs != null)
                try {
                    rs.close();
                }
                catch (SQLException e1) {
                    // Tried to properly clean out
                }
        }
    }

    /**
     * Returns the highest version number for a component
     *
     * @param uuid The component uuid
     * @return The version number, or -1 if no version was found, or null if SQL error
     */
    private int getLastVersionNumberForComponent(UUID uuid) {
        String query = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_GET_LAST_VERSION_NUMBER);
        ResultSet rs = null;

        try {
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, uuid.toString());
            rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
        catch (SQLException e) {
            return -1;
        }
        finally {
            if (rs != null)
                try {
                    rs.close();
                }
                catch (SQLException e1) {
                    // Tried to properly clean out
                }
        }
    }
}

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
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_GET_COMPONENT_STATE_UUID_VERSION;
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
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_LOGLEVEL;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_PASSWORD;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_URL;
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_DB_USER;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.fileupload.FileItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.seasr.central.storage.BackendStorageException;
import org.seasr.central.storage.BackendStorageLink;
import org.seasr.central.storage.Event;
import org.seasr.central.storage.SourceType;
import org.seasr.central.ws.restlets.Tools.GenericExceptionFormatter;
import org.seasr.meandre.support.generic.crypto.Crypto;
import org.seasr.meandre.support.generic.io.ModelUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;

import de.schlichtherle.io.FileInputStream;

/**
 * The SQLite driver to create a backend storage link
 *
 * @author Xavier Llora
 * @author Boris Capitanu
 */

public class SQLiteLink implements BackendStorageLink {

    /** The root folder where the components and contexts are stored */
    private static final String REPOSITORY_FOLDER = "repository";

    /** Date parser for the DATETIME SQLite datatype (Note: use together with 'localtime' in SQL query to retrieve correct timestamp) */
    private static final SimpleDateFormat SQLITE_DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	//-------------------------------------------------------------------------------------

    /** The DB logger */
    private static Logger logger;

	/** The properties available */
	Properties properties = null;

	/** The connection object to queue SQLite database */
	private Connection conn = null;

	/** The connection properties. */
	private Properties conProps;

	//-------------------------------------------------------------------------------------

	static {
	    // Initialize the logger
        logger = Logger.getLogger(SQLiteLink.class.getName());
        logger.setUseParentHandlers(false);

        try {
            FileHandler fileHandler = new FileHandler("logs" + File.separator + "sqlite.log", true);
            fileHandler.setFormatter(new GenericExceptionFormatter());

            logger.addHandler(fileHandler);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
	}

	@Override
	public void init(Properties properties) throws BackendStorageException {
		try {
			// Retain the properties
			this.properties = properties;

			// Set the logging level
			logger.setLevel(Level.parse(properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_LOGLEVEL, Level.ALL.getName())));

			// Initialize the connection
			Class.forName(properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_DRIVER));

			conProps = new Properties();
			conProps.put("user", properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_USER));
			conProps.put("password", properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_PASSWORD));
			conn = DriverManager.getConnection(properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_URL), conProps);

			// Create the authentication schema if needed
			String[] sql = properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_AUTH_SCHEMA).toString().replace('\t', ' ').replace('\r', ' ').split("\n");
			for (String update : sql) {
				update = update.trim();
				if (update.length() > 0)
					conn.createStatement().executeUpdate(update);
			}

			// Create the schema if needed
			sql = properties.get(ORG_SEASR_CENTRAL_STORAGE_DB_SCHEMA).toString().replace('\t', ' ').replace('\r', ' ').split("\n");
			for (String update : sql) {
				update = update.trim();
				if (update.length() > 0)
					conn.createStatement().executeUpdate(update);
			}
		}
        catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Cannot load the DB driver", e);
            close();
            throw new BackendStorageException(e);
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            close();
            throw new BackendStorageException(e);
        }
	}

	@Override
	public boolean close() {
		try {
		    if (conn != null)
		        conn.close();

			return true;
		}
		catch (Exception e) {
			return false;
		}
	}

	//-------------------------------------------------------------------------------------

	@Override
	public UUID addUser(String userName, String password, JSONObject profile) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_ADD).trim();

		try {
			UUID uuid = UUID.randomUUID();
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, uuid.toString());
			ps.setString(2, userName);
			ps.setString(3, computeDigest(password));
			ps.setString(4, profile.toString());
			ps.executeUpdate();

			return uuid;
		}
		catch (SQLException e) {
		    logger.log(Level.SEVERE, null, e);
			throw new BackendStorageException(e);
		}
	}

	@Override
	public void removeUser(UUID userId) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_REMOVE_UUID).trim();

		try {
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, userId.toString());
			ps.executeUpdate();
		}
		catch (SQLException e) {
			logger.log(Level.SEVERE, null, e);
			throw new BackendStorageException(e);
		}
	}

	@Override
	public void removeUser(String userName) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_REMOVE_SCREEN_NAME).trim();

		try {
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, userName);
			ps.executeUpdate();
		}
		catch (SQLException e) {
		    logger.log(Level.SEVERE, null, e);
		    throw new BackendStorageException(e);
		}
	}

	@Override
	public void updateUserPassword(UUID userId, String password) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PASSWORD_UUID).trim();

		try {
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, computeDigest(password));
			ps.setString(2, userId.toString());
			ps.executeUpdate();
		}
		catch (SQLException e) {
			logger.log(Level.SEVERE, null, e);
			throw new BackendStorageException(e);
		}
	}

	@Override
	public void updateUserPassword(String userName, String password) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PASSWORD_SCREEN_NAME).trim();

		try {
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, computeDigest(password));
			ps.setString(2, userName);
			ps.executeUpdate();
		}
		catch (SQLException e) {
		    logger.log(Level.SEVERE, null, e);
		    throw new BackendStorageException(e);
		}
	}

	@Override
	public void updateProfile(UUID userId, JSONObject profile) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PROFILE_UUID).trim();

		try {
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, profile.toString());
			ps.setString(2, userId.toString());
			ps.executeUpdate();
		}
		catch (SQLException e) {
		    logger.log(Level.SEVERE, null, e);
		    throw new BackendStorageException(e);
		}
	}

	@Override
	public void updateProfile(String userName, JSONObject profile) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PROFILE_SCREEN_NAME).trim();

		try {
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, profile.toString());
			ps.setString(2, userName);
			ps.executeUpdate();
		}
		catch (SQLException e) {
		    logger.log(Level.SEVERE, null, e);
		    throw new BackendStorageException(e);
		}
	}

	@Override
	public UUID getUserId(String userName) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_UUID).trim();
		ResultSet rs = null;

		try {
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, userName);
			rs = ps.executeQuery();

			return rs.next() ? UUID.fromString(rs.getString(1)) : null;
		}
		catch (SQLException e) {
		    logger.log(Level.SEVERE, null, e);
		    throw new BackendStorageException(e);
		}
		finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e) { }
            }
		}
	}

	@Override
	public String getUserScreenName(UUID userId) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_SCREEN_NAME).trim();
		ResultSet rs = null;

		try {
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, userId.toString());
			rs = ps.executeQuery();

			return rs.next() ? rs.getString(1) : null;
		}
		catch (SQLException e) {
		    logger.log(Level.SEVERE, null, e);
		    throw new BackendStorageException(e);
		}
		finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e) { }
            }
		}
	}

	@Override
	public JSONObject getUserProfile(String userName) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_PROFILE_SCREEN_NAME).trim();
		ResultSet rs = null;

		try {
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, userName);
			rs = ps.executeQuery();

			return rs.next() ? new JSONObject(rs.getString(1)) : null;
		}
		catch (Exception e) {
		    logger.log(Level.SEVERE, null, e);
		    throw new BackendStorageException(e);
		}
		finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e) { }
            }
		}
	}

	@Override
	public JSONObject getUserProfile(UUID userId) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_PROFILE_UUID).trim();
		ResultSet rs = null;

		try {
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, userId.toString());
			rs = ps.executeQuery();

			return rs.next() ? new JSONObject(rs.getString(1)) : null;
		}
		catch (Exception e) {
		    logger.log(Level.SEVERE, null, e);
		    throw new BackendStorageException(e);
		}
		finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e) { }
            }
		}
	}

	@Override
	public Date getUserCreationTime(String userName) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_CREATEDAT_SCREEN_NAME).trim();
		ResultSet rs = null;

		try {
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, userName);
			rs = ps.executeQuery();

			return (rs.next()) ? SQLITE_DATE_PARSER.parse(rs.getString(1)) : null;
		}
		catch (Exception e) {
		    logger.log(Level.SEVERE, null, e);
		    throw new BackendStorageException(e);
		}
		finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e) { }
            }
		}
	}

	@Override
	public Date getUserCreationTime(UUID userId) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_CREATEDAT_UUID).trim();
		ResultSet rs = null;

		try {
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, userId.toString());
			rs = ps.executeQuery();

            return (rs.next()) ? SQLITE_DATE_PARSER.parse(rs.getString(1)) : null;
		}
		catch (Exception e) {
		    logger.log(Level.SEVERE, null, e);
		    throw new BackendStorageException(e);
		}
		finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e) { }
            }
		}
	}

	@Override
	public Boolean isUserPasswordValid(String userName, String password) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_VALID_PASSWORD_SCREEN_NAME).trim();
		ResultSet rs = null;

		try {
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, userName);
			ps.setString(2, computeDigest(password));
			rs = ps.executeQuery();

			return rs.next() ? rs.getBoolean(1) : null;
		}
		catch (Exception e) {
		    logger.log(Level.SEVERE, null, e);
		    throw new BackendStorageException(e);
		}
        finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e) { }
            }
        }
	}

	@Override
	public Boolean isUserPasswordValid(UUID userId, String password) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_VALID_PASSWORD_UUID).trim();
		ResultSet rs = null;

		try {
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, userId.toString());
			ps.setString(2, computeDigest(password));
			rs = ps.executeQuery();

			return rs.next() ? rs.getBoolean(1) : null;
		}
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e) { }
            }
        }
	}

	@Override
	public long userCount() throws BackendStorageException {
		String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_COUNT).trim();
		ResultSet rs = null;

		try {
			rs = conn.createStatement().executeQuery(sqlQuery);
			rs.next();
			return rs.getLong(1);
		}
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e) { }
            }
        }
	}

	@Override
	public JSONArray listUsers(long offset, long count) throws BackendStorageException {
		String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_LIST).trim();
		JSONArray jaUsers = new JSONArray();
		ResultSet rs = null;

		try {
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setLong(1, offset);
			ps.setLong(2, count);
			rs = ps.executeQuery();

			while (rs.next()) {
				JSONObject joUser = new JSONObject();
				joUser.put("uuid", rs.getString("uuid"));
				joUser.put("screen_name", rs.getString("screen_name"));
				joUser.put("profile", new JSONObject(rs.getString("profile")));
				jaUsers.put(joUser);
			}

			return jaUsers;
		}
		catch (Exception e) {
		    logger.log(Level.SEVERE, null, e);
		    throw new BackendStorageException(e);
		}
        finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e) { }
            }
        }
	}

    @Override
    public void addEvent(SourceType sourceType, UUID sourceId, Event eventCode, JSONObject description) throws BackendStorageException {
        String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_EVENT_ADD).trim();

        try {
            PreparedStatement ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, String.valueOf(sourceType.getSourceTypeCode()));
            ps.setString(2, sourceId.toString());
            ps.setInt(3, eventCode.getEventCode());
            ps.setString(4, description.toString());
            ps.executeUpdate();
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }
    }

    @Override
    public JSONObject addComponent(UUID userId, ExecutableComponentDescription component, Set<FileItem> contexts) throws BackendStorageException {
        String sqlQueryAddId = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_ADD_ID).trim();
        String sqlQueryAdd = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_ADD).trim();

        JSONObject joResult = new JSONObject();

        String origURI = component.getExecutableComponent().getURI();

        boolean isNewComponent = true;

        // Attempt to retrieve the component id for this component
        UUID componentId = getComponentId(origURI, userId);
        int version = -1;

        if (componentId != null) {
            isNewComponent = false;

            // Found the component, get the version number of the last revision
            version = getLastVersionNumberForComponent(componentId);

            // Sanity check
            if (version == -1)
                logger.warning(String.format("No version found for existing component %s (uri: %s)", componentId, origURI));
            else
                version++;
        } else
            // The component was not found, create a new id for it
            componentId = UUID.randomUUID();

        if (version == -1) version = 1;

        logger.fine(String.format("Adding component %s (uuid: %s, version %d, user: %s)", origURI, componentId, version, userId));

        // Make sure we have a place to put the components and contexts
        File fREPOSITORY_COMPONENTS = new File(REPOSITORY_FOLDER, "components");
        File fREPOSITORY_CONTEXTS = new File(REPOSITORY_FOLDER, "contexts");
        fREPOSITORY_COMPONENTS.mkdirs();
        fREPOSITORY_CONTEXTS.mkdirs();

        // Clear any existing contexts
        Set<RDFNode> compContexts = component.getContext();
        compContexts.clear();

        Model tmpModel = component.getExecutableComponent().getModel();

        for (FileItem context : contexts) {
            logger.finer("Processing context file: " + context.getName());
            try {
                File tmpFile = File.createTempFile("context", ".tmp", fREPOSITORY_CONTEXTS);
                try {
                    context.write(tmpFile);
                }
                catch (Exception e) {
                    throw new IOException(e);
                }

                // Compute the MD5 hash for the context file
                final String md5 = Crypto.getHexString(Crypto.createMD5Checksum(tmpFile));

                // Look for context files that have the same MD5 hash value
                File[] files = fREPOSITORY_CONTEXTS.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        return name.startsWith(md5 + "_");
                    }
                });

                if (files.length > 0) {
                    String foundFileName = files[0].getName().substring(md5.length() + 1);
                    logger.fine(String.format("Context file %s already exists (hashed to: %s), skipping it...",
                            context.getName(), foundFileName));
                    tmpFile.delete();
                } else {
                    File ctxFile = new File(fREPOSITORY_CONTEXTS, md5 + "_" + context.getName());
                    if (!tmpFile.renameTo(ctxFile)) {
                        String errorMsg = String.format("Could not rename context file %s to %s", tmpFile, ctxFile);
                        logger.severe(errorMsg);
                        throw new BackendStorageException(errorMsg);
                    }
                    logger.finer("Context file saved as " + ctxFile);
                }

                compContexts.add(tmpModel.createResource(
                        String.format("context://localhost/%s/%s", md5, context.getName())));
            }
            catch (IOException e) {
                logger.log(Level.SEVERE, null, e);
                throw new BackendStorageException(e);
            }
        }

        File fCompFolder = new File(fREPOSITORY_COMPONENTS, componentId.toString());
        fCompFolder.mkdir();

        try {
            // Write the component descriptor file for this version
            Model compModel = component.getModel();
            File fCompDescriptor = new File(fCompFolder, String.valueOf(version) + ".ttl");
            logger.finer("Saving component descriptor as " + fCompDescriptor);
            FileOutputStream fos = new FileOutputStream(fCompDescriptor);
            compModel.write(fos, "TURTLE");
            fos.close();
        }
        catch (IOException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }

        try {
            // Add the new component version
            PreparedStatement psAdd = conn.prepareStatement(sqlQueryAdd);
            psAdd.setString(1, componentId.toString());
            psAdd.setInt(2, version);
            psAdd.executeUpdate();

            if (isNewComponent) {
                // Add the mapping from origURI, userID to component UUID
                PreparedStatement psAddId = conn.prepareStatement(sqlQueryAddId);
                psAddId.setString(1, origURI);
                psAddId.setString(2, userId.toString());
                psAddId.setString(3, componentId.toString());
                psAddId.executeUpdate();
            }
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }

        try {
            joResult.put("uuid", componentId.toString());
            joResult.put("version", version);
        }
        catch (JSONException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }

        return joResult;
    }

    @Override
    public Model getComponent(UUID componentId, int version) throws BackendStorageException {
        String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_GET_COMPONENT_STATE_UUID_VERSION).trim();
        ResultSet rs = null;
        boolean deleted;

        try {
            PreparedStatement ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, componentId.toString());
            ps.setString(2, String.valueOf(version));
            rs = ps.executeQuery();

            if (rs.next())
                deleted = rs.getBoolean(1);
            else
                // Component not found
                return null;
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e) { }
            }
        }

        if (deleted)
            logger.warning(String.format("Retrieving DELETED component (id: %s, version: %d)", componentId, version));

        File fREPOSITORY_COMPONENTS = new File(REPOSITORY_FOLDER, "components");
        File compFile = new File(fREPOSITORY_COMPONENTS, componentId.toString() + File.separator + String.valueOf(version) + ".ttl");

        try {
            return ModelUtils.getModel(compFile.toURI(), null);
        }
        catch (IOException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }
    }

    @Override
    public InputStream getContextInputStream(final String contextId) throws BackendStorageException {
        File fREPOSITORY_CONTEXTS = new File(REPOSITORY_FOLDER, "contexts");

        File[] files = fREPOSITORY_CONTEXTS.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith(contextId + "_");
            }
        });

        if (files.length == 0) return null;

        try {
            return new FileInputStream(files[0]);
        }
        catch (FileNotFoundException e) {
            return null;
        }
    }

	//-------------------------------------------------------------------------------------


    /**
     * Returns the component id for the component matching the specified baseUri and userId
     *
     * @param baseUri The component's base uri
     * @param userId The user id
     * @return The component id, or null if one can't be found
     * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
     */
    protected UUID getComponentId(String baseUri, UUID userId) throws BackendStorageException {
        String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_GET_COMPONENT_ID_BASEURI_USER).trim();
        ResultSet rs = null;

        try {
            PreparedStatement ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, baseUri);
            ps.setString(2, userId.toString());
            rs = ps.executeQuery();

            return rs.next() ? UUID.fromString(rs.getString(1)) : null;
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e) { }
            }
        }
    }

    /**
     * Returns the highest version number for a component
     *
     * @param componentId The component id
     * @return The version number, or -1 if no version was found
     * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
     */
    protected int getLastVersionNumberForComponent(UUID componentId) throws BackendStorageException {
        String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_GET_LAST_VERSION_NUMBER).trim();
        ResultSet rs = null;

        try {
            PreparedStatement ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, componentId.toString());
            rs = ps.executeQuery();

            return rs.next() ? rs.getInt(1) : -1;
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }
        finally {
            if (rs != null) {
                try {
                    rs.close();
                }
                catch (SQLException e) { }
            }
        }
    }

    /**
     * Computes a SHA1 digest for a given string
     *
     * @param string The string
     * @return The SHA1 digest
     */
    protected String computeDigest(String string) {
        try {
            return Crypto.getSHA1Hash(string);
        }
        catch (NoSuchAlgorithmException e) {
            logger.log(Level.WARNING, null, e);
        }
        catch (UnsupportedEncodingException e) {
            logger.log(Level.WARNING, null, e);
        }

        return null;
    }
}

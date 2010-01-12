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
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_GET_ID_ORIGURI_USER;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_GET_LAST_VERSION_NUMBER;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_GET_STATE_UUID_VERSION;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_EVENT_ADD;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_FLOW_ADD;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_FLOW_ADD_ID;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_FLOW_GET_ID_ORIGURI_USER;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_FLOW_GET_LAST_VERSION_NUMBER;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_FLOW_GET_STATE_UUID_VERSION;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_ADD;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_COUNT;
import static org.seasr.central.properties.SCDBProperties.ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_DELETED;
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
import static org.seasr.central.properties.SCProperties.ORG_SEASR_CENTRAL_STORAGE_REPOSITORY_LOCATION;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
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

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.FlowDescription;
import org.seasr.central.storage.BackendStorageLink;
import org.seasr.central.storage.Event;
import org.seasr.central.storage.SourceType;
import org.seasr.central.storage.exceptions.BackendStorageException;
import org.seasr.central.storage.exceptions.InactiveUserException;
import org.seasr.central.ws.restlets.Tools.GenericExceptionFormatter;
import org.seasr.meandre.support.generic.crypto.Crypto;
import org.seasr.meandre.support.generic.io.ModelUtils;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.mchange.v2.c3p0.ComboPooledDataSource;

import de.schlichtherle.io.FileInputStream;

/**
 * The SQLite driver to create a backend storage link
 *
 * @author Xavier Llora
 * @author Boris Capitanu
 */

public class SQLiteLink implements BackendStorageLink {

    /** The root folder where the components and contexts are stored */
    private static String REPOSITORY_LOCATION = null;

    /** Date parser for the DATETIME SQLite datatype (Note: use together with 'localtime' in SQL query to retrieve correct timestamp) */
    private static final SimpleDateFormat SQLITE_DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	//-------------------------------------------------------------------------------------

    /** The DB logger */
    private static Logger logger;

	/** The properties available */
	Properties properties = null;

	/** The DB connection pool */
	private final ComboPooledDataSource dataSource = new ComboPooledDataSource();

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
	    Connection conn = null;
		try {
			// Retain the properties
			this.properties = properties;

			// Set the logging level
			logger.setLevel(Level.parse(properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_LOGLEVEL, Level.ALL.getName())));

			// Get the repository location and make sure it exists
			if (properties.containsKey(ORG_SEASR_CENTRAL_STORAGE_REPOSITORY_LOCATION))
			    REPOSITORY_LOCATION = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_REPOSITORY_LOCATION);
			else
			    throw new BackendStorageException("Missing repository location configuration entry: " +
			            ORG_SEASR_CENTRAL_STORAGE_REPOSITORY_LOCATION);

			new File(REPOSITORY_LOCATION).mkdirs();

			// Initialize the connection
			Class.forName(properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_DRIVER));

			// Set up the DB connection pool (c3p0)
			dataSource.setDriverClass(properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_DRIVER));
			dataSource.setJdbcUrl(properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_URL));
			dataSource.setUser(properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_USER));
			dataSource.setPassword(properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_PASSWORD));

			conn = dataSource.getConnection();
			conn.setAutoCommit(false);

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

			conn.commit();
		}
        catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Cannot load the DB driver", e);
            throw new BackendStorageException(e);
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            rollbackTransaction(conn);
            throw new BackendStorageException(e);
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Cannot load the DB driver", e);
            throw new BackendStorageException(e);
        }
        finally {
            releaseConnection(conn);
        }
	}

	//-------------------------------------------------------------------------------------

	@Override
	public UUID addUser(String userName, String password, JSONObject profile) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_ADD).trim();

	    UUID uuid = UUID.randomUUID();

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
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
		finally {
		    releaseConnection(conn);
		}
	}

	@Override
	public void removeUser(UUID userId) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_REMOVE_UUID).trim();

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, userId.toString());
			ps.executeUpdate();
		}
		catch (SQLException e) {
			logger.log(Level.SEVERE, null, e);
			throw new BackendStorageException(e);
		}
		finally {
		    releaseConnection(conn);
		}
	}

	@Override
	public void removeUser(String userName) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_REMOVE_SCREEN_NAME).trim();

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, userName);
			ps.executeUpdate();
		}
		catch (SQLException e) {
		    logger.log(Level.SEVERE, null, e);
		    throw new BackendStorageException(e);
		}
        finally {
            releaseConnection(conn);
        }
	}

	@Override
	public void updateUserPassword(UUID userId, String password) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PASSWORD_UUID).trim();

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, computeDigest(password));
			ps.setString(2, userId.toString());
			ps.executeUpdate();
		}
		catch (SQLException e) {
			logger.log(Level.SEVERE, null, e);
			throw new BackendStorageException(e);
		}
		finally {
            releaseConnection(conn);
        }
	}

	@Override
	public void updateUserPassword(String userName, String password) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PASSWORD_SCREEN_NAME).trim();

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, computeDigest(password));
			ps.setString(2, userName);
			ps.executeUpdate();
		}
		catch (SQLException e) {
		    logger.log(Level.SEVERE, null, e);
		    throw new BackendStorageException(e);
		}
		finally {
            releaseConnection(conn);
        }
	}

	@Override
	public void updateProfile(UUID userId, JSONObject profile) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PROFILE_UUID).trim();

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, profile.toString());
			ps.setString(2, userId.toString());
			ps.executeUpdate();
		}
		catch (SQLException e) {
		    logger.log(Level.SEVERE, null, e);
		    throw new BackendStorageException(e);
		}
		finally {
            releaseConnection(conn);
        }
	}

	@Override
	public void updateProfile(String userName, JSONObject profile) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PROFILE_SCREEN_NAME).trim();

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
			PreparedStatement ps = conn.prepareStatement(sqlQuery);
			ps.setString(1, profile.toString());
			ps.setString(2, userName);
			ps.executeUpdate();
		}
		catch (SQLException e) {
		    logger.log(Level.SEVERE, null, e);
		    throw new BackendStorageException(e);
		}
		finally {
            releaseConnection(conn);
        }
	}

	@Override
	public UUID getUserId(String userName) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_UUID).trim();
	    Connection conn = null;
	    ResultSet rs = null;

		try {
            conn = dataSource.getConnection();
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
            releaseConnection(conn, rs);
		}
	}

	@Override
	public String getUserScreenName(UUID userId) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_SCREEN_NAME).trim();
        Connection conn = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
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
            releaseConnection(conn, rs);
		}
	}

	@Override
	public JSONObject getUserProfile(String userName) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_PROFILE_SCREEN_NAME).trim();
        Connection conn = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
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
            releaseConnection(conn, rs);
		}
	}

	@Override
	public JSONObject getUserProfile(UUID userId) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_PROFILE_UUID).trim();
        Connection conn = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
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
            releaseConnection(conn, rs);
		}
	}

	@Override
	public Date getUserCreationTime(String userName) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_CREATEDAT_SCREEN_NAME).trim();
        Connection conn = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
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
            releaseConnection(conn, rs);
		}
	}

	@Override
	public Date getUserCreationTime(UUID userId) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_CREATEDAT_UUID).trim();
        Connection conn = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
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
            releaseConnection(conn, rs);
		}
	}

	@Override
	public Boolean isUserPasswordValid(String userName, String password) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_VALID_PASSWORD_SCREEN_NAME).trim();
        Connection conn = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
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
            releaseConnection(conn, rs);
        }
	}

	@Override
	public Boolean isUserPasswordValid(UUID userId, String password) throws BackendStorageException {
	    String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_VALID_PASSWORD_UUID).trim();
        Connection conn = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
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
            releaseConnection(conn, rs);
        }
	}

	@Override
	public long userCount() throws BackendStorageException {
		String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_COUNT).trim();
        Connection conn = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
			rs = conn.createStatement().executeQuery(sqlQuery);
			rs.next();

			return rs.getLong(1);
		}
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }
        finally {
            releaseConnection(conn, rs);
        }
	}

	@Override
	public JSONArray listUsers(long offset, long count) throws BackendStorageException {
		String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_LIST).trim();
		JSONArray jaUsers = new JSONArray();
        Connection conn = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
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
            releaseConnection(conn, rs);
        }
	}

    @Override
    public void addEvent(SourceType sourceType, UUID sourceId, Event eventCode, JSONObject description) throws BackendStorageException {
        String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_EVENT_ADD).trim();

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
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
        finally {
            releaseConnection(conn);
        }
    }

    @Override
    public JSONObject addComponent(UUID userId, ExecutableComponentDescription component, Set<URL> contexts, boolean copyContextFiles) throws BackendStorageException {
        String sqlQueryAddId = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_ADD_ID).trim();
        String sqlQueryAdd = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_ADD).trim();

        JSONObject joResult = new JSONObject();
        String origURI = component.getExecutableComponent().getURI();
        boolean isNewComponent = true;

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            if (!isUserActive(userId, conn))
                throw new InactiveUserException(userId);

            // Attempt to retrieve the component id for this component
            UUID componentId = getComponentId(origURI, userId, conn);
            int version = -1;

            if (componentId != null) {
                isNewComponent = false;

                // Found the component, get the version number of the last revision
                version = getLastVersionNumberForComponent(componentId, conn);

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

            // Make sure we have a place to store the components
            File fREPOSITORY_COMPONENTS = new File(REPOSITORY_LOCATION, "components");
            fREPOSITORY_COMPONENTS.mkdirs();

            File fCompFolder = new File(fREPOSITORY_COMPONENTS, componentId.toString());
            fCompFolder.mkdir();

            // Save the context files to the repository and update the component descriptor
            saveAndRewriteComponentContexts(component, contexts, copyContextFiles);

            // Write the component descriptor file for this version
            Model compModel = component.getModel();
            File fCompDescriptor = new File(fCompFolder, String.valueOf(version) + ".ttl");
            logger.finer("Saving component descriptor as " + fCompDescriptor);
            FileOutputStream fos = new FileOutputStream(fCompDescriptor);
            compModel.write(fos, "TURTLE");
            fos.close();

            // Add the new component version
            PreparedStatement psAdd = conn.prepareStatement(sqlQueryAdd);
            psAdd.setString(1, componentId.toString());
            psAdd.setInt(2, version);
            psAdd.executeUpdate();

            // for testing parallelism only
            if (contexts.size() > 1) {
                logger.info("Sleeping 20 seconds...");
                Thread.sleep(20000);
                logger.info("Waking up");
            }

            if (isNewComponent) {
                // Add the mapping from origURI, userID to component UUID
                PreparedStatement psAddId = conn.prepareStatement(sqlQueryAddId);
                psAddId.setString(1, origURI);
                psAddId.setString(2, userId.toString());
                psAddId.setString(3, componentId.toString());
                psAddId.executeUpdate();
            }

            joResult.put("uuid", componentId.toString());
            joResult.put("version", version);

            conn.commit();
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            rollbackTransaction(conn);
            throw new BackendStorageException(e);
        }
        finally {
            releaseConnection(conn);
        }

        return joResult;
    }

    @Override
    public Model getComponent(UUID componentId, int version) throws BackendStorageException {
        String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_GET_STATE_UUID_VERSION).trim();
        boolean deleted;
        Connection conn = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
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
            releaseConnection(conn, rs);
        }

        if (deleted)
            logger.warning(String.format("Retrieving DELETED component (id: %s, version: %d)", componentId, version));

        File fREPOSITORY_COMPONENTS = new File(REPOSITORY_LOCATION, "components");
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
        File fREPOSITORY_CONTEXTS = new File(REPOSITORY_LOCATION, "contexts");

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

    @Override
    public JSONObject addFlow(UUID userId, FlowDescription flow) throws BackendStorageException {
        String sqlQueryAddId = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_FLOW_ADD_ID).trim();
        String sqlQueryAdd = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_FLOW_ADD).trim();

        JSONObject joResult = new JSONObject();
        String origURI = flow.getFlowComponent().getURI();
        boolean isNewFlow = true;

        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            if (!isUserActive(userId, conn))
                throw new InactiveUserException(userId);

            // Attempt to retrieve the flow id for this flow
            UUID flowId = getFlowId(origURI, userId, conn);
            int version = -1;

            if (flowId != null) {
                isNewFlow = false;

                // Found the flow, get the version number of the last revision
                version = getLastVersionNumberForFlow(flowId, conn);

                // Sanity check
                if (version == -1)
                    logger.warning(String.format("No version found for existing flow %s (uri: %s)", flowId, origURI));
                else
                    version++;
            } else
                // The component was not found, create a new id for it
                flowId = UUID.randomUUID();

            if (version == -1) version = 1;

            logger.fine(String.format("Adding flow %s (uuid: %s, version %d, user: %s)", origURI, flowId, version, userId));

            // Make sure we have a place to put the flows
            File fREPOSITORY_FLOWS = new File(REPOSITORY_LOCATION, "flows");
            fREPOSITORY_FLOWS.mkdir();

            File fFlowFolder = new File(fREPOSITORY_FLOWS, flowId.toString());
            fFlowFolder.mkdir();

            // Write the flow descriptor file for this version
            Model flowModel = flow.getModel();
            File fFlowDescriptor = new File(fFlowFolder, String.valueOf(version) + ".ttl");
            logger.finer("Saving flow descriptor as " + fFlowDescriptor);
            FileOutputStream fos = new FileOutputStream(fFlowDescriptor);
            flowModel.write(fos, "TURTLE");
            fos.close();

            // Add the new flow version
            PreparedStatement psAdd = conn.prepareStatement(sqlQueryAdd);
            psAdd.setString(1, flowId.toString());
            psAdd.setInt(2, version);
            psAdd.executeUpdate();

            if (isNewFlow) {
                // Add the mapping from origURI, userID to component UUID
                PreparedStatement psAddId = conn.prepareStatement(sqlQueryAddId);
                psAddId.setString(1, origURI);
                psAddId.setString(2, userId.toString());
                psAddId.setString(3, flowId.toString());
                psAddId.executeUpdate();
            }

            joResult.put("uuid", flowId.toString());
            joResult.put("version", version);

            conn.commit();
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            rollbackTransaction(conn);
            throw new BackendStorageException(e);
        }
        finally {
            releaseConnection(conn);
        }

        return joResult;
    }

    @Override
    public Model getFlow(UUID flowId, int version) throws BackendStorageException {
        String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_FLOW_GET_STATE_UUID_VERSION).trim();
        boolean deleted;
        Connection conn = null;
        ResultSet rs = null;

        try {
            conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, flowId.toString());
            ps.setString(2, String.valueOf(version));
            rs = ps.executeQuery();

            if (rs.next())
                deleted = rs.getBoolean(1);
            else
                // Flow not found
                return null;
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }
        finally {
            releaseConnection(conn, rs);
        }

        if (deleted)
            logger.warning(String.format("Retrieving DELETED flow (id: %s, version: %d)", flowId, version));

        File fREPOSITORY_FLOWS = new File(REPOSITORY_LOCATION, "flows");
        File flowFile = new File(fREPOSITORY_FLOWS, flowId.toString() + File.separator + String.valueOf(version) + ".ttl");

        try {
            return ModelUtils.getModel(flowFile.toURI(), null);
        }
        catch (IOException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }
    }

	//-------------------------------------------------------------------------------------

    /**
     * Saves the component context files to the repository and updates the component descriptor
     *
     * @param component The component
     * @param contexts The contexts
     * @param copyContextFiles True if file:/// references should be copied to the repository, False if they should be moved
     * @throws URISyntaxException
     * @throws IOException
     */
    protected void saveAndRewriteComponentContexts(ExecutableComponentDescription component, Set<URL> contexts, boolean copyContextFiles)
        throws URISyntaxException, IOException {

        // Make sure we have a place to store the context files
        File fREPOSITORY_CONTEXTS = new File(REPOSITORY_LOCATION, "contexts");
        fREPOSITORY_CONTEXTS.mkdirs();

        // Clear any existing contexts
        Set<RDFNode> compContexts = component.getContext();
        compContexts.clear();

        Model tmpModel = component.getExecutableComponent().getModel();

        for (URL contextURL : contexts) {
            String ctxFileName = contextURL.toString().substring(contextURL.toString().lastIndexOf("/") + 1);
            if (ctxFileName.length() == 0) ctxFileName = "unnamed";

            logger.finer("Processing context file: " + ctxFileName);

            File tmpFile;

            if (contextURL.getProtocol().equals("file") && !copyContextFiles)
                tmpFile = new File(contextURL.toURI());
            else {
                tmpFile = File.createTempFile("context", ".tmp", fREPOSITORY_CONTEXTS);
                // TODO: We should probably use a timeout in case the URL is not responding to prevent hangs
                FileUtils.copyURLToFile(contextURL, tmpFile);
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
                        ctxFileName, foundFileName));
                tmpFile.delete();
            } else {
                File ctxFile = new File(fREPOSITORY_CONTEXTS, md5 + "_" + ctxFileName);
                // We need to synchronize access to ctxFile so that multiple threads don't try to
                // write to the same context file at the same time. Using FileLock seems to be
                // discouraged by the JavaDocs. Using createNewFile() seems to also be discouraged
                // but the operation is atomic and for now I think it will do the trick
                if (ctxFile.createNewFile()) {
                    if (!tmpFile.renameTo(ctxFile)) {
                        ctxFile.delete();
                        throw new IOException(String.format("Could not rename context file %s to %s", tmpFile, ctxFile));
                    }
                    logger.finer("Context file saved as " + ctxFile);
                } else
                    logger.info(String.format("Context file %s created in another thread. Skipping it...", ctxFile));
            }

            compContexts.add(tmpModel.createResource(String.format("context://localhost/%s/%s", md5, ctxFileName)));
        }
    }

    /**
     * Checks whether a user is active (i.e. user exists and is not deleted) or not
     *
     * @param userId The user id
     * @param conn The DB transaction connection to use
     * @return Tue if user exists and is not deleted, False if user exists and is marked as deleted, null if user does not exist
     * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
     */
    protected Boolean isUserActive(UUID userId, Connection conn) throws BackendStorageException {
        String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_DELETED).trim();
        ResultSet rs = null;

        try {
            PreparedStatement ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, userId.toString());
            rs = ps.executeQuery();

            return rs.next() ? !rs.getBoolean(1) : null;
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }
        finally {
            closeResultSet(rs);
        }
    }

    /**
     * Returns the component id for the component matching the specified origUri and userId
     *
     * @param origUri The component's original uri
     * @param userId The user id
     * @param conn The DB transaction connection to use
     * @return The component id, or null if one can't be found
     * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
     */
    protected UUID getComponentId(String origUri, UUID userId, Connection conn) throws BackendStorageException {
        String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_COMPONENT_GET_ID_ORIGURI_USER).trim();
        ResultSet rs = null;

        try {
            PreparedStatement ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, origUri);
            ps.setString(2, userId.toString());
            rs = ps.executeQuery();

            return rs.next() ? UUID.fromString(rs.getString(1)) : null;
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }
        finally {
            closeResultSet(rs);
        }
    }

    /**
     * Returns the highest version number for a component
     *
     * @param componentId The component id
     * @param conn The DB transaction connection to use
     * @return The version number, or -1 if no version was found
     * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
     */
    protected int getLastVersionNumberForComponent(UUID componentId, Connection conn) throws BackendStorageException {
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
            closeResultSet(rs);
        }
    }

    /**
     * Returns the flow id for the flow matching the specified origUri and userId
     *
     * @param origUri The flow's original uri
     * @param userId The user id
     * @param conn The DB transaction connection to use
     * @return The flow id, or null if one can't be found
     * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
     */
    protected UUID getFlowId(String origUri, UUID userId, Connection conn) throws BackendStorageException {
        String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_FLOW_GET_ID_ORIGURI_USER).trim();
        ResultSet rs = null;

        try {
            PreparedStatement ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, origUri);
            ps.setString(2, userId.toString());
            rs = ps.executeQuery();

            return rs.next() ? UUID.fromString(rs.getString(1)) : null;
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }
        finally {
            closeResultSet(rs);
        }
    }

    /**
     * Returns the highest version number for a flow
     *
     * @param flowId The flow id
     * @param conn The DB transaction connection to use
     * @return The version number, or -1 if no version was found
     * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
     */
    protected int getLastVersionNumberForFlow(UUID flowId, Connection conn) throws BackendStorageException {
        String sqlQuery = properties.getProperty(ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_FLOW_GET_LAST_VERSION_NUMBER).trim();
        ResultSet rs = null;

        try {
            PreparedStatement ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, flowId.toString());
            rs = ps.executeQuery();

            return rs.next() ? rs.getInt(1) : -1;
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStorageException(e);
        }
        finally {
            closeResultSet(rs);
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

    /**
     * Rolls back the last DB transaction for a connection
     *
     * @param connection The connection
     * @return True if success / False otherwise
     */
    protected boolean rollbackTransaction(Connection connection) {
        if (connection == null) return false;

        try {
            connection.rollback();
            return true;
        }
        catch (SQLException e) {
            logger.log(Level.WARNING, null, e);
            return false;
        }
    }

    /**
     * Returns a connection back to the connection pool
     *
     * @param connection The connection
     * @param resultSet (Optional) Any ResultSet(s) that need to be closed before the connection is released
     */
    private void releaseConnection(Connection connection, ResultSet... resultSet) {
        if (resultSet != null)
            for (ResultSet rs : resultSet)
                closeResultSet(rs);

        if (connection != null) {
            try {
                connection.close();
            }
            catch (Exception e) {
                logger.log(Level.WARNING, null, e);
            }
        }
    }

    /**
     * Closes a ResultSet
     *
     * @param rs The ResultSet
     */
    protected void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            }
            catch (SQLException e) {
                logger.log(Level.WARNING, null, e);
            }
        }
    }
}

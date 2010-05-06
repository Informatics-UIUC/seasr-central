/*
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
 */

package org.seasr.central.storage.db;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.ExecutableComponentInstanceDescription;
import org.meandre.core.repository.FlowDescription;
import org.meandre.core.utils.vocabulary.RepositoryVocabulary;
import org.seasr.central.storage.BackendStoreLink;
import org.seasr.central.storage.SCError;
import org.seasr.central.storage.SCEvent;
import org.seasr.central.storage.SCRole;
import org.seasr.central.storage.db.properties.DBProperties;
import org.seasr.central.storage.exceptions.*;
import org.seasr.central.util.SCLogFormatter;
import org.seasr.central.ws.restlets.ComponentContext;
import org.seasr.meandre.support.generic.crypto.Crypto;
import org.seasr.meandre.support.generic.io.ModelUtils;
import org.seasr.meandre.support.generic.util.UUIDUtils;

import java.beans.PropertyVetoException;
import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.seasr.central.util.Tools.*;

/**
 * Generic SQL backend store link driver
 *
 * @author Xavier Llora
 * @author Boris Capitanu
 */
public class SQLLink implements BackendStoreLink {

    /** Date parser for the DATETIME SQL datatype */
    private static final SimpleDateFormat SQL_DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final Pattern REGEX_UUID_VERSION =
            Pattern.compile(".*([a-f\\d]{8}(?:-[a-f\\d]{4}){3}-[a-f\\d]{12})/(\\d+)/?$");

    protected static final Logger logger;

    /** The DB configuration properties */
    private Properties properties = null;

    /** The DB connection pool manager */
    private final ComboPooledDataSource dataSource = new ComboPooledDataSource();


    static {
        logger = Logger.getLogger(SQLLink.class.getName());
        logger.setUseParentHandlers(false);
    }

    @Override
    public void init(Properties properties) throws BackendStoreException {
        // Setup logging

        // Remove all existing handlers
        for (Handler handler : logger.getHandlers())
            logger.removeHandler(handler);

        Level logLevel = Level.parse(properties.getProperty(DBProperties.LOG_LEVEL, Level.OFF.getName()).trim());
        if (logLevel != Level.OFF) {
            String logFile = properties.getProperty(DBProperties.LOG_FILE, "").trim();
            if (logFile.length() > 0) {
                try {
                    FileHandler fileHandler = new FileHandler(logFile, true);
                    fileHandler.setFormatter(new SCLogFormatter());
                    fileHandler.setLevel(logLevel);
                    logger.addHandler(fileHandler);
                }
                catch (IOException e) {
                    throw new BackendStoreException("Error creating log file", e);
                }
            }
        }
        logger.setLevel(logLevel);

        this.properties = properties;

        // Prepare the SQL driver
        String dbDriverClass = properties.getProperty(DBProperties.DRIVER, "").trim();
        String dbJDBCUrl = properties.getProperty(DBProperties.JDBC_URL, "").trim();
        String dbUser = properties.getProperty(DBProperties.USER, "").trim();
        String dbPassword = properties.getProperty(DBProperties.PASSWORD, "").trim();

        if (dbDriverClass.length() == 0 || dbJDBCUrl.length() == 0)
            throw new BackendStoreException("Incomplete DB configuration! Need to supply both a driver and JDBC url.");

        try {
            Class.forName(dbDriverClass);

            // Set up the DB connection pool manager (c3p0)
            dataSource.setDriverClass(dbDriverClass);
            dataSource.setJdbcUrl(dbJDBCUrl);
            dataSource.setUser(dbUser);
            dataSource.setPassword(dbPassword);
        }
        catch (PropertyVetoException e) {
            throw new BackendStoreException(e);
        }
        catch (ClassNotFoundException e) {
            throw new BackendStoreException("Cannot load the database driver: " + dbDriverClass, e);
        }

        Connection conn = null;
        Statement stmt = null;
        try {
            // Initialize the database (create tables, etc.)
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            stmt = conn.createStatement();

            // Create the authentication schema
            for (String sql : parseSQLString(properties.getProperty(DBProperties.AUTH_SCHEMA)))
                stmt.executeUpdate(sql);

            // Populate the default roles
            PreparedStatement ps = conn.prepareStatement("INSERT IGNORE INTO sc_role (role_id, name) VALUES (?, ?);");
            try {
                for (SCRole role : SCRole.values()) {
                    ps.setInt(1, role.getRoleId());
                    ps.setString(2, role.name());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            finally {
                closeStatement(ps);
                ps = null;
            }

            // Create the admin user
            stmt.executeUpdate(String.format(
                    "INSERT IGNORE INTO sc_user (user_uuid, screen_name, password, created_at, profile) " +
                    "VALUES (%d, 'admin', '%s', NOW(), '{}');",
                    UUIDUtils.toBigInteger(ADMIN_UUID), computePasswordDigest("admin")
            ));

            // Assign the 'admin' user to the 'admin' role
            stmt.executeUpdate(String.format(
                    "INSERT IGNORE INTO sc_user_role (user_uuid, role_id) VALUES (%d, %d);",
                    UUIDUtils.toBigInteger(ADMIN_UUID), SCRole.ADMIN.getRoleId()
            ));

            // Create the main SC schema
            for (String sql : parseSQLString(properties.getProperty(DBProperties.SC_SCHEMA)))
                stmt.executeUpdate(sql);

            // Create the "public" group
            stmt.executeUpdate(String.format(
                    "INSERT IGNORE INTO sc_group (group_uuid, name, created_at, profile) " +
                    "VALUES (%d, 'public', NOW(), '{}');",
                    UUIDUtils.toBigInteger(PUBLIC_GROUP)
            ));

            // Populate the default event codes
            ps = conn.prepareStatement("INSERT IGNORE INTO sc_event_code (evt_code, description) VALUES (?, ?);");
            try {
                for (SCEvent event : SCEvent.values()) {
                    ps.setInt(1, event.getEventCode());
                    ps.setString(2, event.name());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            finally {
                closeStatement(ps);
                ps = null;
            }

            // Add the SC application-specific errors
            ps = conn.prepareStatement("INSERT IGNORE INTO sc_error (err_code, err_msg) VALUES (?, ?);");
            try {
                for (SCError error : SCError.values()) {
                    ps.setInt(1, error.getErrorCode());
                    ps.setString(2, error.getErrorMessage());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            finally {
                closeStatement(ps);
                ps = null;
            }

            conn.commit();
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            rollbackTransaction(conn);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, stmt);
        }
    }

    @Override
    public String getErrorMessage(SCError error) throws BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_ERROR_MSG).trim();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setInt(1, error.getErrorCode());
            ResultSet rs = ps.executeQuery();

            return rs.next() ? rs.getString(1) : null;
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public JSONArray listRoles(long offset, long count) throws BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_ROLE_LIST).trim();
        JSONArray jaRoles = new JSONArray();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setLong(1, offset);
            ps.setLong(2, count);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                JSONObject joRole = new JSONObject();
                joRole.put("role", rs.getString("role_name"));
                jaRoles.put(joRole);
            }

            return jaRoles;
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public boolean hasRole(String roleName) throws BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_ROLE_EXISTS).trim();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, roleName);
            ResultSet rs = ps.executeQuery();

            return rs.next();
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public UUID addUser(String userName, String password, JSONObject profile) throws BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_USER_ADD).trim();
        Connection conn = null;
        PreparedStatement ps = null;
        UUID userId = UUID.randomUUID();
        BigInteger uid = UUIDUtils.toBigInteger(userId);

        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(uid));
            ps.setString(2, userName);
            ps.setString(3, computePasswordDigest(password));
            ps.setString(4, profile.toString());
            ps.executeUpdate();

            // Record this event
            addEvent(SCEvent.USER_CREATED, uid, null, null, null, null, conn);

            conn.commit();

            return userId;
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            rollbackTransaction(conn);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public void removeUser(UUID userId) throws UserNotFoundException, BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_USER_REMOVE).trim();
        Connection conn = null;
        PreparedStatement ps = null;
        BigInteger uid = UUIDUtils.toBigInteger(userId);

        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            if (!Boolean.TRUE.equals(isUserActive(uid, conn)))
                throw new UserNotFoundException(userId);

            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(uid));
            ps.executeUpdate();

            // Record the event
            addEvent(SCEvent.USER_DELETED, uid, null, null, null, null, conn);

            conn.commit();
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            rollbackTransaction(conn);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public void updateUserPassword(UUID userId, String password) throws UserNotFoundException, BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_USER_UPDATE_PASSWORD).trim();
        Connection conn = null;
        PreparedStatement ps = null;
        BigInteger uid = UUIDUtils.toBigInteger(userId);

        try {
            conn = dataSource.getConnection();

            if (!Boolean.TRUE.equals(isUserActive(uid, conn)))
                throw new UserNotFoundException(userId);

            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, computePasswordDigest(password));
            ps.setBigDecimal(2, new BigDecimal(uid));
            ps.executeUpdate();
            ps.close();
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public void updateUserProfile(UUID userId, JSONObject profile) throws UserNotFoundException, BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_USER_UPDATE_PROFILE).trim();
        Connection conn = null;
        PreparedStatement ps = null;
        BigInteger uid = UUIDUtils.toBigInteger(userId);

        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            if (!Boolean.TRUE.equals(isUserActive(uid, conn)))
                throw new UserNotFoundException(userId);

            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, profile.toString());
            ps.setBigDecimal(2, new BigDecimal(uid));
            ps.executeUpdate();

            // Record the event
            addEvent(SCEvent.USER_PROFILE_UPDATED, uid, null, null, null, profile, conn);

            conn.commit();
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            rollbackTransaction(conn);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public UUID getUserId(String userName) throws UserNotFoundException, BackendStoreException {
        if (userName == null) return null;

        String sqlQuery = properties.getProperty(DBProperties.Q_USER_GET_UUID).trim();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, userName);
            ResultSet rs = ps.executeQuery();

            if (rs.next())
                return UUIDUtils.fromBigInteger(rs.getBigDecimal(1).toBigInteger());
            else
                throw new UserNotFoundException(userName);
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public String getUserScreenName(UUID userId) throws UserNotFoundException, BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_USER_GET_SCREENNAME).trim();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(UUIDUtils.toBigInteger(userId)));
            ResultSet rs = ps.executeQuery();

            if (rs.next())
                return rs.getString(1);
            else
                throw new UserNotFoundException(userId);
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public JSONObject getUserProfile(UUID userId) throws UserNotFoundException, BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_USER_GET_PROFILE).trim();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(UUIDUtils.toBigInteger(userId)));
            ResultSet rs = ps.executeQuery();

            if (rs.next())
                return new JSONObject(rs.getString(1));
            else
                throw new UserNotFoundException(userId);
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public Date getUserCreationTime(UUID userId) throws UserNotFoundException, BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_USER_GET_CREATEDAT).trim();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(UUIDUtils.toBigInteger(userId)));
            ResultSet rs = ps.executeQuery();

            if (rs.next())
                return SQL_DATE_PARSER.parse(rs.getString(1));
            else
                throw new UserNotFoundException(userId);
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public boolean isUserPasswordValid(UUID userId, String password) throws UserNotFoundException, BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_USER_PASSWORDVALID).trim();
        Connection conn = null;
        PreparedStatement ps = null;
        BigInteger uid = UUIDUtils.toBigInteger(userId);

        try {
            conn = dataSource.getConnection();

            if (!Boolean.TRUE.equals(isUserActive(uid, conn)))
                throw new UserNotFoundException(userId);

            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(uid));
            ps.setString(2, computePasswordDigest(password));
            ResultSet rs = ps.executeQuery();

            return rs.next();
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public long getUserCount() throws BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_USER_COUNT).trim();
        Connection conn = null;
        Statement stmt = null;

        try {
            conn = dataSource.getConnection();
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sqlQuery);
            rs.next();

            return rs.getLong(1);
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, stmt);
        }
    }

    @Override
    public JSONArray listUsers(long offset, long count) throws BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_USER_LIST).trim();
        JSONArray jaUsers = new JSONArray();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setLong(1, offset);
            ps.setLong(2, count);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                JSONObject joUser = new JSONObject();
                joUser.put("uuid", UUIDUtils.fromBigInteger(rs.getBigDecimal("user_uuid").toBigInteger()).toString());
                joUser.put("screen_name", rs.getString("screen_name"));
                joUser.put("profile", new JSONObject(rs.getString("profile")));
                jaUsers.put(joUser);
            }

            return jaUsers;
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public UUID createGroup(UUID userId, String groupName, JSONObject profile) throws UserNotFoundException, BackendStoreException {
        Connection conn = null;
        PreparedStatement ps = null;
        UUID groupId = UUID.randomUUID();
        BigInteger uid = UUIDUtils.toBigInteger(userId);
        BigInteger gid = UUIDUtils.toBigInteger(groupId);

        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            if (!Boolean.TRUE.equals(isUserActive(uid, conn)))
                throw new UserNotFoundException(userId);

            // Insert the group into the sc_group table
            ps = conn.prepareStatement(properties.getProperty(DBProperties.Q_GROUP_ADD).trim());
            ps.setBigDecimal(1, new BigDecimal(gid));
            ps.setString(2, groupName);
            ps.setString(3, profile.toString());
            ps.executeUpdate();
            ps.close();

            // Join the user to the group and set the ownership
            ps = conn.prepareStatement(properties.getProperty(DBProperties.Q_GROUP_MEMBERS_ADD).trim());
            ps.setBigDecimal(1, new BigDecimal(uid));
            ps.setBigDecimal(2, new BigDecimal(gid));
            ps.setInt(3, SCRole.ADMIN.getRoleId());
            ps.executeUpdate();

            // Record this event
            addEvent(SCEvent.GROUP_CREATED, uid, gid, null, null, null, conn);

            conn.commit();

            return groupId;
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            rollbackTransaction(conn);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public JSONArray listGroups(long offset, long count) throws BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_GROUP_LIST).trim();
        JSONArray jaGroups = new JSONArray();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setLong(1, offset);
            ps.setLong(2, count);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                JSONObject joGroup = new JSONObject();
                joGroup.put("uuid", UUIDUtils.fromBigInteger(rs.getBigDecimal("group_uuid").toBigInteger()).toString());
                joGroup.put("name", rs.getString("name"));
                joGroup.put("profile", new JSONObject(rs.getString("profile")));
                jaGroups.put(joGroup);
            }

            return jaGroups;
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public UUID getGroupId(String groupName) throws GroupNotFoundException, BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_GROUP_GET_UUID).trim();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, groupName);
            ResultSet rs = ps.executeQuery();

            if (rs.next())
                return UUIDUtils.fromBigInteger(rs.getBigDecimal(1).toBigInteger());
            else
                throw new GroupNotFoundException(groupName);
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public String getGroupName(UUID groupId) throws GroupNotFoundException, BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_GROUP_GET_NAME).trim();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(UUIDUtils.toBigInteger(groupId)));
            ResultSet rs = ps.executeQuery();

            if (rs.next())
                return rs.getString(1);
            else
                throw new GroupNotFoundException(groupId);
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public JSONObject getGroupProfile(UUID groupId) throws GroupNotFoundException, BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_GROUP_GET_PROFILE).trim();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(UUIDUtils.toBigInteger(groupId)));
            ResultSet rs = ps.executeQuery();

            if (rs.next())
                return new JSONObject(rs.getString(1));
            else
                throw new GroupNotFoundException(groupId);
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public Date getGroupCreationTime(UUID groupId) throws GroupNotFoundException, BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_GROUP_GET_CREATEDAT).trim();
        Connection conn = null;
        PreparedStatement ps = null;

        try {
            conn = dataSource.getConnection();
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(UUIDUtils.toBigInteger(groupId)));
            ResultSet rs = ps.executeQuery();

            if (rs.next())
                return SQL_DATE_PARSER.parse(rs.getString(1));
            else
                throw new GroupNotFoundException(groupId);
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public boolean isUserInGroupRole(UUID userId, UUID groupId, SCRole role)
            throws UserNotFoundException, GroupNotFoundException, BackendStoreException {

        String sqlQuery = properties.getProperty(DBProperties.Q_GROUP_IS_USERINROLE).trim();
        Connection conn = null;
        PreparedStatement ps = null;
        BigInteger uid = UUIDUtils.toBigInteger(userId);
        BigInteger gid = UUIDUtils.toBigInteger(groupId);

        try {
            conn = dataSource.getConnection();

            if (!Boolean.TRUE.equals(isUserActive(uid, conn)))
                throw new UserNotFoundException(userId);

            if (!Boolean.TRUE.equals(isGroupActive(gid, conn)))
                throw new GroupNotFoundException(groupId);

            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(uid));
            ps.setBigDecimal(2, new BigDecimal(gid));
            ps.setInt(3, role.getRoleId());
            ResultSet rs = ps.executeQuery();

            return rs.next();
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public void addPendingGroupMember(UUID userId, UUID groupId)
            throws UserNotFoundException, GroupNotFoundException, BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_GROUP_PENDING_ADD).trim();
        Connection conn = null;
        PreparedStatement ps = null;
        BigInteger uid = UUIDUtils.toBigInteger(userId);
        BigInteger gid = UUIDUtils.toBigInteger(groupId);

        try {
            conn = dataSource.getConnection();

            if (!Boolean.TRUE.equals(isUserActive(uid, conn)))
                throw new UserNotFoundException(userId);

            if (!Boolean.TRUE.equals(isGroupActive(gid, conn)))
                throw new GroupNotFoundException(groupId);

            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(uid));
            ps.setBigDecimal(2, new BigDecimal(gid));
            ps.executeUpdate();
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public JSONArray listPendingGroupMembers(UUID groupId, long offset, long count)
            throws GroupNotFoundException, BackendStoreException {

        String sqlQuery = properties.getProperty(DBProperties.Q_GROUP_PENDING_LIST).trim();
        JSONArray jaUsers = new JSONArray();
        Connection conn = null;
        PreparedStatement ps = null;
        BigInteger gid = UUIDUtils.toBigInteger(groupId);

        try {
            conn = dataSource.getConnection();

            if (!Boolean.TRUE.equals(isGroupActive(gid, conn)))
                throw new GroupNotFoundException(groupId);

            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(gid));
            ps.setLong(2, offset);
            ps.setLong(3, count);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                JSONObject joUser = new JSONObject();
                joUser.put("uuid", UUIDUtils.fromBigInteger(rs.getBigDecimal("user_uuid").toBigInteger()).toString());
                joUser.put("requested_at", SQL_DATE_PARSER.parse(rs.getString("requested_at")));
                jaUsers.put(joUser);
            }

            return jaUsers;
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public void addGroupMember(UUID userId, UUID groupId, SCRole role)
            throws UserNotFoundException, GroupNotFoundException, BackendStoreException {

        String sqlQuery = properties.getProperty(DBProperties.Q_GROUP_MEMBERS_ADD).trim();
        Connection conn = null;
        PreparedStatement ps = null;
        BigInteger uid = UUIDUtils.toBigInteger(userId);
        BigInteger gid = UUIDUtils.toBigInteger(groupId);

        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            if (!Boolean.TRUE.equals(isUserActive(uid, conn)))
                throw new UserNotFoundException(userId);

            if (!Boolean.TRUE.equals(isGroupActive(gid, conn)))
                throw new GroupNotFoundException(groupId);

            // Delete the user from the pending members list for this group (if exists)
            deletePendingMember(gid, uid, conn);

            // Add the user to the group
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(uid));
            ps.setBigDecimal(2, new BigDecimal(gid));
            ps.setInt(3, role.getRoleId());
            ps.executeUpdate();

            // Record the event
            addEvent(SCEvent.USER_JOINED_GROUP, uid, gid, null, null, null, conn);

            conn.commit();
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            rollbackTransaction(conn);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public boolean isGroupMember(UUID userId, UUID groupId)
            throws UserNotFoundException, GroupNotFoundException, BackendStoreException {
        // Everyone is in the PUBLIC group
        if (PUBLIC_GROUP.equals(groupId))
            return true;
        else
            if (userId == null || groupId == null)
                return false;

        String sqlQuery = properties.getProperty(DBProperties.Q_USER_GROUP_ISMEMBER).trim();
        Connection conn = null;
        PreparedStatement ps = null;
        BigInteger uid = UUIDUtils.toBigInteger(userId);
        BigInteger gid = UUIDUtils.toBigInteger(groupId);

        try {
            conn = dataSource.getConnection();

            if (!Boolean.TRUE.equals(isUserActive(uid, conn)))
                throw new UserNotFoundException(userId);

            if (!Boolean.TRUE.equals(isGroupActive(gid, conn)))
                throw new GroupNotFoundException(groupId);

            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(uid));
            ps.setBigDecimal(2, new BigDecimal(gid));
            ResultSet rs = ps.executeQuery();

            return rs.next();
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public JSONArray listGroupMembers(UUID groupId, long offset, long count)
            throws GroupNotFoundException, BackendStoreException {

        String sqlQuery = properties.getProperty(DBProperties.Q_GROUP_MEMBERS_LIST).trim();
        JSONArray jaUsers = new JSONArray();
        Connection conn = null;
        PreparedStatement ps = null;
        BigInteger gid = UUIDUtils.toBigInteger(groupId);

        try {
            conn = dataSource.getConnection();

            if (!Boolean.TRUE.equals(isGroupActive(gid, conn)))
                throw new GroupNotFoundException(groupId);

            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(gid));
            ps.setLong(2, offset);
            ps.setLong(3, count);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                JSONObject joUser = new JSONObject();
                joUser.put("uuid", UUIDUtils.fromBigInteger(rs.getBigDecimal("user_uuid").toBigInteger()).toString());
                joUser.put("role", rs.getString("role_name"));
                jaUsers.put(joUser);
            }

            return jaUsers;
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public JSONArray listUserGroups(UUID userId, long offset, long count)
            throws UserNotFoundException, BackendStoreException {

        String sqlQuery = properties.getProperty(DBProperties.Q_USER_GROUP_LIST).trim();
        JSONArray jaGroups = new JSONArray();
        Connection conn = null;
        PreparedStatement ps = null;
        BigInteger uid = UUIDUtils.toBigInteger(userId);

        try {
            conn = dataSource.getConnection();

            if (!Boolean.TRUE.equals(isUserActive(uid, conn)))
                throw new UserNotFoundException(userId);

            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(uid));
            ps.setLong(2, offset);
            ps.setLong(3, count);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                JSONObject joGroup = new JSONObject();
                joGroup.put("uuid", UUIDUtils.fromBigInteger(rs.getBigDecimal("group_uuid").toBigInteger()).toString());
                joGroup.put("role", rs.getString("role_name"));
                jaGroups.put(joGroup);
            }

            return jaGroups;
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public JSONArray listComponentGroupsAsUser(UUID componentId, int version, UUID remoteUserId, long offset, long count)
            throws ComponentNotFoundException, UserNotFoundException, BackendStoreException {

        String sqlQuery = properties.getProperty(DBProperties.Q_COMP_GROUP_LIST).trim();
        Connection conn = null;
        PreparedStatement ps = null;
        JSONArray jaGroups = new JSONArray();

        BigInteger compId = UUIDUtils.toBigInteger(componentId);
        BigInteger ruid = (remoteUserId != null) ? UUIDUtils.toBigInteger(remoteUserId) : null;

        try {
            conn = dataSource.getConnection();

            Long versionId = getComponentVersionId(compId, version, conn);
            if (versionId == null) throw new ComponentNotFoundException(componentId, version);

            if (remoteUserId != null)
                if (!Boolean.TRUE.equals(isUserActive(ruid, conn)))
                    throw new UserNotFoundException(remoteUserId);

            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(compId));
            ps.setTimestamp(2, new Timestamp(versionId));
            if (remoteUserId == null)
                ps.setNull(3, Types.DECIMAL);
            else
                ps.setBigDecimal(3, new BigDecimal(ruid));
            ps.setLong(4, offset);
            ps.setLong(5, count);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                JSONObject joGroup = new JSONObject();
                joGroup.put("uuid", UUIDUtils.fromBigInteger(rs.getBigDecimal("group_uuid").toBigInteger()).toString());
                jaGroups.put(joGroup);
            }

            return jaGroups;
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public JSONArray listFlowGroupsAsUser(UUID flowId, int version, UUID remoteUserId, long offset, long count)
            throws FlowNotFoundException, UserNotFoundException, BackendStoreException {

        String sqlQuery = properties.getProperty(DBProperties.Q_FLOW_GROUP_LIST).trim();
        Connection conn = null;
        PreparedStatement ps = null;
        JSONArray jaGroups = new JSONArray();

        BigInteger fId = UUIDUtils.toBigInteger(flowId);
        BigInteger ruid = (remoteUserId != null) ? UUIDUtils.toBigInteger(remoteUserId) : null;

        try {
            conn = dataSource.getConnection();

            Long versionId = getFlowVersionId(fId, version, conn);
            if (versionId == null) throw new FlowNotFoundException(flowId, version);

            if (remoteUserId != null)
                if (!Boolean.TRUE.equals(isUserActive(ruid, conn)))
                    throw new UserNotFoundException(remoteUserId);

            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(fId));
            ps.setTimestamp(2, new Timestamp(versionId));
            if (remoteUserId == null)
                ps.setNull(3, Types.DECIMAL);
            else
                ps.setBigDecimal(3, new BigDecimal(ruid));
            ps.setLong(4, offset);
            ps.setLong(5, count);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                JSONObject joGroup = new JSONObject();
                joGroup.put("uuid", UUIDUtils.fromBigInteger(rs.getBigDecimal("group_uuid").toBigInteger()).toString());
                jaGroups.put(joGroup);
            }

            return jaGroups;
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public JSONObject addComponent(UUID userId, ExecutableComponentDescription component, Map<URL, String> contexts)
            throws UserNotFoundException, BackendStoreException {

        JSONObject joResult = new JSONObject();
        BigInteger uid = UUIDUtils.toBigInteger(userId);
        Connection conn = null;

        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            if (!Boolean.TRUE.equals(isUserActive(uid, conn)))
                throw new UserNotFoundException(userId);

            SortedMap<BigInteger, ContextFile> contextHashes = retrieveContextsAndComputeHashes(contexts);
            BigInteger coreHash = new BigInteger(getComponentCoreHash(component, (SortedSet)contextHashes.keySet()));

            String compRights = component.getRights();
            BigInteger rightsHash = new BigInteger(getRightsHash(compRights));

            BigInteger compId = null;
            Integer version = null;

            // Check whether the rights text already exists
            if (getRightsTextForHash(rightsHash, conn) == null)
                // Insert the license text into the DB
                addRights(compRights, rightsHash, conn);
            else
                compId = getComponentId(component, conn);

            if (compId == null) {
                // Generate a new id for the component
                compId = UUIDUtils.toBigInteger(UUID.randomUUID());
                version = 1;
            } else {
                Component lastAddComp = getLastAddedComponent(compId, conn);
                if (lastAddComp == null) // sanity check - should not happen
                    throw new BackendStoreException("Problem retrieving last added component for existing comp id: "
                            + UUIDUtils.fromBigInteger(compId));

                // Check whether this component is identical to last added
                if (coreHash.equals(lastAddComp.getComponentCoreHash())
                        && component.getName().equals(lastAddComp.getName())
                        && component.getCreator().equals(lastAddComp.getCreator())
                        && component.getDescription().equals(lastAddComp.getDescription())
                        && rightsHash.equals(lastAddComp.getRightsHash())
                        && component.getExecutableComponent().getURI().equals(lastAddComp.getUri())
                        && component.getTags().getTags().containsAll(lastAddComp.getTags())
                        && lastAddComp.getTags().containsAll(component.getTags().getTags())) {

                    // Component identical to last inserted version, get its version and return info to user
                    int qCompVersion = getComponentVersionCount(compId, conn);

                    joResult.put("uuid", UUIDUtils.fromBigInteger(compId).toString());
                    joResult.put("version", qCompVersion);

                    // Record the event
                    addEvent(SCEvent.COMPONENT_UPLOADED, uid, null, compId, null, joResult, conn);

                    conn.commit();

                    logger.fine(String.format("Ignoring repeated upload of component %s, version %d",
                            joResult.getString("uuid"), qCompVersion));

                    return joResult;
                }
            }

            // Insert this component version into the DB
            long timestamp = addComponent(compId, coreHash, rightsHash, contextHashes, component, conn);

            // Insert the user -> component mapping
            String sqlQuery = properties.getProperty(DBProperties.Q_USER_COMPONENT_ADD).trim();
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(sqlQuery);
                ps.setBigDecimal(1, new BigDecimal(uid));
                ps.setBigDecimal(2, new BigDecimal(compId));
                ps.setTimestamp(3, new Timestamp(timestamp));

                ps.executeUpdate();
            }
            finally {
                closeStatement(ps);
            }

            if (version == null)
                version = getComponentVersionCount(compId, conn);

            joResult.put("uuid", UUIDUtils.fromBigInteger(compId).toString());
            joResult.put("version", version);

            // Record the event
            addEvent(SCEvent.COMPONENT_UPLOADED, uid, null, compId, null, joResult, conn);

            conn.commit();
        }
        catch (BackendStoreException e) {
            logger.log(Level.SEVERE, null, e);
            rollbackTransaction(conn);
            throw e;
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            rollbackTransaction(conn);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn);
        }

        return joResult;
    }

    @Override
    public Model getComponent(UUID componentId, int version) throws ComponentNotFoundException, BackendStoreException {
        Connection conn = null;

        BigInteger compId = UUIDUtils.toBigInteger(componentId);

        try {
            conn = dataSource.getConnection();

            Long versionId = getComponentVersionId(compId, version, conn);
            if (versionId == null) throw new ComponentNotFoundException(componentId, version);

            InputStream is = getComponentDescriptor(compId, versionId, conn);
            if (is == null) throw new ComponentNotFoundException(componentId, version);

            return ModelUtils.getModel(is, null);
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn);
        }
    }

    @Override
    public ComponentContext getComponentContext(UUID componentId, int version, String contextId)
            throws ComponentNotFoundException, ComponentContextNotFoundException, BackendStoreException {

        Connection conn = null;
        BigInteger compId = UUIDUtils.toBigInteger(componentId);

        try {
            conn = dataSource.getConnection();

            Long versionId = getComponentVersionId(compId, version, conn);
            if (versionId == null) throw new ComponentNotFoundException(componentId, version);

            BigInteger ctxHash = new BigInteger(Crypto.fromHexString(contextId));
            ComponentContext context = getComponentContext(compId, versionId, ctxHash, conn);

            if (context != null)
                return context;
            else
                throw new ComponentContextNotFoundException(componentId, version, contextId);
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn);
        }
    }

    @Override
    public boolean hasComponentContext(String contextId) throws BackendStoreException {
        Connection conn = null;

        try {
            conn = dataSource.getConnection();

            return hasContext(new BigInteger(Crypto.fromHexString(contextId)), conn);
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn);
        }
    }

    @Override
    public UUID getComponentOwner(UUID componentId, int version) throws ComponentNotFoundException, BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_COMP_GET_OWNER).trim();
        Connection conn = null;
        PreparedStatement ps = null;

        BigInteger compId = UUIDUtils.toBigInteger(componentId);

        try {
            conn = dataSource.getConnection();

            Long compVerId = getComponentVersionId(compId, version, conn);
            if (compVerId == null) throw new ComponentNotFoundException(componentId, version);

            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(compId));
            ps.setTimestamp(2, new Timestamp(compVerId));
            ResultSet rs = ps.executeQuery();

            return rs.next() ? UUIDUtils.fromBigInteger(rs.getBigDecimal("user_uuid").toBigInteger()) : null;
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public Integer getComponentVersionCount(UUID compId) throws ComponentNotFoundException, BackendStoreException {
        Connection conn;

        try {
            conn = dataSource.getConnection();
            Integer verCount = getComponentVersionCount(UUIDUtils.toBigInteger(compId), conn);
            if (verCount != null)
                return verCount;
            else
                throw new ComponentNotFoundException(compId, -1);
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
    }

    @Override
    public void shareComponent(UUID componentId, int version, UUID groupId, UUID remoteUserId)
            throws ComponentNotFoundException, GroupNotFoundException, UserNotFoundException, BackendStoreException {

        String sqlQuery = properties.getProperty(DBProperties.Q_COMP_SHARE).trim();
        Connection conn = null;
        PreparedStatement ps = null;
        BigInteger compId = UUIDUtils.toBigInteger(componentId);
        BigInteger ruid = (remoteUserId != null) ? UUIDUtils.toBigInteger(remoteUserId) : null;
        BigInteger gid = UUIDUtils.toBigInteger(groupId);

        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            Long compVerId = getComponentVersionId(compId, version, conn);
            if (compVerId == null) throw new ComponentNotFoundException(componentId, version);

            if (!Boolean.TRUE.equals(isGroupActive(gid, conn)))
                throw new GroupNotFoundException(groupId);

            if (remoteUserId != null)
                if (!Boolean.TRUE.equals(isUserActive(ruid, conn)))
                    throw new UserNotFoundException(remoteUserId);

            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(compId));
            ps.setTimestamp(2, new Timestamp(compVerId));
            ps.setBigDecimal(3, new BigDecimal(gid));
            ps.executeUpdate();

            // Record the event
            addEvent(SCEvent.COMPONENT_SHARED, ruid, UUIDUtils.toBigInteger(groupId), compId, null, null, conn);

            conn.commit();
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            rollbackTransaction(conn);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public JSONArray listUserComponents(UUID userId, long offset, long count,
                                        boolean includeOldVersions)
            throws UserNotFoundException, BackendStoreException {
        return listAccessibleUserComponentsAsUser(userId, userId, offset, count, includeOldVersions);
    }

    @Override
    public JSONArray listPublicUserComponents(UUID userId, long offset, long count,
                                              boolean includeOldVersions)
            throws UserNotFoundException, BackendStoreException {
        return listAccessibleUserComponentsAsUser(userId, null, offset, count, includeOldVersions);
    }

    @Override
    public JSONArray listAccessibleUserComponentsAsUser(UUID userId, UUID remoteUserId, long offset, long count,
                                                        boolean includeOldVersions)
            throws UserNotFoundException, BackendStoreException {
        Connection conn = null;
        PreparedStatement ps = null;
        JSONArray jaResult = new JSONArray();
        BigDecimal uid = new BigDecimal(UUIDUtils.toBigInteger(userId));
        BigDecimal ruid = remoteUserId != null ? new BigDecimal(UUIDUtils.toBigInteger(remoteUserId)) : null;

        try {
            conn = dataSource.getConnection();

            if (!Boolean.TRUE.equals(isUserActive(uid.toBigInteger(), conn)))
                throw new UserNotFoundException(userId);

            if (remoteUserId != null)
                if (!Boolean.TRUE.equals(isUserActive(ruid.toBigInteger(), conn)))
                    throw new UserNotFoundException(remoteUserId);

            if (includeOldVersions) {
                ps = conn.prepareStatement(properties.getProperty(
                        DBProperties.Q_USER_COMPONENT_SHARING_LIST_ALL_ASUSER).trim());
                ps.setBigDecimal(1, uid);
                ps.setBigDecimal(2, ruid);
                ps.setBigDecimal(3, uid);
                ps.setBigDecimal(4, ruid);
                ps.setLong(5, offset);
                ps.setLong(6, count);
            } else {
                ps = conn.prepareStatement(properties.getProperty(
                        DBProperties.Q_USER_COMPONENT_SHARING_LIST_LATEST_ASUSER).trim());
                ps.setBigDecimal(1, uid);
                ps.setBigDecimal(2, ruid);
                ps.setBigDecimal(3, uid);
                ps.setBigDecimal(4, ruid);
                ps.setBigDecimal(5, uid);
                ps.setBigDecimal(6, ruid);
                ps.setBigDecimal(7, uid);
                ps.setBigDecimal(8, ruid);
                ps.setLong(9, offset);
                ps.setLong(10, count);
            }
            ResultSet rs = ps.executeQuery();

            Map<String, JSONObject> map = new HashMap<String, JSONObject>();
            while (rs.next()) {
                UUID componentId = UUIDUtils.fromBigInteger(rs.getBigDecimal("comp_uuid").toBigInteger());
                int version = rs.getInt("version");
                BigDecimal gid = rs.getBigDecimal("group_uuid");
                UUID groupId = null;
                if (gid != null)
                    groupId = UUIDUtils.fromBigInteger(gid.toBigInteger());

                String key = componentId.toString() + version;
                JSONObject joCompVer = map.get(key);
                if (joCompVer == null) {
                    joCompVer = new JSONObject();
                    joCompVer.put("uuid", componentId.toString());
                    joCompVer.put("version", version);
                    joCompVer.put("groups", new JSONArray());
                    map.put(key, joCompVer);
                }

                joCompVer.getJSONArray("groups").put(groupId != null ? groupId.toString() : JSONObject.NULL);
            }

            for (JSONObject jo : map.values())
                jaResult.put(jo);

            return jaResult;
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public JSONArray listPublicComponents(long offset, long count, boolean includeOldVersions)
            throws GroupNotFoundException, BackendStoreException {
        return listGroupComponents(PUBLIC_GROUP, offset, count, includeOldVersions);
    }

    @Override
    public JSONArray listGroupComponents(UUID groupId, long offset, long count, boolean includeOldVersions)
            throws GroupNotFoundException, BackendStoreException {
        Connection conn = null;
        PreparedStatement ps = null;
        JSONArray jaResult = new JSONArray();
        BigInteger gid = UUIDUtils.toBigInteger(groupId);

        try {
            conn = dataSource.getConnection();

            if (!Boolean.TRUE.equals(isGroupActive(gid, conn)))
                throw new GroupNotFoundException(groupId);

            if (includeOldVersions)
                ps = conn.prepareStatement(properties.getProperty(
                        DBProperties.Q_GROUP_COMPONENTS_LIST_ALL).trim());
            else
                ps = conn.prepareStatement(properties.getProperty(
                        DBProperties.Q_GROUP_COMPONENTS_LIST_LATEST).trim());

            ps.setBigDecimal(1, new BigDecimal(gid));
            ps.setLong(2, offset);
            ps.setLong(3, count);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                UUID componentId = UUIDUtils.fromBigInteger(rs.getBigDecimal("comp_uuid").toBigInteger());
                int version = rs.getInt("version");

                JSONObject joCompVer = new JSONObject();
                joCompVer.put("uuid", componentId.toString());
                joCompVer.put("version", version);

                jaResult.put(joCompVer);
            }

            return jaResult;
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public JSONObject addFlow(UUID userId, FlowDescription flow) throws UserNotFoundException, BackendStoreException {
        JSONObject joResult = new JSONObject();
        BigInteger uid = UUIDUtils.toBigInteger(userId);
        Connection conn = null;

        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            if (!Boolean.TRUE.equals(isUserActive(uid, conn)))
                throw new UserNotFoundException(userId);

            // Check whether this flow contains any unknown components
            // and also build the list of components used in the flow
            List<Component> components = new ArrayList<Component>();
            List<String> unknownComponents = new ArrayList<String>();
            for (ExecutableComponentInstanceDescription ecid : flow.getExecutableComponentInstances()) {
                String compUri = ecid.getExecutableComponent().getURI();
                Matcher m = REGEX_UUID_VERSION.matcher(compUri);
                if (m.matches()) {
                    BigInteger compId = UUIDUtils.toBigInteger(UUID.fromString(m.group(1)));
                    int compVer = Integer.parseInt(m.group(2));
                    Long compVerId = getComponentVersionId(compId, compVer, conn);
                    if (compVerId == null)
                        unknownComponents.add(compUri);
                    else {
                        Component comp = new Component();
                        comp.setComponentVersionId(compVerId);
                        comp.setComponentCoreHash(getComponentCore(compId, compVerId, conn));
                        components.add(comp);
                    }
                } else
                    unknownComponents.add(compUri);
            }

            if (unknownComponents.size() > 0)
                throw new UnknownComponentsException(unknownComponents);

            BigInteger coreHash = new BigInteger(getFlowCoreHash(flow));

            String flowRights = flow.getRights();
            BigInteger rightsHash = new BigInteger(getRightsHash(flowRights));

            BigInteger flowId = null;
            Integer version = null;

            // Check whether the rights text already exists
            if (getRightsTextForHash(rightsHash, conn) == null)
                // Insert the license text into the DB
                addRights(flowRights, rightsHash, conn);
            else
                flowId = getFlowId(flow, conn);

            if (flowId == null) {
                // Generate a new id for the flow
                flowId = UUIDUtils.toBigInteger(UUID.randomUUID());
                version = 1;
            } else {
                Flow lastAddedFlow = getLastAddedFlow(flowId, conn);
                if (lastAddedFlow == null) // sanity check - should not happen
                    throw new BackendStoreException("Problem retrieving last added flow for existing flow id: "
                            + UUIDUtils.fromBigInteger(flowId));

                // Check whether this flow is identical to last added
                if (coreHash.equals(lastAddedFlow.getFlowCoreHash())
                        && flow.getName().equals(lastAddedFlow.getName())
                        && flow.getCreator().equals(lastAddedFlow.getCreator())
                        && flow.getDescription().equals(lastAddedFlow.getDescription())
                        && rightsHash.equals(lastAddedFlow.getRightsHash())
                        && flow.getFlowComponent().getURI().equals(lastAddedFlow.getUri())
                        && flow.getTags().getTags().containsAll(lastAddedFlow.getTags())
                        && lastAddedFlow.getTags().containsAll(flow.getTags().getTags())) {

                    // Flow identical to last inserted version, get its version and return info to user
                    int qFlowVersion = getFlowVersionCount(flowId, conn);

                    joResult.put("uuid", UUIDUtils.fromBigInteger(flowId).toString());
                    joResult.put("version", qFlowVersion);

                    // Record the event
                    addEvent(SCEvent.FLOW_UPLOADED, uid, null, null, flowId, joResult, conn);

                    conn.commit();

                    logger.fine(String.format("Ignoring repeated upload of flow %s, version %d",
                            joResult.getString("uuid"), qFlowVersion));

                    return joResult;
                }
            }

            // Insert this flow version into the DB
            long timestamp = addFlow(flowId, coreHash, rightsHash, flow, conn);

            // Insert the user -> flow mapping
            String sqlQuery = properties.getProperty(DBProperties.Q_USER_FLOW_ADD).trim();
            PreparedStatement ps = null;
            try {
                ps = conn.prepareStatement(sqlQuery);
                ps.setBigDecimal(1, new BigDecimal(uid));
                ps.setBigDecimal(2, new BigDecimal(flowId));
                ps.setTimestamp(3, new Timestamp(timestamp));

                ps.executeUpdate();
            }
            finally {
                closeStatement(ps);
            }

            // Insert the flow -> component mapping
            sqlQuery = properties.getProperty(DBProperties.Q_FLOW_COMPONENT_ADD).trim();
            try {
                ps = conn.prepareStatement(sqlQuery);
                for (Component c : components) {
                    ps.setBigDecimal(1, new BigDecimal(coreHash));
                    ps.setTimestamp(2, new Timestamp(timestamp));
                    ps.setBigDecimal(3, new BigDecimal(c.getComponentCoreHash()));
                    ps.setTimestamp(4, new Timestamp(c.getComponentVersionId()));
                    ps.addBatch();
                }

                ps.executeBatch();
            }
            finally {
                closeStatement(ps);
            }

            if (version == null)
                version = getFlowVersionCount(flowId, conn);

            joResult.put("uuid", UUIDUtils.fromBigInteger(flowId).toString());
            joResult.put("version", version);

            // Record the event
            addEvent(SCEvent.FLOW_UPLOADED, uid, null, null, flowId, joResult, conn);

            conn.commit();
        }
        catch (BackendStoreException e) {
            logger.log(Level.SEVERE, null, e);
            rollbackTransaction(conn);
            throw e;
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            rollbackTransaction(conn);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn);
        }

        return joResult;

    }

    @Override
    public Model getFlow(UUID flowId, int version) throws FlowNotFoundException, BackendStoreException {
        Connection conn = null;

        BigInteger fId = UUIDUtils.toBigInteger(flowId);

        try {
            conn = dataSource.getConnection();

            Long versionId = getFlowVersionId(fId, version, conn);
            if (versionId == null) throw new FlowNotFoundException(flowId, version);

            InputStream is = getFlowDescriptor(fId, versionId, conn);
            if (is == null) return null;

            return ModelUtils.getModel(is, null);
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn);
        }
    }

    @Override
    public UUID getFlowOwner(UUID flowId, int version) throws FlowNotFoundException, BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_FLOW_GET_OWNER).trim();
        Connection conn = null;
        PreparedStatement ps = null;

        BigInteger fId = UUIDUtils.toBigInteger(flowId);

        try {
            conn = dataSource.getConnection();

            Long versionId = getFlowVersionId(fId, version, conn);
            if (versionId == null) throw new FlowNotFoundException(flowId, version);

            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(fId));
            ps.setTimestamp(2, new Timestamp(versionId));
            ResultSet rs = ps.executeQuery();

            return rs.next() ? UUIDUtils.fromBigInteger(rs.getBigDecimal("user_uuid").toBigInteger()) : null;
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public Integer getFlowVersionCount(UUID flowId) throws FlowNotFoundException, BackendStoreException {
        Connection conn;

        try {
            conn = dataSource.getConnection();
            Integer verCount = getFlowVersionCount(UUIDUtils.toBigInteger(flowId), conn);
            if (verCount != null)
                return verCount;
            else
                throw new FlowNotFoundException(flowId, -1);
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
    }

    @Override
    public void shareFlow(UUID flowId, int version, UUID groupId, UUID remoteUserId)
            throws FlowNotFoundException, GroupNotFoundException, UserNotFoundException, BackendStoreException {
        String sqlQuery = properties.getProperty(DBProperties.Q_FLOW_SHARE).trim();
        Connection conn = null;
        PreparedStatement ps = null;
        BigInteger fId = UUIDUtils.toBigInteger(flowId);
        BigInteger ruid = (remoteUserId != null) ? UUIDUtils.toBigInteger(remoteUserId) : null;
        BigInteger gid = UUIDUtils.toBigInteger(groupId);

        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            Long versionId = getFlowVersionId(fId, version, conn);
            if (versionId == null) throw new FlowNotFoundException(flowId, version);

            if (!Boolean.TRUE.equals(isGroupActive(gid, conn)))
                throw new GroupNotFoundException(groupId);

            if (remoteUserId != null)
                if (!Boolean.TRUE.equals(isUserActive(ruid, conn)))
                    throw new UserNotFoundException(remoteUserId);

            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(fId));
            ps.setTimestamp(2, new Timestamp(versionId));
            ps.setBigDecimal(3, new BigDecimal(gid));
            ps.executeUpdate();

            // Record the event
            addEvent(SCEvent.FLOW_SHARED, ruid, UUIDUtils.toBigInteger(groupId), null, fId, null, conn);

            conn.commit();
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            rollbackTransaction(conn);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public JSONArray listUserFlows(UUID userId, long offset, long count, boolean includeOldVersions)
            throws UserNotFoundException, BackendStoreException {
        return listAccessibleUserFlowsAsUser(userId, userId, offset, count, includeOldVersions);
    }

    @Override
    public JSONArray listPublicUserFlows(UUID userId, long offset, long count, boolean includeOldVersions)
            throws UserNotFoundException, BackendStoreException {
        return listAccessibleUserFlowsAsUser(userId, null, offset, count, includeOldVersions);
    }

    @Override
    public JSONArray listAccessibleUserFlowsAsUser(UUID userId, UUID remoteUserId, long offset, long count,
                                                   boolean includeOldVersions)
            throws UserNotFoundException, BackendStoreException {

        Connection conn = null;
        PreparedStatement ps = null;
        JSONArray jaResult = new JSONArray();
        BigDecimal uid = new BigDecimal(UUIDUtils.toBigInteger(userId));
        BigDecimal ruid = remoteUserId != null ? new BigDecimal(UUIDUtils.toBigInteger(remoteUserId)) : null;

        try {
            conn = dataSource.getConnection();

            if (!Boolean.TRUE.equals(isUserActive(uid.toBigInteger(), conn)))
                throw new UserNotFoundException(userId);

            if (remoteUserId != null)
                if (!Boolean.TRUE.equals(isUserActive(ruid.toBigInteger(), conn)))
                    throw new UserNotFoundException(remoteUserId);

            if (includeOldVersions) {
                ps = conn.prepareStatement(properties.getProperty(
                        DBProperties.Q_USER_FLOW_SHARING_LIST_ALL_ASUSER).trim());
                ps.setBigDecimal(1, uid);
                ps.setBigDecimal(2, ruid);
                ps.setBigDecimal(3, uid);
                ps.setBigDecimal(4, ruid);
                ps.setLong(5, offset);
                ps.setLong(6, count);
            } else {
                ps = conn.prepareStatement(properties.getProperty(
                        DBProperties.Q_USER_FLOW_SHARING_LIST_LATEST_ASUSER).trim());
                ps.setBigDecimal(1, uid);
                ps.setBigDecimal(2, ruid);
                ps.setBigDecimal(3, uid);
                ps.setBigDecimal(4, ruid);
                ps.setBigDecimal(5, uid);
                ps.setBigDecimal(6, ruid);
                ps.setBigDecimal(7, uid);
                ps.setBigDecimal(8, ruid);
                ps.setLong(9, offset);
                ps.setLong(10, count);
            }
            ResultSet rs = ps.executeQuery();

            Map<String, JSONObject> map = new HashMap<String, JSONObject>();
            while (rs.next()) {
                UUID flowId = UUIDUtils.fromBigInteger(rs.getBigDecimal("flow_uuid").toBigInteger());
                int version = rs.getInt("version");
                BigDecimal gid = rs.getBigDecimal("group_uuid");
                UUID groupId = null;
                if (gid != null)
                    groupId = UUIDUtils.fromBigInteger(gid.toBigInteger());

                String key = flowId.toString() + version;
                JSONObject joFlowVer = map.get(key);
                if (joFlowVer == null) {
                    joFlowVer = new JSONObject();
                    joFlowVer.put("uuid", flowId.toString());
                    joFlowVer.put("version", version);
                    joFlowVer.put("groups", new JSONArray());
                    map.put(key, joFlowVer);
                }

                joFlowVer.getJSONArray("groups").put(groupId != null ? groupId.toString() : JSONObject.NULL);
            }

            for (JSONObject jo : map.values())
                jaResult.put(jo);

            return jaResult;
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }

    @Override
    public JSONArray listPublicFlows(long offset, long count, boolean includeOldVersions) throws BackendStoreException {
        try {
            return listGroupFlows(PUBLIC_GROUP, offset, count, includeOldVersions);
        }
        catch (GroupNotFoundException e) {
            // Should never happen
            return null;
        }
    }

    @Override
    public JSONArray listGroupFlows(UUID groupId, long offset, long count, boolean includeOldVersions)
            throws GroupNotFoundException, BackendStoreException {
        Connection conn = null;
        PreparedStatement ps = null;
        JSONArray jaResult = new JSONArray();
        BigInteger gid = UUIDUtils.toBigInteger(groupId);

        try {
            conn = dataSource.getConnection();

            if (!Boolean.TRUE.equals(isGroupActive(gid, conn)))
                throw new GroupNotFoundException(groupId);

            if (includeOldVersions)
                ps = conn.prepareStatement(properties.getProperty(
                        DBProperties.Q_GROUP_FLOWS_LIST_ALL).trim());
            else
                ps = conn.prepareStatement(properties.getProperty(
                        DBProperties.Q_GROUP_FLOWS_LIST_LATEST).trim());

            ps.setBigDecimal(1, new BigDecimal(gid));
            ps.setLong(2, offset);
            ps.setLong(3, count);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                UUID flowId = UUIDUtils.fromBigInteger(rs.getBigDecimal("flow_uuid").toBigInteger());
                int version = rs.getInt("version");

                JSONObject joFlowVer = new JSONObject();
                joFlowVer.put("uuid", flowId.toString());
                joFlowVer.put("version", version);

                jaResult.put(joFlowVer);
            }

            return jaResult;
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, null, e);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn, ps);
        }
    }



    /**
     * Parses a multiline SQL string with comments into individual SQL statement strings
     *
     * @param sqlString The SQL string
     * @return The individual SQL statement strings
     */
    protected Iterable<? extends String> parseSQLString(String sqlString) {
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new StringReader(sqlString));

        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines or comments
                if (line.length() == 0 || line.startsWith("//"))
                    continue;

                sb.append(" ").append(line);

                // Assume that a ";" at the end of a line indicates
                // the end of that SQL statement
                if (line.endsWith(";")) {
                    lines.add(sb.substring(1));
                    sb = new StringBuilder();
                }
            }
        }
        catch (IOException e) {
            // Should not happen
            throw new RuntimeException(e);
        }

        return lines;
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
     * @param statements  (Optional) Any ResultSet(s) that need to be closed before the connection is released
     */
    protected void releaseConnection(Connection connection, Statement... statements) {
        if (statements != null)
            for (Statement stmt : statements)
                closeStatement(stmt);

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
     * Closes a Statement
     *
     * @param stmt The Statement
     */
    protected void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            }
            catch (SQLException e) {
                logger.log(Level.WARNING, null, e);
            }
        }
    }

    /**
     * Retrieves and calculates hash signature for the set of component context files
     *
     * @param contexts The context files
     * @return A map holding the sorted hash values and File references of the context files
     * @throws IOException Thrown if a I/O error occurs
     * @throws URISyntaxException Thrown if there is a problem with the context URLs
     */
    protected SortedMap<BigInteger, ContextFile> retrieveContextsAndComputeHashes(Map<URL, String> contexts)
            throws IOException, URISyntaxException {

        SortedMap<BigInteger, ContextFile> sortedMap = new TreeMap<BigInteger, ContextFile>();

        for (Map.Entry<URL, String> context : contexts.entrySet()) {
            URL url = context.getKey();
            String ctxFileName = url.toString().substring(url.toString().lastIndexOf("/") + 1);

            logger.finer("Processing context file: " + ((ctxFileName.length() > 0) ? ctxFileName : "<unnamed>"));

            File tmpFile;

            if (url.getProtocol().equals("file"))
                tmpFile = new File(url.toURI());
            else {
                tmpFile = File.createTempFile("sc_context", ".tmp");
                // TODO: We should probably use a timeout in case the URL is not responding to prevent hangs
                FileUtils.copyURLToFile(url, tmpFile);
            }

            // Compute the MD5 hash for the context file
            final BigInteger md5 = new BigInteger(Crypto.createMD5Hash(tmpFile));

            sortedMap.put(md5, new ContextFile(ctxFileName, tmpFile, context.getValue()));
        }

        return sortedMap;
    }

    /**
     * Checks whether a user is active (i.e. user exists and is not deleted) or not
     *
     * @param userId The user id
     * @param conn The DB connection to use
     * @return True if user exists and is not deleted, False if user exists and is marked as deleted, null if user does not exist
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected Boolean isUserActive(BigInteger userId, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_USER_GET_DELETED).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(userId));
            ResultSet rs = ps.executeQuery();

            return rs.next() ? !rs.getBoolean(1) : null;
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Checks whether a group is active (i.e. group exists and is not deleted) or not
     *
     * @param groupId The group id
     * @param conn The DB connection to use
     * @return True if group exists and is not deleted, False if group exists and is marked as deleted, null if group does not exist
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected Boolean isGroupActive(BigInteger groupId, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_GROUP_GET_DELETED).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(groupId));
            ResultSet rs = ps.executeQuery();

            return rs.next() ? !rs.getBoolean(1) : null;
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Retrieves the version count for a component
     *
     * @param compId The component id
     * @param conn The DB connection to use
     * @return The version count, or null if no component with that id has been found
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected Integer getComponentVersionCount(BigInteger compId, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_COMP_GET_VERCOUNT).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(compId));
            ResultSet rs = ps.executeQuery();

            return rs.next() ? rs.getInt(1) : null;
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Retrieves the version count for a flow
     *
     * @param flowId The flow id
     * @param conn The DB connection to use
     * @return The version count, or null if no flow with that id has been found
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected Integer getFlowVersionCount(BigInteger flowId, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_FLOW_GET_VERCOUNT).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(flowId));
            ResultSet rs = ps.executeQuery();

            return rs.next() ? rs.getInt(1) : null;
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Retrieves the rights text for a particular hash value
     *
     * @param rightsHash The hash value
     * @param conn The DB connection to use
     * @return The rights text, or null if the hash value is not known to the DB
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected String getRightsTextForHash(BigInteger rightsHash, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_RIGHTS_GET_TEXT).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(rightsHash));
            ResultSet rs = ps.executeQuery();

            return rs.next() ? rs.getString(1) : null;
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Adds a rights text (license) to the DB
     *
     * @param rights The text of the license
     * @param rightsHash The hash signature of the license if known, or null if unknown
     * @param conn The DB connection to use
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected void addRights(String rights, BigInteger rightsHash, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_RIGHTS_ADD).trim();

        if (rightsHash == null)
            rightsHash = new BigInteger(getRightsHash(rights));

        PreparedStatement ps = null;
        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(rightsHash));
            ps.setString(2, rights);
            ps.executeUpdate();
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Retrieves the last added version for a particular component
     *
     * @param compId The component id
     * @param conn The DB connection to use
     * @return The last added component for the given component id, or null if none found
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected Component getLastAddedComponent(BigInteger compId, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_COMP_GET_LASTINSERT).trim();
        Component component = null;
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(compId));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                component = new Component();
                component.setComponentId(compId);
                component.setComponentCoreHash(rs.getBigDecimal("core_hash").toBigInteger());
                component.setName(rs.getString("name"));
                component.setDescription(rs.getString("description"));
                component.setCreator(rs.getString("creator"));
                component.setRightsHash(rs.getBigDecimal("rights_hash").toBigInteger());
                component.setUri(rs.getString("uri"));

                do {
                    component.addTag(rs.getString("tag"));
                } while (rs.next());
            }

            return component;
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Returns the component id based on the attributes that determine its uniqueness.
     * This method should be overridden in case the meaning of what constitutes a unique component
     * is changed from its default assumption that 'uri' values determine the uniqueness of a component.
     * NOTE: If this method is overridden it is very likely that the SQL query associated with it needs to be changed
     *
     * @param component The component whose (subset of) attributes will be used to determine its uniqueness
     * @param conn The DB connection to use
     * @return The component id, or null if no matching component was found
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected BigInteger getComponentId(ExecutableComponentDescription component, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_COMP_GET_ID).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, component.getExecutableComponent().getURI());
            ResultSet rs = ps.executeQuery();

            return rs.next() ? rs.getBigDecimal(1).toBigInteger() : null;
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Retrieves the version id for a particular component version
     *
     * @param componentId The component id
     * @param version The component version
     * @param conn The DB connection to use
     * @return The version id, or null if no results were obtained
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected Long getComponentVersionId(BigInteger componentId, int version, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_COMP_GET_VERID).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(componentId));
            ps.setInt(2, version-1);
            ResultSet rs = ps.executeQuery();

            return rs.next() ? rs.getTimestamp(1).getTime() : null;
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Retrieves the component descriptor for a particular component version
     *
     * @param componentId The component id
     * @param verId The component version id
     * @param conn The DB connection to use
     * @return An InputStream to the descriptor, or null if no results were obtained
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected InputStream getComponentDescriptor(BigInteger componentId, long verId, Connection conn)
            throws SQLException {

        String sqlQuery = properties.getProperty(DBProperties.Q_COMP_GET_DESCRIPTOR).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(componentId));
            ps.setTimestamp(2, new Timestamp(verId));
            ResultSet rs = ps.executeQuery();

            return rs.next() ? rs.getBinaryStream(1) : null;
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Retrieves the specified component context for a particular component version
     *
     * @param componentId The component id
     * @param verId The component version id
     * @param ctxHash The context hash
     * @param conn The DB connection to use
     * @return The component context
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected ComponentContext getComponentContext(BigInteger componentId, long verId, BigInteger ctxHash,
                                                   Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_COMP_CONTEXT_GET).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(componentId));
            ps.setTimestamp(2, new Timestamp(verId));
            ps.setBigDecimal(3, new BigDecimal(ctxHash));
            ResultSet rs = ps.executeQuery();

            return rs.next() ? new ComponentContext(rs.getString("mime_type"), rs.getBinaryStream("data")) : null;
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Adds a component to the DB
     *
     * @param compId The component id
     * @param coreHash The component core hash
     * @param rightsHash The rights hash
     * @param contextHashes The context hashes
     * @param component The component
     * @param conn The DB connection to use
     * @return The component version id assigned by the DB
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     * @throws FileNotFoundException Thrown if one of the specified contexts cannot be found
     */
    protected long addComponent(BigInteger compId, BigInteger coreHash, BigInteger rightsHash,
                                Map<BigInteger, ContextFile> contextHashes, ExecutableComponentDescription component,
                                Connection conn) throws SQLException, FileNotFoundException {

        long timestamp = getCurrentDateTime(conn).getTime();
        PreparedStatement ps = null;

        try {
            // Insert the component contexts
            String sqlQuery = properties.getProperty(DBProperties.Q_CONTEXT_ADD).trim();
            ps = conn.prepareStatement(sqlQuery);
            for (Map.Entry<BigInteger, ContextFile> context : contextHashes.entrySet()) {
                if (!hasContext(context.getKey(), conn)) {
                    ps.setBigDecimal(1, new BigDecimal(context.getKey()));
                    File file = context.getValue().getFile();
                    ps.setBinaryStream(2, new FileInputStream(file), (int)file.length());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
        catch (SQLException e) {
            // TODO: Check the error code and ignore "duplicate key" errors
            logger.log(Level.WARNING, "Ignoring SQLException on adding contexts!", e);
        }
        finally {
            closeStatement(ps);
            ps = null;
        }

        if (!hasComponentCore(coreHash, conn)) {
            try {
                // Insert the component core
                String sqlQuery = properties.getProperty(DBProperties.Q_COMP_CORE_ADD).trim();
                ps = conn.prepareStatement(sqlQuery);
                ps.setBigDecimal(1, new BigDecimal(coreHash));
                ps.setString(2, component.getFiringPolicy());
                ps.setString(3, component.getMode().getURI());
                ps.setString(4, component.getFormat());
                ps.setString(5, component.getRunnable());
                ps.setString(6, component.getLocation().getURI());
                ps.executeUpdate();
            }
            catch (SQLException e) {
                // TODO: Check the error code and ignore "duplicate key" errors
                logger.log(Level.WARNING, "Ignoring SQLException when inserting into sc_component_core");
            }
            finally {
                closeStatement(ps);
                ps = null;
            }

            try {
                // Insert the core -> context mapping
                String sqlQuery = properties.getProperty(DBProperties.Q_CORE_CONTEXT_ADD).trim();
                ps = conn.prepareStatement(sqlQuery);
                for (BigInteger contextHash : contextHashes.keySet()) {
                    ps.setBigDecimal(1, new BigDecimal(coreHash));
                    ps.setBigDecimal(2, new BigDecimal(contextHash));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            finally {
                closeStatement(ps);
                ps = null;
            }
        }

        try {
            // Insert this component version into the DB
            String sqlQuery = properties.getProperty(DBProperties.Q_COMP_ADD).trim();
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(compId));
            ps.setTimestamp(2, new Timestamp(timestamp));
            ps.setBigDecimal(3, new BigDecimal(coreHash));
            ps.setString(4, component.getName());
            ps.setString(5, component.getCreator());
            ps.setTimestamp(6, new Timestamp(component.getCreationDate().getTime()));
            ps.setBigDecimal(7, new BigDecimal(rightsHash));
            ps.setString(8, component.getExecutableComponent().getURI());
            ps.executeUpdate();
        }
        finally {
            closeStatement(ps);
            ps = null;
        }

        try {
            // Insert the component description
            String sqlQuery = properties.getProperty(DBProperties.Q_COMP_ADD_DESCRIPTION).trim();
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(compId));
            ps.setTimestamp(2, new Timestamp(timestamp));
            ps.setString(3, component.getDescription());
            ps.executeUpdate();
        }
        finally {
            closeStatement(ps);
            ps = null;
        }

        try {
            // Insert the component tags
            String sqlQuery = properties.getProperty(DBProperties.Q_COMP_ADD_TAG).trim();
            ps = conn.prepareStatement(sqlQuery);
            for (String tag : component.getTags().getTags()) {
                ps.setBigDecimal(1, new BigDecimal(compId));
                ps.setTimestamp(2, new Timestamp(timestamp));
                ps.setString(3, tag);
                ps.addBatch();
            }
            ps.executeBatch();
        }
        finally {
            closeStatement(ps);
            ps = null;
        }

        try {
            // Insert the component -> context mapping
            String sqlQuery = properties.getProperty(DBProperties.Q_COMP_CONTEXT_ADD).trim();
            ps = conn.prepareStatement(sqlQuery);
            for (Map.Entry<BigInteger, ContextFile> context : contextHashes.entrySet()) {
                ps.setBigDecimal(1, new BigDecimal(compId));
                ps.setTimestamp(2, new Timestamp(timestamp));
                ps.setBigDecimal(3, new BigDecimal(context.getKey()));
                ps.setString(4, context.getValue().getContentType());
                ps.addBatch();
            }
            ps.executeBatch();
        }
        finally {
            closeStatement(ps);
            ps = null;
        }

        Model model = rewriteComponentContexts(component, contextHashes);
        byte[] modelData = ModelUtils.modelToByteArray(model, "TURTLE");
        try {
            // Insert the component descriptor
            String sqlQuery = properties.getProperty(DBProperties.Q_COMP_ADD_DESCRIPTOR).trim();
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(compId));
            ps.setTimestamp(2, new Timestamp(timestamp));
            ps.setBinaryStream(3, new ByteArrayInputStream(modelData), modelData.length);
            ps.executeUpdate();
        }
        finally {
            closeStatement(ps);
            ps = null;
        }

        return timestamp;
    }

    /**
     * Checks whether a particular component core hash exists
     *
     * @param coreHash The core hash
     * @param conn The DB connection to use
     * @return True if exists, False otherwise
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected boolean hasComponentCore(BigInteger coreHash, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_COMP_CORE_EXISTS).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(coreHash));

            return ps.executeQuery().next();
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Retrieves the component core hash for a particular component version
     *
     * @param compId The component id
     * @param compVerId The component version id
     * @param conn The DB connection to use
     * @return The component core hash value, or null if no results were obtained
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected BigInteger getComponentCore(BigInteger compId, long compVerId, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_COMP_GET_COREHASH).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(compId));
            ps.setTimestamp(2, new Timestamp(compVerId));
            ResultSet rs = ps.executeQuery();

            return rs.next() ? rs.getBigDecimal(1).toBigInteger() : null;
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Returns the DB server current DATETIME
     *
     * @param conn The DB connection to use
     * @return The current DB datetime, or null if it can't be obtained
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected Date getCurrentDateTime(Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_DATETIME_NOW).trim();
        Statement stmt = null;

        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sqlQuery);

            return rs.next() ? new Date(rs.getTimestamp(1).getTime()) : null;
        }
        finally {
            closeStatement(stmt);
        }
    }

    /**
     * Checks whether the DB contains the specified context hash
     *
     * @param contextHash The context hash
     * @param conn The DB connection to use
     * @return True if this context hash exists in the DB, False otherwise
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected boolean hasContext(BigInteger contextHash, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_CONTEXT_EXISTS).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(contextHash));

            return ps.executeQuery().next();
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Prepares the component RDF descriptor for storage
     *
     * @param component The component
     * @param contextHashes The new component contexts
     * @return The updated component descriptor
     */
    protected Model rewriteComponentContexts(ExecutableComponentDescription component,
                                           Map<BigInteger, ContextFile> contextHashes) {

        Model model = component.getExecutableComponent().getModel();
        Resource resExecComp = model.listSubjectsWithProperty(RDF.type,
                RepositoryVocabulary.executable_component).nextResource();
        // Remove existing context statements
        model.remove(model.listStatements(resExecComp, RepositoryVocabulary.execution_context, (RDFNode)null));

        // Add the "implementation" context
        resExecComp.addProperty(RepositoryVocabulary.execution_context,
                model.createResource("context://localhost/implementation/"));

        for (Map.Entry<BigInteger, ContextFile> context : contextHashes.entrySet()) {
            String ctxFileName = context.getValue().getFileName();
            String md5 = Crypto.toHexString(context.getKey().toByteArray());

            // Add the modified context info to the model
            resExecComp.addProperty(RepositoryVocabulary.execution_context,
                    model.createResource(String.format("context://localhost/%s/%s", md5, ctxFileName)));
        }

        return model;
    }

    /**
     * Returns the flow id based on the attributes that determine its uniqueness.
     * This method should be overridden in case the meaning of what constitutes a unique flow
     * is changed from its default assumption that 'uri' values determine the uniqueness of a flow.
     * NOTE: If this method is overridden it is very likely that the SQL query associated with it needs to be changed
     *
     * @param flow The flow whose (subset of) attributes will be used to determine its uniqueness
     * @param conn The DB connection to use
     * @return The flow id, or null if no matching flow was found
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected BigInteger getFlowId(FlowDescription flow, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_FLOW_GET_ID).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, flow.getFlowComponent().getURI());
            ResultSet rs = ps.executeQuery();

            return rs.next() ? rs.getBigDecimal(1).toBigInteger() : null;
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Retrieves the last added version for a particular flow
     *
     * @param flowId The flow id
     * @param conn The DB connection to use
     * @return The last added flow for the given flow id, or null if none found
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected Flow getLastAddedFlow(BigInteger flowId, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_FLOW_GET_LASTINSERT).trim();
        Flow flow = null;
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(flowId));
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                flow = new Flow();
                flow.setFlowId(flowId);
                flow.setFlowCoreHash(rs.getBigDecimal("core_hash").toBigInteger());
                flow.setName(rs.getString("name"));
                flow.setDescription(rs.getString("description"));
                flow.setCreator(rs.getString("creator"));
                flow.setRightsHash(rs.getBigDecimal("rights_hash").toBigInteger());
                flow.setUri(rs.getString("uri"));

                do {
                    flow.addTag(rs.getString("tag"));
                } while (rs.next());
            }

            return flow;
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Retrieves the version id for a particular flow version
     *
     * @param flowId The flow id
     * @param version The flow version
     * @param conn The DB connection to use
     * @return The version id, or null if no results were obtained
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected Long getFlowVersionId(BigInteger flowId, int version, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_FLOW_GET_VERID).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(flowId));
            ps.setInt(2, version-1);
            ResultSet rs = ps.executeQuery();

            return rs.next() ? rs.getTimestamp(1).getTime() : null;
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Retrieves the flow descriptor for a particular flow version
     *
     * @param flowId The flow id
     * @param verId The flow version id
     * @param conn The DB connection to use
     * @return An InputStream to the descriptor, or null if no results were obtained
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected InputStream getFlowDescriptor(BigInteger flowId, long verId, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_FLOW_GET_DESCRIPTOR).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(flowId));
            ps.setTimestamp(2, new Timestamp(verId));
            ResultSet rs = ps.executeQuery();

            return rs.next() ? rs.getBinaryStream(1) : null;
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Adds a flow to the DB
     *
     * @param flowId The flow id
     * @param coreHash The flow core hash
     * @param rightsHash The rights hash
     * @param flow The flow
     * @param conn The DB connection to use
     * @return The flow version id assigned by the DB
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected long addFlow(BigInteger flowId, BigInteger coreHash, BigInteger rightsHash, FlowDescription flow,
                         Connection conn) throws SQLException {
        long timestamp = getCurrentDateTime(conn).getTime();
        PreparedStatement ps = null;

        try {
            // Insert this flow version into the DB
            String sqlQuery = properties.getProperty(DBProperties.Q_FLOW_ADD).trim();
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(flowId));
            ps.setTimestamp(2, new Timestamp(timestamp));
            ps.setBigDecimal(3, new BigDecimal(coreHash));
            ps.setString(4, flow.getName());
            ps.setString(5, flow.getCreator());
            ps.setTimestamp(6, new Timestamp(flow.getCreationDate().getTime()));
            ps.setBigDecimal(7, new BigDecimal(rightsHash));
            ps.setString(8, flow.getFlowComponent().getURI());
            ps.executeUpdate();
        }
        finally {
            closeStatement(ps);
            ps = null;
        }

        try {
            // Insert the flow description
            String sqlQuery = properties.getProperty(DBProperties.Q_FLOW_ADD_DESCRIPTION).trim();
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(flowId));
            ps.setTimestamp(2, new Timestamp(timestamp));
            ps.setString(3, flow.getDescription());
            ps.executeUpdate();
        }
        finally {
            closeStatement(ps);
            ps = null;
        }

        try {
            // Insert the flow tags
            String sqlQuery = properties.getProperty(DBProperties.Q_FLOW_ADD_TAG).trim();
            ps = conn.prepareStatement(sqlQuery);
            for (String tag : flow.getTags().getTags()) {
                ps.setBigDecimal(1, new BigDecimal(flowId));
                ps.setTimestamp(2, new Timestamp(timestamp));
                ps.setString(3, tag);
                ps.addBatch();
            }
            ps.executeBatch();
        }
        finally {
            closeStatement(ps);
            ps = null;
        }

        Model model = flow.getModel();
        byte[] modelData = ModelUtils.modelToByteArray(model, "TURTLE");
        try {
            // Insert the flow descriptor
            String sqlQuery = properties.getProperty(DBProperties.Q_FLOW_ADD_DESCRIPTOR).trim();
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(flowId));
            ps.setTimestamp(2, new Timestamp(timestamp));
            ps.setBinaryStream(3, new ByteArrayInputStream(modelData), modelData.length);
            ps.executeUpdate();
        }
        finally {
            closeStatement(ps);
            ps = null;
        }

        return timestamp;
    }

    /**
     * Adds a new event
     *
     * @param eventCode The event code
     * @param userId The user id (or null if not applicable)
     * @param groupId The group id (or null if not applicable)
     * @param compId The component id (or null if not applicable)
     * @param flowId The flow id (or null if not applicable)
     * @param metadata The event metadata (or null)
     * @param conn The DB connection to use
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected void addEvent(SCEvent eventCode, BigInteger userId, BigInteger groupId, BigInteger compId,
                            BigInteger flowId, JSONObject metadata, Connection conn) throws SQLException {

        String sqlQuery = properties.getProperty(DBProperties.Q_EVENT_ADD).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setInt(1, eventCode.getEventCode());

            if (userId != null)
                ps.setBigDecimal(2, new BigDecimal(userId));
            else
                ps.setNull(2, Types.DECIMAL);

            if (groupId != null)
                ps.setBigDecimal(3, new BigDecimal(groupId));
            else
                ps.setNull(3, Types.DECIMAL);

            if (compId != null)
                ps.setBigDecimal(4, new BigDecimal(compId));
            else
                ps.setNull(4, Types.DECIMAL);

            if (flowId != null)
                ps.setBigDecimal(5, new BigDecimal(flowId));
            else
                ps.setNull(5, Types.DECIMAL);

            if (metadata != null)
                ps.setString(6, metadata.toString());
            else
                ps.setNull(6, Types.VARCHAR);

            ps.executeUpdate();
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Get the role id for a role name
     *
     * @param roleName The role name
     * @param conn The DB connection to use
     * @return The role id, or null if the role name is not known
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected Integer getRoleId(String roleName, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_ROLE_GET_ID).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, roleName);
            ResultSet rs = ps.executeQuery();

            return rs.next() ? rs.getInt(1) : null;
        }
        finally {
            closeStatement(ps);
        }
    }

    /**
     * Removes a user from the pending members list of a group
     *
     * @param gid The group id
     * @param uid The user id
     * @param conn The DB connection to use
     * @return True if the user was on the list of pending group members, False otherwise
     * @throws SQLException Thrown if an error occurred while communicating with the SQL server
     */
    protected boolean deletePendingMember(BigInteger gid, BigInteger uid, Connection conn) throws SQLException {
        String sqlQuery = properties.getProperty(DBProperties.Q_GROUP_PENDING_DELETE).trim();
        PreparedStatement ps = null;

        try {
            ps = conn.prepareStatement(sqlQuery);
            ps.setBigDecimal(1, new BigDecimal(gid));
            ps.setBigDecimal(2, new BigDecimal(uid));

            return ps.executeUpdate() == 1;
        }
        finally {
            closeStatement(ps);
        }
    }


    protected class Component {
        Long        comp_ver_id = null;
        BigInteger  comp_uuid = null;
        BigInteger  comp_hash = null;
        String      name = null;
        String      creator = null;
        String      description = null;
        BigInteger  rights_hash = null;
        Date        creation_date = null;
        Set<String> tags = null;
        String      firing_policy = null;
        String      mode = null;
        String      format = null;
        String      runnable = null;
        String      res_location = null;
        String      uri = null;
        Boolean     deleted = null;

        public Long getComponentVersionId() {
            return comp_ver_id;
        }

        public void setComponentVersionId(Long comp_ver_id) {
            this.comp_ver_id = comp_ver_id;
        }

        public BigInteger getComponentId() {
            return comp_uuid;
        }

        public void setComponentId(BigInteger comp_uuid) {
            this.comp_uuid = comp_uuid;
        }

        public BigInteger getComponentCoreHash() {
            return comp_hash;
        }

        public void setComponentCoreHash(BigInteger comp_hash) {
            this.comp_hash = comp_hash;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCreator() {
            return creator;
        }

        public void setCreator(String creator) {
            this.creator = creator;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigInteger getRightsHash() {
            return rights_hash;
        }

        public void setRightsHash(BigInteger rights_hash) {
            this.rights_hash = rights_hash;
        }

        public Date getCreationDate() {
            return creation_date;
        }

        public void setCreationDate(Date creation_date) {
            this.creation_date = creation_date;
        }

        public Set<String> getTags() {
            return tags;
        }

        public void addTag(String tag) {
            if (tags == null)
                tags = new HashSet<String>();
            tags.add(tag);
        }

        public String getFiringPolicy() {
            return firing_policy;
        }

        public void setFiringPolicy(String firing_policy) {
            this.firing_policy = firing_policy;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public String getRunnable() {
            return runnable;
        }

        public void setRunnable(String runnable) {
            this.runnable = runnable;
        }

        public String getResLocation() {
            return res_location;
        }

        public void setResLocation(String res_location) {
            this.res_location = res_location;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public Boolean isDeleted() {
            return deleted;
        }

        public void setDeleted(Boolean deleted) {
            this.deleted = deleted;
        }
    }

    protected class Flow {
        Long        flow_ver_id = null;
        BigInteger  flow_uuid = null;
        BigInteger  flow_hash = null;
        String      name = null;
        String      creator = null;
        String      description = null;
        BigInteger  rights_hash = null;
        Date        creation_date = null;
        Set<String> tags = null;
        String      uri = null;
        Boolean     deleted = null;

        public Long getFlowVersionId() {
            return flow_ver_id;
        }

        public void setFlowVersionId(Long flow_ver_id) {
            this.flow_ver_id = flow_ver_id;
        }

        public BigInteger getFlowId() {
            return flow_uuid;
        }

        public void setFlowId(BigInteger flow_uuid) {
            this.flow_uuid = flow_uuid;
        }

        public BigInteger getFlowCoreHash() {
            return flow_hash;
        }

        public void setFlowCoreHash(BigInteger flow_hash) {
            this.flow_hash = flow_hash;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCreator() {
            return creator;
        }

        public void setCreator(String creator) {
            this.creator = creator;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public BigInteger getRightsHash() {
            return rights_hash;
        }

        public void setRightsHash(BigInteger rights_hash) {
            this.rights_hash = rights_hash;
        }

        public Date getCreationDate() {
            return creation_date;
        }

        public void setCreationDate(Date creation_date) {
            this.creation_date = creation_date;
        }

        public Set<String> getTags() {
            return tags;
        }

        public void addTag(String tag) {
            if (tags == null)
                tags = new HashSet<String>();
            tags.add(tag);
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public Boolean isDeleted() {
            return deleted;
        }

        public void setDeleted(Boolean deleted) {
            this.deleted = deleted;
        }
    }

    protected class ContextFile {
        private final String fileName;
        private final File file;
        private final String contentType;

        public ContextFile(String fileName, File file, String contentType) {
            this.fileName = fileName;
            this.file = file;
            this.contentType = contentType;
        }

        public String getFileName() {
            return fileName;
        }

        public File getFile() {
            return file;
        }

        public String getContentType() {
            return contentType;
        }
    }
}

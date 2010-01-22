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
import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.json.JSONArray;
import org.json.JSONObject;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.FlowDescription;
import org.seasr.central.storage.BackendStoreLink;
import org.seasr.central.storage.Event;
import org.seasr.central.storage.SourceType;
import org.seasr.central.storage.db.properties.DBProperties;
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.util.SCLogFormatter;

import java.beans.PropertyVetoException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic SQL backend store link driver
 *
 * @author Xavier Llora
 * @author Boris Capitanu
 */
public class SQLLink implements BackendStoreLink {

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
        try {
            // Initialize the database (create tables, etc.)
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);

            // Create the authentication schema
            for (String sql : parseSQLString(properties.getProperty(DBProperties.AUTH_SCHEMA)))
                conn.createStatement().executeUpdate(sql);

            // Create the main SC schema
            for (String sql : parseSQLString(properties.getProperty(DBProperties.SC_SCHEMA)))
                conn.createStatement().executeUpdate(sql);

            conn.commit();
        }
        catch (SQLException e) {
            logger.log(Level.SEVERE, null, e);
            rollbackTransaction(conn);
            throw new BackendStoreException(e);
        }
        finally {
            releaseConnection(conn);
        }
    }

    @Override
    public UUID addUser(String userName, String password, JSONObject profile) throws BackendStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeUser(UUID userId) throws BackendStoreException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateUserPassword(UUID userId, String password) throws BackendStoreException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateProfile(UUID userId, JSONObject profile) throws BackendStoreException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public UUID getUserId(String userName) throws BackendStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getUserScreenName(UUID userId) throws BackendStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public JSONObject getUserProfile(UUID userId) throws BackendStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Date getUserCreationTime(UUID userId) throws BackendStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Boolean isUserPasswordValid(UUID userId, String password) throws BackendStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public long userCount() throws BackendStoreException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public JSONArray listUsers(long offset, long count) throws BackendStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addEvent(SourceType sourceType, UUID sourceId, Event event, JSONObject description) throws BackendStoreException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public JSONObject addComponent(UUID userId, ExecutableComponentDescription component, Set<URL> contexts, boolean copyContextFiles) throws BackendStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Model getComponent(UUID componentId, int version) throws BackendStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public InputStream getContextInputStream(String contextId) throws BackendStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public JSONObject addFlow(UUID userId, FlowDescription flow) throws BackendStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Model getFlow(UUID flowId, int version) throws BackendStoreException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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
     * @param resultSet  (Optional) Any ResultSet(s) that need to be closed before the connection is released
     */
    protected void releaseConnection(Connection connection, ResultSet... resultSet) {
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

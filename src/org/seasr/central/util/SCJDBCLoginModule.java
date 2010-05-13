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

package org.seasr.central.util;


import org.mortbay.jetty.plus.jaas.spi.AbstractLoginModule;
import org.mortbay.jetty.plus.jaas.spi.UserInfo;
import org.mortbay.jetty.security.Credential;
import org.mortbay.log.Log;
import org.mortbay.util.Loader;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author Boris Capitanu
 */
public class SCJDBCLoginModule extends AbstractLoginModule {
    private String dbDriver;
    private String dbUrl;
    private String dbUserName;
    private String dbPassword;

    private String userQuery;
    private String rolesQuery;

    private String dbUserTable;
    private String dbUserTableKey;
    private String dbUserTableUserField;
    private String dbUserTablePasswordField;

    private String dbRoleTable;
    private String dbRoleTableKey;
    private String dbRoleTableRoleField;

    private String dbUserRoleTable;
    private String dbUserRoleTableUserKey;
    private String dbUserRoleTableRoleKey;

    private boolean debug = false;


    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options) {
        try {
            super.initialize(subject, callbackHandler, sharedState, options);

            dbDriver                 = (String) options.get("jdbcdriver");
            dbUrl                    = (String) options.get("url");
            dbUserName               = (String) options.get("username");
            dbPassword               = (String) options.get("password");
            dbUserTable              = (String) options.get("usertable");
            dbUserTableKey           = (String) options.get("usertablekey");
            dbUserTableUserField     = (String) options.get("usertableuserfield");
            dbUserTablePasswordField = (String) options.get("usertablepasswordfield");
            dbRoleTable              = (String) options.get("roletable");
            dbRoleTableKey           = (String) options.get("roletablekey");
            dbRoleTableRoleField     = (String) options.get("roletablerolefield");
            dbUserRoleTable          = (String) options.get("userroletable");
            dbUserRoleTableUserKey   = (String) options.get("userroletableuserkey");
            dbUserRoleTableRoleKey   = (String) options.get("userroletablerolekey");
            debug                    = Boolean.parseBoolean((String) options.get("debug"));

            if (dbUserName == null)
                dbUserName = "";

            if (dbPassword == null)
                dbPassword = "";

            if (dbDriver != null)
                Loader.loadClass(getClass(), dbDriver).newInstance();

            userQuery = String.format("SELECT %s, %s FROM %s WHERE %s = ?",
                    dbUserTableKey, dbUserTablePasswordField, dbUserTable, dbUserTableUserField);
            rolesQuery = String.format("SELECT r.%s FROM %s r INNER JOIN %s ur ON r.%s = ur.%s WHERE ur.%s = ?",
                    dbRoleTableRoleField, dbRoleTable, dbUserRoleTable, dbUserRoleTableRoleKey, dbRoleTableKey, dbUserRoleTableUserKey);

            if (debug) {
                Log.debug("userQuery: " + userQuery);
                Log.debug("rolesQuery: " + rolesQuery);
            }
        }
        catch (Exception e) {
            throw new IllegalStateException(getClass().getSimpleName() + " initialize failed", e);
        }
    }

    @Override
    public UserInfo getUserInfo(String userName) throws Exception {

        Connection connection = null;
        PreparedStatement ps = null;

        try {
            connection = getConnection();

            // Query for credential
            ps = connection.prepareStatement(userQuery);
            ps.setString(1, userName);
            ResultSet rs = ps.executeQuery();

            BigDecimal userKey = null;
            String userPassword = null;

            if (rs.next()) {
                userKey = rs.getBigDecimal(1);
                userPassword = rs.getString(2);
            }

            rs.close();
            ps.close();

            if (userKey == null) return null;

            // Query for role names
            ps = connection.prepareStatement(rolesQuery);
            ps.setBigDecimal(1, userKey);
            rs = ps.executeQuery();

            List roles = new ArrayList();

            while (rs.next()) {
                String roleName = rs.getString(1);
                roles.add(roleName);
            }

            rs.close();
            ps.close();

            return userPassword == null ? null : new UserInfo(userName, Credential.getCredential(userPassword), roles);
        }
        finally {
            if (ps != null)
                ps.close();

            if (connection != null)
                connection.close();
        }
    }

    public Connection getConnection() throws Exception {
        if (dbDriver == null || dbUrl == null)
            throw new IllegalStateException("Database connection information not configured");

        return DriverManager.getConnection(dbUrl, dbUserName, dbPassword);
    }
}

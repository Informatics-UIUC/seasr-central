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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
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

    private static final Logger logger;

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

        Level logLevel = Level.parse(properties.getProperty(DBProperties.LOG_LEVEL, "OFF"));
        if (logLevel != Level.OFF) {
            String logFile = properties.getProperty(DBProperties.LOG_FILE);
            if (logFile != null) {
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

        //Connection conn
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
}

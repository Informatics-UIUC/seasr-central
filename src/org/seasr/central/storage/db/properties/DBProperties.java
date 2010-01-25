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

package org.seasr.central.storage.db.properties;

/**
 * Database configuration property names
 *
 * @author Boris Capitanu
 */
public abstract class DBProperties {
    public static final String DRIVER = "org.seasr.central.storage.db.driver";
    public static final String JDBC_URL = "org.seasr.central.storage.db.url";
    public static final String USER = "org.seasr.central.storage.db.user";
    public static final String PASSWORD = "org.seasr.central.storage.db.password";
    public static final String STORAGE_LINK = "org.seasr.central.storage.link";
    public static final String LOG_FILE = "org.seasr.central.storage.db.logfile";
    public static final String LOG_LEVEL = "org.seasr.central.storage.db.loglevel";

    public static final String AUTH_SCHEMA = "org.seasr.central.storage.db.auth_schema";
    public static final String SC_SCHEMA = "org.seasr.central.storage.db.schema";

    public static final String Q_USER_ADD = "org.seasr.central.storage.db.query.user.add";
    public static final String Q_USER_REMOVE = "org.seasr.central.storage.db.query.user.remove";
    public static final String Q_USER_UPDATE_PASSWORD = "org.seasr.central.storage.db.query.user.update.password";
    public static final String Q_USER_UPDATE_PROFILE = "org.seasr.central.storage.db.query.user.update.profile";
    public static final String Q_USER_GET_UUID = "org.seasr.central.storage.db.query.user.get.uuid";
    public static final String Q_USER_GET_SCREENNAME = "org.seasr.central.storage.db.query.user.get.screen_name";
    public static final String Q_USER_GET_PROFILE = "org.seasr.central.storage.db.query.user.get.profile";
    public static final String Q_USER_GET_CREATEDAT = "org.seasr.central.storage.db.query.user.get.createdat";
    public static final String Q_USER_GET_DELETED = "org.seasr.central.storage.db.query.user.get.deleted";
    public static final String Q_USER_PASSWORDVALID = "org.seasr.central.storage.db.query.user.password.valid";
    public static final String Q_USER_COUNT = "org.seasr.central.storage.db.query.user.count";
    public static final String Q_USER_LIST = "org.seasr.central.storage.db.query.user.list";

    public static final String Q_EVENT_ADD = "org.seasr.central.storage.db.query.event.add";
}

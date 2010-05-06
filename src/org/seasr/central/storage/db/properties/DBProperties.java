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

    public static final String COMP_VER_COLS = "org.seasr.central.storage.db.sc_component.version";
    public static final String FLOW_VER_COLS = "org.seasr.central.storage.db.sc_flow.version";

    public static final String AUTH_SCHEMA = "org.seasr.central.storage.db.auth_schema";
    public static final String SC_SCHEMA = "org.seasr.central.storage.db.schema";

    public static final String Q_ERROR_MSG = "org.seasr.central.storage.db.querry.error_msg";

    public static final String Q_ROLE_GET_ID = "org.seasr.central.storage.db.query.role.get.id";
    public static final String Q_ROLE_LIST = "org.seasr.central.storage.db.query.role.list";
    public static final String Q_ROLE_EXISTS = "org.seasr.central.storage.db.query.role.exists";

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
    public static final String Q_USER_GROUP_LIST = "org.seasr.central.storage.db.query.user.group.list";
    public static final String Q_USER_GROUP_ISMEMBER = "org.seasr.central.storage.db.query.user.group.is_member";
    public static final String Q_USER_COMPONENT_ADD = "org.seasr.central.storage.db.query.user.component.add";
    public static final String Q_USER_COMPONENT_LIST_ALL = "org.seasr.central.storage.db.query.user.component.list.all";
    public static final String Q_USER_COMPONENT_LIST_LATEST = "org.seasr.central.storage.db.query.user.component.list.latest";
    public static final String Q_USER_COMPONENT_SHARING_LIST_ALL = "org.seasr.central.storage.db.query.user.component.sharing.list.all";
    public static final String Q_USER_COMPONENT_SHARING_LIST_ALL_ASUSER = "org.seasr.central.storage.db.query.user.component.sharing.list.all.as_user";
    public static final String Q_USER_COMPONENT_SHARING_LIST_LATEST_ASUSER = "org.seasr.central.storage.db.query.user.component.sharing.list.latest.as_user";

    public static final String Q_USER_FLOW_ADD = "org.seasr.central.storage.db.query.user.flow.add";
    public static final String Q_USER_FLOW_LIST_ALL = "org.seasr.central.storage.db.query.user.flow.list.all";
    public static final String Q_USER_FLOW_LIST_LATEST = "org.seasr.central.storage.db.query.user.flow.list.latest";
    public static final String Q_USER_FLOW_SHARING_LIST_ALL = "org.seasr.central.storage.db.query.user.flow.sharing.list.all";
    public static final String Q_USER_FLOW_SHARING_LIST_ALL_ASUSER = "org.seasr.central.storage.db.query.user.flow.sharing.list.all.as_user";
    public static final String Q_USER_FLOW_SHARING_LIST_LATEST_ASUSER = "org.seasr.central.storage.db.query.user.flow.sharing.list.latest.as_user";

    public static final String Q_GROUP_ADD = "org.seasr.central.storage.db.query.group.add";
    public static final String Q_GROUP_LIST = "org.seasr.central.storage.db.query.group.list";
    public static final String Q_GROUP_GET_UUID = "org.seasr.central.storage.db.query.group.get.uuid";
    public static final String Q_GROUP_GET_NAME = "org.seasr.central.storage.db.query.group.get.name";
    public static final String Q_GROUP_GET_PROFILE = "org.seasr.central.storage.db.query.group.get.profile";
    public static final String Q_GROUP_GET_CREATEDAT = "org.seasr.central.storage.db.query.group.get.createdat";
    public static final String Q_GROUP_GET_DELETED = "org.seasr.central.storage.db.query.group.get.deleted";
    public static final String Q_GROUP_IS_USERINROLE = "org.seasr.central.storage.db.query.group.is.userinrole";

    public static final String Q_GROUP_PENDING_ADD = "org.seasr.central.storage.db.query.group.pending.add";
    public static final String Q_GROUP_PENDING_LIST = "org.seasr.central.storage.db.query.group.pending.list";
    public static final String Q_GROUP_PENDING_DELETE = "org.seasr.central.storage.db.query.group.pending.delete";
    public static final String Q_GROUP_MEMBERS_ADD = "org.seasr.central.storage.db.query.group.members.add";
    public static final String Q_GROUP_MEMBERS_LIST = "org.seasr.central.storage.db.query.group.members.list";
    public static final String Q_GROUP_COMPONENTS_LIST_ALL = "org.seasr.central.storage.db.query.group.components.list.all";
    public static final String Q_GROUP_COMPONENTS_LIST_LATEST = "org.seasr.central.storage.db.query.group.components.list.latest";
    public static final String Q_GROUP_FLOWS_LIST_ALL = "org.seasr.central.storage.db.query.group.flows.list.all";
    public static final String Q_GROUP_FLOWS_LIST_LATEST = "org.seasr.central.storage.db.query.group.flows.list.latest";

    public static final String Q_EVENT_ADD = "org.seasr.central.storage.db.query.event.add";

    public static final String Q_RIGHTS_ADD = "org.seasr.central.storage.db.query.rights.add";
    public static final String Q_RIGHTS_GET_TEXT = "org.seasr.central.storage.db.query.rights.get.text";

    public static final String Q_DATETIME_NOW = "org.seasr.central.storage.db.query.datetime.now";

    public static final String Q_COMP_ADD = "org.seasr.central.storage.db.query.component.add";
    public static final String Q_COMP_ADD_DESCRIPTION = "org.seasr.central.storage.db.query.component.add.description";
    public static final String Q_COMP_ADD_TAG = "org.seasr.central.storage.db.query.component.add.tag";
    public static final String Q_CORE_CONTEXT_ADD = "org.seasr.central.storage.db.query.core.context.add";
    public static final String Q_COMP_CONTEXT_ADD = "org.seasr.central.storage.db.query.component.context.add";
    public static final String Q_COMP_CONTEXT_GET = "org.seasr.central.storage.db.query.component.context.get";
    public static final String Q_COMP_ADD_DESCRIPTOR = "org.seasr.central.storage.db.query.component.add.descriptor";
    public static final String Q_COMP_GET_DESCRIPTOR = "org.seasr.central.storage.db.query.component.get.descriptor";

    public static final String Q_COMP_GET_ID = "org.seasr.central.storage.db.query.component.get.id";
    public static final String Q_COMP_GET_COREHASH = "org.seasr.central.storage.db.query.component.get.core_hash";
    public static final String Q_COMP_GET_LASTINSERT = "org.seasr.central.storage.db.query.component.get.last_insert";
    public static final String Q_COMP_GET_VERCOUNT = "org.seasr.central.storage.db.query.component.get.ver_count";
    public static final String Q_COMP_SHARE = "org.seasr.central.storage.db.query.component.share";
    public static final String Q_COMP_GROUP_LIST = "org.seasr.central.storage.db.query.component.group.list";
    public static final String Q_COMP_GET_VERID = "org.seasr.central.storage.db.query.component.get.ver_id";

    public static final String Q_COMP_CORE_EXISTS = "org.seasr.central.storage.db.query.component.core.exists";
    public static final String Q_COMP_CORE_ADD = "org.seasr.central.storage.db.query.component.core.add";

    public static final String Q_CONTEXT_EXISTS = "org.seasr.central.storage.db.query.context.exists";
    public static final String Q_CONTEXT_ADD = "org.seasr.central.storage.db.query.context.add";

    public static final String Q_COMP_GET_OWNER = "org.seasr.central.storage.db.query.component.get.owner";

    public static final String Q_FLOW_ADD = "org.seasr.central.storage.db.query.flow.add";
    public static final String Q_FLOW_ADD_DESCRIPTION = "org.seasr.central.storage.db.query.flow.add.description";
    public static final String Q_FLOW_ADD_TAG = "org.seasr.central.storage.db.query.flow.add.tag";
    public static final String Q_FLOW_ADD_DESCRIPTOR = "org.seasr.central.storage.db.query.flow.add.descriptor";
    public static final String Q_FLOW_GET_DESCRIPTOR = "org.seasr.central.storage.db.query.flow.get.descriptor";
    public static final String Q_FLOW_GET_ID = "org.seasr.central.storage.db.query.flow.get.id";
    public static final String Q_FLOW_GET_LASTINSERT = "org.seasr.central.storage.db.query.flow.get.last_insert";
    public static final String Q_FLOW_GET_VERCOUNT = "org.seasr.central.storage.db.query.flow.get.ver_count";
    public static final String Q_FLOW_SHARE = "org.seasr.central.storage.db.query.flow.share";
    public static final String Q_FLOW_GROUP_LIST = "org.seasr.central.storage.db.query.flow.group.list";
    public static final String Q_FLOW_GET_VERID = "org.seasr.central.storage.db.query.flow.get.ver_id";

    public static final String Q_FLOW_COMPONENT_ADD = "org.seasr.central.storage.db.query.flow.component.add";
    public static final String Q_FLOW_GET_OWNER = "org.seasr.central.storage.db.query.flow.get.owner";
}

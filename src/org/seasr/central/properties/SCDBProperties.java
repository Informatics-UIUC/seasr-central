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

package org.seasr.central.properties;

/**
 * The properties names for a DB backend link
 *
 * @author xavier
 */
public abstract class SCDBProperties extends SCProperties {

	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_AUTH_SCHEMA = "org.seasr.central.storage.db.auth.schema";

	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_SCHEMA = "org.seasr.central.storage.db.schema";

	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_COUNT = "org.seasr.central.storage.db.query.user.count";
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_ADD = "org.seasr.central.storage.db.query.user.add";
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_REMOVE_UUID = "org.seasr.central.storage.db.query.user.remove.uuid";
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_REMOVE_SCREEN_NAME = "org.seasr.central.storage.db.query.user.remove.screen_name";
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_UUID = "org.seasr.central.storage.db.query.user.get.uuid";
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_SCREEN_NAME = "org.seasr.central.storage.db.query.user.get.screen_name";
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_PROFILE_UUID = "org.seasr.central.storage.db.query.user.get.profile.uuid";
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_PROFILE_SCREEN_NAME = "org.seasr.central.storage.db.query.user.get.profile.screen_name";
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_CREATEDAT_UUID = "org.seasr.central.storage.db.query.user.get.createdat.uuid";
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_GET_CREATEDAT_SCREEN_NAME = "org.seasr.central.storage.db.query.user.get.createdat.screen_name";
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_VALID_PASSWORD_UUID = "org.seasr.central.storage.db.query.user.valid.password.uuid";
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_VALID_PASSWORD_SCREEN_NAME = "org.seasr.central.storage.db.query.user.valid.password.screen_name";
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PROFILE_UUID = "org.seasr.central.storage.db.query.user.update.profile.uuid";
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PROFILE_SCREEN_NAME = "org.seasr.central.storage.db.query.user.update.profile.screen_name";
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PASSWORD_UUID = "org.seasr.central.storage.db.query.user.update.password.uuid";
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_UPDATE_PASSWORD_SCREEN_NAME = "org.seasr.central.storage.db.query.user.update.password.screen_name";
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_USER_LIST = "org.seasr.central.storage.db.query.user.db.list";

	// EVENTS
	public final static String ORG_SEASR_CENTRAL_STORAGE_DB_QUERY_EVENT_ADD = "org.seasr.central.storage.db.query.event.add";
}

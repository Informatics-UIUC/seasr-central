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

package org.seasr.central.ws.restlets;

import com.google.gdata.util.ContentType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.seasr.central.storage.BackendStoreLink;
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.util.IdVersionPair;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base restlet that provides common functionality
 *
 * @author Xavier Llora
 * @author Boris Capitanu
 */
public abstract class AbstractBaseRestlet implements RestServlet {

    /** The restlet logger */
    protected Logger logger;

    /** The back end storage link */
    protected BackendStoreLink bsl;

    private static final Pattern patternExtension;


    static {
        patternExtension = Pattern.compile("\\.([a-z]+)$");
    }

    @Override
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void setBackendStoreLink(BackendStoreLink bsl) {
        this.bsl = bsl;
    }

    /**
     * Returns the supported 'Content-Type' values for this restlet
     * format is: <extension, ContentType>
     *
     * @return The supported content types for this restlet
     */
    public abstract Map<String, ContentType> getSupportedResponseTypes();

    /**
     * Returns a partial regular expression that matches the supported resource extensions
     * Example: \\.txt|\\.json|\\.xml   ---- matches .txt or .json or .xml
     *
     * @return A partial regular expression that matches the supported resource extensions
     */
    public String regexExtensionMatcher() {
        StringBuffer sb = new StringBuffer();
        for (String ext : getSupportedResponseTypes().keySet())
            sb.append("|\\." + ext);

        return sb.toString().substring(1);
    }

    /**
     * Returns the desired content type for this request
     *
     * @param request The request
     * @return The content type desired for the response, or null if couldn't determine
     */
    public ContentType getDesiredResponseContentType(HttpServletRequest request) {
        ContentType ct = null;

        Map<String, ContentType> allowedTypes = getSupportedResponseTypes();
        String reqUri = request.getRequestURI().toLowerCase();
        Matcher m = patternExtension.matcher(reqUri);
        if (m.find())
            ct = allowedTypes.get(m.group(1));
        else {
            String accept = request.getHeader("Accept");
            if (accept != null)
                ct = ContentType.getBestContentType(accept,
                        new ArrayList<ContentType>(allowedTypes.values()));
        }

        return ct;
    }

    /**
     * Interprets a string representing either a screen name or a user id
     *
     * @param screenNameOrUserId The string representing a screen name or a user id
     * @return A property map keyed on "uuid" and "screen_name", or null if either could not be determined
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public Properties getUserScreenNameAndId(String screenNameOrUserId) throws BackendStoreException {
        String screenName;
        UUID userId;

        try {
            userId = UUID.fromString(screenNameOrUserId);
            screenName = bsl.getUserScreenName(userId);
        }
        catch (IllegalArgumentException e) {
            screenName = screenNameOrUserId;
            userId = bsl.getUserId(screenName);
        }

        if (userId == null || screenName == null)
            return null;

        Properties result = new Properties();
        result.put("uuid", userId.toString());
        result.put("screen_name", screenName);

        return result;
    }

    /**
     * Interprets a string representing either a group name or a group id
     *
     * @param groupNameOrId The string representing a group name or a group id
     * @return A property map keyed on "uuid" and "name", or null if either could not be determined
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public Properties getGroupNameAndId(String groupNameOrId) throws BackendStoreException {
        String groupName;
        UUID groupId;

        try {
            groupId = UUID.fromString(groupNameOrId);
            groupName = bsl.getGroupName(groupId);
        }
        catch (IllegalArgumentException e) {
            groupName = groupNameOrId;
            groupId = bsl.getGroupId(groupName);
        }

        if (groupId == null || groupName == null)
            return null;

        Properties result = new Properties();
        result.put("uuid", groupId.toString());
        result.put("name", groupName);

        return result;
    }

    /**
     * Retrieves the set of groups that a user belongs to (and makes sure the list includes the "public" group)
     *
     * @param userId The user id (or null if unauthenticated access)
     * @return The set of group ids to which the user belongs
     * @throws org.json.JSONException
     * @throws BackendStoreException
     */
    protected Set<UUID> getAdjustedGroupsForUser(UUID userId) throws JSONException, BackendStoreException {
        Set<UUID> groups = new HashSet<UUID>();

        if (userId != null) {
            JSONArray jaGroups = bsl.listUserGroups(userId, 0, Long.MAX_VALUE);
            for (int i = 0, iMax = jaGroups.length(); i < iMax; i++) {
                JSONObject joGroup = jaGroups.getJSONObject(i);
                groups.add(UUID.fromString(joGroup.getString("uuid")));
            }
        }

        // Make sure everyone belongs to the "public" group
        groups.add(BackendStoreLink.PUBLIC_GROUP);

        return groups;
    }

    /**
     * Builds a data structure that contains the versions of each component or flow and the groups each version is shared with.
     * The versions of components or flows are sorted in decreasing order (highest->lowest)
     *
     * @param jaCompOrFlows The components, versions, and groups with which each version is shared
     * @return The Map
     * @throws JSONException
     */
    protected Map<UUID, SortedMap<Integer, List<UUID>>> buildVersionSharingMap(JSONArray jaCompOrFlows)
            throws JSONException {

        VersionComparator versionComparator = new VersionComparator();

        Map<UUID, SortedMap<Integer, List<UUID>>> map = new HashMap<UUID, SortedMap<Integer, List<UUID>>>();
        for (int i = 0; i < jaCompOrFlows.length(); i++) {
            JSONObject joCompOrFlow = jaCompOrFlows.getJSONObject(i);
            UUID id = UUID.fromString(joCompOrFlow.getString("uuid"));
            int version = joCompOrFlow.getInt("version");
            JSONArray jaGroups = joCompOrFlow.getJSONArray("groups");

            SortedMap<Integer, List<UUID>> revMap = map.get(id);
            if (revMap == null) {
                revMap = new TreeMap<Integer, List<UUID>>(versionComparator);
                map.put(id, revMap);
            }

            List<UUID> groups = new ArrayList<UUID>();
            for (int j = 0, jMax = jaGroups.length(); j < jMax; j++)
                groups.add(UUID.fromString(jaGroups.getString(j)));

            revMap.put(version, groups);
        }
        return map;
    }

    /**
     * Comparator that sorts version numbers in descending order
     */
    class VersionComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer v1, Integer v2) {
            return (-1) * v1.compareTo(v2);   // descending order
        }
    }

    /**
     * Computes the list of accessible components or flows based on the group participation status
     *
     * @param compOrFlowMap The component or flow sharing map
     * @param remoteUserGroups The groups the remote user belongs to (or null if remote user = user queried)
     * @param includeOldVersions True to include old versions, false otherwise
     * @return The list of accessible components or flows
     */
    protected List<IdVersionPair> getAccessibleCompsOrFlows(Map<UUID, SortedMap<Integer, List<UUID>>> compOrFlowMap,
                                               Set<UUID> remoteUserGroups, boolean includeOldVersions) {

        List<IdVersionPair> accessibleCompsOrFlows = new ArrayList<IdVersionPair>();

        for (Map.Entry<UUID, SortedMap<Integer, List<UUID>>> compOrFlow : compOrFlowMap.entrySet()) {
            for (Map.Entry<Integer, List<UUID>> rev : compOrFlow.getValue().entrySet()) {
                // Check whether this component version is shared with any groups that the remote user belongs to
                boolean allowedAccess = false;
                if (remoteUserGroups == null)
                    // By default allow access if the user making the request is the same as the one queried
                    allowedAccess = true;
                else
                    for (UUID groupId : rev.getValue())
                        if (remoteUserGroups.contains(groupId)) {
                            allowedAccess = true;
                            break;
                        }

                if (allowedAccess) {
                    accessibleCompsOrFlows.add(new IdVersionPair(compOrFlow.getKey(), rev.getKey()));
                    if (!includeOldVersions)
                        break;
                }
            }
        }

        return accessibleCompsOrFlows;
    }
}

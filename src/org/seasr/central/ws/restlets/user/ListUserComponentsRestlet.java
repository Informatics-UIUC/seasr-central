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

package org.seasr.central.ws.restlets.user;

import com.google.gdata.util.ContentType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.seasr.central.storage.BackendStoreLink;
import org.seasr.central.storage.exceptions.BackendStoreException;
import org.seasr.central.util.IdVersionPair;
import org.seasr.central.util.Tools;
import org.seasr.central.ws.restlets.AbstractBaseRestlet;
import org.seasr.central.ws.restlets.ContentTypes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

import static org.seasr.central.util.Tools.*;

/**
 * Restlet for obtaining the list of components uploaded by a user
 *
 * @author Boris Capitanu
 */
public class ListUserComponentsRestlet extends AbstractBaseRestlet {

    private static final Map<String, ContentType> supportedResponseTypes = new HashMap<String, ContentType>();

    static {
        supportedResponseTypes.put("json", ContentType.JSON);
        supportedResponseTypes.put("xml", ContentType.APPLICATION_XML);
        supportedResponseTypes.put("html", ContentType.TEXT_HTML);
        supportedResponseTypes.put("txt", ContentType.TEXT_PLAIN);
        supportedResponseTypes.put("sgwt", ContentTypes.SmartGWT);
    }

    @Override
    public Map<String, ContentType> getSupportedResponseTypes() {
        return supportedResponseTypes;
    }

    @Override
    public String getRestContextPathRegexp() {
        return "/services/users/([^/\\s]+)/components(?:/|" + regexExtensionMatcher() + ")?$";
    }

    @Override
    public boolean process(HttpServletRequest request, HttpServletResponse response, String method, String... values) {
        // check for GET
        if (!method.equalsIgnoreCase("GET")) return false;

        ContentType ct = getDesiredResponseContentType(request);
        if (ct == null) {
            sendErrorNotAcceptable(response);
            return true;
        }

        UUID userId;
        String screenName;

        UUID remoteUserId;
        String remoteUser = request.getRemoteUser();

        //TODO: for test purposes
        if (request.getParameterMap().containsKey("remoteUser") && request.getParameter("remoteUser").trim().length() > 0)
            remoteUser = request.getParameter("remoteUser");

        try {
            Properties userProps = getUserScreenNameAndId(values[0]);
            if (userProps != null) {
                userId = UUID.fromString(userProps.getProperty("uuid"));
                screenName = userProps.getProperty("screen_name");
            } else {
                // Specified user does not exist
                sendErrorNotFound(response);
                return true;
            }

            remoteUserId = (remoteUser != null) ? bsl.getUserId(remoteUser) : null;
        }
        catch (BackendStoreException e) {
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
            return true;
        }

        long offset = 0;
        long count = Long.MAX_VALUE;

        String sOffset = request.getParameter("offset");
        String sCount = request.getParameter("count");

        try {
            if (sOffset != null) offset = Long.parseLong(sOffset);
            if (sCount != null) count = Long.parseLong(sCount);
        }
        catch (NumberFormatException e) {
            logger.log(Level.WARNING, null, e);
            sendErrorBadRequest(response);
            return true;
        }

        boolean includeOldVersions = false;
        if (request.getParameterMap().containsKey("includeOldVersions"))
            includeOldVersions = Boolean.parseBoolean(request.getParameter("includeOldVersions"));

        JSONArray jaSuccess = new JSONArray();
        JSONArray jaErrors = new JSONArray();

        try {
            try {
                // Get the list of all the versions of all components owned by a user and
                // the groups each version is shared with
                JSONArray jaResult = bsl.listUserComponents(userId, offset, count);

                // Build a data structure that sorts the versions of each component in decreasing order (highest->lowest)
                Map<UUID, SortedMap<Integer, List<UUID>>> compMap = buildComponentVersionSharingMap(jaResult);

                // Get the set of groups that the remote user belongs to (or null if the remote user is the same as the user queried)
                Set<UUID> remoteUserGroups = (userId.equals(remoteUserId)) ? null : getAdjustedGroupsForUser(remoteUserId);

                // Compute the list of accessible components based on the group participation status
                List<IdVersionPair> accessibleComponents = getAccessibleComponents(compMap, remoteUserGroups, includeOldVersions);
                for (IdVersionPair comp : accessibleComponents) {
                    String compId = comp.getId().toString();
                    JSONObject joResult = new JSONObject();
                    joResult.put("uuid", compId);
                    joResult.put("version", comp.getVersion());
                    joResult.put("url", getComponentBaseAccessUrl(request, compId, comp.getVersion()) + ".ttl");
                    jaSuccess.put(joResult);
                }
            }
            catch (BackendStoreException e) {
                logger.log(Level.SEVERE, null, e);
                jaErrors.put(createJSONErrorObj("Cannot obtain the component list for user " + userId, e));
            }

            JSONObject joContent = new JSONObject();
            joContent.put(Tools.OperationResult.SUCCESS.name(), jaSuccess);
            joContent.put(Tools.OperationResult.FAILURE.name(), jaErrors);

            response.setStatus(HttpServletResponse.SC_OK);

            try {
                sendContent(response, joContent, ct);
            }
            catch (IOException e) {
                logger.log(Level.WARNING, null, e);
            }
        }
        catch (JSONException e) {
            // Should not happen
            logger.log(Level.SEVERE, null, e);
            sendErrorInternalServerError(response);
            return true;
        }

        return true;
    }

    /**
     * Computes the list of accessible components based on the group participation status
     *
     * @param compMap The component sharing map
     * @param remoteUserGroups The groups the remote user belongs to (or null if remote user = user queried)
     * @param includeOldVersions True to include old versions, false otherwise
     * @return The list of accessible components
     */
    protected List<IdVersionPair> getAccessibleComponents(Map<UUID, SortedMap<Integer, List<UUID>>> compMap,
                                               Set<UUID> remoteUserGroups, boolean includeOldVersions) {

        List<IdVersionPair> accessibleComponents = new ArrayList<IdVersionPair>();

        for (Map.Entry<UUID, SortedMap<Integer, List<UUID>>> comp : compMap.entrySet()) {
            for (Map.Entry<Integer, List<UUID>> rev : comp.getValue().entrySet()) {
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
                    accessibleComponents.add(new IdVersionPair(comp.getKey(), rev.getKey()));
                    if (!includeOldVersions)
                        break;
                }
            }
        }

        return accessibleComponents;
    }

    /**
     * Builds a data structure that contains the versions of each component and the groups each version is shared with.
     * The versions of components are sorted in decreasing order (highest->lowest)
     *
     * @param jaComponents The components, versions, and groups with which each version is shared
     * @return The Map
     * @throws JSONException
     */
    protected Map<UUID, SortedMap<Integer, List<UUID>>> buildComponentVersionSharingMap(JSONArray jaComponents)
            throws JSONException {

        VersionComparator versionComparator = new VersionComparator();

        Map<UUID, SortedMap<Integer, List<UUID>>> compMap = new HashMap<UUID, SortedMap<Integer, List<UUID>>>();
        for (int i = 0; i < jaComponents.length(); i++) {
            JSONObject joCompVer = jaComponents.getJSONObject(i);
            UUID compId = UUID.fromString(joCompVer.getString("uuid"));
            int compVersion = joCompVer.getInt("version");
            JSONArray jaGroups = joCompVer.getJSONArray("groups");

            SortedMap<Integer, List<UUID>> revMap = compMap.get(compId);
            if (revMap == null) {
                revMap = new TreeMap<Integer, List<UUID>>(versionComparator);
                compMap.put(compId, revMap);
            }

            List<UUID> groups = new ArrayList<UUID>();
            for (int j = 0, jMax = jaGroups.length(); j < jMax; j++)
                groups.add(UUID.fromString(jaGroups.getString(j)));

            revMap.put(compVersion, groups);
        }
        return compMap;
    }

    /**
     * Retrieves the set of groups that a user belongs to (and makes sure the list includes the "public" group)
     *
     * @param userId The user id (or null if unauthenticated access)
     * @return The set of group ids to which the user belongs
     * @throws JSONException
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
     * Comparator that sorts version numbers in descending order
     */
    class VersionComparator implements Comparator<Integer> {
        @Override
        public int compare(Integer v1, Integer v2) {
            return (-1) * v1.compareTo(v2);   // descending order
        }
    }
}

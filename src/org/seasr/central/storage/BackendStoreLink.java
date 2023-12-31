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

package org.seasr.central.storage;

import com.hp.hpl.jena.rdf.model.Model;
import org.json.JSONArray;
import org.json.JSONObject;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.FlowDescription;
import org.seasr.central.storage.exceptions.*;
import org.seasr.central.ws.restlets.ComponentContext;

import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * The interface that any back end storage driver must implement
 *
 * @author Xavier Llora
 * @author Boris Capitanu
 */
public interface BackendStoreLink {
    public static final UUID PUBLIC_GROUP = new UUID(0, 0);
    public static final UUID ADMIN_UUID = new UUID(0, 1);

    /**
     * Initialize the backend storage link with the given properties.
     *
     * @param properties The properties required to initialize the backend link
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public void init(Properties properties) throws BackendStoreException;

    //-------------------------------------------------------------------------------------

    public String getErrorMessage(SCError error) throws BackendStoreException;

    public JSONArray listRoles(long offset, long count) throws BackendStoreException;
    public boolean hasRole(String roleName) throws BackendStoreException;

    /**
     * Adds a user to the back end storage facility
     *
     * @param userName The user screen name
     * @param password The user password
     * @param profile  The user profile data
     * @return The id assigned to the created user
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public UUID addUser(String userName, String password, JSONObject profile) throws BackendStoreException;

    /**
     * Removes a user from the back end storage facility
     *
     * @param userId The user id
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public void removeUser(UUID userId) throws BackendStoreException, UserNotFoundException;

    /**
     * Updates a user's password
     *
     * @param userId   The user id
     * @param password The new password
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public void updateUserPassword(UUID userId, String password) throws BackendStoreException, UserNotFoundException;

    /**
     * Updates a user's profile
     *
     * @param userId  The user id
     * @param profile The new profile
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public void updateUserProfile(UUID userId, JSONObject profile) throws BackendStoreException, UserNotFoundException;

    /**
     * Returns the id of a user given the screen name
     *
     * @param userName The user's screen name
     * @return The user id or null if the user does not exist
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public UUID getUserId(String userName) throws UserNotFoundException, BackendStoreException;

    /**
     * Returns the screen name of a user given the user id
     *
     * @param userId The user id
     * @return The screen name of the user or null if the user does not exist
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public String getUserScreenName(UUID userId) throws BackendStoreException, UserNotFoundException;

    /**
     * Returns the profile of a user given the user id
     *
     * @param userId The user id
     * @return The profile of the user or null if the user does not exist
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public JSONObject getUserProfile(UUID userId) throws BackendStoreException, UserNotFoundException;

    /**
     * Returns the creation time of a user given the user id
     *
     * @param userId The user id
     * @return The creation time of the user or null if the user does not exist
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public Date getUserCreationTime(UUID userId) throws BackendStoreException, UserNotFoundException;

    /**
     * Checks if a user's password is valid based on the user id
     *
     * @param userId   The user id
     * @param password The password
     * @return True if the password matches, False otherwise (also returned for nonexistent or deleted user)
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public boolean isUserPasswordValid(UUID userId, String password) throws BackendStoreException, UserNotFoundException;

    /**
     * Returns the number of users
     *
     * @return The number of users
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public long getUserCount() throws BackendStoreException;

    /**
     * Lists the users stored in the backend store
     *
     * @param offset The offset where to start
     * @param count  The number of users to be returned
     * @return The list of users
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public JSONArray listUsers(long offset, long count) throws BackendStoreException;


    public UUID createGroup(UUID userId, String groupName, JSONObject profile) throws BackendStoreException, UserNotFoundException;
    public JSONArray listGroups(long offset, long count) throws BackendStoreException;
    public UUID getGroupId(String groupName) throws BackendStoreException, GroupNotFoundException;
    public String getGroupName(UUID groupId) throws GroupNotFoundException, BackendStoreException;
    public JSONObject getGroupProfile(UUID groupId) throws BackendStoreException, GroupNotFoundException;
    public Date getGroupCreationTime(UUID groupId) throws BackendStoreException, GroupNotFoundException;
    public boolean isUserInGroupRole(UUID userId, UUID groupId, SCRole role) throws BackendStoreException, UserNotFoundException, GroupNotFoundException;
    public void addPendingGroupMember(UUID userId, UUID groupId) throws BackendStoreException, UserNotFoundException, GroupNotFoundException;
    public JSONArray listPendingGroupMembers(UUID groupId, long offset, long count) throws BackendStoreException, GroupNotFoundException;
    public void addGroupMember(UUID userId, UUID groupId, SCRole role) throws BackendStoreException, UserNotFoundException, GroupNotFoundException;
    public boolean isGroupMember(UUID userId, UUID groupId) throws BackendStoreException, UserNotFoundException, GroupNotFoundException;
    public JSONArray listGroupMembers(UUID groupId, long offset, long count) throws BackendStoreException, GroupNotFoundException;
    public JSONArray listUserGroups(UUID userId, long offset, long count) throws BackendStoreException, UserNotFoundException;
    public JSONArray listComponentGroupsAsUser(UUID componentId, int version, UUID remoteUserId, long offset, long count)
            throws BackendStoreException, ComponentNotFoundException, UserNotFoundException;
    public JSONArray listFlowGroupsAsUser(UUID flowId, int version, UUID remoteUserId, long offset, long count)
            throws BackendStoreException, FlowNotFoundException, UserNotFoundException;

    /**
     * Adds (or updates) a component
     *
     * @param userId           The user to be credited with the upload
     * @param component        The component
     * @param contexts         The component context files as <URL, ContentType>
     * @return A JSON object keyed on uuid and version containing information about the component
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public JSONObject addComponent(UUID userId, ExecutableComponentDescription component, Map<URL, String> contexts)
            throws UserNotFoundException, BackendStoreException;

    /**
     * Retrieves a component given the component id and version
     *
     * @param componentId The component id
     * @param version     The version to retrieve
     * @return The component descriptor, or null if the component does not exist
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public Model getComponent(UUID componentId, int version) throws ComponentNotFoundException, BackendStoreException;

    /**
     * Retrieves a component context for a particular component version
     *
     * @param componentId The component id
     * @param version     The component version
     * @param contextId The id of the context file
     * @return The component context, or null if no results were obtained
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public ComponentContext getComponentContext(UUID componentId, int version, String contextId)
            throws BackendStoreException, ComponentNotFoundException, ComponentContextNotFoundException;

    /**
     * Checks whether a component context exists in the backend store
     *
     * @param contextId The id of the context file
     * @return True if exists, False otherwise
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public boolean hasComponentContext(String contextId) throws BackendStoreException;

    public UUID getComponentOwner(UUID componentId, int version) throws BackendStoreException, ComponentNotFoundException;

    /**
     * Returns the version count for a component
     *
     * @param componentId The component id
     * @return The version count, or null if the component does not exist
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public Integer getComponentVersionCount(UUID componentId) throws BackendStoreException, ComponentNotFoundException;

    public void shareComponent(UUID componentId, int version, UUID groupId, UUID remoteUserId) throws BackendStoreException, ComponentNotFoundException, GroupNotFoundException, UserNotFoundException;

    public JSONArray listUserComponents(UUID userId, long offset, long count, boolean includeOldVersions) throws BackendStoreException, UserNotFoundException;

    public JSONArray listPublicUserComponents(UUID userId, long offset, long count, boolean includeOldVersions) throws BackendStoreException, UserNotFoundException;

    /**
     * Retrieves the list of components owned by a user that can be accessed by a remote user
     *
     * Depending on who the remote user is, there are three situations:
     *   1. Remote user is nobody (i.e. unauthenticated request)
     *        In this case the method returns all components owned by the specified user that are PUBLIC
     *   2. Remote user is authenticated, but different than the user being queried
     *        In this case the method returns all components owned by the specified user that can be accessed by
     *        the remote user (i.e. components shared with a group to which the remote user belongs, or PUBLIC components)
     *   3. Remote user is authenticated and the same as the user being queried (i.e. querying one's own components)
     *        In this case the method returns all the components the user owns (public, shared, and private)
     *
     * @param userId The user id of the user whose components should be retrieved
     * @param remoteUserId The id of the remote "authenticated" user, or null if non-authenticated request
     * @param offset Offset into the list to start from
     * @param count The number of components to retrieve
     * @param includeOldVersions True to include old versions of components, False to only return latest versions
     * @return The list of components owned by the specified user that can be accessed by the remote user
     * @throws BackendStoreException
     */
    public JSONArray listAccessibleUserComponentsAsUser(UUID userId, UUID remoteUserId, long offset, long count, boolean includeOldVersions) throws BackendStoreException, UserNotFoundException;

    public JSONArray listPublicComponents(long offset, long count, boolean includeOldVersions) throws BackendStoreException, GroupNotFoundException;

    /**
     * Retrieves the list of components that have been shared with a specific group
     *
     * @param groupId The group id
     * @param offset Offset into the list to start from
     * @param count The number of groups to retrieve
     * @param includeOldVersions True to include old versions of components, False to only return latest versions
     * @return A JSON array of components that have been shared with the specified group
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public JSONArray listGroupComponents(UUID groupId, long offset, long count, boolean includeOldVersions) throws BackendStoreException, GroupNotFoundException;

    /**
     * Adds (or updates) a flow
     *
     * @param userId The user to be credited with the upload
     * @param flow   The flow
     * @return A JSON object keyed on uuid and version containing information about the flow
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public JSONObject addFlow(UUID userId, FlowDescription flow) throws BackendStoreException, UserNotFoundException;

    /**
     * Retrieves a flow given the flow id and version
     *
     * @param flowId  The flow id
     * @param version The version to retrieve
     * @return The flow descriptor, or null if the flow does not exist
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public Model getFlow(UUID flowId, int version) throws BackendStoreException, FlowNotFoundException;

    public UUID getFlowOwner(UUID flowId, int version) throws BackendStoreException, FlowNotFoundException;

    /**
     * Returns the version count for a flow
     *
     * @param flowId The flow id
     * @return The version count, or null if the flow does not exist
     * @throws BackendStoreException Thrown if an error occurred while communicating with the backend
     */
    public Integer getFlowVersionCount(UUID flowId) throws BackendStoreException, FlowNotFoundException;

    public void shareFlow(UUID componentId, int version, UUID groupId, UUID remoteUserId) throws BackendStoreException, FlowNotFoundException, GroupNotFoundException, UserNotFoundException;

    public JSONArray listUserFlows(UUID userId, long offset, long count, boolean includeOldVersions) throws BackendStoreException, UserNotFoundException;

    public JSONArray listPublicUserFlows(UUID userId, long offset, long count, boolean includeOldVersions) throws BackendStoreException, UserNotFoundException;

    public JSONArray listAccessibleUserFlowsAsUser(UUID userId, UUID remoteUserId, long offset, long count, boolean includeOldVersions) throws BackendStoreException, UserNotFoundException;

    public JSONArray listPublicFlows(long offset, long count, boolean includeOldVersions) throws BackendStoreException;

    public JSONArray listGroupFlows(UUID groupId, long offset, long count, boolean includeOldVersions) throws BackendStoreException, GroupNotFoundException;

}

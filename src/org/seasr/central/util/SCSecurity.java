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

import org.seasr.central.storage.BackendStoreLink;
import org.seasr.central.storage.SCRole;
import org.seasr.central.storage.exceptions.*;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * @author Boris Capitanu
 */
public abstract class SCSecurity {

    public static boolean canAddUsers(UUID remoteUserId, BackendStoreLink bsl, HttpServletRequest request)
        throws UserNotFoundException, BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        return false;
    }

    public static boolean canListUsers(UUID remoteUserId, BackendStoreLink bsl, HttpServletRequest request)
        throws UserNotFoundException, BackendStoreException {

        // Allow everyone
        return true;
    }

    public static boolean canListGroups(UUID remoteUserId, BackendStoreLink bsl, HttpServletRequest request)
        throws UserNotFoundException, BackendStoreException {

        // Allow any authenticated user
        if (remoteUserId != null)
            return true;

        return false;
    }

    public static boolean canAccessGroupComponents(UUID groupId, UUID remoteUserId,
                                                   BackendStoreLink bsl, HttpServletRequest request)
        throws GroupNotFoundException, UserNotFoundException, BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        if (bsl.isGroupMember(remoteUserId, groupId))
            return true;

        return false;
    }

    public static boolean canAccessGroupFlows(UUID groupId, UUID remoteUserId,
                                              BackendStoreLink bsl, HttpServletRequest request)
        throws GroupNotFoundException, UserNotFoundException, BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        if (bsl.isGroupMember(remoteUserId, groupId))
            return true;

        return false;
    }

    public static boolean canAccessComponent(UUID componentId, int version, UUID remoteUserId,
                                             BackendStoreLink bsl, HttpServletRequest request)
        throws ComponentNotFoundException, UserNotFoundException, BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        UUID ownerId = bsl.getComponentOwner(componentId, version);

        // Allowed if the remote user is the owner of the component
        if (ownerId.equals(remoteUserId))
            return true;

        // Get the number of groups common between the groups the remote user belongs to and the groups
        // with which this component version is shared
        int nCommonGroups = bsl.listComponentGroupsAsUser(componentId, version, remoteUserId, 0, Long.MAX_VALUE).length();

        // Allowed if the remote user belong to a group with which this component version is shared
        if (nCommonGroups > 0)
            return true;

        return false;
    }

    public static boolean canAccessFlow(UUID flowId, int version, UUID remoteUserId,
                                        BackendStoreLink bsl, HttpServletRequest request)
        throws FlowNotFoundException, UserNotFoundException, BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        UUID ownerId = bsl.getFlowOwner(flowId, version);

        // Allowed if the remote user is the owner of the flow
        if (ownerId.equals(remoteUserId))
            return true;

        // Get the number of groups common between the groups the remote user belongs to and the groups
        // with which this flow version is shared
        int nCommonGroups = bsl.listFlowGroupsAsUser(flowId, version, remoteUserId, 0, Long.MAX_VALUE).length();

        // Allowed if the remote user belong to a group with which this component version is shared
        if (nCommonGroups > 0)
            return true;

        return false;
    }

    public static boolean canShareComponent(UUID componentId, int version, UUID remoteUserId,
                                            BackendStoreLink bsl, HttpServletRequest request)
        throws ComponentNotFoundException, UserNotFoundException, BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        UUID ownerId = bsl.getComponentOwner(componentId, version);

        // Allowed if the remote user is the owner of the component
        if (ownerId.equals(remoteUserId))
            return true;

        return false;
    }

    public static boolean canShareFlow(UUID flowId, int version, UUID remoteUserId,
                                       BackendStoreLink bsl, HttpServletRequest request)
        throws FlowNotFoundException, UserNotFoundException, BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        UUID ownerId = bsl.getFlowOwner(flowId, version);

        // Allowed if the remote user is the owner of the flow
        if (ownerId.equals(remoteUserId))
            return true;

        return false;
    }

    public static boolean canUploadComponent(UUID userId, UUID remoteUserId, BackendStoreLink bsl, HttpServletRequest request)
        throws BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        // Allowed if the remote user is the same as the user who will be credited with the upload
        if (userId.equals(remoteUserId))
            return true;

        return false;
    }

    public static boolean canUploadFlow(UUID userId, UUID remoteUserId, BackendStoreLink bsl, HttpServletRequest request)
        throws BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        // Allowed if the remote user is the same as the user who will be credited with the upload
        if (userId.equals(remoteUserId))
            return true;

        return false;
    }

    public static boolean canAddGroupMember(UUID groupId, UUID remoteUserId, BackendStoreLink bsl, HttpServletRequest request)
        throws GroupNotFoundException, UserNotFoundException, BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        // Allowed if the remote user is a member of the group having ADMIN role in that group
        if (bsl.isUserInGroupRole(remoteUserId, groupId, SCRole.ADMIN))
            return true;

        return false;
    }

    public static boolean canAddPendingGroupMember(UUID groupId, UUID userId, UUID remoteUserId,
                                                   BackendStoreLink bsl, HttpServletRequest request)
        throws GroupNotFoundException, UserNotFoundException, BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        // Allowed if the remote user is the same as the user being added to the pending group members list
        if (userId.equals(remoteUserId))
            return true;

        // Allowed if the remote user is a member of the group having ADMIN role in that group
        if (bsl.isUserInGroupRole(remoteUserId, groupId, SCRole.ADMIN))
            return true;

        return false;
    }

    public static boolean canCreateGroup(UUID userId, UUID remoteUserId, BackendStoreLink bsl, HttpServletRequest request)
        throws BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        // Allowed if the remote user is the same as the user who will be the new owner of the group
        if (userId.equals(remoteUserId))
            return true;

        return false;
    }

    public static boolean canAccessPrivateGroupInfo(UUID groupId, UUID remoteUserId,
                                             BackendStoreLink bsl, HttpServletRequest request)
        throws GroupNotFoundException, UserNotFoundException, BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        if (bsl.isGroupMember(remoteUserId, groupId))
            return true;

        return false;
    }

    public static boolean canListGroupMembers(UUID groupId, UUID remoteUserId,
                                             BackendStoreLink bsl, HttpServletRequest request)
        throws GroupNotFoundException, UserNotFoundException, BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        // Allowed if the remote user is a member of the group having ADMIN role in that group
        if (bsl.isUserInGroupRole(remoteUserId, groupId, SCRole.ADMIN))
            return true;

        return false;
    }

    public static boolean canListUserGroupMembership(UUID userId, UUID remoteUserId,
                                                      BackendStoreLink bsl, HttpServletRequest request)
        throws BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        // Allowed if the remote user wants to list his/her own joined groups
        if (userId.equals(remoteUserId))
            return true;

        return false;
    }

    public static boolean canDeleteUser(UUID userId, UUID remoteUserId,
                                        BackendStoreLink bsl, HttpServletRequest request)
        throws BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        return false;
    }

    public static boolean canAccessPrivateUserInfo(UUID userId, UUID remoteUserId,
                                            BackendStoreLink bsl, HttpServletRequest request)
        throws UserNotFoundException, BackendStoreException {

        // Allowed if the remote user has the ADMIN role
        if (request.isUserInRole(SCRole.ADMIN.name()))
            return true;

        // Allowed if the remote user wants to access his/her own info
        if (userId.equals(remoteUserId))
            return true;

        return false;
    }
}

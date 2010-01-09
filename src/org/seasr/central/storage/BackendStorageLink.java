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

package org.seasr.central.storage;

import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;
import org.meandre.core.repository.ExecutableComponentDescription;
import org.meandre.core.repository.FlowDescription;

import com.hp.hpl.jena.rdf.model.Model;


/**
 * The interface that any back end storage driver must implement
 *
 * @author Xavier Llora
 * @author Boris Capitanu
 */

public interface BackendStorageLink {

	/**
	 * Initialize the backend storage link with the given properties.
	 *
	 * @param properties The properties required to initialize the backend link
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public void init(Properties properties) throws BackendStorageException;

	//-------------------------------------------------------------------------------------

	/**
	 * Adds a user to the back end storage facility
	 *
	 * @param userName The user screen name
	 * @param password The user password
	 * @param profile The user profile data
	 * @return The id assigned to the created user
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public UUID addUser(String userName, String password, JSONObject profile) throws BackendStorageException;

	/**
	 * Removes a user from the back end storage facility
	 *
	 * @param userId The user id
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public void removeUser(UUID userId) throws BackendStorageException;

	/**
	 * Removes a user from the back end storage facility
	 *
	 * @param userName The user's screen name
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public void removeUser(String userName) throws BackendStorageException;

	/**
	 * Updates a user's password
	 *
	 * @param userId The user id
	 * @param password The new password
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public void updateUserPassword(UUID userId, String password) throws BackendStorageException;

	/**
	 * Updates a user's password
	 *
	 * @param userName The user's screen name
	 * @param password The new password
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public void updateUserPassword(String userName, String password) throws BackendStorageException;

	/**
	 * Updates a user's profile
	 *
	 * @param userId The user id
	 * @param profile The new profile
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public void updateProfile(UUID userId, JSONObject profile) throws BackendStorageException;

	/**
	 * Updates a user's profile
	 *
	 * @param userName The user screen name
	 * @param profile The new profile
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public void updateProfile(String userName, JSONObject profile) throws BackendStorageException;

	/**
	 * Returns the id of a user given the screen name
	 *
	 * @param userName The user's screen name
	 * @return The user id or null if the user does not exist
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public UUID getUserId(String userName) throws BackendStorageException;

	/**
	 * Returns the screen name of a user given the user id
	 *
	 * @param userId The user id
	 * @return The screen name of the user or null if the user does not exist
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public String getUserScreenName(UUID userId) throws BackendStorageException;

	/**
	 * Returns the profile of a user given the screen name
	 *
	 * @param userName The user's screen name
	 * @return The profile of the user or null if the user does not exist
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public JSONObject getUserProfile(String userName) throws BackendStorageException;

	/**
	 * Returns the profile of a user given the user id
	 *
	 * @param userId The user id
	 * @return The profile of the user or null if the user does not exist
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public JSONObject getUserProfile(UUID userId) throws BackendStorageException;

	/**
	 * Returns the creation time of a user given the screen name
	 *
	 * @param userName The user's screen name
	 * @return The creation time of the user or null if the user does not exist
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public Date getUserCreationTime(String userName) throws BackendStorageException;

	/**
	 * Returns the creation time of a user given the user id
	 *
	 * @param userId The user id
	 * @return The creation time of the user or null if the user does not exist
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public Date getUserCreationTime(UUID userId) throws BackendStorageException;

	/**
	 * Checks if a user's password is valid based on the user's screen name
	 *
	 * @param userName The user's screen name
	 * @param password The password
	 * @return True if the password matches, False if not, or null if the user does not exist
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public Boolean isUserPasswordValid(String userName, String password) throws BackendStorageException;

	/**
	 * Checks if a user's password is valid based on the user id
	 *
	 * @param userId The user id
	 * @param password The password
	 * @return True if the password matches, False if not, or null if the user does not exist
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public Boolean isUserPasswordValid(UUID userId, String password) throws BackendStorageException;

	/**
	 * Returns the number of users
	 *
	 * @return The number of users
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public long userCount() throws BackendStorageException;

	/**
	 * Lists the users stored in the backend store
	 *
	 * @param offset The offset where to start
	 * @param count The number of users to be returned
	 * @return The list of users
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public JSONArray listUsers(long offset, long count) throws BackendStorageException;

	/**
	 * Adds a new event
	 *
	 * @param sourceType The source of the event
	 * @param sourceId The id of the source
	 * @param event The event
	 * @param description The event description
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public void addEvent(SourceType sourceType, UUID sourceId, Event event, JSONObject description) throws BackendStorageException;

    /**
     * Adds (or updates) a component
     *
     * @param userId The user to be credited with the upload
     * @param component The component
     * @param contexts The component context files
     * @param copyContextFiles Flag to indicate whether any file:/// context references should be copied (true) or moved (false) into the repository
     * @return A JSON object keyed on uuid and version containing information about the component
     * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
     */
    public JSONObject addComponent(UUID userId, ExecutableComponentDescription component, Set<URL> contexts, boolean copyContextFiles) throws BackendStorageException;

	/**
	 * Retrieves a component given the component id and version
	 *
	 * @param componentId The component id
	 * @param version The version to retrieve
	 * @return The component descriptor, or null if the component does not exist
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public Model getComponent(UUID componentId, int version) throws BackendStorageException;

	/**
	 * Retrieves an input stream to the context file specified by an id
	 *
	 * @param contextId The id of the context file
	 * @return An input stream to the context file specified
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public InputStream getContextInputStream(String contextId) throws BackendStorageException;

	/**
	 * Adds (or updates) a flow
	 *
	 * @param userId The user to be credited with the upload
	 * @param flow The flow
	 * @return A JSON object keyed on uuid and version containing information about the flow
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public JSONObject addFlow(UUID userId, FlowDescription flow) throws BackendStorageException;

	/**
	 * Retrieves a flow given the flow id and version
	 *
	 * @param flowId The flow id
	 * @param version The version to retrieve
	 * @return The flow descriptor, or null if the flow does not exist
	 * @throws BackendStorageException Thrown if an error occurred while communicating with the backend
	 */
	public Model getFlow(UUID flowId, int version) throws BackendStorageException;
}

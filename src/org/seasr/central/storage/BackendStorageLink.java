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

import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.fileupload.FileItem;
import org.json.JSONArray;
import org.json.JSONObject;
import org.meandre.core.repository.ExecutableComponentDescription;


/**
 * The interface that any back end storage driver must implement
 *
 * @author xavier
 */
public interface BackendStorageLink {

	/**
	 * Initialize the backend storage link with the given properties.
	 *
	 * @param properties The properties required to initialize the backend link
	 * @return true if it could be properly initialized, false otherwise
	 */
	public boolean init( Properties properties );

	/**
	 * Closes the back end storage link.
	 *
	 * @return true if it could be properly close, false otherwise
	 */
	public boolean close();

	//-------------------------------------------------------------------------------------

	/**
	 * Adds a user to the back end storage facility.
	 *
	 * @param user The user name
	 * @param password The password for this users
	 * @param profile The profile information for this user
	 * @return The UUID of the created users. If the user could not be created null is returned
	 */
	public UUID addUser ( String user, String password, JSONObject profile );

	/**
	 * Remove a user from the back end storage facility.
	 *
	 * @param uuid The UUID of the user to remove
	 * @return True if the user could be successfully removed. False otherwise
	 */
	public boolean removeUser ( UUID uuid );

	/**
	 * Remove a user from the back end storage facility.
	 *
	 * @param user The screen name of the user to remove
	 * @return True if the user could be successfully removed. False otherwise
	 */
	public boolean removeUser ( String user );

	/**
	 * Updates the user's password.
	 *
	 * @param uuid The user UUID
	 * @param password The new password to use
	 * @return True if the password could be successfully updated. False otherwise
	 */
	public boolean updateUserPassword ( UUID uuid, String password ) ;

	/**
	 * Updates the user's password.
	 *
	 * @param user The user screen_name
	 * @param password The new password to use
	 * @return True if the password could be successfully updated. False otherwise
	 */
	public boolean updateUserPassword ( String user, String password );

	/**
	 * Updates the user's profile.
	 *
	 * @param uuid The user UUID
	 * @param profile The new profile to use
	 * @return True if the profile could be successfully updated. False otherwise
	 */
	public boolean updateProfile ( UUID uuid, JSONObject profile ) ;

	/**
	 * Updates the user's profile.
	 *
	 * @param user The user screen_name
	 * @param profile The new profile to use
	 * @return True if the profile could be successfully updated. False otherwise
	 */
	public boolean updateProfile ( String user, JSONObject profile ) ;

	/**
	 * Returns the UUID of a user given his screen name.
	 *
	 * @param user The user's screen name
	 * @return The UUID of the user or null if the user does not exist
	 */
	public UUID getUserUUID ( String user );

	/**
	 * Returns the screen name of a user given his screen name.
	 *
	 * @param uuid The user's UUID
	 * @return The screen name of the user or null if the user does not exist
	 */
	public String getUserScreenName ( UUID uuid );

	/**
	 * Returns the profile of a user given his screen name.
	 *
	 * @param user The user's screen name
	 * @return The profile of the user or null if the user does not exist
	 */
	public JSONObject getUserProfile ( String user );


	/**
	 * Returns the profile of a user given his UUID.
	 *
	 * @param uuid The user's UUID
	 * @return The profile of the user or null if the user does not exist
	 */
	public JSONObject getUserProfile ( UUID uuid );

	/**
	 * Returns the creation time of a user given his screen name.
	 *
	 * @param user The user's screen name
	 * @return The creation time of the user or null if the user does not exist
	 */
	public Date getUserCreationTime ( String user );

	/**
	 * Returns the creation time of a user given his UUID.
	 *
	 * @param uuid The user's UUID
	 * @return The creation time of the user or null if the user does not exist
	 */
	public Date getUserCreationTime ( UUID uuid );

	/**
	 * Check if the user password is valid based on user's screen name.
	 *
	 * @param user The user's screen name
	 * @param password The password to check
	 * @return True if the password matches, false otherwise
	 */
	public boolean isUserPasswordValid ( String user, String password );

	/**
	 * Check if the user password is valid based on the UUID of the user.
	 *
	 * @param uuid The user's UUID
	 * @param password The password to check
	 * @return True if the password matches, false otherwise
	 */
	public boolean isUserPasswordValid ( UUID uuid, String password );

	/**
	 * Return the number of users on the back end storage.
	 *
	 * @return The number of users in SC back end storage. -1 indicates failure
	 */
	public long userCount();

	/**
	 * List the users contained on the database. Must provide number of users desired
	 * and offset into the listing.
	 *
	 * @param offset The offset where to start computing
	 * @param count The number of users to be returned
	 * @return The list of retrieved users
	 */
	public JSONArray listUsers ( long offset, long count );

	/**
	 * Add a new event
	 *
	 * @param sourceType The source of the event
	 * @param uuid The UUID of the source
	 * @param event The event
	 * @param description The JSON event description
	 * @return True if success, false otherwise
	 */
	public boolean addEvent(SourceType sourceType, UUID uuid, Event event, JSONObject description);


	public JSONObject addComponent(UUID userId, ExecutableComponentDescription comp, Set<FileItem> contexts);
	//-------------------------------------------------------------------------------------

}

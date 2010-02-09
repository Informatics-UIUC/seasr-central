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

/**
 * Defines the events and associated codes
 *
 * @author Boris Capitanu
 */
public enum Event {

    USER_CREATED            (100),
    USER_DELETED            (101),
    USER_RENAMED            (102),
    USER_PROFILE_UPDATED    (103),
    USER_JOINED_GROUP       (104),
    USER_PARTED_GROUP       (105),

    GROUP_CREATED           (200),
    GROUP_DELETED           (201),
    GROUP_RENAMED           (202),
    GROUP_JOINED            (203),
    GROUP_PARTED            (204),

    COMPONENT_UPLOADED      (300),
    COMPONENT_DELETED       (301),
    COMPONENT_SHARED        (302),
    COMPONENT_UNSHARED      (303),

    FLOW_UPLOADED           (400),
    FLOW_DELETED            (401),
    FLOW_SHARED             (402),
    FLOW_UNSHARED           (403);


    //--------------------------------------------------------------------------------------------

    private final int _code;

    Event(int code) {
        _code = code;
    }

    /**
     * Returns the event code for this event
     *
     * @return The event code for this event
     */
    public int getEventCode() {
        return _code;
    }
}

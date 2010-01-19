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

import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * SEASR Central formatter for log messages
 *
 * @author Boris Capitanu
 */
public class SCLogFormatter extends Formatter {

    @Override
    public String format(LogRecord logRecord) {
        String msg = logRecord.getMessage();
        if (msg == null || msg.length() == 0)
            msg = null;

        StringBuffer sb = (msg != null) ? new StringBuffer(msg) : new StringBuffer();

        Throwable thrown = logRecord.getThrown();
        if (thrown != null) {
            String exClassName = thrown.getClass().getName();
            if (msg == null)
                sb.append(String.format("%s: %s", exClassName, thrown.getMessage()));
            else
                sb.append(String.format(" (%s: %s)", exClassName, thrown.getMessage()));
        }

        String srcClassName = logRecord.getSourceClassName();
        String srcMethodName = logRecord.getSourceMethodName();

        srcClassName = srcClassName.substring(srcClassName.lastIndexOf(".") + 1);

        return String.format("%5$tY-%5$tm-%5$td %5$tH:%5$tM:%5$tS.%5$tL [%s]: %s\t[%s.%s]%n",
                logRecord.getLevel(), sb, srcClassName, srcMethodName, new Date(logRecord.getMillis()));
    }

}

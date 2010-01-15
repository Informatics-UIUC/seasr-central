/**
 * University of Illinois/NCSA
 * Open Source License
 *
 * Copyright (c) 2008, Board of Trustees-University of Illinois.
 * All rights reserved.
 *
 * Developed by:
 *
 * Automated Learning Group
 * National Center for Supercomputing Applications
 * http://www.seasr.org
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal with the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimers.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimers in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the names of Automated Learning Group, The National Center for
 *    Supercomputing Applications, or University of Illinois, nor the names of
 *    its contributors may be used to endorse or promote products derived from
 *    this Software without specific prior written permission.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * WITH THE SOFTWARE.
 */

package org.seasr.central.util;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Version {
    // these macros will be replaced by ANT
    private static final String VERSION = "@VERSION@";
    private static final String REVISION = "@REVISION@";
    
    private static Date _buildDate = null;

    public static String getVersion() {
        if ((VERSION.startsWith("@") && VERSION.endsWith("@")) || VERSION.trim().length() == 0)
            return null;
        else
            return VERSION;
    }

    public static String getRevision() {
        if ((REVISION.startsWith("@") && REVISION.endsWith("@")) || REVISION.trim().length() == 0)
            return null;
        else
            return REVISION;
    }

    public static Date getBuildDate() {
       if (_buildDate != null) return _buildDate;
       
       String classContainer = Version.class.getProtectionDomain().getCodeSource().getLocation().toString();
       String baseDir = null;
       
       try {
           Pattern regex = Pattern.compile("^(.*)WEB-INF[/\\\\]classes.*$");
           Matcher regexMatcher = regex.matcher(classContainer);
           if (regexMatcher.find()) {
               baseDir = regexMatcher.group(1);
           } else
               return null;
       } catch (PatternSyntaxException ex) {
           return null;
       }

       try {
            Manifest manifest = new Manifest(new URL(baseDir + "META-INF" + File.separator + "MANIFEST.MF").openStream());
            String buildDate = manifest.getMainAttributes().getValue("Build-Date");
            _buildDate = new SimpleDateFormat("MMM d, yyyy h:mm:ssa Z").parse(buildDate);
            
            return _buildDate;
        }
        catch (Exception e) {
            System.err.println("version: thrown exception: " + e);
            return null;
        }
    }

    public static String getFullVersion() {
        String version = getVersion();
        String revision = getRevision();

        if (version == null) return null;

        return version + ((revision != null) ? "." + revision : "");
    }
}

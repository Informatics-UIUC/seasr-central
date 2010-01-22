<!--
  ~ University of Illinois/NCSA
  ~ Open Source License
  ~
  ~ Copyright (c) 2008, NCSA.  All rights reserved.
  ~
  ~ Developed by:
  ~ The Automated Learning Group
  ~ University of Illinois at Urbana-Champaign
  ~ http://www.seasr.org
  ~
  ~ Permission is hereby granted, free of charge, to any person obtaining
  ~ a copy of this software and associated documentation files (the
  ~ "Software"), to deal with the Software without restriction, including
  ~ without limitation the rights to use, copy, modify, merge, publish,
  ~ distribute, sublicense, and/or sell copies of the Software, and to
  ~ permit persons to whom the Software is furnished to do so, subject
  ~ to the following conditions:
  ~
  ~ Redistributions of source code must retain the above copyright
  ~ notice, this list of conditions and the following disclaimers.
  ~
  ~ Redistributions in binary form must reproduce the above copyright
  ~ notice, this list of conditions and the following disclaimers in
  ~ the documentation and/or other materials provided with the distribution.
  ~
  ~ Neither the names of The Automated Learning Group, University of
  ~ Illinois at Urbana-Champaign, nor the names of its contributors may
  ~ be used to endorse or promote products derived from this Software
  ~ without specific prior written permission.
  ~
  ~ THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
  ~ EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
  ~ MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
  ~ IN NO EVENT SHALL THE CONTRIBUTORS OR COPYRIGHT HOLDERS BE LIABLE
  ~ FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
  ~ CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
  ~ WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS WITH THE SOFTWARE.
  -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:param name="date"></xsl:param>
    <xsl:param name="context"></xsl:param>
    <xsl:param name="host"></xsl:param>
    <xsl:param name="port"></xsl:param>
    <xsl:param name="user"></xsl:param>

    <xsl:template match="/meandre_response">
        <html xmlns="http://www.w3.org/1999/xhtml" lang="en">
            <head>
                <title>Meandre Server</title>
                <meta http-equiv="Content-Type" content="text/html; charset=utf8"/>
                <meta http-equiv="Content-Language" content="EN"/>
                <meta name="ROBOTS" content="NOINDEX,NOFOLLOW"/>
                <meta name="Description" content="SEASR Central Server"/>
            </head>
        </html>
        <body>
            <pre>
                Not implemented yet
            </pre>
        </body>
    </xsl:template>

</xsl:stylesheet>

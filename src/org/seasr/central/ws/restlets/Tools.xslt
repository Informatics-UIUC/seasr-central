<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
       version="1.0"
       xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
       xmlns:fn="http://www.w3.org/2005/xpath-functions">

       <xsl:param name="date"></xsl:param>
       <xsl:param name="context"></xsl:param>
       <xsl:param name="host"></xsl:param>
       <xsl:param name="port"></xsl:param>
       <xsl:param name="user"></xsl:param>
       
       <xsl:template match="/meandre_response">
             <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
                  <head>
                      <title>Meandre Server</title>
                      <meta http-equiv="content-type" content="text/html; charset=utf8" />
                      <meta http-equiv="content-language" content="EN" />
                      <meta name="ROBOTS" content="NOINDEX,NOFOLLOW"/>
                      <meta name="description" content="Meandre Server"/>
                  </head>
             </html>
             <body>
             	<pre>
             		Not implemented yet
             	</pre>
             </body>
       </xsl:template>
 </xsl:stylesheet>
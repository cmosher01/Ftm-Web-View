<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<xsl:stylesheet
    version="3.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fn="http://www.w3.org/2005/xpath-functions"
    xmlns:xhtml="http://www.w3.org/1999/xhtml"
>
    <xsl:output method="xml" version="1.1" encoding="UTF-8"/>
    <xsl:mode on-no-match="shallow-copy"/>

    <xsl:template match="xhtml:html|xhtml:head|xhtml:meta|xhtml:title|xhtml:body">
        <xsl:element name="div" namespace="http://www.w3.org/1999/xhtml">
            <xsl:attribute name="class">
                <xsl:value-of select="fn:concat('tika-', fn:local-name())"/>
            </xsl:attribute>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:element>
    </xsl:template>
</xsl:stylesheet>

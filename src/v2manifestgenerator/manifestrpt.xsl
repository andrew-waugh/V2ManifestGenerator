<?xml version="1.0"?> 
<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:dam="http://www.prov.vic.gov.au/digitalarchive/"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	version="1.0">
  <xsl:output method="html" indent="yes"/>

<xsl:template match="/">
	<HTML>
	<HEAD>
	<TITLE>PROV Digital Submission Manifest Report</TITLE>
	<META NAME="DC.Creator" CONTENT="PROV"></META>
	<META NAME="DC.Title" CONTENT="PROV Digital Submission Manifest Report"></META>
	<STYLE TYPE="text/css" MEDIA="SCREEN">
	H1 { background: blue; color: white; font-family: Arial, sans-serif; font-weight: normal }
	H2 { background: blue; color: white; font-family: Arial, sans-serif; font-weight: normal }
	P { font-family: Arial, sans-serif; font-weight: normal }
	TR { font-family: Arial, sans-serif; font-weight: normal }
	TD { font-family: Arial, sans-serif; vertical-align: top }
	.emphasise { font-family: Arial, sans-serif; font-weight: bold }
	</STYLE>
	</HEAD>
	<BODY>
	<H1>Manifest Report for
	<xsl:value-of select="//dam:series_type"/> 
	<xsl:text> </xsl:text>
	<xsl:value-of select="//dam:series_number"/>/
	<xsl:value-of select="//dam:consignment_type"/>
	<xsl:value-of select="//dam:consignment_number"/>
	</H1>
	<TABLE>
	<TR>
	<TD>Agency Id:</TD>
	<TD><xsl:value-of select="//dam:agency_id"/></TD>
	</TR>
	<TR>
	<TD>Job Id:</TD>
	<TD><xsl:value-of select="//dam:job_id"/></TD>
	</TR>
	<TR>
	<TD>Series:</TD>
	<TD><xsl:value-of select="//dam:series_type"/>
	<xsl:text> </xsl:text>
	<xsl:value-of select="//dam:series_number"/>/
	<xsl:value-of select="//dam:consignment_type"/>
	<xsl:value-of select="//dam:consignment_number"/>
	</TD>
	</TR>
	<TR>
	<TD>Submitted:</TD>
	<TD>
	<xsl:choose>
	<xsl:when test="/dam:set_manifest/dam:media_transfer">
	by Media
	</xsl:when>
	<xsl:otherwise>
	Electronically
	</xsl:otherwise>
	</xsl:choose>
	</TD>
	</TR>
	<TR>
	<TD>Manifest Created:</TD>
	<TD>
	<xsl:value-of select="substring(//dam:created_timestamp, 9, 2)"/>/
	<xsl:value-of select="substring(//dam:created_timestamp, 6, 2)"/>/
	<xsl:value-of select="substring(//dam:created_timestamp, 1, 4)"/>
	at
	<xsl:value-of select="substring(//dam:created_timestamp, 12, 8)"/>
	</TD>
	</TR>
	</TABLE>
	<xsl:apply-templates/>
	</BODY>
	</HTML>
</xsl:template>

<xsl:template match="dam:media_transfer|dam:electronic_transfer">
	<xsl:apply-templates select="dam:manifest_object_list">
		<xsl:with-param name="filesonly" select="'true'"/>
	</xsl:apply-templates>
	<xsl:apply-templates select="dam:manifest_object_list">
		<xsl:with-param name="filesonly" select="'false'"/>
	</xsl:apply-templates>
	<xsl:apply-templates select="dam:media_list"/>
</xsl:template>

<xsl:template match="dam:manifest_object_list">
	<xsl:param name="filesonly">false</xsl:param>
	<xsl:choose>
		<xsl:when test="$filesonly='true'">
			<H2>Manifest of Files in Set</H2>
		</xsl:when>
		<xsl:otherwise>
			<H2>Manifest of VEOs in Set</H2>
		</xsl:otherwise>
	</xsl:choose>
	<TABLE rules="cols">
	<THEAD>
	<TR>
	<TD>File Identifier (M102)</TD>
	<TD>Record Identifier (M103)</TD>
	<TD>Title Words (M35)</TD>
	<TD colspan="2">Date Range</TD>
	<TD>Classification</TD>
	<TD>Access Category</TD>
	<TD>Sentence (M90)</TD>
	<TD>VEO Name</TD>
	<TD>Size (kbytes)</TD>
	</TR>
	<TR>
	<TD></TD>
	<TD></TD>
	<TD></TD>
	<TD>Registered (M57)</TD>
	<TD>Closed (M144)</TD>
	<TD></TD>
	<TD></TD>
	<TD></TD>
	<TD></TD>
	<TD></TD>
	</TR>
	</THEAD>
	<TBODY>
	<xsl:apply-templates select="dam:manifest_object_item">
		<xsl:with-param name="filesonly" select="$filesonly"/>
		<xsl:sort select="dam:file_identifier"/>
		<xsl:sort select="dam:vers_record_identifier"/>
	</xsl:apply-templates>
	</TBODY>
	</TABLE>
</xsl:template>

<xsl:template match="dam:manifest_object_item">
	<xsl:param name="filesonly">false</xsl:param>
	<xsl:choose>
		<xsl:when test="$filesonly='true'">
			<xsl:if test="dam:vers_record_identifier/@xsi:nil='true'">
	<TR>
	<TD><xsl:value-of select="dam:file_identifier"/></TD>
	<TD></TD>
	<TD><xsl:value-of select="dam:veo_title"/></TD>
	<xsl:apply-templates select="dam:veo_date_range"/>
	<TD><xsl:value-of select="dam:veo_classification"/></TD>
	<TD><xsl:value-of select="dam:veo_access_category"/></TD>
	<TD><xsl:value-of select="dam:veo_disposal_authority"/></TD>
	<TD><xsl:value-of select="dam:computer_filename"/></TD>
	<TD><xsl:value-of select="dam:size_kb"/></TD>
	</TR>
			</xsl:if>
		</xsl:when>
		<xsl:otherwise>
			<xsl:choose>
				<xsl:when test="dam:vers_record_identifier/@xsi:nil='true'">
	<TR bgcolor="yellow">
	<TD><xsl:value-of select="dam:file_identifier"/></TD>
	<TD></TD>
	<TD><xsl:value-of select="dam:veo_title"/></TD>
	<xsl:apply-templates select="dam:veo_date_range"/>
	<TD><xsl:value-of select="dam:veo_classification"/></TD>
	<TD><xsl:value-of select="dam:veo_access_category"/></TD>
	<TD><xsl:value-of select="dam:veo_disposal_authority"/></TD>
	<TD><xsl:value-of select="dam:computer_filename"/></TD>
	<TD><xsl:value-of select="dam:size_kb"/></TD>
	</TR>
				</xsl:when>
				<xsl:otherwise>
	<TR>
	<TD><xsl:value-of select="dam:file_identifier"/></TD>
	<TD><xsl:value-of select="dam:vers_record_identifier"/></TD>
	<TD><xsl:value-of select="dam:veo_title"/></TD>
	<xsl:apply-templates select="dam:veo_date_range"/>
	<TD><xsl:value-of select="dam:veo_classification"/></TD>
	<TD><xsl:value-of select="dam:veo_access_category"/></TD>
	<TD><xsl:value-of select="dam:veo_disposal_authority"/></TD>
	<TD><xsl:value-of select="dam:computer_filename"/></TD>
	<TD><xsl:value-of select="dam:size_kb"/></TD>
	</TR>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>

<xsl:template match="dam:veo_date_range">
	<TD><xsl:value-of select="dam:veo_start_date"/></TD>
	<TD><xsl:value-of select="dam:veo_end_date"/></TD>
</xsl:template>

<xsl:template match="dam:media_list">
	<H2>Manifest of Media submitted with Set</H2>
	<P>
	Total Pieces of Media:
	<xsl:value-of select="dam:media_item/dam:media_item_total_number"/>
	</P>
	<TABLE rules="cols">
	<TR class="HEAD">
	<TD>Media</TD>
	<TD>Media</TD>
	<TD>Date Written</TD>
	</TR>
	<TR class="HEAD">
	<TD>Id</TD>
	<TD>Type</TD>
	<TD></TD>
	</TR>
	<xsl:apply-templates select="dam:media_item">
		<xsl:sort select="dam:media_item_number"/>
	</xsl:apply-templates>
	</TABLE>
</xsl:template>

<xsl:template match="dam:media_item">
	<TR>
	<TD><xsl:value-of select="dam:media_item"/></TD>
	<TD><xsl:value-of select="dam:media_type"/></TD>
	<TD><xsl:value-of select="dam:media_written"/></TD>
	</TR>
</xsl:template>
</xsl:stylesheet>

<%@include file="xml_header.jsp"%>
<%-- Displays transect information in XML format.  This is a template, into
     which data will be passed from WmsController.getTransect().

     It's a good idea to document here what data this template expects:
     name : Name of a person (String).  See the other JSP files for more details. --%>
<transect>
    <name>${name}</name><%-- This is how you display data from the Model --%>
</transect>
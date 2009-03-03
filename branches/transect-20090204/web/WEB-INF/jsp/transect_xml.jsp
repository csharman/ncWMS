<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%-- Displays transect information in XML format.  This is a template, into
     which data will be passed from WmsController.getTransect().

     It's a good idea to document here what data this template expects:
     name : Name of a person (String).  See the other JSP files for more details. --%>


<transect date="${date}">
    <c:forEach var="transectPoint" items="${name}">
   <point>
<latitude>${transectPoint.lat}</latitude>
<longitude>${transectPoint.lon}</longitude>
 <value>${transectPoint.value}</value>
</point>
   
    </c:forEach>
</transect>
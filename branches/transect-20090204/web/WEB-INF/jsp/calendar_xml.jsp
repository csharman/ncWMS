<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%-- Displays Calendar Information for a given layer
layer : Layer with ID (String).  eg 5/SST_AVE --%>


<datasetCalendar>
    <c:forEach var="date" items="${dates}">
        <date>${date}</date>
    </c:forEach>
</datasetCalendar>
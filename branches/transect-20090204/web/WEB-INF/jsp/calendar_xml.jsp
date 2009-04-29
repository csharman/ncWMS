<%@include file="xml_header.jsp"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%-- Displays Calendar Information for a given layer
dates: List<String> representing available date-times for the layer --%>


<datasetCalendar>
    <c:forEach var="date" items="${dates}">
        <date>${date}</date>
    </c:forEach>
</datasetCalendar>
<%@tag description="Displays a single Layer in the menu" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://www.atg.com/taglibs/json" prefix="json"%>
<%@attribute name="dataset" required="true" type="uk.ac.rdg.resc.ncwms.config.Dataset" description="Dataset containing this layer"%>
<%@attribute name="id" required="true" description="ID of layer within the dataset"%>
<%@attribute name="label" required="true" description="Optional: can be used to override the title for this layer"%>
<%@attribute name="server" description="Optional URL to the ncWMS server providing this layer"%>
<json:object>
    <c:if test="${empty dataset}">
        <json:property name="label" value="Dataset does not exist"/>
    </c:if>
    <c:if test="${dataset.ready}">
        <json:property name="id" value="${dataset.id}/${id}"/>
        <json:property name="label" value="${label}"/>
        <c:if test="${not empty server}">
            <json:property name="server" value="${server}"/>
        </c:if>
    </c:if>
    <c:if test="${dataset.loading}">
        <json:property name="label" value="${label} (loading)"/>
    </c:if>
    <c:if test="${dataset.error}">
        <json:property name="label" value="${label} (error)"/>
    </c:if>
</json:object>
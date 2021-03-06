<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>

    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>GlobModel Visualization and Validation Service</title>

        <!-- Resets the browser's CSS definitions. This should reduce the
             problem of fonts appearing differently in different browsers. -->
        <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.5.2/build/reset-fonts-grids/reset-fonts-grids.css">

        <link rel="stylesheet" type="text/css" href="css/vvs.css">

        <script type="text/javascript" src="js/OpenLayers-2.8.js"></script>
        <script type="text/javascript" src="js/LoadingPanel.js"></script>
        <script type="text/javascript" src="js/WMS1_1_1.js"></script>
        <script type="text/javascript" src="js/WMS1_3.js"></script>

        <!-- For the pop-up panel (used in the palette selector) -->
        <link rel="stylesheet" type="text/css" href="http://yui.yahooapis.com/2.5.2/build/container/assets/skins/sam/container.css">
        <script type="text/javascript" src="http://yui.yahooapis.com/2.5.2/build/yahoo-dom-event/yahoo-dom-event.js"></script>
        <script type="text/javascript" src="http://yui.yahooapis.com/2.5.2/build/dragdrop/dragdrop-min.js"></script>
        <script type="text/javascript" src="http://yui.yahooapis.com/2.5.2/build/container/container-min.js"></script>

        <script type="text/javascript" src="js/vvs.js"></script>
    </head>

    <body class="yui-skin-sam">

        <div id="mainPanel">

            <div id="panelHeader">
                <h1>GlobModel Visualization and Validation Service</h1>

                <div id="right">
                    <c:if test="${pageContext.request.userPrincipal != null}">Welcome, <h2>${pageContext.request.userPrincipal.name}</h2></c:if>
                    <a href="<c:url value="/j_spring_security_logout"/>">Logout</a>
                </div>

                <table border="1">
                    <thead>
                        <tr><th></th><th>Visibility</th><th>Time</th><th>Opacity</th></tr>
                    </thead>
                    <tbody>
                        <tr><th>Coastlines:</th><td align="center"><input id="coastVisibility" type="checkbox" checked="checked" onclick="javascript:updateVisibilities()"/></td><td>n/a</td><td>n/a</td></tr>
                        <tr>
                            <th>Sciamachy:</th>
                            <td align="center"><input id="sciaVisibility" type="checkbox" onclick="javascript:updateVisibilities()"/></td>
                            <td>
                                +/-
                                <select id="sciaWindow" onchange="javascript:updateDates()">
                                    <option value="30">Half an hour</option>
                                    <option value="60">One hour</option>
                                    <option value="120">Two hours</option>
                                </select>
                                of GlobModel data
                            </td>
                            <td>
                                <select id="sciaOpacity" onchange="javascript:updateOpacities()">
                                    <option value="1" selected="selected">100%</option>
                                    <option value="0.75">75%</option>
                                    <option value="0.5">50%</option>
                                    <option value="0.25">25%</option>
                                </select>
                            </td>
                        </tr>
                        <tr>
                            <th>GlobModel:</th>
                            <td align="center"><input id ="gmVisibility" type="checkbox" checked="checked" onclick="javascript:updateVisibilities()"/></td>
                            <td>
                                <span id="dayName"></span>&nbsp;August
                                <select id="globModelDay" onchange="javascript:updateDates()">
                                    <option value="21">21</option>
                                    <option value="22">22</option>
                                    <option value="23">23</option>
                                    <option value="24">24</option>
                                    <option value="25">25</option>
                                    <option value="26">26</option>
                                    <option value="27">27</option>
                                    <option value="28">28</option>
                                    <option value="29">29</option>
                                    <option value="30">30</option>
                                    <option value="31">31</option>
                                </select>
                                2006
                                <select id="globModelHour" onchange="javascript:updateDates()">
                                    <option value="0">00:00</option>
                                    <option value="6">06:00</option>
                                    <option value="12">12:00</option>
                                    <option value="18">18:00</option>
                                </select>
                            </td>
                            <td>
                                <select id="gmOpacity" onchange="javascript:updateOpacities()">
                                    <option value="1" selected="selected">100%</option>
                                    <option value="0.75">75%</option>
                                    <option value="0.5">50%</option>
                                    <option value="0.25">25%</option>
                                </select>
                            </td>
                        </tr>

                        <tr>
                            <th>OMI:</th>
                            <td align="center"><input id ="omiVisibility" type="checkbox" checked="checked" onclick="javascript:updateVisibilities()"/></td>
                            <td>
                                <span id="dayOmiName"></span>&nbsp;August
                                <select id="omiDay" onchange="javascript:updateDates()">
                                    <option value="21">21</option>
                                    <option value="22">22</option>
                                    <option value="23">23</option>
                                    <option value="24">24</option>
                                    <option value="25">25</option>
                                    <option value="26">26</option>
                                    <option value="27">27</option>
                                    <option value="28">28</option>
                                    <option value="29">29</option>
                                    <option value="30">30</option>
                                    <option value="31">31</option>
                                </select>
			          2006


                                <select id="omiHour" onchange="javascript:updateDates()">
                                    <option value="2">02:13</option>
                                    <option value="3">03:49</option>
                                    <option value="5">05:31</option>
                                    <option value="7">07:10</option>
                                    <option value="10">10:27</option>
                                    <option value="12">12:06</option>
                                    <option value="13">13:45</option>
                                    <option value="15">15:24</option>
                                    <option value="17">17:03</option>
                                    <option value="18">18:42</option>
                                    <option value="20">20:21</option>
                                    <option value="22">21:59</option>
                                </select>
                            </td>
                            <td>
                                <select id="omiOpacity" onchange="javascript:updateOpacities()">
                                    <option value="1" selected="selected">100%</option>
                                    <option value="0.75">75%</option>
                                    <option value="0.5">50%</option>
                                    <option value="0.25">25%</option>
                                </select>
                            </td>
                        </tr>

                        <tr><th>Topography:</th><td align="center"><input id="topoVisibility" type="checkbox" onclick="javascript:updateVisibilities()"/></td><td>n/a</td><td>n/a</td></tr>
                    </tbody>
                </table>
            </div>

            <div id="mapPanel">
                <div id="map"></div>
                <img id="scaleBar" class="scaleBar" width="40" height="398" alt="scale bar" title="Click to change the colour palette" onclick="javascript:showPaletteSelector();"/>
                <div class="scaleMarkers">
                    <input id="scaleMax" class="scaleMax" type="text" size="3" onblur="javascript:updateColorScale()" value="400"/>
                    <span id="scaleTwoThirds"></span>
                    <span id="units">Dobson Units</span>
                    <span id="scaleOneThird"></span>
                    <input id="scaleMin" class="scaleMin" type="text" size="3" onblur="javascript:updateColorScale()" value="180"/>
                </div>
            </div>
        </div>

        <!-- The palette selection pop-up window.  Initially invisible. -->
        <div id="paletteSelector" style="visibility: hidden">
            <div class="hd">Click to choose a colour palette</div>
            <div class="bd">
                <p>Number of colour bands:&nbsp;
                    <select id="numColorBands" onchange="javascript:updatePaletteSelector()">
                        <option value="10">10</option>
                        <option value="20">20</option>
                        <option value="50">50</option>
                        <option value="254" selected>254</option>
                    </select>
                </p>
                <!-- This is where the table of palette images will go -->
                <div id="paletteDiv"></div>
            </div>
        </div>

    </body>
</html>

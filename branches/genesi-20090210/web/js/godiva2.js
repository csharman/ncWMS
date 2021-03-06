//
// Javascript for GODIVA2 page.
//

var map = null;
var calendar = null; // The calendar object
var datesWithData = null; // Will be populated with the dates on which we have data
                          // for the currently-selected variable
var isoTValue = null; // The currently-selected t value (ISO8601)
var isIE;
var scaleMinVal;
var scaleMaxVal;
var gotScaleRange = false;
var scaleLocked = false; // see toggleLockScale()
var autoLoad = new Object(); // Will contain data for auto-loading data from a permalink
var menu = ''; // The menu that is being displayed (e.g. "mersea", "ecoop")
var bbox = null; // The bounding box of the currently-displayed layer
var featureInfoUrl = null; // The last-called URL for getFeatureInfo (following a click on the map)

var layerSwitcher = null;
var ncwms = null; // Points to the currently-active layer that is coming from this ncWMS
                  // Will point to either ncwms_tiled or ncwms_untiled.
var ncwms_tiled = null; // We shall maintain two separate layers, one tiles (for scalar
var ncwms_untiled = null; // quantities) and one untiled (for vector quantities)

var animation_layer = null; // The layer that will be used to display animations

var servers = ['']; // URLs to the servers from which we will display layers
                    // An empty string means the server that is serving this page.
var activeLayer = null; // The currently-selected layer metadata

var tree = null; // The tree control in the left-hand panel

var paletteSelector = null; // Pop-up panel for selecting a new palette
var paletteName = null; // Name of the currently-selected palette

var popups = []; // Pop-ups (GetFeatureInfo results) shown on the map

// Called when the page has loaded
window.onload = function()
{
    // reset the scale markers
    $('scaleMax').value = '';
    $('scaleMin').value = '';

    // Make sure 100% opacity is selected
    $('opacityValue').value = '100';

    // Detect the browser (IE6 doesn't render PNGs properly so we don't provide
    // the option to have partial overlay opacity)
    isIE = navigator.appVersion.indexOf('MSIE') >= 0;

    // Stop the pink tiles appearing on error
    OpenLayers.Util.onImageLoadError = function() {  this.style.display = ""; this.src="./images/blank.png"; }
    
    // Set up the OpenLayers map widget
    map = new OpenLayers.Map('map');
    var ol_wms = new OpenLayers.Layer.WMS1_1_1( "OpenLayers WMS", 
        "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'basic'});
    var bluemarble_wms = new OpenLayers.Layer.WMS1_1_1( "Blue Marble", 
        "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'satellite' });
    var osm_wms = new OpenLayers.Layer.WMS1_1_1( "Openstreetmap", 
        "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'osm-map' });
    var human_wms = new OpenLayers.Layer.WMS1_1_1( "Human Footprint", 
        "http://labs.metacarta.com/wms-c/Basic.py?", {layers: 'hfoot' });
    var demis_wms = new OpenLayers.Layer.WMS1_1_1( "Demis WMS",
        "http://www2.Demis.nl/MapServer/Request.asp?WRAPDATELINE=TRUE", {layers:
        'Bathymetry,Topography,Hillshading,Coastlines,Builtup+areas,Waterbodies,Rivers,Streams,Railroads,Highways,Roads,Trails,Borders,Cities,Airports'});
        
    // Now for the polar stereographic layers, one for each pole.  We do this
    // as an Untiled layer because, for some reason, if we use a tiled layer
    // this results in lots of spurious tiles being requested when switching
    // from a lat-lon base layer to polar stereographic.
    // The full extent of a polar stereographic projection is (-10700000, -10700000,
    // 14700000, 14700000) but we don't use all of this range because we're only
    // really interested in the stuff near the poles.  Therefore we set maxExtent
    // so the user only sees a quarter of this range and maxResolution so that
    // we can't zoom out too far.
    var polarMaxExtent = new OpenLayers.Bounds(-10700000, -10700000, 14700000, 14700000);
    var halfSideLength = (polarMaxExtent.top - polarMaxExtent.bottom) / (4 * 2);
    var centre = ((polarMaxExtent.top - polarMaxExtent.bottom) / 2) + polarMaxExtent.bottom;
    var low = centre - halfSideLength;
    var high = centre + halfSideLength;
    var polarMaxResolution = (high - low) / 256;
    var windowLow = centre - 2 * halfSideLength;
    var windowHigh = centre + 2 * halfSideLength;
    var polarWindow = new OpenLayers.Bounds(windowLow, windowLow, windowHigh, windowHigh);
    var northPoleBaseLayer = new OpenLayers.Layer.WMS.Untiled(
        "North polar stereographic",
        "http://nsidc.org/cgi-bin/atlas_north",
        {
            layers: 'country_borders,arctic_circle',
            format: 'image/png'
        },
        {
            wrapDateLine: false,
            transitionEffect: 'resize',
            /*/projection: 'EPSG:3408', // NSIDC EASE-Grid North
            maxExtent: new OpenLayers.Bounds(-9036842.762, -9036842.762,
                9036842.762, 9036842.762),
            maxResolution: 2 * 9036842.762 / 256*/
            projection: 'EPSG:32661',
            maxExtent: polarWindow,
            maxResolution: polarMaxResolution
        }
    );
    var southPoleBaseLayer = new OpenLayers.Layer.WMS.Untiled(
        "South polar stereographic",
        "http://nsidc.org/cgi-bin/atlas_south",
        {
            layers: 'country_borders,antarctic_circle',
            format: 'image/png'
        },
        {
            wrapDateLine: false,
            transitionEffect: 'resize',
            /*/projection: 'EPSG:3409', // NSIDC EASE-Grid South
            maxExtent: new OpenLayers.Bounds(-9036842.762, -9036842.762,
                9036842.762, 9036842.762),
            maxResolution: 2 * 9036842.762 / 256*/
            projection: 'EPSG:32761',
            maxExtent: polarWindow,
            maxResolution: polarMaxResolution
        }
    );

    // ESSI WMS (see Stefano Nativi's email to me, Feb 15th)
    /*var essi_wms = new OpenLayers.Layer.WMS.Untiled( "ESSI WMS", 
        "http://athena.pin.unifi.it:8080/ls/servlet/LayerService?",
        {layers: 'sst(time-lat-lon)-T0', transparent: 'true' } );
    essi_wms.setVisibility(false);*/
            
    // The SeaZone Web Map server
    /*var seazone_wms = new OpenLayers.Layer.WMS1_3("SeaZone bathymetry", "http://ws.cadcorp.com/seazone/wms.exe?",
        {layers: 'Bathymetry___Elevation.bds', transparent: 'true'});
    seazone_wms.setVisibility(false);*/
    
    map.addLayers([bluemarble_wms, demis_wms, ol_wms, osm_wms, human_wms, northPoleBaseLayer, southPoleBaseLayer/*, seazone_wms, essi_wms*/]);
    
    map.setBaseLayer(demis_wms);

    // Make sure the Google Earth and Permalink links are kept up to date when
    // the map is moved or zoomed
    map.events.register('moveend', map, setGEarthURL);
    map.events.register('moveend', map, setPermalinkURL);
    // Register an event for when the base layer of the map is changed
    map.events.register('changebaselayer', map, baseLayerChanged);
    
    // If we have loaded Google Maps and the browser is compatible, add it as a base layer
    if (typeof GBrowserIsCompatible == 'function' && GBrowserIsCompatible()) {
        var gmapLayer = new OpenLayers.Layer.Google("Google Maps (satellite)", {type: G_SATELLITE_MAP});
        var gmapLayer2 = new OpenLayers.Layer.Google("Google Maps (political)", {type: G_NORMAL_MAP});
        map.addLayers([gmapLayer, gmapLayer2]);
    }
    
    layerSwitcher = new OpenLayers.Control.LayerSwitcher()
    map.addControl(layerSwitcher);
    
    var loadingpanel = new OpenLayers.Control.LoadingPanel();
    map.addControl(loadingpanel);

    //map.addControl(new OpenLayers.Control.MousePosition({prefix: 'Lon: ', separator: ' Lat:'}));
    map.zoomTo(1);
    
    // Add a listener for changing the base map
    //map.events.register("changebaselayer", map, function() { alert(this.projection) });
    // Add a listener for GetFeatureInfo
    map.events.register('click', map, getFeatureInfo);
    
    // Set up the autoload object
    // Note that we must get the query string from the top-level frame
    // strip off the leading question mark
    populateAutoLoad(window.location);
    if (window.top.location != window.location) {
        // We're in an iframe so we must also use the query string from the top frame
        populateAutoLoad(window.top.location);
    }
    
    // Set up the left-hand menu
    setupTreeControl(menu);
    
    // Set up the palette selector pop-up
    paletteSelector = new YAHOO.widget.Panel("paletteSelector", { 
        width:"400px",
        constraintoviewport: true,
        fixedcenter: true,
        underlay:"shadow",
        close:true,
        visible:false,
        draggable:true,
        modal:true
    });
}

// Populates the autoLoad object from the given window location object
function populateAutoLoad(windowLocation)
{
    var queryString = windowLocation.search.split('?')[1];
    if (queryString != null) {
        var kvps = queryString.split('&');
        for (var i = 0; i < kvps.length; i++) {
            keyAndVal = kvps[i].split('=');
            if (keyAndVal.length > 1) {
                var key = keyAndVal[0].toLowerCase();
                if (key == 'layer') {
                    autoLoad.layer = keyAndVal[1];
                } else if (key == 'elevation') {
                    autoLoad.zValue = keyAndVal[1];
                } else if (key == 'time') {
                    autoLoad.isoTValue = keyAndVal[1];
                } else if (key == 'bbox') {
                    autoLoad.bbox = keyAndVal[1];
                } else if (key == 'scale') {
                    autoLoad.scaleMin = keyAndVal[1].split(',')[0];
                    autoLoad.scaleMax = keyAndVal[1].split(',')[1];
                } else if (key == 'menu') {
                    // we load a specific menu instead of the default
                    menu = keyAndVal[1];
                }
            }
        }
    }
}

function setupTreeControl(menu)
{
    tree = new YAHOO.widget.TreeView('layerSelector');
    // Add an event callback that gets fired when a tree node is clicked
    tree.subscribe('labelClick', treeNodeClicked);
    
    // The servers can be specified using the global "servers" array above
    // but if not, we'll just use the default server
    if (typeof servers == 'undefined' || servers == null) {
        servers = [''];
    }

    // Add a root node in the tree for each server.  If the user has supplied
    // a "menu" option then this will be sent to all the servers.
    for (var i = 0; i < servers.length; i++) {
        var layerRootNode = new YAHOO.widget.TextNode(
            {label: "Loading ...", server: servers[i]},
            tree.getRoot(),
            servers.length == 1 // Only show expanded if this is the only server
        );
        layerRootNode.multiExpand = false;
        getMenu(layerRootNode, {
            menu: menu,
            callback : function(layerRootNode, layers) {
                layerRootNode.data.label = layers.label;
                layerRootNode.label = layers.label;
                // Add layers recursively.
                addNodes(layerRootNode, layers.children);
                tree.draw();
                
                // Now look to see if we are auto-loading a certain layer
                if (typeof autoLoad.layer != 'undefined') {
                    var node = tree.getNodeByProperty('id', autoLoad.layer);
                    if (node == null) {
                        alert("Layer " + autoLoad.layer + " not found");
                    } else {
                        expandParents(node);
                        treeNodeClicked(node); // act as if we have clicked this node
                    }
                }
            }
        });
    }
}

function expandParents(node)
{
    if (node.parent != null) {
        node.parent.expand();
        expandParents(node.parent);
    }
}

// Called when a node in the tree has been clicked
function treeNodeClicked(node)
{
    // We're only interested if this is a displayable layer, i.e. it has an id.
    if (typeof node.data.id != 'undefined') {
        // Update the breadcrumb trail
        var s = node.data.label;
        var theNode = node;
        while(theNode.parent != tree.getRoot()) {
            theNode = theNode.parent;
            s = theNode.data.label + ' &gt; ' + s;
        }
        $('layerPath').innerHTML = s;

        // See if we're auto-loading a certain time value
        if (typeof autoLoad.isoTValue != 'undefined') {
            isoTValue = autoLoad.isoTValue;
        }
        if (isoTValue == null ) {
            // Set to the present time if we don't already have a time selected
            // Set milliseconds to zero (don't know how to create a format string
            // that includes milliseconds).
            isoTValue = new Date().print('%Y-%m-%dT%H:%M:%S.000Z');
        }

        // Get the details of this layer from the server, calling layerSelected()
        // when we have the result
        getLayerDetails(node.data.server, {
            callback: layerSelected,
            layerName: node.data.id,
            time: isoTValue
        });
    }
}

// Recursive method to add nodes to the layer selector tree control
function addNodes(parentNode, layerArray)
{
    for (var i = 0; i < layerArray.length; i++) {
        var layer = layerArray[i];
        if (layer.server == null) {
            // If the layer does not specify a server explicitly, use the URL of
            // the server that provided this layer
            layer.server = parentNode.data.server;
        }
        // The treeview control uses the layer.label string for display
        var newNode = new YAHOO.widget.TextNode(
            {label: layer.label, id: layer.id, server: layer.server},
            parentNode,
            false
        );
        if (typeof layer.children != 'undefined') {
            newNode.multiExpand = false;
            addNodes(newNode, layer.children);
        }
    }
}

// Function that is used by the calendar to see whether a date should be disabled
function isDateDisabled(date, year, month, day)
{
    // datesWithData is a hash of year numbers mapped to a hash of month numbers
    // to an array of day numbers, i.e. {2007 : {0 : [3,4,5]}}.
    // Month numbers are zero-based.
    if (datesWithData == null ||
        datesWithData[year] == null || 
        datesWithData[year][month] == null) {
        // No data for this year or month
        return true;
    }
    // Cycle through the array of days for this month, looking for the one we want
    var numDays = datesWithData[year][month].length;
    for (var d = 0; d < numDays; d++) {
        if (datesWithData[year][month][d] == day) return false; // We have data for this day
    }
    // If we've got this far, we've found no data
    return true;
}

// Event handler for when a user clicks on a map
function getFeatureInfo(e)
{
    var lonLat = map.getLonLatFromPixel(e.xy);
    // Check we haven't clicked off-map
    // Could also check the bbox of the layer but this would only work in lat-lon
    // projection...
    if (ncwms != null && ncwms.maxExtent.containsLonLat(lonLat))
    {
        // Immediately load popup saying "loading"
        var tempPopup = new OpenLayers.Popup (
            "temp", // TODO: does this need to be unique?
            lonLat,
            new OpenLayers.Size(100, 50),
            "Loading...",
            true, // Means "add a close box"
            null  // Do nothing when popup is closed.
        );
        tempPopup.autoSize = true;
        map.addPopup(tempPopup);
        
        var params = {
            REQUEST: "GetFeatureInfo",
            BBOX: map.getExtent().toBBOX(),
            I: e.xy.x,
            J: e.xy.y,
            INFO_FORMAT: 'text/xml',
            QUERY_LAYERS: ncwms.params.LAYERS,
            WIDTH: map.size.w,
            HEIGHT: map.size.h
        };
        if (activeLayer.server != '') {
            // This is the signal to the server to load the data from elsewhere
            params.url = activeLayer.server;
        }
        featureInfoUrl = ncwms.getFullRequestString(
            params,
            'wms' // We must always load from the home server
        );
        // Now make the call to GetFeatureInfo
        OpenLayers.loadURL(featureInfoUrl, '', this, function(response) {
            var xmldoc = response.responseXML;
            var lon = parseFloat(xmldoc.getElementsByTagName('longitude')[0].firstChild.nodeValue);
            var lat = parseFloat(xmldoc.getElementsByTagName('latitude')[0].firstChild.nodeValue);
            var val = parseFloat(xmldoc.getElementsByTagName('value')[0].firstChild.nodeValue);
            var html = "";
            if (lon) {
                // We have a successful result
                var truncVal = toNSigFigs(val, 4);
                html = "<b>Lon:</b> " + lon + "<br /><b>Lat:</b> " + lat +
                    "<br /><b>Value:</b> " + truncVal + "<br />"
                // Add links to alter colour scale min/max
                html += "<a href='#' onclick=setColourScaleMin(" + val + ") " +
                    "title='Sets the minimum of the colour scale to " + truncVal + "'>" +
                    "Set colour min</a><br />";
                html += "<a href='#' onclick=setColourScaleMax(" + val + ") " +
                    "title='Sets the maximum of the colour scale to " + truncVal + "'>" +
                    "Set colour max</a>";
                if (timeSeriesSelected()) {
                    // Construct a GetFeatureInfo request for the timeseries plot
                    // Get a URL for a WMS request that covers the current map extent
                    var urlEls = featureInfoUrl.split('&');
                    // Replace the parameters as needed.  We generate a map that is half the
                    // width and height of the viewport, otherwise it takes too long
                    var newURL = urlEls[0];
                    for (var i = 1; i < urlEls.length; i++) {
                        if (urlEls[i].startsWith('TIME=')) {
                            newURL += '&TIME=' + $('firstFrame').innerHTML + '/' + $('lastFrame').innerHTML;
                        } else if (urlEls[i].startsWith('INFO_FORMAT')) {
                            newURL += '&INFO_FORMAT=image/png';
                        } else {
                            newURL += '&' + urlEls[i];
                        }
                    }
                    // Image will be 400x300, need to allow a little elbow room
                    html += "<br /><a href='#' onclick=popUp('"
                        + newURL + "',450,350) title='Creates a plot of the value"
                        + " at this point over the selected time range'>Create timeseries plot</a>";
                }
            } else {
                html = "Can't get feature info data for this layer <a href='javascript:popUp('whynot.html', 200, 200)'>(why not?)</a>";
            }
            // Remove the "Loading..." popup
            map.removePopup(tempPopup);
            // Show the result in a popup
            var popup = new OpenLayers.Popup (
                "id", // TODO: does this need to be unique?
                lonLat,
                new OpenLayers.Size(100, 50),
                html,
                true, // Means "add a close box"
                null  // Do nothing when popup is closed.
            );
            popup.autoSize = true;
            popups.push(popup);
            map.addPopup(popup);
        });
        Event.stop(e);
    }
}

function popUp(url, width, height)
{
    var day = new Date();
    var id = day.getTime();
    window.open(url, id, 'toolbar=0,scrollbars=0,location=0,statusbar=0,menubar=0,resizable=1,width='
        + width + ',height=' + height + ',left = 300,top = 300');
}

// Clear the popups
function clearPopups() {
    for (var i = 0; i < popups.length; i++) {
        map.removePopup(popups[i]);
    }
    popups.clear();
}

// Called when the user clicks on the name of a displayable layer in the left-hand menu
// Gets the details (units, grid etc) of the given layer. 
function layerSelected(layerDetails)
{
    clearPopups();
    activeLayer = layerDetails;
    gotScaleRange = false;
    resetAnimation();
    
    // Units are ncWMS-specific
    var isNcWMS = false;
    if (typeof layerDetails.units != 'undefined') {
        $('units').innerHTML = '<b>Units: </b>' + layerDetails.units;
        isNcWMS = true;
    } else {
        $('units').innerHTML = '';
    }

    // Set the range selector objects
    var zValue = typeof autoLoad.zValue == 'undefined'
        ? getZValue()
        : parseFloat(autoLoad.zValue);

    // clear the list of z values
    $('zValues').options.length = 0;

    var zAxis = layerDetails.zaxis;
    if (zAxis == null) {
        $('zAxis').innerHTML = ''
        $('zValues').style.visibility = 'hidden';
    } else {
        var axisLabel = zAxis.positive ? 'Elevation' : 'Depth';
        $('zAxis').innerHTML = '<b>' + axisLabel + ' (' + zAxis.units + '): </b>';
        // Populate the drop-down list of z values
        // Make z range selector invisible if there are no z values
        var zValues = zAxis.values;
        var zDiff = 1e10; // Set to some ridiculously-high value
        var nearestIndex = 0;
        for (var j = 0; j < zValues.length; j++) {
            // Create an item in the drop-down list for this z level
            var zLabel = zAxis.positive ? zValues[j] : -zValues[j];
            $('zValues').options[j] = new Option(zLabel, zValues[j]);
            // Find the nearest value to the currently-selected
            // depth level
            var diff = Math.abs(parseFloat(zValues[j]) - zValue);
            if (diff < zDiff) {
                zDiff = diff;
                nearestIndex = j;
            }
        }
        $('zValues').selectedIndex = nearestIndex;
    }
    
    // Only show the scale bar if the data are coming from an ncWMS server
    var scaleVisibility = isNcWMS ? 'visible' : 'hidden';
    $('scaleBar').style.visibility = scaleVisibility;
    $('scaleMin').style.visibility = scaleVisibility;
    $('scaleMax').style.visibility = scaleVisibility;
    $('scaleControls').style.visibility = scaleVisibility;
    $('autoScale').style.visibility = scaleLocked ? 'hidden' : scaleVisibility;
    
    // Set the scale value if this is present in the metadata
    if (typeof layerDetails.scaleRange != 'undefined' &&
            layerDetails.scaleRange != null &&
            layerDetails.scaleRange.length > 1 &&
            layerDetails.scaleRange[0] != layerDetails.scaleRange[1] &&
            !scaleLocked) {
        scaleMinVal = layerDetails.scaleRange[0];
        scaleMaxVal = layerDetails.scaleRange[1];
        $('scaleMin').value = toNSigFigs(scaleMinVal, 4);
        $('scaleMax').value = toNSigFigs(scaleMaxVal, 4);
        gotScaleRange = true;
    }
    
    if (!isIE) {
        // Only show this control if we can use PNGs properly (i.e. not on Internet Explorer)
        $('opacityControl').style.visibility = 'visible';
    }

    // Set the auto-zoom box
    bbox = layerDetails.bbox;
    $('autoZoom').innerHTML = '<a href="#" onclick="map.zoomToExtent(new OpenLayers.Bounds(' +
        bbox[0] + ',' + bbox[1] + ',' + bbox[2] + ',' + bbox[3] +
        '));\">Fit layer to window</a>';

    // Set the link to more details about this dataset
    if (typeof layerDetails.moreInfo != 'undefined' &&
            layerDetails.moreInfo != '') {
        $('moreInfo').innerHTML = '<a target="_blank" href="' + layerDetails.moreInfo +
            '">More information</a>';
    } else {
        $('moreInfo').innerHTML = '';
    }
    
    // Set up the copyright statement
    $('copyright').innerHTML = layerDetails.copyright;

    // Set the palette for this variable
    if (paletteName == null || !scaleLocked) {
        if (typeof layerDetails.defaultPalette != 'undefined') {
            paletteName = layerDetails.defaultPalette;
        }
        updateScaleBar();
    }

    if (!scaleLocked && typeof layerDetails.logScaling != 'undefined') {
        $('scaleSpacing').value = layerDetails.logScaling ? 'logarithmic' : 'linear';
    }

    // Now set up the calendar control
    if (layerDetails.datesWithData == null) {
        // There is no calendar data.  Just update the map
        if (calendar != null) calendar.hide();
        $('date').innerHTML = '';
        $('time').innerHTML = '';
        $('utc').style.visibility = 'hidden';
        updateMap();
    } else {
        datesWithData = layerDetails.datesWithData; // Tells the calendar which dates to disable
        if (calendar == null) {
            // Set up the calendar
            calendar = Calendar.setup({
                flat : 'calendar', // ID of the parent element
                align : 'bl', // Aligned to top-left of parent element
                weekNumbers : false,
                flatCallback : dateSelected
            });
            // For some reason, if we add this to setup() things don't work
            // as expected (dates not selectable on web page when first loaded).
            calendar.setDateStatusHandler(isDateDisabled);
        }
        // Set the range of valid years in the calendar.  Look through
        // the years for which we have data, finding the min and max
        var minYear = 100000000;
        var maxYear = -100000000;
        for (var year in datesWithData) {
            if (typeof datesWithData[year] != 'function') { // avoid built-in functions
                if (year < minYear) minYear = year;
                if (year > maxYear) maxYear = year;
            }
        }
        calendar.setRange(minYear, maxYear);
        // Get the time on the t axis that is nearest to the currently-selected
        // time, as calculated on the server
        calendar.setDate(layerDetails.nearestTime);
        calendar.refresh();
        // N.B. For some reason the call to show() seems sometimes to toggle the
        // visibility of the zValues selector.  Hence we set this visibility
        // below, in updateMap()
        calendar.show();
        // Load the timesteps for this date
        loadTimesteps();
    }
}

// Function that is called when a user clicks on a date in the calendar
function dateSelected(cal)
{
    if (cal.dateClicked) {
        loadTimesteps();
    }
}

// Updates the time selector control.  Finds all the timesteps that occur on
// the same day as the currently-selected date.  Called from the calendar
// control when the user selects a new date
function loadTimesteps()
{
    // Print out date, e.g. "15 Oct 2007"
    $('date').innerHTML = '<b>Date/time: </b>' + calendar.date.print('%d %b %Y');

    // Get the timesteps for this day
    getTimesteps(activeLayer.server, {
        callback: updateTimesteps,
        layerName: activeLayer.id,
        // TODO: Hack! Use date only and adjust server-side logic
        day: makeIsoDate(calendar.date) + 'T00:00:00.000Z'
    });
}

// Gets an ISO Date ("yyyy-mm-dd") for the given Javascript date object.
// Does not contain the time.
function makeIsoDate(date)
{
    // Watch out for low-numbered years when generating the ISO string
    var prefix = '';
    var year = date.getFullYear();
    if (year < 10) prefix = '000';
    else if (year < 100) prefix = '00';
    else if (year < 1000) prefix = '0';
    return prefix + date.print('%Y-%m-%d'); // Date only (no time) in ISO format
}

// Called when we have received the timesteps from the server
function updateTimesteps(times)
{
    // We'll get back a JSON array of ISO8601 times ("hh:mm:ss", UTC, no date information)
    // Build the select box
    var s = '<select id="tValues" onchange="javascript:updateMap()">';
    for (var i = 0; i < times.length; i++) {
        // Construct the full ISO Date-time
        var isoDateTime = makeIsoDate(calendar.date) + 'T' + times[i];// + 'Z';
        // Strip off the trailing "Z" and any zero-length milliseconds
        var stopIndex = times[i].length;
        if (times[i].endsWith('.000Z')) {
            stopIndex -= 5;
        } else if (times[i].endsWith('.00Z')) {
            stopIndex -= 4;
        } else if (times[i].endsWith('.0Z')) {
            stopIndex -= 3;
        } else if (times[i].endsWith('Z')) {
            stopIndex -= 1;
        }
        s += '<option value="' + isoDateTime + '">' + times[i].substring(0, stopIndex) + '</option>';
    }
    s += '</select>';

    $('time').innerHTML = s;
    $('utc').style.visibility = 'visible';

    // If we're autoloading, set the right time in the selection box
    if (autoLoad != null && autoLoad.isoTValue != null) {
        var timeSelect = $('tValues');
        for (i = 0; i < timeSelect.options.length; i++) {
            if (timeSelect.options[i].value == autoLoad.isoTValue) {
                timeSelect.selectedIndex = i;
                break;
            }
        }
    }
    $('setFrames').style.visibility = 'visible';

    if (typeof autoLoad.scaleMin != 'undefined' && typeof autoLoad.scaleMax != 'undefined') {
        $('scaleMin').value = autoLoad.scaleMin;
        $('scaleMax').value = autoLoad.scaleMax;
        validateScale(); // this calls updateMap()
    } else if (!gotScaleRange && !scaleLocked) {// We didn't get a scale range from the layerDetails
        autoScale(true);
    } else {
        updateMap(); // Update the map without changing the scale
    }
}

// Sets the minimum value of the colour scale
function setColourScaleMin(scaleMin)
{
    $('scaleMin').value = scaleMin;
    validateScale(); // This calls updateMap()
}

// Sets the minimum value of the colour scale
function setColourScaleMax(scaleMax)
{
    $('scaleMax').value = scaleMax;
    validateScale(); // This calls updateMap()
}

// Calls the WMS to find the min and max data values, then rescales.
// If this is a newly-selected variable the method gets the min and max values
// for the whole layer.  If not, this gets the min and max values for the viewport.
function autoScale(newVariable)
{
    var dataBounds;
    if ($('tValues')) {
        isoTValue = $('tValues').value;
    }
    if (newVariable) {
        // We use the bounding box of the whole layer 
        dataBounds = bbox[0] + ',' + bbox[1] + ',' + bbox[2] + ',' + bbox[3];
    } else {
        // Use the intersection of the viewport and the layer's bounding box
        dataBounds = getIntersectionBBOX();
    }
    getMinMax(activeLayer.server, {
        callback: gotMinMax,
        layerName: activeLayer.id,
        bbox: dataBounds,
        crs: map.baseLayer.projection.toString(), // (projection is a Projection object)
        elevation: getZValue(),
        time: isoTValue
    });
}

// When the scale is locked, the user cannot change the colour scale either
// by editing manually or clicking "auto".  Furthermore the scale will not change
// when a new layer is loaded
function toggleLockScale()
{
    if (scaleLocked) {
        // We need to unlock the scale
        scaleLocked = false;
        $('lockScale').innerHTML = 'lock';
        $('autoScale').style.visibility = 'visible';
        $('scaleSpacing').disabled = false;
        $('scaleMin').disabled = false;
        $('scaleMax').disabled = false;
    } else {
        // We need to lock the scale
        scaleLocked = true;
        $('lockScale').innerHTML = 'unlock';
        $('autoScale').style.visibility = 'hidden';
        $('scaleSpacing').disabled = true;
        $('scaleMin').disabled = true;
        $('scaleMax').disabled = true;
    }
}

// This function is called when we have received the min and max values from the server
function gotMinMax(minmax)
{
    $('scaleMin').value = toNSigFigs(minmax.min, 4);
    $('scaleMax').value = toNSigFigs(minmax.max, 4);
    validateScale(); // This calls updateMap()
}

// Validates the entries for the scale bar
function validateScale()
{
    var fMin = parseFloat($('scaleMin').value);
    var fMax = parseFloat($('scaleMax').value);
    if (isNaN(fMin)) {
        alert('Scale limits must be set to valid numbers');
        // Reset to the old value
        $('scaleMin').value = scaleMinVal;
    } else if (isNaN(fMax)) {
        alert('Scale limits must be set to valid numbers');
        // Reset to the old value
        $('scaleMax').value = scaleMaxVal;
    } else if (fMin > fMax) {
        alert('Minimum scale value must be less than the maximum');
        // Reset to the old values
        $('scaleMin').value = scaleMinVal;
        $('scaleMax').value = scaleMaxVal;
    } else if (fMin <= 0 && $('scaleSpacing').value == 'logarithmic') {
        alert('Cannot use a logarithmic scale with negative or zero values');
        $('scaleSpacing').value = 'linear';
    } else {
        $('scaleMin').value = fMin;
        $('scaleMax').value = fMax;
        scaleMinVal = fMin;
        scaleMaxVal = fMax;
        updateMap();
    }
}

function resetAnimation()
{
    hideAnimation();
    $('setFrames').style.visibility = 'hidden';
    $('animation').style.visibility = 'hidden';
    $('firstFrame').innerHTML = '';
    $('lastFrame').innerHTML = '';
}
function setFirstAnimationFrame()
{
    $('firstFrame').innerHTML = $('tValues').value;
    $('animation').style.visibility = 'visible';
    setGEarthURL();
}
function setLastAnimationFrame()
{
    $('lastFrame').innerHTML = $('tValues').value;
    $('animation').style.visibility = 'visible';
    setGEarthURL();
}
function createAnimation()
{
    if (!timeSeriesSelected()) {
        alert("Must select a first and last frame for the animation");
        return;
    }
    
    // Get a URL for a WMS request that covers the current map extent
    var urlEls = ncwms.getURL(getMapExtent()).split('&');
    // Replace the parameters as needed.
    var width = $('map').clientWidth;// / 2;
    var height = $('map').clientHeight;// / 2;
    var newURL = urlEls[0];
    for (var i = 1; i < urlEls.length; i++) {
        if (urlEls[i].startsWith('TIME=')) {
            newURL += '&TIME=' + $('firstFrame').innerHTML + '/' + $('lastFrame').innerHTML;
        } else if (urlEls[i].startsWith('FORMAT')) {
            newURL += '&FORMAT=image/gif';
        } else if (urlEls[i].startsWith('WIDTH')) {
            newURL += '&WIDTH=' + width;
        } else if (urlEls[i].startsWith('HEIGHT')) {
            newURL += '&HEIGHT=' + height;
        } else {
            newURL += '&' + urlEls[i];
        }
    }
    $('autoZoom').style.visibility = 'hidden';
    $('hideAnimation').style.visibility = 'visible';
    // We show the "please wait" image then immediately load the animation
    $('loadingAnimationDiv').style.visibility = 'visible'; // This will be hidden by animationLoaded()
    
    // When the mapOverlay has been loaded we call animationLoaded() and place the image correctly
    // on the map
    $('mapOverlay').src = newURL;
    $('mapOverlay').width = width;
    $('mapOverlay').height = height;
}
// Gets the current map extent, checking for out-of-range values
function getMapExtent()
{
    var bounds = map.getExtent();
    var maxBounds = map.maxExtent;
    var top = Math.min(bounds.top, maxBounds.top);
    var bottom = Math.max(bounds.bottom, maxBounds.bottom);
    var left = Math.max(bounds.left, maxBounds.left);
    var right = Math.min(bounds.right, maxBounds.right);
    return new OpenLayers.Bounds(left, bottom, right, top);
}
function animationLoaded()
{
    $('loadingAnimationDiv').style.visibility = 'hidden';
    //$('mapOverlayDiv').style.visibility = 'visible';
    // Load the image into a new layer on the map
    animation_layer = new OpenLayers.Layer.Image(
        "ncWMS", // Name for the layer
        $('mapOverlay').src, // URL to the image
        getMapExtent(), // Image bounds
        new OpenLayers.Size($('mapOverlay').width, $('mapOverlay').height), // Size of image
        { // Other options
            isBaseLayer : false,
            maxResolution: map.baseLayer.maxResolution,
            minResolution: map.baseLayer.minResolution,
            resolutions: map.baseLayer.resolutions
        }
    );
    setVisibleLayer(true);
    map.addLayers([animation_layer]);
}
function hideAnimation()
{
    setVisibleLayer(false);
    $('autoZoom').style.visibility = 'visible';
    $('hideAnimation').style.visibility = 'hidden';
    $('mapOverlayDiv').style.visibility = 'hidden';
}

// Called when the user changes the base layer
function baseLayerChanged(event)
{
    clearPopups();
    // Change the parameters of the map based on the new base layer
    map.setOptions({
       //projection: projCode,
       maxExtent: map.baseLayer.maxExtent,
       maxResolution: map.baseLayer.maxResolution
    });
    map.zoomToMaxExtent();
    if (ncwms != null) {
        ncwms_tiled.maxExtent = map.baseLayer.maxExtent;
        ncwms_tiled.maxResolution = map.baseLayer.maxResolution;
        ncwms_tiled.minResolution = map.baseLayer.minResolution;
        ncwms_tiled.resolutions = map.baseLayer.resolutions;
        // We only wrap the datelinein EPSG:4326
        ncwms_tiled.wrapDateLine = map.baseLayer.projection == 'EPSG:4326';
        ncwms_untiled.maxExtent = map.baseLayer.maxExtent;
        ncwms_untiled.maxResolution = map.baseLayer.maxResolution;
        ncwms_untiled.minResolution = map.baseLayer.minResolution;
        ncwms_untiled.resolutions = map.baseLayer.resolutions;
        ncwms_untiled.wrapDateLine = map.baseLayer.projection == 'EPSG:4326';
        updateMap();
    }
}

// Sets the opacity of the ncwms layer if it exists
function setDataOpacity(value)
{
    if (ncwms != null) ncwms.setOpacity(value);
}

function updateMap()
{
    // Hide the z values selector if it contains no values.  We do this here
    // because it seems that the calendar.show() method can change the visibility
    // unexpectedly
    $('zValues').style.visibility = $('zValues').options.length == 0 ? 'hidden' : 'visible';
    
    var logscale = $('scaleSpacing').value == 'logarithmic';
    
    // Update the intermediate scale markers
    var min = logscale ? Math.log(parseFloat(scaleMinVal)) : parseFloat(scaleMinVal);
    var max = logscale ? Math.log(parseFloat(scaleMaxVal)) : parseFloat(scaleMaxVal);
    var third = (max - min) / 3;
    var scaleOneThird = logscale ? Math.exp(min + third) : min + third;
    var scaleTwoThirds = logscale ? Math.exp(min + 2 * third) : min + 2 * third;
    $('scaleOneThird').innerHTML = toNSigFigs(scaleOneThird, 4);
    $('scaleTwoThirds').innerHTML = toNSigFigs(scaleTwoThirds, 4);
    
    if ($('tValues')) {
        isoTValue = $('tValues').value;
    }
    
    // Set the map bounds automatically
    if (typeof autoLoad.bbox != 'undefined') {
        map.zoomToExtent(getBounds(autoLoad.bbox));
    }
    
    // Make sure the autoLoad object is cleared
    autoLoad = new Object();
    
    // Get the default style for this layer.  There is some defensive programming here to 
    // take old servers into account that don't advertise the supported styles
    var style = typeof activeLayer.supportedStyles == 'undefined' ? 'boxfill' : activeLayer.supportedStyles[0];
    if (paletteName != null) {
        style += '/' + paletteName;
    }

    // Notify the OpenLayers widget
    // TODO get the map projection from the base layer
    // TODO use a more informative title
    var params = {
        layers: activeLayer.id,
        elevation: getZValue(),
        time: isoTValue,
        transparent: 'true',
        styles: style,
        crs: map.baseLayer.projection,
        colorscalerange: scaleMinVal + ',' + scaleMaxVal,
        numcolorbands: $('numColorBands').value,
        logscale: logscale
    };
    if (ncwms == null) {
        // Buffer is set to 1 to avoid loading a large halo of tiles outside the
        // current viewport
        ncwms_tiled = new OpenLayers.Layer.WMS1_3("ncWMS",
            activeLayer.server == '' ? 'wms' : activeLayer.server, 
            params,
            {buffer: 1, wrapDateLine: map.baseLayer.projection == 'EPSG:4326'}
        );
        ncwms_untiled = new OpenLayers.Layer.WMS1_3("ncWMS",
            activeLayer.server == '' ? 'wms' : activeLayer.server, 
            params,
            {buffer: 1, ratio: 1.5, singleTile: true, wrapDateLine: map.baseLayer.projection == 'EPSG:4326'}
        );
        setVisibleLayer(false);
        map.addLayers([ncwms_tiled, ncwms_untiled]);
        // Create a layer for coastlines
        // TOOD: only works at low res (zoomed out)
        //var coastline_wms = new OpenLayers.Layer.WMS( "Coastlines", 
        //    "http://labs.metacarta.com/wms/vmap0?", {layers: 'coastline_01', transparent: 'true' } );
        //map.addLayers([ncwms, coastline_wms]);
        //map.addLayers([ncwms_tiled, ncwms_untiled]);
    } else {
        setVisibleLayer(false);
        ncwms.url = activeLayer.server == '' ? 'wms' : activeLayer.server;
        ncwms.mergeNewParams(params);
    }
    
    var imageURL = ncwms.getURL(new OpenLayers.Bounds(bbox[0], bbox[1], bbox[2], bbox[3]));
    $('testImage').innerHTML = '<a target="_blank" href="' + imageURL + '">link to test image</a>';
    setGEarthURL();
    setPermalinkURL();
}

// Shows a pop-up window with the available palettes for the user to select
// This is called when the user clicks the colour scale bar
function showPaletteSelector()
{
    updatePaletteSelector();
    paletteSelector.render(document.body);
    paletteSelector.show();
}

// Updates the contents of the palette selection table
function updatePaletteSelector()
{
    // Populate the palette selector dialog box
    // TODO: revert to default palette if layer doesn't support this one
    var palettes = activeLayer.palettes;
    if (palettes == null || palettes.length == 0) {
        $('paletteDiv').innerHTML = 'There are no alternative palettes for this layer';
        return;
    }
    
    // TODO test if coming from a different server
    var width = 50;
    var height = 200;
    var paletteUrl = activeLayer.server + 'wms?REQUEST=GetLegendGraphic' +
        '&LAYER=' + activeLayer.id +
        '&COLORBARONLY=true' +
        '&WIDTH=1' +
        '&HEIGHT=' + height +
        '&NUMCOLORBANDS=' + $('numColorBands').value;
    var palStr = '<div style="overflow: auto">'; // ensures scroll bars appear if necessary
    palStr += '<table border="1"><tr>';
    for (var i = 0; i < palettes.length; i++) {
        palStr += '<td><img src="' + paletteUrl + '&PALETTE=' + palettes[i] +
            '" width="' + width + '" height="' + height + '" title="' + palettes[i] +
            '" onclick="paletteSelected(\'' + palettes[i] + '\')"' +
            '/></td>';
    }
    palStr += '</tr></table></div>';
    $('paletteDiv').innerHTML = palStr;
}

// Called when the user selects a new palette in the palette selector
function paletteSelected(thePalette)
{
    paletteName = thePalette;
    paletteSelector.hide();
    // Change the colour scale bar on the main page
    updateScaleBar();
    updateMap();
}

// Updates the colour scale bar URL
function updateScaleBar()
{
    $('scaleBar').src = 'wms?REQUEST=GetLegendGraphic&COLORBARONLY=true&WIDTH=1&HEIGHT=398'
        + '&PALETTE=' + paletteName + '&NUMCOLORBANDS=' + $('numColorBands').value;
}

// Decides whether to display the animation, or the tiled or untiled
// version of the ncwms layer
function setVisibleLayer(animation)
{
    // TODO: repeats code above
    var style = typeof activeLayer.supportedStyles == 'undefined' ? 'boxfill' : activeLayer.supportedStyles[0];
    if (animation) {
        setLayerVisibility(animation_layer, true);
        setLayerVisibility(ncwms_tiled, false);
        setLayerVisibility(ncwms_untiled, false);
    } else if (style.toLowerCase() == 'vector') {
        setLayerVisibility(animation_layer, false);
        setLayerVisibility(ncwms_tiled, false);
        setLayerVisibility(ncwms_untiled, true);
        ncwms = ncwms_untiled;
    } else {
        setLayerVisibility(animation_layer, false);
        setLayerVisibility(ncwms_tiled, true);
        setLayerVisibility(ncwms_untiled, false);
        ncwms = ncwms_tiled;
    }
    layerSwitcher.layerStates = []; // forces redraw
    layerSwitcher.redraw();
}

function setLayerVisibility(layer, visible)
{
    if (layer != null) {
        layer.setVisibility(visible);
        layer.displayInLayerSwitcher = visible;
    }
}

// Gets the Z value set by the user
function getZValue()
{
    // If we have no depth information, assume we're at the surface.  This
    // will be ignored by the map server
    return $('zValues').options.length == 0 ? 0 : $('zValues').value;
}

// Sets the permalink, i.e. the link back to this view of the page
function setPermalinkURL()
{
    if (activeLayer != null) {
        // Note that we must use window.top to get the containing page, in case
        // the Godiva2 page is embedded in an iframe
        // Get the window location, minus any query string or hash
        var loc = window.top.location;
        var url = loc.protocol + '//' + loc.host + loc.pathname;
        url +=
            '?menu=' + menu +
            '&layer=' + activeLayer.id +
            '&elevation=' + getZValue() +
            '&time=' + isoTValue +
            '&scale=' + scaleMinVal + ',' + scaleMaxVal +
            '&bbox=' + map.getExtent().toBBOX();
        $('permalink').innerHTML = '<a target="_blank" href="' + url +
            '">Permalink</a>&nbsp;|&nbsp;<a href="mailto:?subject=Godiva2%20link&body='
            + escape(url) + '">email</a>';
        $('permalink').style.visibility = 'visible';
    }
}

// Sets the URL for "Open in Google Earth"
// TODO: does this screw up if we're looking in polar stereographic coords?
function setGEarthURL()
{
    if (ncwms != null) {
        // Get a URL for a WMS request that covers the current map extent
        var mapBounds = map.getExtent();
        var urlEls = ncwms.getURL(mapBounds).split('&');
        var gEarthURL = urlEls[0];
        for (var i = 1; i < urlEls.length; i++) {
            if (urlEls[i].startsWith('FORMAT')) {
                // Make sure the FORMAT is set correctly
                gEarthURL += '&FORMAT=application/vnd.google-earth.kmz';
            } else if (urlEls[i].startsWith('TIME') && timeSeriesSelected()) {
                // If we can make an animation, do so
                gEarthURL += '&TIME=' + $('firstFrame').innerHTML + '/' + $('lastFrame').innerHTML;
            } else if (urlEls[i].startsWith('BBOX')) {
                // Set the bounding box so that there are no transparent pixels around
                // the edge of the image: i.e. find the intersection of the layer BBOX
                // and the viewport BBOX
                gEarthURL += '&BBOX=' + getIntersectionBBOX();
            } else if (urlEls[i].startsWith('WIDTH')) {
                gEarthURL += '&WIDTH=' + map.size.w;
            } else if (urlEls[i].startsWith('HEIGHT')) {
                gEarthURL += '&HEIGHT=' + map.size.h;
            } else {
                gEarthURL += '&' + urlEls[i];
            }
        }
        if (timeSeriesSelected()) {
            $('googleEarth').innerHTML = '<a href="' + gEarthURL + '">Open animation in Google Earth</a>';
        } else {
            $('googleEarth').innerHTML = '<a href="' + gEarthURL + '">Open in Google Earth</a>';
        }
    }
}

// Returns a bounding box as a string in format "minlon,minlat,maxlon,maxlat"
// that represents the intersection of the currently-visible map layer's 
// bounding box and the viewport's bounding box.
function getIntersectionBBOX()
{
    if (map.baseLayer.projection == 'EPSG:4326') {
        // We compute the intersection of the bounding box and the currently-
        // visible map extent
        var mapBboxEls = map.getExtent().toArray();
        // bbox is the bounding box of the currently-visible layer
        var newBBOX = Math.max(mapBboxEls[0], bbox[0]) + ',';
        newBBOX += Math.max(mapBboxEls[1], bbox[1]) + ',';
        newBBOX += Math.min(mapBboxEls[2], bbox[2]) + ',';
        newBBOX += Math.min(mapBboxEls[3], bbox[3]);
        return newBBOX;
    } else {
        return map.getExtent().toBBOX();
    }
}

// Formats the given value to numSigFigs significant figures
// WARNING: Javascript 1.5 only!
function toNSigFigs(value, numSigFigs)
{
    if (!value.toPrecision) {
        // TODO: do this somewhere more useful
        alert("Your browser doesn't support Javascript 1.5");
        return value;
    } else {
        return value.toPrecision(numSigFigs);
    }
}

// Returns true if the user has selected a time series
function timeSeriesSelected()
{
    return $('firstFrame').innerHTML != '' && $('lastFrame').innerHTML != '';
}

// Takes a BBOX string of the form "minlon,minlat,maxlon,maxlat" and returns
// the corresponding OpenLayers.Bounds object
// TODO: error checking
function getBounds(bboxStr)
{
    var bboxEls = bboxStr.split(",");
    return new OpenLayers.Bounds(parseFloat(bboxEls[0]), parseFloat(bboxEls[1]),
        parseFloat(bboxEls[2]), parseFloat(bboxEls[3]));
}
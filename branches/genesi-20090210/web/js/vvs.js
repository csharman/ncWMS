//
// Javascript for VVS page.
//

var map = null;
var isIE;
var demis_wms = null;
var coastline_wms = null;
var globmodel_layer = null;
var sciamachy_layer = null;
var paletteSelector = null; // Pop-up panel for selecting a new palette

// Called when the page has loaded
window.onload = function()
{
    // Detect the browser (IE6 doesn't render PNGs properly so we don't provide
    // the option to have partial overlay opacity)
    isIE = navigator.appVersion.indexOf('MSIE') >= 0;

    // Stop the pink tiles appearing on error
    OpenLayers.Util.onImageLoadError = function() {  this.style.display = ""; this.src="./images/blank.png"; }

    // Set up the OpenLayers map widget
    map = new OpenLayers.Map('map');
    demis_wms = new OpenLayers.Layer.WMS1_1_1( "Demis WMS",
        "http://www2.Demis.nl/MapServer/Request.asp?WRAPDATELINE=TRUE", {layers:
        'Bathymetry,Topography,Hillshading,Coastlines,Builtup+areas,Waterbodies,Rivers,Streams,Railroads,Highways,Roads,Trails,Borders,Cities,Airports'});

    coastline_wms = new OpenLayers.Layer.WMS1_1_1( "Coastlines",
        "http://labs.metacarta.com/wms/vmap0?", {layers: 'coastline_01', transparent: 'true' } );

    globmodel_layer = new OpenLayers.Layer.WMS1_3(
        'GlobModel',
        'wms',
        {
            layers: 'GLOBMODEL_ozone/colo3',
            transparent: true,
            crs: 'CRS:84'
        },
        {
            buffer: 1,
            wrapDateLine: true
        }
    );
        
    sciamachy_layer = new OpenLayers.Layer.WMS1_3(
        'SCIAMACHY',
        'wms',
        {
            layers: 'SCIAMACHY',
            transparent: true,
            crs: 'CRS:84'
        },
        {
            buffer: 1,
            wrapDateLine: true
        }
    );

    updateDates();  // Looks at the UI and calculates the TIME parameter for GM and SCIA

    map.addLayers([demis_wms, globmodel_layer, sciamachy_layer, coastline_wms]);

    updateVisibilities(); // Looks at the UI and sets the layer visibilities
    updateOpacities();
    updateColorScale();

    map.addControl(new OpenLayers.Control.LoadingPanel());
    
    map.addControl(new OpenLayers.Control.MousePosition({
        prefix: 'Lon: ',
        separator: ' Lat: ',
        numDigits: 3
    }));
    map.zoomTo(1);

    // Set up the palette selector pop-up
    paletteSelector = new YAHOO.widget.Panel("paletteSelector", {
        width:"250px",
        constraintoviewport: true,
        fixedcenter: true,
        underlay:"shadow",
        close:true,
        visible:false,
        draggable:true,
        modal:true
    });
    updatePalette('occam'); // default palette name
}

function updateVisibilities() {
    globmodel_layer.setVisibility($('gmVisibility').checked);
    sciamachy_layer.setVisibility($('sciaVisibility').checked);
    demis_wms.setVisibility($('topoVisibility').checked);
    coastline_wms.setVisibility($('coastVisibility').checked);
}

function updateOpacities() {
    globmodel_layer.setOpacity($('gmOpacity').value);
    sciamachy_layer.setOpacity($('sciaOpacity').value);
}

function updateColorScale() {
    var scaleMin = $('scaleMin').value;
    var scaleMax = $('scaleMax').value;

    var third = (scaleMax - scaleMin) / 3;
    var scaleOneThird = parseFloat(scaleMin) + third;
    var scaleTwoThirds = parseFloat(scaleMin) + 2 * third;

    $('scaleOneThird').innerHTML = toNSigFigs(scaleOneThird, 4);
    $('scaleTwoThirds').innerHTML = toNSigFigs(scaleTwoThirds, 4);

    var conv = 2.1414e-7; // Conversion factor from DU to GlobModel units
                          // N.B. this is a factor of 100 too small due to an
                          // error in the data!
    globmodel_layer.mergeNewParams({ colorScaleRange: scaleMin * conv + ',' + scaleMax * conv });
    sciamachy_layer.mergeNewParams({ colorScaleRange: scaleMin + ',' + scaleMax });
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

function updatePalette(paletteName) {
    paletteSelector.hide();
    $('scaleBar').src = 'wms?REQUEST=GetLegendGraphic&COLORBARONLY=true&WIDTH=1&HEIGHT=398'
        + '&PALETTE=' + paletteName + '&NUMCOLORBANDS=' + $('numColorBands').value;
    globmodel_layer.mergeNewParams({
        styles: 'boxfill/' + paletteName,
        numcolorbands: $('numColorBands').value
    });
    sciamachy_layer.mergeNewParams({
        styles: paletteName,
        numcolorbands: $('numColorBands').value
    });
}

function updateDates() {
    var globModelDay = $('globModelDay').value;
    var globModelHour = $('globModelHour').value;

    var gmDate = new Date();
    gmDate.setUTCFullYear(2006, 7, globModelDay); // 7 = August
    gmDate.setUTCHours(globModelHour, 0, 0, 0); // Sets hours, minutes, seconds and ms
    var days = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    $('dayName').innerHTML = days[gmDate.getDay()];

    // Calculate the date/time range for the sciamachy data
    var sciaWindowMs = $('sciaWindow').value * 60 * 1000;
    var sciaLow = new Date(gmDate.getTime() - sciaWindowMs);
    var sciaHigh = new Date(gmDate.getTime() + sciaWindowMs);

    globmodel_layer.mergeNewParams({ time: dateToIso(gmDate) });
    sciamachy_layer.mergeNewParams({ time: dateToIso(sciaLow) + '/' + dateToIso(sciaHigh) });
}

// Converts a Javascript Date object to an ISO string to millisecond precision,
// in UTC
function dateToIso(date) {
    var zeropad = function (num) { return ((num < 10) ? '0' : '') + num; }

    var millispad = function (num) {
        var pad = '';
        if (num < 10) pad = '00';
        else if (num < 100) pad = '0';
        return pad + num;
    }

    return date.getUTCFullYear() + '-'
         + zeropad(date.getUTCMonth() + 1) + '-'
         + zeropad(date.getUTCDate()) + 'T'
         + zeropad(date.getUTCHours()) + ':'
         + zeropad(date.getUTCMinutes()) + ':'
         + zeropad(date.getUTCSeconds()) + '.'
         + millispad(date.getUTCMilliseconds()) + 'Z';
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
    // TODO: palette names hard-coded for now
    var palettes = ['rainbow', 'occam', 'greyscale', 'occam_pastel-30'];
    if (palettes == null || palettes.length == 0) {
        $('paletteDiv').innerHTML = 'There are no alternative palettes for this layer';
        return;
    }

    var width = 50;
    var height = 200;
    var paletteUrl = 'wms?REQUEST=GetLegendGraphic' +
        '&LAYER=' + 'GLOBMODEL_ozone/colo3' + // TODO: hardcoded
        '&COLORBARONLY=true' +
        '&WIDTH=1' +
        '&HEIGHT=' + height +
        '&NUMCOLORBANDS=' + $('numColorBands').value;
    var palStr = '<div style="overflow: auto">'; // ensures scroll bars appear if necessary
    palStr += '<table border="1"><tr>';
    for (var i = 0; i < palettes.length; i++) {
        palStr += '<td><img src="' + paletteUrl + '&PALETTE=' + palettes[i] +
            '" width="' + width + '" height="' + height + '" title="' + palettes[i] +
            '" onclick="updatePalette(\'' + palettes[i] + '\')"' +
            '/></td>';
    }
    palStr += '</tr></table></div>';
    $('paletteDiv').innerHTML = palStr;
}

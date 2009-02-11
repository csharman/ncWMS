//
// Javascript for VVS page.
//

var map = null;
var isIE;
var globmodel_layer = null;
var sciamachy_layer = null;

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
    var demis_wms = new OpenLayers.Layer.WMS1_1_1( "Demis WMS",
        "http://www2.Demis.nl/MapServer/Request.asp?WRAPDATELINE=TRUE", {layers:
        'Bathymetry,Topography,Hillshading,Coastlines,Builtup+areas,Waterbodies,Rivers,Streams,Railroads,Highways,Roads,Trails,Borders,Cities,Airports'});

    var coastline_wms = new OpenLayers.Layer.WMS1_1_1( "Coastlines",
        "http://labs.metacarta.com/wms/vmap0?", {layers: 'coastline_01', transparent: 'true' } );

    var palette = 'rainbow';
    var scaleMin = 180.0; // Dobson Units
    var scaleMax = 400.0;
    var conv = 2.1414e-7; // Conversion factor from DU to GlobModel units
                          // N.B. this is a factor of 100 too small due to an
                          // error in the data!

    globmodel_layer = new OpenLayers.Layer.WMS1_3(
        'GlobModel',
        'wms',
        {
            layers: 'GLOBMODEL_ozone/colo3',
            styles: 'boxfill/' + palette,
            time: '2006-08-20T12:00:00.000Z',
            transparent: true,
            crs: 'CRS:84',
            colorscalerange: scaleMin * conv + ',' + scaleMax * conv
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
            styles: palette,
            time: '2006-08-20T10:00:00.000Z/2006-08-20T14:00:00.000Z',
            transparent: true,
            crs: 'CRS:84',
            colorscalerange: scaleMin + ',' + scaleMax
        },
        {
            buffer: 1,
            wrapDateLine: true
        }
    );

    map.addLayers([demis_wms, globmodel_layer, sciamachy_layer, coastline_wms/*ol_wms, osm_wms, human_wms*/]);

    //map.setBaseLayer(demis_wms);

    map.addControl(new OpenLayers.Control.LoadingPanel());
    map.addControl(new OpenLayers.Control.LayerSwitcher());
    
    //map.addControl(new OpenLayers.Control.MousePosition({prefix: 'Lon: ', separator: ' Lat:'}));
    map.zoomTo(1);

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

function setGlobModelVisibility(checked) {
    globmodel_layer.setVisibility(checked);
}

function setSciamachyVisibility(checked) {
    sciamachy_layer.setVisibility(checked);
}
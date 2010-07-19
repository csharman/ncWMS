/**
 * Class: Godiva2.Projection
 * Provides simple properties about a projection as set of constants
 */

 Godiva2.Projection =
 {
    /** Calculates the maximum extent for polar projections (1/4 of theoretical max) */
     /*_calculatePolarExtent: function()
     {
        var polarMaxExtent = new OpenLayers.Bounds(-10700000, -10700000, 14700000, 14700000);
        var halfSideLength = (polarMaxExtent.top - polarMaxExtent.bottom) / (4 * 2);
        var centre = ((polarMaxExtent.top - polarMaxExtent.bottom) / 2) + polarMaxExtent.bottom;
        //var low = centre - halfSideLength;
        //var high = centre + halfSideLength;
        //var polarMaxResolution = (high - low) / 256;
        var windowLow = centre - 2 * halfSideLength;
        var windowHigh = centre + 2 * halfSideLength;
        return new OpenLayers.Bounds(windowLow, windowLow, windowHigh, windowHigh);
     },

     _calculatePolarMaxResolution: function()
     {
         var polarExtent = POLAR_EXTENT;
         // TODO: calculate based on a map's tile size?
         return (polarExtent.top - polarExtent.bottom) / 512.0;
     },*/

    LON_LAT: {
        code: 'EPSG:4326',
        units: 'degrees',
        maxResolution: 360.0 / 256, // TODO: calculate based on a map's tile size?
        maxExtent: new OpenLayers.Bounds(-180, -90, 180, 90)
    },

     NORTH_POLAR_STEREOGRAPHIC: {
        code: 'EPSG:32661',
        units: 'm',
        maxResolution: 24804.6875,
        // Extent is 1/4 of full polar stereographic extent
        maxExtent: new OpenLayers.Bounds(-4350000, -4350000, 8350000, 8350000)
     },

    SOUTH_POLAR_STEREOGRAPHIC: {
        code: 'EPSG:32761',
        units: 'm',
        maxResolution: 24804.6875,
        maxExtent: new OpenLayers.Bounds(-4350000, -4350000, 8350000, 8350000)
    }
 };
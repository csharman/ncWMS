# routines that produce output (HTML or XML) for web pages
try:
    from cStringIO import StringIO
except ImportError:
    from StringIO import StringIO

from xml.utils import iso8601
import time, math, calendar
import ncWMS
import config
import nj22dataset

def metadata(req, configLines):
    """ Processes a request for metadata from the Godiva2 web interface """
    params = ncWMS.RequestParser(req.args)
    datasets = ncWMS.getDatasets(configLines)
    metadataItem = params.getParamValue("item")
    if metadataItem == "datasets":
        req.write(getDatasetsDiv(datasets))
    elif metadataItem == "variables":
        dataset = params.getParamValue("dataset")
        req.write(getVariables(datasets, dataset))
    elif metadataItem == "variableDetails":
        req.content_type = "text/xml"
        dataset = params.getParamValue("dataset")
        varID = params.getParamValue("variable")
        req.write(getVariableDetails(datasets, dataset, varID))
    elif metadataItem == "calendar":
        req.content_type = "text/xml"
        dataset = params.getParamValue("dataset")
        varID = params.getParamValue("variable")
        dateTime = params.getParamValue("dateTime")
        req.write(getCalendar(datasets, dataset, varID, dateTime))
    elif metadataItem == "timesteps":
        dataset = params.getParamValue("dataset")
        varID = params.getParamValue("variable")
        tIndex = int(params.getParamValue("tIndex"))
        req.write(getTimesteps(datasets, dataset, varID, tIndex))

def getDatasetsDiv(datasets):
    """ returns a string with a set of divs representing the datasets.
        Quick and dirty. """
    str = StringIO()
    for ds in datasets.keys():
        str.write("<div id=\"%sDiv\">" % ds)
        str.write("<div id=\"%s\">%s</div>" % (ds, datasets[ds].title))
        str.write("<div id=\"%sContent\">" % ds)
        str.write("Variables in the %s dataset will appear here" % datasets[ds].title)
        str.write("</div>")
        str.write("</div>")
    s = str.getvalue()
    str.close()
    return s

def getVariables(datasets, dataset):
    """ returns an HTML table containing a set of variables for the given dataset. """
    str = StringIO()
    str.write("<table cellspacing=\"0\"><tbody>")
    vars = nj22dataset.getVariableMetadata(datasets[dataset].location)
    for varID in vars.keys():
        str.write("<tr><td>")
        str.write("<a href=\"#\" onclick=\"javascript:variableSelected('%s', '%s')\">%s</a>" % (dataset, varID, vars[varID].title))
        str.write("</td></tr>")
    str.write("</tbody></table>")
    s = str.getvalue()
    str.close()
    return s

def getVariableDetails(datasets, dataset, varID):
    """ returns an XML document containing the details of the given variable
        in the given dataset. """
    str = StringIO()
    var = nj22dataset.getVariableMetadata(datasets[dataset].location)[varID]
    str.write("<variableDetails dataset=\"%s\" variable=\"%s\" units=\"%s\">" % (dataset, var.title, var.units))
    str.write("<axes>")
    if var.zvalues is not None:
        str.write("<axis type=\"z\" units=\"%s\" positive=\"%d\">" % (var.zunits, var.zpositive))
        for z in var.zvalues:
            str.write("<value>%f</value>" % math.fabs(z))
        str.write("</axis>")
    str.write("</axes>")
    str.write("<range><min>%f</min><max>%f</max></range>" % (var.validMin, var.validMax))
    str.write("</variableDetails>")
    s = str.getvalue()
    str.close()
    return s

def getCalendar(datasets, dataset, varID, dateTime):
    """ returns an HTML calendar for the given dataset and variable.
        dateTime is a string in ISO 8601 format with the required
        'focus time' """
    # Get an array of time axis values in seconds since the epoch
    tValues = nj22dataset.getVariableMetadata(datasets[dataset].location)[varID].tvalues
    # TODO: is this the right thing to do here?
    if tValues is None:
        return ""
    str = StringIO()
    prettyDateFormat = "%d %b %Y"

    # Find the closest time step to the given dateTime value
    # TODO: binary search would be more efficient
    reqTime = iso8601.parse(dateTime) # Gives seconds since the epoch
    diff = 1e20
    for i in xrange(len(tValues)):
        testDiff = math.fabs(tValues[i] - reqTime)
        if testDiff < diff:
            # Axis is monotonic so we should move closer and closer
            # to the nearest value
            diff = testDiff
            nearestIndex = i
        elif i > 0:
            # We've moved past the closest date
            break
    
    str.write("<root>")
    str.write("<nearestValue>%s</nearestValue>" % iso8601.tostring(tValues[nearestIndex]))
    str.write("<prettyNearestValue>%s</prettyNearestValue>" % time.strftime(prettyDateFormat, time.gmtime(tValues[nearestIndex])))
    str.write("<nearestIndex>%d</nearestIndex>" % nearestIndex)

    # create a struct_time tuple with zero timezone offset (i.e. GMT)
    nearesttime = time.gmtime(tValues[nearestIndex])

    # Now print out the calendar in HTML
    str.write("<calendar>")
    str.write("<table><tbody>")
    # Add the navigation buttons at the top of the month view
    str.write("<tr>")
    str.write("<td><a href=\"#\" onclick=\"javascript:setCalendar('%s','%s','%s'); return false\">&lt;&lt;</a></td>" % (dataset, varID, _getYearBefore(nearesttime)))
    str.write("<td><a href=\"#\" onclick=\"javascript:setCalendar('%s','%s','%s'); return false\">&lt;</a></td>" % (dataset, varID, _getMonthBefore(nearesttime)))
    str.write("<td colspan=\"3\">%s</td>" % _getHeading(nearesttime))
    str.write("<td><a href=\"#\" onclick=\"javascript:setCalendar('%s','%s','%s'); return false\">&gt;</a></td>" % (dataset, varID, _getMonthAfter(nearesttime)))
    str.write("<td><a href=\"#\" onclick=\"javascript:setCalendar('%s','%s','%s'); return false\">&gt;&gt;</a></td>" % (dataset, varID, _getYearAfter(nearesttime)))
    str.write("</tr>")
    # Add the day-of-week headings
    str.write("<tr><th>M</th><th>T</th><th>W</th><th>T</th><th>F</th><th>S</th><th>S</th></tr>")
    # Add the calendar body
    tValIndex = 0 # index in tvalues array
    for week in calendar.monthcalendar(nearesttime[0], nearesttime[1]):
        str.write("<tr>")
        for day in week:
            if day > 0:
                # Search through the t axis and find out whether we have
                # any data for this particular day
                found = 0
                calendarDay = (nearesttime[0], nearesttime[1], day, 0, 0, 0, 0, 0, 0)
                while not found and tValIndex < len(tValues):
                    axisDay = time.gmtime(tValues[tValIndex])
                    res = _compareDays(axisDay, calendarDay)
                    if res == 0:
                        found = 1 # Found data on this day
                    elif res < 0:
                        tValIndex = tValIndex + 1 # Date on axis is before target day
                    else:
                        break # Date on axis is after target day: no point searching further
                if found:
                    tValue = iso8601.tostring(tValues[tValIndex])
                    prettyTValue = time.strftime(prettyDateFormat, axisDay)
                    str.write("<td id=\"t%d\"><a href=\"#\" onclick=\"javascript:getTimesteps('%s','%s','%d','%s','%s'); return false\">%d</a></td>" % (tValIndex, dataset, varID, tValIndex, tValue, prettyTValue, day))
                else:
                    str.write("<td>%d</td>" % day)
            else:
                str.write("<td></td>")
        str.write("</tr>")

    str.write("</tbody></table>")
    str.write("</calendar>")
    str.write("</root>")

    s = str.getvalue()
    str.close()
    return s

def _getHeading(date):
    """ Returns a string, e.g. "Oct 2006" for the given date """
    month = ("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")[date[1] - 1]
    return "%s %d" % (month, date[0])

def _getYearBefore(date):
    """ Returns an ISO8601-formatted date that is exactly one year earlier than
        the given date """
    # Get the tuple of year, month, day etc
    newDate = tuple([date[0] - 1] + list(date[1:]))
    return iso8601.tostring(time.mktime(newDate))

def _getYearAfter(date):
    """ Returns an ISO8601-formatted date that is exactly one year later than
        the given date """
    # Get the tuple of year, month, day etc
    newDate = tuple([date[0] + 1] + list(date[1:]))
    return iso8601.tostring(time.mktime(newDate))

def _getMonthBefore(date):
    """ Returns an ISO8601-formatted date that is exactly one month earlier than
        the given date """
    if date[1] == 1:
        month = 12
        year = date[0] - 1
    else:
        month = date[1] - 1
        year = date[0]
    newDate = tuple([year] + [month] + list(date[2:]))
    return iso8601.tostring(time.mktime(newDate))

def _getMonthAfter(date):
    """ Returns an ISO8601-formatted date that is exactly one month later than
        the given date """
    if date[1] == 12:
        month = 1
        year = date[0] + 1
    else:
        month = date[1] + 1
        year = date[0]
    newDate = tuple([year] + [month] + list(date[2:]))
    return iso8601.tostring(time.mktime(newDate))

def getTimesteps(datasets, dataset, varID, tIndex):
    """ Returns an HTML select box allowing the user to select a 
        set of times for a given day """
    # Get an array of time axis values in seconds since the epoch
    tValues = nj22dataset.getVariableMetadata(datasets[dataset].location)[varID].tvalues
    # TODO: is this the right thing to do here?
    if tValues is None:
        return ""
    str = StringIO()
    
    # We find a list of indices of timesteps that are on the same day
    # as the provided tIndex
    indices = {}
    reftime = time.gmtime(tValues[tIndex])
    indices[tIndex] = "%02d:%02d:%02d" % (reftime[3], reftime[4], reftime[5])
    for i in xrange(tIndex + 1, len(tValues)):
        t = time.gmtime(tValues[i])
        diff = _compareDays(reftime, t)
        if diff == 0:
            indices[i] = "%02d:%02d:%02d" % (t[3], t[4], t[5])
        else:
            break
    for i in xrange(tIndex - 1, -1, -1): # count backwards through the axis indices to zero
        t = time.gmtime(tValues[i])
        diff = _compareDays(reftime, t)
        if diff == 0:
            indices[i] = "%02d:%02d:%02d" % (t[3], t[4], t[5])
        else:
            break

    # Write the selection box with the timesteps
    str.write("<select id=\"tValues\" onchange=\"javascript:updateMap()\">")
    keys = indices.keys()
    keys.sort()
    for i in keys:
        str.write("<option value=\"%s\">%s</option>" % (iso8601.tostring(tValues[i]), indices[i]))
    str.write("</select>")

    s = str.getvalue()
    str.close()
    return s

def _compareDays(d1, d2):
    """ Both arguments are struct_time tuples.  Returns 0 if both dates fall
        on the same day.  Returns -1 if d1 falls before d2 and +1 if d1 falls
        after d2 """
    if d1[0] == d2[0] and d1[1] == d2[1] and d1[2] == d2[2]:
        return 0
    else:
        d1s = time.mktime(d1)
        d2s = time.mktime(d2)
        if d1s < d2s:
            return -1
        else:
            return 1
    
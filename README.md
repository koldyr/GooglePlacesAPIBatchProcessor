# Google Places API Batch Processor

Batch Processor to search specific type of places based on list of input data and defined search area with Google Places API

It will accept input file with list of string as input query for Places API. As result json file with all found places will be generated. Structure of encoded data is following:
<pre>{
    "placeId": string;
    "name": string;
    "address": string
    "location": {
        "lat": number,
        "lng": number
    },
    "rating": number,
    "elevation": number,
    "fireStationDist": number
}</pre>

Elevation and distance to nearest Fire Station is in meters.

You can run it as web or as command-line application. Command-line app has restriction to max 60 results per query - Google API limitation.
With Web application there is no such restriction but you need to provide area to bound your search.


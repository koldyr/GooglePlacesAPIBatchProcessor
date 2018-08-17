let findPlacesService;

function initMap() {
    const map = new google.maps.Map(document.getElementById('map'), {
        center: { lat: 32, lng: -96 },
        zoom: 4
    });

    google.maps.event.addListener(map, 'click', function (event) {
        console.log(event.latLng.toString());
    });

    // const usaMainLand = {south: 27, west: -140, north: 50, east: -50};
    // const colorado = { south: 37, west: -109, north: 41, east: -102 };
    const la = { south: 33, west: -118, north: 35, east: -117 };

    findPlacesService = new FindPlacesService(map, la);
}

function FindPlacesService(gmap, searchArea) {
    const map = gmap;
    const placesService = new google.maps.places.PlacesService(map);
    const elevationService = new google.maps.ElevationService();
    const bounds = new google.maps.LatLngBounds();

    let type = 'store';

    let brands;

    let quadrantIndex;
    const quadrants = getQuadrants(searchArea);
    console.log('quadrants', quadrants.length);

    this.startProcess = function () {
        getBrands().then(doPlacesSearch);
    };

    function doPlacesSearch(data) {
        brands = data;

        nextBrandSearch(brands.shift().toLowerCase());
    }

    function nextBrandSearch(brand) {
        console.log('Staring', brand);

        const places = [];
        quadrantIndex = 0;

        placesService.nearbySearch({
            keyword: brand,
            bounds: quadrants[quadrantIndex],
            type: type
        }, function (results, status, pagination) {
            try {
                handleResults(results, status, pagination, places);

                if (!pagination || !pagination.hasNextPage) {
                    quadrantIndex++;

                    setTimeout(nextQuadrantSearch, 1, brand, places);
                }
            } catch (ex) {
                if (ex.message === google.maps.places.PlacesServiceStatus.OVER_QUERY_LIMIT) {
                    setTimeout(nextBrandSearch, 10000, brand, places);
                }
            }
        });
    }

    function nextQuadrantSearch(brand, places) {
        console.log(brand, 'index:' + quadrantIndex, 'size:' + places.length);

        placesService.nearbySearch({
            keyword: brand,
            bounds: quadrants[quadrantIndex],
            type: type
        }, function (results, status, pagination) {
            try {
                handleResults(results, status, pagination, places);

                if (!pagination || !pagination.hasNextPage) {
                    quadrantIndex++;

                    if (quadrantIndex < quadrants.length) {
                        setTimeout(nextQuadrantSearch, 1, brand, places);
                    } else {
                        if (places.length > 0) {
                            setTimeout(fillElevationData, 1, brand, places);
                        } else {
                            console.log(brand, 'Completed with 0 results');
                        }

                        if (brands.length > 0) {
                            setTimeout(nextBrandSearch, 1, brands.shift().toLowerCase());
                        }
                    }
                }
            } catch (ex) {
                if (ex.message === google.maps.places.PlacesServiceStatus.OVER_QUERY_LIMIT) {
                    setTimeout(nextQuadrantSearch, 10000, brand, places);
                }
            }
        });
    }

    function handleResults(results, status, pagination, places) {
        if (status === google.maps.places.PlacesServiceStatus.OK) {
            for (let i = 0; i < results.length; i++) {
                // createMarker(results[i]);
                places.push(createPlace(results[i]));
            }

            if (pagination && pagination.hasNextPage) {
                pagination.nextPage();
            }
        } else if (status === google.maps.places.PlacesServiceStatus.OVER_QUERY_LIMIT) {
            throw new Error(status);
        } else if (status === google.maps.places.PlacesServiceStatus.ZERO_RESULTS) {
            // ignore
        } else {
            console.log(status.toString());
        }
    }

    function createPlace(result) {
        return {
            name: result.name,
            location: { lat: result.geometry.location.lat(), lng: result.geometry.location.lng() },
            rating: result.rating,
            placeId: result.place_id,
            address: result.vicinity
        }
    }

    function createMarker(place) {
        const marker = new google.maps.Marker({
            map: map,
            position: place.geometry.location,
            title: place.name
        });

        bounds.extend(place.geometry.location);
    }

    function fillElevationData(brand, places) {
        console.log(brand, 'Elevation Data');
        const locations = places.map((retailer) => new google.maps.LatLng(retailer.location.lat, retailer.location.lng));

        elevationService.getElevationForLocations({ locations: locations }, (elevations, status) => {
            if (status === google.maps.ElevationStatus.OK) {
                elevations.forEach((result, index) => {
                    places[index].elevation = result.elevation;
                });
            }

            fillFireStationDistance(brand, places, locations);
        });
    }

    function fillFireStationDistance(brand, places, locations) {
        console.log(brand, 'Fire Station Distance');

        nextFireStationDistance(brand, places, locations, 0);
    }

    function nextFireStationDistance(brand, places, locations, index) {
        const retailerLocation = locations[0];
        const request = {
            location: retailerLocation,
            radius: 6000,
            type: 'fire_station'
        };

        placesService.nearbySearch(request, (fireStations, status) => {
            if (status === google.maps.places.PlacesServiceStatus.OK) {
                locations.shift();

                if (fireStations.length > 0) {
                    places[index].fireStationDist = getNearestFireStation(retailerLocation, fireStations);
                }
            } else if (status === google.maps.places.PlacesServiceStatus.OVER_QUERY_LIMIT) {
                setTimeout(nextFireStationDistance, 10000, brand, places, locations, index);
                return;
            }

            if (locations.length > 0) {
                setTimeout(nextFireStationDistance, 100, brand, places, locations, index + 1);
            } else {
                sendResults(brand, places);
            }
        });
    }

    function sendResults(brand, places) {
        console.log(brand, 'Completed');

        fetch('places', {
            method: 'POST',
            body: JSON.stringify(places),
            headers: {
                'Content-Type': 'application/json',
                'x-brand': brand
            }
        }).catch(error => console.error('Error:', error));
    }

    function getBrands() {
        return fetch('places').then(res => res.json());
    }

    function getQuadrants(searchArea) {
        let x1 = searchArea.west;
        let y1 = searchArea.south;

        let x2 = x1;
        let y2 = y1;

        const gridStep = 0.25;
        const quadrants = [];
        while (x1 < searchArea.east) {
            x2 = x2 + gridStep;
            while (y1 < searchArea.north) {
                y2 = y2 + gridStep;
                quadrants.push(new google.maps.LatLngBounds({ lat: y1, lng: x1 }, { lat: y2, lng: x2 }));
                y1 = y2;
            }
            x1 = x2;
            y1 = y2 = searchArea.south;
        }
        return quadrants;
    }

    function getNearestFireStation(retailerLocation, fireStations) {
        let dist = 1000000000;
        for (let i = 0; i < fireStations.length; i++) {
            const fireStation = fireStations[i];
            const fireStationDist = google.maps.geometry.spherical.computeDistanceBetween(retailerLocation, fireStation.geometry.location);
            dist = Math.min(fireStationDist, dist);
        }
        return dist;
    }
}

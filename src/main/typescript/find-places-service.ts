namespace com.koldyr.places {

    export interface Place {
        placeId: string;
        name: string;
        address: string;
        location: google.maps.LatLngLiteral;
        rating: number;
        elevation?: number;
        fireStationDist?: number;
    }

    export class ProcessContext {

        isRunning: boolean = true;
        places: Array<Place> = [];
        quadrantIndex: number = 0;
        quadrants: Array<google.maps.LatLngBounds>;
        bounds = new google.maps.LatLngBounds();

        status: ResultStatus = ResultStatus.OK;
        resolve: Function;

        constructor(quadrants: Array<google.maps.LatLngBounds>) {
            this.quadrants = quadrants;
        }

        nextQuadrant(): google.maps.LatLngBounds {
            return this.quadrants[this.quadrantIndex++];
        }

        public hasNext(): boolean {
            return this.quadrantIndex < this.quadrants.length;
        }
    }

    export class FindPlacesService {
        private map: google.maps.Map;
        private placesLoader: PlacesLoader;

        private brands: Array<string>;

        private context: ProcessContext;

        constructor(map: google.maps.Map, searchArea: google.maps.LatLngBoundsLiteral) {
            this.map = map;
            this.placesLoader = new PlacesLoader(map);

            this.context = new ProcessContext(this.getQuadrants(searchArea));
        }

        public startProcess(): void {
            this.getBrands().then((data) => {
                this.doPlacesSearch(data);
            });
        }

        public cancel(): void {
            this.context.isRunning = false;
        }

        private doPlacesSearch(data: Array<string>): void {
            this.brands = data;

            this.nextBrandSearch(this.nextBrand());
        }

        private nextBrandSearch(brand: string): void {
            if (!this.context.isRunning) return;

            console.info('Staring', brand);

            const places: Array<Place> = [];

            this.placesLoader.load(brand, this.context).then((context: ProcessContext) => {
                places.unshift(...context.places);
                this.sendResults(brand, places);

                if (this.brands.length > 0) {
                    setTimeout(this.nextBrandSearch.bind(this), 1, this.nextBrand());
                }
            }, (context: ProcessContext) => {
                if (context.status == ResultStatus.REPEAT) {
                    setTimeout(this.nextBrandSearch.bind(this), 7000, brand, places);
                } else {
                    console.error('Error');
                }
            });
        }

        private fillElevationData(brand: string, places: Array<Place>): void {
            console.info(brand, 'Elevation Data');
            const locations = places.map((retailer) => new google.maps.LatLng(retailer.location.lat, retailer.location.lng));

            this.elevationService.getElevationForLocations({locations: locations},
                (elevations: google.maps.ElevationResult[], status: google.maps.ElevationStatus) => {
                    if (status === google.maps.ElevationStatus.OK) {
                        elevations.forEach((result, index) => {
                            places[index].elevation = result.elevation;
                        });
                    }

                    this.fillFireStationDistance(brand, places, locations);
                });
        }

        private fillFireStationDistance(brand: string, places: Array<Place>, locations: Array<google.maps.LatLng>): void {
            console.info(brand, 'Fire Station Distance');

            this.nextFireStationDistance(brand, places, locations, 0);
        }

        private nextFireStationDistance(brand: string, places: Array<Place>, locations: Array<google.maps.LatLng>, fireStationPlaceIndex: number): void {
            if (this.isCanceled) {
                this.sendResults(brand, places);
                return;
            }

            if (fireStationPlaceIndex % 50 === 0) {
                console.debug(brand, fireStationPlaceIndex);
            }

            const retailerLocation: google.maps.LatLng = locations[0];
            const request: google.maps.places.PlaceSearchRequest = {
                location: retailerLocation,
                radius: 6000,
                type: 'fire_station'
            };

            this.placesLoader.nearbySearch(request, (fireStations: google.maps.places.PlaceResult[], status: google.maps.places.PlacesServiceStatus) => {
                try {
                    this.handleFireStationResults(fireStations, status, retailerLocation, places[fireStationPlaceIndex]);

                    locations.shift();

                    if (locations.length > 0) {
                        setTimeout(this.nextFireStationDistance.bind(this), 160, brand, places, locations, fireStationPlaceIndex + 1);
                    } else {
                        this.sendResults(brand, places);

                        if (this.brands.length > 0) {
                            setTimeout(this.nextBrandSearch.bind(this), 1, this.nextBrand());
                        }
                    }
                } catch (ex) {
                    if (ex['status'] === google.maps.places.PlacesServiceStatus.OVER_QUERY_LIMIT ||
                        ex['status'] === google.maps.places.PlacesServiceStatus.UNKNOWN_ERROR) {
                        console.debug('Repeat', brand, fireStationPlaceIndex);

                        setTimeout(this.nextFireStationDistance.bind(this), 3000, brand, places, locations, fireStationPlaceIndex);
                    } else {
                        console.error('handleFireStationResults:', ex);
                    }
                }
            });
        }

        private handleFireStationResults(fireStations: google.maps.places.PlaceResult[], status: google.maps.places.PlacesServiceStatus,
                                         retailerLocation: google.maps.LatLng, place: Place): void {
            if (status === google.maps.places.PlacesServiceStatus.OK) {
                if (fireStations.length > 0) {
                    place.fireStationDist = this.getNearestFireStation(retailerLocation, fireStations);
                }
            } else if (status === google.maps.places.PlacesServiceStatus.OVER_QUERY_LIMIT ||
                status === google.maps.places.PlacesServiceStatus.UNKNOWN_ERROR) {
                const error = new Error();
                error['status'] = status;
                throw error;
            } else if (status === google.maps.places.PlacesServiceStatus.ZERO_RESULTS) {
                // ignore
            } else {
                console.debug(status.toString());
            }
        }

        private sendResults(brand: string, places: Array<Place>): void {
            console.info(brand, 'Completed', places.length);

            fetch('places', {
                method: 'POST',
                body: JSON.stringify(places),
                headers: {
                    'Content-Type': 'application/json',
                    'x-brand': brand
                }
            }).catch(error => console.error('sendResults:', error));
        }

        private getBrands(): Promise<Array<string>> {
            return fetch('places').then(res => res.json());
        }

        private getQuadrants(searchArea: google.maps.LatLngBoundsLiteral): Array<google.maps.LatLngBounds> {
            let x1 = searchArea.west;
            let y1 = searchArea.south;

            let x2 = x1;
            let y2 = y1;

            const gridStep = 0.25;
            const quadrants: Array<google.maps.LatLngBounds> = [];
            while (x1 < searchArea.east) {
                x2 = x2 + gridStep;
                while (y1 < searchArea.north) {
                    y2 = y2 + gridStep;
                    quadrants.push(new google.maps.LatLngBounds({lat: y1, lng: x1}, {lat: y2, lng: x2}));
                    y1 = y2;
                }
                x1 = x2;
                y1 = y2 = searchArea.south;
            }
            return quadrants;
        }

        private getNearestFireStation(retailerLocation: google.maps.LatLng, fireStations: google.maps.places.PlaceResult[]): number {
            let dist = 1000000000;
            for (let i = 0; i < fireStations.length; i++) {
                const fireStation = fireStations[i];
                const fireStationDist = google.maps.geometry.spherical.computeDistanceBetween(retailerLocation, fireStation.geometry.location);
                dist = Math.min(fireStationDist, dist);
            }
            return dist;
        }

        private nextBrand(): string {
            return '"' + this.brands.shift().toLowerCase() + '"';
        }
    }
}

let findPlacesService;

function initMap() {
    const map = new google.maps.Map(document.getElementById('map'), {
        center: {lat: 32, lng: -96},
        zoom: 4
    });

    google.maps.event.addListener(map, 'click', function (event) {
        console.log(event.latLng.toString());
    });

    // const usaMainLand = {south: 27, west: -140, north: 50, east: -50};
    // const colorado = { south: 37, west: -109, north: 41, east: -102 };
    const la: google.maps.LatLngBoundsLiteral = {south: 33, west: -118, north: 35, east: -117};

    findPlacesService = new com.koldyr.places.FindPlacesService(map, la);
}

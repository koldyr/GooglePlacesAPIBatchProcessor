namespace com.koldyr.places {

    export enum ResultStatus {
        ERROR = 'ERROR', REPEAT = 'REPEAT'
    }

    export interface FireStationsData {
        places: Array<Place>;
        status: ResultStatus;
    }

    export class FireStationsLoader {
        private placesService: google.maps.places.PlacesService;

        constructor(map: google.maps.Map) {
            this.placesService = new google.maps.places.PlacesService(map);
        }

        public load(brand: string, places: Array<Place>, locations: Array<google.maps.LatLng>): Promise<FireStationsData> {
            return this.nextFireStationDistance(brand, places, locations, 0);
        }

        private nextFireStationDistance(brand: string, places: Array<Place>, locations: Array<google.maps.LatLng>, index: number): Promise<FireStationsData> {
            if (this.isCanceled) {
                return new Promise<FireStationsData>((resolve: Function) => resolve({places}));
            }

            if (index % 50 === 0) {
                console.debug(brand, index);
            }

            const retailerLocation: google.maps.LatLng = locations[0];
            const request: google.maps.places.PlaceSearchRequest = {
                location: retailerLocation,
                radius: 6000,
                type: 'fire_station'
            };

            this.placesService.nearbySearch(request, (fireStations: google.maps.places.PlaceResult[], status: google.maps.places.PlacesServiceStatus) => {
                try {
                    this.handleFireStationResults(fireStations, status, retailerLocation, places[index]);

                    locations.shift();

                    if (locations.length > 0) {
                        setTimeout(this.nextFireStationDistance.bind(this), 160, brand, places, locations, index + 1);
                    } else {
                        this.sendResults(brand, places);

                        if (this.brands.length > 0) {
                            setTimeout(this.nextBrandSearch.bind(this), 1, this.nextBrand());
                        }
                    }
                } catch (ex) {
                    if (ex['status'] === google.maps.places.PlacesServiceStatus.OVER_QUERY_LIMIT ||
                        ex['status'] === google.maps.places.PlacesServiceStatus.UNKNOWN_ERROR) {
                        console.debug('Repeat', brand, index);

                        setTimeout(this.nextFireStationDistance.bind(this), 3000, brand, places, locations, index);
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
                const error = new Error('');
                error['status'] = status;
                throw error;
            } else if (status === google.maps.places.PlacesServiceStatus.ZERO_RESULTS) {
                // ignore
            } else {
                console.debug(status.toString());
            }
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
    }
}

namespace com.koldyr.places {

    export enum ResultStatus {
        OK = 'OK', ERROR = 'ERROR', REPEAT = 'REPEAT'
    }

    export class FireStationsLoader {
        private placesService: google.maps.places.PlacesService;
        private promise: PromiseFunctions;

        constructor(placesService: google.maps.places.PlacesService) {
            this.placesService = placesService;
        }

        public load(brand: string, context: ProcessContext): Promise<ProcessContext> {
            return new Promise<ProcessContext>((resolve: Function, reject: Function) => {
                this.promise = {resolve, reject};
                this.nextFireStationDistance(brand, context);
            });
        }

        private nextFireStationDistance(brand: string, context: ProcessContext): void {
            if (!context.isRunning) {
                return;
            }

            if (context.locationIndex % 50 === 0) {
                console.debug(brand, context.locationIndex);
            }

            const retailerLocation: google.maps.LatLng = context.nextLocation();
            const request: google.maps.places.PlaceSearchRequest = {
                location: retailerLocation,
                radius: 6000,
                type: 'fire_station'
            };

            this.placesService.nearbySearch(request, (fireStations: google.maps.places.PlaceResult[], status: google.maps.places.PlacesServiceStatus) => {
                try {
                    this.handleFireStationResults(fireStations, status, retailerLocation, context.places[context.locationIndex]);

                    if (context.hasLocation()) {
                        setTimeout(this.nextFireStationDistance.bind(this), 160, brand, context);
                    } else {
                        this.promise.resolve();
                    }
                } catch (ex) {
                    if (ex['status'] === google.maps.places.PlacesServiceStatus.OVER_QUERY_LIMIT ||
                        ex['status'] === google.maps.places.PlacesServiceStatus.UNKNOWN_ERROR) {
                        console.debug('Repeat', brand, context.locationIndex);

                        setTimeout(this.nextFireStationDistance.bind(this), 3000, brand, context);
                    } else {
                        console.error('handleFireStationResults:', ex);
                        this.promise.reject();
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
            let dist = Number.MAX_VALUE;
            for (let i = 0; i < fireStations.length; i++) {
                const fireStation = fireStations[i];
                const fireStationDist = google.maps.geometry.spherical.computeDistanceBetween(retailerLocation, fireStation.geometry.location);
                dist = Math.min(fireStationDist, dist);
            }
            return dist;
        }
    }
}

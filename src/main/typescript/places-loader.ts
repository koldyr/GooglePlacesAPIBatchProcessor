namespace com.koldyr.places {

    export class PlacesLoader {
        private placesService: google.maps.places.PlacesService;

        constructor(map: google.maps.Map) {
            this.placesService = new google.maps.places.PlacesService(map);
        }

        load(request: google.maps.places.PlaceSearchRequest): Promise<Array<Place>> {
            return new Promise<Array<Place>>((resolve: Function, reject: Function) => {
                const places: Array<Place> = [];
                this.placesService.nearbySearch(request,
                    (results: google.maps.places.PlaceResult[], status: google.maps.places.PlacesServiceStatus, pagination: google.maps.places.PlaceSearchPagination) => {
                        try {
                            this.handleSearchResults(results, status, pagination, places);

                            if (!pagination || !pagination.hasNextPage) {
                                resolve(places);
                            }
                        } catch (ex) {
                            if (ex['status'] === google.maps.places.PlacesServiceStatus.OVER_QUERY_LIMIT ||
                                ex['status'] === google.maps.places.PlacesServiceStatus.UNKNOWN_ERROR) {
                                resolve(places);
                            } else {
                                reject();
                            }
                        }
                    });

            });
        }

        private handleSearchResults(results: google.maps.places.PlaceResult[], status: google.maps.places.PlacesServiceStatus,
                                    pagination: google.maps.places.PlaceSearchPagination, places: Array<Place>): void {
            if (status === google.maps.places.PlacesServiceStatus.OK) {
                for (let i = 0; i < results.length; i++) {
                    // createMarker(results[i]);
                    places.push(this.createPlace(results[i]));
                }

                if (pagination && pagination.hasNextPage) {
                    pagination.nextPage();
                }
            } else if (status === google.maps.places.PlacesServiceStatus.OVER_QUERY_LIMIT ||
                status === google.maps.places.PlacesServiceStatus.UNKNOWN_ERROR) {
                const error: Error = new Error();
                error['status'] = status;
                throw error;
            } else if (status === google.maps.places.PlacesServiceStatus.ZERO_RESULTS) {
                // ignore
            } else {
                console.debug(status.toString());
            }
        }

        private createPlace(result: google.maps.places.PlaceResult): Place {
            return {
                name: result.name,
                location: {lat: result.geometry.location.lat(), lng: result.geometry.location.lng()},
                rating: result.rating,
                placeId: result.place_id,
                address: result.vicinity
            };
        }
    }
}

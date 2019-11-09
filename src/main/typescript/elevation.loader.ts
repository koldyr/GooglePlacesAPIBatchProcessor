namespace com.koldyr.places {

    export interface ElevationData {
        places: Array<Place>;
        locations: Array<google.maps.LatLng>;
    }

    export class ElevationLoader {
        private elevationService: google.maps.ElevationService = new google.maps.ElevationService();

        load(places: Array<Place>): Promise<ElevationData> {
            const locations = places.map((retailer) => new google.maps.LatLng(retailer.location.lat, retailer.location.lng));

            return new Promise<ElevationData>((resolve: Function) => {
                this.elevationService.getElevationForLocations({locations: locations},
                    (elevations: google.maps.ElevationResult[], status: google.maps.ElevationStatus) => {
                        if (status === google.maps.ElevationStatus.OK) {
                            elevations.forEach((result, index) => {
                                places[index].elevation = result.elevation;
                            });
                        }

                        resolve({places, locations});
                    });
            });
        }

    }
}

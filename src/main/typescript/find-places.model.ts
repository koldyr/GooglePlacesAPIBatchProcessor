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

    export interface PromiseFunctions {
        resolve: Function;
        reject: Function;
    }

    export interface Loader {
        load(brand: string, context: ProcessContext): Promise<Array<Place>>;
    }

    export enum ResultStatus {
        OK = 'OK', ERROR = 'ERROR', REPEAT = 'REPEAT'
    }

    export class ProcessContext {

        isRunning: boolean = true;
        places: Array<Place> = [];

        quadrantIndex: number = 0;
        quadrants: Array<google.maps.LatLngBounds>;

        locations: Array<google.maps.LatLng>;
        locationIndex: number = -1;

        bounds = new google.maps.LatLngBounds();
        status: ResultStatus = ResultStatus.OK;

        constructor(quadrants: Array<google.maps.LatLngBounds>) {
            this.quadrants = quadrants;
        }

        nextQuadrant(): google.maps.LatLngBounds {
            return this.quadrants[this.quadrantIndex++];
        }

        hasQuadrant(): boolean {
            return this.quadrantIndex < this.quadrants.length;
        }

        nextLocation(): google.maps.LatLng {
            this.locationIndex++;
            return this.locations.shift();
        }

        hasLocation(): boolean {
            return this.locations.length > 0;
        }
    }
}

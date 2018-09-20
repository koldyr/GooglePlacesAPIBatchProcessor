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

            this.placesLoader.load(brand, this.context).then((places: Array<Place>) => {
                this.sendResults(brand, places);

                this.context.quadrantIndex = 0;
                this.context.places = [];

                if (this.brands.length > 0) {
                    setTimeout(this.nextBrandSearch.bind(this), 1, this.nextBrand());
                }
            }, (context: ProcessContext) => {
                if (context.status === ResultStatus.REPEAT) {
                    setTimeout(this.nextBrandSearch.bind(this), 7000, brand);
                } else {
                    console.error('Error');
                }
            });
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

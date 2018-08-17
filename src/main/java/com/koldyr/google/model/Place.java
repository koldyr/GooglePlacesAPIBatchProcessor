package com.koldyr.google.model;

/**
 * Description of class Candidate
 *
 * @created: 2018.08.15
 */
public class Place {

//    @JsonProperty("place_id")
    private String placeId;

    private String name;

//    @JsonProperty("formatted_address")
    private String address;

//    @JsonProperty("geometry")
//    @JsonDeserialize(using = GeometryDeserializer.class)
    private Location location;

    private float rating;

    private float elevation;

    private float fireStationDist;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public float getElevation() {
        return elevation;
    }

    public void setElevation(float elevation) {
        this.elevation = elevation;
    }

    public float getFireStationDist() {
        return fireStationDist;
    }

    public void setFireStationDist(float fireStationDist) {
        this.fireStationDist = fireStationDist;
    }

    @Override
    public String toString() {
        return name;
    }
}

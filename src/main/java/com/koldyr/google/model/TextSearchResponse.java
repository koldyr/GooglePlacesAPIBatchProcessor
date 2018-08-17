package com.koldyr.google.model;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Description of class PlaceResponse
 *
 * @created: 2018.08.15
 */
public class TextSearchResponse {
    private List<Place> results;

    private String status;

    @JsonProperty("next_page_token")
    private String nextPageToken;

    public void setResults(List<Place> results) {
        this.results = results;
    }

    public List<Place> getResults() {
        return results;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }

    public void setNextPageToken(String nextPageToken) {
        this.nextPageToken = nextPageToken;
    }
}

package com.koldyr.google.places;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koldyr.google.model.Place;
import com.koldyr.google.model.TextSearchResponse;

/**
 * Description of class PlacesService
 *
 * @created: 2018.08.15
 */
public class FindPlaceService {
    private static final Logger logger = Logger.getLogger(FindPlaceService.class);

    private final String serviceUrl;

    private final String nextPageUrl;

    private final CloseableHttpClient httpClient;

    private final ObjectMapper objectMapper;

    public FindPlaceService(CloseableHttpClient httpClient, String apiKey) {
        this.httpClient = httpClient;

        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        serviceUrl = "https://maps.googleapis.com/maps/api/place/textsearch/json?key=" + apiKey + "&query=%s";
        nextPageUrl = "https://maps.googleapis.com/maps/api/place/textsearch/json?key=" + apiKey + "&pagetoken=%s";
    }

    public List<Place> find(String name) {
        String input;
        try {
            input = name.trim().toLowerCase() + " stores in usa";
            input = URLEncoder.encode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        final String url = String.format(serviceUrl, input);
        final HttpUriRequest request = RequestBuilder.get(url).build();

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final TextSearchResponse placeResponse = objectMapper.readValue(response.getEntity().getContent(), TextSearchResponse.class);
            response.close();

            final List<Place> places = placeResponse.getResults();
            final String nextPageToken = placeResponse.getNextPageToken();

            if (nextPageToken != null) {
                loadNextPage(nextPageToken, places);
            }
            logger.debug(name + ": " + places.size());

            return places;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    private void loadNextPage(String nextPageToken, List<Place> places) throws InterruptedException {
        Thread.sleep(5000);

        final String url = String.format(nextPageUrl, nextPageToken);
        final HttpUriRequest request = RequestBuilder.get(url).build();

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            TextSearchResponse placeResponse = objectMapper.readValue(response.getEntity().getContent(), TextSearchResponse.class);
            response.close();

            places.addAll(placeResponse.getResults());
            String nextToken = placeResponse.getNextPageToken();

            if (nextToken != null) {
                loadNextPage(nextToken, places);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

}

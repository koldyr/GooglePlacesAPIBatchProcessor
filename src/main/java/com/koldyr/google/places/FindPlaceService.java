package com.koldyr.google.places;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koldyr.google.model.Place;
import com.koldyr.google.model.TextSearchResponse;

import static java.nio.charset.StandardCharsets.*;
import static java.util.Objects.*;

/**
 * Description of class PlacesService
 *
 * @created: 2018.08.15
 */
public class FindPlaceService {
    private static final Logger logger = LogManager.getLogger(FindPlaceService.class);

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
        var input = name.trim().toLowerCase() + " stores in usa";
        input = URLEncoder.encode(input, UTF_8);

        var url = String.format(serviceUrl, input);
        var request = RequestBuilder.get(url).build();

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            var placeResponse = objectMapper.readValue(response.getEntity().getContent(), TextSearchResponse.class);
            response.close();

            var places = placeResponse.getResults();
            var nextPageToken = placeResponse.getNextPageToken();

            if (nonNull(nextPageToken)) {
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

        var url = String.format(nextPageUrl, nextPageToken);
        var request = RequestBuilder.get(url).build();

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            var placeResponse = objectMapper.readValue(response.getEntity().getContent(), TextSearchResponse.class);
            response.close();

            places.addAll(placeResponse.getResults());
            var nextToken = placeResponse.getNextPageToken();

            if (nonNull(nextToken)) {
                loadNextPage(nextToken, places);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

}

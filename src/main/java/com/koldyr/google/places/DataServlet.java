package com.koldyr.google.places;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.koldyr.google.model.Place;

/**
 * Description of class RetailersServlet
 *
 * @created: 2018.08.16
 */
@WebServlet("/places/*")
public class DataServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(DataServlet.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final CollectionType retailersCollection;

    public DataServlet() {
        super();

        retailersCollection = objectMapper.getTypeFactory().constructCollectionType(LinkedList.class, Place.class);
    }

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        final InputStream input = getClass().getResourceAsStream("/input_all.txt");
        final List<String> brandNames = BatchProcessor.loadInputData(input);

        objectMapper.writeValue(httpServletResponse.getOutputStream(), brandNames);

        httpServletResponse.flushBuffer();
    }

    @Override
    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        try {
            final String brand = httpServletRequest.getHeader("x-brand");
            final List<Place> result = objectMapper.readValue(httpServletRequest.getInputStream(), retailersCollection);

            objectMapper.writeValue(new File("C:/Projects/PlacesBatchProcessor/los-angeles/" + brand + ".json"), result);

            logger.debug('"' + brand + "\" completed " + result.size());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}

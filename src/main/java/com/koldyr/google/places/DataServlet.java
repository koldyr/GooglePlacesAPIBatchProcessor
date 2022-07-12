package com.koldyr.google.places;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.koldyr.google.model.Place;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * Description of class DataServlet
 *
 * @created: 2018.08.16
 */
@WebServlet(value = "/places/*", initParams = {
        @WebInitParam(name = "input", value = ""),
        @WebInitParam(name = "output", value = "./encoded/")
})
public class DataServlet extends HttpServlet {

    private static final Logger logger = LogManager.getLogger(DataServlet.class);

    private static final Pattern PATTERN_FILE_NAME = Pattern.compile("[\\[!\\\"#$%&'()*+,/:;<=>?@\\\\^`{|}~]");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final CollectionType placesType = objectMapper.getTypeFactory().constructCollectionType(LinkedList.class, Place.class);

    private String input;

    private String output;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        input = config.getInitParameter("input");
        if (isEmpty(input)) {
            input = "/input_all.txt";
        }
        output = config.getInitParameter("output");
        if (isEmpty(output)) {
            output = "/";
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var inputStream = getClass().getResourceAsStream(input);
        var brandNames = BatchProcessor.loadInputData(inputStream);

        objectMapper.writeValue(response.getOutputStream(), brandNames);

        response.flushBuffer();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            var brand = request.getHeader("x-brand");
            if (isEmpty(brand)) {
                throw new IllegalArgumentException("x-brand");
            }

            final List<Place> result = objectMapper.readValue(request.getInputStream(), placesType);

            var resultFile = new File(output + PATTERN_FILE_NAME.matcher(brand).replaceAll(EMPTY) + ".json");

            if (resultFile.getParentFile().exists()) {
                objectMapper.writeValue(resultFile, result);
            } else {
                if (resultFile.getParentFile().mkdirs()) {
                    objectMapper.writeValue(resultFile, result);
                }
            }

            logger.debug(brand + " completed " + result.size());
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing value for " + e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}

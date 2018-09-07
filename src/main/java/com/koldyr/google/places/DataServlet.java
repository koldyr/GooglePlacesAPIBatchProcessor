package com.koldyr.google.places;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.koldyr.google.model.Place;

/**
 * Description of class DataServlet
 *
 * @created: 2018.08.16
 */
@WebServlet(value = "/places/*", initParams = {
        @WebInitParam(name = "input", value = ""),
        @WebInitParam(name = "output", value = "C:/Projects/PlacesBatchProcessor/los-angeles/")
})
public class DataServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(DataServlet.class);

    private static final Pattern PATTERN_FILE_NAME = Pattern.compile("[\\[!\\\"#$%&'()*+,/:;<=>?@\\\\^`{|}~]");

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final CollectionType placesType = objectMapper.getTypeFactory().constructCollectionType(LinkedList.class, Place.class);

    private String input;

    private String output;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        input = config.getInitParameter("input");
        if (StringUtils.isEmpty(input)) {
            input = "/input_all.txt";
        }
        output = config.getInitParameter("output");
        if (StringUtils.isEmpty(output)) {
            output = "/";
        }
    }

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        final InputStream inputStream = getClass().getResourceAsStream(input);
        final List<String> brandNames = BatchProcessor.loadInputData(inputStream);

        objectMapper.writeValue(httpServletResponse.getOutputStream(), brandNames);

        httpServletResponse.flushBuffer();
    }

    @Override
    protected void doPost(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        try {
            final String brand = httpServletRequest.getHeader("x-brand");
            if (StringUtils.isEmpty(brand)) {
                throw new IllegalArgumentException("x-brand");
            }

            final List<Place> result = objectMapper.readValue(httpServletRequest.getInputStream(), placesType);

            final File resultFile = new File(output + PATTERN_FILE_NAME.matcher(brand).replaceAll(StringUtils.EMPTY) + ".json");
            if (resultFile.getParentFile().mkdirs()) {
                objectMapper.writeValue(resultFile, result);
            }

            logger.debug(brand + " completed " + result.size());
        } catch (IllegalArgumentException e) {
            httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing value for " + e.getMessage());
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            httpServletResponse.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}

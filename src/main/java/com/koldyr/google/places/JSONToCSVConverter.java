package com.koldyr.google.places;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.koldyr.google.model.Place;

/**
 * Description of class JSONToCSVConverter
 *
 * @created: 2018.08.17
 */
public class JSONToCSVConverter {
    private static final Logger logger = Logger.getLogger(JSONToCSVConverter.class);

    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();
        CollectionType placesCollection = objectMapper.getTypeFactory().constructCollectionType(LinkedList.class, Place.class);

        File inputDir = new File(args[0]);
        File[] files = inputDir.listFiles((dir, name) -> name.endsWith(".json"));
//        writeFilePerBrand(objectMapper, placesCollection, files);
        writeSingle(objectMapper, placesCollection, inputDir, files);
    }

    private static void writeFilePerBrand(ObjectMapper objectMapper, CollectionType placesCollection, File[] files) {
        for (File jsonFile : files) {
            try {
                String brand = getBrand(jsonFile);
                List<Place> result = objectMapper.readValue(jsonFile, placesCollection);
                writeCSVFile(brand, jsonFile, result);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private static void writeSingle(ObjectMapper objectMapper, CollectionType placesCollection, File parentDir, File[] files) {
        final File outputName = new File(parentDir, parentDir.getName() + ".csv");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputName)))) {
            writer.write(getFileHeader());

            for (File jsonFile : files) {
                try {
                    List<Place> result = objectMapper.readValue(jsonFile, placesCollection);
                    writeCSVFile(writer, result);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static void writeCSVFile(String brand, File jsonFile, Iterable<Place> places) {
        logger.debug(brand);

        final File outputName = new File(jsonFile.getParentFile(), brand + ".csv");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputName)))) {
            writer.write(getFileHeader());
            for (Place place : places) {
                writer.write(getPlaceLine(place));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static void writeCSVFile(BufferedWriter writer, Iterable<Place> places) throws IOException {
        for (Place place : places) {
            writer.write(getPlaceLine(place));
        }
    }

    private static String getFileHeader() {
        return "placeId,name,address,location,elevation,fireStationDist,rating\n";
    }

    private static String getPlaceLine(Place place) {
        StringBuilder joiner = new StringBuilder();
        joiner.append(place.getPlaceId()).append(',');
        joiner.append(StringEscapeUtils.escapeCsv(place.getName())).append(',');
        joiner.append(StringEscapeUtils.escapeCsv(place.getAddress())).append(',');
        joiner.append('"').append(place.getLocation().getLat()).append(',').append(place.getLocation().getLng()).append("\",");
        joiner.append(place.getElevation()).append(',');
        joiner.append(place.getFireStationDist()).append(',');
        joiner.append(place.getRating()).append('\n');
        return joiner.toString();
    }

    private static String getBrand(File jsonFile) {
        String name = jsonFile.getName();
        int index = name.lastIndexOf('.');
        return name.substring(0, index);
    }
}

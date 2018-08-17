package com.koldyr.google.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.koldyr.google.model.Location;

/**
 * Description of class GeometryDeserializer
 *
 * @created: 2018.08.15
 */
public class GeometryDeserializer extends JsonDeserializer<Location> {

    @Override
    public Location deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
        final ObjectCodec codec = parser.getCodec();
        final JsonNode node = codec.readTree(parser);
        final JsonNode locationNode = node.get("location");

        double lat = locationNode.get("lat").asDouble();
        double lng = locationNode.get("lng").asDouble();
        return new Location(lat, lng);
    }
}

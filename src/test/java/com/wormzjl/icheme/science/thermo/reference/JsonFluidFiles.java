package com.wormzjl.icheme.science.thermo.reference;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** Reads the CoolProp fluid files and parity fixtures bundled under {@code src/test/resources/science/thermo/coolprop}. */
final class JsonFluidFiles {
    static final String RESOURCE_DIRECTORY = "/science/thermo/coolprop/";

    private JsonFluidFiles() {
    }

    static JsonObject resource(String fileName) {
        String path = RESOURCE_DIRECTORY + fileName;
        try (InputStream stream = JsonFluidFiles.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("missing test resource " + path);
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + path, e);
        }
    }

    static double[] doubles(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException("expected an array under " + key);
        }
        JsonArray array = element.getAsJsonArray();
        double[] values = new double[array.size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsDouble();
        }
        return values;
    }

    static double number(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            throw new IllegalArgumentException("expected a number under " + key);
        }
        double value = element.getAsDouble();
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("non-finite value under " + key);
        }
        return value;
    }
}

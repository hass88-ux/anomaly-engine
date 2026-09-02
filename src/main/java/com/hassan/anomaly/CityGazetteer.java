package com.hassan.anomaly;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CityGazetteer {

    private static final Logger log = LoggerFactory.getLogger(CityGazetteer.class);
    private static final String RESOURCE = "/data/canada-cities.csv";

    private final List<Cities.City> cities;
    private final boolean loadedFromFile;

    public CityGazetteer() {
        List<Cities.City> loaded = load();
        this.loadedFromFile = !loaded.isEmpty();
        this.cities = loadedFromFile ? List.copyOf(loaded) : Cities.ALL;

        if (loadedFromFile) {
            log.info("Loaded {} cities from {}", cities.size(), RESOURCE);
        } else {
            log.warn("{} not found - falling back to {} built-in cities",
                    RESOURCE, cities.size());
        }
    }

    public int size() {
        return cities.size();
    }

    public boolean isFullGazetteer() {
        return loadedFromFile;
    }

    public Cities.City nearest(double lat, double lon) {
        double scale = Math.cos(Math.toRadians(lat));

        Cities.City best = cities.get(0);
        double bestDistance = Double.MAX_VALUE;

        for (Cities.City city : cities) {
            double dLat = city.lat() - lat;
            double dLon = (city.lon() - lon) * scale;
            double distance = dLat * dLat + dLon * dLon;

            if (distance < bestDistance) {
                bestDistance = distance;
                best = city;
            }
        }

        return best;
    }

    private List<Cities.City> load() {
        List<Cities.City> out = new ArrayList<>();

        try (InputStream in = CityGazetteer.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                return out;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {

                reader.readLine();

                String line;
                int lineNumber = 1;

                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank()) {
                        continue;
                    }

                    List<String> fields = splitCsv(line);
                    if (fields.size() < 4) {
                        log.warn("Skipping malformed line {} in {}", lineNumber, RESOURCE);
                        continue;
                    }

                    try {
                        out.add(new Cities.City(
                                fields.get(0),
                                fields.get(1),
                                Double.parseDouble(fields.get(2)),
                                Double.parseDouble(fields.get(3))));
                    } catch (NumberFormatException e) {
                        log.warn("Skipping line {} with unparseable coordinates", lineNumber);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Could not read {}", RESOURCE, e);
            return List.of();
        }

        return out;
    }

    private static List<String> splitCsv(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        fields.add(current.toString());
        return fields;
    }
}
package com.hassan.anomaly;

import java.util.List;

public final class Cities {

    private Cities() {}

    public record City(String name, String province, double lat, double lon) {}

    public static final List<City> ALL = List.of(
            new City("Toronto",   "ON", 43.65, -79.38),
            new City("Vancouver", "BC", 49.28, -123.12),
            new City("Montreal",  "QC", 45.50, -73.57),
            new City("Calgary",   "AB", 51.05, -114.07),
            new City("Halifax",   "NS", 44.65, -63.58));
}
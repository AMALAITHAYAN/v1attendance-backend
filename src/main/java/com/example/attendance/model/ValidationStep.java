package com.example.attendance.model;

import java.util.*;
import java.util.stream.Collectors;

public enum ValidationStep {
    WIFI, GEO, FACE, QR;

    public static List<ValidationStep> fromStrings(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return defaultFlow();
        }
        List<ValidationStep> out = new ArrayList<>();
        for (String s : raw) {
            if (s == null) continue;
            out.add(ValidationStep.valueOf(s.trim().toUpperCase(Locale.ROOT)));
        }
        return out;
    }

    public static String toCsv(List<ValidationStep> steps) {
        if (steps == null || steps.isEmpty()) steps = defaultFlow();
        return steps.stream().map(Enum::name).collect(Collectors.joining(","));
    }

    public static List<ValidationStep> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) return defaultFlow();
        String[] parts = csv.split(",");
        List<ValidationStep> list = new ArrayList<>();
        for (String p : parts) {
            if (!p.isBlank()) list.add(ValidationStep.valueOf(p.trim().toUpperCase(Locale.ROOT)));
        }
        return list.isEmpty() ? defaultFlow() : list;
    }

    public static List<ValidationStep> defaultFlow() {
        // Matches your preferred order if smart board fails
        return List.of(WIFI, GEO, FACE, QR);
    }
}

package com.mysite.knitly.global.util;

public class FileNameUtils {
    public static String sanitize(String input) {
        if (input == null || input.isBlank()) return "design";
        String s = input.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", " ");
        if (!s.toLowerCase().endsWith(".pdf")) s += ".pdf";
        return s.length() > 80 ? s.substring(0, 80) : s;
    }
}

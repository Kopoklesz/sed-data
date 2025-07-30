package com.employeemanager.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class DateUtil {
    public static final DateTimeFormatter STANDARD_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String formatDate(LocalDate date) {
        return date != null ? date.format(STANDARD_DATE_FORMATTER) : null;
    }

    public static Optional<LocalDate> parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(LocalDate.parse(dateStr, STANDARD_DATE_FORMATTER));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static LocalDate safeParseDate(String dateStr, LocalDate currentDate) {
        return parseDate(dateStr).orElse(currentDate);
    }
}
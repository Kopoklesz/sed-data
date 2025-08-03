package com.employeemanager.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Központosított dátum konverter Firebase adatbázishoz
 * Egységes dátum formátumot biztosít az alkalmazás minden részében
 */
public class FirebaseDateConverter {

    // Egységes dátum formátum az egész alkalmazásban
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * LocalDate konvertálása String-re Firebase tároláshoz
     */
    public static String dateToString(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : null;
    }

    /**
     * String konvertálása LocalDate-re Firebase-ből való olvasáskor
     */
    public static LocalDate stringToDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            // Fallback más formátumokra
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }

    /**
     * LocalDateTime konvertálása String-re Firebase tároláshoz
     */
    public static String dateTimeToString(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : null;
    }

    /**
     * String konvertálása LocalDateTime-ra Firebase-ből való olvasáskor
     */
    public static LocalDateTime stringToDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDateTime.parse(dateTimeStr, DATETIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * LocalTime konvertálása String-re Firebase tároláshoz
     */
    public static String timeToString(LocalTime time) {
        return time != null ? time.format(TIME_FORMATTER) : null;
    }

    /**
     * String konvertálása LocalTime-ra Firebase-ből való olvasáskor
     */
    public static LocalTime stringToTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalTime.parse(timeStr, TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Ellenőrzi, hogy érvényes dátum string-e
     */
    public static boolean isValidDateString(String dateStr) {
        return stringToDate(dateStr) != null;
    }

    /**
     * Ellenőrzi, hogy érvényes datetime string-e
     */
    public static boolean isValidDateTimeString(String dateTimeStr) {
        return stringToDateTime(dateTimeStr) != null;
    }

    /**
     * Ellenőrzi, hogy érvényes time string-e
     */
    public static boolean isValidTimeString(String timeStr) {
        return stringToTime(timeStr) != null;
    }
}
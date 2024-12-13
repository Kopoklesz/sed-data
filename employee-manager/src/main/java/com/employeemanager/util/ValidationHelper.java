package com.employeemanager.util;

import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public class ValidationHelper {

    private static final Pattern TAX_NUMBER_PATTERN = Pattern.compile("\\d{10}");
    private static final Pattern SOCIAL_SECURITY_PATTERN = Pattern.compile("\\d{9}");
    private static final Pattern EBEV_SERIAL_PATTERN = Pattern.compile("[A-Z0-9]{10}");

    public static boolean isValidTaxNumber(String taxNumber) {
        // Null check hozzáadása
        if (taxNumber == null || taxNumber.trim().isEmpty()) {
            return false;
        }
        return TAX_NUMBER_PATTERN.matcher(taxNumber).matches();
    }

    public static boolean isValidSocialSecurityNumber(String ssn) {
        return SOCIAL_SECURITY_PATTERN.matcher(ssn).matches();
    }

    public static boolean isValidEbevSerial(String serial) {
        return EBEV_SERIAL_PATTERN.matcher(serial).matches();
    }

    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty() && name.length() >= 2;
    }

    public static boolean isValidBirthDate(LocalDate birthDate) {
        return birthDate != null &&
                birthDate.isBefore(LocalDate.now()) &&
                birthDate.isAfter(LocalDate.now().minusYears(100));
    }

    public static boolean isValidWorkHours(int hours) {
        return hours > 0 && hours <= 24;
    }

    public static boolean isValidPayment(double payment) {
        return payment > 0;
    }

    public static void setNumberOnlyListener(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                textField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    public static void setUpperCaseListener(TextField textField) {
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            textField.setText(newValue.toUpperCase());
        });
    }

    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : "";
    }

    public static LocalDate parseDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static boolean isValidWorkDate(LocalDate workDate) {
        return workDate != null && !workDate.isAfter(LocalDate.now());
    }

    public static boolean isValidNotificationDate(LocalDate notificationDate, LocalDate workDate) {
        return notificationDate != null &&
                !notificationDate.isAfter(LocalDate.now()) &&
                (workDate == null || !notificationDate.isAfter(workDate));
    }
}
package com.zero.filetosql.utils;

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

@Slf4j
public class DateUtils {

    // Formatos para LocalDate y LocalDateTime
    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("M/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd MMMM yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy"),
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss zzz")
    };

    private static boolean isValidDate(String value, DateTimeFormatter formatter) {
        try {
            LocalDate.parse(value, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static boolean isValidTimestamp(String value, DateTimeFormatter formatter) {
        try {
            LocalDateTime.parse(value, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public static boolean isDate(String value) {
        return Arrays.stream(DATE_FORMATTERS)
                .anyMatch(formatter -> isValidDate(value, formatter) || isValidTimestamp(value, formatter));
    }

    public static boolean isSimpleDate(String value) {
        return Arrays.stream(DATE_FORMATTERS)
                .anyMatch(formatter -> isValidDate(value, formatter));
    }

    public static boolean isTimestamp(String value) {
        return Arrays.stream(DATE_FORMATTERS)
                .anyMatch(formatter -> isValidTimestamp(value, formatter));
    }

    public static String formatDate(String value) {
        return Arrays.stream(DATE_FORMATTERS)
                .filter(formatter -> isValidDate(value, formatter))
                .findFirst()
                .map(formatter -> LocalDate.parse(value, formatter).toString())
                .orElse(value);
    }

    public static String formatTimestamp(String value) {
        return Arrays.stream(DATE_FORMATTERS)
                .filter(formatter -> isValidTimestamp(value, formatter))
                .findFirst()
                .map(formatter -> LocalDateTime.parse(value, formatter).toString())
                .orElse(value);
    }
}



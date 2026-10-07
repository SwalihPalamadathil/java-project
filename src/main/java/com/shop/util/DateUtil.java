package com.shop.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Date and time formatting and manipulation utility.
 */
public final class DateUtil {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DISPLAY_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    public static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    public static final DateTimeFormatter INVOICE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private DateUtil() {}

    public static String nowString() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    public static String todayString() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    public static String currentMonthYear() {
        return YearMonth.now().format(MONTH_YEAR_FORMATTER);
    }

    public static String formatDisplayDate(String yyyyMmDd) {
        if (yyyyMmDd == null || yyyyMmDd.trim().isEmpty()) return "-";
        try {
            LocalDate date = LocalDate.parse(yyyyMmDd.trim(), DATE_FORMATTER);
            return date.format(DISPLAY_DATE_FORMATTER);
        } catch (Exception e) {
            return yyyyMmDd;
        }
    }

    public static String formatDisplayDateTime(LocalDateTime ldt) {
        if (ldt == null) return "-";
        return ldt.format(DISPLAY_DATE_TIME_FORMATTER);
    }

    public static String formatDisplayDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) return "-";
        try {
            // Support SQLite default format "yyyy-MM-dd HH:mm:ss"
            LocalDateTime ldt = LocalDateTime.parse(dateTimeStr.replace(" ", "T"));
            return ldt.format(DISPLAY_DATE_TIME_FORMATTER);
        } catch (Exception e) {
            return dateTimeStr;
        }
    }
}

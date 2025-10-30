package tmmsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, java.time.LocalDateTime.class, WebMvcConfig::parseFlexibleLocalDateTime);
        registry.addConverter(String.class, java.time.LocalDate.class, WebMvcConfig::parseFlexibleLocalDate);
        registry.addConverter(String.class, java.time.Instant.class, WebMvcConfig::parseFlexibleInstant);
    }

    private static LocalDateTime parseFlexibleLocalDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();

        // Numeric epoch (seconds or millis)
        if (trimmed.matches("^-?\\d{10,13}$")) {
            long epoch = Long.parseLong(trimmed);
            Instant instant = epoch < 1_000_000_000_000L ? Instant.ofEpochSecond(epoch) : Instant.ofEpochMilli(epoch);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        }

        // ISO_INSTANT or ISO_OFFSET_DATE_TIME (e.g., with trailing 'Z' or offsets)
        try {
            Instant instant = Instant.parse(trimmed);
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        } catch (DateTimeParseException ignored) {
        }
        try {
            OffsetDateTime odt = OffsetDateTime.parse(trimmed);
            return odt.toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }

        // Try a set of common patterns
        for (DateTimeFormatter f : commonDateTimeFormatters()) {
            try {
                return LocalDateTime.parse(normalize(trimmed), f);
            } catch (DateTimeParseException ignored) {
            }
        }

        // Fallback: treat as LocalDate with start of day
        LocalDate date = parseFlexibleLocalDate(trimmed);
        if (date != null) {
            return date.atStartOfDay();
        }
        throw new IllegalArgumentException("Invalid date-time format: " + value);
    }

    private static LocalDate parseFlexibleLocalDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();

        for (DateTimeFormatter f : commonDateFormatters()) {
            try {
                return LocalDate.parse(normalize(trimmed), f);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private static Instant parseFlexibleInstant(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String trimmed = value.trim();
        // Numeric epoch
        if (trimmed.matches("^-?\\d{10,13}$")) {
            long epoch = Long.parseLong(trimmed);
            return epoch < 1_000_000_000_000L ? Instant.ofEpochSecond(epoch) : Instant.ofEpochMilli(epoch);
        }
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
        }
        try {
            OffsetDateTime odt = OffsetDateTime.parse(trimmed);
            return odt.toInstant();
        } catch (DateTimeParseException ignored) {
        }

        // Try date-time patterns as LocalDateTime in system zone
        for (DateTimeFormatter f : commonDateTimeFormatters()) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(normalize(trimmed), f);
                return ldt.atZone(ZoneId.systemDefault()).toInstant();
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private static String normalize(String input) {
        // Replace space between date and time with 'T' if needed
        return input.replace(' ', 'T');
    }

    private static List<DateTimeFormatter> commonDateTimeFormatters() {
        List<DateTimeFormatter> list = new ArrayList<>();
        // yyyy-MM-dd'T'HH:mm:ss[.SSS]
        list.add(new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral('T')
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .optionalStart()
                .appendFraction(ChronoField.MILLI_OF_SECOND, 1, 3, true)
                .optionalEnd()
                .optionalEnd()
                .toFormatter());

        // yyyy-MM-dd HH:mm[:ss][.SSS]
        list.add(new DateTimeFormatterBuilder()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .appendLiteral(' ')
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .optionalStart()
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .optionalStart()
                .appendFraction(ChronoField.MILLI_OF_SECOND, 1, 3, true)
                .optionalEnd()
                .optionalEnd()
                .toFormatter());

        // dd/MM/yyyy HH:mm[:ss]
        list.add(new DateTimeFormatterBuilder()
                .appendPattern("dd/MM/yyyy HH:mm")
                .optionalStart()
                .appendPattern(":ss")
                .optionalEnd()
                .toFormatter());

        // MM/dd/yyyy HH:mm[:ss]
        list.add(new DateTimeFormatterBuilder()
                .appendPattern("MM/dd/yyyy HH:mm")
                .optionalStart()
                .appendPattern(":ss")
                .optionalEnd()
                .toFormatter());

        return list;
    }

    private static List<DateTimeFormatter> commonDateFormatters() {
        List<DateTimeFormatter> list = new ArrayList<>();
        list.add(DateTimeFormatter.ISO_LOCAL_DATE);                // yyyy-MM-dd
        list.add(DateTimeFormatter.ofPattern("dd/MM/yyyy"));      // 30/10/2025
        list.add(DateTimeFormatter.ofPattern("MM/dd/yyyy"));      // 10/30/2025
        return list;
    }
}



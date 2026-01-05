package com.talentica.paymentgateway.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Common date/time utility methods for MapStruct mappers
 */
@Mapper(componentModel = "spring")
public interface DateTimeMapper {
    
    DateTimeMapper INSTANCE = Mappers.getMapper(DateTimeMapper.class);
    
    /**
     * Convert LocalDateTime to ZonedDateTime using system default timezone
     */
    default ZonedDateTime map(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.atZone(ZoneId.systemDefault());
    }
    
    /**
     * Convert ZonedDateTime to LocalDateTime
     */
    default LocalDateTime map(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return zonedDateTime.toLocalDateTime();
    }
    
    /**
     * Format LocalDateTime to string
     */
    @Named("formatDateTime")
    default String formatDateTime(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    /**
     * Format ZonedDateTime to string
     */
    @Named("formatZonedDateTime")
    default String formatZonedDateTime(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return zonedDateTime.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }
    
    /**
     * Parse string to LocalDateTime
     */
    @Named("parseDateTime")
    default LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
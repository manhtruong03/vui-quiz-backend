// src/main/java/com/vuiquiz/quizwebsocket/utils/DateTimeUtil.java
package com.vuiquiz.quizwebsocket.utils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public class DateTimeUtil {

    private DateTimeUtil() {
        // Private constructor to prevent instantiation
    }

    public static OffsetDateTime fromMillis(Long millis) {
        if (millis == null) {
            return null;
        }
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneOffset.UTC);
    }
}
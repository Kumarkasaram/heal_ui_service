package com.heal.dashboard.service.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jitendra Kumar : 17/1/19
 */
public class StringUtils {
    private static final Logger logger = LoggerFactory.getLogger(StringUtils.class);
    public static boolean isEmpty(String s) {
        return (s == null || s.trim().length() == 0);
    }

    public static long getLong(String number) {
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static boolean isNumber(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static Double getDouble(String number) {
        try {
            return Double.parseDouble(number);
        } catch (NumberFormatException e) {
            logger.error("Error while parsing number : {}\n", number, e);
            return null;
        }
    }
}

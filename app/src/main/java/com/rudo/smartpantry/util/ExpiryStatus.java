package com.rudo.smartpantry.util;

import com.rudo.smartpantry.model.PantryItem;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Describes how close a pantry item is to its expiry date.
 * Comparison is done at day granularity rather than by raw milliseconds, so
 * an item expiring later today is treated as expiring today regardless of the
 * time the app happens to be opened.
 */
public enum ExpiryStatus {

    /** No expiry date recorded, or the date is beyond the warning window. */
    NONE,

    /** The expiry date falls within the user's chosen warning window. */
    EXPIRING_SOON,

    /** The expiry date has already passed. */
    EXPIRED;

    /**
     * Works out the status of one item.
     * @param item       the pantry item to assess
     * @param windowDays how many days ahead the user wants to be warned
     */
    public static ExpiryStatus of(PantryItem item, int windowDays) {
        if (item == null || !item.hasExpiryDate()) {
            return NONE;
        }

        long daysRemaining = daysUntil(item.getExpiryDate());

        if (daysRemaining < 0) {
            return EXPIRED;
        }
        if (daysRemaining <= windowDays) {
            return EXPIRING_SOON;
        }
        return NONE;
    }

    /**
     * Whole days between today and the given date, negative when in the past.
     * Both dates are reduced to midnight first so that the result counts
     * calendar days rather than elapsed hours.
     */
    public static long daysUntil(long expiryDate) {
        long today = startOfDay(System.currentTimeMillis());
        long target = startOfDay(expiryDate);
        return TimeUnit.MILLISECONDS.toDays(target - today);
    }

    private static long startOfDay(long millis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(millis);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
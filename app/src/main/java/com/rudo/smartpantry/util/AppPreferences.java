package com.rudo.smartpantry.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Reads and writes the user's preferences.
 *
 * SharedPreferences stores small key and value pairs in a private XML file on
 * the device, which suits settings far better than a database table. Access is
 * centralised here so that the key names exist in exactly one place and a typo
 * in one screen cannot silently read a different setting than another writes.
 */
public final class AppPreferences {

    private static final String PREFS_NAME = "smart_pantry_prefs";

    private static final String KEY_SHOW_ALMOST_THERE = "show_almost_there";
    private static final String KEY_EXPIRY_ALERTS = "expiry_alerts";
    private static final String KEY_EXPIRY_WINDOW_DAYS = "expiry_window_days";

    private static final boolean DEFAULT_SHOW_ALMOST_THERE = true;
    private static final boolean DEFAULT_EXPIRY_ALERTS = true;
    private static final int DEFAULT_EXPIRY_WINDOW_DAYS = 7;

    private AppPreferences() {
        // Utility class, never instantiated.
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isShowAlmostThere(Context context) {
        return prefs(context).getBoolean(KEY_SHOW_ALMOST_THERE, DEFAULT_SHOW_ALMOST_THERE);
    }

    public static void setShowAlmostThere(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_SHOW_ALMOST_THERE, enabled).apply();
    }

    public static boolean isExpiryAlertsEnabled(Context context) {
        return prefs(context).getBoolean(KEY_EXPIRY_ALERTS, DEFAULT_EXPIRY_ALERTS);
    }

    public static void setExpiryAlertsEnabled(Context context, boolean enabled) {
        prefs(context).edit().putBoolean(KEY_EXPIRY_ALERTS, enabled).apply();
    }

    public static int getExpiryWindowDays(Context context) {
        return prefs(context).getInt(KEY_EXPIRY_WINDOW_DAYS, DEFAULT_EXPIRY_WINDOW_DAYS);
    }

    public static void setExpiryWindowDays(Context context, int days) {
        prefs(context).edit().putInt(KEY_EXPIRY_WINDOW_DAYS, days).apply();
    }
}
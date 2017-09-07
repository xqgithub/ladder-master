package com.base.common.util;

import android.util.Log;


public class LogUtils {

    private static final String LOG_PREFIX = "thomas_";
    private static final int LOG_PREFIX_LENGTH = LOG_PREFIX.length();
    private static final int MAX_LOG_TAG_LENGTH = 23;
    public static boolean DEBUG = true;

    private LogUtils() {
    }

    public static String makeLogTag(String str) {
        if (str.length() > MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH) {
            return LOG_PREFIX + str.substring(0, MAX_LOG_TAG_LENGTH - LOG_PREFIX_LENGTH - 1);
        }

        return LOG_PREFIX + str;
    }

    /**
     * Don't use this when obfuscating class names!
     */
    public static String makeLogTag(Class cls) {
        return makeLogTag(cls.getSimpleName());
    }

    public static void d(final String tag, String message) {
        if (DEBUG) Log.d(tag, message);
    }

    public static void v(final String tag, String message) {
        if (DEBUG) Log.v(tag, message);
    }

    public static void i(final String tag, String message) {
        if (DEBUG) Log.i(tag, message);
    }

    public static void w(final String tag, String message) {
        if (DEBUG) Log.w(tag, message);
    }

    public static void e(final String tag, String message) {
        if (DEBUG) Log.e(tag, message);
    }
}

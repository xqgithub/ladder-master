package cc.cloudist.app.util;

import cc.cloudist.app.R;
import cc.cloudist.app.LadderApplication;

import java.text.DecimalFormat;

public class TrafficMonitor {

    private TrafficMonitor() {
    }

    // Bytes per second
    public static long txRate;
    public static long rxRate;

    // Bytes for the current session
    public static long txTotal;
    public static long rxTotal;

    // Bytes for the last query
    public static long txLast;
    public static long rxLast;
    public static long timeStampLast;
    static volatile boolean dirty = true;

    private static String[] mUnits = new String[]{
            "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB", "BB", "NB", "DB", "CB"
    };
    private static DecimalFormat mNumberFormat = new DecimalFormat("@@@");

    public static String formatTraffic(long size) {
        double n = size;
        int i = -1;
        while (n >= 100) {
            n /= 1024;
            i = i + 1;
        }
        if (i < 0) {
            return size + " " + LadderApplication.getInstance()
                    .getResources().getQuantityString(R.plurals.bytes, Long.valueOf(size).intValue());
        } else {
            return mNumberFormat.format(n) + ' ' + mUnits[i];
        }
    }

    public static boolean updateRate() {
        long now = System.currentTimeMillis();
        long delta = now - timeStampLast;
        boolean updated = false;
        if (delta != 0) {
            if (dirty) {
                txRate = (txTotal - txLast) * 1000 / delta;
                rxRate = (rxTotal - rxLast) * 1000 / delta;
                txLast = txTotal;
                rxLast = rxTotal;
                dirty = false;
                updated = true;
            } else {
                if (txRate != 0) {
                    txRate = 0;
                    updated = true;
                }

                if (rxRate != 0) {
                    rxRate = 0;
                    updated = true;
                }
            }

            timeStampLast = now;
        }

        return updated;
    }

    public static void update(long tx, long rx) {
        if (txTotal != tx) {
            txTotal = tx;
            dirty = true;
        }
        if (rxTotal != rx) {
            rxTotal = rx;
            dirty = true;
        }
    }

    public static void reset() {
        txRate = 0;
        rxRate = 0;
        txTotal = 0;
        rxTotal = 0;
        txLast = 0;
        rxLast = 0;
        dirty = true;
    }
}

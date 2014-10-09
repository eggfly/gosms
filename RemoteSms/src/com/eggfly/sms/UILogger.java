package com.eggfly.sms;

import java.util.Date;

import android.util.Log;

/**
 * @author eggfly
 */
public class UILogger {
    private static StringBuilder sFullLogStringBuilder = new StringBuilder();

    public static void d(String tag, String msg) {
        Log.d(tag, msg);
        extraLog(tag, msg);
    }

    public static void i(String tag, String msg) {
        Log.i(tag, msg);
        extraLog(tag, msg);
    }

    public static void w(String tag, String msg) {
        Log.w(tag, msg);
        extraLog(tag, msg);
    }

    public static void w(String tag, Throwable tr) {
        Log.w(tag, tr);
        extraLog(tag, "", tr);
    }

    public static void w(String tag, String msg, Throwable tr) {
        Log.w(tag, msg, tr);
        extraLog(tag, msg, tr);
    }

    public static void e(String tag, String msg) {
        Log.e(tag, msg);
        extraLog(tag, msg);
    }

    public static void e(String tag, String msg, Throwable tr) {
        Log.e(tag, msg, tr);
        extraLog(tag, msg, tr);
    }

    private static void extraLog(String tag, String msg, Throwable tr) {
        String trMsg = tr == null ? "" : String.format(": %s", tr.toString());
        final String log = String.format("%s: %s%s", tag, msg, trMsg);
        final String line = String.format("%s: %s\n", new Date(), log);
        // Save to builder
        sFullLogStringBuilder.append(line);
        // Log to UI
        MainActivity.appendLog(line);
    }

    public static String getFullLog() {
        return sFullLogStringBuilder.toString();
    }

    private static void extraLog(String tag, String msg) {
        extraLog(tag, msg, null);
    }

}

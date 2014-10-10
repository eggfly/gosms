
package com.eggfly.sms;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.log4j.Logger;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

/**
 * @author eggfly
 */
public class CommonLogger {

    private static Logger sFileLogger = Logger.getLogger(SmsApp.class);

    public static void v(String tag, String msg) {
        final String line = generateTimeTagMessageLog(tag, msg);
        sFileLogger.trace(line);
        appendUILog(Log.VERBOSE, line);
    }

    public static void d(String tag, String msg) {
        final String line = generateTimeTagMessageLog(tag, msg);
        sFileLogger.debug(line);
        appendUILog(Log.DEBUG, line);
    }

    public static void i(String tag, String msg) {
        final String line = generateTimeTagMessageLog(tag, msg);
        sFileLogger.info(line);
        appendUILog(Log.INFO, line);
    }

    public static void w(String tag, String msg) {
        final String line = generateTimeTagMessageLog(tag, msg);
        sFileLogger.warn(line);
        appendUILog(Log.WARN, line);
    }

    public static void w(String tag, Throwable tr) {
        final String line = generateTimeTagMessageLog(tag, "");
        sFileLogger.info(line);
        appendUILog(Log.WARN, appendThrowable(line, tr));
    }

    public static void w(String tag, String msg, Throwable tr) {
        final String line = generateTimeTagMessageLog(tag, msg);
        sFileLogger.info(line, tr);
        appendUILog(Log.WARN, appendThrowable(line, tr));
    }

    public static void e(String tag, String msg) {
        final String line = generateTimeTagMessageLog(tag, msg);
        sFileLogger.error(line);
        appendUILog(Log.ERROR, line);
    }

    public static void e(String tag, String msg, Throwable tr) {
        final String line = generateTimeTagMessageLog(tag, msg);
        sFileLogger.error(line, tr);
        appendUILog(Log.ERROR, appendThrowable(line, tr));
    }

    private static String appendThrowable(String origin, Throwable tr) {
        String trMsg = tr == null ? "" : String.format(": %s", tr.toString());
        return String.format("%s%s", origin, trMsg);
    }

    private static String generateTimeTagMessageLog(String tag, String msg) {
        final String log = String.format("%s: %s: %s",
                SimpleDateFormat.getTimeInstance(DateFormat.DEFAULT, Locale.CHINA).format(new Date()),
                tag, msg);
        return log;
    }

    private static void appendUILog(int level, String line) {
        // Log to UI
        int color = Color.BLACK;
        String levelPrefix = "";
        switch (level) {
        case Log.VERBOSE:
            color = Color.GRAY;
            levelPrefix = "V";
            break;
        case Log.DEBUG:
            color = Color.GREEN;
            levelPrefix = "D";
            break;
        case Log.INFO:
            color = Color.BLUE;
            levelPrefix = "I";
            break;
        case Log.WARN:
            color = Color.MAGENTA;
            levelPrefix = "W";
            break;
        case Log.ERROR:
            color = Color.RED;
            levelPrefix = "E";
            break;
        default:
            break;
        }
        line = String.format("%s/%s\n", levelPrefix, line);
        SpannableString spannableString = new SpannableString(line);
        spannableString.setSpan(new ForegroundColorSpan(color),
                0, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        MainActivity.appendLog(spannableString);
    }
}

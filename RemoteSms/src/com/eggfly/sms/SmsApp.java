
package com.eggfly.sms;

import android.app.Application;
import android.content.ComponentName;
import android.util.Log;

/**
 * @author eggfly
 */
public class SmsApp extends Application {
    private static final String TAG = "SmsApp";

    @Override
    public void onCreate() {
        super.onCreate();
        ConfigureLog4J.configure();
        ComponentName name = SmsPushService.startPushService(this);
        Log.d(TAG, "startService result: " + name);
        CommonLogger.i(TAG, "SmsApp Created!");
    }
}

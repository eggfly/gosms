
package com.eggfly.sms;

import android.app.Application;
import android.content.ComponentName;
import android.util.Log;

/**
 * @author eggfly
 */
public class App extends Application {
    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();
        ComponentName name = SmsPushService.startPushService(this);
        Log.d(TAG, "startService result: " + name);
    }
}

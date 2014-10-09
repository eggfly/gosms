/**
 * 
 */

package com.eggfly.sms;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;

/**
 * @author eggfly
 */
public class App extends Application {
    private static final String TAG = "App";

    @Override
    public void onCreate() {
        super.onCreate();
        // this.startService(new Intent(this, PushService.class));
        ComponentName name = startService(new Intent(SmsPushService.ACTION_START));
        Log.d(TAG, "startService result: " + name);
    }
}

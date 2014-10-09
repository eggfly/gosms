package com.eggfly.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

/**
 * @author eggfly
 */
public class ConditionsReceiver extends BroadcastReceiver {
    private final static String TAG = "ConditionsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            UILogger.i(TAG, "CONNECTIVITY_CHANGED");
            SmsPushService.startPushService(context);
        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            UILogger.i(TAG, "BOOT_COMPLETED");
            SmsPushService.startPushService(context);
        }
    }
}

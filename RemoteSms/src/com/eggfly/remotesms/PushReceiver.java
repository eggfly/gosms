package com.eggfly.remotesms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

/**
 * @author eggfly
 * 
 */
public class PushReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
			startPushService(context);
		} else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
			startPushService(context);
		}
	}

	private void startPushService(Context context) {
		context.startService(new Intent(PushService.ACTION_START));
	}
}

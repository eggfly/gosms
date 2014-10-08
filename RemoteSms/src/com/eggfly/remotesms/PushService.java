package com.eggfly.remotesms;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * @author eggfly
 * 
 */
public class PushService extends Service {
	public static final String ACTION_START = "com.eggfly.sms.ACTION_START_PUSHSERVICE";
	public volatile boolean mStarted;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		startTcp();
		return super.onStartCommand(intent, flags, startId);
	}

	private void startTcp() {
		if (!mStarted) {
			mStarted = true;
			
		}
	}
}

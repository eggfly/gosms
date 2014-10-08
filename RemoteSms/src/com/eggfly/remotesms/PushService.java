package com.eggfly.remotesms;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * @author eggfly
 * 
 */
public class PushService extends IntentService {
	private static final String TAG = "PushService";
	public static final String ACTION_START = "com.eggfly.remotesms.ACTION_START_PUSHSERVICE";
	public volatile boolean mStarted;

	public PushService() {
		super(TAG);
		Log.d(TAG, "PushService::Constructed");
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(TAG, "onStartCommand: " + intent);
		// startTcp();
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		Log.i(TAG, "PushService destroyed");
	}

	private void startTcp() {
		if (!mStarted) {
			mStarted = true;
			try {
				Socket socket = new Socket("aws.host8.tk", 6666);
				try {
					PrintWriter writer = new PrintWriter(
							socket.getOutputStream(), true);
					writer.println("WORKER");
				} finally {
					socket.close();
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i(TAG, "onHandleIntent" + intent);
	}
}

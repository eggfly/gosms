
package com.eggfly.sms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

/**
 * @author eggfly
 */
public class SmsPushService extends Service {
    private static final String TAG = "SmsPushService";
    public static final String ACTION_START = "com.eggfly.sms.ACTION_START_PUSHSERVICE";
    public volatile SocketTask mSocketTask;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: " + intent);
        startTcp();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "PushService destroyed");
    }

    private void startTcp() {
        if (mSocketTask == null) {
            mSocketTask = new SocketTask();
            mSocketTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        }
    }

    private class SocketTask extends AsyncTask<Void, Void, Void> {
        private static final long DELAY_AFTER_EXCEPTION = 5000;
        private static final long DELAY_AFTER_CONNECTION_CLOSED = 500;

        @Override
        protected Void doInBackground(Void... params) {
            long delayTime = DELAY_AFTER_CONNECTION_CLOSED;
            boolean success = false;
            while (true) {
                try {
                    Socket socket = new Socket("aws.host8.tk", 6666);
                    try {
                        PrintWriter writer = new PrintWriter(
                                socket.getOutputStream(), true);
                        writer.println("WORKER");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String line = reader.readLine();
                        if (!TextUtils.isEmpty(line)) {
                            Log.i(TAG, line);
                            success = true;
                        } else {
                            Log.w(TAG, "REMOTE CONNECTION CLOSED.");
                            delayTime = DELAY_AFTER_CONNECTION_CLOSED;
                        }
                    } finally {
                        socket.close();
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    delayTime = DELAY_AFTER_EXCEPTION;
                } catch (IOException e) {
                    e.printStackTrace();
                    delayTime = DELAY_AFTER_EXCEPTION;
                }

                if (!success) {
                    try {
                        Thread.sleep(delayTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}


package com.eggfly.sms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
    private static SmsPushService sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

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
        mSocketTask.cancel(true);
        Log.i(TAG, "SmsPushService destroyed");
    }

    private void startTcp() {
        if (mSocketTask == null) {
            mSocketTask = new SocketTask();
            mSocketTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
        }
    }

    private class SocketTask extends AsyncTask<Void, Void, Void> {
        public static final int STATE_WAITING_FOR_COMMAND = 0;
        public static final int STATE_NO_CONNECTION = 1;

        private static final long DELAY_AFTER_NO_CONNECTION = 10 * 1000;
        private static final long DELAY_AFTER_EXCEPTION = 5 * 1000;
        private static final long DELAY_AFTER_CONNECTION_CLOSED = 0;
        private long mDelayTime = DELAY_AFTER_CONNECTION_CLOSED;

        private int mCurrentState = STATE_NO_CONNECTION;

        @Override
        protected Void doInBackground(Void... params) {
            while (true) {
                boolean success = false;
                if (!isNetworkAvailable()) {
                    mDelayTime = DELAY_AFTER_NO_CONNECTION;
                    Log.i(TAG, "no network connection");
                } else {
                    success = transport();
                }

                if (!success) {
                    try {
                        Thread.sleep(mDelayTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public int getCurrentState() {
            return mCurrentState;
        }

        /**
         * @param success
         * @return
         */
        private boolean transport() {
            boolean success = false;
            try {
                Socket socket = new Socket("aws.host8.tk", 6666);
                MainActivity.logAppendLine(String.format("socket connected: %s:%s",
                        socket.getLocalAddress(),
                        socket.getLocalPort()));
                try {
                    PrintWriter writer = new PrintWriter(
                            socket.getOutputStream(), true);
                    writer.println("WORKER");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    MainActivity.logAppendLine("socket sent: WORKER\\n");
                    mCurrentState = STATE_WAITING_FOR_COMMAND;
                    MainActivity.notifyServiceStatusChanged();
                    String line = reader.readLine();
                    // TODO
                    mCurrentState = STATE_NO_CONNECTION;
                    MainActivity.notifyServiceStatusChanged();
                    if (!TextUtils.isEmpty(line)) {
                        MainActivity.logAppendLine("socket received: " + line);
                        Log.i(TAG, line);
                        success = true;
                    } else {
                        Log.w(TAG, "REMOTE CONNECTION CLOSED.");
                        mDelayTime = DELAY_AFTER_CONNECTION_CLOSED;
                    }
                } finally {
                    socket.close();
                }
            } catch (UnknownHostException e) {
                Log.w(TAG, e);
                MainActivity.logAppendLine(e.toString());
                mDelayTime = DELAY_AFTER_EXCEPTION;
            } catch (IOException e) {
                Log.w(TAG, e);
                MainActivity.logAppendLine(e.toString());
                mDelayTime = DELAY_AFTER_EXCEPTION;
            }
            return success;
        }
    }

    public static int getServiceStatusResId() {
        if (sInstance == null) {
            return R.string.no_service_present;
        } else {
            return sInstance.getServiceStatusResIdInner();
        }
    }

    private int getServiceStatusResIdInner() {
        if (mSocketTask == null) {
            return R.string.socket_task_not_running;
        } else {
            switch (mSocketTask.getCurrentState()) {
            case SocketTask.STATE_WAITING_FOR_COMMAND:
                return R.string.socket_wait_for_command;
            case SocketTask.STATE_NO_CONNECTION:
                return R.string.socket_no_connection;
            default:
                return R.string.socket_state_unknown;
            }
        }
    }

    public static ComponentName startPushService(Context context) {
        return context.startService(new Intent(context, SmsPushService.class));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            return false;
        } else {
            return ni.isConnectedOrConnecting();
        }
    }
}

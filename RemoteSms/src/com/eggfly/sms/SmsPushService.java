
package com.eggfly.sms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.text.TextUtils;

/**
 * @author eggfly
 */
public class SmsPushService extends Service {
    public static class PushServiceState {
        public final static int SERVICE_NOT_PRESENT = 0;
        public final static int SOCKET_TASK_NOT_RUNNING = 1;
        public final static int SOCKET_NOT_CONNECTED = 2;
        public final static int SOCKET_CONNECTED_WAITING_FOR_COMMAND = 3;
        public final static int SOCKET_NO_NETWORK = 4;
    }

    private static final String TAG = "SmsPushService";
    public static final String ACTION_START = "com.eggfly.sms.ACTION_START_PUSHSERVICE";
    public static final String ACTION_STOP = "com.eggfly.sms.ACTION_STOP_PUSHSERVICE";
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
        CommonLogger.i(TAG, "onStartCommand: " + intent);
        if (intent != null) {
            if (ACTION_STOP.equals(intent.getAction())) {
                stopSelf();
                CommonLogger.i(TAG, "stopSelf");
            }
        }
        startTcp();
        MainActivity.notifyServiceStatusChanged();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sInstance = null;
        mSocketTask.interruptSocket();
        boolean result = mSocketTask.cancel(true);
        CommonLogger.i(TAG, "mSocketTask cancel result: " + result);
        CommonLogger.i(TAG, "SmsPushService destroyed");
        MainActivity.notifyServiceStatusChanged();
    }

    private void startTcp() {
        if (mSocketTask == null) {
            mSocketTask = new SocketTask();
            mSocketTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    (Void[]) null);
        }
    }

    private class SocketTask extends AsyncTask<Void, Void, Void> {

        private static final long DELAY_AFTER_NO_CONNECTION = 10 * 1000;
        private static final long DELAY_AFTER_EXCEPTION = 5 * 1000;
        private static final long DELAY_AFTER_CONNECTION_CLOSED = 0;
        private long mDelayTime = DELAY_AFTER_CONNECTION_CLOSED;

        private int mCurrentState = PushServiceState.SOCKET_NOT_CONNECTED;
        private Socket mSocket;

        @Override
        protected Void doInBackground(Void... params) {
            while (true) {
                boolean success = false;
                if (!isNetworkAvailable()) {
                    mDelayTime = DELAY_AFTER_NO_CONNECTION;
                    CommonLogger.i(TAG, "no network connection");
                } else {
                    success = transport();
                }
                if (isCancelled()) {
                    CommonLogger.i(TAG, "Detected cancelled socket task, return.");
                    return null;
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

        public void interruptSocket() {
            if (mSocket != null) {
                try {
                    mSocket.shutdownInput();
                    mSocket.shutdownOutput();
                    mSocket.close();
                } catch (IOException e) {
                    CommonLogger.w(TAG, "IOExpcetion when interruptSocket", e);
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
                mSocket = new Socket("aws.host8.tk", 6666);
                CommonLogger.i(TAG, String.format("socket connected: %s:%s",
                        mSocket.getLocalAddress(), mSocket.getLocalPort()));
                try {
                    PrintWriter writer = new PrintWriter(
                            mSocket.getOutputStream(), true);
                    writer.println("WORKER");
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(mSocket.getInputStream()));
                    CommonLogger.i(TAG, "socket sent: WORKER\\n");
                    mCurrentState = PushServiceState.SOCKET_CONNECTED_WAITING_FOR_COMMAND;
                    MainActivity.notifyServiceStatusChanged();
                    String line = reader.readLine();
                    if (!TextUtils.isEmpty(line)) {
                        // TODO: handling command state here
                        CommonLogger.i(TAG, "socket received: " + line);
                        handleCommand(line);
                        success = true;
                    } else {
                        CommonLogger.w(TAG, "REMOTE CONNECTION CLOSED.");
                        mDelayTime = DELAY_AFTER_CONNECTION_CLOSED;
                    }
                } finally {
                    mSocket.close();
                    mCurrentState = PushServiceState.SOCKET_NOT_CONNECTED;
                    MainActivity.notifyServiceStatusChanged();
                }
            } catch (UnknownHostException e) {
                CommonLogger.w(TAG, e);
                mDelayTime = DELAY_AFTER_EXCEPTION;
            } catch (IOException e) {
                CommonLogger.w(TAG, e);
                mDelayTime = DELAY_AFTER_EXCEPTION;
            }
            return success;
        }

        private void handleCommand(String line) {
            try {
                JSONObject obj = new JSONObject(line.trim());
                String number = obj.getString("number");
                String msg = obj.getString("message");

                SmsManager smsManager = SmsManager.getDefault();
                ArrayList<String> parts = smsManager.divideMessage(msg);
                ArrayList<PendingIntent> sentIntents = new ArrayList<PendingIntent>();
                ArrayList<PendingIntent> deliveryIntents = new ArrayList<PendingIntent>();
                for (String part : parts) {
                    sentIntents.add(PendingIntent.getBroadcast(SmsPushService.this, 0, new Intent(), 0));
                    deliveryIntents.add(PendingIntent.getBroadcast(SmsPushService.this, 0, new Intent(), 0));
                    CommonLogger.i(TAG, "Msg part: " + part);
                }
                // send message
                smsManager.sendMultipartTextMessage(number, null, parts, sentIntents, deliveryIntents);
                CommonLogger.i(TAG, "sms sent requested: " + line);
            } catch (JSONException e) {
                CommonLogger.e(TAG, "error when parse command", e);
            }
        }
    }

    public static int getServiceState() {
        if (sInstance == null) {
            return PushServiceState.SERVICE_NOT_PRESENT;
        } else {
            return sInstance.getServiceStateInner();
        }
    }

    private int getServiceStateInner() {
        if (mSocketTask == null) {
            // return R.string.socket_task_not_running;
            return PushServiceState.SOCKET_TASK_NOT_RUNNING;
        } else {
            return mSocketTask.getCurrentState();
        }
    }

    public static ComponentName startPushService(Context context) {
        return context.startService(new Intent(context, SmsPushService.class));
    }

    public static void stopPushService(Context context) {
        if (sInstance != null) {
            sInstance.stopSelf();
        }
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

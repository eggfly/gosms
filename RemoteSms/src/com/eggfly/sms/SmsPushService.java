
package com.eggfly.sms;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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
import android.util.Pair;

/**
 * @author eggfly
 */
public class SmsPushService extends Service {
    private final static String DEFAULT_HOST = "aws.host8.tk/comet";
    private final static int DEFAULT_PORT = 8080;
    private static String sHost;
    private final static int INVALID_PORT = -1;
    private static int sPort = INVALID_PORT;

    public static class PushServiceState {
        public final static int SERVICE_NOT_PRESENT = 0;
        public final static int SOCKET_TASK_NOT_RUNNING = 1;
        public final static int SOCKET_NOT_CONNECTED = 2;
        public final static int SOCKET_CONNECTED_WAITING_FOR_COMMAND = 3;
        public final static int SOCKET_NO_NETWORK = 4;
    }

    private static final String TAG = "SmsPushService";
    public static final String ACTION_START = "com.eggfly.sms.ACTION_START_PUSHSERVICE";
    public volatile PushTaskBase mPushTask;
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
        startTcp();
        MainActivity.notifyServiceStatusChanged();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sInstance = null;
        if (mPushTask != null) {
            mPushTask.interruptConnetion();
            boolean result = mPushTask.cancel(true);
            mPushTask = null;
            CommonLogger.i(TAG, "mSocketTask cancel result: " + result);
        }
        CommonLogger.i(TAG, "SmsPushService destroyed");
        MainActivity.notifyServiceStatusChanged();
    }

    private void startTcp() {
        if (mPushTask == null) {
            // mPushTask = new SocketTask();
            mPushTask = new CometTask();
            mPushTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                    (Void[]) null);
        }
    }

    private abstract class PushTaskBase extends AsyncTask<Void, Void, Void> {

        protected int mCurrentState = PushServiceState.SOCKET_NOT_CONNECTED;

        abstract void interruptConnetion();

        abstract int getCurrentState();

        protected void handleCommand(String line) {
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

    private class CometTask extends PushTaskBase {

        private HttpClient mHttpClient;

        @Override
        void interruptConnetion() {
        }

        @Override
        int getCurrentState() {
            return 0;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (!isNetworkAvailable()) {
                CommonLogger.i(TAG, "no network connection");
            } else {
                boolean result = true;
                while (result) {
                    result = transport();
                }
            }
            mPushTask = null;
            return null;
        }

        private boolean transport() {
            boolean success = false;
            try {
                Pair<String, Integer> address = getAddress();
                mHttpClient = new DefaultHttpClient();
                URI uriWithoutPort;
                URI uri;
                try {
                    uriWithoutPort = new URI("http://" + address.first);
                    uri = new URI(uriWithoutPort.getScheme(), uriWithoutPort.getUserInfo(), uriWithoutPort.getHost(), address.second,
                            uriWithoutPort.getPath(), uriWithoutPort.getQuery(), uriWithoutPort.getFragment());
                } catch (URISyntaxException e) {
                    return false;
                }
                HttpGet request = new HttpGet(uri);
                CommonLogger.i(TAG, String.format("http connected"));
                try {
                    mCurrentState = PushServiceState.SOCKET_CONNECTED_WAITING_FOR_COMMAND;
                    MainActivity.notifyServiceStatusChanged();

                    HttpResponse response = mHttpClient.execute(request);
                    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                        InputStream is = response.getEntity().getContent();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        StringBuilder builder = new StringBuilder();
                        try {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                builder.append(line);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            reader.close();
                        }

                        String line = builder.toString();
                        if (!TextUtils.isEmpty(line)) {
                            // TODO: handling command state here
                            CommonLogger.i(TAG, "socket received: " + line);
                            handleCommand(line);
                            success = true;
                        } else {
                            CommonLogger.w(TAG, "EOF - SOCKET CONNECTION CLOSED.");
                        }
                    }

                } finally {
                    mCurrentState = PushServiceState.SOCKET_NOT_CONNECTED;
                    MainActivity.notifyServiceStatusChanged();
                }
            } catch (UnknownHostException e) {
                CommonLogger.w(TAG, e);
            } catch (IOException e) {
                CommonLogger.w(TAG, e);
            }
            return success;
        }
    }

    private class SocketTask extends PushTaskBase {
        private Socket mSocket;

        @Override
        protected Void doInBackground(Void... params) {
            if (!isNetworkAvailable()) {
                CommonLogger.i(TAG, "no network connection");
            } else {
                boolean result = true;
                while (result) {
                    result = transport();
                }
            }
            mPushTask = null;
            return null;
        }

        public void interruptConnetion() {
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
                Pair<String, Integer> address = getAddress();
                mSocket = new Socket(address.first, address.second);
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
                        CommonLogger.w(TAG, "EOF - SOCKET CONNECTION CLOSED.");
                    }
                } finally {
                    mSocket.close();
                    mCurrentState = PushServiceState.SOCKET_NOT_CONNECTED;
                    MainActivity.notifyServiceStatusChanged();
                }
            } catch (UnknownHostException e) {
                CommonLogger.w(TAG, e);
            } catch (IOException e) {
                CommonLogger.w(TAG, e);
            }
            return success;
        }
    }

    public static int getServiceState() {
        if (sInstance == null) {
            return PushServiceState.SERVICE_NOT_PRESENT;
        } else {
            return sInstance.getServiceStateInner();
        }
    }

    private static Pair<String, Integer> getAddress() {
        if (!TextUtils.isEmpty(sHost) && sPort != INVALID_PORT) {
            return new Pair<String, Integer>(sHost, sPort);
        } else {
            return new Pair<String, Integer>(DEFAULT_HOST, DEFAULT_PORT);
        }
    }

    public static void setAddress(String host, int port) {
        sHost = host;
        sPort = port;
    }

    private int getServiceStateInner() {
        if (mPushTask == null) {
            // return R.string.socket_task_not_running;
            return PushServiceState.SOCKET_TASK_NOT_RUNNING;
        } else {
            return mPushTask.getCurrentState();
        }
    }

    public static ComponentName startPushService(Context context) {
        return context.startService(new Intent(context, SmsPushService.class));
    }

    public static void stopPushService(Context context) {
        if (sInstance != null) {
            sInstance.stopSelf();
            // sInstance = null in onDestroy()
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


package com.eggfly.sms;

import com.eggfly.sms.SmsPushService.PushServiceState;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * @author eggfly
 */
public class MainActivity extends Activity implements OnClickListener {
    private static final String TAG = "MainActivity";
    private static MainActivity sInstance;
    private EditText mLogEditText;
    private TextView mStatusTextView;
    private Button mStartServiceButton;
    private Button mStopServiceButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = this;
        setContentView(R.layout.layout_main);

        mLogEditText = (EditText) findViewById(R.id.logEditText);
        mLogEditText.setKeyListener(null);
        // mLogEditText.setText(UILogger.getFullLog());

        mStartServiceButton = (Button) findViewById(R.id.startServiceButton);
        mStartServiceButton.setOnClickListener(this);

        mStopServiceButton = (Button) findViewById(R.id.stopServiceButton);
        mStopServiceButton.setOnClickListener(this);

        mStatusTextView = (TextView) findViewById(R.id.statusTextView);
    }

    @Override
    protected void onResume() {
        CommonLogger.d(TAG, "onResume");
        super.onResume();
        refreshStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sInstance = null;
    }

    public static void appendLog(final CharSequence richtext) {
        if (sInstance != null) {
            sInstance.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    sInstance.mLogEditText.append(richtext);
                }
            });
        }
    }

    public static void notifyServiceStatusChanged() {
        if (sInstance != null) {
            sInstance.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    sInstance.refreshStatus();
                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        if (view == mStartServiceButton) {
            SmsPushService.startPushService(this);
        } else if (view == mStopServiceButton) {
            SmsPushService.stopPushService(this);
        } else {
            return;
        }
        refreshStatus();
    }

    private void refreshStatus() {
        int state = SmsPushService.getServiceState();
        boolean serviceRunning = state != PushServiceState.SERVICE_NOT_PRESENT;
        mStartServiceButton.setEnabled(!serviceRunning);
        mStopServiceButton.setEnabled(serviceRunning);

        mStatusTextView.setText(convertStatusToStringResId(state));
    }

    private int convertStatusToStringResId(int state) {
        switch (state) {
        case PushServiceState.SERVICE_NOT_PRESENT:
            return R.string.no_service_present;
        case PushServiceState.SOCKET_CONNECTED_WAITING_FOR_COMMAND:
            return R.string.socket_wait_for_command;
        case PushServiceState.SOCKET_NO_NETWORK:
            return R.string.socket_no_network;
        case PushServiceState.SOCKET_NOT_CONNECTED:
            return R.string.socket_not_connected;
        case PushServiceState.SOCKET_TASK_NOT_RUNNING:
            return R.string.socket_task_not_running;
        default:
            return R.string.socket_state_unknown;
        }
    }
}

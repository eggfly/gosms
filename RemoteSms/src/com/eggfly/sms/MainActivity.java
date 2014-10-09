
package com.eggfly.sms;

import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
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
        mStatusTextView = (TextView) findViewById(R.id.statusTextView);
        mLogEditText = (EditText) findViewById(R.id.logEditText);
        mLogEditText.setKeyListener(null);
        mStartServiceButton = (Button) findViewById(R.id.startServiceButton);
        mStopServiceButton = (Button) findViewById(R.id.stopServiceButton);
        mStartServiceButton.setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
        refreshStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sInstance = null;
    }

    public static void logAppendLine(String log) {
        final String line = String.format("%s: %s\n", new Date(), log);
        if (sInstance != null) {
            sInstance.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    sInstance.mLogEditText.append(line);
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
            stopService(new Intent(this, SmsPushService.class));
        } else {
            return;
        }
        refreshStatus();
    }

    private void refreshStatus() {
        mStatusTextView.setText(SmsPushService.getServiceStatusResId());
    }
}

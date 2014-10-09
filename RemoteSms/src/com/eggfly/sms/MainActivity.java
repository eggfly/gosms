package com.eggfly.sms;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
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

        mLogEditText = (EditText) findViewById(R.id.logEditText);
        mLogEditText.setKeyListener(null);
        mLogEditText.setText(UILogger.getFullLog());

        mStartServiceButton = (Button) findViewById(R.id.startServiceButton);
        mStartServiceButton.setOnClickListener(this);

        mStopServiceButton = (Button) findViewById(R.id.stopServiceButton);
        mStatusTextView = (TextView) findViewById(R.id.statusTextView);
    }

    @Override
    protected void onResume() {
        UILogger.d(TAG, "onResume");
        super.onResume();
        refreshStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sInstance = null;
    }

    public static void appendLog(final String log) {
        if (sInstance != null) {
            sInstance.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SpannableString spannableString = new SpannableString(log);
                    spannableString.setSpan(new ForegroundColorSpan(Color.RED),
                            0, log.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sInstance.mLogEditText.append(spannableString);
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

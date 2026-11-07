package com.QQBrowsernovelswitch.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;

import com.QQBrowsernovelswitch.R;
import com.QQBrowsernovelswitch.core.VersionCheck;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MainActivity extends AppCompatActivity {

    private static final long SWITCH_DELAY_MS = 2000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable openSwitch = this::openSwitchActivity;
    private boolean navigationPending;
    private boolean switchStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        String currentVersion = VersionCheck.getAppVersionName(this);
        String message = getString(R.string.welcome_message, currentVersion);

        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.welcome_title, getString(R.string.app_name)))
                .setMessage(message)
                .setIcon(R.drawable.icon)
                .setCancelable(false)
                .show();

        handler.postDelayed(openSwitch, SWITCH_DELAY_MS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navigationPending && !switchStarted) {
            navigationPending = false;
            openSwitchActivity();
        }
    }

    private void openSwitchActivity() {
        if (switchStarted || isFinishing() || isDestroyed()) {
            return;
        }
        if (!getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            navigationPending = true;
            return;
        }
        switchStarted = true;
        startActivity(new Intent(MainActivity.this, SwitchActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(openSwitch);
        super.onDestroy();
    }
}

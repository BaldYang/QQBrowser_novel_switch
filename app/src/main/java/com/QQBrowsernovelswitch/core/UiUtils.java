package com.QQBrowsernovelswitch.core;

import android.app.Activity;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class UiUtils {

    private UiUtils() {
    }

    public static boolean isAlive(@Nullable Activity activity) {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    public static void toast(@NonNull Activity activity, @NonNull String text) {
        activity.runOnUiThread(() -> {
            if (isAlive(activity)) {
                Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void toastIfEnabled(@NonNull Activity activity, boolean enabled, @NonNull String text) {
        if (!enabled) {
            return;
        }
        toast(activity, text);
    }
}

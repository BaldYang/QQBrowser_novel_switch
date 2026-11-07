package com.QQBrowsernovelswitch.core;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.QQBrowsernovelswitch.R;

public final class RemoteCheck {

    public interface OnContent {

        void onContent(@NonNull Activity activity, boolean fromSettings, @NonNull String content);
    }

    private RemoteCheck() {
    }

    public static void run(@NonNull Context context, @NonNull String activityName,
                           @StringRes int urlRes,
                           @NonNull String progressText, @NonNull String failureText,
                           @NonNull OnContent onContent) {
        if (!(context instanceof Activity)) {
            return;
        }
        final Activity activity = (Activity) context;
        final boolean fromSettings = "SettingsActivity".equals(activityName);
        final Toast[] progressToast = new Toast[1];

        if (fromSettings) {
            if (!InternetCheck.isNetworkAvailable(context)) {
                UiUtils.toast(activity, activity.getString(R.string.network_check));
                return;
            }
            progressToast[0] = Toast.makeText(activity, progressText, Toast.LENGTH_SHORT);
            progressToast[0].show();
        }

        RemoteContent.Callback callback = new RemoteContent.Callback() {
            @Override
            public void onSuccess(@NonNull String content) {
                cancelProgress(progressToast);
                onContent.onContent(activity, fromSettings, content);
            }

            @Override
            public void onFailure(@NonNull Exception error) {
                cancelProgress(progressToast);
                UiUtils.toastIfEnabled(activity, fromSettings, failureText);
            }
        };

        RemoteContent.load(context, urlRes, callback);
    }

    private static void cancelProgress(Toast[] holder) {
        if (holder[0] != null) {
            holder[0].cancel();
            holder[0] = null;
        }
    }
}

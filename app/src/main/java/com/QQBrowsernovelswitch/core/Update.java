package com.QQBrowsernovelswitch.core;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import com.QQBrowsernovelswitch.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class Update {

    public static void checkUpdate(final Context context, final String activityName) {
        if (!(context instanceof Activity)) {
            return;
        }
        final Activity activity = (Activity) context;
        final boolean fromSettings = "SettingsActivity".equals(activityName);
        final String currentVersion = VersionCheck.getAppVersionName(context);
        if (currentVersion == null || currentVersion.isEmpty() || "N/A".equals(currentVersion)) {
            UiUtils.toastIfEnabled(activity, fromSettings,
                    activity.getString(R.string.update_failed));
            return;
        }

        RemoteCheck.run(context, activityName, R.string.updateUrl,
                context.getString(R.string.update_checking),
                context.getString(R.string.update_failed),
                (hostActivity, fromSettingsMode, content) ->
                        handleContent(hostActivity, fromSettingsMode, currentVersion, content));
    }

    private static void handleContent(Activity activity, boolean fromSettings,
                                      String currentVersion, String content) {
        String time = RemoteContent.field(content, "time:【");
        String newVersion = RemoteContent.field(content, "new_version:【");
        String updateExplanation = RemoteContent.field(content, "update_explanation:【");
        String address = RemoteContent.field(content, "address:【");

        if (newVersion.isEmpty()) {
            UiUtils.toastIfEnabled(activity, fromSettings,
                    activity.getString(R.string.update_failed));
            return;
        }
        if (!isNewerVersion(newVersion, currentVersion)) {
            UiUtils.toastIfEnabled(activity, fromSettings,
                    activity.getString(R.string.update_latest));
            return;
        }
        if (!isTrustedUpdateUrl(address)) {
            UiUtils.toastIfEnabled(activity, fromSettings,
                    activity.getString(R.string.update_failed));
            return;
        }
        if (!UiUtils.isAlive(activity)) {
            return;
        }
        showUpdateDialog(activity, time, newVersion, updateExplanation, address);
    }

    private static boolean isTrustedUpdateUrl(String address) {
        if (address == null || address.trim().isEmpty()) {
            return false;
        }
        try {
            Uri uri = Uri.parse(address.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            String normalized = host.toLowerCase(Locale.ROOT);
            return normalized.equals("github.com")
                    || normalized.equals("www.github.com")
                    || normalized.equals("gitee.com")
                    || normalized.equals("www.gitee.com")
                    || normalized.equals("baldyang.github.io");
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isNewerVersion(String remoteVersion, String currentVersion) {
        int[] remote = parseVersion(remoteVersion);
        int[] current = parseVersion(currentVersion);
        int length = Math.max(remote.length, current.length);
        for (int i = 0; i < length; i++) {
            int r = i < remote.length ? remote[i] : 0;
            int c = i < current.length ? current[i] : 0;
            if (r != c) {
                return r > c;
            }
        }
        return false;
    }

    private static int[] parseVersion(String version) {
        if (version == null) {
            return new int[0];
        }
        String[] parts = version.trim().split("\\.");
        int[] numbers = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String digits = parts[i].replaceAll("\\D", "");
            try {
                numbers[i] = digits.isEmpty() ? 0 : Integer.parseInt(digits);
            } catch (NumberFormatException e) {
                numbers[i] = 0;
            }
        }
        return numbers;
    }

    private static void showUpdateDialog(Activity activity, String time, String newVersion,
                                         String updateExplanation, String address) {
        StringBuilder message = new StringBuilder();
        if (!updateExplanation.isEmpty()) {
            message.append(updateExplanation);
        }
        if (!time.isEmpty()) {
            if (message.length() > 0) {
                message.append('\n');
            }
            message.append(time);
        }
        if (message.length() == 0) {
            message.append(activity.getString(R.string.update_default_message));
        }
        new MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getString(R.string.update_new_title, newVersion))
                .setMessage(message.toString())
                .setCancelable(false)
                .setIcon(R.drawable.icon)
                .setPositiveButton(R.string.update_now,
                        (dialog, which) -> LinkUtils.open(activity, address))
                .setNegativeButton(R.string.update_later, (dialog, which) -> dialog.dismiss())
                .show();
    }
}

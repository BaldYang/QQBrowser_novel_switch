package com.QQBrowsernovelswitch.core;

import android.app.Activity;
import android.content.Context;

import com.QQBrowsernovelswitch.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Announces {

    private static final String ACTIVITY_SWITCH = "SwitchActivity";
    private static final String ANNOUNCE_ID_FILE = "announce_id";

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Object ID_LOCK = new Object();
    private static String showingId;
    private static WeakReference<Activity> showingActivity;

    public static void checkAnnounces(final Context context, final String activityName) {
        RemoteCheck.run(context, activityName, R.string.announcesUrl,
                context.getString(R.string.announce_fetching),
                context.getString(R.string.announce_failed),
                (activity, fromSettings, content) ->
                        handleContent(context, activity, activityName, fromSettings, content));
    }

    private static void handleContent(final Context context, final Activity activity,
                                      final String activityName, final boolean fromSettings,
                                      String pageContent) {
        final String id = RemoteContent.field(pageContent, "id:【");
        final String time = RemoteContent.field(pageContent, "time:【");
        final String title = RemoteContent.field(pageContent, "title:【");
        final String content = RemoteContent.field(pageContent, "content:【");

        if (id.isEmpty() || content.isEmpty()) {
            UiUtils.toastIfEnabled(activity, fromSettings,
                    activity.getString(R.string.announce_failed));
            return;
        }

        IO.execute(() -> {
            String storedId = readStoredId(context);
            synchronized (ID_LOCK) {
                if (ACTIVITY_SWITCH.equals(activityName) && id.equals(storedId)) {
                    return;
                }
                if (id.equals(showingId)
                        && activity == (showingActivity == null ? null : showingActivity.get())) {
                    return;
                }
                showingId = id;
                showingActivity = new WeakReference<>(activity);
            }
            activity.runOnUiThread(() -> {
                if (!UiUtils.isAlive(activity)) {
                    clearShowingId(activity, id);
                    return;
                }
                try {
                    showAnnouncesDialog(activity, time, title, content, () -> {
                        IO.execute(() -> writeStoredId(context, id));
                        clearShowingId(activity, id);
                    });
                } catch (Throwable t) {
                    clearShowingId(activity, id);
                }
            });
        });
    }

    private static void clearShowingId(Activity activity, String id) {
        synchronized (ID_LOCK) {
            if (id.equals(showingId)
                    && showingActivity != null
                    && showingActivity.get() == activity) {
                showingId = null;
                showingActivity = null;
            }
        }
    }

    private static String readStoredId(Context context) {
        File idFile = new File(context.getFilesDir(), ANNOUNCE_ID_FILE);
        if (!idFile.exists()) {
            return "";
        }
        try (FileInputStream fis = new FileInputStream(idFile);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(fis, StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            return line == null ? "" : line.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static void writeStoredId(Context context, String id) {
        try (FileOutputStream fos = new FileOutputStream(
                new File(context.getFilesDir(), ANNOUNCE_ID_FILE))) {
            fos.write(id.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private static void showAnnouncesDialog(Activity activity, String time, String title,
                                            String content, Runnable onConfirmed) {
        StringBuilder message = new StringBuilder(content);
        if (!time.isEmpty()) {
            message.append('\n').append(time);
        }
        String dialogTitle = title.isEmpty()
                ? activity.getString(R.string.announce_title_only)
                : activity.getString(R.string.announce_title, title);
        new MaterialAlertDialogBuilder(activity)
                .setTitle(dialogTitle)
                .setMessage(message.toString())
                .setCancelable(false)
                .setIcon(R.drawable.icon)
                .setNegativeButton(R.string.announce_ack, (dialog, which) -> {
                    dialog.dismiss();
                    onConfirmed.run();
                })
                .show();
    }
}

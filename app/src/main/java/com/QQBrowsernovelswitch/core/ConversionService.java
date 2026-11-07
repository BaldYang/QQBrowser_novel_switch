package com.QQBrowsernovelswitch.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.QQBrowsernovelswitch.R;
import com.QQBrowsernovelswitch.ui.SwitchActivity;

public class ConversionService extends Service {

    private static final String ACTION_START = "start";
    private static final String ACTION_UPDATE = "update";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_PROGRESS = "progress";
    private static final String CHANNEL_ID = "novel_conversion";
    private static final int NOTIFICATION_ID = 1001;

    private NotificationManager notificationManager;
    private boolean foregroundStarted;

    public static void start(Context context, String title) {
        Intent intent = new Intent(context, ConversionService.class)
                .setAction(ACTION_START)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_PROGRESS, 1);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void update(Context context, String title, int progress) {
        Intent intent = new Intent(context, ConversionService.class)
                .setAction(ACTION_UPDATE)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_PROGRESS, progress);
        context.startService(intent);
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, ConversionService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    getString(R.string.notification_channel),
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        String title = intent.getStringExtra(EXTRA_TITLE);
        int progress = intent.getIntExtra(EXTRA_PROGRESS, 1);
        Notification notification = buildNotification(title, progress);
        if (ACTION_START.equals(action) && !foregroundStarted) {
            startForeground(NOTIFICATION_ID, notification);
            foregroundStarted = true;
        } else if (foregroundStarted) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
        return START_NOT_STICKY;
    }

    private Notification buildNotification(String title, int progress) {
        Intent openIntent = new Intent(this, SwitchActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title,
                        title == null ? "" : title, progress))
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setProgress(100, progress, false)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

package com.QQBrowsernovelswitch.core;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.QQBrowsernovelswitch.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public final class ReadingEnvironmentChecker {

    public static final int REQUEST_CODE_STORAGE_PERMISSION = 100;

    private static final int BINDER_RETRY_COUNT = 3;
    private static final long BINDER_RETRY_DELAY_MS = 400;

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static int binderCheckToken;
    private static boolean pendingShizukuReturn;
    private static Toast connectingToast;

    private ReadingEnvironmentChecker() {
    }

    public static void check(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            requestStoragePermissions(activity);
            return;
        }
        checkShizuku(activity);
    }

    public static void checkOnResume(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }
        ShizukuManager.init(activity.getApplicationContext());
        if (ShizukuManager.isBinderReady()
                && ShizukuManager.isSupported()
                && ShizukuManager.hasPermission()) {
            ShizukuManager.bindUserService();
        }
    }

    public static boolean consumePendingShizukuReturn() {
        boolean pending = pendingShizukuReturn;
        pendingShizukuReturn = false;
        return pending;
    }

    public static boolean canConvertNovels(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return hasStoragePermissions(activity);
        }
        return canReadNovels(activity);
    }

    public static boolean canReadNovels(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return hasStoragePermissions(activity);
        }
        return ShizukuManager.getInstalledVersionName(activity) != null
                && ShizukuManager.isBinderReady()
                && ShizukuManager.isSupported()
                && ShizukuManager.hasPermission();
    }

    public static boolean hasStoragePermissions(@NonNull Activity activity) {
        return ContextCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestStoragePermissions(@NonNull Activity activity) {
        if (hasStoragePermissions(activity)) {
            return;
        }
        Toast.makeText(activity, R.string.storage_permission_message,
                Toast.LENGTH_LONG).show();
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_CODE_STORAGE_PERMISSION);
    }

    private static void checkShizuku(@NonNull Activity activity) {
        ShizukuManager.init(activity.getApplicationContext());
        if (ShizukuManager.getInstalledVersionName(activity) == null) {
            dismissConnectingToast();
            showInstallShizukuDialog(activity);
            return;
        }
        if (!ShizukuManager.isBinderReady()) {
            showConnectingToast(activity);
        }
        final int token = ++binderCheckToken;
        awaitBinder(activity, token, 0);
    }

    private static void showConnectingToast(@NonNull Activity activity) {
        dismissConnectingToast();
        connectingToast = Toast.makeText(activity, R.string.shizuku_connecting,
                Toast.LENGTH_LONG);
        connectingToast.show();
    }

    private static void dismissConnectingToast() {
        if (connectingToast != null) {
            connectingToast.cancel();
            connectingToast = null;
        }
    }

    private static void awaitBinder(@NonNull Activity activity, int token, int attempt) {
        if (token != binderCheckToken) {
            return;
        }
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        if (ShizukuManager.isBinderReady()) {
            dismissConnectingToast();
            handleShizukuReady(activity);
            return;
        }
        if (attempt < BINDER_RETRY_COUNT) {
            MAIN_HANDLER.postDelayed(() -> awaitBinder(activity, token, attempt + 1),
                    BINDER_RETRY_DELAY_MS);
            return;
        }
        dismissConnectingToast();
        handleShizukuUnavailable(activity);
    }

    private static void handleShizukuReady(@NonNull Activity activity) {
        if (!ShizukuManager.isServiceAvailable()) {
            showStartShizukuDialog(activity);
            return;
        }
        if (!ShizukuManager.hasPermission()) {
            if (ShizukuManager.requestPermission()) {
                Toast.makeText(activity, R.string.shizuku_permission_message,
                        Toast.LENGTH_SHORT).show();
            } else {
                showStartShizukuDialog(activity);
            }
            return;
        }
        ShizukuManager.bindUserService();
    }

    private static void handleShizukuUnavailable(@NonNull Activity activity) {
        if (ShizukuManager.getInstalledVersionName(activity) == null) {
            showInstallShizukuDialog(activity);
            return;
        }
        showStartShizukuDialog(activity);
    }

    private static void showInstallShizukuDialog(@NonNull Activity activity) {
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.shizuku_not_installed_title)
                .setIcon(R.drawable.icon)
                .setMessage(R.string.shizuku_not_installed_message)
                .setPositiveButton(R.string.download_shizuku,
                        (dialog, which) -> LinkUtils.open(activity,
                                activity.getString(R.string.shizukuDownloadUrl)))
                .show();
    }

    private static void showStartShizukuDialog(@NonNull Activity activity) {
        dismissConnectingToast();
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.shizuku_not_running_title)
                .setIcon(R.drawable.icon)
                .setMessage(R.string.shizuku_not_running_message)
                .setPositiveButton(R.string.open_shizuku, (dialog, which) -> {
                    dialog.dismiss();
                    if (ShizukuManager.openShizukuApp(activity)) {
                        pendingShizukuReturn = true;
                    } else {
                        Toast.makeText(activity, R.string.shizuku_app_not_found,
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton(R.string.go_instructions,
                        (dialog, which) -> LinkUtils.open(activity,
                                activity.getString(R.string.instructionsUrl)))
                .show();
    }
}

package com.QQBrowsernovelswitch.core;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.QQBrowsernovelswitch.BuildConfig;
import com.QQBrowsernovelswitch.R;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import rikka.shizuku.Shizuku;

public final class ShizukuManager {

    private static final String TAG = "ShizukuManager";

    public static final int REQUEST_CODE_PERMISSION = 0x5348;
    
    private static final String[] SHIZUKU_PACKAGES = {
            "moe.shizuku.privileged.api",
            "moe.shizuku.manager"
    };

    private static final String SHIZUKU_MAIN_ACTIVITY = "moe.shizuku.manager.MainActivity";

    private static final String USER_SERVICE_PROCESS_SUFFIX = "shizuku";

    private static final String USER_SERVICE_TAG = "QQBrowserNovelSwitchUserService";

    private static final int USER_SERVICE_VERSION = 8;

    private static final long BIND_TIMEOUT_MS = 8000;

    private static final long PERMISSION_CACHE_MS = 1500;

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface OnServiceReadyListener {

        void onServiceReady();
    }

    private static final List<OnServiceReadyListener> SERVICE_READY_LISTENERS =
            new CopyOnWriteArrayList<>();

    private static boolean initialized;
    private static Context appContext;
    private static IUserService userService;
    private static boolean userServiceBound;
    private static boolean binding;
    private static Runnable bindTimeoutRunnable;
    private static boolean permissionCached;
    private static long permissionCachedAt;

    private static final Shizuku.UserServiceArgs USER_SERVICE_ARGS =
            new Shizuku.UserServiceArgs(
                    new ComponentName(BuildConfig.APPLICATION_ID, ShizukuUserService.class.getName()))
                    .daemon(false)
                    .processNameSuffix(USER_SERVICE_PROCESS_SUFFIX)
                    .version(USER_SERVICE_VERSION)
                    .debuggable(BuildConfig.DEBUG)
                    .tag(USER_SERVICE_TAG);

    private static final Shizuku.OnBinderReceivedListener BINDER_RECEIVED_LISTENER = () -> {
        Log.i(TAG, "Shizuku binder received, version=" + getServerVersion());
        invalidatePermissionCache();
        if (hasPermission()) {
            bindUserService();
        }
    };

    private static final Shizuku.OnBinderDeadListener BINDER_DEAD_LISTENER = () -> {
        Log.w(TAG, "Shizuku binder dead");
        binding = false;
        cancelBindTimeout();
        invalidatePermissionCache();
        userService = null;
        userServiceBound = false;
    };

    private static final Shizuku.OnRequestPermissionResultListener PERMISSION_RESULT_LISTENER =
            (requestCode, grantResult) -> {
                if (requestCode != REQUEST_CODE_PERMISSION) {
                    return;
                }
                boolean granted = grantResult == PackageManager.PERMISSION_GRANTED;
                Log.i(TAG, "permission result: granted=" + granted);
                permissionCached = granted;
                permissionCachedAt = granted ? SystemClock.elapsedRealtime() : 0;
                if (granted) {
                    bindUserService();
                } else {
                    toastRes(R.string.shizuku_permission_denied);
                }
            };

    private static final ServiceConnection USER_SERVICE_CONNECTION = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            if (binder == null || !binder.pingBinder()) {
                Log.w(TAG, "invalid binder for " + name);
                return;
            }
            IUserService service = IUserService.Stub.asInterface(binder);
            if (userServiceBound && userService != null
                    && userService.asBinder() == service.asBinder()) {
                binding = false;
                cancelBindTimeout();
                return;
            }
            userService = service;
            userServiceBound = true;
            binding = false;
            cancelBindTimeout();
            Log.i(TAG, "UserService connected: " + name);
            for (OnServiceReadyListener listener : SERVICE_READY_LISTENERS) {
                try {
                    listener.onServiceReady();
                } catch (Throwable t) {
                    Log.w(TAG, "service ready listener failed", t);
                }
            }
            SERVICE_READY_LISTENERS.clear();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "UserService disconnected: " + name);
            binding = false;
            cancelBindTimeout();
            userService = null;
            userServiceBound = false;
        }
    };

    private ShizukuManager() {
    }

    public static void init(@NonNull Context context) {
        synchronized (ShizukuManager.class) {
            if (initialized) {
                return;
            }
            initialized = true;
            appContext = context.getApplicationContext();
            Shizuku.addBinderReceivedListenerSticky(BINDER_RECEIVED_LISTENER);
            Shizuku.addBinderDeadListener(BINDER_DEAD_LISTENER);
            Shizuku.addRequestPermissionResultListener(PERMISSION_RESULT_LISTENER);
            Log.i(TAG, "initialized, binder ready=" + isBinderReady());
        }
    }

    public static boolean isBinderReady() {
        return Shizuku.pingBinder();
    }

    public static boolean isServiceAvailable() {
        if (!isBinderReady()) {
            return false;
        }
        try {
            Shizuku.getSELinuxContext();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isSupported() {
        return isBinderReady() && !Shizuku.isPreV11();
    }

    public static boolean hasPermission() {
        if (!isSupported()) {
            return false;
        }
        if (permissionCached
                && SystemClock.elapsedRealtime() - permissionCachedAt < PERMISSION_CACHE_MS) {
            return true;
        }
        try {
            boolean granted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
            permissionCached = granted;
            permissionCachedAt = granted ? SystemClock.elapsedRealtime() : 0;
            return granted;
        } catch (Throwable t) {
            Log.w(TAG, "checkSelfPermission failed", t);
            permissionCached = false;
            permissionCachedAt = 0;
            return false;
        }
    }

    public static boolean isReady() {
        return userServiceBound && userService != null;
    }

    @Nullable
    public static IUserService getUserService() {
        return userService;
    }

    public static void addOnServiceReadyListener(@NonNull OnServiceReadyListener listener) {
        if (isReady()) {
            listener.onServiceReady();
            return;
        }
        if (!SERVICE_READY_LISTENERS.contains(listener)) {
            SERVICE_READY_LISTENERS.add(listener);
        }
    }

    public static void removeOnServiceReadyListener(@NonNull OnServiceReadyListener listener) {
        SERVICE_READY_LISTENERS.remove(listener);
    }

    @Nullable
    private static Intent findShizukuLaunchIntent(@NonNull Context context) {
        PackageManager pm = context.getPackageManager();
        for (String packageName : SHIZUKU_PACKAGES) {
            Intent intent = pm.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                return intent;
            }
        }
        return null;
    }

    @Nullable
    public static String getInstalledVersionName(@NonNull Context context) {
        PackageManager packageManager = context.getPackageManager();
        for (String packageName : SHIZUKU_PACKAGES) {
            try {
                PackageInfo info = packageManager.getPackageInfo(packageName, 0);
                if (info != null) {
                    return info.versionName;
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return null;
    }

    public static boolean openShizukuApp(@NonNull Context context) {
        Intent intent = findShizukuLaunchIntent(context);
        if (intent != null) {
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            try {
                context.startActivity(intent);
                return true;
            } catch (Throwable t) {
                Log.w(TAG, "openShizukuApp failed", t);
            }
        }

        for (String packageName : SHIZUKU_PACKAGES) {
            try {
                Intent explicit = new Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setClassName(packageName, SHIZUKU_MAIN_ACTIVITY);
                if (!(context instanceof Activity)) {
                    explicit.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                context.startActivity(explicit);
                return true;
            } catch (Throwable ignored) {
            }
        }
        return false;
    }

    public static boolean requestPermission() {
        if (!isSupported()) {
            return false;
        }
        try {
            Shizuku.requestPermission(REQUEST_CODE_PERMISSION);
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "requestPermission failed", t);
            return false;
        }
    }

    public static void bindUserService() {
        if (!isSupported() || !hasPermission()) {
            Log.w(TAG, "bindUserService skipped: not supported or no permission");
            return;
        }
        if (userServiceBound || binding) {
            return;
        }
        binding = true;
        cancelBindTimeout();
        bindTimeoutRunnable = () -> {
            if (!userServiceBound) {
                binding = false;
                bindTimeoutRunnable = null;
                toastRes(R.string.shizuku_permission_denied);
            }
        };
        MAIN_HANDLER.postDelayed(bindTimeoutRunnable, BIND_TIMEOUT_MS);
        try {
            Shizuku.bindUserService(USER_SERVICE_ARGS, USER_SERVICE_CONNECTION);
            Log.i(TAG, "bindUserService requested");
        } catch (Throwable t) {
            binding = false;
            cancelBindTimeout();
            Log.e(TAG, "bindUserService failed", t);
            toastRes(R.string.shizuku_permission_denied);
        }
    }

    public static int getServerVersion() {
        try {
            return Shizuku.getVersion();
        } catch (Throwable t) {
            return -1;
        }
    }

    private static void cancelBindTimeout() {
        if (bindTimeoutRunnable != null) {
            MAIN_HANDLER.removeCallbacks(bindTimeoutRunnable);
            bindTimeoutRunnable = null;
        }
    }

    private static void invalidatePermissionCache() {
        permissionCached = false;
        permissionCachedAt = 0;
    }

    private static void toastRes(int resId) {
        Context context = appContext;
        if (context != null) {
            toast(context.getString(resId));
        }
    }

    private static void toast(@NonNull String message) {
        Context context = appContext;
        if (context == null) {
            return;
        }
        try {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Log.w(TAG, "toast failed", t);
        }
    }
}

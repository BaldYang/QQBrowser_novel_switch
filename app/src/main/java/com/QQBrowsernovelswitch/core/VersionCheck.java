package com.QQBrowsernovelswitch.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class VersionCheck {

    public static String getAppVersionName(Context context) {
        return getAppVersionName(context, context.getPackageName());
    }

    public static String getAppVersionName(Context context, String packageName) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(
                    packageName,
                    PackageManager.GET_CONFIGURATIONS
            );
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "N/A";
        }
    }
}

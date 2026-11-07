package com.QQBrowsernovelswitch.core;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.QQBrowsernovelswitch.R;

public final class LinkUtils {

    private LinkUtils() {
    }

    public static void open(@NonNull Context context, @Nullable String url) {
        if (url == null || url.trim().isEmpty()) {
            return;
        }
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_app_for_link, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(context, R.string.cannot_open_link, Toast.LENGTH_SHORT).show();
        }
    }
}

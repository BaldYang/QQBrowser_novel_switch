package com.QQBrowsernovelswitch.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.QQBrowsernovelswitch.R;
import com.QQBrowsernovelswitch.core.LinkUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class Instructions {

    private final Context context;

    public Instructions(@NonNull Fragment fragment) {
        this.context = fragment.requireContext();
    }

    public Instructions(@NonNull Activity activity) {
        if (!(activity instanceof FragmentActivity)) {
            throw new IllegalArgumentException("Activity must be a FragmentActivity");
        }
        this.context = activity;
    }

    public void showInstructionsDialog() {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.action_instructions)
                .setMessage(R.string.instructions_message)
                .setIcon(R.drawable.icon)
                .setPositiveButton(R.string.instructions_text,
                        (dialog, which) -> LinkUtils.open(context, context.getString(R.string.instructionsUrl)))
                .setNegativeButton(R.string.instructions_video,
                        (dialog, which) -> context.startActivity(new Intent(context, VideosActivity.class)))
                .setCancelable(true)
                .show();
    }

    public static class VideoPreferenceFragment extends BasePreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.videos_preferences, null);
            onUrlClick("bilibili", R.string.bilibiliUrl);
            onUrlClick("weibo", R.string.weiboUrl);
            onUrlClick("douyin", R.string.douyinUrl);
            onUrlClick("rednote", R.string.rednoteUrl);
            onUrlClick("wechatvideo", R.string.wechatvideoUrl);
        }
    }
}

package com.QQBrowsernovelswitch.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.QQBrowsernovelswitch.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class Reward {

    private final SettingsActivity.SettingsFragment fragment;

    public Reward(SettingsActivity.SettingsFragment fragment) {
        this.fragment = fragment;
    }

    public void setupRewardPreference(Preference reward) {
        reward.setOnPreferenceClickListener(preference -> {
            showRewardDialog(fragment.requireContext());
            return true;
        });
    }

    public static void showRewardDialog(@NonNull Context context) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.action_reward_author)
                .setMessage(R.string.reward_message)
                .setIcon(R.drawable.icon)
                .setCancelable(true)
                .setPositiveButton(R.string.alipay, (dialog, which) ->
                        showPayQrDialog(context, context.getString(R.string.alipay),
                                R.layout.alipay, R.id.alipay, R.drawable.alipay))
                .setNegativeButton(R.string.wechat_pay, (dialog, which) ->
                        showPayQrDialog(context, context.getString(R.string.wechat_pay),
                                R.layout.wechatpay, R.id.wechatpay, R.drawable.wechatpay))
                .show();
    }

    private static void showPayQrDialog(Context context, String title, @LayoutRes int layoutRes,
                                        @IdRes int imageViewId, @DrawableRes int imageRes) {
        View view = LayoutInflater.from(context).inflate(layoutRes, null);
        ImageView image = view.findViewById(imageViewId);
        image.setImageResource(imageRes);
        new MaterialAlertDialogBuilder(context)
                .setTitle(title)
                .setIcon(R.drawable.icon)
                .setView(view)
                .setCancelable(true)
                .show();
    }
}

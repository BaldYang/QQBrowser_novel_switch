package com.QQBrowsernovelswitch.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.QQBrowsernovelswitch.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AuthorActivity extends BasePreferenceActivity {

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_preference_host;
    }

    @Override
    protected int getContainerViewId() {
        return R.id.preference_container;
    }

    @NonNull
    @Override
    protected Fragment createFragment() {
        return new AuthorPreferenceFragment();
    }

    public static class AuthorPreferenceFragment extends BasePreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.author_preferences, null);

            onUrlClick("QQGroup", R.string.QQGroupUrl);
            onClick("wechat", this::showWechatQrDialog);
            onClick("mail", this::sendMail);
        }

        private void showWechatQrDialog() {
            View wechatQr = LayoutInflater.from(requireContext()).inflate(R.layout.wechat_qr, null);
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.wechat_qr_title)
                    .setIcon(R.drawable.icon)
                    .setView(wechatQr)
                    .setCancelable(true)
                    .show();
        }

        private void sendMail() {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"measureyc@outlook.com"});
            try {
                startActivity(Intent.createChooser(emailIntent,
                        getString(R.string.mail_chooser)));
            } catch (ActivityNotFoundException e) {
                Toast.makeText(requireContext(), R.string.no_mail_app,
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}

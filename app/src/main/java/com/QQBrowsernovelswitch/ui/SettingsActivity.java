package com.QQBrowsernovelswitch.ui;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import com.QQBrowsernovelswitch.R;
import com.QQBrowsernovelswitch.core.Announces;
import com.QQBrowsernovelswitch.core.LinkUtils;
import com.QQBrowsernovelswitch.core.Update;
import com.QQBrowsernovelswitch.core.VersionCheck;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class SettingsActivity extends BasePreferenceActivity {

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
        return new SettingsFragment();
    }

    public static class SettingsFragment extends BasePreferenceFragment {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            onClick("instructions", () -> new Instructions(this).showInstructionsDialog());
            onClick("author", () -> startActivity(new Intent(requireContext(), AuthorActivity.class)));
            onClick("announces", () -> Announces.checkAnnounces(requireActivity(), "SettingsActivity"));
            onClick("update", () -> Update.checkUpdate(requireActivity(), "SettingsActivity"));
            onUrlClick("feedback", R.string.feedbackUrl);

            Preference reward = pref();
            if (reward != null) {
                new Reward(this).setupRewardPreference(reward);
            }

            onClick("shareWeb", this::shareWebsite);
            onClick("API", () -> startActivity(new Intent(requireContext(), APIActivity.class)));
            onClick("about", this::showAboutDialog);
        }

        private void shareWebsite() {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.webUrl));
            Intent chooser = Intent.createChooser(shareIntent,
                    getString(R.string.share_website_chooser, getString(R.string.app_name)));
            if (requireActivity().getPackageManager()
                    .resolveActivity(shareIntent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                startActivity(chooser);
            }
        }

        private void showAboutDialog() {
            String aboutMessage = getString(R.string.about_message,
                    VersionCheck.getAppVersionName(requireContext()));
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.about_title, getString(R.string.app_name)))
                    .setIcon(R.drawable.icon)
                    .setMessage(aboutMessage)
                    .setPositiveButton("GitHub", (dialog, which) ->
                            LinkUtils.open(requireContext(), getString(R.string.githubUrl)))
                    .setNegativeButton("Gitee", (dialog, which) ->
                            LinkUtils.open(requireContext(), getString(R.string.giteeUrl)))
                    .setNeutralButton(R.string.about_website, (dialog, which) ->
                            LinkUtils.open(requireContext(), getString(R.string.webUrl)))
                    .show();
        }
    }
}

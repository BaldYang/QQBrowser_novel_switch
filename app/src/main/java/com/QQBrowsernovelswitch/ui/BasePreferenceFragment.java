package com.QQBrowsernovelswitch.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.QQBrowsernovelswitch.core.LinkUtils;

public abstract class BasePreferenceFragment extends PreferenceFragmentCompat {

    @Nullable
    protected final Preference pref() {
        return findPreference("reward");
    }

    protected final void onClick(@NonNull String key, @NonNull Runnable action) {
        Preference preference = findPreference(key);
        if (preference == null) {
            return;
        }
        preference.setOnPreferenceClickListener(pref -> {
            action.run();
            return true;
        });
    }

    protected final void onUrlClick(@NonNull String key, @StringRes int urlRes) {
        onClick(key, () -> LinkUtils.open(requireContext(), getString(urlRes)));
    }
}

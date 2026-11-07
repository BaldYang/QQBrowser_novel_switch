package com.QQBrowsernovelswitch.ui;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.QQBrowsernovelswitch.R;

public class VideosActivity extends BasePreferenceActivity {

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
        return new Instructions.VideoPreferenceFragment();
    }
}

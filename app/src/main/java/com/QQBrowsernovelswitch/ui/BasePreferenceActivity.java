package com.QQBrowsernovelswitch.ui;

import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public abstract class BasePreferenceActivity extends AppCompatActivity {

    @LayoutRes
    protected abstract int getLayoutRes();

    @IdRes
    protected abstract int getContainerViewId();

    @NonNull
    protected abstract Fragment createFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutRes());
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(getContainerViewId(), createFragment())
                    .commit();
        }
    }
}

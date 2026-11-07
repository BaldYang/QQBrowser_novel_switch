package com.QQBrowsernovelswitch.core;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class NovelViewModel extends ViewModel {

    private final MutableLiveData<List<NovelRepository.Novel>> novels = new MutableLiveData<>();

    private boolean loaded;
    private boolean loading;

    public LiveData<List<NovelRepository.Novel>> getNovels() {
        return novels;
    }

    public boolean ensureLoaded(@NonNull Context context) {
        if (loaded || loading) {
            return false;
        }
        return startLoading(context);
    }

    public boolean refresh(@NonNull Context context) {
        if (loading) {
            return false;
        }
        return startLoading(context);
    }

    private boolean startLoading(@NonNull Context context) {
        boolean started = NovelRepository.load(context.getApplicationContext(), result -> {
            loading = false;
            loaded = true;
            novels.setValue(result);
        });
        if (started) {
            loading = true;
        }
        return started;
    }
}

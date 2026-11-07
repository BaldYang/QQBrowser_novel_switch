package com.QQBrowsernovelswitch.core;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ConversionViewModel extends ViewModel {

    private final MutableLiveData<Integer> progress = new MutableLiveData<>();
    private final MutableLiveData<NovelConverter.Result> result = new MutableLiveData<>();
    private final MutableLiveData<String> failure = new MutableLiveData<>();
    private final MutableLiveData<Boolean> cancelled = new MutableLiveData<>();

    private NovelConverter converter;
    private boolean running;
    private String novelName;
    private Context appContext;
    private int lastNotifiedProgress = -1;
    private boolean resultConsumed;
    private boolean failureConsumed;

    public LiveData<Integer> getProgress() {
        return progress;
    }

    public LiveData<NovelConverter.Result> getResult() {
        return result;
    }

    public LiveData<String> getFailure() {
        return failure;
    }

    public LiveData<Boolean> getCancelled() {
        return cancelled;
    }

    public boolean isRunning() {
        return running;
    }

    @Nullable
    public String getNovelName() {
        return novelName;
    }

    public void start(@NonNull Context context, @NonNull NovelRepository.Novel novel) {
        if (running) {
            return;
        }
        running = true;
        novelName = novel.name;
        appContext = context.getApplicationContext();
        lastNotifiedProgress = -1;
        try {
            ConversionService.start(appContext, novel.name);
        } catch (Throwable ignored) {
        }
        progress.setValue(1);
        result.setValue(null);
        failure.setValue(null);
        cancelled.setValue(null);
        resultConsumed = false;
        failureConsumed = false;
        converter = new NovelConverter(context, novel, new NovelConverter.Listener() {
            @Override
            public void onProgress(int percent) {
                progress.setValue(percent);
                if (percent != lastNotifiedProgress) {
                    lastNotifiedProgress = percent;
                    try {
                        ConversionService.update(appContext, novelName, percent);
                    } catch (Throwable ignored) {
                    }
                }
            }

            @Override
            public void onSuccess(@NonNull NovelConverter.Result value) {
                running = false;
                stopService();
                result.setValue(value);
            }

            @Override
            public void onFailure(@NonNull String message) {
                running = false;
                stopService();
                failure.setValue(message);
            }

            @Override
            public void onCancelled() {
                running = false;
                stopService();
                cancelled.setValue(true);
            }
        });
        converter.start();
    }

    public void cancel() {
        if (converter != null) {
            converter.cancel();
        }
    }

    public boolean takeResult() {
        if (result.getValue() == null || resultConsumed) {
            return false;
        }
        resultConsumed = true;
        return true;
    }

    public boolean takeFailure() {
        if (failure.getValue() == null || failureConsumed) {
            return false;
        }
        failureConsumed = true;
        return true;
    }

    private void stopService() {
        try {
            ConversionService.stop(appContext);
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onCleared() {
        if (converter != null) {
            converter.cancel();
        }
        stopService();
        super.onCleared();
    }
}

package com.QQBrowsernovelswitch.core;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class NovelRepository {

    private static final int MAX_BASIC_BYTES = 256 * 1024;
    private static final long WAIT_TIMEOUT_MS = 10000;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Object WAIT_LOCK = new Object();

    private static boolean waitingForService;
    private static volatile boolean loading;
    private static volatile Callback activeCallback;
    private static Callback pendingCallback;

    public static final class Novel {

        public final String id;
        public final String name;
        public final boolean finished;
        public final int chapterNum;
        public final long modified;

        Novel(String id, String name, boolean finished, int chapterNum, long modified) {
            this.id = id;
            this.name = name;
            this.finished = finished;
            this.chapterNum = chapterNum;
            this.modified = modified;
        }
    }

    public interface Callback {

        void onLoaded(@NonNull List<Novel> novels);
    }

    private NovelRepository() {
    }

    public static boolean load(@NonNull Context context, @NonNull Callback callback) {
        activeCallback = callback;
        if (loading) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!ShizukuManager.isReady()) {
                synchronized (WAIT_LOCK) {
                    pendingCallback = callback;
                    if (waitingForService) {
                        return false;
                    }
                    waitingForService = true;
                }
                ShizukuManager.OnServiceReadyListener listener = () -> {
                    Callback pending;
                    synchronized (WAIT_LOCK) {
                        waitingForService = false;
                        pending = pendingCallback;
                        pendingCallback = null;
                    }
                    if (pending != null) {
                        load(context, pending);
                    }
                };
                ShizukuManager.addOnServiceReadyListener(listener);
                MAIN_HANDLER.postDelayed(() -> {
                    Callback pending = null;
                    synchronized (WAIT_LOCK) {
                        if (!waitingForService) {
                            return;
                        }
                        waitingForService = false;
                        pending = pendingCallback;
                        pendingCallback = null;
                    }
                    ShizukuManager.removeOnServiceReadyListener(listener);
                    if (pending != null) {
                        pending.onLoaded(new ArrayList<>());
                    }
                }, WAIT_TIMEOUT_MS);
                return true;
            }
        }

        loading = true;
        final Context appContext = context.getApplicationContext();
        new Thread(() -> {
            List<Novel> novels = new ArrayList<>();
            try {
                novels = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                        ? readWithService(ShizukuManager.getUserService())
                        : readDirect(appContext);
            } catch (Throwable ignored) {
            } finally {
                loading = false;
            }
            final List<Novel> result = novels;
            MAIN_HANDLER.post(() -> {
                Callback callbackNow = activeCallback;
                if (callbackNow != null) {
                    callbackNow.onLoaded(result);
                }
            });
        }, "novel-repository").start();
        return true;
    }

    @NonNull
    private static List<Novel> readWithService(IUserService service) {
        List<Novel> novels = new ArrayList<>();
        if (service == null) {
            return novels;
        }
        String[] ids;
        try {
            ids = NovelFileAccess.listDir(service, NovelFileAccess.BOOK_DIR);
        } catch (Throwable t) {
            return novels;
        }
        if (ids == null) {
            return novels;
        }
        for (String id : ids) {
            if (id == null || !id.matches("\\d+")) {
                continue;
            }
            try {
                long modified = service.getLastModified(
                        NovelFileAccess.BOOK_DIR + "/" + id);
                String text = NovelFileAccess.readText(service,
                        NovelFileAccess.BOOK_DIR + "/" + id + "/basic.txt",
                        MAX_BASIC_BYTES);
                Novel novel = parse(id, text, modified);
                if (novel != null) {
                    novels.add(novel);
                }
            } catch (Throwable ignored) {
            }
        }
        sort(novels);
        return novels;
    }

    @NonNull
    private static List<Novel> readDirect(Context context) {
        List<Novel> novels = new ArrayList<>();
        if (context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            return novels;
        }
        File baseDir = new File(NovelFileAccess.BOOK_DIR);
        File[] dirs = baseDir.listFiles();
        if (dirs == null) {
            return novels;
        }
        for (File dir : dirs) {
            if (!dir.isDirectory() || !dir.getName().matches("\\d+")) {
                continue;
            }
            Novel novel = parse(dir.getName(),
                    NovelFileAccess.readText(new File(dir, "basic.txt"), MAX_BASIC_BYTES),
                    dir.lastModified());
            if (novel != null) {
                novels.add(novel);
            }
        }
        sort(novels);
        return novels;
    }

    private static void sort(@NonNull List<Novel> novels) {
        novels.sort((left, right) -> {
            int byTime = Long.compare(right.modified, left.modified);
            if (byTime != 0) {
                return byTime;
            }
            return Long.compare(parseLong(right.id), parseLong(left.id));
        });
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Novel parse(String id, String text, long modified) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String name = between(text, "\"name\":\"", "\"");
        if (name.isEmpty()) {
            return null;
        }
        String chapterNumText = between(text, "\"chapterNum\":", ",");
        String isFinish = between(text, "\"isFinish\":", ",");
        int chapterNum = 0;
        try {
            chapterNum = Integer.parseInt(chapterNumText.trim());
        } catch (NumberFormatException ignored) {
        }
        boolean finished = "true".equalsIgnoreCase(isFinish.trim());
        return new Novel(id, name, finished, chapterNum, modified);
    }

    private static String between(String source, String start, String end) {
        int from = source.indexOf(start);
        if (from < 0) {
            return "";
        }
        from += start.length();
        int to = source.indexOf(end, from);
        if (to < 0) {
            return "";
        }
        return source.substring(from, to).trim();
    }
}

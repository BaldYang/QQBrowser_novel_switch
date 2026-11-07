package com.QQBrowsernovelswitch.core;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.QQBrowsernovelswitch.R;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NovelConverter {

    public interface Listener {

        void onProgress(int percent);

        void onSuccess(@NonNull Result result);

        void onFailure(@NonNull String message);

        void onCancelled();
    }

    public static final class Result {

        public final File shareFile;
        public final String savedPath;
        public final String author;
        public final int chapterCount;
        public final double seconds;

        Result(File shareFile, String savedPath, String author, int chapterCount, double seconds) {
            this.shareFile = shareFile;
            this.savedPath = savedPath;
            this.author = author;
            this.chapterCount = chapterCount;
            this.seconds = seconds;
        }
    }

    private static final String TAG = "NovelConverter";
    private static final String OUTPUT_BASE_DIR = "QQBrowser_novel_switch/novel";

    private final Context context;
    private final NovelRepository.Novel novel;
    private final Listener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile boolean cancelled;

    public NovelConverter(@NonNull Context context, @NonNull NovelRepository.Novel novel,
                          @NonNull Listener listener) {
        this.context = context.getApplicationContext();
        this.novel = novel;
        this.listener = listener;
    }

    public void start() {
        new Thread(this::runConversion, "novel-converter").start();
    }

    public void cancel() {
        cancelled = true;
    }

    private void runConversion() {
        long startTime = SystemClock.elapsedRealtime();
        IUserService service = null;
        File tempFile = null;
        try {
            boolean useShizuku = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
            service = useShizuku ? ShizukuManager.getUserService() : null;
            if (useShizuku && service == null) {
                throw new IllegalStateException();
            }
            if (!useShizuku && ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                throw new IllegalStateException();
            }

            String chapterDir = NovelFileAccess.BOOK_DIR + "/" + novel.id + "/chapter";
            List<String> chapterFiles = new ArrayList<>();
            for (String name : NovelFileAccess.listDir(service, chapterDir)) {
                if (name != null && name.toLowerCase(Locale.ROOT).endsWith(".txt")) {
                    chapterFiles.add(name);
                }
            }
            if (chapterFiles.isEmpty()) {
                throw new IllegalStateException();
            }
            chapterFiles.sort((left, right) -> {
                Integer leftNumber = numericName(left);
                Integer rightNumber = numericName(right);
                if (leftNumber == null && rightNumber == null) {
                    return left.compareTo(right);
                }
                if (leftNumber == null) {
                    return 1;
                }
                if (rightNumber == null) {
                    return -1;
                }
                return Integer.compare(leftNumber, rightNumber);
            });

            String safeName = sanitize(novel.name);
            String author = readAuthor(service, chapterDir, chapterFiles);
            if (author.isEmpty()) {
                author = context.getString(R.string.unknown_author);
            }
            String status = context.getString(novel.finished
                    ? R.string.status_finished : R.string.status_ongoing);
            String folderName = safeName + "（" + sanitize(status) + "）";

            File outputDir = new File(Environment.getExternalStorageDirectory(),
                    OUTPUT_BASE_DIR + "/" + folderName);
            File targetFile = new File(outputDir, safeName + ".txt");
            tempFile = new File(outputDir, safeName + ".tmp");

            String header = novel.name + "\n" + author + "\n\n";
            int converted = 0;
            boolean first = true;
            int total = chapterFiles.size();
            for (String chapterFile : chapterFiles) {
                if (cancelled) {
                    throw new CancelledException();
                }
                String json = NovelFileAccess.readText(service,
                        chapterDir + "/" + chapterFile);
                if (json.isEmpty()) {
                    throw new IllegalStateException();
                }
                NovelChapterParser.Chapter chapter = NovelChapterParser.parse(
                        json, stripExtension(chapterFile));
                String block = chapter.title + "\n" + chapter.body;
                String text = first ? header + block : "\n\n\n" + block;
                if (!NovelFileAccess.writeText(service, tempFile.getAbsolutePath(),
                        text, !first)) {
                    throw new IllegalStateException();
                }
                first = false;
                converted++;
                int progress = 1 + (int) (converted * 99L / total);
                postProgress(Math.min(progress, 100));
            }
            if (converted == 0 || converted != total) {
                throw new IllegalStateException();
            }
            if (cancelled) {
                throw new CancelledException();
            }
            if (!NovelFileAccess.rename(service, tempFile, targetFile)) {
                throw new IllegalStateException();
            }
            tempFile = null;

            File shareDir = new File(context.getExternalFilesDir(null), "shared");
            if (!shareDir.exists() && !shareDir.mkdirs()) {
                throw new IllegalStateException();
            }
            File shareFile = new File(shareDir, safeName + ".txt");
            if (!NovelFileAccess.copy(service, targetFile, shareFile)) {
                shareFile = targetFile;
            }

            postProgress(100);
            double seconds = (SystemClock.elapsedRealtime() - startTime) / 1000.0;
            Result result = new Result(shareFile, targetFile.getAbsolutePath(),
                    author, converted, seconds);
            mainHandler.post(() -> listener.onSuccess(result));
        } catch (CancelledException e) {
            cleanup(service, tempFile);
            mainHandler.post(listener::onCancelled);
        } catch (Throwable t) {
            Log.e(TAG, "conversion failed", t);
            cleanup(service, tempFile);
            mainHandler.post(() -> listener.onFailure(
                    context.getString(R.string.convert_failed_unknown)));
        }
    }

    private void cleanup(IUserService service, File tempFile) {
        NovelFileAccess.delete(service, tempFile);
    }

    private void postProgress(int percent) {
        mainHandler.post(() -> listener.onProgress(percent));
    }

    private String readAuthor(IUserService service, String chapterDir,
                              List<String> chapterFiles) {
        for (String chapterFile : chapterFiles) {
            try {
                String json = NovelFileAccess.readText(service,
                        chapterDir + "/" + chapterFile);
                if (json.isEmpty()) {
                    continue;
                }
                JSONObject object = new JSONObject(json);
                return NovelChapterParser.extractAuthor(object);
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    private static Integer numericName(String fileName) {
        try {
            return Integer.parseInt(stripExtension(fileName));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String sanitize(String name) {
        String safe = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return safe.isEmpty() ? "novel" : safe;
    }

    private static final class CancelledException extends Exception {
    }
}

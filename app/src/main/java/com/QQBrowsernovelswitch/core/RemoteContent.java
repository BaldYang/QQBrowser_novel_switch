package com.QQBrowsernovelswitch.core;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RemoteContent {

    private static final String TAG = "RemoteContent";
    private static final int CONNECT_TIMEOUT_MS = 8000;
    private static final int READ_TIMEOUT_MS = 10000;
    private static final int MAX_RESPONSE_CHARS = 512 * 1024;
    private static final int MAX_ATTEMPTS = 2;
    private static final long RETRY_DELAY_MS = 300;
    private static final String USER_AGENT = "QQBrowserNovelSwitch";

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Set<String> LOADING = Collections.synchronizedSet(new HashSet<>());

    public interface Callback {

        void onSuccess(@NonNull String content);

        void onFailure(@NonNull Exception error);
    }

    private RemoteContent() {
    }

    public static void load(@NonNull Context context, @StringRes int urlRes,
                            @NonNull Callback callback) {
        loadInternal(context.getString(urlRes), callback);
    }

    private static void loadInternal(@NonNull final String urlString,
                                     @NonNull final Callback callback) {
        if (!LOADING.add(urlString)) {
            return;
        }
        new Thread(() -> {
            Exception lastError = null;
            try {
                for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                    try {
                        final String result = fetch(urlString);
                        MAIN_HANDLER.post(() -> callback.onSuccess(result));
                        return;
                    } catch (Exception e) {
                        lastError = e;
                        Log.w(TAG, "load failed (attempt " + (attempt + 1) + "): " + urlString, e);
                        if (attempt + 1 < MAX_ATTEMPTS) {
                            try {
                                Thread.sleep(RETRY_DELAY_MS);
                            } catch (InterruptedException ignored) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
                final Exception error = lastError;
                MAIN_HANDLER.post(() -> callback.onFailure(error));
            } finally {
                LOADING.remove(urlString);
            }
        }, "remote-content").start();
    }

    @NonNull
    private static String fetch(@NonNull String urlString) throws IOException {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("unexpected HTTP status: " + responseCode);
            }

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append('\n');
                    if (content.length() > MAX_RESPONSE_CHARS) {
                        throw new IOException("response too large");
                    }
                }
            }
            return content.toString();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @NonNull
    public static String field(@NonNull String content, @NonNull String key) {
        Pattern pattern = Pattern.compile(Pattern.quote(key) + "(.*?)" + Pattern.quote("】"),
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return "";
        }
        return clean(matcher.group(1));
    }

    @NonNull
    private static String clean(@NonNull String value) {
        String text = value.replaceAll("(?i)<br\\s*/?>", "\n");
        text = text.replaceAll("<[^>]+>", " ");
        text = text.replace("&nbsp;", " ");
        text = text.replace("&amp;", "&");
        text = text.replace("&lt;", "<");
        text = text.replace("&gt;", ">");
        text = text.replace("&quot;", "\"");
        text = text.replace("&#39;", "'");
        text = text.replace("\r", "");
        String[] lines = text.split("\n", -1);
        StringBuilder builder = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(trimmed.replaceAll("[ \t]+", " "));
        }
        return builder.toString().trim();
    }
}

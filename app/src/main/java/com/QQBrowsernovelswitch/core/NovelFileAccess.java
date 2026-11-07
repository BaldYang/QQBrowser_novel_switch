package com.QQBrowsernovelswitch.core;

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class NovelFileAccess {

    private static final String TAG = "NovelFileAccess";

    public static final String BOOK_DIR =
            "/storage/emulated/0/Android/data/com.tencent.mtt/files/hippyBusiness/novel/book";
    public static final int MAX_TEXT_BYTES = 8 * 1024 * 1024;

    private static final int WRITE_CHUNK = 200_000;

    private NovelFileAccess() {
    }

    public static String[] listDir(IUserService service, String path) throws Exception {
        if (service != null) {
            return service.listDir(path);
        }
        File[] files = new File(path).listFiles();
        if (files == null) {
            return new String[0];
        }
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            names[i] = files[i].getName();
        }
        return names;
    }

    public static String readText(IUserService service, String path) throws Exception {
        return readText(service, path, MAX_TEXT_BYTES);
    }

    public static String readText(IUserService service, String path, int maxBytes) throws Exception {
        if (service != null) {
            return service.readTextFile(path);
        }
        return readText(new File(path), maxBytes);
    }

    public static String readText(File file, int maxBytes) {
        if (!file.isFile() || file.length() > maxBytes) {
            return "";
        }
        try (FileInputStream input = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            int total = 0;
            while (total < data.length) {
                int read = input.read(data, total, data.length - total);
                if (read < 0) {
                    break;
                }
                total += read;
            }
            return new String(data, 0, total, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean writeText(IUserService service, String path, String text, boolean append) {
        if (service != null) {
            try {
                boolean firstChunk = append;
                for (int offset = 0; offset < text.length(); offset += WRITE_CHUNK) {
                    int end = Math.min(offset + WRITE_CHUNK, text.length());
                    if (!service.writeTextFile(path, text.substring(offset, end), firstChunk)) {
                        return false;
                    }
                    firstChunk = true;
                }
                return true;
            } catch (Throwable t) {
                return false;
            }
        }
        File file = new File(path);
        if (!ensureParent(file)) {
            return false;
        }
        try (FileOutputStream output = new FileOutputStream(file, append)) {
            output.write(text.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean ensureParent(File file) {
        File parent = file.getParentFile();
        return parent == null || parent.exists() || parent.mkdirs();
    }

    public static boolean rename(IUserService service, File source, File target) {
        if (service != null) {
            try {
                return service.renameFile(source.getAbsolutePath(), target.getAbsolutePath());
            } catch (Throwable t) {
                return false;
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                Files.move(source.toPath(), target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (Exception ignored) {
            }
        }
        return source.renameTo(target);
    }

    public static boolean copy(IUserService service, File source, File target) {
        if (service != null) {
            try {
                return service.copyFile(source.getAbsolutePath(), target.getAbsolutePath());
            } catch (Throwable t) {
                return false;
            }
        }
        if (!ensureParent(target)) {
            return false;
        }
        try (FileInputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static void delete(IUserService service, File file) {
        if (file == null) {
            return;
        }
        if (service != null) {
            try {
                service.deleteFile(file.getAbsolutePath());
            } catch (Throwable ignored) {
            }
            return;
        }
        if (!file.delete()) {
            Log.w(TAG, "delete failed: " + file.getAbsolutePath());
        }
    }
}

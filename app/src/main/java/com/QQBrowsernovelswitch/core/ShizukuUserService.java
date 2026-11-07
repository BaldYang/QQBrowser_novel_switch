package com.QQBrowsernovelswitch.core;

import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class ShizukuUserService extends IUserService.Stub {

    private static final String TAG = "ShizukuUserService";

    public ShizukuUserService() {
        Log.i(TAG, "created: uid=" + Process.myUid() + ", pid=" + Process.myPid());
    }

    @Override
    public void destroy() {
        Log.i(TAG, "destroy, exiting");
        System.exit(0);
    }

    @Override
    public void exit() {
        destroy();
    }

    @Override
    public int getUid() {
        return Process.myUid();
    }

    @Override
    public String[] listDir(String path) {
        File directory = new File(path);
        File[] files = directory.listFiles();
        if (files == null) {
            return new String[0];
        }
        List<String> names = new ArrayList<>();
        for (File file : files) {
            names.add(file.getName());
        }
        return names.toArray(new String[0]);
    }

    @Override
    public long getLastModified(String path) {
        return new File(path).lastModified();
    }

    @Override
    public boolean writeTextFile(String path, String content, boolean append) {
        File file = new File(path);
        if (!ensureParent(file)) {
            return false;
        }
        try (FileOutputStream output = new FileOutputStream(file, append)) {
            output.write(content.getBytes(StandardCharsets.UTF_8));
            return true;
        } catch (Exception e) {
            Log.w(TAG, "writeTextFile failed: " + path, e);
            return false;
        }
    }

    @Override
    public boolean renameFile(String from, String to) {
        File source = new File(from);
        File target = new File(to);
        if (!ensureParent(target)) {
            return false;
        }
        try {
            Files.move(source.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (Exception atomicFailure) {
            try {
                Files.move(source.toPath(), target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                return true;
            } catch (Exception e) {
                Log.w(TAG, "renameFile failed: " + from + " -> " + to, e);
                return false;
            }
        }
    }

    @Override
    public boolean copyFile(String from, String to) {
        File source = new File(from);
        File target = new File(to);
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
            Log.w(TAG, "copyFile failed: " + from + " -> " + to, e);
            return false;
        }
    }

    private static boolean ensureParent(File file) {
        File parent = file.getParentFile();
        return parent == null || parent.exists() || parent.mkdirs();
    }

    @Override
    public boolean deleteFile(String path) {
        return new File(path).delete();
    }

    @Override
    public String readTextFile(String path) {
        return NovelFileAccess.readText(
                new File(path), NovelFileAccess.MAX_TEXT_BYTES);
    }
}

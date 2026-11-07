package com.QQBrowsernovelswitch.core;

interface IUserService {

    void destroy() = 16777114;

    void exit() = 1;

    int getUid() = 2;

    String[] listDir(String path) = 3;

    String readTextFile(String path) = 4;

    long getLastModified(String path) = 5;

    boolean writeTextFile(String path, String content, boolean append) = 6;

    boolean renameFile(String from, String to) = 7;

    boolean copyFile(String from, String to) = 8;

    boolean deleteFile(String path) = 9;
}

package io.github.xausky.unitymodmanager.utils;

/**
 * Created by xausky on 2018/2/2.
 */

public class ZipUtils {
    public static final int RESULT_STATE_OK = 0;
    public static final int RESULT_STATE_INTERNAL_ERROR = -1;
    public static final int RESULT_STATE_PASSWORD_ERROR = -2;
    public static final int RESULT_STATE_FILE_CONFLICT = -3;
    public static native int patchZip(String backupDir, String fusionDir, String prefix, String target);
    public static native int unzipFile(String zipFile, String targetDir, String password, boolean force);
    static{
        System.loadLibrary("ZipUtils");
    }
}
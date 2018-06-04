package io.github.xausky.unitymodmanager.utils;

/**
 * Created by xausky on 2018/2/2.
 */

public class NativeUtils {
    public static final int RESULT_STATE_OK = 0;
    public static final int RESULT_STATE_INTERNAL_ERROR = -1;
    public static final int RESULT_STATE_PASSWORD_ERROR = -2;
    public static final int RESULT_STATE_FILE_CONFLICT = -3;
    public static native int PatchApk(String input, String output, String mods);
    public static native int GenerateApkMapFile(String input, String output);
    public static native int PatchFolder(String target, String mods, String backup);
    public static native int GenerateFolderMapFile(String input, String output);
    static{
        System.loadLibrary("NativeUtils");
    }
}
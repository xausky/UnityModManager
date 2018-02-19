package io.github.xausky.bh3modmanager;

/**
 * Created by xausky on 2018/2/2.
 */

public class ZipUtils {
    public static native int patchZip(String backupDir, String fusionDir, String prefix, String target);
    public static native int unzipFile(String zipFile, String targetDir, String password);
    static{
        System.loadLibrary("ZipUtils");
    }
}

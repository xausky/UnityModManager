package io.github.xausky.bh3modmanager;

/**
 * Created by xausky on 2018/2/2.
 */

public class FusionZip {
    public static native int patchZip(String backupDir, String fusionDir, String prefix, String target);
    static{
        System.loadLibrary("zip");
    }
}

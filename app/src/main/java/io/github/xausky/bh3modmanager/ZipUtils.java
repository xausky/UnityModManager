package io.github.xausky.bh3modmanager;

import android.content.Context;
import android.os.Environment;

import com.topjohnwu.superuser.Shell;

import java.util.List;

/**
 * Created by xausky on 2018/2/2.
 */

public class ZipUtils {
    public static int patchZip(boolean root , Context context, String backupDir, String fusionDir, String prefix, String target){
        String execFilePath = context.getFilesDir().getAbsolutePath() + "/ZipPatch";
        List<String> result = null;
        if(root){
            Shell.setFlags(Shell.FLAG_REDIRECT_STDERR);
            result = Shell.Sync.su(execFilePath + " " + backupDir + " " + fusionDir + " " + prefix + " " + target);
        } else {
            Shell.setFlags(Shell.FLAG_NON_ROOT_SHELL | Shell.FLAG_REDIRECT_STDERR);
            result = Shell.Sync.sh(execFilePath + " " + backupDir + " " + fusionDir + " " + prefix + " " + target);
        }
        if(result.get(0).contains("SUCCEED")){
            return 0;
        } else {
            return -1;
        }
    }
    public static native int unzipFile(String zipFile, String targetDir, String password);
    static{
        System.loadLibrary("ZipUtils");
    }
}

package io.github.xausky.bh3modmanager;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.lody.virtual.client.core.VirtualCore;
import com.topjohnwu.superuser.Shell;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by xausky on 2018/2/1.
 */

public class MainApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            VirtualCore.get().startup(base);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        OutputStream outputStream = null;
        InputStream inputStream = null;
        Shell.setFlags(Shell.FLAG_NON_ROOT_SHELL | Shell.FLAG_REDIRECT_STDERR);
        String execFilePath = getFilesDir().getAbsolutePath() + "/ZipPatch";
        Log.d(MainService.LOG_TAG, "CPU_ABI:" + Build.CPU_ABI);
        try {
            if(Build.CPU_ABI.equals("x86")){
                inputStream = getAssets().open("x86/ZipPatch");
            } else {
                inputStream = getAssets().open("armeabi-v7a/ZipPatch");
            }

            outputStream = new FileOutputStream(execFilePath);
            int len = 0;
            byte[] buffer = new byte[10240];
            while ((len = inputStream.read(buffer)) > 0){
                outputStream.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(outputStream != null){
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        List<String> result = Shell.Sync.sh("chmod 700 " + execFilePath);
        Log.d(MainService.LOG_TAG, result.toString());
        VirtualCore virtualCore = VirtualCore.get();
        virtualCore.initialize(new VirtualCore.VirtualInitializer() {
            @Override
            public void onMainProcess() {
            }
            @Override
            public void onVirtualProcess() {
            }
            @Override
            public void onServerProcess() {
            }
        });
    }
}

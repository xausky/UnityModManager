package io.github.xausky.bh3modmanager;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Looper;
import android.util.Log;

import com.lody.virtual.client.core.CrashHandler;
import com.lody.virtual.client.core.VirtualCore;

import java.util.List;

/**
 * Created by xausky on 2018/2/1.
 */

public class MainApplication extends Application {
    public static final String LOG_TAG = "BH3ModManager";
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
        final VirtualCore virtualCore = VirtualCore.get();
        virtualCore.initialize(new VirtualCore.VirtualInitializer() {
            @Override
            public void onVirtualProcess() {
                virtualCore.setCrashHandler(new CrashHandler() {
                    @Override
                    public void handleUncaughtException(Thread t, Throwable e) {
                        Log.i(LOG_TAG, "uncaught :" + t, e);
                        if (t == Looper.getMainLooper().getThread()) {
                            System.exit(0);
                        } else {
                            Log.e(LOG_TAG, "ignore uncaught exception of thread: " + t);
                        }
                    }
                });
            }

            @Override
            public void onServerProcess() {
                virtualCore.addVisibleOutsidePackage("com.eg.android.AlipayGphone");
                virtualCore.addVisibleOutsidePackage("com.tencent.mm");
            }
        });
    }
}

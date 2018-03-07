package io.github.xausky.bh3modmanager;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Looper;
import android.util.Log;

import com.lody.virtual.client.core.CrashHandler;
import com.lody.virtual.client.core.VirtualCore;

import java.util.List;

import io.github.xausky.bh3modmanager.fragment.BaseFragment;

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
            BaseFragment.initialize(base);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}

package io.github.xausky.unitymodmanager;


import android.app.Application;
import android.content.Context;

import com.crashlytics.android.Crashlytics;
import com.lody.virtual.client.core.VirtualCore;

import io.fabric.sdk.android.Fabric;

/**
 * Created by xausky on 2018/2/1.
 */

public class MainApplication extends Application {
    public static final String LOG_TAG = "UnityModManager";
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            VirtualCore.get().startup(base);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException("virtual app core startup failed:" + throwable.getMessage());
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
    }
}

package io.github.xausky.unitymodmanager;


import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.crashlytics.android.Crashlytics;
import com.lody.virtual.client.NativeEngine;
import com.lody.virtual.client.core.VirtualCore;
import com.lody.virtual.client.stub.VASettings;

import io.fabric.sdk.android.Fabric;
import io.github.xausky.unitymodmanager.fragment.HomeFragment;
import io.github.xausky.unitymodmanager.fragment.SettingFragment;

/**
 * Created by xausky on 2018/2/1.
 */

public class MainApplication extends Application {
    public static final String LOG_TAG = "UnityModManager";
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        SharedPreferences settings = base.getSharedPreferences(SettingFragment.SETTINGS_PREFERENCE_NAME, Context.MODE_PRIVATE);
        if(Integer.valueOf(settings.getString("apk_modify_model", "0")) == HomeFragment.APK_MODIFY_MODEL_VIRTUAL){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                NativeEngine.disableJit(Build.VERSION.SDK_INT);
            }
            VASettings.ENABLE_IO_REDIRECT = true;
            VASettings.ENABLE_INNER_SHORTCUT = false;
            try {
                VirtualCore.get().startup(base);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                throw new RuntimeException("virtual app core startup failed:" + throwable.getMessage());
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());
    }
}

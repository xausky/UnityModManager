package io.github.xausky.unitymodmanager;

import android.app.Application;
import android.content.Context;

import com.lody.virtual.client.core.VirtualCore;

import io.github.xausky.unitymodmanager.fragment.BaseFragment;

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

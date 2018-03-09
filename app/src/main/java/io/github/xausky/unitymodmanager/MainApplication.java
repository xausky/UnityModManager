package io.github.xausky.unitymodmanager;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import com.lody.virtual.client.core.VirtualCore;

import io.github.xausky.unitymodmanager.adapter.VisibilityAdapter;
import io.github.xausky.unitymodmanager.fragment.BaseFragment;

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
            SharedPreferences preferences = base.getSharedPreferences("default", MODE_PRIVATE);
            SharedPreferences visibilityPreferences = base.getSharedPreferences(VisibilityAdapter.VISIBILITY_SHARED_PREFERENCES_KEY, MODE_PRIVATE);
            if(preferences.getBoolean("first", true)){
                //微信和支付宝默认可见（用于氪金）
                visibilityPreferences.edit()
                        .putBoolean("com.eg.android.AlipayGphone", true)
                        .putBoolean("com.tencent.mm", true).apply();
                preferences.edit().putBoolean("first", false).apply();
            }
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
